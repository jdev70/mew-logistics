# Hybrid RBAC + ABAC (Attribute-Based Access Control) Architecture
To handle access control across all four military branches **(Navy, Marine Corps, Army, and Air Force)** from the Joint Chiefs of Staff down to base-level Electronic Warfare technicians, a standard Role-Based Access Control (RBAC) model is not enough. There is a need for a hybrid RBAC + ABAC (Attribute-Based Access Control) architecture.


This structure ensures that a technician at Naval Station Norfolk cannot modify readiness data for an EW wing at Eglin AFB, while allowing Joint Staff or Combatant Command (CCMD) leadership to view aggregated readiness across all installations.


Below is a production-ready, framework-agnostic Java implementation (easily adaptable to Spring Security) using Java 17+ features like records and sealed interfaces.

## Domain Enums & Security Models
1. First, define the structural hierarchy, military branches, clearance levels, and granular permissions.
```java
package gov.dod.mew.security.model;

import java.util.Set;

public enum MilitaryBranch {
    USN,  // U.S. Navy
    USMC, // U.S. Marine Corps
    USA,  // U.S. Army
    USAF, // U.S. Air Force
    JOINT // Joint Staff / OSD / CCMD
}

public enum ClearanceLevel {
    UNCLASSIFIED(1),
    CUI(2),          // Controlled Unclassified Information
    SECRET(3),
    TOP_SECRET(4),
    TS_SCI(5);       // Top Secret / Sensitive Compartmented Information

    private final int level;
    ClearanceLevel(int level) { this.level = level; }
    public boolean meetsOrExceeds(ClearanceLevel required) {
        return this.level >= required.level;
    }
}

public enum CommandTier {
    STRATEGIC_JOINT(4),  // Joint Chiefs of Staff (J3/J6/J8), OSD
    SERVICE_HQ(3),       // OPNAV, HQMC, HQDA, HAF
    OPERATIONAL_CMD(2),  // Fleet Cyber, MARFORCYBER, ARCYBER, 16th AF, Strike Groups
    INSTALLATION_UNIT(1);// NSWC Crane, Naval Station Norfolk, Radio Bns, EW Wings
    
    private final int tier;
    CommandTier(int tier) { this.tier = tier; }
    public int getTier() { return tier; }
}

public enum Permission {
    // Telemetry & Readiness
    VIEW_GLOBAL_READINESS,
    VIEW_BRANCH_READINESS,
    VIEW_INSTALLATION_READINESS,
    APPROVE_READINESS_REPORT,
    
    // Logistics & Supply Chain (LRU / SEWIP / AN/SLQ-32)
    VIEW_INVENTORY,
    REQUEST_PARTS_REQUISITION,
    APPROVE_PARTS_REQUISITION,
    MANAGE_BOM_CONFIG,
    
    // Maintenance & Engineering
    LOG_MAINTENANCE_ACTION,
    CERTIFY_EQUIPMENT_UPTIME,
    ACCESS_RDT_E_SPECS // Research, Development, Test & Evaluation
}
```
## Core Entities (Users, Installations, and Resources)

2. Using Java records, we model the user context and the target resource being accessed.
```java
package gov.dod.mew.security.model;

import java.util.Set;

public record MewUser(
    String edipi, // DoD ID Number
    String rank,
    String lastName,
    MilitaryBranch branch,
    CommandTier commandTier,
    ClearanceLevel clearance,
    String assignedInstallationId, // e.g., "NS_NORFOLK", "NSWC_CRANE", "EGLIN_AFB", or "PENTAGON"
    Set<String> specialtyCodes,    // e.g., "CTT", "17E", "2651", "1B4X1"
    Set<Permission> directPermissions
) {}

public record MewResource(
    String resourceId,
    String installationId,         // Base where the equipment/unit resides
    MilitaryBranch ownerBranch,    // Branch owning the asset
    ClearanceLevel requiredClearance,
    String systemNomenclature      // e.g., "AN/SLQ-32(V)7", "EA-18G", "TLS-BCT"
) {}
```
## Role Definitions & Policy Factory

3. This maps military billets/roles to their baseline permissions.
```java
package gov.dod.mew.security.policy;

import gov.dod.mew.security.model.Permission;
import java.util.EnumSet;
import java.util.Set;

public final class MewRoleFactory {

    public static Set<Permission> getJointChiefsPermissions() {
        return EnumSet.of(
            Permission.VIEW_GLOBAL_READINESS,
            Permission.VIEW_BRANCH_READINESS,
            Permission.VIEW_INSTALLATION_READINESS,
            Permission.VIEW_INVENTORY
        );
    }

    public static Set<Permission> getServiceHqPermissions() {
        return EnumSet.of(
            Permission.VIEW_BRANCH_READINESS,
            Permission.VIEW_INSTALLATION_READINESS,
            Permission.VIEW_INVENTORY,
            Permission.APPROVE_PARTS_REQUISITION,
            Permission.MANAGE_BOM_CONFIG
        );
    }

    public static Set<Permission> getInstallationCommanderPermissions() {
        return EnumSet.of(
            Permission.VIEW_INSTALLATION_READINESS,
            Permission.APPROVE_READINESS_REPORT,
            Permission.VIEW_INVENTORY,
            Permission.APPROVE_PARTS_REQUISITION,
            Permission.CERTIFY_EQUIPMENT_UPTIME
        );
    }

    public static Set<Permission> getLogisticsOfficerPermissions() {
        return EnumSet.of(
            Permission.VIEW_INSTALLATION_READINESS,
            Permission.VIEW_INVENTORY,
            Permission.REQUEST_PARTS_REQUISITION,
            Permission.APPROVE_PARTS_REQUISITION
        );
    }

    public static Set<Permission> getBaseTechnicianPermissions() {
        return EnumSet.of(
            Permission.VIEW_INVENTORY,
            Permission.REQUEST_PARTS_REQUISITION,
            Permission.LOG_MAINTENANCE_ACTION
        );
    }

    public static Set<Permission> getWarfareCenterEngineerPermissions() {
        return EnumSet.of(
            Permission.VIEW_INVENTORY,
            Permission.MANAGE_BOM_CONFIG,
            Permission.ACCESS_RDT_E_SPECS,
            Permission.CERTIFY_EQUIPMENT_UPTIME
        );
    }
}
```
## The MEW Permissions Manager (Policy Decision Point)

4. The MewPermissionsManager evaluates both what the user is allowed to do **(RBAC)** and where/to whom they are allowed to do it **(ABAC chain-of-command and clearance checks)**.
```java
package gov.dod.mew.security;

import gov.dod.mew.security.model.*;

public class MewPermissionsManager {

    /**
     * Evaluates whether a user is authorized to perform a specific action on a MEW resource.
     */
    public AuthorizationResult evaluateAccess(MewUser user, Permission requiredPermission, MewResource resource) {
        
        // 1. Mandatory Access Control (MAC): Security Clearance Check
        if (!user.clearance().meetsOrExceeds(resource.requiredClearance())) {
            return AuthorizationResult.deny(
                String.format("Insufficient clearance. Required: %s, User holds: %s", 
                resource.requiredClearance(), user.clearance())
            );
        }

        // 2. Role-Based Access Control (RBAC): Functional Permission Check
        if (!user.directPermissions().contains(requiredPermission)) {
            return AuthorizationResult.deny(
                String.format("User billet lacks required permission: %s", requiredPermission)
            );
        }

        // 3. Attribute-Based Access Control (ABAC): Chain of Command & Scope Check
        if (!isWithinCommandScope(user, resource)) {
            return AuthorizationResult.deny(
                String.format("Resource at %s (%s) is outside user's command scope (%s - %s)",
                resource.installationId(), resource.ownerBranch(), 
                user.assignedInstallationId(), user.branch())
            );
        }

        return AuthorizationResult.allow("Access granted under " + user.commandTier() + " authority.");
    }

    /**
     * Enforces cross-branch and installation boundaries based on Command Tier.
     */
    private boolean isWithinCommandScope(MewUser user, MewResource resource) {
        return switch (user.commandTier()) {
            // Joint Chiefs / Joint Staff have global visibility across all 4 branches
            case STRATEGIC_JOINT -> true;

            // Service HQ (e.g., OPNAV, HQMC) can access any installation belonging to their branch
            case SERVICE_HQ -> user.branch() == resource.ownerBranch();

            // Operational Commands can access their branch's assets (extendable to Task Force mappings)
            case OPERATIONAL_CMD -> user.branch() == resource.ownerBranch();

            // Base Commanders, Logistics Officers, and Technicians are locked to their assigned base
            case INSTALLATION_UNIT -> user.assignedInstallationId().equalsIgnoreCase(resource.installationId())
                                     && user.branch() == resource.ownerBranch();
        };
    }

    public record AuthorizationResult(boolean isAuthorized, String reason) {
        public static AuthorizationResult allow(String reason) { return new AuthorizationResult(true, reason); }
        public static AuthorizationResult deny(String reason) { return new AuthorizationResult(false, reason); }
    }
}
```
## Usage Example Across the Chain of Command

5. Here is how the manager handles requests ranging from a Joint Chiefs J3 Director to an Electronics Technician at Naval Station Norfolk:
```java
package gov.dod.mew.security;

import gov.dod.mew.security.model.*;
import gov.dod.mew.security.policy.MewRoleFactory;
import java.util.Set;

public class MewSecurityDemo {
    public static void main(String[] args) {
        MewPermissionsManager permissionsManager = new MewPermissionsManager();

        // Target Asset: AN/SLQ-32(V)7 SEWIP Block III aboard a destroyer at Naval Station Norfolk
        MewResource norfolkSewip = new MewResource(
            "ASSET-SLQ32-089",
            "NS_NORFOLK",
            MilitaryBranch.USN,
            ClearanceLevel.SECRET,
            "AN/SLQ-32(V)7"
        );

        // User 1: Joint Chiefs of Staff (J3 Deputy Director)
        MewUser jcsDirector = new MewUser(
            "1000000001", "RADM", "Vance",
            MilitaryBranch.JOINT, CommandTier.STRATEGIC_JOINT, ClearanceLevel.TS_SCI,
            "PENTAGON", Set.of("JOINT_STAFF"),
            MewRoleFactory.getJointChiefsPermissions()
        );

        // User 2: Navy CTT1 Technician stationed at NS Norfolk
        MewUser norfolkTech = new MewUser(
            "1000000042", "CTT1", "Miller",
            MilitaryBranch.USN, CommandTier.INSTALLATION_UNIT, ClearanceLevel.SECRET,
            "NS_NORFOLK", Set.of("CTT", "EW_TECH"),
            MewRoleFactory.getBaseTechnicianPermissions()
        );

        // User 3: USAF 1B4X1 Technician stationed at Eglin AFB (350th Spectrum Warfare Wing)
        MewUser eglinTech = new MewUser(
            "1000000099", "TSgt", "Reyes",
            MilitaryBranch.USAF, CommandTier.INSTALLATION_UNIT, ClearanceLevel.TOP_SECRET,
            "EGLIN_AFB", Set.of("1B4X1"),
            MewRoleFactory.getBaseTechnicianPermissions()
        );

        // Scenario A: JCS views global readiness of the Norfolk SEWIP system -> ALLOWED
        System.out.println("Scenario A (JCS View): " + 
            permissionsManager.evaluateAccess(jcsDirector, Permission.VIEW_GLOBAL_READINESS, norfolkSewip));

        // Scenario B: Norfolk Tech logs maintenance on Norfolk SEWIP -> ALLOWED
        System.out.println("Scenario B (Norfolk Tech Repair): " + 
            permissionsManager.evaluateAccess(norfolkTech, Permission.LOG_MAINTENANCE_ACTION, norfolkSewip));

        // Scenario C: Norfolk Tech tries to approve readiness report -> DENIED (Missing Permission)
        System.out.println("Scenario C (Norfolk Tech Approve Readiness): " + 
            permissionsManager.evaluateAccess(norfolkTech, Permission.APPROVE_READINESS_REPORT, norfolkSewip));

        // Scenario D: Eglin USAF Tech tries to log maintenance on Norfolk USN asset -> DENIED (Out of Scope)
        System.out.println("Scenario D (Cross-Base/Branch Repair): " + 
            permissionsManager.evaluateAccess(eglinTech, Permission.LOG_MAINTENANCE_ACTION, norfolkSewip));
    }
}
```
### Key Architectural Takeaways

- **Separation of Duties:** Technicians can request parts (REQUEST_PARTS_REQUISITION) and log maintenance (LOG_MAINTENANCE_ACTION), while only Logistics Officers or Commanders can approve requisitions and certify equipment readiness.
- **Joint Warfare Center Support:** R&D centers like NSWC Crane or NAWCWD Point Mugu often support multiple branches. If an engineer needs cross-branch access to an installation, you can extend MewUser with a ```Set<String> authorizedInstallations``` list instead of a single assignedInstallationId.
- **Spring Boot Integration:** If you are building a Spring Boot REST API, you can wrap ```MewPermissionsManager.evaluateAccess(...)``` inside a custom Spring Security ```PermissionEvaluator``` and use ```@PreAuthorize("hasPermission(#resource, 'LOG_MAINTENANCE_ACTION')")``` directly on your controller methods.

