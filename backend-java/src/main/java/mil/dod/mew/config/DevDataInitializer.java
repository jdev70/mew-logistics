package mil.dod.mew.config;

import mil.dod.mew.domain.entity.Asset;
import mil.dod.mew.domain.entity.AssetModel;
import mil.dod.mew.domain.entity.Installation;
import mil.dod.mew.domain.entity.InventoryBalance;
import mil.dod.mew.domain.entity.InventoryTransaction;
import mil.dod.mew.domain.entity.MaterialCatalog;
import mil.dod.mew.domain.entity.MilitaryUnit;
import mil.dod.mew.domain.entity.MissionRole;
import mil.dod.mew.domain.entity.NavalHull;
import mil.dod.mew.domain.entity.Personnel;
import mil.dod.mew.domain.entity.Requisition;
import mil.dod.mew.domain.entity.SecurityAuditLog;
import mil.dod.mew.domain.entity.WorkOrder;
import mil.dod.mew.domain.enums.CommandEchelon;
import mil.dod.mew.domain.enums.EmconPosture;
import mil.dod.mew.domain.enums.FunctionalRole;
import mil.dod.mew.domain.enums.MafStatus;
import mil.dod.mew.domain.enums.ReadinessLevel;
import mil.dod.mew.domain.enums.SupplyStatusCode;
import mil.dod.mew.domain.enums.TransactionType;
import mil.dod.mew.repository.AssetModelRepository;
import mil.dod.mew.repository.AssetRepository;
import mil.dod.mew.repository.InstallationRepository;
import mil.dod.mew.repository.InventoryBalanceRepository;
import mil.dod.mew.repository.InventoryTransactionRepository;
import mil.dod.mew.repository.MaterialCatalogRepository;
import mil.dod.mew.repository.MilitaryUnitRepository;
import mil.dod.mew.repository.MissionRoleRepository;
import mil.dod.mew.repository.NavalHullRepository;
import mil.dod.mew.repository.PersonnelRepository;
import mil.dod.mew.repository.RequisitionRepository;
import mil.dod.mew.repository.SecurityAuditLogRepository;
import mil.dod.mew.repository.WorkOrderRepository;
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
@Profile({"default", "local", "oracle-local"})
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
            backfillLocalAuthorizationAssignments();
            return;
        }

        Installation crane = new Installation();
        crane.setInstallationId("INST-001");
        crane.setInstallationCode("NSWC-CRANE");
        crane.setServiceBranch("JOINT");
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
        norfolk.setServiceBranch("USN");
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
        asset1.setServiceBranch("USN");
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
        psm.setIdentitySubject("MEW-PER-0000");
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
        psm.setServiceBranch("JOINT");
        psm.setCommandTier("STRATEGIC_JOINT");
        psm.setAuthorizationPermissions("ALL");
        psm = personnelRepository.save(psm);

        Personnel tech = new Personnel();
        tech.setPersonnelId("MEW-PER-0002");
        tech.setIdentitySubject("MEW-PER-0002");
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
        tech.setServiceBranch("USN");
        tech.setCommandTier("INSTALLATION_UNIT");
        tech.setAuthorizedInstallations("NS-NORFOLK");
        tech.setAuthorizationPermissions("VIEW_INVENTORY,LOG_MAINTENANCE_ACTION");
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
        wo.setTechnician(tech);
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

        seedInterviewDemoPortfolio(psm, tech, asset1, norfolk, ddg51);

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

    /** Keeps pre-existing local H2 seed data usable after authorization schema upgrades. */
    private void backfillLocalAuthorizationAssignments() {
        installationRepository.findByInstallationCode("NS-NORFOLK").ifPresent(installation -> {
            installation.setServiceBranch("USN");
            installationRepository.save(installation);
        });
        assetRepository.findBySerialNumber("SLQ32V6-DDG51-001").ifPresent(asset -> {
            asset.setServiceBranch("USN");
            assetRepository.save(asset);
        });
        personnelRepository.findByPersonnelId("MEW-PER-0000").ifPresent(personnel -> {
            if (personnel.getIdentitySubject() == null || personnel.getIdentitySubject().isBlank()) {
                personnel.setIdentitySubject("MEW-PER-0000");
            }
            personnel.setServiceBranch("JOINT");
            personnel.setCommandTier("STRATEGIC_JOINT");
            personnel.setAuthorizationPermissions("ALL");
            personnelRepository.save(personnel);
        });
        personnelRepository.findByPersonnelId("MEW-PER-0002").ifPresent(personnel -> {
            if (personnel.getIdentitySubject() == null || personnel.getIdentitySubject().isBlank()) {
                personnel.setIdentitySubject("MEW-PER-0002");
            }
            personnel.setServiceBranch("USN");
            personnel.setCommandTier("INSTALLATION_UNIT");
            personnel.setAuthorizedInstallations("NS-NORFOLK");
            personnel.setAuthorizationPermissions("VIEW_INVENTORY,LOG_MAINTENANCE_ACTION");
            personnelRepository.save(personnel);
            workOrderRepository.findByJcn("N21944-CE02-0001").ifPresent(workOrder -> {
                if (workOrder.getTechnician() == null) {
                    workOrder.setTechnician(personnel);
                    workOrderRepository.save(workOrder);
                }
            });
        });
        personnelRepository.findByPersonnelId("MEW-PER-0000").ifPresent(psm ->
                personnelRepository.findByPersonnelId("MEW-PER-0002").ifPresent(tech ->
                        assetRepository.findBySerialNumber("SLQ32V6-DDG51-001").ifPresent(asset ->
                                seedInterviewDemoPortfolio(psm, tech, asset, asset.getCurrentLocation(), asset.getHull()))));
    }

    /** Adds a small, idempotent local portfolio for an end-to-end interview demonstration. */
    private void seedInterviewDemoPortfolio(Personnel psm, Personnel technician, Asset asset,
                                            Installation installation, NavalHull hull) {
        Personnel lead = ensureDemoPersonnel("MEW-PER-0003", "Taylor", "Brooks", "ETC", "E-7",
                "EW Division Leading Chief / CE02", FunctionalRole.SHIPBOARD_TECHNICIAN,
                technician.getUnit(), installation, "N21944");
        Personnel qa = ensureDemoPersonnel("MEW-PER-0004", "Morgan", "Hale", "CIV (GS-13)", "GS-13",
                "Fleet Maintenance QA Reviewer", FunctionalRole.FLEET_READINESS_ANALYST,
                technician.getUnit(), installation, "N00060");
        ensureDemoPersonnel("MEW-PER-0005", "Casey", "Ward", "LS1", "E-6",
                "Shipboard Supply Petty Officer", FunctionalRole.UNIT_SUPPO,
                technician.getUnit(), installation, "N21944");

        ensureDemoWorkOrder("N21944-CE02-0002", MafStatus.IN_WORK, 2, technician, null, asset, hull, installation,
                "CE02", "Intermittent receiver thermal alarm during continuous emit operations.");
        ensureDemoWorkOrder("N21944-CE02-0003", MafStatus.QA_PENDING, 1, technician, qa, asset, hull, installation,
                "CE02", "BIT fault 041: Starboard antenna control voltage out of tolerance.");
        ensureDemoWorkOrder("N21944-CE02-0004", MafStatus.AWAITING_PARTS, 2, lead, null, asset, hull, installation,
                "CE02", "Cooling-loop pressure alarm; replacement regulator requisition in transit.");
        ensureDemoWorkOrder("N21944-CE02-0005", MafStatus.CLOSED, 2, lead, qa, asset, hull, installation,
                "CE02", "Receiver processor fan tray replacement and post-maintenance test complete.");

        if (auditLogRepository.count() < 8) {
            saveDemoAudit(technician, "MAINTENANCE_ASSIGNMENT", "/api/v1/work-orders/N21944-CE02-0002/",
                    "Assigned IN_WORK MAF N21944-CE02-0002 to CE02 technician.");
            saveDemoAudit(technician, "MAINTENANCE_SUBMISSION", "/api/v1/work-orders/N21944-CE02-0003/submit-maintenance/",
                    "Technician submitted structured maintenance resolution for QA review.");
            saveDemoAudit(qa, "QA_REVIEW_PENDING", "/api/v1/work-orders/N21944-CE02-0003/",
                    "Independent QA review queued for priority 1 maintenance action.");
            saveDemoAudit(psm, "MAF_CLOSURE", "/api/v1/work-orders/N21944-CE02-0005/close/",
                    "Fleet readiness reviewer accepted QA and closed the MAF.");
            saveDemoAudit(lead, "SUPPLY_DEPENDENCY", "/api/v1/work-orders/N21944-CE02-0004/",
                    "Maintenance action remains awaiting repair-part availability.");
        }
    }

    private Personnel ensureDemoPersonnel(String personnelId, String firstName, String lastName, String rank,
                                          String paygrade, String title, FunctionalRole functionalRole,
                                          MilitaryUnit unit, Installation installation, String uic) {
        return personnelRepository.findByPersonnelId(personnelId).orElseGet(() -> {
            Personnel personnel = new Personnel();
            personnel.setPersonnelId(personnelId);
            personnel.setDodId("100000" + personnelId.substring(personnelId.length() - 4));
            personnel.setFirstName(firstName);
            personnel.setLastName(lastName);
            personnel.setRank(rank);
            personnel.setPaygrade(paygrade);
            personnel.setTitle(title);
            personnel.setFunctionalRole(functionalRole);
            personnel.setAssignedUic(uic);
            personnel.setUnit(unit);
            personnel.setInstallation(installation);
            personnel.setSecurityClearance("TS/SCI");
            personnel.setServiceBranch("USN");
            personnel.setCommandTier("INSTALLATION_UNIT");
            personnel.setAuthorizedInstallations("NS-NORFOLK");
            personnel.setAuthorizationPermissions("VIEW_INVENTORY,LOG_MAINTENANCE_ACTION");
            return personnelRepository.save(personnel);
        });
    }

    private void ensureDemoWorkOrder(String jcn, MafStatus status, int priority, Personnel technician,
                                     Personnel qa, Asset asset, NavalHull hull, Installation installation,
                                     String workCenter, String fault) {
        if (workOrderRepository.findByJcn(jcn).isPresent()) {
            return;
        }
        WorkOrder workOrder = new WorkOrder();
        workOrder.setJcn(jcn);
        workOrder.setStatus(status);
        workOrder.setPriorityCode(priority);
        workOrder.setTechnician(technician);
        workOrder.setQaInspector(qa);
        workOrder.setAsset(asset);
        workOrder.setHull(hull);
        workOrder.setInstallation(installation);
        workOrder.setWorkCenter(workCenter);
        workOrder.setDescriptionOfFault(fault);
        workOrderRepository.save(workOrder);
    }

    private void saveDemoAudit(Personnel actor, String eventType, String resourceTarget, String reason) {
        SecurityAuditLog audit = new SecurityAuditLog();
        audit.setActor(actor);
        audit.setActorName(actor.getRank() + " " + actor.getFirstName() + " " + actor.getLastName());
        audit.setActorRole(actor.getFunctionalRole().name());
        audit.setActorUic(actor.getAssignedUic());
        audit.setActorClearance(actor.getSecurityClearance());
        audit.setEventType(eventType);
        audit.setDecision("ALLOW");
        audit.setResourceTarget(resourceTarget);
        audit.setReason(reason);
        auditLogRepository.save(audit);
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
        inst.setServiceBranch(branch);
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
