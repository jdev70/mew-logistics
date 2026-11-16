package mil.navy.nswc.mew.repository;

import mil.navy.nswc.mew.domain.entity.Requisition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RequisitionRepository extends JpaRepository<Requisition, UUID> {
    Optional<Requisition> findByDocumentNumber(String documentNumber);
}
