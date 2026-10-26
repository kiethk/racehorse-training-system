'use client';

import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  DataTable,
  FilterBar,
  FormField,
  MetricCard,
  Notice,
  SearchInput,
  Select,
  StatusBadge,
} from '@/components/ui';
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

type Filter = 'ALL' | 'ACTION' | 'OVERDUE' | 'DUE_SOON' | 'OK';

// "All" lists every real status; NONE (protocol not applicable) is never a row. An empty list would make the
// server fall back to its default "needs action" set, so All is sent explicitly.
const FILTER_STATUSES: Record<Filter, PreventiveDueStatus[]> = {
  ALL: ['OVERDUE', 'MISSING', 'DUE_SOON', 'OK'],
  ACTION: ['OVERDUE', 'MISSING', 'DUE_SOON'],
  OVERDUE: ['OVERDUE', 'MISSING'],
  DUE_SOON: ['DUE_SOON'],
  OK: ['OK'],
};
const FILTER_OPTIONS: { value: Filter; label: string }[] = [
  { value: 'ALL', label: 'All' },
  { value: 'ACTION', label: 'Needs action' },
  { value: 'OVERDUE', label: 'Overdue' },
  { value: 'DUE_SOON', label: 'Due soon' },
  { value: 'OK', label: 'Up to date' },
];
/** Most urgent first, matching the order the server returns rows in. */
const URGENCY: Record<PreventiveDueStatus, number> = { OVERDUE: 0, MISSING: 1, DUE_SOON: 2, OK: 3, NONE: 4 };
const PAGE_SIZE = 20;
const SEARCH_DELAY_MS = 300;

interface DueWorkTabProps {
  protocols: PreventiveProtocol[];
  canRecord: boolean;
}

export function DueWorkTab({ protocols, canRecord }: DueWorkTabProps) {
  const [rows, setRows] = useState<PreventiveDueHorse[]>([]);
  const [total, setTotal] = useState(0);
  const [summary, setSummary] = useState<PreventiveDueSummary | null>(null);
  const [filter, setFilter] = useState<Filter>('ALL');
  const [protocolId, setProtocolId] = useState<number | undefined>();
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [completing, setCompleting] = useState<PreventiveDueHorse | null>(null);

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
      setRows(result.content);
      setTotal(result.totalElements);
      setSummary(counts);
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

  const columns: DataTableColumn<PreventiveDueHorse>[] = [
    {
      id: 'horse',
      header: 'Horse',
      sortValue: (row) => row.horseName?.toLowerCase(),
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
      // "Now" (no date yet) sorts before any dated dose.
      sortValue: (row) => row.earliestDueOn ?? '',
      render: (row) => <span className="whitespace-nowrap">{row.earliestDueOn ? formatDate(row.earliestDueOn) : 'Now'}</span>,
    },
    { id: 'status', header: 'Status', sortValue: (row) => URGENCY[row.worstStatus], render: (row) => <DueStatusPill status={row.worstStatus} /> },
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
        <FormField label="Status" className="w-48">
          <Select value={filter} onChange={(e) => changeFilter(e.target.value as Filter)}>
            {FILTER_OPTIONS.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
          </Select>
        </FormField>
        <Button type="button" variant="secondary" icon="refresh" onClick={load} disabled={loading}>Refresh</Button>
      </FilterBar>

      {error && <Notice tone="error" title="Could not load" onDismiss={() => setError(null)}>{error}</Notice>}

      <DataTable
        rows={rows}
        columns={columns}
        getRowKey={(row) => row.horseId}
        loading={loading}
        ariaLabel="Horses needing preventive care"
        emptyTitle={search ? 'No horse matches this search' : 'Nothing here'}
        emptyDescription={filter === 'ALL'
          ? 'No horse has a preventive care record for this filter yet.'
          : filter === 'OK' ? 'No horse is up to date for this filter yet.' : 'Every horse is covered for this filter.'}
        pagination={{ page, pageSize: PAGE_SIZE, total, onPageChange: setPage, disabled: loading }}
      />

      <CompleteVaccinationModal
        horse={completing}
        protocols={protocols}
        onClose={() => setCompleting(null)}
        onRecorded={load}
      />
    </div>
  );
}
