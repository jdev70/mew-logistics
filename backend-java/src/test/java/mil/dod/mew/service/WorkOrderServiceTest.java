package mil.dod.mew.service;

import mil.dod.mew.domain.entity.WorkOrder;
import mil.dod.mew.domain.enums.MafStatus;
import mil.dod.mew.repository.InventoryBalanceRepository;
import mil.dod.mew.repository.WorkOrderPartRepository;
import mil.dod.mew.repository.WorkOrderRepository;
import mil.dod.mew.service.exception.BusinessValidationException;
import mil.dod.mew.web.dto.MaintenanceCompletionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class WorkOrderServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private WorkOrderPartRepository workOrderPartRepository;
    @Mock
    private InventoryBalanceRepository balanceRepository;
    @Mock
    private InventoryService inventoryService;
    @InjectMocks
    private WorkOrderService workOrderService;

    @Test
    void completedMaintenanceMovesWorkOrderToQaPendingAndRetainsEvidence() {
        UUID workOrderId = UUID.randomUUID();
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(workOrderId);
        workOrder.setStatus(MafStatus.IN_WORK);
        when(workOrderRepository.findById(workOrderId)).thenReturn(Optional.of(workOrder));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkOrder updated = workOrderService.submitMaintenanceCompletion(workOrderId, completionRequest());

        assertEquals(MafStatus.QA_PENDING, updated.getStatus());
        assertEquals("C - Repaired", updated.getActionTakenCode());
        assertEquals("Verified BIT fault 160 and isolated the signal path.", updated.getInitialDiagnostics());
        assertEquals("1 - Fully Operational", updated.getFinalEquipmentStatus());
        assertEquals("Replaced the receiver module and verified BIT pass.", updated.getRepairReplacement());
        assertEquals(new BigDecimal("2.50"), updated.getMaintenanceHours());
        assertNotNull(updated.getMaintenanceSubmittedAt());
        verify(workOrderRepository).save(workOrder);
    }

    @Test
    void closeRequiresQaSubmissionFirst() {
        UUID workOrderId = UUID.randomUUID();
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(workOrderId);
        workOrder.setStatus(MafStatus.IN_WORK);
        when(workOrderRepository.findById(workOrderId)).thenReturn(Optional.of(workOrder));

        assertThrows(BusinessValidationException.class, () -> workOrderService.closeWorkOrder(workOrderId));
    }

    private MaintenanceCompletionRequest completionRequest() {
        MaintenanceCompletionRequest completion = new MaintenanceCompletionRequest();
        completion.setActionTakenCode("C - Repaired");
        completion.setInitialDiagnostics("Verified BIT fault 160 and isolated the signal path.");
        completion.setSafetyTagOut("Applied electrical and RF tag-out.");
        completion.setIsolationInspection("Inspected the receiver path and cabling.");
        completion.setFaultIdentification("Identified a failed low-noise amplifier.");
        completion.setRepairReplacement("Replaced the receiver module and verified BIT pass.");
        completion.setRestorationTesting("Restored power and completed operational testing.");
        completion.setFinalEquipmentStatus("1 - Fully Operational");
        completion.setMaintenanceHours(new BigDecimal("2.50"));
        return completion;
    }
}
