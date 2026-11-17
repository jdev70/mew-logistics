package mil.dod.mew.repository;

import mil.dod.mew.domain.entity.Installation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InstallationRepository extends JpaRepository<Installation, UUID> {
    Optional<Installation> findByDodaac(String dodaac);
    Optional<Installation> findByInstallationCode(String installationCode);
}
