package mil.navy.nswc.mew.web.dto;

public class ReceiveShipmentResponse {
    private String message;
    private String documentNumber;
    private String materialReceived;
    private Integer quantityAdded;
    private String backorderNotice;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
    public String getMaterialReceived() { return materialReceived; }
    public void setMaterialReceived(String materialReceived) { this.materialReceived = materialReceived; }
    public Integer getQuantityAdded() { return quantityAdded; }
    public void setQuantityAdded(Integer quantityAdded) { this.quantityAdded = quantityAdded; }
    public String getBackorderNotice() { return backorderNotice; }
    public void setBackorderNotice(String backorderNotice) { this.backorderNotice = backorderNotice; }
}
