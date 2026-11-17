package mil.dod.mew.domain.entity;

import mil.dod.mew.domain.enums.TransactionType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

/**
 * Immutable inventory audit ledger entry. Updates to existing rows are rejected.
 */
@Entity
@Table(name = "mew_inventory_tx")
public class InventoryTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "balance_id", nullable = false)
    private InventoryBalance balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 3, nullable = false)
    private TransactionType transactionType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "previous_balance", nullable = false, updatable = false)
    private Integer previousBalance;

    @Column(name = "new_balance", nullable = false, updatable = false)
    private Integer newBalance;

    @Column(name = "reference_document", length = 100)
    private String referenceDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_requisition_id")
    private Requisition relatedRequisition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_work_order_id")
    private WorkOrder relatedWorkOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_id")
    private Personnel performedBy;

    @PreUpdate
    protected void preventMutation() {
        throw new IllegalStateException("Inventory transactions are immutable ledgers and cannot be modified.");
    }

    public InventoryBalance getBalance() { return balance; }
    public void setBalance(InventoryBalance balance) { this.balance = balance; }
    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getPreviousBalance() { return previousBalance; }
    public void setPreviousBalance(Integer previousBalance) { this.previousBalance = previousBalance; }
    public Integer getNewBalance() { return newBalance; }
    public void setNewBalance(Integer newBalance) { this.newBalance = newBalance; }
    public String getReferenceDocument() { return referenceDocument; }
    public void setReferenceDocument(String referenceDocument) { this.referenceDocument = referenceDocument; }
    public Requisition getRelatedRequisition() { return relatedRequisition; }
    public void setRelatedRequisition(Requisition relatedRequisition) { this.relatedRequisition = relatedRequisition; }
    public WorkOrder getRelatedWorkOrder() { return relatedWorkOrder; }
    public void setRelatedWorkOrder(WorkOrder relatedWorkOrder) { this.relatedWorkOrder = relatedWorkOrder; }
    public Personnel getPerformedBy() { return performedBy; }
    public void setPerformedBy(Personnel performedBy) { this.performedBy = performedBy; }
}
