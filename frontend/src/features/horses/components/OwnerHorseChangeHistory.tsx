import { EntityLink, Panel, Table, TBody, Td, Th, THead, Tr } from '@/components/ui';
import { entityHref } from '@/lib/entityRoute';
import { formatDate } from '@/lib/display';
import { diffFields } from '../lib/horseEdit';
import type { HorseEditRequest } from '../types';
import { EditRequestStatusBadge } from './GroomHorseEditRequestsView';

export function OwnerHorseChangeHistory({ requests }: { requests: HorseEditRequest[] }) {
  return (
    <Panel padded className="space-y-4">
      <h3 className="text-base font-semibold text-[var(--color-text-primary)]">Change history</h3>
      {requests.length === 0 ? (
        <p className="text-sm text-[var(--color-text-muted)]">No change requests yet</p>
      ) : (
        <ol className="divide-y divide-[var(--color-border)]">
          {requests.map((request) => {
            const changes = diffFields(request.original, request.requested);
            return (
              <li key={request.id} className="space-y-3 py-4 first:pt-0 last:pb-0">
                <div className="flex flex-wrap items-center gap-x-5 gap-y-2 text-sm text-[var(--color-text-secondary)]">
                  <EditRequestStatusBadge status={request.status} />
                  <span>Submitted {formatDate(request.createdAt)}</span>
                  {request.reviewedAt && <span>Reviewed {formatDate(request.reviewedAt)}</span>}
                  {request.reviewerName && <span>By <EntityLink href={request.reviewerId ? entityHref('user', request.reviewerId, 'HORSE_OWNER') : null}>{request.reviewerName}</EntityLink></span>}
                </div>
                {request.reviewFeedback && <p className="text-sm text-[var(--color-text-secondary)]">Feedback: {request.reviewFeedback}</p>}
                {changes.length > 0 && (
                  <details className="group text-sm">
                    <summary className="cursor-pointer font-medium text-[var(--color-primary)] focus-visible:outline-2 focus-visible:outline-[var(--color-focus)]">
                      View changed fields
                    </summary>
                    <Table bare className="mt-3" aria-label={`Changed fields for request ${request.id}`}>
                      <THead><Tr><Th>Field</Th><Th>Before</Th><Th>After</Th></Tr></THead>
                      <TBody>
                        {changes.map((change) => (
                          <Tr key={change.field}>
                            <Td>{change.label}</Td>
                            <Td>{change.from || '—'}</Td>
                            <Td>{change.to || '—'}</Td>
                          </Tr>
                        ))}
                      </TBody>
                    </Table>
                  </details>
                )}
              </li>
            );
          })}
        </ol>
      )}
    </Panel>
  );
}
