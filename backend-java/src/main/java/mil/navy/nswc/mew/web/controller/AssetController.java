package mil.navy.nswc.mew.web.controller;

import mil.navy.nswc.mew.domain.entity.Asset;
import mil.navy.nswc.mew.domain.enums.ReadinessLevel;
import mil.navy.nswc.mew.repository.AssetRepository;
import mil.navy.nswc.mew.service.exception.ResourceNotFoundException;

import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

    private final AssetRepository assetRepository;

    public AssetController(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @GetMapping({ "", "/" })
    public List<Asset> listAssets(
            @RequestParam(value = "readiness_status", required = false) ReadinessLevel readinessStatus,
            @RequestParam(value = "asset_class", required = false) String assetClass) {
        if (readinessStatus != null) {
            return assetRepository.findByReadinessStatus(readinessStatus);
        }
        if (assetClass != null && !assetClass.isBlank()) {
            return assetRepository.findByAssetModelAssetClassIgnoreCase(assetClass);
        }
        return assetRepository.findAll();
    }

    @GetMapping({ "/{id}", "/{id}/" })
    public Asset getAsset(@PathVariable @NonNull UUID id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + id));
    }
}
