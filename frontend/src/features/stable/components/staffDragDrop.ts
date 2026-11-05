'use client';

import { createDragDrop } from '@/components/ui';
import type { DragStaff } from '../types/manager';

export const staffDragDrop = createDragDrop<DragStaff>();
