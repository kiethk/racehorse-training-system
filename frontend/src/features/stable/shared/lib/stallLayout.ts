import type { StableStall } from '../../types';

/** Stalls per groom block (API: block 1 = stalls 1–3, block 2 = 4–6, …). */
export const BLOCK_SIZE = 3;

export interface StallSlot {
  stall: StableStall;
  /** 0 = top row (first half of the stall numbers), 1 = bottom row. Same split as the 2D floor plan. */
  row: 0 | 1;
  column: number;
  blockNumber: number;
}

export interface BlockSlot {
  blockNumber: number;
  row: 0 | 1;
  firstColumn: number;
  lastColumn: number;
  stallIds: number[];
  /** The block's groom when every stall agrees; null when unassigned or mixed. */
  groomId: number | null;
  /** Stalls in the block point at different grooms (data drift; the manager board flags it too). */
  mixed: boolean;
}

export interface AreaLayout {
  slots: StallSlot[];
  blocks: BlockSlot[];
  /** Stalls in the longest row; the barn is sized from it. */
  columns: number;
}

export function blockNumberOf(stallNumber: number) {
  return Math.ceil(stallNumber / BLOCK_SIZE);
}

/** Places an area's stalls on two rows facing a centre aisle, in stall-number order. */
export function layoutArea(stalls: readonly StableStall[]): AreaLayout {
  const ordered = [...stalls].sort((a, b) => a.stallNumber - b.stallNumber);
  const half = Math.ceil(ordered.length / 2);
  const slots: StallSlot[] = ordered.map((stall, index) => ({
    stall,
    row: index < half ? 0 : 1,
    column: index < half ? index : index - half,
    blockNumber: blockNumberOf(stall.stallNumber),
  }));

  const byBlock = new Map<number, StallSlot[]>();
  slots.forEach((slot) => byBlock.set(slot.blockNumber, [...(byBlock.get(slot.blockNumber) ?? []), slot]));
  const blocks = [...byBlock.entries()].map(([blockNumber, members]): BlockSlot => {
    const groomIds = new Set(members.map((slot) => slot.stall.groomId));
    const [only] = groomIds;
    return {
      blockNumber,
      row: members[0].row,
      firstColumn: Math.min(...members.map((slot) => slot.column)),
      lastColumn: Math.max(...members.map((slot) => slot.column)),
      stallIds: members.map((slot) => slot.stall.id),
      groomId: groomIds.size === 1 ? only ?? null : null,
      mixed: groomIds.size > 1,
    };
  });

  return { slots, blocks, columns: half };
}

/** Stable colour slot per person, so neighbouring grooms in one area never share a colour while the palette lasts. */
export function assignColorSlots(personIds: readonly (number | null)[], paletteSize: number): Map<number, number> {
  const unique = [...new Set(personIds.filter((id): id is number => id != null))].sort((a, b) => a - b);
  return new Map(unique.map((id, index) => [id, index % paletteSize]));
}
