'use client';

import { useCallback, useState, type ReactNode } from 'react';

import { DataTable, HorseAvatar, Pill, StatusBadge, Tooltip } from '@/components/ui';
import type { DataTableColumn } from '@/components/ui/DataTable';
import type { Horse } from '@/features/stable/types';
import { HorseSpecialtyBadges } from './HorseSpecialtyBadges';
import { formatHorseSex } from '../../lib/sex';

export interface HorseTableProps {
  horses: Horse[];
  loading?: boolean;
  error?: ReactNode;
  renderAction?: (horse: Horse) => ReactNode;
  searchValue?: string;
  onSearchChange?: (value: string) => void;
  resetPageKey?: string | number;
  filters?: ReactNode;
  searchable?: boolean;
  /** When true, shows the Specialty column between Breed and Status. */
  showSpecialty?: boolean;
}

/**
 * The vet's training decision. Only an eligible horse trains, so other statuses show a dash.
 * The reason stays in the tooltip to keep the row short.
 */
function TrainingCell({ horse }: { horse: Horse }) {
  if (horse.currentStatus !== 'ELIGIBLE' || !horse.trainingDecision) {
    return <span className="text-[var(--color-text-muted)]">—</span>;
  }
  if (horse.trainingDecision === 'ALLOWED') {
    return <Pill tone="success" icon="check" size="sm">Allowed</Pill>;
  }
  return (
    <Tooltip content={horse.trainingDecisionReason?.trim() || 'Training is blocked'}>
      <span tabIndex={0} className="inline-flex rounded-full outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]">
        <Pill tone="danger" icon="lock" size="sm">Blocked</Pill>
      </span>
    </Tooltip>
  );
}

/**
 * Shared horse list table.
 * Columns: Horse (avatar + name), Registration number, Breed, Sex, (Specialty), Training, Status, and optional Actions.
 * Built-in search (name / registration / breed) and pagination via DataTable; no API calls here.
 */
export function HorseTable({
  horses,
  loading,
  error,
  renderAction,
  searchValue,
  onSearchChange,
  resetPageKey,
  filters,
  searchable = true,
  showSpecialty = false,
}: HorseTableProps) {
  const [internalSearch, setInternalSearch] = useState('');
  const activeSearch = searchValue !== undefined ? searchValue : internalSearch;
  const handleSearchChange = useCallback(
    (value: string) => {
      if (searchValue === undefined) setInternalSearch(value);
      onSearchChange?.(value);
    },
    [searchValue, onSearchChange],
  );

  const baseColumns: DataTableColumn<Horse>[] = [
    {
      id: 'horse',
      header: 'Horse',
      sortValue: (h) => h.name,
      render: (h) => (
        <div className="flex items-center gap-2.5">
          <HorseAvatar name={h.name} image={h.imageUrl} size={32} rounded="md" />
          <span className="font-medium text-[var(--color-text-primary)]">{h.name}</span>
        </div>
      ),
    },
    {
      id: 'registrationNumber',
      header: 'Registration number',
      sortValue: (h) => h.registrationNumber ?? '',
      render: (h) => (
        <span className="text-[var(--color-text-secondary)]">
          {h.registrationNumber?.trim() || '—'}
        </span>
      ),
    },
    {
      id: 'breed',
      header: 'Breed',
      sortValue: (h) => h.breed ?? '',
      render: (h) => (
        <span className="text-[var(--color-text-secondary)]">
          {h.breed?.trim() || '—'}
        </span>
      ),
    },
    {
      id: 'sex',
      header: 'Sex',
      sortValue: (h) => h.sex ?? '',
      render: (h) => <span className="text-[var(--color-text-secondary)]">{formatHorseSex(h.sex)}</span>,
    },
    ...(showSpecialty
      ? [
          {
            id: 'specialty',
            header: 'Specialty',
            render: (h: Horse) => {
              if (h.specialty === undefined || h.specialty === null) {
                return <span className="text-[var(--color-text-muted)]">—</span>;
              }
              return (
                <HorseSpecialtyBadges specialty={h.specialty} size="sm" stacked />
              );
            },
          },
        ]
      : []),
    {
      id: 'training',
      header: 'Training',
      // Blocked horses sort first so the trainer can find them at once.
      sortValue: (h) => (h.trainingDecision === 'BLOCKED' ? 0 : 1),
      render: (h) => <TrainingCell horse={h} />,
    },
    {
      id: 'status',
      header: 'Status',
      render: (h) => <StatusBadge status={h.currentStatus} />,
    },
  ];

  const columns: DataTableColumn<Horse>[] = renderAction
    ? [
        ...baseColumns,
        {
          id: 'actions',
          header: 'Actions',
          align: 'right' as const,
          render: (h) => renderAction(h),
        },
      ]
    : baseColumns;

  return (
    <DataTable<Horse>
      key={resetPageKey != null ? String(resetPageKey) : undefined}
      rows={horses}
      columns={columns}
      getRowKey={(h) => h.id}
      loading={loading}
      error={error}
      pageSize={10}
      ariaLabel="Horses"
      emptyTitle="No horses found"
      emptyDescription="No horses match the current search."
      getSearchText={
        searchable
          ? (h) => [h.name, h.registrationNumber, h.breed].filter(Boolean).join(' ')
          : undefined
      }
      searchPlaceholder={searchable ? 'Search by name, registration or breed…' : undefined}
      filterValue={searchable ? activeSearch : undefined}
      onFilterValueChange={searchable ? handleSearchChange : undefined}
      toolbarExtras={filters}
    />
  );
}
