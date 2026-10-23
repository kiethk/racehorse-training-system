'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Button,
  ChoiceInput,
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
import { cn } from '@/lib/cn';
import { displayError, formatDate } from '@/lib/display';
import { preventiveCareService } from '../../services/preventiveCareService';
import type { PreventiveDueItem, PreventiveDueStatus, PreventiveProtocol } from '../../types/preventive';
import { BatchVaccinationModal } from './BatchVaccinationModal';
import { DueStatusPill } from './preventiveDisplay';

type Filter = 'ACTION' | 'OVERDUE' | 'DUE_SOON' | 'OK';

const FILTER_STATUSES: Record<Filter, PreventiveDueStatus[]> = {
  ACTION: ['OVERDUE', 'MISSING', 'DUE_SOON'],
  OVERDUE: ['OVERDUE', 'MISSING'],
  DUE_SOON: ['DUE_SOON'],
  OK: ['OK'],
};

interface NormalizedDueProtocol {
  protocolId: number;
  name: string;
  code?: string;
  mandatory?: boolean;
  lastDoseOn?: string | null;
  nextDueOn?: string | null;
  status: PreventiveDueStatus;
}

function getProtocol(item: Partial<PreventiveDueItem> | any): NormalizedDueProtocol {
  const p = item?.protocol || item?.protocolStatus || item;
  return {
    protocolId: p?.protocolId ?? p?.id ?? 0,
    name: p?.name ?? p?.protocolName ?? 'Unknown Protocol',
    code: p?.code ?? '',
    mandatory: Boolean(p?.mandatory),
    lastDoseOn: p?.lastDoseOn ?? null,
    nextDueOn: p?.nextDueOn ?? null,
    status: p?.status ?? 'NONE',
  };
}

const rowKey = (item: PreventiveDueItem) => {
  const p = getProtocol(item);
  return `${item?.horseId ?? 0}:${p.protocolId}`;
};

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
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [batchOpen, setBatchOpen] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [rows, actionable] = await Promise.all([
        preventiveCareService.listDue({ statuses: FILTER_STATUSES[filter], protocolId }),
        preventiveCareService.listDue({ statuses: FILTER_STATUSES.ACTION }),
      ]);
      setItems(Array.isArray(rows) ? rows : []);
      setSummary(Array.isArray(actionable) ? actionable : []);
      setSelected(new Set());
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

  const counts = useMemo(() => {
    const list = Array.isArray(summary) ? summary : [];
    return {
      overdue: list.filter((i) => {
        const status = getProtocol(i).status;
        return status === 'OVERDUE' || status === 'MISSING';
      }).length,
      dueSoon: list.filter((i) => getProtocol(i).status === 'DUE_SOON').length,
      horses: new Set(list.map((i) => i?.horseId).filter(Boolean)).size,
    };
  }, [summary]);

  const safeItems = Array.isArray(items) ? items : [];
  const selectedItems = safeItems.filter((item) => selected.has(rowKey(item)));
  const selectedProtocolIds = new Set(selectedItems.map((item) => getProtocol(item).protocolId));
  const selectedHorses = [...new Map(selectedItems.map((item) => [item.horseId, { id: item.horseId, name: item.horseName }])).values()];
  const allSelected = safeItems.length > 0 && selected.size === safeItems.length;

  const toggle = (key: string) => {
    setSelected((current) => {
      const next = new Set(current);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
  };

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
      ) : safeItems.length === 0 ? (
        <Panel padded>
          <EmptyState icon="check" title="Nothing here"
            description={filter === 'OK' ? 'No horse is up to date for this filter yet.' : 'Every horse is covered for this filter.'} />
        </Panel>
      ) : (
        <Panel>
          <Table bare aria-label="Preventive care work">
            <THead>
              <Tr>
                {canRecord && (
                  <Th className="w-10">
                    <ChoiceInput
                      aria-label="Select all rows"
                      checked={allSelected}
                      onChange={() => setSelected(allSelected ? new Set() : new Set(safeItems.map(rowKey)))}
                    />
                  </Th>
                )}
                <Th>Horse</Th>
                <Th>Protocol</Th>
                <Th>Last dose</Th>
                <Th>Next due</Th>
                <Th>Status</Th>
              </Tr>
            </THead>
            <TBody>
              {safeItems.map((item, index) => {
                const proto = getProtocol(item);
                const key = rowKey(item) || `row-${index}`;
                return (
                  <Tr key={key} className={cn(selected.has(key) && 'bg-[var(--color-primary-subtle)]')}>
                    {canRecord && (
                      <Td>
                        <ChoiceInput
                          aria-label={`Select ${item.horseName ?? 'Horse'} – ${proto.name}`}
                          checked={selected.has(key)}
                          onChange={() => toggle(key)}
                        />
                      </Td>
                    )}
                    <Td>
                      <div className="flex items-center gap-2">
                        <span className="font-semibold">{item.horseName ?? `Horse #${item.horseId}`}</span>
                        {item.horseStatus && <StatusBadge status={item.horseStatus} size="sm" />}
                      </div>
                    </Td>
                    <Td>
                      <div className="flex items-center gap-2">
                        <span>{proto.name}</span>
                        {proto.mandatory && <Pill tone="info" size="sm">Mandatory</Pill>}
                      </div>
                    </Td>
                    <Td className="whitespace-nowrap text-[var(--color-text-secondary)]">{formatDate(proto.lastDoseOn)}</Td>
                    <Td className="whitespace-nowrap">{proto.nextDueOn ? formatDate(proto.nextDueOn) : 'Now'}</Td>
                    <Td><DueStatusPill status={proto.status} /></Td>
                  </Tr>
                );
              })}
            </TBody>
          </Table>
        </Panel>
      )}

      {canRecord && selected.size > 0 && (
        <div className="sticky bottom-4 z-[var(--z-dropdown)] flex items-center justify-between gap-3 rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] px-4 py-3 shadow-[var(--shadow-popover)]">
          <span className="text-sm text-[var(--color-text-secondary)]">
            <strong className="text-[var(--color-text-primary)]">{selectedHorses.length}</strong> horse{selectedHorses.length === 1 ? '' : 's'} selected
            {selectedProtocolIds.size > 1 && ' · several protocols selected, pick one in the form'}
          </span>
          <div className="flex gap-2">
            <Button variant="tertiary" onClick={() => setSelected(new Set())}>Clear</Button>
            <Button variant="primary" icon="pill" onClick={() => setBatchOpen(true)}>Record batch vaccination</Button>
          </div>
        </div>
      )}

      <BatchVaccinationModal
        open={batchOpen}
        onClose={() => setBatchOpen(false)}
        horses={selectedHorses}
        protocols={protocols}
        initialProtocolId={selectedProtocolIds.size === 1 ? [...selectedProtocolIds][0] : undefined}
        onRecorded={load}
      />
    </div>
  );
}
