import type { ReactNode } from 'react';

import { StatusBadge } from '@/components/ui';
import type { HorseSpecialty } from '@/features/horses/types';
import type { Horse } from '@/features/stable/types';
import { HorseProfilePhoto } from './HorseProfilePhoto';
import { HorseTrainingClearance } from './HorseTrainingClearance';

/** Shared identity banner. Actions belong to the actor; clearance stays consistent. */
export function HorseProfileHeader({
  horse,
  horseId,
  actions,
}: {
  horse: Horse | null;
  horseId: number;
  /** Kept for existing callers; detailed specialty evidence now lives in Overview. */
  specialty?: HorseSpecialty | null;
  actions?: ReactNode;
}) {
  const name = horse?.name ?? `Horse #${horseId}`;

  return (
    <header className="grid min-w-0 items-center gap-6 bg-[var(--color-surface)] p-6 lg:grid-cols-[minmax(0,1fr)_15rem]">
      <div className="flex min-w-0 items-center gap-6">
        <HorseProfilePhoto name={name} image={horse?.imageUrl} />
        <div className="min-w-0 space-y-3">
          <p className="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">Horse profile</p>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="min-w-0 break-words text-2xl font-bold tracking-tight text-[var(--color-text-primary)]">{name}</h1>
            {horse && <StatusBadge status={horse.currentStatus} />}
          </div>
          <p className="text-sm text-[var(--color-text-secondary)]">{horse?.breed?.trim() || 'Breed not recorded'}</p>
          {actions && <div className="flex flex-wrap items-center gap-2 pt-1">{actions}</div>}
        </div>
      </div>
      {horse && <HorseTrainingClearance horse={horse} />}
    </header>
  );
}
