import { Component, OnInit, inject, signal, computed, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { MewApiService } from './services/mew-api.service';
import { ThemeService } from './services/theme.service';
import { KeycloakAuthService } from './services/keycloak-auth.service';
import {
  AoSystemBreakdown,
  ComponentAnalyticsItem,
  InstallationDashboard,
  InventoryBalance,
  InventoryTransactionRecord,
  MewAsset,
  MewViewpoint,
  MilitaryUnitDashboard,
  OperationalDomainFilter,
  PersonnelRecord,
  ProximityDepotRoute,
  RequisitionSummary,
  WorkOrderSummary,
} from './models/mew.models';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
})
export class AppComponent implements OnInit {
  readonly api = inject(MewApiService);
  readonly themeService = inject(ThemeService);
  readonly auth = inject(KeycloakAuthService);

  // Active Navigation Viewpoint (Default to Predictive Analytics & Visualizations)
  readonly activeViewpoint = signal<MewViewpoint>('analytics');

  // =========================================================================
  // 0. INTERACTIVE ANALYTICS & VISUALIZATIONS STATE (MEW_ANALYTICS.md)
  // =========================================================================
  readonly analyticsSubTab = signal<'ao' | 'depletion' | 'staffing'>('ao');
  readonly selectedAnalyticsSystem = signal<AoSystemBreakdown | null>(null);
  readonly selectedAnalyticsComponent = signal<ComponentAnalyticsItem | null>(null);
  readonly selectedDepotRoute = signal<ProximityDepotRoute | null>(null);
  readonly optempoMultiplier = signal<number>(1.0);
  readonly analyticsDomainFilter = signal<'ALL' | 'Surface' | 'Subsurface' | 'Airborne' | 'Ground'>('ALL');

  readonly filteredAoSystems = computed(() => {
    const an = this.api.analytics();
    if (!an) return [];
    const dom = this.analyticsDomainFilter();
    return dom === 'ALL'
      ? an.ao_system_breakdown
      : an.ao_system_breakdown.filter((s) => s.domain === dom);
  });

  readonly simulatedComponentAnalytics = computed(() => {
    const an = this.api.analytics();
    if (!an) return [];
    const mult = this.optempoMultiplier();
    return an.component_analytics.map((cmp) => {
      const adjDemand = Number((cmp.monthly_demand_rate * mult).toFixed(2));
      const adjDays = Math.max(
        2,
        Math.min(180, Math.round(cmp.days_to_depletion / mult))
      );
      let adjStatus: string = 'SUSTAINABLE';
      if (adjDays <= cmp.lead_time_days) {
        adjStatus = 'CRITICAL_STOCKOUT_RISK';
      } else if (adjDays <= Math.round(cmp.lead_time_days * 1.6)) {
        adjStatus = 'REORDER_WARNING';
      }
      return {
        ...cmp,
        monthly_demand_rate: adjDemand,
        days_to_depletion: adjDays,
        depletion_status: adjStatus,
      };
    });
  });

  // Global Domain Filter
  readonly selectedDomain = signal<OperationalDomainFilter>('ALL');

  // =========================================================================
  // 1. FLEET READINESS MASTER-DETAIL STATE
  // =========================================================================
  readonly unitSearchQuery = signal<string>('');
  readonly selectedUnit = signal<MilitaryUnitDashboard | null>(null);

  // =========================================================================
  // 2. HULL & MEW SYSTEM LOADOUTS MASTER-DETAIL STATE
  // =========================================================================
  readonly assetSearchQuery = signal<string>('');
  readonly selectedReadinessFilter = signal<string>('ALL');
  readonly selectedEwCategoryFilter = signal<string>('ALL');
  readonly selectedBranchFilter = signal<string>('ALL');
  readonly selectedAsset = signal<MewAsset | null>(null);

  // =========================================================================
  // 3. SUPPLY, LRUs & MAINTENANCE MASTER-DETAIL STATE
  // =========================================================================
  readonly supplySubTab = signal<'inventory' | 'requisitions' | 'workorders'>('inventory');
  readonly inventorySearchQuery = signal<string>('');
  readonly showOnlyLowStock = signal<boolean>(false);
  readonly selectedBalance = signal<InventoryBalance | null>(null);
  readonly selectedRequisition = signal<RequisitionSummary | null>(null);
  readonly selectedWorkOrder = signal<WorkOrderSummary | null>(null);
  readonly mafActionFeedback = signal<string | null>(null);

  // =========================================================================
  // 4. SHORE INSTALLATIONS & PERSONNEL MASTER-DETAIL STATE
  // =========================================================================
  readonly installationSearchQuery = signal<string>('');
  readonly selectedInstallationCode = signal<string>('ALL');
  readonly installationDetailTab = signal<'personnel' | 'assets'>('personnel');
  readonly selectedPersonnelCategory = signal<string>('ALL');
  readonly selectedClearanceFilter = signal<string>('ALL');
  readonly selectedOpStatusFilter = signal<string>('ALL');
  readonly personnelSearchQuery = signal<string>('');
  readonly selectedPersonnel = signal<PersonnelRecord | null>(null);

  // =========================================================================
  // 5. IMMUTABLE AUDIT LEDGER MASTER-DETAIL STATE
  // =========================================================================
  readonly ledgerSubTab = signal<'inventory' | 'security'>('inventory');
  readonly ledgerTypeFilter = signal<string>('ALL');
  readonly ledgerSearchQuery = signal<string>('');
  readonly selectedTransaction = signal<InventoryTransactionRecord | null>(null);

  // =========================================================================
  // BARCODE SCANNER / SHIPMENT RECEIVER STATE (Docked + Modal)
  // =========================================================================
  readonly scannerModalOpen = signal<boolean>(false);
  readonly scannerDocNumber = signal<string>('');
  readonly scannerReceivedQty = signal<number | null>(null);
  readonly scannerFeedback = signal<{
    type: 'success' | 'error';
    text: string;
    subtext?: string;
  } | null>(null);

  // =========================================================================
  // PERSONA-SCOPED ASSET CRUD MODAL & FEEDBACK STATE
  // =========================================================================
  readonly newAssetModalOpen = signal<boolean>(false);
  readonly newAssetSerial = signal<string>('');
  readonly newAssetReadiness = signal<MewAsset['readiness_status']>('FMC');
  readonly crudFeedback = signal<{ type: 'success' | 'error'; text: string } | null>(null);

  constructor() {
    // Automatically keep default detail selections populated whenever telemetry loads or filters change
    effect(
      () => {
        const units = this.filteredUnits();
        if (units.length && (!this.selectedUnit() || !units.some((u) => u.id === this.selectedUnit()?.id))) {
          this.selectedUnit.set(units[0]);
        }

        const assets = this.filteredAssets();
        if (assets.length && (!this.selectedAsset() || !assets.some((a) => a.id === this.selectedAsset()?.id))) {
          this.selectedAsset.set(assets[0]);
        }

        const balances = this.filteredInventoryBalances();
        if (
          balances.length &&
          (!this.selectedBalance() || !balances.some((b) => b.id === this.selectedBalance()?.id))
        ) {
          this.selectedBalance.set(balances[0]);
        }

        const pers = this.filteredPersonnel();
        if (
          pers.length &&
          (!this.selectedPersonnel() || !pers.some((p) => p.id === this.selectedPersonnel()?.id))
        ) {
          this.selectedPersonnel.set(pers[0]);
        }

        const txs = this.filteredTransactions();
        if (
          txs.length &&
          (!this.selectedTransaction() || !txs.some((t) => t.id === this.selectedTransaction()?.id))
        ) {
          this.selectedTransaction.set(txs[0]);
        }

        const wos = this.api.workOrders();
        if (
          wos.length &&
          (!this.selectedWorkOrder() || !wos.some((w) => w.id === this.selectedWorkOrder()?.id))
        ) {
          this.selectedWorkOrder.set(wos[0]);
        }

        const pendingReqs = this.pendingRequisitionsList();
        if (pendingReqs.length && !this.scannerDocNumber()) {
          this.scannerDocNumber.set(pendingReqs[0].document_number);
          this.scannerReceivedQty.set(pendingReqs[0].quantity);
        }

        const analytics = this.api.analytics();
        if (analytics) {
          if (
            analytics.ao_system_breakdown.length &&
            (!this.selectedAnalyticsSystem() ||
              !analytics.ao_system_breakdown.some(
                (s) => s.system_id === this.selectedAnalyticsSystem()?.system_id
              ))
          ) {
            this.selectedAnalyticsSystem.set(analytics.ao_system_breakdown[0]);
          }
          if (
            analytics.component_analytics.length &&
            (!this.selectedAnalyticsComponent() ||
              !analytics.component_analytics.some(
                (c) => c.component_id === this.selectedAnalyticsComponent()?.component_id
              ))
          ) {
            this.selectedAnalyticsComponent.set(analytics.component_analytics[0]);
          }
          if (analytics.proximity_depot_routes.length && !this.selectedDepotRoute()) {
            this.selectedDepotRoute.set(analytics.proximity_depot_routes[0]);
          }
        }
      },
      { allowSignalWrites: true }
    );
  }

  // =========================================================================
  // COMPUTED COLLECTIONS & DETAIL CONTEXT
  // =========================================================================
  readonly ewCategories = computed(() => {
    const set = new Set<string>();
    this.api.assets().forEach((a) => {
      if (a.ew_category) set.add(a.ew_category);
    });
    return Array.from(set).sort();
  });

  readonly filteredUnits = computed(() => {
    const domain = this.selectedDomain();
    const branch = this.selectedBranchFilter();
    const q = this.unitSearchQuery().trim().toLowerCase();
    return this.api.units().filter((u) => {
      if (domain !== 'ALL' && !(u.command_type || '').toLowerCase().includes(domain.toLowerCase())) {
        return false;
      }
      if (branch !== 'ALL') {
        const uic = (u.uic || '').toUpperCase();
        const unitBranch = uic.startsWith('M') ? 'USMC' : uic.startsWith('W') ? 'USA' : uic.startsWith('F') ? 'USAF' : 'USN';
        if (unitBranch !== branch) return false;
      }
      if (q) {
        const hay = `${u.name} ${u.uic} ${u.command_type} ${u.installation_name}`.toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });
  });

  readonly filteredAssets = computed(() => {
    const domain = this.selectedDomain();
    const readiness = this.selectedReadinessFilter();
    const category = this.selectedEwCategoryFilter();
    const branch = this.selectedBranchFilter();
    const q = this.assetSearchQuery().trim().toLowerCase();

    return this.api.assets().filter((a) => {
      if (domain !== 'ALL' && a.domain !== domain) return false;
      if (readiness !== 'ALL' && a.readiness_status !== readiness) return false;
      if (category !== 'ALL' && a.ew_category !== category) return false;
      if (branch !== 'ALL' && (a.service_branch || 'USN') !== branch) return false;
      if (q) {
        const hay = `${a.serial_number} ${a.nomenclature} ${a.common_name} ${a.hull_number || ''} ${
          a.vessel_name || ''
        } ${a.mission_role_code || ''} ${a.current_location?.name || ''}`.toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });
  });

  readonly downAssetsList = computed(() => {
    const domain = this.selectedDomain();
    const selUnit = this.selectedUnit();
    const allDown = this.api
      .assets()
      .filter(
        (a) =>
          a.readiness_status !== 'FMC' &&
          (domain === 'ALL' || a.domain === domain)
      );
    if (selUnit) {
      const unitMatches = allDown.filter(
        (a) =>
          a.assigned_unit_name === selUnit.name ||
          (a.vessel_name && selUnit.name.includes(a.vessel_name))
      );
      if (unitMatches.length > 0) return unitMatches;
    }
    return allDown;
  });

  // 30-Day Readiness Trend Series for Selected Unit
  readonly activeTrendSeries = computed(() => {
    const unit = this.selectedUnit() || this.filteredUnits()[0];
    if (!unit || !unit.readiness_trends?.length) {
      return [];
    }
    return [...unit.readiness_trends].sort((a, b) =>
      a.snapshot_date.localeCompare(b.snapshot_date)
    );
  });

  readonly trendSvgPolyline = computed(() => {
    const points = this.activeTrendSeries();
    if (!points.length) return '';
    const width = 520;
    const height = 115;
    const stepX = points.length > 1 ? width / (points.length - 1) : width;

    return points
      .map((pt, idx) => {
        const x = Math.round(idx * stepX);
        const rate = Math.max(0, Math.min(100, Number(pt.operational_readiness_rate || 0)));
        const y = Math.round(height - (rate / 100) * (height - 20) - 10);
        return `${x},${y}`;
      })
      .join(' ');
  });

  readonly trendSvgArea = computed(() => {
    const line = this.trendSvgPolyline();
    if (!line) return '';
    return `0,115 ${line} 520,115`;
  });

  // Compatible Components for Selected Asset in Loadout View
  readonly selectedAssetCompatibleComponents = computed(() => {
    const asset = this.selectedAsset();
    if (!asset || !asset.system_id) return [];
    return this.api
      .components()
      .filter((c) => (c.compatible_system_ids || []).includes(asset.system_id!));
  });

  // Selected Component Catalog Metadata in Supply View
  readonly selectedBalanceCatalogDetail = computed(() => {
    const bal = this.selectedBalance();
    if (!bal) return null;
    return (
      this.api.components().find((c) => c.nsn === bal.nsn || c.component_id === bal.component_id) ||
      null
    );
  });

  readonly filteredInstallationsList = computed(() => {
    const q = this.installationSearchQuery().trim().toLowerCase();
    return this.api.installations().filter((inst) => {
      if (!q) return true;
      const hay = `${inst.installation_code || ''} ${inst.name} ${inst.city || ''} ${
        inst.state_or_country || ''
      } ${inst.mission_category || ''} ${inst.primary_ew_focus || ''}`.toLowerCase();
      return hay.includes(q);
    });
  });

  readonly activeInstallationDetail = computed<InstallationDashboard | null>(() => {
    const code = this.selectedInstallationCode();
    const list = this.api.installations();
    if (!list.length) return null;
    if (code === 'ALL') return null;
    return list.find((i) => i.installation_code === code || i.dodaac === code) || null;
  });

  readonly filteredPersonnel = computed(() => {
    const instCode = this.selectedInstallationCode();
    const cat = this.selectedPersonnelCategory();
    const clr = this.selectedClearanceFilter();
    const op = this.selectedOpStatusFilter();
    const q = this.personnelSearchQuery().trim().toLowerCase();

    return this.api.personnel().filter((p) => {
      if (instCode !== 'ALL' && p.installation_code !== instCode) return false;
      if (cat !== 'ALL' && p.personnel_category !== cat) return false;
      if (clr !== 'ALL' && p.security_clearance !== clr) return false;
      if (op !== 'ALL' && p.operational_status !== op) return false;
      if (q) {
        const hay = `${p.personnel_id} ${p.dod_id} ${p.rank} ${p.first_name} ${p.last_name} ${p.specialty_code} ${p.title} ${p.installation_code}`.toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });
  });

  readonly installationHousedAssets = computed(() => {
    const code = this.selectedInstallationCode();
    if (code === 'ALL') return this.api.assets();
    return this.api
      .assets()
      .filter(
        (a) =>
          a.current_location?.installation_code === code ||
          a.current_location?.dodaac === code
      );
  });

  readonly selectedPersonnelWorkOrders = computed(() => {
    const p = this.selectedPersonnel();
    if (!p) return [];
    const last = p.last_name.toLowerCase();
    return this.api
      .workOrders()
      .filter((wo) => (wo.technician_name || '').toLowerCase().includes(last));
  });

  readonly filteredInventoryBalances = computed(() => {
    const q = this.inventorySearchQuery().trim().toLowerCase();
    const lowOnly = this.showOnlyLowStock();

    return this.api.inventoryBalances().filter((b) => {
      if (lowOnly && !b.needs_replenishment && b.quantity_on_hand > b.reorder_point) {
        return false;
      }
      if (q) {
        const hay = `${b.component_id} ${b.nsn} ${b.nomenclature} ${b.installation_code} ${b.installation_name} ${b.component_category}`.toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });
  });

  readonly filteredTransactions = computed(() => {
    const tType = this.ledgerTypeFilter();
    const q = this.ledgerSearchQuery().trim().toLowerCase();

    return this.api.transactions().filter((tx) => {
      if (tType !== 'ALL' && tx.transaction_type !== tType) return false;
      if (q) {
        const hay = `${tx.reference_document || ''} ${tx.nsn} ${tx.nomenclature} ${tx.installation_name} ${tx.performed_by_name}`.toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });
  });

  readonly pendingRequisitionsList = computed(() =>
    this.api.requisitions().filter((r) => r.status !== 'Received')
  );

  ngOnInit(): void {
    this.api.loadAllTelemetry();
  }

  setViewpoint(vp: MewViewpoint): void {
    this.activeViewpoint.set(vp);
  }

  setDomainFilter(domain: OperationalDomainFilter): void {
    this.selectedDomain.set(domain);
  }

  selectUnit(unit: MilitaryUnitDashboard): void {
    this.selectedUnit.set(unit);
  }

  selectAsset(asset: MewAsset): void {
    this.selectedAsset.set(asset);
    this.activeViewpoint.set('loadouts');
  }

  selectInstallation(code: string): void {
    this.selectedInstallationCode.set(code);
  }

  selectPersonnelRecord(person: PersonnelRecord): void {
    this.selectedPersonnel.set(person);
  }

  selectBalanceRecord(bal: InventoryBalance): void {
    this.selectedBalance.set(bal);
  }

  selectWorkOrderRecord(wo: WorkOrderSummary): void {
    this.selectedWorkOrder.set(wo);
    this.mafActionFeedback.set(null);
  }

  advanceMafStatus(wo: WorkOrderSummary): void {
    this.api.advanceWorkOrderStatus(
      wo.id,
      (msg) => {
        this.mafActionFeedback.set(msg);
      },
      (err) => {
        this.mafActionFeedback.set(err);
      }
    );
  }

  selectRequisitionForReceipt(req: RequisitionSummary): void {
    this.selectedRequisition.set(req);
    this.scannerDocNumber.set(req.document_number);
    this.scannerReceivedQty.set(req.quantity);
    this.scannerFeedback.set(null);
  }

  openScannerModal(req?: RequisitionSummary): void {
    this.scannerFeedback.set(null);
    if (req) {
      this.scannerDocNumber.set(req.document_number);
      this.scannerReceivedQty.set(req.quantity);
    } else {
      const firstOpen = this.pendingRequisitionsList()[0];
      this.scannerDocNumber.set(firstOpen ? firstOpen.document_number : '');
      this.scannerReceivedQty.set(firstOpen ? firstOpen.quantity : 1);
    }
    this.scannerModalOpen.set(true);
  }

  closeScannerModal(): void {
    this.scannerModalOpen.set(false);
    this.scannerFeedback.set(null);
  }

  onScannerRequisitionChange(docNum: string): void {
    this.scannerDocNumber.set(docNum);
    const match = this.api.requisitions().find((r) => r.document_number === docNum);
    if (match) {
      this.scannerReceivedQty.set(match.quantity);
    }
  }

  submitShipmentReceipt(): void {
    const doc = this.scannerDocNumber().trim();
    if (!doc) {
      this.scannerFeedback.set({
        type: 'error',
        text: 'Please enter or select a MILSTRIP Requisition Document Number.',
      });
      return;
    }

    this.api.receiveShipment(
      {
        document_number: doc,
        received_quantity: this.scannerReceivedQty(),
      },
      (res) => {
        this.scannerFeedback.set({
          type: 'success',
          text: `${res.message} (+${res.quantity_added} ${res.material_received})`,
          subtext: res.backorder_notice,
        });
      },
      (errMsg) => {
        this.scannerFeedback.set({
          type: 'error',
          text: errMsg,
        });
      }
    );
  }

  readinessBadgeClass(status: string): string {
    switch (status) {
      case 'FMC':
        return 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/40';
      case 'PMC':
        return 'bg-amber-500/15 text-amber-400 border border-amber-500/40';
      case 'NMCS':
      case 'NMCM':
        return 'bg-rose-500/15 text-rose-400 border border-rose-500/40';
      case 'DLM':
        return 'bg-purple-500/15 text-purple-400 border border-purple-500/40';
      default:
        return 'bg-slate-500/15 text-slate-300 border border-slate-500/40';
    }
  }

  domainBadgeClass(domain: string | null): string {
    switch (domain) {
      case 'Surface':
        return 'bg-cyan-500/15 text-cyan-400 border border-cyan-500/30';
      case 'Subsurface':
        return 'bg-indigo-500/15 text-indigo-400 border border-indigo-500/30';
      case 'Airborne':
        return 'bg-sky-500/15 text-sky-400 border border-sky-500/30';
      default:
        return 'bg-slate-500/15 text-slate-400 border border-slate-500/30';
    }
  }

  priorityBadgeClass(priorityDisplay: string): string {
    const p = (priorityDisplay || '').toLowerCase();
    if (p.includes('urgent')) {
      return 'bg-rose-500/15 text-rose-400 border border-rose-500/40';
    }
    if (p.includes('priority')) {
      return 'bg-amber-500/15 text-amber-400 border border-amber-500/40';
    }
    return 'bg-cyan-500/15 text-cyan-400 border border-cyan-500/30';
  }

  // =========================================================================
  // GEOSPATIAL SVG NETWORK MAP PROJECTION HELPERS (560x220 Tactical Canvas)
  // =========================================================================
  projectLonToX(lon: number | null): number {
    if (lon === null || lon === undefined) return 280;
    // Wrap Pacific FDNF (Yokosuka ~139E -> -220) so INDOPACOM sits cleanly on the left
    const adjustedLon = lon > 0 ? lon - 360 : lon;
    // Map range [-230 .. -65] -> [30 .. 530]
    const clamped = Math.max(-230, Math.min(-65, adjustedLon));
    return Math.round(30 + ((clamped + 230) / 165) * 500);
  }

  projectLatToY(lat: number | null): number {
    if (lat === null || lat === undefined) return 110;
    // Map latitude [18 .. 52] -> [195 .. 25]
    const clamped = Math.max(18, Math.min(52, lat));
    return Math.round(195 - ((clamped - 18) / 34) * 170);
  }

  getNodeCoords(code: string): { x: number; y: number } {
    const node = this.api.analytics()?.geospatial_nodes.find((n) => n.code === code);
    if (!node) return { x: 280, y: 110 };
    return {
      x: this.projectLonToX(node.lon),
      y: this.projectLatToY(node.lat),
    };
  }

  trackByPersonaId(_index: number, item: { personnel_id: string }): string {
    return item.personnel_id;
  }

  submitCreateAsset(): void {
    const serial = this.newAssetSerial().trim();
    if (!serial) {
      this.crudFeedback.set({ type: 'error', text: 'Serial number is required.' });
      return;
    }
    this.api.createAsset(
      { serial_number: serial, readiness_status: this.newAssetReadiness() },
      (msg) => {
        this.crudFeedback.set({ type: 'success', text: msg });
        this.newAssetSerial.set('');
        this.newAssetModalOpen.set(false);
      },
      (err) => this.crudFeedback.set({ type: 'error', text: err })
    );
  }

  changeAssetReadiness(asset: MewAsset, status: MewAsset['readiness_status']): void {
    this.api.updateAssetStatus(
      asset,
      status,
      (msg) => this.crudFeedback.set({ type: 'success', text: msg }),
      (err) => this.crudFeedback.set({ type: 'error', text: err })
    );
  }

  removeAsset(asset: MewAsset): void {
    this.api.deleteAsset(
      asset.id,
      (msg) => {
        this.crudFeedback.set({ type: 'success', text: msg });
        this.selectedAsset.set(null);
      },
      (err) => this.crudFeedback.set({ type: 'error', text: err })
    );
  }
}
