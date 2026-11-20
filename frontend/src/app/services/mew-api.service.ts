import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { runtimeConfig } from '../config/runtime';
import {
  InstallationDashboard,
  InventoryBalance,
  InventoryTransactionRecord,
  MewAnalyticsPayload,
  MewAsset,
  MewComponent,
  MilitaryUnitDashboard,
  PaginatedResponse,
  PersonnelRecord,
  RbacActiveContext,
  AuthorizationContextPayload,
  ReceiveShipmentPayload,
  ReceiveShipmentResponse,
  RequisitionSummary,
  ReadinessDashboardSummary,
  ReadinessException,
  ReadinessShortage,
  ReadinessWorkOrder,
  SecurityAuditLogRecord,
  WorkOrderSummary,
  WorkOrderLifecycleEvent,
} from '../models/mew.models';

@Injectable({
  providedIn: 'root',
})
export class MewApiService {
  private readonly http = inject(HttpClient);
  readonly apiBaseUrl = runtimeConfig.apiBaseUrl;

  // Reactive Signals State (SV-4 State Management)
  readonly loading = signal<boolean>(false);
  readonly connectionMode = signal<'LIVE_API' | 'SIMULATION_FALLBACK'>('LIVE_API');
  readonly lastSyncedAt = signal<Date>(new Date());

  readonly units = signal<MilitaryUnitDashboard[]>([]);
  readonly assets = signal<MewAsset[]>([]);
  readonly installations = signal<InstallationDashboard[]>([]);
  readonly personnel = signal<PersonnelRecord[]>([]);
  readonly components = signal<MewComponent[]>([]);
  readonly inventoryBalances = signal<InventoryBalance[]>([]);
  readonly requisitions = signal<RequisitionSummary[]>([]);
  readonly workOrders = signal<WorkOrderSummary[]>([]);
  readonly transactions = signal<InventoryTransactionRecord[]>([]);
  readonly securityAuditLogs = signal<SecurityAuditLogRecord[]>([]);
  readonly analytics = signal<MewAnalyticsPayload | null>(null);
  readonly readinessSummary = signal<ReadinessDashboardSummary | null>(null);
  readonly readinessExceptions = signal<ReadinessException[]>([]);
  readonly readinessShortages = signal<ReadinessShortage[]>([]);

  // Hybrid RBAC + ABAC Active Persona & Policy Context
  readonly activeRbacContext = signal<RbacActiveContext>({
    persona_id: 'MEW-PER-0001',
    actor_name: 'CDR Marcus Vance',
    functional_role: 'FLEET_READINESS_ANALYST',
    personnel_category: 'Active_Duty_Officer',
    security_clearance: 'TS/SCI',
    assigned_uic: 'N00060',
    installation_code: 'NAVIFOR-SUF',
    subordinate_uics: [],
    authorized_cage_codes: [],
    active_redaction_policies: [
      'HIERARCHICAL ECHELON TREE ACCESS',
      'BULK CSV EXPORT AUTHORIZED',
    ],
    can_export_csv: true,
    capabilities: {
      can_create_asset: false,
      can_update_asset: false,
      can_delete_asset: false,
      can_create_work_order: false,
      can_close_work_order: false,
      can_delete_work_order: false,
      can_create_requisition: false,
      can_delete_requisition: false,
    },
  });

  // Server-provided capabilities improve UX; the API remains the enforcement point.
  readonly crudPermissions = computed(() => {
    const ctx = this.activeRbacContext();
    const capabilities = ctx?.capabilities;
    return {
      role: ctx?.functional_role || 'UNKNOWN',
      canCreateAsset: capabilities?.can_create_asset ?? false,
      canUpdateAsset: capabilities?.can_update_asset ?? false,
      canDeleteAsset: capabilities?.can_delete_asset ?? false,
      canCreateWorkOrder: capabilities?.can_create_work_order ?? false,
      canCloseWorkOrder: capabilities?.can_close_work_order ?? false,
      canDeleteWorkOrder: capabilities?.can_delete_work_order ?? false,
      canCreateRequisition: capabilities?.can_create_requisition ?? false,
      canDeleteRequisition: capabilities?.can_delete_requisition ?? false,
    };
  });

  // Derived Fleet-Wide KPI Signals
  readonly fleetMetrics = computed(() => {
    const allAssets = this.assets();
    const totalAssets = allAssets.length;
    const fmcCount = allAssets.filter((a) => a.readiness_status === 'FMC').length;
    const pmcCount = allAssets.filter((a) => a.readiness_status === 'PMC').length;
    const nmcsCount = allAssets.filter((a) => a.readiness_status === 'NMCS').length;
    const nmcmCount = allAssets.filter((a) => a.readiness_status === 'NMCM').length;
    const dlmCount = allAssets.filter((a) => a.readiness_status === 'DLM').length;
    const nmcTotal = nmcsCount + nmcmCount + dlmCount;

    const avgAo =
      totalAssets > 0
        ? allAssets.reduce((sum, a) => sum + Number(a.operational_availability_pct || 0), 0) /
          totalAssets
        : 94.2;
    const avgMtbf =
      totalAssets > 0
        ? allAssets.reduce((sum, a) => sum + Number(a.mtbf_hours || 0), 0) / totalAssets
        : 1420.5;
    const avgMttr =
      totalAssets > 0
        ? allAssets.reduce((sum, a) => sum + Number(a.mttr_hours || 0), 0) / totalAssets
        : 18.4;

    const missionCapablePct =
      totalAssets > 0 ? ((fmcCount + pmcCount) / totalAssets) * 100 : 100;

    const allPers = this.personnel();
    const deployablePers = allPers.filter((p) => p.is_fully_deployable !== false && p.operational_status !== 'Leave').length;
    const tsSciCount = allPers.filter((p) => p.security_clearance === 'TS/SCI').length;

    const openReqs = this.requisitions().filter((r) => r.status !== 'Received');
    const urgentReqs = openReqs.filter((r) =>
      (r.priority_display || '').toLowerCase().includes('urgent')
    );
    const lowStockBalances = this.inventoryBalances().filter(
      (b) => b.needs_replenishment || b.quantity_on_hand <= b.reorder_point
    );

    return {
      totalAssets,
      fmcCount,
      pmcCount,
      nmcsCount,
      nmcmCount,
      dlmCount,
      nmcTotal,
      missionCapablePct: Number(missionCapablePct.toFixed(1)),
      avgAo: Number(avgAo.toFixed(1)),
      avgMtbf: Number(avgMtbf.toFixed(1)),
      avgMttr: Number(avgMttr.toFixed(1)),
      totalPersonnel: allPers.length,
      deployablePersonnel: deployablePers,
      tsSciCount,
      openRequisitionsCount: openReqs.length,
      urgentRequisitionsCount: urgentReqs.length,
      lowStockCount: lowStockBalances.length,
    };
  });

  private unwrap<T>(res: PaginatedResponse<T> | T[]): T[] {
    if (Array.isArray(res)) {
      return res;
    }
    if (res && Array.isArray((res as PaginatedResponse<T>).results)) {
      return (res as PaginatedResponse<T>).results;
    }
    return [];
  }

  private getHeaders(): { headers: { [header: string]: string } } {
    return { headers: {} };
  }

  loadAllTelemetry(): void {
    this.loading.set(true);
    const opts = this.getHeaders();

    forkJoin({
      rbac: this.http
        .get<AuthorizationContextPayload>(`${this.apiBaseUrl}/authorization/context/`, opts)
        .pipe(catchError(() => of(null))),
      units: this.http
        .get<PaginatedResponse<MilitaryUnitDashboard> | MilitaryUnitDashboard[]>(
          `${this.apiBaseUrl}/dashboard/units/`,
          opts
        )
        .pipe(map((r) => this.unwrap(r)), catchError(() => of(null))),
      assets: this.http
        .get<PaginatedResponse<MewAsset> | MewAsset[]>(`${this.apiBaseUrl}/assets/`, opts)
        .pipe(map((r) => this.unwrap(r)), catchError(() => of(null))),
      readinessAssets: this.http
        .get<PaginatedResponse<ReadinessException> | ReadinessException[]>(`${this.apiBaseUrl}/readiness/assets/`, opts)
        .pipe(map((r) => this.unwrap(r)), catchError(() => of(null))),
      installations: this.http
        .get<PaginatedResponse<InstallationDashboard> | InstallationDashboard[]>(
          `${this.apiBaseUrl}/readiness/reference/installations/`,
          opts
        )
        .pipe(map((r) => this.unwrap(r)), catchError(() => of(null))),
      personnel: this.http
        .get<PaginatedResponse<PersonnelRecord> | PersonnelRecord[]>(
          `${this.apiBaseUrl}/readiness/reference/personnel/`,
          opts
        )
        .pipe(map((r) => this.unwrap(r)), catchError(() => of(null))),
      components: this.http
        .get<PaginatedResponse<MewComponent> | MewComponent[]>(
          `${this.apiBaseUrl}/readiness/reference/catalog-items/`,
          opts
        )
        .pipe(map((r) => this.unwrap(r)), catchError(() => of(null))),
      balances: this.http
        .get<PaginatedResponse<InventoryBalance> | InventoryBalance[]>(
          `${this.apiBaseUrl}/inventory-balances/`,
          opts
        )
        .pipe(map((r) => this.unwrap(r)), catchError(() => of(null))),
      readinessBalances: this.http.get<PaginatedResponse<any> | any[]>(`${this.apiBaseUrl}/readiness/supply/inventory/`, opts)
        .pipe(map((r) => this.unwrap(r)), catchError(() => of(null))),
      requisitions: this.http
        .get<PaginatedResponse<RequisitionSummary> | RequisitionSummary[]>(
          `${this.apiBaseUrl}/requisitions/`,
          opts
        )
        .pipe(map((r) => this.unwrap(r)), catchError(() => of(null))),
      readinessRequisitions: this.http.get<PaginatedResponse<any> | any[]>(`${this.apiBaseUrl}/readiness/supply/requisitions/`, opts)
        .pipe(map((r) => this.unwrap(r)), catchError(() => of(null))),
      workOrders: this.http
        .get<PaginatedResponse<WorkOrderSummary> | WorkOrderSummary[]>(
          `${this.apiBaseUrl}/work-orders/`,
          opts
        )
        .pipe(map((r) => this.unwrap(r)), catchError(() => of(null))),
      readinessWorkOrders: this.http
        .get<PaginatedResponse<ReadinessWorkOrder> | ReadinessWorkOrder[]>(`${this.apiBaseUrl}/readiness/work-orders/`, opts)
        .pipe(map((r) => this.unwrap(r)), catchError(() => of(null))),
      transactions: this.http
        .get<PaginatedResponse<InventoryTransactionRecord> | InventoryTransactionRecord[]>(
          `${this.apiBaseUrl}/inventory-transactions/`,
          opts
        )
        .pipe(map((r) => this.unwrap(r)), catchError(() => of(null))),
      auditLogs: this.http
        .get<PaginatedResponse<SecurityAuditLogRecord> | SecurityAuditLogRecord[]>(
          `${this.apiBaseUrl}/readiness/reference/audit-logs/`,
          opts
        )
        .pipe(map((r) => this.unwrap(r)), catchError(() => of(null))),
      analytics: this.http
        .get<MewAnalyticsPayload>(`${this.apiBaseUrl}/analytics/overview/`, opts)
        .pipe(catchError(() => of(null))),
      readinessSummary: this.http
        .get<ReadinessDashboardSummary>(`${this.apiBaseUrl}/readiness/dashboard/summary/`, opts)
        .pipe(catchError(() => of(null))),
      readinessExceptions: this.http
        .get<ReadinessException[]>(`${this.apiBaseUrl}/readiness/dashboard/exceptions/`, opts)
        .pipe(catchError(() => of(null))),
      readinessShortages: this.http
        .get<ReadinessShortage[]>(`${this.apiBaseUrl}/readiness/dashboard/shortages/`, opts)
        .pipe(catchError(() => of(null))),
    }).subscribe((payload) => {
      const hasLiveApi = payload.assets !== null && payload.units !== null;

      if (hasLiveApi) {
        this.connectionMode.set('LIVE_API');
        if (payload.rbac) {
          this.activeRbacContext.set(payload.rbac.active_context);
        }
        this.units.set(payload.units || []);
        this.assets.set(payload.readinessAssets
          ? payload.readinessAssets.map((asset) => this.toDashboardAsset(asset))
          : (payload.assets || []));
        this.installations.set((payload.installations || []).map((installation: any) => this.toDashboardInstallation(installation)));
        this.personnel.set((payload.personnel || []).map((person: any) => this.toDashboardPersonnel(person)));
        this.components.set((payload.components || []).map((component: any) => this.toDashboardComponent(component)));
        this.inventoryBalances.set(payload.readinessBalances ? payload.readinessBalances.map((item: any) => ({
          id: item.inventory_id, installation: item.installation, installation_code: '', installation_name: '',
          material: item.part_nsn, component_id: item.part_number, nsn: item.part_nsn, nomenclature: item.nomenclature,
          component_category: '', unit_price: 0, critical_item: item.dmsms_obsolescence_risk === 'CRITICAL',
          quantity_on_hand: item.on_hand_qty, reorder_point: item.allowance_qty, safety_stock: 0,
          needs_replenishment: item.on_hand_qty < item.allowance_qty,
        })) : (payload.balances || []));
        this.requisitions.set(payload.readinessRequisitions ? payload.readinessRequisitions.map((item: any) => ({
          id: item.requisition_doc_number, document_number: item.requisition_doc_number, material_name: item.part_nsn,
          nsn: item.part_nsn, quantity: item.quantity_ordered, priority_display: item.priority_designator,
          work_order_jcn: item.work_order_id, status: item.status, estimated_delivery_date: item.estimated_delivery_date,
        })) : (payload.requisitions || []));
        this.workOrders.set(payload.readinessWorkOrders
          ? payload.readinessWorkOrders.map((workOrder) => this.toDashboardWorkOrder(workOrder))
          : (payload.workOrders || []));
        this.transactions.set(payload.transactions || []);
        this.securityAuditLogs.set((payload.auditLogs || []).map((auditLog: any) => this.toDashboardAuditLog(auditLog)));
        this.analytics.set(payload.analytics || null);
      } else {
        this.connectionMode.set('SIMULATION_FALLBACK');
        this.loadFallbackSyntheticDataset();
      }

      this.readinessSummary.set(payload.readinessSummary);
      this.readinessExceptions.set(payload.readinessExceptions || []);
      this.readinessShortages.set(payload.readinessShortages || []);

      this.lastSyncedAt.set(new Date());
      this.loading.set(false);
    });
  }

  /** Adapts Django's installation contract to the existing Angular installation views. */
  private toDashboardInstallation(installation: any): InstallationDashboard {
    const state = installation.state_code || '';
    return {
      id: installation.installation_id,
      installation_id: installation.installation_id,
      installation_code: installation.installation_code,
      name: installation.installation_name,
      dodaac: installation.alias_codes || '',
      location_display: state || installation.aor_command || '',
      region: installation.aor_command || '',
      mission_category: installation.mission_category || '',
      city: '',
      state_or_country: state,
      latitude: installation.latitude,
      longitude: installation.longitude,
      primary_ew_focus: installation.mission_category || '',
      dsn_prefix: '',
      max_ew_billets: 0,
      scif_accredited: Boolean(installation.scif_accreditation),
      rf_anechoic_chamber: Boolean(installation.open_air_spectrum_range),
      readiness_rating: installation.emcon_readiness_level || 'UNASSESSED',
      last_inspection_date: installation.last_spectrum_certification || null,
      personnel_count: Number(installation.personnel_count || 0),
      housed_assets_count: 0,
    };
  }

  /** Adapts Django's personnel + command hierarchy contract to the dashboard roster views. */
  private toDashboardPersonnel(person: any): PersonnelRecord {
    const active = person.account_status === 'ACTIVE';
    const scifAuthorized = Boolean(person.scif_badge_active);
    return {
      id: person.edipi,
      personnel_id: person.edipi,
      dod_id: person.edipi,
      first_name: person.first_name,
      middle_initial: '',
      last_name: person.last_name,
      personnel_category: person.service_branch,
      paygrade: person.rank,
      rank: person.rank,
      rank_full: person.rank,
      specialty_code: person.mos_rating_afsc || '',
      title: person.mos_title || '',
      role: person.rbac_role,
      functional_role: person.rbac_role,
      functional_role_display: String(person.rbac_role || '').replaceAll('_', ' '),
      assigned_uic: person.unit,
      installation: person.installation_code || null,
      installation_code: person.installation_code || '',
      installation_deployed_at: person.installation_name || '',
      installation_location: person.installation_state || '',
      installation_mission_category: person.unit_command || person.unit_parent_command || '',
      security_clearance: String(person.clearance_level || '').replaceAll('_', '//'),
      operational_status: person.account_status,
      is_medically_deployable: active,
      is_training_current: active,
      is_fully_deployable: active && scifAuthorized,
      date_assigned: '',
      projected_rotation_date: '',
      dsn_phone: '',
      official_email: person.mil_email || '',
    };
  }

  /** Adapts Django's EW catalog contract to the dashboard's component catalogue. */
  private toDashboardComponent(component: any): MewComponent {
    return {
      id: String(component.catalog_id),
      component_id: component.tamcn || component.nsn,
      nsn: component.nsn,
      nomenclature: component.nomenclature,
      part_number: component.tamcn || '',
      cage_code: component.cage_code || '',
      manufacturer: '',
      component_category: component.category || '',
      repairability_code: '',
      unit_of_issue: 'EA',
      unit_price: component.unit_cost_usd || 0,
      critical_item: false,
      lead_time_days: 0,
      expected_mtbf_hours: component.mtbf_hours || 0,
      compatible_system_ids: [],
    };
  }

  /** Adapts immutable Django audit records to the existing security ledger table. */
  private toDashboardAuditLog(auditLog: any): SecurityAuditLogRecord {
    const eventType = auditLog.event_type || auditLog.action_type || 'AUDIT_EVENT';
    return {
      id: String(auditLog.audit_id),
      created_at: auditLog.event_timestamp,
      actor_name: auditLog.actor_name || auditLog.actor_edipi || 'SYSTEM',
      actor_role: auditLog.actor_edipi === 'SYSTEM' ? 'SYSTEM' : 'AUDITED SUBJECT',
      actor_uic: '',
      actor_clearance: auditLog.classification_accessed || 'UNCLASSIFIED',
      actor_cage_scope: '',
      event_type: eventType,
      event_type_display: String(eventType).replaceAll('_', ' '),
      decision: 'RECORDED',
      decision_display: 'Recorded',
      resource_target: auditLog.resource_target || `${auditLog.entity_type}:${auditLog.entity_id}`,
      reason: `Immutable audit record for ${auditLog.resource_target || `${auditLog.entity_type}:${auditLog.entity_id}`}.`,
    };
  }

  /** Adapts Django's asset contract to the existing Angular read-only loadout views. */
  private toDashboardAsset(asset: ReadinessException): MewAsset {
    const readiness = asset.readiness_status.startsWith('PMC') ? 'PMC'
      : asset.readiness_status === 'DEPOT_OVERHAUL' ? 'DLM' : asset.readiness_status;
    return {
      id: asset.asset_id,
        assignment_id: asset.nsn || null,
      serial_number: asset.serial_number,
        system_id: asset.nsn || null,
      nomenclature: asset.nomenclature,
        common_name: asset.category || '',
        ew_category: asset.category || '',
        asset_class: asset.category || '',
      manufacturer: '',
        hull_number: asset.platform_hull_or_tail || null,
        vessel_name: asset.unit || null,
      ship_class: null,
        domain: asset.service_branch === 'USAF' ? 'Airborne' : 'Surface',
        mission_role_code: asset.command || 'EW-CORE',
        mission_role_name: asset.category || '',
      quantity_installed: 1,
        operational_status: asset.readiness_status,
        installation_date: asset.last_pmcs_date || null,
        assigned_unit_name: asset.unit || '',
        current_location: asset.installation_code ? {
          id: asset.installation_code, installation_id: asset.installation_code,
          installation_code: asset.installation_code, name: asset.installation_name || asset.installation_code,
          dodaac: '', location_display: asset.storage_location || '', region: '', mission_category: '',
        } : null,
      readiness_status: readiness,
      readiness_status_display: asset.readiness_status,
      engine_hours: '0',
      last_status_change: '',
      mtbf_hours: 0,
      mttr_hours: 0,
      operational_availability_pct: 0,
      active_work_orders: [],
      pending_requisitions: [],
    };
  }

  private toDashboardWorkOrder(workOrder: ReadinessWorkOrder): WorkOrderSummary {
    return {
      id: workOrder.work_order_id,
      jcn: workOrder.work_order_id,
      status: workOrder.status,
      status_display: workOrder.status,
      priority_display: workOrder.work_order_type,
      is_grounding_fault: workOrder.work_order_type === 'CASREP',
      asset_serial: workOrder.asset,
      description_of_fault: workOrder.failure_description,
      technician_name: workOrder.assigned_tech_name || 'Unassigned',
      date_opened: workOrder.opened_at,
      date_closed: workOrder.closed_at,
    };
  }

  receiveShipment(
    payload: ReceiveShipmentPayload,
    onSuccess: (res: ReceiveShipmentResponse) => void,
    onError: (errMsg: string) => void
  ): void {
    if (this.connectionMode() === 'SIMULATION_FALLBACK') {
      // Simulate receipt & short-ship backorder in local fallback state
      const currentReqs = [...this.requisitions()];
      const idx = currentReqs.findIndex((r) => r.document_number === payload.document_number);
      if (idx === -1) {
        onError(`Requisition ${payload.document_number} not found.`);
        return;
      }
      const target = { ...currentReqs[idx] };
      if (target.status === 'Received') {
        onError(`Shipment ${payload.document_number} has already been received.`);
        return;
      }
      const qtyReceived = payload.received_quantity || target.quantity;
      if (qtyReceived <= 0 || qtyReceived > target.quantity) {
        onError(`Invalid quantity. Must be between 1 and ${target.quantity}.`);
        return;
      }
      let backorderNotice: string | undefined;
      if (qtyReceived < target.quantity) {
        const diff = target.quantity - qtyReceived;
        const boDoc = `${target.document_number}-BO`;
        currentReqs.unshift({
          ...target,
          id: `bo-${Date.now()}`,
          document_number: boDoc,
          quantity: diff,
          status: 'Submitted',
        });
        target.quantity = qtyReceived;
        backorderNotice = `Short shipment. Backorder created: ${boDoc} for ${diff} units.`;
      }
      target.status = 'Received';
      currentReqs[currentReqs.findIndex((r) => r.id === target.id)] = target;
      this.requisitions.set(currentReqs);

      // Update balance and append immutable transaction
      const balances = [...this.inventoryBalances()];
      const bIdx = balances.findIndex((b) => b.nsn === target.nsn);
      let prevBal = 8;
      let newBal = 8 + qtyReceived;
      if (bIdx >= 0) {
        prevBal = balances[bIdx].quantity_on_hand;
        newBal = prevBal + qtyReceived;
        balances[bIdx] = {
          ...balances[bIdx],
          quantity_on_hand: newBal,
          needs_replenishment: newBal <= balances[bIdx].reorder_point,
        };
        this.inventoryBalances.set(balances);
      }
      this.transactions.update((txs) => [
        {
          id: `tx-${Date.now()}`,
          created_at: new Date().toISOString(),
          installation_name: 'Naval Station Norfolk',
          nsn: target.nsn,
          nomenclature: target.material_name,
          transaction_type: 'RCT',
          transaction_type_display: 'Receipt (+)',
          quantity: qtyReceived,
          previous_balance: prevBal,
          new_balance: newBal,
          reference_document: `REQ-${target.document_number}`,
          performed_by_name: 'GS-12 Logistics Specialist',
        },
        ...txs,
      ]);

      onSuccess({
        message: 'Shipment received and added to inventory.',
        document_number: target.document_number,
        material_received: target.material_name,
        quantity_added: qtyReceived,
        backorder_notice: backorderNotice,
      });
      return;
    }

    this.http
      .post<ReceiveShipmentResponse>(
        `${this.apiBaseUrl}/readiness/supply/receive-shipment/`,
        payload,
        this.getHeaders()
      )
      .subscribe({
        next: (res) => {
          this.loadAllTelemetry();
          onSuccess(res);
        },
        error: (err) => {
          const msg =
            err?.error?.error ||
            err?.message ||
            'Failed to process shipment receipt.';
          onError(msg);
        },
      });
  }

  exportFleetCsv(assetClass?: string): void {
    const cls = assetClass ? `?asset_class=${encodeURIComponent(assetClass)}` : '';
    this.http
      .get(`${this.apiBaseUrl}/dashboard/units/export_csv/${cls}`, { responseType: 'blob' })
      .subscribe((file) => {
        const link = document.createElement('a');
        link.href = URL.createObjectURL(file);
        link.download = 'mew-fleet-readiness.csv';
        link.click();
        URL.revokeObjectURL(link.href);
      });
  }

  // =========================================================================
  // PERSONA-SCOPED CRUD OPERATIONS (POST / PUT / DELETE)
  // =========================================================================

  createAsset(
    payload: { serial_number: string; readiness_status: MewAsset['readiness_status']; operational_status?: string },
    onSuccess: (msg: string) => void,
    onError: (errMsg: string) => void
  ): void {
    if (this.connectionMode() === 'SIMULATION_FALLBACK') {
      const created: MewAsset = {
        id: `sim-asset-${Date.now()}`,
        assignment_id: `ASGN-${Math.floor(1000 + Math.random() * 9000)}`,
        serial_number: payload.serial_number,
        system_id: 'SYS-101',
        nomenclature: 'AN/SLQ-32(V)6',
        common_name: 'SEWIP Block 2',
        ew_category: 'Electronic Support (ES)',
        asset_class: 'Electronic Support (ES)',
        manufacturer: 'Lockheed Martin',
        hull_number: 'DDG-51',
        vessel_name: 'USS Arleigh Burke',
        ship_class: 'Arleigh Burke-class Flight I',
        domain: 'Surface',
        mission_role_code: 'ASMD-EW',
        mission_role_name: 'Anti-Ship Missile Defense',
        quantity_installed: 1,
        operational_status: payload.operational_status || 'FOC (Full Operational Capability)',
        installation_date: new Date().toISOString().slice(0, 10),
        assigned_unit_name: 'USS Arleigh Burke (DDG-51)',
        current_location: null,
        readiness_status: payload.readiness_status,
        readiness_status_display: payload.readiness_status,
        engine_hours: '1200.00',
        last_status_change: new Date().toISOString(),
        mtbf_hours: 2450,
        mttr_hours: 4.5,
        operational_availability_pct: 99.0,
        active_work_orders: [],
        pending_requisitions: [],
      };
      this.assets.update((list) => [created, ...list]);
      onSuccess(`Created asset ${created.serial_number}.`);
      return;
    }

    this.http
      .post<MewAsset>(`${this.apiBaseUrl}/assets/`, payload, this.getHeaders())
      .subscribe({
        next: (res) => {
          this.loadAllTelemetry();
          onSuccess(`Created asset ${res.serial_number}.`);
        },
        error: (err) => onError(err?.error?.error || err?.message || 'Failed to create asset.'),
      });
  }

  updateAssetStatus(
    asset: MewAsset,
    readinessStatus: MewAsset['readiness_status'],
    onSuccess: (msg: string) => void,
    onError: (errMsg: string) => void
  ): void {
    if (this.connectionMode() === 'SIMULATION_FALLBACK') {
      this.assets.update((list) =>
        list.map((a) =>
          a.id === asset.id
            ? { ...a, readiness_status: readinessStatus, readiness_status_display: readinessStatus }
            : a
        )
      );
      onSuccess(`Updated ${asset.serial_number} to ${readinessStatus}.`);
      return;
    }

    this.http
      .put<MewAsset>(
        `${this.apiBaseUrl}/assets/${asset.id}/`,
        { ...asset, readiness_status: readinessStatus },
        this.getHeaders()
      )
      .subscribe({
        next: () => {
          this.loadAllTelemetry();
          onSuccess(`Updated ${asset.serial_number} to ${readinessStatus}.`);
        },
        error: (err) => onError(err?.error?.error || err?.message || 'Failed to update asset.'),
      });
  }

  deleteAsset(
    assetId: string,
    onSuccess: (msg: string) => void,
    onError: (errMsg: string) => void
  ): void {
    if (this.connectionMode() === 'SIMULATION_FALLBACK') {
      this.assets.update((list) => list.filter((a) => a.id !== assetId));
      onSuccess('Asset deleted.');
      return;
    }

    this.http
      .delete<void>(`${this.apiBaseUrl}/assets/${assetId}/`, this.getHeaders())
      .subscribe({
        next: () => {
          this.loadAllTelemetry();
          onSuccess('Asset deleted.');
        },
        error: (err) => onError(err?.error?.error || err?.message || 'Failed to delete asset.'),
      });
  }

  deleteWorkOrder(
    woId: string,
    onSuccess: (msg: string) => void,
    onError: (errMsg: string) => void
  ): void {
    if (this.connectionMode() === 'SIMULATION_FALLBACK') {
      this.workOrders.update((list) => list.filter((w) => w.id !== woId));
      onSuccess('Work order deleted.');
      return;
    }

    this.http
      .delete<void>(`${this.apiBaseUrl}/work-orders/${woId}/`, this.getHeaders())
      .subscribe({
        next: () => {
          this.loadAllTelemetry();
          onSuccess('Work order deleted.');
        },
        error: (err) => onError(err?.error?.error || err?.message || 'Failed to delete work order.'),
      });
  }

  deleteRequisition(
    reqId: string,
    onSuccess: (msg: string) => void,
    onError: (errMsg: string) => void
  ): void {
    if (this.connectionMode() === 'SIMULATION_FALLBACK') {
      this.requisitions.update((list) => list.filter((r) => r.id !== reqId));
      onSuccess('Requisition deleted.');
      return;
    }

    this.http
      .delete<void>(`${this.apiBaseUrl}/requisitions/${reqId}/`, this.getHeaders())
      .subscribe({
        next: () => {
          this.loadAllTelemetry();
          onSuccess('Requisition deleted.');
        },
        error: (err) => onError(err?.error?.error || err?.message || 'Failed to delete requisition.'),
      });
  }

  submitWorkOrderMaintenance(
    woId: string,
    completion: {
      completed_work: string;
      action_taken_code: string;
      initial_diagnostics: string;
      safety_tag_out: string;
      isolation_inspection: string;
      fault_identification: string;
      repair_replacement: string;
      restoration_testing: string;
      final_equipment_status: string;
      maintenance_hours: number;
    },
    onSuccess: (msg: string) => void,
    onError: (errMsg: string) => void
  ): void {
    if (this.connectionMode() === 'SIMULATION_FALLBACK') {
      const list = [...this.workOrders()];
      const idx = list.findIndex((w) => w.id === woId);
      if (idx === -1) {
        onError('Work Order not found.');
        return;
      }
      const current = { ...list[idx] };
      current.status = 'QA_PENDING';
      current.status_display = 'QA_PENDING';
      Object.assign(current, completion);
      current.maintenance_submitted_at = new Date().toISOString();
      current.tag_out_verified = true;
      current.esd_precautions_verified = true;
      if (current.requires_mdf_zeroization) {
        current.mdf_zeroization_verified = true;
      }
      list[idx] = current;
      this.workOrders.set(list);
      onSuccess(`Maintenance submitted for QA: ${current.jcn || current.id}.`);
      return;
    }

    this.http
      .post<WorkOrderSummary>(
        `${this.apiBaseUrl}/work-orders/${woId}/submit-maintenance/`,
        completion,
        this.getHeaders()
      )
      .subscribe({
        next: () => {
          this.loadAllTelemetry();
          onSuccess('Maintenance submitted for QA review.');
        },
        error: (err) => {
          onError(err?.error?.error || 'Failed to submit maintenance completion.');
        },
      });
  }

  closeWorkOrder(
    woId: string,
    onSuccess: (msg: string) => void,
    onError: (errMsg: string) => void
  ): void {
    if (this.connectionMode() === 'SIMULATION_FALLBACK') {
      this.workOrders.update((list) => list.map((workOrder) =>
        workOrder.id === woId ? { ...workOrder, status: 'CLOSED', status_display: 'CLOSED' } : workOrder));
      onSuccess('QA accepted the maintenance action and closed the MAF.');
      return;
    }

    this.http.post<WorkOrderSummary>(`${this.apiBaseUrl}/work-orders/${woId}/close/`, {}, this.getHeaders())
      .subscribe({
        next: () => {
          this.loadAllTelemetry();
          onSuccess('QA accepted the maintenance action and closed the MAF.');
        },
        error: (err) => onError(err?.error?.error || 'Failed to close work order.'),
      });
  }

  reviewWorkOrderQa(
    woId: string,
    decision: 'APPROVE' | 'REWORK',
    rationale: string,
    onSuccess: (msg: string) => void,
    onError: (errMsg: string) => void
  ): void {
    if (this.connectionMode() === 'SIMULATION_FALLBACK') {
      this.workOrders.update((list) => list.map((workOrder) => workOrder.id === woId
        ? { ...workOrder, status: decision === 'APPROVE' ? 'RETURNED_TO_SERVICE' : 'IN_WORK', status_display: decision === 'APPROVE' ? 'RETURNED_TO_SERVICE' : 'IN_WORK' }
        : workOrder));
      onSuccess(decision === 'APPROVE' ? 'QA approved; equipment returned to service.' : 'QA returned the action for rework.');
      return;
    }
    this.http.post<WorkOrderSummary>(`${this.apiBaseUrl}/work-orders/${woId}/qa-decision/`, { decision, rationale }, this.getHeaders())
      .subscribe({
        next: () => { this.loadAllTelemetry(); onSuccess(decision === 'APPROVE' ? 'QA approved; equipment returned to service.' : 'QA returned the action for rework.'); },
        error: (err) => onError(err?.error?.error || 'Failed to record QA decision.'),
      });
  }

  getWorkOrderLifecycleHistory(
    woId: string,
    onSuccess: (events: WorkOrderLifecycleEvent[]) => void,
    onError: (errMsg: string) => void
  ): void {
    if (this.connectionMode() === 'SIMULATION_FALLBACK') {
      const workOrder = this.workOrders().find((item) => item.id === woId);
      if (!workOrder) {
        onError('Work Order not found.');
        return;
      }
      const submittedAt = workOrder.maintenance_submitted_at || workOrder.date_opened;
      const events: WorkOrderLifecycleEvent[] = [];
      if (workOrder.maintenance_submitted_at || workOrder.status === 'QA_PENDING' || workOrder.status === 'CLOSED') {
        events.push({
          event_type: 'TECHNICIAN_SUBMITTED_FOR_QA', previous_status: 'IN_WORK', new_status: 'QA_PENDING',
          actor_name: workOrder.technician_name || 'Assigned technician', occurred_at: submittedAt,
          reason: 'Technician submitted completion evidence for independent QA review.',
        });
      }
      if (workOrder.status === 'RETURNED_TO_SERVICE' || workOrder.status === 'CLOSED') {
        events.push({
          event_type: 'QA_APPROVED_FOR_RETURN_TO_SERVICE', previous_status: 'QA_PENDING', new_status: 'RETURNED_TO_SERVICE',
          actor_name: workOrder.qa_inspector_name || 'Fleet readiness reviewer', occurred_at: new Date().toISOString(),
          reason: 'Reviewer approved the maintenance action and returned the equipment to service.',
        });
      }
      if (workOrder.status === 'CLOSED') events.push({ event_type: 'WORK_ORDER_CLOSED', previous_status: 'RETURNED_TO_SERVICE', new_status: 'CLOSED', actor_name: workOrder.qa_inspector_name || 'Fleet readiness reviewer', occurred_at: new Date().toISOString(), reason: 'Reviewer closed the QA-approved work order.' });
      onSuccess(events);
      return;
    }

    this.http.get<WorkOrderLifecycleEvent[]>(
      `${this.apiBaseUrl}/work-orders/${woId}/lifecycle-history/`, this.getHeaders()
    ).subscribe({
      next: onSuccess,
      error: (err) => onError(err?.error?.error || 'Failed to load lifecycle history.'),
    });
  }

  private loadFallbackSyntheticDataset(): void {
    const today = new Date();
    const makeTrends = (total: number, fmc: number, pmc: number, nmc: number) =>
      Array.from({ length: 14 }, (_, i) => {
        const d = new Date(today);
        d.setDate(today.getDate() - (13 - i));
        const variance = i % 3 === 0 ? -1 : 0;
        const f = Math.max(1, fmc + variance);
        const p = pmc;
        const n = Math.max(0, total - f - p);
        return {
          snapshot_date: d.toISOString().slice(0, 10),
          total_assets: total,
          fmc_assets: f,
          pmc_assets: p,
          nmc_assets: n,
          operational_readiness_rate: Number((((f + p) / total) * 100).toFixed(1)),
        };
      });

    const fallbackAssets: MewAsset[] = [
      {
        id: 'ast-1',
        assignment_id: 'ASGN-0001',
        serial_number: 'SN-DDG-51-SYS-101-ASGN-0001',
        system_id: 'SYS-101',
        nomenclature: 'AN/SLQ-32(V)6',
        common_name: 'SEWIP Block 2',
        ew_category: 'Electronic Support (ES)',
        asset_class: 'Electronic Support (ES)',
        manufacturer: 'Lockheed Martin',
        hull_number: 'DDG-51',
        vessel_name: 'USS Arleigh Burke',
        ship_class: 'Arleigh Burke-class Flight I',
        domain: 'Surface',
        mission_role_code: 'ISRT-SIGINT',
        mission_role_name: 'Maritime ISR & Threat Geolocation',
        quantity_installed: 1,
        operational_status: 'FOC (Full Operational Capability)',
        installation_date: '2023-04-12',
        assigned_unit_name: 'USS Arleigh Burke (DDG-51)',
        current_location: {
          id: 'inst-13',
          installation_id: 'INST-013',
          installation_code: 'NAVSTA-NORVA',
          name: 'Naval Station Norfolk',
          dodaac: 'N62688',
          location_display: 'Norfolk, VA',
          region: 'CONUS_East',
          mission_category: 'Fleet_Homeport',
        },
        readiness_status: 'NMCS',
        readiness_status_display: 'Non-Mission Capable (Supply)',
        engine_hours: 1840.5,
        last_status_change: new Date().toISOString(),
        mtbf_hours: 1380.4,
        mttr_hours: 22.5,
        operational_availability_pct: 91.4,
        active_work_orders: [
          {
            id: 'wo-101',
            description_of_fault:
              '[CASREP / NMCS] Low-Band RF Receiver/Digitizer LRU phase noise & local oscillator unlock fault',
            technician_name: 'CTT1 Vance',
            date_opened: new Date(Date.now() - 86400000 * 2).toISOString(),
            is_grounding_fault: true,
            status: 'Awaiting Parts',
          },
        ],
        pending_requisitions: [
          {
            id: 'req-101',
            document_number: 'REQ-MEW-0014',
            material_name: 'Low-Band RF Receiver/Digitizer LRU',
            nsn: '5865-01-684-1101',
            quantity: 2,
            priority_display: 'Urgent (01)',
            status: 'In Transit',
            estimated_delivery_date: '2026-11-18',
          },
        ],
      },
      {
        id: 'ast-2',
        assignment_id: 'ASGN-0002',
        serial_number: 'SN-DDG-125-SYS-102-ASGN-0002',
        system_id: 'SYS-102',
        nomenclature: 'AN/SLQ-32(V)7',
        common_name: 'SEWIP Block 3',
        ew_category: 'Electronic Attack (EA) / ES',
        asset_class: 'Electronic Attack (EA) / ES',
        manufacturer: 'Northrop Grumman',
        hull_number: 'DDG-125',
        vessel_name: 'USS Jack H. Lucas',
        ship_class: 'Arleigh Burke-class Flight III',
        domain: 'Surface',
        mission_role_code: 'ASMD-EW',
        mission_role_name: 'Anti-Ship Missile Defense',
        quantity_installed: 1,
        operational_status: 'FOC (Full Operational Capability)',
        installation_date: '2024-08-19',
        assigned_unit_name: 'USS Jack H. Lucas (DDG-125)',
        current_location: {
          id: 'inst-14',
          installation_id: 'INST-014',
          installation_code: 'NBSD-CA',
          name: 'Naval Base San Diego',
          dodaac: 'N63406',
          location_display: 'San Diego, CA',
          region: 'CONUS_West',
          mission_category: 'Fleet_Homeport',
        },
        readiness_status: 'PMC',
        readiness_status_display: 'Partially Mission Capable',
        engine_hours: 960.2,
        last_status_change: new Date().toISOString(),
        mtbf_hours: 1610.0,
        mttr_hours: 14.2,
        operational_availability_pct: 95.8,
        active_work_orders: [
          {
            id: 'wo-102',
            description_of_fault:
              '[DEGRADED / PMC] GaN AESA T/R Sub-Array Module VSWR fault on port array channel 4',
            technician_name: 'CTTC Mercer',
            date_opened: new Date(Date.now() - 86400000).toISOString(),
            is_grounding_fault: false,
            status: 'Open',
          },
        ],
        pending_requisitions: [
          {
            id: 'req-102',
            document_number: 'REQ-MEW-0019',
            material_name: 'GaN Solid-State AESA Transmit/Receive (T/R) Module',
            nsn: '5865-01-691-2201',
            quantity: 2,
            priority_display: 'Priority (02)',
            status: 'Submitted',
            estimated_delivery_date: '2026-11-22',
          },
        ],
      },
      {
        id: 'ast-3',
        assignment_id: 'ASGN-0003',
        serial_number: 'SN-CVN-78-SYS-104-ASGN-0003',
        system_id: 'SYS-104',
        nomenclature: 'Mk 53 Mod 9',
        common_name: 'Nulka Active Decoy System',
        ew_category: 'Off-Board Electronic Decoy',
        asset_class: 'Off-Board Electronic Decoy',
        manufacturer: 'BAE Systems',
        hull_number: 'CVN-78',
        vessel_name: 'USS Gerald R. Ford',
        ship_class: 'Gerald R. Ford-class',
        domain: 'Surface',
        mission_role_code: 'ASMD-EW',
        mission_role_name: 'Anti-Ship Missile Defense',
        quantity_installed: 4,
        operational_status: 'FOC (Full Operational Capability)',
        installation_date: '2022-11-05',
        assigned_unit_name: 'USS Gerald R. Ford (CVN-78)',
        current_location: {
          id: 'inst-13',
          installation_id: 'INST-013',
          installation_code: 'NAVSTA-NORVA',
          name: 'Naval Station Norfolk',
          dodaac: 'N62688',
          location_display: 'Norfolk, VA',
          region: 'CONUS_East',
          mission_category: 'Fleet_Homeport',
        },
        readiness_status: 'FMC',
        readiness_status_display: 'Fully Mission Capable',
        engine_hours: 3120.0,
        last_status_change: new Date().toISOString(),
        mtbf_hours: 2140.0,
        mttr_hours: 9.5,
        operational_availability_pct: 99.1,
        active_work_orders: [],
        pending_requisitions: [],
      },
      {
        id: 'ast-4',
        assignment_id: 'ASGN-0004',
        serial_number: 'SN-SSN-774-SYS-106-ASGN-0004',
        system_id: 'SYS-106',
        nomenclature: 'AN/BLQ-10(V)',
        common_name: 'Submarine EW Support Suite',
        ew_category: 'SIGINT / Electronic Support',
        asset_class: 'SIGINT / Electronic Support',
        manufacturer: 'Lockheed Martin',
        hull_number: 'SSN-774',
        vessel_name: 'USS Virginia',
        ship_class: 'Virginia-class Block I',
        domain: 'Subsurface',
        mission_role_code: 'ISRT-SIGINT',
        mission_role_name: 'Maritime ISR & Threat Geolocation',
        quantity_installed: 1,
        operational_status: 'FOC (Full Operational Capability)',
        installation_date: '2023-09-14',
        assigned_unit_name: 'USS Virginia (SSN-774)',
        current_location: {
          id: 'inst-3',
          installation_id: 'INST-003',
          installation_code: 'NUWC-NPT',
          name: 'Naval Undersea Warfare Center Division Newport',
          dodaac: 'N66604',
          location_display: 'Newport, RI',
          region: 'CONUS_East',
          mission_category: 'R&D_Warfare_Center',
        },
        readiness_status: 'FMC',
        readiness_status_display: 'Fully Mission Capable',
        engine_hours: 2450.8,
        last_status_change: new Date().toISOString(),
        mtbf_hours: 1890.2,
        mttr_hours: 12.0,
        operational_availability_pct: 98.4,
        active_work_orders: [],
        pending_requisitions: [],
      },
      {
        id: 'ast-5',
        assignment_id: 'ASGN-0005',
        serial_number: 'SN-VAQ-132-SYS-108-ASGN-0005',
        system_id: 'SYS-108',
        nomenclature: 'AN/ALQ-249',
        common_name: 'Next Generation Jammer Mid-Band',
        ew_category: 'Airborne Electronic Attack',
        asset_class: 'Airborne Electronic Attack',
        manufacturer: 'Raytheon',
        hull_number: 'VAQ-132',
        vessel_name: 'Scorpions (EA-18G Squadron)',
        ship_class: 'EA-18G Growler',
        domain: 'Airborne',
        mission_role_code: 'SEAD-ESCORT',
        mission_role_name: 'Standoff & Escort Radar Jamming',
        quantity_installed: 4,
        operational_status: 'IOC (Initial Operational Capability)',
        installation_date: '2025-02-10',
        assigned_unit_name: 'VAQ-132 Scorpions (EA-18G)',
        current_location: {
          id: 'inst-16',
          installation_id: 'INST-016',
          installation_code: 'NAS-WHIDBEY',
          name: 'Naval Air Station Whidbey Island',
          dodaac: 'N00620',
          location_display: 'Oak Harbor, WA',
          region: 'CONUS_West',
          mission_category: 'Fleet_Homeport',
        },
        readiness_status: 'NMCM',
        readiness_status_display: 'Non-Mission Capable (Maintenance)',
        engine_hours: 715.4,
        last_status_change: new Date().toISOString(),
        mtbf_hours: 1240.0,
        mttr_hours: 19.6,
        operational_availability_pct: 93.2,
        active_work_orders: [
          {
            id: 'wo-105',
            description_of_fault:
              '[TECH ASSIST / NMCM] ALQ-249 Pod Ram Air Turbine (RAT) variable-frequency generator voltage regulation fault',
            technician_name: 'AT2 K. Callahan',
            date_opened: new Date(Date.now() - 3600000 * 14).toISOString(),
            is_grounding_fault: true,
            status: 'In Progress',
          },
        ],
        pending_requisitions: [],
      },
      {
        id: 'ast-6',
        assignment_id: 'ASGN-0006',
        serial_number: 'SN-DDG-100-SYS-105-ASGN-0006',
        system_id: 'SYS-105',
        nomenclature: 'AN/SEQ-4',
        common_name: 'ODIN Optical Dazzler',
        ew_category: 'Directed Energy / Optical EA',
        asset_class: 'Directed Energy / Optical EA',
        manufacturer: 'NSWC Dahlgren',
        hull_number: 'DDG-100',
        vessel_name: 'USS Kidd',
        ship_class: 'Arleigh Burke-class Flight IIA',
        domain: 'Surface',
        mission_role_code: 'C-UAS-DE',
        mission_role_name: 'Counter-UAS Sensor Dazzling',
        quantity_installed: 1,
        operational_status: 'FOC (Full Operational Capability)',
        installation_date: '2023-07-22',
        assigned_unit_name: 'USS Kidd (DDG-100)',
        current_location: {
          id: 'inst-2',
          installation_id: 'INST-002',
          installation_code: 'NSWC-DD',
          name: 'Naval Surface Warfare Center Dahlgren Division',
          dodaac: 'N00178',
          location_display: 'Dahlgren, VA',
          region: 'CONUS_East',
          mission_category: 'R&D_Warfare_Center',
        },
        readiness_status: 'FMC',
        readiness_status_display: 'Fully Mission Capable',
        engine_hours: 1530.0,
        last_status_change: new Date().toISOString(),
        mtbf_hours: 1750.0,
        mttr_hours: 11.4,
        operational_availability_pct: 97.9,
        active_work_orders: [],
        pending_requisitions: [],
      },
    ];

    this.assets.set(fallbackAssets);
    this.units.set([
      {
        id: 'u-1',
        name: 'USS Arleigh Burke (DDG-51)',
        uic: 'N20001',
        command_type: 'Naval_Surface_Force',
        installation_name: 'Naval Station Norfolk',
        personnel_readiness_pct: 96.4,
        equipment_readiness_pct: 66.7,
        readiness_trends: makeTrends(3, 2, 0, 1),
        down_assets: [fallbackAssets[0]],
        fleet_avg_mtbf: 1620.4,
        fleet_avg_mttr: 17.8,
        fleet_avg_ao: 94.6,
      },
      {
        id: 'u-2',
        name: 'USS Jack H. Lucas (DDG-125)',
        uic: 'N20003',
        command_type: 'Naval_Surface_Force',
        installation_name: 'Naval Base San Diego',
        personnel_readiness_pct: 98.1,
        equipment_readiness_pct: 100.0,
        readiness_trends: makeTrends(3, 2, 1, 0),
        down_assets: [fallbackAssets[1]],
        fleet_avg_mtbf: 1740.0,
        fleet_avg_mttr: 13.5,
        fleet_avg_ao: 96.9,
      },
      {
        id: 'u-3',
        name: 'USS Gerald R. Ford (CVN-78)',
        uic: 'N20005',
        command_type: 'Naval_Surface_Force',
        installation_name: 'Naval Station Norfolk',
        personnel_readiness_pct: 99.0,
        equipment_readiness_pct: 100.0,
        readiness_trends: makeTrends(3, 3, 0, 0),
        down_assets: [],
        fleet_avg_mtbf: 2140.0,
        fleet_avg_mttr: 9.5,
        fleet_avg_ao: 99.1,
      },
      {
        id: 'u-4',
        name: 'USS Virginia (SSN-774)',
        uic: 'N20007',
        command_type: 'Naval_Subsurface_Force',
        installation_name: 'SUBASE New London, CT',
        personnel_readiness_pct: 97.5,
        equipment_readiness_pct: 100.0,
        readiness_trends: makeTrends(2, 2, 0, 0),
        down_assets: [],
        fleet_avg_mtbf: 1890.2,
        fleet_avg_mttr: 12.0,
        fleet_avg_ao: 98.4,
      },
      {
        id: 'u-5',
        name: 'VAQ-132 Scorpions (EA-18G Squadron)',
        uic: 'N20009',
        command_type: 'Naval_Airborne_Force',
        installation_name: 'Naval Air Station Whidbey Island',
        personnel_readiness_pct: 94.8,
        equipment_readiness_pct: 66.7,
        readiness_trends: makeTrends(3, 2, 0, 1),
        down_assets: [fallbackAssets[4]],
        fleet_avg_mtbf: 1310.5,
        fleet_avg_mttr: 19.6,
        fleet_avg_ao: 93.2,
      },
    ]);

    this.installations.set([
      {
        id: 'inst-1',
        installation_id: 'INST-001',
        installation_code: 'NSWC-CRANE',
        name: 'Naval Surface Warfare Center Crane Division',
        dodaac: 'N00164',
        city: 'Crane',
        state_or_country: 'IN',
        location_display: 'Crane, IN',
        region: 'CONUS_Midwest',
        latitude: 38.8912,
        longitude: -86.8378,
        mission_category: 'R&D_Warfare_Center',
        primary_ew_focus: 'Surface Ship EW (AN/SLQ-32 SEWIP) & Off-Board Decoys',
        dsn_prefix: '482',
        max_ew_billets: 450,
        scif_accredited: true,
        rf_anechoic_chamber: true,
        readiness_rating: 'C1_Fully_Mission_Capable',
        last_inspection_date: '2026-08-14',
        personnel_count: 24,
        housed_assets_count: 4,
      },
      {
        id: 'inst-2',
        installation_id: 'INST-002',
        installation_code: 'NSWC-DD',
        name: 'Naval Surface Warfare Center Dahlgren Division',
        dodaac: 'N00178',
        city: 'Dahlgren',
        state_or_country: 'VA',
        location_display: 'Dahlgren, VA',
        region: 'CONUS_East',
        latitude: 38.3489,
        longitude: -77.0436,
        mission_category: 'R&D_Warfare_Center',
        primary_ew_focus: 'Combat System Integration & Real-Time Spectrum Operations',
        dsn_prefix: '249',
        max_ew_billets: 380,
        scif_accredited: true,
        rf_anechoic_chamber: true,
        readiness_rating: 'C1_Fully_Mission_Capable',
        last_inspection_date: '2026-07-02',
        personnel_count: 19,
        housed_assets_count: 3,
      },
      {
        id: 'inst-13',
        installation_id: 'INST-013',
        installation_code: 'NAVSTA-NORVA',
        name: 'Naval Station Norfolk',
        dodaac: 'N62688',
        city: 'Norfolk',
        state_or_country: 'VA',
        location_display: 'Norfolk, VA',
        region: 'CONUS_East',
        latitude: 36.9461,
        longitude: -76.3133,
        mission_category: 'Fleet_Homeport',
        primary_ew_focus: 'Atlantic Fleet Surface Combatants (DDG/CG SEWIP)',
        dsn_prefix: '564',
        max_ew_billets: 620,
        scif_accredited: true,
        rf_anechoic_chamber: false,
        readiness_rating: 'C1_Fully_Mission_Capable',
        last_inspection_date: '2026-09-19',
        personnel_count: 38,
        housed_assets_count: 6,
      },
      {
        id: 'inst-14',
        installation_id: 'INST-014',
        installation_code: 'NBSD-CA',
        name: 'Naval Base San Diego',
        dodaac: 'N63406',
        city: 'San Diego',
        state_or_country: 'CA',
        location_display: 'San Diego, CA',
        region: 'CONUS_West',
        latitude: 32.6836,
        longitude: -117.1292,
        mission_category: 'Fleet_Homeport',
        primary_ew_focus: 'Pacific Fleet Surface Combatants (DDG/CG SEWIP)',
        dsn_prefix: '526',
        max_ew_billets: 590,
        scif_accredited: true,
        rf_anechoic_chamber: false,
        readiness_rating: 'C1_Fully_Mission_Capable',
        last_inspection_date: '2026-10-04',
        personnel_count: 34,
        housed_assets_count: 8,
      },
      {
        id: 'inst-16',
        installation_id: 'INST-016',
        installation_code: 'NAS-WHIDBEY',
        name: 'Naval Air Station Whidbey Island',
        dodaac: 'N00620',
        city: 'Oak Harbor',
        state_or_country: 'WA',
        location_display: 'Oak Harbor, WA',
        region: 'CONUS_West',
        latitude: 48.3517,
        longitude: -122.6558,
        mission_category: 'Fleet_Homeport',
        primary_ew_focus: 'Airborne Electronic Attack (EA-18G Growler Squadrons)',
        dsn_prefix: '820',
        max_ew_billets: 480,
        scif_accredited: true,
        rf_anechoic_chamber: false,
        readiness_rating: 'C2_Substantially_Mission_Capable',
        last_inspection_date: '2026-06-25',
        personnel_count: 27,
        housed_assets_count: 3,
      },
      {
        id: 'inst-17',
        installation_id: 'INST-017',
        installation_code: 'CFAY-YOKO',
        name: 'Commander, Fleet Activities Yokosuka',
        dodaac: 'N61054',
        city: 'Yokosuka',
        state_or_country: 'Japan',
        location_display: 'Yokosuka, Japan',
        region: 'INDOPACOM_FDNF',
        latitude: 35.2814,
        longitude: 139.6722,
        mission_category: 'Fleet_Homeport',
        primary_ew_focus: 'Forward-Deployed Seventh Fleet Surface & Carrier EW',
        dsn_prefix: '243',
        max_ew_billets: 410,
        scif_accredited: true,
        rf_anechoic_chamber: false,
        readiness_rating: 'C1_Fully_Mission_Capable',
        last_inspection_date: '2026-09-01',
        personnel_count: 29,
        housed_assets_count: 4,
      },
    ]);

    this.personnel.set([
      {
        id: 'per-1',
        personnel_id: 'MEW-PER-0001',
        dod_id: '1492837465',
        first_name: 'Marcus',
        middle_initial: 'J',
        last_name: 'Vance',
        personnel_category: 'Active_Duty_Enlisted',
        paygrade: 'E-6',
        rank: 'CTT1',
        rank_full: 'Cryptologic Technician (Technical) First Class',
        specialty_code: 'NEC C17A (SEWIP Block 3 Tech)',
        title: 'EW Work Center Supervisor / Master Training Specialist',
        role: 'Planned Maintenance System (PMS) Oversight & Tactical Emitter Analysis',
        installation: 'inst-13',
        installation_code: 'NAVSTA-NORVA',
        installation_deployed_at: 'Naval Station Norfolk',
        installation_location: 'Norfolk, VA',
        installation_mission_category: 'Fleet_Homeport',
        security_clearance: 'TS/SCI',
        operational_status: 'In_Garrison',
        is_medically_deployable: true,
        is_training_current: true,
        is_fully_deployable: true,
        date_assigned: '2024-05-14',
        projected_rotation_date: '2027-05-14',
        dsn_phone: '312-564-4821',
        official_email: 'marcus.j.vance14@us.navy.mil',
      },
      {
        id: 'per-2',
        personnel_id: 'MEW-PER-0002',
        dod_id: '8392019482',
        first_name: 'Elena',
        middle_initial: 'R',
        last_name: 'Rostova',
        personnel_category: 'DoD_Civilian',
        paygrade: 'GS-13',
        rank: 'CIV (GS-13)',
        rank_full: 'Civilian Electronics Engineer (GS-13)',
        specialty_code: 'GS-0855 (Electronics Engineering)',
        title: 'Lead RF / GaN AESA Systems Engineer',
        role: 'High-Power Solid-State Amplifier & DRFM Hardware Engineering',
        installation: 'inst-1',
        installation_code: 'NSWC-CRANE',
        installation_deployed_at: 'Naval Surface Warfare Center Crane Division',
        installation_location: 'Crane, IN',
        installation_mission_category: 'R&D_Warfare_Center',
        security_clearance: 'TS/SCI',
        operational_status: 'In_Garrison',
        is_medically_deployable: true,
        is_training_current: true,
        is_fully_deployable: true,
        date_assigned: '2023-02-10',
        projected_rotation_date: 'N/A (Permanent Billet)',
        dsn_phone: '312-482-7731',
        official_email: 'elena.r.rostova08@us.navy.mil',
      },
      {
        id: 'per-3',
        personnel_id: 'MEW-PER-0003',
        dod_id: '5928371049',
        first_name: 'Devon',
        middle_initial: 'K',
        last_name: 'Mercer',
        personnel_category: 'Active_Duty_Enlisted',
        paygrade: 'E-7',
        rank: 'CTTC',
        rank_full: 'Chief Cryptologic Technician (Technical)',
        specialty_code: 'NEC C19A (Senior EW Analyst)',
        title: 'EW Division Leading Chief Petty Officer (LCPO)',
        role: 'Fleet Electromagnetic Readiness & Direct Support Team Lead',
        installation: 'inst-14',
        installation_code: 'NBSD-CA',
        installation_deployed_at: 'Naval Base San Diego',
        installation_location: 'San Diego, CA',
        installation_mission_category: 'Fleet_Homeport',
        security_clearance: 'TS/SCI',
        operational_status: 'Underway_Afloat_Support',
        is_medically_deployable: true,
        is_training_current: true,
        is_fully_deployable: true,
        date_assigned: '2024-09-01',
        projected_rotation_date: '2027-09-01',
        dsn_phone: '312-526-9104',
        official_email: 'devon.k.mercer22@us.navy.mil',
      },
      {
        id: 'per-4',
        personnel_id: 'MEW-PER-0004',
        dod_id: '7748291034',
        first_name: 'Hannah',
        middle_initial: 'M',
        last_name: 'Sterling',
        personnel_category: 'Active_Duty_Officer',
        paygrade: 'O-4',
        rank: 'LCDR',
        rank_full: 'Lieutenant Commander',
        specialty_code: '1440 (Engineering Duty Officer)',
        title: 'SEWIP Fleet Modernization & T&E Officer',
        role: 'Shipyard Alteration Scheduling & Sea Range Qualification Trials',
        installation: 'inst-1',
        installation_code: 'NSWC-CRANE',
        installation_deployed_at: 'Naval Surface Warfare Center Crane Division',
        installation_location: 'Crane, IN',
        installation_mission_category: 'R&D_Warfare_Center',
        security_clearance: 'TS/SCI',
        operational_status: 'Range_Test_Detachment',
        is_medically_deployable: true,
        is_training_current: true,
        is_fully_deployable: true,
        date_assigned: '2025-01-15',
        projected_rotation_date: '2028-01-15',
        dsn_phone: '312-482-3319',
        official_email: 'hannah.m.sterling04@us.navy.mil',
      },
    ]);

    this.components.set([
      {
        id: 'cmp-201',
        component_id: 'CMP-201',
        nsn: '5865-01-684-1101',
        nomenclature: 'Low-Band RF Receiver/Digitizer LRU',
        part_number: 'LM-SEWIP-RX201',
        cage_code: '03538',
        manufacturer: 'Lockheed Martin',
        component_category: 'RF Receiver / Exciter LRU',
        repairability_code: 'Depot_Repairable_LRU',
        unit_of_issue: 'EA',
        unit_price: 48500.0,
        critical_item: true,
        lead_time_days: 21,
        expected_mtbf_hours: 3200.0,
        compatible_system_ids: ['SYS-101', 'SYS-102'],
      },
      {
        id: 'cmp-203',
        component_id: 'CMP-203',
        nsn: '5865-01-691-2201',
        nomenclature: 'GaN Solid-State AESA Transmit/Receive (T/R) Module',
        part_number: 'NG-BLK3-GAN88',
        cage_code: '76823',
        manufacturer: 'Northrop Grumman',
        component_category: 'Solid-State EA Amplifier Module',
        repairability_code: 'Depot_Repairable_LRU',
        unit_of_issue: 'EA',
        unit_price: 112000.0,
        critical_item: true,
        lead_time_days: 45,
        expected_mtbf_hours: 2400.0,
        compatible_system_ids: ['SYS-102', 'SYS-108'],
      },
      {
        id: 'cmp-206',
        component_id: 'CMP-206',
        nsn: '5865-01-540-4401',
        nomenclature: 'Mk 234 Mod 2 Nulka Active Electronic Decoy Canister',
        part_number: 'BAE-NULKA-234M2',
        cage_code: '89954',
        manufacturer: 'BAE Systems',
        component_category: 'Off-Board Electronic Decoy Round',
        repairability_code: 'Expendable_Round',
        unit_of_issue: 'EA',
        unit_price: 145000.0,
        critical_item: true,
        lead_time_days: 60,
        expected_mtbf_hours: 5000.0,
        compatible_system_ids: ['SYS-104'],
      },
      {
        id: 'cmp-208',
        component_id: 'CMP-208',
        nsn: '5865-01-702-5501',
        nomenclature: 'ODIN Multi-Spectral Laser Dazzler Optical Bench Assembly',
        part_number: 'NSWCDD-SEQ4-OB1',
        cage_code: '00178',
        manufacturer: 'NSWC Dahlgren',
        component_category: 'Directed Energy / Electro-Optical LRU',
        repairability_code: 'Depot_Repairable_LRU',
        unit_of_issue: 'EA',
        unit_price: 185000.0,
        critical_item: true,
        lead_time_days: 45,
        expected_mtbf_hours: 1950.0,
        compatible_system_ids: ['SYS-105'],
      },
      {
        id: 'cmp-210',
        component_id: 'CMP-210',
        nsn: '5865-01-655-6601',
        nomenclature: 'Submarine Photonics/EW Mast Broadband RF Downconverter',
        part_number: 'LM-BLQ10-RFD6',
        cage_code: '03538',
        manufacturer: 'Lockheed Martin',
        component_category: 'Subsurface EW Mast & RF LRU',
        repairability_code: 'Depot_Repairable_LRU',
        unit_of_issue: 'EA',
        unit_price: 91500.0,
        critical_item: true,
        lead_time_days: 35,
        expected_mtbf_hours: 2900.0,
        compatible_system_ids: ['SYS-106', 'SYS-107'],
      },
      {
        id: 'cmp-212',
        component_id: 'CMP-212',
        nsn: '6115-01-698-7701',
        nomenclature: 'ALQ-249 Ram Air Turbine (RAT) High-Capacity Power Generator',
        part_number: 'RTX-NGJ-RAT01',
        cage_code: '07933',
        manufacturer: 'Raytheon',
        component_category: 'Airborne EA Pod Power & Cooling',
        repairability_code: 'Depot_Repairable_LRU',
        unit_of_issue: 'EA',
        unit_price: 96000.0,
        critical_item: true,
        lead_time_days: 30,
        expected_mtbf_hours: 2100.0,
        compatible_system_ids: ['SYS-108'],
      },
    ]);

    this.inventoryBalances.set([
      {
        id: 'bal-1',
        installation: 'inst-1',
        installation_code: 'NSWC-CRANE',
        installation_name: 'Naval Surface Warfare Center Crane Division',
        material: 'cmp-201',
        component_id: 'CMP-201',
        nsn: '5865-01-684-1101',
        nomenclature: 'Low-Band RF Receiver/Digitizer LRU',
        component_category: 'RF Receiver / Exciter LRU',
        unit_price: 48500.0,
        critical_item: true,
        quantity_on_hand: 22,
        reorder_point: 8,
        safety_stock: 4,
        needs_replenishment: false,
      },
      {
        id: 'bal-2',
        installation: 'inst-13',
        installation_code: 'NAVSTA-NORVA',
        installation_name: 'Naval Station Norfolk',
        material: 'cmp-201',
        component_id: 'CMP-201',
        nsn: '5865-01-684-1101',
        nomenclature: 'Low-Band RF Receiver/Digitizer LRU',
        component_category: 'RF Receiver / Exciter LRU',
        unit_price: 48500.0,
        critical_item: true,
        quantity_on_hand: 3,
        reorder_point: 4,
        safety_stock: 2,
        needs_replenishment: true,
      },
      {
        id: 'bal-3',
        installation: 'inst-14',
        installation_code: 'NBSD-CA',
        installation_name: 'Naval Base San Diego',
        material: 'cmp-203',
        component_id: 'CMP-203',
        nsn: '5865-01-691-2201',
        nomenclature: 'GaN Solid-State AESA Transmit/Receive (T/R) Module',
        component_category: 'Solid-State EA Amplifier Module',
        unit_price: 112000.0,
        critical_item: true,
        quantity_on_hand: 2,
        reorder_point: 4,
        safety_stock: 2,
        needs_replenishment: true,
      },
      {
        id: 'bal-4',
        installation: 'inst-16',
        installation_code: 'NAS-WHIDBEY',
        installation_name: 'Naval Air Station Whidbey Island',
        material: 'cmp-212',
        component_id: 'CMP-212',
        nsn: '6115-01-698-7701',
        nomenclature: 'ALQ-249 Ram Air Turbine (RAT) High-Capacity Power Generator',
        component_category: 'Airborne EA Pod Power & Cooling',
        unit_price: 96000.0,
        critical_item: true,
        quantity_on_hand: 9,
        reorder_point: 4,
        safety_stock: 2,
        needs_replenishment: false,
      },
    ]);

    this.requisitions.set([
      {
        id: 'req-101',
        document_number: 'REQ-MEW-0014',
        material_name: 'Low-Band RF Receiver/Digitizer LRU',
        nsn: '5865-01-684-1101',
        quantity: 2,
        priority_display: 'Urgent (01)',
        status: 'In Transit',
        estimated_delivery_date: '2026-11-18',
      },
      {
        id: 'req-102',
        document_number: 'REQ-MEW-0019',
        material_name: 'GaN Solid-State AESA Transmit/Receive (T/R) Module',
        nsn: '5865-01-691-2201',
        quantity: 2,
        priority_display: 'Priority (02)',
        status: 'Submitted',
        estimated_delivery_date: '2026-11-22',
      },
      {
        id: 'req-103',
        document_number: 'REQ-MEW-0008',
        material_name: 'Mk 234 Mod 2 Nulka Active Electronic Decoy Canister',
        nsn: '5865-01-540-4401',
        quantity: 4,
        priority_display: 'Routine (03)',
        status: 'Received',
        estimated_delivery_date: '2026-11-10',
      },
    ]);

    this.workOrders.set([
      {
        id: 'wo-101',
        jcn: 'N2001-CE-0001',
        casrep_number: 'CASREP-26-001',
        priority_code: 1,
        priority_display: '1 - Mandatory / CASREP',
        hull_number: 'DDG-51',
        vessel_name: 'USS Arleigh Burke',
        system_nomenclature: 'AN/SLQ-32(V)6',
        eic: 'D710100',
        apl_ael: '8810101201A',
        work_center: 'CE02',
        when_discovered_code: 'D',
        when_discovered_display: 'D - During Normal Tactical Operation',
        equipment_status_code: '3',
        equipment_status_display: '3 - Non-Operational (NMCM / NMCS)',
        malfunction_code: '127',
        malfunction_display: '127 - Calibration / Phase Drift',
        description_of_fault:
          '[CASREP / NMCS] Low-Band RF Receiver/Digitizer LRU phase noise & local oscillator unlock fault',
        technician_name: 'CTT1 Vance',
        technician_clearance: 'TS/SCI',
        qa_inspector_name: 'CTTC Mercer',
        org_man_hours: 4.5,
        depot_man_hours: 0.0,
        tag_out_verified: true,
        esd_precautions_verified: true,
        cwp_controlled_work_package: false,
        requires_mdf_zeroization: true,
        mdf_zeroization_verified: false,
        date_opened: new Date(Date.now() - 86400000 * 2).toISOString(),
        is_grounding_fault: true,
        status: 'AWAITING_PARTS',
        parts_used_details: [
          {
            id: 'wop-1',
            nsn: '5865-01-684-1101',
            cage_code: '03538',
            nomenclature: 'Low-Band RF Receiver/Digitizer LRU',
            quantity: 1,
            unit_price: 48500,
            defective_serial_number: 'SN-OLD-CMP-201-4412',
            replacement_serial_number: 'SN-NEW-CMP-201-8890',
            requires_dlr_turn_in: true,
            dlr_turn_in_status: 'Pending_Turn_In',
          },
        ],
      },
      {
        id: 'wo-105',
        jcn: 'N2009-AT-0002',
        casrep_number: 'CASREP-26-002',
        priority_code: 1,
        priority_display: '1 - Mandatory / CASREP',
        hull_number: 'VAQ-132',
        vessel_name: 'Scorpions (EA-18G Squadron)',
        system_nomenclature: 'AN/ALQ-249',
        eic: 'D710800',
        apl_ael: '8810108212A',
        work_center: 'AT01',
        when_discovered_code: 'F',
        when_discovered_display: 'F - Built-In-Test (BIT) Automated Fault',
        equipment_status_code: '3',
        equipment_status_display: '3 - Non-Operational (NMCM / NMCS)',
        malfunction_code: '020',
        malfunction_display: '020 - Burned Out / Thermal Trip',
        description_of_fault:
          '[TECH ASSIST / NMCM] ALQ-249 Pod Ram Air Turbine (RAT) variable-frequency generator voltage regulation fault',
        technician_name: 'AT2 Callahan',
        technician_clearance: 'Secret',
        qa_inspector_name: 'LCDR Sterling',
        org_man_hours: 8.0,
        depot_man_hours: 12.5,
        tag_out_verified: true,
        esd_precautions_verified: true,
        cwp_controlled_work_package: true,
        requires_mdf_zeroization: false,
        mdf_zeroization_verified: false,
        date_opened: new Date(Date.now() - 3600000 * 14).toISOString(),
        is_grounding_fault: true,
        status: 'IN_WORK',
        parts_used_details: [
          {
            id: 'wop-2',
            nsn: '6115-01-698-7701',
            cage_code: '07933',
            nomenclature: 'ALQ-249 Ram Air Turbine (RAT) High-Capacity Power Generator',
            quantity: 1,
            unit_price: 96000,
            defective_serial_number: 'SN-OLD-CMP-212-1904',
            replacement_serial_number: 'SN-NEW-CMP-212-7315',
            requires_dlr_turn_in: true,
            dlr_turn_in_status: 'Pending_Turn_In',
          },
        ],
      },
      {
        id: 'wo-102',
        jcn: 'N2003-CE-0003',
        casrep_number: '',
        priority_code: 2,
        priority_display: '2 - Essential',
        hull_number: 'DDG-125',
        vessel_name: 'USS Jack H. Lucas',
        system_nomenclature: 'AN/SLQ-32(V)7',
        eic: 'D710200',
        apl_ael: '8810102203A',
        work_center: 'CE02',
        when_discovered_code: 'B',
        when_discovered_display: 'B - During Preventive Maintenance (PMS)',
        equipment_status_code: '2',
        equipment_status_display: '2 - Reduced Capability (PMC)',
        malfunction_code: '374',
        malfunction_display: '374 - Waveguide Arcing / High VSWR',
        description_of_fault:
          '[DEGRADED / PMC] GaN AESA T/R Sub-Array Module VSWR fault on port array channel 4',
        technician_name: 'CTTC Mercer',
        technician_clearance: 'TS/SCI',
        qa_inspector_name: 'CIV (GS-13) Rostova',
        org_man_hours: 3.0,
        depot_man_hours: 0.0,
        tag_out_verified: true,
        esd_precautions_verified: true,
        cwp_controlled_work_package: false,
        requires_mdf_zeroization: true,
        mdf_zeroization_verified: false,
        date_opened: new Date(Date.now() - 86400000).toISOString(),
        is_grounding_fault: false,
        status: 'OPEN',
        parts_used_details: [
          {
            id: 'wop-3',
            nsn: '5865-01-691-2201',
            cage_code: '76823',
            nomenclature: 'GaN Solid-State AESA Transmit/Receive (T/R) Module',
            quantity: 1,
            unit_price: 112000,
            defective_serial_number: 'SN-OLD-CMP-203-6610',
            replacement_serial_number: 'SN-NEW-CMP-203-9902',
            requires_dlr_turn_in: true,
            dlr_turn_in_status: 'Pending_Turn_In',
          },
        ],
      },
    ]);

    this.transactions.set([
      {
        id: 'tx-1',
        created_at: new Date(Date.now() - 3600000 * 5).toISOString(),
        installation_name: 'Naval Station Norfolk',
        nsn: '5865-01-684-1102',
        nomenclature: 'High-Band Digital Receiver/Exciter (DREX) Assembly',
        transaction_type: 'DED',
        transaction_type_display: 'Deduction (-)',
        quantity: 1,
        previous_balance: 9,
        new_balance: 8,
        reference_document: 'WO-8F29A1B4',
        performed_by_name: 'CTT1 Vance',
      },
      {
        id: 'tx-2',
        created_at: new Date(Date.now() - 3600000 * 19).toISOString(),
        installation_name: 'Naval Surface Warfare Center Crane Division',
        nsn: '5865-01-540-4401',
        nomenclature: 'Mk 234 Mod 2 Nulka Active Electronic Decoy Canister',
        transaction_type: 'RCT',
        transaction_type_display: 'Receipt (+)',
        quantity: 4,
        previous_balance: 18,
        new_balance: 22,
        reference_document: 'REQ-REQ-MEW-0008',
        performed_by_name: 'CIV (GS-12) Logistics Lead',
      },
    ]);

    this.analytics.set({
      ao_system_breakdown: [
        {
          system_id: 'SYS-101',
          nomenclature: 'AN/SLQ-32(V)6',
          common_name: 'SEWIP Block 2',
          ew_category: 'Electronic Support (ES)',
          domain: 'Surface',
          installed_count: 5,
          fmc_count: 4,
          pmc_count: 0,
          nmc_count: 1,
          mtbm_hours: 1380.4,
          mttr_hours: 22.5,
          ldt_hours: 94.5,
          mdt_hours: 117.0,
          computed_ao_pct: 92.2,
          total_operating_hours: 8420,
          swap_c_power_kw: 42.0,
          swap_c_cooling_gpm: 18.5,
          swap_c_weight_lbs: 6400,
          vswr_ratio: 1.56,
          thermal_load_pct: 75,
        },
        {
          system_id: 'SYS-102',
          nomenclature: 'AN/SLQ-32(V)7',
          common_name: 'SEWIP Block 3',
          ew_category: 'Electronic Attack (EA) / ES',
          domain: 'Surface',
          installed_count: 4,
          fmc_count: 3,
          pmc_count: 1,
          nmc_count: 0,
          mtbm_hours: 1610.0,
          mttr_hours: 14.2,
          ldt_hours: 48.0,
          mdt_hours: 62.2,
          computed_ao_pct: 96.3,
          total_operating_hours: 5190,
          swap_c_power_kw: 118.5,
          swap_c_cooling_gpm: 44.0,
          swap_c_weight_lbs: 11200,
          vswr_ratio: 1.42,
          thermal_load_pct: 78,
        },
        {
          system_id: 'SYS-104',
          nomenclature: 'Mk 53 Mod 9',
          common_name: 'Nulka Active Decoy System',
          ew_category: 'Off-Board Electronic Decoy',
          domain: 'Surface',
          installed_count: 4,
          fmc_count: 4,
          pmc_count: 0,
          nmc_count: 0,
          mtbm_hours: 2140.0,
          mttr_hours: 9.5,
          ldt_hours: 4.0,
          mdt_hours: 13.5,
          computed_ao_pct: 99.4,
          total_operating_hours: 6840,
        },
        {
          system_id: 'SYS-106',
          nomenclature: 'AN/BLQ-10(V)',
          common_name: 'Submarine EW Support Suite',
          ew_category: 'SIGINT / Electronic Support',
          domain: 'Subsurface',
          installed_count: 2,
          fmc_count: 2,
          pmc_count: 0,
          nmc_count: 0,
          mtbm_hours: 1890.2,
          mttr_hours: 12.0,
          ldt_hours: 4.0,
          mdt_hours: 16.0,
          computed_ao_pct: 99.2,
          total_operating_hours: 4920,
        },
        {
          system_id: 'SYS-108',
          nomenclature: 'AN/ALQ-249',
          common_name: 'Next Generation Jammer Mid-Band',
          ew_category: 'Airborne Electronic Attack',
          domain: 'Airborne',
          installed_count: 2,
          fmc_count: 1,
          pmc_count: 0,
          nmc_count: 1,
          mtbm_hours: 1240.0,
          mttr_hours: 19.6,
          ldt_hours: 18.0,
          mdt_hours: 37.6,
          computed_ao_pct: 97.1,
          total_operating_hours: 2810,
          swap_c_power_kw: 36.0,
          swap_c_cooling_gpm: 14.5,
          swap_c_weight_lbs: 1420,
          vswr_ratio: 1.67,
          thermal_load_pct: 92,
        },
      ],
      component_analytics: [
        {
          component_id: 'CMP-201',
          nsn: '5865-01-684-1101',
          nomenclature: 'Low-Band RF Receiver/Digitizer LRU',
          component_category: 'RF Receiver / Exciter LRU',
          manufacturer: 'Lockheed Martin',
          cage_code: '03538',
          unit_price: 48500,
          critical_item: true,
          parent_lru_id: 'CMP-202',
          parent_lru_name: 'High-Band Digital Receiver/Exciter (DREX) Assembly',
          compatible_system_ids: ['SYS-101', 'SYS-102'],
          total_fleet_stock: 25,
          total_reorder_point: 12,
          active_faults: 2,
          casrep_grounding_faults: 1,
          lead_time_days: 21,
          expected_mtbf_hours: 3200,
          monthly_demand_rate: 14.5,
          days_to_depletion: 19,
          depletion_status: 'CRITICAL_STOCKOUT_RISK',
          casrep_impact_score: 92,
        },
        {
          component_id: 'CMP-203',
          nsn: '5865-01-691-2201',
          nomenclature: 'GaN Solid-State AESA Transmit/Receive (T/R) Module',
          component_category: 'Solid-State EA Amplifier Module',
          manufacturer: 'Northrop Grumman',
          cage_code: '76823',
          unit_price: 112000,
          critical_item: true,
          parent_lru_id: null,
          parent_lru_name: 'Top-Level System LRU',
          compatible_system_ids: ['SYS-102', 'SYS-108'],
          total_fleet_stock: 18,
          total_reorder_point: 12,
          active_faults: 1,
          casrep_grounding_faults: 0,
          lead_time_days: 45,
          expected_mtbf_hours: 2400,
          monthly_demand_rate: 11.2,
          days_to_depletion: 38,
          depletion_status: 'CRITICAL_STOCKOUT_RISK',
          casrep_impact_score: 85,
        },
        {
          component_id: 'CMP-212',
          nsn: '6115-01-698-7701',
          nomenclature: 'ALQ-249 Ram Air Turbine (RAT) High-Capacity Power Generator',
          component_category: 'Airborne EA Pod Power & Cooling',
          manufacturer: 'Raytheon',
          cage_code: '07933',
          unit_price: 96000,
          critical_item: true,
          parent_lru_id: null,
          parent_lru_name: 'Top-Level System LRU',
          compatible_system_ids: ['SYS-108'],
          total_fleet_stock: 16,
          total_reorder_point: 10,
          active_faults: 1,
          casrep_grounding_faults: 1,
          lead_time_days: 30,
          expected_mtbf_hours: 2100,
          monthly_demand_rate: 8.6,
          days_to_depletion: 44,
          depletion_status: 'REORDER_WARNING',
          casrep_impact_score: 78,
        },
        {
          component_id: 'CMP-206',
          nsn: '5865-01-540-4401',
          nomenclature: 'Mk 234 Mod 2 Nulka Active Electronic Decoy Canister',
          component_category: 'Off-Board Electronic Decoy Round',
          manufacturer: 'BAE Systems',
          cage_code: '89954',
          unit_price: 145000,
          critical_item: true,
          parent_lru_id: 'CMP-207',
          parent_lru_name: 'Mk 53 Decoy Launcher Fire Control Interface Unit (FCIU)',
          compatible_system_ids: ['SYS-104'],
          total_fleet_stock: 42,
          total_reorder_point: 16,
          active_faults: 0,
          casrep_grounding_faults: 0,
          lead_time_days: 60,
          expected_mtbf_hours: 5000,
          monthly_demand_rate: 4.1,
          days_to_depletion: 140,
          depletion_status: 'SUSTAINABLE',
          casrep_impact_score: 55,
        },
      ],
      supply_bottleneck_funnel: [
        { stage: '1. Installed Fleet MEW Loadouts', count: 23, severity: 'NOMINAL' },
        { stage: '2. Active Maintenance Actions (MAFs)', count: 8, severity: 'INFO' },
        { stage: '3. Open MILSTRIP Requisitions', count: 6, severity: 'WARN' },
        { stage: '4. DLR Carcass Turn-Ins Pending Zeroization', count: 5, severity: 'WARN' },
        { stage: '5. IPD-01 CASREP Grounding Bottlenecks', count: 2, severity: 'CRIT' },
      ],
      nec_staffing_matrix: [
        {
          nec_code: 'NEC C16A',
          system_id: 'SYS-101 / SYS-103',
          system_name: 'AN/SLQ-32(V)6 SEWIP Block 2',
          total_qualified: 18,
          deployable_count: 17,
          required_billets: 16,
          coverage_pct: 100.0,
          spof_risk: 'RESILIENT',
        },
        {
          nec_code: 'NEC C17A',
          system_id: 'SYS-102',
          system_name: 'AN/SLQ-32(V)7 SEWIP Block 3 (AESA EA)',
          total_qualified: 12,
          deployable_count: 10,
          required_billets: 14,
          coverage_pct: 71.4,
          spof_risk: 'MARGINAL_COVERAGE',
        },
        {
          nec_code: 'NEC C19A',
          system_id: 'SYS-106 / SYS-107',
          system_name: 'AN/BLQ-10 Submarine EW & Senior Emitter Analysis',
          total_qualified: 6,
          deployable_count: 5,
          required_billets: 10,
          coverage_pct: 50.0,
          spof_risk: 'CRITICAL_SPOF_GAP',
        },
        {
          nec_code: 'NEC 8341',
          system_id: 'SYS-108',
          system_name: 'AN/ALQ-249 Next Gen Jammer Pod Calibration',
          total_qualified: 11,
          deployable_count: 10,
          required_billets: 12,
          coverage_pct: 83.3,
          spof_risk: 'MARGINAL_COVERAGE',
        },
      ],
      geospatial_nodes: [
        { code: 'NSWC-CRANE', name: 'NSWC Crane Division', mission_category: 'R&D_Warfare_Center', lat: 38.8912, lon: -86.8378, readiness_rating: 'C1', personnel_count: 24 },
        { code: 'NAVSTA-NORVA', name: 'Naval Station Norfolk', mission_category: 'Fleet_Homeport', lat: 36.9461, lon: -76.3133, readiness_rating: 'C1', personnel_count: 38 },
        { code: 'NBSD-CA', name: 'Naval Base San Diego', mission_category: 'Fleet_Homeport', lat: 32.6836, lon: -117.1292, readiness_rating: 'C1', personnel_count: 34 },
        { code: 'NAS-WHIDBEY', name: 'NAS Whidbey Island', mission_category: 'Fleet_Homeport', lat: 48.3517, lon: -122.6558, readiness_rating: 'C2', personnel_count: 27 },
        { code: 'PMRF-HI', name: 'PMRF Barking Sands', mission_category: 'Test_Evaluation_Range', lat: 22.0228, lon: -159.785, readiness_rating: 'C1', personnel_count: 12 },
        { code: 'CFAY-YOKO', name: 'CFA Yokosuka', mission_category: 'Fleet_Homeport', lat: 35.2814, lon: 139.6722, readiness_rating: 'C1', personnel_count: 29 },
      ],
      lifecycle_metrics: {
        qa_pending: 2,
        rework_events: 1,
        returned_to_service: 1,
        closed: 5,
        transition_events: 14,
        avg_qa_turnaround_hours: 5.5,
        avg_closure_turnaround_hours: 1.2,
        avg_qa_aging_hours: 3.8,
        rework_rate_pct: 16.7,
      },
      proximity_depot_routes: [
        {
          hull_number: 'DDG-51',
          vessel_name: 'USS Arleigh Burke',
          readiness_status: 'NMCS',
          system_nomenclature: 'AN/SLQ-32(V)6',
          origin_homeport: 'NAVSTA-NORVA',
          origin_lat: 36.9461,
          origin_lon: -76.3133,
          needed_component_id: 'CMP-201',
          needed_component_name: 'Low-Band RF Receiver/Digitizer LRU',
          nearest_depot_code: 'NSWC-CRANE',
          nearest_depot_name: 'Naval Surface Warfare Center Crane Division',
          depot_qty_available: 22,
          distance_nm: 512.4,
          estimated_transit_hours: 6.3,
        },
        {
          hull_number: 'VAQ-132',
          vessel_name: 'Scorpions (EA-18G Squadron)',
          readiness_status: 'NMCM',
          system_nomenclature: 'AN/ALQ-249',
          origin_homeport: 'NAS-WHIDBEY',
          origin_lat: 48.3517,
          origin_lon: -122.6558,
          needed_component_id: 'CMP-212',
          needed_component_name: 'ALQ-249 Ram Air Turbine (RAT) Power Generator',
          nearest_depot_code: 'NBSD-CA',
          nearest_depot_name: 'Naval Base San Diego',
          depot_qty_available: 9,
          distance_nm: 974.2,
          estimated_transit_hours: 8.9,
        },
      ],
    });
  }
}
