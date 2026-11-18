package com.defense.mew.security;

import com.defense.mew.entity.EwLogisticsAsset;
import com.defense.mew.entity.PersonnelRbac;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

@Component
public class MewAccessGuard {

    private static final Map<String, Integer> CLEARANCE_RANK = Map.of(
            "UNCLASSIFIED", 0,
            "CONFIDENTIAL", 1,
            "SECRET", 2,
            "SECRET//NOFORN", 2,
            "TS/SCI", 3,
            "TS//SCI", 3);

    private static final Set<String> LOGISTICS_WRITE_ROLES = Set.of(
            "COMMAND_APPROVER",
            "S4_LOGISTICS_OFFICER",
            "EW_ASSET_CUSTODIAN");

    /**
     * Evaluates if a user's clearance, SCIF badge, and account status permit
     * viewing an asset.
     */
    public boolean canViewAsset(PersonnelRbac user, EwLogisticsAsset asset) {
        if (user == null || !"ACTIVE".equalsIgnoreCase(user.getAccountStatus())) {
            return false;
        }

        String marking = asset.getClassificationMarking().toUpperCase().trim();
        int userLevel = CLEARANCE_RANK.getOrDefault(user.getClearanceLevel().toUpperCase().trim(), 0);
        int requiredLevel = CLEARANCE_RANK.getOrDefault(marking, 99);

        if (userLevel < requiredLevel) {
            return false;
        }

        // TS//SCI assets strictly require an active SCIF badge
        if (marking.contains("TS//SCI") || marking.contains("TS/SCI")) {
            if (!Boolean.TRUE.equals(user.getScifBadgeActive())) {
                return false;
            }
        }

        // Multi-tenant / Joint boundary check:
        // COMMAND_APPROVER and SCIF_SYSADMIN have theater/joint visibility;
        // standard operators and custodians are scoped to their service branch or unit
        // UIC.
        if ("COMMAND_APPROVER".equals(user.getRbacRole()) || "SCIF_SYSADMIN".equals(user.getRbacRole())) {
            return true;
        }

        return user.getServiceBranch().equalsIgnoreCase(asset.getServiceBranch());
    }

    /**
     * Throws 403 Forbidden if the user cannot view the requested asset.
     */
    public void enforceAssetReadAccess(PersonnelRbac user, EwLogisticsAsset asset) {
        if (!canViewAsset(user, asset)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    String.format("Access Denied: Clearance [%s] or Role [%s] insufficient for asset [%s] (%s).",
                            user.getClearanceLevel(), user.getRbacRole(), asset.getAssetId(),
                            asset.getClassificationMarking()));
        }
    }

    /**
     * Enforces both read clearance AND write/custody RBAC role before modifying
     * asset readiness.
     */
    public void enforceAssetWriteAccess(PersonnelRbac user, EwLogisticsAsset asset) {
        enforceAssetReadAccess(user, asset);

        if (!LOGISTICS_WRITE_ROLES.contains(user.getRbacRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    String.format("Access Denied: RBAC Role [%s] does not have inventory write privileges.",
                            user.getRbacRole()));
        }

        // Non-approvers must belong to the owning unit UIC to modify asset status
        if (!"COMMAND_APPROVER".equals(user.getRbacRole())
                && !user.getUnit().getUic().equalsIgnoreCase(asset.getUnit().getUic())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    String.format("Access Denied: User UIC [%s] cannot modify assets assigned to UIC [%s].",
                            user.getUnit().getUic(), asset.getUnit().getUic()));
        }
    }
}
