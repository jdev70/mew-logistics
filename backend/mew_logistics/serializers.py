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
    UnitReadinessSnapshot,
    WorkOrder,
)

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


class PersonnelDashboardSerializer(serializers.ModelSerializer):
    is_fully_deployable = serializers.BooleanField(read_only=True)

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
    """Provides material details and delivery status for dashboard tracking."""
    material_name = serializers.CharField(source='material.nomenclature', read_only=True)
    nsn = serializers.CharField(source='material.nsn', read_only=True)
    priority_display = serializers.CharField(source='get_priority_display', read_only=True)

    class Meta:
        model = Requisition
        fields = [
            'id', 'document_number', 'material_name', 'nsn', 
            'quantity', 'priority_display', 'status', 'estimated_delivery_date'
        ]


class WorkOrderDashboardSerializer(serializers.ModelSerializer):
    """Focuses on active maintenance faults deadlining equipment."""
    technician_name = serializers.SerializerMethodField()

    class Meta:
        model = WorkOrder
        fields = [
            'id', 'description_of_fault', 'technician_name', 
            'date_opened', 'is_grounding_fault', 'status'
        ]

    def get_technician_name(self, obj):
        if obj.technician:
            if obj.technician.last_name:
                return f"{obj.technician.rank} {obj.technician.last_name}"
            if obj.technician.user:
                return f"{obj.technician.rank} {obj.technician.user.last_name}"
        return "Unassigned"

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
    hull_number = serializers.CharField(source='hull.hull_number', read_only=True)
    vessel_name = serializers.CharField(source='hull.vessel_name', read_only=True)
    ship_class = serializers.CharField(source='hull.ship_class', read_only=True)
    domain = serializers.CharField(source='hull.domain', read_only=True)
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
            'common_name', 'ew_category', 'asset_class', 'manufacturer',
            'hull_number', 'vessel_name', 'ship_class', 'domain',
            'mission_role_code', 'mission_role_name', 'quantity_installed',
            'operational_status', 'installation_date', 'assigned_unit_name',
            'current_location', 'readiness_status', 'readiness_status_display', 
            'engine_hours', 'last_status_change', 'mtbf_hours', 'mttr_hours', 'operational_availability_pct',
            'active_work_orders', 'pending_requisitions'
        ]

    def get_active_work_orders(self, obj):
        # Filter to only show unresolved maintenance issues
        active_wos = obj.work_orders.exclude(status='Closed')
        return WorkOrderDashboardSerializer(active_wos, many=True).data

    def get_pending_requisitions(self, obj):
        # Filter to only show parts that haven't arrived yet
        pending_reqs = obj.requisition_set.exclude(status='Received')
        return RequisitionDashboardSerializer(pending_reqs, many=True).data

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
    readiness_trends = serializers.SerializerMethodField()
    down_assets = serializers.SerializerMethodField()
    fleet_avg_mtbf = serializers.FloatField(read_only=True)
    fleet_avg_mttr = serializers.FloatField(read_only=True)
    fleet_avg_ao = serializers.FloatField(read_only=True)
    
    
    class Meta:
        model = MilitaryUnit
        fields = [
            'id', 'name', 'uic', 'command_type', 'installation_name',
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
        return AssetDashboardSerializer(down_assets, many=True).data


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