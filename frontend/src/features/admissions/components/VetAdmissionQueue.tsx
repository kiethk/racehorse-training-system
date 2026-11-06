'use client';

import { useEffect, useMemo, useState, useCallback, type ReactNode } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import { ApiError } from '@/services/api';
import { Button } from '@/components/ui/Button';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { Icon } from '@/components/ui/Icon';
import { Pill } from '@/components/ui/StatusBadge';
import { Tabs, type TabItem } from '@/components/ui/Tabs';
import { DetailSkeleton, EmptyState, ListSkeleton } from '@/components/ui/states';
import { FilterBar } from '@/components/ui/FilterBar';
import { Panel } from '@/components/ui/Panel';
import { Pagination } from '@/components/ui/Pagination';
import { FormField } from '@/components/ui/FormField';
import { SearchInput } from '@/components/ui/Input';
import { Notice } from '@/components/ui/Notice';
import { FilterChips } from '@/components/ui/SegmentedControl';
import { AdmissionListLayout } from '../shared/components/AdmissionListLayout';
import { AdmissionDetailLayout } from '../shared/components/AdmissionDetailLayout';
import { AdmissionDetailHeader } from '../shared/components/AdmissionDetailHeader';
import { AdmissionPipeline } from '../shared/components/AdmissionPipeline';
import { AdmissionSideCard, InfoRow } from '../shared/components/AdmissionInfoSection';
import { AdmissionStatusBadge } from '../shared/components/AdmissionStatusBadge';
import { AdmissionDocumentsSection } from '../shared/components/AdmissionDocumentsSection';
import { MedicalFindingsSection } from '../shared/components/MedicalFindingsSection';
import { CareRecordDetailModal } from '@/features/care/components/CareRecordDetailModal';
import { getCareTypeTone } from '@/features/schedule/utils/timelineUtils';
import { formatDate, formatDateTime } from '@/lib/display';
import { getScheduleExamAction, getScheduleStatusDisplay } from '@/features/care/lib/scheduleStatusDisplay';
import { admissionsApi } from '../services/api';
import type {
  AdmissionDetailResponse,
  AdmissionDocument,
  AdmissionStatus,
  CareSchedule,
  CareScheduleStatus,
  CareType,
  HorseHealthMetricResponse,
  VetAdmissionQueueItem,
  VetQueueSummary,
  VetReviewResponse,
} from '../types';
import { VetReviewForm } from './VetReviewForm';

type QueuePillFilter = 'ACTIVE' | 'IN_PROGRESS' | 'APPROVED';

function errorText(error: unknown) {
  if (error instanceof ApiError) {
    return `${error.status === 403 ? 'Permission denied' : error.status === 409 ? 'State conflict' : 'Request error'} (${error.status}${error.errorCode ? ` · ${error.errorCode}` : ''}): ${error.message}`;
  }
  return error instanceof Error ? error.message : 'Operation failed. Please try again.';
}

function formatLabel(value: string) {
  return value.replaceAll('_', ' ').toLowerCase().replace(/^./, (letter) => letter.toUpperCase());
}

function calculateAge(dateOfBirth: string | null | undefined): string {
  if (!dateOfBirth) return 'Age unavailable';
  const dob = new Date(dateOfBirth);
  if (Number.isNaN(dob.getTime())) return 'Age unavailable';
  const now = new Date();
  let years = now.getFullYear() - dob.getFullYear();
  const m = now.getMonth() - dob.getMonth();
  if (m < 0 || (m === 0 && now.getDate() < dob.getDate())) {
    years--;
  }
  if (years < 1) {
    const months = Math.max(0, (now.getFullYear() - dob.getFullYear()) * 12 + now.getMonth() - dob.getMonth());
    return `${months} mo`;
  }
  return `${years} yo`;
}

function getPriorityBadge(careType: CareType | undefined, purpose?: string | null) {
  const tone = getCareTypeTone(careType ?? 'ROUTINE', purpose);
  return { label: tone.label, tone: tone.pillTone, icon: tone.icon };
}

function isVetStageCompleted(status: AdmissionStatus | string | null | undefined): boolean {
  return status === 'TRAINER_REVIEW' || status === 'MANAGER_REVIEW' || status === 'APPROVED';
}

function MedicalNoteBlock({
  label,
  value,
}: {
  label: string;
  value: string | null | undefined;
}) {
  if (!value) return null;
  return (
    <div className="rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3">
      <span className="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">
        {label}
      </span>
      <p className="mt-1 whitespace-pre-wrap text-xs leading-relaxed text-[var(--color-text-primary)]">
        {value}
      </p>
    </div>
  );
}

export function VetAdmissionQueue() {
  const { user } = useAuth();
  const router = useRouter();
  const searchParams = useSearchParams();

  // Server-side queue state
  const [items, setItems] = useState<VetAdmissionQueueItem[]>([]);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [summary, setSummary] = useState<VetQueueSummary>({
    total: 0,
    awaiting: 0,
    inProgress: 0,
  });

  // Full-page examination workspace & selection state
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [targetScheduleId, setTargetScheduleId] = useState<number | null>(null);
  const [deepLinkedSchedule, setDeepLinkedSchedule] = useState<CareSchedule | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [isDrawerDirty, setIsDrawerDirty] = useState(false);
  const [showCloseConfirm, setShowCloseConfirm] = useState(false);

  // Detail data for opened admission
  const [detail, setDetail] = useState<AdmissionDetailResponse | null>(null);
  const [documents, setDocuments] = useState<AdmissionDocument[]>([]);
  const [metrics, setMetrics] = useState<HorseHealthMetricResponse[]>([]);
  const [careSchedules, setCareSchedules] = useState<CareSchedule[]>([]);
  const [stallCodes, setStallCodes] = useState<Record<number, string>>({});
  const [activeTab, setActiveTab] = useState<string>('exam');

  // Filters state
  const [searchQuery, setSearchQuery] = useState('');
  const [queuePillFilter, setQueuePillFilter] = useState<QueuePillFilter>('ACTIVE');

  // Request & operation state
  const [loading, setLoading] = useState(true);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [startingExam, setStartingExam] = useState(false);
  const [error, setError] = useState('');
  const [detailError, setDetailError] = useState('');
  const [success, setSuccess] = useState('');
  const [scheduleWarning, setScheduleWarning] = useState('');
  const [reload, setReload] = useState(0);

  // Deep link support via ?id=, ?scheduleId=, or ?tab= query param
  useEffect(() => {
    const tabParam = searchParams.get('tab');
    if (tabParam === 'ACTIVE' || tabParam === 'IN_PROGRESS' || tabParam === 'APPROVED') {
      Promise.resolve().then(() => {
        setQueuePillFilter(tabParam);
        setPage(0);
      });
    }
    const idParam = searchParams.get('id');
    const scheduleIdParam = searchParams.get('scheduleId');
    if (scheduleIdParam) {
      const parsedScheduleId = Number(scheduleIdParam);
      if (!Number.isNaN(parsedScheduleId) && parsedScheduleId > 0) {
        let active = true;
        Promise.resolve().then(() => {
          if (!active) return;
          setTargetScheduleId(parsedScheduleId);
          setDeepLinkedSchedule(null);
          setSelectedId(null);
          setDetail(null);
          setDrawerOpen(true);
          setLoadingDetail(true);
          setDetailError('');
        });
        admissionsApi.getCareScheduleDetail(parsedScheduleId)
          .then(({ schedule }) => {
            if (!active) return;
            if (!schedule.admissionId) {
              setDetailError('This care schedule is not linked to an Admission record.');
              return;
            }
            setDeepLinkedSchedule(schedule);
            setSelectedId(schedule.admissionId);
            setActiveTab('exam');
          })
          .catch((cause) => {
            if (active) setDetailError(errorText(cause));
          })
          .finally(() => {
            if (active) setLoadingDetail(false);
          });
        return () => {
          active = false;
        };
      }
    } else if (idParam) {
      const parsed = Number(idParam);
      if (!Number.isNaN(parsed) && parsed > 0) {
        Promise.resolve().then(() => {
          setTargetScheduleId(null);
          setDeepLinkedSchedule(null);
          setSelectedId(parsed);
          setActiveTab('exam');
          setDrawerOpen(true);
        });
      }
    }
  }, [searchParams]);


  // Synchronized queue and pill count loading
  useEffect(() => {
    let active = true;
    Promise.resolve().then(() => {
      if (!active) return;
      setLoading(true);
      setError('');
    });

    const trimmedSearch = searchQuery.trim() || undefined;

    Promise.all([
      admissionsApi.getVetQueue({
        search: trimmedSearch,
        page: 0,
        size: 100,
      }),
      admissionsApi.getVetQueue({
        search: trimmedSearch,
        scheduleStatus: 'COMPLETED',
        page: 0,
        size: 100,
      }),
    ])
      .then(([fullPage, completedPage]) => {
        if (!active) return;

        const isRowVetApproved = (r: VetAdmissionQueueItem) =>
          isVetStageCompleted(r.admissionStatus) || r.careSchedule?.status === 'COMPLETED';
        const activeRows = fullPage.content.filter(
          (r) => !isRowVetApproved(r) && r.admissionStatus !== 'REJECTED'
        );

        const approvedByAdmission = new Map<number, VetAdmissionQueueItem>();
        const combinedCompleted = [
          ...(completedPage?.content ?? []),
          ...fullPage.content.filter(isRowVetApproved),
        ].filter((r) => r.admissionStatus !== 'REJECTED');
        for (const row of combinedCompleted) {
          const existing = approvedByAdmission.get(row.admissionId);
          if (
            !existing ||
            (row.careSchedule?.careType === 'INITIAL' &&
              existing.careSchedule?.careType !== 'INITIAL')
          ) {
            approvedByAdmission.set(row.admissionId, row);
          }
        }
        const approvedRows = Array.from(approvedByAdmission.values()).sort((a, b) => {
          const aPendingStage =
            a.admissionStatus === 'TRAINER_REVIEW' || a.admissionStatus === 'MANAGER_REVIEW' ? 0 : 1;
          const bPendingStage =
            b.admissionStatus === 'TRAINER_REVIEW' || b.admissionStatus === 'MANAGER_REVIEW' ? 0 : 1;
          if (aPendingStage !== bPendingStage) return aPendingStage - bPendingStage;
          return (b.careSchedule?.id ?? b.admissionId) - (a.careSchedule?.id ?? a.admissionId);
        });

        const awaitingCount = activeRows.filter(
          (r) =>
            r.careSchedule?.status === 'REQUESTED' ||
            r.careSchedule?.status === 'SCHEDULED' ||
            r.careSchedule?.status === 'OVERDUE'
        ).length;
        const inProgressRows = activeRows.filter(
          (r) =>
            r.careSchedule?.status === 'IN_PROGRESS' ||
            r.careSchedule?.status === 'DELAYED' ||
            r.careSchedule?.status === 'PAUSED'
        );

        setSummary({
          total: activeRows.length + approvedRows.length,
          active: activeRows.length,
          awaiting: awaitingCount,
          inProgress: inProgressRows.length,
          approved: approvedRows.length,
        });

        let filtered = activeRows;
        if (queuePillFilter === 'APPROVED') {
          filtered = approvedRows;
        } else if (queuePillFilter === 'ACTIVE') {
          filtered = activeRows;
        } else if (queuePillFilter === 'IN_PROGRESS') {
          filtered = inProgressRows;
        }

        setTotalElements(filtered.length);
        const startIndex = page * pageSize;
        setItems(filtered.slice(startIndex, startIndex + pageSize));
      })
      .catch((cause) => {
        if (active) setError(errorText(cause));
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [
    searchQuery,
    queuePillFilter,
    page,
    pageSize,
    reload,
  ]);

  const displayedItems = items;

  const effectiveTotal = useMemo(() => {
    if (queuePillFilter === 'ACTIVE' && summary.active != null) return summary.active;
    if (queuePillFilter === 'IN_PROGRESS' && summary.inProgress != null) return summary.inProgress;
    if (queuePillFilter === 'APPROVED' && summary.approved != null) return summary.approved;
    return totalElements;
  }, [queuePillFilter, summary, totalElements]);

  const hasActiveFilters = searchQuery !== '' || queuePillFilter !== 'ACTIVE';

  const resetFilters = () => {
    setSearchQuery('');
    setQueuePillFilter('ACTIVE');
    setPage(0);
  };

  // Fetch admission details via vet-enforced endpoint when selected
  useEffect(() => {
    if (selectedId === null) return;
    let active = true;

    Promise.resolve().then(() => {
      if (active) {
        setLoadingDetail(true);
        setDetailError('');
        setDetail(null);
        setDocuments([]);
        setMetrics([]);
        setCareSchedules([]);
      }
    });

    Promise.all([
      admissionsApi.getVetAdmissionDetail(selectedId),
      admissionsApi.getDocuments(selectedId),
      admissionsApi.getCareSchedules({ admissionId: selectedId, size: 50 }),
    ])
      .then(async ([detailData, docs, schedulesPage]) => {
        if (!active) return;
        setDetail(detailData);
        setDocuments(docs);
        setCareSchedules(schedulesPage.content);
        if (detailData.quarantineStallCode) {
          setStallCodes((prev) => ({
            ...prev,
            [detailData.admissionId]: detailData.quarantineStallCode!,
          }));
        }
        if (detailData.horseId) {
          try {
            const metricData = await admissionsApi.getHorseHealthMetrics(detailData.horseId);
            if (active) setMetrics(metricData);
          } catch {
            if (active) setMetrics([]);
          }
        }
      })
      .catch((cause) => {
        if (active) setDetailError(errorText(cause));
      })
      .finally(() => {
        if (active) setLoadingDetail(false);
      });

    return () => {
      active = false;
    };
  }, [selectedId, reload]);

  const selectedRow = items.find((row) =>
    targetScheduleId
      ? row.careSchedule?.id === targetScheduleId
      : row.admissionId === selectedId,
  ) ?? null;

  const detailScheduleFallback: CareSchedule | null = detail?.initialExamSchedule
    ? {
        id: detail.initialExamSchedule.scheduleId,
        horseId: detail.horseId ?? 0,
        admissionId: detail.admissionId,
        careType: (detail.initialExamSchedule.careType as CareType) || 'INITIAL',
        status: (detail.initialExamSchedule.status as CareScheduleStatus) || 'REQUESTED',
        veterinarianId: detail.initialExamSchedule.veterinarianId,
        assignedVetId: detail.initialExamSchedule.veterinarianId,
        scheduledAt: detail.initialExamSchedule.scheduledAt,
        scheduledDate: detail.initialExamSchedule.scheduledDate,
      }
    : null;

  const isVetStageDone =
    isVetStageCompleted(detail?.status) || isVetStageCompleted(selectedRow?.admissionStatus);
  const initialCareSchedule =
    careSchedules.find((schedule) => schedule.careType === 'INITIAL') ?? detailScheduleFallback;

  const explicitlySelectedSchedule = targetScheduleId
    ? careSchedules.find((schedule) => schedule.id === targetScheduleId) ??
      (selectedRow?.careSchedule?.id === targetScheduleId ? selectedRow.careSchedule : null) ??
      (deepLinkedSchedule?.id === targetScheduleId ? deepLinkedSchedule : null)
    : null;
  const assignedActiveSchedule = careSchedules
    .filter((schedule) =>
      ['SCHEDULED', 'IN_PROGRESS', 'DELAYED', 'OVERDUE', 'PAUSED'].includes(schedule.status) &&
      (schedule.veterinarianId ?? schedule.assignedVetId) === user?.userId,
    )
    .sort((a, b) => {
      if (a.status !== b.status) return a.status === 'IN_PROGRESS' || a.status === 'DELAYED' ? -1 : 1;
      return new Date(a.scheduledAt ?? a.scheduledDate ?? 0).getTime()
        - new Date(b.scheduledAt ?? b.scheduledDate ?? 0).getTime();
    })[0] ?? null;
  const currentActiveSchedule = isVetStageDone
    ? initialCareSchedule ?? explicitlySelectedSchedule ?? selectedRow?.careSchedule ?? null
    : explicitlySelectedSchedule ??
      assignedActiveSchedule ??
      selectedRow?.careSchedule ??
      detailScheduleFallback;
  const currentScheduleRecord = detail?.healthRecords?.find(
    (record) => record.careScheduleId === currentActiveSchedule?.id,
  );
  const displayedTrainingDecision =
    currentScheduleRecord?.trainingDecision ??
    (currentActiveSchedule?.careType === 'INITIAL' ? detail?.vetTrainingDecision : null) ??
    detail?.vetTrainingDecision;

  const assignedVetId = currentActiveSchedule?.veterinarianId ?? currentActiveSchedule?.assignedVetId ?? null;
  const assignedElsewhere = Boolean(assignedVetId && assignedVetId !== user?.userId);

  const isApprovedAdmission = detail?.status === 'APPROVED' || selectedRow?.admissionStatus === 'APPROVED';
  const isCompletedExam =
    isVetStageDone ||
    isApprovedAdmission ||
    currentActiveSchedule?.status === 'COMPLETED' ||
    Boolean(detail?.initialVetExam && detail?.status !== 'VET_REVIEW');

  // Requirement: Disable Start for REQUESTED schedules; allow starting when SCHEDULED, OVERDUE, or PAUSED
  // If admission has already passed VET_REVIEW (TRAINER_REVIEW / MANAGER_REVIEW / APPROVED) or the exam is COMPLETED,
  // examining is disabled (read-only record viewing only).
  const canStartExam = Boolean(
    !isVetStageDone &&
    !isApprovedAdmission &&
    currentActiveSchedule &&
      ['SCHEDULED', 'OVERDUE', 'PAUSED'].includes(currentActiveSchedule.status) &&
      assignedVetId === user?.userId,
  );

  const canCompleteExam = Boolean(
    !isVetStageDone &&
    !isApprovedAdmission &&
    currentActiveSchedule &&
      ['SCHEDULED', 'IN_PROGRESS', 'DELAYED', 'OVERDUE', 'PAUSED'].includes(currentActiveSchedule.status) &&
      assignedVetId === user?.userId,
  );

  const latestMetrics = metrics[0] ?? null;

  const selectedHistory = useMemo(() => {
    const list: Array<{
      id: number;
      label: string;
      status: CareScheduleStatus;
      description: string;
      scheduledAt: string | null;
      assignedVetId: number | null;
      createdAt?: string;
    }> = [];

    careSchedules
      .filter((cs) => cs.admissionId === selectedId)
      .forEach((cs) => {
        list.push({
          id: cs.id,
          label: `${formatLabel(cs.careType)} Care Schedule #${cs.id}`,
          status: cs.status,
          description: cs.description || `${formatLabel(cs.careType)} care schedule`,
          scheduledAt: cs.scheduledAt || cs.scheduledDate || null,
          assignedVetId: cs.veterinarianId ?? cs.assignedVetId ?? null,
          createdAt: cs.createdAt,
        });
      });

    return list.sort((a, b) => new Date(a.createdAt || 0).getTime() - new Date(b.createdAt || 0).getTime());
  }, [careSchedules, selectedId, detail]);

  const handleOpenAdmission = (id: number, schedule: CareSchedule | null, _forceViewRecord = false) => {
    setTargetScheduleId(schedule?.id ?? null);
    setDeepLinkedSchedule(null);
    setSelectedId(id);
    setActiveTab('exam');
    setScheduleWarning('');
    setIsDrawerDirty(false);
    setDrawerOpen(true);
  };

  const handleDirtyChange = useCallback((dirty: boolean) => {
    setIsDrawerDirty(dirty);
  }, []);

  const handleRequestCloseDrawer = useCallback(() => {
    if (isDrawerDirty) {
      setShowCloseConfirm(true);
    } else {
      setDrawerOpen(false);
      setSelectedId(null);
      setTargetScheduleId(null);
      setDeepLinkedSchedule(null);
      router.replace('/veterinarian/admissions', { scroll: false });
    }
  }, [isDrawerDirty, router]);

  // Keep the full-page examination workspace aligned with the Trainer detail flow.
  useEffect(() => {
    if (!drawerOpen) return;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, [drawerOpen]);

  function reloadQueue() {
    setLoading(true);
    setReload((curr) => curr + 1);
  }

  function handleRefresh() {
    setSuccess('');
    setError('');
    setScheduleWarning('');
    reloadQueue();
  }

  async function handleStartExam() {
    if (!currentActiveSchedule || startingExam || !canStartExam) return;
    try {
      setStartingExam(true);
      setError('');
      await admissionsApi.startCareSchedule(currentActiveSchedule.id);
      setSuccess(`Examination #${currentActiveSchedule.id} started. You can now record clinical findings.`);
      setReload((curr) => curr + 1);
    } catch (cause) {
      setError(errorText(cause));
    } finally {
      setStartingExam(false);
    }
  }

  function handleReviewSuccess(result: VetReviewResponse | CareSchedule, completionKind: 'INITIAL' | 'CARE_SCHEDULE') {
    if (completionKind === 'INITIAL' && 'admissionId' in result) {
      setSuccess(`Admission #${result.admissionId} (${detail?.candidate?.name ?? selectedRow?.candidateName ?? 'Horse'}): clinical examination completed and forwarded to Trainer assessment.`);
    } else {
      setSuccess(`Care schedule #${currentActiveSchedule?.id ?? '—'} (${detail?.candidate?.name ?? selectedRow?.candidateName ?? 'Horse'}): examination completed without changing the Admission workflow.`);
    }
    setScheduleWarning('');
    setIsDrawerDirty(false);
    setDrawerOpen(false);
    setQueuePillFilter('APPROVED');
    setPage(0);
    reloadQueue();
  }

  const workspaceTabs: TabItem[] = [
    {
      id: 'documents',
      label: 'Documents',
      icon: 'file-text',
      count: documents.length > 0 ? documents.length : undefined,
    },
    { id: 'exam', label: 'Clinical Examination', icon: 'stethoscope' },
  ];

  // Opening a row must do exactly what that row's action button does.
  const handleRowAction = (row: (typeof items)[number]) => {
    const isVetApproved =
      isVetStageCompleted(row.admissionStatus) ||
      row.careSchedule?.status === 'COMPLETED' ||
      queuePillFilter === 'APPROVED';
    if (isVetApproved) {
      const targetSchedule =
        row.careSchedule?.careType === 'INITIAL' ? row.careSchedule : null;
      handleOpenAdmission(row.admissionId, targetSchedule, true);
      return;
    }
    // Examine = go straight to the examination workspace (it auto-starts the
    // SCHEDULED/OVERDUE exam). Continue/View results/Details keep opening the drawer.
    if (
      (row.careSchedule?.status === 'SCHEDULED' || row.careSchedule?.status === 'OVERDUE') &&
      row.careSchedule.id
    ) {
      router.push(`/veterinarian/schedule/${row.careSchedule.id}?back=admissions`);
      return;
    }
    handleOpenAdmission(row.admissionId, row.careSchedule);
  };



  const examDetailsCard = detail ? (
    <AdmissionSideCard title="Examination details">
      <InfoRow label="Priority" value={selectedRow ? getPriorityBadge(selectedRow.careSchedule?.careType, selectedRow.careSchedule?.purpose).label : 'Standard'} />
      <InfoRow label="Schedule" value={currentActiveSchedule ? getScheduleStatusDisplay(currentActiveSchedule.status).label : 'Not scheduled'} />
      <InfoRow label="Quarantine stall" value={detail.quarantineStallCode || 'Not assigned'} />
      <InfoRow label="Trainer" value={detail.trainerName || (detail.trainerId ? `Trainer #${detail.trainerId}` : 'Pending assignment')} />
      {(currentActiveSchedule?.scheduledDate || currentActiveSchedule?.scheduledAt) && (
        <InfoRow label="Scheduled" value={formatDate((currentActiveSchedule.scheduledDate || currentActiveSchedule.scheduledAt)!)} />
      )}
    </AdmissionSideCard>
  ) : null;

  return (
    <QueueShell showHeader={!drawerOpen}>
      {success && <Notice tone="success" onDismiss={() => setSuccess('')}>{success}</Notice>}
      {error && <Notice tone="error" onDismiss={() => setError('')}>{error}</Notice>}
      {scheduleWarning && <Notice tone="warning" onDismiss={() => setScheduleWarning('')}>{scheduleWarning}</Notice>}

      {!drawerOpen && (
        <>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <FilterChips
          label="Queue"
          value={queuePillFilter}
          onChange={(value) => {
            setQueuePillFilter(value as QueuePillFilter);
            setPage(0);
          }}
          options={[
            { value: 'ACTIVE', label: 'Active', count: summary.active ?? summary.total },
            { value: 'APPROVED', label: 'Approved', count: summary.approved ?? 0 },
          ]}
        />
        <Button type="button" onClick={handleRefresh} disabled={loading} icon="refresh" variant="secondary" size="sm" className="transition-all active:scale-[0.98]">
          Refresh
        </Button>
      </div>

      <FilterBar className="[&_input]:min-h-9" onSubmit={(event) => event.preventDefault()}>
        <FormField label="Search admissions" className="min-w-[14rem] flex-1">
          <SearchInput
            label="Search admissions"
            placeholder="Horse name, admission number, or breed"
            value={searchQuery}
            onChange={(value) => {
              setSearchQuery(value);
              setPage(0);
            }}
          />
        </FormField>
        <Button type="button" variant="secondary" onClick={resetFilters} disabled={!hasActiveFilters}>
          Clear
        </Button>
      </FilterBar>

      {loading ? (
        <Panel padded>
          <ListSkeleton rows={4} />
        </Panel>
      ) : displayedItems.length === 0 ? (
        <Panel padded>
          <EmptyState
            icon="clipboard"
            title="No matching admissions found"
            description="No candidates match your current search query or active filter settings."
            action={<Button size="sm" variant="secondary" onClick={resetFilters}>Clear filters</Button>}
          />
        </Panel>
      ) : (
        <div className="space-y-4">
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            {displayedItems.map((row) => {
              const isApproved = row.admissionStatus === 'APPROVED';
              const isVetApproved =
                isVetStageCompleted(row.admissionStatus) ||
                row.careSchedule?.status === 'COMPLETED' ||
                queuePillFilter === 'APPROVED';
              const stallCode =
                stallCodes[row.admissionId] ||
                row.quarantineStallCode ||
                (row.quarantineStallId ? `Q-Stall #${row.quarantineStallId}` : 'Quarantine');
              const status: CareScheduleStatus | undefined = isVetApproved
                ? 'COMPLETED'
                : row.careSchedule?.status;
              const examAction = getScheduleExamAction(status);
              const action = isVetApproved
                ? ({ label: 'View Record', icon: 'file-text' as const, primary: false })
                : ({ label: examAction.label, icon: examAction.icon, primary: examAction.label === 'Examine' });

              const rowVetId = row.careSchedule?.veterinarianId ?? row.careSchedule?.assignedVetId ?? null;
              const isAssignedToMe = Boolean(rowVetId && user?.userId && rowVetId === user.userId);

              return (
                <Panel
                  key={row.careSchedule?.id ? `schedule-${row.careSchedule.id}` : `admission-${row.admissionId}`}
                  padded
                  onClick={() => handleRowAction(row)}
                  className="cursor-pointer border border-[var(--color-border)] transition-all duration-200 ease-out hover:-translate-y-0.5 hover:shadow-md hover:border-[var(--color-border-strong)]"
                >
                  <div className="flex items-center justify-between gap-3">
                    {/* Left: Avatar + Details */}
                    <div className="flex items-center gap-3 min-w-0 flex-1">
                      <HorseAvatar name={row.candidateName} image={row.imageUrl} size={42} rounded="md" />
                      <div className="min-w-0 flex-1">
                        <div className="text-base font-bold text-[var(--color-text-primary)] truncate">
                          {row.candidateName}
                        </div>
                        {((status && status !== 'SCHEDULED' && status !== 'IN_PROGRESS') ||
                          Boolean((row.careSchedule?.rolloverCount ?? 0) > 0) ||
                          row.careSchedule?.rescheduleReason === 'PEER_OFFLOAD') && (
                          <div className="mt-1 flex flex-wrap items-center gap-1.5">
                            {status && status !== 'SCHEDULED' && status !== 'IN_PROGRESS' && (
                              <Pill
                                tone={getScheduleStatusDisplay(status).tone}
                                size="sm"
                                icon={getScheduleStatusDisplay(status).icon}
                              >
                                {getScheduleStatusDisplay(status).label}
                              </Pill>
                            )}
                            {Boolean((row.careSchedule?.rolloverCount ?? 0) > 0) && (
                              <Pill tone="warning" size="sm">
                                Rolled {row.careSchedule?.rolloverCount}x
                              </Pill>
                            )}
                            {row.careSchedule?.rescheduleReason === 'PEER_OFFLOAD' && (
                              <Pill tone="neutral" size="sm">
                                Transferred
                              </Pill>
                            )}
                          </div>
                        )}
                        <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-[var(--color-text-secondary)]">
                          <span className="inline-flex items-center gap-1 shrink-0">
                            <Icon name="building" size={12} />
                            <span>{isApproved ? 'Stall' : 'Quarantine Stall'} <strong>{stallCode}</strong></span>
                          </span>
                          <span>·</span>
                          <span className="inline-flex items-center gap-1 shrink-0">
                            <Icon name="calendar" size={12} className="text-[var(--color-primary)]" />
                            {row.careSchedule?.scheduledDate || row.careSchedule?.scheduledAt ? (
                              <strong>{formatDate((row.careSchedule.scheduledDate || row.careSchedule.scheduledAt)!)}</strong>
                            ) : (
                              <span className="italic text-[var(--color-text-muted)]">Awaiting slot</span>
                            )}
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* Right: Action Button */}
                    <div className="shrink-0">
                      <Button
                        size="sm"
                        variant={action.primary ? 'primary' : 'secondary'}
                        icon={action.icon}
                        className="min-w-[6.5rem] transition-all active:scale-[0.97]"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleRowAction(row);
                        }}
                      >
                        {action.label}
                      </Button>
                    </div>
                  </div>
                </Panel>
              );
            })}
          </div>

          <Panel className="overflow-hidden">
            <Pagination
              page={page}
              pageSize={pageSize}
              total={effectiveTotal}
              onPageChange={setPage}
              disabled={loading}
              className="border-t-0"
            />
          </Panel>
        </div>
      )}
        </>
      )}

      {/* Full-page clinical examination workspace */}
      {drawerOpen && (selectedId !== null || targetScheduleId !== null) && (
        <AdmissionDetailLayout
          onBack={handleRequestCloseDrawer}
          header={detail ? (
            <AdmissionDetailHeader
              detail={detail}
              horsePhotoUrl={detail.documents.find((doc) => doc.documentType === 'HORSE_PHOTO')
                ? admissionsApi.assetUrl(detail.documents.find((doc) => doc.documentType === 'HORSE_PHOTO')!.fileUrl)
                : undefined}
            />
          ) : (
            <div className="border-b border-[var(--color-border)] bg-[var(--color-surface)] p-4 text-sm font-semibold text-[var(--color-text-primary)]">
              {selectedRow?.candidateName ?? 'Loading admission'}
            </div>
          )}
          pipeline={detail ? <AdmissionPipeline detail={detail} /> : <DetailSkeleton />}
          content={(
            <div className="min-w-0 space-y-5">
              <Tabs tabs={workspaceTabs} active={activeTab} onChange={setActiveTab} />
              {loadingDetail || (!detail && !detailError) ? (
                <DetailSkeleton />
              ) : detailError ? (
                <EmptyState
                  icon="alert-triangle"
                  title="Could not load examination record"
                  description={detailError}
                  action={
                    selectedId === null
                      ? <Button onClick={handleRequestCloseDrawer}>Close</Button>
                      : <Button onClick={handleRefresh}>Retry</Button>
                  }
                />
              ) : detail ? (
                <div>
                  {/* Tab 1: Clinical Examination & VetReviewForm */}
                  {/* Tab 1: Clinical Examination & VetReviewForm (kept mounted to preserve state across tab switches) */}
                  <div className={activeTab === 'exam' ? 'block' : 'hidden'}>
                    {isCompletedExam ? (
                      <div className="space-y-5">
                        <div className="rounded-[var(--radius-md)] border border-[var(--color-success)] bg-[var(--color-success-soft)] p-5">
                          <div className="flex flex-wrap items-center justify-between gap-3">
                            <div className="flex items-center gap-2.5">
                              <Icon name="check" size={24} className="text-[var(--color-success)] shrink-0" />
                              <div>
                                <h3 className="text-base font-bold text-[var(--color-success)]">
                                  {isApprovedAdmission ? 'Admission Approved' : 'Clinical Examination Completed'}
                                </h3>
                                <p className="mt-0.5 text-sm text-[var(--color-text-secondary)]">
                                  {isApprovedAdmission
                                    ? 'This admission has been finalized and approved by management. Clinical examination records are locked for editing.'
                                    : 'The veterinary examination for this candidate has been completed and forwarded for Trainer and Manager review.'}
                                </p>
                              </div>
                            </div>
                            {(currentActiveSchedule?.id ?? detail.initialExamSchedule?.scheduleId) && (
                              <Button
                                type="button"
                                size="sm"
                                variant="secondary"
                                icon="clipboard"
                                onClick={() => {
                                  const sid = currentActiveSchedule?.id ?? detail.initialExamSchedule?.scheduleId;
                                  if (sid) {
                                    router.push(`/veterinarian/records?scheduleId=${sid}&back=admissions`);
                                  }
                                }}
                              >
                                Full Clinical Record
                              </Button>
                            )}
                          </div>
                          {displayedTrainingDecision && (
                            <div className="mt-3">
                              <Pill
                                tone={displayedTrainingDecision === 'ALLOWED' ? 'success' : 'danger'}
                              >
                                Training Decision: {formatLabel(displayedTrainingDecision)}
                              </Pill>
                            </div>
                          )}
                        </div>

                        {detail.initialVetExam ? (
                          <MedicalFindingsSection exam={detail.initialVetExam} />
                        ) : detail.healthRecords && detail.healthRecords.length > 0 ? (
                          <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-5">
                            <h4 className="text-sm font-bold text-[var(--color-text-primary)] mb-3">
                              Recorded Health Findings
                            </h4>
                            <div className="space-y-3">
                              {detail.healthRecords.map((record) => (
                                <div
                                  key={record.id}
                                  className="rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3 text-sm"
                                >
                                  <div className="flex justify-between items-center mb-2">
                                    <span className="font-semibold text-[var(--color-text-primary)]">
                                      {formatLabel(record.recordType)} Examination
                                    </span>
                                    <span className="text-xs text-[var(--color-text-muted)]">
                                      {formatDateTime(record.examinedAt)}
                                    </span>
                                  </div>
                                  {record.trainingDecision && (
                                    <div className="mb-2">
                                      <Pill tone={record.trainingDecision === 'ALLOWED' ? 'success' : 'danger'}>
                                        {record.trainingDecision === 'ALLOWED' ? 'Allowed to Train' : 'Blocked'}
                                      </Pill>
                                    </div>
                                  )}
                                  <div className="grid gap-2 sm:grid-cols-2">

                                    {record.findings && <div><span className="text-xs text-[var(--color-text-muted)] block">Findings</span>{record.findings}</div>}
                                    {record.diagnosis && <div><span className="text-xs text-[var(--color-text-muted)] block">Diagnosis</span>{record.diagnosis}</div>}
                                    {record.treatment && <div><span className="text-xs text-[var(--color-text-muted)] block">Treatment</span>{record.treatment}</div>}
                                  </div>
                                </div>
                              ))}
                            </div>
                          </div>
                        ) : (
                          <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-5 text-center text-sm text-[var(--color-text-muted)]">
                            No additional examination records found for this admission.
                          </div>
                        )}
                      </div>
                    ) : canCompleteExam ? (
                      <VetReviewForm
                        key={`${detail.admissionId}-${currentActiveSchedule?.id}`}
                        admissionId={detail.admissionId}
                        horseId={detail.horseId}
                        candidateName={detail.candidate?.name ?? selectedRow?.candidateName ?? 'Candidate'}
                        quarantineStallCode={detail.quarantineStallCode}
                        careScheduleId={currentActiveSchedule?.id}
                        careType={currentActiveSchedule?.careType}
                        scheduleStatus={currentActiveSchedule?.status}
                        schedulePurpose={currentActiveSchedule?.purpose}
                        onStartExam={handleStartExam}
                        asideTop={examDetailsCard}
                        onSuccess={handleReviewSuccess}
                        onDirtyChange={handleDirtyChange}
                      />
                    ) : (
                      <div className="p-8 text-center text-[var(--color-text-muted)]">
                        <Icon name="stethoscope" size={32} className="mx-auto mb-2 opacity-40" />
                        <p className="text-base font-semibold text-[var(--color-text-secondary)]">
                          Clinical review locked
                        </p>
                        <p className="text-xs mt-1">
                          {currentActiveSchedule?.status === 'REQUESTED'
                            ? 'The care schedule is waiting for automatic assignment.'
                            : assignedElsewhere
                              ? 'This case is assigned to another veterinarian.'
                              : 'No active examination found for this admission.'}
                        </p>
                      </div>
                    )}
                  </div>

                  {/* View Record (common form) */}
                  {activeTab === 'record' && (
                    <CareRecordDetailModal
                      scheduleId={currentActiveSchedule?.id ?? null}
                      open={true}
                      onClose={() => setActiveTab('exam')}
                      onScheduleUpdated={() => {}}
                    />
                  )}

                  {/* Tab 4: Intake Documents & Pedigree */}
                  {activeTab === 'documents' && (
                    <div className="grid gap-5 xl:grid-cols-2">
                      <div>
                        <AdmissionDocumentsSection documents={documents} assetUrl={admissionsApi.assetUrl} />
                      </div>

                      <div className="space-y-4">
                        <div className="rounded-[var(--radius-md)] bg-[var(--color-surface-subtle)] p-3.5 border border-[var(--color-border)]">
                          <h4 className="text-xs font-bold text-[var(--color-text-primary)]">
                            Pedigree &amp; Registry Information
                          </h4>
                          <dl className="mt-2 space-y-1.5 text-xs">
                            <div className="flex justify-between border-b border-[var(--color-border)] pb-1">
                              <span className="text-[var(--color-text-muted)]">Registry:</span>
                              <span className="font-semibold">{detail.candidate?.registryName || 'Not recorded'}</span>
                            </div>
                            <div className="flex justify-between border-b border-[var(--color-border)] pb-1">
                              <span className="text-[var(--color-text-muted)]">Registration No:</span>
                              <span className="font-semibold font-metric">{detail.candidate?.registrationNumber || 'Not recorded'}</span>
                            </div>
                            <div className="flex justify-between border-b border-[var(--color-border)] pb-1">
                              <span className="text-[var(--color-text-muted)]">Sire:</span>
                              <span className="font-semibold">{detail.candidate?.sireName || 'Not recorded'}</span>
                            </div>
                            <div className="flex justify-between">
                              <span className="text-[var(--color-text-muted)]">Dam:</span>
                              <span className="font-semibold">{detail.candidate?.damName || 'Not recorded'}</span>
                            </div>
                          </dl>
                        </div>

                        <div className="rounded-[var(--radius-md)] bg-[var(--color-surface-subtle)] p-3.5 border border-[var(--color-border)]">
                          <h4 className="text-xs font-bold text-[var(--color-text-primary)]">
                            Groom Physical Screening
                          </h4>
                          <dl className="mt-2 space-y-1 text-xs">
                            <div className="flex justify-between">
                              <span className="text-[var(--color-text-muted)]">Outcome:</span>
                              <span className="font-semibold">{detail.groomDecision ? formatLabel(detail.groomDecision) : 'Pending'}</span>
                            </div>
                            <div className="flex justify-between">
                              <span className="text-[var(--color-text-muted)]">Reviewed At:</span>
                              <span className="font-metric">{formatDateTime(detail.groomReviewedAt)}</span>
                            </div>
                          </dl>
                          {detail.groomFeedback && (
                            <p className="mt-2 rounded bg-[var(--color-surface)] p-2 text-xs text-[var(--color-text-secondary)] border border-[var(--color-border)]">
                              {detail.groomFeedback}
                            </p>
                          )}
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              ) : null}
            </div>
          )}
          sidebar={examDetailsCard}
          asideHidden={activeTab === 'exam' && canCompleteExam}
        />
      )}

      {/* Discard Changes Confirmation Dialog */}
      <ConfirmDialog
        open={showCloseConfirm}
        title="Discard Unsaved Clinical Changes?"
        description="You have unsaved changes in this examination form. Returning to Admissions now will lose any unrecorded clinical documentation."
        confirmLabel="Discard & Go Back"
        cancelLabel="Keep Editing"
        tone="danger"
        onConfirm={() => {
          setIsDrawerDirty(false);
          setShowCloseConfirm(false);
          setDrawerOpen(false);
          setSelectedId(null);
          setTargetScheduleId(null);
          setDeepLinkedSchedule(null);
          router.replace('/veterinarian/admissions', { scroll: false });
        }}
        onCancel={() => setShowCloseConfirm(false)}
      />
    </QueueShell>
  );
}

/** The list keeps its page header; an open admission brings its own header instead. */
function QueueShell({ showHeader, children }: { showHeader: boolean; children: ReactNode }) {
  if (!showHeader) return <div className="w-full min-w-0 space-y-5">{children}</div>;
  return (
    <AdmissionListLayout title="Veterinary admissions">
      {children}
    </AdmissionListLayout>
  );
}
