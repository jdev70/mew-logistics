import csv
import json
import random
from datetime import date, timedelta
from decimal import Decimal
from django.core.management.base import BaseCommand
from django.db import transaction
from django.utils import timezone

from mew_logistics.models import (
    Asset,
    AssetModel,
    AssetStatusLog,
    Installation,
    InventoryBalance,
    InventoryTransaction,
    MaterialCatalog,
    MilitaryUnit,
    Personnel,
    ReadinessLevel,
    Requisition,
    TransactionType,
    UnitReadinessSnapshot,
    WorkOrder,
    WorkOrderPart,
)
from mew_logistics.management.commands.generate_mew_installations import (
    generate_installations_dataset,
    sync_installations_to_db,
)
from mew_logistics.management.commands.generate_mew_loadouts import (
    generate_assignments,
    sync_loadouts_to_db,
)
from mew_logistics.management.commands.generate_mew_personnel import (
    generate_mew_personnel,
    sync_personnel_to_db,
)
from mew_logistics.tasks import (
    calculate_fleet_reliability_metrics,
    generate_daily_readiness_snapshots,
)

# Reference Catalog: MEW Line Replaceable Units (LRUs), Shop Replaceable Units (SRUs) & Components
MEW_COMPONENTS = [
    {
        "component_id": "CMP-201",
        "nsn": "5865-01-684-1101",
        "nomenclature": "Low-Band RF Receiver/Digitizer LRU",
        "part_number": "LM-SEWIP-RX201",
        "cage_code": "03538",
        "manufacturer": "Lockheed Martin",
        "component_category": "RF Receiver / Exciter LRU",
        "repairability_code": "Depot_Repairable_LRU",
        "unit_of_issue": "EA",
        "unit_price": "48500.00",
        "critical_item": True,
        "lead_time_days": 21,
        "expected_mtbf_hours": 3200.0,
        "compatible_system_ids": ["SYS-101", "SYS-102"],
        "typical_fault": "Low-Band RF Receiver/Digitizer LRU phase noise & local oscillator unlock fault",
    },
    {
        "component_id": "CMP-202",
        "nsn": "5865-01-684-1102",
        "nomenclature": "High-Band Digital Receiver/Exciter (DREX) Assembly",
        "part_number": "LM-SEWIP-DREX2",
        "cage_code": "03538",
        "manufacturer": "Lockheed Martin",
        "component_category": "RF Receiver / Exciter LRU",
        "repairability_code": "Depot_Repairable_LRU",
        "unit_of_issue": "EA",
        "unit_price": "64200.00",
        "critical_item": True,
        "lead_time_days": 28,
        "expected_mtbf_hours": 2800.0,
        "compatible_system_ids": ["SYS-101", "SYS-102", "SYS-103"],
        "typical_fault": "High-Band DREX Assembly ADC channel calibration drift exceeding BIT threshold",
    },
    {
        "component_id": "CMP-203",
        "nsn": "5865-01-691-2201",
        "nomenclature": "GaN Solid-State AESA Transmit/Receive (T/R) Module",
        "part_number": "NG-BLK3-GAN88",
        "cage_code": "76823",
        "manufacturer": "Northrop Grumman",
        "component_category": "Solid-State EA Amplifier Module",
        "repairability_code": "Depot_Repairable_LRU",
        "unit_of_issue": "EA",
        "unit_price": "112000.00",
        "critical_item": True,
        "lead_time_days": 45,
        "expected_mtbf_hours": 2400.0,
        "compatible_system_ids": ["SYS-102", "SYS-108"],
        "typical_fault": "GaN AESA T/R Sub-Array Module VSWR fault and high-power amplifier thermal trip",
    },
    {
        "component_id": "CMP-204",
        "nsn": "5865-01-691-2204",
        "nomenclature": "Digital Radio Frequency Memory (DRFM) Coherent Jammer Card",
        "part_number": "NG-DRFM-VPX6",
        "cage_code": "76823",
        "manufacturer": "Northrop Grumman",
        "component_category": "DRFM / Signal Processing Card",
        "repairability_code": "Depot_Repairable_LRU",
        "unit_of_issue": "EA",
        "unit_price": "78900.00",
        "critical_item": True,
        "lead_time_days": 30,
        "expected_mtbf_hours": 2650.0,
        "compatible_system_ids": ["SYS-102", "SYS-108", "SYS-110"],
        "typical_fault": "DRFM Coherent Jammer Card FPGA false-target synthesis memory parity error",
    },
    {
        "component_id": "CMP-205",
        "nsn": "5865-01-677-3301",
        "nomenclature": "Specific Emitter Identification (SEI) Signal Processor Blade",
        "part_number": "LM-SEI-VPX301",
        "cage_code": "03538",
        "manufacturer": "Lockheed Martin",
        "component_category": "DRFM / Signal Processing Card",
        "repairability_code": "Organizational_LRU",
        "unit_of_issue": "EA",
        "unit_price": "39400.00",
        "critical_item": True,
        "lead_time_days": 14,
        "expected_mtbf_hours": 3600.0,
        "compatible_system_ids": ["SYS-101", "SYS-103", "SYS-106", "SYS-109"],
        "typical_fault": "SEI Signal Processor Blade pulse descriptor word (PDW) bus timeout",
    },
    {
        "component_id": "CMP-206",
        "nsn": "5865-01-540-4401",
        "nomenclature": "Mk 234 Mod 2 Nulka Active Electronic Decoy Canister",
        "part_number": "BAE-NULKA-234M2",
        "cage_code": "89954",
        "manufacturer": "BAE Systems",
        "component_category": "Off-Board Electronic Decoy Round",
        "repairability_code": "Expendable_Round",
        "unit_of_issue": "EA",
        "unit_price": "145000.00",
        "critical_item": True,
        "lead_time_days": 60,
        "expected_mtbf_hours": 5000.0,
        "compatible_system_ids": ["SYS-104"],
        "typical_fault": "Mk 234 Mod 2 Nulka Decoy Canister flight control battery & cartridge shelf-life fault",
    },
    {
        "component_id": "CMP-207",
        "nsn": "5865-01-540-4408",
        "nomenclature": "Mk 53 Decoy Launcher Fire Control Interface Unit (FCIU)",
        "part_number": "BAE-MK53-FCIU9",
        "cage_code": "89954",
        "manufacturer": "BAE Systems",
        "component_category": "Launcher Fire Control Electronics",
        "repairability_code": "Organizational_LRU",
        "unit_of_issue": "EA",
        "unit_price": "28750.00",
        "critical_item": False,
        "lead_time_days": 14,
        "expected_mtbf_hours": 4100.0,
        "compatible_system_ids": ["SYS-104"],
        "typical_fault": "Mk 53 Launcher Fire Control Interface Unit ignition interlock relay fault",
    },
    {
        "component_id": "CMP-208",
        "nsn": "5865-01-702-5501",
        "nomenclature": "ODIN Multi-Spectral Laser Dazzler Optical Bench Assembly",
        "part_number": "NSWCDD-SEQ4-OB1",
        "cage_code": "00178",
        "manufacturer": "NSWC Dahlgren",
        "component_category": "Directed Energy / Electro-Optical LRU",
        "repairability_code": "Depot_Repairable_LRU",
        "unit_of_issue": "EA",
        "unit_price": "185000.00",
        "critical_item": True,
        "lead_time_days": 45,
        "expected_mtbf_hours": 1950.0,
        "compatible_system_ids": ["SYS-105"],
        "typical_fault": "ODIN Multi-Spectral Laser Dazzler beam combiner alignment & diode bank degradation",
    },
    {
        "component_id": "CMP-209",
        "nsn": "5865-01-702-5509",
        "nomenclature": "Electro-Optical Gimbal Stabilization & Tracking Servo LRU",
        "part_number": "NSWCDD-SEQ4-GMB",
        "cage_code": "00178",
        "manufacturer": "NSWC Dahlgren",
        "component_category": "Directed Energy / Electro-Optical LRU",
        "repairability_code": "Organizational_LRU",
        "unit_of_issue": "EA",
        "unit_price": "42300.00",
        "critical_item": False,
        "lead_time_days": 21,
        "expected_mtbf_hours": 3100.0,
        "compatible_system_ids": ["SYS-105"],
        "typical_fault": "Electro-Optical Gimbal azimuth resolver feedback oscillation under sea state 4",
    },
    {
        "component_id": "CMP-210",
        "nsn": "5865-01-655-6601",
        "nomenclature": "Submarine Photonics/EW Mast Broadband RF Downconverter",
        "part_number": "LM-BLQ10-RFD6",
        "cage_code": "03538",
        "manufacturer": "Lockheed Martin",
        "component_category": "Subsurface EW Mast & RF LRU",
        "repairability_code": "Depot_Repairable_LRU",
        "unit_of_issue": "EA",
        "unit_price": "91500.00",
        "critical_item": True,
        "lead_time_days": 35,
        "expected_mtbf_hours": 2900.0,
        "compatible_system_ids": ["SYS-106", "SYS-107"],
        "typical_fault": "Submarine EW Mast Broadband RF Downconverter low-noise amplifier (LNA) gain loss",
    },
    {
        "component_id": "CMP-211",
        "nsn": "5985-01-655-6612",
        "nomenclature": "OE-538 Mast Pressure-Hull Coaxial Feedthrough & Radome Seal Kit",
        "part_number": "L3H-OE538-SEAL",
        "cage_code": "55820",
        "manufacturer": "L3Harris",
        "component_category": "Subsurface EW Mast & RF LRU",
        "repairability_code": "Consumable_Kit",
        "unit_of_issue": "KT",
        "unit_price": "6850.00",
        "critical_item": False,
        "lead_time_days": 7,
        "expected_mtbf_hours": 1800.0,
        "compatible_system_ids": ["SYS-106", "SYS-107"],
        "typical_fault": "OE-538 Multifunction Mast coaxial pressure-hull feedthrough moisture ingress warning",
    },
    {
        "component_id": "CMP-212",
        "nsn": "6115-01-698-7701",
        "nomenclature": "ALQ-249 Ram Air Turbine (RAT) High-Capacity Power Generator",
        "part_number": "RTX-NGJ-RAT01",
        "cage_code": "07933",
        "manufacturer": "Raytheon",
        "component_category": "Airborne EA Pod Power & Cooling",
        "repairability_code": "Depot_Repairable_LRU",
        "unit_of_issue": "EA",
        "unit_price": "96000.00",
        "critical_item": True,
        "lead_time_days": 30,
        "expected_mtbf_hours": 2100.0,
        "compatible_system_ids": ["SYS-108"],
        "typical_fault": "ALQ-249 Pod Ram Air Turbine (RAT) variable-frequency generator voltage regulation fault",
    },
    {
        "component_id": "CMP-213",
        "nsn": "1660-01-698-7715",
        "nomenclature": "Airborne Pod Environmental Control & Liquid Cooling Pump Assembly",
        "part_number": "RTX-NGJ-ECP15",
        "cage_code": "07933",
        "manufacturer": "Raytheon",
        "component_category": "Airborne EA Pod Power & Cooling",
        "repairability_code": "Organizational_LRU",
        "unit_of_issue": "EA",
        "unit_price": "24600.00",
        "critical_item": False,
        "lead_time_days": 14,
        "expected_mtbf_hours": 2750.0,
        "compatible_system_ids": ["SYS-108", "SYS-109"],
        "typical_fault": "Airborne Pod polyalphaolefin (PAO) coolant loop flow sensor & pump cavitation fault",
    },
    {
        "component_id": "CMP-214",
        "nsn": "5985-01-662-8801",
        "nomenclature": "Wideband Interferometer Direction-Finding (DF) Antenna Array Element",
        "part_number": "NG-DF-ARR88",
        "cage_code": "76823",
        "manufacturer": "Northrop Grumman",
        "component_category": "RF Antenna & Waveguide Assembly",
        "repairability_code": "Organizational_LRU",
        "unit_of_issue": "EA",
        "unit_price": "19800.00",
        "critical_item": False,
        "lead_time_days": 10,
        "expected_mtbf_hours": 3800.0,
        "compatible_system_ids": ["SYS-101", "SYS-103", "SYS-109"],
        "typical_fault": "Wideband Interferometer DF Antenna element phase mismatch & salt-fog corrosion",
    },
    {
        "component_id": "CMP-215",
        "nsn": "5865-01-710-9901",
        "nomenclature": "NEMESIS Swarm Networked Transceiver & GPS-Denied Timing Module",
        "part_number": "NRL-NEM-CSAC9",
        "cage_code": "81995",
        "manufacturer": "Naval Research Lab",
        "component_category": "Distributed Signature Emulation LRU",
        "repairability_code": "Depot_Repairable_LRU",
        "unit_of_issue": "EA",
        "unit_price": "53000.00",
        "critical_item": True,
        "lead_time_days": 28,
        "expected_mtbf_hours": 2500.0,
        "compatible_system_ids": ["SYS-110"],
        "typical_fault": "NEMESIS Chip-Scale Atomic Clock (CSAC) cross-platform phase synchronization drift",
    },
    {
        "component_id": "CMP-216",
        "nsn": "6130-01-642-0016",
        "nomenclature": "High-Voltage Solid-State Power Conditioning & Inverter LRU",
        "part_number": "LM-HVPS-400HZ",
        "cage_code": "03538",
        "manufacturer": "Lockheed Martin",
        "component_category": "Power Conditioning & High-Voltage Supply",
        "repairability_code": "Organizational_LRU",
        "unit_of_issue": "EA",
        "unit_price": "31250.00",
        "critical_item": False,
        "lead_time_days": 14,
        "expected_mtbf_hours": 3400.0,
        "compatible_system_ids": ["SYS-101", "SYS-102", "SYS-105"],
        "typical_fault": "400Hz 3-Phase High-Voltage Power Conditioning LRU ripple voltage transient fault",
    },
]


def ensure_prerequisite_mew_data():
    """
    Ensures MEW Installations, Naval Hulls/Systems/Loadouts, and Personnel exist
    in the database before generating component inventory, requisitions, and work orders.
    """
    if not Installation.objects.filter(installation_code__isnull=False).exists():
        inst_data = generate_installations_dataset()
        inst_map = sync_installations_to_db(inst_data)
    else:
        inst_map = {
            inst.installation_code: (inst, inst.units.first())
            for inst in Installation.objects.filter(installation_code__isnull=False)
        }

    if not Asset.objects.filter(assignment_id__isnull=False).exists():
        assignments = generate_assignments()
        sync_loadouts_to_db(assignments)

    if not Personnel.objects.filter(personnel_id__startswith="MEW-PER-").exists():
        inst_data = generate_installations_dataset()
        roster = generate_mew_personnel(inst_data, num_records=150)
        sync_personnel_to_db(roster, inst_map=inst_map)


@transaction.atomic
def sync_components_and_simulate_logistics(
    seed: int = 2026, clear_existing_logistics: bool = True
) -> dict:
    """
    1. Syncs MEW_COMPONENTS into MaterialCatalog and links compatible AssetModels.
    2. Provisions initial InventoryBalance stock across installations via InventoryTransaction.execute().
    3. Simulates realistic Requisitions, WorkOrders, and WorkOrderParts across deployed MEW Assets,
       exercising all Django signals (receipts, deductions, NMCS/NMCM/PMC/FMC transitions).
    4. Backfills 30 days of UnitReadinessSnapshot trends and AssetStatusLog history for analytics.
    """
    random.seed(seed)
    now = timezone.now()

    if clear_existing_logistics:
        InventoryTransaction.objects.all().delete()
        WorkOrderPart.objects.all().delete()
        Requisition.objects.all().delete()
        WorkOrder.objects.all().delete()
        InventoryBalance.objects.all().delete()

    # 1. Sync MaterialCatalog (MEW Components) & M2M links to AssetModel
    systems_by_id = {
        s.system_id: s
        for s in AssetModel.objects.filter(system_id__isnull=False)
    }
    components_by_id = {}
    components_by_sys_id = {}
    fault_by_component_id = {}

    for comp in MEW_COMPONENTS:
        mat_obj, _ = MaterialCatalog.objects.update_or_create(
            nsn=comp["nsn"],
            defaults={
                "component_id": comp["component_id"],
                "nomenclature": comp["nomenclature"],
                "part_number": comp["part_number"],
                "cage_code": comp["cage_code"],
                "manufacturer": comp["manufacturer"],
                "component_category": comp["component_category"],
                "repairability_code": comp["repairability_code"],
                "unit_of_issue": comp["unit_of_issue"],
                "unit_price": Decimal(comp["unit_price"]),
                "critical_item": comp["critical_item"],
                "lead_time_days": comp["lead_time_days"],
                "expected_mtbf_hours": comp["expected_mtbf_hours"],
                "compatible_system_ids": comp["compatible_system_ids"],
            },
        )
        matched_systems = [
            systems_by_id[sid]
            for sid in comp["compatible_system_ids"]
            if sid in systems_by_id
        ]
        if matched_systems:
            mat_obj.compatible_systems.set(matched_systems)

        components_by_id[comp["component_id"]] = mat_obj
        fault_by_component_id[comp["component_id"]] = comp["typical_fault"]
        for sid in comp["compatible_system_ids"]:
            components_by_sys_id.setdefault(sid, []).append(mat_obj)

    # 2. Provision Warehouse Inventory Balances at all active Installations
    installations = list(Installation.objects.all())
    all_personnel = list(Personnel.objects.all())
    logistics_clerks = [
        p
        for p in all_personnel
        if "Logistics" in p.title or "Supply" in p.role or p.rank in ("CTT1", "CTTC")
    ] or all_personnel

    balances_created = 0
    for inst in installations:
        is_depot = inst.mission_category == "R&D_Warfare_Center"
        for comp_id, mat_obj in components_by_id.items():
            reorder_pt = 8 if is_depot else 4
            safety_stk = 4 if is_depot else 2
            initial_qty = random.randint(18, 35) if is_depot else random.randint(8, 16)

            bal, _ = InventoryBalance.objects.update_or_create(
                installation=inst,
                material=mat_obj,
                defaults={
                    "quantity_on_hand": 0,
                    "reorder_point": reorder_pt,
                    "safety_stock": safety_stk,
                },
            )
            clerk = random.choice(logistics_clerks) if logistics_clerks else None
            InventoryTransaction.execute(
                balance_id=bal.id,
                transaction_type=TransactionType.RECEIPT,
                quantity=initial_qty,
                performed_by=clerk,
                ref_doc=f"INIT-PROV-{inst.installation_code or inst.dodaac}-{comp_id}",
            )
            balances_created += 1

    # 3. Generate Maintenance WorkOrders, WorkOrderParts, and Requisitions across MEW Assets
    assets = list(
        Asset.objects.select_related(
            "asset_model", "assigned_unit", "current_location", "hull"
        ).all()
    )

    wo_count = 0
    req_count = 0
    req_seq = 1

    for idx, asset in enumerate(assets):
        sys_id = asset.asset_model.system_id
        compat_parts = components_by_sys_id.get(sys_id) or list(
            components_by_id.values()
        )
        primary_part = random.choice(compat_parts)
        fault_desc = fault_by_component_id.get(
            primary_part.component_id,
            f"Scheduled LRU replacement and calibration for {primary_part.nomenclature}",
        )

        # Select a technician stationed at the asset's installation (fallback to any technician)
        local_techs = [
            p
            for p in all_personnel
            if p.installation_id == asset.current_location_id
        ] or all_personnel
        tech = random.choice(local_techs) if local_techs else None
        clerk = random.choice(local_techs) if local_techs else None

        # A) Every asset gets 1 to 2 historical CLOSED WorkOrders + RECEIVED Requisitions
        #    so the immutable ledger, MTBF/MTTR/Ao calculations, and repair histories are populated.
        num_historical = random.randint(1, 2)
        for h_idx in range(num_historical):
            hist_part = random.choice(compat_parts)
            hist_qty = random.randint(1, 2)
            days_ago_open = random.randint(12, 45)
            repair_duration_hours = random.uniform(6.0, 38.0)

            # First simulate receiving a supply replenishment for this part
            doc_num = f"REQ-MEW-{req_seq:04d}"
            req_seq += 1
            hist_req = Requisition.objects.create(
                document_number=doc_num,
                requesting_unit=asset.assigned_unit,
                material=hist_part,
                quantity=hist_qty + 1,
                priority=Requisition.UrgencyPriority.ROUTINE,
                tied_to_asset=asset,
                status="Submitted",
                estimated_delivery_date=(now - timedelta(days=days_ago_open)).date(),
            )
            hist_req._received_by = clerk
            hist_req.status = "Received"
            hist_req.save(update_fields=["status"])
            req_count += 1

            # Create and close the historical WorkOrder (triggers inventory deduction signal)
            hist_wo = WorkOrder.objects.create(
                asset=asset,
                technician=tech,
                description_of_fault=fault_by_component_id.get(
                    hist_part.component_id,
                    f"Replaced degraded {hist_part.nomenclature}",
                ),
                is_grounding_fault=True,
                status="Open",
            )
            WorkOrderPart.objects.create(
                work_order=hist_wo,
                material=hist_part,
                quantity=hist_qty,
            )
            hist_wo.status = "Closed"
            hist_wo.date_closed = now - timedelta(
                days=days_ago_open - 2
            )
            hist_wo.save(update_fields=["status", "date_closed"])
            wo_count += 1

            # Backdate the AssetStatusLog entries produced by this historical cycle
            # so calculate_fleet_reliability_metrics() computes realistic MTBF, MTTR, and Ao.
            recent_logs = list(
                AssetStatusLog.objects.filter(asset=asset).order_by("-created_at")[:2]
            )
            if len(recent_logs) >= 2:
                up_log, down_log = recent_logs[0], recent_logs[1]
                down_time = now - timedelta(days=days_ago_open)
                up_time = down_time + timedelta(hours=repair_duration_hours)
                AssetStatusLog.objects.filter(pk=down_log.pk).update(
                    created_at=down_time
                )
                AssetStatusLog.objects.filter(pk=up_log.pk).update(created_at=up_time)

        # Backdate the initial 'NEW -> FMC' AssetStatusLog entry so total uptime spans 60-120 days
        first_log = (
            AssetStatusLog.objects.filter(asset=asset, old_status__isnull=True)
            .order_by("created_at")
            .first()
        )
        if first_log:
            AssetStatusLog.objects.filter(pk=first_log.pk).update(
                created_at=now - timedelta(days=random.randint(60, 120))
            )

        # B) Determine current live operational state for each asset to showcase dashboard views:
        #    - ~65% Fully Mission Capable (FMC)
        #    - ~15% Partially Mission Capable (PMC - open minor fault)
        #    - ~10% Non-Mission Capable Supply (NMCS - grounding fault + open Urgent Requisition)
        #    - ~10% Non-Mission Capable Maintenance (NMCM - grounding fault, parts on hand)
        bucket = idx % 10
        if bucket in (0,):  # NMCS: Grounding Work Order + Pending Urgent Requisition
            open_wo = WorkOrder.objects.create(
                asset=asset,
                technician=tech,
                description_of_fault=f"[CASREP / NMCS] {fault_desc}",
                is_grounding_fault=True,
                status="Awaiting Parts",
            )
            WorkOrderPart.objects.create(
                work_order=open_wo,
                material=primary_part,
                quantity=1,
            )
            doc_num = f"REQ-MEW-{req_seq:04d}"
            req_seq += 1
            Requisition.objects.create(
                document_number=doc_num,
                requesting_unit=asset.assigned_unit,
                material=primary_part,
                quantity=random.randint(1, 2),
                priority=Requisition.UrgencyPriority.URGENT,
                tied_to_asset=asset,
                work_order=open_wo,
                status="In Transit",
                estimated_delivery_date=(
                    now + timedelta(days=random.randint(2, 7))
                ).date(),
            )
            wo_count += 1
            req_count += 1

        elif bucket in (1,):  # NMCM: Grounding Work Order with parts on hand
            open_wo = WorkOrder.objects.create(
                asset=asset,
                technician=tech,
                description_of_fault=f"[TECH ASSIST / NMCM] {fault_desc}",
                is_grounding_fault=True,
                status="In Progress",
            )
            WorkOrderPart.objects.create(
                work_order=open_wo,
                material=primary_part,
                quantity=1,
            )
            wo_count += 1

        elif bucket in (2, 3):  # PMC: Non-grounding degraded fault + Priority Requisition
            open_wo = WorkOrder.objects.create(
                asset=asset,
                technician=tech,
                description_of_fault=f"[DEGRADED / PMC] {fault_desc}",
                is_grounding_fault=False,
                status="Open",
            )
            WorkOrderPart.objects.create(
                work_order=open_wo,
                material=primary_part,
                quantity=1,
            )
            doc_num = f"REQ-MEW-{req_seq:04d}"
            req_seq += 1
            Requisition.objects.create(
                document_number=doc_num,
                requesting_unit=asset.assigned_unit,
                material=primary_part,
                quantity=1,
                priority=Requisition.UrgencyPriority.PRIORITY,
                tied_to_asset=asset,
                work_order=open_wo,
                status="Submitted",
                estimated_delivery_date=(
                    now + timedelta(days=random.randint(5, 14))
                ).date(),
            )
            wo_count += 1
            req_count += 1

    # 4. Run reliability & readiness calculations and backfill 30-day readiness trend snapshots
    calculate_fleet_reliability_metrics()
    generate_daily_readiness_snapshots()
    _backfill_unit_readiness_trends(days=30)

    return {
        "components": len(components_by_id),
        "balances": balances_created,
        "work_orders": wo_count,
        "requisitions": req_count,
        "transactions": InventoryTransaction.objects.count(),
    }


def _backfill_unit_readiness_trends(days: int = 30):
    """
    Populates historical UnitReadinessSnapshot entries over the past `days`
    so the dashboard's 30-day readiness trend charts have rich time-series data.
    """
    today = timezone.now().date()
    units = list(MilitaryUnit.objects.all())

    for unit in units:
        today_snap = UnitReadinessSnapshot.objects.filter(
            unit=unit, snapshot_date=today
        ).first()
        if not today_snap or today_snap.total_assets == 0:
            continue

        total_assets = today_snap.total_assets
        total_pers = today_snap.total_personnel

        for offset in range(1, days):
            snap_date = today - timedelta(days=offset)
            # Simulate realistic slight historical variance around current readiness
            nmc = min(
                total_assets,
                max(0, today_snap.nmc_assets + random.choice([-1, 0, 0, 1])),
            )
            pmc = min(
                total_assets - nmc,
                max(0, today_snap.pmc_assets + random.choice([-1, 0, 0, 1])),
            )
            fmc = max(0, total_assets - nmc - pmc)
            deployable_pers = max(
                0,
                min(
                    total_pers,
                    today_snap.deployable_personnel + random.randint(-2, 2),
                ),
            )

            UnitReadinessSnapshot.objects.update_or_create(
                unit=unit,
                snapshot_date=snap_date,
                defaults={
                    "total_personnel": total_pers,
                    "deployable_personnel": deployable_pers,
                    "total_assets": total_assets,
                    "fmc_assets": fmc,
                    "pmc_assets": pmc,
                    "nmc_assets": nmc,
                },
            )


def export_components_and_logistics(
    csv_filename: str = "mew_components_catalog.csv",
    json_filename: str = "mew_components_logistics_dataset.json",
):
    # Export MEW Components catalog to CSV
    csv_rows = [
        {
            **{k: v for k, v in comp.items() if k != "compatible_system_ids"},
            "compatible_system_ids": ",".join(comp["compatible_system_ids"]),
        }
        for comp in MEW_COMPONENTS
    ]
    with open(csv_filename, mode="w", newline="", encoding="utf-8") as cf:
        writer = csv.DictWriter(cf, fieldnames=csv_rows[0].keys())
        writer.writeheader()
        writer.writerows(csv_rows)

    # Export full logistics & analytics snapshot to JSON
    balances_data = [
        {
            "installation_code": b.installation.installation_code or b.installation.dodaac,
            "installation_name": b.installation.name,
            "component_id": b.material.component_id,
            "nsn": b.material.nsn,
            "nomenclature": b.material.nomenclature,
            "quantity_on_hand": b.quantity_on_hand,
            "reorder_point": b.reorder_point,
            "safety_stock": b.safety_stock,
            "needs_replenishment": b.needs_replenishment(),
        }
        for b in InventoryBalance.objects.select_related("installation", "material")
    ]

    requisitions_data = [
        {
            "document_number": r.document_number,
            "requesting_unit": r.requesting_unit.name,
            "nsn": r.material.nsn,
            "nomenclature": r.material.nomenclature,
            "quantity": r.quantity,
            "priority": r.get_priority_display(),
            "status": r.status,
            "tied_to_asset": r.tied_to_asset.serial_number if r.tied_to_asset else None,
            "estimated_delivery_date": (
                r.estimated_delivery_date.isoformat()
                if r.estimated_delivery_date
                else None
            ),
        }
        for r in Requisition.objects.select_related(
            "requesting_unit", "material", "tied_to_asset"
        )
    ]

    work_orders_data = [
        {
            "work_order_id": str(wo.id),
            "asset_serial": wo.asset.serial_number,
            "system_nomenclature": wo.asset.asset_model.nomenclature,
            "technician": str(wo.technician) if wo.technician else "Unassigned",
            "description_of_fault": wo.description_of_fault,
            "is_grounding_fault": wo.is_grounding_fault,
            "status": wo.status,
            "date_opened": wo.date_opened.isoformat(),
            "date_closed": wo.date_closed.isoformat() if wo.date_closed else None,
        }
        for wo in WorkOrder.objects.select_related(
            "asset__asset_model", "technician"
        )
    ]

    payload = {
        "mew_components": MEW_COMPONENTS,
        "inventory_balances": balances_data,
        "requisitions": requisitions_data,
        "work_orders": work_orders_data,
    }
    with open(json_filename, mode="w", encoding="utf-8") as jf:
        json.dump(payload, jf, indent=2)

    print(f"[+] Exported {len(MEW_COMPONENTS)} MEW components to {csv_filename}")
    print(f"[+] Exported full MEW logistics dataset to {json_filename}")


class Command(BaseCommand):
    help = (
        "Generates MEW Components/LRUs, Warehouse Inventory, Requisitions, Work Orders, "
        "Immutable Ledger Transactions, and Readiness/Reliability Analytics."
    )

    def add_arguments(self, parser):
        parser.add_argument(
            "--csv-file",
            type=str,
            default="mew_components_catalog.csv",
            help="Path for exported components CSV file (default: mew_components_catalog.csv)",
        )
        parser.add_argument(
            "--json-file",
            type=str,
            default="mew_components_logistics_dataset.json",
            help="Path for exported logistics JSON file (default: mew_components_logistics_dataset.json)",
        )
        parser.add_argument(
            "--no-export",
            action="store_true",
            help="Skip exporting CSV and JSON files to disk",
        )

    def handle(self, *args, **options):
        ensure_prerequisite_mew_data()
        stats = sync_components_and_simulate_logistics()

        self.stdout.write(
            self.style.SUCCESS(
                f"[+] Synced {stats['components']} MEW Components, {stats['balances']} Warehouse Balances, "
                f"{stats['work_orders']} Work Orders, {stats['requisitions']} Requisitions, and "
                f"{stats['transactions']} Immutable Ledger Transactions."
            )
        )

        if not options["no_export"]:
            export_components_and_logistics(
                csv_filename=options["csv_file"],
                json_filename=options["json_file"],
            )
