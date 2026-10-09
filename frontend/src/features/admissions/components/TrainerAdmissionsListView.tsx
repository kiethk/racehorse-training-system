'use client';

import { useEffect, useState, useMemo } from 'react';
import { useSearchParams, useRouter, usePathname } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { ListSkeleton } from '@/components/ui/states';
import { Notice } from '@/components/ui/Notice';
import { type DataTableColumn } from '@/components/ui/DataTable';
import { FilterChips } from '@/components/ui/SegmentedControl';
import { formatDate } from '@/lib/display';
import { trainerAdmissionsApi } from '../services/trainerAdmissionService';
import type { TrainerAdmissionQueue } from '../types/trainer';
import { AdmissionListLayout } from '../shared/components/AdmissionListLayout';
import { AdmissionFilterBar, FilterDateRange } from '../shared/components/AdmissionFilterBar';
import { AdmissionTable } from '../shared/components/AdmissionTable';

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

  const applyFilters = () => {
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

  type Row = TrainerAdmissionQueue['pending'][number];
  const extraColumns: DataTableColumn<Row>[] = [
    {
      id: 'stall',
      header: 'Quarantine stall',
      sortValue: (admission) => admission.quarantineStallCode ?? '',
      render: (admission) => admission.quarantineStallCode
        ? `Stall ${admission.quarantineStallCode}`
        : <span className="italic text-[var(--color-text-muted)]">Unassigned</span>,
    },
    ...(urlFilters.tab === 'REVIEWED' ? [{
      id: 'reviewed',
      header: 'Evaluated',
      sortValue: (admission: Row) => admission.trainerReviewedAt ? new Date(admission.trainerReviewedAt) : null,
      render: (admission: Row) => formatDate(admission.trainerReviewedAt),
    }] : []),
  ];

  return (
    <AdmissionListLayout title="Admissions" description="Assess racing potential and readiness of candidate horses in the quarantine facility.">
      <FilterChips
        label="Application type"
        value={urlFilters.tab}
        onChange={handleTabChange}
        options={[
          { value: 'PENDING', label: 'Pending evaluation', count: pendingCount },
          { value: 'REVIEWED', label: 'Evaluated by you', count: reviewedCount },
        ]}
      />

      <AdmissionFilterBar
        search={draft.candidateName}
        onSearchChange={(candidateName) => setDraft({ ...draft, candidateName })}
        onApply={applyFilters}
        onClear={clearFilters}
      >
        <FilterDateRange
          from={draft.submittedFrom}
          to={draft.submittedTo}
          onFromChange={(submittedFrom) => setDraft({ ...draft, submittedFrom })}
          onToChange={(submittedTo) => setDraft({ ...draft, submittedTo })}
        />
      </AdmissionFilterBar>

      {error && (
        <Notice tone="error">{error}</Notice>
      )}

      <AdmissionTable
        admissions={filteredAdmissions}
        detailHref={(id) => `/trainer/admissions/${id}${detailQuery}`}
        extraColumns={extraColumns}
        simplifiedStatus
        action={(admission) => admission.status === 'TRAINER_REVIEW' ? { label: 'Evaluate', primary: true } : { label: 'View' }}
        searchable={false}
        emptyTitle="No admission records found"
        emptyDescription="Try adjusting your search filters or switch the application type."
        emptyAction={<Button size="sm" onClick={clearFilters}>Clear filters</Button>}
        pagination={{ page: tablePage, pageSize: 10, total: filteredAdmissions.length, onPageChange: setTablePage }}
      />
    </AdmissionListLayout>
  );
}
