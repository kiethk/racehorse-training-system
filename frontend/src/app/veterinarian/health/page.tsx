'use client';

import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useRouter } from 'next/navigation';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { Panel } from '@/components/ui/Panel';
import { Button } from '@/components/ui/Button';
import { Pill } from '@/components/ui/StatusBadge';
import { DataTable } from '@/components/ui/DataTable';
import { FilterChips, Tabs, SearchInput, MetricCard, Notice, PageHeader } from '@/components/ui';
import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { Icon } from '@/components/ui/Icon';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { InjuryCorrectionModal } from '@/features/health/components/InjuryCorrectionModal';
import { healthApi } from '@/features/health/services/healthApi';
import { stableApi } from '@/features/stable/services/stableService';
import { formatDate } from '@/lib/display';
import type { InjuryRecord, HorseHealthSummary, HealthRecord } from '@/features/health/types';
import type { Horse, StableStall } from '@/features/stable/types';
import type { HorseHealthMetric } from '@/features/horses/types';
import type { HorsePreventiveStatus } from '@/features/care/types/preventive';

type StatusFilter = 'ALL' | 'ALLOWED' | 'BLOCKED' | 'HAS_INJURY' | 'URGENT';
type CategoryFilter = 'ALL' | 'STABLE' | 'ELIGIBLE';

interface HorseListItem {
  horse: Horse;
  stallCode: string;
  summary: HorseHealthSummary | null;
  activeInjuries: InjuryRecord[];
}

export default function HealthRecordsPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [horses, setHorses] = useState<Horse[]>([]);
  const [stalls, setStalls] = useState<StableStall[]>([]);
  const [summaries, setSummaries] = useState<Record<number, HorseHealthSummary>>({});
  const [injuriesByHorse, setInjuriesByHorse] = useState<Record<number, InjuryRecord[]>>({});
  const [reloadKey, setReloadKey] = useState(0);

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

        const targetHorses = horseList || [];
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
      setInjuries(inj || []);
      setMetrics(met || []);
      setHealthRecords(rec || []);
      setPreventive(prev);
    } finally {
      setDetailLoading(false);
    }
  }, []);

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
  }, []);

  const stallMap = useMemo(() => {
    const map = new Map<number, string>();
    for (const s of stalls) {
      map.set(s.id, s.stallCode);
    }
    return map;
  }, [stalls]);

  const listItems = useMemo<HorseListItem[]>(() => {
    return horses.map((h) => ({
      horse: h,
      stallCode: (h.currentStallId && stallMap.get(h.currentStallId)) || 'Unassigned',
      summary: summaries[h.id] ?? null,
      activeInjuries: (injuriesByHorse[h.id] || []).filter((i) => i.status === 'ACTIVE'),
    }));
  }, [horses, stallMap, summaries, injuriesByHorse]);

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
      if (statusFilter === 'HAS_INJURY' && item.activeInjuries.length === 0 && (item.summary?.activeInjuryCount ?? 0) === 0) return false;
      if (statusFilter === 'URGENT' && !item.summary?.hasPendingUrgent) return false;
      return true;
    });
  }, [listItems, search, categoryFilter, statusFilter]);

  const pagedItems = useMemo(() => {
    const start = page * pageSize;
    return filteredItems.slice(start, start + pageSize);
  }, [filteredItems, page]);

  const stats = useMemo(() => {
    let allowedCount = 0;
    let blockedCount = 0;
    let openInjuriesCount = 0;
    let urgentCasesCount = 0;

    for (const item of listItems) {
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
  }, [listItems]);

  const handleOpenInjuryModal = (injury: InjuryRecord) => {
    setSelectedInjury(injury);
    setModalOpen(true);
  };

  const handleModalSuccess = () => {
    setModalOpen(false);
    refreshData();
    if (selectedHorseId != null) loadHorseDetail(selectedHorseId);
  };

  const selectedHorse = useMemo(() => horses.find((h) => h.id === selectedHorseId) || null, [horses, selectedHorseId]);

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
        <AppShell>
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
                        <span>Stall: <strong>{(selectedHorse.currentStallId && stallMap.get(selectedHorse.currentStallId)) || 'Unassigned'}</strong></span>
                      </span>
                      <span>·</span>
                      <span>Status: <strong>{selectedHorse.currentStatus}</strong></span>
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
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-1 text-xs">
                    <div>
                      <span className="text-[var(--color-text-muted)] font-semibold uppercase tracking-wider text-[11px] block">
                        Last Examined
                      </span>
                      <span className="font-medium text-[var(--color-text-primary)]">
                        {healthSummary?.lastExaminedAt ? formatDate(healthSummary.lastExaminedAt) : 'No examination on record'}
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
                    <h2 className="text-sm font-bold text-[var(--color-text-primary)]">Latest Vitals Telemetry</h2>
                  </div>
                  {latestMetric ? (
                    <div className="grid grid-cols-2 gap-2.5 text-xs pt-1">
                      {latestMetric.heartRate != null && (
                        <div className="rounded bg-[var(--color-surface-subtle)] p-2 border border-[var(--color-border)]">
                          <span className="text-[var(--color-text-muted)] block text-[10px] uppercase font-bold">Heart Rate</span>
                          <span className="font-metric text-sm font-bold text-[var(--color-text-primary)]">{latestMetric.heartRate} bpm</span>
                        </div>
                      )}
                      {latestMetric.temperature != null && (
                        <div className="rounded bg-[var(--color-surface-subtle)] p-2 border border-[var(--color-border)]">
                          <span className="text-[var(--color-text-muted)] block text-[10px] uppercase font-bold">Temperature</span>
                          <span className="font-metric text-sm font-bold text-[var(--color-text-primary)]">{latestMetric.temperature} °C</span>
                        </div>
                      )}
                      {latestMetric.respiratoryRate != null && (
                        <div className="rounded bg-[var(--color-surface-subtle)] p-2 border border-[var(--color-border)]">
                          <span className="text-[var(--color-text-muted)] block text-[10px] uppercase font-bold">Respiration</span>
                          <span className="font-metric text-sm font-bold text-[var(--color-text-primary)]">{latestMetric.respiratoryRate} rpm</span>
                        </div>
                      )}
                      {latestMetric.weight != null && (
                        <div className="rounded bg-[var(--color-surface-subtle)] p-2 border border-[var(--color-border)]">
                          <span className="text-[var(--color-text-muted)] block text-[10px] uppercase font-bold">Weight</span>
                          <span className="font-metric text-sm font-bold text-[var(--color-text-primary)]">{latestMetric.weight} kg</span>
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
                          "{latestMetric.notes}"
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
                                <span className="text-[var(--color-text-muted)] font-normal">· {rec.recordType}</span>
                              </div>
                              {rec.trainingDecision && (
                                <Pill tone={rec.trainingDecision === 'ALLOWED' ? 'success' : 'danger'} size="sm">
                                  {rec.trainingDecision}
                                </Pill>
                              )}
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
                        <h2 className="text-sm font-bold text-[var(--color-text-primary)]">Injuries & Conditions</h2>
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
                                Diagnosed {formatDate(inj.diagnosedAt)}{inj.status ? ` · ${inj.status}` : ''}
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
                                {p.name} <span className="text-[var(--color-text-muted)] font-mono text-[10px]">({p.code})</span>
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
        </AppShell>
        {selectedInjury && (
          <InjuryCorrectionModal
            open={modalOpen}
            onClose={() => setModalOpen(false)}
            horseId={selectedInjury.horseId}
            injury={selectedInjury}
            onSuccess={handleModalSuccess}
          />
        )}
      </RoleGuard>
    );
  }

  return (
    <RoleGuard allowedRoles={['VETERINARIAN']}>
      <AppShell>
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
              <MetricCard label="Active Injuries" value={loading ? '—' : stats.openInjuriesCount} tone="warning" />
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
                    { id: 'stall', header: 'Stall', render: (row: HorseListItem) => <span className="font-medium">{row.stallCode}</span> },
                    {
                      id: 'status',
                      header: 'Status',
                      render: (row: HorseListItem) => {
                        const d = row.summary?.trainingDecision ?? row.horse.trainingDecision ?? 'ALLOWED';
                        return <Pill tone={d === 'ALLOWED' ? 'success' : 'danger'} size="sm">{d}</Pill>;
                      },
                    },
                    {
                      id: 'injury',
                      header: 'Injuries',
                      render: (row: HorseListItem) => {
                        const c = row.activeInjuries.length > 0 ? row.activeInjuries.length : (row.summary?.activeInjuryCount ?? 0);
                        return c > 0 ? (
                          <Pill tone="danger" size="sm">{c} active</Pill>
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
      </AppShell>
    </RoleGuard>
  );
}
