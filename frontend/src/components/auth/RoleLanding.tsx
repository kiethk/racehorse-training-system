'use client';

import { useAuth } from '@/context/AuthContext';
import { ROLE_LABELS } from '@/lib/roleRoute';
import type { Role } from '@/types/auth';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { Panel } from '@/components/ui/Panel';

interface RoleLandingProps {
  role: Role;
}

/**
 * Temporary role landing page shell.
 * Replace section content later with real dashboards.
 */
export function RoleLanding({ role }: RoleLandingProps) {
  const { user } = useAuth();

  return (
    <AppShell>
      <PageContainer>
        <div className="space-y-6">
          {/* Page header */}
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-[var(--color-text-primary)]">
              Welcome, {user?.fullName}
            </h1>
            <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
              You are logged in as a {ROLE_LABELS[role]}.
            </p>
          </div>

          <Panel padded className="space-y-4">
            <h2 className="text-lg font-semibold text-[var(--color-text-primary)]">
              Dashboard Overview
            </h2>
            <div className="rounded-[var(--radius-sm)] border border-dashed border-[var(--color-border-strong)] bg-[var(--color-surface-subtle)] p-8 text-center">
              <p className="text-sm text-[var(--color-text-muted)]">
                Dashboard content will be implemented in feature issues.
              </p>
            </div>
          </Panel>
        </div>
      </PageContainer>
    </AppShell>
  );
}
