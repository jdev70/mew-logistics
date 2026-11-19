package mil.dod.mew.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import mil.dod.mew.service.ReadinessApiClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Read-only gateway route for Django maintenance work orders during migration. */
@RestController
@RequestMapping("/api/v1/readiness/work-orders")
public class ReadinessWorkOrderController {
    private final ReadinessApiClient readinessApi;

    public ReadinessWorkOrderController(ReadinessApiClient readinessApi) {
        this.readinessApi = readinessApi;
    }

    @GetMapping({"", "/"})
    public ResponseEntity<JsonNode> list() {
        return readinessApi.get("/api/v1/maintenance-work-orders/", Map.of("page_size", "2000"));
    }
}
