'use client';

import React, { useState, useEffect, useCallback, useMemo, useRef, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { Panel } from '@/components/ui/Panel';
import { Button, LinkButton } from '@/components/ui/Button';
import { Pill } from '@/components/ui/StatusBadge';
import { DataTable } from '@/components/ui/DataTable';
import { FilterChips, Tabs, SearchInput, MetricCard, Notice, PageHeader } from '@/components/ui';
import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { Icon } from '@/components/ui/Icon';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { InjuryCorrectionModal } from '@/features/health/components/InjuryCorrectionModal';
import { TreatmentPlanModal } from '@/features/care/shared/components/TreatmentPlanModal';
import { healthApi } from '@/features/health/services/healthApi';
import { stableApi } from '@/features/stable/services/stableService';
import { careService } from '@/features/care/services/careService';
import { formatDate } from '@/lib/display';
import type { InjuryRecord, HorseHealthSummary, HealthRecord } from '@/features/health/types';
import type { Horse, StableStall } from '@/features/stable/types';
import type { HorseHealthMetric } from '@/features/horses/types';
import type { HorsePreventiveStatus } from '@/features/care/types/preventive';
import type { TreatmentPlan } from '@/features/care/types';

type StatusFilter = 'ALL' | 'ALLOWED' | 'BLOCKED' | 'HAS_INJURY' | 'URGENT';
type CategoryFilter = 'ALL' | 'STABLE' | 'ELIGIBLE';

interface HorseListItem {
  horse: Horse;
  stallCode: string;
  summary: HorseHealthSummary | null;
  activeInjuries: InjuryRecord[];
}

function HealthRecordsContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const paramHorseId = searchParams?.get('horseId');

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [horses, setHorses] = useState<Horse[]>([]);
  const [stalls, setStalls] = useState<StableStall[]>([]);
  const [summaries, setSummaries] = useState<Record<number, HorseHealthSummary>>({});
  const [reloadKey, setReloadKey] = useState(0);
  const fetchingIdsRef = useRef<Set<number>>(new Set());

  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
  const [categoryFilter, setCategoryFilter] = useState<CategoryFilter>('ALL');
  const [page, setPage] = useState(0);
  const pageSize = 20;

  const [selectedHorseId, setSelectedHorseId] = useState<number | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [healthSummary, setHealthSummary] = useState<HorseHealthSummary | null>(null);
  const [injuries, setInjuries] = useState<InjuryRecord[]>([]);
  const [metrics, setMetrics] = useState<HorseHealthMetric[]>([]);
  const [healthRecords, setHealthRecords] = useState<HealthRecord[]>([]);
  const [preventive, setPreventive] = useState<HorsePreventiveStatus | null>(null);
  const [selectedInjury, setSelectedInjury] = useState<InjuryRecord | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [planTargetRecord, setPlanTargetRecord] = useState<HealthRecord | null>(null);
  const [plansByRecordId, setPlansByRecordId] = useState<Record<number, TreatmentPlan[]>>({});
  const [expandedRecordPlans, setExpandedRecordPlans] = useState<Set<number>>(new Set());
  const [openFollowUp, setOpenFollowUp] = useState<{ id: number; scheduledAt: string | null } | null>(null);
  const [followUpLoading, setFollowUpLoading] = useState(false);

  const refreshData = useCallback(() => {
    fetchingIdsRef.current.clear();
    setSummaries({});
    setReloadKey((k) => k + 1);
  }, []);

  // 1. Initial Stable & Horse Roster Fetch (Fast, avoids N+1 query storm)
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

  const loadHorseDetail = useCallback(async (horseId: number) => {
    setDetailLoading(true);
    try {
      const [sum, inj, met, rec, prev] = await Promise.all([
        healthApi.getHealthSummary(horseId).catch(() => null),
        healthApi.getHorseInjuries(horseId).catch(() => []),
        healthApi.getHealthMetrics(horseId).catch(() => []),
        healthApi.getHealthRecords(horseId).catch(() => []),
        healthApi.getPreventiveStatus(horseId).catch(() => null),
      ]);
      setHealthSummary(sum);
      setInjuries((inj || []).filter((i) => i.status !== 'SUPERSEDED'));
      setMetrics(met || []);
      setHealthRecords(rec || []);
      setPreventive(prev);

      if (rec && rec.length > 0) {
        const planEntries = await Promise.all(
          rec.map(async (r) => {
            try {
              const plans = await careService.getTreatmentPlans(r.id);
              return [r.id, plans || []] as const;
            } catch {
              return [r.id, []] as const;
            }
          })
        );
        const map = Object.fromEntries(planEntries);
        setPlansByRecordId(map);
        const withPlans = rec.filter((r) => (map[r.id] || []).length > 0).map((r) => r.id);
        setExpandedRecordPlans(new Set(withPlans));
      } else {
        setPlansByRecordId({});
        setExpandedRecordPlans(new Set());
      }
    } finally {
      setDetailLoading(false);
    }
  }, []);

  // 2. AC-3.1: Deep-Link handler for ?horseId=X (e.g. from Dashboard Watchlist)
  useEffect(() => {
    if (paramHorseId) {
      const id = Number(paramHorseId);
      if (!Number.isNaN(id) && id > 0) {
        // eslint-disable-next-line react-hooks/set-state-in-effect
        setSelectedHorseId(id);
        void loadHorseDetail(id);
      }
    }
  }, [paramHorseId, loadHorseDetail]);

  const openHorse = useCallback(
    (horseId: number) => {
      setSelectedHorseId(horseId);
      void loadHorseDetail(horseId);
    },
    [loadHorseDetail],
  );

  const closeHorse = useCallback(() => {
    setSelectedHorseId(null);
    setHealthSummary(null);
    setInjuries([]);
    setMetrics([]);
    setHealthRecords([]);
    setPreventive(null);
    setPlansByRecordId({});
    setExpandedRecordPlans(new Set());
    router.replace('/veterinarian/health');
  }, [router]);

  const stallMap = useMemo(() => {
    const map = new Map<number, string>();
    for (const s of stalls) {
      map.set(s.id, s.stallCode);
    }
    return map;
  }, [stalls]);

  // Rejected horses are out of the clinical roster: no training to clear, so keep them out of the list and metrics.
  const activeHorses = useMemo(
    () => horses.filter((h) => h.currentStatus !== 'REJECTED'),
    [horses],
  );

  // VET-HLT-08: the vet needs a way back into the examination that will lift a BLOCKED decision.
  useEffect(() => {
    if (selectedHorseId == null) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setOpenFollowUp(null);
      return;
    }
    let ignore = false;
    setFollowUpLoading(true);
    careService
      .listSchedules({ horseId: selectedHorseId, careType: 'ROUTINE', size: 20 })
      .then((page) => {
        if (ignore) return;
        const open = (page.content || []).filter(
          (s) => s.status === 'SCHEDULED' || s.status === 'IN_PROGRESS',
        );
        const next = open.find((s) => s.purpose === 'FOLLOW_UP') ?? open[0];
        setOpenFollowUp(next ? { id: next.id, scheduledAt: next.scheduledAt ?? null } : null);
      })
      .catch(() => {
        if (!ignore) setOpenFollowUp(null);
      })
      .finally(() => {
        if (!ignore) setFollowUpLoading(false);
      });
    return () => {
      ignore = true;
    };
  }, [selectedHorseId]);

  const listItems = useMemo<HorseListItem[]>(() => {
    return activeHorses.map((h) => ({
      horse: h,
      stallCode: (h.currentStallId && stallMap.get(h.currentStallId)) || 'Unassigned',
      summary: summaries[h.id] ?? null,
      activeInjuries: [],
    }));
  }, [activeHorses, stallMap, summaries]);

  const filteredItems = useMemo(() => {
    const q = search.trim().toLowerCase();
    return listItems.filter((item) => {
      if (q) {
        const name = item.horse.name?.toLowerCase() || '';
        const id = String(item.horse.id);
        const stall = item.stallCode.toLowerCase();
        if (!(name.includes(q) || id.includes(q) || stall.includes(q))) {
          return false;
        }
      }
      if (categoryFilter === 'STABLE' && !item.horse.currentStallId) return false;
      if (categoryFilter === 'ELIGIBLE' && item.horse.currentStatus !== 'ELIGIBLE') return false;
      const decision = item.summary?.trainingDecision ?? item.horse.trainingDecision ?? 'ALLOWED';
      if (statusFilter === 'ALLOWED' && decision !== 'ALLOWED') return false;
      if (statusFilter === 'BLOCKED' && decision !== 'BLOCKED') return false;
      if (statusFilter === 'HAS_INJURY') {
        const sum = summaries[item.horse.id];
        if (sum !== undefined && (sum.activeInjuryCount ?? 0) === 0) return false;
      }
      if (statusFilter === 'URGENT') {
        const sum = summaries[item.horse.id];
        if (sum !== undefined && !sum.hasPendingUrgent) return false;
      }
      return true;
    });
  }, [listItems, search, categoryFilter, statusFilter, summaries]);

  const pagedItems = useMemo(() => {
    const start = page * pageSize;
    return filteredItems.slice(start, start + pageSize);
  }, [filteredItems, page]);

  // 3. AC-8: Progressive batch fetch of Health Summaries for ALL horses in roster
  useEffect(() => {
    let ignore = false;
    if (activeHorses.length === 0) return;

    const uncached = activeHorses.filter(
      (h) => summaries[h.id] === undefined && !fetchingIdsRef.current.has(h.id)
    );
    if (uncached.length === 0) return;

    // Prioritize visible page items
    const pagedIdSet = new Set(pagedItems.map((item) => item.horse.id));
    const priorityItems = uncached.filter((h) => pagedIdSet.has(h.id));
    const remainingItems = uncached.filter((h) => !pagedIdSet.has(h.id));
    const sortedQueue = [...priorityItems, ...remainingItems];

    sortedQueue.forEach((h) => fetchingIdsRef.current.add(h.id));

    const BATCH_SIZE = 8;
    const fetchQueue = async () => {
      for (let i = 0; i < sortedQueue.length; i += BATCH_SIZE) {
        if (ignore) break;
        const batch = sortedQueue.slice(i, i + BATCH_SIZE);
        const results = await Promise.all(
          batch.map(async (h) => {
            try {
              const sum = await healthApi.getHealthSummary(h.id);
              return { id: h.id, sum };
            } catch {
              return { id: h.id, sum: null };
            }
          }),
        );
        if (ignore) break;
        setSummaries((prev) => {
          const next = { ...prev };
          for (const { id, sum } of results) {
            next[id] = sum ?? ({
              horseId: id,
              activeInjuryCount: 0,
              hasPendingUrgent: false,
              trainingDecision: 'ALLOWED',
            } as HorseHealthSummary);
          }
          return next;
        });
      }
    };

    void fetchQueue();

    return () => {
      ignore = true;
    };
  }, [activeHorses, summaries, pagedItems]);

  const stats = useMemo(() => {
    let allowedCount = 0;
    let blockedCount = 0;
    let openInjuriesCount = 0;
    let urgentCasesCount = 0;

    for (const h of activeHorses) {
      const decision = summaries[h.id]?.trainingDecision ?? h.trainingDecision ?? 'ALLOWED';
      if (decision === 'BLOCKED') {
        blockedCount++;
      } else {
        allowedCount++;
      }
      const injuryCount = summaries[h.id]?.activeInjuryCount ?? 0;
      if (injuryCount > 0) {
        openInjuriesCount++;
      }
      if (summaries[h.id]?.hasPendingUrgent) {
        urgentCasesCount++;
      }
    }
    return { allowedCount, blockedCount, openInjuriesCount, urgentCasesCount };
  }, [activeHorses, summaries]);

  const handleOpenInjuryModal = (injury: InjuryRecord) => {
    setSelectedInjury(injury);
    setModalOpen(true);
  };

  const handleModalSuccess = () => {
    setModalOpen(false);
    refreshData();
    if (selectedHorseId != null) loadHorseDetail(selectedHorseId);
  };

  const selectedHorse = useMemo(
    () => horses.find((h) => h.id === selectedHorseId) || null,
    [horses, selectedHorseId],
  );

  // Handle non-existent deep-linked horse ID gracefully
  useEffect(() => {
    if (selectedHorseId != null && !loading && horses.length > 0 && !selectedHorse) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setError(`Horse #${selectedHorseId} was not found in stable records.`);
      setSelectedHorseId(null);
      router.replace('/veterinarian/health');
    }
  }, [selectedHorseId, loading, horses, selectedHorse, router]);

  if (selectedHorseId != null && selectedHorse) {
    const decision = healthSummary?.trainingDecision ?? selectedHorse.trainingDecision ?? 'ALLOWED';
    const reason = healthSummary?.trainingDecisionReason ?? selectedHorse.trainingDecisionReason ?? null;
    const latestMetric = metrics[0] || null;
    const sortedRecords = [...healthRecords].sort((a, b) => {
      const ta = a.examinedAt ? new Date(a.examinedAt).getTime() : 0;
      const tb = b.examinedAt ? new Date(b.examinedAt).getTime() : 0;
      return tb - ta;
    });

    return (
      <RoleGuard allowedRoles={['VETERINARIAN']}>
        <PageContainer>
          <div className="space-y-6">
            {/* Patient Header */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-[var(--color-border)] pb-4">
              <div className="flex items-start gap-3.5">
                <HorseAvatar name={selectedHorse.name} image={null} size={50} rounded="md" />
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-[var(--color-text-primary)]">
                      {selectedHorse.name}
                    </h1>
                    <span className="text-xs font-mono text-[var(--color-text-muted)] bg-[var(--color-surface-muted)] px-1.5 py-0.5 rounded-[var(--radius-xs)] border border-[var(--color-border)]">
                      #{selectedHorse.id}
                    </span>
                    {selectedHorse.breed && (
                      <span className="text-xs text-[var(--color-text-muted)] font-medium">
                        ({selectedHorse.breed})
                      </span>
                    )}
                    <Pill tone={decision === 'ALLOWED' ? 'success' : 'danger'}>
                      {decision === 'ALLOWED' ? 'Fit for Training' : 'Training Blocked'}
                    </Pill>
                  </div>
                  <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-[var(--color-text-secondary)]">
                    <span className="inline-flex items-center gap-1">
                      <Icon name="building" size={12} />
                      <span>
                        Stall:{' '}
                        <strong>
                          {(selectedHorse.currentStallId && stallMap.get(selectedHorse.currentStallId)) ||
                            'Unassigned'}
                        </strong>
                      </span>
                    </span>
                    <span>·</span>
                    <span>
                      Status: <strong>{selectedHorse.currentStatus}</strong>
                    </span>
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <Button
                  variant="secondary"
                  size="sm"
                  icon="arrow-left"
                  onClick={closeHorse}
                  className="transition-all active:scale-[0.98]"
                >
                  Back to stable list
                </Button>
              </div>
            </div>

            {/* Top Overview: Status & Clearance + Vitals */}
            <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
              <Panel padded className="space-y-3 lg:col-span-2 border border-[var(--color-border)]">
                <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-2.5">
                  <div className="flex items-center gap-2">
                    <Icon name="shield" size={16} className="text-[var(--color-primary)]" />
                    <h2 className="text-sm font-bold text-[var(--color-text-primary)]">
                      Clinical Status & Training Clearance
                    </h2>
                  </div>
                  <Pill tone={decision === 'ALLOWED' ? 'success' : 'danger'}>{decision}</Pill>
                </div>
                {reason && (
                  <div className="rounded-[var(--radius-sm)] border border-[var(--color-warning)] bg-[var(--color-warning-soft)]/20 p-2.5 text-xs text-[var(--color-warning)]">
                    <strong>Clearance Reason:</strong> {reason}
                  </div>
                )}
                {decision === 'BLOCKED' && (
                  <div className="flex flex-wrap items-center justify-between gap-2 rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)]/20 p-2.5 text-xs">
                    <span className="text-[var(--color-danger)] font-medium">
                      {followUpLoading
                        ? 'Looking for an open follow-up examination…'
                        : openFollowUp
                          ? `Open follow-up #${openFollowUp.id}${
                              openFollowUp.scheduledAt ? ` · ${formatDate(openFollowUp.scheduledAt)}` : ''
                            } — complete it to reassess clearance.`
                          : 'No open follow-up examination for this horse. Schedule one in Routine Care to reassess clearance.'}
                    </span>
                    {!followUpLoading &&
                      (openFollowUp ? (
                        <Button
                          variant="primary"
                          size="sm"
                          icon="stethoscope"
                          onClick={() => router.push(`/veterinarian/routine/${openFollowUp.id}`)}
                          className="transition-all active:scale-[0.98]"
                        >
                          Reassess clearance
                        </Button>
                      ) : (
                        <LinkButton
                          href="/veterinarian/routine"
                          size="sm"
                          icon="calendar"
                          className="transition-all active:scale-[0.98]"
                        >
                          Open Routine Care
                        </LinkButton>
                      ))}
                  </div>
                )}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-1 text-xs">
                  <div>
                    <span className="text-[var(--color-text-muted)] font-semibold uppercase tracking-wider text-[11px] block">
                      Last Examined
                    </span>
                    <span className="font-medium text-[var(--color-text-primary)]">
                      {healthSummary?.lastExaminedAt
                        ? formatDate(healthSummary.lastExaminedAt)
                        : 'No examination on record'}
                    </span>
                  </div>
                  <div>
                    <span className="text-[var(--color-text-muted)] font-semibold uppercase tracking-wider text-[11px] block">
                      Last Diagnosis
                    </span>
                    <span className="font-medium text-[var(--color-text-primary)]">
                      {healthSummary?.lastDiagnosis || 'None reported'}
                    </span>
                  </div>
                </div>
              </Panel>

              <Panel padded className="space-y-2 border border-[var(--color-border)]">
                <div className="flex items-center gap-2 border-b border-[var(--color-border)] pb-2.5">
                  <Icon name="heart-pulse" size={16} className="text-[var(--color-medical)]" />
                  <h2 className="text-sm font-bold text-[var(--color-text-primary)]">
                    Latest Vitals Telemetry
                  </h2>
                </div>
                {latestMetric ? (
                  <div className="grid grid-cols-2 gap-2.5 text-xs pt-1">
                    {latestMetric.heartRate != null && (
                      <div className="rounded bg-[var(--color-surface-subtle)] p-2 border border-[var(--color-border)]">
                        <span className="text-[var(--color-text-muted)] block text-[10px] uppercase font-bold">
                          Heart Rate
                        </span>
                        <span className="font-metric text-sm font-bold text-[var(--color-text-primary)]">
                          {latestMetric.heartRate} bpm
                        </span>
                      </div>
                    )}
                    {latestMetric.temperature != null && (
                      <div className="rounded bg-[var(--color-surface-subtle)] p-2 border border-[var(--color-border)]">
                        <span className="text-[var(--color-text-muted)] block text-[10px] uppercase font-bold">
                          Temperature
                        </span>
                        <span className="font-metric text-sm font-bold text-[var(--color-text-primary)]">
                          {latestMetric.temperature} °C
                        </span>
                      </div>
                    )}
                    {latestMetric.respiratoryRate != null && (
                      <div className="rounded bg-[var(--color-surface-subtle)] p-2 border border-[var(--color-border)]">
                        <span className="text-[var(--color-text-muted)] block text-[10px] uppercase font-bold">
                          Respiration
                        </span>
                        <span className="font-metric text-sm font-bold text-[var(--color-text-primary)]">
                          {latestMetric.respiratoryRate} rpm
                        </span>
                      </div>
                    )}
                    {latestMetric.weight != null && (
                      <div className="rounded bg-[var(--color-surface-subtle)] p-2 border border-[var(--color-border)]">
                        <span className="text-[var(--color-text-muted)] block text-[10px] uppercase font-bold">
                          Weight
                        </span>
                        <span className="font-metric text-sm font-bold text-[var(--color-text-primary)]">
                          {latestMetric.weight} kg
                        </span>
                      </div>
                    )}
                    {latestMetric.hydrationStatus && (
                      <div className="col-span-2 text-xs">
                        <span className="text-[var(--color-text-muted)]">Hydration: </span>
                        <span className="font-semibold">{latestMetric.hydrationStatus}</span>
                      </div>
                    )}
                    {latestMetric.notes && (
                      <div className="col-span-2 text-xs text-[var(--color-text-secondary)] italic">
                        &quot;{latestMetric.notes}&quot;
                      </div>
                    )}
                  </div>
                ) : (
                  <p className="text-xs text-[var(--color-text-muted)] pt-3">No telemetry recorded yet.</p>
                )}
              </Panel>
            </div>

            {/* 2-Column Clinical Workspace: Medical Records History (Left 7/12) & Injuries/Preventive (Right 5/12) */}
            <div className="grid grid-cols-1 gap-6 lg:grid-cols-12 items-start">
              {/* Left: Health Records History */}
              <div className="lg:col-span-7 space-y-4">
                <Panel padded className="space-y-3 border border-[var(--color-border)]">
                  <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-2.5">
                    <div className="flex items-center gap-2">
                      <Icon name="clipboard" size={16} className="text-[var(--color-primary)]" />
                      <h2 className="text-sm font-bold text-[var(--color-text-primary)]">
                        Clinical Examination History
                      </h2>
                    </div>
                    <span className="text-xs text-[var(--color-text-muted)]">
                      {sortedRecords.length} record{sortedRecords.length === 1 ? '' : 's'}
                    </span>
                  </div>

                  {detailLoading ? (
                    <ListSkeleton rows={3} />
                  ) : sortedRecords.length === 0 ? (
                    <p className="text-xs text-[var(--color-text-muted)] py-4 text-center">
                      No health records on file for this horse.
                    </p>
                  ) : (
                    <div className="space-y-3">
                      {sortedRecords.map((rec) => (
                        <div
                          key={rec.id}
                          className="rounded-md border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3.5 space-y-2 transition-all duration-150 hover:border-[var(--color-border-strong)]"
                        >
                          <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[var(--color-border)] pb-2">
                            <div className="font-semibold text-xs text-[var(--color-text-primary)] flex items-center gap-1.5">
                              <Icon name="calendar" size={12} className="text-[var(--color-text-muted)]" />
                              <span>{formatDate(rec.examinedAt)}</span>
                              <span className="text-[var(--color-text-muted)] font-normal">
                                · {rec.recordType}
                              </span>
                            </div>
                            <div className="flex items-center gap-2">
                              {rec.trainingDecision && (
                                <Pill
                                  tone={rec.trainingDecision === 'ALLOWED' ? 'success' : 'danger'}
                                  size="sm"
                                >
                                  {rec.trainingDecision}
                                </Pill>
                              )}
                              <Button
                                variant="secondary"
                                size="sm"
                                icon="file-text"
                                onClick={() =>
                                  router.push(
                                    `/veterinarian/records?recordId=${rec.id}&horseId=${rec.horseId}${
                                      rec.careScheduleId != null
                                        ? `&scheduleId=${rec.careScheduleId}`
                                        : ''
                                    }${selectedHorse ? `&horseName=${encodeURIComponent(selectedHorse.name)}` : ''}`
                                  )
                                }
                                className="text-[11px] h-7 px-2 transition-all active:scale-[0.98]"
                              >
                                View Record
                              </Button>
                              {/* AC-3.4: Target Clinical Record Treatment Plan Action */}
                              {rec.recordType !== 'VACCINATION' && (
                                <Button
                                  variant="secondary"
                                  size="sm"
                                  icon="plus"
                                  onClick={() => setPlanTargetRecord(rec)}
                                  className="text-[11px] h-7 px-2 transition-all active:scale-[0.98]"
                                >
                                  Plan Treatment
                                </Button>
                              )}
                            </div>
                          </div>

                          {rec.diagnosis && (
                            <div className="text-xs">
                              <span className="font-bold text-[var(--color-text-primary)]">Diagnosis: </span>
                              <span className="text-[var(--color-text-secondary)]">{rec.diagnosis}</span>
                            </div>
                          )}
                          {rec.findings && (
                            <div className="text-xs">
                              <span className="font-semibold text-[var(--color-text-muted)]">Findings: </span>
                              <span className="text-[var(--color-text-secondary)]">{rec.findings}</span>
                            </div>
                          )}
                          {rec.treatment && (
                            <div className="text-xs">
                              <span className="font-semibold text-[var(--color-text-muted)]">Treatment: </span>
                              <span className="text-[var(--color-text-secondary)]">{rec.treatment}</span>
                            </div>
                          )}
                          {rec.restrictionDetails && (
                            <div className="text-xs text-[var(--color-danger)] font-medium">
                              <strong>Restrictions:</strong> {rec.restrictionDetails}
                            </div>
                          )}

                          {/* AC-3.4: Associated Treatment Plans & Prescriptions */}
                          {(() => {
                            const plans = plansByRecordId[rec.id] || [];
                            const isExpanded = expandedRecordPlans.has(rec.id);
                            const toggleExpand = () => {
                              setExpandedRecordPlans((prev) => {
                                const next = new Set(prev);
                                if (next.has(rec.id)) next.delete(rec.id);
                                else next.add(rec.id);
                                return next;
                              });
                            };

                            if (plans.length === 0) return null;

                            return (
                              <div className="mt-3 pt-2.5 border-t border-[var(--color-border)]">
                                {/* eslint-disable-next-line no-restricted-syntax */}
                                <button
                                  type="button"
                                  onClick={toggleExpand}
                                  className="flex items-center justify-between w-full text-left text-xs font-semibold text-[var(--color-text-primary)] hover:text-[var(--color-primary)] transition-colors py-1"
                                >
                                  <span className="flex items-center gap-1.5">
                                    <Icon
                                      name={isExpanded ? 'chevron-down' : 'chevron-right'}
                                      size={12}
                                      className="text-[var(--color-text-muted)]"
                                    />
                                    <span>Associated Treatment Plans ({plans.length})</span>
                                  </span>
                                  <span className="text-[11px] text-[var(--color-text-muted)] font-normal">
                                    {isExpanded ? 'Hide' : 'Show details'}
                                  </span>
                                </button>

                                {isExpanded && (
                                  <div className="space-y-2 mt-2">
                                    {plans.map((plan) => {
                                      const planTone =
                                        plan.status === 'ACTIVE'
                                          ? 'success'
                                          : plan.status === 'COMPLETED'
                                          ? 'neutral'
                                          : 'danger';

                                      return (
                                        <div
                                          key={plan.id}
                                          className="rounded-md border border-[var(--color-border)] bg-[var(--color-surface)] p-2.5 space-y-2 text-xs"
                                        >
                                          <div className="flex items-center justify-between gap-2">
                                            <div className="font-semibold text-[var(--color-text-primary)]">
                                              {plan.treatmentName}
                                            </div>
                                            <Pill tone={planTone} size="sm">
                                              {plan.status}
                                            </Pill>
                                          </div>

                                          {plan.instructions && (
                                            <div className="text-[11px] text-[var(--color-text-secondary)]">
                                              <span className="font-medium text-[var(--color-text-primary)]">
                                                Instructions:{' '}
                                              </span>
                                              {plan.instructions}
                                            </div>
                                          )}

                                          {(plan.startDate || plan.endDate) && (
                                            <div className="text-[10px] text-[var(--color-text-muted)]">
                                              <span>{plan.startDate ? formatDate(plan.startDate) : 'Start'}</span>
                                              {' → '}
                                              <span>{plan.endDate ? formatDate(plan.endDate) : 'Ongoing'}</span>
                                            </div>
                                          )}

                                          {/* Prescriptions list for each plan */}
                                          {plan.prescriptions && plan.prescriptions.length > 0 && (
                                            <div className="mt-2 pt-2 border-t border-[var(--color-border)] space-y-1.5">
                                              <div className="text-[10px] font-bold uppercase tracking-wider text-[var(--color-text-muted)]">
                                                Prescriptions ({plan.prescriptions.length})
                                              </div>
                                              <div className="space-y-1.5">
                                                {plan.prescriptions.map((rx) => (
                                                  <div
                                                    key={rx.id}
                                                    className="rounded bg-[var(--color-surface-subtle)] border border-[var(--color-border)] p-2 text-[11px] space-y-1"
                                                  >
                                                    <div className="flex items-center justify-between font-medium text-[var(--color-text-primary)]">
                                                      <span>{rx.medicationName}</span>
                                                      <span className="text-[var(--color-text-muted)] font-mono text-[10px]">
                                                        {rx.dosage} · {rx.route}
                                                      </span>
                                                    </div>
                                                    <div className="text-[10px] text-[var(--color-text-secondary)] flex flex-wrap gap-x-2">
                                                      <span>Frequency: {rx.frequency}</span>
                                                      {(rx.startDate || rx.endDate) && (
                                                        <span>
                                                          ({rx.startDate ? formatDate(rx.startDate) : ''}
                                                          {rx.endDate ? ` – ${formatDate(rx.endDate)}` : ''})
                                                        </span>
                                                      )}
                                                    </div>
                                                    {rx.instructions && (
                                                      <div className="text-[10px] text-[var(--color-text-muted)] italic">
                                                        {rx.instructions}
                                                      </div>
                                                    )}
                                                  </div>
                                                ))}
                                              </div>
                                            </div>
                                          )}
                                        </div>
                                      );
                                    })}
                                  </div>
                                )}
                              </div>
                            );
                          })()}
                        </div>
                      ))}
                    </div>
                  )}
                </Panel>
              </div>

              {/* Right: Active Injuries & Preventive protocols */}
              <div className="lg:col-span-5 space-y-4">
                {/* Injuries Panel */}
                <Panel padded className="space-y-3 border border-[var(--color-border)]">
                  <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-2.5">
                    <div className="flex items-center gap-2">
                      <Icon name="alert-triangle" size={15} className="text-[var(--color-warning)]" />
                      <h2 className="text-sm font-bold text-[var(--color-text-primary)]">
                        Injuries & Conditions
                      </h2>
                    </div>
                    <Pill tone={injuries.length > 0 ? 'warning' : 'neutral'} size="sm">
                      {injuries.length}
                    </Pill>
                  </div>

                  {injuries.length === 0 ? (
                    <p className="text-xs text-[var(--color-text-muted)] py-3 text-center">
                      No active injury records.
                    </p>
                  ) : (
                    <div className="space-y-2">
                      {injuries.map((inj) => (
                        <div
                          key={inj.id}
                          className="flex items-start justify-between gap-3 rounded-md border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3"
                        >
                          <div className="min-w-0 flex-1">
                            <div className="font-semibold text-xs text-[var(--color-text-primary)]">
                              {inj.injuryType} — <span className="uppercase text-[11px]">{inj.severity}</span>
                            </div>
                            <div className="text-[11px] text-[var(--color-text-secondary)] mt-0.5">
                              Diagnosed {formatDate(inj.diagnosedAt)}
                              {inj.status ? ` · ${inj.status}` : ''}
                            </div>
                            {inj.description && (
                              <div className="text-xs text-[var(--color-text-muted)] mt-1 line-clamp-2">
                                {inj.description}
                              </div>
                            )}
                          </div>
                          <Button
                            variant="secondary"
                            size="sm"
                            onClick={() => handleOpenInjuryModal(inj)}
                            className="shrink-0 transition-all active:scale-[0.97]"
                          >
                            View / Correct
                          </Button>
                        </div>
                      ))}
                    </div>
                  )}
                </Panel>

                {/* Preventive Protocols */}
                <Panel padded className="space-y-3 border border-[var(--color-border)]">
                  <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-2.5">
                    <div className="flex items-center gap-2">
                      <Icon name="shield" size={15} className="text-[var(--color-medical)]" />
                      <h2 className="text-sm font-bold text-[var(--color-text-primary)]">
                        Preventive Immunization & Care
                      </h2>
                    </div>
                  </div>

                  {preventive && preventive.protocols.length > 0 ? (
                    <div className="space-y-2">
                      {preventive.protocols.map((p) => (
                        <div
                          key={p.protocolId}
                          className="flex items-center justify-between rounded-md border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-2.5"
                        >
                          <div className="min-w-0 flex-1 pr-2">
                            <div className="font-semibold text-xs text-[var(--color-text-primary)] truncate">
                              {p.name}{' '}
                              <span className="text-[var(--color-text-muted)] font-mono text-[10px]">
                                ({p.code})
                              </span>
                            </div>
                            <div className="text-[11px] text-[var(--color-text-secondary)] mt-0.5">
                              Due: <strong>{p.nextDueOn ? formatDate(p.nextDueOn) : 'Immediate'}</strong>
                            </div>
                          </div>
                          <Pill
                            tone={
                              p.status === 'OVERDUE'
                                ? 'danger'
                                : p.status === 'DUE_SOON'
                                ? 'warning'
                                : p.status === 'MISSING'
                                ? 'danger'
                                : 'success'
                            }
                            size="sm"
                          >
                            {p.status}
                          </Pill>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-xs text-[var(--color-text-muted)] py-3 text-center">
                      No preventive protocol coverage data.
                    </p>
                  )}
                </Panel>
              </div>
            </div>
          </div>
        </PageContainer>

        {selectedInjury && (
          <InjuryCorrectionModal
            open={modalOpen}
            onClose={() => setModalOpen(false)}
            horseId={selectedInjury.horseId}
            injury={selectedInjury}
            onSuccess={handleModalSuccess}
          />
        )}

        {planTargetRecord && selectedHorse && (
          <TreatmentPlanModal
            open={Boolean(planTargetRecord)}
            onClose={() => setPlanTargetRecord(null)}
            healthRecordId={planTargetRecord.id}
            horseName={selectedHorse.name}
            onSuccess={() => {
              const targetRecordId = planTargetRecord.id;
              setPlanTargetRecord(null);
              if (selectedHorseId != null) {
                void loadHorseDetail(selectedHorseId);
              }
              void careService.getTreatmentPlans(targetRecordId).then((plans) => {
                setPlansByRecordId((prev) => ({ ...prev, [targetRecordId]: plans || [] }));
                setExpandedRecordPlans((prev) => new Set(prev).add(targetRecordId));
              }).catch(() => {});
            }}
          />
        )}
      </RoleGuard>
    );
  }

  return (
    <RoleGuard allowedRoles={['VETERINARIAN']}>
      <PageContainer>
        <div className="space-y-6">
          <PageHeader
            eyebrow="Clinical Registry"
            title="Stable Health Overview"
            actions={
              <div className="flex items-center gap-2">
                <Button
                  variant="secondary"
                  icon="refresh"
                  onClick={refreshData}
                  disabled={loading}
                  className="transition-all active:scale-[0.98]"
                >
                  Refresh
                </Button>
                <Button
                  variant="secondary"
                  icon="clipboard"
                  onClick={() => router.push('/veterinarian/admissions')}
                  className="transition-all active:scale-[0.98]"
                >
                  Admission Queue
                </Button>
              </div>
            }
          />

          {error && <Notice tone="error">{error}</Notice>}

          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <MetricCard label="Cleared" value={loading ? '—' : stats.allowedCount} tone="success" />
            <MetricCard label="Blocked" value={loading ? '—' : stats.blockedCount} tone="danger" />
            <MetricCard
              label="Active Injuries"
              value={loading ? '—' : stats.openInjuriesCount}
              tone="warning"
            />
            <MetricCard label="Urgent Cases" value={loading ? '—' : stats.urgentCasesCount} tone="info" />
          </div>

          <Panel padded className="space-y-4">
            <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
              <div className="flex flex-1 flex-col gap-2 sm:flex-row sm:items-center">
                <div className="w-full sm:w-72">
                  <SearchInput
                    value={search}
                    onChange={(v) => {
                      setSearch(v);
                      setPage(0);
                    }}
                    placeholder="Search horse, ID, stall..."
                  />
                </div>
                <FilterChips
                  label="Status"
                  options={[
                    { value: 'ALL', label: 'All' },
                    { value: 'ALLOWED', label: 'Allowed' },
                    { value: 'BLOCKED', label: 'Blocked' },
                    { value: 'HAS_INJURY', label: 'Has injury' },
                    { value: 'URGENT', label: 'Urgent' },
                  ]}
                  value={statusFilter}
                  onChange={(v) => {
                    setStatusFilter(v as StatusFilter);
                    setPage(0);
                  }}
                />
              </div>
              <Tabs
                tabs={[
                  { id: 'ALL', label: 'All' },
                  { id: 'STABLE', label: 'In stable' },
                  { id: 'ELIGIBLE', label: 'Eligible' },
                ]}
                active={categoryFilter}
                onChange={(v) => {
                  setCategoryFilter(v as CategoryFilter);
                  setPage(0);
                }}
              />
            </div>

            {loading ? (
              <ListSkeleton rows={6} />
            ) : filteredItems.length === 0 ? (
              <EmptyState title="No horses found" description="Try adjusting filters or search." />
            ) : (
              <DataTable
                rows={pagedItems}
                columns={[
                  {
                    id: 'horse',
                    header: 'Horse',
                    render: (row: HorseListItem) => (
                      <div className="flex items-center gap-2.5">
                        <HorseAvatar name={row.horse.name} image={null} size={32} rounded="md" />
                        <div>
                          <div className="font-semibold text-sm text-[var(--color-text-primary)]">
                            {row.horse.name}
                          </div>
                          <div className="text-[11px] font-mono text-[var(--color-text-muted)]">
                            #{row.horse.id} {row.horse.breed ? `· ${row.horse.breed}` : ''}
                          </div>
                        </div>
                      </div>
                    ),
                  },
                  {
                    id: 'stall',
                    header: 'Stall',
                    render: (row: HorseListItem) => <span className="font-medium">{row.stallCode}</span>,
                  },
                  {
                    id: 'status',
                    header: 'Status',
                    render: (row: HorseListItem) => {
                      const d = row.summary?.trainingDecision ?? row.horse.trainingDecision ?? 'ALLOWED';
                      return (
                        <Pill tone={d === 'ALLOWED' ? 'success' : 'danger'} size="sm">
                          {d}
                        </Pill>
                      );
                    },
                  },
                  {
                    id: 'injury',
                    header: 'Injuries',
                    render: (row: HorseListItem) => {
                      const c = row.summary?.activeInjuryCount ?? 0;
                      return c > 0 ? (
                        <Pill tone="danger" size="sm">
                          {c} active
                        </Pill>
                      ) : (
                        <span className="text-[var(--color-text-muted)] text-xs">None</span>
                      );
                    },
                  },
                  {
                    id: 'action',
                    header: 'Action',
                    align: 'right',
                    render: (row: HorseListItem) => (
                      <Button
                        variant="secondary"
                        size="sm"
                        icon="chevron-right"
                        onClick={() => openHorse(row.horse.id)}
                        className="transition-all active:scale-[0.97]"
                      >
                        View record
                      </Button>
                    ),
                  },
                ]}
                getRowKey={(row: HorseListItem) => row.horse.id}
                pagination={{
                  page,
                  pageSize,
                  total: filteredItems.length,
                  onPageChange: setPage,
                }}
              />
            )}
          </Panel>
        </div>
      </PageContainer>
    </RoleGuard>
  );
}

export default function HealthRecordsPage() {
  return (
    <Suspense fallback={<ListSkeleton rows={6} />}>
      <HealthRecordsContent />
    </Suspense>
  );
}
