'use client';

import { HorseProfileView } from '@/features/horses/shared/components/HorseProfileView';
import { toIsoDate } from '../../lib/dateRange';
import { HorseAttendanceList } from './HorseAttendanceList';
import { HorseFitnessTrendView } from './HorseFitnessTrendView';
import { HorseScheduleView } from '../schedule/HorseScheduleView';
import { TrainerHorseIncidentsTab } from '../incidents/TrainerHorseIncidentsTab';

interface TrainerHorseDetailViewProps {
  horseId: number;
}

/**
 * Head Trainer's horse detail view.
 * Wraps the shared HorseProfileView and adds "Training schedule", "Course runs", "Fitness & Trends" and "Incident Reports" tabs.
 */
export function TrainerHorseDetailView({ horseId }: TrainerHorseDetailViewProps) {
  const today = toIsoDate(new Date());

  return (
    <HorseProfileView
      horseId={horseId}
      returnTo="/trainer/horses"
      backLabel="Back to horses"
      extraTabs={({ horse, refreshHorse }) => [
        {
          id: 'training-schedule',
          label: 'Training schedule',
          content: (
            <div className="rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4">
              <HorseScheduleView target={{ horseId: horse.id, name: horse.name, horse }} startDate={today} hideHeader />
            </div>
          ),
        },
        {
          id: 'course-runs',
          label: 'Course runs',
          content: <HorseAttendanceList horseId={horse.id} />,
        },
        {
          id: 'fitness-trends',
          label: 'Fitness & Trends',
          content: <HorseFitnessTrendView horseId={horse.id} hideHeader onReported={refreshHorse} />,
        },
        {
          id: 'incidents',
          label: 'Incident Reports',
          content: <TrainerHorseIncidentsTab horseId={horse.id} horseName={horse.name} onReported={refreshHorse} />,
        },
      ]}
    />
  );
}
