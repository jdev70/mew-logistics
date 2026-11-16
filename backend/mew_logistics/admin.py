import csv
from django.contrib import admin
from django.http import HttpResponse
from .models import (
    Asset,
    AssetModel,
    AssetStatusLog,
    HullSystemAssignment,
    Installation,
    InventoryBalance,
    InventoryTransaction,
    MaterialCatalog,
    MewSystem,
    MilitaryUnit,
    MissionRole,
    NavalHull,
    Personnel,
    Requisition,
    UnitReadinessSnapshot,
    WorkOrder,
)

@admin.action(description="Export selected units with reliability metrics to CSV")
def export_reliability_metrics_csv(modeladmin, request, queryset):
    """
    Admin action to export selected units. Re-evaluates the queryset 
    using the custom manager to calculate MTBF, MTTR, and Ao on the fly.
    """
    # Grab the IDs of the rows the admin checked, and push them through the custom manager
    annotated_units = modeladmin.model.objects.filter(
        id__in=queryset.values_list('id', flat=True)
    ).with_reliability_metrics()

    response = HttpResponse(content_type='text/csv')
    response['Content-Disposition'] = 'attachment; filename="admin_unit_metrics_export.csv"'
    
    writer = csv.writer(response)
    writer.writerow([
        'Unit Name', 
        'UIC', 
        'Installation',
        'Avg MTBF (Hours)', 
        'Avg MTTR (Hours)', 
        'Operational Availability (Ao %)'
    ])
    
    for unit in annotated_units:
        writer.writerow([
            unit.name,
            unit.uic,
            unit.installation.name if unit.installation else 'N/A',
            unit.avg_mtbf or 0.0,
            unit.avg_mttr or 0.0,
            unit.avg_ao or 0.0
        ])
        
    return response


@admin.register(Installation)
class InstallationAdmin(admin.ModelAdmin):
    list_display = (
        'installation_id',
        'installation_code',
        'name',
        'dodaac',
        'location_display',
        'region',
        'mission_category',
        'readiness_rating',
    )
    search_fields = ('installation_id', 'installation_code', 'name', 'dodaac', 'city')
    list_filter = ('region', 'mission_category', 'readiness_rating', 'scif_accredited')


@admin.register(MilitaryUnit)
class MilitaryUnitAdmin(admin.ModelAdmin):
    list_display = ('name', 'uic', 'command_type', 'installation')
    search_fields = ('name', 'uic')
    list_filter = ('command_type', 'installation')
    
    # Register the CSV export action in the admin dropdown
    actions = [export_reliability_metrics_csv]


@admin.register(Personnel)
class PersonnelAdmin(admin.ModelAdmin):
    list_display = (
        'personnel_id',
        'dod_id',
        'rank',
        'last_name',
        'first_name',
        'personnel_category',
        'specialty_code',
        'installation_code',
        'security_clearance',
        'operational_status',
    )
    search_fields = (
        'personnel_id',
        'dod_id',
        'first_name',
        'last_name',
        'official_email',
        'specialty_code',
        'title',
    )
    list_filter = (
        'personnel_category',
        'security_clearance',
        'operational_status',
        'installation_mission_category',
        'installation',
    )


@admin.register(NavalHull)
class NavalHullAdmin(admin.ModelAdmin):
    list_display = (
        'hull_id',
        'hull_number',
        'vessel_name',
        'ship_class',
        'domain',
        'homeport',
    )
    search_fields = ('hull_id', 'hull_number', 'vessel_name', 'ship_class', 'homeport')
    list_filter = ('domain', 'ship_class', 'homeport')


@admin.register(MissionRole)
class MissionRoleAdmin(admin.ModelAdmin):
    list_display = ('role_id', 'role_code', 'role_name', 'description')
    search_fields = ('role_id', 'role_code', 'role_name')


@admin.register(AssetModel)
class AssetModelAdmin(admin.ModelAdmin):
    list_display = (
        'system_id',
        'nomenclature',
        'common_name',
        'ew_category',
        'manufacturer',
        'national_stock_number',
    )
    search_fields = ('system_id', 'nomenclature', 'common_name', 'manufacturer')
    list_filter = ('ew_category', 'manufacturer')


@admin.register(Asset)
class AssetAdmin(admin.ModelAdmin):
    list_display = (
        'assignment_id',
        'serial_number',
        'asset_model',
        'hull',
        'mission_role',
        'quantity_installed',
        'operational_status',
        'readiness_status',
        'operational_availability_pct',
    )
    search_fields = ('assignment_id', 'serial_number', 'asset_model__nomenclature', 'hull__hull_number')
    list_filter = ('readiness_status', 'operational_status', 'asset_model__ew_category')


@admin.register(HullSystemAssignment)
class HullSystemAssignmentAdmin(admin.ModelAdmin):
    list_display = (
        'assignment_id',
        'hull_number',
        'vessel_name',
        'domain',
        'nomenclature',
        'common_name',
        'quantity_installed',
        'role_code',
        'operational_status',
        'installation_date',
    )
    search_fields = (
        'assignment_id',
        'hull_number',
        'vessel_name',
        'nomenclature',
        'common_name',
        'role_code',
    )
    list_filter = ('domain', 'ew_category', 'role_code', 'operational_status')


@admin.register(MaterialCatalog)
class MaterialCatalogAdmin(admin.ModelAdmin):
    list_display = (
        'component_id',
        'nsn',
        'nomenclature',
        'component_category',
        'manufacturer',
        'unit_of_issue',
        'unit_price',
        'critical_item',
        'lead_time_days',
    )
    search_fields = ('component_id', 'nsn', 'nomenclature', 'part_number', 'manufacturer')
    list_filter = ('component_category', 'critical_item', 'repairability_code', 'manufacturer')


@admin.register(InventoryBalance)
class InventoryBalanceAdmin(admin.ModelAdmin):
    list_display = (
        'installation',
        'material',
        'quantity_on_hand',
        'reorder_point',
        'safety_stock',
    )
    search_fields = ('installation__name', 'installation__installation_code', 'material__nsn', 'material__nomenclature')
    list_filter = ('installation', 'material__critical_item')


@admin.register(Requisition)
class RequisitionAdmin(admin.ModelAdmin):
    list_display = (
        'document_number',
        'requesting_unit',
        'material',
        'quantity',
        'priority',
        'status',
        'tied_to_asset',
        'estimated_delivery_date',
    )
    search_fields = ('document_number', 'material__nsn', 'material__nomenclature', 'requesting_unit__name')
    list_filter = ('status', 'priority')


@admin.register(WorkOrder)
class WorkOrderAdmin(admin.ModelAdmin):
    list_display = (
        'id',
        'asset',
        'technician',
        'is_grounding_fault',
        'status',
        'date_opened',
        'date_closed',
    )
    search_fields = ('asset__serial_number', 'description_of_fault', 'technician__last_name')
    list_filter = ('status', 'is_grounding_fault')


@admin.register(InventoryTransaction)
class InventoryTransactionAdmin(admin.ModelAdmin):
    list_display = (
        'created_at',
        'balance',
        'transaction_type',
        'quantity',
        'previous_balance',
        'new_balance',
        'reference_document',
        'performed_by',
    )
    search_fields = ('reference_document', 'balance__material__nsn', 'balance__installation__name')
    list_filter = ('transaction_type',)


@admin.register(AssetStatusLog)
class AssetStatusLogAdmin(admin.ModelAdmin):
    list_display = ('created_at', 'asset', 'old_status', 'new_status')
    list_filter = ('new_status', 'old_status')


@admin.register(UnitReadinessSnapshot)
class UnitReadinessSnapshotAdmin(admin.ModelAdmin):
    list_display = (
        'snapshot_date',
        'unit',
        'total_personnel',
        'deployable_personnel',
        'total_assets',
        'fmc_assets',
        'pmc_assets',
        'nmc_assets',
    )
    list_filter = ('snapshot_date', 'unit')



