package mil.dod.mew.web.controller;

import mil.dod.mew.service.DashboardService;
import mil.dod.mew.web.dto.UnitDashboardDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard/units")
public class MilitaryUnitDashboardController {

    private final DashboardService dashboardService;

    public MilitaryUnitDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping({"", "/"})
    public List<UnitDashboardDto> listUnitMetrics(
            @RequestParam(value = "asset_class", required = false) String assetClass) {
        return dashboardService.getUnitDashboardMetrics(assetClass);
    }

    @GetMapping({"/export_csv", "/export_csv/"})
    public ResponseEntity<String> exportCsv(
            @RequestParam(value = "asset_class", required = false) String assetClass) {
        String csv = dashboardService.exportUnitMetricsCsv(assetClass);
        String suffix = (assetClass == null || assetClass.isBlank())
                ? "All_Assets"
                : assetClass.trim().replace(" ", "_");
        String filename = "unit_reliability_" + suffix + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
