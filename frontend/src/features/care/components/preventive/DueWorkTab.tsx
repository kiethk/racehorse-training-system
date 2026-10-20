'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Button,
  EmptyState,
  FilterChips,
  FormField,
  ListSkeleton,
  MetricCard,
  Notice,
  Panel,
  Pill,
  Select,
  StatusBadge,
  Table,
  TBody,
  Td,
  Th,
  THead,
  Tr,
} from '@/components/ui';
import { displayError, formatDate } from '@/lib/display';
import { preventiveCareService } from '../../services/preventiveCareService';
import type { PreventiveDueItem, PreventiveDueStatus, PreventiveProtocol } from '../../types/preventive';
import { RecordVaccinationModal } from './RecordVaccinationModal';
import { DueStatusPill } from './preventiveDisplay';

type Filter = 'ACTION' | 'OVERDUE' | 'DUE_SOON' | 'OK';

const FILTER_STATUSES: Record<Filter, PreventiveDueStatus[]> = {
  ACTION: ['OVERDUE', 'MISSING', 'DUE_SOON'],
  OVERDUE: ['OVERDUE', 'MISSING'],
  DUE_SOON: ['DUE_SOON'],
  OK: ['OK'],
};

const rowKey = (item: PreventiveDueItem) => `${item.horseId}:${item.protocol.protocolId}`;

interface DueWorkTabProps {
  protocols: PreventiveProtocol[];
  canRecord: boolean;
}

export function DueWorkTab({ protocols, canRecord }: DueWorkTabProps) {
  const [items, setItems] = useState<PreventiveDueItem[]>([]);
  const [summary, setSummary] = useState<PreventiveDueItem[]>([]);
  const [filter, setFilter] = useState<Filter>('ACTION');
  const [protocolId, setProtocolId] = useState<number | undefined>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [recording, setRecording] = useState<PreventiveDueItem | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [rows, actionable] = await Promise.all([
        preventiveCareService.listDue({ statuses: FILTER_STATUSES[filter], protocolId }),
        preventiveCareService.listDue({ statuses: FILTER_STATUSES.ACTION }),
      ]);
      setItems(rows);
      setSummary(actionable);
    } catch (cause) {
      setError(displayError(cause, 'Could not load preventive care work.'));
    } finally {
      setLoading(false);
    }
  }, [filter, protocolId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const counts = useMemo(() => ({
    overdue: summary.filter((i) => i.protocol.status === 'OVERDUE' || i.protocol.status === 'MISSING').length,
    dueSoon: summary.filter((i) => i.protocol.status === 'DUE_SOON').length,
    horses: new Set(summary.map((i) => i.horseId)).size,
  }), [summary]);

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <MetricCard label="Overdue or no record" value={counts.overdue} icon="alert-triangle" tone="danger"
          onClick={() => setFilter('OVERDUE')} active={filter === 'OVERDUE'} />
        <MetricCard label="Due soon" value={counts.dueSoon} icon="clock" tone="warning"
          onClick={() => setFilter('DUE_SOON')} active={filter === 'DUE_SOON'} />
        <MetricCard label="Horses needing care" value={counts.horses} icon="horse"
          onClick={() => setFilter('ACTION')} active={filter === 'ACTION'} />
      </div>

      <Panel padded className="flex flex-wrap items-end justify-between gap-3">
        <FilterChips<Filter>
          label="Status"
          value={filter}
          onChange={setFilter}
          options={[
            { value: 'ACTION', label: 'Needs action' },
            { value: 'OVERDUE', label: 'Overdue' },
            { value: 'DUE_SOON', label: 'Due soon' },
            { value: 'OK', label: 'Up to date' },
          ]}
        />
        <div className="flex items-end gap-2">
          <FormField label="Protocol" className="w-56">
            <Select value={protocolId ?? ''} onChange={(e) => setProtocolId(e.target.value ? Number(e.target.value) : undefined)}>
              <option value="">All protocols</option>
              {protocols.map((protocol) => (
                <option key={protocol.id} value={protocol.id}>{protocol.name}</option>
              ))}
            </Select>
          </FormField>
          <Button variant="secondary" icon="refresh" onClick={load} disabled={loading}>Refresh</Button>
        </div>
      </Panel>

      {error && <Notice tone="error" title="Could not load" onDismiss={() => setError(null)}>{error}</Notice>}

      {loading ? (
        <ListSkeleton />
      ) : items.length === 0 ? (
        <Panel padded>
          <EmptyState icon="check" title="Nothing here"
            description={filter === 'OK' ? 'No horse is up to date for this filter yet.' : 'Every horse is covered for this filter.'} />
        </Panel>
      ) : (
        <Panel>
          <Table bare aria-label="Preventive care work">
            <THead>
              <Tr>
                <Th>Horse</Th>
                <Th>Protocol</Th>
                <Th>Last dose</Th>
                <Th>Next due</Th>
                <Th>Status</Th>
                {canRecord && <Th className="w-px"><span className="sr-only">Actions</span></Th>}
              </Tr>
            </THead>
            <TBody>
              {items.map((item) => (
                <Tr key={rowKey(item)}>
                  <Td>
                    <div className="flex items-center gap-2">
                      <span className="font-semibold">{item.horseName}</span>
                      <StatusBadge status={item.horseStatus} size="sm" />
                    </div>
                  </Td>
                  <Td>
                    <div className="flex items-center gap-2">
                      <span>{item.protocol.name}</span>
                      {item.protocol.mandatory && <Pill tone="info" size="sm">Mandatory</Pill>}
                    </div>
                  </Td>
                  <Td className="whitespace-nowrap text-[var(--color-text-secondary)]">{formatDate(item.protocol.lastDoseOn)}</Td>
                  <Td className="whitespace-nowrap">{item.protocol.nextDueOn ? formatDate(item.protocol.nextDueOn) : 'Now'}</Td>
                  <Td><DueStatusPill status={item.protocol.status} /></Td>
                  {canRecord && (
                    <Td className="text-right">
                      <Button
                        variant="secondary"
                        size="sm"
                        icon="pill"
                        aria-label={`Record ${item.protocol.name} for ${item.horseName}`}
                        onClick={() => setRecording(item)}
                      >
                        Record
                      </Button>
                    </Td>
                  )}
                </Tr>
              ))}
            </TBody>
          </Table>
        </Panel>
      )}

      <RecordVaccinationModal
        item={recording}
        protocols={protocols}
        onClose={() => setRecording(null)}
        onRecorded={load}
      />
    </div>
  );
}
