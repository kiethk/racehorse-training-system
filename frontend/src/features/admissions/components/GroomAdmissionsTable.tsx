'use client';

import { useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { MetricCard } from '@/components/ui/MetricCard';
import { Panel, SectionTitle } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { GroomAdmissionReview } from './GroomAdmissionReview';
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

export function GroomAdmissionsTable({ initialFilters }: { initialFilters: GroomQueueFilters }) {
  const router = useRouter();
  const [draft, setDraft] = useState(initialFilters);
  const [applied, setApplied] = useState(initialFilters);
  const [result, setResult] = useState<GroomQueueResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedAdmissionId, setSelectedAdmissionId] = useState<number | null>(null);
  const [reviewVersion, setReviewVersion] = useState(0);

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
  }, [applied, reviewVersion]);

  const applications = useMemo(() => result?.content ?? [], [result]);

  useEffect(() => {
    if (applications.length === 0) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setSelectedAdmissionId(null);
      return;
    }
    if (!applications.some((application) => application.admissionId === selectedAdmissionId)) {
      setSelectedAdmissionId(applications[0].admissionId);
    }
  }, [applications, selectedAdmissionId]);

  const apply = () => {
    const next = { ...draft, candidateName: draft.candidateName.trim(), page: 0 };
    setError(null);
    setApplied(next);
    router.replace(`/groom/admissions${toQuery(next)}`);
  };

  const clear = () => {
    setDraft(initial);
    setError(null);
    setApplied(initial);
    router.replace('/groom/admissions');
  };

  const changePage = (page: number) => {
    const next = { ...applied, page };
    setError(null);
    setApplied(next);
    setDraft(next);
    router.replace(`/groom/admissions${toQuery(next)}`);
  };

  const total = result?.totalElements ?? 0;
  const awaitingReview = applications.filter((application) => application.status === 'GROOM_REVIEW').length;
  const waitingForStall = applications.filter((application) => application.status === 'WAITING_FOR_STALL').length;
  const rejected = applications.filter((application) => application.status === 'REJECTED').length;

  if (loading && !result) {
    return <Panel><ListSkeleton rows={6} /></Panel>;
  }

  if (error && !result) {
    return (
      <Panel>
        <EmptyState
          icon="alert-triangle"
          title="Unable to load applications"
          description={error}
          action={<Button size="sm" onClick={() => { setError(null); setReviewVersion((value) => value + 1); }}>Retry</Button>}
        />
      </Panel>
    );
  }

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
        <MetricCard label="APPLICATIONS" value={total} unit="total" icon="users" tone="neutral" />
        <MetricCard label="AWAITING MY REVIEW" value={awaitingReview} unit="applications" icon="clock" tone="warning" />
        <MetricCard label="WAITING FOR STALL" value={waitingForStall} unit="applications" icon="shield" tone="info" />
        <MetricCard label="REJECTED" value={rejected} unit="flagged" icon="alert-triangle" tone="danger" />
      </div>

      <form
        className="grid gap-3 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-3 sm:grid-cols-2 xl:grid-cols-[minmax(220px,1.4fr)_minmax(170px,1fr)_minmax(150px,1fr)_minmax(150px,1fr)_auto] xl:items-end"
        onSubmit={(event) => { event.preventDefault(); apply(); }}
      >
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

      {error && <div role="alert" className="border-l-2 border-[var(--color-danger)] bg-[var(--color-danger-soft)] px-4 py-3 text-sm text-[var(--color-text-primary)]">{error}</div>}

      <div className="flex flex-col gap-4 lg:flex-row lg:items-start">
        <div className="w-full shrink-0 lg:w-1/3">
          <Panel className="flex h-[750px] flex-col overflow-hidden">
            <div className="flex shrink-0 items-center justify-between border-b border-[var(--color-border)] px-4 py-3">
              <SectionTitle>Admission applications</SectionTitle>
              <span className="text-[11px] font-medium text-[var(--color-text-muted)]">{applications.length} results</span>
            </div>
            <div className="flex-1 overflow-y-auto scroll-slim">
              {applications.length === 0 ? (
                <EmptyState title={total ? 'No matching applications' : 'No applications'} description="Change the filters or clear them to see other records." action={<Button size="sm" onClick={clear}>Clear filters</Button>} />
              ) : (
                <ul className="divide-y divide-[var(--color-border)]">
                  {applications.map((admission) => {
                    const selected = selectedAdmissionId === admission.admissionId;
                    return (
                      <li key={admission.admissionId}>
                        <button type="button" onClick={() => setSelectedAdmissionId(admission.admissionId)} className={`flex w-full items-start gap-3 px-4 py-3 text-left transition-colors outline-none ${selected ? 'bg-[var(--color-primary-subtle)]' : 'hover:bg-[var(--color-surface-subtle)]'}`}>
                          <div className="min-w-0 flex-1">
                            <div className="mb-1 flex items-center justify-between gap-2">
                              <span className="truncate text-[13px] font-bold text-[var(--color-text-primary)]">{admission.candidateName}</span>
                              <Pill tone={statusTone(admission.status)} size="sm">{prettyStatus(admission.status)}</Pill>
                            </div>
                            <div className="truncate text-[11px] text-[var(--color-text-muted)]">{admission.breed || 'Breed not provided'} · Submitted {new Date(admission.submittedAt).toLocaleDateString()}</div>
                          </div>
                        </button>
                      </li>
                    );
                  })}
                </ul>
              )}
            </div>
            {result && result.totalElements > 0 && (
              <div className="flex shrink-0 flex-wrap items-center justify-between gap-2 border-t border-[var(--color-border)] px-3 py-2 text-xs text-[var(--color-text-secondary)]">
                <span>Showing {result.page * result.size + 1}-{Math.min((result.page + 1) * result.size, result.totalElements)} of {result.totalElements}</span>
                <div className="flex items-center gap-1">
                  <Button size="sm" icon="chevron-left" aria-label="Previous page" disabled={result.page <= 0} onClick={() => changePage(result.page - 1)} />
                  <span className="min-w-16 text-center">{result.page + 1}/{Math.max(1, result.totalPages)}</span>
                  <Button size="sm" iconRight="chevron-right" aria-label="Next page" disabled={result.page + 1 >= result.totalPages} onClick={() => changePage(result.page + 1)} />
                </div>
              </div>
            )}
          </Panel>
        </div>

        <div className="w-full lg:flex-1">
          {selectedAdmissionId ? (
            <Panel className="min-h-[750px] overflow-hidden p-0">
              <GroomAdmissionReview
                admissionId={selectedAdmissionId}
                returnTo={`/groom/admissions${toQuery(applied)}`}
                embedded
                onUpdated={() => setReviewVersion((value) => value + 1)}
              />
            </Panel>
          ) : (
            <div className="flex h-[750px] items-center justify-center rounded-[var(--radius-md)] border-2 border-dashed border-[var(--color-border)]">
              <div className="text-center text-[var(--color-text-muted)]">
                <Icon name="clipboard" size={32} className="mx-auto mb-3 opacity-50" />
                <p className="text-[14px] font-medium text-[var(--color-text-secondary)]">Select an application</p>
                <p className="mt-1 text-[12px]">Choose an application from the list to view details</p>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
