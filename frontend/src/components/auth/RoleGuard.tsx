'use client';

import { useEffect, type ReactNode } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import { getRoleRoute } from '@/lib/roleRoute';
import type { Role } from '@/types/auth';

interface RoleGuardProps {
  allowedRoles: Role[];
  children: ReactNode;
}

/**
 * Wraps a page so that:
 * - While auth is loading → shows a spinner
 * - If not authenticated → redirects to /login
 * - If authenticated but wrong role → redirects to the user's correct landing page
 * - If role matches → renders children
 */
export function RoleGuard({ allowedRoles, children }: RoleGuardProps) {
  const { user, loading, isAuthenticated } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (loading) return;

    if (!isAuthenticated || !user) {
      router.replace('/login');
      return;
    }

    if (!allowedRoles.includes(user.role)) {
      router.replace(getRoleRoute(user.role));
    }
  }, [loading, isAuthenticated, user, allowedRoles, router]);

  if (loading) {
    return <AuthLoadingScreen />;
  }

  if (!isAuthenticated || !user) {
    return <AuthLoadingScreen />;
  }

  if (!allowedRoles.includes(user.role)) {
    return <AuthLoadingScreen />;
  }

  return <>{children}</>;
}

function AuthLoadingScreen() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-[var(--color-background)]">
      <div className="flex flex-col items-center gap-3">
        <span
          className="h-7 w-7 animate-spin rounded-full border-[3px] border-[var(--color-border-strong)] border-t-[var(--color-primary)]"
          aria-label="Loading…"
        />
        <p className="text-[12px] text-[var(--color-text-muted)]">Loading…</p>
      </div>
    </div>
  );
}
