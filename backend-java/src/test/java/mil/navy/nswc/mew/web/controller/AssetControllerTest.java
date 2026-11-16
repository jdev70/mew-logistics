package mil.navy.nswc.mew.web.controller;

import mil.navy.nswc.mew.domain.entity.Asset;
import mil.navy.nswc.mew.domain.enums.ReadinessLevel;
import mil.navy.nswc.mew.repository.AssetModelRepository;
import mil.navy.nswc.mew.repository.AssetRepository;
import mil.navy.nswc.mew.repository.MilitaryUnitRepository;
import mil.navy.nswc.mew.service.RbacPolicyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssetController.class)
@SuppressWarnings("null")
class AssetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AssetRepository assetRepository;

    @MockBean
    private AssetModelRepository assetModelRepository;

    @MockBean
    private MilitaryUnitRepository unitRepository;

    @MockBean
    private RbacPolicyService rbacPolicyService;

    @Test
    void createAssetReturnsCreated() throws Exception {
        Asset saved = new Asset();
        saved.setId(UUID.randomUUID());
        saved.setSerialNumber("SLQ32V6-TEST-01");
        saved.setReadinessStatus(ReadinessLevel.FMC);

        when(assetRepository.save(any(Asset.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/assets/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serial_number\":\"SLQ32V6-TEST-01\",\"readiness_status\":\"FMC\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serial_number").value("SLQ32V6-TEST-01"));
    }

    @Test
    void updateAssetReturnsUpdatedRecord() throws Exception {
        UUID id = UUID.randomUUID();
        Asset existing = new Asset();
        existing.setId(id);
        existing.setSerialNumber("SLQ32V6-TEST-01");

        Asset updated = new Asset();
        updated.setId(id);
        updated.setSerialNumber("SLQ32V6-TEST-01");
        updated.setReadinessStatus(ReadinessLevel.PMC);

        when(assetRepository.findById(id)).thenReturn(Optional.of(existing));
        when(assetRepository.save(any(Asset.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/assets/" + id + "/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serial_number\":\"SLQ32V6-TEST-01\",\"readiness_status\":\"PMC\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readiness_status").value("PMC"));
    }

    @Test
    void deleteAssetReturnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        when(assetRepository.existsById(id)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/assets/" + id + "/"))
                .andExpect(status().isNoContent());

        verify(assetRepository).deleteById(id);
    }
}
