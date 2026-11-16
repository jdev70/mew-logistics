import csv
import json
import os
import random
from datetime import date, timedelta
from django.core.management.base import BaseCommand
from django.db import transaction
from faker import Faker

from mew_logistics.models import OperationalStatus, Personnel
from mew_logistics.management.commands.generate_mew_installations import (
    export_installations,
    generate_installations_dataset,
    sync_installations_to_db,
)
from mew_logistics.tasks import generate_daily_readiness_snapshots

PERSONNEL_PROFILES = [
    {
        "category": "Active_Duty_Enlisted",
        "paygrade": "E-5",
        "rank": "CTT2",
        "rank_full": "Cryptologic Technician (Technical) Second Class",
        "specialty_code": "NEC C16A (AN/SLQ-32(V)6 Maintainer)",
        "title": "Surface EW Systems Technician",
        "role": "Shipboard RF Receiver/Exciter Diagnostics & LRU Replacement",
        "clearance": "TS/SCI",
        "eligible_missions": (
            "Fleet_Homeport",
            "Training_Schoolhouse",
            "Cryptologic_Operations",
        ),
    },
    {
        "category": "Active_Duty_Enlisted",
        "paygrade": "E-6",
        "rank": "CTT1",
        "rank_full": "Cryptologic Technician (Technical) First Class",
        "specialty_code": "NEC C17A (SEWIP Block 3 Tech)",
        "title": "EW Work Center Supervisor / Master Training Specialist",
        "role": "Planned Maintenance System (PMS) Oversight & Tactical Emitter Analysis",
        "clearance": "TS/SCI",
        "eligible_missions": (
            "Fleet_Homeport",
            "Training_Schoolhouse",
            "Cryptologic_Operations",
        ),
    },
    {
        "category": "Active_Duty_Enlisted",
        "paygrade": "E-7",
        "rank": "CTTC",
        "rank_full": "Chief Cryptologic Technician (Technical)",
        "specialty_code": "NEC C19A (Senior EW Analyst)",
        "title": "EW Division Leading Chief Petty Officer (LCPO)",
        "role": "Fleet Electromagnetic Readiness & Direct Support Team Lead",
        "clearance": "TS/SCI",
        "eligible_missions": (
            "Fleet_Homeport",
            "Operational_Command",
            "Cryptologic_Operations",
        ),
    },
    {
        "category": "Active_Duty_Enlisted",
        "paygrade": "E-5",
        "rank": "AT2",
        "rank_full": "Aviation Electronics Technician Second Class",
        "specialty_code": "NEC 8341 (ALQ-99/ALQ-249 Pod Tech)",
        "title": "Airborne Electronic Attack Avionics Technician",
        "role": "EA-18G Tactical Jamming System Calibration & Pod Maintenance",
        "clearance": "Secret",
        "eligible_missions": ("Fleet_Homeport", "Test_Evaluation_Range"),
    },
    {
        "category": "Active_Duty_Officer",
        "paygrade": "O-3",
        "rank": "LT",
        "rank_full": "Lieutenant",
        "specialty_code": "1810 (Cryptologic Warfare Officer)",
        "title": "Electronic Warfare Officer (EWO)",
        "role": "Tactical Spectrum Employment & Threat Library Integration",
        "clearance": "TS/SCI",
        "eligible_missions": (
            "Fleet_Homeport",
            "Operational_Command",
            "Cryptologic_Operations",
        ),
    },
    {
        "category": "Active_Duty_Officer",
        "paygrade": "O-3",
        "rank": "LT",
        "rank_full": "Lieutenant",
        "specialty_code": "1320 (Naval Flight Officer - EA-18G)",
        "title": "Electronic Warfare Mission Commander (ECMO)",
        "role": "Airborne Maritime Electromagnetic Suppression & SEAD Tactics",
        "clearance": "TS/SCI",
        "eligible_missions": ("Fleet_Homeport", "Test_Evaluation_Range"),
    },
    {
        "category": "Active_Duty_Officer",
        "paygrade": "O-4",
        "rank": "LCDR",
        "rank_full": "Lieutenant Commander",
        "specialty_code": "1440 (Engineering Duty Officer)",
        "title": "SEWIP Fleet Modernization & T&E Officer",
        "role": "Shipyard Alteration Scheduling & Sea Range Qualification Trials",
        "clearance": "TS/SCI",
        "eligible_missions": (
            "R&D_Warfare_Center",
            "Test_Evaluation_Range",
            "Fleet_Homeport",
        ),
    },
    {
        "category": "Active_Duty_Officer",
        "paygrade": "O-5",
        "rank": "CDR",
        "rank_full": "Commander",
        "specialty_code": "1810 (Information Warfare)",
        "title": "Director of Maritime Electromagnetic Maneuver Warfare",
        "role": "Theater Spectrum Operations & Multi-Domain EW Policy",
        "clearance": "TS/SCI",
        "eligible_missions": (
            "Operational_Command",
            "Cryptologic_Operations",
            "R&D_Warfare_Center",
        ),
    },
    {
        "category": "DoD_Civilian",
        "paygrade": "GS-13",
        "rank": "CIV (GS-13)",
        "rank_full": "Civilian Electronics Engineer (GS-13)",
        "specialty_code": "GS-0855 (Electronics Engineering)",
        "title": "Lead RF / GaN AESA Systems Engineer",
        "role": "High-Power Solid-State Amplifier & DRFM Hardware Engineering",
        "clearance": "TS/SCI",
        "eligible_missions": ("R&D_Warfare_Center", "Test_Evaluation_Range"),
    },
    {
        "category": "DoD_Civilian",
        "paygrade": "GS-14",
        "rank": "CIV (GS-14)",
        "rank_full": "Senior Scientist / Engineer (GS-14)",
        "specialty_code": "GS-1515 (Operations Research)",
        "title": "Principal Cognitive EW & Threat Modeling Lead",
        "role": "Anti-Ship Missile Seeker Simulation & Countermeasure Algorithm Design",
        "clearance": "TS/SCI",
        "eligible_missions": ("R&D_Warfare_Center",),
    },
    {
        "category": "DoD_Civilian",
        "paygrade": "GS-12",
        "rank": "CIV (GS-12)",
        "rank_full": "Civilian Logistics Specialist (GS-12)",
        "specialty_code": "GS-0346 (Logistics Management)",
        "title": "MEW Depot Provisioning & LRU Supply Chain Lead",
        "role": "NSN Spares Forecasting, MTBF Tracking & Depot Repair Turnaround",
        "clearance": "Secret",
        "eligible_missions": ("R&D_Warfare_Center", "Fleet_Homeport"),
    },
    {
        "category": "Defense_Contractor",
        "paygrade": "CTR",
        "rank": "CTR",
        "rank_full": "Defense Contractor (Field Service Engineer)",
        "specialty_code": "OEM Technical Representative",
        "title": "Senior SEWIP Field Service Representative",
        "role": "On-Site Pier-Side LRU Calibration & Firmware Baseline Upgrades",
        "clearance": "Secret",
        "eligible_missions": (
            "R&D_Warfare_Center",
            "Test_Evaluation_Range",
            "Fleet_Homeport",
        ),
    },
]


def load_installations(
    json_file: str = "mew_installations.json", export_if_missing: bool = True
) -> list:
    """Loads installations from JSON; generates them automatically if missing."""
    if not os.path.exists(json_file):
        print(f"[!] {json_file} not found. Generating installations dataset first...")
        data = generate_installations_dataset()
        if export_if_missing:
            export_installations(data, json_file=json_file)
        return data

    with open(json_file, mode="r", encoding="utf-8") as f:
        installations = json.load(f)
    print(f"[+] Loaded {len(installations)} installations from {json_file}")
    return installations


def generate_mew_personnel(
    installations: list, num_records: int = 150, seed: int = 404
) -> list:
    """Assigns synthetic personnel to loaded MEW installations based on mission compatibility."""
    fake = Faker("en_US")
    Faker.seed(seed)
    random.seed(seed)

    dataset = list()
    tour_lengths = (730, 1095, 1460)
    statuses = (
        "In_Garrison",
        "Underway_Afloat_Support",
        "Range_Test_Detachment",
        "TDY_Schoolhouse",
        "Leave",
    )
    status_weights = (0.65, 0.15, 0.10, 0.05, 0.05)

    seen_dod_ids = set()
    seen_emails = set()

    for idx in range(1, num_records + 1):
        # 1. Pick a role profile first
        profile = random.choice(PERSONNEL_PROFILES)

        # 2. Filter loaded installations that support this role's mission category
        matching_installations = [
            inst
            for inst in installations
            if inst["mission_category"] in profile["eligible_missions"]
        ]
        # Fallback to all installations if none match
        target_pool = matching_installations if matching_installations else installations
        assigned_base = random.choice(target_pool)

        # 3. Synthesize personal attributes via Faker
        first_name = fake.first_name()
        middle_initial = fake.random_uppercase_letter()
        last_name = fake.last_name()
        dod_id = fake.numerify(text="##########")
        while dod_id in seen_dod_ids:
            dod_id = fake.numerify(text="##########")
        seen_dod_ids.add(dod_id)

        dsn_phone = f"312-{assigned_base['dsn_prefix']}-{fake.numerify(text='####')}"

        # 4. Calculate assignment and rotation dates
        date_assigned = fake.date_between(start_date="-3y", end_date="-14d")
        if profile["category"] == "DoD_Civilian":
            projected_rotation_date = "N/A (Permanent Billet)"
        else:
            tour_days = random.choice(tour_lengths)
            projected_rotation_date = (
                date_assigned + timedelta(days=tour_days)
            ).isoformat()

        clean_last = last_name.lower().replace("'", "").replace("-", "")
        domain = (
            "us.navy.mil"
            if profile["category"] != "Defense_Contractor"
            else "ctr.navy.mil"
        )
        email_num = fake.random_int(min=1, max=99)
        email = f"{first_name.lower()}.{middle_initial.lower()}.{clean_last}{email_num}@{domain}"
        if email in seen_emails:
            email = f"{first_name.lower()}.{middle_initial.lower()}.{clean_last}{idx}@{domain}"
        seen_emails.add(email)

        op_status = random.choices(statuses, weights=status_weights, k=1).pop()

        record = {
            "personnel_id": f"MEW-PER-{idx:04d}",
            "dod_id": dod_id,
            "first_name": first_name,
            "middle_initial": middle_initial,
            "last_name": last_name,
            "personnel_category": profile["category"],
            "paygrade": profile["paygrade"],
            "rank": profile["rank"],
            "rank_full": profile["rank_full"],
            "specialty_code": profile["specialty_code"],
            "title": profile["title"],
            "role": profile["role"],
            "installation_id": assigned_base["installation_id"],
            "installation_code": assigned_base["installation_code"],
            "installation_deployed_at": assigned_base["installation_name"],
            "installation_location": assigned_base["location_display"],
            "installation_mission_category": assigned_base["mission_category"],
            "security_clearance": profile["clearance"],
            "operational_status": op_status,
            "date_assigned": date_assigned.isoformat(),
            "projected_rotation_date": projected_rotation_date,
            "dsn_phone": dsn_phone,
            "official_email": email,
        }
        dataset.append(record)

    return dataset


def export_to_json(data: list, json_file: str = "mew_personnel.json"):
    if not data:
        return
    with open(json_file, mode="w", encoding="utf-8") as jf:
        json.dump(data, jf, indent=2)
    print(f"[+] Exported {len(data)} MEW personnel records to {json_file}")


def export_to_csv(data: list, csv_file: str = "mew_personnel.csv"):
    if not data:
        return
    headers = list(next(iter(data)).keys())
    with open(csv_file, mode="w", newline="", encoding="utf-8") as cf:
        writer = csv.DictWriter(cf, fieldnames=headers)
        writer.writeheader()
        writer.writerows(data)
    print(f"[+] Exported {len(data)} MEW personnel records to {csv_file}")


@transaction.atomic
def sync_personnel_to_db(
    personnel_roster: list, inst_map: dict, clear_existing: bool = False
) -> int:
    """
    Syncs the generated MEW personnel roster into the Django database.
    Uses bulk operations for speed while supporting idempotent re-runs.
    """
    if clear_existing:
        Personnel.objects.filter(personnel_id__startswith="MEW-PER-").delete()

    existing_by_pid = {
        p.personnel_id: p
        for p in Personnel.objects.filter(
            personnel_id__in=[r["personnel_id"] for r in personnel_roster]
        )
    }

    to_create = []
    to_update = []

    for rec in personnel_roster:
        inst_tuple = inst_map.get(rec["installation_code"])
        inst_obj = inst_tuple[0] if inst_tuple else None
        unit_obj = inst_tuple[1] if inst_tuple else None

        op_status = rec["operational_status"]
        is_deployable = op_status != OperationalStatus.LEAVE

        fields = {
            "dod_id": rec["dod_id"],
            "first_name": rec["first_name"],
            "middle_initial": rec["middle_initial"],
            "last_name": rec["last_name"],
            "personnel_category": rec["personnel_category"],
            "paygrade": rec["paygrade"],
            "rank": rec["rank"],
            "rank_full": rec["rank_full"],
            "specialty_code": rec["specialty_code"],
            "mos": rec["specialty_code"][:50],
            "title": rec["title"],
            "role": rec["role"],
            "installation": inst_obj,
            "unit": unit_obj,
            "installation_code": rec["installation_code"],
            "installation_deployed_at": rec["installation_deployed_at"],
            "installation_location": rec["installation_location"],
            "installation_mission_category": rec["installation_mission_category"],
            "security_clearance": rec["security_clearance"],
            "operational_status": op_status,
            "deployment_status": op_status,
            "is_medically_deployable": is_deployable,
            "is_training_current": True,
            "date_assigned": date.fromisoformat(rec["date_assigned"]),
            "projected_rotation_date": rec["projected_rotation_date"],
            "dsn_phone": rec["dsn_phone"],
            "official_email": rec["official_email"],
        }

        if rec["personnel_id"] in existing_by_pid:
            obj = existing_by_pid[rec["personnel_id"]]
            for k, v in fields.items():
                setattr(obj, k, v)
            to_update.append(obj)
        else:
            to_create.append(Personnel(personnel_id=rec["personnel_id"], **fields))

    if to_create:
        Personnel.objects.bulk_create(to_create, batch_size=500)
    if to_update:
        Personnel.objects.bulk_update(
            to_update,
            fields=[
                "dod_id",
                "first_name",
                "middle_initial",
                "last_name",
                "personnel_category",
                "paygrade",
                "rank",
                "rank_full",
                "specialty_code",
                "mos",
                "title",
                "role",
                "installation",
                "unit",
                "installation_code",
                "installation_deployed_at",
                "installation_location",
                "installation_mission_category",
                "security_clearance",
                "operational_status",
                "deployment_status",
                "is_medically_deployable",
                "is_training_current",
                "date_assigned",
                "projected_rotation_date",
                "dsn_phone",
                "official_email",
            ],
            batch_size=500,
        )

    return len(to_create) + len(to_update)


class Command(BaseCommand):
    help = "Generates synthetic MEW Personnel and Installations, seeds the database, and exports JSON/CSV."

    def add_arguments(self, parser):
        parser.add_argument(
            "-n",
            "--count",
            type=int,
            default=1000,
            help="Number of synthetic MEW personnel records to generate (default: 1000)",
        )
        parser.add_argument(
            "--installations-json",
            type=str,
            default="mew_installations.json",
            help="Path to installations JSON file (default: mew_installations.json)",
        )
        parser.add_argument(
            "--json-file",
            type=str,
            default="mew_personnel.json",
            help="Path for exported personnel JSON file (default: mew_personnel.json)",
        )
        parser.add_argument(
            "--csv-file",
            type=str,
            default="mew_personnel.csv",
            help="Path for exported personnel CSV file (default: mew_personnel.csv)",
        )
        parser.add_argument(
            "--no-export",
            action="store_true",
            help="Skip exporting JSON and CSV files to disk",
        )
        parser.add_argument(
            "--clear",
            action="store_true",
            help="Clear existing MEW-PER-* records before seeding",
        )

    def handle(self, *args, **options):
        num_records = options["count"]
        export_files = not options["no_export"]

        installations = load_installations(
            json_file=options["installations_json"],
            export_if_missing=export_files,
        )
        inst_map = sync_installations_to_db(installations)
        self.stdout.write(
            self.style.SUCCESS(
                f"[+] Synced {len(inst_map)} MEW installations and units to the database."
            )
        )

        personnel_roster = generate_mew_personnel(
            installations=installations, num_records=num_records
        )
        total_synced = sync_personnel_to_db(
            personnel_roster,
            inst_map=inst_map,
            clear_existing=options["clear"],
        )
        self.stdout.write(
            self.style.SUCCESS(
                f"[+] Synced {total_synced} MEW personnel records to the database."
            )
        )

        if export_files:
            export_to_json(personnel_roster, json_file=options["json_file"])
            export_to_csv(personnel_roster, csv_file=options["csv_file"])

        # Recalculate unit readiness snapshots so personnel readiness percentages are up-to-date
        generate_daily_readiness_snapshots()
        self.stdout.write(
            self.style.SUCCESS("[+] Updated daily unit readiness snapshots.")
        )
