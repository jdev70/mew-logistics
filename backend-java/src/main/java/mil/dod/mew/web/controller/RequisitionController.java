package mil.dod.mew.web.controller;

import mil.dod.mew.domain.entity.Requisition;
import mil.dod.mew.repository.RequisitionRepository;
import mil.dod.mew.service.RbacPolicyService;
import mil.dod.mew.service.RbacPolicyService.Action;
import mil.dod.mew.service.RbacPolicyService.Resource;
import mil.dod.mew.security.MewResourceFactory;
import mil.dod.mew.security.model.Permission;
import mil.dod.mew.service.RequisitionService;
import mil.dod.mew.service.exception.ResourceNotFoundException;
import mil.dod.mew.web.dto.ReceiveShipmentRequest;
import mil.dod.mew.web.dto.ReceiveShipmentResponse;
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
        return requisitionRepository.findAll().stream()
                .filter(requisition -> rbacPolicyService.canAccess(Permission.REQUEST_PARTS_REQUISITION,
                        MewResourceFactory.forRequisition(requisition)))
                .map(this::toDto).collect(Collectors.toList());
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
        Requisition requisition = requisitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found: " + id));
        rbacPolicyService.requireAccess(Permission.REQUEST_PARTS_REQUISITION, MewResourceFactory.forRequisition(requisition),
                "/api/v1/requisitions/" + id + "/");
        return requisition;
    }

    @PostMapping({ "", "/" })
    public ResponseEntity<Requisition> createRequisition(
            @Valid @RequestBody @NonNull Requisition requisition) {
        rbacPolicyService.requirePermission(Resource.REQUISITION, Action.CREATE, "/api/v1/requisitions/");
        rbacPolicyService.requireAccess(Permission.REQUEST_PARTS_REQUISITION, MewResourceFactory.forRequisition(requisition),
                "/api/v1/requisitions/");
        requisition.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(requisitionRepository.save(requisition));
    }

    @PutMapping({ "/{id}", "/{id}/" })
    public Requisition updateRequisition(
            @PathVariable @NonNull UUID id,
            @Valid @RequestBody @NonNull Requisition payload) {
        rbacPolicyService.requirePermission(Resource.REQUISITION, Action.UPDATE,
                "/api/v1/requisitions/" + id + "/");
        Requisition existing = requisitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found: " + id));
        rbacPolicyService.requireAccess(Permission.REQUEST_PARTS_REQUISITION, MewResourceFactory.forRequisition(existing),
                "/api/v1/requisitions/" + id + "/");
        payload.setId(existing.getId());
        payload.setCreatedAt(existing.getCreatedAt());
        return requisitionRepository.save(payload);
    }

    @DeleteMapping({ "/{id}", "/{id}/" })
    public ResponseEntity<Void> deleteRequisition(
            @PathVariable @NonNull UUID id) {
        rbacPolicyService.requirePermission(Resource.REQUISITION, Action.DELETE,
                "/api/v1/requisitions/" + id + "/");
        Requisition existing = requisitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found: " + id));
        rbacPolicyService.requireAccess(Permission.APPROVE_PARTS_REQUISITION, MewResourceFactory.forRequisition(existing),
                "/api/v1/requisitions/" + id + "/");
        requisitionRepository.delete(existing);
        return ResponseEntity.noContent().build();
    }

    @PostMapping({ "/receive-shipment", "/receive-shipment/" })
    public ResponseEntity<ReceiveShipmentResponse> receiveShipment(
            @Valid @RequestBody ReceiveShipmentRequest request) {
        rbacPolicyService.requirePermission(Resource.REQUISITION, Action.UPDATE,
                "/api/v1/requisitions/receive-shipment/");
        Requisition requisition = requisitionRepository.findByDocumentNumber(request.getDocumentNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found: " + request.getDocumentNumber()));
        rbacPolicyService.requireAccess(Permission.REQUEST_PARTS_REQUISITION, MewResourceFactory.forRequisition(requisition),
                "/api/v1/requisitions/receive-shipment/");
        return ResponseEntity.ok(requisitionService.receiveShipment(request));
    }
}
