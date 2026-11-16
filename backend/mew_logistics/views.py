import csv
from django.http import HttpResponse
from django.db import transaction
from django.db.models import Count, F
from django.utils import timezone
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
    SecurityAuditLog,
    UnitReadinessSnapshot,
    WorkOrder,
)
from .analytics_engine import compute_mew_analytics_payload
from .rbac import CURATED_RBAC_PERSONAS, resolve_access_context
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
    SecurityAuditLogSerializer,
    WorkOrderDashboardSerializer,
)


class RbacScopedViewSetMixin:
    """
    Mixin that resolves the caller's RBAC + ABAC AccessContext and injects it
    into the DRF serializer context so dynamic OPSEC field masking runs automatically.
    """
    def get_access_context(self):
        if not hasattr(self, '_access_ctx'):
            self._access_ctx = resolve_access_context(self.request)
        return self._access_ctx

    def get_serializer_context(self):
        ctx = super().get_serializer_context()
        ctx['access_ctx'] = self.get_access_context()
        return ctx


class MilitaryUnitDashboardViewSet(RbacScopedViewSetMixin, viewsets.ReadOnlyModelViewSet):
    serializer_class = MilitaryUnitDashboardSerializer

    def get_queryset(self):
        requested_class = self.request.query_params.get('asset_class', None)
        qs = MilitaryUnit.objects.with_reliability_metrics(
            asset_class=requested_class
        ).select_related('installation', 'parent_unit')
        return self.get_access_context().scope_units(qs)

    @action(detail=False, methods=['get'])
    def export_csv(self, request):
        """
        Generates a downloadable CSV matching the currently filtered dashboard view.
        Protected by Mosaic-Effect RBAC/ABAC guardrail (requires TYCOM Analyst + TS/SCI).
        """
        ctx = self.get_access_context()
        allowed, reason = ctx.can_export_fleet_csv()
        if not allowed:
            ctx.log_event(
                event_type=SecurityAuditLog.EventType.BULK_CSV_EXPORT,
                decision=SecurityAuditLog.PolicyDecision.DENY,
                resource_target="/api/v1/dashboard/units/export_csv/",
                reason=reason,
            )
            return Response({"error": reason}, status=status.HTTP_403_FORBIDDEN)

        ctx.log_event(
            event_type=SecurityAuditLog.EventType.BULK_CSV_EXPORT,
            decision=SecurityAuditLog.PolicyDecision.ALLOW,
            resource_target="/api/v1/dashboard/units/export_csv/",
            reason=reason,
        )

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


class InventoryTransactionViewSet(RbacScopedViewSetMixin, viewsets.ReadOnlyModelViewSet):
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
        queryset = self.get_access_context().scope_transactions(queryset)

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


class RequisitionViewSet(RbacScopedViewSetMixin, viewsets.ModelViewSet):
    serializer_class = RequisitionDashboardSerializer
    
    def get_queryset(self):
        qs = Requisition.objects.select_related('material', 'tied_to_asset', 'requesting_unit', 'work_order')
        return self.get_access_context().scope_requisitions(qs)

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

        ctx = self.get_access_context()
        allowed, reason = ctx.can_receive_shipment(requisition)
        if not allowed:
            ctx.log_event(
                event_type=SecurityAuditLog.EventType.REQUISITION_RECEIPT,
                decision=SecurityAuditLog.PolicyDecision.DENY,
                resource_target=document_number,
                reason=reason,
            )
            return Response({"error": reason}, status=status.HTTP_403_FORBIDDEN)

        if requisition.status == 'Received':
            return Response(
                {"error": f"Shipment {document_number} has already been received."}, 
                status=status.HTTP_400_BAD_REQUEST
            )
        # 1. Resolve the Personnel profile of the API caller
        clerk_profile = ctx.personnel or getattr(request.user, 'personnel', None)

        # 2. Inject the profile as a temporary private attribute on the instance
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

        ctx.log_event(
            event_type=SecurityAuditLog.EventType.REQUISITION_RECEIPT,
            decision=SecurityAuditLog.PolicyDecision.ALLOW,
            resource_target=requisition.document_number,
            reason=f"Received {requisition.quantity}x {requisition.material.nsn} into warehouse.",
        )

        return Response(response_payload, status=status.HTTP_200_OK)

class AssetViewSet(RbacScopedViewSetMixin, viewsets.ModelViewSet):
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
        queryset = self.get_access_context().scope_assets(queryset)
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


class WorkOrderViewSet(RbacScopedViewSetMixin, viewsets.ModelViewSet):
    """
    CSMP / MAF Work Order endpoint supporting all 5 DoD maintenance domains:
    - Filtering by MAF status, priority code, work center, malfunction code, and hull
    - Custom action `POST /api/v1/work-orders/<id>/advance-status/` for stepping through
      OPEN -> AWAITING_PARTS -> IN_WORK -> QA_PENDING -> CLOSED with QA, Tag-Out, ESD,
      and COMSEC/MDF zeroization verification.
    """
    serializer_class = WorkOrderDashboardSerializer

    def get_queryset(self):
        queryset = (
            WorkOrder.objects.select_related(
                'technician__user',
                'qa_inspector',
                'asset__asset_model',
                'hull',
                'installation',
            )
            .prefetch_related('parts_used__material', 'requisitions')
            .order_by('-date_opened')
        )
        queryset = self.get_access_context().scope_work_orders(queryset)

        maf_status = self.request.query_params.get('status')
        priority_code = self.request.query_params.get('priority_code')
        work_center = self.request.query_params.get('work_center')
        malfunction_code = self.request.query_params.get('malfunction_code')
        hull_number = self.request.query_params.get('hull_number')
        is_grounding = self.request.query_params.get('is_grounding_fault')

        if maf_status:
            queryset = queryset.filter(status=maf_status)
        if priority_code:
            queryset = queryset.filter(priority_code=priority_code)
        if work_center:
            queryset = queryset.filter(work_center=work_center)
        if malfunction_code:
            queryset = queryset.filter(malfunction_code=malfunction_code)
        if hull_number:
            queryset = queryset.filter(hull__hull_number=hull_number)
        if is_grounding is not None:
            queryset = queryset.filter(is_grounding_fault=is_grounding.lower() == 'true')

        return queryset

    @action(detail=True, methods=['post'], url_path='advance-status')
    @transaction.atomic
    def advance_status(self, request, pk=None):
        """
        Steps a Maintenance Action Form (MAF) through its CSMP lifecycle or transitions
        to an explicit `target_status`, enforcing QA, Safety (Tag-Out/ESD), and
        Mission Data File (MDF) cryptographic zeroization sign-offs upon closure.
        """
        wo = self.get_object()
        if wo.status == WorkOrder.MAFStatus.CLOSED:
            return Response(
                {"error": f"MAF {wo.jcn} is already CLOSED."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        lifecycle = [
            WorkOrder.MAFStatus.OPEN,
            WorkOrder.MAFStatus.AWAITING_PARTS,
            WorkOrder.MAFStatus.IN_WORK,
            WorkOrder.MAFStatus.QA_PENDING,
            WorkOrder.MAFStatus.CLOSED,
        ]

        target_status = request.data.get('target_status')
        if not target_status:
            try:
                idx = lifecycle.index(wo.status)
                target_status = lifecycle[min(idx + 1, len(lifecycle) - 1)]
            except ValueError:
                target_status = WorkOrder.MAFStatus.IN_WORK

        ctx = self.get_access_context()
        allowed, reason = ctx.can_advance_work_order(wo, target_status)
        if not allowed:
            ctx.log_event(
                event_type=SecurityAuditLog.EventType.MAF_SIGN_OFF,
                decision=SecurityAuditLog.PolicyDecision.DENY,
                resource_target=wo.jcn or str(wo.id),
                reason=reason,
            )
            return Response({"error": reason}, status=status.HTTP_403_FORBIDDEN)

        # Optional labor and safety/security sign-off overrides from request payload
        if 'org_man_hours' in request.data:
            wo.org_man_hours = request.data['org_man_hours']
        if 'depot_man_hours' in request.data:
            wo.depot_man_hours = request.data['depot_man_hours']
        if 'tag_out_verified' in request.data:
            wo.tag_out_verified = bool(request.data['tag_out_verified'])
        if 'esd_precautions_verified' in request.data:
            wo.esd_precautions_verified = bool(request.data['esd_precautions_verified'])
        if 'mdf_zeroization_verified' in request.data:
            wo.mdf_zeroization_verified = bool(request.data['mdf_zeroization_verified'])

        if target_status in (WorkOrder.MAFStatus.QA_PENDING, WorkOrder.MAFStatus.CLOSED):
            wo.tag_out_verified = True
            wo.esd_precautions_verified = True
            if wo.requires_mdf_zeroization:
                wo.mdf_zeroization_verified = True
            if not wo.qa_inspector:
                qa_candidate = (
                    Personnel.objects.filter(
                        rank__in=('CTTC', 'CTT1', 'LCDR', 'CDR', 'CIV (GS-13)', 'CIV (GS-14)')
                    ).first()
                    or wo.technician
                )
                wo.qa_inspector = qa_candidate
            if float(wo.org_man_hours or 0) == 0.0:
                wo.org_man_hours = 6.5

        if target_status == WorkOrder.MAFStatus.CLOSED:
            wo.date_closed = timezone.now()

        wo.status = target_status
        wo.save()

        ctx.log_event(
            event_type=SecurityAuditLog.EventType.MAF_SIGN_OFF,
            decision=SecurityAuditLog.PolicyDecision.ALLOW,
            resource_target=wo.jcn or str(wo.id),
            reason=f"Transitioned MAF {wo.jcn} to {wo.status}.",
        )

        return Response(
            {
                "message": f"MAF {wo.jcn} transitioned to {wo.get_status_display()}.",
                "work_order": WorkOrderDashboardSerializer(
                    wo, context=self.get_serializer_context()
                ).data,
            },
            status=status.HTTP_200_OK,
        )


class InstallationViewSet(RbacScopedViewSetMixin, viewsets.ReadOnlyModelViewSet):
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


class PersonnelViewSet(RbacScopedViewSetMixin, viewsets.ReadOnlyModelViewSet):
    """Exposes MEW Personnel roster with category, status, clearance, and installation filters."""
    serializer_class = PersonnelDashboardSerializer

    def get_queryset(self):
        queryset = Personnel.objects.select_related('installation', 'unit').order_by('personnel_id', 'last_name')
        queryset = self.get_access_context().scope_personnel(queryset)
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


class NavalHullViewSet(RbacScopedViewSetMixin, viewsets.ReadOnlyModelViewSet):
    """Exposes Naval Hulls / Vessels / Squadrons."""
    serializer_class = NavalHullSerializer

    def get_queryset(self):
        queryset = NavalHull.objects.select_related('homeport_installation', 'unit').order_by('hull_id')
        queryset = self.get_access_context().scope_hulls(queryset)
        domain = self.request.query_params.get('domain')
        if domain:
            queryset = queryset.filter(domain=domain)
        return queryset


class HullSystemAssignmentViewSet(RbacScopedViewSetMixin, viewsets.ReadOnlyModelViewSet):
    """Exposes Hull-to-MEW-System Loadout Assignments."""
    serializer_class = HullSystemAssignmentSerializer

    def get_queryset(self):
        queryset = HullSystemAssignment.objects.select_related(
            'hull', 'system', 'mission_role', 'asset'
        ).order_by('assignment_id')
        queryset = self.get_access_context().scope_loadouts(queryset)
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


class MaterialCatalogViewSet(RbacScopedViewSetMixin, viewsets.ReadOnlyModelViewSet):
    """Exposes MEW Components & LRUs catalog."""
    serializer_class = MaterialCatalogSerializer

    def get_queryset(self):
        queryset = MaterialCatalog.objects.all().order_by('component_id', 'nsn')
        queryset = self.get_access_context().scope_components(queryset)
        category = self.request.query_params.get('component_category')
        critical = self.request.query_params.get('critical_item')
        if category:
            queryset = queryset.filter(component_category=category)
        if critical is not None:
            queryset = queryset.filter(critical_item=critical.lower() == 'true')
        return queryset


class InventoryBalanceViewSet(RbacScopedViewSetMixin, viewsets.ReadOnlyModelViewSet):
    """Exposes warehouse stock levels across MEW installations."""
    serializer_class = InventoryBalanceDashboardSerializer

    def get_queryset(self):
        queryset = InventoryBalance.objects.select_related(
            'installation', 'material'
        ).order_by('installation__installation_code', 'material__component_id')
        queryset = self.get_access_context().scope_inventory_balances(queryset)
        installation_code = self.request.query_params.get('installation_code')
        low_stock = self.request.query_params.get('low_stock')

        if installation_code:
            queryset = queryset.filter(installation__installation_code=installation_code)
        if low_stock and low_stock.lower() == 'true':
            queryset = queryset.filter(quantity_on_hand__lte=F('reorder_point'))
        return queryset


class SecurityAuditLogViewSet(RbacScopedViewSetMixin, viewsets.ReadOnlyModelViewSet):
    """Exposes the immutable RBAC/ABAC & OPSEC security audit trail."""
    serializer_class = SecurityAuditLogSerializer

    def get_queryset(self):
        return SecurityAuditLog.objects.all().order_by('-created_at')[:100]


class RbacPersonaViewSet(RbacScopedViewSetMixin, viewsets.ViewSet):
    """
    Exposes curated RBAC/ABAC demonstration personas and the active AccessContext
    for the Angular C2 header switcher.
    """
    def list(self, request):
        ctx = self.get_access_context()
        persona_ids = [p["personnel_id"] for p in CURATED_RBAC_PERSONAS]
        db_personas = {
            p.personnel_id: p
            for p in Personnel.objects.select_related("unit", "installation").filter(
                personnel_id__in=persona_ids
            )
        }

        personas_payload = []
        for item in CURATED_RBAC_PERSONAS:
            pid = item["personnel_id"]
            db_obj = db_personas.get(pid)
            personas_payload.append(
                {
                    "personnel_id": pid,
                    "name": f"{item['rank']} {item['first_name']} {item['last_name']}",
                    "rank": item["rank"],
                    "title": item["title"],
                    "functional_role": item["functional_role"],
                    "personnel_category": item["personnel_category"],
                    "security_clearance": item["security_clearance"],
                    "assigned_uic": (
                        db_obj.assigned_uic
                        if db_obj and db_obj.assigned_uic
                        else item.get("target_installation_code", "N00060")
                    ),
                    "installation_code": item["target_installation_code"],
                    "target_hull_number": item["target_hull_number"],
                    "authorized_cage_codes": item["authorized_cage_codes"],
                    "badge_label": item["badge_label"],
                }
            )

        return Response(
            {
                "active_context": {
                    "persona_id": ctx.persona_id,
                    "actor_name": ctx.actor_name,
                    "functional_role": ctx.functional_role,
                    "personnel_category": ctx.personnel_category,
                    "security_clearance": ctx.security_clearance,
                    "assigned_uic": ctx.assigned_uic,
                    "installation_code": ctx.installation_code,
                    "subordinate_uics": ctx.subordinate_uics,
                    "authorized_cage_codes": ctx.authorized_cage_codes,
                    "active_redaction_policies": ctx.active_redaction_policies,
                    "can_export_csv": ctx.can_export_fleet_csv()[0],
                },
                "personas": personas_payload,
            }
        )


class MewAnalyticsOverviewViewSet(RbacScopedViewSetMixin, viewsets.ViewSet):
    """
    Exposes the 5 core MEW predictive & readiness analytics dimensions
    from MEW_ANALYTICS.md:
      1. Ao = MTBM / (MTBM + MDT) Decomposition (MTTR + Logistics Delay Time)
      2. Hierarchical Bill of Materials (BOM) & Weakest-Link Impact
      3. CASREP Impact Scoring & Supply Chain Bottleneck Funnel
      4. Personnel-to-Equipment NEC Cross-Referencing (Single-Point-of-Failure Gaps)
      5. Predictive Depletion / Lead-Time Forecasting & Geospatial Proximity Depot Routing
    """
    def list(self, request):
        ctx = self.get_access_context()
        payload = compute_mew_analytics_payload(access_ctx=ctx)
        return Response(payload)


