'use client';

import React, { useState, useEffect, useCallback, useMemo } from 'react';
import Link from 'next/link';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { Panel } from '@/components/ui/Panel';
import { Button } from '@/components/ui/Button';
import { Pill } from '@/components/ui/StatusBadge';
import { InjuryCorrectionModal } from '@/features/health/components/InjuryCorrectionModal';
import { healthApi } from '@/features/health/services/healthApi';
import { stableApi } from '@/features/stable/services/stableService';
import type { InjuryRecord, HorseHealthSummary } from '@/features/health/types';
import type { Horse, StableStall } from '@/features/stable/types';

interface HorseHealthOverviewItem {
  horse: Horse;
  stallCode: string;
  summary: HorseHealthSummary | null;
  activeInjuries: InjuryRecord[];
}

export default function HealthRecordsPage() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [horses, setHorses] = useState<Horse[]>([]);
  const [stalls, setStalls] = useState<StableStall[]>([]);
  const [summaries, setSummaries] = useState<Record<number, HorseHealthSummary>>({});
  const [injuriesByHorse, setInjuriesByHorse] = useState<Record<number, InjuryRecord[]>>({});
  const [selectedInjury, setSelectedInjury] = useState<InjuryRecord | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);

  const refreshData = useCallback(() => {
    setReloadKey((k) => k + 1);
  }, []);

  useEffect(() => {
    let ignore = false;
    const run = async () => {
      setLoading(true);
      setError(null);
      try {
        const [horseList, stallList] = await Promise.all([
          stableApi.getHorses(),
          stableApi.getStalls(),
        ]);

        if (ignore) return;
        setHorses(horseList || []);
        setStalls(stallList || []);

        // Prioritize active stable horses
        const eligibleHorses = (horseList || []).filter(
          (h) => h.currentStatus === 'ELIGIBLE' || h.currentStallId != null,
        );
        const targetHorses = eligibleHorses.length > 0 ? eligibleHorses : (horseList || []);

        // Fetch health summaries for horses in stable
        const summaryResults = await Promise.all(
          targetHorses.map(async (h) => {
            try {
              const sum = await healthApi.getHealthSummary(h.id);
              return { horseId: h.id, summary: sum };
            } catch {
              return { horseId: h.id, summary: null };
            }
          }),
        );

        if (ignore) return;
        const sumMap: Record<number, HorseHealthSummary> = {};
        for (const res of summaryResults) {
          if (res.summary) sumMap[res.horseId] = res.summary;
        }
        setSummaries(sumMap);

        // Only fetch detailed injury records for horses indicated to have active injuries or blocked status
        const horsesWithInjuries = targetHorses.filter((h) => {
          const sum = sumMap[h.id];
          return (sum && sum.activeInjuryCount > 0) || h.trainingDecision === 'BLOCKED';
        });

        const injuryResults = await Promise.all(
          horsesWithInjuries.map(async (h) => {
            try {
              const injs = await healthApi.getHorseInjuries(h.id);
              return { horseId: h.id, injuries: injs || [] };
            } catch {
              return { horseId: h.id, injuries: [] };
            }
          }),
        );

        if (ignore) return;
        const injMap: Record<number, InjuryRecord[]> = {};
        for (const res of injuryResults) {
          injMap[res.horseId] = res.injuries;
        }
        setInjuriesByHorse(injMap);
      } catch (err: unknown) {
        if (!ignore) {
          const msg = err instanceof Error ? err.message : 'Failed to load stable health data';
          setError(msg);
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    };

    run();
    return () => {
      ignore = true;
    };
  }, [reloadKey]);

  const stallMap = useMemo(() => {
    const map = new Map<number, string>();
    for (const s of stalls) {
      map.set(s.id, s.stallCode);
    }
    return map;
  }, [stalls]);

  const overviewList = useMemo<HorseHealthOverviewItem[]>(() => {
    return horses.map((h) => ({
      horse: h,
      stallCode: (h.currentStallId && stallMap.get(h.currentStallId)) || 'Unassigned',
      summary: summaries[h.id] ?? null,
      activeInjuries: (injuriesByHorse[h.id] || []).filter((i) => i.status === 'ACTIVE'),
    }));
  }, [horses, stallMap, summaries, injuriesByHorse]);

  // Aggregate stats from real backend summaries & horse data
  const stats = useMemo(() => {
    let allowedCount = 0;
    let blockedCount = 0;
    let openInjuriesCount = 0;
    let urgentCasesCount = 0;

    for (const item of overviewList) {
      const decision = item.summary?.trainingDecision ?? item.horse.trainingDecision ?? 'ALLOWED';
      if (decision === 'BLOCKED') {
        blockedCount++;
      } else {
        allowedCount++;
      }

      if (item.activeInjuries.length > 0 || (item.summary?.activeInjuryCount ?? 0) > 0) {
        openInjuriesCount++;
      }

      if (item.summary?.hasPendingUrgent) {
        urgentCasesCount++;
      }
    }

    return { allowedCount, blockedCount, openInjuriesCount, urgentCasesCount };
  }, [overviewList]);

  const handleOpenInjuryModal = (injury: InjuryRecord) => {
    setSelectedInjury(injury);
    setModalOpen(true);
  };

  const handleModalSuccess = () => {
    setModalOpen(false);
    refreshData();
  };

  return (
    <RoleGuard allowedRoles={['VETERINARIAN']}>
      <AppShell>
        <PageContainer>
          <div className="space-y-6">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
              <div>
                <h1 className="text-2xl font-bold tracking-tight text-[var(--color-text-primary)]">
                  Stable Health Overview
                </h1>
                <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
                  Monitor health status, active injuries, and training clearance for all stable horses.
                </p>
              </div>

              <div className="flex items-center gap-2">
                <Button variant="secondary" icon="refresh" onClick={refreshData} disabled={loading}>
                  Refresh
                </Button>
                <Link href="/veterinarian/admissions">
                  <Button variant="secondary" icon="search">
                    Admission Queue
                  </Button>
                </Link>
              </div>
            </div>

            {error && (
              <div className="p-4 rounded-lg bg-red-50 text-red-700 dark:bg-red-950/40 dark:text-red-300 border border-red-200 dark:border-red-900/50 text-sm">
                {error}
              </div>
            )}

            {/* Farm health metrics pills */}
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
              <Panel padded className="flex flex-col">
                <span className="text-xs font-semibold uppercase text-[var(--color-text-muted)]">
                  Cleared (Allowed)
                </span>
                <span className="mt-2 text-2xl font-extrabold text-[var(--color-success)]">
                  {loading ? '...' : stats.allowedCount}
                </span>
              </Panel>
              <Panel padded className="flex flex-col">
                <span className="text-xs font-semibold uppercase text-[var(--color-text-muted)]">
                  Blocked from Training
                </span>
                <span className="mt-2 text-2xl font-extrabold text-[var(--color-danger)]">
                  {loading ? '...' : stats.blockedCount}
                </span>
              </Panel>
              <Panel padded className="flex flex-col">
                <span className="text-xs font-semibold uppercase text-[var(--color-text-muted)]">
                  Active Injuries
                </span>
                <span className="mt-2 text-2xl font-extrabold text-amber-600 dark:text-amber-400">
                  {loading ? '...' : stats.openInjuriesCount}
                </span>
              </Panel>
              <Panel padded className="flex flex-col">
                <span className="text-xs font-semibold uppercase text-[var(--color-text-muted)]">
                  Urgent Cases
                </span>
                <span className="mt-2 text-2xl font-extrabold text-[var(--color-primary)]">
                  {loading ? '...' : stats.urgentCasesCount}
                </span>
              </Panel>
            </div>

            {/* Table */}
            <Panel padded className="space-y-4">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead>
                    <tr className="border-b border-[var(--color-border)] text-[var(--color-text-muted)] uppercase">
                      <th className="pb-3 font-semibold">Horse</th>
                      <th className="pb-3 font-semibold">Stall</th>
                      <th className="pb-3 font-semibold">Training Clearance</th>
                      <th className="pb-3 font-semibold">Last Examined</th>
                      <th className="pb-3 font-semibold">Active Injuries</th>
                      <th className="pb-3 font-semibold text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[var(--color-border)]">
                    {loading ? (
                      <tr>
                        <td colSpan={6} className="py-8 text-center text-sm text-[var(--color-text-muted)]">
                          Loading stable health overview...
                        </td>
                      </tr>
                    ) : overviewList.length === 0 ? (
                      <tr>
                        <td colSpan={6} className="py-8 text-center text-sm text-[var(--color-text-muted)]">
                          No horse records found.
                        </td>
                      </tr>
                    ) : (
                      overviewList.map((item) => {
                        const decision = item.summary?.trainingDecision ?? item.horse.trainingDecision ?? 'ALLOWED';
                        const reason = item.summary?.trainingDecisionReason ?? item.horse.trainingDecisionReason ?? null;
                        const lastExaminedAt = item.summary?.lastExaminedAt;
                        const lastExam = lastExaminedAt
                          ? new Date(lastExaminedAt).toLocaleDateString('en-US')
                          : 'None';
                        const activeCount = item.activeInjuries.length > 0
                          ? item.activeInjuries.length
                          : (item.summary?.activeInjuryCount ?? 0);
                        const firstInjury = item.activeInjuries[0] ?? null;

                        return (
                          <tr key={item.horse.id} className="hover:bg-[var(--color-surface-muted)]">
                            <td className="py-3 font-medium text-[var(--color-text-primary)]">
                              {item.horse.name} <span className="font-mono text-[var(--color-text-muted)]">#{item.horse.id}</span>
                            </td>
                            <td className="py-3 text-[var(--color-text-secondary)]">
                              {item.stallCode}
                            </td>
                            <td className="py-3">
                              <Pill tone={decision === 'ALLOWED' ? 'success' : 'danger'}>
                                {decision}
                              </Pill>
                              {reason && (
                                <p className="mt-0.5 text-[10px] text-[var(--color-danger)] font-medium max-w-xs truncate" title={reason}>
                                  {reason}
                                </p>
                              )}
                            </td>
                            <td className="py-3 text-[var(--color-text-secondary)]">
                              {lastExam}
                            </td>
                            <td className="py-3">
                              {activeCount > 0 ? (
                                <span className="text-[var(--color-danger)] font-semibold">
                                  {activeCount} active
                                </span>
                              ) : (
                                <span className="text-[var(--color-text-muted)]">None</span>
                              )}
                            </td>
                            <td className="py-3 text-right">
                              {firstInjury ? (
                                <Button
                                  variant="secondary"
                                  onClick={() => handleOpenInjuryModal(firstInjury)}
                                >
                                  View Injury
                                </Button>
                              ) : (
                                <Button variant="secondary" disabled>
                                  Normal
                                </Button>
                              )}
                            </td>
                          </tr>
                        );
                      })
                    )}
                  </tbody>
                </table>
              </div>
            </Panel>
          </div>

          {/* Injury Record Modal */}
          {selectedInjury && (
            <InjuryCorrectionModal
              open={modalOpen}
              onClose={() => setModalOpen(false)}
              horseId={selectedInjury.horseId}
              injury={selectedInjury}
              onSuccess={handleModalSuccess}
            />
          )}
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
