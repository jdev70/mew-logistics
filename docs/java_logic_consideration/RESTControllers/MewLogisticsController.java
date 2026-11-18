package com.defense.mew.controller;

import com.defense.mew.dto.AssetReadinessUpdateRequest;
import com.defense.mew.dto.EwLogisticsAssetDto;
import com.defense.mew.entity.EwLogisticsAsset;
import com.defense.mew.entity.PersonnelRbac;
import com.defense.mew.repository.EwLogisticsAssetRepository;
import com.defense.mew.security.MewAccessGuard;
import com.defense.mew.security.RbacContextFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/mew/assets")
@RequiredArgsConstructor
public class MewLogisticsController {

    private final EwLogisticsAssetRepository assetRepository;
    private final MewAccessGuard accessGuard;

    /**
     * Returns all EW/MEW assets the authenticated user is cleared and authorized to
     * view.
     * Automatically filters out TS//SCI or SECRET//NOFORN items if user lacks
     * clearance/SCIF badge.
     */
    @GetMapping
    public ResponseEntity<List<EwLogisticsAssetDto>> getAuthorizedAssets(
            @RequestParam(required = false) String installationCode,
            @RequestParam(required = false) String uic,
            @RequestParam(required = false) String readinessStatus,
            HttpServletRequest request) {
        PersonnelRbac currentUser = getCurrentUser(request);

        List<EwLogisticsAsset> candidates;
        if (uic != null && !uic.isBlank()) {
            candidates = assetRepository.findByUnitUic(uic);
        } else if (installationCode != null && !installationCode.isBlank()) {
            candidates = assetRepository.findByInstallationCode(installationCode);
        } else {
            candidates = assetRepository.findAll();
        }

        List<EwLogisticsAssetDto> authorizedDtos = candidates.stream()
                .filter(asset -> readinessStatus == null
                        || asset.getReadinessStatus().equalsIgnoreCase(readinessStatus))
                .filter(asset -> accessGuard.canViewAsset(currentUser, asset))
                .map(EwLogisticsAssetDto::fromEntity)
                .toList();

        return ResponseEntity.ok(authorizedDtos);
    }

    /**
     * Retrieves a single serialized EW asset if the user passes clearance & RBAC
     * checks.
     */
    @GetMapping("/{assetId}")
    public ResponseEntity<EwLogisticsAssetDto> getAssetById(
            @PathVariable String assetId,
            HttpServletRequest request) {
        PersonnelRbac currentUser = getCurrentUser(request);
        EwLogisticsAsset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found: " + assetId));

        accessGuard.enforceAssetReadAccess(currentUser, asset);
        return ResponseEntity.ok(EwLogisticsAssetDto.fromEntity(asset));
    }

    /**
     * Updates readiness status, ERO work order, and crypto-load state.
     * Requires both classification clearance AND write-capable RBAC role
     * (Custodian, S-4, or Approver).
     */
    @PatchMapping("/{assetId}/readiness")
    public ResponseEntity<EwLogisticsAssetDto> updateAssetReadiness(
            @PathVariable String assetId,
            @Valid @RequestBody AssetReadinessUpdateRequest updateRequest,
            HttpServletRequest request) {
        PersonnelRbac currentUser = getCurrentUser(request);
        EwLogisticsAsset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found: " + assetId));

        accessGuard.enforceAssetWriteAccess(currentUser, asset);

        asset.setReadinessStatus(updateRequest.readinessStatus());
        asset.setGcssEroWorkOrder(
                "FMC".equalsIgnoreCase(updateRequest.readinessStatus()) ? "NONE" : updateRequest.gcssEroWorkOrder());
        asset.setCryptoKeyLoaded(updateRequest.cryptoKeyLoaded());
        asset.setLastPmcsDate(LocalDate.now());

        EwLogisticsAsset saved = assetRepository.save(asset);
        return ResponseEntity.ok(EwLogisticsAssetDto.fromEntity(saved));
    }

    private PersonnelRbac getCurrentUser(HttpServletRequest request) {
        return (PersonnelRbac) request.getAttribute(RbacContextFilter.CURRENT_USER_ATTR);
    }
}
