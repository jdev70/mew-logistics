package mil.dod.mew;

import com.jayway.jsonpath.JsonPath;
import mil.dod.mew.domain.enums.ReadinessLevel;
import mil.dod.mew.repository.AssetRepository;
import mil.dod.mew.repository.WorkOrderRepository;
import mil.dod.mew.repository.WorkOrderLifecycleEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "mew.security.mode=jwt")
@AutoConfigureMockMvc
@Import(AuthorizationIntegrationTest.JwtDecoderConfiguration.class)
@SuppressWarnings("null")
class AuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private WorkOrderRepository workOrderRepository;
    @Autowired
    private WorkOrderLifecycleEventRepository lifecycleEvents;
    @Autowired
    private AssetRepository assetRepository;

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/v1/assets/"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void permitsAngularCorsPreflightBeforeAuthentication() throws Exception {
        mockMvc.perform(options("/api/v1/assets/")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    @Test
    void strategicUserCanViewFleetAssets() throws Exception {
        mockMvc.perform(get("/api/v1/assets/").with(jwt().jwt(token -> token.subject("MEW-PER-0000"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serial_number").exists());
    }

    @Test
    void installationTechnicianSeesOnlyAssignedInventory() throws Exception {
        mockMvc.perform(get("/api/v1/inventory-balances/").with(jwt().jwt(token -> token.subject("MEW-PER-0002"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].installation_code").value("NS-NORFOLK"));
    }

    @Test
    void assignedTechnicianCanViewTheirScopedAssetWithoutReadRollback() throws Exception {
        mockMvc.perform(get("/api/v1/assets/").with(jwt().jwt(token -> token.subject("MEW-PER-0002"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].serial_number").value("SLQ32V6-DDG51-001"));
    }

    @Test
    void unknownJwtSubjectCannotMutateAssets() throws Exception {
        mockMvc.perform(post("/api/v1/assets/")
                        .with(jwt().jwt(token -> token.subject("unknown-subject")))
                        .contentType("application/json")
                        .content("{\"serial_number\":\"UNAUTHORIZED-TEST\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @Transactional
    void technicianSubmissionAndReviewerClosureFollowTheMafLifecycle() throws Exception {
        var workOrder = workOrderRepository.findByJcn("N21944-CE02-0002").orElseThrow();

        mockMvc.perform(post("/api/v1/work-orders/{id}/submit-maintenance/", workOrder.getId())
                        .with(jwt().jwt(token -> token.subject("MEW-PER-0002")))
                        .contentType("application/json")
                        .content("""
                                {
                                  "maintenance_hours": 2.5,
                                  "action_taken_code": "C - Repaired",
                                  "initial_diagnostics": "Verified BIT fault and isolated the signal path.",
                                  "safety_tag_out": "Applied electrical and RF tag-out.",
                                  "isolation_inspection": "Inspected receiver path and cabling.",
                                  "fault_identification": "Identified a failed low-noise amplifier.",
                                  "repair_replacement": "Replaced the receiver module and verified BIT pass.",
                                  "restoration_testing": "Restored power and completed operational testing.",
                                  "final_equipment_status": "1 - Fully Operational"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QA_PENDING"))
                .andExpect(jsonPath("$.maintenance_submitted_at").exists());

        mockMvc.perform(post("/api/v1/work-orders/{id}/close/", workOrder.getId())
                        .with(jwt().jwt(token -> token.subject("MEW-PER-0000"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(get("/api/v1/work-orders/{id}/lifecycle-history/", workOrder.getId())
                        .with(jwt().jwt(token -> token.subject("MEW-PER-0000"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].event_type").value("TECHNICIAN_SUBMITTED_FOR_QA"))
                .andExpect(jsonPath("$[0].previous_status").value("IN_WORK"))
                .andExpect(jsonPath("$[1].event_type").value("QA_APPROVED_AND_CLOSED"))
                .andExpect(jsonPath("$[1].new_status").value("CLOSED"));
        org.junit.jupiter.api.Assertions.assertEquals(2, lifecycleEvents.findByWorkOrderIdOrderByOccurredAtAsc(workOrder.getId()).size());
    }

    @Test
    void analyticsAssetTotalsReconcileWithThePersistedAssetSet() throws Exception {
        String response = mockMvc.perform(get("/api/v1/analytics/overview/")
                        .with(jwt().jwt(token -> token.subject("MEW-PER-0000"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Map<String, Object>> breakdown = JsonPath.read(response, "$.ao_system_breakdown");
        int expectedAssets = Math.toIntExact(assetRepository.count());
        long expectedFmc = assetRepository.findAll().stream()
                .filter(asset -> asset.getReadinessStatus() == ReadinessLevel.FMC).count();
        int reportedAssets = breakdown.stream()
                .mapToInt(item -> ((Number) item.get("total_count")).intValue()).sum();
        long reportedFmc = breakdown.stream()
                .mapToLong(item -> ((Number) item.get("fmc_count")).longValue()).sum();

        org.junit.jupiter.api.Assertions.assertEquals(expectedAssets, reportedAssets);
        org.junit.jupiter.api.Assertions.assertEquals(expectedFmc, reportedFmc);
    }

    @TestConfiguration
    static class JwtDecoderConfiguration {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject("decoder-not-used-by-mockmvc")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(300))
                    .build();
        }
    }
}
