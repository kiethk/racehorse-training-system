'use client';

import { Canvas } from '@react-three/fiber';
import {
  Component,
  Suspense,
  useCallback,
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
  type Dispatch,
  type ReactNode,
  type SetStateAction,
} from 'react';
import * as THREE from 'three';
import type { Area, Horse, StableStall } from '../../../types';
import { blockNumberOf, layoutArea, type AreaLayout } from '../../lib/stallLayout';
import { STALL_TONE_TOKEN, stallState, type StallTone } from '../StallCell';
import { BarnStall, type DropState } from './BarnStall';
import { BarnStructure } from './BarnStructure';
import { CameraRig, type BarnViewMode } from './CameraRig';
import { GroomKey, GroomLink, groomBadgesFor, type GroomBadge } from './GroomKey';
import { HorseFigure, preloadHorseModel } from './HorseFigure';
import {
  ENTRANCE,
  ENTRANCE_X,
  STALL_PITCH,
  barnSize,
  blockStandPosition,
  initials,
  readToken,
  stallPosition,
  useSceneColors,
  type SceneColors,
} from './sceneResources';
import { LabelLayer, LabelProjector, type LabelElements, type SceneLabel } from './SceneLabels';
import { StaffFigure } from './StaffFigure';
import { canReceiveHorse, useHorseDrag } from './useHorseDrag';
import { groomDropBlock, useGroomDrag } from './useGroomDrag';

export interface BarnSceneProps {
  area: Area;
  /** Stalls of `area` only. */
  stalls: StableStall[];
  horseByStallId: ReadonlyMap<number, Horse>;
  selectedStallId: number | null;
  onSelectStall: (stall: StableStall | null) => void;
  /** Stalls in the viewer's own scope; everything else is faded. */
  ownStallIds?: ReadonlySet<number>;
  /** With `ownStallIds`: other stalls are bare frames with no label, groom or interaction. */
  lockOthers?: boolean;
  /** Display name for a person, or null when the viewer may not read the staff directory. */
  personName: (kind: 'groom' | 'trainer', id: number) => string | null;
  currentUserId?: number | null;
  viewMode: BarnViewMode;
  /** Changing it frames the whole barn again. */
  resetSignal: number;
  /** Lets the viewer drag a horse onto an empty stall of this area. */
  onMoveHorse?: (horse: Horse, to: StableStall) => void;
  /** Lets the viewer drag a groom onto another block of this area (Manager staffing). */
  onMoveGroom?: (groomId: number, toBlockNumber: number) => void;
  /** Clicking the trainer figure opens the trainer choice for this area (Manager staffing). */
  onTrainerClick?: () => void;
  /** Called when WebGL is missing or the scene fails, so the screen can fall back to the plan. */
  onUnavailable: () => void;
  onHint: (hint: string | null) => void;
}

interface ContentsProps extends BarnSceneProps {
  colors: SceneColors;
  locked: boolean;
  setLocked: (locked: boolean) => void;
  setCursor: (cursor: string) => void;
  onLabels: (labels: SceneLabel[]) => void;
  labelElements: LabelElements;
  layout: AreaLayout;
  groomBadges: ReadonlyMap<number, GroomBadge>;
  hoveredGroomId: number | null;
  setHoveredGroomId: (groomId: number | null) => void;
  pinnedGroomId: number | null;
  setPinnedGroomId: Dispatch<SetStateAction<number | null>>;
}

function BarnContents(props: ContentsProps) {
  const { area, layout, groomBadges, horseByStallId, selectedStallId, ownStallIds, colors, onMoveHorse, onHint } = props;
  const { hoveredGroomId, setHoveredGroomId, pinnedGroomId, setPinnedGroomId } = props;
  const { drag, startDrag, consumeClick } = useHorseDrag({ layout, horseByStallId, onMoveHorse, setLocked: props.setLocked });
  const groomDrag = useGroomDrag({ layout, onMoveGroom: props.onMoveGroom, setLocked: props.setLocked });
  const groomTargetBlock = groomDropBlock(groomDrag.drag);
  const { width, depth } = barnSize(layout.columns);
  const slotById = useMemo(() => new Map(layout.slots.map((slot) => [slot.stall.id, slot])), [layout]);

  const [hoveredStallId, setHoveredStallId] = useState<number | null>(null);

  // Frame the selected stall; "Reset view" frames the whole barn until the next selection.
  const [framing, setFraming] = useState({ stallId: selectedStallId, reset: props.resetSignal });
  if (framing.stallId !== selectedStallId || framing.reset !== props.resetSignal) {
    setFraming({ stallId: framing.reset !== props.resetSignal ? null : selectedStallId, reset: props.resetSignal });
  }
  const focusSlot = framing.stallId != null ? slotById.get(framing.stallId) : undefined;

  const groomColors = useMemo(() => new Map([...groomBadges].map(([id, badge]) => [id, badge.color])), [groomBadges]);
  const ringMaterials = useMemo(
    () => new Map([...groomColors].map(([id, color]) => [id, new THREE.MeshBasicMaterial({ color, transparent: true, opacity: 0.85 })])),
    [groomColors],
  );
  useEffect(() => () => ringMaterials.forEach((material) => material.dispose()), [ringMaterials]);

  const toneColors = useMemo(
    () => Object.fromEntries(Object.entries(STALL_TONE_TOKEN).map(([tone, token]) => [tone, readToken(token)])) as Record<StallTone, string>,
    [],
  );

  const focusGroomId = hoveredGroomId ?? pinnedGroomId;
  const selectedSlot = selectedStallId != null ? slotById.get(selectedStallId) : undefined;
  const hoveredSlot = hoveredStallId != null ? slotById.get(hoveredStallId) : undefined;
  const highlightedGroomId = focusGroomId ?? hoveredSlot?.stall.groomId ?? selectedSlot?.stall.groomId ?? null;

  // Someone else's stall in a locked view: drawn as a bare frame, with no label, groom or interaction.
  const { lockOthers } = props;
  const isLocked = useCallback(
    (stallId: number) => !!lockOthers && !!ownStallIds && !ownStallIds.has(stallId),
    [lockOthers, ownStallIds],
  );
  const visibleBlocks = useMemo(
    () => layout.blocks.filter((block) => block.stallIds.some((stallId) => !isLocked(stallId))),
    [layout, isLocked],
  );

  // The cursor is styled on the canvas wrapper, never on document.body.
  const { setCursor } = props;
  useEffect(() => {
    setCursor(drag?.active || groomDrag.drag?.active ? 'grabbing' : hoveredStallId != null ? 'pointer' : '');
  }, [setCursor, drag?.active, groomDrag.drag?.active, hoveredStallId]);

  const { personName: nameOf } = props;
  useEffect(() => {
    if (groomDrag.drag?.active) {
      const name = nameOf('groom', groomDrag.drag.groomId) ?? `Groom #${groomDrag.drag.groomId}`;
      onHint(groomTargetBlock != null ? `${name} → block ${groomTargetBlock}` : `Moving ${name} · drop on another block · Esc to cancel`);
    } else if (drag?.active) {
      onHint(drag.target ? `${drag.horse.name} → stall ${drag.target.stall.stallCode}` : `Moving ${drag.horse.name} · Esc to cancel`);
    } else if (hoveredSlot) {
      const horse = horseByStallId.get(hoveredSlot.stall.id);
      onHint(`Stall ${hoveredSlot.stall.stallCode} · ${horse ? horse.name : stallState(hoveredSlot.stall, horse).label}`);
    } else {
      onHint(null);
    }
  }, [drag, groomDrag.drag, groomTargetBlock, nameOf, hoveredSlot, horseByStallId, onHint]);

  // Kept in a ref so `select` stays stable and the memoised stalls do not re-render on every parent render.
  const onSelectStall = useRef(props.onSelectStall);
  useEffect(() => { onSelectStall.current = props.onSelectStall; }, [props.onSelectStall]);
  const consumeGroomClick = groomDrag.consumeClick;
  const select = useCallback((stallId: number) => {
    const afterHorseDrag = consumeClick();
    const afterGroomDrag = consumeGroomClick();
    if (afterHorseDrag || afterGroomDrag) return;
    const slot = slotById.get(stallId);
    if (slot) onSelectStall.current(slot.stall);
  }, [slotById, consumeClick, consumeGroomClick]);

  const { personName, currentUserId, onLabels } = props;

  const labels = useMemo((): SceneLabel[] => {
    const stallLabels = layout.slots.filter((slot) => !isLocked(slot.stall.id)).map((slot): SceneLabel => {
      const [x, z] = stallPosition(slot.column, slot.row, layout.columns);
      const horse = horseByStallId.get(slot.stall.id);
      const selected = slot.stall.id === selectedStallId;
      const hovered = slot.stall.id === hoveredStallId;
      const inFocus = focusGroomId == null || slot.stall.groomId === focusGroomId;
      const badge = slot.stall.groomId != null ? groomBadges.get(slot.stall.groomId) ?? null : null;
      return {
        kind: 'stall',
        key: `stall-${slot.stall.id}`,
        // Over the middle of the stall, above the horse's head.
        position: [x, 2.1, z],
        // Selected and hovered first, then the focused groom's horses, then other horses, then empty stalls.
        priority: selected ? 100 : hovered ? 98
          : horse && focusGroomId != null && inFocus ? 97
          : (horse ? 62 : 10) + (inFocus ? 5 : 0),
        code: slot.stall.stallCode,
        horseName: horse?.name ?? null,
        toneColor: toneColors[stallState(slot.stall, horse).tone],
        groom: horse || selected || hovered ? badge : null,
        selected,
        dimmed: (ownStallIds ? !ownStallIds.has(slot.stall.id) : false) || !inFocus,
      };
    });
    const groomLabels = visibleBlocks.map((block): SceneLabel => {
      const [x, z] = blockStandPosition(block, layout.columns);
      const badge = block.groomId != null ? groomBadges.get(block.groomId) : undefined;
      const highlighted = block.groomId != null && block.groomId === highlightedGroomId;
      return {
        kind: 'staff',
        key: `block-${block.blockNumber}`,
        position: [x, 2.2, z],
        priority: highlighted ? 95 : 60,
        name: block.mixed ? 'Mixed grooms' : badge?.name ?? 'No groom',
        badge: badge?.initials ?? '?',
        color: badge?.color ?? null,
        caption: `Block ${block.blockNumber}`,
        isSelf: block.groomId != null && block.groomId === currentUserId,
        highlighted,
        faded: focusGroomId != null && block.groomId !== focusGroomId,
      };
    });
    if (area.type !== 'REGULAR') return [...stallLabels, ...groomLabels];
    const trainerName = area.trainerId != null ? personName('trainer', area.trainerId) : null;
    return [...stallLabels, ...groomLabels, {
      kind: 'staff',
      key: 'trainer',
      position: [ENTRANCE_X(layout.columns), 2.3, 0],
      priority: 50,
      name: area.trainerId == null ? 'No trainer' : trainerName ?? 'Trainer',
      badge: trainerName ? initials(trainerName) : area.trainerId == null ? '?' : 'T',
      color: area.trainerId != null ? colors.primary : null,
      caption: 'Trainer',
      isSelf: area.trainerId != null && area.trainerId === currentUserId,
      highlighted: false,
      faded: focusGroomId != null,
    }];
  }, [layout, horseByStallId, selectedStallId, hoveredStallId, ownStallIds, focusGroomId, personName, groomBadges,
    currentUserId, highlightedGroomId, area, colors.primary, toneColors, isLocked, visibleBlocks]);
  useEffect(() => { onLabels(labels); }, [labels, onLabels]);

  // Lines from the highlighted groom to each horse they look after.
  const groomLinks = useMemo(() => {
    if (highlightedGroomId == null) return [];
    const color = groomColors.get(highlightedGroomId);
    if (!color) return [];
    return layout.blocks.filter((block) => block.groomId === highlightedGroomId).flatMap((block) => {
      const [gx, gz] = blockStandPosition(block, layout.columns);
      return layout.slots
        .filter((slot) => slot.blockNumber === block.blockNumber && horseByStallId.has(slot.stall.id))
        .filter((slot) => !(drag?.active && drag.horse.id === horseByStallId.get(slot.stall.id)?.id))
        .map((slot) => {
          const [hx, hz] = stallPosition(slot.column, slot.row, layout.columns);
          // A low arc from the groom's shoulder to the horse's back, high enough to clear the partitions.
          const points: [number, number, number][] = [0, 0.25, 0.5, 0.75, 1].map((t) => [
            gx + (hx - gx) * t, 1.6 + Math.sin(Math.PI * t) * 0.9, gz + (hz - gz) * t,
          ]);
          return { key: `${block.blockNumber}-${slot.stall.id}`, color, points };
        });
    });
  }, [highlightedGroomId, groomColors, layout, horseByStallId, drag]);

  return (
    <>
      <color attach="background" args={[colors.background]} />
      <ambientLight intensity={1.1} />
      <hemisphereLight args={[colors.surface, colors.borderStrong, 1]} />
      <directionalLight
        position={[-8, 18, 10]}
        intensity={1.8}
        castShadow
        shadow-mapSize={[1024, 1024]}
        shadow-camera-left={-width / 2 - 2}
        shadow-camera-right={width / 2 + 2}
        shadow-camera-top={depth / 2 + 2}
        shadow-camera-bottom={-depth / 2 - 2}
      />

      <BarnStructure columns={layout.columns} />

      {/* Each groom's spot in the aisle: a mat in their colour along the front of their three stalls. */}
      {visibleBlocks.map((block) => {
        const color = block.groomId != null ? groomColors.get(block.groomId) : undefined;
        if (!color) return null;
        const [x, z] = blockStandPosition(block, layout.columns);
        const span = (block.lastColumn - block.firstColumn + 1) * STALL_PITCH - 0.3;
        const highlighted = block.groomId === highlightedGroomId;
        return (
          <mesh key={`zone-${block.blockNumber}`} position={[x, 0.012, z]} rotation={[-Math.PI / 2, 0, 0]} raycast={() => null}>
            <planeGeometry args={[span, 1.1]} />
            <meshBasicMaterial color={color} transparent opacity={highlighted ? 0.4 : 0.18} depthWrite={false} />
          </mesh>
        );
      })}

      {groomLinks.map((link) => (
        <GroomLink key={link.key} points={link.points} color={link.color} />
      ))}

      {layout.slots.map((slot) => {
        const [x, z] = stallPosition(slot.column, slot.row, layout.columns);
        const horse = horseByStallId.get(slot.stall.id);
        const state = stallState(slot.stall, horse);
        const inFocus = focusGroomId == null || slot.stall.groomId === focusGroomId;
        const dropState: DropState = drag?.active && drag.target?.stall.id === slot.stall.id
          ? canReceiveHorse(slot, drag, horseByStallId) ? 'valid' : 'invalid'
          : groomTargetBlock != null && blockNumberOf(slot.stall.stallNumber) === groomTargetBlock
            ? 'valid'
            : null;
        return (
          <BarnStall
            key={slot.stall.id}
            stallId={slot.stall.id}
            x={x}
            z={z}
            row={slot.row}
            toneColor={toneColors[state.tone]}
            groomColor={slot.stall.groomId != null ? groomColors.get(slot.stall.groomId) ?? null : null}
            selected={slot.stall.id === selectedStallId}
            hovered={slot.stall.id === hoveredStallId}
            dimmed={(ownStallIds ? !ownStallIds.has(slot.stall.id) : false) || !inFocus}
            locked={isLocked(slot.stall.id)}
            dropState={dropState}
            colors={colors}
            onSelect={select}
            onHover={setHoveredStallId}
          />
        );
      })}

      <Suspense fallback={null}>
        {layout.slots.map((slot) => {
          const horse = horseByStallId.get(slot.stall.id);
          if (!horse) return null;
          const dragging = drag?.active && drag.horse.id === horse.id;
          const [x, z] = dragging ? drag.point : stallPosition(slot.column, slot.row, layout.columns);
          const groomId = slot.stall.groomId;
          return (
            <HorseFigure
              key={horse.id}
              x={x}
              z={z}
              y={dragging ? 0.6 : 0}
              row={slot.row}
              ringMaterial={groomId != null && !dragging ? ringMaterials.get(groomId) ?? null : null}
              dimmed={(ownStallIds ? !ownStallIds.has(slot.stall.id) : false) || (focusGroomId != null && groomId !== focusGroomId)}
              draggable={!!onMoveHorse}
              onPointerDown={onMoveHorse ? startDrag(horse, slot) : undefined}
            />
          );
        })}
      </Suspense>

      {visibleBlocks.map((block) => {
        const [x, z] = blockStandPosition(block, layout.columns);
        const dragging = block.groomId != null && groomDrag.drag?.active && groomDrag.drag.groomId === block.groomId;
        const [fx, fz] = dragging && groomDrag.drag ? groomDrag.drag.point : [x, z];
        return (
          <StaffFigure
            // Keyed by the person, so a groom moved to another block walks there instead of reappearing.
            key={block.groomId != null ? `groom-${block.groomId}` : `block-${block.blockNumber}`}
            kind="groom"
            x={fx}
            z={fz}
            follow={dragging}
            onPointerDown={props.onMoveGroom && block.groomId != null
              ? groomDrag.startDrag(block.groomId, block.blockNumber, [x, z])
              : undefined}
            color={block.groomId != null ? groomColors.get(block.groomId) ?? null : null}
            highlighted={block.groomId != null && block.groomId === highlightedGroomId}
            faded={focusGroomId != null && block.groomId !== focusGroomId}
            colors={colors}
            onHover={(active) => setHoveredGroomId(active ? block.groomId : null)}
            onClick={() => setPinnedGroomId((pinned) => (pinned === block.groomId ? null : block.groomId))}
          />
        );
      })}

      {area.type === 'REGULAR' && (
        <StaffFigure
          kind="trainer"
          x={ENTRANCE_X(layout.columns)}
          z={0}
          color={area.trainerId != null ? colors.primary : null}
          highlighted={false}
          faded={focusGroomId != null}
          colors={colors}
          onClick={props.onTrainerClick}
          onHover={props.onTrainerClick ? (active) => props.setCursor(active ? 'pointer' : '') : undefined}
        />
      )}

      <LabelProjector labels={labels} elements={props.labelElements} />

      <CameraRig
        viewMode={props.viewMode}
        focus={focusSlot ? stallPosition(focusSlot.column, focusSlot.row, layout.columns) : null}
        width={width}
        depth={depth}
        centerX={-ENTRANCE / 2}
        locked={props.locked}
      />
    </>
  );
}

class SceneErrorBoundary extends Component<{ children: ReactNode; onError: () => void }, { failed: boolean }> {
  state = { failed: false };
  static getDerivedStateFromError() { return { failed: true }; }
  componentDidCatch(error: unknown) {
    console.error('3D barn failed; falling back to the plan.', error);
    this.props.onError();
  }
  render() { return this.state.failed ? null : this.props.children; }
}

/** The 3D barn for one area. Loaded on demand; the 2D plan stays the default and the fallback. */
export default function BarnScene(props: BarnSceneProps) {
  const colors = useSceneColors();
  const [locked, setLocked] = useState(false);
  const [cursor, setCursor] = useState('');
  const [labels, setLabels] = useState<SceneLabel[]>([]);
  const labelElements = useRef(new Map<string, HTMLElement>());
  const invalidateScene = useRef<(() => void) | null>(null);
  const [hoveredGroomId, setHoveredGroomId] = useState<number | null>(null);
  const [pinnedGroomId, setPinnedGroomId] = useState<number | null>(null);
  // The key folds itself in the top view, where it would cover the front row of stalls.
  const [keyFold, setKeyFold] = useState({ viewMode: props.viewMode, collapsed: props.viewMode === 'top' });
  if (keyFold.viewMode !== props.viewMode) setKeyFold({ viewMode: props.viewMode, collapsed: props.viewMode === 'top' });
  const { stalls, personName } = props;
  // Esc leaves the focus on one groom, like clicking empty space or "Show everyone".
  useEffect(() => {
    if (pinnedGroomId == null) return;
    const leave = (event: KeyboardEvent) => { if (event.key === 'Escape') setPinnedGroomId(null); };
    window.addEventListener('keydown', leave);
    return () => window.removeEventListener('keydown', leave);
  }, [pinnedGroomId]);
  const layout = useMemo(() => layoutArea(stalls), [stalls]);
  const groomBadges = useMemo(() => groomBadgesFor(layout, colors, personName), [layout, colors, personName]);
  useEffect(() => { preloadHorseModel(); }, []);
  // New label elements are placed on the next frame; request one, since the loop only runs on demand.
  useLayoutEffect(() => { invalidateScene.current?.(); }, [labels]);

  return (
    <SceneErrorBoundary onError={props.onUnavailable}>
      <Canvas
        onCreated={(state) => { invalidateScene.current = state.invalidate; }}
        style={{ cursor }}
        aria-label={`3D view of ${props.area.name}`}
        orthographic
        frameloop="demand"
        shadows
        dpr={[1, 1.5]}
        camera={{ position: [18, 22, 24], zoom: 20, near: 0.1, far: 200 }}
        gl={{ antialias: true, powerPreference: 'high-performance' }}
        // Rendered inside <canvas> as plain HTML fallback; a failed WebGL context throws into the boundary instead.
        fallback={<span>3D view of {props.area.name}</span>}
        onPointerMissed={() => { props.onSelectStall(null); setPinnedGroomId(null); }}
      >
        <BarnContents
          {...props}
          colors={colors}
          locked={locked}
          setLocked={setLocked}
          setCursor={setCursor}
          onLabels={setLabels}
          labelElements={labelElements}
          layout={layout}
          groomBadges={groomBadges}
          hoveredGroomId={hoveredGroomId}
          setHoveredGroomId={setHoveredGroomId}
          pinnedGroomId={pinnedGroomId}
          setPinnedGroomId={setPinnedGroomId}
        />
      </Canvas>
      <LabelLayer labels={labels} elements={labelElements} />
      <GroomKey
        layout={layout}
        badges={groomBadges}
        horseByStallId={props.horseByStallId}
        currentUserId={props.currentUserId}
        focusedId={pinnedGroomId}
        onFocus={(id) => setPinnedGroomId((pinned) => (pinned === id ? null : id))}
        onHover={setHoveredGroomId}
        onClearFocus={() => { setPinnedGroomId(null); setHoveredGroomId(null); }}
        collapsed={keyFold.collapsed}
        onToggleCollapsed={() => setKeyFold((fold) => ({ ...fold, collapsed: !fold.collapsed }))}
      />
    </SceneErrorBoundary>
  );
}
