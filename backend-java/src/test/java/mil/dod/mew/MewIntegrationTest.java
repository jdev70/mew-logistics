package mil.dod.mew;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
class MewIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allCoreGetEndpointsReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serial_number").exists());
        mockMvc.perform(get("/api/v1/dashboard/units/")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/installations/")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/work-orders/")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].asset_serial").exists());
        mockMvc.perform(get("/api/v1/requisitions/")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].material_name").exists());
        mockMvc.perform(get("/api/v1/inventory-balances/")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].installation_name").exists());
        mockMvc.perform(get("/api/v1/inventory-transactions/")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nsn").exists());
        mockMvc.perform(get("/api/v1/personnel/")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].personnel_id").exists());
        mockMvc.perform(get("/api/v1/components/")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nsn").exists());
        mockMvc.perform(get("/api/v1/security-audit-logs/")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].event_type").exists());
        mockMvc.perform(get("/api/v1/authorization/context/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active_context.persona_id").value("MEW-PER-0000"));
        mockMvc.perform(get("/api/v1/analytics/overview/")).andExpect(status().isOk());
    }
}
