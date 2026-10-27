'use client';

import { useThree, type ThreeEvent } from '@react-three/fiber';
import { useCallback, useEffect, useRef, useState } from 'react';
import * as THREE from 'three';
import { blockNumberOf, type AreaLayout, type StallSlot } from '../../lib/stallLayout';
import { slotAt } from './useHorseDrag';

export interface GroomDragState {
  groomId: number;
  fromBlock: number;
  /** Ground point under the pointer, in world units. */
  point: [number, number];
  /** False until the pointer has moved past the threshold; until then it is still a click. */
  active: boolean;
  /** Stall under the pointer; its block is where the groom would go. */
  target: StallSlot | null;
}

const DRAG_THRESHOLD_PX = 4;
const GROUND = new THREE.Plane(new THREE.Vector3(0, 1, 0), 0);

/** The block a dragged groom would be dropped on, or null when it is not a different block. */
export function groomDropBlock(drag: GroomDragState | null): number | null {
  if (!drag?.active || !drag.target) return null;
  const block = blockNumberOf(drag.target.stall.stallNumber);
  return block === drag.fromBlock ? null : block;
}

/**
 * Drag a groom figure onto another block of the barn (Manager staffing). Same mechanics as dragging a horse:
 * pointer events are followed on the window and projected onto the ground; orbiting pauses meanwhile.
 */
export function useGroomDrag({ layout, onMoveGroom, setLocked }: {
  layout: AreaLayout;
  onMoveGroom?: (groomId: number, toBlockNumber: number) => void;
  setLocked: (locked: boolean) => void;
}) {
  const { camera, gl, controls, invalidate } = useThree();
  const [drag, setDrag] = useState<GroomDragState | null>(null);
  const suppressClick = useRef(false);
  const cancelActive = useRef<(() => void) | null>(null);

  useEffect(() => () => cancelActive.current?.(), []);

  const consumeClick = useCallback(() => {
    const suppressed = suppressClick.current;
    suppressClick.current = false;
    return suppressed;
  }, []);

  const startDrag = useCallback((groomId: number, fromBlock: number, origin: [number, number]) =>
    (event: ThreeEvent<PointerEvent>) => {
      if (event.button !== 0 || !onMoveGroom) return;
      event.stopPropagation();
      const orbit = controls as unknown as { enabled: boolean } | null;
      if (orbit) orbit.enabled = false;
      setLocked(true);
      const start = { x: event.clientX, y: event.clientY };
      const raycaster = new THREE.Raycaster();
      const hit = new THREE.Vector3();
      let current: GroomDragState = { groomId, fromBlock, point: origin, active: false, target: null };

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
          suppressClick.current = true;
          window.setTimeout(() => { suppressClick.current = false; }, 0);
        }
        const block = groomDropBlock(current);
        if (commit && block != null) onMoveGroom(current.groomId, block);
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
    }, [camera, controls, gl, invalidate, layout, onMoveGroom, setLocked]);

  return { drag, startDrag, consumeClick };
}
