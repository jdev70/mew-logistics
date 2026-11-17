package mil.dod.mew.repository;

import mil.dod.mew.domain.entity.InventoryBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM InventoryBalance b WHERE b.id = :id")
    Optional<InventoryBalance> findByIdForUpdate(@Param("id") UUID id);

    Optional<InventoryBalance> findByInstallationIdAndMaterialId(UUID installationId, UUID materialId);
}
