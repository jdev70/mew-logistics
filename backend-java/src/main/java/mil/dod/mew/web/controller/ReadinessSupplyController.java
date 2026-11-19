package mil.dod.mew.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import mil.dod.mew.service.ReadinessApiClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Read-only Django supply routes for incremental dashboard migration. */
@RestController
@RequestMapping("/api/v1/readiness/supply")
public class ReadinessSupplyController {
    private final ReadinessApiClient readinessApi;
    public ReadinessSupplyController(ReadinessApiClient readinessApi) { this.readinessApi = readinessApi; }

    @GetMapping({"/inventory", "/inventory/"})
    public ResponseEntity<JsonNode> inventory() {
        return readinessApi.get("/api/v1/parts-inventory/", Map.of("page_size", "2000"));
    }

    @GetMapping({"/requisitions", "/requisitions/"})
    public ResponseEntity<JsonNode> requisitions() {
        return readinessApi.get("/api/v1/supply-requisitions/", Map.of("page_size", "2000"));
    }
}
