package mil.dod.mew.web.controller;

import mil.dod.mew.domain.entity.WorkOrder;
import mil.dod.mew.domain.entity.Personnel;
import mil.dod.mew.domain.enums.FunctionalRole;
import mil.dod.mew.repository.WorkOrderRepository;
import mil.dod.mew.service.RbacPolicyService;
import mil.dod.mew.service.RbacPolicyService.Action;
import mil.dod.mew.service.RbacPolicyService.Resource;
import mil.dod.mew.security.MewResourceFactory;
import mil.dod.mew.security.AuthenticatedMewUserResolver;
import mil.dod.mew.security.model.Permission;
import mil.dod.mew.service.WorkOrderService;
import mil.dod.mew.service.exception.ResourceNotFoundException;
import mil.dod.mew.service.exception.AccessDeniedException;
import mil.dod.mew.web.dto.MaintenanceCompletionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/work-orders")
public class WorkOrderController {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderService workOrderService;
    private final RbacPolicyService rbacPolicyService;
    private final AuthenticatedMewUserResolver userResolver;

    public WorkOrderController(WorkOrderRepository workOrderRepository,
                               WorkOrderService workOrderService,
                               RbacPolicyService rbacPolicyService,
                               AuthenticatedMewUserResolver userResolver) {
        this.workOrderRepository = workOrderRepository;
        this.workOrderService = workOrderService;
        this.rbacPolicyService = rbacPolicyService;
        this.userResolver = userResolver;
    }

    @GetMapping({"", "/"})
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listWorkOrders() {
        return workOrderRepository.findAll().stream()
                .filter(workOrder -> rbacPolicyService.canAccess(Permission.LOG_MAINTENANCE_ACTION,
                        MewResourceFactory.forWorkOrder(workOrder)))
                .map(this::toDto).collect(Collectors.toList());
    }

    private Map<String, Object> toDto(WorkOrder w) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", w.getId());
        dto.put("jcn", w.getJcn());
        dto.put("status", w.getStatus().name());
        dto.put("status_display", w.getStatus().name());
        dto.put("priority_code", w.getPriorityCode());
        dto.put("priority_display", w.getPriorityCode() == 1 ? "1 - Mandatory / CASREP" : "2 - Essential");
        dto.put("casrep_number", w.getCasrepNumber());
        dto.put("is_grounding_fault", w.isGroundingFault());
        dto.put("asset_serial", w.getAsset() != null ? w.getAsset().getSerialNumber() : "");
        dto.put("system_nomenclature", w.getAsset() != null && w.getAsset().getAssetModel() != null
                ? w.getAsset().getAssetModel().getNomenclature() : "");
        dto.put("hull_number", w.getHull() != null ? w.getHull().getHullNumber() : null);
        dto.put("vessel_name", w.getHull() != null ? w.getHull().getVesselName() : null);
        dto.put("installation_code", w.getInstallation() != null ? w.getInstallation().getInstallationCode() : null);
        dto.put("eic", w.getEic());
        dto.put("apl_ael", w.getAplAel());
        dto.put("work_center", w.getWorkCenter());
        dto.put("description_of_fault", w.getDescriptionOfFault());
        dto.put("completed_work", w.getCompletedWork());
        dto.put("action_taken_code", w.getActionTakenCode());
        dto.put("initial_diagnostics", w.getInitialDiagnostics());
        dto.put("safety_tag_out", w.getSafetyTagOut());
        dto.put("isolation_inspection", w.getIsolationInspection());
        dto.put("fault_identification", w.getFaultIdentification());
        dto.put("repair_replacement", w.getRepairReplacement());
        dto.put("restoration_testing", w.getRestorationTesting());
        dto.put("final_equipment_status", w.getFinalEquipmentStatus());
        dto.put("maintenance_hours", w.getMaintenanceHours());
        dto.put("maintenance_submitted_at", w.getMaintenanceSubmittedAt());
        dto.put("technician_name", personnelName(w.getTechnician()));
        dto.put("qa_inspector_name", personnelName(w.getQaInspector()));
        dto.put("created_at", w.getCreatedAt());
        return dto;
    }

    @PostMapping({"/{id}/submit-maintenance", "/{id}/submit-maintenance/"})
    @Transactional
    public Map<String, Object> submitMaintenance(@PathVariable @NonNull UUID id,
                                                  @Valid @RequestBody MaintenanceCompletionRequest payload) {
        WorkOrder existing = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found: " + id));
        rbacPolicyService.requirePermission(Resource.WORK_ORDER, Action.UPDATE,
                "/api/v1/work-orders/" + id + "/submit-maintenance/");
        rbacPolicyService.requireAccess(Permission.LOG_MAINTENANCE_ACTION, MewResourceFactory.forWorkOrder(existing),
                "/api/v1/work-orders/" + id + "/submit-maintenance/");
        requireAssignedTechnician(existing);
        return toDto(workOrderService.submitMaintenanceCompletion(id, payload));
    }

    @GetMapping({"/{id}", "/{id}/"})
    @Transactional(readOnly = true)
    public WorkOrder getWorkOrder(@PathVariable @NonNull UUID id) {
        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found: " + id));
        rbacPolicyService.requireAccess(Permission.LOG_MAINTENANCE_ACTION, MewResourceFactory.forWorkOrder(workOrder),
                "/api/v1/work-orders/" + id + "/");
        return workOrder;
    }

    @PostMapping({"", "/"})
    public ResponseEntity<WorkOrder> createWorkOrder(
            @Valid @RequestBody @NonNull WorkOrder workOrder) {
        rbacPolicyService.requirePermission(Resource.WORK_ORDER, Action.CREATE, "/api/v1/work-orders/");
        rbacPolicyService.requireAccess(Permission.LOG_MAINTENANCE_ACTION, MewResourceFactory.forWorkOrder(workOrder),
                "/api/v1/work-orders/");
        workOrder.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(workOrderRepository.save(workOrder));
    }

    @PutMapping({"/{id}", "/{id}/"})
    public WorkOrder updateWorkOrder(
            @PathVariable @NonNull UUID id,
            @Valid @RequestBody @NonNull WorkOrder payload) {
        rbacPolicyService.requirePermission(Resource.WORK_ORDER, Action.UPDATE, "/api/v1/work-orders/" + id + "/");
        WorkOrder existing = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found: " + id));
        rbacPolicyService.requireAccess(Permission.LOG_MAINTENANCE_ACTION, MewResourceFactory.forWorkOrder(existing),
                "/api/v1/work-orders/" + id + "/");
        payload.setId(existing.getId());
        payload.setCreatedAt(existing.getCreatedAt());
        return workOrderRepository.save(payload);
    }

    @DeleteMapping({"/{id}", "/{id}/"})
    public ResponseEntity<Void> deleteWorkOrder(
            @PathVariable @NonNull UUID id) {
        rbacPolicyService.requirePermission(Resource.WORK_ORDER, Action.DELETE, "/api/v1/work-orders/" + id + "/");
        WorkOrder existing = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found: " + id));
        rbacPolicyService.requireAccess(Permission.LOG_MAINTENANCE_ACTION, MewResourceFactory.forWorkOrder(existing),
                "/api/v1/work-orders/" + id + "/");
        workOrderRepository.delete(existing);
        return ResponseEntity.noContent().build();
    }

    @PostMapping({"/{id}/close", "/{id}/close/"})
    @Transactional
    public ResponseEntity<Map<String, Object>> closeWorkOrder(
            @PathVariable @NonNull UUID id) {
        rbacPolicyService.requirePermission(Resource.WORK_ORDER, Action.UPDATE, "/api/v1/work-orders/" + id + "/close/");
        WorkOrder existing = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found: " + id));
        rbacPolicyService.requireAccess(Permission.LOG_MAINTENANCE_ACTION, MewResourceFactory.forWorkOrder(existing),
                "/api/v1/work-orders/" + id + "/close/");
        requireReviewer();
        return ResponseEntity.ok(toDto(workOrderService.closeWorkOrder(id)));
    }

    private void requireAssignedTechnician(WorkOrder workOrder) {
        Personnel caller = userResolver.currentPersonnel();
        if (caller.getFunctionalRole() == FunctionalRole.SHIPBOARD_TECHNICIAN
                && (workOrder.getTechnician() == null
                || !caller.getId().equals(workOrder.getTechnician().getId()))) {
            throw new AccessDeniedException("Only the assigned technician can submit this maintenance action.");
        }
    }

    private void requireReviewer() {
        if (userResolver.currentPersonnel().getFunctionalRole() != FunctionalRole.FLEET_READINESS_ANALYST) {
            throw new AccessDeniedException("Only a fleet readiness reviewer can accept QA and close this work order.");
        }
    }

    private String personnelName(Personnel personnel) {
        if (personnel == null) {
            return "Unassigned";
        }
        return (personnel.getRank() + " " + personnel.getFirstName() + " " + personnel.getLastName()).trim();
    }
}
