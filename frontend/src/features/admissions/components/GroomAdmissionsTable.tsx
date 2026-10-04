'use client';

import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { Panel, SectionTitle } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { admissionsApi } from '../services/api';
import type { AdmissionStatus, GroomQueueFilters, GroomQueueResponse } from '../types';

const statuses: { value: AdmissionStatus | ''; label: string }[] = [
  { value: '', label: 'All statuses' },
  { value: 'GROOM_REVIEW', label: 'Groom review' },
  { value: 'WAITING_FOR_STALL', label: 'Waiting for stall' },
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

function prettyStatus(status: AdmissionStatus) {
  return status.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function statusTone(status: AdmissionStatus): 'success' | 'warning' | 'danger' | 'info' | 'primary' | 'neutral' {
  if (status === 'GROOM_REVIEW') return 'primary';
  if (status === 'WAITING_FOR_STALL') return 'warning';
  if (status === 'APPROVED') return 'success';
  if (status === 'REJECTED') return 'danger';
  if (status === 'VET_REVIEW' || status === 'TRAINER_REVIEW' || status === 'MANAGER_REVIEW') return 'info';
  return 'neutral';
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
  const showingFrom = total === 0 ? 0 : result!.page * result!.size + 1;
  const showingTo = result ? Math.min((result.page + 1) * result.size, total) : 0;

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
    return <div className="space-y-5"><PageHeading /><Panel><ListSkeleton rows={8} /></Panel></div>;
  }

  if (error && !result) {
    return (
      <div className="space-y-5">
        <PageHeading />
        <Panel>
          <EmptyState
            title="Unable to load applications"
            description={error}
            action={<Button size="sm" onClick={() => setReloadKey((value) => value + 1)}>Retry</Button>}
          />
        </Panel>
      </div>
    );
  }

  return (
    <div className="space-y-5">
      <PageHeading />

      <form
        className="grid gap-3 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-3 sm:grid-cols-2 xl:grid-cols-[minmax(260px,1.5fr)_minmax(180px,0.8fr)_minmax(180px,0.8fr)_minmax(180px,0.8fr)_auto] xl:items-end"
        onSubmit={(event) => { event.preventDefault(); apply(); }}
      >
        <label className="block text-[11px] font-medium text-[var(--color-text-muted)]">
          Search horse name
          <input value={draft.candidateName} onChange={(event) => setDraft({ ...draft, candidateName: event.target.value })} placeholder="Search horse name" className="mt-1.5 h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-3 text-[12px] text-[var(--color-text-primary)] outline-none placeholder:text-[var(--color-text-muted)] focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]" />
        </label>
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
      </form>

      {error && <div role="alert" className="border-l-2 border-[var(--color-danger)] bg-[var(--color-danger-soft)] px-4 py-3 text-[12px] text-[var(--color-text-primary)]">{error}</div>}

      <Panel className="overflow-hidden">
        <div className="flex items-center justify-between border-b border-[var(--color-border)] bg-[var(--color-surface-subtle)] px-4 py-3">
          <SectionTitle>Admission applications</SectionTitle>
          <span className="text-[11px] text-[var(--color-text-muted)]">{total} applications</span>
        </div>

        {applications.length === 0 ? (
          <EmptyState
            title={total > 0 ? 'No matching applications' : 'No applications'}
            description={total > 0 ? 'Change the filters or clear them to see other records.' : 'New applications will appear here.'}
            action={total > 0 ? <Button size="sm" onClick={clear}>Clear filters</Button> : undefined}
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[720px] border-collapse text-left">
              <thead className="bg-[var(--color-surface-subtle)] text-[10px] font-medium uppercase tracking-wide text-[var(--color-text-muted)]">
                <tr>
                  <th scope="col" className="px-4 py-3">Horse</th>
                  <th scope="col" className="px-4 py-3">Status</th>
                  <th scope="col" className="px-4 py-3">Submitted</th>
                  <th scope="col" className="px-4 py-3 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--color-border)]">
                {applications.map((admission) => (
                  <tr key={admission.admissionId} className="bg-[var(--color-surface)] transition-colors hover:bg-[var(--color-surface-subtle)]">
                    <td className="px-4 py-3">
                      <div className="flex min-w-0 items-center gap-3">
                        <HorseAvatar name={admission.candidateName} size={36} />
                        <div className="min-w-0">
                          <p className="truncate text-[13px] font-medium text-[var(--color-text-primary)]">{admission.candidateName}</p>
                          <p className="truncate text-[11px] text-[var(--color-text-muted)]">{admission.breed || 'Breed not provided'}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3"><Pill tone={statusTone(admission.status)} size="sm">{prettyStatus(admission.status)}</Pill></td>
                    <td className="px-4 py-3 text-[12px] text-[var(--color-text-secondary)]">{formatDate(admission.submittedAt)}</td>
                    <td className="px-4 py-3 text-right">
                      <Link href={`/groom/admissions/${admission.admissionId}${toQuery(applied)}`} className="inline-flex h-8 items-center rounded-[var(--radius-sm)] bg-[var(--color-primary-soft)] px-4 text-[12px] font-medium text-[var(--color-primary)] transition-colors hover:bg-[var(--color-primary)] hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]">View</Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {result && total > 0 && (
          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-[var(--color-border)] px-4 py-3 text-[11px] text-[var(--color-text-secondary)]">
            <span>Showing {showingFrom}-{showingTo} of {total}</span>
            <div className="flex items-center gap-2">
              <Button size="sm" variant="secondary" disabled={result.page <= 0 || loading} onClick={() => changePage(result.page - 1)}>Previous</Button>
              <span className="min-w-16 text-center">{result.page + 1} / {Math.max(1, result.totalPages)}</span>
              <Button size="sm" variant="secondary" disabled={result.page + 1 >= result.totalPages || loading} onClick={() => changePage(result.page + 1)}>Next</Button>
            </div>
          </div>
        )}
      </Panel>
    </div>
  );
}

function PageHeading() {
  return <header><h1 className="text-[21px] font-semibold tracking-tight text-[var(--color-text-primary)]">Admissions</h1></header>;
}
