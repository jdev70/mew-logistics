package mil.dod.mew.repository;

import mil.dod.mew.domain.entity.Personnel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PersonnelRepository extends JpaRepository<Personnel, UUID> {
    Optional<Personnel> findByPersonnelId(String personnelId);
    Optional<Personnel> findByIdentitySubject(String identitySubject);
    Optional<Personnel> findByDodId(String dodId);
}
