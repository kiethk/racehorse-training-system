'use client';

import { HorseProfileView } from '@/features/horses/shared/components/HorseProfileView';
import { HorseFitnessTrendView } from './HorseFitnessTrendView';
import { TrainerHorseIncidentsTab } from './TrainerHorseIncidentsTab';

interface TrainerHorseDetailViewProps {
  horseId: number;
}

/**
 * Head Trainer's horse detail view.
 * Wraps the shared HorseProfileView and adds "Fitness & Trends" and "Incident Reports" tabs.
 * Mirrors the structure of ManagerHorseDetailView without an arbitrary horse selector.
 */
export function TrainerHorseDetailView({ horseId }: TrainerHorseDetailViewProps) {
  return (
    <HorseProfileView
      horseId={horseId}
      returnTo="/trainer/horses"
      backLabel="Back to horses"
      extraTabs={({ horse }) => [
        {
          id: 'fitness-trends',
          label: 'Fitness & Trends',
          content: <HorseFitnessTrendView horseId={horse.id} hideHeader />,
        },
        {
          id: 'incidents',
          label: 'Incident Reports',
          content: <TrainerHorseIncidentsTab horseId={horse.id} horseName={horse.name} />,
        },
      ]}
    />
  );
}
