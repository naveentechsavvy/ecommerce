import Keycloak from 'keycloak-js';
import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';

export const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM ?? 'apparel',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'admin-portal',
});

let initPromise: Promise<boolean> | null = null;
const initOnce = () =>
  (initPromise ??= keycloak.init({ onLoad: 'login-required', pkceMethod: 'S256', checkLoginIframe: false }));

export async function getToken(): Promise<string | undefined> {
  if (!keycloak.authenticated) return undefined;
  await keycloak.updateToken(30);
  return keycloak.token;
}

export type Role = 'ADMIN' | 'OPERATIONS';

interface AuthState {
  name?: string;
  roles: Role[];
  isAdmin: boolean;
  logout: () => void;
}

const Ctx = createContext<AuthState | null>(null);

/** The whole portal requires login; users without ADMIN/OPERATIONS see an access-denied screen. */
export function AuthGate({ children }: { children: ReactNode }) {
  const [state, setState] = useState<'loading' | 'ready' | 'error'>('loading');

  useEffect(() => {
    initOnce().then(() => setState('ready')).catch(() => setState('error'));
  }, []);

  if (state === 'loading') return <div className="center">Signing in…</div>;
  if (state === 'error') return <div className="center">Could not reach the identity server. Is Keycloak running on :8180?</div>;

  const realmRoles = (keycloak.tokenParsed?.realm_access?.roles ?? []) as string[];
  const roles = realmRoles.filter((r): r is Role => r === 'ADMIN' || r === 'OPERATIONS');
  const value: AuthState = {
    name: (keycloak.tokenParsed as { name?: string } | undefined)?.name,
    roles,
    isAdmin: roles.includes('ADMIN'),
    logout: () => keycloak.logout({ redirectUri: window.location.origin }),
  };

  if (roles.length === 0) {
    return (
      <div className="center">
        <p>Your account doesn't have back-office access.</p>
        <button className="btn" onClick={value.logout}>Sign out</button>
      </div>
    );
  }
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error('useAuth outside AuthGate');
  return ctx;
}
