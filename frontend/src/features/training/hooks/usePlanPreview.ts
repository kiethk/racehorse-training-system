'use client';

import { useEffect, useState } from 'react';
import { displayError } from '@/lib/display';
import { toIsoDate } from '../lib/dateRange';
import { trainingService } from '../services/trainingService';
import type { PlanPreviewResponse, TrainingDay } from '../types';

/** Wait this long after the last change before asking the backend, so quick clicks send one request. */
const PREVIEW_DELAY_MS = 300;

interface PlanPreviewInput {
  courseId: number | '';
  startDate: string;
  trainingDays: TrainingDay[];
  selectedHorseIds: number[];
}

/**
 * Live preview of the plan being set up: the course period and which horses are free, busy or blocked.
 *
 * - Debounced: a request goes out only after the inputs stop changing for PREVIEW_DELAY_MS.
 * - Cancelled when superseded: a slow answer for an old date can never overwrite the answer for the new one.
 * - The last answer stays on screen while the next one loads; `scheduleStale` tells the caller to dim it, and
 *   only a change of course, date or weekdays counts, because ticking a horse does not change any horse's status.
 */
export function usePlanPreview({ courseId, startDate, trainingDays, selectedHorseIds }: PlanPreviewInput) {
  const [preview, setPreview] = useState<PlanPreviewResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  /** The inputs the preview on screen was computed for. */
  const [loadedFor, setLoadedFor] = useState<{ schedule: string; request: string } | null>(null);

  // A past date is rejected by the backend anyway; not asking avoids an error flash while a date is being typed.
  const scheduleReady = courseId !== '' && startDate !== '' && startDate >= toIsoDate(new Date()) && trainingDays.length > 0;
  const scheduleKey = scheduleReady ? `${courseId}|${startDate}|${[...trainingDays].sort().join(',')}` : null;
  const requestKey = scheduleKey ? `${scheduleKey}|${[...selectedHorseIds].sort((a, b) => a - b).join(',')}` : null;

  // Keyed on the content of the inputs, not on the arrays: ticking a horse and unticking it again, or re-selecting
  // the same horses, builds a new array with the same ids and must not send a request for an answer already shown.
  const alreadyLoaded = loadedFor?.request === requestKey;

  useEffect(() => {
    if (courseId === '' || !scheduleKey || !requestKey || alreadyLoaded) return;
    const controller = new AbortController();
    const timer = setTimeout(() => {
      trainingService
        .previewPlan({ courseId, startDate, trainingDays, selectedHorseIds }, controller.signal)
        .then((data) => {
          setPreview(data);
          setError(null);
          setLoadedFor({ schedule: scheduleKey, request: requestKey });
        })
        .catch((cause) => {
          if (controller.signal.aborted) return;   // superseded by a newer request: not an error
          setError(displayError(cause, 'Unable to check the horses for this schedule.'));
          setLoadedFor({ schedule: scheduleKey, request: requestKey });
        });
    }, PREVIEW_DELAY_MS);

    return () => {
      clearTimeout(timer);
      controller.abort();
    };
    // The arrays are read through the keys, which change exactly when their content does.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [scheduleKey, requestKey, alreadyLoaded]);

  return {
    preview,
    error,
    /** True before the first answer arrives. */
    initialLoading: scheduleReady && preview === null && error === null,
    /** The preview on screen belongs to another course, date or set of weekdays. */
    scheduleStale: preview !== null && loadedFor?.schedule !== scheduleKey,
    /** Any newer request (including a change of the selected horses) has not answered yet. */
    pending: requestKey !== null && loadedFor?.request !== requestKey,
  };
}
