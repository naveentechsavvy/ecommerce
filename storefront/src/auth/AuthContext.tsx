import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { initAuth, keycloak } from './keycloak';

interface AuthState {
  ready: boolean;
  authenticated: boolean;
  name?: string;
  email?: string;
  login: () => void;
  register: () => void;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);

  useEffect(() => {
    initAuth().then((auth) => {
      setAuthenticated(auth);
      setReady(true);
    });
    keycloak.onAuthLogout = () => setAuthenticated(false);
  }, []);

  const parsed = keycloak.tokenParsed as { name?: string; email?: string } | undefined;
  const value: AuthState = {
    ready,
    authenticated,
    name: parsed?.name,
    email: parsed?.email,
    login: () => keycloak.login({ redirectUri: window.location.href }),
    register: () => keycloak.register({ redirectUri: window.location.href }),
    logout: () => keycloak.logout({ redirectUri: window.location.origin }),
  };
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth outside AuthProvider');
  return ctx;
}
