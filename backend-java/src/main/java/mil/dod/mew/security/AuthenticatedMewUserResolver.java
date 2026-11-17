package mil.dod.mew.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import mil.dod.mew.domain.entity.Personnel;
import mil.dod.mew.repository.PersonnelRepository;
import mil.dod.mew.security.model.ClearanceLevel;
import mil.dod.mew.security.model.CommandTier;
import mil.dod.mew.security.model.MewUser;
import mil.dod.mew.security.model.MilitaryBranch;
import mil.dod.mew.security.model.Permission;
import mil.dod.mew.service.exception.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Resolves a verified identity subject to the persisted MEW authorization assignment. */
@Component
public class AuthenticatedMewUserResolver {
    private final PersonnelRepository personnelRepository;

    public AuthenticatedMewUserResolver(PersonnelRepository personnelRepository) {
        this.personnelRepository = personnelRepository;
    }

    @Transactional(readOnly = true, noRollbackFor = AccessDeniedException.class)
    public MewUser currentUser() {
        Personnel personnel = currentPersonnel();
        return new MewUser(personnel.getIdentitySubject(), enumValue(personnel.getServiceBranch(), MilitaryBranch.class, "service branch"),
                enumValue(personnel.getCommandTier(), CommandTier.class, "command tier"),
                enumValue(personnel.getSecurityClearance(), ClearanceLevel.class, "security clearance"),
                personnel.getInstallation() == null ? "" : personnel.getInstallation().getInstallationCode(),
                stringSet(personnel.getAuthorizedInstallations()), stringSet(personnel.getAuthorizedCageCodes()),
                permissions(personnel.getAuthorizationPermissions()));
    }

    @Transactional(readOnly = true)
    public Personnel currentPersonnel() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("An authenticated identity is required.");
        }
        String subject = authentication.getName();
        if (subject == null || subject.isBlank()) {
            throw new AccessDeniedException("Authenticated identity has no subject.");
        }
        return personnelRepository.findByIdentitySubject(subject)
                .orElseThrow(() -> new AccessDeniedException("No active MEW authorization assignment exists for this identity."));
    }

    private Set<String> stringSet(String raw) {
        if (raw == null || raw.isBlank() || "[]".equals(raw.trim())) {
            return Collections.emptySet();
        }
        return java.util.Arrays.stream(raw.replace("[", "").replace("]", "").replace("\"", "").split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }

    private Set<Permission> permissions(String raw) {
        if (raw != null && "ALL".equalsIgnoreCase(raw.trim())) {
            return Collections.unmodifiableSet(EnumSet.allOf(Permission.class));
        }
        EnumSet<Permission> permissions = EnumSet.noneOf(Permission.class);
        for (String value : stringSet(raw)) {
            permissions.add(enumValue(value, Permission.class, "authorization permission"));
        }
        return Collections.unmodifiableSet(permissions);
    }

    private <T extends Enum<T>> T enumValue(String raw, Class<T> type, String label) {
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase().replace('-', '_').replace('/', '_'));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new AccessDeniedException("Invalid persisted " + label + ".");
        }
    }
}
