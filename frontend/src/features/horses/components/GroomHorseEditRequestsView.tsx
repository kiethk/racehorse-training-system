'use client';

import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  EmptyState,
  FilterChips,
  LinkButton,
  ListSkeleton,
  PageHeader,
  Pill,
  ScreenLayout,
  Table,
  TBody,
  Td,
  Th,
  THead,
  Tr,
} from '@/components/ui';
import { displayError, formatDateTime } from '@/lib/display';
import { horseEditRequestService } from '../services/horseEditRequestService';
import { diffFields } from '../lib/horseEdit';
import type { HorseEditRequest, HorseEditRequestStatus } from '../types';

export const EDIT_STATUS_TONE: Record<HorseEditRequestStatus, 'warning' | 'success' | 'danger' | 'neutral'> = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  CANCELLED: 'neutral',
};

export const EDIT_STATUS_LABEL: Record<HorseEditRequestStatus, string> = {
  PENDING: 'Waiting for review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  CANCELLED: 'Cancelled',
};

const FILTERS = [
  { value: 'PENDING', label: 'Waiting for review' },
  { value: '', label: 'All' },
];

/** Change requests for horses in the groom's stalls, oldest first. */
export function GroomHorseEditRequestsView() {
  const [status, setStatus] = useState<string>('PENDING');
  const [items, setItems] = useState<HorseEditRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setItems(await horseEditRequestService.listForGroom((status || undefined) as HorseEditRequestStatus | undefined));
    } catch (cause) {
      setError(displayError(cause, 'Unable to load change requests.'));
    } finally {
      setLoading(false);
    }
  }, [status]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void load();
  }, [load]);

  return (
    <ScreenLayout>
      <PageHeader
        title="Horse change requests"
        description="Owners ask to update details or documents of horses in your stalls. Changes apply only after you approve them."
      />
      <FilterChips label="Status" options={FILTERS} value={status} onChange={setStatus} />
      {loading && <ListSkeleton rows={5} />}
      {!loading && error && (
        <EmptyState icon="alert-triangle" title="Unable to load change requests" description={error}
          action={<Button size="sm" onClick={() => void load()}>Retry</Button>} />
      )}
      {!loading && !error && items.length === 0 && (
        <EmptyState icon="check" title="No change requests waiting for you"
          description="New requests from owners of horses in your stalls appear here." />
      )}
      {!loading && !error && items.length > 0 && (
        <Table>
          <THead>
            <Tr>
              <Th>Horse</Th>
              <Th>Owner</Th>
              <Th>Submitted</Th>
              <Th>Changes</Th>
              <Th>Status</Th>
              <Th><span className="sr-only">Actions</span></Th>
            </Tr>
          </THead>
          <TBody>
            {items.map((item) => {
              const fields = diffFields(item.original, item.requested).length;
              return (
                <Tr key={item.id}>
                  <Td className="font-medium">{item.horseName ?? `Horse #${item.horseId}`}</Td>
                  <Td>{item.ownerName ?? `Owner #${item.ownerId}`}</Td>
                  <Td>{formatDateTime(item.createdAt)}</Td>
                  <Td>
                    {[fields > 0 && `${fields} field${fields > 1 ? 's' : ''}`,
                      item.documents.length > 0 && `${item.documents.length} document${item.documents.length > 1 ? 's' : ''}`]
                      .filter(Boolean).join(' · ') || '—'}
                  </Td>
                  <Td><Pill tone={EDIT_STATUS_TONE[item.status]} size="sm">{EDIT_STATUS_LABEL[item.status]}</Pill></Td>
                  <Td className="text-right">
                    <LinkButton href={`/groom/horse-edit-requests/${item.id}`} size="sm"
                      variant={item.status === 'PENDING' ? 'primary' : 'secondary'}>
                      {item.status === 'PENDING' ? 'Review' : 'View'}
                    </LinkButton>
                  </Td>
                </Tr>
              );
            })}
          </TBody>
        </Table>
      )}
    </ScreenLayout>
  );
}
