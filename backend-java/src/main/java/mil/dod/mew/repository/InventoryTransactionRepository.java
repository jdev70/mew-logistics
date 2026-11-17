package mil.dod.mew.repository;

import mil.dod.mew.domain.entity.InventoryTransaction;
import mil.dod.mew.domain.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {
    List<InventoryTransaction> findAllByOrderByCreatedAtDesc();
    List<InventoryTransaction> findByTransactionTypeOrderByCreatedAtDesc(TransactionType type);
    List<InventoryTransaction> findByBalanceInstallationIdOrderByCreatedAtDesc(UUID installationId);
    List<InventoryTransaction> findByBalanceMaterialNsnOrderByCreatedAtDesc(String nsn);
}
