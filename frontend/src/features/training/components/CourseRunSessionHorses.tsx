'use client';

import Link from 'next/link';
import { useEffect, useMemo, useState } from 'react';
import { HorseAvatar, Spinner } from '@/components/ui';
import { cn } from '@/lib/cn';
import { displayError } from '@/lib/display';
import { SESSION_HORSE_OUTCOME } from '../lib/status';
import { trainingService } from '../services/trainingService';
import type { SessionHorseOutcomeItem } from '../types';
import { StatusPill } from './StatusPill';

/** The horses that were in one Session of a course run, each with its outcome. Loaded when the row is opened. */
export function CourseRunSessionHorses({ courseRunId, sessionId }: { courseRunId: number; sessionId: number }) {
  const [horses, setHorses] = useState<SessionHorseOutcomeItem[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    trainingService
      .getCourseRunSessionHorses(courseRunId, sessionId)
      .then((result) => active && setHorses(result))
      .catch((cause) => active && setError(displayError(cause, 'Unable to load the horses.')));
    return () => {
      active = false;
    };
  }, [courseRunId, sessionId]);

  const sortedHorses = useMemo(() => {
    if (!horses) return [];
    const active = horses.filter((h) => h.outcome !== 'CANCELLED');
    const cancelled = horses.filter((h) => h.outcome === 'CANCELLED');
    return [...active, ...cancelled];
  }, [horses]);

  if (error) return <p className="text-sm text-[var(--color-danger)]">{error}</p>;
  if (!horses) return <Spinner />;
  if (horses.length === 0) return <p className="text-sm text-[var(--color-text-muted)]">No horses recorded for this session.</p>;
  return (
    <div className="space-y-2 py-1">
      <div className="flex items-center justify-between">
        <h4 className="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">
          Horses in this session ({horses.length})
        </h4>
      </div>
      <ul className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
        {sortedHorses.map((horse) => {
          const isCancelled = horse.outcome === 'CANCELLED';
          return (
            <li
              key={horse.workoutId}
              className={cn(
                'flex items-center justify-between gap-3 rounded-[var(--radius-md)] border border-[var(--color-border)] p-2.5 shadow-sm',
                isCancelled ? 'bg-[var(--color-surface-subtle)] opacity-60' : 'bg-[var(--color-surface)]',
              )}
            >
              <div className="flex items-center gap-2 min-w-0">
                <HorseAvatar name={horse.horseName} size={24} rounded="md" />
                <Link
                  href={`/trainer/horses/${horse.horseId}?tab=course-runs`}
                  className={cn(
                    'truncate text-sm font-medium hover:underline',
                    isCancelled ? 'text-[var(--color-text-muted)] line-through' : 'text-[var(--color-primary)]',
                  )}
                >
                  {horse.horseName}
                </Link>
              </div>
              <StatusPill status={SESSION_HORSE_OUTCOME[horse.outcome]} size="sm" />
            </li>
          );
        })}
      </ul>
    </div>
  );
}
