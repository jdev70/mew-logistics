package mil.dod.mew.repository;

import mil.dod.mew.domain.entity.NavalHull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NavalHullRepository extends JpaRepository<NavalHull, UUID> {
    Optional<NavalHull> findByHullNumber(String hullNumber);
}
