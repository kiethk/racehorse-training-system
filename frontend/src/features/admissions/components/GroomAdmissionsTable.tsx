'use client';

import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { FilterBar } from '@/components/ui/FilterBar';
import { DataTable, type DataTableColumn } from '@/components/ui/DataTable';
import { admissionsApi } from '../services/api';
import type { AdmissionStatus, AdmissionSummaryResponse, GroomQueueFilters, GroomQueueResponse } from '../types';
import { AdmissionStatusBadge } from '../shared/components/AdmissionStatusBadge';
import { AdmissionListLayout } from '../shared/components/AdmissionListLayout';
import { AdmissionSearchField } from '../shared/components/AdmissionSearchField';

const statuses: { value: AdmissionStatus | ''; label: string }[] = [
  { value: '', label: 'All statuses' },
  { value: 'GROOM_REVIEW', label: 'Groom review' },
  { value: 'WAITING_FOR_STALL', label: 'Waiting for stall' },
  { value: 'WAITING_FOR_ARRIVAL', label: 'Waiting for arrival' },
  { value: 'ARRIVAL_EXPIRED', label: 'Arrival expired' },
  { value: 'VET_REVIEW', label: 'Vet review' },
  { value: 'TRAINER_REVIEW', label: 'Trainer review' },
  { value: 'MANAGER_REVIEW', label: 'Manager review' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'REJECTED', label: 'Rejected' },
];

const emptyFilters: GroomQueueFilters = {
  candidateName: '', status: '', submittedFrom: '', submittedTo: '', page: 0,
};

function toQuery(filters: GroomQueueFilters) {
  const params = new URLSearchParams();
  if (filters.candidateName) params.set('candidateName', filters.candidateName);
  if (filters.status) params.set('status', filters.status);
  if (filters.submittedFrom) params.set('submittedFrom', filters.submittedFrom);
  if (filters.submittedTo) params.set('submittedTo', filters.submittedTo);
  if (filters.page > 0) params.set('page', String(filters.page));
  const query = params.toString();
  return query ? `?${query}` : '';
}

function formatDate(value: string) {
  return new Date(value).toLocaleDateString('en-GB', {
    day: '2-digit', month: 'short', year: 'numeric',
  });
}

export function GroomAdmissionsTable({ initialFilters }: { initialFilters: GroomQueueFilters }) {
  const router = useRouter();
  const [draft, setDraft] = useState(initialFilters);
  const [applied, setApplied] = useState(initialFilters);
  const [result, setResult] = useState<GroomQueueResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoading(true);
    setError(null);

    admissionsApi.getGroomQueue(applied)
      .then((data) => { if (active) setResult(data); })
      .catch((cause: unknown) => {
        if (active) setError(cause instanceof Error ? cause.message : 'Unable to load admissions.');
      })
      .finally(() => { if (active) setLoading(false); });

    return () => { active = false; };
  }, [applied, reloadKey]);

  const applications = useMemo(() => result?.content ?? [], [result]);
  const total = result?.totalElements ?? 0;

  const columns: DataTableColumn<AdmissionSummaryResponse>[] = [
    {
      id: 'horse',
      header: 'Horse',
      render: (admission) => (
        <div className="flex min-w-0 items-center gap-3">
          <HorseAvatar name={admission.candidateName} size={36} />
          <div className="min-w-0">
            <p className="truncate font-medium">{admission.candidateName}</p>
            <p className="truncate text-xs text-[var(--color-text-muted)]">{admission.breed || 'Breed not provided'}</p>
          </div>
        </div>
      ),
    },
    { id: 'status', header: 'Status', render: (admission) => <AdmissionStatusBadge status={admission.status} /> },
    { id: 'submitted', header: 'Submitted', render: (admission) => formatDate(admission.submittedAt) },
    {
      id: 'action',
      header: 'Action',
      align: 'right',
      render: (admission) => (
        <Link href={`/groom/admissions/${admission.admissionId}${toQuery(applied)}`} className="inline-flex min-h-9 items-center justify-center rounded-[var(--radius-sm)] bg-[var(--color-primary-soft)] px-4 text-xs font-semibold text-[var(--color-primary)] transition-colors hover:bg-[var(--color-primary-subtle)] focus-visible:outline-2 focus-visible:outline-[var(--color-focus)]">
          View
        </Link>
      ),
    },
  ];

  const apply = () => {
    const next = { ...draft, candidateName: draft.candidateName.trim(), page: 0 };
    setApplied(next);
    router.replace(`/groom/admissions${toQuery(next)}`);
  };

  const clear = () => {
    setDraft(emptyFilters);
    setApplied(emptyFilters);
    router.replace('/groom/admissions');
  };

  const changePage = (page: number) => {
    const next = { ...applied, page };
    setDraft(next);
    setApplied(next);
    router.replace(`/groom/admissions${toQuery(next)}`);
  };

  if (loading && !result) {
    return <AdmissionListLayout title="Admissions" description="Review applications, quarantine capacity and horse arrivals."><ListSkeleton rows={8} /></AdmissionListLayout>;
  }

  if (error && !result) {
    return (
      <AdmissionListLayout title="Admissions" description="Review applications, quarantine capacity and horse arrivals.">
        <EmptyState
          title="Unable to load applications"
          description={error}
          action={<Button size="sm" onClick={() => setReloadKey((value) => value + 1)}>Retry</Button>}
        />
      </AdmissionListLayout>
    );
  }

  return (
    <AdmissionListLayout title="Admissions" description="Review applications, quarantine capacity and horse arrivals.">
      <FilterBar
        layout="grid"
        className="sm:grid-cols-2 xl:grid-cols-[minmax(260px,1.5fr)_minmax(180px,0.8fr)_minmax(180px,0.8fr)_minmax(180px,0.8fr)_auto] xl:items-end"
        onSubmit={(event) => { event.preventDefault(); apply(); }}
      >
        <AdmissionSearchField value={draft.candidateName} onChange={(candidateName) => setDraft({ ...draft, candidateName })} />
        <label className="block text-[11px] font-medium text-[var(--color-text-muted)]">
          Status
          <select value={draft.status} onChange={(event) => setDraft({ ...draft, status: event.target.value as AdmissionStatus | '' })} className="mt-1.5 h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-3 text-[12px] text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]">
            {statuses.map((status) => <option key={status.value || 'all'} value={status.value}>{status.label}</option>)}
          </select>
        </label>
        <label className="block text-[11px] font-medium text-[var(--color-text-muted)]">
          From
          <input type="date" value={draft.submittedFrom} onChange={(event) => setDraft({ ...draft, submittedFrom: event.target.value })} className="mt-1.5 h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-3 text-[12px] text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]" />
        </label>
        <label className="block text-[11px] font-medium text-[var(--color-text-muted)]">
          To
          <input type="date" min={draft.submittedFrom || undefined} value={draft.submittedTo} onChange={(event) => setDraft({ ...draft, submittedTo: event.target.value })} className="mt-1.5 h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-3 text-[12px] text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]" />
        </label>
        <div className="flex items-center gap-2 sm:col-span-2 xl:col-span-1">
          <Button type="submit" variant="primary" size="sm">Apply</Button>
          <Button type="button" variant="secondary" size="sm" onClick={clear}>Clear</Button>
        </div>
      </FilterBar>

      {error && <div role="alert" className="border-l-2 border-[var(--color-danger)] bg-[var(--color-danger-soft)] px-4 py-3 text-[12px] text-[var(--color-text-primary)]">{error}</div>}

      <DataTable
        rows={applications}
        columns={columns}
        getRowKey={(admission) => admission.admissionId}
        ariaLabel="Admission records"
        loading={loading}
        emptyTitle={total > 0 ? 'No matching applications' : 'No applications'}
        emptyDescription={total > 0 ? 'Change the filters or clear them to see other records.' : 'New applications will appear here.'}
        emptyAction={total > 0 ? <Button size="sm" onClick={clear}>Clear filters</Button> : undefined}
        pagination={result ? { page: result.page, pageSize: result.size, total, onPageChange: changePage, disabled: loading } : undefined}
      />
    </AdmissionListLayout>
  );
}
