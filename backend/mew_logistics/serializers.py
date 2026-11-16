from rest_framework import serializers
from .models import (
    Installation,
    MaterialCatalog,
    AssetModel,
    MilitaryUnit,
    Asset, 
    Requisition, 
    WorkOrder, 
    UnitReadinessSnapshot,
    InventoryTransaction
)

# ==========================================
# 1. BASE & REFERENCE SERIALIZERS
# ==========================================

class InstallationBaseSerializer(serializers.ModelSerializer):
    class Meta:
        model = Installation
        fields = ['id', 'name', 'dodaac']


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


class AssetModelSerializer(serializers.ModelSerializer):
    class Meta:
        model = AssetModel
        fields = ['id', 'nomenclature', 'asset_class']

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
    and ONLY open work orders and pending requisitions to keep payloads light.
    """
    nomenclature = serializers.CharField(source='asset_model.nomenclature', read_only=True)
    asset_class = serializers.CharField(source='asset_model.asset_class', read_only=True)
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
            'id', 'serial_number', 'nomenclature', 'asset_class', 
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