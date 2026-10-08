'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { Button } from '@/components/ui/Button';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { stableApi } from '../services/stableService';
import type { Area, Horse, StableStall, UserSummary } from '../types';
import { AssignHorseDialog } from './AssignHorseDialog';

export function StableMap() {
  const { user } = useAuth();
  const [areas, setAreas] = useState<Area[]>([]);
  const [stalls, setStalls] = useState<StableStall[]>([]);
  const [horses, setHorses] = useState<Horse[]>([]);
  /**
   * Ngựa CHƯA xếp chuồng — danh sách riêng cho hộp thoại xếp ngựa.
   * Không dùng chung với `horses` được: `horses` lấy bằng mine=true nên
   * backend đã loại hết ngựa chưa có chuồng, lọc lại ở client sẽ luôn rỗng.
   */
  const [unassignedHorses, setUnassignedHorses] = useState<Horse[]>([]);
  /**
   * Chỉ dùng để hiện TÊN Groom trên từng ô chuồng.
   *
   * Trainer không còn phân công Groom (V63 — việc đó thuộc Quản lý câu lạc
   * bộ), nhưng vẫn phải biết ai chăm con nào để điều phối lot: khi hai ngựa
   * cùng một Groom bị xếp vào cùng một lot thì vướng BR-09, và cách chữa là
   * dời ngựa sang chuồng của Groom khác hoặc chọn Groom riêng cho buổi tập.
   */
  const [grooms, setGrooms] = useState<UserSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [assignHorseStall, setAssignHorseStall] = useState<StableStall | null>(null);
  /** Chuồng đang chờ xác nhận gỡ ngựa. */
  const [unassignStall, setUnassignStall] = useState<StableStall | null>(null);
  const [unassigning, setUnassigning] = useState(false);
  const [unassignError, setUnassignError] = useState<string | null>(null);

  const loadAll = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [areasRes, stallsRes, horsesRes, groomsRes, unassignedRes] = await Promise.all([
        stableApi.getAreas(),
        stableApi.getStalls(),
        stableApi.getHorses({ mine: true }),        // đã xếp chuồng -> vẽ lên ô
        stableApi.getGrooms(),
        stableApi.getHorses({ unassigned: true }),  // chưa xếp -> cho hộp thoại
      ]);
      setAreas(areasRes);
      setStalls(stallsRes);
      setHorses(horsesRes);
      setGrooms(groomsRes);
      setUnassignedHorses(unassignedRes);
    } catch (err) {
      console.error('Failed to load stable data:', err);
      setError('Unable to load the stable map. Please try again later.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadAll();
  }, [loadAll]);

  const horseByStallId = useMemo(() => {
    const m = new Map<number, Horse>();
    horses.forEach((h) => {
      if (h.currentStallId) m.set(h.currentStallId, h);
    });
    return m;
  }, [horses]);

  const groomById = useMemo(
    () => new Map(grooms.map((g) => [g.id, g] as const)),
    [grooms],
  );

  // Chỉ hiện khu REGULAR do chính Trainer này phụ trách (nếu có trainerId), hoặc tất cả khu REGULAR
  const myAreas = useMemo(() => {
    const filtered = areas.filter(
      (a) => a.type === 'REGULAR' && (user?.userId ? a.trainerId === user.userId : true),
    );
    return filtered.length > 0 ? filtered : areas.filter((a) => a.type === 'REGULAR');
  }, [areas, user]);

  const stallsByAreaId = useMemo(() => {
    const m = new Map<number, StableStall[]>();
    stalls.forEach((s) => {
      const list = m.get(s.areaId) ?? [];
      list.push(s);
      m.set(s.areaId, list);
    });
    return m;
  }, [stalls]);

  if (loading) return <ListSkeleton rows={4} />;

  if (error) {
    return (
      <Panel padded>
        <EmptyState icon="alert-triangle" title="Unable to load data" description={error} />
      </Panel>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-[18px] font-semibold tracking-tight text-[var(--color-text-primary)]">
            Stable map
          </h1>
          <p className="text-[12px] text-[var(--color-text-secondary)]">
            Assign horses to stalls and view the responsible groom.
          </p>
        </div>
        <Button variant="secondary" size="sm" onClick={() => loadAll()}>
          Refresh
        </Button>
      </div>

      {myAreas.length === 0 ? (
        <Panel padded>
          <EmptyState
            icon="activity"
            title="No training area assigned"
            description="You have not been assigned a REGULAR stable area."
          />
        </Panel>
      ) : (
        myAreas.map((area) => {
          const areaStalls = stallsByAreaId.get(area.id) ?? [];
          return (
            <Panel key={area.id} padded>
              <div className="mb-4 flex items-center justify-between border-b border-[var(--color-border)] pb-3">
                <div className="flex items-center gap-2">
                  <h2 className="text-[15px] font-semibold text-[var(--color-text-primary)]">
                    Khu {area.code} — {area.name}
                  </h2>
                  <Pill tone="primary" size="sm">
                    {areaStalls.length} stalls
                  </Pill>
                </div>
              </div>

              {areaStalls.length === 0 ? (
                <p className="text-[12px] text-[var(--color-text-muted)]">This area has no stalls.</p>
              ) : (
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
                  {areaStalls.map((stall) => {
                    const currentHorse = horseByStallId.get(stall.id);
                    const currentGroom = stall.groomId ? groomById.get(stall.groomId) : null;
                    const isOccupied = stall.status === 'OCCUPIED' || !!currentHorse;

                    return (
                      <div
                        key={stall.id}
                        className={`flex flex-col justify-between rounded-[var(--radius-md)] border p-3 transition ${
                          isOccupied
                            ? 'border-[var(--color-border-strong)] bg-[var(--color-surface-muted)]'
                            : 'border-dashed border-[var(--color-border)] bg-[var(--color-surface)]'
                        }`}
                      >
                        <div>
                          <div className="flex items-center justify-between">
                            <span className="text-[13px] font-bold text-[var(--color-text-primary)]">
                              Stall {stall.stallCode}
                            </span>
                            <span
                              className={`rounded-full px-2 py-0.5 text-[10px] font-medium ${
                                isOccupied
                                  ? 'bg-[var(--color-success-soft)] text-[var(--color-success)]'
                                  : 'bg-[var(--color-surface-muted)] text-[var(--color-text-secondary)]'
                              }`}
                            >
                              {isOccupied ? 'Occupied' : 'Available'}
                            </span>
                          </div>

                          <div className="mt-2 text-[12px]">
                            <div className="text-[11px] text-[var(--color-text-muted)]">Horse:</div>
                            <div className="font-semibold text-[var(--color-text-primary)] truncate">
                              {currentHorse ? currentHorse.name : 'No horse assigned'}
                            </div>
                            {currentHorse?.breed && (
                              <div className="text-[11px] text-[var(--color-text-secondary)]">
                                {currentHorse.breed}
                              </div>
                            )}
                          </div>

                          <div className="mt-2">
                            <div className="text-[11px] text-[var(--color-text-muted)]">Responsible groom:</div>
                            {stall.groomId ? (
                              <Pill tone="info" size="sm">
                                {currentGroom ? currentGroom.fullName : `#${stall.groomId}`}
                              </Pill>
                            ) : (
                              <span className="text-[11px] text-[var(--color-text-muted)] italic">
                                Unassigned
                              </span>
                            )}
                          </div>
                        </div>

                        <div className="mt-4 flex gap-1.5 pt-2 border-t border-[var(--color-border)]">
                          <button
                            type="button"
                            onClick={() => setAssignHorseStall(stall)}
                            disabled={isOccupied}
                            className={`flex-1 rounded px-2 py-1 text-[11px] font-medium text-center transition ${
                              isOccupied
                                ? 'cursor-not-allowed opacity-40 bg-[var(--color-surface)] text-[var(--color-text-muted)]'
                                : 'bg-[var(--color-primary-soft)] text-[var(--color-primary)] hover:bg-[var(--color-primary)] hover:text-white'
                            }`}
                          >
                            {isOccupied ? 'Horse assigned' : 'Assign horse'}
                          </button>
                          {isOccupied && (
                            <button
                              type="button"
                              onClick={() => {
                                setUnassignError(null);
                                setUnassignStall(stall);
                              }}
                              className="flex-1 rounded px-2 py-1 text-[11px] font-medium text-center bg-[var(--color-surface)] border border-[var(--color-border)] text-[var(--color-danger)] hover:bg-[var(--color-danger-soft)] transition"
                            >
                              Unassign horse
                            </button>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </Panel>
          );
        })
      )}

      {/* Dialogs */}
      <AssignHorseDialog
        open={assignHorseStall !== null}
        stall={assignHorseStall}
        horses={unassignedHorses}
        onClose={() => setAssignHorseStall(null)}
        onAssigned={loadAll}
      />

      <ConfirmDialog
        open={unassignStall !== null}
        tone="danger"
        title={`Unassign horse from stall ${unassignStall?.stallCode ?? ''}?`}
        confirmLabel="Unassign horse"
        cancelLabel="Cancel"
        loading={unassigning}
        description={
          <div className="space-y-1.5">
            <p>
              The horse{' '}
              <strong>
                {unassignStall ? horseByStallId.get(unassignStall.id)?.name ?? '' : ''}
              </strong>{' '}
              will no longer be assigned to a stall.
            </p>
            <p>
              <strong>Upcoming</strong> training sessions will become
              &quot;groom unassigned&quot;. Completed sessions remain unchanged.
            </p>
            {unassignError && (
              <p className="text-[var(--color-danger)]">{unassignError}</p>
            )}
          </div>
        }
        onCancel={() => setUnassignStall(null)}
        onConfirm={async () => {
          if (!unassignStall) return;
          const horse = horseByStallId.get(unassignStall.id);
          if (!horse) return;
          try {
            setUnassigning(true);
            setUnassignError(null);
            await stableApi.unassignHorseFromStall(horse.id);
            setUnassignStall(null);
            await loadAll();
          } catch (err) {
            setUnassignError(err instanceof Error ? err.message : 'Unable to unassign the horse.');
          } finally {
            setUnassigning(false);
          }
        }}
      />
    </div>
  );
}
