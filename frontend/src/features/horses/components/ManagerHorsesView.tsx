'use client';

import { useMemo, useState } from 'react';

import { Button, LinkButton } from '@/components/ui';
import type { HorseStatus } from '@/features/stable/types';
import { useHorses } from '../hooks/useHorses';
import { HorseFilterBar } from '../shared/components/HorseFilterBar';
import { HorseListLayout } from '../shared/components/HorseListLayout';
import { HorseTable } from '../shared/components/HorseTable';

export function ManagerHorsesView() {
  const [draftSearch, setDraftSearch] = useState('');
  const [draftStatus, setDraftStatus] = useState<HorseStatus | ''>('');

  const [appliedSearch, setAppliedSearch] = useState('');
  const [appliedStatus, setAppliedStatus] = useState<HorseStatus | ''>('');
  const [resetPageKey, setResetPageKey] = useState(0);

  const { horses, loading, error, reload } = useHorses({
    status: appliedStatus || undefined,
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
    </HorseListLayout>
  );
}
