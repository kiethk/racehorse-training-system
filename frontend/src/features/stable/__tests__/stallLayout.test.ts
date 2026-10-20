import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { assignColorSlots, blockNumberOf, layoutArea } from '../shared/lib/stallLayout.ts';
import type { StableStall } from '../types';

function stalls(count: number, groomOf: (n: number) => number | null = () => null): StableStall[] {
  return Array.from({ length: count }, (_, i) => ({
    id: 100 + i + 1, areaId: 1, groomId: groomOf(i + 1), stallNumber: i + 1,
    stallCode: `A${String(i + 1).padStart(2, '0')}`, status: 'AVAILABLE' as const,
  }));
}

describe('Stable 3D layout', () => {
  it('numbers blocks of three stalls from one', () => {
    assert.deepEqual([1, 3, 4, 6, 7, 12].map(blockNumberOf), [1, 1, 2, 2, 3, 4]);
  });

  it('splits twelve stalls into two rows of six with two blocks per row', () => {
    const layout = layoutArea(stalls(12).reverse());
    assert.equal(layout.columns, 6);
    assert.deepEqual(layout.slots.map((s) => `${s.stall.stallNumber}:${s.row}${s.column}`).slice(0, 7),
      ['1:00', '2:01', '3:02', '4:03', '5:04', '6:05', '7:10']);
    assert.deepEqual(layout.blocks.map((b) => [b.blockNumber, b.row, b.firstColumn, b.lastColumn]),
      [[1, 0, 0, 2], [2, 0, 3, 5], [3, 1, 0, 2], [4, 1, 3, 5]]);
  });

  it('reports the block groom only when every stall agrees', () => {
    const layout = layoutArea(stalls(12, (n) => (n <= 3 ? 10 : n === 4 ? 11 : n <= 6 ? 12 : null)));
    assert.deepEqual(layout.blocks.map((b) => [b.groomId, b.mixed]), [[10, false], [null, true], [null, false], [null, false]]);
  });

  it('handles odd and empty areas', () => {
    const odd = layoutArea(stalls(5));
    assert.equal(odd.columns, 3);
    assert.deepEqual(odd.slots.map((s) => s.row), [0, 0, 0, 1, 1]);
    const empty = layoutArea([]);
    assert.deepEqual([empty.slots.length, empty.blocks.length, empty.columns], [0, 0, 0]);
  });

  it('gives each person a distinct colour slot while the palette lasts', () => {
    const slots = assignColorSlots([12, null, 10, 12, 11], 2);
    assert.deepEqual([...slots.entries()], [[10, 0], [11, 1], [12, 0]]);
  });
});
