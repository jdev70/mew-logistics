package mil.dod.mew.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import mil.dod.mew.service.ReadinessApiClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

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

    @PostMapping({"/receive-shipment", "/receive-shipment/"})
    public Map<String, Object> receiveShipment(@RequestBody Map<String, Object> request) {
        String document = String.valueOf(request.getOrDefault("document_number", "")).trim();
        int quantity = ((Number) request.getOrDefault("received_quantity", 0)).intValue();
        if (document.isEmpty() || quantity <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "document_number and positive received_quantity are required.");
        JsonNode requisitions = readinessApi.get("/api/v1/supply-requisitions/", Map.of("page_size", "2000")).getBody();
        JsonNode req = find(requisitions, "requisition_doc_number", document);
        if (req == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Requisition not found.");
        JsonNode inventory = readinessApi.get("/api/v1/parts-inventory/", Map.of("page_size", "2000")).getBody();
        JsonNode line = findInventory(inventory, req.path("unit").asText(), req.path("part_nsn").asText());
        if (line == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "No matching inventory line.");
        readinessApi.post("/api/v1/supply-requisitions/" + document + "/receive/", Map.of("inventory_id", line.path("inventory_id").asText(), "quantity_received", quantity));
        return Map.of("message", "Shipment received and added to inventory.", "document_number", document, "material_received", req.path("part_nsn").asText(), "quantity_added", quantity);
    }

    private JsonNode find(JsonNode payload, String field, String value) { for (JsonNode row : payload.path("results")) if (value.equals(row.path(field).asText())) return row; return null; }
    private JsonNode findInventory(JsonNode payload, String unit, String nsn) { for (JsonNode row : payload.path("results")) if (unit.equals(row.path("unit").asText()) && nsn.equals(row.path("part_nsn").asText())) return row; return null; }
}
