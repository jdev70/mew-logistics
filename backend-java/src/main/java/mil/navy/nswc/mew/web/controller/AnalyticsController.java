package mil.navy.nswc.mew.web.controller;

import mil.navy.nswc.mew.domain.entity.Asset;
import mil.navy.nswc.mew.domain.entity.Installation;
import mil.navy.nswc.mew.repository.AssetRepository;
import mil.navy.nswc.mew.repository.InstallationRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/analytics/overview")
public class AnalyticsController {

    private final AssetRepository assetRepository;
    private final InstallationRepository installationRepository;

    public AnalyticsController(AssetRepository assetRepository,
                               InstallationRepository installationRepository) {
        this.assetRepository = assetRepository;
        this.installationRepository = installationRepository;
    }

    @GetMapping({"", "/"})
    @Transactional(readOnly = true)
    public Map<String, Object> getAnalyticsOverview() {
        List<Asset> assets = assetRepository.findAll();
        List<Map<String, Object>> aoBreakdown = assets.stream().map(a -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("system_id", a.getAssetModel() != null ? a.getAssetModel().getSystemId() : "SYS-101");
            item.put("nomenclature", a.getAssetModel() != null ? a.getAssetModel().getNomenclature() : "MEW System");
            item.put("common_name", a.getAssetModel() != null ? a.getAssetModel().getCommonName() : "");
            item.put("domain", a.getHull() != null ? a.getHull().getDomain() : "Surface");
            item.put("total_count", 1);
            item.put("fmc_count", "FMC".equals(a.getReadinessStatus().name()) ? 1 : 0);
            item.put("avg_ao", a.getOperationalAvailabilityPct());
            item.put("avg_mtbf", a.getMtbfHours());
            item.put("avg_mttr", a.getMttrHours());
            return item;
        }).collect(Collectors.toList());

        List<Installation> installations = installationRepository.findAll();
        List<Map<String, Object>> nodes = installations.stream().map(inst -> {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("code", inst.getInstallationCode());
            node.put("name", inst.getName());
            node.put("lat", inst.getLatitude() != null ? inst.getLatitude() : 36.85);
            node.put("lon", inst.getLongitude() != null ? inst.getLongitude() : -76.29);
            node.put("category", inst.getMissionCategory());
            return node;
        }).collect(Collectors.toList());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ao_system_breakdown", aoBreakdown);
        payload.put("component_analytics", Collections.emptyList());
        payload.put("supply_bottleneck_funnel", Collections.emptyList());
        payload.put("nec_staffing_matrix", Collections.emptyList());
        payload.put("geospatial_nodes", nodes);
        payload.put("proximity_depot_routes", Collections.emptyList());
        return payload;
    }
}
