"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { api, ApiError, type CurrentUser } from "@/lib/client-api";

type AuthContextValue = {
  user: CurrentUser | null;
  loading: boolean;
  authError: string | null;
  refresh: () => Promise<CurrentUser | null>;
  clear: () => void;
  reportError: (message: string | null) => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [authError, setAuthError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), 12000);
    try {
      const current = await api<CurrentUser>("/api/auth/me", { signal: controller.signal });
      setUser(current);
      setAuthError(null);
      return current;
    } catch (error) {
      setUser(null);
      setAuthError(error instanceof ApiError && error.status === 401
        ? null : "無法確認登入狀態，請檢查後端服務連線。");
      return null;
    } finally {
      window.clearTimeout(timeout);
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const clear = useCallback(() => {
    setUser(null);
    setAuthError(null);
  }, []);

  const value = useMemo(() => ({
    user,
    loading,
    authError,
    refresh,
    clear,
    reportError: setAuthError,
  }), [user, loading, authError, refresh, clear]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within AuthProvider");
  return context;
}
