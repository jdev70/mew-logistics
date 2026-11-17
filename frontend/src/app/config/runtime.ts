/** Default development configuration. Use `npm run start:keycloak` for local OIDC sign-in. */
export const runtimeConfig = {
  apiBaseUrl: 'http://localhost:8080/api/v1',
  keycloak: {
    enabled: false,
    url: 'http://localhost:8180',
    realm: 'mew-local',
    clientId: 'mew-dashboard',
  },
} as const;
