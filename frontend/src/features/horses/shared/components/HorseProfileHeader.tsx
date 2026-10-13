import type { ReactNode } from 'react';

import { HorseAvatar, Pill, StatusBadge, Tooltip } from '@/components/ui';
import { HorseSpecialtyBadges } from '@/features/horses/shared/components/HorseSpecialtyBadges';
import type { HorseSpecialty } from '@/features/horses/types';
import type { Horse } from '@/features/stable/types';
import { formatDate } from '@/lib/display';

/**
 * Shared identity header for a horse profile — designed to sit inside the
 * rounded card top of HorseDetailLayout (no Panel/border/shadow of its own).
 * Role-specific actions are passed via `actions`; this component owns no navigation.
 */
export function HorseProfileHeader({
  horse,
  horseId,
  specialty,
  actions,
}: {
  horse: Horse | null;
  horseId: number;
  specialty: HorseSpecialty | null;
  actions?: ReactNode;
}) {
  const name = horse?.name ?? `Horse #${horseId}`;

  return (
    <header className="flex min-w-0 shrink-0 items-start gap-3 border-b border-[var(--color-border)] bg-[var(--color-surface)] p-4 sm:items-start sm:gap-4 sm:px-6 sm:py-5">
      <HorseAvatar name={name} size={56} rounded="md" />

      <div className="min-w-0 flex-1 space-y-2">
        {/* Name + status + training gate */}
        <div className="flex flex-wrap items-center gap-2">
          <h1 className="text-xl font-semibold tracking-tight text-[var(--color-text-primary)] truncate">
            {name}
          </h1>
          {horse && <StatusBadge status={horse.currentStatus} />}
          {horse?.trainingDecision === 'BLOCKED' && (
            <Tooltip content={horse.trainingDecisionReason || 'Training is blocked'}>
              <span
                tabIndex={0}
                className="inline-flex rounded-full outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
              >
                <Pill tone="danger" icon="lock" size="sm">Training blocked</Pill>
              </span>
            </Tooltip>
          )}
        </div>

        {/* Subtitle line */}
        <p className="text-sm text-[var(--color-text-secondary)]">
          {[
            horse?.registrationNumber && `Reg. ${horse.registrationNumber}`,
            horse?.breed,
            horse?.dateOfBirth && `Born ${formatDate(horse.dateOfBirth)}`,
          ]
            .filter(Boolean)
            .join(' · ') || 'No details on record.'}
        </p>

        {/* Specialty badges */}
        <HorseSpecialtyBadges specialty={specialty} size="md" />
      </div>

      {actions && (
        <div className="flex shrink-0 items-center gap-2 self-start">{actions}</div>
      )}
    </header>
  );
}
