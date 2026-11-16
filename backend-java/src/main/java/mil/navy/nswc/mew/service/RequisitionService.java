package mil.navy.nswc.mew.service;

import mil.navy.nswc.mew.domain.entity.Installation;
import mil.navy.nswc.mew.domain.entity.InventoryBalance;
import mil.navy.nswc.mew.domain.entity.Requisition;
import mil.navy.nswc.mew.domain.entity.SecurityAuditLog;
import mil.navy.nswc.mew.domain.enums.SupplyStatusCode;
import mil.navy.nswc.mew.domain.enums.TransactionType;
import mil.navy.nswc.mew.repository.InventoryBalanceRepository;
import mil.navy.nswc.mew.repository.RequisitionRepository;
import mil.navy.nswc.mew.repository.SecurityAuditLogRepository;
import mil.navy.nswc.mew.service.exception.BusinessValidationException;
import mil.navy.nswc.mew.service.exception.ResourceNotFoundException;
import mil.navy.nswc.mew.web.dto.ReceiveShipmentRequest;
import mil.navy.nswc.mew.web.dto.ReceiveShipmentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequisitionService {

    private final RequisitionRepository requisitionRepository;
    private final InventoryBalanceRepository balanceRepository;
    private final InventoryService inventoryService;
    private final SecurityAuditLogRepository auditLogRepository;

    public RequisitionService(RequisitionRepository requisitionRepository,
                              InventoryBalanceRepository balanceRepository,
                              InventoryService inventoryService,
                              SecurityAuditLogRepository auditLogRepository) {
        this.requisitionRepository = requisitionRepository;
        this.balanceRepository = balanceRepository;
        this.inventoryService = inventoryService;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Scanner-friendly MILSTRIP shipment receipt with automatic short-ship (-BO) backorder splitting.
     */
    @Transactional
    public ReceiveShipmentResponse receiveShipment(ReceiveShipmentRequest request) {
        Requisition requisition = requisitionRepository.findByDocumentNumber(request.getDocumentNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Requisition not found: " + request.getDocumentNumber()));

        if ("Received".equalsIgnoreCase(requisition.getStatus())) {
            throw new BusinessValidationException(
                    "Shipment " + requisition.getDocumentNumber() + " has already been received.");
        }

        int orderedQty = requisition.getQuantity();
        int receivedQty = request.getReceivedQuantity() != null
                ? request.getReceivedQuantity()
                : orderedQty;

        if (receivedQty <= 0 || receivedQty > orderedQty) {
            throw new BusinessValidationException(
                    "Invalid received_quantity. Must be between 1 and " + orderedQty + ".");
        }

        boolean backorderCreated = false;
        String backorderDoc = null;
        int backorderQty = 0;

        if (receivedQty < orderedQty) {
            backorderQty = orderedQty - receivedQty;
            backorderDoc = requisition.getDocumentNumber() + "-BO";

            Requisition backorder = new Requisition();
            backorder.setDocumentNumber(backorderDoc);
            backorder.setRequestingUnit(requisition.getRequestingUnit());
            backorder.setMaterial(requisition.getMaterial());
            backorder.setQuantity(backorderQty);
            backorder.setPriority(requisition.getPriority());
            backorder.setTiedToAsset(requisition.getTiedToAsset());
            backorder.setWorkOrder(requisition.getWorkOrder());
            backorder.setStatus("Submitted");
            backorder.setSupplyStatusCode(SupplyStatusCode.BB);
            backorder.setEstimatedDeliveryDate(requisition.getEstimatedDeliveryDate());
            requisitionRepository.save(backorder);

            requisition.setQuantity(receivedQty);
            backorderCreated = true;
        }

        requisition.setStatus("Received");
        requisition.setSupplyStatusCode(SupplyStatusCode.D7);
        requisitionRepository.save(requisition);

        Installation installation = requisition.getRequestingUnit().getInstallation();
        InventoryBalance balance = balanceRepository
                .findByInstallationIdAndMaterialId(installation.getId(), requisition.getMaterial().getId())
                .orElseGet(() -> {
                    InventoryBalance created = new InventoryBalance();
                    created.setInstallation(installation);
                    created.setMaterial(requisition.getMaterial());
                    created.setQuantityOnHand(0);
                    return balanceRepository.save(created);
                });

        inventoryService.executeTransaction(
                balance.getId(),
                TransactionType.RCT,
                receivedQty,
                null,
                requisition.getDocumentNumber(),
                requisition,
                requisition.getWorkOrder()
        );

        SecurityAuditLog audit = new SecurityAuditLog();
        audit.setEventType("REQUISITION_RECEIPT");
        audit.setDecision("ALLOW");
        audit.setResourceTarget(requisition.getDocumentNumber());
        audit.setReason("Received " + receivedQty + "x " + requisition.getMaterial().getNsn() + " into warehouse.");
        auditLogRepository.save(audit);

        ReceiveShipmentResponse response = new ReceiveShipmentResponse();
        response.setMessage("Shipment received and added to inventory.");
        response.setDocumentNumber(requisition.getDocumentNumber());
        response.setMaterialReceived(requisition.getMaterial().getNomenclature());
        response.setQuantityAdded(receivedQty);
        if (backorderCreated) {
            response.setBackorderNotice(
                    "Short shipment. Backorder created: " + backorderDoc + " for " + backorderQty + " units.");
        }
        return response;
    }
}
