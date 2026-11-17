package mil.dod.mew.repository;

import mil.dod.mew.domain.entity.UnitReadinessSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UnitReadinessSnapshotRepository extends JpaRepository<UnitReadinessSnapshot, Long> {
    Optional<UnitReadinessSnapshot> findByUnitIdAndSnapshotDate(UUID unitId, LocalDate snapshotDate);
    List<UnitReadinessSnapshot> findByUnitIdOrderBySnapshotDateDesc(UUID unitId);
}
