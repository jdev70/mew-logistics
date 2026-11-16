package mil.navy.nswc.mew.repository;

import mil.navy.nswc.mew.domain.entity.InventoryTransaction;
import mil.navy.nswc.mew.domain.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {
    List<InventoryTransaction> findAllByOrderByCreatedAtDesc();
    List<InventoryTransaction> findByTransactionTypeOrderByCreatedAtDesc(TransactionType type);
    List<InventoryTransaction> findByBalanceInstallationIdOrderByCreatedAtDesc(UUID installationId);
    List<InventoryTransaction> findByBalanceMaterialNsnOrderByCreatedAtDesc(String nsn);
}
