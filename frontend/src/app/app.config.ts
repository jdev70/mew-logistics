import { APP_INITIALIZER, ApplicationConfig } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { KeycloakAuthService } from './services/keycloak-auth.service';
import { keycloakAuthInterceptor } from './services/keycloak-auth.interceptor';

function initializeKeycloak(auth: KeycloakAuthService): () => Promise<void> {
  return () => auth.initialize();
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(withInterceptors([keycloakAuthInterceptor])),
    provideAnimations(),
    {
      provide: APP_INITIALIZER,
      useFactory: initializeKeycloak,
      deps: [KeycloakAuthService],
      multi: true,
    },
  ],
};
