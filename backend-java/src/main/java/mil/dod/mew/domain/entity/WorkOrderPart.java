package mil.dod.mew.domain.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "mew_work_order_part")
public class WorkOrderPart extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private MaterialCatalog material;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "defective_serial_number", length = 60)
    private String defectiveSerialNumber = "";

    @Column(name = "replacement_serial_number", length = 60)
    private String replacementSerialNumber = "";

    @Column(name = "requires_dlr_turn_in", nullable = false)
    private boolean requiresDlrTurnIn = true;

    public WorkOrder getWorkOrder() { return workOrder; }
    public void setWorkOrder(WorkOrder workOrder) { this.workOrder = workOrder; }
    public MaterialCatalog getMaterial() { return material; }
    public void setMaterial(MaterialCatalog material) { this.material = material; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getDefectiveSerialNumber() { return defectiveSerialNumber; }
    public void setDefectiveSerialNumber(String defectiveSerialNumber) { this.defectiveSerialNumber = defectiveSerialNumber; }
    public String getReplacementSerialNumber() { return replacementSerialNumber; }
    public void setReplacementSerialNumber(String replacementSerialNumber) { this.replacementSerialNumber = replacementSerialNumber; }
    public boolean isRequiresDlrTurnIn() { return requiresDlrTurnIn; }
    public void setRequiresDlrTurnIn(boolean requiresDlrTurnIn) { this.requiresDlrTurnIn = requiresDlrTurnIn; }
}
