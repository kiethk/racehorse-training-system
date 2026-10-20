'use client';

import { useEffect, useMemo, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Button, LinkButton } from '@/components/ui';
import { useHorses } from '@/features/horses/hooks/useHorses';
import { HorseFilterBar } from '@/features/horses/shared/components/HorseFilterBar';
import { HorseListLayout } from '@/features/horses/shared/components/HorseListLayout';
import { HorseTable } from '@/features/horses/shared/components/HorseTable';
import type { HorseStatus } from '@/features/stable/types';

/**
 * Head Trainer's horse list view.
 * Mirrors ManagerHorsesView: presents the table of assigned horses with search and filters,
 * where selecting a horse navigates to /trainer/horses/[id].
 */
export function TrainerHorsesView() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const requestedId = searchParams.get('horseId');

  // Backwards compatibility: redirect /trainer/horses?horseId=X to /trainer/horses/X
  useEffect(() => {
    if (requestedId) {
      router.replace(`/trainer/horses/${encodeURIComponent(requestedId)}`);
    }
  }, [requestedId, router]);

  const [draftSearch, setDraftSearch] = useState('');
  const [draftStatus, setDraftStatus] = useState<HorseStatus | ''>('');

  const [appliedSearch, setAppliedSearch] = useState('');
  const [appliedStatus, setAppliedStatus] = useState<HorseStatus | ''>('');
  const [resetPageKey, setResetPageKey] = useState(0);

  const { horses, loading, error, reload } = useHorses({
    mine: true,
    status: appliedStatus || undefined,
    includeSpecialty: true,
  });

  const handleApply = () => {
    setAppliedSearch(draftSearch.trim());
    setAppliedStatus(draftStatus);
    setResetPageKey((k) => k + 1);
  };

  const handleClear = () => {
    setDraftSearch('');
    setDraftStatus('');
    setAppliedSearch('');
    setAppliedStatus('');
    setResetPageKey((k) => k + 1);
  };

  const filteredHorses = useMemo(() => {
    const q = appliedSearch.trim().toLowerCase();
    if (!q) return horses;
    return horses.filter((h) => {
      const matchName = h.name.toLowerCase().includes(q);
      const matchReg = h.registrationNumber?.toLowerCase().includes(q) ?? false;
      const matchBreed = h.breed?.toLowerCase().includes(q) ?? false;
      return matchName || matchReg || matchBreed;
    });
  }, [horses, appliedSearch]);

  return (
    <HorseListLayout
      title="Horses"
      description="Horses assigned to stalls in your training areas."
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
    >
      <HorseFilterBar
        search={draftSearch}
        onSearchChange={setDraftSearch}
        status={draftStatus}
        onStatusChange={setDraftStatus}
        onApply={handleApply}
        onClear={handleClear}
      />

      <HorseTable
        horses={filteredHorses}
        loading={loading}
        error={error}
        searchable={false}
        resetPageKey={resetPageKey}
        showSpecialty
        renderAction={(horse) => (
          <LinkButton
            href={`/trainer/horses/${horse.id}`}
            size="sm"
            variant="secondary"
          >
            View
          </LinkButton>
        )}
      />
    </HorseListLayout>
  );
}
