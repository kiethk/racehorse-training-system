import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { areaView, assignmentBatch, blockStalls, compareAreas, createDraft, editCount, planAssignment } from '../lib/managerAssignments.ts';
import type { StableSnapshot } from '../types/manager';

function fixture(): StableSnapshot {
  const areas = [
    { id: 1, code: 'A', name: 'Area A', type: 'REGULAR' as const, trainerId: 1 },
    { id: 2, code: 'B', name: 'Area B', type: 'REGULAR' as const, trainerId: 2 },
    { id: 3, code: 'Q-A', name: 'Quarantine A', type: 'QUARANTINE' as const, trainerId: null },
  ];
  return { areas, stalls: areas.flatMap(a => Array.from({ length: 12 }, (_, i) => ({ id: a.id * 100 + i + 1,
    areaId: a.id, groomId: null, stallNumber: i + 1, stallCode: a.code + String(i + 1).padStart(2, '0'), status: 'AVAILABLE' as const }))),
    horses: [], trainers: [1, 2, 3].map(id => ({ id, fullName: `Trainer ${id}`, email: '', role: 'HEAD_TRAINER' })),
    grooms: [10, 11, 12].map(id => ({ id, fullName: `Groom ${id}`, email: '', role: 'GROOM' })) };
}
const groom = (areaId: number, blockNumber = 1) => ({ kind: 'groom' as const, areaId, blockNumber });
const trainer = (areaId: number) => ({ kind: 'trainer' as const, areaId });
function assign(data: StableSnapshot, area: number, number: number, id: number) {
  blockStalls(data, area, number).forEach(s => { s.groomId = id; });
}
function horse(data: StableSnapshot, stallId: number) {
  data.horses.push({ id: stallId, name: 'Horse', currentStallId: stallId, currentStatus: 'ELIGIBLE', ownerId: null, breed: null, dateOfBirth: null });
}

describe('Manager Stable assignment draft', () => {
  it('starts clean and stages a whole block without mutating server data', () => {
    const data = fixture(), initial = createDraft(data);
    assert.equal(editCount(data, initial), 0);
    const { draft } = planAssignment(data, initial, groom(1), 10);
    assert.equal(editCount(data, draft), 1);
    assert.deepEqual([draft.grooms[101], draft.grooms[102], draft.grooms[103]], [10, 10, 10]);
    assert.equal(initial.grooms[101], null); assert.equal(data.stalls[0].groomId, null);
  });
  it('swaps occupied blocks in one batch and sends the original full baseline', () => {
    const data = fixture(); assign(data, 1, 1, 10); assign(data, 2, 1, 11); horse(data, 101); horse(data, 201);
    const { draft, changes } = planAssignment(data, createDraft(data), groom(2), 10);
    assert.equal(draft.grooms[101], 11); assert.equal(draft.grooms[201], 10);
    assert.equal(changes.length, 2);
    assert.deepEqual(assignmentBatch(data, draft).groomAssignments, [
      { areaId: 1, blockNumber: 1, groomId: 11, expectedGroomIds: [10, 10, 10] },
      { areaId: 2, blockNumber: 1, groomId: 10, expectedGroomIds: [11, 11, 11] },
    ]);
  });
  it('blocks removal when any slot contains a horse', () => {
    const data = fixture(); assign(data, 1, 1, 10); horse(data, 102);
    assert.throws(() => planAssignment(data, createDraft(data), groom(1), null), /Swap Groom/);
  });
  it('blocks moving a Groom away from an occupied block to an empty target', () => {
    const data = fixture(); assign(data, 1, 1, 10); horse(data, 103);
    assert.throws(() => planAssignment(data, createDraft(data), groom(2), 10), /Swap Groom/);
  });
  it('allows replacement by an unassigned Groom without an unassigned intermediate state', () => {
    const data = fixture(); assign(data, 1, 1, 10); horse(data, 101);
    const { draft } = planAssignment(data, createDraft(data), groom(1), 12);
    assert.equal(draft.grooms[101], 12); assert.equal(draft.grooms[102], 12);
  });
  it('allows removing an empty assigned block', () => {
    const data = fixture(); assign(data, 1, 1, 10);
    const { draft } = planAssignment(data, createDraft(data), groom(1), null);
    assert.equal(draft.grooms[101], null); assert.equal(editCount(data, draft), 1);
  });
  it('moves an empty block allocation instead of assigning a fourth stall', () => {
    const data = fixture(); assign(data, 1, 1, 10);
    const { draft } = planAssignment(data, createDraft(data), groom(2), 10);
    assert.equal(Object.values(draft.grooms).filter(id => id === 10).length, 3);
    assert.equal(draft.grooms[101], null); assert.equal(draft.grooms[201], 10);
  });
  it('allows Q Groom assignment without a Trainer', () => {
    const data = fixture(); const { draft } = planAssignment(data, createDraft(data), groom(3), 10);
    assert.equal(draft.grooms[301], 10); assert.equal(draft.trainers[3], null);
  });
  it('rejects Trainer assignment to Q', () => {
    const data = fixture(); assert.throws(() => planAssignment(data, createDraft(data), trainer(3), 1), /Quarantine/);
  });
  it('requires a Trainer before assigning a regular Groom', () => {
    const data = fixture(); data.areas[0].trainerId = null;
    assert.throws(() => planAssignment(data, createDraft(data), groom(1), 10), /Assign a Trainer/);
  });
  it('rejects removing a Trainer while Grooms remain', () => {
    const data = fixture(); assign(data, 1, 1, 10);
    assert.throws(() => planAssignment(data, createDraft(data), trainer(1), null), /Remove Groom/);
  });
  it('limits Trainer to two Areas but allows an explicit drag swap', () => {
    const data = fixture(); data.areas.push({ ...data.areas[0], id: 4, code: 'C' });
    const initial = createDraft(data);
    assert.throws(() => planAssignment(data, initial, trainer(2), 1), /at most two/);
    const { draft } = planAssignment(data, initial, trainer(2), 1, { kind: 'trainer', personId: 1, source: trainer(1) });
    assert.equal(draft.trainers[1], 2); assert.equal(draft.trainers[2], 1);
  });
  it('keeps incomplete or mixed legacy block ownership visible', () => {
    const data = fixture(); data.stalls[0].groomId = 10; data.stalls[1].groomId = 11;
    const view = areaView(data, createDraft(data), data.areas[0]);
    assert.equal(view.blocks[0].mixed, true); assert.equal(view.blocks[0].groomId, null);
  });
  it('can repair mixed blocks using all three baseline IDs', () => {
    const data = fixture(); data.stalls[0].groomId = 10; data.stalls[1].groomId = 11;
    const { draft } = planAssignment(data, createDraft(data), groom(1), 12);
    assert.deepEqual(assignmentBatch(data, draft).groomAssignments[0].expectedGroomIds, [10, 11, null]);
  });
  it('does not hide an existing Groom in a mixed source block', () => {
    const data = fixture(); data.stalls[0].groomId = 10; data.stalls[1].groomId = 11;
    assert.throws(() => planAssignment(data, createDraft(data), groom(2), 10), /mixed block/);
  });
  it('counts warnings from actual occupied unassigned slots, not the first Groom', () => {
    const data = fixture(); data.stalls[0].groomId = 10; horse(data, 102); data.stalls.reverse();
    const view = areaView(data, createDraft(data), data.areas[0]);
    assert.equal(view.blocks[0].missingGroom, true); assert.equal(view.attention, 1);
    assert.deepEqual(view.blocks[0].stalls.map(s => s.stallNumber), [1, 2, 3]);
  });
  it('keeps reserved/empty unassigned areas neutral', () => {
    const data = fixture(); data.areas[0].trainerId = null; data.stalls[0].status = 'RESERVED';
    const view = areaView(data, createDraft(data), data.areas[0]);
    assert.equal(view.attention, 0); assert.equal(view.occupied, 0);
  });
  it('sorts generated Area sequences Z before AA and Q-Z before Q-AA', () => {
    const data = fixture();
    const regular = ['AA', 'B', 'Z', 'A'].map(code => ({ ...data.areas[0], code })).sort(compareAreas);
    assert.deepEqual(regular.map(a => a.code), ['A', 'B', 'Z', 'AA']);
    assert.deepEqual(['Q-AA', 'Q-Z'].map(code => ({ ...data.areas[2], code })).sort(compareAreas).map(a => a.code), ['Q-Z', 'Q-AA']);
  });
});
