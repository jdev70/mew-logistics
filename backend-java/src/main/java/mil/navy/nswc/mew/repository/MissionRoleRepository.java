package mil.navy.nswc.mew.repository;

import mil.navy.nswc.mew.domain.entity.MissionRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MissionRoleRepository extends JpaRepository<MissionRole, UUID> {
    Optional<MissionRole> findByRoleCode(String roleCode);
}
