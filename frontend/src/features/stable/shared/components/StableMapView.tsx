'use client';

import dynamic from 'next/dynamic';
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { EmptyState, IconButton, Notice, Panel, Pill, SegmentedControl, Spinner } from '@/components/ui';
import type { Area, Horse, StableStall } from '../../types';
import { AreaPicker } from './AreaPicker';
import type { BarnViewMode } from './barn3d/CameraRig';
import { OWN_LABEL, StableFloorPlan, orderStableAreas, type StableFloorPlanProps } from './StableFloorPlan';
import { StableLegend } from './StableLegend';

// three.js is only downloaded when someone opens the 3D view.
const BarnScene = dynamic(() => import('./barn3d/BarnScene'), {
  ssr: false,
  loading: () => (
    <div className="flex h-full items-center justify-center gap-2 text-sm text-[var(--color-text-secondary)]">
      <Spinner size="sm" /> Preparing the 3D barn…
    </div>
  ),
});

type StableView = 'plan' | '3d';
const VIEW_KEY = 'rtms.stable.view';

function readStoredView(): StableView {
  try {
    return window.localStorage.getItem(VIEW_KEY) === '3d' ? '3d' : 'plan';
  } catch {
    return 'plan';
  }
}

interface StableMapViewProps extends StableFloorPlanProps {
  /** Staff names for the 3D figures; null when the viewer may not read the staff directory. */
  personName: (kind: 'groom' | 'trainer', id: number) => string | null;
  currentUserId?: number | null;
  /** When given, horses can be dragged onto empty stalls in the 3D view. */
  onMoveHorse?: (horse: Horse, to: StableStall) => void;
  /** Extra note after the colour key. */
  legendNote?: ReactNode;
}

/** The stable map with two views of the same data: the 2D plan (default, accessible) and the 3D barn. */
export function StableMapView({ personName, currentUserId, onMoveHorse, legendNote, ...plan }: StableMapViewProps) {
  const { areas, stalls, horseByStallId, selectedStallId, onSelectStall, ownStallIds, lockOthers, lens } = plan;
  const [view, setView] = useState<StableView>('plan');
  const [unavailable, setUnavailable] = useState(false);
  const [viewMode, setViewMode] = useState<BarnViewMode>('isometric');
  const [resetSignal, setResetSignal] = useState(0);
  const [pickedAreaId, setPickedAreaId] = useState<number | null>(null);
  const [hint, setHint] = useState<string | null>(null);

  // Restored after mount: localStorage does not exist on the server render.
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setView(readStoredView());
  }, []);

  const changeView = (next: StableView) => {
    setView(next);
    setUnavailable(false);
    try { window.localStorage.setItem(VIEW_KEY, next); } catch { /* Private mode: the choice just is not remembered. */ }
  };

  const orderedAreas = useMemo(() => orderStableAreas(areas), [areas]);
  const selectedAreaId = stalls.find((stall) => stall.id === selectedStallId)?.areaId;
  // Follow the selection: picking a stall in the plan opens its area in 3D.
  const areaId = selectedAreaId ?? pickedAreaId ?? orderedAreas[0]?.id;
  const area = orderedAreas.find((item) => item.id === areaId) ?? orderedAreas[0];
  const areaStalls = useMemo(() => stalls.filter((stall) => stall.areaId === area?.id), [stalls, area]);
  // A locked view only knows about the viewer's own stalls, so counts cover only those.
  const isCounted = useCallback(
    (stall: StableStall) => !lockOthers || !ownStallIds || ownStallIds.has(stall.id),
    [lockOthers, ownStallIds],
  );
  const countedStalls = areaStalls.filter(isCounted);
  const occupied = countedStalls.filter((stall) => horseByStallId.has(stall.id)).length;
  // Per-area counts for the area picker, from data already on screen.
  const occupancy = useMemo(() => {
    const counts = new Map<number, { occupied: number; total: number }>();
    stalls.filter(isCounted).forEach((stall) => {
      const count = counts.get(stall.areaId) ?? { occupied: 0, total: 0 };
      counts.set(stall.areaId, { occupied: count.occupied + Number(horseByStallId.has(stall.id)), total: count.total + 1 });
    });
    return counts;
  }, [stalls, horseByStallId, isCounted]);

  const handleUnavailable = useCallback(() => {
    setUnavailable(true);
    setView('plan');
  }, []);

  const pickArea = (next: Area) => {
    setPickedAreaId(next.id);
    if (selectedAreaId != null && selectedAreaId !== next.id) onSelectStall(null);
  };

  const ownNote = !ownStallIds ? null
    : lockOthers ? `Only ${OWN_LABEL[lens].toLowerCase()} are shown; other stalls are locked.`
    : `${OWN_LABEL[lens]} are shown at full strength; the rest are faded.`;
  const is3d = view === '3d' && !unavailable;

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <SegmentedControl<StableView>
          label="Stable view"
          value={is3d ? '3d' : 'plan'}
          onChange={changeView}
          options={[{ value: 'plan', label: 'Plan', icon: 'grid' }, { value: '3d', label: '3D barn', icon: 'layers' }]}
        />
        {is3d && orderedAreas.length > 0 && (
          <div className="flex flex-wrap items-center gap-2">
            {orderedAreas.length > 1 && area && (
              <AreaPicker areas={orderedAreas} value={area} onChange={pickArea} occupancy={occupancy} />
            )}
            <SegmentedControl<BarnViewMode>
              label="Camera"
              value={viewMode}
              onChange={setViewMode}
              options={[{ value: 'isometric', label: 'Isometric' }, { value: 'top', label: 'Top' }]}
            />
            <IconButton icon="rotate" label="Show the whole barn" size="sm" variant="secondary" onClick={() => setResetSignal((n) => n + 1)} />
          </div>
        )}
      </div>

      {unavailable && (
        <Notice tone="warning" onDismiss={() => setUnavailable(false)}>
          The 3D barn could not start in this browser (WebGL is unavailable), so the plan is shown instead.
        </Notice>
      )}

      <StableLegend>{legendNote ?? ownNote}</StableLegend>

      {!is3d ? (
        <StableFloorPlan {...plan} />
      ) : !area ? (
        <Panel padded>
          <EmptyState icon="building" title={plan.emptyTitle ?? 'No stable areas to show'} description={plan.emptyDescription} />
        </Panel>
      ) : (
        <Panel className="relative h-[clamp(28rem,calc(100vh-18rem),44rem)] overflow-hidden">
          {areaStalls.length === 0 ? (
            <EmptyState icon="building" title={`${area.name} has no stalls yet`} />
          ) : (
            <BarnScene
              area={area}
              stalls={areaStalls}
              horseByStallId={horseByStallId}
              selectedStallId={selectedStallId}
              onSelectStall={onSelectStall}
              ownStallIds={ownStallIds}
              lockOthers={lockOthers}
              personName={personName}
              currentUserId={currentUserId}
              viewMode={viewMode}
              resetSignal={resetSignal}
              onMoveHorse={onMoveHorse}
              onUnavailable={handleUnavailable}
              onHint={setHint}
            />
          )}
          <div className="pointer-events-none absolute inset-x-3 top-3 flex items-start justify-between gap-3">
            <div data-scene-overlay className="flex items-center gap-2 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-1.5 shadow-[var(--shadow-panel)]">
              <span className="text-sm font-semibold text-[var(--color-text-primary)]">{area.name}</span>
              {area.type === 'QUARANTINE' && <Pill tone="warning" size="sm" icon="shield">Quarantine</Pill>}
              <span className="text-xs text-[var(--color-text-muted)]">{occupied} of {lockOthers ? 'your ' : ''}{countedStalls.length} occupied</span>
            </div>
            <p data-scene-overlay className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-1.5 text-xs text-[var(--color-text-secondary)] shadow-[var(--shadow-panel)]">
              {hint ?? (onMoveHorse
                ? 'Drag to orbit · Scroll to zoom · Click a stall · Drag a horse to an empty stall'
                : 'Drag to orbit · Scroll to zoom · Click a stall or a groom in the key')}
            </p>
          </div>
        </Panel>
      )}
    </div>
  );
}
