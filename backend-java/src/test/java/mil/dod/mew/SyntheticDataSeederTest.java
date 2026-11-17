package mil.dod.mew;

import mil.dod.mew.domain.entity.InventoryTransaction;
import mil.dod.mew.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"mew.synthetic.scenario=demo", "mew.synthetic.history-days=7"})
@ActiveProfiles("synthetic-data")
class SyntheticDataSeederTest {
    @Autowired InstallationRepository installations;
    @Autowired PersonnelRepository personnel;
    @Autowired NavalHullRepository hulls;
    @Autowired HullSystemAssignmentRepository loadouts;
    @Autowired AssetRepository assets;
    @Autowired MaterialCatalogRepository materials;
    @Autowired InventoryBalanceRepository balances;
    @Autowired InventoryTransactionRepository transactions;
    @Autowired WorkOrderRepository workOrders;
    @Autowired RequisitionRepository requisitions;
    @Autowired SecurityAuditLogRepository audits;
    @Autowired UnitReadinessSnapshotRepository snapshots;

    @Test
    @Transactional
    void seedsAConsistentFictionalDataset() {
        assertTrue(installations.findByInstallationCode("NSWC-CRANE").isPresent());
        assertTrue(installations.findAll().stream().anyMatch(base -> "USMC".equals(base.getServiceBranch())));
        assertTrue(installations.findAll().stream().anyMatch(base -> "USAF".equals(base.getServiceBranch())));
        assertTrue(installations.findAll().stream().anyMatch(base -> "USA".equals(base.getServiceBranch())));
        assertTrue(installations.findAll().stream().allMatch(base -> base.getCatalogSourceId() != null
                && !base.getCatalogSourceId().isBlank() && base.getCatalogSourceUrl().startsWith("https://")
                && base.getCatalogVerifiedOn() != null && base.isSyntheticOperationalData()));
        assertEquals("CFA-YOKOSUKA", hulls.findByHullNumber("DDG-114").orElseThrow().getHomeportInstallation().getInstallationCode());
        assertTrue(loadouts.findAll().stream().anyMatch(loadout -> "AN/SLQ-32(V)7".equals(loadout.getNomenclature())));
        var marcus = personnel.findByIdentitySubject("demo:marcus-vance").orElseThrow();
        assertEquals("CDR", marcus.getRank());
        assertEquals("TS/SCI", marcus.getSecurityClearance());
        assertEquals("FLEET_READINESS_ANALYST", marcus.getFunctionalRole().name());
        assertTrue(marcus.getAuthorizationPermissions().contains("VIEW_GLOBAL_READINESS"));
        assertFalse(personnel.findAll().isEmpty());
        Set<String> seededLastNames = personnel.findAll().stream()
                .filter(person -> !"MEW-PER-0001".equals(person.getPersonnelId()))
                .map(person -> person.getLastName())
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(seededLastNames.size() >= 40,
                "Synthetic demo personnel should use a varied surname pool");
        assertFalse(assets.findAll().isEmpty());
        assertFalse(materials.findAll().isEmpty());
        assertFalse(workOrders.findAll().isEmpty());
        assertEquals(workOrders.count(), requisitions.count());
        assertFalse(audits.findAll().isEmpty());
        assertFalse(snapshots.findAll().isEmpty());

        for (InventoryTransaction transaction : transactions.findAll()) {
            int expected = transaction.getTransactionType().name().equals("RCT")
                    ? transaction.getPreviousBalance() + transaction.getQuantity()
                    : transaction.getPreviousBalance() - transaction.getQuantity();
            assertEquals(expected, transaction.getNewBalance());
        }
        assertTrue(balances.findAll().stream().allMatch(b -> b.getQuantityOnHand() >= 0));
    }
}
