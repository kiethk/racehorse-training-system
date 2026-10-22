'use client';

import { EmptyState, Panel, Pill, Table, TBody, Td, Th, THead, Tr } from '@/components/ui';
import { DueStatusPill } from '@/features/care/components/preventive/preventiveDisplay';
import type { PreventiveProtocol, PreventiveProtocolStatus } from '@/features/care/types/preventive';
import { formatDate } from '@/lib/display';

/**
 * Preventive vaccination status per protocol for one horse, with the next due date.
 * A protocol that was never administered has no due date yet; its interval is shown instead.
 */
export function HorsePreventivePanel({
  protocols,
  protocolDefinitions,
}: {
  protocols: PreventiveProtocolStatus[];
  protocolDefinitions: PreventiveProtocol[];
}) {
  const intervalByProtocol = new Map(protocolDefinitions.map((protocol) => [protocol.id, protocol.intervalDays]));

  if (protocols.length === 0) {
    return (
      <Panel padded>
        <EmptyState
          icon="shield"
          title="No preventive protocol"
          description="No vaccination or deworming protocol is active for this stable."
        />
      </Panel>
    );
  }

  return (
    <Panel padded className="space-y-3">
      <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">Preventive vaccination</h3>

      <Table bare>
        <THead>
          <Tr>
            <Th>Protocol</Th>
            <Th>Last dose</Th>
            <Th>Next due</Th>
            <Th>Status</Th>
          </Tr>
        </THead>
        <TBody>
          {protocols.map((protocol) => {
            const interval = intervalByProtocol.get(protocol.protocolId);
            return (
              <Tr key={protocol.protocolId}>
                <Td>
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-medium">{protocol.name}</span>
                    <Pill tone="neutral" size="sm">
                      {protocol.category === 'DEWORMING' ? 'Deworming' : 'Vaccine'}
                    </Pill>
                    {protocol.mandatory && (
                      <Pill tone="info" size="sm">
                        Mandatory
                      </Pill>
                    )}
                  </div>
                  {interval ? (
                    <span className="text-xs text-[var(--color-text-muted)]">Every {interval} days</span>
                  ) : null}
                </Td>
                <Td className="whitespace-nowrap text-[var(--color-text-secondary)]">
                  {formatDate(protocol.lastDoseOn)}
                </Td>
                <Td className="whitespace-nowrap font-medium">
                  {protocol.nextDueOn ? formatDate(protocol.nextDueOn) : 'No record yet'}
                </Td>
                <Td>
                  <DueStatusPill status={protocol.status} />
                </Td>
              </Tr>
            );
          })}
        </TBody>
      </Table>
    </Panel>
  );
}
