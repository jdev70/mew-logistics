package mil.dod.mew.repository;

import mil.dod.mew.domain.entity.HullSystemAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HullSystemAssignmentRepository extends JpaRepository<HullSystemAssignment, UUID> {
}
