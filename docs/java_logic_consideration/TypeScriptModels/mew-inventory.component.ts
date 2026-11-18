// PrimeNG Inventory & Classification Banner Component

import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { DialogModule } from 'primeng/dialog';
import { CheckboxModule } from 'primeng/checkbox';
import { ToastModule } from 'primeng/toast';
import { ToolbarModule } from 'primeng/toolbar';
import { MessageService } from 'primeng/api';

import { MewLogisticsService } from '../../services/mew-logistics.service';
import { EdipiAuthService } from '../../services/edipi-auth.service';
import {
  EwLogisticsAsset,
  PersonnelRbac,
} from '../../models/mew-logistics.model';

type TagSeverity = 'success' | 'info' | 'warning' | 'danger' | 'secondary' | 'contrast' | undefined;

@Component({
  selector: 'app-mew-inventory',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    TagModule,
    ButtonModule,
    DropdownModule,
    InputTextModule,
    DialogModule,
    CheckboxModule,
    ToastModule,
    ToolbarModule,
  ],
  providers: [MessageService],
  templateUrl: './mew-inventory.component.html',
  styleUrls: ['./mew-inventory.component.scss'],
})
export class MewInventoryComponent implements OnInit {
  private readonly mewService = inject(MewLogisticsService);
  private readonly authService = inject(EdipiAuthService);
  private readonly messageService = inject(MessageService);

  // State Signals
  currentUser = signal<PersonnelRbac | null>(null);
  assets = signal<EwLogisticsAsset[]>([]);
  loading = signal<boolean>(false);

  // Filter Signals
  selectedBranch = signal<string | null>(null);
  selectedInstallation = signal<string | null>(null);
  selectedReadiness = signal<string | null>(null);
  globalSearch = signal<string>('');
  edipiInput = signal<string>(this.authService.activeEdipi());

  // Edit Dialog State
  editDialogVisible = signal<boolean>(false);
  selectedAsset = signal<EwLogisticsAsset | null>(null);
  editStatus = signal<'FMC' | 'PMC-M' | 'PMC-S' | 'NMCM'>('FMC');
  editEro = signal<string>('NONE');
  editCryptoLoaded = signal<boolean>(false);

  // Highest classification present in visible dataset for top security banner
  highestVisibleClassification = computed<string>(() => {
    const list = this.assets();
    if (list.some((a) => a.classificationMarking === 'TS//SCI')) {
      return 'TOP SECRET // SCI';
    }
    if (list.some((a) => a.classificationMarking === 'SECRET//NOFORN')) {
      return 'SECRET // NOFORN';
    }
    if (list.some((a) => a.classificationMarking === 'SECRET')) {
      return 'SECRET';
    }
    return 'CONFIDENTIAL // CUI';
  });

  // Dropdown Options
  branchOptions = [
    { label: 'All Branches', value: null },
    { label: 'USMC (Marine Corps)', value: 'USMC' },
    { label: 'USA (Army)', value: 'USA' },
    { label: 'USAF (Air Force)', value: 'USAF' },
    { label: 'USN (Navy)', value: 'USN' },
  ];

  installationOptions = [
    { label: 'All Installations', value: null },
    { label: 'MCB Camp Pendleton (CA)', value: 'MCB-CAMP-PENDLETON' },
    { label: 'MCB Quantico (VA)', value: 'MCB-QUANTICO' },
    { label: 'Camp Lejeune (NC)', value: 'MCB-CAMP-LEJEUNE' },
    { label: 'Aberdeen Proving Ground (MD)', value: 'APG-MD' },
    { label: 'Redstone Arsenal (AL)', value: 'REDSTONE-ARSENAL' },
    { label: 'Tobyhanna Army Depot (PA)', value: 'TOBYHANNA-DEPOT' },
    { label: 'Goodfellow AFB (TX)', value: 'GOODFELLOW-AFB' },
    { label: 'Tinker AFB (OK)', value: 'TINKER-AFB' },
    { label: 'Nellis AFB (NV)', value: 'NELLIS-AFB' },
    { label: 'Robins AFB (GA)', value: 'ROBINS-AFB' },
    { label: 'NSA Crane (IN)', value: 'NSA-CRANE' },
    { label: 'Naval Base San Diego (CA)', value: 'NB-SAN-DIEGO' },
    { label: 'Naval Station Norfolk (VA)', value: 'NAVSTA-NORFOLK' },
    { label: 'NAS Whidbey Island (WA)', value: 'NAS-WHIDBEY-ISLAND' },
    { label: 'NAS Patuxent River (MD)', value: 'NAS-PAX-RIVER' },
    { label: 'NAS Fallon (NV)', value: 'NAS-FALLON' },
  ];

  readinessOptions = [
    { label: 'All Readiness', value: null },
    { label: 'FMC (Fully Mission Capable)', value: 'FMC' },
    { label: 'PMC-M (Partially MC - Maint)', value: 'PMC-M' },
    { label: 'PMC-S (Partially MC - Supply)', value: 'PMC-S' },
    { label: 'NMCM (Not MC - Maint)', value: 'NMCM' },
  ];

  editReadinessOptions = [
    { label: 'FMC', value: 'FMC' },
    { label: 'PMC-M', value: 'PMC-M' },
    { label: 'PMC-S', value: 'PMC-S' },
    { label: 'NMCM', value: 'NMCM' },
  ];

  ngOnInit(): void {
    this.loadSessionAndAssets();
  }

  switchEdipi(): void {
    this.authService.setEdipi(this.edipiInput());
    this.loadSessionAndAssets();
  }

  loadSessionAndAssets(): void {
    this.loading.set(true);
    this.mewService.getCurrentUserProfile().subscribe({
      next: (profile) => {
        this.currentUser.set(profile);
        this.fetchAssets();
      },
      error: (err) => {
        this.loading.set(false);
        this.currentUser.set(null);
        this.assets.set([]);
        this.messageService.add({
          severity: 'error',
          summary: 'RBAC Authentication Failed',
          detail: err.error?.message || 'Invalid or inactive EDIPI.',
        });
      },
    });
  }

  fetchAssets(): void {
    this.loading.set(true);
    this.mewService
      .getAuthorizedAssets({
        installationCode: this.selectedInstallation() ?? undefined,
        readinessStatus: this.selectedReadiness() ?? undefined,
      })
      .subscribe({
        next: (data) => {
          const branch = this.selectedBranch();
          const filtered = branch
            ? data.filter((item) => item.serviceBranch === branch)
            : data;
          this.assets.set(filtered);
          this.loading.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.messageService.add({
            severity: 'error',
            summary: 'Access Error',
            detail: err.error?.message || 'Unable to retrieve EW assets.',
          });
        },
      });
  }

  openEditDialog(asset: EwLogisticsAsset): void {
    this.selectedAsset.set(asset);
    this.editStatus.set(asset.readinessStatus);
    this.editEro.set(
      asset.gcssEroWorkOrder === 'NONE' ? 'ERO-90124' : asset.gcssEroWorkOrder
    );
    this.editCryptoLoaded.set(asset.cryptoKeyLoaded);
    this.editDialogVisible.set(true);
  }

  saveReadinessUpdate(): void {
    const asset = this.selectedAsset();
    if (!asset) return;

    const status = this.editStatus();
    const ero = status === 'FMC' ? 'NONE' : this.editEro();

    this.mewService
      .updateAssetReadiness(asset.assetId, {
        readinessStatus: status,
        gcssEroWorkOrder: ero,
        cryptoKeyLoaded: this.editCryptoLoaded(),
      })
      .subscribe({
        next: (updated) => {
          this.assets.update((list) =>
            list.map((item) => (item.assetId === updated.assetId ? updated : item))
          );
          this.editDialogVisible.set(false);
          this.messageService.add({
            severity: 'success',
            summary: 'Readiness Updated',
            detail: `${updated.assetId} updated to ${updated.readinessStatus}.`,
          });
        },
        error: (err) => {
          this.messageService.add({
            severity: 'error',
            summary: 'RBAC Write Denied (403)',
            detail:
              err.error?.message ||
              'You do not have write privileges for this UIC.',
          });
        },
      });
  }

  getReadinessSeverity(status: string): TagSeverity {
    switch (status) {
      case 'FMC':
        return 'success';
      case 'PMC-M':
      case 'PMC-S':
        return 'warning';
      case 'NMCM':
        return 'danger';
      default:
        return 'info';
    }
  }

  getBannerClass(marking: string): string {
    if (marking.includes('TS')) return 'banner-ts-sci';
    if (marking.includes('SECRET')) return 'banner-secret';
    return 'banner-confidential';
  }
}
