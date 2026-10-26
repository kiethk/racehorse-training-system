'use client';

import { useState } from 'react';
import { Button } from '@/components/ui';
import { HorseProfileView } from '@/features/horses/shared/components/HorseProfileView';
import type { Horse } from '@/features/stable/types';
import { ChangeHorseStatusModal } from './ChangeHorseStatusModal';
import { StableTeamTab } from './StableTeamTab';
import { HistoryTab } from './HistoryTab';

interface ManagerHorseDetailViewProps {
  horseId: number;
}

/**
 * Manager-specific horse detail view.
 * Wraps the shared HorseProfileView and adds "Stable & Team" and "History" tabs
 * via the extraTabs callback so the loaded horse is reused without a second fetch.
 * Provides status transition controls (ELIGIBLE <-> INACTIVE) for Club Manager.
 */
export function ManagerHorseDetailView({ horseId }: ManagerHorseDetailViewProps) {
  const [modalHorse, setModalHorse] = useState<Horse | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [onSuccessCallback, setOnSuccessCallback] = useState<(() => void) | null>(null);

  return (
    <>
      <HorseProfileView
        horseId={horseId}
        returnTo="/manager/horses"
        backLabel="Back to horses"
        headerActions={({ horse, reload }) => {
          const isEligible = horse.currentStatus === 'ELIGIBLE';
          const isInactive = horse.currentStatus === 'INACTIVE';

          if (!isEligible && !isInactive) return null;

          return (
            <Button
              type="button"
              variant={isInactive ? 'primary' : 'secondary'}
              size="sm"
              icon={isInactive ? 'check' : 'alert-triangle'}
              onClick={() => {
                setModalHorse(horse);
                setOnSuccessCallback(() => reload);
                setModalOpen(true);
              }}
            >
              {isInactive ? 'Restore to Active' : 'Change Status'}
            </Button>
          );
        }}
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

      <ChangeHorseStatusModal
        horse={modalHorse}
        open={modalOpen}
        onClose={() => {
          setModalOpen(false);
          setModalHorse(null);
        }}
        onSuccess={() => {
          if (onSuccessCallback) {
            onSuccessCallback();
          }
        }}
      />
    </>
  );
}

