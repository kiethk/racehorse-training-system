'use client';

import { useEffect, type ReactNode } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import { LoadingScreen } from '@/components/ui/Spinner';

/**
 * Gate for the signed-in area: waits for the session to restore and sends
 * unauthenticated visitors to /login. Role checks stay in RoleGuard, per page.
 */
export function AuthGate({ children }: { children: ReactNode }) {
  const { user, loading, isAuthenticated } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!loading && (!isAuthenticated || !user)) router.replace('/login');
  }, [loading, isAuthenticated, user, router]);

  if (loading || !isAuthenticated || !user) return <LoadingScreen fullScreen />;

  return <>{children}</>;
}
