'use client';

import {useEffect, useMemo, useState} from 'react';
import {usePathname, useRouter, useSearchParams} from 'next/navigation';
import {Button} from '@/components/ui/Button';
import {Panel} from '@/components/ui/Panel';
import {Icon} from '@/components/ui/Icon';
import {EmptyState, ListSkeleton} from '@/components/ui/states';
import {admissionsApi} from '../services/api';
import type {AdmissionStatus, AdmissionSummaryResponse} from '../types';
import {AdmissionTable} from '../shared/components/AdmissionTable';

const VALID_STATUSES: ReadonlyArray<AdmissionStatus | 'ALL'> = [
  'ALL',
  'GROOM_REVIEW',
  'WAITING_FOR_STALL',
  'VET_REVIEW',
  'PENDING_RECHECK',
  'TRAINER_REVIEW',
  'MANAGER_REVIEW',
  'APPROVED',
  'REJECTED',
];

const FILTER_KEYS = ['candidateName', 'status', 'submittedFrom', 'submittedTo'] as const;

interface ManagerQueueFilters {
  candidateName: string;
  status: AdmissionStatus | 'ALL';
  submittedFrom: string;
  submittedTo: string;
}

const initialFilters: ManagerQueueFilters = {
  candidateName: '',
  status: 'ALL',
  submittedFrom: '',
  submittedTo: '',
};

export function ManagerAdmissionsListView() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const urlFilters: ManagerQueueFilters = useMemo(() => {
    const rawStatus = searchParams.get('status') as AdmissionStatus | 'ALL' | null;
    const safeStatus: AdmissionStatus | 'ALL' =
      rawStatus && (VALID_STATUSES as ReadonlyArray<string>).includes(rawStatus) ? rawStatus : 'ALL';
    return {
      candidateName: searchParams.get('candidateName') || '',
      status: safeStatus,
      submittedFrom: searchParams.get('submittedFrom') || '',
      submittedTo: searchParams.get('submittedTo') || '',
    };
  }, [searchParams]);

  const [admissions, setAdmissions] = useState<AdmissionSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  const [draft, setDraft] = useState<ManagerQueueFilters>(urlFilters);

  useEffect(() => {
    let active = true;
    async function loadQueue() {
      try {
        setLoading(true);
        setError(null);
        const data = await admissionsApi.getAdmissions();
        if (active) setAdmissions(data);
      } catch (err) {
        console.error('Failed to load manager queue:', err);
        if (active) setError('Failed to load admissions. Please try again.');
      } finally {
        if (active) setLoading(false);
      }
    }
    loadQueue();
    return () => { active = false; };
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setDraft(urlFilters);
  }, [urlFilters]);

  const filteredAdmissions = useMemo(() => {
    return admissions
      .filter((a) => {
        if (urlFilters.status !== 'ALL' && a.status !== urlFilters.status) return false;
        if (urlFilters.candidateName && !a.candidateName.toLowerCase().includes(urlFilters.candidateName.toLowerCase())) return false;
        
        if (urlFilters.submittedFrom || urlFilters.submittedTo) {
          const submittedTime = new Date(a.submittedAt).getTime();
          if (urlFilters.submittedFrom) {
            const fromTime = new Date(urlFilters.submittedFrom).getTime();
            if (submittedTime < fromTime) return false;
          }
          if (urlFilters.submittedTo) {
            const toDate = new Date(urlFilters.submittedTo);
            toDate.setHours(23, 59, 59, 999);
            if (submittedTime > toDate.getTime()) return false;
          }
        }
        
        return true;
      })
      .sort((a, b) => new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime());
  }, [admissions, urlFilters]);

  const updateUrl = (filters: ManagerQueueFilters) => {
    const params = new URLSearchParams();
    if (filters.candidateName) params.set('candidateName', filters.candidateName.trim());
    if (filters.status !== 'ALL') params.set('status', filters.status);
    if (filters.submittedFrom) params.set('submittedFrom', filters.submittedFrom);
    if (filters.submittedTo) params.set('submittedTo', filters.submittedTo);
    const qs = params.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname);
  };

  const apply = () => {
    setError(null);
    updateUrl({ ...draft, candidateName: draft.candidateName.trim() });
  };

  const clear = () => {
    setDraft(initialFilters);
    setError(null);
    updateUrl(initialFilters);
  };

  if (loading) {
    return (
      <Panel>
        <ListSkeleton rows={10} />
      </Panel>
    );
  }

  const statuses: { value: AdmissionStatus | 'ALL'; label: string }[] = [
    { value: 'ALL', label: 'All statuses' },
    { value: 'GROOM_REVIEW', label: 'Groom Review' },
    { value: 'WAITING_FOR_STALL', label: 'Waiting for Stall' },
    { value: 'VET_REVIEW', label: 'Vet Review' },
    { value: 'PENDING_RECHECK', label: 'Pending Recheck' },
    { value: 'TRAINER_REVIEW', label: 'Trainer Review' },
    { value: 'MANAGER_REVIEW', label: 'Manager Review' },
    { value: 'APPROVED', label: 'Approved' },
    { value: 'REJECTED', label: 'Rejected' },
  ];
  
  // Only forward the known Admission filter keys — do not blindly copy all params
  const detailParams = new URLSearchParams();
  FILTER_KEYS.forEach((key) => {
    const val = searchParams.get(key);
    if (val) detailParams.set(key, val);
  });
  const detailQuery = detailParams.toString() ? `?${detailParams.toString()}` : '';

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-[20px] font-semibold tracking-tight text-[var(--color-text-primary)]">
          Admissions
        </h1>
      </div>

      <form
        className="grid gap-3 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-3 sm:grid-cols-2 xl:grid-cols-[minmax(220px,1.4fr)_minmax(170px,1fr)_minmax(150px,1fr)_minmax(150px,1fr)_auto] xl:items-end"
        onSubmit={(event) => { event.preventDefault(); apply(); }}
      >
        <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
          Search horse name
          <span className="relative mt-1.5 block">
            <Icon name="search" size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--color-text-muted)]" />
            <input value={draft.candidateName} onChange={(event) => setDraft({ ...draft, candidateName: event.target.value })} placeholder="Search horse name" className="h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] pl-9 pr-3 text-sm text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]" />
          </span>
        </label>
        <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
          Status
          <select value={draft.status} onChange={(event) => setDraft({ ...draft, status: event.target.value as AdmissionStatus | 'ALL' })} className="mt-1.5 h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-sm text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]">
            {statuses.map((status) => <option key={status.value} value={status.value}>{status.label}</option>)}
          </select>
        </label>
        <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
          From
          <input type="date" value={draft.submittedFrom} onChange={(event) => setDraft({ ...draft, submittedFrom: event.target.value })} className="mt-1.5 h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-sm text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]" />
        </label>
        <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
          To
          <input type="date" min={draft.submittedFrom || undefined} value={draft.submittedTo} onChange={(event) => setDraft({ ...draft, submittedTo: event.target.value })} className="mt-1.5 h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-sm text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]" />
        </label>
        <div className="flex items-center gap-2 sm:col-span-2 xl:col-span-1">
          <Button type="submit" variant="primary" size="sm">Apply</Button>
          <Button type="button" variant="secondary" size="sm" onClick={clear}>Clear</Button>
        </div>
      </form>

      {error && <div role="alert" className="border-l-2 border-[var(--color-danger)] bg-[var(--color-danger-soft)] px-4 py-3 text-sm text-[var(--color-text-primary)]">{error}</div>}

      <Panel className="overflow-hidden">
        {filteredAdmissions.length === 0 ? (
            <EmptyState
                title="No matching applications"
                description="Change the filters or clear them to see other records."
                action={
                  <Button size="sm" onClick={clear}>
                    Clear filters
                  </Button>
                }
            />
        ) : (
            <AdmissionTable
                admissions={filteredAdmissions}
                detailHref={id => `/manager/admissions/${id}${detailQuery}`}
            />
        )}
      </Panel>
    </div>
  );
}
