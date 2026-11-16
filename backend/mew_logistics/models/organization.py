from django.db import models
from django.utils.translation import gettext_lazy as _
from .base import BaseModel, OperationalDomain
from ..managers import MilitaryUnitManager  # Assuming managers.py is in the app root


class MissionCategory(models.TextChoices):
    RD_WARFARE_CENTER = "R&D_Warfare_Center", _("R&D Warfare Center")
    OPERATIONAL_COMMAND = "Operational_Command", _("Operational Command")
    CRYPTOLOGIC_OPERATIONS = "Cryptologic_Operations", _("Cryptologic Operations")
    TRAINING_SCHOOLHOUSE = "Training_Schoolhouse", _("Training Schoolhouse")
    TEST_EVALUATION_RANGE = "Test_Evaluation_Range", _("Test & Evaluation Range")
    FLEET_HOMEPORT = "Fleet_Homeport", _("Fleet Homeport")


class InstallationReadinessRating(models.TextChoices):
    C1 = "C1_Fully_Mission_Capable", _("C1 - Fully Mission Capable")
    C2 = "C2_Substantially_Mission_Capable", _("C2 - Substantially Mission Capable")
    C3 = "C3_Marginally_Mission_Capable", _("C3 - Marginally Mission Capable")
    C4 = "C4_Not_Mission_Capable", _("C4 - Not Mission Capable")


class CommandEchelon(models.TextChoices):
    TYCOM_FLEET = "TYCOM_FLEET", _("Fleet / Type Commander (TYCOM)")
    STRIKE_GROUP = "STRIKE_GROUP", _("Carrier Strike Group / DESRON / Air Wing")
    COMBATANT_UNIT = "COMBATANT_UNIT", _("Combatant Vessel / Squadron (Hull)")
    SHORE_DEPOT = "SHORE_DEPOT", _("Shore Logistics & Fleet Homeport")
    WARFARE_CENTER = "WARFARE_CENTER", _("R&D Warfare Center / Test Range")


class EmconPosture(models.TextChoices):
    ALPHA = "ALPHA", _("EMCON ALPHA - Complete Electronic Silence (Mask Coordinates)")
    BRAVO = "BRAVO", _("EMCON BRAVO - Restricted Emissions (Mask Coordinates)")
    CHARLIE = "CHARLIE", _("EMCON CHARLIE - Tactical Mission Operations")
    DELTA = "DELTA", _("EMCON DELTA - Normal In-Port / Garrison")


class Installation(BaseModel):
    installation_id = models.CharField(
        max_length=20,
        unique=True,
        null=True,
        blank=True,
        db_index=True,
        help_text="MEW Installation Identifier (e.g., INST-001)",
    )
    installation_code = models.CharField(
        max_length=30,
        unique=True,
        null=True,
        blank=True,
        db_index=True,
        help_text="Installation short code (e.g., NSWC-CRANE)",
    )
    name = models.CharField(max_length=255)
    dodaac = models.CharField(max_length=6, unique=True, help_text="DoD Activity Address Code / UIC")
    city = models.CharField(max_length=100, blank=True, default="")
    state_or_country = models.CharField(max_length=100, blank=True, default="")
    location_display = models.CharField(max_length=200, blank=True, default="")
    region = models.CharField(max_length=100)
    latitude = models.DecimalField(max_digits=9, decimal_places=6, null=True, blank=True)
    longitude = models.DecimalField(max_digits=9, decimal_places=6, null=True, blank=True)
    mission_category = models.CharField(
        max_length=50,
        choices=MissionCategory.choices,
        blank=True,
        default="",
    )
    primary_ew_focus = models.CharField(max_length=255, blank=True, default="")
    dsn_prefix = models.CharField(max_length=10, blank=True, default="")
    max_ew_billets = models.PositiveIntegerField(default=0)
    scif_accredited = models.BooleanField(default=True)
    rf_anechoic_chamber = models.BooleanField(default=False)
    readiness_rating = models.CharField(
        max_length=50,
        choices=InstallationReadinessRating.choices,
        blank=True,
        default="",
    )
    last_inspection_date = models.DateField(null=True, blank=True)

    def save(self, *args, **kwargs):
        if not self.location_display and self.city and self.state_or_country:
            self.location_display = f"{self.city}, {self.state_or_country}"
        super().save(*args, **kwargs)

    def __str__(self):
        code = self.installation_code or self.dodaac
        return f"{self.name} ({code})"


class MilitaryUnit(BaseModel):
    name = models.CharField(max_length=255)
    uic = models.CharField(max_length=6, unique=True, help_text="Unit Identification Code")
    parent_unit = models.ForeignKey('self', on_delete=models.SET_NULL, null=True, blank=True, related_name='subordinate_units')
    echelon_level = models.CharField(
        max_length=30,
        choices=CommandEchelon.choices,
        default=CommandEchelon.COMBATANT_UNIT,
        help_text="Hierarchical command echelon for tree-based multi-tenancy",
    )

    objects = MilitaryUnitManager()
    installation = models.ForeignKey(Installation, on_delete=models.PROTECT, related_name='units')
    command_type = models.CharField(max_length=100)
    
    personnel_readiness_pct = models.DecimalField(max_digits=5, decimal_places=2, default=100.00)
    equipment_readiness_pct = models.DecimalField(max_digits=5, decimal_places=2, default=100.00)

    def get_subordinate_unit_ids(self, include_self: bool = True) -> list:
        """
        Traverses the hierarchical command tree (Fleet -> Strike Group -> Hull)
        and returns all subordinate MilitaryUnit UUIDs.
        """
        if self.echelon_level == CommandEchelon.TYCOM_FLEET:
            return list(MilitaryUnit.objects.values_list("id", flat=True))

        visited = set()
        if include_self:
            visited.add(self.id)

        frontier = [self.id]
        while frontier:
            children = list(
                MilitaryUnit.objects.filter(parent_unit_id__in=frontier)
                .exclude(id__in=visited)
                .values_list("id", flat=True)
            )
            if not children:
                break
            visited.update(children)
            frontier = children
        return list(visited)

    def get_subordinate_uics(self, include_self: bool = True) -> list:
        ids = self.get_subordinate_unit_ids(include_self=include_self)
        return list(MilitaryUnit.objects.filter(id__in=ids).values_list("uic", flat=True))

    def __str__(self):
        return f"{self.name} [{self.uic}]"


class NavalHull(BaseModel):
    """
    Represents a U.S. Naval Platform (Surface Ship, Submarine, or Airborne EW Squadron)
    capable of hosting Maritime Electromagnetic Warfare (MEW) systems.
    """
    hull_id = models.CharField(
        max_length=20,
        unique=True,
        db_index=True,
        help_text="Reference Catalog Hull Identifier (e.g., HULL-001)",
    )
    hull_number = models.CharField(
        max_length=20,
        unique=True,
        db_index=True,
        help_text="Hull classification symbol or squadron designation (e.g., DDG-51, VAQ-132)",
    )
    vessel_name = models.CharField(
        max_length=150,
        help_text="Vessel or Squadron name (e.g., USS Arleigh Burke)",
    )
    ship_class = models.CharField(
        max_length=150,
        help_text="Ship class or aircraft platform (e.g., Arleigh Burke-class Flight I)",
    )
    domain = models.CharField(
        max_length=50,
        choices=OperationalDomain.choices,
        default=OperationalDomain.SURFACE,
    )
    homeport = models.CharField(
        max_length=150,
        help_text="Assigned homeport display name (e.g., NS Norfolk, VA)",
    )
    homeport_installation = models.ForeignKey(
        Installation,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="berthed_hulls",
    )
    unit = models.OneToOneField(
        MilitaryUnit,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="naval_hull",
        help_text="Links the Naval Hull to its corresponding MilitaryUnit for readiness tracking",
    )
    emcon_posture = models.CharField(
        max_length=20,
        choices=EmconPosture.choices,
        default=EmconPosture.CHARLIE,
        help_text="Emission Control (EMCON) posture driving ABAC geospatial/telemetry masking",
    )

    class Meta:
        verbose_name = "Naval Hull / Platform"
        verbose_name_plural = "Naval Hulls / Platforms"
        indexes = [
            models.Index(fields=["domain", "ship_class"]),
            models.Index(fields=["homeport"]),
        ]

    def __str__(self):
        return f"{self.vessel_name} ({self.hull_number})"
