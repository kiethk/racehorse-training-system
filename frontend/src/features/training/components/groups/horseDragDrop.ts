'use client';

import { createDragDrop } from '@/components/ui';

export interface DraggableHorseItem {
  id: number;
  name: string;
  breed?: string | null;
  stableLocation?: string | null;
  /** Group ID if the horse currently belongs to a group; null/undefined if unassigned in library. */
  sourceGroupId?: number | null;
}

export const horseDragDrop = createDragDrop<DraggableHorseItem>();
