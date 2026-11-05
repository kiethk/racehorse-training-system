'use client';

import { DataTable, Notice, Pill } from '@/components/ui';
import type { DataTableColumn } from '@/components/ui/DataTable';
import type { PreventiveProtocol } from '../../types/preventive';

const columns: DataTableColumn<PreventiveProtocol>[] = [
  {
    id: 'name',
    header: 'Protocol',
    sortValue: (row) => row.name,
    render: (row) => (
      <div>
        <span className="font-semibold">{row.name}</span>
        <span className="block font-mono text-xs text-[var(--color-text-muted)]">{row.code}</span>
      </div>
    ),
  },
  {
    id: 'category',
    header: 'Type',
    render: (row) => <Pill tone="neutral" size="sm">{row.category === 'VACCINE' ? 'Vaccine' : 'Deworming'}</Pill>,
  },
  {
    id: 'mandatory',
    header: 'Requirement',
    render: (row) => (row.mandatory ? <Pill tone="neutral" size="sm">Mandatory</Pill> : <Pill tone="neutral" size="sm">Recommended</Pill>),
  },
  { id: 'interval', header: 'Repeat every', align: 'right', sortValue: (row) => row.intervalDays, render: (row) => `${row.intervalDays} days` },
  { id: 'warning', header: 'Warn before', align: 'right', render: (row) => `${row.warningDays} days` },
  {
    id: 'default',
    header: 'Default dose',
    render: (row) => (
      <span className="text-[var(--color-text-secondary)]">
        {[row.defaultDosage, row.defaultRoute].filter(Boolean).join(' · ') || '—'}
      </span>
    ),
  },
];

export function ProtocolsTab({ protocols, loading }: { protocols: PreventiveProtocol[]; loading: boolean }) {
  return (
    <div className="space-y-4">
      <Notice tone="info">
        Protocols are read-only here. The next due date of each horse is its last dose plus the repeat interval.
      </Notice>
      <DataTable
        rows={protocols}
        columns={columns}
        getRowKey={(row) => row.id}
        loading={loading}
        ariaLabel="Preventive protocols"
        emptyTitle="No protocols"
      />
    </div>
  );
}
