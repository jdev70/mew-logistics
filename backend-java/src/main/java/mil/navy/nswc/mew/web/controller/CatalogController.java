package mil.navy.nswc.mew.web.controller;

import mil.navy.nswc.mew.domain.entity.HullSystemAssignment;
import mil.navy.nswc.mew.domain.entity.Installation;
import mil.navy.nswc.mew.domain.entity.MaterialCatalog;
import mil.navy.nswc.mew.domain.entity.NavalHull;
import mil.navy.nswc.mew.domain.entity.Personnel;
import mil.navy.nswc.mew.domain.entity.SecurityAuditLog;
import mil.navy.nswc.mew.repository.HullSystemAssignmentRepository;
import mil.navy.nswc.mew.repository.InstallationRepository;
import mil.navy.nswc.mew.repository.MaterialCatalogRepository;
import mil.navy.nswc.mew.repository.NavalHullRepository;
import mil.navy.nswc.mew.repository.PersonnelRepository;
import mil.navy.nswc.mew.repository.SecurityAuditLogRepository;
import mil.navy.nswc.mew.service.RbacPolicyService;
import mil.navy.nswc.mew.service.RbacPolicyService.Action;
import mil.navy.nswc.mew.service.RbacPolicyService.Resource;
import mil.navy.nswc.mew.service.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {

    private final InstallationRepository installationRepository;
    private final PersonnelRepository personnelRepository;
    private final NavalHullRepository navalHullRepository;
    private final HullSystemAssignmentRepository loadoutRepository;
    private final MaterialCatalogRepository materialRepository;
    private final SecurityAuditLogRepository auditLogRepository;
    private final RbacPolicyService rbacPolicyService;

    public CatalogController(InstallationRepository installationRepository,
                             PersonnelRepository personnelRepository,
                             NavalHullRepository navalHullRepository,
                             HullSystemAssignmentRepository loadoutRepository,
                             MaterialCatalogRepository materialRepository,
                             SecurityAuditLogRepository auditLogRepository,
                             RbacPolicyService rbacPolicyService) {
        this.installationRepository = installationRepository;
        this.personnelRepository = personnelRepository;
        this.navalHullRepository = navalHullRepository;
        this.loadoutRepository = loadoutRepository;
        this.materialRepository = materialRepository;
        this.auditLogRepository = auditLogRepository;
        this.rbacPolicyService = rbacPolicyService;
    }

    @GetMapping({"/installations", "/installations/"})
    public List<Installation> listInstallations() {
        return installationRepository.findAll();
    }

    @PostMapping({"/installations", "/installations/"})
    public ResponseEntity<Installation> createInstallation(
            @RequestHeader(value = "X-MEW-Persona-ID", required = false) String personaId,
            @Valid @RequestBody @NonNull Installation installation) {
        rbacPolicyService.requirePermission(personaId, Resource.CATALOG, Action.CREATE, "/api/v1/installations/");
        installation.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(installationRepository.save(installation));
    }

    @PutMapping({"/installations/{id}", "/installations/{id}/"})
    public Installation updateInstallation(
            @RequestHeader(value = "X-MEW-Persona-ID", required = false) String personaId,
            @PathVariable @NonNull UUID id,
            @Valid @RequestBody @NonNull Installation payload) {
        rbacPolicyService.requirePermission(personaId, Resource.CATALOG, Action.UPDATE, "/api/v1/installations/" + id + "/");
        Installation existing = installationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Installation not found: " + id));
        payload.setId(existing.getId());
        payload.setCreatedAt(existing.getCreatedAt());
        return installationRepository.save(payload);
    }

    @DeleteMapping({"/installations/{id}", "/installations/{id}/"})
    public ResponseEntity<Void> deleteInstallation(
            @RequestHeader(value = "X-MEW-Persona-ID", required = false) String personaId,
            @PathVariable @NonNull UUID id) {
        rbacPolicyService.requirePermission(personaId, Resource.CATALOG, Action.DELETE, "/api/v1/installations/" + id + "/");
        if (!installationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Installation not found: " + id);
        }
        installationRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping({"/personnel", "/personnel/"})
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPersonnel() {
        return personnelRepository.findAll().stream().map(this::toPersonnelDto).collect(Collectors.toList());
    }

    private Map<String, Object> toPersonnelDto(Personnel p) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", p.getId());
        dto.put("personnel_id", p.getPersonnelId());
        dto.put("dod_id", p.getDodId());
        dto.put("first_name", p.getFirstName());
        dto.put("middle_initial", p.getMiddleInitial());
        dto.put("last_name", p.getLastName());
        dto.put("personnel_category", p.getPersonnelCategory());
        dto.put("paygrade", p.getPaygrade());
        dto.put("rank", p.getRank());
        dto.put("rank_full", p.getRank() + " " + p.getFirstName() + " " + p.getLastName());
        dto.put("specialty_code", p.getSpecialtyCode());
        dto.put("title", p.getTitle());
        dto.put("role", p.getTitle());
        dto.put("functional_role", p.getFunctionalRole() != null ? p.getFunctionalRole().name() : "");
        dto.put("assigned_uic", p.getAssignedUic());
        dto.put("installation_code", p.getInstallation() != null ? p.getInstallation().getInstallationCode() : "");
        dto.put("installation_deployed_at", p.getInstallation() != null ? p.getInstallation().getName() : "");
        dto.put("installation_location", p.getInstallation() != null ? p.getInstallation().getLocationDisplay() : "");
        dto.put("unit_name", p.getUnit() != null ? p.getUnit().getName() : "");
        dto.put("security_clearance", p.getSecurityClearance());
        dto.put("operational_status", p.getOperationalStatus());
        dto.put("is_fully_deployable", !"Leave".equalsIgnoreCase(p.getOperationalStatus()));
        return dto;
    }

    @PostMapping({"/personnel", "/personnel/"})
    public ResponseEntity<Personnel> createPersonnel(
            @RequestHeader(value = "X-MEW-Persona-ID", required = false) String personaId,
            @Valid @RequestBody @NonNull Personnel person) {
        rbacPolicyService.requirePermission(personaId, Resource.CATALOG, Action.CREATE, "/api/v1/personnel/");
        person.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(personnelRepository.save(person));
    }

    @PutMapping({"/personnel/{id}", "/personnel/{id}/"})
    public Personnel updatePersonnel(
            @RequestHeader(value = "X-MEW-Persona-ID", required = false) String personaId,
            @PathVariable @NonNull UUID id,
            @Valid @RequestBody @NonNull Personnel payload) {
        rbacPolicyService.requirePermission(personaId, Resource.CATALOG, Action.UPDATE, "/api/v1/personnel/" + id + "/");
        Personnel existing = personnelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personnel not found: " + id));
        payload.setId(existing.getId());
        payload.setCreatedAt(existing.getCreatedAt());
        return personnelRepository.save(payload);
    }

    @DeleteMapping({"/personnel/{id}", "/personnel/{id}/"})
    public ResponseEntity<Void> deletePersonnel(
            @RequestHeader(value = "X-MEW-Persona-ID", required = false) String personaId,
            @PathVariable @NonNull UUID id) {
        rbacPolicyService.requirePermission(personaId, Resource.CATALOG, Action.DELETE, "/api/v1/personnel/" + id + "/");
        if (!personnelRepository.existsById(id)) {
            throw new ResourceNotFoundException("Personnel not found: " + id);
        }
        personnelRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping({"/hulls", "/hulls/"})
    @Transactional(readOnly = true)
    public List<NavalHull> listHulls() {
        return navalHullRepository.findAll();
    }

    @PostMapping({"/hulls", "/hulls/"})
    public ResponseEntity<NavalHull> createHull(
            @RequestHeader(value = "X-MEW-Persona-ID", required = false) String personaId,
            @Valid @RequestBody @NonNull NavalHull hull) {
        rbacPolicyService.requirePermission(personaId, Resource.CATALOG, Action.CREATE, "/api/v1/hulls/");
        hull.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(navalHullRepository.save(hull));
    }

    @PutMapping({"/hulls/{id}", "/hulls/{id}/"})
    public NavalHull updateHull(
            @RequestHeader(value = "X-MEW-Persona-ID", required = false) String personaId,
            @PathVariable @NonNull UUID id,
            @Valid @RequestBody @NonNull NavalHull payload) {
        rbacPolicyService.requirePermission(personaId, Resource.CATALOG, Action.UPDATE, "/api/v1/hulls/" + id + "/");
        NavalHull existing = navalHullRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NavalHull not found: " + id));
        payload.setId(existing.getId());
        payload.setCreatedAt(existing.getCreatedAt());
        return navalHullRepository.save(payload);
    }

    @DeleteMapping({"/hulls/{id}", "/hulls/{id}/"})
    public ResponseEntity<Void> deleteHull(
            @RequestHeader(value = "X-MEW-Persona-ID", required = false) String personaId,
            @PathVariable @NonNull UUID id) {
        rbacPolicyService.requirePermission(personaId, Resource.CATALOG, Action.DELETE, "/api/v1/hulls/" + id + "/");
        if (!navalHullRepository.existsById(id)) {
            throw new ResourceNotFoundException("NavalHull not found: " + id);
        }
        navalHullRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping({"/loadouts", "/loadouts/"})
    @Transactional(readOnly = true)
    public List<HullSystemAssignment> listLoadouts() {
        return loadoutRepository.findAll();
    }

    @GetMapping({"/components", "/components/"})
    @Transactional(readOnly = true)
    public List<MaterialCatalog> listComponents() {
        return materialRepository.findAll();
    }

    @PostMapping({"/components", "/components/"})
    public ResponseEntity<MaterialCatalog> createComponent(
            @RequestHeader(value = "X-MEW-Persona-ID", required = false) String personaId,
            @Valid @RequestBody @NonNull MaterialCatalog component) {
        rbacPolicyService.requirePermission(personaId, Resource.CATALOG, Action.CREATE, "/api/v1/components/");
        component.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(materialRepository.save(component));
    }

    @PutMapping({"/components/{id}", "/components/{id}/"})
    public MaterialCatalog updateComponent(
            @RequestHeader(value = "X-MEW-Persona-ID", required = false) String personaId,
            @PathVariable @NonNull UUID id,
            @Valid @RequestBody @NonNull MaterialCatalog payload) {
        rbacPolicyService.requirePermission(personaId, Resource.CATALOG, Action.UPDATE, "/api/v1/components/" + id + "/");
        MaterialCatalog existing = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MaterialCatalog not found: " + id));
        payload.setId(existing.getId());
        payload.setCreatedAt(existing.getCreatedAt());
        return materialRepository.save(payload);
    }

    @DeleteMapping({"/components/{id}", "/components/{id}/"})
    public ResponseEntity<Void> deleteComponent(
            @RequestHeader(value = "X-MEW-Persona-ID", required = false) String personaId,
            @PathVariable @NonNull UUID id) {
        rbacPolicyService.requirePermission(personaId, Resource.CATALOG, Action.DELETE, "/api/v1/components/" + id + "/");
        if (!materialRepository.existsById(id)) {
            throw new ResourceNotFoundException("MaterialCatalog not found: " + id);
        }
        materialRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping({"/security-audit-logs", "/security-audit-logs/"})
    @Transactional(readOnly = true)
    public List<SecurityAuditLog> listAuditLogs() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc();
    }
}
