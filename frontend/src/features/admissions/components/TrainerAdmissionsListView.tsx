'use client';

import { useEffect, useState, useMemo } from 'react';
import Link from 'next/link';
import { useSearchParams, useRouter, usePathname } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { ListSkeleton } from '@/components/ui/states';
import { Notice } from '@/components/ui/Notice';
import { FilterBar } from '@/components/ui/FilterBar';
import { DataTable, type DataTableColumn } from '@/components/ui/DataTable';
import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { trainerAdmissionsApi } from '../services/trainerAdmissionService';
import type { TrainerAdmissionQueue } from '../types/trainer';
import { AdmissionStatusBadge } from '../shared/components/AdmissionStatusBadge';
import { AdmissionListLayout } from '../shared/components/AdmissionListLayout';
import { AdmissionSearchField } from '../shared/components/AdmissionSearchField';

type Tab = 'PENDING' | 'REVIEWED';

interface TrainerQueueFilters {
  tab: Tab;
  candidateName: string;
  submittedFrom: string;
  submittedTo: string;
}

export function TrainerAdmissionsListView() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const urlFilters: TrainerQueueFilters = useMemo(() => {
    const rawTab = searchParams.get('tab') as Tab | null;
    const safeTab: Tab = rawTab === 'REVIEWED' ? 'REVIEWED' : 'PENDING';
    return {
      tab: safeTab,
      candidateName: searchParams.get('candidateName') || '',
      submittedFrom: searchParams.get('submittedFrom') || '',
      submittedTo: searchParams.get('submittedTo') || '',
    };
  }, [searchParams]);

  const [queue, setQueue] = useState<TrainerAdmissionQueue>({ pending: [], reviewed: [] });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [draft, setDraft] = useState<TrainerQueueFilters>(urlFilters);
  const [tablePage, setTablePage] = useState(0);

  useEffect(() => {
    let active = true;
    async function loadQueue() {
      try {
        setLoading(true);
        setError(null);
        const data = await trainerAdmissionsApi.getQueue();
        if (active) setQueue(data);
      } catch (err) {
        console.error('Failed to load admissions:', err);
        if (active)
          setError(
            err instanceof Error
              ? err.message
              : 'Failed to load admission applications. Please try again.',
          );
      } finally {
        if (active) setLoading(false);
      }
    }
    loadQueue();
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setDraft(urlFilters);
  }, [urlFilters]);

  // Backend đã lọc theo Trainer đang đăng nhập và tách sẵn hai nhóm, nên
  // số đếm chỉ là độ dài mảng — không lọc lại theo status/trainerId ở client.
  const pendingCount = queue.pending.length;
  const reviewedCount = queue.reviewed.length;

  // Chỉ còn lọc theo tên và ngày nộp — hai thứ người dùng gõ tại màn hình này.
  const filteredAdmissions = useMemo(() => {
    const source = urlFilters.tab === 'REVIEWED' ? queue.reviewed : queue.pending;

    return source.filter((a) => {
      // Filter by horse name
      if (
        urlFilters.candidateName &&
        !a.candidateName.toLowerCase().includes(urlFilters.candidateName.toLowerCase())
      ) {
        return false;
      }

      // Filter by submission date
      if (urlFilters.submittedFrom || urlFilters.submittedTo) {
        const submittedTime = new Date(a.submittedAt).getTime();
        if (urlFilters.submittedFrom) {
          const fromTime = new Date(urlFilters.submittedFrom).getTime();
          if (submittedTime < fromTime) return false;
        }
        if (urlFilters.submittedTo) {
          const toTime = new Date(urlFilters.submittedTo).getTime() + 24 * 60 * 60 * 1000 - 1;
          if (submittedTime > toTime) return false;
        }
      }

      return true;
    });
  }, [queue, urlFilters]);

  const updateUrl = (filters: TrainerQueueFilters) => {
    const params = new URLSearchParams();
    if (filters.tab !== 'PENDING') params.set('tab', filters.tab);
    if (filters.candidateName) params.set('candidateName', filters.candidateName);
    if (filters.submittedFrom) params.set('submittedFrom', filters.submittedFrom);
    if (filters.submittedTo) params.set('submittedTo', filters.submittedTo);
    const qs = params.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname);
  };

  const handleTabChange = (nextTab: Tab) => {
    const next = { ...draft, tab: nextTab };
    setDraft(next);
    setTablePage(0);
    updateUrl(next);
  };

  const applyFilters = (e: React.FormEvent) => {
    e.preventDefault();
    setTablePage(0);
    updateUrl(draft);
  };

  const clearFilters = () => {
    const reset: TrainerQueueFilters = {
      tab: draft.tab,
      candidateName: '',
      submittedFrom: '',
      submittedTo: '',
    };
    setDraft(reset);
    setTablePage(0);
    updateUrl(reset);
  };

  const detailQuery = useMemo(() => {
    const p = new URLSearchParams();
    if (urlFilters.tab !== 'PENDING') p.set('tab', urlFilters.tab);
    if (urlFilters.candidateName) p.set('candidateName', urlFilters.candidateName);
    if (urlFilters.submittedFrom) p.set('submittedFrom', urlFilters.submittedFrom);
    if (urlFilters.submittedTo) p.set('submittedTo', urlFilters.submittedTo);
    const s = p.toString();
    return s ? `?${s}` : '';
  }, [urlFilters]);

  if (loading) return <AdmissionListLayout title="Admissions" description="Assess racing potential and readiness of candidate horses in the quarantine facility."><ListSkeleton rows={6} /></AdmissionListLayout>;

  const columns: DataTableColumn<TrainerAdmissionQueue['pending'][number]>[] = [
    {
      id: 'horse',
      header: 'Horse',
      sortValue: (admission) => admission.candidateName,
      render: (admission) => (
        <div className="flex min-w-0 items-center gap-3">
          <HorseAvatar name={admission.candidateName} image={admission.imageUrl} size={36} rounded="md" />
          <div className="min-w-0">
            <span className="block truncate font-semibold">{admission.candidateName}</span>
            <span className="block truncate text-xs text-[var(--color-text-muted)]">{admission.breed || 'Unknown breed'}</span>
          </div>
        </div>
      ),
    },
    { id: 'status', header: 'Status', sortValue: (admission) => admission.status, render: (admission) => <AdmissionStatusBadge status={admission.status} simplified /> },
    {
      id: 'stall',
      header: 'Quarantine stall',
      sortValue: (admission) => admission.quarantineStallCode ?? '',
      render: (admission) => admission.quarantineStallCode
        ? `Stall ${admission.quarantineStallCode}`
        : <span className="italic text-[var(--color-text-muted)]">Unassigned</span>,
    },
    { id: 'submitted', header: 'Submitted', sortValue: (admission) => new Date(admission.submittedAt), render: (admission) => new Date(admission.submittedAt).toLocaleDateString('en-GB', { year: 'numeric', month: 'short', day: 'numeric' }) },
    ...(urlFilters.tab === 'REVIEWED' ? [{
      id: 'reviewed',
      header: 'Evaluated',
      sortValue: (admission: TrainerAdmissionQueue['pending'][number]) => admission.trainerReviewedAt ? new Date(admission.trainerReviewedAt) : null,
      render: (admission: TrainerAdmissionQueue['pending'][number]) => admission.trainerReviewedAt
        ? new Date(admission.trainerReviewedAt).toLocaleDateString('en-GB', { year: 'numeric', month: 'short', day: 'numeric' })
        : '—',
    }] : []),
    {
      id: 'action',
      header: 'Action',
      align: 'right',
      render: (admission) => (
        <Link
          href={`/trainer/admissions/${admission.admissionId}${detailQuery}`}
          className={`${admission.status === 'TRAINER_REVIEW' ? 'bg-[var(--color-primary)] text-[var(--color-text-inverse)] hover:opacity-90' : 'bg-[var(--color-primary-soft)] text-[var(--color-primary)] hover:bg-[var(--color-primary-subtle)]'} inline-flex min-h-9 items-center justify-center rounded-[var(--radius-sm)] px-4 text-xs font-semibold transition-colors focus-visible:outline-2 focus-visible:outline-[var(--color-focus)]`}
        >
          {admission.status === 'TRAINER_REVIEW' ? 'Evaluate' : 'View'}
        </Link>
      ),
    },
  ];

  return (
    <AdmissionListLayout title="Admissions" description="Assess racing potential and readiness of candidate horses in the quarantine facility.">
      {/* Filter Toolbar (Merged review type filter) */}
      <FilterBar
        layout="grid"
        onSubmit={applyFilters}
        className="sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-5 xl:items-end"
      >
        <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
          Application Type
          <select
            value={draft.tab}
            onChange={(e) => handleTabChange(e.target.value as Tab)}
            className="mt-1 h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-sm text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
          >
            <option value="PENDING">Pending Evaluation ({pendingCount})</option>
            <option value="REVIEWED">Evaluated by You ({reviewedCount})</option>
          </select>
        </label>

        <AdmissionSearchField
          label="Candidate horse name"
          placeholder="Search horse name"
          value={draft.candidateName}
          onChange={(candidateName) => setDraft({ ...draft, candidateName })}
        />

        <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
          Submitted From
          <input
            type="date"
            value={draft.submittedFrom}
            onChange={(e) => setDraft({ ...draft, submittedFrom: e.target.value })}
            className="mt-1 h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-sm text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
          />
        </label>

        <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
          Submitted To
          <input
            type="date"
            min={draft.submittedFrom || undefined}
            value={draft.submittedTo}
            onChange={(e) => setDraft({ ...draft, submittedTo: e.target.value })}
            className="mt-1 h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-sm text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
          />
        </label>

        <div className="flex items-center gap-2">
          <Button type="submit" variant="primary" size="sm">
            Apply
          </Button>
          <Button type="button" variant="secondary" size="sm" onClick={clearFilters}>
            Reset
          </Button>
        </div>
      </FilterBar>

      {error && (
        <Notice tone="error">{error}</Notice>
      )}

      <DataTable
        rows={filteredAdmissions}
        columns={columns}
        getRowKey={(admission) => admission.admissionId}
        ariaLabel="Trainer admission records"
        emptyTitle="No admission records found"
        emptyDescription="Try adjusting your search filters or switch the application type."
        emptyAction={<Button size="sm" onClick={clearFilters}>Reset filters</Button>}
        pagination={{ page: tablePage, pageSize: 10, total: filteredAdmissions.length, onPageChange: setTablePage }}
      />
    </AdmissionListLayout>
  );
}
