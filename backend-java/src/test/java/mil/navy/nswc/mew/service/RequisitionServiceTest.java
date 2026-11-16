package mil.navy.nswc.mew.service;

import mil.navy.nswc.mew.domain.entity.Installation;
import mil.navy.nswc.mew.domain.entity.InventoryBalance;
import mil.navy.nswc.mew.domain.entity.MaterialCatalog;
import mil.navy.nswc.mew.domain.entity.MilitaryUnit;
import mil.navy.nswc.mew.domain.entity.Requisition;
import mil.navy.nswc.mew.domain.entity.SecurityAuditLog;
import mil.navy.nswc.mew.domain.enums.SupplyStatusCode;
import mil.navy.nswc.mew.domain.enums.TransactionType;
import mil.navy.nswc.mew.repository.InventoryBalanceRepository;
import mil.navy.nswc.mew.repository.RequisitionRepository;
import mil.navy.nswc.mew.repository.SecurityAuditLogRepository;
import mil.navy.nswc.mew.service.exception.BusinessValidationException;
import mil.navy.nswc.mew.web.dto.ReceiveShipmentRequest;
import mil.navy.nswc.mew.web.dto.ReceiveShipmentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class RequisitionServiceTest {

    @Mock
    private RequisitionRepository requisitionRepository;
    @Mock
    private InventoryBalanceRepository balanceRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private SecurityAuditLogRepository auditLogRepository;

    @InjectMocks
    private RequisitionService requisitionService;

    private Requisition requisition;
    private InventoryBalance balance;

    @BeforeEach
    void setUp() {
        Installation installation = new Installation();
        installation.setId(UUID.randomUUID());
        installation.setName("NSWC Crane");

        MilitaryUnit unit = new MilitaryUnit();
        unit.setId(UUID.randomUUID());
        unit.setInstallation(installation);

        MaterialCatalog material = new MaterialCatalog();
        material.setId(UUID.randomUUID());
        material.setNsn("5985-01-654-3210");
        material.setNomenclature("GaN T/R Module");

        requisition = new Requisition();
        requisition.setId(UUID.randomUUID());
        requisition.setDocumentNumber("N2000161000001");
        requisition.setRequestingUnit(unit);
        requisition.setMaterial(material);
        requisition.setQuantity(5);
        requisition.setStatus("In Transit");

        balance = new InventoryBalance();
        balance.setId(UUID.randomUUID());
        balance.setInstallation(installation);
        balance.setMaterial(material);
        balance.setQuantityOnHand(2);
    }

    @Test
    void receiveFullShipmentUpdatesStatusAndAddsInventory() {
        when(requisitionRepository.findByDocumentNumber("N2000161000001"))
                .thenReturn(Optional.of(requisition));
        when(balanceRepository.findByInstallationIdAndMaterialId(any(), any()))
                .thenReturn(Optional.of(balance));

        ReceiveShipmentResponse resp = requisitionService.receiveShipment(
                new ReceiveShipmentRequest("N2000161000001", null));

        assertEquals("Received", requisition.getStatus());
        assertEquals(SupplyStatusCode.D7, requisition.getSupplyStatusCode());
        assertEquals(5, resp.getQuantityAdded());
        assertNull(resp.getBackorderNotice());
        verify(inventoryService).executeTransaction(
                eq(balance.getId()), eq(TransactionType.RCT), eq(5), any(),
                eq("N2000161000001"), eq(requisition), any());
        verify(auditLogRepository).save(any(SecurityAuditLog.class));
    }

    @Test
    void receiveShortShipmentSplitsBackorder() {
        when(requisitionRepository.findByDocumentNumber("N2000161000001"))
                .thenReturn(Optional.of(requisition));
        when(balanceRepository.findByInstallationIdAndMaterialId(any(), any()))
                .thenReturn(Optional.of(balance));

        ReceiveShipmentResponse resp = requisitionService.receiveShipment(
                new ReceiveShipmentRequest("N2000161000001", 3));

        assertEquals(3, resp.getQuantityAdded());
        assertNotNull(resp.getBackorderNotice());
        ArgumentCaptor<Requisition> captor = ArgumentCaptor.forClass(Requisition.class);
        verify(requisitionRepository, times(2)).save(captor.capture());

        Requisition backorder = captor.getAllValues().get(0);
        assertEquals("N2000161000001-BO", backorder.getDocumentNumber());
        assertEquals(2, backorder.getQuantity());
        assertEquals(SupplyStatusCode.BB, backorder.getSupplyStatusCode());
    }

    @Test
    void receiveAlreadyReceivedShipmentThrowsValidation() {
        requisition.setStatus("Received");
        when(requisitionRepository.findByDocumentNumber("N2000161000001"))
                .thenReturn(Optional.of(requisition));

        assertThrows(BusinessValidationException.class, () ->
                requisitionService.receiveShipment(new ReceiveShipmentRequest("N2000161000001", 5)));
    }
}
