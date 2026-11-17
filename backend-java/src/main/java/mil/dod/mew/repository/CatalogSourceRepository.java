package mil.dod.mew.repository;

import mil.dod.mew.domain.entity.CatalogSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CatalogSourceRepository extends JpaRepository<CatalogSource, UUID> {
    Optional<CatalogSource> findBySourceId(String sourceId);
}
