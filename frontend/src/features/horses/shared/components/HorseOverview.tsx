import type { ReactNode } from 'react';

import type { HorseSpecialty } from '@/features/horses/types';
import type { Horse } from '@/features/stable/types';
import { formatDate } from '@/lib/display';
import { formatHorseSex } from '../../lib/sex';
import { HorseInfoRow, HorseInfoSection } from './HorseInfoSection';
import { HorseSpecialtySection } from './HorseSpecialtySection';

/** Common first tab. Actors can append context without duplicating the profile. */
export function HorseOverview({ horse, specialty = null, children }: {
  horse: Horse;
  specialty?: HorseSpecialty | null;
  children?: ReactNode;
}) {
  return (
    <div className="@container min-w-0 space-y-5">
      <div className="grid min-w-0 items-start gap-5 @3xl:grid-cols-[minmax(0,1.2fr)_minmax(0,1fr)]">
        <HorseInfoSection title="Profile details" icon="clipboard">
          <div className="grid min-w-0 grid-cols-1 gap-x-8 gap-y-6 sm:grid-cols-2">
            <HorseInfoRow stacked label="Registration number" value={horse.registrationNumber?.trim()} />
            <HorseInfoRow stacked label="Registry" value={horse.registryName?.trim()} />
            <HorseInfoRow stacked label="Date of birth" value={horse.dateOfBirth ? formatDate(horse.dateOfBirth) : null} />
            <HorseInfoRow stacked label="Sex" value={formatHorseSex(horse.sex)} />
          </div>
        </HorseInfoSection>
        <HorseSpecialtySection specialty={specialty} />
      </div>
      {children}
    </div>
  );
}
