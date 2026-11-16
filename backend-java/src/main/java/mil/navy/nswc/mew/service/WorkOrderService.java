package mil.navy.nswc.mew.service;

import mil.navy.nswc.mew.domain.entity.Installation;
import mil.navy.nswc.mew.domain.entity.InventoryBalance;
import mil.navy.nswc.mew.domain.entity.WorkOrder;
import mil.navy.nswc.mew.domain.entity.WorkOrderPart;
import mil.navy.nswc.mew.domain.enums.MafStatus;
import mil.navy.nswc.mew.domain.enums.TransactionType;
import mil.navy.nswc.mew.repository.InventoryBalanceRepository;
import mil.navy.nswc.mew.repository.WorkOrderPartRepository;
import mil.navy.nswc.mew.repository.WorkOrderRepository;
import mil.navy.nswc.mew.service.exception.BusinessValidationException;
import mil.navy.nswc.mew.service.exception.ResourceNotFoundException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderPartRepository workOrderPartRepository;
    private final InventoryBalanceRepository balanceRepository;
    private final InventoryService inventoryService;

    public WorkOrderService(WorkOrderRepository workOrderRepository,
                            WorkOrderPartRepository workOrderPartRepository,
                            InventoryBalanceRepository balanceRepository,
                            InventoryService inventoryService) {
        this.workOrderRepository = workOrderRepository;
        this.workOrderPartRepository = workOrderPartRepository;
        this.balanceRepository = balanceRepository;
        this.inventoryService = inventoryService;
    }

    /**
     * Closes a CSMP/MAF work order and deducts all consumed parts from the installation's inventory.
     */
    @Transactional
    public WorkOrder closeWorkOrder(@NonNull UUID workOrderId) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found: " + workOrderId));

        if (workOrder.getStatus() == MafStatus.CLOSED) {
            throw new BusinessValidationException("WorkOrder is already closed.");
        }

        Installation installation = workOrder.getInstallation() != null
                ? workOrder.getInstallation()
                : workOrder.getAsset().getAssignedUnit().getInstallation();

        List<WorkOrderPart> parts = workOrderPartRepository.findByWorkOrderId(workOrderId);
        for (WorkOrderPart part : parts) {
            InventoryBalance balance = balanceRepository
                    .findByInstallationIdAndMaterialId(installation.getId(), part.getMaterial().getId())
                    .orElseThrow(() -> new BusinessValidationException(
                            "No inventory balance found for NSN " + part.getMaterial().getNsn()));

            inventoryService.executeTransaction(
                    balance.getId(),
                    TransactionType.DED,
                    part.getQuantity(),
                    workOrder.getTechnician(),
                    workOrder.getJcn(),
                    null,
                    workOrder
            );
        }

        workOrder.setStatus(MafStatus.CLOSED);
        return workOrderRepository.save(workOrder);
    }
}
