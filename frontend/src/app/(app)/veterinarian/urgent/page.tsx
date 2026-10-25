'use client';

import React, { useEffect, useState, useMemo } from 'react';
import Link from 'next/link';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { PageHeader } from '@/components/ui/PageHeader';
import { Panel } from '@/components/ui/Panel';
import { Button } from '@/components/ui/Button';
import { Pill } from '@/components/ui/StatusBadge';
import { Icon } from '@/components/ui/Icon';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { FilterChips } from '@/components/ui/SegmentedControl';
import { SearchInput } from '@/components/ui/Input';
import { MetricCard } from '@/components/ui/MetricCard';
import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { Notice } from '@/components/ui/Notice';
import { admissionsApi } from '@/features/admissions/services/api';
import type { UrgentAssignmentAlert } from '@/features/admissions/types';

export default function UrgentCasesPage() {
  const [urgentCases, setUrgentCases] = useState<UrgentAssignmentAlert[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<'ALL' | 'PENDING' | 'IN_PROGRESS'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  const loadData = async (silent = false) => {
    try {
      if (!silent) setLoading(true);
      setError(null);
      const pending = await admissionsApi.getPendingUrgentAlerts();
      setUrgentCases(pending || []);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to load urgent cases list');
    } finally {
      if (!silent) setLoading(false);
    }
  };

  useEffect(() => {
    let active = true;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadData();
    let interval: ReturnType<typeof setInterval> | null = setInterval(() => {
      if (active && document.visibilityState === 'visible') loadData(true);
    }, 15000);
    const handleVisibility = () => {
      if (document.visibilityState === 'hidden') {
        if (interval) { clearInterval(interval); interval = null; }
      } else {
        if (!interval) interval = setInterval(() => {
          if (active) loadData(true);
        }, 15000);
        if (active) loadData(true);
      }
    };
    document.addEventListener('visibilitychange', handleVisibility);
    return () => {
      active = false;
      if (interval) clearInterval(interval);
      document.removeEventListener('visibilitychange', handleVisibility);
    };
  }, []);

  const handleRefresh = async () => {
    await loadData(false);
  };

  const pendingCount = useMemo(
    () => urgentCases.filter((c) => c.status === 'SCHEDULED').length,
    [urgentCases],
  );
  const inProgressCount = useMemo(
    () => urgentCases.filter((c) => c.status === 'IN_PROGRESS').length,
    [urgentCases],
  );
  const criticalCount = useMemo(
    () => urgentCases.filter((c) => c.severity === 'CRITICAL').length,
    [urgentCases],
  );

  const filteredCases = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return urgentCases.filter((c) => {
      if (filter === 'PENDING' && c.status !== 'SCHEDULED') return false;
      if (filter === 'IN_PROGRESS' && c.status !== 'IN_PROGRESS') return false;

      if (!q) return true;
      return (
        c.horseName.toLowerCase().includes(q) ||
        (c.stallCode && c.stallCode.toLowerCase().includes(q)) ||
        (c.stableLocation && c.stableLocation.toLowerCase().includes(q)) ||
        (c.reportedByName && c.reportedByName.toLowerCase().includes(q)) ||
        c.title.toLowerCase().includes(q) ||
        String(c.horseId).includes(q) ||
        String(c.scheduleId).includes(q)
      );
    });
  }, [urgentCases, filter, searchQuery]);

  return (
    <RoleGuard allowedRoles={['VETERINARIAN']}>
      <PageContainer>
        <div className="space-y-6">
          {/* Header */}
          <PageHeader
            eyebrow="Clinical Emergency Operations"
            title="Urgent Cases"
            actions={
              <div className="flex items-center gap-2">
                {urgentCases.length > 0 && (
                  <Pill tone="danger">{urgentCases.length} active</Pill>
                )}
                <Button variant="secondary" icon="refresh" onClick={handleRefresh} loading={loading}>
                  Refresh
                </Button>
              </div>
            }
          />

          {/* Metric Cards Banner */}
          <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4">
            <MetricCard
              label="Total Urgent"
              value={urgentCases.length}
              icon="heart-pulse"
              tone={urgentCases.length > 0 ? 'default' : 'neutral'}
            />
            <MetricCard
              label="Pending Intake"
              value={pendingCount}
              icon="clock"
              tone={pendingCount > 0 ? 'warning' : 'neutral'}
            />
            <MetricCard
              label="In Examination"
              value={inProgressCount}
              icon="activity"
              tone={inProgressCount > 0 ? 'info' : 'neutral'}
            />
            <MetricCard
              label="Critical Priority"
              value={criticalCount}
              icon="alert-triangle"
              tone={criticalCount > 0 ? 'danger' : 'neutral'}
            />
          </div>

          {/* Filter & Search Bar */}
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="w-full sm:max-w-xs">
              <SearchInput
                value={searchQuery}
                onChange={setSearchQuery}
                placeholder="Search horse, stall, reporter…"
                label="Search urgent cases"
              />
            </div>

            <FilterChips
              label="Urgent cases filter"
              value={filter}
              onChange={(value) => setFilter(value as 'ALL' | 'PENDING' | 'IN_PROGRESS')}
              options={[
                { value: 'ALL', label: 'All', count: urgentCases.length },
                {
                  value: 'PENDING',
                  label: 'Pending Intake',
                  count: pendingCount,
                },
                {
                  value: 'IN_PROGRESS',
                  label: 'In Progress',
                  count: inProgressCount,
                },
              ]}
            />
          </div>

          {/* Error Banner */}
          {error && (
            <Notice tone="error">
              <div className="flex items-center justify-between gap-2">
                <span>{error}</span>
                <Button variant="link" size="sm" onClick={handleRefresh}>
                  Retry
                </Button>
              </div>
            </Notice>
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
                title={searchQuery ? 'No matching urgent cases found' : 'No urgent cases requiring attention'}
                description={
                  searchQuery
                    ? `No emergency cases matched "${searchQuery}". Try adjusting your search query or filter.`
                    : 'When staff report high or critical severity clinical incidents, emergency cases will appear here immediately.'
                }
                action={
                  searchQuery ? (
                    <Button variant="secondary" size="sm" onClick={() => setSearchQuery('')}>
                      Clear search
                    </Button>
                  ) : (
                    <Link
                      href="/veterinarian/admissions"
                      className="inline-flex items-center text-xs font-medium text-[var(--color-primary)] hover:underline"
                    >
                      Check admission queue &rarr;
                    </Link>
                  )
                }
              />
            </Panel>
          ) : (
            <div className="grid gap-3 sm:gap-4">
              {filteredCases.map((item) => (
                <Panel
                  key={item.scheduleId}
                  className={`overflow-hidden border border-[var(--color-border)] transition-all duration-200 ease-out hover:-translate-y-0.5 hover:shadow-md hover:border-[var(--color-border-strong)] ${
                    item.status === 'SCHEDULED'
                      ? 'border-l-4 border-l-[var(--color-danger)]'
                      : 'border-l-4 border-l-[var(--color-warning)]'
                  }`}
                >
                  <div className="p-4 sm:p-5 flex flex-col md:flex-row md:items-center justify-between gap-4">
                    <div className="flex items-start gap-3.5 flex-1 min-w-0">
                      <HorseAvatar name={item.horseName} image={null} size={48} rounded="md" />

                      <div className="space-y-1.5 flex-1 min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="font-bold text-sm sm:text-base text-[var(--color-text-primary)]">
                            {item.horseName}
                          </span>
                          <span className="text-[11px] font-mono text-[var(--color-text-muted)] bg-[var(--color-surface-muted)] px-1.5 py-0.5 rounded-[var(--radius-xs)] border border-[var(--color-border)]">
                            #{item.horseId}
                          </span>
                          <Pill tone={item.severity === 'CRITICAL' ? 'danger' : 'warning'}>
                            {item.severity}
                          </Pill>
                          <Pill tone={item.status === 'SCHEDULED' ? 'warning' : 'info'}>
                            {item.status === 'SCHEDULED' ? 'Pending Intake' : 'In Progress'}
                          </Pill>
                          {item.stallCode && (
                            <span className="inline-flex items-center gap-1 text-[11px] font-medium text-[var(--color-text-secondary)] bg-[var(--color-surface-subtle)] px-2 py-0.5 rounded-[var(--radius-xs)] border border-[var(--color-border)]">
                              <Icon name="building" size={11} />
                              <span>Stall {item.stallCode}</span>
                              {item.stableLocation && (
                                <span className="text-[var(--color-text-muted)]">({item.stableLocation})</span>
                              )}
                            </span>
                          )}
                        </div>

                        <h2 className="text-sm font-semibold text-[var(--color-text-primary)] truncate">
                          {item.title}
                        </h2>

                        <p className="text-xs text-[var(--color-text-secondary)] line-clamp-2 leading-relaxed">
                          {item.description || 'No detailed clinical description provided by reporter.'}
                        </p>

                        <div className="flex flex-wrap items-center gap-y-1 gap-x-4 text-xs text-[var(--color-text-muted)] pt-1">
                          <span className="flex items-center gap-1">
                            <Icon name="user" size={12} />
                            <span>Reported by:</span>
                            <strong className="text-[var(--color-text-secondary)]">
                              {item.reportedByName || 'Staff'}
                            </strong>
                            {item.reportedByRole && (
                              <span className="rounded bg-[var(--color-surface-muted)] px-1 py-0.5 text-[9px] font-bold uppercase text-[var(--color-text-secondary)] border border-[var(--color-border)]">
                                {item.reportedByRole}
                              </span>
                            )}
                          </span>

                          <span className="flex items-center gap-1">
                            <Icon name="clock" size={12} />
                            <span>
                              {new Date(item.reportedAt).toLocaleDateString('en-US', {
                                month: 'short',
                                day: 'numeric',
                              })}{' '}
                              at{' '}
                              {new Date(item.reportedAt).toLocaleTimeString('en-US', {
                                hour: '2-digit',
                                minute: '2-digit',
                              })}
                            </span>
                          </span>

                          <span className="font-mono text-[11px] text-[var(--color-text-muted)]">
                            Case #{item.scheduleId}
                          </span>
                        </div>
                      </div>
                    </div>

                    <div className="shrink-0 flex items-center md:self-center">
                      <Link href={`/veterinarian/urgent/${item.scheduleId}`}>
                        <Button
                          variant={item.status === 'SCHEDULED' ? 'destructive' : 'primary'}
                          icon="chevron-right"
                          className="transition-all active:scale-[0.97]"
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
