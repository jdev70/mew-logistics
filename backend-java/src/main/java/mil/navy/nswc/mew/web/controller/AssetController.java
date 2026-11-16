package mil.navy.nswc.mew.web.controller;

import mil.navy.nswc.mew.domain.entity.Asset;
import mil.navy.nswc.mew.domain.entity.AssetModel;
import mil.navy.nswc.mew.domain.entity.Installation;
import mil.navy.nswc.mew.domain.entity.MilitaryUnit;
import mil.navy.nswc.mew.domain.entity.MissionRole;
import mil.navy.nswc.mew.domain.enums.CommandEchelon;
import mil.navy.nswc.mew.domain.enums.ReadinessLevel;
import mil.navy.nswc.mew.repository.AssetModelRepository;
import mil.navy.nswc.mew.repository.AssetRepository;
import mil.navy.nswc.mew.repository.InstallationRepository;
import mil.navy.nswc.mew.repository.MilitaryUnitRepository;
import mil.navy.nswc.mew.repository.MissionRoleRepository;
import mil.navy.nswc.mew.service.RbacPolicyService;
import mil.navy.nswc.mew.service.RbacPolicyService.Action;
import mil.navy.nswc.mew.service.RbacPolicyService.Resource;
import mil.navy.nswc.mew.service.exception.BusinessValidationException;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/v1/assets", "/api/v1/equipment"})
public class AssetController {

    private final AssetRepository assetRepository;
    private final AssetModelRepository assetModelRepository;
    private final MilitaryUnitRepository unitRepository;
    private final MissionRoleRepository missionRoleRepository;
    private final InstallationRepository installationRepository;
    private final RbacPolicyService rbacPolicyService;

    public AssetController(AssetRepository assetRepository,
                           AssetModelRepository assetModelRepository,
                           MilitaryUnitRepository unitRepository,
                           MissionRoleRepository missionRoleRepository,
                           InstallationRepository installationRepository,
                           RbacPolicyService rbacPolicyService) {
        this.assetRepository = assetRepository;
        this.assetModelRepository = assetModelRepository;
        this.unitRepository = unitRepository;
        this.missionRoleRepository = missionRoleRepository;
        this.installationRepository = installationRepository;
        this.rbacPolicyService = rbacPolicyService;
    }

    @GetMapping({ "", "/" })
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAssets(
            @RequestParam(value = "readiness_status", required = false) ReadinessLevel readinessStatus,
            @RequestParam(value = "asset_class", required = false) String assetClass) {
        List<Asset> assets;
        if (readinessStatus != null) {
            assets = assetRepository.findByReadinessStatus(readinessStatus);
        } else if (assetClass != null && !assetClass.isBlank()) {
            assets = assetRepository.findByAssetModelAssetClassIgnoreCase(assetClass);
        } else {
            assets = assetRepository.findAll();
        }
        return assets.stream().map(this::toDto).collect(Collectors.toList());
    }

    @GetMapping({ "/{id}", "/{id}/" })
    @Transactional(readOnly = true)
    public Map<String, Object> getAsset(@PathVariable @NonNull UUID id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + id));
        return toDto(asset);
    }

    @PostMapping({ "", "/" })
    @Transactional
    public ResponseEntity<Map<String, Object>> createAsset(
            @RequestHeader(value = "X-MEW-Persona-ID", required = false) String personaId,
            @RequestBody @NonNull Map<String, Object> payload) {
        rbacPolicyService.requirePermission(personaId, Resource.ASSET, Action.CREATE, "/api/v1/assets/");

        String serialNumber = asString(payload.get("serial_number"));
        if (serialNumber == null || serialNumber.isBlank()) {
            throw new BusinessValidationException("serial_number is required.");
        }

        Asset asset = new Asset();
        asset.setSerialNumber(serialNumber.trim());
        asset.setAssignmentId(asString(payload.get("assignment_id")));

        String readiness = asString(payload.get("readiness_status"));
        if (readiness != null && !readiness.isBlank()) {
            asset.setReadinessStatus(ReadinessLevel.valueOf(readiness.trim()));
        }
        String opStatus = asString(payload.get("operational_status"));
        if (opStatus != null && !opStatus.isBlank()) {
            asset.setOperationalStatus(opStatus);
        }
        if (payload.get("quantity_installed") instanceof Number) {
            asset.setQuantityInstalled(((Number) payload.get("quantity_installed")).intValue());
        }
        String instDate = asString(payload.get("installation_date"));
        if (instDate != null && !instDate.isBlank()) {
            asset.setInstallationDate(LocalDate.parse(instDate));
        }
        if (payload.get("engine_hours") != null) {
            asset.setEngineHours(new BigDecimal(String.valueOf(payload.get("engine_hours"))));
        }
        if (payload.get("mtbf_hours") instanceof Number) {
            asset.setMtbfHours(((Number) payload.get("mtbf_hours")).doubleValue());
        }
        if (payload.get("mttr_hours") instanceof Number) {
            asset.setMttrHours(((Number) payload.get("mttr_hours")).doubleValue());
        }
        if (payload.get("operational_availability_pct") instanceof Number) {
            asset.setOperationalAvailabilityPct(((Number) payload.get("operational_availability_pct")).doubleValue());
        }
        String domain = asString(payload.get("domain"));
        if (domain != null && !domain.isBlank()) {
            asset.setDomain(domain.trim());
        }
        String branch = asString(payload.get("service_branch"));
        if (branch != null && !branch.isBlank()) {
            asset.setServiceBranch(branch.trim().toUpperCase());
        } else {
            asset.setServiceBranch(inferServiceBranch(asString(payload.get("assigned_unit_name")),
                    asString(payload.get("serial_number"))));
        }

        asset.setAssetModel(resolveOrCreateAssetModel(payload));
        asset.setMissionRole(resolveOrCreateMissionRole(payload));
        asset.setCurrentLocation(resolveInstallation(payload));
        asset.setAssignedUnit(resolveOrCreateUnit(payload, asset.getCurrentLocation()));

        Asset saved = assetRepository.save(asset);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @PutMapping({ "/{id}", "/{id}/" })
    @Transactional
    @SuppressWarnings("null")
    public Map<String, Object> updateAsset(
            @RequestHeader(value = "X-MEW-Persona-ID", required = false) String personaId,
            @PathVariable @NonNull UUID id,
            @RequestBody @NonNull Map<String, Object> payload) {
        rbacPolicyService.requirePermission(personaId, Resource.ASSET, Action.UPDATE, "/api/v1/assets/" + id + "/");
        Asset existing = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + id));
        if (payload.containsKey("readiness_status") && payload.get("readiness_status") != null) {
            existing.setReadinessStatus(ReadinessLevel.valueOf(String.valueOf(payload.get("readiness_status"))));
        }
        if (payload.containsKey("serial_number") && payload.get("serial_number") != null) {
            existing.setSerialNumber(String.valueOf(payload.get("serial_number")));
        }
        if (payload.containsKey("operational_status") && payload.get("operational_status") != null) {
            existing.setOperationalStatus(String.valueOf(payload.get("operational_status")));
        }
        return toDto(assetRepository.save(existing));
    }

    @DeleteMapping({ "/{id}", "/{id}/" })
    public ResponseEntity<Void> deleteAsset(
            @RequestHeader(value = "X-MEW-Persona-ID", required = false) String personaId,
            @PathVariable @NonNull UUID id) {
        rbacPolicyService.requirePermission(personaId, Resource.ASSET, Action.DELETE, "/api/v1/assets/" + id + "/");
        if (!assetRepository.existsById(id)) {
            throw new ResourceNotFoundException("Asset not found: " + id);
        }
        assetRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private AssetModel resolveOrCreateAssetModel(Map<String, Object> payload) {
        String systemId = asString(payload.get("system_id"));
        String nomenclature = asString(payload.get("nomenclature"));
        if (systemId != null && !systemId.isBlank()) {
            return assetModelRepository.findBySystemId(systemId).orElseGet(() -> {
                AssetModel m = new AssetModel();
                m.setSystemId(systemId);
                m.setNomenclature(nomenclature != null ? nomenclature : systemId);
                m.setCommonName(defaultStr(asString(payload.get("common_name"))));
                m.setEwCategory(defaultStr(asString(payload.get("ew_category"))));
                m.setAssetClass(defaultStr(asString(payload.get("asset_class"))));
                m.setManufacturer(defaultStr(asString(payload.get("manufacturer"))));
                m.setCageCode(defaultStr(asString(payload.get("cage_code"))));
                return assetModelRepository.save(m);
            });
        }
        return assetModelRepository.findAll().stream().findFirst().orElse(null);
    }

    private MissionRole resolveOrCreateMissionRole(Map<String, Object> payload) {
        String roleCode = asString(payload.get("mission_role_code"));
        if (roleCode == null || roleCode.isBlank()) {
            return null;
        }
        return missionRoleRepository.findByRoleCode(roleCode).orElseGet(() -> {
            MissionRole r = new MissionRole();
            r.setRoleId("ROLE-" + roleCode);
            r.setRoleCode(roleCode);
            String roleName = asString(payload.get("mission_role_name"));
            r.setRoleName(roleName != null && !roleName.isBlank() ? roleName : roleCode);
            return missionRoleRepository.save(r);
        });
    }

    @SuppressWarnings("null")
    private Installation resolveInstallation(Map<String, Object> payload) {
        String locId = asString(payload.get("current_location_id"));
        if (locId != null && !locId.isBlank()) {
            try {
                UUID uuid = UUID.fromString(locId);
                return installationRepository.findById(uuid).orElse(null);
            } catch (IllegalArgumentException ignored) {
                // Ignore non-UUID placeholders such as REPLACE-WITH-...
            }
        }
        String instCode = asString(payload.get("installation_code"));
        if (instCode != null && !instCode.isBlank()) {
            return installationRepository.findByInstallationCode(instCode).orElse(null);
        }
        return null;
    }

    private MilitaryUnit resolveOrCreateUnit(Map<String, Object> payload, Installation preferredInst) {
        String unitName = asString(payload.get("assigned_unit_name"));
        if (unitName != null && !unitName.isBlank()) {
            return unitRepository.findAll().stream()
                    .filter(u -> unitName.equalsIgnoreCase(u.getName()))
                    .findFirst()
                    .orElseGet(() -> {
                        Installation inst = preferredInst != null ? preferredInst
                                : installationRepository.findAll().stream().findFirst().orElse(null);
                        if (inst == null) {
                            return null;
                        }
                        MilitaryUnit u = new MilitaryUnit();
                        u.setName(unitName);
                        u.setUic(("U" + Math.abs(unitName.hashCode())).substring(0, 6));
                        u.setEchelonLevel(CommandEchelon.COMBATANT_UNIT);
                        u.setInstallation(inst);
                        u.setCommandType(defaultStr(asString(payload.get("domain"))));
                        return unitRepository.save(u);
                    });
        }
        return unitRepository.findAll().stream().findFirst().orElse(null);
    }

    private String asString(Object val) {
        return val != null ? String.valueOf(val) : null;
    }

    private String defaultStr(String val) {
        return val != null ? val : "";
    }

    private Map<String, Object> toDto(Asset a) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", a.getId());
        dto.put("assignment_id", a.getAssignmentId());
        dto.put("serial_number", a.getSerialNumber());
        dto.put("system_id", a.getAssetModel() != null ? a.getAssetModel().getSystemId() : null);
        dto.put("nomenclature", a.getAssetModel() != null ? a.getAssetModel().getNomenclature() : "MEW System");
        dto.put("common_name", a.getAssetModel() != null ? a.getAssetModel().getCommonName() : "");
        dto.put("ew_category", a.getAssetModel() != null ? a.getAssetModel().getEwCategory() : "");
        dto.put("asset_class", a.getAssetModel() != null ? a.getAssetModel().getAssetClass() : "");
        dto.put("manufacturer", a.getAssetModel() != null ? a.getAssetModel().getManufacturer() : "");
        dto.put("cage_code", a.getAssetModel() != null ? a.getAssetModel().getCageCode() : "");
        dto.put("hull_number", a.getHull() != null ? a.getHull().getHullNumber() : null);
        dto.put("vessel_name", a.getHull() != null ? a.getHull().getVesselName() : null);
        dto.put("ship_class", a.getHull() != null ? a.getHull().getShipClass() : null);
        dto.put("domain", resolveDomain(a));
        dto.put("service_branch", a.getServiceBranch() != null && !a.getServiceBranch().isBlank()
                ? a.getServiceBranch()
                : inferServiceBranch(
                        a.getAssignedUnit() != null ? a.getAssignedUnit().getName() : null,
                        a.getSerialNumber()));
        dto.put("emcon_posture", a.getHull() != null && a.getHull().getEmconPosture() != null
                ? a.getHull().getEmconPosture().name() : null);
        dto.put("mission_role_code", a.getMissionRole() != null ? a.getMissionRole().getRoleCode() : null);
        dto.put("mission_role_name", a.getMissionRole() != null ? a.getMissionRole().getRoleName() : null);
        dto.put("quantity_installed", a.getQuantityInstalled());
        dto.put("operational_status", a.getOperationalStatus());
        dto.put("installation_date", a.getInstallationDate());
        dto.put("assigned_unit_name", a.getAssignedUnit() != null ? a.getAssignedUnit().getName() : "");
        if (a.getCurrentLocation() != null) {
            Map<String, Object> loc = new LinkedHashMap<>();
            loc.put("id", a.getCurrentLocation().getId());
            loc.put("name", a.getCurrentLocation().getName());
            loc.put("location_display", a.getCurrentLocation().getLocationDisplay());
            dto.put("current_location", loc);
        } else {
            dto.put("current_location", null);
        }
        dto.put("readiness_status", a.getReadinessStatus().name());
        dto.put("readiness_status_display", a.getReadinessStatus().getLabel());
        dto.put("engine_hours", a.getEngineHours());
        dto.put("last_status_change", a.getLastStatusChange());
        dto.put("mtbf_hours", a.getMtbfHours());
        dto.put("mttr_hours", a.getMttrHours());
        dto.put("operational_availability_pct", a.getOperationalAvailabilityPct());
        dto.put("active_work_orders", Collections.emptyList());
        dto.put("pending_requisitions", Collections.emptyList());
        return dto;
    }

    private String resolveDomain(Asset a) {
        if (a.getDomain() != null && !a.getDomain().isBlank()) {
            return a.getDomain();
        }
        if (a.getHull() != null && a.getHull().getDomain() != null && !a.getHull().getDomain().isBlank()) {
            return a.getHull().getDomain();
        }
        if (a.getAssignedUnit() != null && a.getAssignedUnit().getCommandType() != null) {
            String ct = a.getAssignedUnit().getCommandType();
            if ("Ground".equalsIgnoreCase(ct) || "Airborne".equalsIgnoreCase(ct)
                    || "Surface".equalsIgnoreCase(ct) || "Subsurface".equalsIgnoreCase(ct)) {
                return ct;
            }
        }
        return "Surface";
    }

    private String inferServiceBranch(String unitName, String serial) {
        String combined = ((unitName != null ? unitName : "") + " " + (serial != null ? serial : "")).toUpperCase();
        if (combined.contains("RADBN") || combined.contains("VMGR") || combined.contains("MARFOR")
                || combined.contains("MEWG") || combined.contains("CESAS")) {
            return "USMC";
        }
        if (combined.contains("CAVALRY") || combined.contains("CYBER") || combined.contains("AIRBORNE")
                || combined.contains("TLS") || combined.contains("BCT")) {
            return "USA";
        }
        if (combined.contains("ECS") || combined.contains("SWW") || combined.contains("AGRS")
                || combined.contains("EA37B") || combined.contains("ALQ250")) {
            return "USAF";
        }
        return "USN";
    }
}
