'use client';

import { useCallback, useEffect, useState, type ReactNode } from 'react';

import {
  Button,
  DetailSkeleton,
  EmptyState,
  Notice,
  Panel,
  StatusBadge,
} from '@/components/ui';
import { horseService } from '@/features/horses/services/horseService';
import { HorseDetailLayout } from '@/features/horses/shared/components/HorseDetailLayout';
import { HorseInfoRow, HorseInfoSection } from '@/features/horses/shared/components/HorseInfoSection';
import { HorseProfileHeader } from '@/features/horses/shared/components/HorseProfileHeader';
import type { HorseSpecialty } from '@/features/horses/types';
import type { Horse } from '@/features/stable/types';
import { displayError, formatDate } from '@/lib/display';

export interface HorseProfileViewProps {
  horseId: number;
  /** Href for the back link rendered by HorseDetailLayout. */
  returnTo?: string;
  /** Label for the back link. Defaults to 'Back to horses'. */
  backLabel?: string;
  /** Optional actions rendered in the top header banner. */
  headerActions?: ReactNode;
  /** Optional sidebar column content (e.g. action cards). */
  sidebar?: ReactNode;
}

function HorseProfileContent({
  horseId,
  returnTo,
  backLabel,
  headerActions,
  sidebar,
}: HorseProfileViewProps) {
  const [horse, setHorse] = useState<Horse | null>(null);
  const [specialty, setSpecialty] = useState<HorseSpecialty | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [specialtyWarning, setSpecialtyWarning] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  const reload = useCallback(() => {
    setReloadKey((k) => k + 1);
  }, []);

  useEffect(() => {
    let active = true;

    async function loadData() {
      setLoading(true);
      setError(null);
      setSpecialtyWarning(null);
      setHorse(null);
      setSpecialty(null);

      try {
        const horsePromise = horseService.getById(horseId);
        const specialtyPromise = horseService.getSpecialty(horseId).catch((err: unknown) => {
          if (active) {
            setSpecialtyWarning(displayError(err, 'Unable to load racing specialty data.'));
          }
          return null;
        });

        const [horseData, specialtyData] = await Promise.all([horsePromise, specialtyPromise]);
        if (active) {
          setHorse(horseData);
          setSpecialty(specialtyData);
        }
      } catch (err: unknown) {
        if (active) {
          setError(displayError(err, 'Unable to load horse profile.'));
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    void loadData();

    return () => {
      active = false;
    };
  }, [horseId, reloadKey]);

  /* ── Loading ── */
  if (loading) {
    return (
      <HorseDetailLayout
        returnTo={returnTo}
        backLabel={backLabel}
        header={null}
        sidebar={sidebar}
        content={
          <Panel padded>
            <DetailSkeleton />
          </Panel>
        }
      />
    );
  }

  /* ── Error ── */
  if (error || !horse) {
    return (
      <HorseDetailLayout
        returnTo={returnTo}
        backLabel={backLabel}
        header={null}
        sidebar={sidebar}
        content={
          <Panel padded>
            <EmptyState
              icon="alert-triangle"
              title="Unable to load horse profile"
              description={error ?? 'Horse profile could not be loaded.'}
              action={
                <Button size="sm" icon="refresh" onClick={reload}>
                  Retry
                </Button>
              }
            />
          </Panel>
        }
      />
    );
  }

  /* ── Content ── */
  return (
    <HorseDetailLayout
      returnTo={returnTo}
      backLabel={backLabel}
      header={
        <HorseProfileHeader
          horse={horse}
          horseId={horseId}
          specialty={specialty}
          actions={headerActions}
        />
      }
      sidebar={sidebar}
      content={
        <div className="space-y-5">
          {specialtyWarning && (
            <Notice tone="warning">{specialtyWarning}</Notice>
          )}

          <HorseInfoSection title="Basic Information">
            <HorseInfoRow
              label="Registration Number"
              value={horse.registrationNumber?.trim() || undefined}
            />
            <HorseInfoRow
              label="Breed"
              value={horse.breed?.trim() || undefined}
            />
            <HorseInfoRow
              label="Date of Birth"
              value={horse.dateOfBirth ? formatDate(horse.dateOfBirth) : undefined}
            />
            <HorseInfoRow
              label="Current Status"
              value={<StatusBadge status={horse.currentStatus} />}
            />
          </HorseInfoSection>

          {(horse.trainingDecision || horse.trainingDecisionReason) && (
            <HorseInfoSection title="Training Gate">
              <HorseInfoRow
                label="Decision"
                value={horse.trainingDecision || undefined}
              />
              {horse.trainingDecisionReason && (
                <HorseInfoRow
                  label="Block Reason"
                  value={horse.trainingDecisionReason}
                />
              )}
            </HorseInfoSection>
          )}
        </div>
      }
    />
  );
}

export function HorseProfileView({
  horseId,
  returnTo,
  backLabel,
  headerActions,
  sidebar,
}: HorseProfileViewProps) {
  return (
    <HorseProfileContent
      key={horseId}
      horseId={horseId}
      returnTo={returnTo}
      backLabel={backLabel}
      headerActions={headerActions}
      sidebar={sidebar}
    />
  );
}
