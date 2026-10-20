'use client';

import { HorseProfileView } from '@/features/horses/shared/components/HorseProfileView';
import { StableTeamTab } from './StableTeamTab';
import { HistoryTab } from './HistoryTab';

interface ManagerHorseDetailViewProps {
  horseId: number;
}

/**
 * Manager-specific horse detail view.
 * Wraps the shared HorseProfileView and adds "Stable & Team" and "History" tabs
 * via the extraTabs callback so the loaded horse is reused without a second fetch.
 */
export function ManagerHorseDetailView({ horseId }: ManagerHorseDetailViewProps) {
  return (
    <HorseProfileView
      horseId={horseId}
      returnTo="/manager/horses"
      backLabel="Back to horses"
      extraTabs={({ horse }) => [
        {
          id: 'stable-team',
          label: 'Stable & Team',
          content: (
            <StableTeamTab
              ownerId={horse.ownerId ?? null}
              currentStallId={horse.currentStallId ?? null}
            />
          ),
        },
        {
          id: 'history',
          label: 'History',
          content: <HistoryTab horseId={horse.id} />,
        },
      ]}
    />
  );
}
