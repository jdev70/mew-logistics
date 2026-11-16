package mil.navy.nswc.mew.service;

import mil.navy.nswc.mew.domain.entity.SecurityAuditLog;
import mil.navy.nswc.mew.repository.SecurityAuditLogRepository;
import mil.navy.nswc.mew.service.exception.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Enforces Hybrid RBAC + ABAC CRUD policies based on the active X-MEW-Persona-ID.
 */
@Service
public class RbacPolicyService {

    public enum Resource {
        ASSET, WORK_ORDER, REQUISITION, CATALOG
    }

    public enum Action {
        CREATE, UPDATE, DELETE
    }

    public static class PersonaProfile {
        public final String personnelId;
        public final String name;
        public final String rank;
        public final String title;
        public final String functionalRole;
        public final String personnelCategory;
        public final String securityClearance;
        public final String assignedUic;
        public final String installationCode;
        public final String targetHullNumber;
        public final List<String> authorizedCageCodes;
        public final String badgeLabel;

        public PersonaProfile(String personnelId, String name, String rank, String title,
                              String functionalRole, String personnelCategory, String securityClearance,
                              String assignedUic, String installationCode, String targetHullNumber,
                              List<String> authorizedCageCodes, String badgeLabel) {
            this.personnelId = personnelId;
            this.name = name;
            this.rank = rank;
            this.title = title;
            this.functionalRole = functionalRole;
            this.personnelCategory = personnelCategory;
            this.securityClearance = securityClearance;
            this.assignedUic = assignedUic;
            this.installationCode = installationCode;
            this.targetHullNumber = targetHullNumber;
            this.authorizedCageCodes = authorizedCageCodes;
            this.badgeLabel = badgeLabel;
        }
    }

    private final SecurityAuditLogRepository auditLogRepository;
    private final Map<String, PersonaProfile> personas = new LinkedHashMap<>();

    public RbacPolicyService(SecurityAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
        registerPersonas();
    }

    private void registerPersonas() {
        personas.put("MEW-PER-0000", new PersonaProfile(
                "MEW-PER-0000", "CIV (GS-15) Evelyn Thorne", "CIV (GS-15)",
                "PEO IWS 2.0 / NSWC Crane Enterprise MEW Product Support Manager (PSM)",
                "FLEET_READINESS_ANALYST", "DoD_Civilian", "TS/SCI",
                "N00060", "NSWC-CRANE", null, Collections.emptyList(),
                "Enterprise PSM / ORSA Lead (Cross-Fleet Analytics & BOM | TS/SCI)"));
        personas.put("MEW-PER-0001", new PersonaProfile(
                "MEW-PER-0001", "CDR Marcus Vance", "CDR",
                "TYCOM Director of Maritime Electromagnetic Maneuver Warfare",
                "FLEET_READINESS_ANALYST", "Active_Duty_Officer", "TS/SCI",
                "N00060", "NAVIFOR-SUF", null, Collections.emptyList(),
                "TYCOM Command Staff (Full Echelon Tree | TS/SCI)"));
        personas.put("MEW-PER-0002", new PersonaProfile(
                "MEW-PER-0002", "CTT1 Devon Mercer", "CTT1",
                "Shipboard EW Work Center Supervisor (CE02)",
                "SHIPBOARD_TECHNICIAN", "Active_Duty_Enlisted", "TS/SCI",
                "N20001", "NAVSTA-NORVA", "DDG-51", Collections.emptyList(),
                "Shipboard Tech (Isolated to DDG-51 UIC | TS/SCI)"));
        personas.put("MEW-PER-0003", new PersonaProfile(
                "MEW-PER-0003", "LT Hannah Sterling", "LT",
                "Squadron Supply & AVCAL Spares Officer (VAQ-132)",
                "UNIT_SUPPO", "Active_Duty_Officer", "Secret",
                "N20009", "NAS-WHIDBEY", "VAQ-132", Collections.emptyList(),
                "Unit SUPPO (Isolated to VAQ-132 UIC | Secret)"));
        personas.put("MEW-PER-0004", new PersonaProfile(
                "MEW-PER-0004", "CIV (GS-12) Arthur Keller", "CIV (GS-12)",
                "NSWC Crane Depot Provisioning & DLR Turn-In Manager",
                "SHORE_DEPOT_MANAGER", "DoD_Civilian", "Secret",
                "N00164", "NSWC-CRANE", null, Collections.emptyList(),
                "Shore Depot Manager (Warehouse Scope | Coords Redacted)"));
        personas.put("MEW-PER-0005", new PersonaProfile(
                "MEW-PER-0005", "CTR Elena Rostova", "CTR",
                "Lockheed Martin SEWIP Field Service Representative (FSR)",
                "WARFARE_CENTER_ENGINEER", "Defense_Contractor", "Secret",
                "N00164", "NSWC-CRANE", null, Collections.singletonList("03538"),
                "OEM Contractor (CAGE 03538 Scope Only | Platforms Redacted)"));
    }

    public PersonaProfile resolvePersona(String personaId) {
        if (personaId != null && personas.containsKey(personaId)) {
            return personas.get(personaId);
        }
        return personas.get("MEW-PER-0000");
    }

    public boolean isAllowed(PersonaProfile persona, Resource resource, Action action) {
        String role = persona.functionalRole;
        switch (role) {
            case "FLEET_READINESS_ANALYST":
                return true;
            case "SHIPBOARD_TECHNICIAN":
                return (resource == Resource.WORK_ORDER && (action == Action.CREATE || action == Action.UPDATE))
                        || (resource == Resource.ASSET && action == Action.UPDATE);
            case "UNIT_SUPPO":
                return resource == Resource.REQUISITION && (action == Action.CREATE || action == Action.UPDATE);
            case "SHORE_DEPOT_MANAGER":
                return (resource == Resource.CATALOG || resource == Resource.REQUISITION)
                        && (action == Action.CREATE || action == Action.UPDATE);
            case "WARFARE_CENTER_ENGINEER":
                return (resource == Resource.WORK_ORDER && action == Action.CREATE)
                        || (resource == Resource.ASSET && action == Action.UPDATE);
            default:
                return false;
        }
    }

    public void requirePermission(String personaId, Resource resource, Action action, String target) {
        PersonaProfile persona = resolvePersona(personaId);
        if (!isAllowed(persona, resource, action)) {
            String reason = String.format(
                    "RBAC/ABAC denied %s %s for persona %s (%s).",
                    action, resource, persona.personnelId, persona.functionalRole);
            logAudit(persona, "ACCESS_DENIED", "DENY", target, reason);
            throw new AccessDeniedException(reason);
        }
        logAudit(persona, "MAF_SIGN_OFF", "ALLOW", target,
                String.format("Authorized %s %s for %s.", action, resource, persona.personnelId));
    }

    private void logAudit(PersonaProfile persona, String eventType, String decision, String target, String reason) {
        SecurityAuditLog log = new SecurityAuditLog();
        log.setActorName(persona.name);
        log.setActorRole(persona.functionalRole);
        log.setActorUic(persona.assignedUic);
        log.setActorClearance(persona.securityClearance);
        log.setActorCageScope(String.join(",", persona.authorizedCageCodes));
        log.setEventType(eventType);
        log.setDecision(decision);
        log.setResourceTarget(target);
        log.setReason(reason);
        auditLogRepository.save(log);
    }

    public Map<String, Object> buildPersonasPayload(String requestedPersonaId) {
        PersonaProfile active = resolvePersona(requestedPersonaId);

        Map<String, Object> activeContext = new LinkedHashMap<>();
        activeContext.put("persona_id", active.personnelId);
        activeContext.put("actor_name", active.name);
        activeContext.put("functional_role", active.functionalRole);
        activeContext.put("personnel_category", active.personnelCategory);
        activeContext.put("security_clearance", active.securityClearance);
        activeContext.put("assigned_uic", active.assignedUic);
        activeContext.put("installation_code", active.installationCode);
        activeContext.put("subordinate_uics", Collections.emptyList());
        activeContext.put("authorized_cage_codes", active.authorizedCageCodes);
        activeContext.put("can_export_csv", "FLEET_READINESS_ANALYST".equals(active.functionalRole));
        activeContext.put("active_redaction_policies", Arrays.asList(
                "HIERARCHICAL ECHELON TREE ACCESS",
                "PERSONA CRUD POLICY: " + active.functionalRole
        ));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("active_context", activeContext);
        payload.put("personas", personas.values());
        return payload;
    }
}
