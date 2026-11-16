package mil.navy.nswc.mew;

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

import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "mew.security.mode=jwt")
@AutoConfigureMockMvc
@Import(AuthorizationIntegrationTest.JwtDecoderConfiguration.class)
@SuppressWarnings("null")
class AuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/v1/assets/"))
                .andExpect(status().isUnauthorized());
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
    void unknownJwtSubjectCannotMutateAssets() throws Exception {
        mockMvc.perform(post("/api/v1/assets/")
                        .with(jwt().jwt(token -> token.subject("unknown-subject")))
                        .contentType("application/json")
                        .content("{\"serial_number\":\"UNAUTHORIZED-TEST\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
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
