'use client';

import { TBody, THead, Table, Td, Th, Tr } from '@/components/ui/Table';
import { useEffect, useMemo, useState, useCallback, type ReactNode } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import { ApiError } from '@/services/api';
import { Button } from '@/components/ui/Button';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { EntityLink } from '@/components/ui/EntityLink';
import { entityHref } from '@/lib/entityRoute';
import { Icon } from '@/components/ui/Icon';
import { Pill } from '@/components/ui/StatusBadge';
import { Tabs, type TabItem } from '@/components/ui/Tabs';
import { DetailSkeleton, EmptyState, ListSkeleton } from '@/components/ui/states';
import { FilterBar } from '@/components/ui/FilterBar';
import { Panel } from '@/components/ui/Panel';
import { Pagination } from '@/components/ui/Pagination';
import { FormField } from '@/components/ui/FormField';
import { SearchInput, Select } from '@/components/ui/Input';
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

type QueuePillFilter = 'ACTIVE' | 'AWAITING' | 'IN_PROGRESS' | 'APPROVED' | 'REVIEWED';

const examStatusOptions: Array<{ value: CareScheduleStatus | 'ALL'; label: string }> = [
  { value: 'ALL', label: 'All Exam States' },
  { value: 'REQUESTED', label: 'Requested' },
  { value: 'SCHEDULED', label: 'Scheduled' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'COMPLETED', label: 'Completed' },
];

const examTypeOptions: Array<{ value: CareType | 'ALL'; label: string }> = [
  { value: 'ALL', label: 'All Exam Types' },
  { value: 'INITIAL', label: 'Initial Exam' },
  { value: 'ROUTINE', label: 'Routine Exam' },
  { value: 'URGENT', label: 'Urgent Exam' },
];

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

function examTone(status: CareScheduleStatus | undefined) {
  if (status === 'COMPLETED') return 'success' as const;
  if (status === 'IN_PROGRESS') return 'primary' as const;
  if (status === 'PAUSED') return 'warning' as const;
  if (status === 'SCHEDULED') return 'info' as const;
  if (status === 'REQUESTED') return 'warning' as const;
  return 'neutral' as const;
}

function examIcon(status: CareScheduleStatus | undefined) {
  if (status === 'COMPLETED') return 'check' as const;
  if (status === 'IN_PROGRESS') return 'activity' as const;
  if (status === 'PAUSED') return 'clock' as const;
  if (status === 'SCHEDULED') return 'calendar' as const;
  if (status === 'REQUESTED') return 'clock' as const;
  return 'circle' as const;
}

function getPriorityBadge(careType: CareType | undefined, purpose?: string | null) {
  const tone = getCareTypeTone(careType ?? 'ROUTINE', purpose);
  return { label: tone.label, tone: tone.pillTone, icon: tone.icon };
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
  const [admissionFilter, setAdmissionFilter] = useState<'ALL' | AdmissionStatus>('ALL');
  const [examStatusFilter, setExamStatusFilter] = useState<CareScheduleStatus | 'ALL'>('ALL');
  const [examTypeFilter, setExamTypeFilter] = useState<CareType | 'ALL'>('ALL');
  const [priorityFilter, setPriorityFilter] = useState<'ALL' | 'URGENT' | 'NORMAL'>('ALL');

  // Request & operation state
  const [loading, setLoading] = useState(true);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [startingExam, setStartingExam] = useState(false);
  const [error, setError] = useState('');
  const [detailError, setDetailError] = useState('');
  const [success, setSuccess] = useState('');
  const [scheduleWarning, setScheduleWarning] = useState('');
  const [reload, setReload] = useState(0);

  // Deep link support via ?id= query param
  useEffect(() => {
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


  // Server-side filtered queue loading with legacy backend fallback
  useEffect(() => {
    let active = true;
    Promise.resolve().then(() => {
      if (!active) return;
      setLoading(true);
      setError('');
    });

    admissionsApi
      .getVetQueueSummary()
      .then(async (summaryData) => {
        if (!active) return;

        // If backend hasn't been restarted, summaryData.approved will be undefined or missing.
        const isLegacyBackend = summaryData.approved == null;

        if (isLegacyBackend) {
          // Fetch unfiltered queue without pill so client can inspect admissions
          const fullPage = await admissionsApi.getVetQueue({
            search: searchQuery.trim() || undefined,
            admissionStatus: admissionFilter !== 'ALL' ? admissionFilter : undefined,
            scheduleStatus: examStatusFilter !== 'ALL' ? examStatusFilter : undefined,
            careType: examTypeFilter !== 'ALL' ? examTypeFilter : undefined,
            priority: priorityFilter !== 'ALL' ? priorityFilter : undefined,
            page: 0,
            size: 100,
          });
          if (!active) return;

          const allContent = fullPage.content;
          const approvedCount = allContent.filter((r) => r.admissionStatus === 'APPROVED').length;
          const activeRows = allContent.filter((r) => r.admissionStatus !== 'APPROVED');
          const awaitingCount = activeRows.filter(
            (r) => r.careSchedule?.status === 'REQUESTED' || r.careSchedule?.status === 'SCHEDULED'
          ).length;
          const inProgressCount = activeRows.filter(
            (r) => r.careSchedule?.status === 'IN_PROGRESS'
          ).length;

          setSummary({
            total: allContent.length,
            active: activeRows.length,
            awaiting: awaitingCount,
            inProgress: inProgressCount,
            approved: approvedCount,
            reviewed: allContent.filter((r) => r.careSchedule?.status === 'COMPLETED').length,
          });

          let filtered = allContent;
          if (queuePillFilter === 'APPROVED') {
            filtered = allContent.filter((r) => r.admissionStatus === 'APPROVED');
          } else if (queuePillFilter === 'REVIEWED') {
            filtered = allContent.filter((r) => r.careSchedule?.status === 'COMPLETED');
          } else if (queuePillFilter === 'ACTIVE') {
            filtered = activeRows;
          } else if (queuePillFilter === 'AWAITING') {
            filtered = activeRows.filter(
              (r) => r.careSchedule?.status === 'REQUESTED' || r.careSchedule?.status === 'SCHEDULED'
            );
          } else if (queuePillFilter === 'IN_PROGRESS') {
            filtered = activeRows.filter((r) => r.careSchedule?.status === 'IN_PROGRESS');
          }

          setTotalElements(filtered.length);
          const startIndex = page * pageSize;
          setItems(filtered.slice(startIndex, startIndex + pageSize));
        } else {
          setSummary(summaryData);
          const queuePage = await admissionsApi.getVetQueue({
            search: searchQuery.trim() || undefined,
            pill: queuePillFilter,
            admissionStatus: admissionFilter !== 'ALL' ? admissionFilter : undefined,
            scheduleStatus: examStatusFilter !== 'ALL' ? examStatusFilter : undefined,
            careType: examTypeFilter !== 'ALL' ? examTypeFilter : undefined,
            priority: priorityFilter !== 'ALL' ? priorityFilter : undefined,
            page,
            size: pageSize,
          });
          if (!active) return;
          setItems(queuePage.content);
          setTotalElements(queuePage.totalElements);
        }
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
    admissionFilter,
    examStatusFilter,
    examTypeFilter,
    priorityFilter,
    page,
    pageSize,
    reload,
  ]);

  const displayedItems = useMemo(() => {
    return items.filter((row) => {
      if (queuePillFilter === 'ACTIVE' || queuePillFilter === 'AWAITING' || queuePillFilter === 'IN_PROGRESS') {
        return row.admissionStatus !== 'APPROVED';
      }
      if (queuePillFilter === 'APPROVED') {
        return row.admissionStatus === 'APPROVED';
      }
      return true;
    });
  }, [items, queuePillFilter]);

  // The summary counts describe the unfiltered queue; once any filter narrows
  // the list, the server's filtered totalElements is the only correct total.
  const filtersNarrowList =
    searchQuery.trim() !== '' ||
    admissionFilter !== 'ALL' ||
    examStatusFilter !== 'ALL' ||
    examTypeFilter !== 'ALL' ||
    priorityFilter !== 'ALL';

  const effectiveTotal = useMemo(() => {
    if (!filtersNarrowList) {
      if (queuePillFilter === 'ACTIVE' && summary.active != null) return summary.active;
      if (queuePillFilter === 'AWAITING' && summary.awaiting != null) return summary.awaiting;
      if (queuePillFilter === 'IN_PROGRESS' && summary.inProgress != null) return summary.inProgress;
      if (queuePillFilter === 'APPROVED' && summary.approved != null) return summary.approved;
      if (queuePillFilter === 'REVIEWED' && summary.reviewed != null) return summary.reviewed;
    }
    return totalElements;
  }, [queuePillFilter, summary, totalElements, filtersNarrowList]);

  const hasActiveFilters =
    searchQuery !== '' ||
    queuePillFilter !== 'ACTIVE' ||
    admissionFilter !== 'ALL' ||
    examStatusFilter !== 'ALL' ||
    examTypeFilter !== 'ALL' ||
    priorityFilter !== 'ALL';

  const resetFilters = () => {
    setSearchQuery('');
    setQueuePillFilter('ACTIVE');
    setAdmissionFilter('ALL');
    setExamStatusFilter('ALL');
    setExamTypeFilter('ALL');
    setPriorityFilter('ALL');
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
        if (detailData.status === 'APPROVED') {
          setActiveTab('exam');
        }
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

  const explicitlySelectedSchedule = targetScheduleId
    ? careSchedules.find((schedule) => schedule.id === targetScheduleId) ??
      (deepLinkedSchedule?.id === targetScheduleId ? deepLinkedSchedule : null)
    : null;
  const assignedActiveSchedule = careSchedules
    .filter((schedule) =>
      ['SCHEDULED', 'IN_PROGRESS'].includes(schedule.status) &&
      (schedule.veterinarianId ?? schedule.assignedVetId) === user?.userId,
    )
    .sort((a, b) => {
      if (a.status !== b.status) return a.status === 'IN_PROGRESS' ? -1 : 1;
      return new Date(a.scheduledAt ?? a.scheduledDate ?? 0).getTime()
        - new Date(b.scheduledAt ?? b.scheduledDate ?? 0).getTime();
    })[0] ?? null;
  const currentActiveSchedule =
    explicitlySelectedSchedule ??
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

  // Requirement: Disable Start for REQUESTED schedules; only allow starting when SCHEDULED
  // If admission is already APPROVED, examining is disabled (read-only record viewing only).
  const canStartExam = Boolean(
    !isApprovedAdmission &&
    currentActiveSchedule &&
      currentActiveSchedule.status === 'SCHEDULED' &&
      assignedVetId === user?.userId,
  );

  const canCompleteExam = Boolean(
    !isApprovedAdmission &&
    currentActiveSchedule &&
      ['SCHEDULED', 'IN_PROGRESS'].includes(currentActiveSchedule.status) &&
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

  const handleOpenAdmission = (id: number, schedule: CareSchedule | null, forceViewRecord = false) => {
    setTargetScheduleId(schedule?.id ?? null);
    setDeepLinkedSchedule(null);
    setSelectedId(id);
    const row = items.find((r) => r.admissionId === id);
    const isApproved = forceViewRecord || row?.admissionStatus === 'APPROVED' || queuePillFilter === 'APPROVED';
    // When approved, user only views records; a completed exam has no review form to fill either.
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
    const isApproved = row.admissionStatus === 'APPROVED' || queuePillFilter === 'APPROVED';
    if (isApproved) {
      handleOpenAdmission(row.admissionId, row.careSchedule, true);
      return;
    }
    // Examine = go straight to the examination workspace (it auto-starts the
    // SCHEDULED exam). Continue/View results/Details keep opening the drawer.
    if (row.careSchedule?.status === 'SCHEDULED' && row.careSchedule.id) {
      router.push(`/veterinarian/schedule/${row.careSchedule.id}?back=admissions`);
      return;
    }
    handleOpenAdmission(row.admissionId, row.careSchedule);
  };



  const examDetailsCard = detail ? (
    <AdmissionSideCard title="Examination details">
      <InfoRow label="Priority" value={selectedRow ? getPriorityBadge(selectedRow.careSchedule?.careType, selectedRow.careSchedule?.purpose).label : 'Standard'} />
      <InfoRow label="Schedule" value={currentActiveSchedule ? formatLabel(currentActiveSchedule.status) : 'Not scheduled'} />
      <InfoRow label="Quarantine stall" value={detail.quarantineStallCode || 'Not assigned'} />
      <InfoRow label="Trainer" value={<EntityLink href={detail.trainerId ? entityHref('user', detail.trainerId, 'VETERINARIAN') : null}>{detail.trainerName || (detail.trainerId ? `Trainer #${detail.trainerId}` : 'Pending assignment')}</EntityLink>} />
      {currentActiveSchedule?.scheduledAt && <InfoRow label="Scheduled" value={formatDateTime(currentActiveSchedule.scheduledAt)} />}
      {canStartExam && (
        <Button type="button" className="w-full transition-all active:scale-[0.97]" variant="primary" loading={startingExam} onClick={handleStartExam} icon="activity">
          Start examination
        </Button>
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
            { value: 'AWAITING', label: 'Awaiting', count: summary.awaiting },
            { value: 'IN_PROGRESS', label: 'In exam', count: summary.inProgress },
            { value: 'APPROVED', label: 'Approved', count: summary.approved ?? 0 },
            { value: 'REVIEWED', label: 'Reviewed', count: summary.reviewed ?? 0 },
          ]}
        />
        <Button type="button" onClick={handleRefresh} disabled={loading} icon="refresh" variant="secondary" size="sm" className="transition-all active:scale-[0.98]">
          Refresh
        </Button>
      </div>

      <FilterBar className="[&_input]:min-h-9 [&_select]:min-h-9" onSubmit={(event) => event.preventDefault()}>
        <FormField label="Search admissions" className="min-w-[14rem] flex-[1.4]">
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
        <FormField label="Stage" className="min-w-[9rem] flex-1">
          <Select
            value={admissionFilter}
            onChange={(e) => {
              setAdmissionFilter(e.target.value as typeof admissionFilter);
              setPage(0);
            }}
          >
            <option value="ALL">All stages</option>
            <option value="VET_REVIEW">Vet review</option>
            <option value="TRAINER_REVIEW">Trainer review</option>
            <option value="MANAGER_REVIEW">Manager review</option>
            <option value="APPROVED">Approved</option>
            <option value="REJECTED">Rejected</option>
          </Select>
        </FormField>
        <FormField label="Exam status" className="min-w-[9rem] flex-1">
          <Select
            value={examStatusFilter}
            onChange={(e) => {
              setExamStatusFilter(e.target.value as CareScheduleStatus | 'ALL');
              setPage(0);
            }}
          >
            {examStatusOptions.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </Select>
        </FormField>
        <FormField label="Exam type" className="min-w-[9rem] flex-1">
          <Select
            value={examTypeFilter}
            onChange={(e) => {
              setExamTypeFilter(e.target.value as CareType | 'ALL');
              setPage(0);
            }}
          >
            {examTypeOptions.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </Select>
        </FormField>
        <FormField label="Priority" className="min-w-[9rem] flex-1">
          <Select
            value={priorityFilter}
            onChange={(e) => {
              setPriorityFilter(e.target.value as typeof priorityFilter);
              setPage(0);
            }}
          >
            <option value="ALL">All priorities</option>
            <option value="URGENT">Urgent</option>
            <option value="NORMAL">Standard</option>
          </Select>
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
              const priority = getPriorityBadge(row.careSchedule?.careType, row.careSchedule?.purpose);
              const stallCode =
                stallCodes[row.admissionId] ||
                row.quarantineStallCode ||
                (row.quarantineStallId ? `Q-Stall #${row.quarantineStallId}` : 'Quarantine');
              const isApproved = row.admissionStatus === 'APPROVED' || queuePillFilter === 'APPROVED';
              const status = row.careSchedule?.status;
              const action = isApproved
                ? ({ label: 'View Record', icon: 'clipboard' as const, primary: false })
                : status === 'PAUSED'
                ? ({ label: 'Continue (Paused)', icon: 'activity' as const, primary: true })
                : status === 'IN_PROGRESS'
                ? ({ label: 'Continue', icon: 'activity' as const, primary: true })
                : status === 'SCHEDULED'
                ? ({ label: 'Examine', icon: 'stethoscope' as const, primary: true })
                : status === 'COMPLETED'
                ? ({ label: 'View results', icon: 'check' as const, primary: false })
                : ({ label: 'Details', icon: 'chevron-right' as const, primary: false });

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
                        <div className="flex flex-wrap items-center gap-2">
                          <EntityLink href={row.horseId ? entityHref('horse', row.horseId, 'VETERINARIAN') : null} className="text-base font-bold text-[var(--color-text-primary)] truncate">
                            {row.candidateName}
                          </EntityLink>
                          <Pill tone={priority.tone} size="sm" icon={priority.icon}>
                            {priority.label}
                          </Pill>
                          <AdmissionStatusBadge status={row.admissionStatus} size="sm" />
                          {row.careSchedule && (
                            <Pill
                              tone={examTone(row.careSchedule.status)}
                              size="sm"
                              icon={examIcon(row.careSchedule.status)}
                            >
                              {formatLabel(row.careSchedule.status)}
                            </Pill>
                          )}
                        </div>
                        <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-[var(--color-text-secondary)]">
                          <span className="inline-flex items-center gap-1 shrink-0">
                            <Icon name="building" size={12} />
                            <span>Stall: <strong>{stallCode}</strong></span>
                          </span>
                          <span>·</span>
                          <span className="inline-flex items-center gap-1 shrink-0">
                            <Icon name="calendar" size={12} className="text-[var(--color-primary)]" />
                            {row.careSchedule?.scheduledAt ? (
                              <strong>{formatDateTime(row.careSchedule.scheduledAt)}</strong>
                            ) : row.careSchedule?.scheduledDate ? (
                              <span>Target: <strong>{formatDate(row.careSchedule.scheduledDate)}</strong></span>
                            ) : (
                              <span className="italic text-[var(--color-text-muted)]">Awaiting slot</span>
                            )}
                          </span>
                          {isAssignedToMe && (
                            <>
                              <span>·</span>
                              <span className="font-semibold text-[var(--color-primary)]">Assigned to you</span>
                            </>
                          )}
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
              <EntityLink href={selectedRow?.horseId ? entityHref('horse', selectedRow.horseId, 'VETERINARIAN') : null}>{selectedRow?.candidateName ?? 'Loading admission'}</EntityLink>
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
                    {isApprovedAdmission ? (
                      <div className="space-y-5">
                        <div className="rounded-[var(--radius-md)] border border-[var(--color-success)] bg-[var(--color-success-soft)] p-5">
                          <div className="flex items-center gap-2.5">
                            <Icon name="check" size={24} className="text-[var(--color-success)] shrink-0" />
                            <div>
                              <h3 className="text-base font-bold text-[var(--color-success)]">
                                Admission Approved
                              </h3>
                              <p className="mt-0.5 text-sm text-[var(--color-text-secondary)]">
                                This admission has been finalized and approved by management. Clinical examination records are locked for editing.
                              </p>
                            </div>
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
                        documents={documents}
                      />
                    ) : currentActiveSchedule?.status === 'COMPLETED' ? (
                      <div className="rounded-[var(--radius-md)] border border-[var(--color-success)] bg-[var(--color-success-soft)] p-5 text-center">
                        <Icon name="check" size={28} className="mx-auto text-[var(--color-success)]" />
                        <h3 className="mt-2 text-lg font-bold text-[var(--color-success)]">
                          Examination Completed
                        </h3>
                        <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
                          {currentActiveSchedule.careType === 'INITIAL'
                            ? 'The veterinary review for this intake stage has already been submitted and finalized.'
                            : 'This care schedule has already been completed. The Admission workflow was not changed.'}
                        </p>
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
