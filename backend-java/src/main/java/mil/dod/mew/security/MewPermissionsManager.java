package mil.dod.mew.security;
import mil.dod.mew.security.model.*;
/** Policy-decision point ported from Django's AccessContext scope rules. */
public class MewPermissionsManager {
    public AuthorizationResult evaluateAccess(MewUser user, Permission permission, MewResource resource) {
        if (!user.clearance().meetsOrExceeds(resource.requiredClearance())) return AuthorizationResult.deny("Insufficient clearance.");
        if (!user.permissions().contains(permission)) return AuthorizationResult.deny("Missing permission: " + permission + ".");
        if (!user.authorizedCageCodes().isEmpty() && (resource.cageCode() == null || !user.authorizedCageCodes().contains(resource.cageCode()))) return AuthorizationResult.deny("Resource CAGE is outside the caller's authorized scope.");
        boolean inScope = switch (user.commandTier()) {
            case STRATEGIC_JOINT -> true;
            case SERVICE_HQ, OPERATIONAL_CMD -> user.branch() == resource.ownerBranch();
            case INSTALLATION_UNIT -> user.branch() == resource.ownerBranch() && (user.assignedInstallationCode().equalsIgnoreCase(resource.installationCode()) || user.authorizedInstallations().contains(resource.installationCode()));
        };
        return inScope ? AuthorizationResult.allow("Access granted.") : AuthorizationResult.deny("Resource is outside the caller's command scope.");
    }
    public record AuthorizationResult(boolean authorized, String reason) { public static AuthorizationResult allow(String reason) { return new AuthorizationResult(true, reason); } public static AuthorizationResult deny(String reason) { return new AuthorizationResult(false, reason); } }
}
