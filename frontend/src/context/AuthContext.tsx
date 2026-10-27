'use client';

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import * as authService from '@/services/auth';
import { refreshAccessToken, setAccessToken, AUTH_UNAUTHORIZED_EVENT } from '@/services/api';
import type { AuthUser } from '@/types/auth';

interface AuthContextValue {
  user: AuthUser | null;
  loading: boolean;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
  updateUser: (updated: AuthUser) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  const updateUser = useCallback((updated: AuthUser) => {
    setUser(current => current?.userId === updated.userId ? updated : current);
  }, []);

  const refreshUser = useCallback(async () => {
    try {
      const res = await authService.getCurrentUser();
      setUser(res.success ? res.data : null);
    } catch {
      // 401 or backend unavailable — treat as unauthenticated
      setUser(null);
    }
  }, []);

  // Listen to unauthorized event dispatched on failed refresh
  useEffect(() => {
    function handleUnauthorized() {
      setUser(null);
      setAccessToken(null);
    }
    window.addEventListener(AUTH_UNAUTHORIZED_EVENT, handleUnauthorized);
    return () => window.removeEventListener(AUTH_UNAUTHORIZED_EVENT, handleUnauthorized);
  }, []);

  // On mount: restore session from cookie by fetching a new access token
  useEffect(() => {
    let cancelled = false;
    async function init() {
      try {
        // 1. Attempt to refresh the access token using the shared single-flight mechanism
        const refreshed = await refreshAccessToken();

        if (refreshed) {
          // 2. Fetch user profile
          const meRes = await authService.getCurrentUser();
          if (!cancelled) setUser(meRes.success ? meRes.data : null);
        } else {
          if (!cancelled) setUser(null);
        }
      } catch {
        if (!cancelled) setUser(null);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    init();
    return () => { cancelled = true; };
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const res = await authService.login({ email, password });
    if (!res.success) {
      throw new Error(res.message || 'Login failed');
    }
    // Fetch the full user profile so context is populated
    const meRes = await authService.getCurrentUser();
    if (!meRes.success) {
      throw new Error(meRes.message || 'Failed to load user profile');
    }
    setUser(meRes.data);
  }, []);

  const logout = useCallback(async () => {
    try {
      await authService.logout();
    } catch {
      // Even if the backend call fails, clear local state
    } finally {
      setUser(null);
    }
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ user, loading, isAuthenticated: user !== null, login, logout, refreshUser, updateUser }),
    [user, loading, login, logout, refreshUser, updateUser]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used inside <AuthProvider>');
  }
  return ctx;
}
