from django.db import models
from django.utils.translation import gettext_lazy as _
from .base import BaseModel, CapabilityStatus, OperationalDomain, ReadinessLevel
from .organization import Installation, MilitaryUnit, NavalHull


class MissionRole(BaseModel):
    """
    Reference Catalog: Tactical Mission Roles for Maritime Electromagnetic Warfare (MEW).
    """
    role_id = models.CharField(
        max_length=20,
        unique=True,
        db_index=True,
        help_text="Reference Catalog Role Identifier (e.g., ROLE-01)",
    )
    role_code = models.CharField(
        max_length=30,
        unique=True,
        db_index=True,
        help_text="Tactical Mission Role Code (e.g., ASMD-EW, ISRT-SIGINT)",
    )
    role_name = models.CharField(
        max_length=150,
        help_text="Tactical Mission Role Name (e.g., Anti-Ship Missile Defense)",
    )
    description = models.TextField(blank=True, default="")
    compatible_domains = models.JSONField(
        default=list,
        blank=True,
        help_text="Operational domains compatible with this mission role (Surface, Subsurface, Airborne)",
    )

    class Meta:
        verbose_name = "Tactical Mission Role"
        verbose_name_plural = "Tactical Mission Roles"
        ordering = ["role_id"]

    def __str__(self):
        return f"{self.role_code} - {self.role_name}"


class AssetModel(BaseModel):
    """
    Reference Catalog: Maritime Electromagnetic Warfare (MEW) Systems & Equipment Models.
    Correlates with MEW_SYSTEMS while maintaining compatibility with unit reliability aggregations.
    """
    system_id = models.CharField(
        max_length=20,
        unique=True,
        null=True,
        blank=True,
        db_index=True,
        help_text="MEW System Catalog Identifier (e.g., SYS-101)",
    )
    nomenclature = models.CharField(
        max_length=255,
        help_text="Official AN/ military designation (e.g., AN/SLQ-32(V)6)",
    )
    common_name = models.CharField(
        max_length=255,
        blank=True,
        default="",
        help_text="Program or common system name (e.g., SEWIP Block 2)",
    )
    ew_category = models.CharField(
        max_length=100,
        blank=True,
        default="",
        help_text="Electromagnetic Warfare category (e.g., Electronic Support (ES))",
    )
    asset_class = models.CharField(
        max_length=100,
        blank=True,
        default="",
        help_text="Asset classification used by fleet reliability aggregations",
    )
    compatible_domains = models.JSONField(
        default=list,
        blank=True,
        help_text="Compatible operational domains (e.g., ['Surface', 'Subsurface', 'Airborne'])",
    )
    manufacturer = models.CharField(
        max_length=150,
        blank=True,
        default="",
        help_text="Prime defense contractor / OEM (e.g., Lockheed Martin, Northrop Grumman)",
    )
    cage_code = models.CharField(
        max_length=10,
        blank=True,
        default="",
        db_index=True,
        help_text="5-character Commercial and Government Entity (CAGE) Code for OEM program isolation",
    )
    national_stock_number = models.CharField(
        max_length=16,
        unique=True,
        null=True,
        blank=True,
    )
    lin = models.CharField(
        max_length=10,
        help_text="Line Item Number",
        null=True,
        blank=True,
    )

    class Meta:
        indexes = [
            models.Index(fields=["asset_class"]),
            models.Index(fields=["ew_category"]),
            models.Index(fields=["nomenclature"]),
        ]

    def save(self, *args, **kwargs):
        # Keep ew_category and asset_class synchronized for dashboard filtering
        if self.ew_category and not self.asset_class:
            self.asset_class = self.ew_category
        elif self.asset_class and not self.ew_category:
            self.ew_category = self.asset_class

        if not self.cage_code and self.manufacturer:
            oem_cage_map = {
                "Lockheed Martin": "03538",
                "Northrop Grumman": "76823",
                "BAE Systems": "89954",
                "Raytheon": "07933",
                "L3Harris": "55820",
                "NSWC Dahlgren": "00178",
                "Naval Research Lab": "81995",
            }
            self.cage_code = oem_cage_map.get(self.manufacturer, "00164")
        super().save(*args, **kwargs)

    def __str__(self):
        if self.common_name:
            return f"{self.nomenclature} ({self.common_name})"
        return self.nomenclature


class MewSystem(AssetModel):
    """
    Proxy model alias for AssetModel representing Maritime Electromagnetic Warfare Systems.
    """
    class Meta:
        proxy = True
        verbose_name = "MEW System"
        verbose_name_plural = "MEW Systems"


class MaterialCatalog(BaseModel):
    """
    Reference Catalog: MEW Line Replaceable Units (LRUs), Shop Replaceable Units (SRUs),
    and spare components ordered, stocked, and installed by installation/fleet maintenance staff.
    """
    component_id = models.CharField(
        max_length=20,
        unique=True,
        null=True,
        blank=True,
        db_index=True,
        help_text="MEW Component / LRU Catalog Identifier (e.g., CMP-201)",
    )
    nsn = models.CharField(max_length=16, unique=True, verbose_name="National Stock Number")
    nomenclature = models.CharField(max_length=255)
    part_number = models.CharField(
        max_length=60,
        blank=True,
        default="",
        help_text="OEM / Manufacturer Part Number",
    )
    cage_code = models.CharField(
        max_length=10,
        blank=True,
        default="",
        help_text="5-character Commercial and Government Entity (CAGE) Code",
    )
    manufacturer = models.CharField(
        max_length=150,
        blank=True,
        default="",
        help_text="Component OEM / Defense Contractor",
    )
    component_category = models.CharField(
        max_length=100,
        blank=True,
        default="",
        help_text="Subsystem classification (e.g., RF Receiver/Exciter LRU, GaN T/R Module)",
    )
    repairability_code = models.CharField(
        max_length=50,
        blank=True,
        default="Depot_Repairable_LRU",
        help_text="Maintenance level (e.g., Depot_Repairable_LRU, Organizational_LRU, Expendable_Round)",
    )
    unit_of_issue = models.CharField(max_length=4, default="EA")
    unit_price = models.DecimalField(max_digits=12, decimal_places=2)
    critical_item = models.BooleanField(default=False)
    lead_time_days = models.PositiveIntegerField(
        default=14,
        help_text="Standard supply/depot replenishment lead time in days",
    )
    expected_mtbf_hours = models.FloatField(
        default=2500.0,
        help_text="Component engineering MTBF estimate in hours",
    )
    parent_lru = models.ForeignKey(
        'self',
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='sub_assemblies',
        help_text="Parent LRU in the Hierarchical Bill of Materials (BOM)",
    )
    compatible_systems = models.ManyToManyField(
        AssetModel,
        related_name="compatible_components",
        blank=True,
        help_text="MEW Systems that utilize this component/LRU",
    )
    compatible_system_ids = models.JSONField(
        default=list,
        blank=True,
        help_text="List of compatible MEW System IDs (e.g., ['SYS-101', 'SYS-102'])",
    )

    class Meta:
        indexes = [
            models.Index(fields=["component_category", "critical_item"]),
            models.Index(fields=["nsn"]),
        ]

    def __str__(self):
        return f"{self.nsn} - {self.nomenclature}"


class MewComponent(MaterialCatalog):
    """
    Proxy model alias for MaterialCatalog representing MEW LRUs & Spare Components.
    """
    class Meta:
        proxy = True
        verbose_name = "MEW Component / LRU"
        verbose_name_plural = "MEW Components / LRUs"


class Asset(BaseModel):
    """
    Represents a deployed MEW System instance installed aboard a Naval Hull / Military Unit.
    """
    assignment_id = models.CharField(
        max_length=20,
        unique=True,
        null=True,
        blank=True,
        db_index=True,
        help_text="Hull-to-System Loadout Assignment ID (e.g., ASGN-0001)",
    )
    asset_model = models.ForeignKey(AssetModel, on_delete=models.PROTECT, related_name='instances')
    serial_number = models.CharField(max_length=100, unique=True)
    assigned_unit = models.ForeignKey(MilitaryUnit, on_delete=models.CASCADE, related_name='assets')
    current_location = models.ForeignKey(Installation, on_delete=models.SET_NULL, null=True, related_name='housed_assets')

    # MEW Hull & Tactical Mission Loadout Correlation
    hull = models.ForeignKey(
        NavalHull,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="installed_assets",
    )
    mission_role = models.ForeignKey(
        MissionRole,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="assigned_assets",
    )
    quantity_installed = models.PositiveIntegerField(default=1)
    operational_status = models.CharField(
        max_length=60,
        choices=CapabilityStatus.choices,
        default=CapabilityStatus.FOC,
    )
    installation_date = models.DateField(null=True, blank=True)

    readiness_status = models.CharField(
        max_length=4,
        choices=ReadinessLevel.choices,
        default=ReadinessLevel.FULLY_MISSION_CAPABLE,
    )
    last_status_change = models.DateTimeField(auto_now_add=True)
    engine_hours = models.DecimalField(max_digits=10, decimal_places=2, default=0.0)

    mtbf_hours = models.FloatField(default=0.0, help_text="Mean Time Between Failures")
    mttr_hours = models.FloatField(default=0.0, help_text="Mean Time To Repair")
    operational_availability_pct = models.FloatField(default=100.0, help_text="Operational Availability (Ao)")

    class Meta:
        indexes = [
            models.Index(fields=['readiness_status', 'assigned_unit']),
            models.Index(fields=['operational_availability_pct']),
            models.Index(fields=['readiness_status', 'operational_availability_pct']),
        ]

    def __str__(self):
        hull_str = f" [{self.hull.hull_number}]" if self.hull else ""
        return f"{self.asset_model.nomenclature}{hull_str} - SN: {self.serial_number}"

    @property
    def mtbf(self):
        return self.mtbf_hours

    @property
    def mttr(self):
        return self.mttr_hours

    @property
    def ao(self):
        return self.operational_availability_pct


class HullSystemAssignment(BaseModel):
    """
    Normalized & flattened relational model representing a validated Hull-to-MEW-System Loadout Assignment.
    Links NavalHull, AssetModel (MewSystem), MissionRole, and the tracked Asset instance.
    """
    assignment_id = models.CharField(
        max_length=20,
        unique=True,
        db_index=True,
        help_text="Assignment Identifier (e.g., ASGN-0001)",
    )
    hull = models.ForeignKey(
        NavalHull,
        on_delete=models.CASCADE,
        related_name="system_assignments",
    )
    system = models.ForeignKey(
        AssetModel,
        on_delete=models.CASCADE,
        related_name="hull_assignments",
    )
    mission_role = models.ForeignKey(
        MissionRole,
        on_delete=models.PROTECT,
        related_name="hull_assignments",
    )
    asset = models.OneToOneField(
        Asset,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="loadout_assignment",
    )

    # Denormalized / flattened relational attributes matching mew_hull_system_loadouts.csv
    hull_number = models.CharField(max_length=20, blank=True, default="")
    vessel_name = models.CharField(max_length=150, blank=True, default="")
    ship_class = models.CharField(max_length=150, blank=True, default="")
    domain = models.CharField(
        max_length=50,
        choices=OperationalDomain.choices,
        default=OperationalDomain.SURFACE,
    )
    homeport = models.CharField(max_length=150, blank=True, default="")
    nomenclature = models.CharField(max_length=255, blank=True, default="")
    common_name = models.CharField(max_length=255, blank=True, default="")
    ew_category = models.CharField(max_length=100, blank=True, default="")
    quantity_installed = models.PositiveIntegerField(default=1)
    role_code = models.CharField(max_length=30, blank=True, default="")
    role_name = models.CharField(max_length=150, blank=True, default="")
    operational_status = models.CharField(
        max_length=60,
        choices=CapabilityStatus.choices,
        default=CapabilityStatus.FOC,
    )
    installation_date = models.DateField(null=True, blank=True)

    class Meta:
        verbose_name = "Hull System Loadout Assignment"
        verbose_name_plural = "Hull System Loadout Assignments"
        ordering = ["assignment_id"]
        indexes = [
            models.Index(fields=["domain", "operational_status"]),
            models.Index(fields=["hull_number", "nomenclature"]),
        ]

    def save(self, *args, **kwargs):
        if self.hull:
            if not self.hull_number:
                self.hull_number = self.hull.hull_number
            if not self.vessel_name:
                self.vessel_name = self.hull.vessel_name
            if not self.ship_class:
                self.ship_class = self.hull.ship_class
            if not self.domain:
                self.domain = self.hull.domain
            if not self.homeport:
                self.homeport = self.hull.homeport
        if self.system:
            if not self.nomenclature:
                self.nomenclature = self.system.nomenclature
            if not self.common_name:
                self.common_name = self.system.common_name
            if not self.ew_category:
                self.ew_category = self.system.ew_category or self.system.asset_class
        if self.mission_role:
            if not self.role_code:
                self.role_code = self.mission_role.role_code
            if not self.role_name:
                self.role_name = self.mission_role.role_name
        super().save(*args, **kwargs)

    def __str__(self):
        return f"{self.assignment_id}: {self.vessel_name} ({self.hull_number}) -> {self.nomenclature} [{self.role_code}]"
