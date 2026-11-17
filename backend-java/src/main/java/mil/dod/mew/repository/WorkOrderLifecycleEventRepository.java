package mil.dod.mew.repository;

import mil.dod.mew.domain.entity.WorkOrderLifecycleEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkOrderLifecycleEventRepository extends JpaRepository<WorkOrderLifecycleEvent, UUID> {
    List<WorkOrderLifecycleEvent> findByWorkOrderIdOrderByOccurredAtAsc(UUID workOrderId);
}
