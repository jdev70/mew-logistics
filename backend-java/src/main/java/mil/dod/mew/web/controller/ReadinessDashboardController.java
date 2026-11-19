package mil.dod.mew.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import mil.dod.mew.service.ReadinessApiClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Stable Java gateway routes backed by the Django readiness data service. */
@RestController
@RequestMapping("/api/v1/readiness/dashboard")
public class ReadinessDashboardController {
    private final ReadinessApiClient readinessApi;

    public ReadinessDashboardController(ReadinessApiClient readinessApi) {
        this.readinessApi = readinessApi;
    }

    @GetMapping({"/summary", "/summary/"})
    public ResponseEntity<JsonNode> summary(
            @RequestParam(value = "branch", required = false) String branch,
            @RequestParam(value = "installation_code", required = false) String installationCode) {
        return readinessApi.get("/api/v1/dashboard/summary/", Map.of(
                "branch", branch == null ? "" : branch,
                "installation_code", installationCode == null ? "" : installationCode));
    }

    @GetMapping({"/exceptions", "/exceptions/"})
    public ResponseEntity<JsonNode> exceptions() {
        return readinessApi.get("/api/v1/dashboard/exceptions/", Map.of());
    }

    @GetMapping({"/shortages", "/shortages/"})
    public ResponseEntity<JsonNode> shortages() {
        return readinessApi.get("/api/v1/dashboard/shortages/", Map.of());
    }
}
