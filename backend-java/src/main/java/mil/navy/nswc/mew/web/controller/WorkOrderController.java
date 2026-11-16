package mil.navy.nswc.mew.web.controller;

import mil.navy.nswc.mew.domain.entity.WorkOrder;
import mil.navy.nswc.mew.repository.WorkOrderRepository;
import mil.navy.nswc.mew.service.RbacPolicyService;
import mil.navy.nswc.mew.service.RbacPolicyService.Action;
import mil.navy.nswc.mew.service.RbacPolicyService.Resource;
import mil.navy.nswc.mew.service.WorkOrderService;
import mil.navy.nswc.mew.service.exception.ResourceNotFoundException;
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
import org.springframework.web.bind.annotation.RequestHeader;
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

    public WorkOrderController(WorkOrderRepository workOrderRepository,
                               WorkOrderService workOrderService,
                               RbacPolicyService rbacPolicyService) {
        this.workOrderRepository = workOrderRepository;
        this.workOrderService = workOrderService;
        this.rbacPolicyService = rbacPolicyService;
    }

    @GetMapping({"", "/"})
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listWorkOrders() {
        return workOrderRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
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
        dto.put("created_at", w.getCreatedAt());
        return dto;
    }

    @GetMapping({"/{id}", "/{id}/"})
    public WorkOrder getWorkOrder(@PathVariable @NonNull UUID id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found: " + id));
    }

    @PostMapping({"", "/"})
    public ResponseEntity<WorkOrder> createWorkOrder(
            @RequestHeader(value = "X-MEW-Persona-ID", required = false) String personaId,
            @Valid @RequestBody @NonNull WorkOrder workOrder) {
        rbacPolicyService.requirePermission(personaId, Resource.WORK_ORDER, Action.CREATE, "/api/v1/work-orders/");
        workOrder.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(workOrderRepository.save(workOrder));
    }

    @PutMapping({"/{id}", "/{id}/"})
    public WorkOrder updateWorkOrder(
            @RequestHeader(value = "X-MEW-Persona-ID", required = false) String personaId,
            @PathVariable @NonNull UUID id,
            @Valid @RequestBody @NonNull WorkOrder payload) {
        rbacPolicyService.requirePermission(personaId, Resource.WORK_ORDER, Action.UPDATE, "/api/v1/work-orders/" + id + "/");
        WorkOrder existing = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found: " + id));
        payload.setId(existing.getId());
        payload.setCreatedAt(existing.getCreatedAt());
        return workOrderRepository.save(payload);
    }

    @DeleteMapping({"/{id}", "/{id}/"})
    public ResponseEntity<Void> deleteWorkOrder(
            @RequestHeader(value = "X-MEW-Persona-ID", required = false) String personaId,
            @PathVariable @NonNull UUID id) {
        rbacPolicyService.requirePermission(personaId, Resource.WORK_ORDER, Action.DELETE, "/api/v1/work-orders/" + id + "/");
        if (!workOrderRepository.existsById(id)) {
            throw new ResourceNotFoundException("WorkOrder not found: " + id);
        }
        workOrderRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping({"/{id}/close", "/{id}/close/"})
    public ResponseEntity<WorkOrder> closeWorkOrder(
            @RequestHeader(value = "X-MEW-Persona-ID", required = false) String personaId,
            @PathVariable @NonNull UUID id) {
        rbacPolicyService.requirePermission(personaId, Resource.WORK_ORDER, Action.UPDATE, "/api/v1/work-orders/" + id + "/close/");
        return ResponseEntity.ok(workOrderService.closeWorkOrder(id));
    }
}
