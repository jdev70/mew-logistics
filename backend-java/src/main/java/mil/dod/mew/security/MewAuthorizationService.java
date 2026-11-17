package mil.dod.mew.security;

import mil.dod.mew.security.model.MewResource;
import mil.dod.mew.security.model.Permission;
import mil.dod.mew.service.exception.AccessDeniedException;
import org.springframework.stereotype.Service;

/** Application-facing authorization service for permission and resource-scope decisions. */
@Service
public class MewAuthorizationService {
    private final AuthenticatedMewUserResolver userResolver;
    private final MewPermissionsManager permissionsManager;

    public MewAuthorizationService(AuthenticatedMewUserResolver userResolver) {
        this.userResolver = userResolver;
        this.permissionsManager = new MewPermissionsManager();
    }

    public void requirePermission(Permission permission) {
        if (!userResolver.currentUser().permissions().contains(permission)) {
            throw new AccessDeniedException("Missing permission: " + permission + ".");
        }
    }

    public void requireAccess(Permission permission, MewResource resource) {
        MewPermissionsManager.AuthorizationResult result = permissionsManager.evaluateAccess(
                userResolver.currentUser(), permission, resource);
        if (!result.authorized()) {
            throw new AccessDeniedException(result.reason());
        }
    }
}
