'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, DataTable, EmptyState, HorseAvatar, LinkButton, ListSkeleton, Notice, Pill } from '@/components/ui';
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

const statuses: { value: HorseEditRequestStatus | ''; label: string }[] = [
  { value: '', label: 'All statuses' },
  { value: 'PENDING', label: 'Groom review' },
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
const initialFilters: Filters = { ...emptyFilters, status: 'PENDING' };

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
  const [draft, setDraft] = useState<Filters>(initialFilters);
  const [applied, setApplied] = useState<Filters>(initialFilters);
  const [items, setItems] = useState<HorseEditRequest[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      // Status filters on the server; search and dates filter the (already scoped) list here.
      setItems(await horseEditRequestService.listForGroom(applied.status || undefined));
    } catch (cause) {
      setError(displayError(cause, 'Unable to load change requests.'));
    } finally {
      setLoading(false);
    }
  }, [applied.status]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void load();
  }, [load]);

  const rows = useMemo(() => {
    const term = applied.search.trim().toLowerCase();
    return (items ?? []).filter((item) => {
      const day = item.createdAt.slice(0, 10);
      if (applied.from && day < applied.from) return false;
      if (applied.to && day > applied.to) return false;
      if (!term) return true;
      return `${item.horseName ?? ''} ${item.ownerName ?? ''} ${item.horseRegistrationNumber ?? ''}`.toLowerCase().includes(term);
    });
  }, [items, applied]);

  const columns: DataTableColumn<HorseEditRequest>[] = [
    {
      id: 'horse',
      header: 'Horse',
      sortValue: (item) => item.horseName ?? '',
      render: (item) => (
        <div className="flex min-w-0 items-center gap-3">
          <HorseAvatar name={item.horseName ?? `Horse #${item.horseId}`} image={`/api/horses/${item.horseId}/photo`} size={32} rounded="md" />
          <div className="min-w-0">
            <span className="block truncate font-semibold">{item.horseName ?? `Horse #${item.horseId}`}</span>
            <span className="block truncate text-xs text-[var(--color-text-muted)]">{item.horseRegistrationNumber || 'UELN not recorded'}</span>
          </div>
        </div>
      ),
    },
    { id: 'status', header: 'Status', sortValue: (item) => item.status, render: (item) => <EditRequestStatusBadge status={item.status} /> },
    { id: 'owner', header: 'Owner', sortValue: (item) => item.ownerName ?? '', render: (item) => item.ownerName ?? `#${item.ownerId}` },
    { id: 'changes', header: 'Changes', render: changeSummary },
    { id: 'submitted', header: 'Submitted', sortValue: (item) => new Date(item.createdAt), render: (item) => formatDate(item.createdAt) },
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
      <AdmissionFilterBar
        search={draft.search}
        searchLabel="Search"
        searchPlaceholder="Horse, owner or UELN"
        onSearchChange={(search) => setDraft({ ...draft, search })}
        onApply={() => setApplied({ ...draft, search: draft.search.trim() })}
        onClear={() => { setDraft(emptyFilters); setApplied(emptyFilters); }}
      >
        <FilterSelect label="Status" value={draft.status} onChange={(status) => setDraft({ ...draft, status })} options={statuses} />
        <FilterDateRange
          from={draft.from}
          to={draft.to}
          onFromChange={(from) => setDraft({ ...draft, from })}
          onToChange={(to) => setDraft({ ...draft, to })}
        />
      </AdmissionFilterBar>

      {error && <Notice tone="error">{error}</Notice>}

      <DataTable
        rows={rows}
        columns={columns}
        getRowKey={(item) => item.id}
        ariaLabel="Horse change requests"
        loading={loading}
        pageSize={10}
        emptyTitle={filtered ? 'No matching change requests' : 'No change requests'}
        emptyDescription={filtered
          ? 'Change the filters or clear them to see other requests.'
          : 'Requests from owners of horses in your stalls will appear here.'}
      />
    </AdmissionListLayout>
  );
}
