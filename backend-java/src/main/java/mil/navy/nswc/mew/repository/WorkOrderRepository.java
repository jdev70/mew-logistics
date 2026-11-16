package mil.navy.nswc.mew.repository;

import mil.navy.nswc.mew.domain.entity.WorkOrder;
import mil.navy.nswc.mew.domain.enums.MafStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {
    Optional<WorkOrder> findByJcn(String jcn);
    List<WorkOrder> findByAssetIdAndStatusNot(UUID assetId, MafStatus status);
}
