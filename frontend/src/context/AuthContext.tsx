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
import type { AuthUser } from '@/types/auth';

interface AuthContextValue {
  user: AuthUser | null;
  loading: boolean;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  const refreshUser = useCallback(async () => {
    try {
      const res = await authService.getCurrentUser();
      setUser(res.success ? res.data : null);
    } catch {
      // 401 or backend unavailable — treat as unauthenticated
      setUser(null);
    }
  }, []);

  // On mount: restore session from cookie by fetching a new access token
  useEffect(() => {
    let cancelled = false;
    async function init() {
      try {
        const { setAccessToken } = await import('@/services/api');
        const API_URL = process.env.NEXT_PUBLIC_API_URL || '';
        
        // 1. Attempt to refresh the access token using HttpOnly cookie
        const refreshRes = await fetch(`${API_URL}/api/auth/refresh`, {
          method: 'POST',
          credentials: 'include',
          headers: { 'Content-Type': 'application/json' },
        });

        if (refreshRes.ok) {
          const data = await refreshRes.json();
          if (data?.data?.accessToken) {
            setAccessToken(data.data.accessToken);
            // 2. Fetch user profile
            const meRes = await authService.getCurrentUser();
            if (!cancelled) setUser(meRes.success ? meRes.data : null);
          } else {
            if (!cancelled) setUser(null);
          }
        } else {
          setAccessToken(null);
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
    () => ({ user, loading, isAuthenticated: user !== null, login, logout, refreshUser }),
    [user, loading, login, logout, refreshUser]
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
