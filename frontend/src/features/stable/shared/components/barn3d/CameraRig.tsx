'use client';

import { OrbitControls } from '@react-three/drei';
import { useFrame, useThree } from '@react-three/fiber';
import { useEffect, useMemo, useRef } from 'react';
import * as THREE from 'three';
import type { OrbitControls as OrbitControlsImpl } from 'three-stdlib';

export type BarnViewMode = 'isometric' | 'top';

interface CameraRigProps {
  viewMode: BarnViewMode;
  /** World point to frame; null frames the whole barn. */
  focus: [number, number] | null;
  /** Barn footprint, used to fit the whole area in view. */
  width: number;
  depth: number;
  /** Shift of the barn centre (the entrance makes it asymmetric). */
  centerX: number;
  /** Disables orbiting while something is being dragged. */
  locked: boolean;
}

const OFFSET: Record<BarnViewMode, THREE.Vector3> = {
  isometric: new THREE.Vector3(18, 22, 24),
  top: new THREE.Vector3(0, 32, 0.02),
};

function prefersReducedMotion() {
  return typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

/** Orthographic camera with orbit limits; eases to the selected stall and back. Renders only while moving. */
export function CameraRig({ viewMode, focus, width, depth, centerX, locked }: CameraRigProps) {
  const controls = useRef<OrbitControlsImpl | null>(null);
  const size = useThree((state) => state.size);
  const invalidate = useThree((state) => state.invalidate);
  const transition = useRef<{ position: THREE.Vector3; target: THREE.Vector3; zoom: number; instant: boolean } | null>(null);
  const [focusX, focusZ] = focus ?? [centerX, 0];
  const focused = focus != null;

  const fitZoom = useMemo(() => {
    // On-screen footprint of the barn: straight for the top view; rotated (~37°) and foreshortened for isometric.
    const top = viewMode === 'top';
    const footprintWidth = top ? width : width * 0.8 + depth * 0.6;
    const footprintHeight = top ? depth : (width * 0.6 + depth * 0.8) * 0.62;
    const zoom = Math.min(size.width / (footprintWidth * 1.08), size.height / (footprintHeight * 1.2));
    return THREE.MathUtils.clamp(zoom, 6, 80);
  }, [size.width, size.height, width, depth, viewMode]);

  useEffect(() => {
    const target = new THREE.Vector3(focusX, 0, focusZ);
    transition.current = {
      position: OFFSET[viewMode].clone().add(target),
      target,
      zoom: fitZoom * (focused ? 1.35 : 1),
      instant: prefersReducedMotion(),
    };
    invalidate();
  }, [focusX, focusZ, focused, viewMode, fitZoom, invalidate]);

  // The camera is moved here, outside React, as R3F intends; frames are requested only while it moves.
  useFrame(({ camera }, delta) => {
    const next = transition.current;
    const control = controls.current;
    if (!next || !control || !(camera instanceof THREE.OrthographicCamera)) return;
    const amount = next.instant ? 1 : 1 - Math.exp(-7 * delta);
    camera.position.lerp(next.position, amount);
    control.target.lerp(next.target, amount);
    camera.zoom = THREE.MathUtils.lerp(camera.zoom, next.zoom, amount);
    camera.updateProjectionMatrix();
    control.update();
    const settled = camera.position.distanceTo(next.position) < 0.02
      && control.target.distanceTo(next.target) < 0.02
      && Math.abs(camera.zoom - next.zoom) < 0.05;
    if (settled) {
      camera.position.copy(next.position);
      control.target.copy(next.target);
      camera.zoom = next.zoom;
      camera.updateProjectionMatrix();
      transition.current = null;
    }
    invalidate();
  });

  // Keep panning on the barn: the orbit target may not leave its footprint. Camera and target move by the
  // same amount, so the viewing angle does not change.
  const clampPan = () => {
    const control = controls.current;
    if (!control) return;
    const { target, object } = control;
    const x = THREE.MathUtils.clamp(target.x, centerX - width / 2, centerX + width / 2);
    const z = THREE.MathUtils.clamp(target.z, -depth / 2, depth / 2);
    if (x === target.x && z === target.z) return;
    const shift = new THREE.Vector3(x - target.x, 0, z - target.z);
    target.add(shift);
    object.position.add(shift);
  };

  const polar = viewMode === 'top' ? [0.02, 0.2] : [0.55, 1.05];

  return (
    <OrbitControls
      ref={controls}
      makeDefault
      enabled={!locked}
      enableDamping
      dampingFactor={0.1}
      // Relative to the zoom that fits the whole barn: close enough to read a stall, never inside the walls,
      // never so far out that the barn becomes a speck. Focusing a stall uses 1.35×, inside this range.
      minZoom={fitZoom * 0.7}
      maxZoom={fitZoom * 2.2}
      onChange={clampPan}
      minPolarAngle={polar[0]}
      maxPolarAngle={polar[1]}
      minAzimuthAngle={-0.95}
      maxAzimuthAngle={0.95}
      screenSpacePanning
      onStart={() => { transition.current = null; }}
    />
  );
}
