import random
from django.db import models
from django.utils.translation import gettext_lazy as _
from .base import BaseModel, ReadinessLevel
from .organization import Installation, MilitaryUnit, NavalHull
from .personnel import Personnel
from .assets import Asset, MaterialCatalog


class WorkOrder(BaseModel):
    """
    Current Ship's Maintenance Project (CSMP) / Maintenance Action Form (MAF - OPNAV 4790/2K).
    Captures all 5 DoD MEW maintenance domains defined in WORKORDER_DESIGN_CONSIDERATIONS.md:
      1. Work Order & Tracking Identifiers (JCN, MAF Status, Priority, CASREP link)
      2. Configuration & Component Metadata (EIC, APL/AEL, Hull/Installation FK)
      3. Failure & Diagnostic Codes (When Discovered, Equipment Status, Malfunction Code, Narrative)
      4. Supply & Requisition Linkage (via WorkOrderPart & Requisition)
      5. Labor, QA, & Security Sign-Off (Work Center, Man-Hours, Tag-Out/ESD/CWP, COMSEC/MDF Zeroization)
    """

    class MAFStatus(models.TextChoices):
        OPEN = "OPEN", _("OPEN - Initiated")
        DEFERRED = "DEFERRED", _("DEFERRED - Deferred Action")
        AWAITING_PARTS = "AWAITING_PARTS", _("AWP - Awaiting Parts")
        IN_WORK = "IN_WORK", _("IN_WORK - Active Maintenance")
        QA_PENDING = "QA_PENDING", _("QA_PENDING - QA & Safety Sign-Off")
        CLOSED = "CLOSED", _("CLOSED - Completed & Verified")

    class TypeAvailabilityPriority(models.IntegerChoices):
        MANDATORY_CASREP = 1, _("1 - Mandatory / CASREP")
        ESSENTIAL = 2, _("2 - Essential")
        ROUTINE = 3, _("3 - Routine")
        DESIRABLE = 4, _("4 - Desirable")

    class WhenDiscoveredCode(models.TextChoices):
        BEFORE_OPERATION = "A", _("A - Before Operation / Pre-Fire")
        PREVENTIVE_MAINT = "B", _("B - During Preventive Maintenance (PMS)")
        OPERATIONAL_TEST = "C", _("C - During Operational / Range Testing")
        NORMAL_OPERATION = "D", _("D - During Normal Tactical Operation")
        BIT_DIAGNOSTIC = "F", _("F - Built-In-Test (BIT) Automated Fault")

    class EquipmentOperabilityCode(models.TextChoices):
        OPERATIONAL = "1", _("1 - Operational (Full Capability)")
        REDUCED_CAPABILITY = "2", _("2 - Reduced Capability (PMC)")
        NON_OPERATIONAL = "3", _("3 - Non-Operational (NMCM / NMCS)")

    class MalfunctionCode(models.TextChoices):
        BURNED_OUT = "020", _("020 - Burned Out / Thermal Trip")
        CALIBRATION_DRIFT = "127", _("127 - Calibration / Phase Drift")
        LOW_GAIN_OUTPUT = "160", _("160 - Low RF Gain / Power Output")
        DIGITAL_MEMORY_FAULT = "290", _("290 - Digital / FPGA Memory Parity Fault")
        WAVEGUIDE_ARCING = "374", _("374 - Waveguide Arcing / High VSWR")
        COOLANT_SEAL_LEAK = "520", _("520 - Coolant Loop / Radome Seal Leak")

    # =========================================================================
    # 1. WORK ORDER & TRACKING IDENTIFIERS (CSMP / MAF)
    # =========================================================================
    jcn = models.CharField(
        max_length=20,
        unique=True,
        null=True,
        blank=True,
        db_index=True,
        help_text="11-char Job Control Number: 5-char UIC + 2-char Work Center + 4-digit Serial (e.g., N20001-CE02-0001)",
    )
    status = models.CharField(
        max_length=50,
        choices=MAFStatus.choices,
        default=MAFStatus.OPEN,
    )
    priority_code = models.IntegerField(
        choices=TypeAvailabilityPriority.choices,
        default=TypeAvailabilityPriority.ESSENTIAL,
    )
    casrep_number = models.CharField(
        max_length=30,
        blank=True,
        default="",
        help_text="Casualty Report identifier if failure degrades primary EW mission area (e.g., CASREP-26-004)",
    )
    is_grounding_fault = models.BooleanField(
        default=False,
        help_text="True if component failure deadlines the MEW system or degrades primary warfare mission area",
    )

    # =========================================================================
    # 2. CONFIGURATION & COMPONENT METADATA
    # =========================================================================
    asset = models.ForeignKey(Asset, on_delete=models.CASCADE, related_name='work_orders')
    hull = models.ForeignKey(
        NavalHull,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='work_orders',
    )
    installation = models.ForeignKey(
        Installation,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='work_orders',
    )
    eic = models.CharField(
        max_length=10,
        blank=True,
        default="D720100",
        help_text="7-character Equipment Identification Code mapping parent EW system & subsystem",
    )
    apl_ael = models.CharField(
        max_length=15,
        blank=True,
        default="881040291A",
        help_text="11-character Allowance Parts List / Allowance Equipage List index",
    )

    # =========================================================================
    # 3. FAILURE & DIAGNOSTIC CODES (OPNAV 4790 / 3-M)
    # =========================================================================
    when_discovered_code = models.CharField(
        max_length=2,
        choices=WhenDiscoveredCode.choices,
        default=WhenDiscoveredCode.NORMAL_OPERATION,
    )
    equipment_status_code = models.CharField(
        max_length=2,
        choices=EquipmentOperabilityCode.choices,
        default=EquipmentOperabilityCode.REDUCED_CAPABILITY,
    )
    malfunction_code = models.CharField(
        max_length=5,
        choices=MalfunctionCode.choices,
        default=MalfunctionCode.LOW_GAIN_OUTPUT,
    )
    description_of_fault = models.TextField(
        help_text="Narrative / Discrepancy text detailing BIT fault codes, RF power loss readings, or cooling leaks"
    )

    # =========================================================================
    # 5. LABOR, QA, & SECURITY SIGN-OFF
    # =========================================================================
    work_center = models.CharField(
        max_length=20,
        default="CE02",
        help_text="Assigned shop (e.g., CE02 - Electronic Warfare Div, AT01 - Airborne EA Shop, GX04 - Crane RF Depot)",
    )
    technician = models.ForeignKey(
        Personnel,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='assigned_work',
    )
    qa_inspector = models.ForeignKey(
        Personnel,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='qa_inspected_work',
    )
    org_man_hours = models.DecimalField(
        max_digits=6,
        decimal_places=1,
        default=0.0,
        help_text="Organizational-level (shipboard) labor hours expended",
    )
    depot_man_hours = models.DecimalField(
        max_digits=6,
        decimal_places=1,
        default=0.0,
        help_text="Intermediate / Depot-level (NSWC Crane / RMC) labor hours expended",
    )

    # Safety & QA Checklists
    tag_out_verified = models.BooleanField(
        default=True,
        help_text="Electrical / High-Power RF Radiation Isolation Tag-Out verified",
    )
    esd_precautions_verified = models.BooleanField(
        default=True,
        help_text="Electrostatic Discharge (ESD) handling precautions verified",
    )
    cwp_controlled_work_package = models.BooleanField(
        default=False,
        help_text="Controlled Work Package (CWP) required for sub-safe or high-power array alteration",
    )

    # Classification / COMSEC / MDF Handling
    requires_mdf_zeroization = models.BooleanField(
        default=False,
        help_text="True if defective LRU contains classified Mission Data Files (MDF) or volatile COMSEC keys",
    )
    mdf_zeroization_verified = models.BooleanField(
        default=False,
        help_text="Confirms cryptographic / threat library zeroization prior to depot turn-in shipment",
    )

    date_opened = models.DateTimeField(auto_now_add=True)
    date_closed = models.DateTimeField(null=True, blank=True)

    class Meta:
        ordering = ["-date_opened"]
        indexes = [
            models.Index(fields=["status", "priority_code"]),
            models.Index(fields=["jcn"]),
            models.Index(fields=["work_center", "status"]),
        ]

    def save(self, *args, **kwargs):
        # Normalize legacy status strings to CSMP MAFStatus codes
        status_map = {
            "Open": self.MAFStatus.OPEN,
            "Deferred": self.MAFStatus.DEFERRED,
            "Awaiting Parts": self.MAFStatus.AWAITING_PARTS,
            "In Progress": self.MAFStatus.IN_WORK,
            "QA Pending": self.MAFStatus.QA_PENDING,
            "Closed": self.MAFStatus.CLOSED,
        }
        if self.status in status_map:
            self.status = status_map[self.status]

        # Auto-populate hull and installation from the parent asset
        if self.asset_id and self.asset:
            if not self.hull_id and self.asset.hull_id:
                self.hull = self.asset.hull
            if not self.installation_id and self.asset.current_location_id:
                self.installation = self.asset.current_location

        # Align priority_code, equipment_status_code, and is_grounding_fault
        if self.is_grounding_fault:
            self.equipment_status_code = self.EquipmentOperabilityCode.NON_OPERATIONAL
            if self.priority_code > 2:
                self.priority_code = self.TypeAvailabilityPriority.MANDATORY_CASREP
        elif self.priority_code == self.TypeAvailabilityPriority.MANDATORY_CASREP:
            self.is_grounding_fault = True
            self.equipment_status_code = self.EquipmentOperabilityCode.NON_OPERATIONAL

        # Auto-generate CSMP Job Control Number (JCN) if not set
        if not self.jcn:
            uic_part = "N00164"
            if self.asset_id and self.asset and self.asset.assigned_unit_id:
                uic_part = (self.asset.assigned_unit.uic or "N00164")[:5].upper()
            wc_part = (self.work_center or "CE02")[:4].upper()
            serial_part = f"{random.randint(1, 9999):04d}"
            candidate = f"{uic_part}-{wc_part}-{serial_part}"
            while WorkOrder.objects.filter(jcn=candidate).exclude(pk=self.pk).exists():
                candidate = f"{uic_part}-{wc_part}-{random.randint(1, 9999):04d}"
            self.jcn = candidate

        super().save(*args, **kwargs)

    def __str__(self):
        return f"{self.jcn or f'WO-{str(self.id)[:8].upper()}'} ({self.asset.serial_number}) - {self.status}"


class Requisition(BaseModel):
    class UrgencyPriority(models.IntegerChoices):
        ROUTINE = 3, _('Routine (IPD 13)')
        PRIORITY = 2, _('Priority (IPD 03)')
        URGENT = 1, _('Urgent / CASREP (IPD 01)')

    class SupplyStatusCode(models.TextChoices):
        BA_IN_PROCESS = "BA", _("BA - Item Being Processed for Release")
        BB_BACKORDERED = "BB", _("BB - Item Backordered Against Due-In")
        BV_SHIPPED = "BV", _("BV - Item Procured & on Contract / Shipped")
        AS_DELAYED = "AS", _("AS - Shipping Delayed")
        D7_RECEIVED = "D7", _("D7 - Shipment Received at Destination")

    document_number = models.CharField(
        max_length=25,
        unique=True,
        help_text="14-character MILSTRIP Requisition Tracker (UIC + Julian Date + Serial)",
    )
    requesting_unit = models.ForeignKey(MilitaryUnit, on_delete=models.CASCADE, related_name='requisitions')
    material = models.ForeignKey(MaterialCatalog, on_delete=models.PROTECT, related_name='requisitions')
    quantity = models.PositiveIntegerField()
    priority = models.IntegerField(choices=UrgencyPriority.choices, default=UrgencyPriority.ROUTINE)
    ipd_code = models.CharField(
        max_length=2,
        default="13",
        help_text="MILSTRIP Issue Priority Designator (01 through 15)",
    )
    supply_status_code = models.CharField(
        max_length=4,
        choices=SupplyStatusCode.choices,
        default=SupplyStatusCode.BA_IN_PROCESS,
    )
    requires_dlr_turn_in = models.BooleanField(
        default=True,
        help_text="Flags whether failed EW component is a Depot Level Repairable (DLR) requiring carcass turn-in",
    )
    tied_to_asset = models.ForeignKey(Asset, on_delete=models.SET_NULL, null=True, blank=True)
    work_order = models.ForeignKey(
        WorkOrder,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='requisitions',
    )
    status = models.CharField(max_length=50, default='Submitted')
    estimated_delivery_date = models.DateField(null=True, blank=True)

    def save(self, *args, **kwargs):
        if self.priority == self.UrgencyPriority.URGENT:
            self.ipd_code = "01"
        elif self.priority == self.UrgencyPriority.PRIORITY and self.ipd_code == "13":
            self.ipd_code = "03"

        if self.status == "Received":
            self.supply_status_code = self.SupplyStatusCode.D7_RECEIVED
        elif self.status == "In Transit" and self.supply_status_code == self.SupplyStatusCode.BA_IN_PROCESS:
            self.supply_status_code = self.SupplyStatusCode.BV_SHIPPED

        if self.material_id and self.material:
            self.requires_dlr_turn_in = (
                self.material.repairability_code == "Depot_Repairable_LRU"
            )
        super().save(*args, **kwargs)

    def __str__(self):
        return f"REQ {self.document_number} - {self.material.nsn}"


class InventoryBalance(BaseModel):
    installation = models.ForeignKey(Installation, on_delete=models.CASCADE, related_name='inventory')
    material = models.ForeignKey(MaterialCatalog, on_delete=models.CASCADE, related_name='balances')
    quantity_on_hand = models.PositiveIntegerField(default=0)
    reorder_point = models.PositiveIntegerField(default=5)
    safety_stock = models.PositiveIntegerField(default=2)

    class Meta:
        unique_together = ('installation', 'material')

    def needs_replenishment(self):
        return self.quantity_on_hand <= self.reorder_point


class WorkOrderPart(BaseModel):
    """
    Links a CSMP WorkOrder to its required MaterialCatalog LRU(s), tracking
    removed (defective) vs. installed (replacement) serial numbers and DLR carcass turn-in.
    """
    work_order = models.ForeignKey(WorkOrder, on_delete=models.CASCADE, related_name='parts_used')
    material = models.ForeignKey(MaterialCatalog, on_delete=models.PROTECT)
    quantity = models.PositiveIntegerField(default=1)
    defective_serial_number = models.CharField(
        max_length=60,
        blank=True,
        default="",
        help_text="Serial number of the failed LRU removed from the system",
    )
    replacement_serial_number = models.CharField(
        max_length=60,
        blank=True,
        default="",
        help_text="Serial number of the replacement LRU installed",
    )
    requires_dlr_turn_in = models.BooleanField(
        default=True,
        help_text="True if failed carcass must be returned to NSWC Crane / Depot",
    )
    dlr_turn_in_status = models.CharField(
        max_length=40,
        default="Pending_Turn_In",
        help_text="Pending_Turn_In, Zeroized_Awaiting_Shipment, Shipped_To_Depot, or Not_Required",
    )

    def save(self, *args, **kwargs):
        if not self.defective_serial_number and self.material_id:
            self.defective_serial_number = f"SN-OLD-{self.material.component_id or 'LRU'}-{random.randint(1000, 9999)}"
        if not self.replacement_serial_number and self.material_id:
            self.replacement_serial_number = f"SN-NEW-{self.material.component_id or 'LRU'}-{random.randint(1000, 9999)}"
        if self.material_id and self.material:
            self.requires_dlr_turn_in = (
                self.material.repairability_code == "Depot_Repairable_LRU"
            )
            if not self.requires_dlr_turn_in:
                self.dlr_turn_in_status = "Not_Required"
        super().save(*args, **kwargs)

    def __str__(self):
        return f"{self.quantity}x {self.material.nomenclature} for {self.work_order.jcn or self.work_order.id}"
