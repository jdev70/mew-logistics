package mil.dod.mew.config;

import mil.dod.mew.domain.entity.*;
import mil.dod.mew.domain.enums.*;
import mil.dod.mew.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Creates an explicitly fictional, deterministic logistics dataset.  It is deliberately
 * profile-gated so production, Oracle, and the small interview-demo initializer never run it.
 */
@Component
@Profile("synthetic-data")
public class SyntheticDataSeeder implements CommandLineRunner {
    private final InstallationRepository installations;
    private final CatalogSourceRepository catalogSources;
    private final CatalogAttributionRepository attributions;
    private final MilitaryUnitRepository units;
    private final NavalHullRepository hulls;
    private final MissionRoleRepository roles;
    private final AssetModelRepository models;
    private final PersonnelRepository personnel;
    private final MaterialCatalogRepository materials;
    private final AssetRepository assets;
    private final HullSystemAssignmentRepository assignments;
    private final InventoryBalanceRepository balances;
    private final InventoryTransactionRepository transactions;
    private final WorkOrderRepository workOrders;
    private final WorkOrderPartRepository workOrderParts;
    private final RequisitionRepository requisitions;
    private final SecurityAuditLogRepository audits;
    private final UnitReadinessSnapshotRepository snapshots;

    @Value("${mew.synthetic.seed:20261117}") private long seed;
    @Value("${mew.synthetic.scenario:standard}") private String scenario;
    @Value("${mew.synthetic.history-days:180}") private int historyDays;
    private Random random;

    public SyntheticDataSeeder(InstallationRepository installations, CatalogSourceRepository catalogSources,
            CatalogAttributionRepository attributions,
            MilitaryUnitRepository units,
            NavalHullRepository hulls, MissionRoleRepository roles, AssetModelRepository models,
            PersonnelRepository personnel, MaterialCatalogRepository materials, AssetRepository assets,
            HullSystemAssignmentRepository assignments, InventoryBalanceRepository balances,
            InventoryTransactionRepository transactions, WorkOrderRepository workOrders,
            WorkOrderPartRepository workOrderParts, RequisitionRepository requisitions,
            SecurityAuditLogRepository audits, UnitReadinessSnapshotRepository snapshots) {
        this.installations = installations; this.catalogSources = catalogSources; this.attributions = attributions; this.units = units; this.hulls = hulls; this.roles = roles;
        this.models = models; this.personnel = personnel; this.materials = materials; this.assets = assets;
        this.assignments = assignments; this.balances = balances; this.transactions = transactions;
        this.workOrders = workOrders; this.workOrderParts = workOrderParts; this.requisitions = requisitions;
        this.audits = audits; this.snapshots = snapshots;
    }

    @Override @Transactional
    public void run(String... ignored) {
        if (installations.findByInstallationCode("NSWC-CRANE").isPresent()) return;
        random = new Random(seed);
        int scale = "demo".equalsIgnoreCase(scenario) ? 1 : "stress".equalsIgnoreCase(scenario) ? 8 : 3;
        List<Installation> baseList = createBases(scale);
        List<MilitaryUnit> unitList = createUnits(baseList, scale);
        List<Personnel> people = createPersonnel(unitList, scale);
        List<MissionRole> roleList = createRoles();
        List<AssetModel> modelList = createModels();
        List<NavalHull> hullList = createHulls(baseList, unitList, scale);
        List<Asset> assetList = createLoadouts(hullList, modelList, roleList);
        List<MaterialCatalog> materialList = createMaterials(modelList, scale);
        createSupplyAndMaintenance(baseList, people, assetList, materialList);
        createReadinessHistory(unitList, assetList, people);
    }

    private List<Installation> createBases(int scale) {
        // Real installations, chosen for geographically coherent platform, aviation, expeditionary,
        // RDT&E, or Army watercraft/EW support relationships. Operational event data remains fictional.
        String[][] catalog = {
                {"NSWC-CRANE", "Naval Surface Warfare Center, Crane Division", "Crane", "IN", "NORTHCOM", "R&D_Warfare_Center", "USN", "38.8912", "-86.8378"},
                {"NS-NORFOLK", "Naval Station Norfolk", "Norfolk", "VA", "NORTHCOM", "Fleet_Homeport", "USN", "36.9460", "-76.3305"},
                {"NB-SAN-DIEGO", "Naval Base San Diego", "San Diego", "CA", "NORTHCOM", "Fleet_Homeport", "USN", "32.6767", "-117.1200"},
                {"NAS-WHIDBEY", "Naval Air Station Whidbey Island", "Oak Harbor", "WA", "NORTHCOM", "Naval_Aviation", "USN", "48.3518", "-122.6566"},
                {"NSB-NEW-LONDON", "Naval Submarine Base New London", "Groton", "CT", "NORTHCOM", "Submarine_Homeport", "USN", "41.3612", "-72.0900"},
                {"CFA-YOKOSUKA", "Commander Fleet Activities Yokosuka", "Yokosuka", "Japan", "INDOPACOM", "Fleet_Homeport", "USN", "35.2836", "139.6678"},
                {"MCB-CAMP-PENDLETON", "Marine Corps Base Camp Pendleton", "Oceanside", "CA", "NORTHCOM", "Expeditionary_EW", "USMC", "33.3040", "-117.3500"},
                {"MCB-CAMP-LEJEUNE", "Marine Corps Base Camp Lejeune", "Jacksonville", "NC", "NORTHCOM", "Expeditionary_EW", "USMC", "34.5530", "-77.3380"},
                {"MCAS-CHERRY-POINT", "Marine Corps Air Station Cherry Point", "Cherry Point", "NC", "NORTHCOM", "Marine_Aviation", "USMC", "34.9009", "-76.8807"},
                {"MCAS-YUMA", "Marine Corps Air Station Yuma", "Yuma", "AZ", "NORTHCOM", "Marine_Aviation", "USMC", "32.6566", "-114.6060"},
                {"OFFUTT-AFB", "Offutt Air Force Base", "Bellevue", "NE", "NORTHCOM", "Airborne_ISR", "USAF", "41.1183", "-95.9125"},
                {"DAVIS-MONTHAN-AFB", "Davis-Monthan Air Force Base", "Tucson", "AZ", "NORTHCOM", "Airborne_EW", "USAF", "32.1665", "-110.8830"},
                {"EGLIN-AFB", "Eglin Air Force Base", "Valparaiso", "FL", "NORTHCOM", "EW_Test_and_Evaluation", "USAF", "30.4832", "-86.5254"},
                {"JBLM", "Joint Base Lewis-McChord", "Tacoma", "WA", "NORTHCOM", "Army_MDTF_and_Watercraft", "USA", "47.1079", "-122.5897"},
                {"FORT-SHAFTER", "Fort Shafter", "Honolulu", "HI", "INDOPACOM", "Army_MDTF_and_EW", "USA", "21.3492", "-157.8844"},
                {"JBLE", "Joint Base Langley-Eustis", "Hampton", "VA", "NORTHCOM", "Army_Watercraft", "USA", "37.0834", "-76.3605"}
        };
        List<Installation> result = new ArrayList<>();
        for (int i = 0; i < catalog.length; i++) {
            String[] b = catalog[i]; Installation x = new Installation();
            x.setInstallationId(String.format("INST-%03d", i + 1)); x.setInstallationCode(b[0]);
            x.setName(b[1]); x.setDodaac(String.format("D%05d", i + 100)); x.setCity(b[2]);
            x.setStateOrCountry(b[3]); x.setRegion(b[4]); x.setMissionCategory(b[5]); x.setServiceBranch(b[6]);
            x.setLatitude(new BigDecimal(b[7])); x.setLongitude(new BigDecimal(b[8]));
            SourceDefinition source = sourceForBranch(b[6]);
            x.setPrimaryEwFocus("Maritime electromagnetic warfare equipment and systems support");
            x.setCatalogSourceId(source.id()); x.setCatalogSourceUrl(source.url());
            x.addCatalogSource(source.entity());
            x.setCatalogVerifiedOn(LocalDate.of(2026, 11, 17));
            x.setPublicAssociationType(associationFor(b[5])); x.setCatalogConfidence("PUBLIC_ATTRIBUTED");
            x.setSyntheticOperationalData(true);
            x.setRemarks("Public MEW/EW catalog basis: " + source.title()
                    + ". Location and mission association are public-source references; readiness and logistics values are demonstration data.");
            x.setDsnPrefix(String.format("%03d", 400 + i)); x.setMaxEwBillets(80 + random.nextInt(220));
            x.setScifAccredited(true); x.setRfAnechoicChamber("NSWC-CRANE".equals(b[0]) || "EGLIN-AFB".equals(b[0])); x.setReadinessRating("C" + (i == 1 ? "2" : "1"));
            x.setLastInspectionDate(LocalDate.now().minusDays(20 + random.nextInt(300)));
            Installation saved = installations.save(x);
            attributeInstallation(saved, source.entity(), "Public installation identity and mission association from the reviewed source manifest.");
            result.add(saved);
        }
        return result;
    }

    private SourceDefinition sourceForBranch(String branch) {
        SourceDefinition definition = switch (branch) {
            case "USN" -> new SourceDefinition("USN-PUBLIC-INSTALLATIONS", "Navy installation and NAVSEA public references",
                    "https://cnrma.cnic.navy.mil/Installations/NAVSTA-Norfolk/About/Installation-Guide/");
            case "USMC" -> new SourceDefinition("USMC-PUBLIC-INSTALLATIONS", "Marine Corps installation and aviation public references",
                    "https://www.marines.mil/Units/Installations/");
            case "USAF" -> new SourceDefinition("USAF-PUBLIC-EW", "Air Force installation and electromagnetic warfare public references",
                    "https://www.dm.af.mil/Media/News/Article/4255968/the-ea-37b-at-dm/");
            case "USA" -> new SourceDefinition("USA-PUBLIC-INSTALLATIONS", "Army installation and multidomain public references",
                    "https://home.army.mil/lewis-mcchord/index.php/about");
            default -> new SourceDefinition("DOD-MIRTA-2025", "DoD MIRTA public installation reference",
                    "https://www.acq.osd.mil/eie/imr/rpid/disdi/index.html");
        };
        CatalogSource source = catalogSources.findBySourceId(definition.id()).orElseGet(CatalogSource::new);
        source.setSourceId(definition.id());
        source.setTitle(definition.title());
        source.setSourceUrl(definition.url());
        source.setSourceVersion("2026 public reference");
        source.setRetrievedOn(LocalDate.of(2026, 11, 17));
        source.setReviewStatus("VERIFIED");
        source.setLastReviewedOn(LocalDate.of(2026, 11, 17));
        source.setReviewer("Synthetic dataset maintainer");
        source.setLicenseNotes("Public reference used for catalog attribution; operational records remain synthetic.");
        return new SourceDefinition(definition.id(), definition.title(), definition.url(), catalogSources.save(source));
    }

    private String associationFor(String missionCategory) {
        return switch (missionCategory) {
            case "R&D_Warfare_Center" -> "SUSTAINMENT_OR_TEST_SUPPORT";
            case "Fleet_Homeport", "Submarine_Homeport" -> "PLATFORM_HOMEPORT";
            case "Naval_Aviation", "Marine_Aviation", "Airborne_EW" -> "AVIATION_EW_SUPPORT";
            case "Expeditionary_EW", "Army_MDTF_and_EW" -> "EW_MISSION_SUPPORT";
            default -> "INSTALLATION_REFERENCE";
        };
    }

    private record SourceDefinition(String id, String title, String url, CatalogSource entity) {
        private SourceDefinition(String id, String title, String url) { this(id, title, url, null); }
    }

    private void attributeInstallation(Installation installation, CatalogSource source, String rationale) {
        CatalogAttribution attribution = standardAttribution(source, rationale);
        attribution.setInstallation(installation);
        attributions.save(attribution);
    }

    private void attributeHull(NavalHull hull, CatalogSource source, String rationale) {
        CatalogAttribution attribution = standardAttribution(source, rationale);
        attribution.setNavalHull(hull);
        attributions.save(attribution);
    }

    private void attributeLoadout(HullSystemAssignment loadout, CatalogSource source, String rationale) {
        CatalogAttribution attribution = standardAttribution(source, rationale);
        attribution.setHullSystemAssignment(loadout);
        attributions.save(attribution);
    }

    private CatalogAttribution standardAttribution(CatalogSource source, String rationale) {
        CatalogAttribution attribution = new CatalogAttribution();
        attribution.setCatalogSource(source);
        attribution.setFactCategory(CatalogFactCategory.PUBLICLY_ATTRIBUTED_FACT);
        attribution.setConfidence("PUBLIC_ATTRIBUTED");
        attribution.setRationale(rationale);
        attribution.setReviewer("Synthetic dataset maintainer");
        attribution.setReviewedOn(LocalDate.of(2026, 11, 17));
        return attribution;
    }

    private List<MilitaryUnit> createUnits(List<Installation> baseList, int scale) {
        List<MilitaryUnit> result = new ArrayList<>();
        MilitaryUnit tycom = unit("Fleet Maritime EW Readiness Command", "N00060", baseList.get(0), null, CommandEchelon.TYCOM_FLEET, "Type Commander");
        result.add(units.save(tycom));
        for (int i = 0; i < baseList.size(); i++) {
            MilitaryUnit depot = unit(baseList.get(i).getName() + " MEW Support Unit", String.format("M%05d", 100 + i), baseList.get(i), tycom,
                    "R&D_Warfare_Center".equals(baseList.get(i).getMissionCategory()) ? CommandEchelon.WARFARE_CENTER : CommandEchelon.SHORE_DEPOT, "MEW support");
            result.add(units.save(depot));
        }
        for (int i = 0; i < baseList.size(); i++) {
            Installation base = baseList.get(i % baseList.size());
            result.add(units.save(unit("Maritime EW Unit " + (i + 1), String.format("M%05d", 200 + i), base, tycom,
                    CommandEchelon.COMBATANT_UNIT, "Guided Missile Destroyer")));
        }
        return result;
    }

    private MilitaryUnit unit(String name, String uic, Installation base, MilitaryUnit parent, CommandEchelon level, String type) {
        MilitaryUnit x = new MilitaryUnit(); x.setName(name); x.setUic(uic); x.setInstallation(base); x.setParentUnit(parent);
        x.setEchelonLevel(level); x.setCommandType(type); x.setPersonnelReadinessPct(decimal(86 + random.nextInt(14)));
        x.setEquipmentReadinessPct(decimal(82 + random.nextInt(18))); return x;
    }

    private List<Personnel> createPersonnel(List<MilitaryUnit> unitList, int scale) {
        List<Personnel> result = new ArrayList<>();
        // Fictional, deterministic identity data for a more representative demo roster.  These
        // names intentionally do not represent real personnel records or DoD identity data.
        String[] first = {"Avery", "Cameron", "Dana", "Elliot", "Harper", "Jordan", "Kai", "Logan",
                "Mason", "Noel", "Parker", "Quinn", "Reese", "Sawyer", "Taylor", "Val", "Adrian", "Bailey",
                "Charlie", "Devin", "Emerson", "Finley", "Gray", "Hayden", "Isaac", "Jules", "Kendall", "Lane",
                "Micah", "Nico", "Oakley", "Peyton", "Remy", "Shawn", "Tobin", "Wren", "Alden", "Blair",
                "Cory", "Drew", "Ellis", "Frankie", "Gale", "Hollis", "Indigo", "Jamie", "Kieran", "Lee"};
        String[] last = {"Adams", "Bennett", "Caldwell", "Dalton", "Ellison", "Fletcher", "Garcia", "Holloway",
                "Ingram", "Jamison", "Kim", "Lawson", "Mendoza", "Nguyen", "Ortiz", "Patel", "Quincy", "Ramirez",
                "Santos", "Turner", "Usher", "Vasquez", "Walker", "Xu", "Young", "Zimmerman", "Anderson", "Brooks",
                "Chen", "Diaz", "Edwards", "Foster", "Green", "Hughes", "Ibrahim", "Jefferson", "Kaur", "Lewis",
                "Mitchell", "Nelson", "Owens", "Price", "Robinson", "Singh", "Thomas", "Underwood", "Williams", "Yates"};
        // The active synthetic profile resolves to this persisted assignment.  The value is
        // deliberately a synthetic subject; production Keycloak subjects are provisioned and
        // mapped by the identity-management workflow, never guessed by a seed fixture.
        MilitaryUnit tycom = unitList.get(0);
        Personnel marcus = new Personnel(); marcus.setPersonnelId("MEW-PER-0001");
        marcus.setIdentitySubject("demo:marcus-vance"); marcus.setDodId("8999999001");
        marcus.setFirstName("Marcus"); marcus.setLastName("Vance"); marcus.setMiddleInitial("R");
        marcus.setPersonnelCategory("Active_Duty_Officer"); marcus.setPaygrade("O-5"); marcus.setRank("CDR");
        marcus.setSpecialtyCode("Synthetic-MEW-Leadership");
        marcus.setTitle("TYCOM Director of Maritime Electromagnetic Maneuver Warfare");
        marcus.setFunctionalRole(FunctionalRole.FLEET_READINESS_ANALYST); marcus.setAssignedUic(tycom.getUic());
        marcus.setUnit(tycom); marcus.setInstallation(tycom.getInstallation()); marcus.setSecurityClearance("TS/SCI");
        marcus.setServiceBranch("USN"); marcus.setCommandTier("OPERATIONAL_CMD");
        marcus.setAuthorizedInstallations("[]"); marcus.setAuthorizedCageCodes("[]");
        marcus.setAuthorizationPermissions("VIEW_GLOBAL_READINESS,VIEW_BRANCH_READINESS,VIEW_INSTALLATION_READINESS,APPROVE_READINESS_REPORT,VIEW_INVENTORY,REQUEST_PARTS_REQUISITION,APPROVE_PARTS_REQUISITION,MANAGE_BOM_CONFIG,LOG_MAINTENANCE_ACTION,CERTIFY_EQUIPMENT_UPTIME,ACCESS_RDTE_SPECS");
        marcus.setOperationalStatus("Active"); result.add(personnel.save(marcus));
        int count = scale * 45;
        for (int i = 0; i < count; i++) {
            MilitaryUnit unit = unitList.get(1 + (i % (unitList.size() - 1))); FunctionalRole role = roleFor(unit, i);
            Personnel p = new Personnel(); p.setPersonnelId(String.format("MEW-PER-%04d", i + 100));
            p.setIdentitySubject("demo:" + seed + ":" + p.getPersonnelId()); p.setDodId(String.format("8%09d", i + 1));
            p.setFirstName(first[i % first.length]);
            p.setLastName(last[(i * 17 + 11) % last.length]);
            p.setMiddleInitial(String.valueOf((char) ('A' + ((i * 7) % 26))));
            p.setPersonnelCategory(role == FunctionalRole.WARFARE_CENTER_ENGINEER ? "DoD_Civilian" : "Active_Duty_Enlisted");
            p.setPaygrade(role == FunctionalRole.WARFARE_CENTER_ENGINEER ? "GS-12" : "E-6"); p.setRank(role == FunctionalRole.WARFARE_CENTER_ENGINEER ? "CIV" : "CTT1");
            p.setSpecialtyCode(role == FunctionalRole.SHIPBOARD_TECHNICIAN ? "MEW-Tech" : "MEW-Support");
            p.setTitle(role.name().replace('_', ' ')); p.setFunctionalRole(role); p.setAssignedUic(unit.getUic()); p.setUnit(unit); p.setInstallation(unit.getInstallation());
            p.setSecurityClearance(i % 7 == 0 ? "TS/SCI" : "Secret"); p.setServiceBranch(unit.getInstallation().getServiceBranch());
            p.setCommandTier(role == FunctionalRole.FLEET_READINESS_ANALYST ? "OPERATIONAL_CMD" : "INSTALLATION_UNIT");
            p.setAuthorizedInstallations(unit.getInstallation().getInstallationCode()); p.setAuthorizedCageCodes("03538,06481,96214");
            p.setAuthorizationPermissions(role == FunctionalRole.SHIPBOARD_TECHNICIAN ? "VIEW_INVENTORY,LOG_MAINTENANCE_ACTION" : "VIEW_INVENTORY,REQUEST_PARTS_REQUISITION");
            p.setOperationalStatus(i % 11 == 0 ? "Temporarily_Unavailable" : "In_Garrison"); result.add(personnel.save(p));
        }
        return result;
    }

    private FunctionalRole roleFor(MilitaryUnit unit, int index) {
        if (unit.getEchelonLevel() == CommandEchelon.COMBATANT_UNIT) return index % 6 == 0 ? FunctionalRole.UNIT_SUPPO : FunctionalRole.SHIPBOARD_TECHNICIAN;
        return index % 3 == 0 ? FunctionalRole.WARFARE_CENTER_ENGINEER : index % 3 == 1 ? FunctionalRole.SHORE_DEPOT_MANAGER : FunctionalRole.FLEET_READINESS_ANALYST;
    }

    private List<MissionRole> createRoles() {
        String[][] source = {{"ASMD", "Anti-Ship Missile Defense"}, {"EA", "Electronic Attack"}, {"ES", "Electronic Support"}, {"SIGINT", "Signals Intelligence"}, {"MDEW", "Maritime Domain Electromagnetic Warfare"}};
        List<MissionRole> result = new ArrayList<>();
        for (int i = 0; i < source.length; i++) { MissionRole r = new MissionRole(); r.setRoleId("ROLE-" + (i + 1)); r.setRoleCode(source[i][0]); r.setRoleName(source[i][1]); r.setDescription("MEW readiness reporting role"); r.setCompatibleDomains("[Surface,Airborne,Subsurface,Ground]"); result.add(roles.save(r)); }
        return result;
    }

    private List<AssetModel> createModels() {
        String[][] source = {
                {"AN/SLQ-32(V)6", "SEWIP Block 2", "Electronic Support (ES)", "03538"},
                {"AN/SLQ-32(V)7", "SEWIP Block 3", "Electronic Attack (EA)", "03538"},
                {"AN/BLQ-10", "Submarine Electronic Warfare System", "Electronic Support (ES)", "96214"},
                {"AN/ALQ-249", "Next Generation Jammer", "Electronic Attack (EA)", "06481"},
                {"AN/ALQ-231(V)3", "Intrepid Tiger II", "Electronic Attack (EA)", "26916"},
                {"AN/ASQ-239", "F-35 Electronic Warfare Suite", "Electronic Support (ES)", "81755"},
                {"AN/ALQ-184", "Electronic Countermeasures Pod", "Electronic Attack (EA)", "96214"},
                {"TLS-BCT", "Terrestrial Layer System - Brigade Combat Team", "Electronic Attack (EA)", "03538"},
                {"AN/ULQ-35", "Electronic Warfare System", "Electronic Support (ES)", "80063"}
        };
        List<AssetModel> result = new ArrayList<>();
        for (int i = 0; i < source.length; i++) { AssetModel m = new AssetModel(); m.setSystemId("SYS-" + (i + 1)); m.setNomenclature(source[i][0]); m.setCommonName(source[i][1]); m.setEwCategory(source[i][2]); m.setAssetClass(source[i][2]); m.setCompatibleDomains("[Surface,Airborne,Subsurface,Ground]"); m.setManufacturer("Representative OEM"); m.setCageCode(source[i][3]); m.setNationalStockNumber(String.format("5999-98-%04d", 1000 + i)); m.setLin("MEW" + (100 + i)); result.add(models.save(m)); }
        return result;
    }

    private List<NavalHull> createHulls(List<Installation> baseList, List<MilitaryUnit> unitList, int scale) {
        String[][] platformCatalog = {
                {"DDG-114", "USS Ralph Johnson", "Arleigh Burke-class Flight IIA", "Surface", "CFA-YOKOSUKA", "USN"},
                {"CVN-78", "USS Gerald R. Ford", "Gerald R. Ford-class Aircraft Carrier", "Surface", "NS-NORFOLK", "USN"},
                {"SSN-774", "USS Virginia", "Virginia-class Submarine", "Subsurface", "NSB-NEW-LONDON", "USN"},
                {"BuNo-168934", "EA-18G Growler", "EA-18G Growler", "Airborne", "NAS-WHIDBEY", "USN"},
                {"LHA-6", "USS America", "America-class Amphibious Assault Ship", "Surface", "NB-SAN-DIEGO", "USN"},
                {"BuNo-169540", "F-35B Lightning II", "F-35B Lightning II", "Airborne", "MCAS-YUMA", "USMC"},
                {"USMC-654321", "JLTV MEW Vehicle", "Joint Light Tactical Vehicle", "Ground", "MCB-CAMP-LEJEUNE", "USMC"},
                {"AF-64-14841", "RC-135V/W Rivet Joint", "RC-135V/W Rivet Joint", "Airborne", "OFFUTT-AFB", "USAF"},
                {"AF-19-0042", "EA-37B Compass Call", "EA-37B Compass Call", "Airborne", "DAVIS-MONTHAN-AFB", "USAF"},
                {"AF-91-0368", "F-16CM Fighting Falcon", "F-16CM Fighting Falcon", "Airborne", "EGLIN-AFB", "USAF"},
                {"LSV-4", "General Frank S. Besson Jr.", "General Frank S. Besson-class LSV", "Surface", "JBLM", "USA"},
                {"LCU-2015", "Runnymede-class Landing Craft", "Runnymede-class LCU", "Surface", "JBLE", "USA"},
                {"USA-W89412", "MDTF EW Heavy Truck", "Multi-Domain Task Force EW Platform", "Ground", "FORT-SHAFTER", "USA"}
        };
        Map<String, Installation> baseByCode = new HashMap<>(); baseList.forEach(base -> baseByCode.put(base.getInstallationCode(), base));
        List<MilitaryUnit> combatants = unitList.stream().filter(u -> u.getEchelonLevel() == CommandEchelon.COMBATANT_UNIT).toList();
        List<NavalHull> result = new ArrayList<>();
        for (int i = 0; i < platformCatalog.length; i++) { String[] p = platformCatalog[i]; Installation home = baseByCode.get(p[4]); MilitaryUnit unit = combatants.stream().filter(u -> u.getInstallation().getId().equals(home.getId())).findFirst().orElseThrow(); NavalHull h = new NavalHull(); h.setHullId(String.format("HULL-%03d", i + 1)); h.setHullNumber(p[0]); h.setVesselName(p[1]); h.setShipClass(p[2]); h.setDomain(p[3]); h.setHomeport(home.getLocationDisplay()); h.setHomeportInstallation(home); h.setUnit(unit); h.setEmconPosture(i % 7 == 0 ? EmconPosture.BRAVO : EmconPosture.CHARLIE); SourceDefinition source = sourceForBranch(p[5]); h.addCatalogSource(source.entity()); h.setRemarks("Publicly attributable platform identity and class. The generated readiness, system serial, and maintenance history are demonstration data, not live status."); NavalHull saved = hulls.save(h); attributeHull(saved, source.entity(), "Public platform identity and class from the reviewed source manifest."); result.add(saved); }
        return result;
    }

    private List<Asset> createLoadouts(List<NavalHull> hullList, List<AssetModel> modelList, List<MissionRole> roleList) {
        String[] systems = {"AN/SLQ-32(V)7", "AN/SLQ-32(V)6", "AN/BLQ-10", "AN/ALQ-249", "AN/SLQ-32(V)6", "AN/ASQ-239", "AN/ALQ-231(V)3", "AN/ALQ-184", "AN/ALQ-184", "AN/ALQ-184", "TLS-BCT", "AN/ULQ-35", "TLS-BCT"};
        Map<String, AssetModel> modelByNomenclature = new HashMap<>(); modelList.forEach(model -> modelByNomenclature.put(model.getNomenclature(), model));
        List<Asset> result = new ArrayList<>(); int seq = 1;
        for (int platformIndex = 0; platformIndex < hullList.size(); platformIndex++) {
            NavalHull hull = hullList.get(platformIndex); AssetModel model = modelByNomenclature.get(systems[platformIndex]); MissionRole role = roleList.get(platformIndex % roleList.size());
            Asset a = new Asset(); a.setAssignmentId(String.format("ASGN-%04d", seq)); a.setAssetModel(model); a.setSerialNumber(String.format("SIM-%s-%04d", model.getSystemId(), seq)); a.setAssignedUnit(hull.getUnit()); a.setCurrentLocation(hull.getHomeportInstallation()); a.setHull(hull); a.setMissionRole(role); a.setQuantityInstalled(1); a.setInstallationDate(LocalDate.now().minusDays(180 + random.nextInt(900))); a.setEngineHours(decimal(1000 + random.nextInt(9000))); a.setMtbfHours(1800.0 + random.nextInt(1800)); a.setMttrHours(2.0 + random.nextDouble() * 10); ReadinessLevel status = readinessFor(seq); a.setReadinessStatus(status); a.setOperationalStatus(status == ReadinessLevel.FMC ? "FOC (Full Operational Capability)" : "Restricted operational capability"); a.setOperationalAvailabilityPct(status == ReadinessLevel.FMC ? 97.0 : status == ReadinessLevel.PMC ? 82.0 : 55.0); a.setDomain(hull.getDomain()); a.setServiceBranch(hull.getHomeportInstallation().getServiceBranch()); a.setLastStatusChange(Instant.now().minus(random.nextInt(80), ChronoUnit.DAYS)); a = assets.save(a); result.add(a);
            HullSystemAssignment x = new HullSystemAssignment(); x.setAssignmentId(a.getAssignmentId()); x.setHull(hull); x.setSystem(model); x.setMissionRole(role); x.setAsset(a); x.setHullNumber(hull.getHullNumber()); x.setVesselName(hull.getVesselName()); x.setShipClass(hull.getShipClass()); x.setDomain(hull.getDomain()); x.setHomeport(hull.getHomeport()); x.setNomenclature(model.getNomenclature()); x.setCommonName(model.getCommonName()); x.setEwCategory(model.getEwCategory()); x.setQuantityInstalled(1); x.setRoleCode(role.getRoleCode()); x.setRoleName(role.getRoleName()); x.setOperationalStatus(a.getOperationalStatus()); x.setInstallationDate(a.getInstallationDate()); SourceDefinition source = sourceForBranch(hull.getHomeportInstallation().getServiceBranch()); x.addCatalogSource(source.entity()); HullSystemAssignment saved = assignments.save(x); attributeLoadout(saved, source.entity(), "Public platform-system catalog association recorded for provenance; generated asset status remains synthetic."); seq++;
        }
        return result;
    }

    // Keep the small demo profile useful as well: every five assets include one visible degradation case.
    private ReadinessLevel readinessFor(int sequence) { int n = sequence % 10; return n < 7 ? ReadinessLevel.FMC : n < 9 ? ReadinessLevel.PMC : n == 9 ? ReadinessLevel.NMCM : ReadinessLevel.NMCS; }

    private List<MaterialCatalog> createMaterials(List<AssetModel> modelList, int scale) {
        List<MaterialCatalog> result = new ArrayList<>(); int count = scale * 20;
        for (int i = 0; i < count; i++) { MaterialCatalog m = new MaterialCatalog(); m.setComponentId(String.format("CMP-%04d", i + 1)); m.setNsn(String.format("5999-98-%04d", 2000 + i)); m.setNomenclature(i % 5 == 0 ? "Receiver Assembly" : "Replaceable Module " + (i + 1)); m.setPartNumber("PN-" + (1000 + i)); m.setCageCode(new String[]{"03538", "06481", "96214"}[i % 3]); m.setManufacturer("Representative OEM"); m.setComponentCategory(i % 5 == 0 ? "Receiver LRU" : "Module"); m.setRepairabilityCode(i % 4 == 0 ? "Consumable" : "Depot_Repairable_LRU"); m.setUnitPrice(decimal(250 + random.nextInt(80000))); m.setCriticalItem(i % 6 == 0); m.setLeadTimeDays(7 + random.nextInt(m.isCriticalItem() ? 100 : 35)); m.setExpectedMtbfHours(1000.0 + random.nextInt(5000)); m.getCompatibleSystems().add(modelList.get(i % modelList.size())); if (i > 0 && i % 5 != 0) m.setParentLru(result.get(i - (i % 5))); result.add(materials.save(m)); }
        return result;
    }

    private void createSupplyAndMaintenance(List<Installation> baseList, List<Personnel> people, List<Asset> assetList, List<MaterialCatalog> materialList) {
        int seq = 1;
        for (Installation base : baseList) for (MaterialCatalog material : materialList) {
            int finalBalance = material.isCriticalItem() && seq % 5 == 0 ? 0 : 2 + random.nextInt(18); InventoryBalance b = new InventoryBalance(); b.setInstallation(base); b.setMaterial(material); b.setQuantityOnHand(finalBalance); b.setSafetyStock(material.isCriticalItem() ? 3 : 1); b.setReorderPoint(material.isCriticalItem() ? 5 : 3); b = balances.save(b);
            int receipt = finalBalance + 1 + random.nextInt(8); int deduction = receipt - finalBalance;
            ledger(b, TransactionType.RCT, receipt, 0, receipt, "RCT-" + seq, null, null, people.get(seq % people.size()), 120 - (seq % 60));
            ledger(b, TransactionType.DED, deduction, receipt, finalBalance, "DED-" + seq, null, null, people.get(seq % people.size()), 60 - (seq % 30)); seq++;
        }
        int workSeq = 1;
        for (Asset asset : assetList) if (asset.getReadinessStatus() != ReadinessLevel.FMC) {
            Personnel tech = people.stream().filter(p -> p.getUnit().getId().equals(asset.getAssignedUnit().getId()) && p.getFunctionalRole() == FunctionalRole.SHIPBOARD_TECHNICIAN).findFirst().orElse(people.get(0)); Personnel qa = people.stream().filter(p -> p.getFunctionalRole() == FunctionalRole.FLEET_READINESS_ANALYST).findFirst().orElse(people.get(0)); MaterialCatalog part = materialList.get(workSeq % materialList.size());
            WorkOrder wo = new WorkOrder(); wo.setJcn(String.format("%s-CE02-%04d", asset.getAssignedUnit().getUic(), workSeq)); wo.setAsset(asset); wo.setHull(asset.getHull()); wo.setInstallation(asset.getCurrentLocation()); wo.setTechnician(tech); wo.setQaInspector(qa); wo.setPriorityCode(asset.getReadinessStatus() == ReadinessLevel.NMCS ? 1 : 2); wo.setStatus(asset.getReadinessStatus() == ReadinessLevel.NMCS ? MafStatus.AWAITING_PARTS : MafStatus.IN_WORK); wo.setCasrepNumber(asset.getReadinessStatus() == ReadinessLevel.NMCS ? "CASREP-" + workSeq : ""); wo.setDescriptionOfFault("Diagnostic fault requiring replacement of a compatible module."); wo.setMaintenanceHours(decimal(1 + random.nextInt(12))); wo.setCreatedAt(Instant.now().minus(2 + workSeq, ChronoUnit.DAYS)); wo = workOrders.save(wo);
            WorkOrderPart wp = new WorkOrderPart(); wp.setWorkOrder(wo); wp.setMaterial(part); wp.setQuantity(1); wp.setRequiresDlrTurnIn("Depot_Repairable_LRU".equals(part.getRepairabilityCode())); if (wp.isRequiresDlrTurnIn()) wp.setDefectiveSerialNumber("DEF-" + workSeq); workOrderParts.save(wp);
            Requisition r = new Requisition(); r.setDocumentNumber(String.format("REQ%011d", workSeq)); r.setRequestingUnit(asset.getAssignedUnit()); r.setMaterial(part); r.setQuantity(1); r.setPriority(wo.getPriorityCode()); r.setTiedToAsset(asset); r.setWorkOrder(wo); r.setStatus(asset.getReadinessStatus() == ReadinessLevel.NMCS ? "Submitted" : "In Transit"); r.setEstimatedDeliveryDate(LocalDate.now().plusDays(part.getLeadTimeDays())); r = requisitions.save(r);
            audit(tech, "MAINTENANCE_ASSIGNMENT", "ALLOW", "/api/v1/work-orders/" + wo.getJcn(), "Maintenance event linked to asset readiness."); audit(tech, "PARTS_REQUISITION", "ALLOW", "/api/v1/requisitions/" + r.getDocumentNumber(), "Demand created from maintenance action."); if (asset.getReadinessStatus() == ReadinessLevel.NMCS) audit(qa, "SUPPLY_DEPENDENCY", "ALLOW", "/api/v1/work-orders/" + wo.getJcn(), "Critical part unavailable; asset remains supply limited."); workSeq++;
        }
        // Authorization outcomes are events in the same synthetic story, not unrelated random rows.
        Personnel analyst = people.stream().filter(p -> p.getFunctionalRole() == FunctionalRole.FLEET_READINESS_ANALYST).findFirst().orElse(people.get(0));
        Personnel technician = people.stream().filter(p -> p.getFunctionalRole() == FunctionalRole.SHIPBOARD_TECHNICIAN).findFirst().orElse(people.get(0));
        audit(analyst, "CROSS_UNIT_QUERY", "ALLOW", "/api/v1/dashboard/units/", "Fleet readiness aggregation within assigned command scope.");
        audit(technician, "BULK_CSV_EXPORT", "DENY", "/api/v1/dashboard/units/export_csv/", "Technician account lacks cross-unit export permission.");
        audit(technician, "OPSEC_MASK_APPLIED", "MASKED", "/api/v1/assets/", "Asset view masked location fields outside technician scope.");
    }

    private void ledger(InventoryBalance b, TransactionType type, int quantity, int previous, int next, String ref, Requisition req, WorkOrder wo, Personnel actor, int daysAgo) { InventoryTransaction t = new InventoryTransaction(); t.setBalance(b); t.setTransactionType(type); t.setQuantity(quantity); t.setPreviousBalance(previous); t.setNewBalance(next); t.setReferenceDocument(ref); t.setRelatedRequisition(req); t.setRelatedWorkOrder(wo); t.setPerformedBy(actor); t.setCreatedAt(Instant.now().minus(Math.max(daysAgo, 1), ChronoUnit.DAYS)); transactions.save(t); }

    private void audit(Personnel actor, String event, String decision, String target, String reason) { SecurityAuditLog a = new SecurityAuditLog(); a.setActor(actor); a.setActorName(actor.getRank() + " " + actor.getFirstName() + " " + actor.getLastName()); a.setActorRole(actor.getFunctionalRole().name()); a.setActorUic(actor.getAssignedUic()); a.setActorClearance(actor.getSecurityClearance()); a.setActorCageScope(actor.getAuthorizedCageCodes()); a.setEventType(event); a.setDecision(decision); a.setResourceTarget(target); a.setReason(reason); a.setCreatedAt(Instant.now().minus(random.nextInt(30), ChronoUnit.DAYS)); audits.save(a); }

    private void createReadinessHistory(List<MilitaryUnit> unitList, List<Asset> assetList, List<Personnel> people) { LocalDate today = LocalDate.now(); for (MilitaryUnit unit : unitList) { List<Asset> unitAssets = assetList.stream().filter(a -> a.getAssignedUnit().getId().equals(unit.getId())).toList(); int total = unitAssets.size(); int fmc = (int) unitAssets.stream().filter(a -> a.getReadinessStatus() == ReadinessLevel.FMC).count(); int pmc = (int) unitAssets.stream().filter(a -> a.getReadinessStatus() == ReadinessLevel.PMC).count(); int peopleCount = (int) people.stream().filter(p -> p.getUnit().getId().equals(unit.getId())).count(); for (int d = 0; d < historyDays; d++) { UnitReadinessSnapshot s = new UnitReadinessSnapshot(); s.setUnit(unit); s.setSnapshotDate(today.minusDays(d)); s.setTotalAssets(total); int historicalNmc = Math.max(0, total - fmc - pmc + (d % 17 == 0 ? 1 : 0)); int historicalPmc = Math.max(0, Math.min(total - historicalNmc, pmc)); s.setNmcAssets(historicalNmc); s.setPmcAssets(historicalPmc); s.setFmcAssets(Math.max(0, total - historicalNmc - historicalPmc)); s.setTotalPersonnel(peopleCount); s.setDeployablePersonnel(Math.max(0, peopleCount - (d % 23 == 0 ? 1 : 0))); snapshots.save(s); } } }

    private BigDecimal decimal(double value) { return BigDecimal.valueOf(value).setScale(2, java.math.RoundingMode.HALF_UP); }
}
