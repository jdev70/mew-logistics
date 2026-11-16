package mil.navy.nswc.mew.web.controller;

import mil.navy.nswc.mew.domain.entity.Asset;
import mil.navy.nswc.mew.domain.enums.ReadinessLevel;
import mil.navy.nswc.mew.repository.AssetModelRepository;
import mil.navy.nswc.mew.repository.AssetRepository;
import mil.navy.nswc.mew.repository.MilitaryUnitRepository;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

    private final AssetRepository assetRepository;
    private final AssetModelRepository assetModelRepository;
    private final MilitaryUnitRepository unitRepository;
    private final RbacPolicyService rbacPolicyService;

    public AssetController(AssetRepository assetRepository,
                           AssetModelRepository assetModelRepository,
                           MilitaryUnitRepository unitRepository,
                           RbacPolicyService rbacPolicyService) {
        this.assetRepository = assetRepository;
        this.assetModelRepository = assetModelRepository;
        this.unitRepository = unitRepository;
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
            @Valid @RequestBody @NonNull Asset asset) {
        rbacPolicyService.requirePermission(personaId, Resource.ASSET, Action.CREATE, "/api/v1/assets/");
        asset.setId(null);
        if (asset.getAssetModel() == null) {
            assetModelRepository.findAll().stream().findFirst().ifPresent(asset::setAssetModel);
        }
        if (asset.getAssignedUnit() == null) {
            unitRepository.findAll().stream().findFirst().ifPresent(asset::setAssignedUnit);
        }
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
        dto.put("domain", a.getHull() != null ? a.getHull().getDomain() : "Surface");
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
}
