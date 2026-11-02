import type { ReactNode } from 'react';

import type { HorseSpecialty } from '@/features/horses/types';
import type { Horse } from '@/features/stable/types';
import { formatDate } from '@/lib/display';
import { cn } from '@/lib/cn';
import { formatHorseSex } from '../../lib/sex';
import { HorseInfoRow, HorseInfoSection } from './HorseInfoSection';
import { HorseSpecialtySection } from './HorseSpecialtySection';

/** Common first tab. Actors can append context without duplicating the profile. */
export function HorseOverview({
  horse,
  specialty = null,
  showSpecialty = true,
  children,
}: {
  horse: Horse;
  specialty?: HorseSpecialty | null;
  showSpecialty?: boolean;
  children?: ReactNode;
}) {
  return (
    <div className="@container min-w-0 space-y-5">
      <div className={cn('grid min-w-0 items-start gap-5', showSpecialty ? '@3xl:grid-cols-[minmax(0,1.2fr)_minmax(0,1fr)]' : 'grid-cols-1')}>
        <HorseInfoSection title="Profile details" icon="clipboard">
          <div className="space-y-3.5">
            <HorseInfoRow label="Registration number" value={horse.registrationNumber?.trim()} />
            <HorseInfoRow label="Registry" value={horse.registryName?.trim()} />
            <HorseInfoRow label="Date of birth" value={horse.dateOfBirth ? formatDate(horse.dateOfBirth) : null} />
            <HorseInfoRow label="Sex" value={formatHorseSex(horse.sex)} />
          </div>
        </HorseInfoSection>
        {showSpecialty && <HorseSpecialtySection specialty={specialty} />}
      </div>
      {children}
    </div>
  );
}
