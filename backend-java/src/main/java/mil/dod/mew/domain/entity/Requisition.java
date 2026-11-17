package mil.dod.mew.domain.entity;

import mil.dod.mew.domain.enums.SupplyStatusCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "mew_requisition")
public class Requisition extends BaseEntity {

    @Column(name = "document_number", length = 25, unique = true, nullable = false)
    private String documentNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requesting_unit_id", nullable = false)
    private MilitaryUnit requestingUnit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private MaterialCatalog material;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "priority", nullable = false)
    private Integer priority = 3;

    @Column(name = "ipd_code", length = 2, nullable = false)
    private String ipdCode = "13";

    @Enumerated(EnumType.STRING)
    @Column(name = "supply_status_code", length = 4, nullable = false)
    private SupplyStatusCode supplyStatusCode = SupplyStatusCode.BA;

    @Column(name = "requires_dlr_turn_in", nullable = false)
    private boolean requiresDlrTurnIn = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tied_to_asset_id")
    private Asset tiedToAsset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id")
    private WorkOrder workOrder;

    @Column(name = "status", length = 50, nullable = false)
    private String status = "Submitted";

    @Column(name = "estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;

    @PrePersist
    @PreUpdate
    protected void syncMilstripCodes() {
        if (priority != null && priority == 1) {
            this.ipdCode = "01";
        } else if (priority != null && priority == 2 && "13".equals(this.ipdCode)) {
            this.ipdCode = "03";
        }
        if ("Received".equalsIgnoreCase(this.status)) {
            this.supplyStatusCode = SupplyStatusCode.D7;
        } else if ("In Transit".equalsIgnoreCase(this.status) && this.supplyStatusCode == SupplyStatusCode.BA) {
            this.supplyStatusCode = SupplyStatusCode.BV;
        }
        if (this.material != null) {
            this.requiresDlrTurnIn = "Depot_Repairable_LRU".equals(this.material.getRepairabilityCode());
        }
    }

    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
    public MilitaryUnit getRequestingUnit() { return requestingUnit; }
    public void setRequestingUnit(MilitaryUnit requestingUnit) { this.requestingUnit = requestingUnit; }
    public MaterialCatalog getMaterial() { return material; }
    public void setMaterial(MaterialCatalog material) { this.material = material; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public String getIpdCode() { return ipdCode; }
    public void setIpdCode(String ipdCode) { this.ipdCode = ipdCode; }
    public SupplyStatusCode getSupplyStatusCode() { return supplyStatusCode; }
    public void setSupplyStatusCode(SupplyStatusCode supplyStatusCode) { this.supplyStatusCode = supplyStatusCode; }
    public boolean isRequiresDlrTurnIn() { return requiresDlrTurnIn; }
    public void setRequiresDlrTurnIn(boolean requiresDlrTurnIn) { this.requiresDlrTurnIn = requiresDlrTurnIn; }
    public Asset getTiedToAsset() { return tiedToAsset; }
    public void setTiedToAsset(Asset tiedToAsset) { this.tiedToAsset = tiedToAsset; }
    public WorkOrder getWorkOrder() { return workOrder; }
    public void setWorkOrder(WorkOrder workOrder) { this.workOrder = workOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getEstimatedDeliveryDate() { return estimatedDeliveryDate; }
    public void setEstimatedDeliveryDate(LocalDate estimatedDeliveryDate) { this.estimatedDeliveryDate = estimatedDeliveryDate; }
}
