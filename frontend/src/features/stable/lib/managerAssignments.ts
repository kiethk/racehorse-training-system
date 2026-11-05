import type { Area, StableStall } from '../types/index';
import type { AssignmentBatch, AssignmentChange, AssignmentDraft, AssignmentTarget, DragStaff, StableSnapshot } from '../types/manager';

export function createDraft(data: Pick<StableSnapshot, 'areas' | 'stalls'>): AssignmentDraft {
  return {
    trainers: Object.fromEntries(data.areas.map(a => [a.id, a.trainerId])),
    grooms: Object.fromEntries(data.stalls.map(s => [s.id, s.groomId])),
  };
}

export function blockStalls(data: Pick<StableSnapshot, 'stalls'>, areaId: number, blockNumber: number) {
  return data.stalls.filter(s => s.areaId === areaId && s.stallNumber >= (blockNumber - 1) * 3 + 1 && s.stallNumber <= blockNumber * 3)
    .sort((a, b) => a.stallNumber - b.stallNumber);
}

export function blockTarget(stall: StableStall): AssignmentTarget {
  return { kind: 'groom', areaId: stall.areaId, blockNumber: Math.floor((stall.stallNumber - 1) / 3) + 1 };
}

export function targetKey(target: AssignmentTarget) {
  return target.kind === 'trainer' ? `trainer:${target.areaId}` : `groom:${target.areaId}:${target.blockNumber}`;
}

export function horseMap(data: Pick<StableSnapshot, 'horses'>) {
  return new Map(data.horses.filter(h => h.currentStallId != null).map(h => [h.currentStallId!, h]));
}

export function trainerChangeReason(data: StableSnapshot, areaId: number): string | undefined {
  const area = data.areas.find(a => a.id === areaId);
  if (!area || area.type !== 'REGULAR') return 'Quarantine areas do not have a Trainer.';
  const stallIds = new Set(data.stalls.filter(s => s.areaId === areaId).map(s => s.id));
  if (data.horses.some(h => h.currentStallId != null && stallIds.has(h.currentStallId))) {
    return 'Area has horses. Trainer reassignment is unavailable.';
  }
  if (data.stalls.some(s => s.areaId === areaId && ['OCCUPIED', 'RESERVED'].includes(s.status))) {
    return 'Area has occupied or reserved stalls.';
  }
  const check = data.trainerChecks.find(c => c.areaId === areaId);
  if (!check || check.trainerId !== area.trainerId) return 'Trainer assignment status changed. Refresh the board.';
  if (!check.canChange) return check.reasons.join(' ') || 'Trainer reassignment is unavailable.';
}

export function areaView(data: StableSnapshot, draft: AssignmentDraft, area: Area, horses = horseMap(data)) {
  const stalls = data.stalls.filter(s => s.areaId === area.id);
  const occupied = stalls.filter(s => horses.has(s.id)).length;
  const blocks = [1, 2, 3, 4].map(number => {
    const rows = blockStalls(data, area.id, number);
    const ids = rows.map(s => draft.grooms[s.id] ?? null);
    const mixed = new Set(ids).size > 1;
    return {
      number, stalls: rows, mixed,
      groomId: !mixed && rows.length === 3 ? ids[0] ?? null : null,
      occupied: rows.filter(s => horses.has(s.id)).length,
      missingGroom: rows.some(s => horses.has(s.id) && draft.grooms[s.id] == null),
      complete: rows.length === 3,
    };
  });
  const missingTrainer = area.type === 'REGULAR' && occupied > 0 && draft.trainers[area.id] == null;
  return { area, stalls, blocks, occupied, missingTrainer,
    hasGrooms: stalls.some(s => draft.grooms[s.id] != null),
    attention: Number(missingTrainer) + blocks.filter(b => b.missingGroom).length };
}

export function compareAreas(a: Area, b: Area) {
  const value = (code: string) => {
    const letters = code.replace(/^Q-/, '');
    return /^[A-Z]+$/.test(letters) ? [...letters].reduce((n, ch) => n * 26 + ch.charCodeAt(0) - 64, 0) : Number.MAX_SAFE_INTEGER;
  };
  return value(a.code) - value(b.code) || a.code.localeCompare(b.code, undefined, { numeric: true });
}

export function assignmentBatch(data: StableSnapshot, draft: AssignmentDraft): AssignmentBatch {
  return {
    trainerAssignments: data.areas.filter(a => a.trainerId !== draft.trainers[a.id]).map(a => ({
      areaId: a.id, trainerId: draft.trainers[a.id], expectedTrainerId: a.trainerId,
    })),
    groomAssignments: data.areas.flatMap(a => [1, 2, 3, 4].flatMap(blockNumber => {
      const rows = blockStalls(data, a.id, blockNumber);
      if (!rows.some(s => s.groomId !== draft.grooms[s.id])) return [];
      const ids = rows.map(s => draft.grooms[s.id]);
      if (rows.length !== 3 || new Set(ids).size !== 1) throw new Error('Assign all three stalls in a block together.');
      return [{ areaId: a.id, blockNumber, groomId: ids[0], expectedGroomIds: rows.map(s => s.groomId) }];
    })),
  };
}

export function editCount(data: StableSnapshot, draft: AssignmentDraft) {
  const batch = assignmentBatch(data, draft);
  return batch.trainerAssignments.length + batch.groomAssignments.length;
}

export function personName(data: StableSnapshot, kind: 'trainer' | 'groom', id: number | null) {
  if (id == null) return 'Unassigned';
  return (kind === 'trainer' ? data.trainers : data.grooms).find(u => u.id === id)?.fullName ?? `${kind === 'trainer' ? 'Trainer' : 'Groom'} #${id}`;
}

export function targetLabel(data: StableSnapshot, target: AssignmentTarget) {
  const area = data.areas.find(a => a.id === target.areaId);
  if (target.kind === 'trainer') return `Area ${area?.code ?? target.areaId} · Trainer`;
  const rows = blockStalls(data, target.areaId, target.blockNumber);
  return rows.length ? `${rows[0].stallCode}–${rows.at(-1)!.stallCode}` : `Block ${target.blockNumber}`;
}

export function staffLocations(data: StableSnapshot, draft: AssignmentDraft, kind: 'trainer' | 'groom', id: number): AssignmentTarget[] {
  if (kind === 'trainer') return data.areas.filter(a => draft.trainers[a.id] === id).map(a => ({ kind, areaId: a.id }));
  const targets = new Map<string, AssignmentTarget>();
  data.stalls.filter(s => draft.grooms[s.id] === id).forEach(s => { const t = blockTarget(s); targets.set(targetKey(t), t); });
  return [...targets.values()];
}

/** Pure local draft operation. Server remains authoritative for occupancy, capacity and workout conflicts. */
export function planAssignment(data: StableSnapshot, current: AssignmentDraft, target: AssignmentTarget,
  personId: number | null, drag?: DragStaff): { draft: AssignmentDraft; changes: AssignmentChange[] } {
  const next = { trainers: { ...current.trainers }, grooms: { ...current.grooms } };
  const touched = new Map<string, AssignmentTarget>();
  const touch = (t: AssignmentTarget) => touched.set(targetKey(t), t);
  const area = data.areas.find(a => a.id === target.areaId);
  if (!area) throw new Error('Area no longer exists. Refresh the board.');
  const horses = horseMap(data);
  const setBlock = (t: AssignmentTarget, id: number | null) => {
    if (t.kind !== 'groom') return;
    const rows = blockStalls(data, t.areaId, t.blockNumber);
    if (rows.length !== 3) throw new Error('This block does not contain three stalls. Refresh the board.');
    if (id == null && rows.some(s => horses.has(s.id))) throw new Error('This block has horses. Swap Groom instead of removing.');
    rows.forEach(s => { next.grooms[s.id] = id; }); touch(t);
  };
  if (personId != null && !(target.kind === 'trainer' ? data.trainers : data.grooms).some(u => u.id === personId)) {
    throw new Error('This employee is unavailable. Refresh the staff list.');
  }
  if (target.kind === 'trainer') {
    if (area.type !== 'REGULAR') throw new Error('Quarantine areas do not have a Trainer.');
    const previous = current.trainers[target.areaId];
    next.trainers[target.areaId] = personId; touch(target);
    if (drag?.source?.kind === 'trainer' && drag.source.areaId !== target.areaId) {
      if (current.trainers[drag.source.areaId] !== personId) throw new Error('Assignment changed. Select the employee again.');
      next.trainers[drag.source.areaId] = previous; touch(drag.source);
    }
  } else {
    const rows = blockStalls(data, target.areaId, target.blockNumber);
    const oldIds = [...new Set(rows.map(s => current.grooms[s.id]))];
    if (personId != null) {
      const sources = staffLocations(data, current, 'groom', personId).filter(t => targetKey(t) !== targetKey(target));
      if (sources.length > 1) throw new Error('This Groom has assignments across multiple blocks. Resolve those assignments first.');
      if (sources.length) {
        const source = sources[0];
        if (source.kind !== 'groom') throw new Error('Invalid Groom assignment.');
        const sourceIds = blockStalls(data, source.areaId, source.blockNumber).map(s => current.grooms[s.id]);
        if (oldIds.length > 1 || sourceIds.length !== 3 || sourceIds.some(id => id !== personId)) {
          throw new Error('Resolve the mixed block before swapping this Groom.');
        }
        setBlock(source, oldIds[0] ?? null);
      }
    }
    setBlock(target, personId);
  }
  for (const t of touched.values()) {
    if (t.kind === 'trainer' && current.trainers[t.areaId] !== next.trainers[t.areaId]) {
      const reason = trainerChangeReason(data, t.areaId);
      if (reason) throw new Error(reason);
    }
    const a = data.areas.find(a => a.id === t.areaId)!;
    const groomed = data.stalls.some(s => s.areaId === a.id && next.grooms[s.id] != null);
    if (a.type === 'REGULAR' && groomed && next.trainers[a.id] == null) {
      throw new Error('Assign a Trainer first. Remove Groom assignments before removing the Trainer.');
    }
  }
  if (personId != null && target.kind === 'trainer' && data.areas.filter(a => next.trainers[a.id] === personId).length > 2) {
    throw new Error('A Trainer can manage at most two regular areas.');
  }
  if (personId != null && target.kind === 'groom') {
    const assigned = data.stalls.filter(s => next.grooms[s.id] === personId);
    if (assigned.length > 3 || staffLocations(data, next, 'groom', personId).length > 1) {
      throw new Error('A Groom can manage only one block of three stalls.');
    }
  }
  const changes: AssignmentChange[] = [];
  for (const t of touched.values()) {
    const ids = (d: AssignmentDraft) => t.kind === 'trainer' ? [d.trainers[t.areaId]]
      : blockStalls(data, t.areaId, t.blockNumber).map(s => d.grooms[s.id]);
    const before = ids(current), after = ids(next);
    if (before.every((id, i) => id === after[i])) continue;
    const names = (values: (number | null)[]) => [...new Set(values)].map(id => personName(data, t.kind, id)).join(', ');
    const groomCount = t.kind === 'trainer' ? new Set(data.stalls.filter(s => s.areaId === t.areaId)
      .map(s => next.grooms[s.id]).filter(id => id != null)).size : 0;
    changes.push({ label: targetLabel(data, t), before: names(before), after: names(after),
      ...(groomCount ? { note: `${groomCount} ${groomCount === 1 ? 'Groom will' : 'Grooms will'} report to ${names(after)}.` } : {}),
    });
  }
  return { draft: next, changes };
}

export function overlayDraft(data: StableSnapshot, draft: AssignmentDraft): StableSnapshot {
  return { ...data, areas: data.areas.map(a => ({ ...a, trainerId: draft.trainers[a.id] })),
    stalls: data.stalls.map(s => ({ ...s, groomId: draft.grooms[s.id] })) };
}
