// Angular 17 Functional HTTP Interceptor & Session Store

import { Injectable, signal } from '@angular/core';

@Injectable({
    providedIn: 'root',
})
export class EdipiAuthService {
    // Default test EDIPI (can be switched dynamically via role switcher in UI)
    private readonly activeEdipiSignal = signal<string>('1734918204');

    readonly activeEdipi = this.activeEdipiSignal.asReadonly();

    setEdipi(edipi: string): void {
        this.activeEdipiSignal.set(edipi.trim());
    }
}
