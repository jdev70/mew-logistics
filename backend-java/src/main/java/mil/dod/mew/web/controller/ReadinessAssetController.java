package mil.dod.mew.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import mil.dod.mew.service.ReadinessApiClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Read-only Django asset inventory route used during incremental dashboard migration. */
@RestController
@RequestMapping("/api/v1/readiness/assets")
public class ReadinessAssetController {
    private final ReadinessApiClient readinessApi;

    public ReadinessAssetController(ReadinessApiClient readinessApi) {
        this.readinessApi = readinessApi;
    }

    @GetMapping({"", "/"})
    public ResponseEntity<JsonNode> list(
            @RequestParam(value = "branch", required = false) String branch,
            @RequestParam(value = "installation_code", required = false) String installationCode,
            @RequestParam(value = "readiness_status", required = false) String readinessStatus) {
        return readinessApi.get("/api/v1/assets/", Map.of(
                "branch", branch == null ? "" : branch,
                "installation_code", installationCode == null ? "" : installationCode,
                "readiness_status", readinessStatus == null ? "" : readinessStatus,
                "page_size", "2000"));
    }
}
