package mil.dod.mew.security.model;
import java.util.Set;
/** Immutable authorization context derived from verified identity and persisted assignments. */
public record MewUser(String subject, MilitaryBranch branch, CommandTier commandTier, ClearanceLevel clearance, String assignedInstallationCode, Set<String> authorizedInstallations, Set<String> authorizedCageCodes, Set<Permission> permissions) { }
