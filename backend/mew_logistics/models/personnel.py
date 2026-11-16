from django.db import models
from django.contrib.auth.models import User
from django.utils.translation import gettext_lazy as _
from .base import BaseModel
from .organization import Installation, MilitaryUnit


class PersonnelCategory(models.TextChoices):
    ACTIVE_DUTY_ENLISTED = "Active_Duty_Enlisted", _("Active Duty Enlisted")
    ACTIVE_DUTY_OFFICER = "Active_Duty_Officer", _("Active Duty Officer")
    DOD_CIVILIAN = "DoD_Civilian", _("DoD Civilian")
    DEFENSE_CONTRACTOR = "Defense_Contractor", _("Defense Contractor")


class SecurityClearance(models.TextChoices):
    SECRET = "Secret", _("Secret")
    TS_SCI = "TS/SCI", _("TS/SCI")


class OperationalStatus(models.TextChoices):
    IN_GARRISON = "In_Garrison", _("In Garrison")
    UNDERWAY_AFLOAT_SUPPORT = "Underway_Afloat_Support", _("Underway / Afloat Support")
    RANGE_TEST_DETACHMENT = "Range_Test_Detachment", _("Range Test Detachment")
    TDY_SCHOOLHOUSE = "TDY_Schoolhouse", _("TDY Schoolhouse")
    LEAVE = "Leave", _("Leave")


class FunctionalRole(models.TextChoices):
    SHIPBOARD_TECHNICIAN = (
        "SHIPBOARD_TECHNICIAN",
        _("Shipboard / Squadron EW Technician (CTT/ET/AT)"),
    )
    UNIT_SUPPO = (
        "UNIT_SUPPO",
        _("Unit Supply / Logistics Officer (SUPPO)"),
    )
    SHORE_DEPOT_MANAGER = (
        "SHORE_DEPOT_MANAGER",
        _("Shore-Based Depot / Inventory Manager"),
    )
    WARFARE_CENTER_ENGINEER = (
        "WARFARE_CENTER_ENGINEER",
        _("Warfare Center / OEM Systems Engineer"),
    )
    FLEET_READINESS_ANALYST = (
        "FLEET_READINESS_ANALYST",
        _("Fleet Readiness Analyst / Command Staff (TYCOM)"),
    )


class Personnel(BaseModel):
    user = models.OneToOneField(User, on_delete=models.CASCADE, null=True, blank=True)

    # Identifiers & Personal Attributes
    personnel_id = models.CharField(
        max_length=20,
        unique=True,
        null=True,
        blank=True,
        db_index=True,
        help_text="MEW Personnel Identifier (e.g., MEW-PER-0001)",
    )
    dod_id = models.CharField(
        max_length=10,
        unique=True,
        null=True,
        blank=True,
        db_index=True,
        help_text="10-digit DoD ID / EDIPI",
    )
    first_name = models.CharField(max_length=100, blank=True, default="")
    middle_initial = models.CharField(max_length=1, blank=True, default="")
    last_name = models.CharField(max_length=100, blank=True, default="")

    # Classification, Rank & Role
    personnel_category = models.CharField(
        max_length=50,
        choices=PersonnelCategory.choices,
        default=PersonnelCategory.ACTIVE_DUTY_ENLISTED,
    )
    paygrade = models.CharField(max_length=20, blank=True, default="")
    rank = models.CharField(max_length=30)
    rank_full = models.CharField(max_length=100, blank=True, default="")
    specialty_code = models.CharField(
        max_length=100,
        blank=True,
        default="",
        help_text="NEC, Officer Designator, GS Series, or Contractor Specialty",
    )
    mos = models.CharField(
        max_length=50,
        blank=True,
        default="",
        help_text="Military Occupational Specialty / legacy code",
    )
    title = models.CharField(max_length=150, blank=True, default="")
    role = models.CharField(max_length=255, blank=True, default="")

    # Hybrid RBAC + ABAC Access Control Attributes
    functional_role = models.CharField(
        max_length=40,
        choices=FunctionalRole.choices,
        default=FunctionalRole.SHIPBOARD_TECHNICIAN,
        help_text="Core RBAC functional duty role governing permitted actions and views",
    )
    assigned_uic = models.CharField(
        max_length=10,
        blank=True,
        default="",
        db_index=True,
        help_text="ABAC Unit Identification Code dynamically binding tenant data scope",
    )
    authorized_cage_codes = models.JSONField(
        default=list,
        blank=True,
        help_text="ABAC CAGE Code scope restricting external OEM contractors to supported hardware (e.g., ['03538'])",
    )

    # Assignment & Installation Correlation
    unit = models.ForeignKey(
        MilitaryUnit,
        on_delete=models.CASCADE,
        related_name="personnel",
        null=True,
        blank=True,
    )
    installation = models.ForeignKey(
        Installation,
        on_delete=models.SET_NULL,
        related_name="personnel",
        null=True,
        blank=True,
    )
    installation_code = models.CharField(max_length=30, blank=True, default="")
    installation_deployed_at = models.CharField(max_length=255, blank=True, default="")
    installation_location = models.CharField(max_length=200, blank=True, default="")
    installation_mission_category = models.CharField(max_length=50, blank=True, default="")

    # Security, Readiness & Rotation
    security_clearance = models.CharField(
        max_length=50,
        choices=SecurityClearance.choices,
        default=SecurityClearance.SECRET,
    )
    operational_status = models.CharField(
        max_length=50,
        choices=OperationalStatus.choices,
        default=OperationalStatus.IN_GARRISON,
    )
    deployment_status = models.CharField(max_length=50, default="Garrison")
    is_medically_deployable = models.BooleanField(default=True)
    is_training_current = models.BooleanField(default=True)

    date_assigned = models.DateField(null=True, blank=True)
    projected_rotation_date = models.CharField(
        max_length=50,
        blank=True,
        default="N/A (Permanent Billet)",
        help_text="ISO rotation date or N/A (Permanent Billet) for DoD Civilians",
    )

    # Contact Information
    dsn_phone = models.CharField(max_length=25, blank=True, default="")
    official_email = models.EmailField(max_length=255, unique=True, null=True, blank=True)

    class Meta:
        verbose_name_plural = "Personnel"
        indexes = [
            models.Index(fields=["personnel_category", "operational_status"]),
            models.Index(fields=["installation_code"]),
            models.Index(fields=["security_clearance"]),
        ]

    def save(self, *args, **kwargs):
        # Auto-populate installation and assigned_uic from unit if not explicitly provided
        if self.unit_id and self.unit:
            if not self.installation and self.unit.installation_id:
                self.installation = self.unit.installation
            if not self.assigned_uic and self.unit.uic:
                self.assigned_uic = self.unit.uic

        # Sync denormalized installation attributes from Installation FK when available
        if self.installation:
            if not self.installation_code and self.installation.installation_code:
                self.installation_code = self.installation.installation_code
            if not self.installation_deployed_at:
                self.installation_deployed_at = self.installation.name
            if not self.installation_location and self.installation.location_display:
                self.installation_location = self.installation.location_display
            if not self.installation_mission_category and self.installation.mission_category:
                self.installation_mission_category = self.installation.mission_category
            if not self.assigned_uic and self.installation.dodaac:
                self.assigned_uic = self.installation.dodaac

        # Auto-infer RBAC functional_role and ABAC CAGE scope if using default
        if self.functional_role == FunctionalRole.SHIPBOARD_TECHNICIAN:
            if self.personnel_category == PersonnelCategory.DEFENSE_CONTRACTOR:
                self.functional_role = FunctionalRole.WARFARE_CENTER_ENGINEER
                if not self.authorized_cage_codes:
                    self.authorized_cage_codes = ["03538"]
            elif "Logistics" in self.title or "Supply" in self.role or "0346" in self.specialty_code:
                self.functional_role = FunctionalRole.SHORE_DEPOT_MANAGER
            elif self.personnel_category == PersonnelCategory.DOD_CIVILIAN or "1440" in self.specialty_code:
                self.functional_role = FunctionalRole.WARFARE_CENTER_ENGINEER
            elif self.rank in ("CDR", "LCDR", "CTTC") and self.installation_mission_category in (
                "Operational_Command",
                "Cryptologic_Operations",
            ):
                self.functional_role = FunctionalRole.FLEET_READINESS_ANALYST
            elif self.personnel_category == PersonnelCategory.ACTIVE_DUTY_OFFICER and self.rank == "LT":
                self.functional_role = FunctionalRole.UNIT_SUPPO

        # Keep specialty_code and legacy mos field aligned
        if self.specialty_code and not self.mos:
            self.mos = self.specialty_code[:50]
        elif self.mos and not self.specialty_code:
            self.specialty_code = self.mos

        # Keep deployment_status aligned with operational_status
        if self.operational_status and self.deployment_status == "Garrison":
            self.deployment_status = self.operational_status

        super().save(*args, **kwargs)

    def is_fully_deployable(self):
        return (
            self.is_medically_deployable
            and self.is_training_current
            and self.operational_status != OperationalStatus.LEAVE
        )

    def __str__(self):
        spec = self.specialty_code or self.mos
        mid = f" {self.middle_initial}." if self.middle_initial else ""
        return f"{self.rank} {self.first_name}{mid} {self.last_name} - {spec}".strip()
