'use client';

import { useEffect, useMemo, useRef, useState, useCallback } from 'react';
import { useAuth } from '@/context/AuthContext';
import { ApiError, getServerTime } from '@/services/api';
import { Button } from '@/components/ui/Button';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { Icon } from '@/components/ui/Icon';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { Tabs, type TabItem } from '@/components/ui/Tabs';
import { DetailSkeleton, EmptyState, ListSkeleton } from '@/components/ui/states';
import { admissionsApi } from '../services/api';
import type {
  AdmissionDetailResponse,
  AdmissionDocument,
  AdmissionStatus,
  AdmissionSummaryResponse,
  CareSchedule,
  CareScheduleStatus,
  CareType,
  HorseHealthMetricResponse,
  PendingVetOfferResponse,
  VetReviewResponse,
} from '../types';
import { VetReviewForm } from './VetReviewForm';
import { VET_OFFERS_CHANGED_EVENT } from './VetOfferNotifier';

type QueueRow = AdmissionSummaryResponse & {
  careSchedule: CareSchedule | null;
};

type QueuePillFilter = 'ALL' | 'AWAITING' | 'IN_PROGRESS' | 'RECHECK';

const examStatusOptions: Array<{ value: CareScheduleStatus | 'ALL'; label: string }> = [
  { value: 'ALL', label: 'All Exam States' },
  { value: 'REQUESTED', label: 'Requested' },
  { value: 'AWAITING_VET_CONFIRMATION', label: 'Awaiting Confirmation' },
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

function formatDate(value: string | null | undefined, includeTime = false) {
  if (!value) return 'Not scheduled';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat(undefined, includeTime
    ? { dateStyle: 'medium', timeStyle: 'short' }
    : { dateStyle: 'medium' }).format(date);
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
  if (status === 'SCHEDULED') return 'info' as const;
  if (status === 'REQUESTED' || status === 'AWAITING_VET_CONFIRMATION') return 'warning' as const;
  return 'neutral' as const;
}

function examIcon(status: CareScheduleStatus | undefined) {
  if (status === 'COMPLETED') return 'check' as const;
  if (status === 'IN_PROGRESS') return 'activity' as const;
  if (status === 'SCHEDULED') return 'calendar' as const;
  if (status === 'AWAITING_VET_CONFIRMATION' || status === 'REQUESTED') return 'clock' as const;
  return 'circle' as const;
}

function admissionTone(status: AdmissionStatus) {
  if (status === 'PENDING_RECHECK') return 'warning' as const;
  if (status === 'APPROVED') return 'success' as const;
  if (status === 'REJECTED') return 'danger' as const;
  return 'info' as const;
}

function getPriorityBadge(careType: CareType | undefined) {
  if (careType === 'URGENT') {
    return { label: 'P1 · Urgent', tone: 'danger' as const, icon: 'alert-triangle' as const };
  }
  if (careType === 'INITIAL') {
    return { label: 'P2 · Initial', tone: 'info' as const, icon: 'stethoscope' as const };
  }
  return { label: 'P4 · Routine', tone: 'neutral' as const, icon: 'clock' as const };
}

function formatCountdown(expiresAt: string): string {
  if (!expiresAt) return 'Expired';
  const targetTime = new Date(expiresAt).getTime();
  if (Number.isNaN(targetTime)) return 'Expired';
  const diffMs = targetTime - getServerTime();
  if (diffMs <= 0) return 'Expired';
  const totalSeconds = Math.floor(diffMs / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}m ${seconds.toString().padStart(2, '0')}s`;
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
      <span className="text-[10px] font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">
        {label}
      </span>
      <p className="mt-1 whitespace-pre-wrap text-[12px] leading-relaxed text-[var(--color-text-primary)]">
        {value}
      </p>
    </div>
  );
}

export function VetAdmissionQueue() {
  const { user } = useAuth();
  const [admissions, setAdmissions] = useState<AdmissionSummaryResponse[]>([]);
  const [careSchedules, setCareSchedules] = useState<CareSchedule[]>([]);
  const [pendingOffers, setPendingOffers] = useState<PendingVetOfferResponse[]>([]);
  const [actioningOfferId, setActioningOfferId] = useState<number | null>(null);
  const [stallCodes, setStallCodes] = useState<Record<number, string>>({});

  // Drawer & Selection state
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [isDrawerDirty, setIsDrawerDirty] = useState(false);
  const [showCloseConfirm, setShowCloseConfirm] = useState(false);

  // Detail data for opened admission
  const [detail, setDetail] = useState<AdmissionDetailResponse | null>(null);
  const [documents, setDocuments] = useState<AdmissionDocument[]>([]);
  const [metrics, setMetrics] = useState<HorseHealthMetricResponse[]>([]);
  const [activeTab, setActiveTab] = useState<string>('exam');

  // Filters state
  const [searchQuery, setSearchQuery] = useState('');
  const [queuePillFilter, setQueuePillFilter] = useState<QueuePillFilter>('ALL');
  const [admissionFilter, setAdmissionFilter] = useState<'ALL' | 'VET_REVIEW' | 'PENDING_RECHECK'>('ALL');
  const [examStatusFilter, setExamStatusFilter] = useState<CareScheduleStatus | 'ALL'>('ALL');
  const [examTypeFilter, setExamTypeFilter] = useState<CareType | 'ALL'>('ALL');
  const [priorityFilter, setPriorityFilter] = useState<'ALL' | 'URGENT' | 'NORMAL'>('ALL');
  const [isFilterPopoverOpen, setIsFilterPopoverOpen] = useState(false);
  const filterPopoverRef = useRef<HTMLDivElement>(null);

  // Request & operation state
  const [loading, setLoading] = useState(true);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [startingExam, setStartingExam] = useState(false);
  const [error, setError] = useState('');
  const [detailError, setDetailError] = useState('');
  const [success, setSuccess] = useState('');
  const [scheduleWarning, setScheduleWarning] = useState('');
  const [reload, setReload] = useState(0);

  // Live countdown timer tick only when pending offers exist
  const [, setTick] = useState(0);
  useEffect(() => {
    if (pendingOffers.length === 0) return;
    const timer = setInterval(() => {
      setTick((t) => t + 1);
    }, 1000);
    return () => clearInterval(timer);
  }, [pendingOffers.length]);

  // Listen to offer change events from other tabs / notification components
  useEffect(() => {
    const handleOffersChanged = () => {
      setLoading(true);
      setReload((current) => current + 1);
    };
    window.addEventListener(VET_OFFERS_CHANGED_EVENT, handleOffersChanged);
    return () => {
      window.removeEventListener(VET_OFFERS_CHANGED_EVENT, handleOffersChanged);
    };
  }, []);

  // Close filter popover on click outside
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (filterPopoverRef.current && !filterPopoverRef.current.contains(e.target as Node)) {
        setIsFilterPopoverOpen(false);
      }
    };
    if (isFilterPopoverOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isFilterPopoverOpen]);

  // Load Admissions & Care Schedules
  useEffect(() => {
    let active = true;
    Promise.all([
      admissionsApi.getAdmissions('VET_REVIEW'),
      admissionsApi.getAdmissions('PENDING_RECHECK'),
      admissionsApi.getPendingOffers().catch(() => []),
    ])
      .then(async ([initial, rechecks, offers]) => {
        if (!active) return;
        const nextAdmissions = [...initial, ...rechecks].filter(
          (row, index, all) => all.findIndex((candidate) => candidate.admissionId === row.admissionId) === index,
        );
        const scheduleGroups = await Promise.all(
          nextAdmissions.map(async (admission) => {
            const firstPage = await admissionsApi.getCareSchedules({ admissionId: admission.admissionId, size: 100 });
            const remainingPages = await Promise.all(
              Array.from({ length: Math.max(0, firstPage.totalPages - 1) }, (_, index) =>
                admissionsApi.getCareSchedules({ admissionId: admission.admissionId, page: index + 1, size: 100 }),
              ),
            );
            return [firstPage, ...remainingPages].flatMap((page) => page.content);
          }),
        );
        if (!active) return;
        setAdmissions(nextAdmissions);
        setCareSchedules(scheduleGroups.flat());
        setPendingOffers(offers || []);
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
  }, [reload]);

  // Merge admissions with careSchedules and sort by business priority
  const rows = useMemo<QueueRow[]>(() => {
    return admissions
      .map((admission) => {
        const relatedSchedules = careSchedules
          .filter((cs) => cs.admissionId === admission.admissionId)
          .sort((a, b) => new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime());
        const activeSchedule =
          relatedSchedules.find((cs) => !['COMPLETED', 'CANCELLED'].includes(cs.status)) ??
          relatedSchedules[0] ??
          null;

        return {
          ...admission,
          careSchedule: activeSchedule,
        };
      })
      .sort((a, b) => {
        const priorityA =
          a.careSchedule?.careType === 'URGENT' ? 400 : a.careSchedule?.careType === 'INITIAL' ? 300 : a.careSchedule ? 100 : 0;
        const priorityB =
          b.careSchedule?.careType === 'URGENT' ? 400 : b.careSchedule?.careType === 'INITIAL' ? 300 : b.careSchedule ? 100 : 0;
        const priorityDiff = priorityB - priorityA;
        return priorityDiff || new Date(a.submittedAt).getTime() - new Date(b.submittedAt).getTime();
      });
  }, [admissions, careSchedules]);

  // Counts for Compact Filter Pills
  const totalCount = rows.length;
  const requestedCount = rows.filter(
    (r) => r.careSchedule?.status === 'REQUESTED' || r.careSchedule?.status === 'AWAITING_VET_CONFIRMATION',
  ).length;
  const inProgressCount = rows.filter((r) => r.careSchedule?.status === 'IN_PROGRESS').length;
  const rechecksCount = rows.filter((r) => r.status === 'PENDING_RECHECK').length;

  // Filter rows based on search, pill filters, and popover filters
  const filteredRows = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    return rows.filter((row) => {
      // 1. Search Query
      if (query) {
        const matchesName = row.candidateName.toLowerCase().includes(query);
        const matchesId = String(row.admissionId).includes(query);
        const matchesBreed = row.breed.toLowerCase().includes(query);
        if (!matchesName && !matchesId && !matchesBreed) return false;
      }

      // 2. Compact Filter Pills
      if (queuePillFilter === 'AWAITING') {
        const isAwaiting =
          row.careSchedule?.status === 'REQUESTED' || row.careSchedule?.status === 'AWAITING_VET_CONFIRMATION';
        if (!isAwaiting) return false;
      } else if (queuePillFilter === 'IN_PROGRESS') {
        if (row.careSchedule?.status !== 'IN_PROGRESS') return false;
      } else if (queuePillFilter === 'RECHECK') {
        if (row.status !== 'PENDING_RECHECK') return false;
      }

      // 3. Popover Filters
      if (admissionFilter !== 'ALL' && row.status !== admissionFilter) return false;
      if (examStatusFilter !== 'ALL' && row.careSchedule?.status !== examStatusFilter) return false;
      if (examTypeFilter !== 'ALL' && row.careSchedule?.careType !== examTypeFilter) return false;

      if (priorityFilter === 'URGENT') {
        if (row.careSchedule?.careType !== 'URGENT') return false;
      } else if (priorityFilter === 'NORMAL') {
        if (row.careSchedule?.careType === 'URGENT') return false;
      }

      return true;
    });
  }, [rows, searchQuery, queuePillFilter, admissionFilter, examStatusFilter, examTypeFilter, priorityFilter]);

  // Count active popover filters
  const activePopoverFilterCount = useMemo(() => {
    let count = 0;
    if (admissionFilter !== 'ALL') count++;
    if (examStatusFilter !== 'ALL') count++;
    if (examTypeFilter !== 'ALL') count++;
    if (priorityFilter !== 'ALL') count++;
    return count;
  }, [admissionFilter, examStatusFilter, examTypeFilter, priorityFilter]);

  // Fetch admission details when selected
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
      }
    });

    Promise.all([
      admissionsApi.getAdmissionDetail(selectedId),
      admissionsApi.getDocuments(selectedId),
    ])
      .then(async ([detailData, docs]) => {
        if (!active) return;
        setDetail(detailData);
        setDocuments(docs);
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

  const selectedRow = rows.find((row) => row.admissionId === selectedId) ?? null;

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

  const currentActiveSchedule = selectedRow?.careSchedule ?? detailScheduleFallback;

  const assignedVetId = currentActiveSchedule?.veterinarianId ?? currentActiveSchedule?.assignedVetId ?? null;
  const assignedElsewhere = Boolean(assignedVetId && user?.userId && assignedVetId !== user.userId);
  const isAssignedToCurrentVet = Boolean(assignedVetId && user?.userId && assignedVetId === user.userId);

  const canStartExam = Boolean(
    currentActiveSchedule &&
      (currentActiveSchedule.status === 'SCHEDULED' ||
        (currentActiveSchedule.status === 'REQUESTED' && currentActiveSchedule.careType === 'URGENT')) &&
      !assignedElsewhere,
  );
  const canCompleteExam = Boolean(
    currentActiveSchedule &&
      (['SCHEDULED', 'IN_PROGRESS'].includes(currentActiveSchedule.status) ||
        (currentActiveSchedule.status === 'REQUESTED' && currentActiveSchedule.careType === 'URGENT')) &&
      !assignedElsewhere,
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
      .filter((cs) => cs.admissionId === selectedId || (detail?.horseId && cs.horseId === detail.horseId))
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

  const handleOpenAdmission = (id: number) => {
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
    }
  }, [isDrawerDirty]);

  // Handle Esc key for drawer
  useEffect(() => {
    if (!drawerOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        if (showCloseConfirm) return;
        handleRequestCloseDrawer();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [drawerOpen, showCloseConfirm, handleRequestCloseDrawer]);

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

  async function handleAcceptOffer(offer: PendingVetOfferResponse) {
    try {
      setActioningOfferId(offer.id);
      setError('');
      await admissionsApi.acceptOffer(offer.id);
      setPendingOffers((current) => current.filter((item) => item.id !== offer.id));
      setSuccess(`Accepted care offer for ${offer.horseName}! Examination has been scheduled.`);
      window.dispatchEvent(new Event(VET_OFFERS_CHANGED_EVENT));
      reloadQueue();
    } catch (cause) {
      if (cause instanceof ApiError && cause.status === 409) {
        setError(`Offer for ${offer.horseName} is no longer available (claimed or expired).`);
      } else {
        setError(errorText(cause));
      }
      setPendingOffers((current) => current.filter((item) => item.id !== offer.id));
      reloadQueue();
    } finally {
      setActioningOfferId(null);
    }
  }

  async function handleDeclineOffer(offer: PendingVetOfferResponse) {
    try {
      setActioningOfferId(offer.id);
      setError('');
      await admissionsApi.declineOffer(offer.id);
      setPendingOffers((current) => current.filter((item) => item.id !== offer.id));
      setSuccess(`Declined care offer for ${offer.horseName}.`);
      window.dispatchEvent(new Event(VET_OFFERS_CHANGED_EVENT));
      reloadQueue();
    } catch (cause) {
      if (cause instanceof ApiError && cause.status === 409) {
        setError(`State conflict while declining offer for ${offer.horseName}.`);
      } else {
        setError(errorText(cause));
      }
      setPendingOffers((current) => current.filter((item) => item.id !== offer.id));
      reloadQueue();
    } finally {
      setActioningOfferId(null);
    }
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

  function handleReviewSuccess(result: VetReviewResponse, warning?: string) {
    const outcomeMessage =
      result.decision === 'APPROVED'
        ? 'Review approved! The admission moved to Trainer review; horse remains in quarantine.'
        : result.decision === 'RECHECK_REQUIRED'
          ? 'Recheck scheduled! Horse remains in quarantine with training locked until follow-up.'
          : 'Admission rejected! Quarantine stall released.';
    setSuccess(`Admission #${result.admissionId} (${selectedRow?.candidateName ?? 'Horse'}): ${outcomeMessage}`);
    setScheduleWarning(warning || '');
    setIsDrawerDirty(false);
    setDrawerOpen(false);
    reloadQueue();
  }

  const workspaceTabs: TabItem[] = [
    { id: 'exam', label: 'Clinical Examination', icon: 'stethoscope' },
    {
      id: 'history',
      label: 'Medical Records',
      icon: 'clipboard',
      count: (detail?.healthRecords?.length ?? 0) > 0 ? detail?.healthRecords?.length : undefined,
    },
    {
      id: 'vitals',
      label: 'Vitals History',
      icon: 'heart-pulse',
      count: metrics.length > 0 ? metrics.length : undefined,
    },
    {
      id: 'documents',
      label: 'Intake & Pedigree',
      icon: 'file-text',
      count: documents.length > 0 ? documents.length : undefined,
    },
  ];

  return (
    <div className="space-y-4">
      {/* Page Header with Compact Interactive Filter Pills (R1) */}
      <header className="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--color-border)] pb-3">
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2.5">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-[var(--color-primary-soft)] text-[var(--color-primary)]">
              <Icon name="stethoscope" size={20} />
            </span>
            <h1 className="text-xl font-bold tracking-tight text-[var(--color-text-primary)]">
              Veterinary Admissions
            </h1>
          </div>

          {/* Compact Interactive Filter Pills */}
          <div className="flex flex-wrap items-center gap-1.5 pl-1 sm:border-l sm:border-[var(--color-border)] sm:pl-3">
            <button
              type="button"
              onClick={() => setQueuePillFilter('ALL')}
              className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px] font-semibold transition-colors ${
                queuePillFilter === 'ALL'
                  ? 'bg-[var(--color-primary)] text-white'
                  : 'bg-[var(--color-surface-muted)] text-[var(--color-text-secondary)] hover:bg-[var(--color-border)]'
              }`}
            >
              <span>Active</span>
              <span className="rounded-full bg-black/15 px-1.5 py-0.2 text-[10px] font-mono">
                {totalCount}
              </span>
            </button>

            <button
              type="button"
              onClick={() => setQueuePillFilter((curr) => (curr === 'AWAITING' ? 'ALL' : 'AWAITING'))}
              className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px] font-semibold transition-colors ${
                queuePillFilter === 'AWAITING'
                  ? 'bg-[var(--color-warning)] text-white'
                  : requestedCount > 0
                    ? 'bg-[var(--color-warning-soft)] text-[var(--color-warning)] hover:opacity-80'
                    : 'bg-[var(--color-surface-muted)] text-[var(--color-text-muted)] hover:bg-[var(--color-border)]'
              }`}
            >
              <span>Awaiting</span>
              <span className="rounded-full bg-black/15 px-1.5 py-0.2 text-[10px] font-mono">
                {requestedCount}
              </span>
            </button>

            <button
              type="button"
              onClick={() => setQueuePillFilter((curr) => (curr === 'IN_PROGRESS' ? 'ALL' : 'IN_PROGRESS'))}
              className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px] font-semibold transition-colors ${
                queuePillFilter === 'IN_PROGRESS'
                  ? 'bg-[var(--color-info)] text-white'
                  : inProgressCount > 0
                    ? 'bg-[var(--color-info-soft)] text-[var(--color-info)] hover:opacity-80'
                    : 'bg-[var(--color-surface-muted)] text-[var(--color-text-muted)] hover:bg-[var(--color-border)]'
              }`}
            >
              <span>In Exam</span>
              <span className="rounded-full bg-black/15 px-1.5 py-0.2 text-[10px] font-mono">
                {inProgressCount}
              </span>
            </button>

            <button
              type="button"
              onClick={() => setQueuePillFilter((curr) => (curr === 'RECHECK' ? 'ALL' : 'RECHECK'))}
              className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px] font-semibold transition-colors ${
                queuePillFilter === 'RECHECK'
                  ? 'bg-[var(--color-danger)] text-white'
                  : rechecksCount > 0
                    ? 'bg-[var(--color-danger-soft)] text-[var(--color-danger)] hover:opacity-80'
                    : 'bg-[var(--color-surface-muted)] text-[var(--color-text-muted)] hover:bg-[var(--color-border)]'
              }`}
            >
              <span>Recheck</span>
              <span className="rounded-full bg-black/15 px-1.5 py-0.2 text-[10px] font-mono">
                {rechecksCount}
              </span>
            </button>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Button
            onClick={handleRefresh}
            disabled={loading}
            icon="refresh"
            variant="secondary"
            size="sm"
          >
            Refresh
          </Button>
        </div>
      </header>

      {/* Global Alerts */}
      {success && (
        <div
          role="status"
          className="flex items-start justify-between gap-2 rounded-[var(--radius-md)] border border-[var(--color-success)] bg-[var(--color-success-soft)] p-3 text-[13px] text-[var(--color-success)]"
        >
          <div className="flex items-start gap-2">
            <Icon name="check" size={16} className="mt-0.5 shrink-0" />
            <span className="font-medium">{success}</span>
          </div>
          <button
            type="button"
            onClick={() => setSuccess('')}
            className="text-[var(--color-success)] hover:opacity-70"
            aria-label="Dismiss alert"
          >
            <Icon name="x" size={14} />
          </button>
        </div>
      )}

      {error && (
        <div
          role="alert"
          className="flex items-start justify-between gap-2 rounded-[var(--radius-md)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] p-3 text-[13px] text-[var(--color-danger)]"
        >
          <div className="flex items-start gap-2">
            <Icon name="alert-triangle" size={16} className="mt-0.5 shrink-0" />
            <span>{error}</span>
          </div>
          <button
            type="button"
            onClick={() => setError('')}
            className="text-[var(--color-danger)] hover:opacity-70"
            aria-label="Dismiss alert"
          >
            <Icon name="x" size={14} />
          </button>
        </div>
      )}

      {scheduleWarning && (
        <div
          role="alert"
          className="flex items-start justify-between gap-2 rounded-[var(--radius-md)] border border-[var(--color-warning)] bg-[var(--color-warning-soft)] p-3 text-[13px] text-[var(--color-warning)]"
        >
          <div className="flex items-start gap-2">
            <Icon name="alert-triangle" size={16} className="mt-0.5 shrink-0" />
            <span>{scheduleWarning}</span>
          </div>
          <button
            type="button"
            onClick={() => setScheduleWarning('')}
            className="text-[var(--color-warning)] hover:opacity-70"
            aria-label="Dismiss warning"
          >
            <Icon name="x" size={14} />
          </button>
        </div>
      )}

      {/* Pending Offers Section at Top of List (R1) */}
      {pendingOffers.length > 0 && (
        <div className="rounded-[var(--radius-lg)] border-2 border-[var(--color-warning)] bg-[var(--color-warning-soft)]/20 p-4">
          <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[var(--color-warning)]/30 pb-3">
            <div className="flex items-center gap-2">
              <span className="flex h-6 w-6 items-center justify-center rounded-full bg-[var(--color-warning-soft)] text-[var(--color-warning)]">
                <Icon name="bell" size={14} />
              </span>
              <h2 className="text-[13px] font-bold text-[var(--color-text-primary)]">
                Pending Care &amp; Examination Offers ({pendingOffers.length})
              </h2>
            </div>
            <span className="text-[11px] text-[var(--color-text-secondary)]">
              Respond before the countdown expires to secure the examination assignment
            </span>
          </div>

          <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {pendingOffers.map((offer) => {
              const countdown = formatCountdown(offer.expiresAt);
              const isExpired = countdown === 'Expired';
              const isActioning = actioningOfferId === offer.id;

              return (
                <div
                  key={offer.id}
                  className="flex flex-col justify-between rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-3.5 shadow-xs"
                >
                  <div>
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <h3 className="text-[13px] font-bold text-[var(--color-text-primary)]">
                          {offer.horseName}
                        </h3>
                        <p className="text-[11px] text-[var(--color-text-secondary)]">
                          {offer.admissionId ? `Admission #${offer.admissionId}` : `Horse #${offer.horseId}`}
                          {offer.breed ? ` · ${offer.breed}` : ''}
                        </p>
                      </div>
                      <Pill tone={offer.careType === 'URGENT' ? 'danger' : 'info'} size="sm">
                        {offer.careType}
                      </Pill>
                    </div>

                    <div className="mt-2 space-y-1 text-[11px] text-[var(--color-text-secondary)]">
                      <div className="flex items-center gap-1.5">
                        <Icon name="calendar" size={12} className="text-[var(--color-text-muted)]" />
                        <span>Proposed: {formatDate(offer.proposedScheduledAt, true)}</span>
                      </div>
                      <div className="flex items-center gap-1.5">
                        <Icon
                          name="clock"
                          size={12}
                          className={isExpired ? 'text-[var(--color-danger)]' : 'text-[var(--color-warning)]'}
                        />
                        <span
                          className={`font-metric font-semibold ${
                            isExpired ? 'text-[var(--color-danger)]' : 'text-[var(--color-warning)]'
                          }`}
                        >
                          Expires in: {countdown}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="mt-3 flex items-center justify-end gap-2 border-t border-[var(--color-border)] pt-2.5">
                    <Button
                      size="sm"
                      variant="secondary"
                      disabled={isActioning || isExpired}
                      loading={isActioning}
                      onClick={() => handleDeclineOffer(offer)}
                    >
                      Decline
                    </Button>
                    <Button
                      size="sm"
                      variant="primary"
                      disabled={isActioning || isExpired}
                      loading={isActioning}
                      icon="check"
                      onClick={() => handleAcceptOffer(offer)}
                    >
                      Accept Offer
                    </Button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Main Full-Width Schedule List Panel (R1) */}
      <Panel className="overflow-hidden">
        {/* Toolbar: Search input & Popover Filters Button */}
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3">
          {/* Visible Search Input */}
          <div className="relative flex-1 min-w-[240px] max-w-md">
            <span className="pointer-events-none absolute left-2.5 top-2.5 text-[var(--color-text-muted)]">
              <Icon name="search" size={14} />
            </span>
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search candidate name, admission #, or breed..."
              className="w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] py-1.5 pl-8 pr-7 text-[12px] text-[var(--color-text-primary)] outline-none transition-colors placeholder:text-[var(--color-text-muted)] focus:border-[var(--color-primary)]"
            />
            {searchQuery && (
              <button
                type="button"
                onClick={() => setSearchQuery('')}
                className="absolute right-2 top-2 text-[var(--color-text-muted)] hover:text-[var(--color-text-primary)]"
                aria-label="Clear search"
              >
                <Icon name="x" size={14} />
              </button>
            )}
          </div>

          {/* Consolidated Popover Filters Button */}
          <div className="relative" ref={filterPopoverRef}>
            <button
              type="button"
              onClick={() => setIsFilterPopoverOpen((prev) => !prev)}
              className={`inline-flex items-center gap-1.5 rounded-[var(--radius-sm)] border px-3 py-1.5 text-[12px] font-medium transition-colors ${
                activePopoverFilterCount > 0 || isFilterPopoverOpen
                  ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)] text-[var(--color-primary)] font-semibold'
                  : 'border-[var(--color-border)] bg-[var(--color-surface)] text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]'
              }`}
            >
              <Icon name="filter" size={14} />
              <span>Filters</span>
              {activePopoverFilterCount > 0 && (
                <span className="flex h-4 w-4 items-center justify-center rounded-full bg-[var(--color-primary)] text-[10px] font-bold text-white">
                  {activePopoverFilterCount}
                </span>
              )}
            </button>

            {/* Filter Popover Dropdown */}
            {isFilterPopoverOpen && (
              <div className="absolute right-0 top-full mt-1.5 z-30 w-72 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-3.5 shadow-lg shadow-black/10 animate-in fade-in duration-150">
                <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-2 mb-2.5">
                  <span className="text-[12px] font-bold text-[var(--color-text-primary)]">
                    Filter Admission Queue
                  </span>
                  {activePopoverFilterCount > 0 && (
                    <button
                      type="button"
                      onClick={() => {
                        setAdmissionFilter('ALL');
                        setExamStatusFilter('ALL');
                        setExamTypeFilter('ALL');
                        setPriorityFilter('ALL');
                      }}
                      className="text-[11px] font-semibold text-[var(--color-primary)] hover:underline"
                    >
                      Reset All
                    </button>
                  )}
                </div>

                <div className="space-y-2.5">
                  <div>
                    <label className="mb-1 block text-[10px] font-bold uppercase tracking-wider text-[var(--color-text-muted)]">
                      Admission Stage
                    </label>
                    <select
                      value={admissionFilter}
                      onChange={(e) => setAdmissionFilter(e.target.value as typeof admissionFilter)}
                      className="w-full rounded border border-[var(--color-border)] bg-[var(--color-surface)] px-2 py-1 text-[11px] text-[var(--color-text-primary)] outline-none focus:border-[var(--color-primary)]"
                    >
                      <option value="ALL">All Stages</option>
                      <option value="VET_REVIEW">Initial Review</option>
                      <option value="PENDING_RECHECK">Pending Recheck</option>
                    </select>
                  </div>

                  <div>
                    <label className="mb-1 block text-[10px] font-bold uppercase tracking-wider text-[var(--color-text-muted)]">
                      Exam Status
                    </label>
                    <select
                      value={examStatusFilter}
                      onChange={(e) => setExamStatusFilter(e.target.value as CareScheduleStatus | 'ALL')}
                      className="w-full rounded border border-[var(--color-border)] bg-[var(--color-surface)] px-2 py-1 text-[11px] text-[var(--color-text-primary)] outline-none focus:border-[var(--color-primary)]"
                    >
                      {examStatusOptions.map((opt) => (
                        <option key={opt.value} value={opt.value}>
                          {opt.label}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="mb-1 block text-[10px] font-bold uppercase tracking-wider text-[var(--color-text-muted)]">
                      Exam Type
                    </label>
                    <select
                      value={examTypeFilter}
                      onChange={(e) => setExamTypeFilter(e.target.value as CareType | 'ALL')}
                      className="w-full rounded border border-[var(--color-border)] bg-[var(--color-surface)] px-2 py-1 text-[11px] text-[var(--color-text-primary)] outline-none focus:border-[var(--color-primary)]"
                    >
                      {examTypeOptions.map((opt) => (
                        <option key={opt.value} value={opt.value}>
                          {opt.label}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="mb-1 block text-[10px] font-bold uppercase tracking-wider text-[var(--color-text-muted)]">
                      Priority Level
                    </label>
                    <select
                      value={priorityFilter}
                      onChange={(e) => setPriorityFilter(e.target.value as typeof priorityFilter)}
                      className="w-full rounded border border-[var(--color-border)] bg-[var(--color-surface)] px-2 py-1 text-[11px] text-[var(--color-text-primary)] outline-none focus:border-[var(--color-primary)]"
                    >
                      <option value="ALL">All Priorities</option>
                      <option value="URGENT">Urgent (P1)</option>
                      <option value="NORMAL">Standard (P2-P4)</option>
                    </select>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Full-Width Admission Schedule List Content */}
        {loading ? (
          <ListSkeleton rows={6} />
        ) : filteredRows.length === 0 ? (
          <div className="p-8">
            <EmptyState
              icon="search"
              title="No matching admissions found"
              description="No candidates match your current search query or active filter settings."
              action={
                <Button
                  size="sm"
                  variant="secondary"
                  onClick={() => {
                    setSearchQuery('');
                    setQueuePillFilter('ALL');
                    setAdmissionFilter('ALL');
                    setExamStatusFilter('ALL');
                    setExamTypeFilter('ALL');
                    setPriorityFilter('ALL');
                  }}
                >
                  Reset All Filters
                </Button>
              }
            />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <ul className="divide-y divide-[var(--color-border)]">
              {filteredRows.map((row) => {
                const priority = getPriorityBadge(row.careSchedule?.careType);
                const stallCode =
                  stallCodes[row.admissionId] ||
                  (row.quarantineStallId ? `Q-Stall #${row.quarantineStallId}` : 'Quarantine');
                const rowVetId = row.careSchedule?.veterinarianId ?? row.careSchedule?.assignedVetId ?? null;
                const isAssignedToUser = Boolean(rowVetId && user?.userId && rowVetId === user.userId);
                const isExamInProgress = row.careSchedule?.status === 'IN_PROGRESS';
                const isScheduled = row.careSchedule?.status === 'SCHEDULED';
                const isCompleted = row.careSchedule?.status === 'COMPLETED';

                return (
                  <li
                    key={row.admissionId}
                    className="flex flex-wrap items-center justify-between gap-4 p-4 transition-colors hover:bg-[var(--color-surface-subtle)]"
                  >
                    {/* Left: Horse Avatar & Metadata */}
                    <div className="flex items-center gap-3.5 min-w-[260px] flex-1">
                      <HorseAvatar
                        name={row.candidateName}
                        image={row.imageUrl}
                        size={46}
                        rounded="md"
                      />

                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="text-[14px] font-bold text-[var(--color-text-primary)] truncate">
                            {row.candidateName}
                          </span>
                          <Pill tone={priority.tone} size="sm" icon={priority.icon}>
                            {priority.label}
                          </Pill>
                          <Pill tone={admissionTone(row.status)} size="sm">
                            {row.status === 'PENDING_RECHECK' ? 'Recheck' : 'Initial Review'}
                          </Pill>
                        </div>

                        <div className="mt-1 flex flex-wrap items-center gap-2 text-[11px] text-[var(--color-text-secondary)]">
                          <span className="font-metric font-semibold text-[var(--color-text-primary)]">
                            #{row.admissionId}
                          </span>
                          <span>·</span>
                          <span className="truncate">{row.breed}</span>
                          <span>·</span>
                          <span>{calculateAge(row.dateOfBirth)}</span>
                          <span>·</span>
                          <span className="inline-flex items-center gap-1 rounded bg-[var(--color-isolated-soft)] px-1.5 py-0.2 text-[10px] font-semibold text-[var(--color-isolated)]">
                            <Icon name="shield" size={10} />
                            {stallCode}
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* Middle: Schedule Status & Time */}
                    <div className="flex flex-wrap items-center gap-4 text-[12px]">
                      <div>
                        {row.careSchedule ? (
                          <Pill
                            tone={examTone(row.careSchedule.status)}
                            size="sm"
                            icon={examIcon(row.careSchedule.status)}
                          >
                            {formatLabel(row.careSchedule.status)}
                          </Pill>
                        ) : (
                          <Pill tone="neutral" size="sm">
                            No Schedule
                          </Pill>
                        )}
                      </div>

                      <div className="min-w-[130px] text-[11px]">
                        {row.careSchedule?.scheduledAt ? (
                          <div className="flex items-center gap-1.5 text-[var(--color-text-secondary)]">
                            <Icon name="calendar" size={12} className="text-[var(--color-text-muted)]" />
                            <span className="font-metric">{formatDate(row.careSchedule.scheduledAt, true)}</span>
                          </div>
                        ) : row.careSchedule?.scheduledDate ? (
                          <span className="text-[var(--color-text-muted)] font-metric">
                            Target: {formatDate(row.careSchedule.scheduledDate)}
                          </span>
                        ) : (
                          <span className="text-[var(--color-text-muted)] italic">Awaiting slot</span>
                        )}
                      </div>

                      <div className="min-w-[110px] text-[11px]">
                        {isAssignedToUser ? (
                          <strong className="text-[var(--color-primary)] font-semibold">
                            Assigned to you
                          </strong>
                        ) : rowVetId ? (
                          <span className="text-[var(--color-text-secondary)]">Vet #{rowVetId}</span>
                        ) : (
                          <span className="text-[var(--color-text-muted)]">Unassigned</span>
                        )}
                      </div>
                    </div>

                    {/* Right: Drawer Trigger Action Button */}
                    <div className="shrink-0">
                      {isExamInProgress ? (
                        <Button
                          size="sm"
                          variant="primary"
                          icon="activity"
                          onClick={() => handleOpenAdmission(row.admissionId)}
                        >
                          Tiếp tục
                        </Button>
                      ) : isScheduled ? (
                        <Button
                          size="sm"
                          variant="primary"
                          icon="stethoscope"
                          onClick={() => handleOpenAdmission(row.admissionId)}
                        >
                          Khám
                        </Button>
                      ) : isCompleted ? (
                        <Button
                          size="sm"
                          variant="secondary"
                          icon="check"
                          onClick={() => handleOpenAdmission(row.admissionId)}
                        >
                          Xem kết quả
                        </Button>
                      ) : (
                        <Button
                          size="sm"
                          variant="secondary"
                          icon="chevron-right"
                          onClick={() => handleOpenAdmission(row.admissionId)}
                        >
                          Chi tiết
                        </Button>
                      )}
                    </div>
                  </li>
                );
              })}
            </ul>
          </div>
        )}
      </Panel>

      {/* 1100px Clinical Drawer (R2) */}
      {drawerOpen && selectedId && (
        <div className="fixed inset-0 z-50 flex justify-end">
          {/* Backdrop with blur */}
          <div
            className="fixed inset-0 bg-black/45 backdrop-blur-sm transition-opacity animate-in fade-in duration-200"
            onClick={handleRequestCloseDrawer}
            aria-hidden="true"
          />

          {/* Drawer Container (1100px on desktop, full-screen sheet on <768px) */}
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="drawer-horse-name"
            className="relative z-50 flex h-full w-full max-w-full flex-col bg-[var(--color-surface)] shadow-2xl transition-transform animate-in slide-in-from-right duration-300 md:max-w-[1100px]"
          >
            {/* Consolidated Header (No duplicate headers, no violet border box) */}
            <div className="border-b border-[var(--color-border)] bg-[var(--color-surface)] px-4 py-3 sm:px-5 sm:py-3.5 shrink-0">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-3">
                  <HorseAvatar
                    name={detail?.candidate?.name ?? selectedRow?.candidateName ?? 'Candidate'}
                    image={selectedRow?.imageUrl}
                    size={42}
                    rounded="md"
                  />
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <h2
                        id="drawer-horse-name"
                        className="text-[17px] font-bold tracking-tight text-[var(--color-text-primary)]"
                      >
                        {detail?.candidate?.name ?? selectedRow?.candidateName ?? 'Candidate'}
                      </h2>
                      {selectedRow && (
                        <Pill
                          tone={getPriorityBadge(selectedRow.careSchedule?.careType).tone}
                          size="sm"
                        >
                          {getPriorityBadge(selectedRow.careSchedule?.careType).label}
                        </Pill>
                      )}
                      {currentActiveSchedule && (
                        <Pill tone={examTone(currentActiveSchedule.status)} size="sm">
                          {formatLabel(currentActiveSchedule.status)}
                        </Pill>
                      )}
                      <span className="inline-flex items-center gap-1 rounded bg-[var(--color-isolated-soft)] px-2 py-0.5 text-[10px] font-bold text-[var(--color-isolated)]">
                        <Icon name="shield" size={10} />
                        {detail?.quarantineStallCode || 'Quarantine Stall'}
                      </span>
                    </div>

                    {/* Consolidated Metadata Line */}
                    <div className="mt-1 flex flex-wrap items-center gap-2 text-[11px] text-[var(--color-text-secondary)]">
                      <span className="font-metric font-semibold text-[var(--color-text-primary)]">
                        Admission #{detail?.admissionId ?? selectedId}
                      </span>
                      {detail?.horseId && <span>· Horse #{detail.horseId}</span>}
                      {detail?.ownerId && <span>· Owner #{detail.ownerId}</span>}
                      <span>· {detail?.candidate?.breed ?? selectedRow?.breed ?? 'Equine'}</span>
                      <span>· {calculateAge(detail?.candidate?.dateOfBirth ?? selectedRow?.dateOfBirth)}</span>
                      {currentActiveSchedule?.scheduledAt && (
                        <span>· Scheduled: {formatDate(currentActiveSchedule.scheduledAt, true)}</span>
                      )}
                    </div>
                  </div>
                </div>

                {/* Header Actions */}
                <div className="flex items-center gap-2">
                  {canStartExam && (
                    <Button
                      size="sm"
                      variant="primary"
                      loading={startingExam}
                      onClick={handleStartExam}
                      icon="activity"
                    >
                      Bắt đầu khám
                    </Button>
                  )}
                  <button
                    type="button"
                    onClick={handleRequestCloseDrawer}
                    className="flex h-11 w-11 min-h-[44px] min-w-[44px] sm:h-8 sm:w-8 sm:min-h-0 sm:min-w-0 items-center justify-center rounded-full text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-text-primary)] transition-colors"
                    aria-label="Close drawer"
                  >
                    <Icon name="x" size={18} />
                  </button>
                </div>
              </div>

              {/* Drawer Tabs */}
              <div className="mt-3">
                <Tabs
                  tabs={workspaceTabs}
                  active={activeTab}
                  onChange={setActiveTab}
                />
              </div>
            </div>

            {/* Drawer Body (Scrollable with sticky decision panel support) */}
            <div className="flex-1 overflow-y-auto p-4 sm:p-5 scroll-slim">
              {loadingDetail || (!detail && !detailError) ? (
                <DetailSkeleton />
              ) : detailError ? (
                <EmptyState
                  icon="alert-triangle"
                  title="Could not load examination record"
                  description={detailError}
                  action={<Button onClick={handleRefresh}>Retry</Button>}
                />
              ) : detail && selectedRow ? (
                <div>
                  {/* Tab 1: Clinical Examination & VetReviewForm */}
                  {activeTab === 'exam' && (
                    <div>
                      {canCompleteExam ? (
                        <VetReviewForm
                          key={`${detail.admissionId}-${currentActiveSchedule?.id}`}
                          admissionId={detail.admissionId}
                          horseId={detail.horseId}
                          candidateName={detail.candidate?.name ?? selectedRow.candidateName}
                          quarantineStallCode={detail.quarantineStallCode}
                          careScheduleId={currentActiveSchedule?.id}
                          careType={currentActiveSchedule?.careType}
                          scheduleStatus={currentActiveSchedule?.status}
                          onStartExam={handleStartExam}
                          onSuccess={handleReviewSuccess}
                          onDirtyChange={handleDirtyChange}
                        />
                      ) : currentActiveSchedule?.status === 'COMPLETED' ? (
                        <div className="rounded-[var(--radius-md)] border border-[var(--color-success)] bg-[var(--color-success-soft)] p-5 text-center">
                          <Icon name="check" size={28} className="mx-auto text-[var(--color-success)]" />
                          <h3 className="mt-2 text-[15px] font-bold text-[var(--color-success)]">
                            Examination Completed
                          </h3>
                          <p className="mt-1 text-[13px] text-[var(--color-text-secondary)]">
                            The veterinary review for this intake stage has already been submitted and finalized.
                          </p>
                          {detail.vetDecision && (
                            <div className="mt-3">
                              <Pill
                                tone={
                                  detail.vetDecision === 'APPROVED'
                                    ? 'success'
                                    : detail.vetDecision === 'RECHECK_REQUIRED'
                                      ? 'warning'
                                      : 'danger'
                                }
                              >
                                Decision: {detail.vetDecision.replace(/_/g, ' ')}
                              </Pill>
                            </div>
                          )}
                        </div>
                      ) : (
                        <div className="p-8 text-center text-[var(--color-text-muted)]">
                          <Icon name="stethoscope" size={32} className="mx-auto mb-2 opacity-40" />
                          <p className="text-[14px] font-semibold text-[var(--color-text-secondary)]">
                            Clinical review locked
                          </p>
                          <p className="text-[12px] mt-1">
                            {currentActiveSchedule?.status === 'REQUESTED' ||
                            currentActiveSchedule?.status === 'AWAITING_VET_CONFIRMATION'
                              ? 'The care schedule must be scheduled and accepted before review documentation begins.'
                              : assignedElsewhere
                                ? 'This case is assigned to another veterinarian.'
                                : 'No active examination found for this admission.'}
                          </p>
                        </div>
                      )}
                    </div>
                  )}

                  {/* Tab 2: Medical Records History */}
                  {activeTab === 'history' && (
                    <div className="space-y-5">
                      <div>
                        <h3 className="text-[13px] font-bold text-[var(--color-text-primary)]">
                          Completed Health Records
                        </h3>
                        {detail.healthRecords && detail.healthRecords.length > 0 ? (
                          <ul className="mt-3 space-y-3">
                            {detail.healthRecords.map((record) => (
                              <li
                                key={record.id}
                                className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-xs"
                              >
                                <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[var(--color-border)] pb-2.5">
                                  <div className="flex items-center gap-2">
                                    <span className="text-[13px] font-bold text-[var(--color-text-primary)]">
                                      {formatLabel(record.recordType)} Exam
                                    </span>
                                    <span className="font-metric text-[11px] text-[var(--color-text-muted)]">
                                      Record #{record.id}
                                    </span>
                                  </div>
                                  <div className="flex items-center gap-2">
                                    {record.vetDecision && (
                                      <Pill
                                        tone={
                                          record.vetDecision === 'APPROVED'
                                            ? 'success'
                                            : record.vetDecision === 'RECHECK_REQUIRED'
                                              ? 'warning'
                                              : 'danger'
                                        }
                                        size="sm"
                                      >
                                        {formatLabel(record.vetDecision)}
                                      </Pill>
                                    )}
                                    <span className="font-metric text-[11px] text-[var(--color-text-secondary)]">
                                      {formatDate(record.examinedAt, true)}
                                    </span>
                                  </div>
                                </div>

                                <div className="mt-3 grid gap-3 sm:grid-cols-2">
                                  <MedicalNoteBlock label="Symptoms" value={record.symptoms} />
                                  <MedicalNoteBlock label="Findings" value={record.findings} />
                                  <MedicalNoteBlock label="Diagnosis" value={record.diagnosis} />
                                  <MedicalNoteBlock label="Treatment Plan" value={record.treatment} />
                                  <MedicalNoteBlock label="Notes & Context" value={record.notes} />
                                  <MedicalNoteBlock label="Rejection Reason" value={record.rejectionReason} />
                                </div>

                                {record.trainingDecision && (
                                  <div className="mt-2.5 flex items-center gap-2">
                                    <span
                                      className={`inline-flex items-center px-2 py-0.5 rounded text-[11px] font-semibold ${
                                        record.trainingDecision === 'ALLOWED'
                                          ? 'bg-[var(--color-success-soft)] text-[var(--color-success)]'
                                          : record.trainingDecision === 'RESTRICTED'
                                            ? 'bg-[var(--color-warning-soft)] text-[var(--color-warning)]'
                                            : 'bg-[var(--color-danger-soft)] text-[var(--color-danger)]'
                                      }`}
                                    >
                                      Training Decision: {record.trainingDecision}
                                    </span>
                                  </div>
                                )}

                                {record.restrictionDetails && (
                                  <div className="mt-2 rounded bg-[var(--color-warning-soft)] p-2 text-[12px] text-[var(--color-warning)]">
                                    <strong>Restriction Protocol:</strong> {record.restrictionDetails}
                                  </div>
                                )}

                                {record.followUpDate && (
                                  <div className="mt-3 flex items-center gap-2 rounded bg-[var(--color-warning-soft)] p-2 text-[12px] font-semibold text-[var(--color-warning)]">
                                    <Icon name="calendar" size={14} />
                                    <span>Mandatory Follow-Up Scheduled: {formatDate(record.followUpDate)}</span>
                                  </div>
                                )}
                              </li>
                            ))}
                          </ul>
                        ) : (
                          <p className="mt-2 text-[12px] text-[var(--color-text-muted)] italic">
                            No health records filed for this candidate yet.
                          </p>
                        )}
                      </div>

                      <div className="border-t border-[var(--color-border)] pt-4">
                        <h3 className="text-[13px] font-bold text-[var(--color-text-primary)]">
                          Examination Schedule Log
                        </h3>
                        {selectedHistory.length > 0 ? (
                          <ol className="mt-3 space-y-2.5">
                            {selectedHistory.map((item, idx) => (
                              <li
                                key={`${item.id}-${idx}`}
                                className="flex items-start gap-3 rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3 text-[12px]"
                              >
                                <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-[var(--color-primary-soft)] text-[10px] font-bold text-[var(--color-primary)]">
                                  {idx + 1}
                                </span>
                                <div className="min-w-0 flex-1">
                                  <div className="flex flex-wrap items-center justify-between gap-2">
                                    <span className="font-bold text-[var(--color-text-primary)]">
                                      {item.label}
                                    </span>
                                    <Pill tone={examTone(item.status)} size="sm">
                                      {formatLabel(item.status)}
                                    </Pill>
                                  </div>
                                  <p className="mt-0.5 text-[11px] text-[var(--color-text-secondary)]">
                                    {item.description}
                                  </p>
                                  <div className="mt-1 flex flex-wrap items-center gap-3 text-[10px] text-[var(--color-text-muted)]">
                                    <span>Scheduled: {item.scheduledAt ? formatDate(item.scheduledAt, true) : 'Pending'}</span>
                                    <span>·</span>
                                    <span>Assigned Vet: {item.assignedVetId ? `#${item.assignedVetId}` : 'Unassigned'}</span>
                                  </div>
                                </div>
                              </li>
                            ))}
                          </ol>
                        ) : (
                          <p className="mt-2 text-[12px] text-[var(--color-text-muted)] italic">
                            No care schedule logs recorded.
                          </p>
                        )}
                      </div>
                    </div>
                  )}

                  {/* Tab 3: Vitals & Health Metrics History */}
                  {activeTab === 'vitals' && (
                    <div className="space-y-4">
                      <div className="flex items-center justify-between">
                        <h3 className="text-[13px] font-bold text-[var(--color-text-primary)]">
                          Recorded Vitals &amp; Telemetry
                        </h3>
                        <span className="text-[11px] text-[var(--color-text-muted)]">
                          {metrics.length} readings on record
                        </span>
                      </div>

                      {metrics.length > 0 ? (
                        <div className="overflow-x-auto rounded-[var(--radius-md)] border border-[var(--color-border)]">
                          <table className="w-full text-left text-[12px]">
                            <thead className="border-b border-[var(--color-border)] bg-[var(--color-surface-subtle)] text-[10px] font-semibold uppercase text-[var(--color-text-secondary)]">
                              <tr>
                                <th className="px-3 py-2">Recorded At</th>
                                <th className="px-3 py-2">Temp</th>
                                <th className="px-3 py-2">Heart Rate</th>
                                <th className="px-3 py-2">Resp Rate</th>
                                <th className="px-3 py-2">Weight</th>
                                <th className="px-3 py-2">BCS</th>
                                <th className="px-3 py-2">Hydration</th>
                                <th className="px-3 py-2">Notes</th>
                              </tr>
                            </thead>
                            <tbody className="divide-y divide-[var(--color-border)] font-metric">
                              {metrics.map((m) => (
                                <tr key={m.id} className="hover:bg-[var(--color-surface-subtle)]">
                                  <td className="px-3 py-2 text-[var(--color-text-primary)]">
                                    {formatDate(m.recordedAt, true)}
                                  </td>
                                  <td className="px-3 py-2">
                                    {m.temperature != null ? `${m.temperature} °C` : '—'}
                                  </td>
                                  <td className="px-3 py-2">
                                    {m.heartRate != null ? `${m.heartRate} bpm` : '—'}
                                  </td>
                                  <td className="px-3 py-2">
                                    {m.respiratoryRate != null ? `${m.respiratoryRate} rpm` : '—'}
                                  </td>
                                  <td className="px-3 py-2">
                                    {m.weight != null ? `${m.weight} kg` : '—'}
                                  </td>
                                  <td className="px-3 py-2">
                                    {m.bodyConditionScore != null ? `${m.bodyConditionScore} / 9` : '—'}
                                  </td>
                                  <td className="px-3 py-2 font-sans">
                                    {m.hydrationStatus || '—'}
                                  </td>
                                  <td className="px-3 py-2 font-sans text-[var(--color-text-secondary)] max-w-xs truncate">
                                    {m.notes || '—'}
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      ) : (
                        <div className="rounded-[var(--radius-md)] border border-dashed border-[var(--color-border)] p-6 text-center text-[var(--color-text-muted)]">
                          <Icon name="heart-pulse" size={24} className="mx-auto mb-1.5 opacity-50" />
                          <p className="text-[12px] font-medium text-[var(--color-text-secondary)]">
                            No telemetry metrics recorded yet
                          </p>
                        </div>
                      )}

                      {latestMetrics && (
                        <div className="rounded-[var(--radius-md)] bg-[var(--color-surface-subtle)] p-3 border border-[var(--color-border)]">
                          <span className="text-[10px] font-bold uppercase tracking-wider text-[var(--color-text-muted)]">
                            Latest Telemetry Baseline ({formatDate(latestMetrics.recordedAt, true)})
                          </span>
                          <div className="mt-2 grid grid-cols-2 gap-2 sm:grid-cols-3 xl:grid-cols-6 font-metric text-[12px]">
                            <div>
                              <span className="text-[9px] text-[var(--color-text-muted)] block font-sans">TEMP</span>
                              <strong>{latestMetrics.temperature ?? '—'} °C</strong>
                            </div>
                            <div>
                              <span className="text-[9px] text-[var(--color-text-muted)] block font-sans">PULSE</span>
                              <strong>{latestMetrics.heartRate ?? '—'} bpm</strong>
                            </div>
                            <div>
                              <span className="text-[9px] text-[var(--color-text-muted)] block font-sans">RESP</span>
                              <strong>{latestMetrics.respiratoryRate ?? '—'} rpm</strong>
                            </div>
                            <div>
                              <span className="text-[9px] text-[var(--color-text-muted)] block font-sans">WEIGHT</span>
                              <strong>{latestMetrics.weight ?? '—'} kg</strong>
                            </div>
                            <div>
                              <span className="text-[9px] text-[var(--color-text-muted)] block font-sans">BCS</span>
                              <strong>{latestMetrics.bodyConditionScore ?? '—'} / 9</strong>
                            </div>
                            <div>
                              <span className="text-[9px] text-[var(--color-text-muted)] block font-sans">HYDRATION</span>
                              <strong className="font-sans text-[11px]">{latestMetrics.hydrationStatus ?? 'Normal'}</strong>
                            </div>
                          </div>
                        </div>
                      )}
                    </div>
                  )}

                  {/* Tab 4: Intake Documents & Pedigree */}
                  {activeTab === 'documents' && (
                    <div className="grid gap-5 xl:grid-cols-2">
                      <div>
                        <h3 className="text-[13px] font-bold text-[var(--color-text-primary)]">
                          Attached Admission Documents
                        </h3>
                        {documents.length > 0 ? (
                          <ul className="mt-3 space-y-2">
                            {documents.map((doc) => (
                              <li
                                key={doc.id}
                                className="flex items-start gap-3 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-3 text-[12px]"
                              >
                                <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded bg-[var(--color-primary-soft)] text-[var(--color-primary)]">
                                  <Icon name="file-text" size={14} />
                                </span>
                                <div className="min-w-0 flex-1">
                                  <a
                                    href={doc.fileUrl}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    className="font-semibold text-[var(--color-primary)] hover:underline truncate block"
                                  >
                                    {formatLabel(doc.documentType)}
                                  </a>
                                  <div className="mt-0.5 flex items-center gap-2 text-[10px] text-[var(--color-text-muted)] font-metric">
                                    {doc.recordDate && <span>Dated: {formatDate(doc.recordDate)}</span>}
                                    <span>Uploaded: {formatDate(doc.uploadedAt)}</span>
                                  </div>
                                </div>
                              </li>
                            ))}
                          </ul>
                        ) : (
                          <p className="mt-2 text-[12px] text-[var(--color-text-muted)] italic">
                            No documents attached.
                          </p>
                        )}
                      </div>

                      <div className="space-y-4">
                        <div className="rounded-[var(--radius-md)] bg-[var(--color-surface-subtle)] p-3.5 border border-[var(--color-border)]">
                          <h4 className="text-[12px] font-bold text-[var(--color-text-primary)]">
                            Pedigree &amp; Registry Information
                          </h4>
                          <dl className="mt-2 space-y-1.5 text-[11px]">
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
                          <h4 className="text-[12px] font-bold text-[var(--color-text-primary)]">
                            Groom Physical Screening
                          </h4>
                          <dl className="mt-2 space-y-1 text-[11px]">
                            <div className="flex justify-between">
                              <span className="text-[var(--color-text-muted)]">Outcome:</span>
                              <span className="font-semibold">{detail.groomDecision ? formatLabel(detail.groomDecision) : 'Pending'}</span>
                            </div>
                            <div className="flex justify-between">
                              <span className="text-[var(--color-text-muted)]">Reviewed At:</span>
                              <span className="font-metric">{formatDate(detail.groomReviewedAt, true)}</span>
                            </div>
                          </dl>
                          {detail.groomFeedback && (
                            <p className="mt-2 rounded bg-[var(--color-surface)] p-2 text-[11px] text-[var(--color-text-secondary)] border border-[var(--color-border)]">
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
          </div>
        </div>
      )}

      {/* Discard Changes Confirmation Dialog for Drawer (R2) */}
      <ConfirmDialog
        open={showCloseConfirm}
        title="Discard Unsaved Clinical Changes?"
        description="You have unsaved changes in this examination form. Closing the drawer now will lose any unrecorded clinical documentation."
        confirmLabel="Discard & Close"
        cancelLabel="Keep Editing"
        tone="danger"
        onConfirm={() => {
          setIsDrawerDirty(false);
          setShowCloseConfirm(false);
          setDrawerOpen(false);
        }}
        onCancel={() => setShowCloseConfirm(false)}
      />
    </div>
  );
}
