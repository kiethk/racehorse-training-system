'use client';

import { useEffect, useState } from 'react';
import { Spinner } from '@/components/ui';
import { displayError } from '@/lib/display';
import { SESSION_HORSE_OUTCOME } from '../lib/status';
import { trainingService } from '../services/trainingService';
import type { SessionHorseOutcomeItem } from '../types';
import { StatusPill } from './StatusPill';

/** The horses that were in one Session of a plan, each with its outcome. Loaded when the row is opened. */
export function PlanSessionHorses({ planId, sessionId }: { planId: number; sessionId: number }) {
  const [horses, setHorses] = useState<SessionHorseOutcomeItem[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    trainingService
      .getPlanSessionHorses(planId, sessionId)
      .then((result) => active && setHorses(result))
      .catch((cause) => active && setError(displayError(cause, 'Unable to load the horses.')));
    return () => {
      active = false;
    };
  }, [planId, sessionId]);

  if (error) return <p className="text-sm text-[var(--color-danger)]">{error}</p>;
  if (!horses) return <Spinner />;
  if (horses.length === 0) return <p className="text-sm text-[var(--color-text-muted)]">No horses.</p>;
  return (
    <ul className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
      {horses.map((horse) => (
        <li key={horse.workoutId} className="flex items-center justify-between gap-2 text-sm">
          <span>{horse.horseName}</span>
          <StatusPill status={SESSION_HORSE_OUTCOME[horse.outcome]} />
        </li>
      ))}
    </ul>
  );
}
