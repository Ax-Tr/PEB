import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { auth } from "../../shared/api";
import { unregisterPush } from "../../shared/push";

interface AuthState {
  ready: boolean; // finished the initial token check
  authed: boolean;
  refreshAuthed: () => Promise<void>;
  logout: () => Promise<void>;
}

const AuthCtx = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }): React.ReactElement {
  const [ready, setReady] = useState(false);
  const [authed, setAuthed] = useState(false);

  const refreshAuthed = useCallback(async () => {
    setAuthed(await auth.isAuthenticated());
  }, []);

  const logout = useCallback(async () => {
    await unregisterPush(); // best-effort, while the session is still valid
    await auth.logout();
    setAuthed(false);
  }, []);

  useEffect(() => {
    void (async () => {
      await refreshAuthed();
      setReady(true);
    })();
  }, [refreshAuthed]);

  const value = useMemo<AuthState>(
    () => ({ ready, authed, refreshAuthed, logout }),
    [ready, authed, refreshAuthed, logout],
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
