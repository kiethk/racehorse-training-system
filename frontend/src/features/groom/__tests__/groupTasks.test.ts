import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { groupTasks, type TaskBlock } from '../lib/groupTasks.ts';
import type { TodayTaskItem } from '../types/index.ts';

function task(overrides: Partial<TodayTaskItem>): TodayTaskItem {
  return {
    source: 'SOP',
    refId: 1,
    startTime: '05:00:00',
    endTime: null,
    horseId: 1,
    horseName: 'Alpha',
    title: 'Feeding',
    note: null,
    status: 'PENDING',
    actionable: true,
    completionId: null,
    completionNote: null,
    completionPhotoUrls: [],
    ...overrides,
  };
}

describe('groupTasks', () => {
  it('puts routine tasks of the same duty and time into one block', () => {
    const entries = groupTasks([
      task({ refId: 1, horseName: 'Charlie' }),
      task({ refId: 2, horseName: 'Alpha' }),
      task({ refId: 3, horseName: 'Bravo', status: 'COMPLETED' }),
    ]);

    assert.equal(entries.length, 1);
    const block = entries[0] as TaskBlock;
    assert.equal(block.kind, 'block');
    assert.deepEqual(block.tasks.map((t) => t.horseName), ['Alpha', 'Bravo', 'Charlie']);
    assert.deepEqual(block.pending.map((t) => t.refId), [2, 1]);
    assert.deepEqual(block.done.map((t) => t.refId), [3]);
  });

  it('keeps different duties or times apart', () => {
    const entries = groupTasks([
      task({ refId: 1, startTime: '05:00:00', title: 'Feeding' }),
      task({ refId: 2, startTime: '05:30:00', title: 'Mucking out' }),
      task({ refId: 3, startTime: '05:00:00', title: 'Hoof care' }),
    ]);

    assert.equal(entries.length, 3);
    assert.deepEqual(entries.map((e) => e.startTime), ['05:00:00', '05:00:00', '05:30:00']);
  });

  it('leaves workouts and vet visits as single rows, ordered by time with the blocks', () => {
    const entries = groupTasks([
      task({ refId: 1, source: 'WORKOUT', actionable: false, startTime: '06:00:00', title: 'Training session' }),
      task({ refId: 2, startTime: '05:00:00' }),
      task({ refId: 3, source: 'PREVENTIVE_CARE', actionable: false, startTime: '05:00:00', title: 'Veterinary' }),
    ]);

    assert.deepEqual(entries.map((e) => e.kind), ['block', 'single', 'single']);
    assert.deepEqual(entries.map((e) => e.startTime), ['05:00:00', '05:00:00', '06:00:00']);
  });

  it('shows each completion once with the horses it covered, and skips completions without evidence', () => {
    const [entry] = groupTasks([
      task({ refId: 1, horseName: 'Alpha', status: 'COMPLETED', completionId: 7, completionNote: 'All ate', completionPhotoUrls: ['/p/0'] }),
      task({ refId: 2, horseName: 'Bravo', status: 'COMPLETED', completionId: 7, completionNote: 'All ate', completionPhotoUrls: ['/p/0'] }),
      task({ refId: 3, horseName: 'Charlie', status: 'COMPLETED', completionId: 8 }),
    ]);

    const block = entry as TaskBlock;
    assert.equal(block.evidence.length, 1);
    assert.deepEqual(block.evidence[0].horseNames, ['Alpha', 'Bravo']);
    assert.deepEqual(block.evidence[0].photoUrls, ['/p/0']);
  });
});
