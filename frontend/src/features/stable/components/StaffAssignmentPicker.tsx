'use client';

import { useState } from 'react';
import { Avatar, Modal, SearchInput, SelectableCard } from '@/components/ui';
import type { AssignmentDraft, AssignmentTarget, DragStaff, StableSnapshot } from '../types/manager';
import { personName, planAssignment, staffLocations, targetLabel } from '../lib/managerAssignments';

export function StaffAssignmentPicker({ data, draft, target, onClose, onAssign }: {
  data: StableSnapshot; draft: AssignmentDraft; target: AssignmentTarget;
  onClose: () => void; onAssign: (target: AssignmentTarget, staff: DragStaff) => void;
}) {
  const [search, setSearch] = useState('');
  const rows = (target.kind === 'trainer' ? data.trainers : data.grooms).filter(u => u.fullName.toLowerCase().includes(search.trim().toLowerCase()))
    .flatMap(person => {
      let reason = '';
      try { planAssignment(data, draft, target, person.id); } catch (error) { reason = (error as Error).message; }
      const locations = staffLocations(data, draft, target.kind, person.id);
      if (reason && target.kind === 'trainer' && locations.length) {
        return locations.filter(source => source.areaId !== target.areaId).map(source => {
          const drag: DragStaff = { kind: 'trainer', personId: person.id, source };
          let swapReason = '';
          try { planAssignment(data, draft, target, person.id, drag); } catch (error) { swapReason = (error as Error).message; }
          return { person, locations, reason: swapReason, drag, swapLabel: `Swap from ${targetLabel(data, source)}` };
        });
      }
      return [{ person, locations, reason, drag: { kind: target.kind, personId: person.id } as DragStaff, swapLabel: '' }];
    }).sort((a, b) => Number(!!a.reason) - Number(!!b.reason) || a.locations.length - b.locations.length || a.person.fullName.localeCompare(b.person.fullName));
  return (
    <Modal open title={target.kind === 'trainer' ? 'Assign Trainer' : 'Assign Groom'} description={targetLabel(data, target)} onClose={onClose}>
      <div className="space-y-3">
        <SearchInput value={search} onChange={setSearch} label="Find employee" placeholder="Search by name..." />
        <div className="space-y-2">
          {rows.map(({ person, locations, reason, drag, swapLabel }) => <SelectableCard key={`${person.id}:${swapLabel}`} disabled={!!reason}
            title={reason || undefined} onClick={() => onAssign(target, drag)}
            className="disabled:cursor-not-allowed disabled:opacity-60">
            <span className="flex items-center gap-3">
              <Avatar name={person.fullName} />
              <span className="min-w-0 flex-1">
                <span className="block text-sm font-medium">{personName(data, target.kind, person.id)}</span>
                <span className="mt-0.5 block text-xs text-[var(--color-text-secondary)]">
                  {reason || swapLabel || (locations.length ? locations.map(t => targetLabel(data, t)).join(' · ') : 'Unassigned')}
                </span>
              </span>
            </span>
          </SelectableCard>)}
          {!rows.length && <p className="py-4 text-center text-sm text-[var(--color-text-muted)]">No matching staff</p>}
        </div>
      </div>
    </Modal>
  );
}
