'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';

import { Button, EmptyState, Icon, Panel, Skeleton } from '@/components/ui';
import type { Area, StableStall } from '@/features/stable/types';
import { HorseInfoRow, HorseInfoSection } from '../shared/components/HorseInfoSection';
import { managerHorseService, type UserSummaryResponse } from '../services/managerHorseService';
import { displayError } from '@/lib/display';

interface StableTeamTabProps {
  /** horse.ownerId — resolved without staff API. */
  ownerId: number | null;
  /** horse.currentStallId — null means not yet assigned. */
  currentStallId: number | null;
}

type LoadState = 'loading' | 'error' | 'done';

interface StableTeamData {
  stall: StableStall | null;
  area: Area | null;
  groom: UserSummaryResponse | null;
  trainer: UserSummaryResponse | null;
  owner: UserSummaryResponse | null;
}

export function StableTeamTab({ ownerId, currentStallId }: StableTeamTabProps) {
  const [state, setState] = useState<LoadState>('loading');
  const [error, setError] = useState<string | null>(null);
  const [data, setData] = useState<StableTeamData | null>(null);
  const [retryKey, setRetryKey] = useState(0);

  useEffect(() => {
    let active = true;

    async function loadTeam() {
      setState('loading');
      setError(null);
      try {
        let stall: StableStall | null = null;
        let area: Area | null = null;
        let groom: UserSummaryResponse | null = null;
        let trainer: UserSummaryResponse | null = null;
        let owner: UserSummaryResponse | null = null;

        if (currentStallId != null) {
          stall = await managerHorseService.getStallById(currentStallId);
          if (stall.areaId) {
            area = await managerHorseService.getAreaById(stall.areaId);
          }
          if (stall.groomId) {
            groom = await managerHorseService.getUserById(stall.groomId);
          }
          if (area?.trainerId) {
            trainer = await managerHorseService.getUserById(area.trainerId);
          }
        }

        if (ownerId) {
          owner = await managerHorseService.getUserById(ownerId);
        }

        if (active) {
          setData({ stall, area, groom, trainer, owner });
          setState('done');
        }
      } catch (err) {
        if (active) {
          setError(displayError(err, 'Unable to load stable and team information.'));
          setState('error');
        }
      }
    }

    void loadTeam();

    return () => {
      active = false;
    };
  }, [currentStallId, ownerId, retryKey]);

  if (state === 'loading') {
    return (
      <Panel className="space-y-3 p-5">
        <Skeleton className="h-4 w-32" />
        <Skeleton className="h-4 w-64" />
        <Skeleton className="h-4 w-48" />
        <Skeleton className="h-4 w-56" />
      </Panel>
    );
  }

  if (state === 'error') {
    return (
      <Panel padded>
        <EmptyState
          icon="alert-triangle"
          title="Unable to load stable and team"
          description={error ?? 'Data could not be loaded.'}
          action={
            <Button size="sm" icon="refresh" onClick={() => setRetryKey((k) => k + 1)}>
              Retry
            </Button>
          }
        />
      </Panel>
    );
  }

  const NOT_ASSIGNED = <span className="font-normal italic text-[var(--color-text-muted)]">Not assigned</span>;
  const d = data!;

  return (
    <div className="space-y-5">
      {/* Owner */}
      <HorseInfoSection title="Owner" icon="user">
        <div className="space-y-3.5">
          <HorseInfoRow
            label="Owner Name"
            value={
              d.owner ? (
                <Link
                  href={`/manager/accounts?userId=${d.owner.id}`}
                  className="group inline-flex items-center gap-1 font-semibold text-[var(--color-primary)] hover:underline"
                  title="View owner account details"
                >
                  <span>{d.owner.fullName}</span>
                  <Icon name="external-link" size={12} className="shrink-0 opacity-70 group-hover:opacity-100 transition-opacity" />
                </Link>
              ) : (
                NOT_ASSIGNED
              )
            }
          />
          {d.owner?.email && (
            <HorseInfoRow label="Email" value={d.owner.email} />
          )}
        </div>
      </HorseInfoSection>

      {/* Stable assignment */}
      <HorseInfoSection title="Stable Assignment" icon="home">
        {d.stall ? (
          <div className="space-y-3.5">
            <HorseInfoRow
              label="Stall Location"
              value={
                <Link
                  href="/manager/stable"
                  className="group inline-flex items-center gap-1 font-semibold text-[var(--color-primary)] hover:underline"
                  title="View stall on stable board"
                >
                  <span>Stall {d.stall.stallCode}</span>
                  <Icon name="external-link" size={12} className="shrink-0 opacity-70 group-hover:opacity-100 transition-opacity" />
                </Link>
              }
            />
            <HorseInfoRow label="Area" value={d.area ? `${d.area.name}` : '—'} />
            <HorseInfoRow label="Area Type" value={d.area?.type ?? '—'} />
          </div>
        ) : (
          <p className="text-sm text-[var(--color-text-muted)] italic">Not assigned to a stall.</p>
        )}
      </HorseInfoSection>

      {/* Care team */}
      <HorseInfoSection title="Care Team" icon="users">
        <div className="space-y-3.5">
          <HorseInfoRow
            label="Assigned Trainer"
            value={
              d.trainer ? (
                <Link
                  href={`/manager/accounts?userId=${d.trainer.id}`}
                  className="group inline-flex items-center gap-1 font-semibold text-[var(--color-primary)] hover:underline"
                  title="View trainer account details"
                >
                  <span>{d.trainer.fullName}</span>
                  <Icon name="external-link" size={12} className="shrink-0 opacity-70 group-hover:opacity-100 transition-opacity" />
                </Link>
              ) : (
                NOT_ASSIGNED
              )
            }
          />
          <HorseInfoRow
            label="Assigned Groom"
            value={
              d.groom ? (
                <Link
                  href={`/manager/accounts?userId=${d.groom.id}`}
                  className="group inline-flex items-center gap-1 font-semibold text-[var(--color-primary)] hover:underline"
                  title="View groom account details"
                >
                  <span>{d.groom.fullName}</span>
                  <Icon name="external-link" size={12} className="shrink-0 opacity-70 group-hover:opacity-100 transition-opacity" />
                </Link>
              ) : (
                NOT_ASSIGNED
              )
            }
          />
        </div>
      </HorseInfoSection>
    </div>
  );
}
