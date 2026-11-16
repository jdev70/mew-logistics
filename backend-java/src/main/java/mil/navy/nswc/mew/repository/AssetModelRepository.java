package mil.navy.nswc.mew.repository;

import mil.navy.nswc.mew.domain.entity.AssetModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AssetModelRepository extends JpaRepository<AssetModel, UUID> {
    Optional<AssetModel> findBySystemId(String systemId);
}
