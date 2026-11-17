package mil.dod.mew.service;

import mil.dod.mew.domain.entity.Installation;
import mil.dod.mew.domain.entity.InventoryBalance;
import mil.dod.mew.domain.entity.WorkOrder;
import mil.dod.mew.domain.entity.WorkOrderPart;
import mil.dod.mew.domain.enums.MafStatus;
import mil.dod.mew.domain.enums.TransactionType;
import mil.dod.mew.repository.InventoryBalanceRepository;
import mil.dod.mew.repository.WorkOrderPartRepository;
import mil.dod.mew.repository.WorkOrderRepository;
import mil.dod.mew.service.exception.BusinessValidationException;
import mil.dod.mew.service.exception.ResourceNotFoundException;
import mil.dod.mew.web.dto.MaintenanceCompletionRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

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

    /** Records completed maintenance evidence and submits the MAF for independent QA review. */
    @Transactional
    public WorkOrder submitMaintenanceCompletion(@NonNull UUID workOrderId, MaintenanceCompletionRequest completion) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found: " + workOrderId));
        if (workOrder.getStatus() == MafStatus.CLOSED) {
            throw new BusinessValidationException("WorkOrder is already closed.");
        }
        if (workOrder.getStatus() == MafStatus.QA_PENDING) {
            throw new BusinessValidationException("WorkOrder is already awaiting QA review.");
        }
        workOrder.setActionTakenCode(completion.getActionTakenCode().trim());
        workOrder.setInitialDiagnostics(completion.getInitialDiagnostics().trim());
        workOrder.setSafetyTagOut(completion.getSafetyTagOut().trim());
        workOrder.setIsolationInspection(completion.getIsolationInspection().trim());
        workOrder.setFaultIdentification(completion.getFaultIdentification().trim());
        workOrder.setRepairReplacement(completion.getRepairReplacement().trim());
        workOrder.setRestorationTesting(completion.getRestorationTesting().trim());
        workOrder.setFinalEquipmentStatus(completion.getFinalEquipmentStatus().trim());
        workOrder.setCompletedWork(formatCompletionNarrative(completion));
        workOrder.setMaintenanceHours(completion.getMaintenanceHours());
        workOrder.setMaintenanceSubmittedAt(LocalDateTime.now());
        workOrder.setStatus(MafStatus.QA_PENDING);
        return workOrderRepository.save(workOrder);
    }

    private String formatCompletionNarrative(MaintenanceCompletionRequest completion) {
        return "Action Taken Code: " + completion.getActionTakenCode().trim() + "\n\n"
                + "Initial Diagnostics: " + completion.getInitialDiagnostics().trim() + "\n\n"
                + "Safety & Tag-Out: " + completion.getSafetyTagOut().trim() + "\n\n"
                + "Isolation & Inspection: " + completion.getIsolationInspection().trim() + "\n\n"
                + "Fault Identification: " + completion.getFaultIdentification().trim() + "\n\n"
                + "Repair / Replacement: " + completion.getRepairReplacement().trim() + "\n\n"
                + "Restoration & Testing: " + completion.getRestorationTesting().trim() + "\n\n"
                + "Final Equipment Status: " + completion.getFinalEquipmentStatus().trim();
    }

    /**
     * Closes a CSMP/MAF work order and deducts all consumed parts from the installation's inventory.
     */
    @Transactional
    public WorkOrder closeWorkOrder(@NonNull UUID workOrderId) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found: " + workOrderId));

        if (workOrder.getStatus() != MafStatus.QA_PENDING) {
            throw new BusinessValidationException("WorkOrder must be submitted to QA before it can be closed.");
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
