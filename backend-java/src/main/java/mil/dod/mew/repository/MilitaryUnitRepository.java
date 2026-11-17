package mil.dod.mew.repository;

import mil.dod.mew.domain.entity.MilitaryUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MilitaryUnitRepository extends JpaRepository<MilitaryUnit, UUID> {
    Optional<MilitaryUnit> findByUic(String uic);
    List<MilitaryUnit> findByParentUnitIdIn(List<UUID> parentIds);
}
