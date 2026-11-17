package mil.dod.mew.service;

import mil.dod.mew.domain.entity.InventoryBalance;
import mil.dod.mew.domain.entity.InventoryTransaction;
import mil.dod.mew.domain.enums.TransactionType;
import mil.dod.mew.repository.InventoryBalanceRepository;
import mil.dod.mew.repository.InventoryTransactionRepository;
import mil.dod.mew.service.exception.BusinessValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class InventoryServiceTest {

    @Mock
    private InventoryBalanceRepository balanceRepository;

    @Mock
    private InventoryTransactionRepository transactionRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private UUID balanceId;
    private InventoryBalance balance;

    @BeforeEach
    void setUp() {
        balanceId = UUID.randomUUID();
        balance = new InventoryBalance();
        balance.setId(balanceId);
        balance.setQuantityOnHand(10);
    }

    @Test
    void executeReceiptIncreasesBalanceAndRecordsImmutableLedger() {
        when(balanceRepository.findByIdForUpdate(balanceId)).thenReturn(Optional.of(balance));
        when(transactionRepository.save(any(InventoryTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InventoryTransaction tx = inventoryService.executeTransaction(
                balanceId, TransactionType.RCT, 5, null, "REQ-1001", null, null);

        assertEquals(15, balance.getQuantityOnHand());
        assertEquals(10, tx.getPreviousBalance());
        assertEquals(15, tx.getNewBalance());
        assertEquals(5, tx.getQuantity());
        assertEquals(TransactionType.RCT, tx.getTransactionType());
        verify(balanceRepository).save(balance);
        verify(transactionRepository).save(any(InventoryTransaction.class));
    }

    @Test
    void executeDeductionThrowsWhenStockInsufficient() {
        when(balanceRepository.findByIdForUpdate(balanceId)).thenReturn(Optional.of(balance));

        assertThrows(BusinessValidationException.class, () ->
                inventoryService.executeTransaction(
                        balanceId, TransactionType.DED, 25, null, "WO-001", null, null));

        assertEquals(10, balance.getQuantityOnHand());
        verify(transactionRepository, never()).save(any());
    }
}
