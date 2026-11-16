from dataclasses import dataclass, field
from typing import List, Optional
from django.db import connection
from django.db.models import Q, QuerySet

from .models import (
    CommandEchelon,
    EmconPosture,
    FunctionalRole,
    MilitaryUnit,
    Personnel,
    PersonnelCategory,
    SecurityAuditLog,
    SecurityClearance,
)

# Pre-configured RBAC + ABAC Demonstration Personas
CURATED_RBAC_PERSONAS = [
    {
        "personnel_id": "MEW-PER-0000",
        "dod_id": "1001122334",
        "first_name": "Evelyn",
        "middle_initial": "A",
        "last_name": "Thorne",
        "personnel_category": PersonnelCategory.DOD_CIVILIAN,
        "paygrade": "GS-15",
        "rank": "CIV (GS-15)",
        "rank_full": "Principal Product Support Manager & ORSA Lead (GS-15)",
        "specialty_code": "GS-1515 / GS-0346 (Operations Research & Product Support)",
        "title": "PEO IWS 2.0 / NSWC Crane Enterprise MEW Product Support Manager (PSM)",
        "role": "Cross-Fleet Ao/MTBM/MDT Analytics, Predictive LRU Depletion, BOM Impact & NEC Staffing Readiness",
        "functional_role": FunctionalRole.FLEET_READINESS_ANALYST,
        "security_clearance": SecurityClearance.TS_SCI,
        "target_hull_number": None,
        "target_installation_code": "NSWC-CRANE",
        "authorized_cage_codes": [],
        "badge_label": "Enterprise PSM / ORSA Lead (Cross-Fleet Analytics & BOM | TS/SCI)",
    },
    {
        "personnel_id": "MEW-PER-0001",
        "dod_id": "1004928371",
        "first_name": "Marcus",
        "middle_initial": "J",
        "last_name": "Vance",
        "personnel_category": PersonnelCategory.ACTIVE_DUTY_OFFICER,
        "paygrade": "O-5",
        "rank": "CDR",
        "rank_full": "Commander",
        "specialty_code": "1810 (Information Warfare / EW)",
        "title": "TYCOM Director of Maritime Electromagnetic Maneuver Warfare",
        "role": "Hierarchical Fleet Readiness Oversight, CASREP Analytics & Multi-Domain EW Policy",
        "functional_role": FunctionalRole.FLEET_READINESS_ANALYST,
        "security_clearance": SecurityClearance.TS_SCI,
        "target_hull_number": None,
        "target_installation_code": "NAVIFOR-SUF",
        "authorized_cage_codes": [],
        "badge_label": "TYCOM Command Staff (Full Echelon Tree | TS/SCI)",
    },
    {
        "personnel_id": "MEW-PER-0002",
        "dod_id": "2008471920",
        "first_name": "Devon",
        "middle_initial": "K",
        "last_name": "Mercer",
        "personnel_category": PersonnelCategory.ACTIVE_Duty_Enlisted
        if hasattr(PersonnelCategory, "ACTIVE_Duty_Enlisted")
        else PersonnelCategory.ACTIVE_DUTY_ENLISTED,
        "paygrade": "E-6",
        "rank": "CTT1",
        "rank_full": "Cryptologic Technician (Technical) First Class",
        "specialty_code": "NEC C16A (AN/SLQ-32(V)6 Maintainer)",
        "title": "Shipboard EW Work Center Supervisor (CE02)",
        "role": "USS Arleigh Burke (DDG-51) Shipboard RF Diagnostics & MAF Execution",
        "functional_role": FunctionalRole.SHIPBOARD_TECHNICIAN,
        "security_clearance": SecurityClearance.TS_SCI,
        "target_hull_number": "DDG-51",
        "target_installation_code": "NAVSTA-NORVA",
        "authorized_cage_codes": [],
        "badge_label": "Shipboard Tech (Isolated to DDG-51 UIC | TS/SCI)",
    },
    {
        "personnel_id": "MEW-PER-0003",
        "dod_id": "3009182736",
        "first_name": "Hannah",
        "middle_initial": "M",
        "last_name": "Sterling",
        "personnel_category": PersonnelCategory.ACTIVE_DUTY_OFFICER,
        "paygrade": "O-3",
        "rank": "LT",
        "rank_full": "Lieutenant (Supply Corps)",
        "specialty_code": "3100 (Unit Supply Officer - SUPPO)",
        "title": "Squadron Supply & AVCAL Spares Officer (VAQ-132)",
        "role": "VAQ-132 Growler Squadron Requisition Approval & Inbound Shipment Receipt",
        "functional_role": FunctionalRole.UNIT_SUPPO,
        "security_clearance": SecurityClearance.SECRET,
        "target_hull_number": "VAQ-132",
        "target_installation_code": "NAS-WHIDBEY",
        "authorized_cage_codes": [],
        "badge_label": "Unit SUPPO (Isolated to VAQ-132 UIC | Secret)",
    },
    {
        "personnel_id": "MEW-PER-0004",
        "dod_id": "4007261549",
        "first_name": "Arthur",
        "middle_initial": "B",
        "last_name": "Keller",
        "personnel_category": PersonnelCategory.DOD_CIVILIAN,
        "paygrade": "GS-12",
        "rank": "CIV (GS-12)",
        "rank_full": "Civilian Logistics Management Specialist (GS-12)",
        "specialty_code": "GS-0346 (Logistics Management)",
        "title": "NSWC Crane Depot Provisioning & DLR Turn-In Manager",
        "role": "Regional Warehouse LRU Fulfillment, NSN Stocking & Carcass Turn-In (Coords Masked)",
        "functional_role": FunctionalRole.SHORE_DEPOT_MANAGER,
        "security_clearance": SecurityClearance.SECRET,
        "target_hull_number": None,
        "target_installation_code": "NSWC-CRANE",
        "authorized_cage_codes": [],
        "badge_label": "Shore Depot Manager (Warehouse Scope | Coords Redacted)",
    },
    {
        "personnel_id": "MEW-PER-0005",
        "dod_id": "5001928374",
        "first_name": "Elena",
        "middle_initial": "R",
        "last_name": "Rostova",
        "personnel_category": PersonnelCategory.DOD_CIVILIAN,
        "paygrade": "GS-13",
        "rank": "CIV (GS-13)",
        "rank_full": "Civilian Electronics Engineer (GS-13)",
        "specialty_code": "GS-0855 (Electronics Engineering)",
        "title": "Lead SEWIP / GaN AESA Reliability Engineer (NSWC Crane)",
        "role": "Cross-Fleet MTBF/MTTR Engineering & 3-M Failure Analysis (EMCON Coords Masked)",
        "functional_role": FunctionalRole.WARFARE_CENTER_ENGINEER,
        "security_clearance": SecurityClearance.TS_SCI,
        "target_hull_number": None,
        "target_installation_code": "NSWC-CRANE",
        "authorized_cage_codes": [],
        "badge_label": "Warfare Center Engineer (Cross-Fleet MTBF | EMCON Masked)",
    },
    {
        "personnel_id": "MEW-PER-0006",
        "dod_id": "6008374651",
        "first_name": "Nolan",
        "middle_initial": "T",
        "last_name": "Mercer-OEM",
        "personnel_category": PersonnelCategory.DEFENSE_CONTRACTOR,
        "paygrade": "CTR",
        "rank": "CTR",
        "rank_full": "Defense Contractor (OEM Field Service Engineer)",
        "specialty_code": "Lockheed Martin OEM Technical Representative (CAGE 03538)",
        "title": "Senior SEWIP Block 2 / BLQ-10 Field Service Representative",
        "role": "CAGE 03538 Hardware Diagnostics Only — Hull Identity & Fleet Disposition Redacted",
        "functional_role": FunctionalRole.WARFARE_CENTER_ENGINEER,
        "security_clearance": SecurityClearance.SECRET,
        "target_hull_number": None,
        "target_installation_code": "NSWC-CRANE",
        "authorized_cage_codes": ["03538"],
        "badge_label": "OEM Contractor (CAGE 03538 Scope Only | Platforms Redacted)",
    },
]


@dataclass
class AccessContext:
    personnel: Optional[Personnel] = None
    persona_id: str = "MEW-PER-0001"
    actor_name: str = "CDR Marcus Vance"
    functional_role: str = FunctionalRole.FLEET_READINESS_ANALYST
    personnel_category: str = PersonnelCategory.ACTIVE_DUTY_OFFICER
    security_clearance: str = SecurityClearance.TS_SCI
    assigned_uic: str = "N00060"
    unit_id: Optional[str] = None
    subordinate_unit_ids: List[str] = field(default_factory=list)
    subordinate_uics: List[str] = field(default_factory=list)
    authorized_cage_codes: List[str] = field(default_factory=list)
    installation_code: str = "NAVIFOR-SUF"

    @property
    def is_oem_contractor(self) -> bool:
        return (
            self.personnel_category == PersonnelCategory.DEFENSE_CONTRACTOR
            or len(self.authorized_cage_codes) > 0
        )

    @property
    def is_unit_isolated(self) -> bool:
        return self.functional_role in (
            FunctionalRole.SHIPBOARD_TECHNICIAN,
            FunctionalRole.UNIT_SUPPO,
        )

    @property
    def is_depot_manager(self) -> bool:
        return self.functional_role == FunctionalRole.SHORE_DEPOT_MANAGER

    @property
    def is_warfare_engineer(self) -> bool:
        return self.functional_role == FunctionalRole.WARFARE_CENTER_ENGINEER

    @property
    def is_fleet_analyst(self) -> bool:
        return self.functional_role == FunctionalRole.FLEET_READINESS_ANALYST

    def should_mask_geospatial(self, emcon_posture: Optional[str] = None) -> bool:
        """
        Geospatial & Telemetry Masking (Section 2 & 3 of RBAC_MULTITENANCY_DESIGN_CONSIDERATIONS.md):
        - Shore Depot Managers and OEM Contractors never see live operational coordinates.
        - Under EMCON ALPHA or BRAVO, users without TS/SCI + Fleet Analyst role have coordinates masked.
        """
        if self.is_depot_manager or self.is_oem_contractor:
            return True
        if emcon_posture in (EmconPosture.ALPHA, EmconPosture.BRAVO):
            if self.security_clearance != SecurityClearance.TS_SCI or not self.is_fleet_analyst:
                return True
        if self.security_clearance != SecurityClearance.TS_SCI and self.is_warfare_engineer:
            return True
        return False

    def should_mask_platform_identity(self) -> bool:
        """
        Program & Vendor Isolation (Section 1 & 3):
        OEM Contractors only see LRU/System serial numbers, CAGE codes, and MTBF/failure modes
        without exposing which warships (hull_number / vessel_name) are degraded.
        """
        return self.is_oem_contractor

    @property
    def active_redaction_policies(self) -> List[str]:
        rules = []
        if self.is_unit_isolated:
            rules.append(f"UIC-ISOLATED ({self.assigned_uic})")
            rules.append("MOSAIC-EFFECT GUARD (SINGLE-HULL VIEW)")
        if self.is_oem_contractor:
            cages = ",".join(self.authorized_cage_codes) or "03538"
            rules.append(f"VENDOR CAGE SCOPE ({cages})")
            rules.append("PLATFORM IDENTITY REDACTED")
        if self.is_depot_manager:
            rules.append("SHORE WAREHOUSE SCOPE")
            rules.append("LIVE SHIP COORDS MASKED (FPO HUB ONLY)")
        if self.is_warfare_engineer and not self.is_oem_contractor:
            rules.append("ENGINEERING RELIABILITY VIEW")
            rules.append("EMCON ALPHA/BRAVO COORDS MASKED")
        if self.is_fleet_analyst:
            rules.append("HIERARCHICAL ECHELON TREE ACCESS")
            rules.append("BULK CSV EXPORT AUTHORIZED")
        return rules

    # =========================================================================
    # TENANT & ABAC QUERYSET SCOPING METHODS
    # =========================================================================
    def scope_units(self, qs: QuerySet) -> QuerySet:
        if self.is_fleet_analyst:
            if self.subordinate_unit_ids:
                return qs.filter(id__in=self.subordinate_unit_ids)
            return qs
        if self.is_unit_isolated:
            if self.unit_id:
                return qs.filter(id=self.unit_id)
            return qs.filter(uic=self.assigned_uic)
        if self.is_depot_manager:
            # Depot managers see shore depot units only (preventing fleet readiness Mosaic Effect)
            return qs.filter(
                Q(installation__installation_code=self.installation_code)
                | Q(echelon_level__in=[CommandEchelon.SHORE_DEPOT, CommandEchelon.WARFARE_CENTER])
            )
        if self.is_oem_contractor:
            # OEM Contractors are blocked from unit-level readiness heatmaps (Mosaic Effect protection)
            return qs.none()
        return qs

    def scope_assets(self, qs: QuerySet) -> QuerySet:
        if self.is_oem_contractor:
            return qs.filter(asset_model__cage_code__in=self.authorized_cage_codes)
        if self.is_unit_isolated:
            if self.unit_id:
                return qs.filter(assigned_unit_id=self.unit_id)
            return qs.filter(assigned_unit__uic=self.assigned_uic)
        if self.is_fleet_analyst and self.subordinate_unit_ids:
            return qs.filter(assigned_unit_id__in=self.subordinate_unit_ids)
        return qs

    def scope_hulls(self, qs: QuerySet) -> QuerySet:
        if self.is_oem_contractor:
            return qs.none()
        if self.is_unit_isolated:
            if self.unit_id:
                return qs.filter(unit_id=self.unit_id)
            return qs.filter(unit__uic=self.assigned_uic)
        return qs

    def scope_loadouts(self, qs: QuerySet) -> QuerySet:
        if self.is_oem_contractor:
            return qs.filter(system__cage_code__in=self.authorized_cage_codes)
        if self.is_unit_isolated:
            if self.unit_id:
                return qs.filter(hull__unit_id=self.unit_id)
            return qs.filter(hull__unit__uic=self.assigned_uic)
        return qs

    def scope_work_orders(self, qs: QuerySet) -> QuerySet:
        if self.is_oem_contractor:
            return qs.filter(
                Q(asset__asset_model__cage_code__in=self.authorized_cage_codes)
                | Q(parts_used__material__cage_code__in=self.authorized_cage_codes)
            ).distinct()
        if self.is_unit_isolated:
            if self.unit_id:
                return qs.filter(asset__assigned_unit_id=self.unit_id)
            return qs.filter(asset__assigned_unit__uic=self.assigned_uic)
        return qs

    def scope_requisitions(self, qs: QuerySet) -> QuerySet:
        if self.is_oem_contractor:
            return qs.filter(material__cage_code__in=self.authorized_cage_codes)
        if self.is_unit_isolated:
            if self.unit_id:
                return qs.filter(requesting_unit_id=self.unit_id)
            return qs.filter(requesting_unit__uic=self.assigned_uic)
        return qs

    def scope_components(self, qs: QuerySet) -> QuerySet:
        if self.is_oem_contractor:
            return qs.filter(cage_code__in=self.authorized_cage_codes)
        return qs

    def scope_inventory_balances(self, qs: QuerySet) -> QuerySet:
        if self.is_oem_contractor:
            return qs.filter(material__cage_code__in=self.authorized_cage_codes)
        if self.is_unit_isolated and self.installation_code:
            return qs.filter(installation__installation_code=self.installation_code)
        return qs

    def scope_personnel(self, qs: QuerySet) -> QuerySet:
        if self.is_oem_contractor:
            return qs.filter(personnel_id=self.persona_id)
        if self.is_unit_isolated:
            if self.unit_id:
                return qs.filter(
                    Q(unit_id=self.unit_id) | Q(installation_code=self.installation_code)
                )
            return qs.filter(assigned_uic=self.assigned_uic)
        return qs

    def scope_transactions(self, qs: QuerySet) -> QuerySet:
        if self.is_oem_contractor:
            return qs.filter(balance__material__cage_code__in=self.authorized_cage_codes)
        if self.is_unit_isolated and self.installation_code:
            return qs.filter(balance__installation__installation_code=self.installation_code)
        return qs

    # =========================================================================
    # ACTION AUTHORIZATION CHECKS
    # =========================================================================
    def can_export_fleet_csv(self) -> tuple[bool, str]:
        if self.is_fleet_analyst and self.security_clearance == SecurityClearance.TS_SCI:
            return True, "Authorized: TYCOM Fleet Readiness Analyst with TS/SCI clearance."
        return (
            False,
            f"Mosaic-Effect OPSEC Denial: Role '{self.functional_role}' ({self.security_clearance}) "
            f"is not authorized to export cross-fleet readiness aggregations.",
        )

    def can_receive_shipment(self, requisition) -> tuple[bool, str]:
        if self.is_depot_manager or self.is_fleet_analyst:
            return True, "Authorized: Depot Manager / Command Staff."
        if self.functional_role == FunctionalRole.UNIT_SUPPO:
            if not self.unit_id or str(requisition.requesting_unit_id) == str(self.unit_id):
                return True, f"Authorized: Unit SUPPO for {self.assigned_uic}."
            return (
                False,
                f"ABAC Scope Denial: SUPPO ({self.assigned_uic}) cannot receive shipments for another unit ({requisition.requesting_unit.uic}).",
            )
        return (
            False,
            f"RBAC Denial: Role '{self.functional_role}' cannot commit warehouse shipment receipts (requires UNIT_SUPPO or SHORE_DEPOT_MANAGER).",
        )

    def can_advance_work_order(self, work_order, target_status: str) -> tuple[bool, str]:
        if self.is_depot_manager:
            return (
                False,
                "RBAC Denial: Shore Depot Inventory Managers cannot sign off shipboard CSMP Maintenance Action Forms.",
            )
        if self.functional_role == FunctionalRole.UNIT_SUPPO:
            return (
                False,
                "RBAC Denial: Supply Officers (SUPPO) manage requisitions and COSAL spares, not technical MAF sign-offs.",
            )
        if self.is_oem_contractor:
            # Verify CAGE match
            asset_cage = work_order.asset.asset_model.cage_code if work_order.asset and work_order.asset.asset_model else ""
            if asset_cage not in self.authorized_cage_codes:
                return (
                    False,
                    f"ABAC CAGE Scope Denial: OEM Contractor ({self.authorized_cage_codes}) cannot modify MAF for CAGE {asset_cage}.",
                )
            if target_status in ("CLOSED", "Closed"):
                return (
                    False,
                    "RBAC Denial: External Defense Contractors cannot perform final military QA/COMSEC closure sign-off on a CSMP MAF.",
                )
        return True, "Authorized by CSMP RBAC/ABAC policy."

    def log_event(
        self,
        event_type: str,
        decision: str,
        resource_target: str,
        reason: str,
    ):
        SecurityAuditLog.objects.create(
            actor=self.personnel,
            actor_name=self.actor_name,
            actor_role=self.functional_role,
            actor_uic=self.assigned_uic,
            actor_clearance=self.security_clearance,
            actor_cage_scope=",".join(self.authorized_cage_codes),
            event_type=event_type,
            decision=decision,
            resource_target=resource_target,
            reason=reason,
        )


def resolve_access_context(request) -> AccessContext:
    """
    Resolves the active caller's RBAC + ABAC AccessContext from:
      1. `X-MEW-Persona-ID` HTTP Header or `?persona_id=` query param, OR
      2. Authenticated `request.user.personnel` profile, OR
      3. Default `MEW-PER-0001` (TYCOM Fleet Readiness Analyst).
    Also binds PostgreSQL session variables for Row-Level Security (RLS) awareness.
    """
    persona_id = None
    if request is not None:
        persona_id = request.headers.get("X-MEW-Persona-ID") or request.query_params.get(
            "persona_id"
        )

    person = None
    if persona_id:
        person = (
            Personnel.objects.select_related("unit", "installation")
            .filter(personnel_id=persona_id)
            .first()
        )
    elif (
        request is not None
        and hasattr(request, "user")
        and getattr(request.user, "is_authenticated", False)
    ):
        person = getattr(request.user, "personnel", None)

    if not person:
        person = (
            Personnel.objects.select_related("unit", "installation")
            .filter(personnel_id="MEW-PER-0001")
            .first()
        )

    if not person:
        return AccessContext()

    unit_id_str = str(person.unit_id) if person.unit_id else None
    sub_ids = (
        [str(uid) for uid in person.unit.get_subordinate_unit_ids(include_self=True)]
        if person.unit
        else []
    )
    sub_uics = (
        person.unit.get_subordinate_uics(include_self=True)
        if person.unit
        else ([person.assigned_uic] if person.assigned_uic else [])
    )

    ctx = AccessContext(
        personnel=person,
        persona_id=person.personnel_id or "MEW-PER-0001",
        actor_name=f"{person.rank} {person.first_name} {person.last_name}".strip(),
        functional_role=person.functional_role,
        personnel_category=person.personnel_category,
        security_clearance=person.security_clearance,
        assigned_uic=person.assigned_uic or (person.unit.uic if person.unit else "N00060"),
        unit_id=unit_id_str,
        subordinate_unit_ids=sub_ids,
        subordinate_uics=sub_uics,
        authorized_cage_codes=list(person.authorized_cage_codes or []),
        installation_code=person.installation_code or "NSWC-CRANE",
    )

    # Set PostgreSQL session variables for Row-Level Security (RLS) policy evaluation
    try:
        with connection.cursor() as cursor:
            cursor.execute(
                "SELECT set_config('app.current_persona_id', %s, true), "
                "set_config('app.current_role', %s, true), "
                "set_config('app.current_uic', %s, true), "
                "set_config('app.current_clearance', %s, true);",
                [
                    ctx.persona_id,
                    ctx.functional_role,
                    ctx.assigned_uic,
                    ctx.security_clearance,
                ],
            )
    except Exception:
        # Ignore on SQLite or non-transactional test connections
        pass

    return ctx
