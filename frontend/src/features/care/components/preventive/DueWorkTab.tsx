'use client';

import { useCallback, useEffect, useState } from 'react';
import { cn } from '@/lib/cn';
import {
  Button,
  ChoiceInput,
  FilterBar,
  FilterChips,
  FormField,
  MetricCard,
  Notice,
  Panel,
  Pill,
  SearchInput,
  Select,
  StatusBadge,
  Table,
  TBody,
  Td,
  Th,
  THead,
  Tr,
} from '@/components/ui';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import type { DataTableColumn } from '@/components/ui/DataTable';
import { displayError, formatDate } from '@/lib/display';
import { preventiveCareService } from '../../services/preventiveCareService';
import type {
  PreventiveDueHorse,
  PreventiveDueStatus,
  PreventiveDueSummary,
  PreventiveProtocol,
} from '../../types/preventive';
import { CompleteVaccinationModal } from './CompleteVaccinationModal';
import { DueStatusPill } from './preventiveDisplay';

type Filter = 'ACTION' | 'OVERDUE' | 'DUE_SOON' | 'OK';

const FILTER_STATUSES: Record<Filter, PreventiveDueStatus[]> = {
  ACTION: ['OVERDUE', 'MISSING', 'DUE_SOON'],
  OVERDUE: ['OVERDUE', 'MISSING'],
  DUE_SOON: ['DUE_SOON'],
  OK: ['OK'],
};

const SEARCH_DELAY_MS = 300;
const PAGE_SIZE = 20;

interface NormalizedDueProtocol {
  protocolId: number;
  name: string;
  code?: string;
  mandatory?: boolean;
  lastDoseOn?: string | null;
  nextDueOn?: string | null;
  status: PreventiveDueStatus;
}

type PreventiveDueItem = PreventiveDueHorse;

function getProtocol(item: unknown): NormalizedDueProtocol {
  const obj = item as Record<string, unknown> | null;
  const p = (obj?.protocol || obj?.protocolStatus || obj) as Record<string, unknown> | null;
  const g = (k: string) => (p ? p[k] : undefined);
  return {
    protocolId: (g('protocolId') ?? g('id') ?? 0) as number,
    name: (g('name') ?? g('protocolName') ?? 'Unknown Protocol') as string,
    code: (g('code') ?? '') as string,
    mandatory: Boolean(g('mandatory')),
    lastDoseOn: (g('lastDoseOn') ?? null) as string | null,
    nextDueOn: (g('nextDueOn') ?? null) as string | null,
    status: (g('status') ?? 'NONE') as PreventiveDueStatus,
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
  const [rows, setRows] = useState<PreventiveDueHorse[]>([]);
  const [summary, setSummary] = useState<PreventiveDueSummary | null>(null);
  const [filter, setFilter] = useState<Filter>('ACTION');
  const [protocolId, setProtocolId] = useState<number | undefined>();
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [completing, setCompleting] = useState<PreventiveDueHorse | null>(null);
  const [selected, setSelected] = useState<Set<string>>(new Set());

  // Ask the server once typing pauses, not on every keystroke.
  useEffect(() => {
    const timer = setTimeout(() => setSearch(searchInput), SEARCH_DELAY_MS);
    return () => clearTimeout(timer);
  }, [searchInput]);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [result, counts] = await Promise.all([
        preventiveCareService.listDueHorses({ statuses: FILTER_STATUSES[filter], protocolId, search, page, size: PAGE_SIZE }),
        preventiveCareService.dueSummary(),
      ]);
      setRows(Array.isArray(result) ? result : []);
      setSummary(counts);
      setSelected(new Set());
    } catch (cause) {
      setError(displayError(cause, 'Could not load preventive care work.'));
    } finally {
      setLoading(false);
    }
  }, [filter, protocolId, search, page]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const changeFilter = (next: Filter) => {
    setFilter(next);
    setPage(0);
  };

  const toggle = (key: string) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
  };

  const safeItems = Array.isArray(rows) ? rows : [];
  const selectedItems = safeItems.filter((item) => selected.has(rowKey(item)));
  const selectedHorses = [...new Map(selectedItems.map((item) => [item.horseId, { id: item.horseId, name: item.horseName }])).values()];
  const allSelected = safeItems.length > 0 && selected.size === safeItems.length;

  const columns: DataTableColumn<PreventiveDueHorse>[] = [
    {
      id: 'horse',
      header: 'Horse',
      render: (row) => (
        <div className="flex items-center gap-2">
          <span className="font-semibold">{row.horseName}</span>
          <StatusBadge status={row.horseStatus} size="sm" />
        </div>
      ),
    },
    {
      id: 'protocols',
      header: 'Protocols',
      render: (row) => (
        <span className="text-[var(--color-text-secondary)]">{row.protocols.map((p) => p.name).join(' · ')}</span>
      ),
    },
    {
      id: 'nextDue',
      header: 'Next due',
      render: (row) => <span className="whitespace-nowrap">{row.earliestDueOn ? formatDate(row.earliestDueOn) : 'Now'}</span>,
    },
    { id: 'status', header: 'Status', render: (row) => <DueStatusPill status={row.worstStatus} /> },
  ];
  if (canRecord) {
    columns.push({
      id: 'action',
      header: '',
      align: 'right',
      render: (row) => (
        <Button variant="secondary" size="sm" icon="check" onClick={() => setCompleting(row)}>Complete</Button>
      ),
    });
  }

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <MetricCard label="Overdue or no record" value={summary?.overdue ?? '—'} icon="alert-triangle"
          onClick={() => changeFilter('OVERDUE')} active={filter === 'OVERDUE'} />
        <MetricCard label="Due soon" value={summary?.dueSoon ?? '—'} icon="clock"
          onClick={() => changeFilter('DUE_SOON')} active={filter === 'DUE_SOON'} />
        <MetricCard label="Horses needing care" value={summary?.horses ?? '—'} icon="horse"
          onClick={() => changeFilter('ACTION')} active={filter === 'ACTION'} />
      </div>

      <FilterBar onSubmit={(event) => event.preventDefault()}>
        <FormField label="Search horse" className="min-w-[14rem] flex-1">
          <SearchInput
            label="Search horse"
            placeholder="Horse name"
            value={searchInput}
            onChange={(value) => {
              setSearchInput(value);
              setPage(0);
            }}
          />
        </FormField>
        <FormField label="Protocol" className="w-56">
          <Select
            value={protocolId ?? ''}
            onChange={(e) => {
              setProtocolId(e.target.value ? Number(e.target.value) : undefined);
              setPage(0);
            }}
          >
            <option value="">All protocols</option>
            {protocols.map((protocol) => (
              <option key={protocol.id} value={protocol.id}>{protocol.name}</option>
            ))}
          </Select>
        </FormField>
        <FilterChips<Filter>
          label="Status"
          value={filter}
          onChange={changeFilter}
          options={[
            { value: 'ACTION', label: 'Needs action' },
            { value: 'OVERDUE', label: 'Overdue' },
            { value: 'DUE_SOON', label: 'Due soon' },
            { value: 'OK', label: 'Up to date' },
          ]}
        />
        <Button type="button" variant="secondary" icon="refresh" onClick={load} disabled={loading}>Refresh</Button>
      </FilterBar>

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
          </span>
          <Button variant="tertiary" onClick={() => setSelected(new Set())}>Clear</Button>
        </div>
      )}

      <CompleteVaccinationModal
        horse={completing}
        protocols={protocols}
        onClose={() => setCompleting(null)}
        onRecorded={load}
      />
    </div>
  );
}
