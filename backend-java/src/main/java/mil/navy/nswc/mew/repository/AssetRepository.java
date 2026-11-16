package mil.navy.nswc.mew.repository;

import mil.navy.nswc.mew.domain.entity.Asset;
import mil.navy.nswc.mew.domain.enums.ReadinessLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<Asset, UUID> {
    Optional<Asset> findBySerialNumber(String serialNumber);
    List<Asset> findByAssignedUnitId(UUID unitId);
    List<Asset> findByReadinessStatus(ReadinessLevel readinessStatus);
    List<Asset> findByAssetModelAssetClassIgnoreCase(String assetClass);
}
