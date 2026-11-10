'use client';

import { createDragDrop } from '@/components/ui';
import type { Exercise } from '../types';

export const exerciseDragDrop = createDragDrop<Exercise>();
