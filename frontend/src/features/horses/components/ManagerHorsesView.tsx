'use client';

import { useState } from 'react';

import { Button, LinkButton, PageHeader, ScreenLayout } from '@/components/ui';
import type { HorseStatus } from '@/features/stable/types';
import { useHorses } from '../hooks/useHorses';
import { HorseFilterBar } from '../shared/components/HorseFilterBar';
import { HorseTable } from '../shared/components/HorseTable';

export function ManagerHorsesView() {
  const [status, setStatus] = useState<HorseStatus | ''>('');
  const { horses, loading, error, reload } = useHorses({
    status: status || undefined,
  });

  return (
    <ScreenLayout variant="list">
      <PageHeader
        title="Horses"
        actions={
          <Button
            type="button"
            variant="secondary"
            size="sm"
            icon="refresh"
            onClick={reload}
            loading={loading}
          >
            Refresh
          </Button>
        }
      />

      <HorseTable
        horses={horses}
        loading={loading}
        error={error}
        resetPageKey={status}
        filters={<HorseFilterBar status={status} onStatusChange={setStatus} />}
        renderAction={(horse) => (
          <LinkButton
            href={`/manager/horses/${horse.id}`}
            size="sm"
            variant="secondary"
          >
            View
          </LinkButton>
        )}
      />
    </ScreenLayout>
  );
}
