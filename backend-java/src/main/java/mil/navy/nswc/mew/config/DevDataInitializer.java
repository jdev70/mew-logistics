package mil.navy.nswc.mew.config;

import mil.navy.nswc.mew.domain.entity.Asset;
import mil.navy.nswc.mew.domain.entity.AssetModel;
import mil.navy.nswc.mew.domain.entity.Installation;
import mil.navy.nswc.mew.domain.entity.InventoryBalance;
import mil.navy.nswc.mew.domain.entity.MaterialCatalog;
import mil.navy.nswc.mew.domain.entity.MilitaryUnit;
import mil.navy.nswc.mew.domain.entity.MissionRole;
import mil.navy.nswc.mew.domain.entity.NavalHull;
import mil.navy.nswc.mew.domain.entity.Requisition;
import mil.navy.nswc.mew.domain.entity.WorkOrder;
import mil.navy.nswc.mew.domain.enums.CommandEchelon;
import mil.navy.nswc.mew.domain.enums.EmconPosture;
import mil.navy.nswc.mew.domain.enums.MafStatus;
import mil.navy.nswc.mew.domain.enums.ReadinessLevel;
import mil.navy.nswc.mew.domain.enums.SupplyStatusCode;
import mil.navy.nswc.mew.repository.AssetModelRepository;
import mil.navy.nswc.mew.repository.AssetRepository;
import mil.navy.nswc.mew.repository.InstallationRepository;
import mil.navy.nswc.mew.repository.InventoryBalanceRepository;
import mil.navy.nswc.mew.repository.MaterialCatalogRepository;
import mil.navy.nswc.mew.repository.MilitaryUnitRepository;
import mil.navy.nswc.mew.repository.MissionRoleRepository;
import mil.navy.nswc.mew.repository.NavalHullRepository;
import mil.navy.nswc.mew.repository.RequisitionRepository;
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
        balanceRepository.save(craneBalance);

        InventoryBalance norfolkBalance = new InventoryBalance();
        norfolkBalance.setInstallation(norfolk);
        norfolkBalance.setMaterial(ganModule);
        norfolkBalance.setQuantityOnHand(5);
        norfolkBalance.setReorderPoint(3);
        norfolkBalance.setSafetyStock(1);
        balanceRepository.save(norfolkBalance);

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
    }
}
