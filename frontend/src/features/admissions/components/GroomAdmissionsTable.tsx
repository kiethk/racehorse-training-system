'use client';

import { useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { Notice } from '@/components/ui/Notice';
import { admissionsApi } from '../services/api';
import type { AdmissionStatus, GroomQueueFilters, GroomQueueResponse } from '../types';
import { AdmissionListLayout } from '../shared/components/AdmissionListLayout';
import { AdmissionFilterBar, FilterDateRange, FilterSelect } from '../shared/components/AdmissionFilterBar';
import { AdmissionTable } from '../shared/components/AdmissionTable';

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
      <AdmissionFilterBar
        search={draft.candidateName}
        onSearchChange={(candidateName) => setDraft({ ...draft, candidateName })}
        onApply={apply}
        onClear={clear}
      >
        <FilterSelect label="Status" value={draft.status} onChange={(status) => setDraft({ ...draft, status })} options={statuses} />
        <FilterDateRange
          from={draft.submittedFrom}
          to={draft.submittedTo}
          onFromChange={(submittedFrom) => setDraft({ ...draft, submittedFrom })}
          onToChange={(submittedTo) => setDraft({ ...draft, submittedTo })}
        />
      </AdmissionFilterBar>

      {error && <Notice tone="error">{error}</Notice>}

      <AdmissionTable
        admissions={applications}
        detailHref={(id) => `/groom/admissions/${id}${toQuery(applied)}`}
        searchable={false}
        loading={loading}
        emptyTitle={total > 0 ? 'No matching applications' : 'No applications'}
        emptyDescription={total > 0 ? 'Change the filters or clear them to see other records.' : 'New applications will appear here.'}
        emptyAction={total > 0 ? <Button size="sm" onClick={clear}>Clear filters</Button> : undefined}
        pagination={result ? { page: result.page, pageSize: result.size, total, onPageChange: changePage, disabled: loading } : undefined}
      />
    </AdmissionListLayout>
  );
}
