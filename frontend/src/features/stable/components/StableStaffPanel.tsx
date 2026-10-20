'use client';

import { useState } from 'react';
import { Avatar, Button, Icon, Panel, Pill, SearchInput, SelectableCard } from '@/components/ui';
import { cn } from '@/lib/cn';
import type { AreaType, UserSummary } from '../types';
import type { AssignmentDraft, DragStaff, StableSnapshot } from '../types/manager';
import { staffLocations, targetLabel } from '../lib/managerAssignments';
import { staffDragDrop } from './staffDragDrop';

interface Props {
  data: StableSnapshot;
  draft: AssignmentDraft;
  activeTab: AreaType;
  disabled: boolean;
  selected: DragStaff | null;
  canReturn: boolean;
  onSelect: (staff: DragStaff | null) => void;
  onReturn: (staff: DragStaff) => void;
}

export function StableStaffPanel({ data, draft, activeTab, disabled, selected, canReturn, onSelect, onReturn }: Props) {
  const [search, setSearch] = useState('');
  const [staffKind, setStaffKind] = useState<'trainer' | 'groom'>('trainer');
  const kind = activeTab === 'QUARANTINE' ? 'groom' : staffKind;
  const people = (kind === 'trainer' ? data.trainers : data.grooms).map(person => ({
    person, locations: staffLocations(data, draft, kind, person.id),
  })).filter(({ person, locations }) => person.fullName.toLowerCase().includes(search.trim().toLowerCase())
    && (locations.length === 0 || locations.some(t => data.areas.find(a => a.id === t.areaId)?.type === activeTab)));
  const available = people.filter(p => p.locations.length === 0);
  const assigned = people.filter(p => p.locations.length > 0);
  const { activeItem: dragging } = staffDragDrop.useDragDrop();
  const { dropProps } = staffDragDrop.useDropZone({
    disabled,
    accept: staff => !!staff.source && canReturn,
    onDrop: onReturn,
  });

  return (
    <aside className="min-w-0 lg:sticky lg:top-[var(--sticky-top)] lg:self-start" aria-label="Staff assignments"
      {...dropProps}>
      <Panel className="overflow-hidden">
        <div className="space-y-3 border-b border-[var(--color-border)] p-4">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold">Staff</h2>
            {selected && <Button size="sm" variant="tertiary" onClick={() => onSelect(null)}>Cancel</Button>}
          </div>
          {activeTab === 'REGULAR' && <div className="grid grid-cols-2 gap-1 rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] p-1" role="group" aria-label="Staff role">
            {(['trainer', 'groom'] as const).map(role => <Button key={role} size="sm" variant={kind === role ? 'secondary' : 'tertiary'}
              aria-pressed={kind === role} onClick={() => setStaffKind(role)}>{role === 'trainer' ? 'Trainers' : 'Grooms'}</Button>)}
          </div>}
          <SearchInput value={search} onChange={setSearch} label="Search staff" placeholder="Search staff..." />
        </div>
        {dragging?.source && <div className={cn('m-3 rounded-[var(--radius-md)] border border-dashed p-3 text-center text-xs',
          canReturn ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)] text-[var(--color-primary)]'
            : 'border-[var(--color-border)] text-[var(--color-text-muted)]')}
          {...dropProps}>
          {canReturn ? 'Return to staff' : 'Swap only'}
        </div>}
        <div className="scroll-slim max-h-[calc(100vh-20rem)] space-y-4 overflow-y-auto p-3">
          {([{ title: 'Unassigned', rows: available }, { title: 'Assigned', rows: assigned }]).map(group => (
            <section key={group.title} aria-label={`${group.title} ${kind}s`}>
              <div className="mb-2 flex items-center justify-between px-1 text-xs text-[var(--color-text-secondary)]">
                <h3 className="font-medium">{group.title}</h3><span>{group.rows.length}</span>
              </div>
              <div className="space-y-2">
                {group.rows.map(({ person, locations }) => (
                  <StaffCard key={person.id} person={person} kind={kind} locations={locations} data={data}
                    disabled={disabled} selected={selected?.kind === kind && selected.personId === person.id} onSelect={onSelect} />
                ))}
                {group.rows.length === 0 && <p className="px-1 py-2 text-xs text-[var(--color-text-muted)]">{search ? 'No matches' : 'None'}</p>}
              </div>
            </section>
          ))}
          {people.length === 0 && !search && <div className="flex items-center gap-2 text-xs text-[var(--color-text-muted)]"><Icon name="users" size={16} />No staff available</div>}
        </div>
      </Panel>
    </aside>
  );
}

function StaffCard({ person, kind, locations, data, disabled, selected, onSelect }: {
  person: UserSummary;
  kind: DragStaff['kind'];
  locations: ReturnType<typeof staffLocations>;
  data: StableSnapshot;
  disabled: boolean;
  selected: boolean;
  onSelect: Props['onSelect'];
}) {
  const { dragProps } = staffDragDrop.useDraggable({ item: { kind, personId: person.id }, label: person.fullName, disabled });
  return (
    <SelectableCard {...dragProps} disabled={disabled} selected={selected}
      aria-label={`Select ${kind} ${person.fullName}`} onClick={() => onSelect({ kind, personId: person.id })}
      className={cn('bg-[var(--color-surface)]', !disabled && 'cursor-grab active:cursor-grabbing')}>
      <span className="flex min-w-0 items-center gap-2.5">
        <Avatar name={person.fullName} />
        <span className="min-w-0 flex-1">
          <span className="block truncate text-sm font-medium">{person.fullName}</span>
          <span className="mt-0.5 block truncate text-xs text-[var(--color-text-secondary)]">
            {locations.length ? locations.map(t => kind === 'trainer' ? data.areas.find(a => a.id === t.areaId)?.code : targetLabel(data, t)).join(' · ') : 'Unassigned'}
          </span>
        </span>
        {kind === 'trainer' && locations.length > 0 && <Pill tone="neutral" size="sm">{locations.length}/2</Pill>}
      </span>
    </SelectableCard>
  );
}
