package mil.navy.nswc.mew.repository;

import mil.navy.nswc.mew.domain.entity.MaterialCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MaterialCatalogRepository extends JpaRepository<MaterialCatalog, UUID> {
    Optional<MaterialCatalog> findByNsn(String nsn);
}
