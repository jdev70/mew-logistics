package mil.navy.nswc.mew.web.controller;

import mil.navy.nswc.mew.domain.entity.Requisition;
import mil.navy.nswc.mew.repository.RequisitionRepository;
import mil.navy.nswc.mew.service.RbacPolicyService;
import mil.navy.nswc.mew.service.RbacPolicyService.Action;
import mil.navy.nswc.mew.service.RbacPolicyService.Resource;
import mil.navy.nswc.mew.service.RequisitionService;
import mil.navy.nswc.mew.service.exception.ResourceNotFoundException;
import mil.navy.nswc.mew.web.dto.ReceiveShipmentRequest;
import mil.navy.nswc.mew.web.dto.ReceiveShipmentResponse;
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
@RequestMapping("/api/v1/requisitions")
public class RequisitionController {

    private final RequisitionRepository requisitionRepository;
    private final RequisitionService requisitionService;
    private final RbacPolicyService rbacPolicyService;

    public RequisitionController(RequisitionRepository requisitionRepository,
            RequisitionService requisitionService,
            RbacPolicyService rbacPolicyService) {
        this.requisitionRepository = requisitionRepository;
        this.requisitionService = requisitionService;
        this.rbacPolicyService = rbacPolicyService;
    }

    @GetMapping({ "", "/" })
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRequisitions() {
        return requisitionRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    private Map<String, Object> toDto(Requisition r) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", r.getId());
        dto.put("document_number", r.getDocumentNumber());
        dto.put("material_name", r.getMaterial() != null ? r.getMaterial().getNomenclature() : "");
        dto.put("nsn", r.getMaterial() != null ? r.getMaterial().getNsn() : "");
        dto.put("cage_code", r.getMaterial() != null ? r.getMaterial().getCageCode() : "");
        dto.put("unit_price", r.getMaterial() != null ? r.getMaterial().getUnitPrice() : 0);
        dto.put("quantity", r.getQuantity());
        dto.put("priority_display", r.getPriority() == 1 ? "Urgent / CASREP" : (r.getPriority() == 2 ? "Priority" : "Routine"));
        dto.put("ipd_code", r.getIpdCode());
        dto.put("supply_status_code", r.getSupplyStatusCode() != null ? r.getSupplyStatusCode().name() : "BA");
        dto.put("requires_dlr_turn_in", r.isRequiresDlrTurnIn());
        dto.put("work_order_jcn", r.getWorkOrder() != null ? r.getWorkOrder().getJcn() : null);
        dto.put("status", r.getStatus());
        dto.put("estimated_delivery_date", r.getEstimatedDeliveryDate());
        return dto;
    }

    @GetMapping({ "/{id}", "/{id}/" })
    @Transactional(readOnly = true)
    public Requisition getRequisition(@PathVariable @NonNull UUID id) {
        return requisitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found: " + id));
    }

    @PostMapping({ "", "/" })
    public ResponseEntity<Requisition> createRequisition(
            @RequestHeader(value = "X-MEW-Persona-ID", required = false) String personaId,
            @Valid @RequestBody @NonNull Requisition requisition) {
        rbacPolicyService.requirePermission(personaId, Resource.REQUISITION, Action.CREATE, "/api/v1/requisitions/");
        requisition.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(requisitionRepository.save(requisition));
    }

    @PutMapping({ "/{id}", "/{id}/" })
    public Requisition updateRequisition(
            @RequestHeader(value = "X-MEW-Persona-ID", required = false) String personaId,
            @PathVariable @NonNull UUID id,
            @Valid @RequestBody @NonNull Requisition payload) {
        rbacPolicyService.requirePermission(personaId, Resource.REQUISITION, Action.UPDATE,
                "/api/v1/requisitions/" + id + "/");
        Requisition existing = requisitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found: " + id));
        payload.setId(existing.getId());
        payload.setCreatedAt(existing.getCreatedAt());
        return requisitionRepository.save(payload);
    }

    @DeleteMapping({ "/{id}", "/{id}/" })
    public ResponseEntity<Void> deleteRequisition(
            @RequestHeader(value = "X-MEW-Persona-ID", required = false) String personaId,
            @PathVariable @NonNull UUID id) {
        rbacPolicyService.requirePermission(personaId, Resource.REQUISITION, Action.DELETE,
                "/api/v1/requisitions/" + id + "/");
        if (!requisitionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Requisition not found: " + id);
        }
        requisitionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping({ "/receive-shipment", "/receive-shipment/" })
    public ResponseEntity<ReceiveShipmentResponse> receiveShipment(
            @RequestHeader(value = "X-MEW-Persona-ID", required = false) String personaId,
            @Valid @RequestBody ReceiveShipmentRequest request) {
        rbacPolicyService.requirePermission(personaId, Resource.REQUISITION, Action.UPDATE,
                "/api/v1/requisitions/receive-shipment/");
        return ResponseEntity.ok(requisitionService.receiveShipment(request));
    }
}
