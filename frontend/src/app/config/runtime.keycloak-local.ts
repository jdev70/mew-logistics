/** Local-only Keycloak configuration, selected by `npm run start:keycloak`. */
export const runtimeConfig = {
  apiBaseUrl: 'http://localhost:8080/api/v1',
  keycloak: {
    enabled: true,
    url: 'http://localhost:8180',
    realm: 'mew-local',
    clientId: 'mew-dashboard',
  },
} as const;
