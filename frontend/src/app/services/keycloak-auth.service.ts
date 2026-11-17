import { Injectable, signal } from '@angular/core';
import Keycloak from 'keycloak-js';
import { runtimeConfig } from '../config/runtime';

/** Browser OIDC boundary. Tokens remain in the Keycloak adapter's memory only. */
@Injectable({ providedIn: 'root' })
export class KeycloakAuthService {
  private readonly enabled = runtimeConfig.keycloak.enabled;
  private readonly client = new Keycloak({
    url: runtimeConfig.keycloak.url,
    realm: runtimeConfig.keycloak.realm,
    clientId: runtimeConfig.keycloak.clientId,
  });
  private initialization?: Promise<void>;

  readonly initialized = signal(false);
  readonly authenticated = signal(false);

  initialize(): Promise<void> {
    if (!this.initialization) {
      this.initialization = this.initializeClient();
    }
    return this.initialization;
  }

  async accessToken(): Promise<string | undefined> {
    if (!this.enabled || !this.client.authenticated) {
      return undefined;
    }
    await this.client.updateToken(30);
    return this.client.token;
  }

  signOut(): void {
    if (this.enabled && this.client.authenticated) {
      void this.client.logout({ redirectUri: window.location.origin });
    }
  }

  private async initializeClient(): Promise<void> {
    if (!this.enabled) {
      this.initialized.set(true);
      return;
    }

    const isAuthenticated = await this.client.init({
      onLoad: 'login-required',
      pkceMethod: 'S256',
      checkLoginIframe: false,
    });
    this.authenticated.set(isAuthenticated);
    this.initialized.set(true);
  }
}
