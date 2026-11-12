import Keycloak from 'keycloak-js';

export const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM ?? 'apparel',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'storefront',
});

let initPromise: Promise<boolean> | null = null;

/**
 * check-sso: shoppers browse anonymously; if they already have a Keycloak session
 * they are silently signed in. Init runs once even under React StrictMode.
 */
export function initAuth(): Promise<boolean> {
  if (!initPromise) {
    initPromise = keycloak
      .init({
        onLoad: 'check-sso',
        pkceMethod: 'S256',
        silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
      })
      .catch((e) => {
        console.warn('Keycloak unavailable - continuing as guest', e);
        return false;
      });
  }
  return initPromise;
}

/** Fresh access token, or undefined for guests. */
export async function getToken(): Promise<string | undefined> {
  if (!keycloak.authenticated) return undefined;
  try {
    await keycloak.updateToken(30);
  } catch {
    return undefined;
  }
  return keycloak.token;
}
