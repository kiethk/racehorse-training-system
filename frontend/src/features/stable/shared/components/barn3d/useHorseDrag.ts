'use client';

import { useThree, type ThreeEvent } from '@react-three/fiber';
import { useCallback, useEffect, useRef, useState } from 'react';
import * as THREE from 'three';
import type { Horse, StableStall } from '../../../types';
import type { AreaLayout, StallSlot } from '../../lib/stallLayout';
import { STALL_DEPTH, STALL_WIDTH, stallPosition } from './sceneResources';

export interface DragState {
  horse: Horse;
  from: StallSlot;
  /** Ground point under the pointer, in world units. */
  point: [number, number];
  /** False until the pointer has moved past the threshold; until then it is still a click. */
  active: boolean;
  target: StallSlot | null;
}

const DRAG_THRESHOLD_PX = 4;
const GROUND = new THREE.Plane(new THREE.Vector3(0, 1, 0), 0);

export function slotAt(layout: AreaLayout, [x, z]: [number, number]) {
  return layout.slots.find((slot) => {
    const [sx, sz] = stallPosition(slot.column, slot.row, layout.columns);
    return Math.abs(x - sx) <= STALL_WIDTH / 2 && Math.abs(z - sz) <= STALL_DEPTH / 2;
  }) ?? null;
}

/** A horse may only be dropped on another stall of this area that is AVAILABLE and empty. */
export function canReceiveHorse(slot: StallSlot | null, drag: DragState, horseByStallId: ReadonlyMap<number, Horse>) {
  return !!slot && slot.stall.id !== drag.from.stall.id && slot.stall.status === 'AVAILABLE' && !horseByStallId.has(slot.stall.id);
}

/**
 * Drag a horse across the barn floor: pointer events are followed on the window and projected onto the ground,
 * so the drag keeps working when the pointer leaves the horse. Orbiting is paused while dragging.
 */
export function useHorseDrag({ layout, horseByStallId, onMoveHorse, setLocked }: {
  layout: AreaLayout;
  horseByStallId: ReadonlyMap<number, Horse>;
  onMoveHorse?: (horse: Horse, to: StableStall) => void;
  setLocked: (locked: boolean) => void;
}) {
  const { camera, gl, controls, invalidate } = useThree();
  const [drag, setDrag] = useState<DragState | null>(null);
  const suppressClick = useRef(false);
  const cancelActive = useRef<(() => void) | null>(null);

  // Leaving the 3D view mid-drag must not leave window listeners or a locked camera behind.
  useEffect(() => () => cancelActive.current?.(), []);

  /** True when a click is just the tail of a drag and should be ignored. */
  const consumeClick = useCallback(() => {
    const suppressed = suppressClick.current;
    suppressClick.current = false;
    return suppressed;
  }, []);

  const startDrag = useCallback((horse: Horse, from: StallSlot) => (event: ThreeEvent<PointerEvent>) => {
    if (event.button !== 0) return;
    event.stopPropagation();
    const orbit = controls as unknown as { enabled: boolean } | null;
    if (orbit) orbit.enabled = false;
    setLocked(true);
    const start = { x: event.clientX, y: event.clientY };
    const raycaster = new THREE.Raycaster();
    const hit = new THREE.Vector3();
    let current: DragState = { horse, from, point: stallPosition(from.column, from.row, layout.columns), active: false, target: null };

    const toGround = (clientX: number, clientY: number): [number, number] | null => {
      const rect = gl.domElement.getBoundingClientRect();
      const ndc = new THREE.Vector2(((clientX - rect.left) / rect.width) * 2 - 1, -((clientY - rect.top) / rect.height) * 2 + 1);
      raycaster.setFromCamera(ndc, camera);
      return raycaster.ray.intersectPlane(GROUND, hit) ? [hit.x, hit.z] : null;
    };
    const finish = (commit: boolean) => {
      window.removeEventListener('pointermove', move);
      window.removeEventListener('pointerup', up);
      window.removeEventListener('keydown', key);
      cancelActive.current = null;
      if (orbit) orbit.enabled = true;
      setLocked(false);
      if (current.active) {
        // Swallow only the click that this pointerup may produce, never a later, real one.
        suppressClick.current = true;
        window.setTimeout(() => { suppressClick.current = false; }, 0);
      }
      if (commit && current.active && current.target && canReceiveHorse(current.target, current, horseByStallId)) {
        onMoveHorse?.(current.horse, current.target.stall);
      }
      setDrag(null);
      invalidate();
    };
    const move = (moveEvent: PointerEvent) => {
      const moved = Math.hypot(moveEvent.clientX - start.x, moveEvent.clientY - start.y) > DRAG_THRESHOLD_PX;
      const point = toGround(moveEvent.clientX, moveEvent.clientY);
      if (!point || (!current.active && !moved)) return;
      current = { ...current, active: true, point, target: slotAt(layout, point) };
      setDrag(current);
      invalidate();
    };
    const up = () => finish(true);
    const key = (keyEvent: KeyboardEvent) => { if (keyEvent.key === 'Escape') finish(false); };
    window.addEventListener('pointermove', move);
    window.addEventListener('pointerup', up);
    window.addEventListener('keydown', key);
    cancelActive.current = () => finish(false);
    setDrag(current);
  }, [camera, controls, gl, horseByStallId, invalidate, layout, onMoveHorse, setLocked]);

  return { drag, startDrag, consumeClick };
}
