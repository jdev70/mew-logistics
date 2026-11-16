# DoD MEW Logistics and Readiness Dashboard - Design Considerations

### To support PrimeNG across both real-time telemetry/geospatial and logistical inventory/requisition views with live theme switching, use PrimeNG v18+ (Design Token architecture) paired with CSS custom properties. This setup lets operators toggle between a Low-Light Tactical Dark mode (for C2 watch floors) and a Daylight High-Contrast mode (for bright deckplates or field tablets) without reloading the application.

1. Dual-Mode Theme Architecture (styles.scss)
    - Define semantic tokens mapped to a data-theme attribute on the <html> or <body> tag. PrimeNG components will automatically inherit these variables when overridden in your global styleshees.
2. scss
    - **Default:** Low-Light Tactical Dark (C2 / CIC Watch Floor) */
:root,
[data-theme='tactical-dark'] {
  --dod-bg-canvas: #0b0f19;
  --dod-bg-surface: #131b2e;
  --dod-bg-elevated: #1c2742;
  --dod-bg-hover: #233052;
  --dod-border-color: #283655;

  --dod-text-primary: #f1f5f9;
  --dod-text-secondary: #94a3b8;
  --dod-accent-primary: #38bdf8;
  --dod-accent-contrast: #0b0f19;

  --dod-status-ok: #10b981;
  --dod-status-warn: #f59e0b;
  --dod-status-crit: #ef4444;

  /* Map & Telemetry Overlays */
  --dod-glass-bg: rgba(19, 27, 46, 0.85);
}

/* Variant: Daylight High-Contrast (Field Tablets / Bright Environments) */
[data-theme='daylight-tactical'] {
  --dod-bg-canvas: #e2e8f0;
  --dod-bg-surface: #ffffff;
  --dod-bg-elevated: #f8fafc;
  --dod-bg-hover: #f1f5f9;
  --dod-border-color: #94a3b8;

  --dod-text-primary: #0f172a;
  --dod-text-secondary: #475569;
  --dod-accent-primary: #0284c7;
  --dod-accent-contrast: #ffffff;

  --dod-status-ok: #047857;
  --dod-status-warn: #b45309;
  --dod-status-crit: #b91c1c;

  /* Map & Telemetry Overlays */
  --dod-glass-bg: rgba(255, 255, 255, 0.92);
}

/* Global PrimeNG Overrides for High-Density DoD Formatting */
body {
  background-color: var(--dod-bg-canvas);
  color: var(--dod-text-primary);
  font-family: 'Inter', sans-serif;
  margin: 0;
  overflow: hidden;
}

.p-component {
  font-family: 'Inter', sans-serif;
  font-size: 0.875rem; /* 14px base for dense operational UI */
}

/* Compact PrimeNG Table for NSNs, LRUs & Requisitions */
.dod-grid .p-datatable-thead > tr > th {
  background-color: var(--dod-bg-elevated);
  color: var(--dod-text-secondary);
  border-color: var(--dod-border-color);
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  padding: 0.5rem 0.75rem;
  text-transform: uppercase;
}

.dod-grid .p-datatable-tbody > tr {
  background-color: var(--dod-bg-surface);
  color: var(--dod-text-primary);
}

.dod-grid .p-datatable-tbody > tr:hover {
  background-color: var(--dod-bg-hover);
}

.dod-grid .p-datatable-tbody > tr > td {
  border-color: var(--dod-border-color);
  padding: 0.35rem 0.75rem; /* Strict ~34px row height */
}

.mono-data {
  font-family: 'JetBrains Mono', monospace;
  font-variant-numeric: tabular-nums;
}
Use code with caution.

2. Angular Theme Service (theme.service.ts)

Use Angular Signals to manage theme state cleanly across your PrimeNG components and geospatial map layers (e.g., switching MapLibre/Leaflet vector tile styles between dark and light rasters simultaneously).
typescript
import { Injectable, signal, effect } from '@angular/core';

export type DodTheme = 'tactical-dark' | 'daylight-tactical';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly currentTheme = signal<DodTheme>('tactical-dark');

  constructor() {
    effect(() => {
      const theme = this.currentTheme();
      document.documentElement.setAttribute('data-theme', theme);
      localStorage.setItem('dod-ui-theme', theme);
    });
  }

  toggleTheme(): void {
    this.currentTheme.update(t =>
      t === 'tactical-dark' ? 'daylight-tactical' : 'tactical-dark'
    );
  }
}
Use code with caution.

3. UI Formatting by Operational Workspace


A. Real-Time Telemetry & Geospatial View

• Full-Bleed Map Canvas: Render your geospatial view (CesiumJS, MapLibre, or Leaflet) at 100% height and width inside the central CSS Grid pane.
• Floating HUD Overlays: Place PrimeNG p-card or p-panel widgets over the map corners using --dod-glass-bg with backdrop-filter: blur(8px) and a crisp 1px solid var(--dod-border-color) border.
• Telemetry Readouts: Group live RF/EW metrics (frequency, bearing, signal strength, GPS coordinates) inside p-splitter drawers using .mono-data so rapidly changing numbers never cause layout shifts.
• Status Badges: Use PrimeNG p-tag with rounded="false" (sharp 2px corners fit tactical UIs better than pill shapes) and pair color with status icons (pi-check-circle, pi-exclamation-triangle, pi-times-circle).

B. Logistical Inventory & Requisition View

• Filter & Action Toolbar: Use PrimeNG p-toolbar at the top of the workspace housing a p-iconfield quick search (by NSN, CAGE code, or Hull Number), p-multiselect filters for readiness status (FMC, PMC, NMC), and a primary p-button for creating requisitions.
• Virtual-Scrolled Data Grid: Configure PrimeNG p-table with [virtualScroll]="true", [virtualScrollItemSize]="34", and styleClass="dod-grid p-datatable-sm p-datatable-gridlines". This handles thousands of Line Replaceable Units (LRUs) and requisition line items at 60fps.
• Slide-Over Requisition Drawer: Clicking a row opens a PrimeNG p-drawer (formerly p-sidebar) on the right side (position="right", styleClass="w-30rem"). Use p-stepper inside the drawer to track requisition workflows (Draft -> Supply Review -> Approved -> In Transit).

4. Recommended App Shell Structure (app.component.html)

html
<div class="app-shell">
  <!-- Top Classification Banner -->
  <header class="class-banner unclass">UNCLASSIFIED // FOR OFFICIAL USE ONLY</header>

  <!-- Main Workspace -->
  <div class="workspace-grid">
    <!-- Left Navigation Rail -->
    <nav class="nav-rail">
      <button pButton icon="pi pi-compass" pTooltip="Telemetry & Map" class="p-button-text"></button>
      <button pButton icon="pi pi-box" pTooltip="LRU Inventory" class="p-button-text"></button>
      <button pButton icon="pi pi-file-edit" pTooltip="Requisitions" class="p-button-text"></button>
      <div class="spacer"></div>
      <button
        pButton
        [icon]="themeService.currentTheme() === 'tactical-dark' ? 'pi pi-sun' : 'pi pi-moon'"
        (click)="themeService.toggleTheme()"
        pTooltip="Toggle Daylight / Dark Mode"
        class="p-button-text">
      </button>
    </nav>

    <!-- Dynamic Router Content -->
    <main class="viewport-content">
      <router-outlet></router-outlet>
    </main>
  </div>

  <!-- Bottom Classification Banner -->
  <footer class="class-banner unclass">UNCLASSIFIED // FOR OFFICIAL USE ONLY</footer>
</div>