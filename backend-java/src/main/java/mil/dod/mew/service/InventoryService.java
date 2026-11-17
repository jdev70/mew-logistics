package mil.dod.mew.service;

import mil.dod.mew.domain.entity.InventoryBalance;
import mil.dod.mew.domain.entity.InventoryTransaction;
import mil.dod.mew.domain.entity.Personnel;
import mil.dod.mew.domain.entity.Requisition;
import mil.dod.mew.domain.entity.WorkOrder;
import mil.dod.mew.domain.enums.TransactionType;
import mil.dod.mew.repository.InventoryBalanceRepository;
import mil.dod.mew.repository.InventoryTransactionRepository;
import mil.dod.mew.service.exception.BusinessValidationException;
import mil.dod.mew.service.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InventoryService {

    private final InventoryBalanceRepository balanceRepository;
    private final InventoryTransactionRepository transactionRepository;

    public InventoryService(InventoryBalanceRepository balanceRepository,
                            InventoryTransactionRepository transactionRepository) {
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Executes an atomic inventory movement with pessimistic locking and appends an immutable ledger entry.
     */
    @Transactional
    public InventoryTransaction executeTransaction(
            UUID balanceId,
            TransactionType transactionType,
            int quantity,
            Personnel performedBy,
            String referenceDocument,
            Requisition relatedRequisition,
            WorkOrder relatedWorkOrder) {

        if (quantity < 0) {
            throw new BusinessValidationException("Transaction quantity cannot be negative.");
        }

        InventoryBalance balance = balanceRepository.findByIdForUpdate(balanceId)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryBalance not found: " + balanceId));

        int prevQty = balance.getQuantityOnHand();
        int newQty;
        int recordedQty = quantity;

        if (transactionType == TransactionType.RCT) {
            newQty = prevQty + quantity;
        } else if (transactionType == TransactionType.DED) {
            if (prevQty < quantity) {
                throw new BusinessValidationException(
                        "Insufficient stock. Cannot deduct " + quantity + " from " + prevQty + ".");
            }
            newQty = prevQty - quantity;
        } else {
            newQty = quantity;
            recordedQty = Math.abs(newQty - prevQty);
        }

        balance.setQuantityOnHand(newQty);
        balanceRepository.save(balance);

        InventoryTransaction tx = new InventoryTransaction();
        tx.setBalance(balance);
        tx.setTransactionType(transactionType);
        tx.setQuantity(recordedQty);
        tx.setPreviousBalance(prevQty);
        tx.setNewBalance(newQty);
        tx.setReferenceDocument(referenceDocument);
        tx.setRelatedRequisition(relatedRequisition);
        tx.setRelatedWorkOrder(relatedWorkOrder);
        tx.setPerformedBy(performedBy);

        return transactionRepository.save(tx);
    }
}
