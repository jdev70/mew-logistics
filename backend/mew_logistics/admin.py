import csv
from django.contrib import admin
from django.http import HttpResponse
from .models import MilitaryUnit

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

@admin.register(MilitaryUnit)
class MilitaryUnitAdmin(admin.ModelAdmin):
    list_display = ('name', 'uic', 'command_type', 'installation')
    search_fields = ('name', 'uic')
    list_filter = ('command_type', 'installation')
    
    # Register the CSV export action in the admin dropdown
    actions = [export_reliability_metrics_csv]
