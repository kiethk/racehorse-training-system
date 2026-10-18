'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Avatar,
  Button,
  EmptyState,
  Icon,
  ListSkeleton,
  Modal,
  Notice,
  PageHeader,
  Panel,
  Pill,
  ScreenLayout,
} from '@/components/ui';
import { useAuth } from '@/context/AuthContext';
import { horseService } from '@/features/horses/services/horseService';
import type { HorseSpecialty } from '@/features/horses/types';
import { displayError } from '@/lib/display';
import { compareAreas } from '../lib/managerAssignments';
import { stableApi } from '../services/stableService';
import { StallCell } from '../shared/components/StallCell';
import { StallDetailPanel } from '../shared/components/StallDetailPanel';
import type { Area, Horse, StableStall, UserSummary } from '../types';

interface BlockInfo {
  number: number;
  stalls: StableStall[];
  groomId: number | null;
  occupied: number;
}

interface TrainerAreaViewData {
  area: Area;
  stalls: StableStall[];
  occupied: number;
  blocks: BlockInfo[];
}

/**
 * Head Trainer's view of the stable.
 * Mirrors the Manager's card layout, but scoped strictly to REGULAR areas
 * managed by the logged-in Trainer, with read-only staff assignments and stall statuses.
 */
export function TrainerStableView() {
  const { user } = useAuth();
  const [areas, setAreas] = useState<Area[]>([]);
  const [stalls, setStalls] = useState<StableStall[]>([]);
  const [horses, setHorses] = useState<Horse[]>([]);
  const [trainers, setTrainers] = useState<UserSummary[]>([]);
  const [grooms, setGrooms] = useState<UserSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [selectedStall, setSelectedStall] = useState<StableStall | null>(null);
  const [specialty, setSpecialty] = useState<{ horseId: number; value: HorseSpecialty } | null>(null);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [areaList, stallList, horseList, trainerList, groomList] = await Promise.all([
        stableApi.getAreas('REGULAR'),
        stableApi.getStalls(),
        stableApi.getHorses(),
        stableApi.getTrainers(),
        stableApi.getGrooms(),
      ]);
      setAreas(areaList);
      setStalls(stallList);
      setHorses(horseList);
      setTrainers(trainerList);
      setGrooms(groomList);
    } catch (err) {
      console.error('Failed to load stable data for trainer:', err);
      setError(displayError(err, 'Unable to load stable data.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  // Horse lookup by stallId
  const horseByStallId = useMemo(() => {
    const map = new Map<number, Horse>();
    horses.forEach((h) => {
      if (h.currentStallId != null) map.set(h.currentStallId, h);
    });
    return map;
  }, [horses]);

  const groomById = useMemo(
    () => new Map(grooms.map((g) => [g.id, g] as const)),
    [grooms],
  );

  const trainerById = useMemo(
    () => new Map(trainers.map((t) => [t.id, t] as const)),
    [trainers],
  );

  // Filter strictly to REGULAR areas managed by this Head Trainer
  const myAreas = useMemo(() => {
    if (!user?.userId) return [];
    return areas
      .filter((a) => a.type === 'REGULAR' && a.trainerId === user.userId)
      .sort(compareAreas);
  }, [areas, user]);

  // Construct structured area views for display
  const areaViews: TrainerAreaViewData[] = useMemo(() => {
    return myAreas.map((area) => {
      const areaStalls = stalls.filter((s) => s.areaId === area.id);
      const occupied = areaStalls.filter((s) => horseByStallId.has(s.id)).length;
      const blocks: BlockInfo[] = [1, 2, 3, 4].map((blockNumber) => {
        const blockStallList = areaStalls
          .filter(
            (s) =>
              s.stallNumber >= (blockNumber - 1) * 3 + 1 &&
              s.stallNumber <= blockNumber * 3,
          )
          .sort((a, b) => a.stallNumber - b.stallNumber);

        const groomIds = blockStallList.map((s) => s.groomId).filter((id): id is number => id != null);
        const groomId = groomIds.length > 0 ? groomIds[0] : null;
        const blockOccupied = blockStallList.filter((s) => horseByStallId.has(s.id)).length;

        return {
          number: blockNumber,
          stalls: blockStallList,
          groomId,
          occupied: blockOccupied,
        };
      });

      return {
        area,
        stalls: areaStalls,
        occupied,
        blocks,
      };
    });
  }, [myAreas, stalls, horseByStallId]);

  const totalStalls = useMemo(
    () => areaViews.reduce((sum, v) => sum + v.stalls.length, 0),
    [areaViews],
  );

  const totalOccupied = useMemo(
    () => areaViews.reduce((sum, v) => sum + v.occupied, 0),
    [areaViews],
  );

  // Selected stall details
  const selectedHorse = selectedStall ? horseByStallId.get(selectedStall.id) : undefined;
  const selectedGroom = selectedStall?.groomId ? groomById.get(selectedStall.groomId) : undefined;
  const selectedArea = selectedStall ? areas.find((a) => a.id === selectedStall.areaId) : undefined;
  const selectedTrainer = selectedArea?.trainerId ? trainerById.get(selectedArea.trainerId) : undefined;

  useEffect(() => {
    if (!selectedHorse?.id) return;
    let active = true;
    horseService
      .getSpecialty(selectedHorse.id)
      .then((val) => {
        if (active) setSpecialty({ horseId: selectedHorse.id, value: val });
      })
      .catch(() => undefined);
    return () => {
      active = false;
    };
  }, [selectedHorse?.id]);

  return (
    <ScreenLayout variant="dashboard">
      <PageHeader
        title="Stable"
        description="View your assigned training areas, stalls, horses, and caring grooms."
        actions={
          <Button
            size="sm"
            variant="secondary"
            icon="refresh"
            disabled={loading}
            loading={loading && areas.length > 0}
            onClick={() => void loadData()}
          >
            Refresh
          </Button>
        }
      />

      {error && (
        <Notice tone="error">
          {error}{' '}
          <Button variant="link" size="sm" onClick={() => void loadData()}>
            Retry
          </Button>
        </Notice>
      )}

      {loading && areas.length === 0 ? (
        <ListSkeleton rows={4} />
      ) : myAreas.length === 0 ? (
        <Panel padded>
          <EmptyState
            icon="building"
            title="No training areas assigned"
            description="You are currently not assigned to manage any regular stable areas. Please contact the Club Manager to assign areas to your account."
            action={
              <Button size="sm" icon="refresh" onClick={() => void loadData()}>
                Refresh
              </Button>
            }
          />
        </Panel>
      ) : (
        <div className="space-y-4">
          {/* Summary Stats Bar */}
          <div className="flex flex-wrap items-center gap-3 text-sm text-[var(--color-text-secondary)]">
            <span>
              <strong className="font-semibold text-[var(--color-text-primary)]">
                {myAreas.length}
              </strong>{' '}
              {myAreas.length === 1 ? 'area' : 'areas'}
            </span>
            <span aria-hidden="true" className="text-[var(--color-border-strong)]">
              /
            </span>
            <span>
              <strong className="font-semibold text-[var(--color-text-primary)]">
                {totalStalls}
              </strong>{' '}
              stalls
            </span>
            <span aria-hidden="true" className="text-[var(--color-border-strong)]">
              /
            </span>
            <span>
              <strong className="font-semibold text-[var(--color-text-primary)]">
                {totalOccupied}
              </strong>{' '}
              occupied
            </span>
          </div>

          {/* Area Cards Grid */}
          <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
            {areaViews.map((view) => {
              const assignedTrainer = view.area.trainerId
                ? trainerById.get(view.area.trainerId)
                : null;
              const isFull = view.occupied === view.stalls.length && view.stalls.length > 0;

              return (
                <Panel
                  key={view.area.id}
                  data-area-card={view.area.code}
                  className="min-w-0 overflow-hidden"
                >
                  <div className="flex items-center justify-between gap-3 px-4 py-4">
                    <div>
                      <h2 className="text-lg font-semibold text-[var(--color-text-primary)]">
                        Area {view.area.code}
                      </h2>
                      <p className="mt-0.5 text-xs text-[var(--color-text-secondary)]">
                        {view.occupied} / {view.stalls.length} occupied
                      </p>
                    </div>
                    {isFull ? (
                      <Pill tone="info" size="sm">
                        Full
                      </Pill>
                    ) : (
                      <Pill tone="neutral" size="sm">
                        {view.stalls.length - view.occupied} available
                      </Pill>
                    )}
                  </div>

                  <div className="space-y-3 px-3 pb-3">
                    {/* Read-Only Trainer Assignment Display */}
                    <div className="flex min-w-0 items-center gap-2.5 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-2 text-sm">
                      {assignedTrainer ? (
                        <Avatar name={assignedTrainer.fullName} />
                      ) : (
                        <Icon name="user" size={16} className="text-[var(--color-text-muted)]" />
                      )}
                      <div className="min-w-0 flex-1">
                        <span className="block text-xs text-[var(--color-text-secondary)]">
                          Assigned Trainer
                        </span>
                        <p className="truncate font-medium text-[var(--color-text-primary)]">
                          {assignedTrainer?.fullName ?? 'Unassigned'}
                        </p>
                      </div>
                    </div>

                    {/* 4 Blocks of Stalls (2x2 grid) */}
                    <div className="grid grid-cols-2 gap-3">
                      {view.blocks.map((block) => {
                        const assignedGroom = block.groomId
                          ? groomById.get(block.groomId)
                          : null;

                        return (
                          <div
                            key={block.number}
                            className="min-w-0 space-y-2 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-2"
                          >
                            {/* 3 Stalls in Block */}
                            <div className="grid grid-cols-3 gap-1.5">
                              {block.stalls.map((stall) => (
                                <StallCell
                                  key={stall.id}
                                  stall={stall}
                                  horse={horseByStallId.get(stall.id)}
                                  selected={selectedStall?.id === stall.id}
                                  onSelect={setSelectedStall}
                                  compact
                                />
                              ))}
                            </div>

                            {/* Read-Only Groom Display */}
                            <div className="flex min-w-0 items-center gap-2 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] px-2 py-1.5 text-xs">
                              {assignedGroom ? (
                                <Avatar name={assignedGroom.fullName} size="sm" />
                              ) : (
                                <Icon
                                  name="user"
                                  size={13}
                                  className="text-[var(--color-text-muted)] shrink-0"
                                />
                              )}
                              <span className="min-w-0 flex-1 truncate font-medium text-[var(--color-text-primary)]">
                                {assignedGroom?.fullName ?? 'Unassigned Groom'}
                              </span>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                </Panel>
              );
            })}
          </div>
        </div>
      )}

      {/* Read-Only Stall Detail Modal */}
      <Modal
        open={selectedStall !== null}
        title={`Stall ${selectedStall?.stallCode ?? ''} details`}
        onClose={() => setSelectedStall(null)}
      >
        {selectedStall && (
          <StallDetailPanel
            stall={selectedStall}
            area={areas.find((a) => a.id === selectedStall.areaId)}
            horse={selectedHorse}
            specialty={
              specialty && specialty.horseId === selectedHorse?.id ? specialty.value : null
            }
            hint="View stall status, horse profile, and assigned staff."
          >
            <dl className="space-y-2 text-sm">
              <div className="flex justify-between gap-3">
                <dt className="text-[var(--color-text-secondary)]">Assigned Groom</dt>
                <dd className="text-right font-medium text-[var(--color-text-primary)]">
                  {selectedGroom?.fullName ??
                    (selectedStall.groomId ? `Groom #${selectedStall.groomId}` : 'Unassigned')}
                </dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-[var(--color-text-secondary)]">Assigned Trainer</dt>
                <dd className="text-right font-medium text-[var(--color-text-primary)]">
                  {selectedTrainer?.fullName ?? 'Unassigned'}
                </dd>
              </div>
            </dl>
          </StallDetailPanel>
        )}
      </Modal>
    </ScreenLayout>
  );
}
