package mil.dod.mew.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import mil.dod.mew.service.ReadinessApiClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/** Scoped Django reference data replacing legacy Java/H2 dashboard feeds. */
@RestController
@RequestMapping("/api/v1/readiness/reference")
public class ReadinessReferenceController {
    private final ReadinessApiClient api;
    public ReadinessReferenceController(ReadinessApiClient api) { this.api = api; }
    @GetMapping({"/installations", "/installations/"}) public ResponseEntity<JsonNode> installations() { return api.get("/api/v1/installations/", Map.of("page_size", "2000")); }
    @GetMapping({"/catalog-items", "/catalog-items/"}) public ResponseEntity<JsonNode> catalogItems() { return api.get("/api/v1/catalog-items/", Map.of("page_size", "2000")); }
    @GetMapping({"/audit-logs", "/audit-logs/"}) public ResponseEntity<JsonNode> auditLogs() { return api.get("/api/v1/security-audit-logs/", Map.of("page_size", "2000")); }
}
