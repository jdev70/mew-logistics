import csv
import json
import random
from datetime import date, timedelta
from django.core.management.base import BaseCommand
from django.db import transaction

from mew_logistics.models import (
    Asset,
    AssetModel,
    CapabilityStatus,
    HullSystemAssignment,
    Installation,
    MilitaryUnit,
    MissionCategory,
    MissionRole,
    NavalHull,
    ReadinessLevel,
)
from mew_logistics.tasks import (
    calculate_fleet_reliability_metrics,
    generate_daily_readiness_snapshots,
)

# 1. Reference Catalog: U.S. Naval Hulls
NAVAL_HULLS = [
    {"hull_id": "HULL-001", "hull_number": "DDG-51", "vessel_name": "USS Arleigh Burke", "ship_class": "Arleigh Burke-class Flight I", "domain": "Surface", "homeport": "NS Norfolk, VA"},
    {"hull_id": "HULL-002", "hull_number": "DDG-100", "vessel_name": "USS Kidd", "ship_class": "Arleigh Burke-class Flight IIA", "domain": "Surface", "homeport": "NS Everett, WA"},
    {"hull_id": "HULL-003", "hull_number": "DDG-125", "vessel_name": "USS Jack H. Lucas", "ship_class": "Arleigh Burke-class Flight III", "domain": "Surface", "homeport": "NB San Diego, CA"},
    {"hull_id": "HULL-004", "hull_number": "CG-65", "vessel_name": "USS Chosin", "ship_class": "Ticonderoga-class", "domain": "Surface", "homeport": "NB San Diego, CA"},
    {"hull_id": "HULL-005", "hull_number": "CVN-78", "vessel_name": "USS Gerald R. Ford", "ship_class": "Gerald R. Ford-class", "domain": "Surface", "homeport": "NS Norfolk, VA"},
    {"hull_id": "HULL-006", "hull_number": "LCS-10", "vessel_name": "USS Gabrielle Giffords", "ship_class": "Independence-class LCS", "domain": "Surface", "homeport": "NB San Diego, CA"},
    {"hull_id": "HULL-007", "hull_number": "SSN-774", "vessel_name": "USS Virginia", "ship_class": "Virginia-class Block I", "domain": "Subsurface", "homeport": "SUBASE New London, CT"},
    {"hull_id": "HULL-008", "hull_number": "SSN-792", "vessel_name": "USS Vermont", "ship_class": "Virginia-class Block IV", "domain": "Subsurface", "homeport": "JB Pearl Harbor-Hickam, HI"},
    {"hull_id": "HULL-009", "hull_number": "VAQ-132", "vessel_name": "Scorpions (EA-18G Squadron)", "ship_class": "EA-18G Growler", "domain": "Airborne", "homeport": "NAS Whidbey Island, WA"},
    {"hull_id": "HULL-010", "hull_number": "VP-45", "vessel_name": "Pelicans (P-8A Squadron)", "ship_class": "P-8A Poseidon", "domain": "Airborne", "homeport": "NAS Jacksonville, FL"},
]

# 2. Reference Catalog: Maritime Electromagnetic Warfare (MEW) Systems
MEW_SYSTEMS = [
    {"system_id": "SYS-101", "nomenclature": "AN/SLQ-32(V)6", "common_name": "SEWIP Block 2", "ew_category": "Electronic Support (ES)", "compatible_domains": ["Surface"], "manufacturer": "Lockheed Martin", "nsn": "5865-01-610-0101"},
    {"system_id": "SYS-102", "nomenclature": "AN/SLQ-32(V)7", "common_name": "SEWIP Block 3", "ew_category": "Electronic Attack (EA) / ES", "compatible_domains": ["Surface"], "manufacturer": "Northrop Grumman", "nsn": "5865-01-610-0102"},
    {"system_id": "SYS-103", "nomenclature": "AN/SLQ-32C(V)6", "common_name": "SEWIP Lite", "ew_category": "Electronic Support (ES)", "compatible_domains": ["Surface"], "manufacturer": "Lockheed Martin", "nsn": "5865-01-610-0103"},
    {"system_id": "SYS-104", "nomenclature": "Mk 53 Mod 9", "common_name": "Nulka Active Decoy System", "ew_category": "Off-Board Electronic Decoy", "compatible_domains": ["Surface"], "manufacturer": "BAE Systems", "nsn": "5865-01-610-0104"},
    {"system_id": "SYS-105", "nomenclature": "AN/SEQ-4", "common_name": "ODIN Optical Dazzler", "ew_category": "Directed Energy / Optical EA", "compatible_domains": ["Surface"], "manufacturer": "NSWC Dahlgren", "nsn": "5865-01-610-0105"},
    {"system_id": "SYS-106", "nomenclature": "AN/BLQ-10(V)", "common_name": "Submarine EW Support Suite", "ew_category": "SIGINT / Electronic Support", "compatible_domains": ["Subsurface"], "manufacturer": "Lockheed Martin", "nsn": "5865-01-610-0106"},
    {"system_id": "SYS-107", "nomenclature": "OE-538", "common_name": "Submarine Multifunction Mast", "ew_category": "RF Antenna / Mast Array", "compatible_domains": ["Subsurface"], "manufacturer": "L3Harris", "nsn": "5865-01-610-0107"},
    {"system_id": "SYS-108", "nomenclature": "AN/ALQ-249", "common_name": "Next Generation Jammer Mid-Band", "ew_category": "Airborne Electronic Attack", "compatible_domains": ["Airborne"], "manufacturer": "Raytheon", "nsn": "5865-01-610-0108"},
    {"system_id": "SYS-109", "nomenclature": "AN/ALQ-240(V)1", "common_name": "Maritime Patrol ESM Suite", "ew_category": "Electronic Support (ES)", "compatible_domains": ["Airborne"], "manufacturer": "Northrop Grumman", "nsn": "5865-01-610-0109"},
    {"system_id": "SYS-110", "nomenclature": "NEMESIS-LEAD", "common_name": "Distributed Decoy Payload", "ew_category": "Distributed Signature Emulation", "compatible_domains": ["Surface", "Subsurface", "Airborne"], "manufacturer": "Naval Research Lab", "nsn": "5865-01-610-0110"},
]

# 3. Reference Catalog: Tactical Mission Roles
MISSION_ROLES = [
    {"role_id": "ROLE-01", "role_code": "ASMD-EW", "role_name": "Anti-Ship Missile Defense", "description": "Soft-kill seeker jamming and off-board decoy seduction."},
    {"role_id": "ROLE-02", "role_code": "ISRT-SIGINT", "role_name": "Maritime ISR & Threat Geolocation", "description": "Passive emitter intercept, fingerprinting, and bearing estimation."},
    {"role_id": "ROLE-03", "role_code": "SEAD-ESCORT", "role_name": "Standoff & Escort Radar Jamming", "description": "Suppression of adversary coastal surveillance and targeting radars."},
    {"role_id": "ROLE-04", "role_code": "C-UAS-DE", "role_name": "Counter-UAS Sensor Dazzling", "description": "Electro-optical/infrared degradation of unmanned aerial scouts."},
    {"role_id": "ROLE-05", "role_code": "DECOY-DIST", "role_name": "Distributed Fleet Signature Emulation", "description": "Coordinated phantom fleet projection across networked nodes."},
]

# Domain-to-Role Compatibility Mapping
DOMAIN_ROLES = {
    "Surface": ["ROLE-01", "ROLE-02", "ROLE-04", "ROLE-05"],
    "Subsurface": ["ROLE-02", "ROLE-05"],
    "Airborne": ["ROLE-02", "ROLE-03", "ROLE-05"],
}


def random_install_date(start_year=2018, end_year=2026):
    start = date(start_year, 1, 1)
    end = date(end_year, 10, 1)
    delta = (end - start).days
    return (start + timedelta(days=random.randint(0, delta))).isoformat()


def generate_assignments(seed: int = 42):
    random.seed(seed)
    assignments = []
    counter = 1

    for hull in NAVAL_HULLS:
        domain = hull["domain"]
        # Filter systems compatible with this hull's domain
        valid_systems = [s for s in MEW_SYSTEMS if domain in s["compatible_domains"]]

        # Enforce realistic class-specific constraints
        if "LCS" in hull["ship_class"]:
            valid_systems = [
                s
                for s in valid_systems
                if s["nomenclature"] not in ("AN/SLQ-32(V)7", "AN/SEQ-4")
            ]
        elif "Flight III" in hull["ship_class"]:
            valid_systems = [
                s for s in valid_systems if s["nomenclature"] != "AN/SLQ-32C(V)6"
            ]

        # Assign 2 to 3 distinct systems per hull
        selected_systems = random.sample(
            valid_systems, k=min(len(valid_systems), random.randint(2, 3))
        )
        valid_role_ids = DOMAIN_ROLES[domain]

        for sys in selected_systems:
            role_id = random.choice(valid_role_ids)
            role_obj = next(r for r in MISSION_ROLES if r["role_id"] == role_id)

            # Determine realistic installed quantities
            qty = (
                random.choice([2, 4])
                if "Decoy" in sys["ew_category"] or "Jammer" in sys["ew_category"]
                else 1
            )

            assignments.append(
                {
                    "assignment_id": f"ASGN-{counter:04d}",
                    "hull_id": hull["hull_id"],
                    "hull_number": hull["hull_number"],
                    "vessel_name": hull["vessel_name"],
                    "ship_class": hull["ship_class"],
                    "domain": domain,
                    "homeport": hull["homeport"],
                    "system_id": sys["system_id"],
                    "nomenclature": sys["nomenclature"],
                    "common_name": sys["common_name"],
                    "ew_category": sys["ew_category"],
                    "quantity_installed": qty,
                    "role_id": role_obj["role_id"],
                    "role_code": role_obj["role_code"],
                    "role_name": role_obj["role_name"],
                    "operational_status": random.choices(
                        [
                            "FOC (Full Operational Capability)",
                            "IOC (Initial Operational Capability)",
                            "Depot Upgrade",
                        ],
                        weights=[0.75, 0.15, 0.10],
                    )[0],
                    "installation_date": random_install_date(),
                }
            )
            counter += 1

    return assignments


def _resolve_or_create_homeport_installation(homeport_str: str) -> Installation:
    """
    Matches a hull's homeport string (e.g., 'NS Norfolk, VA', 'NB San Diego, CA')
    to an existing Installation or creates a Fleet_Homeport Installation if needed.
    """
    homeport_map = {
        "NS Norfolk, VA": ("NAVSTA-NORVA", "Naval Station Norfolk", "Norfolk", "VA", "CONUS_East", "N62688"),
        "NB San Diego, CA": ("NBSD-CA", "Naval Base San Diego", "San Diego", "CA", "CONUS_West", "N63406"),
        "NAS Whidbey Island, WA": ("NAS-WHIDBEY", "Naval Air Station Whidbey Island", "Oak Harbor", "WA", "CONUS_West", "N00620"),
        "NS Everett, WA": ("NAVSTA-EVRT", "Naval Station Everett", "Everett", "WA", "CONUS_West", "N68967"),
        "SUBASE New London, CT": ("SUBASE-NLON", "Naval Submarine Base New London", "Groton", "CT", "CONUS_East", "N00129"),
        "JB Pearl Harbor-Hickam, HI": ("JBPHH-HI", "Joint Base Pearl Harbor-Hickam", "Pearl Harbor", "HI", "INDOPACOM", "N62813"),
        "NAS Jacksonville, FL": ("NAS-JAX", "Naval Air Station Jacksonville", "Jacksonville", "FL", "CONUS_South", "N00207"),
    }
    code, full_name, city, state, region, default_dodaac = homeport_map.get(
        homeport_str,
        (
            homeport_str[:12].upper().replace(" ", "-"),
            homeport_str,
            homeport_str.split(",")[0].strip(),
            homeport_str.split(",")[-1].strip() if "," in homeport_str else "",
            "CONUS",
            f"N{abs(hash(homeport_str)) % 90000 + 10000}",
        ),
    )

    inst = Installation.objects.filter(installation_code=code).first()
    if not inst:
        inst = Installation.objects.filter(name__icontains=city).first()
    if not inst:
        inst, _ = Installation.objects.update_or_create(
            dodaac=default_dodaac,
            defaults={
                "installation_code": code,
                "name": full_name,
                "city": city,
                "state_or_country": state,
                "location_display": f"{city}, {state}".strip(", "),
                "region": region,
                "mission_category": MissionCategory.FLEET_HOMEPORT,
            },
        )
    return inst


def _map_capability_to_readiness(op_status: str) -> str:
    if op_status == CapabilityStatus.FOC:
        return ReadinessLevel.FULLY_MISSION_CAPABLE
    if op_status == CapabilityStatus.IOC:
        return ReadinessLevel.PARTIALLY_MISSION_CAPABLE
    return ReadinessLevel.DEPOT_LEVEL_MAINTENANCE


@transaction.atomic
def sync_loadouts_to_db(assignments: list) -> dict:
    """
    Populates MissionRole, AssetModel (MewSystem), NavalHull (and linked MilitaryUnit),
    Asset, and HullSystemAssignment tables from the reference catalogs and generated assignments.
    """
    # 1. Sync Mission Roles
    role_map = {}
    for role_data in MISSION_ROLES:
        compatible_domains = [
            dom
            for dom, role_ids in DOMAIN_ROLES.items()
            if role_data["role_id"] in role_ids
        ]
        role_obj, _ = MissionRole.objects.update_or_create(
            role_id=role_data["role_id"],
            defaults={
                "role_code": role_data["role_code"],
                "role_name": role_data["role_name"],
                "description": role_data["description"],
                "compatible_domains": compatible_domains,
            },
        )
        role_map[role_data["role_id"]] = role_obj

    # 2. Sync MEW Systems into AssetModel
    system_map = {}
    for sys_data in MEW_SYSTEMS:
        sys_obj, _ = AssetModel.objects.update_or_create(
            system_id=sys_data["system_id"],
            defaults={
                "nomenclature": sys_data["nomenclature"],
                "common_name": sys_data["common_name"],
                "ew_category": sys_data["ew_category"],
                "asset_class": sys_data["ew_category"],
                "compatible_domains": sys_data["compatible_domains"],
                "manufacturer": sys_data["manufacturer"],
                "national_stock_number": sys_data["nsn"],
            },
        )
        system_map[sys_data["system_id"]] = sys_obj

    # 3. Sync Naval Hulls & corresponding MilitaryUnits
    hull_map = {}
    for idx, hull_data in enumerate(NAVAL_HULLS, start=1):
        homeport_inst = _resolve_or_create_homeport_installation(hull_data["homeport"])
        uic = f"N20{idx:03d}"

        unit_obj, _ = MilitaryUnit.objects.update_or_create(
            uic=uic,
            defaults={
                "name": f"{hull_data['vessel_name']} ({hull_data['hull_number']})",
                "installation": homeport_inst,
                "command_type": f"Naval_{hull_data['domain']}_Force",
            },
        )

        hull_obj, _ = NavalHull.objects.update_or_create(
            hull_id=hull_data["hull_id"],
            defaults={
                "hull_number": hull_data["hull_number"],
                "vessel_name": hull_data["vessel_name"],
                "ship_class": hull_data["ship_class"],
                "domain": hull_data["domain"],
                "homeport": hull_data["homeport"],
                "homeport_installation": homeport_inst,
                "unit": unit_obj,
            },
        )
        hull_map[hull_data["hull_id"]] = hull_obj

    # 4. Sync Hull-System Loadout Assignments & Asset instances
    assignment_count = 0
    for asgn in assignments:
        hull_obj = hull_map[asgn["hull_id"]]
        sys_obj = system_map[asgn["system_id"]]
        role_obj = role_map[asgn["role_id"]]
        install_dt = date.fromisoformat(asgn["installation_date"])
        readiness = _map_capability_to_readiness(asgn["operational_status"])
        serial_num = f"SN-{hull_obj.hull_number}-{sys_obj.system_id}-{asgn['assignment_id']}"

        asset_obj, _ = Asset.objects.update_or_create(
            assignment_id=asgn["assignment_id"],
            defaults={
                "asset_model": sys_obj,
                "serial_number": serial_num,
                "assigned_unit": hull_obj.unit,
                "current_location": hull_obj.homeport_installation,
                "hull": hull_obj,
                "mission_role": role_obj,
                "quantity_installed": asgn["quantity_installed"],
                "operational_status": asgn["operational_status"],
                "installation_date": install_dt,
                "readiness_status": readiness,
                "engine_hours": round(random.uniform(250.0, 4500.0), 2),
            },
        )

        HullSystemAssignment.objects.update_or_create(
            assignment_id=asgn["assignment_id"],
            defaults={
                "hull": hull_obj,
                "system": sys_obj,
                "mission_role": role_obj,
                "asset": asset_obj,
                "hull_number": asgn["hull_number"],
                "vessel_name": asgn["vessel_name"],
                "ship_class": asgn["ship_class"],
                "domain": asgn["domain"],
                "homeport": asgn["homeport"],
                "nomenclature": asgn["nomenclature"],
                "common_name": asgn["common_name"],
                "ew_category": asgn["ew_category"],
                "quantity_installed": asgn["quantity_installed"],
                "role_code": asgn["role_code"],
                "role_name": asgn["role_name"],
                "operational_status": asgn["operational_status"],
                "installation_date": install_dt,
            },
        )
        assignment_count += 1

    return {
        "hulls": len(hull_map),
        "systems": len(system_map),
        "roles": len(role_map),
        "assignments": assignment_count,
    }


def export_loadouts(
    dataset: list,
    csv_filename: str = "mew_hull_system_loadouts.csv",
    json_filename: str = "mew_relational_dataset.json",
):
    if not dataset:
        return

    with open(csv_filename, mode="w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=dataset[0].keys())
        writer.writeheader()
        writer.writerows(dataset)

    normalized_db = {
        "naval_hulls": NAVAL_HULLS,
        "mew_systems": [
            {k: v for k, v in s.items() if k != "nsn"} for s in MEW_SYSTEMS
        ],
        "mission_roles": MISSION_ROLES,
        "hull_system_assignments": dataset,
    }
    with open(json_filename, mode="w", encoding="utf-8") as f:
        json.dump(normalized_db, f, indent=2)

    print(f"Generated {len(dataset)} validated hull-to-system assignments.")
    print(f"Saved: {csv_filename} and {json_filename}")


class Command(BaseCommand):
    help = "Generates Naval Hulls, MEW Systems, Mission Roles, and Hull-to-System Loadouts, seeds DB, and exports CSV/JSON."

    def add_arguments(self, parser):
        parser.add_argument(
            "--csv-file",
            type=str,
            default="mew_hull_system_loadouts.csv",
            help="Path for exported CSV file (default: mew_hull_system_loadouts.csv)",
        )
        parser.add_argument(
            "--json-file",
            type=str,
            default="mew_relational_dataset.json",
            help="Path for exported JSON file (default: mew_relational_dataset.json)",
        )
        parser.add_argument(
            "--no-export",
            action="store_true",
            help="Skip exporting CSV and JSON files to disk",
        )

    def handle(self, *args, **options):
        dataset = generate_assignments()
        counts = sync_loadouts_to_db(dataset)

        self.stdout.write(
            self.style.SUCCESS(
                f"[+] Synced {counts['hulls']} Naval Hulls, {counts['systems']} MEW Systems, "
                f"{counts['roles']} Mission Roles, and {counts['assignments']} Hull-System Loadouts to DB."
            )
        )

        if not options["no_export"]:
            export_loadouts(
                dataset,
                csv_filename=options["csv_file"],
                json_filename=options["json_file"],
            )

        calculate_fleet_reliability_metrics()
        generate_daily_readiness_snapshots()
        self.stdout.write(
            self.style.SUCCESS(
                "[+] Recalculated fleet reliability metrics and daily readiness snapshots."
            )
        )
