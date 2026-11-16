import { Injectable, signal, effect } from '@angular/core';

export type DodTheme = 'tactical-dark' | 'daylight-tactical';

@Injectable({
  providedIn: 'root',
})
export class ThemeService {
  readonly currentTheme = signal<DodTheme>(this.getInitialTheme());

  constructor() {
    effect(() => {
      const theme = this.currentTheme();
      const root = document.documentElement;
      root.setAttribute('data-theme', theme);
      if (theme === 'tactical-dark') {
        root.classList.add('dark');
        root.classList.remove('light');
      } else {
        root.classList.add('light');
        root.classList.remove('dark');
      }
      try {
        localStorage.setItem('dod-ui-theme', theme);
      } catch {
        // Ignore storage errors
      }
    });
  }

  toggleTheme(): void {
    this.currentTheme.update((t) =>
      t === 'tactical-dark' ? 'daylight-tactical' : 'tactical-dark'
    );
  }

  private getInitialTheme(): DodTheme {
    try {
      const saved = localStorage.getItem('dod-ui-theme');
      if (saved === 'tactical-dark' || saved === 'daylight-tactical') {
        return saved;
      }
    } catch {
      // Default to Low-Light Tactical Dark (C2 / CIC Watch Floor)
    }
    return 'tactical-dark';
  }
}
