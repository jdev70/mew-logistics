import { Component, OnInit, inject, signal, computed, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { MewApiService } from './services/mew-api.service';
import { ThemeService } from './services/theme.service';
import {
  InstallationDashboard,
  InventoryBalance,
  InventoryTransactionRecord,
  MewAsset,
  MewViewpoint,
  MilitaryUnitDashboard,
  OperationalDomainFilter,
  PersonnelRecord,
  RequisitionSummary,
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

  // Active Navigation Viewpoint
  readonly activeViewpoint = signal<MewViewpoint>('readiness');

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
  readonly selectedAsset = signal<MewAsset | null>(null);

  // =========================================================================
  // 3. SUPPLY, LRUs & MAINTENANCE MASTER-DETAIL STATE
  // =========================================================================
  readonly supplySubTab = signal<'inventory' | 'requisitions' | 'workorders'>('inventory');
  readonly inventorySearchQuery = signal<string>('');
  readonly showOnlyLowStock = signal<boolean>(false);
  readonly selectedBalance = signal<InventoryBalance | null>(null);
  readonly selectedRequisition = signal<RequisitionSummary | null>(null);

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

        const pendingReqs = this.pendingRequisitionsList();
        if (pendingReqs.length && !this.scannerDocNumber()) {
          this.scannerDocNumber.set(pendingReqs[0].document_number);
          this.scannerReceivedQty.set(pendingReqs[0].quantity);
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
    const q = this.unitSearchQuery().trim().toLowerCase();
    return this.api.units().filter((u) => {
      if (domain !== 'ALL' && !(u.command_type || '').toLowerCase().includes(domain.toLowerCase())) {
        return false;
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
    const q = this.assetSearchQuery().trim().toLowerCase();

    return this.api.assets().filter((a) => {
      if (domain !== 'ALL' && a.domain !== domain) return false;
      if (readiness !== 'ALL' && a.readiness_status !== readiness) return false;
      if (category !== 'ALL' && a.ew_category !== category) return false;
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
}
