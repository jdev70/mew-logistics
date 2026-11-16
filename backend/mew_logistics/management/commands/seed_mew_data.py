from datetime import date
from django.core.management.base import BaseCommand
from django.contrib.auth.models import User

from mew_logistics.models import (
    Asset,
    AssetModel,
    AssetStatusLog,
    CommandEchelon,
    EmconPosture,
    HullSystemAssignment,
    Installation,
    InventoryBalance,
    InventoryTransaction,
    MaterialCatalog,
    MilitaryUnit,
    MissionRole,
    NavalHull,
    OperationalStatus,
    Personnel,
    Requisition,
    SecurityAuditLog,
    UnitReadinessSnapshot,
    WorkOrder,
    WorkOrderPart,
)
from mew_logistics.rbac import CURATED_RBAC_PERSONAS
from mew_logistics.management.commands.generate_mew_installations import (
    generate_installations_dataset,
    sync_installations_to_db,
)
from mew_logistics.management.commands.generate_mew_personnel import (
    generate_mew_personnel,
    sync_personnel_to_db,
)
from mew_logistics.management.commands.generate_mew_loadouts import (
    generate_assignments,
    sync_loadouts_to_db,
)
from mew_logistics.management.commands.generate_mew_components import (
    sync_components_and_simulate_logistics,
)


def _configure_hierarchical_tenancy_and_personas():
    """
    Establishes the hierarchical MilitaryUnit command tree (TYCOM -> Hulls/Detachments),
    assigns realistic EMCON postures to Naval Hulls, and binds the 6 curated RBAC+ABAC
    personas (`MEW-PER-0001` .. `MEW-PER-0006`) to their target Hulls/Installations.
    """
    navifor_inst = Installation.objects.filter(installation_code="NAVIFOR-SUF").first()
    tycom_unit = None
    if navifor_inst:
        tycom_unit = navifor_inst.units.first()
        if tycom_unit:
            tycom_unit.echelon_level = CommandEchelon.TYCOM_FLEET
            tycom_unit.name = "Naval Information Forces (NAVIFOR // TYCOM)"
            tycom_unit.save(update_fields=["echelon_level", "name"])

    # Assign echelon levels & parent_unit hierarchy across all MilitaryUnits
    for unit in MilitaryUnit.objects.select_related("installation").all():
        if tycom_unit and unit.id != tycom_unit.id and not unit.parent_unit_id:
            unit.parent_unit = tycom_unit
        if hasattr(unit, "naval_hull") and unit.naval_hull:
            unit.echelon_level = CommandEchelon.COMBATANT_UNIT
        elif unit.installation and unit.installation.mission_category == "R&D_Warfare_Center":
            unit.echelon_level = CommandEchelon.WARFARE_CENTER
        elif unit.id != (tycom_unit.id if tycom_unit else None):
            unit.echelon_level = CommandEchelon.SHORE_DEPOT
        unit.save(update_fields=["parent_unit", "echelon_level"])

    # Assign EMCON postures to Naval Hulls for ABAC geospatial masking
    for hull in NavalHull.objects.all():
        if hull.domain == "Subsurface":
            hull.emcon_posture = EmconPosture.ALPHA
        elif hull.hull_number in ("DDG-125", "VAQ-132"):
            hull.emcon_posture = EmconPosture.BRAVO
        else:
            hull.emcon_posture = EmconPosture.CHARLIE
        hull.save(update_fields=["emcon_posture"])

    # Upsert the 6 curated RBAC+ABAC personas (`MEW-PER-0001` .. `MEW-PER-0006`)
    for idx, p_cfg in enumerate(CURATED_RBAC_PERSONAS, start=1):
        target_inst = Installation.objects.filter(
            installation_code=p_cfg["target_installation_code"]
        ).first() or Installation.objects.first()

        target_unit = target_inst.units.first() if target_inst else None
        if p_cfg["functional_role"] == "FLEET_READINESS_ANALYST" and tycom_unit:
            target_unit = tycom_unit
        elif p_cfg.get("target_hull_number"):
            hull_obj = NavalHull.objects.filter(
                hull_number=p_cfg["target_hull_number"]
            ).select_related("unit", "homeport_installation").first()
            if hull_obj and hull_obj.unit:
                target_unit = hull_obj.unit
                if hull_obj.homeport_installation:
                    target_inst = hull_obj.homeport_installation

        Personnel.objects.update_or_create(
            personnel_id=p_cfg["personnel_id"],
            defaults={
                "dod_id": p_cfg["dod_id"],
                "first_name": p_cfg["first_name"],
                "middle_initial": p_cfg["middle_initial"],
                "last_name": p_cfg["last_name"],
                "personnel_category": p_cfg["personnel_category"],
                "paygrade": p_cfg["paygrade"],
                "rank": p_cfg["rank"],
                "rank_full": p_cfg["rank_full"],
                "specialty_code": p_cfg["specialty_code"],
                "title": p_cfg["title"],
                "role": p_cfg["role"],
                "functional_role": p_cfg["functional_role"],
                "security_clearance": p_cfg["security_clearance"],
                "unit": target_unit,
                "installation": target_inst,
                "assigned_uic": target_unit.uic if target_unit else "N00060",
                "authorized_cage_codes": p_cfg["authorized_cage_codes"],
                "operational_status": OperationalStatus.IN_GARRISON,
                "date_assigned": date(2024, 6, 15),
                "projected_rotation_date": (
                    "N/A (Permanent Billet)"
                    if p_cfg["personnel_category"] == "DoD_Civilian"
                    else "2027-06-15"
                ),
                "dsn_phone": f"312-{target_inst.dsn_prefix if target_inst else '482'}-000{idx}",
                "official_email": f"{p_cfg['first_name'].lower()}.{p_cfg['last_name'].lower()}.persona{idx}@us.navy.mil",
            },
        )

    # Seed baseline SecurityAuditLog entries demonstrating OPSEC & RBAC/ABAC governance
    SecurityAuditLog.objects.create(
        actor_name="CDR Marcus Vance",
        actor_role="FLEET_READINESS_ANALYST",
        actor_uic=tycom_unit.uic if tycom_unit else "N00060",
        actor_clearance="TS/SCI",
        event_type=SecurityAuditLog.EventType.CROSS_UNIT_QUERY,
        decision=SecurityAuditLog.PolicyDecision.ALLOW,
        resource_target="TYCOM Echelon Tree (All Subordinate Hulls)",
        reason="Hierarchical tree tenancy granted full AOR readiness visibility.",
    )
    SecurityAuditLog.objects.create(
        actor_name="CTR Nolan Mercer-OEM",
        actor_role="WARFARE_CENTER_ENGINEER",
        actor_uic="NSWC-CRANE",
        actor_clearance="Secret",
        actor_cage_scope="03538",
        event_type=SecurityAuditLog.EventType.OPSEC_MASK_APPLIED,
        decision=SecurityAuditLog.PolicyDecision.MASKED,
        resource_target="CAGE 03538 SEWIP Telemetry (/api/v1/assets/)",
        reason="Redacted warship hull numbers, vessel names, and live coordinates for OEM contractor.",
    )
    SecurityAuditLog.objects.create(
        actor_name="CTT1 Devon Mercer",
        actor_role="SHIPBOARD_TECHNICIAN",
        actor_uic="N20001",
        actor_clearance="TS/SCI",
        event_type=SecurityAuditLog.EventType.BULK_CSV_EXPORT,
        decision=SecurityAuditLog.PolicyDecision.DENY,
        resource_target="/api/v1/dashboard/units/export_csv/",
        reason="Mosaic-Effect OPSEC Guard: Shipboard Technician isolated to single hull (DDG-51) cannot export cross-fleet readiness CSV.",
    )


class Command(BaseCommand):
    help = "Seeds the complete Maritime Electromagnetic Warfare (MEW) Installations, Personnel, Hulls/Systems, Components, Inventory, and Analytics dataset."

    def add_arguments(self, parser):
        parser.add_argument(
            "--personnel-count",
            type=int,
            default=150,
            help="Number of synthetic MEW personnel to generate (default: 150)",
        )

    def handle(self, *args, **options):
        self.stdout.write("Clearing old data...")

        # Teardown order (delete ledgers and bridge tables first)
        SecurityAuditLog.objects.all().delete()
        InventoryTransaction.objects.all().delete()
        WorkOrderPart.objects.all().delete()
        Requisition.objects.all().delete()
        WorkOrder.objects.all().delete()
        AssetStatusLog.objects.all().delete()
        UnitReadinessSnapshot.objects.all().delete()
        InventoryBalance.objects.all().delete()
        HullSystemAssignment.objects.all().delete()
        MaterialCatalog.objects.all().delete()
        Asset.objects.all().delete()
        AssetModel.objects.all().delete()
        MissionRole.objects.all().delete()
        NavalHull.objects.all().delete()
        Personnel.objects.all().delete()
        MilitaryUnit.objects.all().delete()
        Installation.objects.all().delete()
        User.objects.exclude(is_superuser=True).delete()

        # 1. MEW Installations & Base Detachment Units
        self.stdout.write("1. Generating MEW Installations...")
        installations_data = generate_installations_dataset()
        inst_map = sync_installations_to_db(installations_data)

        # 2. MEW Personnel Roster
        self.stdout.write("2. Generating MEW Personnel Roster...")
        personnel_roster = generate_mew_personnel(
            installations_data, num_records=options["personnel_count"]
        )
        sync_personnel_to_db(personnel_roster, inst_map=inst_map)

        # 3. Naval Hulls, MEW Systems, Mission Roles & Hull-System Loadouts
        self.stdout.write("3. Generating Naval Hulls, MEW Systems & Loadouts...")
        assignments = generate_assignments()
        sync_loadouts_to_db(assignments)

        # 4. MEW Components (LRUs/SRUs), Warehouse Inventory, Requisitions, Work Orders & Analytics
        self.stdout.write(
            "4. Generating MEW Components, Warehouse Inventory, Requisitions, Work Orders & Analytics..."
        )
        stats = sync_components_and_simulate_logistics(clear_existing_logistics=False)

        # 5. Hierarchical Echelon Tree, EMCON Postures & Curated RBAC/ABAC Personas
        self.stdout.write(
            "5. Configuring Hierarchical Multi-Tenancy, EMCON Postures & RBAC/ABAC Personas..."
        )
        _configure_hierarchical_tenancy_and_personas()

        self.stdout.write(
            self.style.SUCCESS(
                f"Successfully seeded full MEW dataset ({len(inst_map)} Installations, "
                f"{len(personnel_roster)} Personnel, {len(assignments)} Deployed MEW Systems, "
                f"{stats['components']} Component Types, {stats['work_orders']} Work Orders, "
                f"{stats['requisitions']} Requisitions, and {stats['transactions']} Ledger Transactions)!"
            )
        )
