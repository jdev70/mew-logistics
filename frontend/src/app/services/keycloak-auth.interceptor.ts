import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap } from 'rxjs';
import { runtimeConfig } from '../config/runtime';
import { KeycloakAuthService } from './keycloak-auth.service';

/** Attaches a refreshed bearer token only to requests for the MEW API. */
export const keycloakAuthInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith(runtimeConfig.apiBaseUrl)) {
    return next(request);
  }

  const auth = inject(KeycloakAuthService);
  return from(auth.accessToken()).pipe(
    switchMap((token) => next(token
      ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : request)),
  );
};
