import csv
from django.http import HttpResponse
from django.db import transaction
from django.db.models import Count, F
from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response
from django.shortcuts import get_object_or_404

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
from .serializers import (
    AssetDashboardSerializer,
    AssetModelSerializer,
    HullSystemAssignmentSerializer,
    InstallationDashboardSerializer,
    InventoryBalanceDashboardSerializer,
    InventoryTransactionDashboardSerializer,
    MaterialCatalogSerializer,
    MilitaryUnitDashboardSerializer,
    MissionRoleSerializer,
    NavalHullSerializer,
    PersonnelDashboardSerializer,
    RequisitionDashboardSerializer,
    WorkOrderDashboardSerializer,
)

class MilitaryUnitDashboardViewSet(viewsets.ReadOnlyModelViewSet):
    serializer_class = MilitaryUnitDashboardSerializer

    def get_queryset(self):
        requested_class = self.request.query_params.get('asset_class', None)
        return MilitaryUnit.objects.with_reliability_metrics(
            asset_class=requested_class
        ).select_related('installation')

    @action(detail=False, methods=['get'])
    def export_csv(self, request):
        """
        Generates a downloadable CSV matching the currently filtered dashboard view.
        Route: /api/v1/dashboard/units/export_csv/?asset_class=Ground Combat
        """
        # 1. Fetch the annotated and filtered data using the existing manager logic
        queryset = self.get_queryset()

        # 2. Setup the HTTP response for a CSV download
        response = HttpResponse(content_type='text/csv')
        
        # Dynamically name the file based on the applied filter
        asset_class = request.query_params.get('asset_class', 'All_Assets')
        filename = f"unit_reliability_{asset_class.replace(' ', '_')}.csv"
        response['Content-Disposition'] = f'attachment; filename="{filename}"'

        # 3. Initialize the CSV writer and set headers
        writer = csv.writer(response)
        writer.writerow([
            'Unit Name', 
            'UIC', 
            'Command Type',
            'Installation',
            'Personnel Readiness (%)',
            'Equipment Readiness (%)',
            'Avg MTBF (Hours)',
            'Avg MTTR (Hours)',
            'Operational Availability (Ao %)'
        ])

        # 4. Stream the data rows
        for unit in queryset:
            writer.writerow([
                unit.name,
                unit.uic,
                unit.command_type,
                unit.installation.name if unit.installation else 'N/A',
                unit.personnel_readiness_pct,
                unit.equipment_readiness_pct,
                unit.fleet_avg_mtbf or 0.0,
                unit.fleet_avg_mttr or 0.0,
                unit.fleet_avg_ao or 0.0,
            ])

        return response


class InventoryTransactionViewSet(viewsets.ReadOnlyModelViewSet):
    """
    Exposes the immutable inventory audit trail.
    Only allows GET requests (list and retrieve).
    """
    serializer_class = InventoryTransactionDashboardSerializer

    def get_queryset(self):
        # 1. Heavily optimize the SQL query to fetch all foreign keys in a single trip
        queryset = InventoryTransaction.objects.select_related(
            'balance__material',
            'balance__installation',
            'performed_by__user'
        ).order_by('-created_at')

        # 2. Allow the dashboard to filter the ledger dynamically
        installation_id = self.request.query_params.get('installation_id')
        nsn = self.request.query_params.get('nsn')
        transaction_type = self.request.query_params.get('type')
        
        if installation_id:
            queryset = queryset.filter(balance__installation_id=installation_id)
            
        if nsn:
            queryset = queryset.filter(balance__material__nsn=nsn)
            
        if transaction_type:
            queryset = queryset.filter(transaction_type=transaction_type)

        return queryset


class RequisitionViewSet(viewsets.ModelViewSet):
    serializer_class = RequisitionDashboardSerializer
    
    def get_queryset(self):
        return Requisition.objects.select_related('material', 'tied_to_asset')

    @action(detail=False, methods=['post'], url_path='receive-shipment')
    @transaction.atomic  # Ensures the split and the receipt both succeed, or neither do
    def receive_shipment(self, request):
        """
        Scanner-friendly endpoint for receiving warehouse shipments.
        Accepts an optional 'received_quantity' for short-ships.
        Expected POST payload: {"document_number": "REQ-123456", "received_quantity": 2}
        """
        document_number = request.data.get('document_number')
        received_quantity = request.data.get('received_quantity')
        
        if not document_number:
            return Response(
                {"error": "Please provide a document_number to scan."}, 
                status=status.HTTP_400_BAD_REQUEST
            )

        requisition = get_object_or_404(Requisition, document_number=document_number)

        if requisition.status == 'Received':
            return Response(
                {"error": f"Shipment {document_number} has already been received."}, 
                status=status.HTTP_400_BAD_REQUEST
            )
        # 1. Resolve the Personnel profile of the API caller
        # Assumes request.user is authenticated via JWT/Session
        clerk_profile = getattr(request.user, 'personnel', None)

        # 2. Inject the profile as a temporary private attribute on the instance
        # This data only lives in memory and won't throw a database schema error
        requisition._received_by = clerk_profile

        # 3. Validate the incoming quantity
        if received_quantity is not None:
            try:
                received_quantity = int(received_quantity)
                if received_quantity <= 0 or received_quantity > requisition.quantity:
                    raise ValueError
            except ValueError:
                return Response(
                    {"error": f"Invalid received_quantity. Must be between 1 and {requisition.quantity}."},
                    status=status.HTTP_400_BAD_REQUEST
                )
        else:
            # Default to full receipt if the scanner doesn't specify a quantity
            received_quantity = requisition.quantity

        backorder_created = False
        backorder_doc = None

        # 2. Handle the Partial Shipment (Short-Ship) Split
        if received_quantity < requisition.quantity:
            backorder_qty = requisition.quantity - received_quantity
            
            # Create a clone for the remaining balance
            backorder = Requisition.objects.create(
                document_number=f"{requisition.document_number}-BO",
                requesting_unit=requisition.requesting_unit,
                material=requisition.material,
                quantity=backorder_qty,
                priority=requisition.priority,
                tied_to_asset=requisition.tied_to_asset,
                status='Submitted',
                estimated_delivery_date=requisition.estimated_delivery_date
            )
            backorder_created = True
            backorder_doc = backorder.document_number

        if backorder_created:
            # Shrink the current requisition to match what physically arrived
            requisition.quantity = received_quantity

        requisition.status = 'Received'
        
        update_fields = ['status']
        if backorder_created:
            update_fields.append('quantity')

        requisition.save(update_fields=update_fields)
            
        # 4. Build the scanner response payload
        response_payload = {
            "message": "Shipment received and added to inventory.",
            "document_number": requisition.document_number,
            "material_received": requisition.material.nomenclature,
            "quantity_added": requisition.quantity
        }
        
        if backorder_created:
            response_payload["backorder_notice"] = f"Short shipment. Backorder created: {backorder_doc} for {backorder_qty} units."

        return Response(response_payload, status=status.HTTP_200_OK)

class AssetViewSet(viewsets.ModelViewSet):
    """Standard CRUD endpoint for MEW Assets with domain/readiness filtering."""
    serializer_class = AssetDashboardSerializer

    def get_queryset(self):
        queryset = Asset.objects.select_related(
            'asset_model',
            'current_location',
            'assigned_unit',
            'hull',
            'mission_role',
        ).prefetch_related(
            'work_orders',
            'requisition_set',
        )
        domain = self.request.query_params.get('domain')
        readiness_status = self.request.query_params.get('readiness_status')
        ew_category = self.request.query_params.get('ew_category')
        hull_number = self.request.query_params.get('hull_number')

        if domain:
            queryset = queryset.filter(hull__domain=domain)
        if readiness_status:
            queryset = queryset.filter(readiness_status=readiness_status)
        if ew_category:
            queryset = queryset.filter(asset_model__ew_category=ew_category)
        if hull_number:
            queryset = queryset.filter(hull__hull_number=hull_number)

        return queryset


class WorkOrderViewSet(viewsets.ModelViewSet):
    """Endpoint for tracking maintenance tasks."""
    serializer_class = WorkOrderDashboardSerializer

    def get_queryset(self):
        return WorkOrder.objects.select_related('technician__user', 'asset').order_by('-date_opened')


class InstallationViewSet(viewsets.ReadOnlyModelViewSet):
    """Exposes MEW Installations with personnel and asset counts."""
    serializer_class = InstallationDashboardSerializer

    def get_queryset(self):
        queryset = Installation.objects.annotate(
            personnel_count=Count('personnel', distinct=True),
            housed_assets_count=Count('housed_assets', distinct=True),
        ).order_by('installation_id', 'name')

        mission_category = self.request.query_params.get('mission_category')
        region = self.request.query_params.get('region')
        if mission_category:
            queryset = queryset.filter(mission_category=mission_category)
        if region:
            queryset = queryset.filter(region=region)
        return queryset


class PersonnelViewSet(viewsets.ReadOnlyModelViewSet):
    """Exposes MEW Personnel roster with category, status, clearance, and installation filters."""
    serializer_class = PersonnelDashboardSerializer

    def get_queryset(self):
        queryset = Personnel.objects.select_related('installation', 'unit').order_by('personnel_id', 'last_name')
        category = self.request.query_params.get('personnel_category')
        op_status = self.request.query_params.get('operational_status')
        clearance = self.request.query_params.get('security_clearance')
        installation_code = self.request.query_params.get('installation_code')

        if category:
            queryset = queryset.filter(personnel_category=category)
        if op_status:
            queryset = queryset.filter(operational_status=op_status)
        if clearance:
            queryset = queryset.filter(security_clearance=clearance)
        if installation_code:
            queryset = queryset.filter(installation_code=installation_code)
        return queryset


class NavalHullViewSet(viewsets.ReadOnlyModelViewSet):
    """Exposes Naval Hulls / Vessels / Squadrons."""
    serializer_class = NavalHullSerializer

    def get_queryset(self):
        queryset = NavalHull.objects.select_related('homeport_installation', 'unit').order_by('hull_id')
        domain = self.request.query_params.get('domain')
        if domain:
            queryset = queryset.filter(domain=domain)
        return queryset


class HullSystemAssignmentViewSet(viewsets.ReadOnlyModelViewSet):
    """Exposes Hull-to-MEW-System Loadout Assignments."""
    serializer_class = HullSystemAssignmentSerializer

    def get_queryset(self):
        queryset = HullSystemAssignment.objects.select_related(
            'hull', 'system', 'mission_role', 'asset'
        ).order_by('assignment_id')
        domain = self.request.query_params.get('domain')
        hull_number = self.request.query_params.get('hull_number')
        role_code = self.request.query_params.get('role_code')

        if domain:
            queryset = queryset.filter(domain=domain)
        if hull_number:
            queryset = queryset.filter(hull_number=hull_number)
        if role_code:
            queryset = queryset.filter(role_code=role_code)
        return queryset


class MaterialCatalogViewSet(viewsets.ReadOnlyModelViewSet):
    """Exposes MEW Components & LRUs catalog."""
    serializer_class = MaterialCatalogSerializer

    def get_queryset(self):
        queryset = MaterialCatalog.objects.all().order_by('component_id', 'nsn')
        category = self.request.query_params.get('component_category')
        critical = self.request.query_params.get('critical_item')
        if category:
            queryset = queryset.filter(component_category=category)
        if critical is not None:
            queryset = queryset.filter(critical_item=critical.lower() == 'true')
        return queryset


class InventoryBalanceViewSet(viewsets.ReadOnlyModelViewSet):
    """Exposes warehouse stock levels across MEW installations."""
    serializer_class = InventoryBalanceDashboardSerializer

    def get_queryset(self):
        queryset = InventoryBalance.objects.select_related(
            'installation', 'material'
        ).order_by('installation__installation_code', 'material__component_id')
        installation_code = self.request.query_params.get('installation_code')
        low_stock = self.request.query_params.get('low_stock')

        if installation_code:
            queryset = queryset.filter(installation__installation_code=installation_code)
        if low_stock and low_stock.lower() == 'true':
            queryset = queryset.filter(quantity_on_hand__lte=F('reorder_point'))
        return queryset
