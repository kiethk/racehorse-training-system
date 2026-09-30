'use client';

// Setup verified: FE-BE-DB connection tested successfully on 2026-09-10

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import { getRoleRoute } from '@/lib/roleRoute';

export default function Home() {
  const { user, loading, isAuthenticated } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (loading) return;

    if (!isAuthenticated || !user) {
      router.replace('/login');
    } else {
      router.replace(getRoleRoute(user.role));
    }
  }, [loading, isAuthenticated, user, router]);

  // Show a lightweight spinner while the redirect is resolving
  return (
    <div className="flex min-h-screen items-center justify-center bg-[var(--color-background)]">
      <span
        className="h-7 w-7 animate-spin rounded-full border-[3px] border-[var(--color-border-strong)] border-t-[var(--color-primary)]"
        aria-label="Loading…"
      />
    </div>
  );
}