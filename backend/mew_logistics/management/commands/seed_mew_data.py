import random
import uuid
from datetime import timedelta
from django.utils import timezone
from django.core.management.base import BaseCommand
from django.contrib.auth.models import User

from mew_logistics.models import (
    Installation, MilitaryUnit, Personnel, AssetModel, Asset, 
    MaterialCatalog, InventoryBalance, Requisition, WorkOrder, 
    WorkOrderPart, InventoryTransaction, AssetStatusLog, 
    ReadinessLevel, TransactionType
)
from mew_logistics.tasks import generate_daily_readiness_snapshots

class Command(BaseCommand):
    help = 'Generates extensive, realistic synthetic data utilizing new Ledger Signals'

    def handle(self, *args, **kwargs):
        self.stdout.write("Clearing old data...")
        
        # New teardown order (must delete ledgers and bridge tables first)
        InventoryTransaction.objects.all().delete()
        WorkOrderPart.objects.all().delete()
        WorkOrder.objects.all().delete()
        Requisition.objects.all().delete()
        AssetStatusLog.objects.all().delete()
        InventoryBalance.objects.all().delete()
        MaterialCatalog.objects.all().delete()
        Asset.objects.all().delete()
        AssetModel.objects.all().delete()
        Personnel.objects.all().delete()
        MilitaryUnit.objects.all().delete()
        Installation.objects.all().delete()
        User.objects.exclude(is_superuser=True).delete()

        now = timezone.now()

        # ==========================================
        # 1. CORE DATA (Installations, Units, Personnel, Catalogs)
        # ==========================================
        self.stdout.write("Building Core Data...")
        inst1 = Installation.objects.create(name="Fort Liberty", dodaac="W11AAA", region="CONUS-EAST")
        unit1 = MilitaryUnit.objects.create(name="82nd Airborne Division", uic="W82AA1", installation=inst1, command_type="FORSCOM")
        
        mechanics = [
            Personnel.objects.create(
                user=User.objects.create_user(username=f"mech_{i}", last_name=f"Tech_{i}"),
                unit=unit1, rank="SGT", mos="91B"
            ) for i in range(5)
        ]

        model_tank = AssetModel.objects.create(nomenclature="M1A2 Abrams", national_stock_number="2350-01-328-5964", asset_class="Ground Combat")
        
        mat1 = MaterialCatalog.objects.create(nsn="2530-01-123-4567", nomenclature="Track Pad Assembly", unit_of_issue="EA", unit_price=150.00)
        mat2 = MaterialCatalog.objects.create(nsn="2840-01-987-6543", nomenclature="Turbine Engine Module", unit_of_issue="EA", unit_price=250000.00)

        # ==========================================
        # 2. INITIAL WAREHOUSE STOCK (Prevents negative balance crashes)
        # ==========================================
        self.stdout.write("Initializing warehouse balances...")
        bal1 = InventoryBalance.objects.create(installation=inst1, material=mat1, quantity_on_hand=50, reorder_point=20, safety_stock=5)
        bal2 = InventoryBalance.objects.create(installation=inst1, material=mat2, quantity_on_hand=5, reorder_point=2, safety_stock=1)

        # ==========================================
        # 3. ASSETS & ACTIVE LOGISTICS
        # ==========================================
        self.stdout.write("Creating Assets, Work Orders, and Requisitions...")
        
        for i in range(20):
            asset = Asset.objects.create(
                asset_model=model_tank,
                serial_number=f"SN-ABRAMS-{random.randint(1000, 9999)}",
                assigned_unit=unit1,
                current_location=inst1,
                engine_hours=random.uniform(100, 2000)
            )

            # Generate a mix of Open and Closed Work Orders
            if random.choice([True, False]):
                wo = WorkOrder.objects.create(
                    asset=asset, technician=random.choice(mechanics),
                    description_of_fault="Routine maintenance and track pad replacement",
                    is_grounding_fault=False, status='Open'
                )
                
                # Attach parts to the Work Order using the new bridge table
                WorkOrderPart.objects.create(work_order=wo, material=mat1, quantity=random.randint(1, 4))
                
                # If we simulate closing it, our new Signals will automatically 
                # deduct the stock and write the Immutable Ledger entry!
                if random.choice([True, False]):
                    wo.status = 'Closed'
                    wo.save(update_fields=['status']) 
                    # ↳ SIGNAL FIRES HERE: Deducts mat1 from bal1 and creates InventoryTransaction

            # Generate incoming Supply Requisitions
            if random.choice([True, False]):
                req = Requisition.objects.create(
                    document_number=f"REQ-{uuid.uuid4().hex[:6].upper()}",
                    requesting_unit=unit1, material=mat2, quantity=1,
                    priority=Requisition.UrgencyPriority.ROUTINE, status='Submitted'
                )

                # If we simulate receiving it, we inject the mechanic profile to test the API injection pattern
                if random.choice([True, False]):
                    req._received_by = random.choice(mechanics) # Inject memory attribute
                    req.status = 'Received'
                    req.save(update_fields=['status']) 
                    # ↳ SIGNAL FIRES HERE: Adds mat2 to bal2 and creates InventoryTransaction

        # ==========================================
        # 4. RUN BACKGROUND CELERY TASKS
        # ==========================================
        self.stdout.write("Running Celery metrics calculations...")
        from mew_logistics.tasks import calculate_fleet_reliability_metrics, generate_daily_readiness_snapshots
        calculate_fleet_reliability_metrics()
        generate_daily_readiness_snapshots()

        self.stdout.write(self.style.SUCCESS('Successfully seeded data using new Signal automations!'))