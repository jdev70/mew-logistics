package mil.navy.nswc.mew.web.controller;

import mil.navy.nswc.mew.repository.RequisitionRepository;
import mil.navy.nswc.mew.service.RequisitionService;
import mil.navy.nswc.mew.web.dto.ReceiveShipmentRequest;
import mil.navy.nswc.mew.web.dto.ReceiveShipmentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RequisitionController.class)
@SuppressWarnings("null")
class RequisitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RequisitionRepository requisitionRepository;

    @MockBean
    private RequisitionService requisitionService;

    @Test
    void receiveShipmentReturnsOkPayload() throws Exception {
        ReceiveShipmentResponse response = new ReceiveShipmentResponse();
        response.setMessage("Shipment received and added to inventory.");
        response.setDocumentNumber("N2000161000001");
        response.setMaterialReceived("GaN T/R Module");
        response.setQuantityAdded(4);

        when(requisitionService.receiveShipment(any(ReceiveShipmentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/requisitions/receive-shipment/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"document_number\":\"N2000161000001\",\"received_quantity\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document_number").value("N2000161000001"))
                .andExpect(jsonPath("$.quantity_added").value(4));
    }

    @Test
    void receiveShipmentRejectsMissingDocumentNumber() throws Exception {
        mockMvc.perform(post("/api/v1/requisitions/receive-shipment/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
