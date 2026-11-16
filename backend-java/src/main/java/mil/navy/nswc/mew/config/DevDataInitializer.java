package mil.navy.nswc.mew.config;

import mil.navy.nswc.mew.domain.entity.Asset;
import mil.navy.nswc.mew.domain.entity.AssetModel;
import mil.navy.nswc.mew.domain.entity.Installation;
import mil.navy.nswc.mew.domain.entity.InventoryBalance;
import mil.navy.nswc.mew.domain.entity.InventoryTransaction;
import mil.navy.nswc.mew.domain.entity.MaterialCatalog;
import mil.navy.nswc.mew.domain.entity.MilitaryUnit;
import mil.navy.nswc.mew.domain.entity.MissionRole;
import mil.navy.nswc.mew.domain.entity.NavalHull;
import mil.navy.nswc.mew.domain.entity.Personnel;
import mil.navy.nswc.mew.domain.entity.Requisition;
import mil.navy.nswc.mew.domain.entity.SecurityAuditLog;
import mil.navy.nswc.mew.domain.entity.WorkOrder;
import mil.navy.nswc.mew.domain.enums.CommandEchelon;
import mil.navy.nswc.mew.domain.enums.EmconPosture;
import mil.navy.nswc.mew.domain.enums.FunctionalRole;
import mil.navy.nswc.mew.domain.enums.MafStatus;
import mil.navy.nswc.mew.domain.enums.ReadinessLevel;
import mil.navy.nswc.mew.domain.enums.SupplyStatusCode;
import mil.navy.nswc.mew.domain.enums.TransactionType;
import mil.navy.nswc.mew.repository.AssetModelRepository;
import mil.navy.nswc.mew.repository.AssetRepository;
import mil.navy.nswc.mew.repository.InstallationRepository;
import mil.navy.nswc.mew.repository.InventoryBalanceRepository;
import mil.navy.nswc.mew.repository.InventoryTransactionRepository;
import mil.navy.nswc.mew.repository.MaterialCatalogRepository;
import mil.navy.nswc.mew.repository.MilitaryUnitRepository;
import mil.navy.nswc.mew.repository.MissionRoleRepository;
import mil.navy.nswc.mew.repository.NavalHullRepository;
import mil.navy.nswc.mew.repository.PersonnelRepository;
import mil.navy.nswc.mew.repository.RequisitionRepository;
import mil.navy.nswc.mew.repository.SecurityAuditLogRepository;
import mil.navy.nswc.mew.repository.WorkOrderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Seeds sample Maritime Electromagnetic Warfare (MEW) data for local H2 development.
 */
@Component
@Profile("default")
public class DevDataInitializer implements CommandLineRunner {

    private final InstallationRepository installationRepository;
    private final MilitaryUnitRepository unitRepository;
    private final NavalHullRepository hullRepository;
    private final MissionRoleRepository roleRepository;
    private final AssetModelRepository modelRepository;
    private final MaterialCatalogRepository materialRepository;
    private final AssetRepository assetRepository;
    private final InventoryBalanceRepository balanceRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final PersonnelRepository personnelRepository;
    private final SecurityAuditLogRepository auditLogRepository;
    private final WorkOrderRepository workOrderRepository;
    private final RequisitionRepository requisitionRepository;

    public DevDataInitializer(InstallationRepository installationRepository,
                              MilitaryUnitRepository unitRepository,
                              NavalHullRepository hullRepository,
                              MissionRoleRepository roleRepository,
                              AssetModelRepository modelRepository,
                              MaterialCatalogRepository materialRepository,
                              AssetRepository assetRepository,
                              InventoryBalanceRepository balanceRepository,
                              InventoryTransactionRepository transactionRepository,
                              PersonnelRepository personnelRepository,
                              SecurityAuditLogRepository auditLogRepository,
                              WorkOrderRepository workOrderRepository,
                              RequisitionRepository requisitionRepository) {
        this.installationRepository = installationRepository;
        this.unitRepository = unitRepository;
        this.hullRepository = hullRepository;
        this.roleRepository = roleRepository;
        this.modelRepository = modelRepository;
        this.materialRepository = materialRepository;
        this.assetRepository = assetRepository;
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
        this.personnelRepository = personnelRepository;
        this.auditLogRepository = auditLogRepository;
        this.workOrderRepository = workOrderRepository;
        this.requisitionRepository = requisitionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (installationRepository.count() > 0) {
            return;
        }

        Installation crane = new Installation();
        crane.setInstallationId("INST-001");
        crane.setInstallationCode("NSWC-CRANE");
        crane.setName("NSWC Crane Division");
        crane.setDodaac("N00164");
        crane.setCity("Crane");
        crane.setStateOrCountry("IN");
        crane.setRegion("CONUS Midwest");
        crane.setMissionCategory("R&D_Warfare_Center");
        crane.setPrimaryEwFocus("SEWIP / Surface EW Depot & Sustainment");
        crane.setReadinessRating("C1_Fully_Mission_Capable");
        crane = installationRepository.save(crane);

        Installation norfolk = new Installation();
        norfolk.setInstallationId("INST-002");
        norfolk.setInstallationCode("NS-NORFOLK");
        norfolk.setName("Naval Station Norfolk");
        norfolk.setDodaac("N00188");
        norfolk.setCity("Norfolk");
        norfolk.setStateOrCountry("VA");
        norfolk.setRegion("CONUS East");
        norfolk.setMissionCategory("Fleet_Homeport");
        norfolk.setPrimaryEwFocus("Atlantic Fleet Surface EW Support");
        norfolk.setReadinessRating("C1_Fully_Mission_Capable");
        norfolk = installationRepository.save(norfolk);

        MilitaryUnit tycom = new MilitaryUnit();
        tycom.setName("COMNAVSURFLANT");
        tycom.setUic("N00060");
        tycom.setEchelonLevel(CommandEchelon.TYCOM_FLEET);
        tycom.setInstallation(norfolk);
        tycom.setCommandType("Type Commander");
        tycom.setPersonnelReadinessPct(new BigDecimal("98.50"));
        tycom.setEquipmentReadinessPct(new BigDecimal("96.20"));
        tycom = unitRepository.save(tycom);

        MilitaryUnit ddgUnit = new MilitaryUnit();
        ddgUnit.setName("USS Arleigh Burke (DDG-51)");
        ddgUnit.setUic("N21944");
        ddgUnit.setParentUnit(tycom);
        ddgUnit.setEchelonLevel(CommandEchelon.COMBATANT_UNIT);
        ddgUnit.setInstallation(norfolk);
        ddgUnit.setCommandType("Guided Missile Destroyer");
        ddgUnit.setPersonnelReadinessPct(new BigDecimal("97.00"));
        ddgUnit.setEquipmentReadinessPct(new BigDecimal("94.50"));
        ddgUnit = unitRepository.save(ddgUnit);

        NavalHull ddg51 = new NavalHull();
        ddg51.setHullId("HULL-001");
        ddg51.setHullNumber("DDG-51");
        ddg51.setVesselName("USS Arleigh Burke");
        ddg51.setShipClass("Arleigh Burke-class Flight I");
        ddg51.setDomain("Surface");
        ddg51.setHomeport("NS Norfolk, VA");
        ddg51.setHomeportInstallation(norfolk);
        ddg51.setUnit(ddgUnit);
        ddg51.setEmconPosture(EmconPosture.CHARLIE);
        ddg51 = hullRepository.save(ddg51);

        MissionRole asmd = new MissionRole();
        asmd.setRoleId("ROLE-01");
        asmd.setRoleCode("ASMD-EW");
        asmd.setRoleName("Anti-Ship Missile Defense");
        asmd = roleRepository.save(asmd);

        AssetModel slq32v6 = new AssetModel();
        slq32v6.setSystemId("SYS-101");
        slq32v6.setNomenclature("AN/SLQ-32(V)6");
        slq32v6.setCommonName("SEWIP Block 2");
        slq32v6.setEwCategory("Electronic Support (ES)");
        slq32v6.setAssetClass("Electronic Support (ES)");
        slq32v6.setManufacturer("Lockheed Martin");
        slq32v6.setCageCode("03538");
        slq32v6 = modelRepository.save(slq32v6);

        MaterialCatalog ganModule = new MaterialCatalog();
        ganModule.setComponentId("CMP-201");
        ganModule.setNsn("5985-01-654-3210");
        ganModule.setNomenclature("GaN T/R Module Assembly");
        ganModule.setPartNumber("LM-SEWIP-TR-402");
        ganModule.setCageCode("03538");
        ganModule.setManufacturer("Lockheed Martin");
        ganModule.setComponentCategory("GaN T/R Module");
        ganModule.setUnitPrice(new BigDecimal("48500.00"));
        ganModule.setCriticalItem(true);
        ganModule = materialRepository.save(ganModule);

        Asset asset1 = new Asset();
        asset1.setAssignmentId("ASGN-0001");
        asset1.setAssetModel(slq32v6);
        asset1.setSerialNumber("SLQ32V6-DDG51-001");
        asset1.setAssignedUnit(ddgUnit);
        asset1.setCurrentLocation(norfolk);
        asset1.setHull(ddg51);
        asset1.setMissionRole(asmd);
        asset1.setReadinessStatus(ReadinessLevel.FMC);
        asset1.setInstallationDate(LocalDate.now().minusYears(2));
        asset1.setMtbfHours(2450.0);
        asset1.setMttrHours(4.5);
        asset1.setOperationalAvailabilityPct(99.2);
        asset1 = assetRepository.save(asset1);

        InventoryBalance craneBalance = new InventoryBalance();
        craneBalance.setInstallation(crane);
        craneBalance.setMaterial(ganModule);
        craneBalance.setQuantityOnHand(12);
        craneBalance.setReorderPoint(4);
        craneBalance.setSafetyStock(2);
        craneBalance = balanceRepository.save(craneBalance);

        InventoryBalance norfolkBalance = new InventoryBalance();
        norfolkBalance.setInstallation(norfolk);
        norfolkBalance.setMaterial(ganModule);
        norfolkBalance.setQuantityOnHand(2);
        norfolkBalance.setReorderPoint(5);
        norfolkBalance.setSafetyStock(2);
        norfolkBalance = balanceRepository.save(norfolkBalance);

        Personnel psm = new Personnel();
        psm.setPersonnelId("MEW-PER-0000");
        psm.setDodId("1000000001");
        psm.setFirstName("Evelyn");
        psm.setLastName("Thorne");
        psm.setPersonnelCategory("DoD_Civilian");
        psm.setPaygrade("GS-15");
        psm.setRank("CIV (GS-15)");
        psm.setTitle("PEO IWS 2.0 / NSWC Crane Enterprise MEW PSM");
        psm.setFunctionalRole(FunctionalRole.FLEET_READINESS_ANALYST);
        psm.setAssignedUic("N00060");
        psm.setUnit(tycom);
        psm.setInstallation(crane);
        psm.setSecurityClearance("TS/SCI");
        psm = personnelRepository.save(psm);

        Personnel tech = new Personnel();
        tech.setPersonnelId("MEW-PER-0002");
        tech.setDodId("1000000002");
        tech.setFirstName("Devon");
        tech.setLastName("Mercer");
        tech.setPersonnelCategory("Active_Duty_Enlisted");
        tech.setPaygrade("E-6");
        tech.setRank("CTT1");
        tech.setTitle("Shipboard EW Work Center Supervisor (CE02)");
        tech.setFunctionalRole(FunctionalRole.SHIPBOARD_TECHNICIAN);
        tech.setAssignedUic("N21944");
        tech.setUnit(ddgUnit);
        tech.setInstallation(norfolk);
        tech.setSecurityClearance("TS/SCI");
        tech = personnelRepository.save(tech);

        InventoryTransaction initTx = new InventoryTransaction();
        initTx.setBalance(craneBalance);
        initTx.setTransactionType(TransactionType.RCT);
        initTx.setQuantity(12);
        initTx.setPreviousBalance(0);
        initTx.setNewBalance(12);
        initTx.setReferenceDocument("N0016460900001");
        initTx.setPerformedBy(psm);
        transactionRepository.save(initTx);

        SecurityAuditLog initAudit = new SecurityAuditLog();
        initAudit.setActor(psm);
        initAudit.setActorName("CIV (GS-15) Evelyn Thorne");
        initAudit.setActorRole("FLEET_READINESS_ANALYST");
        initAudit.setActorUic("N00060");
        initAudit.setActorClearance("TS/SCI");
        initAudit.setEventType("CROSS_UNIT_QUERY");
        initAudit.setDecision("ALLOW");
        initAudit.setResourceTarget("/api/v1/dashboard/units/");
        initAudit.setReason("Initial baseline readiness telemetry initialized.");
        auditLogRepository.save(initAudit);

        WorkOrder wo = new WorkOrder();
        wo.setJcn("N21944-CE02-0001");
        wo.setStatus(MafStatus.IN_WORK);
        wo.setPriorityCode(2);
        wo.setAsset(asset1);
        wo.setHull(ddg51);
        wo.setInstallation(norfolk);
        wo.setDescriptionOfFault("BIT fault 160: Port array low RF gain output during operational test.");
        workOrderRepository.save(wo);

        Requisition req = new Requisition();
        req.setDocumentNumber("N2194461000001");
        req.setRequestingUnit(ddgUnit);
        req.setMaterial(ganModule);
        req.setQuantity(4);
        req.setPriority(2);
        req.setSupplyStatusCode(SupplyStatusCode.BV);
        req.setTiedToAsset(asset1);
        req.setWorkOrder(wo);
        req.setStatus("In Transit");
        req.setEstimatedDeliveryDate(LocalDate.now().plusDays(3));
        requisitionRepository.save(req);

        // --- Additional Service Branches (USMC, USA, USAF) ---
        seedBranchSample(
                "INST-006", "MCB-PENDLETON", "Marine Corps Base Camp Pendleton", "M00681",
                "Oceanside", "CA", "CONUS West", "Expeditionary_EW_Battalion",
                "1st Radio Battalion / Expeditionary SIGINT & Ground EA",
                "VMGR-352 (KC-130J)", "M20001", "Airborne",
                "SYS-201", "AN/ALQ-231", "Intrepid Tiger II", "Electronic Attack (EA)",
                "Northrop Grumman", "26916", "ASGN-0101", "ALQ231-VMGR352-001", "USMC",
                2450.0, 3.2, 98.5);

        seedBranchSample(
                "INST-007", "MCB-LEJEUNE", "Marine Corps Base Camp Lejeune", "M67001",
                "Jacksonville", "NC", "CONUS East", "Expeditionary_EW_Battalion",
                "2d Radio Battalion / Littoral & Amphibious EMSO",
                "2d Radio Battalion", "M20002", "Ground",
                "SYS-202", "CESAS II", "Comm Emitter Sensing & Attack II", "Electronic Attack (EA)",
                "General Dynamics", "1V0L6", "ASGN-0102", "CESAS2-2RADBN-004", "USMC",
                2100.0, 4.0, 97.8);

        seedBranchSample(
                "INST-010", "FT-CAVAZOS", "Fort Cavazos", "W90001",
                "Killeen", "TX", "CONUS Central", "Operational_Command",
                "1st Cavalry Division / Multi-Domain EW",
                "1st Cavalry Division EW Platoon", "W30001", "Ground",
                "SYS-301", "TLS-BCT", "Terrestrial Layer System - BCT", "Electronic Attack (EA)",
                "Lockheed Martin", "03538", "ASGN-0201", "TLSBCT-1CD-012", "USA",
                1950.0, 5.0, 96.9);

        seedBranchSample(
                "INST-011", "DAVIS-MONTHAN", "Davis-Monthan AFB", "F48001",
                "Tucson", "AZ", "CONUS West", "Operational_Command",
                "43rd Electronic Combat Squadron / Compass Call",
                "43rd Electronic Combat Squadron", "F40001", "Airborne",
                "SYS-401", "EA-37B", "Compass Call", "Electronic Attack (EA)",
                "BAE Systems", "94117", "ASGN-0301", "EA37B-43ECS-002", "USAF",
                2300.0, 6.0, 97.5);
    }

    private void seedBranchSample(
            String instId, String instCode, String instName, String dodaac,
            String city, String state, String region, String missionCat, String ewFocus,
            String unitName, String uic, String domain,
            String sysId, String nomenclature, String commonName, String ewCat,
            String mfg, String cage, String asgnId, String serial, String branch,
            double mtbf, double mttr, double ao) {
        Installation inst = new Installation();
        inst.setInstallationId(instId);
        inst.setInstallationCode(instCode);
        inst.setName(instName);
        inst.setDodaac(dodaac);
        inst.setCity(city);
        inst.setStateOrCountry(state);
        inst.setRegion(region);
        inst.setMissionCategory(missionCat);
        inst.setPrimaryEwFocus(ewFocus);
        inst.setReadinessRating("C1_Fully_Mission_Capable");
        inst = installationRepository.save(inst);

        MilitaryUnit unit = new MilitaryUnit();
        unit.setName(unitName);
        unit.setUic(uic);
        unit.setEchelonLevel(CommandEchelon.COMBATANT_UNIT);
        unit.setInstallation(inst);
        unit.setCommandType(domain);
        unit.setPersonnelReadinessPct(new BigDecimal("96.00"));
        unit.setEquipmentReadinessPct(new BigDecimal("95.00"));
        unit = unitRepository.save(unit);

        AssetModel model = new AssetModel();
        model.setSystemId(sysId);
        model.setNomenclature(nomenclature);
        model.setCommonName(commonName);
        model.setEwCategory(ewCat);
        model.setAssetClass(ewCat);
        model.setManufacturer(mfg);
        model.setCageCode(cage);
        model = modelRepository.save(model);

        Asset asset = new Asset();
        asset.setAssignmentId(asgnId);
        asset.setAssetModel(model);
        asset.setSerialNumber(serial);
        asset.setAssignedUnit(unit);
        asset.setCurrentLocation(inst);
        asset.setDomain(domain);
        asset.setServiceBranch(branch);
        asset.setReadinessStatus(ReadinessLevel.FMC);
        asset.setInstallationDate(LocalDate.now().minusMonths(6));
        asset.setMtbfHours(mtbf);
        asset.setMttrHours(mttr);
        asset.setOperationalAvailabilityPct(ao);
        assetRepository.save(asset);
    }
}
