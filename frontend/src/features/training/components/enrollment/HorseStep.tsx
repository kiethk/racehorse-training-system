'use client';

import { useState } from 'react';
import {
  Button,
  ChoiceInput,
  EmptyState,
  FilterChips,
  Icon,
  ListSkeleton,
  Notice,
  Panel,
  Pill,
  SectionTitle,
  Spinner,
  Tooltip,
  type SegmentOption,
} from '@/components/ui';
import { cn } from '@/lib/cn';
import { formatDate } from '@/lib/display';
import type { HorsePlanAvailability, PlanAvailability } from '../../types';

type HorseFilter = 'ALL' | 'AVAILABLE' | 'BUSY' | 'UNAVAILABLE';

const SHORT_DATE: Intl.DateTimeFormatOptions = { day: '2-digit', month: 'short' };

/** Busy horses stay selectable so the trainer can build the group first and then move it to a common free date. */
export function isSelectable(horse: HorsePlanAvailability): boolean {
  return horse.availability === 'AVAILABLE' || horse.availability === 'BUSY';
}

/** Free horses first, busy ones next, horses that cannot train last; the backend name order stays inside a group. */
const AVAILABILITY_ORDER: Record<PlanAvailability, number> = { AVAILABLE: 0, BUSY: 1, BLOCKED: 2, NO_GROOM: 2 };

function matchesFilter(availability: PlanAvailability, filter: HorseFilter): boolean {
  if (filter === 'ALL') return true;
  if (filter === 'UNAVAILABLE') return availability === 'BLOCKED' || availability === 'NO_GROOM';
  return availability === filter;
}

/**
 * Step 3: pick the horses of the group. Every horse of the trainer's area is listed with what the chosen schedule
 * means for it, so a clash is visible before the plan is created. Horses that cannot train are shown, not hidden,
 * so the trainer knows why they are missing; the filter hides them on request.
 */
export function HorseStep({
  horses,
  loading,
  stale,
  error,
  scheduleReady,
  selectedIds,
  onToggle,
  onSelect,
}: {
  horses: HorsePlanAvailability[];
  loading: boolean;
  /** The list belongs to the previous schedule and is being refreshed. */
  stale: boolean;
  error: string | null;
  scheduleReady: boolean;
  selectedIds: number[];
  onToggle: (horseId: number) => void;
  onSelect: (horseIds: number[]) => void;
}) {
  const [filter, setFilter] = useState<HorseFilter>('ALL');

  const count = (wanted: HorseFilter) => horses.filter((horse) => matchesFilter(horse.availability, wanted)).length;
  const options: SegmentOption<HorseFilter>[] = [
    { value: 'ALL', label: 'All', count: horses.length },
    { value: 'AVAILABLE', label: 'Free', count: count('AVAILABLE') },
    { value: 'BUSY', label: 'Busy', count: count('BUSY') },
    { value: 'UNAVAILABLE', label: 'Cannot train', count: count('UNAVAILABLE') },
  ];
  const shown = horses
    .filter((horse) => matchesFilter(horse.availability, filter))
    .sort((a, b) => AVAILABILITY_ORDER[a.availability] - AVAILABILITY_ORDER[b.availability]);
  const freeIds = horses.filter((horse) => horse.availability === 'AVAILABLE').map((horse) => horse.horseId);
  const allFreeSelected = freeIds.length > 0 && freeIds.every((id) => selectedIds.includes(id));

  return (
    <Panel padded className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <SectionTitle>3. Select horses for the group ({selectedIds.length} selected)</SectionTitle>
        <div className="flex items-center gap-2">
          {stale && <Spinner size="sm" label="Checking the new schedule…" />}
          {freeIds.length > 0 && (
            <Button
              size="sm"
              type="button"
              onClick={() => onSelect(allFreeSelected ? selectedIds.filter((id) => !freeIds.includes(id)) : [...new Set([...selectedIds, ...freeIds])])}
            >
              {allFreeSelected ? 'Clear free horses' : 'Select all free horses'}
            </Button>
          )}
        </div>
      </div>

      {error && <Notice tone="error">{error}</Notice>}

      {!scheduleReady && horses.length === 0 ? (
        <p className="text-xs text-[var(--color-text-muted)]">
          Choose a course, a start date and the training days to see which horses are free.
        </p>
      ) : loading ? (
        <ListSkeleton rows={3} />
      ) : horses.length === 0 ? (
        <EmptyState
          icon="horse"
          title="No horses in your area"
          description="Only admitted horses in a stall of an area you manage can be enrolled."
        />
      ) : (
        <>
          <FilterChips label="Horse availability" options={options} value={filter} onChange={setFilter} />
          <ul
            className={cn(
              'grid grid-cols-1 gap-2 sm:grid-cols-2 md:grid-cols-3 transition-opacity duration-[var(--duration-fast)]',
              stale && 'opacity-60',
            )}
          >
            {shown.map((horse) => (
              <li key={horse.horseId} className="h-full">
                <HorseCard horse={horse} selected={selectedIds.includes(horse.horseId)} onToggle={onToggle} />
              </li>
            ))}
          </ul>
          {shown.length === 0 && <p className="text-xs text-[var(--color-text-muted)]">No horse matches this filter.</p>}
        </>
      )}
    </Panel>
  );
}

function HorseCard({
  horse,
  selected,
  onToggle,
}: {
  horse: HorsePlanAvailability;
  selected: boolean;
  onToggle: (horseId: number) => void;
}) {
  const selectable = isSelectable(horse);
  const busy = horse.availability === 'BUSY';

  return (
    <label
      className={cn(
        'flex h-full items-start gap-2.5 rounded-[var(--radius-md)] border p-2.5 text-xs transition-colors duration-[var(--duration-fast)]',
        selectable ? 'cursor-pointer hover:bg-[var(--color-surface-muted)]' : 'cursor-not-allowed bg-[var(--color-surface-muted)] opacity-75',
        selected && !busy && 'border-[var(--color-primary)] bg-[var(--color-primary-soft)]',
        selected && busy && 'border-[var(--color-warning)] bg-[var(--color-warning-soft)]',
        !selected && 'border-[var(--color-border)]',
      )}
    >
      <ChoiceInput
        type="checkbox"
        className="mt-0.5"
        checked={selected}
        disabled={!selectable}
        onChange={() => onToggle(horse.horseId)}
      />
      <div className="min-w-0 flex-1 space-y-1">
        <div className="flex min-w-0 items-center justify-between gap-1.5">
          <span className="truncate font-semibold text-[var(--color-text-primary)]" title={horse.horseName}>
            {horse.horseName}
          </span>
          <AvailabilityHint horse={horse} />
        </div>
        <p className="truncate text-[var(--color-text-muted)]">
          {horse.breed || 'Breed unknown'}
          {horse.stallCode && ` · Stall ${horse.stallCode}`}
        </p>
      </div>
    </label>
  );
}

/**
 * The status pill. Why a horse is busy or cannot train is kept out of the card and shown on hover or keyboard
 * focus; the info icon and the help cursor tell the trainer there is more to see.
 */
function AvailabilityHint({ horse }: { horse: HorsePlanAvailability }) {
  const details = availabilityDetails(horse);
  if (!details) return <AvailabilityPill availability={horse.availability} />;

  return (
    <Tooltip content={<div className="max-w-64 space-y-0.5">{details}</div>}>
      <span
        tabIndex={0}
        aria-label={`${horse.horseName}: details`}
        className="inline-flex shrink-0 cursor-help items-center gap-1 rounded-[var(--radius-sm)] focus-visible:outline-2 focus-visible:outline-[var(--color-focus)]"
      >
        <AvailabilityPill availability={horse.availability} />
        <Icon name="info" size={13} className="text-[var(--color-text-muted)]" />
      </span>
    </Tooltip>
  );
}

function availabilityDetails(horse: HorsePlanAvailability) {
  if (horse.availability === 'BUSY') {
    return (
      <>
        {horse.conflicts.map((plan) => (
          <p key={plan.planId}>
            {plan.courseName}: {formatDate(plan.startDate, SHORT_DATE)} – {formatDate(plan.endDate, SHORT_DATE)}
          </p>
        ))}
        {horse.earliestStart && <p>Free for this course from {formatDate(horse.earliestStart)}</p>}
      </>
    );
  }
  return horse.reason ? <p>{horse.reason}</p> : null;
}

function AvailabilityPill({ availability }: { availability: PlanAvailability }) {
  switch (availability) {
    case 'AVAILABLE':
      return <Pill tone="success" size="sm" icon="check">Free</Pill>;
    case 'BUSY':
      return <Pill tone="warning" size="sm" icon="calendar">Busy</Pill>;
    case 'BLOCKED':
      return <Pill tone="danger" size="sm" icon="lock">Training blocked</Pill>;
    case 'NO_GROOM':
      return <Pill tone="neutral" size="sm" icon="user">No groom</Pill>;
  }
}
