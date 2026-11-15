'use client';

import React, { useEffect, useState, useMemo } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
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
import { formatDateTime } from '@/lib/display';
import type { UrgentAssignmentAlert } from '@/features/admissions/types';

export default function UrgentCasesPage() {
  const router = useRouter();
  const [urgentCases, setUrgentCases] = useState<UrgentAssignmentAlert[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<'ALL' | 'ACTIVE' | 'COMPLETED'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  const loadData = async (silent = false) => {
    try {
      if (!silent) setLoading(true);
      setError(null);
      const allCases = await admissionsApi.getPendingUrgentAlerts(true);
      setUrgentCases(allCases || []);
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

  const activeCount = useMemo(
    () => urgentCases.filter((c) => c.status !== 'COMPLETED').length,
    [urgentCases],
  );
  const completedCount = useMemo(
    () => urgentCases.filter((c) => c.status === 'COMPLETED').length,
    [urgentCases],
  );
  const criticalCount = useMemo(
    () => urgentCases.filter((c) => c.incidentType === 'TRAUMA' && c.status !== 'COMPLETED').length,
    [urgentCases],
  );

  const filteredCases = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    const list = urgentCases.filter((c) => {
      if (filter === 'ACTIVE' && c.status === 'COMPLETED') return false;
      if (filter === 'COMPLETED' && c.status !== 'COMPLETED') return false;

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

    // Sắp xếp theo thời gian mới nhất đứng trước (reportedAt DESC, scheduledAt DESC, scheduleId DESC)
    return list.sort((a, b) => {
      const timeA = a.reportedAt
        ? new Date(a.reportedAt).getTime()
        : a.scheduledAt
        ? new Date(a.scheduledAt).getTime()
        : 0;
      const timeB = b.reportedAt
        ? new Date(b.reportedAt).getTime()
        : b.scheduledAt
        ? new Date(b.scheduledAt).getTime()
        : 0;
      if (timeB !== timeA) {
        return timeB - timeA;
      }
      return (b.scheduleId || 0) - (a.scheduleId || 0);
    });
  }, [urgentCases, filter, searchQuery]);

  return (
    <RoleGuard allowedRoles={['VETERINARIAN']}>
      <PageContainer>
        <div className="space-y-6">
          {/* Header */}
          <PageHeader
            title="Urgent Cases"
            actions={
              <div className="flex items-center gap-2">
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
              label="Active Cases"
              value={activeCount}
              icon="activity"
              tone={activeCount > 0 ? 'warning' : 'neutral'}
            />
            <MetricCard
              label="Completed"
              value={completedCount}
              icon="check"
              tone={completedCount > 0 ? 'success' : 'neutral'}
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
              onChange={(value) => setFilter(value as 'ALL' | 'ACTIVE' | 'COMPLETED')}
              options={[
                { value: 'ALL', label: 'All', count: urgentCases.length },
                {
                  value: 'ACTIVE',
                  label: 'Active',
                  count: activeCount,
                },
                {
                  value: 'COMPLETED',
                  label: 'Completed',
                  count: completedCount,
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
            <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
              {filteredCases.map((item) => (
                <Panel
                  key={item.scheduleId}
                  padded
                  onClick={() => router.push(`/veterinarian/urgent/${item.scheduleId}`)}
                  className={`cursor-pointer border border-[var(--color-border)] transition-all duration-200 ease-out hover:-translate-y-0.5 hover:shadow-md hover:border-[var(--color-border-strong)] ${
                    item.status === 'COMPLETED'
                      ? 'border-l-4 border-l-[var(--color-success)]'
                      : item.status === 'SCHEDULED'
                        ? 'border-l-4 border-l-[var(--color-danger)]'
                        : 'border-l-4 border-l-[var(--color-warning)]'
                  }`}
                >
                  <div className="flex items-center justify-between gap-3">
                    {/* Left: Avatar + Details */}
                    <div className="flex items-center gap-3 min-w-0 flex-1">
                      <HorseAvatar
                        name={item.horseName}
                        image={item.horseId ? `/api/horses/${item.horseId}/photo` : null}
                        size={42}
                        rounded="md"
                      />
                      <div className="min-w-0 flex-1">
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="text-base font-bold text-[var(--color-text-primary)] truncate">
                            {item.horseName}
                          </span>
                          <Pill
                            tone={item.incidentType === 'TRAUMA' ? 'danger' : 'warning'}
                            size="sm"
                            icon="alert-triangle"
                          >
                            {item.incidentType}
                          </Pill>
                          <Pill
                            tone={
                              item.status === 'COMPLETED'
                                ? 'success'
                                : 'warning'
                            }
                            size="sm"
                          >
                            {item.status === 'COMPLETED' ? 'Completed' : 'Active'}
                          </Pill>
                        </div>
                        <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-[var(--color-text-secondary)]">
                          <span className="inline-flex items-center gap-1 min-w-0 max-w-[200px] truncate">
                            <Icon name="alert-triangle" size={12} className="text-[var(--color-danger)] shrink-0" />
                            <strong className="truncate">{item.title}</strong>
                          </span>
                          <span>·</span>
                          <span className="inline-flex items-center gap-1 shrink-0">
                            <Icon name="building" size={12} />
                            <span>Stall: <strong>{item.stallCode || 'Unassigned'}</strong></span>
                          </span>
                          <span>·</span>
                          <span className="inline-flex items-center gap-1 shrink-0">
                            <Icon name="clock" size={12} />
                            <span>{formatDateTime(item.reportedAt)}</span>
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* Right: Action Button */}
                    <div className="shrink-0">
                      <Button
                        variant={
                          item.status === 'COMPLETED'
                            ? 'secondary'
                            : item.status === 'SCHEDULED'
                              ? 'destructive'
                              : 'primary'
                        }
                        size="sm"
                        icon="chevron-right"
                        onClick={(e) => {
                          e.stopPropagation();
                          router.push(`/veterinarian/urgent/${item.scheduleId}`);
                        }}
                        className="transition-all active:scale-[0.97]"
                      >
                        {item.status === 'COMPLETED'
                          ? 'View Record'
                          : item.status === 'SCHEDULED'
                            ? 'Intake Case'
                            : 'Examine'}
                      </Button>
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
