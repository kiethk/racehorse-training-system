'use client';

import { useRouter, useSearchParams } from 'next/navigation';
import {
  Button,
  EmptyState,
  ListSkeleton,
  Notice,
  Panel,
  ScreenLayout,
  Select,
} from '@/components/ui';
import { HorseProfileView } from '@/features/horses/shared/components/HorseProfileView';
import { useTrainerHorses } from '../hooks/useTrainerHorses';
import { HorseFitnessTrendView } from './HorseFitnessTrendView';
import { TrainerHorseIncidentsTab } from './TrainerHorseIncidentsTab';

/**
 * The Horses page for Trainers:
 * Refactored to use the shared HorseProfileView component as requested for unified UI/UX,
 * with tabs for Overview, Fitness & Trends, and Incident Reports.
 */
export function TrainerHorsesView() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const requestedId = Number(searchParams.get('horseId')) || null;
  const { horses, loading, error, reload } = useTrainerHorses();

  const selectedHorse = horses.find((h) => h.id === requestedId) ?? horses[0] ?? null;
  const selectedId = selectedHorse?.id ?? null;

  if (loading) {
    return (
      <ScreenLayout variant="detail">
        <Panel padded>
          <ListSkeleton rows={4} />
        </Panel>
      </ScreenLayout>
    );
  }

  if (error) {
    return (
      <ScreenLayout variant="detail">
        <Panel padded>
          <EmptyState
            icon="alert-triangle"
            title="Unable to load your horses"
            description={error}
            action={
              <Button size="sm" icon="refresh" onClick={reload}>
                Try again
              </Button>
            }
          />
        </Panel>
      </ScreenLayout>
    );
  }

  if (!selectedId || !selectedHorse) {
    return (
      <ScreenLayout variant="detail">
        <Panel padded>
          <EmptyState
            icon="horse"
            title="No horses assigned yet"
            description="Horses will appear here once they are assigned to a stall in your area."
          />
        </Panel>
      </ScreenLayout>
    );
  }

  const horseSwitcher = (
    <div className="flex items-center gap-2">
      <span className="text-xs font-medium text-[var(--color-text-secondary)]">Horse:</span>
      <Select
        value={selectedId}
        onChange={(e) => router.replace(`/trainer/horses?horseId=${e.target.value}`)}
        className="text-xs"
      >
        {horses.map((horse) => (
          <option key={horse.id} value={horse.id}>
            {horse.name}{horse.breed ? ` · ${horse.breed}` : ''}
          </option>
        ))}
      </Select>
    </div>
  );

  return (
    <div className="space-y-4">
      {requestedId !== null && requestedId !== selectedId && (
        <Notice tone="warning">
          The requested horse is not in your areas, so the first horse is shown instead.
        </Notice>
      )}

      <HorseProfileView
        key={selectedId}
        horseId={selectedId}
        headerActions={horseSwitcher}
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
    </div>
  );
}
