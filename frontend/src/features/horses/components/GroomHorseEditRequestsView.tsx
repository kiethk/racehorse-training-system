'use client';

import { useRouter } from 'next/navigation';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, DataTable, EmptyState, EntityLink, HorseAvatar, LinkButton, ListSkeleton, Notice, Pill, Tabs } from '@/components/ui';
import { entityHref } from '@/lib/entityRoute';
import type { DataTableColumn } from '@/components/ui/DataTable';
import { displayError, formatDate } from '@/lib/display';
import { AdmissionListLayout } from '@/features/admissions/shared/components/AdmissionListLayout';
import { AdmissionFilterBar, FilterDateRange, FilterSelect } from '@/features/admissions/shared/components/AdmissionFilterBar';
import { horseEditRequestService } from '../services/horseEditRequestService';
import { diffFields } from '../lib/horseEdit';
import type { HorseEditRequest, HorseEditRequestStatus } from '../types';

export const EDIT_STATUS_TONE: Record<HorseEditRequestStatus, 'primary' | 'success' | 'danger' | 'neutral'> = {
  PENDING: 'primary',
  APPROVED: 'success',
  REJECTED: 'danger',
  CANCELLED: 'neutral',
};

export const EDIT_STATUS_LABEL: Record<HorseEditRequestStatus, string> = {
  PENDING: 'Groom review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  CANCELLED: 'Cancelled',
};

export function EditRequestStatusBadge({ status }: { status: HorseEditRequestStatus }) {
  return <Pill tone={EDIT_STATUS_TONE[status]} size="sm">{EDIT_STATUS_LABEL[status]}</Pill>;
}

const historyStatuses: { value: HorseEditRequestStatus | ''; label: string }[] = [
  { value: '', label: 'All statuses' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'CANCELLED', label: 'Cancelled' },
];

interface Filters {
  search: string;
  status: HorseEditRequestStatus | '';
  from: string;
  to: string;
}

const emptyFilters: Filters = { search: '', status: '', from: '', to: '' };

function changeSummary(request: HorseEditRequest): string {
  const fields = diffFields(request.original, request.requested).length;
  const docs = request.documents.length;
  return [
    fields > 0 && `${fields} field${fields > 1 ? 's' : ''}`,
    docs > 0 && `${docs} document${docs > 1 ? 's' : ''}`,
  ].filter(Boolean).join(' · ') || '—';
}

/** Change requests for horses in the groom's stalls; same layout as the groom admission list. */
export function GroomHorseEditRequestsView() {
  const router = useRouter();
  const [tab, setTab] = useState<'pending' | 'history'>('pending');
  const [draft, setDraft] = useState<Filters>(emptyFilters);
  const [applied, setApplied] = useState<Filters>(emptyFilters);
  const [items, setItems] = useState<HorseEditRequest[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setItems(await horseEditRequestService.listForGroom());
    } catch (cause) {
      setError(displayError(cause, 'Unable to load change requests.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void load();
  }, [load]);

  const rows = useMemo(() => {
    const term = applied.search.trim().toLowerCase();
    return (items ?? []).filter((item) => {
      if (tab === 'pending' && item.status !== 'PENDING') return false;
      if (tab === 'history' && (item.status === 'PENDING' || (applied.status && item.status !== applied.status))) return false;
      const day = item.createdAt.slice(0, 10);
      if (applied.from && day < applied.from) return false;
      if (applied.to && day > applied.to) return false;
      if (!term) return true;
      return `${item.horseName ?? ''} ${item.ownerName ?? ''} ${item.horseRegistrationNumber ?? ''}`.toLowerCase().includes(term);
    });
  }, [items, applied, tab]);

  const changeTab = (next: string) => {
    setTab(next as 'pending' | 'history');
    setDraft((current) => ({ ...current, status: '' }));
    setApplied((current) => ({ ...current, status: '' }));
  };

  const columns: DataTableColumn<HorseEditRequest>[] = [
    {
      id: 'horse',
      header: 'Horse',
      sortValue: (item) => item.horseName ?? '',
      render: (item) => (
        <div className="flex min-w-0 items-center gap-3">
          <HorseAvatar name={item.horseName ?? `Horse #${item.horseId}`} image={`/api/horses/${item.horseId}/photo`} size={32} rounded="md" />
          <div className="min-w-0">
            <EntityLink href={entityHref('horse', item.horseId, 'GROOM')} className="block truncate font-semibold">{item.horseName ?? `Horse #${item.horseId}`}</EntityLink>
            <span className="block truncate text-xs text-[var(--color-text-muted)]">{item.horseRegistrationNumber || 'UELN not recorded'}</span>
          </div>
        </div>
      ),
    },
    { id: 'status', header: 'Status', sortValue: (item) => item.status, render: (item) => <EditRequestStatusBadge status={item.status} /> },
    { id: 'owner', header: 'Owner', sortValue: (item) => item.ownerName ?? '', render: (item) => <EntityLink href={entityHref('owner', item.ownerId, 'GROOM')}>{item.ownerName ?? `#${item.ownerId}`}</EntityLink> },
    { id: 'changes', header: 'Changes', render: changeSummary },
    { id: 'submitted', header: 'Submitted', sortValue: (item) => new Date(item.createdAt), render: (item) => formatDate(item.createdAt) },
    ...(tab === 'history' ? [
      { id: 'reviewer', header: 'Reviewer', sortValue: (item: HorseEditRequest) => item.reviewerName ?? '', render: (item: HorseEditRequest) => <EntityLink href={item.reviewerId ? entityHref('user', item.reviewerId, 'GROOM') : null}>{item.reviewerName ?? '—'}</EntityLink> },
      { id: 'reviewed', header: 'Reviewed', sortValue: (item: HorseEditRequest) => item.reviewedAt ? new Date(item.reviewedAt) : new Date(0), render: (item: HorseEditRequest) => item.reviewedAt ? formatDate(item.reviewedAt) : '—' },
    ] satisfies DataTableColumn<HorseEditRequest>[] : []),
    {
      id: 'actions',
      header: 'Action',
      align: 'right',
      render: (item) => (
        <LinkButton href={`/groom/horse-edit-requests/${item.id}`} size="sm"
          variant={item.status === 'PENDING' ? 'primary' : 'secondary'} className="min-w-[4.5rem]">
          {item.status === 'PENDING' ? 'Review' : 'View'}
        </LinkButton>
      ),
    },
  ];

  const title = 'Horse change requests';
  if (loading && !items) return <AdmissionListLayout title={title} ><ListSkeleton rows={8} /></AdmissionListLayout>;
  if (error && !items) {
    return (
      <AdmissionListLayout title={title} >
        <EmptyState icon="alert-triangle" title="Unable to load change requests" description={error}
          action={<Button size="sm" onClick={() => void load()}>Retry</Button>} />
      </AdmissionListLayout>
    );
  }

  const filtered = (items?.length ?? 0) > 0;
  return (
    <AdmissionListLayout title={title} >
      <Tabs tabs={[{ id: 'pending', label: 'Pending' }, { id: 'history', label: 'History' }]} active={tab} onChange={changeTab} />
      <AdmissionFilterBar
        search={draft.search}
        searchLabel="Search"
        searchPlaceholder="Horse, owner or UELN"
        onSearchChange={(search) => setDraft({ ...draft, search })}
        onApply={() => setApplied({ ...draft, search: draft.search.trim() })}
        onClear={() => { setDraft(emptyFilters); setApplied(emptyFilters); }}
      >
        {tab === 'history' && <FilterSelect label="Status" value={draft.status} onChange={(status) => setDraft({ ...draft, status })} options={historyStatuses} />}
        <FilterDateRange
          from={draft.from}
          to={draft.to}
          onFromChange={(from) => setDraft({ ...draft, from })}
          onToChange={(to) => setDraft({ ...draft, to })}
        />
      </AdmissionFilterBar>

      {error && <Notice tone="error">{error}</Notice>}

      <DataTable
        key={`${tab}:${applied.status}:${applied.search}:${applied.from}:${applied.to}`}
        rows={rows}
        columns={columns}
        getRowKey={(item) => item.id}
        onRowClick={(item) => router.push(`/groom/horse-edit-requests/${item.id}`)}
        ariaLabel="Horse change requests"
        loading={loading}
        pageSize={10}
        emptyTitle={filtered ? 'No matching change requests' : 'No change requests'}
      />
    </AdmissionListLayout>
  );
}
