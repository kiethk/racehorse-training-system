'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { HorseAvatar, Spinner } from '@/components/ui';
import { displayError } from '@/lib/display';
import { WORKOUT_STATUS } from '../../lib/status';
import { trainingService } from '../../services/trainingService';
import type { SessionHorse } from '../../types';
import { StatusPill } from '../shared/StatusPill';

/** The horses of one Session of a course run, each with its status. Loaded when the row is opened. */
export function CourseRunSessionHorses({ sessionId }: { sessionId: number }) {
  const [horses, setHorses] = useState<SessionHorse[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    trainingService
      .getSession(sessionId)
      .then(({ horses: found }) => active && setHorses(found))
      .catch((cause) => active && setError(displayError(cause, 'Unable to load the horses.')));
    return () => {
      active = false;
    };
  }, [sessionId]);

  if (error) return <p className="text-sm text-[var(--color-danger)]">{error}</p>;
  if (!horses) return <Spinner />;
  if (horses.length === 0) return <p className="text-sm text-[var(--color-text-muted)]">No horses in this session.</p>;
  return (
    <div className="space-y-2 py-1">
      <div className="flex items-center justify-between">
        <h4 className="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">
          Horses in this session ({horses.length})
        </h4>
      </div>
      <ul className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
        {horses.map((horse) => (
          <li
            key={horse.workoutId}
            className="flex items-center justify-between gap-3 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-2.5 shadow-sm"
          >
            <div className="flex items-center gap-2 min-w-0">
              <HorseAvatar name={horse.horseName} size={24} rounded="md" />
              <Link
                href={`/trainer/horses/${horse.horseId}?tab=course-runs`}
                className="truncate text-sm font-medium text-[var(--color-primary)] hover:underline"
              >
                {horse.horseName}
              </Link>
            </div>
            <StatusPill status={WORKOUT_STATUS[horse.status]} size="sm" />
          </li>
        ))}
      </ul>
    </div>
  );
}
