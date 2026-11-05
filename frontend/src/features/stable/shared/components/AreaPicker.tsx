'use client';

import { useMemo, useState, type KeyboardEvent } from 'react';
import { Button, IconButton, Popover, PopoverContent, PopoverTrigger, SearchInput } from '@/components/ui';
import { cn } from '@/lib/cn';
import type { Area } from '../../types';
import { filterAreas } from '../lib/areaSearch';

const GROUP_LABEL = { REGULAR: 'Regular', QUARANTINE: 'Quarantine' } as const;

interface AreaPickerProps {
  /** Already ordered with `orderStableAreas`; ‹ › step through this order. */
  areas: Area[];
  value: Area;
  onChange: (area: Area) => void;
  /** Occupied and total stalls per area id, shown next to each name. */
  occupancy: ReadonlyMap<number, { occupied: number; total: number }>;
}

/** Chooses the area shown in the 3D barn. Searchable, so it keeps working however many areas the club adds. */
export function AreaPicker({ areas, value, onChange, occupancy }: AreaPickerProps) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const groups = useMemo(() => filterAreas(areas, query), [areas, query]);
  const index = areas.findIndex((area) => area.id === value.id);

  const choose = (area: Area) => {
    onChange(area);
    setOpen(false);
    setQuery('');
  };
  const chooseFirst = (event: KeyboardEvent) => {
    const first = groups[0]?.areas[0];
    if (event.key === 'Enter' && first) {
      event.preventDefault();
      choose(first);
    }
  };
  const stats = (area: Area) => {
    const count = occupancy.get(area.id);
    return count ? `${count.occupied}/${count.total} occupied` : '';
  };

  return (
    <div className="flex items-center gap-1">
      <IconButton icon="chevron-left" label="Previous area" size="sm" variant="tertiary"
        disabled={index <= 0} onClick={() => onChange(areas[index - 1])} />
      <Popover open={open} onOpenChange={(next) => { setOpen(next); if (!next) setQuery(''); }}>
        <PopoverTrigger asChild>
          <Button variant="secondary" size="sm" iconRight="chevron-down" aria-label={`Area: ${value.name}. Change area`}>
            <span className="font-metric font-semibold">{value.code}</span>
            <span className="max-w-40 truncate text-[var(--color-text-secondary)]">{value.name}</span>
          </Button>
        </PopoverTrigger>
        <PopoverContent align="end" className="w-80 p-2">
          <SearchInput value={query} onChange={setQuery} label="Search areas" placeholder="Search by code or name"
            autoFocus onKeyDown={chooseFirst} />
          <div className="scroll-slim mt-2 max-h-80 space-y-2 overflow-y-auto">
            {groups.length === 0 && (
              <p className="px-2 py-4 text-center text-xs text-[var(--color-text-muted)]">No area matches “{query.trim()}”.</p>
            )}
            {groups.map((group) => (
              <div key={group.type} role="group" aria-label={GROUP_LABEL[group.type]}>
                <p className="px-2 pb-1 text-xs font-semibold text-[var(--color-text-muted)]">{GROUP_LABEL[group.type]}</p>
                {group.areas.map((area) => {
                  const current = area.id === value.id;
                  return (
                    <Button
                      key={area.id}
                      variant="tertiary"
                      size="sm"
                      aria-pressed={current}
                      className={cn('w-full justify-start gap-2', current && 'bg-[var(--color-primary-soft)] text-[var(--color-primary)]')}
                      onClick={() => choose(area)}
                    >
                      <span className="font-metric w-10 shrink-0 text-left font-semibold">{area.code}</span>
                      <span className="min-w-0 flex-1 truncate text-left">{area.name}</span>
                      <span className="shrink-0 text-xs text-[var(--color-text-muted)]">{stats(area)}</span>
                    </Button>
                  );
                })}
              </div>
            ))}
          </div>
        </PopoverContent>
      </Popover>
      <IconButton icon="chevron-right" label="Next area" size="sm" variant="tertiary"
        disabled={index < 0 || index >= areas.length - 1} onClick={() => onChange(areas[index + 1])} />
    </div>
  );
}
