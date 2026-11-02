'use client';

import { useEffect, useState } from 'react';
import { displayError } from '@/lib/display';
import { toIsoDate } from '../lib/dateRange';
import { trainingService } from '../services/trainingService';
import type { GroupPlanPreview, TrainingDay } from '../types';

/** Wait this long after the last change before asking the backend, so quick clicks send one request. */
const PREVIEW_DELAY_MS = 300;

interface Input {
  groupId: number | '';
  courseId: number | '';
  startDate: string;
  trainingDays: TrainingDay[];
}

/**
 * Live preview of the course being assigned: the real period, the time of every Session, the days with no free slot and the
 * plans that overlap. Debounced, and a slow answer for an old input can never overwrite the answer for the new one.
 */
export function useGroupPlanPreview({ groupId, courseId, startDate, trainingDays }: Input) {
  const [preview, setPreview] = useState<GroupPlanPreview | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loadedFor, setLoadedFor] = useState<string | null>(null);

  // A past date is rejected by the backend anyway; not asking avoids an error flash while a date is being typed.
  const ready = groupId !== '' && courseId !== '' && startDate >= toIsoDate(new Date()) && trainingDays.length > 0;
  const key = ready ? `${groupId}|${courseId}|${startDate}|${[...trainingDays].sort().join(',')}` : null;

  useEffect(() => {
    if (!key || key === loadedFor || groupId === '' || courseId === '') return;
    const controller = new AbortController();
    const timer = setTimeout(() => {
      trainingService
        .previewGroupPlan({ groupId, courseId, startDate, trainingDays }, controller.signal)
        .then((data) => {
          setPreview(data);
          setError(null);
          setLoadedFor(key);
        })
        .catch((cause) => {
          if (controller.signal.aborted) return;
          setPreview(null);
          setError(displayError(cause, 'Unable to preview this schedule.'));
          setLoadedFor(key);
        });
    }, PREVIEW_DELAY_MS);
    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [key, loadedFor, groupId, courseId, startDate, trainingDays]);

  return { preview, error, pending: key !== null && key !== loadedFor };
}
