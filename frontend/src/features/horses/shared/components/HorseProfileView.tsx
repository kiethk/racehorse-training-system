'use client';

import { useCallback, useEffect, useState, type ReactNode } from 'react';

import {
  Button,
  DetailSkeleton,
  EmptyState,
  Notice,
  Panel,
} from '@/components/ui';
import { horseService } from '@/features/horses/services/horseService';
import { HorseDetailLayout } from '@/features/horses/shared/components/HorseDetailLayout';
import { HorseDetailTabs, type HorseDetailTab } from './HorseDetailTabs';
import { HorseOverview } from './HorseOverview';
import { HorseProfileHeader } from '@/features/horses/shared/components/HorseProfileHeader';
import type { HorseSpecialty } from '@/features/horses/types';
import type { Horse } from '@/features/stable/types';
import { displayError } from '@/lib/display';

export interface HorseProfileContext {
  horse: Horse;
  specialty: HorseSpecialty | null;
}

type ProfileContent = ReactNode | ((profile: HorseProfileContext) => ReactNode);

export interface HorseProfileActionsContext extends HorseProfileContext {
  reload: () => void;
}

export interface HorseProfileViewProps {
  horseId: number;
  /** Href for the back link rendered by HorseDetailLayout. */
  returnTo?: string;
  /** Label for the back link. Defaults to 'Back to horses'. */
  backLabel?: string;
  /** Optional actions rendered in the top header banner. */
  headerActions?: ReactNode | ((context: HorseProfileActionsContext) => ReactNode);
  /** Optional sidebar column content (e.g. action cards). */
  sidebar?: ReactNode;
  /** Extra actor tabs appear after the shared Overview tab; overview is reserved. */
  extraTabs?: readonly HorseDetailTab[] | ((profile: HorseProfileContext) => readonly HorseDetailTab[]);
  /** Additional actor context below the common Overview cards. */
  overviewContent?: ProfileContent;
}

function HorseProfileContent({
  horseId,
  returnTo,
  backLabel,
  headerActions,
  sidebar,
  extraTabs = [],
  overviewContent,
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

  const profile: HorseProfileContext = { horse, specialty };
  const actorTabs = typeof extraTabs === 'function' ? extraTabs(profile) : extraTabs;
  const overview = typeof overviewContent === 'function' ? overviewContent(profile) : overviewContent;
  const tabs: HorseDetailTab[] = [
    { id: 'overview', label: 'Overview', content: <HorseOverview horse={horse} specialty={specialty}>{overview}</HorseOverview> },
    ...actorTabs.filter((tab) => tab.id !== 'overview'),
  ];

  const renderedActions =
    typeof headerActions === 'function'
      ? headerActions({ ...profile, reload })
      : headerActions;

  return (
    <HorseDetailLayout
      returnTo={returnTo}
      backLabel={backLabel}
      header={
        <HorseProfileHeader horse={horse} horseId={horseId} specialty={specialty} actions={renderedActions} />
      }
      sidebar={sidebar}
      content={
        <div className="space-y-5">
          {specialtyWarning && <Notice tone="warning">{specialtyWarning}</Notice>}
          <HorseDetailTabs tabs={tabs} />
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
  extraTabs = [],
  overviewContent,
}: HorseProfileViewProps) {
  return (
    <HorseProfileContent
      key={horseId}
      horseId={horseId}
      returnTo={returnTo}
      backLabel={backLabel}
      headerActions={headerActions}
      sidebar={sidebar}
      extraTabs={extraTabs}
      overviewContent={overviewContent}
    />
  );
}
