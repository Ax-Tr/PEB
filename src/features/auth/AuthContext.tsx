import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { auth } from "../../shared/api";
import { unregisterPush } from "../../shared/push";

interface AuthState {
  ready: boolean; // finished the initial token check
  authed: boolean;
  hasTenant: boolean; // whether the authed user has an onboarded business
  refreshAuthed: () => Promise<void>;
  refreshTenant: () => Promise<void>; // call after business creation to re-gate
  logout: () => Promise<void>;
}

const AuthCtx = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }): React.ReactElement {
  const [ready, setReady] = useState(false);
  const [authed, setAuthed] = useState(false);
  const [hasTenant, setHasTenant] = useState(false);

  const refreshAuthed = useCallback(async () => {
    const isAuth = await auth.isAuthenticated();
    setAuthed(isAuth);
    if (isAuth) {
      setHasTenant(await auth.hasTenant());
    } else {
      setHasTenant(false);
    }
  }, []);

  const refreshTenant = useCallback(async () => {
    setHasTenant(await auth.hasTenant());
  }, []);

  const logout = useCallback(async () => {
    await unregisterPush(); // best-effort, while the session is still valid
    await auth.logout();
    setAuthed(false);
    setHasTenant(false);
  }, []);

  useEffect(() => {
    void (async () => {
      await refreshAuthed();
      setReady(true);
    })();
  }, [refreshAuthed]);

  const value = useMemo<AuthState>(
    () => ({ ready, authed, hasTenant, refreshAuthed, refreshTenant, logout }),
    [ready, authed, hasTenant, refreshAuthed, refreshTenant, logout],
  );
  return <AuthCtx.Provider value={value}>{children}</AuthCtx.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthCtx);
  if (!ctx) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return ctx;
}
