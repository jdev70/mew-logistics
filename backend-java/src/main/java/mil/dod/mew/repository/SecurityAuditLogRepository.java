package mil.dod.mew.repository;

import mil.dod.mew.domain.entity.SecurityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SecurityAuditLogRepository extends JpaRepository<SecurityAuditLog, UUID> {
    List<SecurityAuditLog> findAllByOrderByCreatedAtDesc();
}
