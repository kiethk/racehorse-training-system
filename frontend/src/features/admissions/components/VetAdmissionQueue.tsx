'use client';

import { useEffect, useMemo, useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { ApiError } from '@/services/api';
import { Button } from '@/components/ui/Button';
import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { Icon } from '@/components/ui/Icon';
import { MetricCard } from '@/components/ui/MetricCard';
import { Panel, SectionTitle } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { Tabs, type TabItem } from '@/components/ui/Tabs';
import { DetailSkeleton, EmptyState, ListSkeleton } from '@/components/ui/states';
import { admissionsApi } from '../services/api';
import type {
  AdmissionDetailResponse,
  AdmissionDocument,
  AdmissionStatus,
  AdmissionSummaryResponse,
  HorseHealthMetricResponse,
  VetExamResponse,
  VetExamStatus,
  VetExamType,
  VetReviewResponse,
} from '../types';
import { VetReviewForm } from './VetReviewForm';

type QueueRow = AdmissionSummaryResponse & { exam: VetExamResponse | null };

const examStatusOptions: Array<{ value: VetExamStatus | 'ALL'; label: string }> = [
  { value: 'ALL', label: 'All Exam States' },
  { value: 'REQUESTED', label: 'Requested' },
  { value: 'SCHEDULED', label: 'Scheduled' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'COMPLETED', label: 'Completed' },
];

const examTypeOptions: Array<{ value: VetExamType | 'ALL'; label: string }> = [
  { value: 'ALL', label: 'All Exam Types' },
  { value: 'INITIAL', label: 'Initial Exam' },
  { value: 'FOLLOW_UP', label: 'Follow-Up Recheck' },
  { value: 'URGENT', label: 'Urgent Exam' },
  { value: 'ROUTINE', label: 'Routine Exam' },
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
  return `${years} yo (${years} ${years === 1 ? 'yr' : 'yrs'})`;
}

function examTone(status: VetExamStatus | undefined) {
  if (status === 'COMPLETED') return 'success' as const;
  if (status === 'IN_PROGRESS') return 'primary' as const;
  if (status === 'SCHEDULED') return 'info' as const;
  if (status === 'REQUESTED') return 'warning' as const;
  return 'neutral' as const;
}

function examIcon(status: VetExamStatus | undefined) {
  if (status === 'COMPLETED') return 'check' as const;
  if (status === 'IN_PROGRESS') return 'activity' as const;
  if (status === 'SCHEDULED') return 'calendar' as const;
  if (status === 'REQUESTED') return 'clock' as const;
  return 'circle' as const;
}

function admissionTone(status: AdmissionStatus) {
  if (status === 'PENDING_RECHECK') return 'warning' as const;
  if (status === 'APPROVED') return 'success' as const;
  if (status === 'REJECTED') return 'danger' as const;
  return 'info' as const;
}

function getPriorityBadge(priority: number | undefined, examType: VetExamType | undefined) {
  if (examType === 'URGENT' || (priority && priority >= 400)) {
    return { label: 'P1 · Urgent', tone: 'danger' as const, icon: 'alert-triangle' as const };
  }
  if (examType === 'INITIAL' || (priority && priority >= 300)) {
    return { label: 'P2 · Initial', tone: 'info' as const, icon: 'stethoscope' as const };
  }
  if (examType === 'FOLLOW_UP' || (priority && priority >= 200)) {
    return { label: 'P3 · Recheck', tone: 'warning' as const, icon: 'calendar' as const };
  }
  if (priority && priority >= 100) {
    return { label: 'P4 · Routine', tone: 'neutral' as const, icon: 'clock' as const };
  }
  return { label: 'P— · Standard', tone: 'neutral' as const, icon: 'clock' as const };
}

function InfoItem({
  label,
  value,
  subvalue,
}: {
  label: string;
  value: string | number | null | undefined;
  subvalue?: string | null;
}) {
  return (
    <div>
      <dt className="text-[10px] font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">
        {label}
      </dt>
      <dd className="mt-1 text-[13px] font-medium text-[var(--color-text-primary)]">
        {value ?? <span className="text-[var(--color-text-muted)] italic">Not available</span>}
        {subvalue && (
          <span className="block text-[11px] font-normal text-[var(--color-text-secondary)]">
            {subvalue}
          </span>
        )}
      </dd>
    </div>
  );
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
  const [exams, setExams] = useState<VetExamResponse[]>([]);
  const [stallCodes, setStallCodes] = useState<Record<number, string>>({});
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [detail, setDetail] = useState<AdmissionDetailResponse | null>(null);
  const [documents, setDocuments] = useState<AdmissionDocument[]>([]);
  const [metrics, setMetrics] = useState<HorseHealthMetricResponse[]>([]);
  const [activeTab, setActiveTab] = useState<string>('exam');

  // Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [admissionFilter, setAdmissionFilter] = useState<'ALL' | 'VET_REVIEW' | 'PENDING_RECHECK'>('ALL');
  const [examStatusFilter, setExamStatusFilter] = useState<VetExamStatus | 'ALL'>('ALL');
  const [examTypeFilter, setExamTypeFilter] = useState<VetExamType | 'ALL'>('ALL');
  const [priorityFilter, setPriorityFilter] = useState<'ALL' | 'URGENT' | 'NORMAL'>('ALL');

  // Request & operation state
  const [loading, setLoading] = useState(true);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [startingExam, setStartingExam] = useState(false);
  const [error, setError] = useState('');
  const [detailError, setDetailError] = useState('');
  const [success, setSuccess] = useState('');
  const [reload, setReload] = useState(0);

  // Initial load of queues
  useEffect(() => {
    let active = true;
    Promise.all([
      admissionsApi.getAdmissions('VET_REVIEW'),
      admissionsApi.getAdmissions('PENDING_RECHECK'),
      admissionsApi.getVetExams({ size: 50 }),
    ])
      .then(([initial, rechecks, examPage]) => {
        if (!active) return;
        const nextAdmissions = [...initial, ...rechecks].filter(
          (row, index, all) => all.findIndex((candidate) => candidate.admissionId === row.admissionId) === index,
        );
        setAdmissions(nextAdmissions);
        setExams(examPage.content);
        setSelectedId((current) =>
          nextAdmissions.some((row) => row.admissionId === current)
            ? current
            : nextAdmissions[0]?.admissionId ?? null,
        );
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

  // Merge admissions with exams and sort by business priority
  const rows = useMemo<QueueRow[]>(() => {
    return admissions
      .map((admission) => {
        const related = exams
          .filter((exam) => exam.admissionId === admission.admissionId)
          .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        const activeExam = related.find((exam) => !['COMPLETED', 'CANCELLED'].includes(exam.status));
        return { ...admission, exam: activeExam ?? related[0] ?? null };
      })
      .sort((a, b) => {
        const priorityDiff = (b.exam?.priority ?? 0) - (a.exam?.priority ?? 0);
        return priorityDiff || new Date(a.submittedAt).getTime() - new Date(b.submittedAt).getTime();
      });
  }, [admissions, exams]);

  // Apply search query and filters
  const filteredRows = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    return rows.filter((row) => {
      // Search filter: candidate name, admissionId, breed
      if (query) {
        const matchesName = row.candidateName.toLowerCase().includes(query);
        const matchesId = String(row.admissionId).includes(query);
        const matchesBreed = row.breed.toLowerCase().includes(query);
        if (!matchesName && !matchesId && !matchesBreed) return false;
      }

      // Admission stage filter
      if (admissionFilter !== 'ALL' && row.status !== admissionFilter) return false;

      // Exam status filter
      if (examStatusFilter !== 'ALL' && row.exam?.status !== examStatusFilter) return false;

      // Exam type filter
      if (examTypeFilter !== 'ALL' && row.exam?.examType !== examTypeFilter) return false;

      // Priority filter
      if (priorityFilter === 'URGENT') {
        const isUrgent = row.exam?.examType === 'URGENT' || (row.exam?.priority ?? 0) >= 400;
        if (!isUrgent) return false;
      } else if (priorityFilter === 'NORMAL') {
        const isUrgent = row.exam?.examType === 'URGENT' || (row.exam?.priority ?? 0) >= 400;
        if (isUrgent) return false;
      }

      return true;
    });
  }, [rows, searchQuery, admissionFilter, examStatusFilter, examTypeFilter, priorityFilter]);

  // Derive selected admission ID directly from filteredRows without triggering cascading renders
  const selectedAdmissionId = useMemo(() => {
    if (selectedId !== null && filteredRows.some((row) => row.admissionId === selectedId)) {
      return selectedId;
    }
    return filteredRows[0]?.admissionId ?? null;
  }, [filteredRows, selectedId]);

  // Fetch admission details, documents, and historical health metrics
  useEffect(() => {
    if (selectedAdmissionId === null) return;
    let active = true;
    /* eslint-disable react-hooks/set-state-in-effect -- reset the visible request state when the selected remote resource changes */
    setLoadingDetail(true);
    setDetailError('');
    setDetail(null);
    setDocuments([]);
    setMetrics([]);
    /* eslint-enable react-hooks/set-state-in-effect */

    Promise.all([
      admissionsApi.getAdmissionDetail(selectedAdmissionId),
      admissionsApi.getDocuments(selectedAdmissionId),
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
  }, [selectedAdmissionId, reload]);

  const selectedRow = rows.find((row) => row.admissionId === selectedAdmissionId) ?? null;
  const selectedExam = selectedRow?.exam ?? null;
  const selectedExamHistory = exams
    .filter((exam) => exam.admissionId === selectedAdmissionId)
    .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());

  const assignedElsewhere = Boolean(
    selectedExam?.assignedVetId && user?.userId && selectedExam.assignedVetId !== user.userId,
  );
  const canCompleteExam = Boolean(
    selectedExam && ['SCHEDULED', 'IN_PROGRESS'].includes(selectedExam.status) && !assignedElsewhere,
  );
  const latestMetrics = metrics[0] ?? null;

  function handleSelectAdmission(id: number) {
    if (id !== selectedId) {
      setSelectedId(id);
      setActiveTab('exam');
    }
  }

  function handleRefresh() {
    setSuccess('');
    setError('');
    setLoading(true);
    setReload((curr) => curr + 1);
  }

  async function handleStartExam() {
    if (!selectedExam || startingExam) return;
    try {
      setStartingExam(true);
      setError('');
      await admissionsApi.startVetExam(selectedExam.id);
      setSuccess(`Examination #${selectedExam.id} started. You can now record clinical findings.`);
      setReload((curr) => curr + 1);
    } catch (cause) {
      setError(errorText(cause));
    } finally {
      setStartingExam(false);
    }
  }

  function handleReviewSuccess(result: VetReviewResponse) {
    const outcomeMessage =
      result.decision === 'APPROVED'
        ? 'Veterinary review approved! The admission moved to Trainer review; the horse remains a quarantined CANDIDATE.'
        : result.decision === 'RECHECK_REQUIRED'
          ? 'Recheck scheduled! Horse remains in quarantine with training locked until follow-up.'
          : 'Admission rejected! Quarantine stall released and training lock retained.';
    setSuccess(`Admission #${result.admissionId} (${selectedRow?.candidateName ?? 'Horse'}): ${outcomeMessage}`);
    setReload((curr) => curr + 1);
  }

  // Top metric stats calculation
  const totalCount = rows.length;
  const requestedCount = rows.filter((r) => r.exam?.status === 'REQUESTED').length;
  const inProgressCount = rows.filter((r) => r.exam?.status === 'IN_PROGRESS').length;
  const rechecksCount = rows.filter((r) => r.status === 'PENDING_RECHECK').length;

  // Tab configuration for detail workspace
  const workspaceTabs: TabItem[] = [
    { id: 'exam', label: 'Clinical Examination', icon: 'stethoscope' },
    {
      id: 'history',
      label: 'Medical Records',
      icon: 'clipboard',
      count: detail?.healthRecords?.length ?? 0,
    },
    {
      id: 'vitals',
      label: 'Vitals History',
      icon: 'heart-pulse',
      count: metrics.length,
    },
    {
      id: 'documents',
      label: 'Intake & Pedigree',
      icon: 'file-text',
      count: documents.length,
    },
  ];

  return (
    <div className="space-y-4">
      {/* Workspace Header */}
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-2.5">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-[var(--color-primary-soft)] text-[var(--color-primary)]">
              <Icon name="stethoscope" size={20} />
            </span>
            <h1 className="text-2xl font-bold tracking-tight text-[var(--color-text-primary)]">
              Veterinary Admissions
            </h1>
            <span className="inline-flex items-center gap-1 rounded-full bg-[var(--color-primary-soft)] px-2.5 py-0.5 text-[11px] font-semibold text-[var(--color-primary)]">
              Clinical Workspace
            </span>
          </div>
          <p className="mt-1 text-[13px] text-[var(--color-text-secondary)]">
            Manage initial quarantine intake exams, evaluate biosecurity vitals, and issue clearance decisions.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            onClick={handleRefresh}
            disabled={loading}
            icon="refresh"
            variant="secondary"
            size="sm"
          >
            Refresh Queue
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

      {/* Metric Cards Row */}
      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
        <MetricCard
          label="ACTIVE INTAKE"
          value={totalCount}
          unit="horses in queue"
          icon="horse"
          tone="neutral"
        />
        <MetricCard
          label="AWAITING SCHEDULE"
          value={requestedCount}
          unit="exams requested"
          icon="clock"
          tone="warning"
        />
        <MetricCard
          label="IN EXAMINATION"
          value={inProgressCount}
          unit="active physicals"
          icon="stethoscope"
          tone="info"
        />
        <MetricCard
          label="PENDING RECHECK"
          value={rechecksCount}
          unit="quarantine re-tests"
          icon="calendar"
          tone="danger"
        />
      </div>

      {/* Main Two-Column Master-Detail Layout */}
      {loading ? (
        <Panel>
          <ListSkeleton rows={6} />
        </Panel>
      ) : rows.length === 0 ? (
        <Panel padded>
          <EmptyState
            icon="check"
            title="Veterinary Admission Queue is Clear"
            description="There are currently no horses awaiting initial quarantine examination or follow-up recheck."
          />
        </Panel>
      ) : (
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start">
          {/* Left Column: Vet Queue (30% width on desktop) */}
          <div className="w-full shrink-0 lg:w-[32%] xl:w-[28%]">
            <Panel className="flex max-h-[820px] flex-col overflow-hidden">
              {/* Queue Header & Result Count */}
              <div className="flex items-center justify-between border-b border-[var(--color-border)] px-4 py-3 shrink-0">
                <div className="flex items-center gap-2">
                  <SectionTitle>Examination Queue</SectionTitle>
                  <span className="font-metric rounded-full bg-[var(--color-surface-muted)] px-2 py-0.5 text-[10px] font-semibold text-[var(--color-text-secondary)]">
                    {filteredRows.length} of {rows.length}
                  </span>
                </div>
              </div>

              {/* Search Bar */}
              <div className="border-b border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3 shrink-0">
                <div className="relative">
                  <span className="pointer-events-none absolute left-2.5 top-2.5 text-[var(--color-text-muted)]">
                    <Icon name="search" size={14} />
                  </span>
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="Search candidate name or ID…"
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

                {/* Filter Controls Row */}
                <div className="mt-2 grid grid-cols-2 gap-2">
                  <select
                    value={admissionFilter}
                    onChange={(e) =>
                      setAdmissionFilter(e.target.value as typeof admissionFilter)
                    }
                    className="rounded border border-[var(--color-border)] bg-[var(--color-surface)] px-2 py-1 text-[11px] text-[var(--color-text-primary)] outline-none focus:border-[var(--color-primary)]"
                    aria-label="Admission Stage"
                  >
                    <option value="ALL">All Stages</option>
                    <option value="VET_REVIEW">Initial Review</option>
                    <option value="PENDING_RECHECK">Pending Recheck</option>
                  </select>

                  <select
                    value={examStatusFilter}
                    onChange={(e) =>
                      setExamStatusFilter(e.target.value as VetExamStatus | 'ALL')
                    }
                    className="rounded border border-[var(--color-border)] bg-[var(--color-surface)] px-2 py-1 text-[11px] text-[var(--color-text-primary)] outline-none focus:border-[var(--color-primary)]"
                    aria-label="Exam Status"
                  >
                    {examStatusOptions.map((opt) => (
                      <option key={opt.value} value={opt.value}>
                        {opt.label}
                      </option>
                    ))}
                  </select>

                  <select
                    value={examTypeFilter}
                    onChange={(e) =>
                      setExamTypeFilter(e.target.value as VetExamType | 'ALL')
                    }
                    className="rounded border border-[var(--color-border)] bg-[var(--color-surface)] px-2 py-1 text-[11px] text-[var(--color-text-primary)] outline-none focus:border-[var(--color-primary)]"
                    aria-label="Exam Type"
                  >
                    {examTypeOptions.map((opt) => (
                      <option key={opt.value} value={opt.value}>
                        {opt.label}
                      </option>
                    ))}
                  </select>

                  <select
                    value={priorityFilter}
                    onChange={(e) =>
                      setPriorityFilter(e.target.value as typeof priorityFilter)
                    }
                    className="rounded border border-[var(--color-border)] bg-[var(--color-surface)] px-2 py-1 text-[11px] text-[var(--color-text-primary)] outline-none focus:border-[var(--color-primary)]"
                    aria-label="Priority Filter"
                  >
                    <option value="ALL">All Priorities</option>
                    <option value="URGENT">Urgent (P1)</option>
                    <option value="NORMAL">Standard (P2-P4)</option>
                  </select>
                </div>
              </div>

              {/* Scrollable Queue List */}
              <div className="overflow-y-auto scroll-slim flex-1">
                {filteredRows.length > 0 ? (
                  <ul className="divide-y divide-[var(--color-border)]">
                    {filteredRows.map((row) => {
                      const isSelected = selectedAdmissionId === row.admissionId;
                      const priority = getPriorityBadge(row.exam?.priority, row.exam?.examType);
                      const stallCode =
                        stallCodes[row.admissionId] ||
                        (row.quarantineStallId ? `Q-Stall #${row.quarantineStallId}` : 'Quarantine');
                      const isAssignedToUser =
                        row.exam?.assignedVetId && user?.userId && row.exam.assignedVetId === user.userId;

                      return (
                        <li key={row.admissionId}>
                          <button
                            type="button"
                            onClick={() => handleSelectAdmission(row.admissionId)}
                            aria-current={isSelected ? 'true' : undefined}
                            className={`flex w-full text-left gap-3 px-3.5 py-3 transition-colors outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-[var(--color-focus)] ${
                              isSelected
                                ? 'border-l-4 border-l-[var(--color-primary)] bg-[var(--color-primary-subtle)]'
                                : 'border-l-4 border-l-transparent hover:bg-[var(--color-surface-subtle)]'
                            }`}
                          >
                            <HorseAvatar
                              name={row.candidateName}
                              image={row.imageUrl}
                              size={42}
                              rounded="md"
                            />

                            <div className="min-w-0 flex-1">
                              {/* Row 1: Name and Priority */}
                              <div className="flex items-center justify-between gap-1.5">
                                <span className="truncate text-[13px] font-bold text-[var(--color-text-primary)]">
                                  {row.candidateName}
                                </span>
                                <Pill tone={priority.tone} size="sm" icon={priority.icon}>
                                  {priority.label}
                                </Pill>
                              </div>

                              {/* Row 2: Admission Code, Breed & Quarantine Stall */}
                              <div className="mt-1 flex flex-wrap items-center gap-1.5 text-[11px] text-[var(--color-text-secondary)]">
                                <span className="font-metric font-medium text-[var(--color-text-primary)]">
                                  #{row.admissionId}
                                </span>
                                <span>·</span>
                                <span className="truncate">{row.breed}</span>
                                <span>·</span>
                                <span className="inline-flex items-center gap-1 rounded bg-[var(--color-isolated-soft)] px-1.5 py-0.2 text-[10px] font-semibold text-[var(--color-isolated)]">
                                  <Icon name="shield" size={10} />
                                  {stallCode}
                                </span>
                              </div>

                              {/* Row 3: Status Pills */}
                              <div className="mt-2 flex flex-wrap items-center gap-1.5">
                                <Pill tone={admissionTone(row.status)} size="sm">
                                  {row.status === 'PENDING_RECHECK' ? 'Recheck Stage' : 'Initial Intake'}
                                </Pill>

                                {row.exam ? (
                                  <Pill tone={examTone(row.exam.status)} size="sm" icon={examIcon(row.exam.status)}>
                                    {formatLabel(row.exam.status)}
                                  </Pill>
                                ) : (
                                  <Pill tone="neutral" size="sm">
                                    No exam linked
                                  </Pill>
                                )}

                                {/* Training Locked Indicator */}
                                <span className="inline-flex items-center gap-1 rounded bg-[var(--color-danger-soft)] px-1.5 py-0.5 text-[10px] font-semibold text-[var(--color-danger)]">
                                  <Icon name="lock" size={10} />
                                  Locked
                                </span>
                              </div>

                              {/* Row 4: Assigned Vet & Scheduled Info */}
                              <div className="mt-1.5 flex items-center justify-between text-[10px] text-[var(--color-text-muted)]">
                                <span className="truncate">
                                  {isAssignedToUser ? (
                                    <strong className="text-[var(--color-primary)]">
                                      Assigned to you
                                    </strong>
                                  ) : row.exam?.assignedVetId ? (
                                    `Vet #${row.exam.assignedVetId}`
                                  ) : (
                                    'Vet unassigned'
                                  )}
                                </span>
                                {row.exam?.scheduledAt ? (
                                  <span className="flex items-center gap-1 font-metric">
                                    <Icon name="calendar" size={10} />
                                    {formatDate(row.exam.scheduledAt, true)}
                                  </span>
                                ) : row.exam?.requestedForDate ? (
                                  <span className="font-metric">
                                    Target: {formatDate(row.exam.requestedForDate)}
                                  </span>
                                ) : null}
                              </div>
                            </div>
                          </button>
                        </li>
                      );
                    })}
                  </ul>
                ) : (
                  <div className="p-6">
                    <EmptyState
                      icon="search"
                      title="No matching admissions"
                      description="Try clearing your search term or broadening the status and priority filters."
                      action={
                        <Button
                          size="sm"
                          variant="secondary"
                          onClick={() => {
                            setSearchQuery('');
                            setAdmissionFilter('ALL');
                            setExamStatusFilter('ALL');
                            setExamTypeFilter('ALL');
                            setPriorityFilter('ALL');
                          }}
                        >
                          Reset Filters
                        </Button>
                      }
                    />
                  </div>
                )}
              </div>
            </Panel>
          </div>

          {/* Right Column: Clinical Workspace (70% width on desktop) */}
          <div className="min-w-0 flex-1">
            {loadingDetail || (!detail && !detailError) ? (
              <Panel>
                <DetailSkeleton />
              </Panel>
            ) : detailError ? (
              <Panel padded>
                <EmptyState
                  icon="alert-triangle"
                  title="Could not load clinical details"
                  description={detailError}
                  action={<Button onClick={handleRefresh}>Retry</Button>}
                />
              </Panel>
            ) : detail && selectedRow ? (
              <div className="space-y-4">
                {/* Horse Clinical Summary Header */}
                <Panel className="overflow-hidden">
                  <div className="flex flex-wrap items-start justify-between gap-4 border-b border-[var(--color-border)] p-5">
                    <div className="flex items-start gap-4">
                      <HorseAvatar
                        name={detail.candidate?.name ?? selectedRow.candidateName}
                        image={selectedRow.imageUrl}
                        size={68}
                        rounded="md"
                      />
                      <div>
                        <div className="flex flex-wrap items-center gap-2">
                          <h2 className="text-[22px] font-bold tracking-tight text-[var(--color-text-primary)]">
                            {detail.candidate?.name ?? selectedRow.candidateName}
                          </h2>
                          <Pill tone={admissionTone(detail.status)}>
                            {formatLabel(detail.status)}
                          </Pill>
                          {selectedExam && (
                            <Pill tone={examTone(selectedExam.status)} icon={examIcon(selectedExam.status)}>
                              {formatLabel(selectedExam.status)}
                            </Pill>
                          )}
                          {selectedExam && (
                            <Pill
                              tone={getPriorityBadge(selectedExam.priority, selectedExam.examType).tone}
                              icon={getPriorityBadge(selectedExam.priority, selectedExam.examType).icon}
                            >
                              {getPriorityBadge(selectedExam.priority, selectedExam.examType).label}
                            </Pill>
                          )}
                        </div>

                        <div className="mt-1 flex flex-wrap items-center gap-2 text-[12px] text-[var(--color-text-secondary)]">
                          <span className="font-metric font-semibold text-[var(--color-text-primary)]">
                            Admission #{detail.admissionId}
                          </span>
                          <span>·</span>
                          <span>
                            Horse: {detail.horseId ? `#${detail.horseId}` : 'Intake profile'}
                          </span>
                          <span>·</span>
                          <span>{detail.candidate?.breed ?? 'Breed unspecified'}</span>
                          <span>·</span>
                          <span>{calculateAge(detail.candidate?.dateOfBirth)}</span>
                          {detail.candidate?.registrationNumber && (
                            <>
                              <span>·</span>
                              <span>Reg: {detail.candidate.registrationNumber}</span>
                            </>
                          )}
                        </div>
                      </div>
                    </div>

                    {/* Prominent Training Lock & Quarantine Banner */}
                    <div className="flex flex-col items-end gap-2">
                      <div className="flex items-center gap-2">
                        <span className="inline-flex items-center gap-1.5 rounded-full bg-[var(--color-isolated-soft)] px-3 py-1 text-[12px] font-bold text-[var(--color-isolated)]">
                          <Icon name="shield" size={14} />
                          {detail.quarantineStallCode ? `Quarantine Stall: ${detail.quarantineStallCode}` : 'Quarantine Facility'}
                        </span>
                        <span className="inline-flex items-center gap-1.5 rounded-full bg-[var(--color-danger-soft)] px-3 py-1 text-[12px] font-bold text-[var(--color-danger)]">
                          <Icon name="lock" size={14} />
                          Training Strictly Locked
                        </span>
                      </div>
                      <p className="text-[11px] text-[var(--color-text-muted)] text-right max-w-xs">
                        Training suspension active until a veterinarian renders a final clinical approval.
                      </p>
                    </div>
                  </div>

                  {/* Summary Details Grid */}
                  <dl className="grid grid-cols-2 gap-4 bg-[var(--color-surface-subtle)] px-5 py-4 sm:grid-cols-3 xl:grid-cols-6">
                    <InfoItem label="Owner" value={`#${detail.ownerId}`} />
                    <InfoItem
                      label="Date of Birth"
                      value={formatDate(detail.candidate?.dateOfBirth)}
                    />
                    <InfoItem
                      label="Quarantine Stall"
                      value={detail.quarantineStallCode || (detail.quarantineStallId ? `#${detail.quarantineStallId}` : null)}
                    />
                    <InfoItem
                      label="Exam Type"
                      value={selectedExam ? formatLabel(selectedExam.examType) : null}
                    />
                    <InfoItem
                      label="Assigned Vet"
                      value={
                        selectedExam?.assignedVetId
                          ? selectedExam.assignedVetId === user?.userId
                            ? 'Dr. (You)'
                            : `#${selectedExam.assignedVetId}`
                          : 'Unassigned'
                      }
                    />
                    <InfoItem
                      label="Exam Scheduled"
                      value={formatDate(selectedExam?.scheduledAt, true)}
                    />
                  </dl>
                </Panel>

                {/* Quick Action & Status Advisory Bar */}
                {selectedExam?.status === 'SCHEDULED' && !assignedElsewhere && (
                  <div className="flex flex-wrap items-center justify-between gap-3 rounded-[var(--radius-md)] border border-[var(--color-info)] bg-[var(--color-info-soft)] p-4">
                    <div className="flex items-center gap-3">
                      <Icon name="clock" size={20} className="text-[var(--color-info)]" />
                      <div>
                        <h3 className="text-[13px] font-bold text-[var(--color-text-primary)]">
                          Examination Scheduled
                        </h3>
                        <p className="text-[12px] text-[var(--color-text-secondary)]">
                          Scheduled for {formatDate(selectedExam.scheduledAt, true)}. You can begin the physical examination now or record the results directly.
                        </p>
                      </div>
                    </div>
                    <Button
                      size="sm"
                      variant="primary"
                      loading={startingExam}
                      onClick={handleStartExam}
                      icon="activity"
                    >
                      Start Examination Now
                    </Button>
                  </div>
                )}

                {selectedExam?.status === 'REQUESTED' && (
                  <div className="flex items-start gap-3 rounded-[var(--radius-md)] border border-[var(--color-warning)] bg-[var(--color-warning-soft)] p-4">
                    <Icon name="clock" size={20} className="mt-0.5 shrink-0 text-[var(--color-warning)]" />
                    <div>
                      <h3 className="text-[13px] font-bold text-[var(--color-text-primary)]">
                        Awaiting Exam Scheduling
                      </h3>
                      <p className="mt-0.5 text-[12px] text-[var(--color-text-secondary)]">
                        This exam request is queued for automatic scheduling. Once scheduled and assigned, the clinical entry form unlocks.
                      </p>
                    </div>
                  </div>
                )}

                {assignedElsewhere && (
                  <div className="flex items-start gap-3 rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface-muted)] p-4">
                    <Icon name="user" size={20} className="mt-0.5 shrink-0 text-[var(--color-text-secondary)]" />
                    <div>
                      <h3 className="text-[13px] font-bold text-[var(--color-text-primary)]">
                        Assigned to Veterinarian #{selectedExam?.assignedVetId}
                      </h3>
                      <p className="mt-0.5 text-[12px] text-[var(--color-text-secondary)]">
                        You have read-only access to this candidate&apos;s clinical records and history. Only the assigned veterinarian can submit the final review.
                      </p>
                    </div>
                  </div>
                )}

                {/* Workspace Underlined Tabs */}
                <Panel className="overflow-hidden">
                  <div className="px-4 pt-2">
                    <Tabs
                      tabs={workspaceTabs}
                      active={activeTab}
                      onChange={setActiveTab}
                    />
                  </div>

                  <div className="p-5">
                    {/* Tab 1: Clinical Examination & Review Form */}
                    {activeTab === 'exam' && (
                      <div className="space-y-5">
                        {canCompleteExam ? (
                          <VetReviewForm
                            key={`${detail.admissionId}-${selectedExam?.id}`}
                            admissionId={detail.admissionId}
                            candidateName={detail.candidate?.name ?? selectedRow.candidateName}
                            quarantineStallCode={detail.quarantineStallCode}
                            onSuccess={handleReviewSuccess}
                          />
                        ) : selectedExam?.status === 'COMPLETED' ? (
                          <div className="rounded-[var(--radius-md)] border border-[var(--color-success)] bg-[var(--color-success-soft)] p-5 text-center">
                            <Icon name="check" size={28} className="mx-auto text-[var(--color-success)]" />
                            <h3 className="mt-2 text-[15px] font-bold text-[var(--color-success)]">
                              Examination Completed
                            </h3>
                            <p className="mt-1 text-[13px] text-[var(--color-text-secondary)]">
                              The veterinary review for this stage has already been submitted and finalized.
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
                          <div className="p-6 text-center text-[var(--color-text-muted)]">
                            <Icon name="stethoscope" size={32} className="mx-auto mb-2 opacity-40" />
                            <p className="text-[14px] font-semibold text-[var(--color-text-secondary)]">
                              Clinical form locked
                            </p>
                            <p className="text-[12px] mt-1">
                              {selectedExam?.status === 'REQUESTED'
                                ? 'The exam request must be scheduled before review documentation begins.'
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
                      <div className="space-y-6">
                        {/* Completed Health Records */}
                        <div>
                          <SectionTitle>Completed Health Records</SectionTitle>
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
                            <p className="mt-2 text-[13px] text-[var(--color-text-muted)] italic">
                              No health records have been filed for this horse yet.
                            </p>
                          )}
                        </div>

                        {/* Examination Schedule Timeline */}
                        <div className="border-t border-[var(--color-border)] pt-5">
                          <SectionTitle>Examination Schedule &amp; Log</SectionTitle>
                          {selectedExamHistory.length > 0 ? (
                            <ol className="mt-3 space-y-3">
                              {selectedExamHistory.map((exam, idx) => (
                                <li
                                  key={exam.id}
                                  className="flex items-start gap-3 rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3"
                                >
                                  <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-[var(--color-primary-soft)] text-[11px] font-bold text-[var(--color-primary)]">
                                    {idx + 1}
                                  </span>
                                  <div className="min-w-0 flex-1">
                                    <div className="flex flex-wrap items-center justify-between gap-2">
                                      <span className="text-[13px] font-bold text-[var(--color-text-primary)]">
                                        {formatLabel(exam.examType)} Examination #{exam.id}
                                      </span>
                                      <Pill tone={examTone(exam.status)} size="sm">
                                        {formatLabel(exam.status)}
                                      </Pill>
                                    </div>
                                    <p className="mt-1 text-[12px] text-[var(--color-text-secondary)]">
                                      {exam.reason || 'Veterinary intake examination'} · Duration: {exam.durationMinutes} min
                                    </p>
                                    <div className="mt-1 flex flex-wrap items-center gap-3 text-[11px] text-[var(--color-text-muted)]">
                                      <span>
                                        Scheduled:{' '}
                                        {exam.scheduledAt
                                          ? formatDate(exam.scheduledAt, true)
                                          : exam.requestedForDate
                                            ? `Target date: ${formatDate(exam.requestedForDate)}`
                                            : 'Awaiting scheduling'}
                                      </span>
                                      <span>·</span>
                                      <span>
                                        Assigned Vet: {exam.assignedVetId ? `#${exam.assignedVetId}` : 'Unassigned'}
                                      </span>
                                    </div>
                                  </div>
                                </li>
                              ))}
                            </ol>
                          ) : (
                            <p className="mt-2 text-[13px] text-[var(--color-text-muted)] italic">
                              No examination schedule items recorded.
                            </p>
                          )}
                        </div>
                      </div>
                    )}

                    {/* Tab 3: Vitals & Health Metrics History */}
                    {activeTab === 'vitals' && (
                      <div className="space-y-4">
                        <div className="flex items-center justify-between">
                          <SectionTitle>Recorded Vitals &amp; Health Metrics</SectionTitle>
                          <span className="text-[11px] text-[var(--color-text-muted)]">
                            {metrics.length} telemetry readings on record
                          </span>
                        </div>

                        {metrics.length > 0 ? (
                          <div className="overflow-x-auto rounded-[var(--radius-md)] border border-[var(--color-border)]">
                            <table className="w-full text-left text-[12px]">
                              <thead className="border-b border-[var(--color-border)] bg-[var(--color-surface-subtle)] text-[11px] font-semibold uppercase text-[var(--color-text-secondary)]">
                                <tr>
                                  <th className="px-3 py-2.5">Date &amp; Time</th>
                                  <th className="px-3 py-2.5">Temperature</th>
                                  <th className="px-3 py-2.5">Heart Rate</th>
                                  <th className="px-3 py-2.5">Resp Rate</th>
                                  <th className="px-3 py-2.5">Weight</th>
                                  <th className="px-3 py-2.5">BCS</th>
                                  <th className="px-3 py-2.5">Hydration</th>
                                  <th className="px-3 py-2.5">Notes</th>
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
                            <Icon name="heart-pulse" size={28} className="mx-auto mb-2 opacity-50" />
                            <p className="text-[13px] font-medium text-[var(--color-text-secondary)]">
                              No recorded vitals yet
                            </p>
                            <p className="text-[11px] mt-0.5">
                              Telemetry metrics recorded during the clinical examination will be stored and tracked here.
                            </p>
                          </div>
                        )}

                        {latestMetrics && (
                          <div className="mt-4 rounded-[var(--radius-md)] bg-[var(--color-surface-subtle)] p-4 border border-[var(--color-border)]">
                            <span className="text-[11px] font-bold uppercase tracking-wider text-[var(--color-text-muted)]">
                              Latest Baseline Summary (Recorded {formatDate(latestMetrics.recordedAt, true)})
                            </span>
                            <div className="mt-2 grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-6 font-metric text-[13px]">
                              <div>
                                <span className="text-[10px] text-[var(--color-text-muted)] block font-sans">TEMP</span>
                                <strong>{latestMetrics.temperature ?? '—'} °C</strong>
                              </div>
                              <div>
                                <span className="text-[10px] text-[var(--color-text-muted)] block font-sans">PULSE</span>
                                <strong>{latestMetrics.heartRate ?? '—'} bpm</strong>
                              </div>
                              <div>
                                <span className="text-[10px] text-[var(--color-text-muted)] block font-sans">RESP</span>
                                <strong>{latestMetrics.respiratoryRate ?? '—'} rpm</strong>
                              </div>
                              <div>
                                <span className="text-[10px] text-[var(--color-text-muted)] block font-sans">WEIGHT</span>
                                <strong>{latestMetrics.weight ?? '—'} kg</strong>
                              </div>
                              <div>
                                <span className="text-[10px] text-[var(--color-text-muted)] block font-sans">BCS</span>
                                <strong>{latestMetrics.bodyConditionScore ?? '—'} / 9</strong>
                              </div>
                              <div>
                                <span className="text-[10px] text-[var(--color-text-muted)] block font-sans">HYDRATION</span>
                                <strong className="font-sans text-[12px]">{latestMetrics.hydrationStatus ?? 'Normal'}</strong>
                              </div>
                            </div>
                          </div>
                        )}
                      </div>
                    )}

                    {/* Tab 4: Intake Documents & Pedigree */}
                    {activeTab === 'documents' && (
                      <div className="grid gap-6 xl:grid-cols-2">
                        {/* Documents List */}
                        <div>
                          <SectionTitle>Admission Documents</SectionTitle>
                          {documents.length > 0 ? (
                            <ul className="mt-3 space-y-2">
                              {documents.map((doc) => (
                                <li
                                  key={doc.id}
                                  className="flex items-start gap-3 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-3 hover:bg-[var(--color-surface-subtle)] transition-colors"
                                >
                                  <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded bg-[var(--color-primary-soft)] text-[var(--color-primary)]">
                                    <Icon name="file-text" size={16} />
                                  </span>
                                  <div className="min-w-0 flex-1">
                                    <a
                                      href={doc.fileUrl}
                                      target="_blank"
                                      rel="noopener noreferrer"
                                      className="text-[13px] font-semibold text-[var(--color-primary)] hover:underline truncate block"
                                    >
                                      {formatLabel(doc.documentType)}
                                    </a>
                                    <div className="mt-0.5 flex items-center gap-2 text-[11px] text-[var(--color-text-muted)] font-metric">
                                      {doc.recordDate && <span>Dated: {formatDate(doc.recordDate)}</span>}
                                      <span>Uploaded: {formatDate(doc.uploadedAt)}</span>
                                    </div>
                                    {doc.note && (
                                      <p className="mt-1 text-[11px] text-[var(--color-text-secondary)]">
                                        {doc.note}
                                      </p>
                                    )}
                                  </div>
                                </li>
                              ))}
                            </ul>
                          ) : (
                            <p className="mt-2 text-[13px] text-[var(--color-text-muted)] italic">
                              No documents attached to this admission application.
                            </p>
                          )}
                        </div>

                        {/* Pedigree & Groom Review Info */}
                        <div className="space-y-4">
                          <Panel padded className="bg-[var(--color-surface-subtle)]">
                            <SectionTitle>Pedigree &amp; Registry</SectionTitle>
                            <dl className="mt-3 space-y-2.5 text-[12px]">
                              <div className="flex justify-between border-b border-[var(--color-border)] pb-1.5">
                                <span className="text-[var(--color-text-muted)]">Registry Name:</span>
                                <span className="font-semibold text-[var(--color-text-primary)]">
                                  {detail.candidate?.registryName || 'Not recorded'}
                                </span>
                              </div>
                              <div className="flex justify-between border-b border-[var(--color-border)] pb-1.5">
                                <span className="text-[var(--color-text-muted)]">Registration No:</span>
                                <span className="font-semibold text-[var(--color-text-primary)] font-metric">
                                  {detail.candidate?.registrationNumber || 'Not recorded'}
                                </span>
                              </div>
                              <div className="flex justify-between border-b border-[var(--color-border)] pb-1.5">
                                <span className="text-[var(--color-text-muted)]">Sire:</span>
                                <span className="font-semibold text-[var(--color-text-primary)]">
                                  {detail.candidate?.sireName || 'Not recorded'}
                                </span>
                              </div>
                              <div className="flex justify-between">
                                <span className="text-[var(--color-text-muted)]">Dam:</span>
                                <span className="font-semibold text-[var(--color-text-primary)]">
                                  {detail.candidate?.damName || 'Not recorded'}
                                </span>
                              </div>
                            </dl>
                            {detail.candidate?.pedigreeNotes && (
                              <p className="mt-3 border-t border-[var(--color-border)] pt-2 text-[11px] italic text-[var(--color-text-secondary)]">
                                &quot;{detail.candidate.pedigreeNotes}&quot;
                              </p>
                            )}
                          </Panel>

                          {/* Groom Review Result */}
                          <Panel padded className="bg-[var(--color-surface-subtle)]">
                            <SectionTitle>Initial Groom Physical Screening</SectionTitle>
                            <dl className="mt-3 space-y-2 text-[12px]">
                              <div className="flex justify-between">
                                <span className="text-[var(--color-text-muted)]">Groom Outcome:</span>
                                <span className="font-semibold text-[var(--color-text-primary)]">
                                  {detail.groomDecision ? formatLabel(detail.groomDecision) : 'Pending'}
                                </span>
                              </div>
                              <div className="flex justify-between">
                                <span className="text-[var(--color-text-muted)]">Reviewed At:</span>
                                <span className="font-metric text-[var(--color-text-primary)]">
                                  {formatDate(detail.groomReviewedAt, true)}
                                </span>
                              </div>
                            </dl>
                            {detail.groomFeedback && (
                              <div className="mt-3 rounded-[var(--radius-sm)] bg-[var(--color-surface)] p-2.5 text-[12px] border border-[var(--color-border)]">
                                <span className="font-semibold text-[var(--color-text-primary)]">
                                  Groom Inspection Note:
                                </span>
                                <p className="mt-1 text-[var(--color-text-secondary)]">
                                  {detail.groomFeedback}
                                </p>
                              </div>
                            )}
                          </Panel>
                        </div>
                      </div>
                    )}
                  </div>
                </Panel>
              </div>
            ) : (
              <Panel>
                <EmptyState
                  icon="clipboard"
                  title="Select an Admission"
                  description="Choose a candidate from the left examination queue to review clinical vitals and record a medical decision."
                />
              </Panel>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
