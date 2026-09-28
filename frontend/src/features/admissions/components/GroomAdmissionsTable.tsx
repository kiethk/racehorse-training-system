'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { Icon } from '@/components/ui/Icon';
import { EmptyState } from '@/components/ui/states';
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

const initial: GroomQueueFilters = {
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

function statusTone(status: AdmissionStatus) {
  if (status === 'GROOM_REVIEW') return 'text-[var(--color-primary)] bg-[var(--color-primary-soft)]';
  if (status === 'WAITING_FOR_STALL') return 'text-[var(--color-warning)] bg-[var(--color-warning-soft)]';
  if (status === 'APPROVED') return 'text-[var(--color-success)] bg-[var(--color-success-soft)]';
  if (status === 'REJECTED') return 'text-[var(--color-danger)] bg-[var(--color-danger-soft)]';
  return 'text-[var(--color-text-secondary)] bg-[var(--color-surface-muted)]';
}

function prettyStatus(status: AdmissionStatus) {
  return status.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase());
}

export function GroomAdmissionsTable({ initialFilters }: { initialFilters: GroomQueueFilters }) {
  const router = useRouter();
  const [draft, setDraft] = useState(initialFilters);
  const [applied, setApplied] = useState(initialFilters);
  const [result, setResult] = useState<GroomQueueResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    admissionsApi.getGroomQueue(applied)
      .then((data) => { if (active) setResult(data); })
      .catch((cause: unknown) => {
        if (active) setError(cause instanceof Error ? cause.message : 'Unable to load admissions.');
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [applied]);

  const apply = () => {
    const next = { ...draft, candidateName: draft.candidateName.trim(), page: 0 };
    setLoading(true);
    setError(null);
    setApplied(next);
    router.replace(`/groom/admissions${toQuery(next)}`);
  };

  const clear = () => {
    setDraft(initial);
    setLoading(true);
    setError(null);
    setApplied(initial);
    router.replace('/groom/admissions');
  };

  const changePage = (page: number) => {
    const next = { ...applied, page };
    setLoading(true);
    setError(null);
    setApplied(next);
    setDraft(next);
    router.replace(`/groom/admissions${toQuery(next)}`);
  };

  return (
    <section className="space-y-4">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold text-[var(--color-text-primary)]">Admission applications</h1>
          <p className="mt-1 text-sm text-[var(--color-text-secondary)]">Groom admission records</p>
        </div>
        {result && <p className="text-sm text-[var(--color-text-secondary)]">{result.totalElements} applications</p>}
      </header>

      <form className="grid gap-3 border-y border-[var(--color-border)] py-4 sm:grid-cols-2 xl:grid-cols-[minmax(220px,1.4fr)_minmax(170px,1fr)_minmax(150px,1fr)_minmax(150px,1fr)_auto] xl:items-end" onSubmit={(event) => { event.preventDefault(); apply(); }}>
        <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
          Horse name
          <span className="relative mt-1.5 block">
            <Icon name="search" size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--color-text-muted)]" />
            <input value={draft.candidateName} onChange={(event) => setDraft({ ...draft, candidateName: event.target.value })} placeholder="Search horse name" className="h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] pl-9 pr-3 text-sm text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]" />
          </span>
        </label>
        <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
          Status
          <select value={draft.status} onChange={(event) => setDraft({ ...draft, status: event.target.value as AdmissionStatus | '' })} className="mt-1.5 h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-sm text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]">
            {statuses.map((status) => <option key={status.value || 'all'} value={status.value}>{status.label}</option>)}
          </select>
        </label>
        <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
          Submitted from
          <input type="date" value={draft.submittedFrom} onChange={(event) => setDraft({ ...draft, submittedFrom: event.target.value })} className="mt-1.5 h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-sm text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]" />
        </label>
        <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
          Submitted to
          <input type="date" min={draft.submittedFrom || undefined} value={draft.submittedTo} onChange={(event) => setDraft({ ...draft, submittedTo: event.target.value })} className="mt-1.5 h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-sm text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]" />
        </label>
        <div className="flex items-center gap-2 sm:col-span-2 xl:col-span-1">
          <Button type="submit" variant="primary" size="sm" icon="search">Apply</Button>
          <Button type="button" variant="secondary" size="sm" icon="x" onClick={clear}>Clear</Button>
        </div>
      </form>

      {error ? (
        <div className="border-y border-[var(--color-border)] py-2">
          <EmptyState icon="alert-triangle" title="Unable to load applications" description={error} action={<Button size="sm" onClick={() => { setLoading(true); setError(null); setApplied({ ...applied }); }}>Retry</Button>} />
        </div>
      ) : loading ? (
        <div className="space-y-3 py-4" aria-label="Loading applications">
          {Array.from({ length: 5 }).map((_, index) => <div key={index} className="h-12 animate-pulse rounded bg-[var(--color-surface-muted)]" />)}
        </div>
      ) : result?.content.length === 0 ? (
        <div className="border-y border-[var(--color-border)] py-2">
          <EmptyState title={applied.candidateName || applied.status || applied.submittedFrom || applied.submittedTo ? 'No matching applications' : 'No applications'} description="Change the filters or clear them to see other records." action={<Button size="sm" onClick={clear}>Clear filters</Button>} />
        </div>
      ) : (
        <>
          <div className="overflow-x-auto border-y border-[var(--color-border)]">
            <table className="w-full min-w-[760px] border-collapse text-left">
              <thead>
                <tr className="h-10 border-b border-[var(--color-border)] text-[11px] font-semibold uppercase text-[var(--color-text-muted)]">
                  <th className="w-[38%] px-3">Horse</th>
                  <th className="w-[24%] px-3">Status</th>
                  <th className="w-[24%] px-3">Submitted</th>
                  <th className="w-[14%] px-3 text-center">Action</th>
                </tr>
              </thead>
              <tbody>
                {result?.content.map((admission) => (
                  <tr key={admission.admissionId} className="h-[62px] border-b border-[var(--color-border)] last:border-b-0 hover:bg-[var(--color-surface-muted)]/50">
                    <td className="px-3">
                      <div className="flex items-center gap-3">
                        <HorseAvatar name={admission.candidateName} size={36} />
                        <div className="min-w-0">
                          <p className="truncate text-sm font-medium text-[var(--color-text-primary)]">{admission.candidateName}</p>
                          <p className="truncate text-xs text-[var(--color-text-muted)]">{admission.breed || 'Breed not provided'}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-3"><span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-medium ${statusTone(admission.status)}`}>{prettyStatus(admission.status)}</span></td>
                    <td className="px-3 text-sm text-[var(--color-text-secondary)]">{new Date(admission.submittedAt).toLocaleDateString()}</td>
                    <td className="px-3 text-center">
                      <Link href={`/groom/admissions/${admission.admissionId}${toQuery(applied)}`} className="inline-flex h-8 min-w-16 items-center justify-center rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] px-3 text-xs font-medium text-[var(--color-text-primary)] hover:bg-[var(--color-surface-muted)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]">View</Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="flex flex-wrap items-center justify-between gap-3 text-sm text-[var(--color-text-secondary)]">
            <span>{result?.totalElements ? `Showing ${result.page * result.size + 1}-${Math.min((result.page + 1) * result.size, result.totalElements)} of ${result.totalElements}` : 'No records'}</span>
            <div className="flex items-center gap-2">
              <Button size="sm" icon="chevron-left" aria-label="Previous page" disabled={!result || result.page <= 0} onClick={() => result && changePage(result.page - 1)} />
              <span className="min-w-20 text-center">Page {(result?.page ?? 0) + 1} of {Math.max(1, result?.totalPages ?? 1)}</span>
              <Button size="sm" iconRight="chevron-right" aria-label="Next page" disabled={!result || result.page + 1 >= result.totalPages} onClick={() => result && changePage(result.page + 1)} />
            </div>
          </div>
        </>
      )}
    </section>
  );
}
