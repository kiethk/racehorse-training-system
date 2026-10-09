'use client';

import { useEffect, type ReactNode } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import { LoadingScreen } from '@/components/ui/Spinner';
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
 *
 * Inside the (app) route group AuthGate has already handled the first two cases,
 * so the spinner here only fills the content area and the shell stays mounted.
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

  if (loading || !isAuthenticated || !user || !allowedRoles.includes(user.role)) {
    return <LoadingScreen />;
  }

  return <>{children}</>;
}
