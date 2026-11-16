import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { MewApiService } from './services/mew-api.service';
import { ThemeService } from './services/theme.service';
import {
  MewAsset,
  MewViewpoint,
  OperationalDomainFilter,
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

  // Global & View Filters
  readonly selectedDomain = signal<OperationalDomainFilter>('ALL');
  readonly selectedUnitId = signal<string>('ALL');

  // Loadout Matrix Filters
  readonly assetSearchQuery = signal<string>('');
  readonly selectedReadinessFilter = signal<string>('ALL');
  readonly selectedEwCategoryFilter = signal<string>('ALL');
  readonly inspectedAsset = signal<MewAsset | null>(null);

  // Supply & Maintenance Sub-tab & Filters
  readonly supplySubTab = signal<'inventory' | 'requisitions' | 'workorders'>('inventory');
  readonly inventorySearchQuery = signal<string>('');
  readonly showOnlyLowStock = signal<boolean>(false);

  // Installations & Personnel Filters
  readonly selectedInstallationCode = signal<string>('ALL');
  readonly selectedPersonnelCategory = signal<string>('ALL');
  readonly selectedClearanceFilter = signal<string>('ALL');
  readonly personnelSearchQuery = signal<string>('');

  // Audit Ledger Filters
  readonly ledgerTypeFilter = signal<string>('ALL');
  readonly ledgerSearchQuery = signal<string>('');

  // Interactive Scanner / Receive Shipment Modal State
  readonly scannerModalOpen = signal<boolean>(false);
  readonly scannerDocNumber = signal<string>('');
  readonly scannerReceivedQty = signal<number | null>(null);
  readonly scannerFeedback = signal<{
    type: 'success' | 'error';
    text: string;
    subtext?: string;
  } | null>(null);

  // Computed Filtered Collections
  readonly ewCategories = computed(() => {
    const set = new Set<string>();
    this.api.assets().forEach((a) => {
      if (a.ew_category) set.add(a.ew_category);
    });
    return Array.from(set).sort();
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

  readonly filteredUnits = computed(() => {
    const domain = this.selectedDomain();
    return this.api.units().filter((u) => {
      if (domain === 'ALL') return true;
      return (u.command_type || '').toLowerCase().includes(domain.toLowerCase());
    });
  });

  readonly downAssetsList = computed(() => {
    const domain = this.selectedDomain();
    return this.api
      .assets()
      .filter(
        (a) =>
          a.readiness_status !== 'FMC' &&
          (domain === 'ALL' || a.domain === domain)
      );
  });

  // 30-Day Readiness Trend Series for the SVG Chart
  readonly activeTrendSeries = computed(() => {
    const units = this.filteredUnits();
    const unitId = this.selectedUnitId();
    const targetUnit =
      unitId !== 'ALL'
        ? units.find((u) => u.id === unitId) || units[0]
        : units[0];

    if (!targetUnit || !targetUnit.readiness_trends?.length) {
      return [];
    }
    return [...targetUnit.readiness_trends].sort((a, b) =>
      a.snapshot_date.localeCompare(b.snapshot_date)
    );
  });

  readonly trendSvgPolyline = computed(() => {
    const points = this.activeTrendSeries();
    if (!points.length) return '';
    const width = 680;
    const height = 160;
    const stepX = points.length > 1 ? width / (points.length - 1) : width;

    return points
      .map((pt, idx) => {
        const x = Math.round(idx * stepX);
        const rate = Math.max(0, Math.min(100, Number(pt.operational_readiness_rate || 0)));
        const y = Math.round(height - (rate / 100) * (height - 24) - 12);
        return `${x},${y}`;
      })
      .join(' ');
  });

  readonly trendSvgArea = computed(() => {
    const line = this.trendSvgPolyline();
    if (!line) return '';
    return `0,160 ${line} 680,160`;
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

  readonly filteredPersonnel = computed(() => {
    const instCode = this.selectedInstallationCode();
    const cat = this.selectedPersonnelCategory();
    const clr = this.selectedClearanceFilter();
    const q = this.personnelSearchQuery().trim().toLowerCase();

    return this.api.personnel().filter((p) => {
      if (instCode !== 'ALL' && p.installation_code !== instCode) return false;
      if (cat !== 'ALL' && p.personnel_category !== cat) return false;
      if (clr !== 'ALL' && p.security_clearance !== clr) return false;
      if (q) {
        const hay = `${p.personnel_id} ${p.dod_id} ${p.rank} ${p.first_name} ${p.last_name} ${p.specialty_code} ${p.title} ${p.installation_code}`.toLowerCase();
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

  inspectAsset(asset: MewAsset): void {
    this.inspectedAsset.set(asset);
  }

  closeAssetInspector(): void {
    this.inspectedAsset.set(null);
  }

  selectInstallationForPersonnel(code: string | null): void {
    if (!code) return;
    if (this.selectedInstallationCode() === code) {
      this.selectedInstallationCode.set('ALL');
    } else {
      this.selectedInstallationCode.set(code);
    }
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
