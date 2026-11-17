package mil.navy.nswc.mew;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs only through `mvn -Poracle-local-it verify` with MEW_ORACLE_LOCAL_PASSWORD set.
 * It validates the existing local Oracle schema; it never creates, updates, or drops it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
class OracleLocalSmokeIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void localOracleServesSeededReadinessData() throws Exception {
        mockMvc.perform(get("/api/v1/assets/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serial_number").exists());
        mockMvc.perform(get("/api/v1/inventory-balances/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].installation_code").exists());
    }
}
