'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { Panel } from '@/components/ui/Panel';
import { Button } from '@/components/ui/Button';
import { Pill } from '@/components/ui/StatusBadge';
import { Icon } from '@/components/ui/Icon';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { admissionsApi } from '@/features/admissions/services/api';
import type { UrgentAssignmentAlert } from '@/features/admissions/types';

export default function UrgentCasesPage() {
  const [urgentCases, setUrgentCases] = useState<UrgentAssignmentAlert[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<'ALL' | 'PENDING' | 'IN_PROGRESS'>('ALL');

  useEffect(() => {
    let active = true;
    const load = async () => {
      try {
        setLoading(true);
        setError(null);
        const pending = await admissionsApi.getPendingUrgentAlerts();
        if (active) setUrgentCases(pending || []);
      } catch (err) {
        if (active) setError(err instanceof Error ? err.message : 'Unable to load urgent cases list');
      } finally {
        if (active) setLoading(false);
      }
    };

    load();
    const interval = setInterval(load, 15000);
    return () => {
      active = false;
      clearInterval(interval);
    };
  }, []);

  const handleRefresh = async () => {
    try {
      setLoading(true);
      setError(null);
      const pending = await admissionsApi.getPendingUrgentAlerts();
      setUrgentCases(pending || []);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to load urgent cases list');
    } finally {
      setLoading(false);
    }
  };

  const filteredCases = urgentCases.filter((c) => {
    if (filter === 'PENDING') return c.status === 'SCHEDULED';
    if (filter === 'IN_PROGRESS') return c.status === 'IN_PROGRESS';
    return true;
  });

  return (
    <RoleGuard allowedRoles={['VETERINARIAN']}>
      <PageContainer>
          <div className="space-y-6">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
              <div>
                <div className="flex items-center gap-3">
                  <h1 className="text-2xl font-bold tracking-tight text-[var(--color-text-primary)]">
                    Urgent Cases
                  </h1>
                  {urgentCases.length > 0 && (
                    <Pill tone="danger">{urgentCases.length} pending</Pill>
                  )}
                </div>
                <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
                  High-priority clinical incidents reported by grooms and assigned emergency cases.
                </p>
              </div>

              <div className="flex items-center gap-2">
                <Button variant="secondary" icon="refresh" onClick={handleRefresh} loading={loading}>
                  Refresh
                </Button>
              </div>
            </div>

            {/* Filter Tabs */}
            <div className="flex items-center gap-2 border-b border-[var(--color-border)] pb-2">
              <button
                type="button"
                onClick={() => setFilter('ALL')}
                className={`px-3 py-1.5 text-xs font-medium rounded-[var(--radius-sm)] transition-colors ${
                  filter === 'ALL'
                    ? 'bg-[var(--color-primary)] text-white'
                    : 'text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]'
                }`}
              >
                All ({urgentCases.length})
              </button>
              <button
                type="button"
                onClick={() => setFilter('PENDING')}
                className={`px-3 py-1.5 text-xs font-medium rounded-[var(--radius-sm)] transition-colors ${
                  filter === 'PENDING'
                    ? 'bg-[var(--color-danger)] text-white'
                    : 'text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]'
                }`}
              >
                Pending Intake ({urgentCases.filter((c) => c.status === 'SCHEDULED').length})
              </button>
              <button
                type="button"
                onClick={() => setFilter('IN_PROGRESS')}
                className={`px-3 py-1.5 text-xs font-medium rounded-[var(--radius-sm)] transition-colors ${
                  filter === 'IN_PROGRESS'
                    ? 'bg-amber-600 text-white'
                    : 'text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]'
                }`}
              >
                In Progress ({urgentCases.filter((c) => c.status === 'IN_PROGRESS').length})
              </button>
            </div>

            {/* Error banner */}
            {error && (
              <div className="rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] p-4 text-xs text-[var(--color-danger)] flex items-center justify-between">
                <span>{error}</span>
                <button type="button" onClick={handleRefresh} className="underline font-semibold ml-2">
                  Retry
                </button>
              </div>
            )}

            {/* Content List */}
            {loading && urgentCases.length === 0 ? (
              <Panel padded>
                <ListSkeleton rows={4} />
              </Panel>
            ) : filteredCases.length === 0 ? (
              <Panel padded>
                <EmptyState
                  icon="check"
                  title="No urgent cases requiring attention"
                  description="When staff report high or critical severity clinical incidents, emergency cases will appear here immediately."
                  action={
                    <Link
                      href="/veterinarian/admissions"
                      className="inline-flex items-center text-xs font-medium text-[var(--color-primary)] hover:underline"
                    >
                      Check admission queue &rarr;
                    </Link>
                  }
                />
              </Panel>
            ) : (
              <div className="grid gap-4">
                {filteredCases.map((item) => (
                  <Panel
                    key={item.scheduleId}
                    className={`overflow-hidden border-l-4 transition-all hover:shadow-md ${
                      item.status === 'SCHEDULED'
                        ? 'border-l-[var(--color-danger)] border-t border-r border-b border-[var(--color-border)]'
                        : 'border-l-amber-500 border-t border-r border-b border-[var(--color-border)]'
                    }`}
                  >
                    <div className="p-5 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                      <div className="space-y-2 flex-1">
                        <div className="flex flex-wrap items-center gap-2">
                          <Pill tone="danger">{item.severity}</Pill>
                          <Pill tone={item.status === 'SCHEDULED' ? 'warning' : 'info'}>
                            {item.status === 'SCHEDULED' ? 'Pending Intake' : 'In Progress'}
                          </Pill>
                          <span className="text-xs font-mono text-[var(--color-text-muted)]">
                            #{item.scheduleId}
                          </span>
                        </div>

                        <h2 className="text-base font-bold text-[var(--color-text-primary)]">
                          {item.title}
                        </h2>

                        <p className="text-xs text-[var(--color-text-secondary)] line-clamp-2">
                          {item.description || 'No detailed description provided by reporter.'}
                        </p>

                        <div className="flex flex-wrap items-center gap-y-1 gap-x-4 text-xs text-[var(--color-text-muted)] pt-1">
                          <span className="flex items-center gap-1">
                            <Icon name="horse" size={12} />
                            <strong>Horse:</strong> {item.horseName} (#{item.horseId})
                          </span>
                          {item.stallCode && (
                            <span className="flex items-center gap-1">
                              <Icon name="building" size={12} />
                              <strong>Location:</strong> {item.stallCode} ({item.stableLocation})
                            </span>
                          )}
                          <span className="flex items-center gap-1">
                            <Icon name="user" size={12} />
                            <strong>Reported by:</strong>
                            <span>{item.reportedByName}</span>
                            {item.reportedByRole && (
                              <span className="rounded bg-[var(--color-surface-muted)] px-1 py-0.5 text-[9px] font-bold uppercase text-[var(--color-text-secondary)] border border-[var(--color-border)]">
                                {item.reportedByRole}
                              </span>
                            )}
                          </span>
                          <span className="flex items-center gap-1">
                            <Icon name="clock" size={12} />
                            {new Date(item.reportedAt).toLocaleTimeString('en-US', {
                              hour: '2-digit',
                              minute: '2-digit',
                            })}
                          </span>
                        </div>
                      </div>

                      <div className="shrink-0 flex items-center gap-2">
                        <Link href={`/veterinarian/urgent/${item.scheduleId}`}>
                          <Button
                            variant={item.status === 'SCHEDULED' ? 'destructive' : 'primary'}
                            icon="chevron-right"
                          >
                            {item.status === 'SCHEDULED' ? 'Intake Case' : 'Examine'}
                          </Button>
                        </Link>
                      </div>
                    </div>
                  </Panel>
                ))}
              </div>
            )}
          </div>
        </PageContainer>
    </RoleGuard>
  );
}
