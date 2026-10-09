'use client';

import {useEffect, useMemo, useState} from 'react';
import {usePathname, useRouter, useSearchParams} from 'next/navigation';
import {Button} from '@/components/ui/Button';
import {ListSkeleton} from '@/components/ui/states';
import {Notice} from '@/components/ui/Notice';
import {admissionsApi} from '../services/api';
import type {AdmissionStatus, AdmissionSummaryResponse} from '../types';
import {AdmissionTable} from '../shared/components/AdmissionTable';
import {AdmissionListLayout} from '../shared/components/AdmissionListLayout';
import {AdmissionFilterBar, FilterDateRange, FilterSelect} from '../shared/components/AdmissionFilterBar';

const VALID_STATUSES: ReadonlyArray<AdmissionStatus | 'ALL'> = [
  'ALL',
  'GROOM_REVIEW',
  'WAITING_FOR_STALL',
  'WAITING_FOR_ARRIVAL',
  'ARRIVAL_EXPIRED',
  'VET_REVIEW',
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
      <AdmissionListLayout title="Admissions" description="Review and manage horse admission applications.">
        <ListSkeleton rows={10} />
      </AdmissionListLayout>
    );
  }

  const statuses: { value: AdmissionStatus | 'ALL'; label: string }[] = [
    { value: 'ALL', label: 'All statuses' },
    { value: 'GROOM_REVIEW', label: 'Groom Review' },
    { value: 'WAITING_FOR_STALL', label: 'Waiting for Stall' },
    { value: 'WAITING_FOR_ARRIVAL', label: 'Waiting for Arrival' },
    { value: 'ARRIVAL_EXPIRED', label: 'Arrival Expired' },
    { value: 'VET_REVIEW', label: 'Vet Review' },
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
    <AdmissionListLayout title="Admissions" description="Review and manage horse admission applications.">
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
        admissions={filteredAdmissions}
        detailHref={id => `/manager/admissions/${id}${detailQuery}`}
        searchable={false}
        emptyTitle="No matching applications"
        emptyDescription="Change the filters or clear them to see other records."
        emptyAction={<Button size="sm" onClick={clear}>Clear filters</Button>}
      />
    </AdmissionListLayout>
  );
}
