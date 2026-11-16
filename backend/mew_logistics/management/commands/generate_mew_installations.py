import csv
import json
import random
from datetime import date
from django.core.management.base import BaseCommand
from django.db import transaction
from faker import Faker

from mew_logistics.models import Installation, MilitaryUnit

BASE_MEW_INSTALLATIONS = [
    {
        "installation_code": "NSWC-CRANE",
        "installation_name": "Naval Surface Warfare Center Crane Division",
        "city": "Crane",
        "state_or_country": "IN",
        "region": "CONUS_Midwest",
        "latitude": 38.8912,
        "longitude": -86.8378,
        "mission_category": "R&D_Warfare_Center",
        "primary_ew_focus": "Surface Ship EW (AN/SLQ-32 SEWIP) & Off-Board Decoys",
        "dsn_prefix": "482",
        "max_ew_billets": 450,
    },
    {
        "installation_code": "NSWC-DD",
        "installation_name": "Naval Surface Warfare Center Dahlgren Division",
        "city": "Dahlgren",
        "state_or_country": "VA",
        "region": "CONUS_East",
        "latitude": 38.3489,
        "longitude": -77.0436,
        "mission_category": "R&D_Warfare_Center",
        "primary_ew_focus": "Combat System Integration & Real-Time Spectrum Operations",
        "dsn_prefix": "249",
        "max_ew_billets": 380,
    },
    {
        "installation_code": "NUWC-NPT",
        "installation_name": "Naval Undersea Warfare Center Division Newport",
        "city": "Newport",
        "state_or_country": "RI",
        "region": "CONUS_East",
        "latitude": 41.5295,
        "longitude": -71.3128,
        "mission_category": "R&D_Warfare_Center",
        "primary_ew_focus": "Submarine & UUV Electromagnetic Warfare (AN/BLQ-10)",
        "dsn_prefix": "841",
        "max_ew_billets": 310,
    },
    {
        "installation_code": "NIWC-PAC",
        "installation_name": "Naval Information Warfare Center Pacific",
        "city": "San Diego",
        "state_or_country": "CA",
        "region": "CONUS_West",
        "latitude": 32.7042,
        "longitude": -117.2415,
        "mission_category": "R&D_Warfare_Center",
        "primary_ew_focus": "Maritime C4ISR, SIGINT & Spectrum Dominance",
        "dsn_prefix": "553",
        "max_ew_billets": 420,
    },
    {
        "installation_code": "NRL-DC",
        "installation_name": "U.S. Naval Research Laboratory (Tactical EW Div)",
        "city": "Washington",
        "state_or_country": "DC",
        "region": "CONUS_East",
        "latitude": 38.8239,
        "longitude": -77.0247,
        "mission_category": "R&D_Warfare_Center",
        "primary_ew_focus": "Advanced Radar Deception & Active Off-Board Decoys",
        "dsn_prefix": "297",
        "max_ew_billets": 210,
    },
    {
        "installation_code": "NAWCAD-PAX",
        "installation_name": "Naval Air Warfare Center Aircraft Division Patuxent River",
        "city": "Patuxent River",
        "state_or_country": "MD",
        "region": "CONUS_East",
        "latitude": 38.2856,
        "longitude": -76.4117,
        "mission_category": "R&D_Warfare_Center",
        "primary_ew_focus": "EMP Survivability & Maritime Aircraft Spectrum Testing",
        "dsn_prefix": "342",
        "max_ew_billets": 275,
    },
    {
        "installation_code": "NAVIFOR-SUF",
        "installation_name": "Naval Information Forces (NAVIFOR)",
        "city": "Suffolk",
        "state_or_country": "VA",
        "region": "CONUS_East",
        "latitude": 36.7282,
        "longitude": -76.5836,
        "mission_category": "Operational_Command",
        "primary_ew_focus": "Information Warfare Readiness & Maritime EW Doctrine",
        "dsn_prefix": "564",
        "max_ew_billets": 190,
    },
    {
        "installation_code": "NIOC-MD",
        "installation_name": "Naval Information Operations Command Maryland",
        "city": "Fort Meade",
        "state_or_country": "MD",
        "region": "CONUS_East",
        "latitude": 39.1084,
        "longitude": -76.7713,
        "mission_category": "Cryptologic_Operations",
        "primary_ew_focus": "Fleet Cryptologic Direct Support & Threat Libraries",
        "dsn_prefix": "622",
        "max_ew_billets": 340,
    },
    {
        "installation_code": "IWTC-CORRY",
        "installation_name": "Information Warfare Training Command Corry Station",
        "city": "Pensacola",
        "state_or_country": "FL",
        "region": "CONUS_South",
        "latitude": 30.4053,
        "longitude": -87.2839,
        "mission_category": "Training_Schoolhouse",
        "primary_ew_focus": "CTT Enlisted Technical & Surface EW Operator Training",
        "dsn_prefix": "459",
        "max_ew_billets": 260,
    },
    {
        "installation_code": "NBVC-PTMUGU",
        "installation_name": "Naval Base Ventura County Point Mugu Sea Range",
        "city": "Point Mugu",
        "state_or_country": "CA",
        "region": "CONUS_West",
        "latitude": 34.1194,
        "longitude": -119.1197,
        "mission_category": "Test_Evaluation_Range",
        "primary_ew_focus": "Over-Water EW Live-Fire & Airborne EA Testing",
        "dsn_prefix": "351",
        "max_ew_billets": 230,
    },
    {
        "installation_code": "PMRF-HI",
        "installation_name": "Pacific Missile Range Facility Barking Sands",
        "city": "Kekaha",
        "state_or_country": "HI",
        "region": "INDOPACOM",
        "latitude": 22.0228,
        "longitude": -159.7850,
        "mission_category": "Test_Evaluation_Range",
        "primary_ew_focus": "Open-Ocean Integrated Air/Surface EW & Missile Defense",
        "dsn_prefix": "421",
        "max_ew_billets": 180,
    },
    {
        "installation_code": "NSWC-PHD",
        "installation_name": "NSWC Port Hueneme Division",
        "city": "Port Hueneme",
        "state_or_country": "CA",
        "region": "CONUS_West",
        "latitude": 34.1558,
        "longitude": -119.2028,
        "mission_category": "Test_Evaluation_Range",
        "primary_ew_focus": "Underway In-Service Engineering & Ship Self-Defense T&E",
        "dsn_prefix": "551",
        "max_ew_billets": 295,
    },
    {
        "installation_code": "NAVSTA-NORVA",
        "installation_name": "Naval Station Norfolk",
        "city": "Norfolk",
        "state_or_country": "VA",
        "region": "CONUS_East",
        "latitude": 36.9461,
        "longitude": -76.3133,
        "mission_category": "Fleet_Homeport",
        "primary_ew_focus": "Atlantic Fleet Surface Combatants (DDG/CG SEWIP)",
        "dsn_prefix": "564",
        "max_ew_billets": 620,
    },
    {
        "installation_code": "NBSD-CA",
        "installation_name": "Naval Base San Diego",
        "city": "San Diego",
        "state_or_country": "CA",
        "region": "CONUS_West",
        "latitude": 32.6836,
        "longitude": -117.1292,
        "mission_category": "Fleet_Homeport",
        "primary_ew_focus": "Pacific Fleet Surface Combatants (DDG/CG SEWIP)",
        "dsn_prefix": "526",
        "max_ew_billets": 590,
    },
    {
        "installation_code": "NAVSTA-MAY",
        "installation_name": "Naval Station Mayport",
        "city": "Jacksonville",
        "state_or_country": "FL",
        "region": "CONUS_South",
        "latitude": 30.3917,
        "longitude": -81.4167,
        "mission_category": "Fleet_Homeport",
        "primary_ew_focus": "Fourth Fleet / Atlantic Destroyer Squadrons",
        "dsn_prefix": "960",
        "max_ew_billets": 310,
    },
    {
        "installation_code": "NAS-WHIDBEY",
        "installation_name": "Naval Air Station Whidbey Island",
        "city": "Oak Harbor",
        "state_or_country": "WA",
        "region": "CONUS_West",
        "latitude": 48.3517,
        "longitude": -122.6558,
        "mission_category": "Fleet_Homeport",
        "primary_ew_focus": "Airborne Electronic Attack (EA-18G Growler Squadrons)",
        "dsn_prefix": "820",
        "max_ew_billets": 480,
    },
    {
        "installation_code": "CFAY-YOKO",
        "installation_name": "Commander, Fleet Activities Yokosuka",
        "city": "Yokosuka",
        "state_or_country": "Japan",
        "region": "INDOPACOM_FDNF",
        "latitude": 35.2814,
        "longitude": 139.6722,
        "mission_category": "Fleet_Homeport",
        "primary_ew_focus": "Forward-Deployed Seventh Fleet Surface & Carrier EW",
        "dsn_prefix": "243",
        "max_ew_billets": 410,
    },
]


def generate_installations_dataset(seed: int = 2026) -> list:
    """Enriches the master MEW installations list with synthetic operational readiness metrics."""
    fake = Faker("en_US")
    Faker.seed(seed)
    random.seed(seed)

    enriched = list()
    readiness_levels = (
        "C1_Fully_Mission_Capable",
        "C2_Substantially_Mission_Capable",
        "C3_Marginally_Mission_Capable",
    )
    readiness_weights = (0.72, 0.22, 0.06)

    for idx, base in enumerate(BASE_MEW_INSTALLATIONS, start=1):
        record = dict(base)
        record["installation_id"] = f"INST-{idx:03d}"
        record["uic"] = f"N{fake.numerify(text='#####')}"
        record["location_display"] = f"{base['city']}, {base['state_or_country']}"
        record["scif_accredited"] = True
        record["rf_anechoic_chamber"] = base["mission_category"] in (
            "R&D_Warfare_Center",
            "Test_Evaluation_Range",
        )
        record["readiness_rating"] = random.choices(
            readiness_levels, weights=readiness_weights, k=1
        ).pop()
        record["last_inspection_date"] = fake.date_between(
            start_date="-1y", end_date="today"
        ).isoformat()
        enriched.append(record)

    return enriched


def export_installations(
    data: list,
    json_file: str = "mew_installations.json",
    csv_file: str = "mew_installations.csv",
):
    if not data:
        return

    with open(json_file, mode="w", encoding="utf-8") as jf:
        json.dump(data, jf, indent=2)
    print(f"[+] Exported {len(data)} MEW installations to {json_file}")

    headers = list(next(iter(data)).keys())
    with open(csv_file, mode="w", newline="", encoding="utf-8") as cf:
        writer = csv.DictWriter(cf, fieldnames=headers)
        writer.writeheader()
        writer.writerows(data)
    print(f"[+] Exported {len(data)} MEW installations to {csv_file}")


@transaction.atomic
def sync_installations_to_db(installations_data: list) -> dict:
    """
    Upserts the generated MEW installations (and a default MilitaryUnit per installation)
    into the database, returning a map of installation_code -> (Installation, MilitaryUnit).
    """
    inst_map = {}
    for item in installations_data:
        inst, _ = Installation.objects.update_or_create(
            installation_code=item["installation_code"],
            defaults={
                "installation_id": item["installation_id"],
                "name": item["installation_name"],
                "dodaac": item["uic"],
                "city": item["city"],
                "state_or_country": item["state_or_country"],
                "location_display": item["location_display"],
                "region": item["region"],
                "latitude": item["latitude"],
                "longitude": item["longitude"],
                "mission_category": item["mission_category"],
                "primary_ew_focus": item["primary_ew_focus"],
                "dsn_prefix": item["dsn_prefix"],
                "max_ew_billets": item["max_ew_billets"],
                "scif_accredited": item["scif_accredited"],
                "rf_anechoic_chamber": item["rf_anechoic_chamber"],
                "readiness_rating": item["readiness_rating"],
                "last_inspection_date": date.fromisoformat(item["last_inspection_date"]),
            },
        )
        unit, _ = MilitaryUnit.objects.update_or_create(
            uic=item["uic"],
            defaults={
                "name": f"{item['installation_name']} EW Detachment",
                "installation": inst,
                "command_type": item["mission_category"],
            },
        )
        inst_map[item["installation_code"]] = (inst, unit)
    return inst_map


class Command(BaseCommand):
    help = "Generates synthetic MEW Installations dataset, syncs to database, and exports JSON/CSV."

    def add_arguments(self, parser):
        parser.add_argument(
            "--json-file",
            type=str,
            default="mew_installations.json",
            help="Path for exported JSON file (default: mew_installations.json)",
        )
        parser.add_argument(
            "--csv-file",
            type=str,
            default="mew_installations.csv",
            help="Path for exported CSV file (default: mew_installations.csv)",
        )
        parser.add_argument(
            "--no-export",
            action="store_true",
            help="Skip exporting JSON and CSV files to disk",
        )

    def handle(self, *args, **options):
        installations_data = generate_installations_dataset()
        inst_map = sync_installations_to_db(installations_data)
        self.stdout.write(
            self.style.SUCCESS(
                f"[+] Synced {len(inst_map)} MEW installations and units to the database."
            )
        )
        if not options["no_export"]:
            export_installations(
                installations_data,
                json_file=options["json_file"],
                csv_file=options["csv_file"],
            )
