from rest_framework import serializers
from .models import (
    Asset,
    AssetModel,
    HullSystemAssignment,
    Installation,
    InventoryBalance,
    InventoryTransaction,
    MaterialCatalog,
    MilitaryUnit,
    MissionRole,
    NavalHull,
    Personnel,
    Requisition,
    SecurityAuditLog,
    UnitReadinessSnapshot,
    WorkOrder,
    WorkOrderPart,
)
from .rbac import resolve_access_context

# ==========================================
# 1. BASE & REFERENCE SERIALIZERS
# ==========================================

class InstallationBaseSerializer(serializers.ModelSerializer):
    class Meta:
        model = Installation
        fields = [
            'id',
            'installation_id',
            'installation_code',
            'name',
            'dodaac',
            'location_display',
            'region',
            'mission_category',
        ]


class InstallationDashboardSerializer(serializers.ModelSerializer):
    personnel_count = serializers.IntegerField(read_only=True)
    housed_assets_count = serializers.IntegerField(read_only=True)

    class Meta:
        model = Installation
        fields = [
            'id',
            'installation_id',
            'installation_code',
            'name',
            'dodaac',
            'city',
            'state_or_country',
            'location_display',
            'region',
            'latitude',
            'longitude',
            'mission_category',
            'primary_ew_focus',
            'dsn_prefix',
            'max_ew_billets',
            'scif_accredited',
            'rf_anechoic_chamber',
            'readiness_rating',
            'last_inspection_date',
            'personnel_count',
            'housed_assets_count',
        ]

    def to_representation(self, instance):
        data = super().to_representation(instance)
        request = self.context.get('request')
        ctx = self.context.get('access_ctx') or (
            resolve_access_context(request) if request else None
        )
        if ctx and ctx.should_mask_geospatial():
            data['latitude'] = None
            data['longitude'] = None
            if ctx.is_oem_contractor:
                data['location_display'] = 'REDACTED // FLEET FPO HUB'
        return data


class PersonnelDashboardSerializer(serializers.ModelSerializer):
    is_fully_deployable = serializers.BooleanField(read_only=True)
    functional_role_display = serializers.CharField(
        source='get_functional_role_display', read_only=True
    )

    class Meta:
        model = Personnel
        fields = [
            'id',
            'personnel_id',
            'dod_id',
            'first_name',
            'middle_initial',
            'last_name',
            'personnel_category',
            'paygrade',
            'rank',
            'rank_full',
            'specialty_code',
            'title',
            'role',
            'functional_role',
            'functional_role_display',
            'assigned_uic',
            'authorized_cage_codes',
            'installation',
            'installation_code',
            'installation_deployed_at',
            'installation_location',
            'installation_mission_category',
            'security_clearance',
            'operational_status',
            'is_medically_deployable',
            'is_training_current',
            'is_fully_deployable',
            'date_assigned',
            'projected_rotation_date',
            'dsn_phone',
            'official_email',
        ]


class NavalHullSerializer(serializers.ModelSerializer):
    homeport_code = serializers.CharField(
        source='homeport_installation.installation_code', read_only=True
    )

    class Meta:
        model = NavalHull
        fields = [
            'id',
            'hull_id',
            'hull_number',
            'vessel_name',
            'ship_class',
            'domain',
            'homeport',
            'homeport_code',
            'homeport_installation',
            'unit',
        ]


class MissionRoleSerializer(serializers.ModelSerializer):
    class Meta:
        model = MissionRole
        fields = [
            'id',
            'role_id',
            'role_code',
            'role_name',
            'description',
            'compatible_domains',
        ]


class HullSystemAssignmentSerializer(serializers.ModelSerializer):
    readiness_status = serializers.CharField(
        source='asset.readiness_status', read_only=True
    )
    operational_availability_pct = serializers.FloatField(
        source='asset.operational_availability_pct', read_only=True
    )

    class Meta:
        model = HullSystemAssignment
        fields = [
            'id',
            'assignment_id',
            'hull',
            'hull_number',
            'vessel_name',
            'ship_class',
            'domain',
            'homeport',
            'system',
            'nomenclature',
            'common_name',
            'ew_category',
            'quantity_installed',
            'mission_role',
            'role_code',
            'role_name',
            'operational_status',
            'installation_date',
            'asset',
            'readiness_status',
            'operational_availability_pct',
        ]


class MaterialCatalogSerializer(serializers.ModelSerializer):
    class Meta:
        model = MaterialCatalog
        fields = [
            'id',
            'component_id',
            'nsn',
            'nomenclature',
            'part_number',
            'cage_code',
            'manufacturer',
            'component_category',
            'repairability_code',
            'unit_of_issue',
            'unit_price',
            'critical_item',
            'lead_time_days',
            'expected_mtbf_hours',
            'compatible_system_ids',
        ]


class InventoryBalanceDashboardSerializer(serializers.ModelSerializer):
    installation_code = serializers.CharField(
        source='installation.installation_code', read_only=True
    )
    installation_name = serializers.CharField(
        source='installation.name', read_only=True
    )
    component_id = serializers.CharField(
        source='material.component_id', read_only=True
    )
    nsn = serializers.CharField(source='material.nsn', read_only=True)
    nomenclature = serializers.CharField(
        source='material.nomenclature', read_only=True
    )
    component_category = serializers.CharField(
        source='material.component_category', read_only=True
    )
    unit_price = serializers.DecimalField(
        source='material.unit_price', max_digits=12, decimal_places=2, read_only=True
    )
    critical_item = serializers.BooleanField(
        source='material.critical_item', read_only=True
    )
    needs_replenishment = serializers.BooleanField(read_only=True)

    class Meta:
        model = InventoryBalance
        fields = [
            'id',
            'installation',
            'installation_code',
            'installation_name',
            'material',
            'component_id',
            'nsn',
            'nomenclature',
            'component_category',
            'unit_price',
            'critical_item',
            'quantity_on_hand',
            'reorder_point',
            'safety_stock',
            'needs_replenishment',
        ]


class AssetModelSerializer(serializers.ModelSerializer):
    class Meta:
        model = AssetModel
        fields = [
            'id',
            'system_id',
            'nomenclature',
            'common_name',
            'ew_category',
            'asset_class',
            'compatible_domains',
            'manufacturer',
            'national_stock_number',
        ]

# ==========================================
# 2. LOGISTICS & MAINTENANCE SERIALIZERS
# ==========================================

class RequisitionDashboardSerializer(serializers.ModelSerializer):
    """Provides MILSTRIP material details, IPD/Supply status, and DLR turn-in flags."""
    material_name = serializers.CharField(source='material.nomenclature', read_only=True)
    nsn = serializers.CharField(source='material.nsn', read_only=True)
    cage_code = serializers.CharField(source='material.cage_code', read_only=True)
    unit_price = serializers.DecimalField(
        source='material.unit_price', max_digits=12, decimal_places=2, read_only=True
    )
    priority_display = serializers.CharField(source='get_priority_display', read_only=True)
    supply_status_display = serializers.CharField(
        source='get_supply_status_code_display', read_only=True
    )
    work_order_jcn = serializers.CharField(source='work_order.jcn', read_only=True)

    class Meta:
        model = Requisition
        fields = [
            'id', 'document_number', 'material_name', 'nsn', 'cage_code',
            'unit_price', 'quantity', 'priority', 'priority_display',
            'ipd_code', 'supply_status_code', 'supply_status_display',
            'requires_dlr_turn_in', 'work_order_jcn',
            'status', 'estimated_delivery_date'
        ]


class WorkOrderPartSerializer(serializers.ModelSerializer):
    nsn = serializers.CharField(source='material.nsn', read_only=True)
    cage_code = serializers.CharField(source='material.cage_code', read_only=True)
    nomenclature = serializers.CharField(source='material.nomenclature', read_only=True)
    unit_price = serializers.DecimalField(
        source='material.unit_price', max_digits=12, decimal_places=2, read_only=True
    )

    class Meta:
        model = WorkOrderPart
        fields = [
            'id',
            'nsn',
            'cage_code',
            'nomenclature',
            'quantity',
            'unit_price',
            'defective_serial_number',
            'replacement_serial_number',
            'requires_dlr_turn_in',
            'dlr_turn_in_status',
        ]


class WorkOrderDashboardSerializer(serializers.ModelSerializer):
    """
    Exposes all 5 CSMP / MAF domains from WORKORDER_DESIGN_CONSIDERATIONS.md:
    1. Identifiers (JCN, MAF Status, Priority, CASREP)
    2. Configuration (EIC, APL/AEL, Hull, Asset, Parts with Old/New SNs)
    3. Failure & Diagnostic Codes (When Discovered, Status Code, Malfunction Code, Narrative)
    4. Supply Linkage (Requisitions & DLR Carcass Turn-In)
    5. Labor, QA & Security Sign-Off (Work Center, Tech, QA, Man-Hours, Tag-Out/ESD/CWP, MDF Zeroization)
    """
    status_display = serializers.CharField(source='get_status_display', read_only=True)
    priority_display = serializers.CharField(source='get_priority_code_display', read_only=True)
    when_discovered_display = serializers.CharField(
        source='get_when_discovered_code_display', read_only=True
    )
    equipment_status_display = serializers.CharField(
        source='get_equipment_status_code_display', read_only=True
    )
    malfunction_display = serializers.CharField(
        source='get_malfunction_code_display', read_only=True
    )
    asset_serial = serializers.CharField(source='asset.serial_number', read_only=True)
    system_nomenclature = serializers.CharField(
        source='asset.asset_model.nomenclature', read_only=True
    )
    hull_number = serializers.CharField(source='hull.hull_number', read_only=True)
    vessel_name = serializers.CharField(source='hull.vessel_name', read_only=True)
    installation_code = serializers.CharField(
        source='installation.installation_code', read_only=True
    )
    technician_name = serializers.SerializerMethodField()
    technician_clearance = serializers.CharField(
        source='technician.security_clearance', read_only=True
    )
    qa_inspector_name = serializers.SerializerMethodField()
    parts_used_details = WorkOrderPartSerializer(
        source='parts_used', many=True, read_only=True
    )

    class Meta:
        model = WorkOrder
        fields = [
            'id',
            'jcn',
            'status',
            'status_display',
            'priority_code',
            'priority_display',
            'casrep_number',
            'is_grounding_fault',
            'asset',
            'asset_serial',
            'system_nomenclature',
            'hull_number',
            'vessel_name',
            'installation_code',
            'eic',
            'apl_ael',
            'when_discovered_code',
            'when_discovered_display',
            'equipment_status_code',
            'equipment_status_display',
            'malfunction_code',
            'malfunction_display',
            'description_of_fault',
            'work_center',
            'technician_name',
            'technician_clearance',
            'qa_inspector_name',
            'org_man_hours',
            'depot_man_hours',
            'tag_out_verified',
            'esd_precautions_verified',
            'cwp_controlled_work_package',
            'requires_mdf_zeroization',
            'mdf_zeroization_verified',
            'date_opened',
            'date_closed',
            'parts_used_details',
        ]

    def get_technician_name(self, obj):
        if obj.technician:
            if obj.technician.last_name:
                return f"{obj.technician.rank} {obj.technician.last_name}"
            if obj.technician.user:
                return f"{obj.technician.rank} {obj.technician.user.last_name}"
        return "Unassigned"

    def get_qa_inspector_name(self, obj):
        if obj.qa_inspector and obj.qa_inspector.last_name:
            return f"{obj.qa_inspector.rank} {obj.qa_inspector.last_name}"
        return "Pending QA"

    def to_representation(self, instance):
        data = super().to_representation(instance)
        request = self.context.get('request')
        ctx = self.context.get('access_ctx') or (
            resolve_access_context(request) if request else None
        )
        if ctx and ctx.should_mask_platform_identity():
            data['hull_number'] = 'REDACTED'
            data['vessel_name'] = 'REDACTED PLATFORM [CAGE-SCOPED]'
        return data

# ==========================================
# 3. ASSET SERIALIZER (DEEP NESTING)
# ==========================================

class AssetDashboardSerializer(serializers.ModelSerializer):
    """
    Renders the asset, its base nomenclature, its current location,
    hull/mission role context, and ONLY open work orders and pending requisitions.
    """
    system_id = serializers.CharField(source='asset_model.system_id', read_only=True)
    nomenclature = serializers.CharField(source='asset_model.nomenclature', read_only=True)
    common_name = serializers.CharField(source='asset_model.common_name', read_only=True)
    ew_category = serializers.CharField(source='asset_model.ew_category', read_only=True)
    asset_class = serializers.CharField(source='asset_model.asset_class', read_only=True)
    manufacturer = serializers.CharField(source='asset_model.manufacturer', read_only=True)
    cage_code = serializers.CharField(source='asset_model.cage_code', read_only=True)
    hull_number = serializers.CharField(source='hull.hull_number', read_only=True)
    vessel_name = serializers.CharField(source='hull.vessel_name', read_only=True)
    ship_class = serializers.CharField(source='hull.ship_class', read_only=True)
    domain = serializers.CharField(source='hull.domain', read_only=True)
    emcon_posture = serializers.CharField(source='hull.emcon_posture', read_only=True)
    mission_role_code = serializers.CharField(source='mission_role.role_code', read_only=True)
    mission_role_name = serializers.CharField(source='mission_role.role_name', read_only=True)
    assigned_unit_name = serializers.CharField(source='assigned_unit.name', read_only=True)
    current_location = InstallationBaseSerializer(read_only=True)
    readiness_status_display = serializers.CharField(source='get_readiness_status_display', read_only=True)
    
    # Nested operational data
    active_work_orders = serializers.SerializerMethodField()
    pending_requisitions = serializers.SerializerMethodField()
    mtbf_hours = serializers.FloatField(read_only=True)
    mttr_hours = serializers.FloatField(read_only=True)
    operational_availability_pct = serializers.FloatField(read_only=True)

    class Meta:
        model = Asset
        fields = [
            'id', 'assignment_id', 'serial_number', 'system_id', 'nomenclature',
            'common_name', 'ew_category', 'asset_class', 'manufacturer', 'cage_code',
            'hull_number', 'vessel_name', 'ship_class', 'domain', 'emcon_posture',
            'mission_role_code', 'mission_role_name', 'quantity_installed',
            'operational_status', 'installation_date', 'assigned_unit_name',
            'current_location', 'readiness_status', 'readiness_status_display', 
            'engine_hours', 'last_status_change', 'mtbf_hours', 'mttr_hours', 'operational_availability_pct',
            'active_work_orders', 'pending_requisitions'
        ]

    def get_active_work_orders(self, obj):
        # Filter to only show unresolved maintenance issues
        active_wos = obj.work_orders.exclude(status__in=['Closed', 'CLOSED'])
        return WorkOrderDashboardSerializer(active_wos, many=True, context=self.context).data

    def get_pending_requisitions(self, obj):
        # Filter to only show parts that haven't arrived yet
        pending_reqs = obj.requisition_set.exclude(status='Received')
        return RequisitionDashboardSerializer(pending_reqs, many=True, context=self.context).data

    def to_representation(self, instance):
        data = super().to_representation(instance)
        request = self.context.get('request')
        ctx = self.context.get('access_ctx') or (
            resolve_access_context(request) if request else None
        )
        if not ctx:
            return data

        if ctx.should_mask_platform_identity():
            data['hull_number'] = 'REDACTED'
            data['vessel_name'] = 'REDACTED PLATFORM [CAGE-SCOPED]'
            data['assigned_unit_name'] = 'REDACTED FLEET UNIT'
            if data.get('current_location'):
                data['current_location']['name'] = 'FLEET FPO HUB (OPSEC MASKED)'
                data['current_location']['location_display'] = 'REDACTED'
        elif ctx.should_mask_geospatial(data.get('emcon_posture')):
            if data.get('current_location'):
                data['current_location']['location_display'] = (
                    f"FPO HUB // MASKED (EMCON {data.get('emcon_posture') or 'ACTIVE'})"
                )
        return data

# ==========================================
# 4. UNIT & READINESS DASHBOARD SERIALIZERS
# ==========================================

class UnitReadinessTrendSerializer(serializers.ModelSerializer):
    """Time-series data for rendering 30/60/90 day charts."""
    operational_readiness_rate = serializers.FloatField(read_only=True)

    class Meta:
        model = UnitReadinessSnapshot
        fields = [
            'snapshot_date', 'total_assets', 'fmc_assets', 
            'pmc_assets', 'nmc_assets', 'operational_readiness_rate'
        ]


class MilitaryUnitDashboardSerializer(serializers.ModelSerializer):
    """
    The master payload for the unit commander's dashboard. 
    Provides unit stats, time-series readiness trends, and a breakdown 
    of all non-mission capable (NMC) assets holding up readiness.
    """
    installation_name = serializers.CharField(source='installation.name', read_only=True)
    parent_uic = serializers.CharField(source='parent_unit.uic', read_only=True)
    readiness_trends = serializers.SerializerMethodField()
    down_assets = serializers.SerializerMethodField()
    fleet_avg_mtbf = serializers.FloatField(read_only=True)
    fleet_avg_mttr = serializers.FloatField(read_only=True)
    fleet_avg_ao = serializers.FloatField(read_only=True)
    
    
    class Meta:
        model = MilitaryUnit
        fields = [
            'id', 'name', 'uic', 'echelon_level', 'parent_uic', 'command_type', 'installation_name',
            'personnel_readiness_pct', 'equipment_readiness_pct',
            'readiness_trends', 'down_assets',
            'fleet_avg_mtbf', 'fleet_avg_mttr', 'fleet_avg_ao'
        ]

    def get_readiness_trends(self, obj):
        # Grab the last 30 days of readiness snapshots for the charts
        snapshots = UnitReadinessSnapshot.objects.filter(unit=obj).order_by('-snapshot_date')[:30]
        return UnitReadinessTrendSerializer(snapshots, many=True).data

    def get_down_assets(self, obj):
        # Only serialize assets that are PMC, NMCM, NMCS, or DLM
        down_assets = obj.assets.exclude(readiness_status='FMC')
        return AssetDashboardSerializer(down_assets, many=True, context=self.context).data


class InventoryTransactionDashboardSerializer(serializers.ModelSerializer):
    """
    Read-only serializer exposing the inventory audit ledger.
    Flattens deep relationships (like NSN and Installation) for fast UI rendering.
    """
    nsn = serializers.CharField(source='balance.material.nsn', read_only=True)
    nomenclature = serializers.CharField(source='balance.material.nomenclature', read_only=True)
    installation_name = serializers.CharField(source='balance.installation.name', read_only=True)
    
    transaction_type_display = serializers.CharField(source='get_transaction_type_display', read_only=True)
    performed_by_name = serializers.SerializerMethodField()

    class Meta:
        model = InventoryTransaction
        fields = [
            'id', 'created_at', 'installation_name', 'nsn', 'nomenclature',
            'transaction_type', 'transaction_type_display', 'quantity',
            'previous_balance', 'new_balance', 'reference_document', 
            'performed_by_name'
        ]

    def get_performed_by_name(self, obj):
        if obj.performed_by:
            if obj.performed_by.last_name:
                return f"{obj.performed_by.rank} {obj.performed_by.last_name}"
            if obj.performed_by.user:
                return f"{obj.performed_by.rank} {obj.performed_by.user.last_name}"
        # Fallback for automated background tasks or depot receipts
        return "Automated System"


class SecurityAuditLogSerializer(serializers.ModelSerializer):
    event_type_display = serializers.CharField(
        source='get_event_type_display', read_only=True
    )
    decision_display = serializers.CharField(
        source='get_decision_display', read_only=True
    )

    class Meta:
        model = SecurityAuditLog
        fields = [
            'id',
            'created_at',
            'actor_name',
            'actor_role',
            'actor_uic',
            'actor_clearance',
            'actor_cage_scope',
            'event_type',
            'event_type_display',
            'decision',
            'decision_display',
            'resource_target',
            'reason',
        ]
