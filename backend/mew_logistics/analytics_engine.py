import math
from django.db.models import Sum

from .models import (
    Asset,
    AssetModel,
    Installation,
    InventoryBalance,
    MaterialCatalog,
    Personnel,
    Requisition,
    WorkOrder,
)


def _haversine_nm(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """Calculates great-circle distance in Nautical Miles (NM) between two coordinates."""
    r_nm = 3440.065
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dlambda = math.radians(lon2 - lon1)
    a = (
        math.sin(dphi / 2.0) ** 2
        + math.cos(phi1) * math.cos(phi2) * math.sin(dlambda / 2.0) ** 2
    )
    return round(2 * r_nm * math.atan2(math.sqrt(a), math.sqrt(1 - a)), 1)


def compute_mew_analytics_payload(access_ctx=None) -> dict:
    """
    Computes the 5 core analytical dimensions defined in MEW_ANALYTICS.md:
      1. Ao = MTBM / (MTBM + MDT) Readiness Decomposition (MTTR + Logistics Delay Time)
      2. Hierarchical Bill of Materials (BOM) & Weakest-Link Impact Tree
      3. CASREP Impact Scoring & Supply Chain Bottleneck Funnel
      4. Personnel-to-Equipment NEC Cross-Referencing (Single-Point-of-Failure Staffing Gaps)
      5. Predictive Depletion / Lead-Time Forecasting & Geospatial Proximity Depot Routing
    """
    assets_qs = Asset.objects.select_related(
        "asset_model", "hull", "current_location", "assigned_unit", "mission_role"
    ).prefetch_related("work_orders__parts_used__material", "requisition_set__material")
    if access_ctx:
        assets_qs = access_ctx.scope_assets(assets_qs)
    assets = list(assets_qs)

    components_qs = MaterialCatalog.objects.select_related("parent_lru").all()
    if access_ctx:
        components_qs = access_ctx.scope_components(components_qs)
    components = list(components_qs)

    balances = list(
        InventoryBalance.objects.select_related("installation", "material").all()
    )
    work_orders = list(
        WorkOrder.objects.select_related("asset__asset_model", "hull", "technician")
        .prefetch_related("parts_used__material")
        .all()
    )
    requisitions = list(
        Requisition.objects.select_related("material", "requesting_unit", "tied_to_asset").all()
    )
    personnel_list = list(Personnel.objects.select_related("installation", "unit").all())
    installations = list(Installation.objects.all())

    # Map stock totals and depot locations per component_id
    stock_by_comp = {}
    reorder_by_comp = {}
    depot_stock_locations = {}
    for b in balances:
        cid = b.material.component_id or b.material.nsn
        stock_by_comp[cid] = stock_by_comp.get(cid, 0) + b.quantity_on_hand
        reorder_by_comp[cid] = reorder_by_comp.get(cid, 0) + b.reorder_point
        if b.quantity_on_hand > b.safety_stock and b.installation:
            depot_stock_locations.setdefault(cid, []).append(
                {
                    "installation_code": b.installation.installation_code or b.installation.dodaac,
                    "name": b.installation.name,
                    "lat": float(b.installation.latitude or 38.8912),
                    "lon": float(b.installation.longitude or -86.8378),
                    "qty_available": b.quantity_on_hand,
                }
            )

    # =========================================================================
    # 1. Ao = MTBM / (MTBM + MDT) DECOMPOSITION BY MEW SYSTEM
    # =========================================================================
    systems_map = {}
    for a in assets:
        sid = a.asset_model.system_id or a.asset_model.nomenclature
        entry = systems_map.setdefault(
            sid,
            {
                "system_id": sid,
                "nomenclature": a.asset_model.nomenclature,
                "common_name": a.asset_model.common_name,
                "ew_category": a.asset_model.ew_category,
                "domain": a.hull.domain if a.hull else "Surface",
                "installed_count": 0,
                "fmc_count": 0,
                "pmc_count": 0,
                "nmc_count": 0,
                "mtbm_sum": 0.0,
                "mttr_sum": 0.0,
                "ldt_sum": 0.0,
                "operating_hours_sum": 0.0,
            },
        )
        entry["installed_count"] += 1
        if a.readiness_status == "FMC":
            entry["fmc_count"] += 1
        elif a.readiness_status == "PMC":
            entry["pmc_count"] += 1
        else:
            entry["nmc_count"] += 1

        mtbm = float(a.mtbf_hours or 1450.0)
        mttr = float(a.mttr_hours or 14.0)
        # Logistics Delay Time (LDT) factored when waiting on supply (NMCS / open requisitions)
        open_reqs = [r for r in a.requisition_set.all() if r.status != "Received"]
        ldt = (
            sum(float(r.material.lead_time_days) * 4.5 for r in open_reqs)
            if open_reqs
            else (18.0 if a.readiness_status == "NMCS" else 4.0)
        )
        entry["mtbm_sum"] += mtbm
        entry["mttr_sum"] += mttr
        entry["ldt_sum"] += ldt
        entry["operating_hours_sum"] += float(a.engine_hours or 500.0)

    swap_c_profiles = {
        "SYS-101": {"power_kw": 42.0, "cooling_gpm": 18.5, "weight_lbs": 6400, "vswr": 1.28, "thermal_pct": 64},
        "SYS-102": {"power_kw": 118.5, "cooling_gpm": 44.0, "weight_lbs": 11200, "vswr": 1.42, "thermal_pct": 78},
        "SYS-103": {"power_kw": 24.0, "cooling_gpm": 10.0, "weight_lbs": 3100, "vswr": 1.19, "thermal_pct": 52},
        "SYS-104": {"power_kw": 14.5, "cooling_gpm": 0.0, "weight_lbs": 4850, "vswr": 1.15, "thermal_pct": 48},
        "SYS-105": {"power_kw": 65.0, "cooling_gpm": 28.0, "weight_lbs": 5200, "vswr": 1.12, "thermal_pct": 71},
        "SYS-106": {"power_kw": 29.0, "cooling_gpm": 12.0, "weight_lbs": 2950, "vswr": 1.22, "thermal_pct": 59},
        "SYS-107": {"power_kw": 8.5, "cooling_gpm": 0.0, "weight_lbs": 1150, "vswr": 1.18, "thermal_pct": 45},
        "SYS-108": {"power_kw": 36.0, "cooling_gpm": 14.5, "weight_lbs": 1420, "vswr": 1.39, "thermal_pct": 81},
        "SYS-109": {"power_kw": 16.0, "cooling_gpm": 6.5, "weight_lbs": 680, "vswr": 1.21, "thermal_pct": 54},
        "SYS-110": {"power_kw": 12.0, "cooling_gpm": 4.0, "weight_lbs": 420, "vswr": 1.16, "thermal_pct": 50},
    }

    ao_system_breakdown = []
    for sid, d in sorted(systems_map.items()):
        n = max(1, d["installed_count"])
        avg_mtbm = round(d["mtbm_sum"] / n, 1)
        avg_mttr = round(d["mttr_sum"] / n, 1)
        avg_ldt = round(d["ldt_sum"] / n, 1)
        avg_mdt = round(avg_mttr + avg_ldt, 1)
        formula_ao = round((avg_mtbm / (avg_mtbm + avg_mdt)) * 100.0, 1) if (avg_mtbm + avg_mdt) > 0 else 100.0
        swap_c = swap_c_profiles.get(
            d["system_id"],
            {"power_kw": 30.0, "cooling_gpm": 12.0, "weight_lbs": 3500, "vswr": 1.24, "thermal_pct": 62},
        )
        # Degraded/NMC systems show elevated VSWR & thermal load
        vswr_adj = round(swap_c["vswr"] + (0.28 if d["nmc_count"] > 0 else 0.0), 2)
        thermal_adj = min(96, swap_c["thermal_pct"] + (11 if d["nmc_count"] > 0 else 0))

        ao_system_breakdown.append(
            {
                "system_id": d["system_id"],
                "nomenclature": d["nomenclature"],
                "common_name": d["common_name"],
                "ew_category": d["ew_category"],
                "domain": d["domain"],
                "installed_count": d["installed_count"],
                "fmc_count": d["fmc_count"],
                "pmc_count": d["pmc_count"],
                "nmc_count": d["nmc_count"],
                "mtbm_hours": avg_mtbm,
                "mttr_hours": avg_mttr,
                "ldt_hours": avg_ldt,
                "mdt_hours": avg_mdt,
                "computed_ao_pct": formula_ao,
                "total_operating_hours": round(d["operating_hours_sum"], 0),
                "swap_c_power_kw": swap_c["power_kw"],
                "swap_c_cooling_gpm": swap_c["cooling_gpm"],
                "swap_c_weight_lbs": swap_c["weight_lbs"],
                "vswr_ratio": vswr_adj,
                "thermal_load_pct": thermal_adj,
            }
        )

    # =========================================================================
    # 2 & 3 & 5. HIERARCHICAL BOM, CASREP IMPACT SCORING & PREDICTIVE DEPLETION
    # =========================================================================
    active_faults_by_comp = {}
    casrep_faults_by_comp = {}
    for wo in work_orders:
        if wo.status not in ("CLOSED", "Closed"):
            for pt in wo.parts_used.all():
                cid = pt.material.component_id or pt.material.nsn
                active_faults_by_comp[cid] = active_faults_by_comp.get(cid, 0) + pt.quantity
                if wo.is_grounding_fault:
                    casrep_faults_by_comp[cid] = casrep_faults_by_comp.get(cid, 0) + pt.quantity

    component_analytics = []
    for comp in components:
        cid = comp.component_id or comp.nsn
        total_stock = stock_by_comp.get(cid, 0)
        total_rop = reorder_by_comp.get(cid, 10)
        active_faults = active_faults_by_comp.get(cid, 0)
        casrep_faults = casrep_faults_by_comp.get(cid, 0)

        # Calculate fleet OPTEMPO demand & Predictive Depletion
        compat_sys_hours = sum(
            s["total_operating_hours"]
            for s in ao_system_breakdown
            if s["system_id"] in (comp.compatible_system_ids or [])
        ) or 3500.0
        mtbf_est = max(500.0, float(comp.expected_mtbf_hours or 2500.0))
        monthly_demand_rate = round(max(0.8, (compat_sys_hours / mtbf_est) * 1.8 + active_faults * 0.9), 2)
        daily_burn = monthly_demand_rate / 30.0
        days_to_depletion = int(round(total_stock / daily_burn)) if daily_burn > 0 else 180
        days_to_depletion = min(180, max(3, days_to_depletion))

        lead_time = int(comp.lead_time_days or 14)
        # Stockout risk when lead_time exceeds or approaches days_to_depletion
        if days_to_depletion <= lead_time:
            depletion_status = "CRITICAL_STOCKOUT_RISK"
        elif days_to_depletion <= int(lead_time * 1.6):
            depletion_status = "REORDER_WARNING"
        else:
            depletion_status = "SUSTAINABLE"

        # CASREP Impact Score (0-100): weights mission criticality, active CASREPs, lead time, and stock posture
        impact_score = 15
        if comp.critical_item:
            impact_score += 25
        if casrep_faults > 0:
            impact_score += min(35, casrep_faults * 20)
        elif active_faults > 0:
            impact_score += 15
        if lead_time >= 30:
            impact_score += 15
        elif lead_time >= 20:
            impact_score += 8
        if total_stock <= total_rop or depletion_status != "SUSTAINABLE":
            impact_score += 15
        impact_score = min(100, impact_score)

        component_analytics.append(
            {
                "component_id": cid,
                "nsn": comp.nsn,
                "nomenclature": comp.nomenclature,
                "component_category": comp.component_category,
                "manufacturer": comp.manufacturer,
                "cage_code": comp.cage_code,
                "unit_price": float(comp.unit_price),
                "critical_item": comp.critical_item,
                "parent_lru_id": comp.parent_lru.component_id if comp.parent_lru else None,
                "parent_lru_name": comp.parent_lru.nomenclature if comp.parent_lru else "Top-Level System LRU",
                "compatible_system_ids": comp.compatible_system_ids or [],
                "total_fleet_stock": total_stock,
                "total_reorder_point": total_rop,
                "active_faults": active_faults,
                "casrep_grounding_faults": casrep_faults,
                "lead_time_days": lead_time,
                "expected_mtbf_hours": mtbf_est,
                "monthly_demand_rate": monthly_demand_rate,
                "days_to_depletion": days_to_depletion,
                "depletion_status": depletion_status,
                "casrep_impact_score": impact_score,
            }
        )

    component_analytics.sort(key=lambda x: (-x["casrep_impact_score"], x["days_to_depletion"]))

    # Supply Chain Bottleneck Funnel
    total_reqs = len(requisitions)
    open_reqs_list = [r for r in requisitions if r.status != "Received"]
    urgent_reqs_list = [r for r in open_reqs_list if r.ipd_code == "01" or r.priority == 1]
    dlr_carcass_pending = sum(
        1
        for wo in work_orders
        if wo.status not in ("CLOSED", "Closed")
        for pt in wo.parts_used.all()
        if pt.requires_dlr_turn_in
    )
    supply_bottleneck_funnel = [
        {"stage": "1. Installed Fleet MEW Loadouts", "count": len(assets), "severity": "NOMINAL"},
        {"stage": "2. Active Maintenance Actions (MAFs)", "count": sum(1 for w in work_orders if w.status not in ("CLOSED", "Closed")), "severity": "INFO"},
        {"stage": "3. Open MILSTRIP Requisitions", "count": len(open_reqs_list), "severity": "WARN"},
        {"stage": "4. DLR Carcass Turn-Ins Pending Zeroization", "count": dlr_carcass_pending, "severity": "WARN"},
        {"stage": "5. IPD-01 CASREP Grounding Bottlenecks", "count": len(urgent_reqs_list), "severity": "CRIT"},
    ]

    # =========================================================================
    # 4. PERSONNEL-TO-EQUIPMENT NEC CROSS-REFERENCING (SPOF STAFFING GAPS)
    # =========================================================================
    nec_requirements_catalog = [
        {
            "nec_code": "NEC C16A",
            "Target_System": "SYS-101 / SYS-103",
            "system_name": "AN/SLQ-32(V)6 SEWIP Block 2",
            "match_key": "C16A",
            "required_per_hull": 2,
        },
        {
            "nec_code": "NEC C17A",
            "Target_System": "SYS-102",
            "system_name": "AN/SLQ-32(V)7 SEWIP Block 3 (AESA EA)",
            "match_key": "C17A",
            "required_per_hull": 2,
        },
        {
            "nec_code": "NEC C19A",
            "Target_System": "SYS-106 / SYS-107",
            "system_name": "AN/BLQ-10 Submarine EW & Senior Emitter Analysis",
            "match_key": "C19A",
            "required_per_hull": 1,
        },
        {
            "nec_code": "NEC 8341",
            "Target_System": "SYS-108",
            "system_name": "AN/ALQ-249 Next Gen Jammer Pod Calibration",
            "match_key": "8341",
            "required_per_hull": 3,
        },
        {
            "nec_code": "GS-0855 / 1440",
            "Target_System": "SYS-105 / SYS-110",
            "system_name": "AN/SEQ-4 ODIN Directed Energy & NEMESIS Swarm",
            "match_key": "0855",
            "required_per_hull": 2,
        },
    ]

    nec_staffing_matrix = []
    for req_spec in nec_requirements_catalog:
        qualified = [
            p
            for p in personnel_list
            if req_spec["match_key"] in (p.specialty_code or "")
        ]
        deployable = [
            p
            for p in qualified
            if p.operational_status != "Leave" and p.is_medically_deployable
        ]
        active_billets_needed = max(6, len(assets) // 2)
        coverage_pct = round(min(100.0, (len(deployable) / active_billets_needed) * 100.0), 1)
        if coverage_pct < 65.0 or len(deployable) <= 3:
            spof_risk = "CRITICAL_SPOF_GAP"
        elif coverage_pct < 85.0:
            spof_risk = "MARGINAL_COVERAGE"
        else:
            spof_risk = "RESILIENT"

        nec_staffing_matrix.append(
            {
                "nec_code": req_spec["nec_code"],
                "system_id": req_spec["Target_System"],
                "system_name": req_spec["system_name"],
                "total_qualified": len(qualified),
                "deployable_count": len(deployable),
                "required_billets": active_billets_needed,
                "coverage_pct": coverage_pct,
                "spof_risk": spof_risk,
            }
        )

    # =========================================================================
    # 5B. GEOSPATIAL FLEET & NEAREST-DEPOT PROXIMITY ROUTING
    # =========================================================================
    geospatial_nodes = []
    for inst in installations:
        if inst.latitude is not None and inst.longitude is not None:
            geospatial_nodes.append(
                {
                    "code": inst.installation_code or inst.dodaac,
                    "name": inst.name,
                    "mission_category": inst.mission_category,
                    "lat": float(inst.latitude),
                    "lon": float(inst.longitude),
                    "readiness_rating": inst.readiness_rating or "C1",
                    "personnel_count": sum(
                        1 for p in personnel_list if p.installation_id == inst.id
                    ),
                }
            )

    proximity_depot_routes = []
    for a in assets:
        if a.readiness_status == "FMC":
            continue
        loc = a.current_location
        hull_lat = float(loc.latitude) if loc and loc.latitude is not None else 36.9461
        hull_lon = float(loc.longitude) if loc and loc.longitude is not None else -76.3133

        # Determine needed part from open work orders or requisitions
        needed_comp = None
        open_wos = [w for w in a.work_orders.all() if w.status not in ("CLOSED", "Closed")]
        if open_wos and open_wos[0].parts_used.exists():
            needed_comp = open_wos[0].parts_used.first().material
        elif a.requisition_set.exists():
            needed_comp = a.requisition_set.first().material

        cid = (needed_comp.component_id if needed_comp else "CMP-201") or "CMP-201"
        comp_name = needed_comp.nomenclature if needed_comp else "Low-Band RF Receiver/Digitizer LRU"

        candidates = depot_stock_locations.get(cid, [])
        best_depot = None
        best_dist = 99999.0
        for dep in candidates:
            if loc and dep["installation_code"] == (loc.installation_code or loc.dodaac):
                continue
            dist = _haversine_nm(hull_lat, hull_lon, dep["lat"], dep["lon"])
            if dist < best_dist:
                best_dist = dist
                best_depot = dep

        if best_depot:
            proximity_depot_routes.append(
                {
                    "hull_number": "REDACTED" if (access_ctx and access_ctx.should_mask_platform_identity()) else (a.hull.hull_number if a.hull else "SHORE"),
                    "vessel_name": "REDACTED PLATFORM" if (access_ctx and access_ctx.should_mask_platform_identity()) else (a.hull.vessel_name if a.hull else a.assigned_unit.name),
                    "readiness_status": a.readiness_status,
                    "system_nomenclature": a.asset_model.nomenclature,
                    "origin_homeport": loc.installation_code if loc else "NAVSTA-NORVA",
                    "origin_lat": None if (access_ctx and access_ctx.should_mask_geospatial()) else hull_lat,
                    "origin_lon": None if (access_ctx and access_ctx.should_mask_geospatial()) else hull_lon,
                    "needed_component_id": cid,
                    "needed_component_name": comp_name,
                    "nearest_depot_code": best_depot["installation_code"],
                    "nearest_depot_name": best_depot["name"],
                    "depot_qty_available": best_depot["qty_available"],
                    "distance_nm": best_dist,
                    "estimated_transit_hours": round(max(4.0, best_dist / 180.0 + 3.5), 1),
                }
            )

    return {
        "ao_system_breakdown": ao_system_breakdown,
        "component_analytics": component_analytics,
        "supply_bottleneck_funnel": supply_bottleneck_funnel,
        "nec_staffing_matrix": nec_staffing_matrix,
        "geospatial_nodes": geospatial_nodes,
        "proximity_depot_routes": proximity_depot_routes,
    }
