from django.core.management.base import BaseCommand
from django.contrib.auth.models import User

from mew_logistics.models import (
    Asset,
    AssetModel,
    AssetStatusLog,
    HullSystemAssignment,
    Installation,
    InventoryBalance,
    InventoryTransaction,
    MaterialCatalog,
    MilitaryUnit,
    MissionRole,
    NavalHull,
    Personnel,
    Requisition,
    UnitReadinessSnapshot,
    WorkOrder,
    WorkOrderPart,
)
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

        self.stdout.write(
            self.style.SUCCESS(
                f"Successfully seeded full MEW dataset ({len(inst_map)} Installations, "
                f"{len(personnel_roster)} Personnel, {len(assignments)} Deployed MEW Systems, "
                f"{stats['components']} Component Types, {stats['work_orders']} Work Orders, "
                f"{stats['requisitions']} Requisitions, and {stats['transactions']} Ledger Transactions)!"
            )
        )
