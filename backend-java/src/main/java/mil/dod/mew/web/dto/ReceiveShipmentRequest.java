package mil.dod.mew.web.dto;

import javax.validation.constraints.NotBlank;

public class ReceiveShipmentRequest {

    @NotBlank(message = "Please provide a document_number to scan.")
    private String documentNumber;

    private Integer receivedQuantity;

    public ReceiveShipmentRequest() {}

    public ReceiveShipmentRequest(String documentNumber, Integer receivedQuantity) {
        this.documentNumber = documentNumber;
        this.receivedQuantity = receivedQuantity;
    }

    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
    public Integer getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(Integer receivedQuantity) { this.receivedQuantity = receivedQuantity; }
}
