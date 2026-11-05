'use client';

import { useFrame, useThree, type ThreeEvent } from '@react-three/fiber';
import { memo, useEffect, useLayoutEffect, useRef } from 'react';
import type * as THREE from 'three';
import { mix, sharedGeometries, type SceneColors } from './sceneResources';

interface StaffFigureProps {
  kind: 'groom' | 'trainer';
  x: number;
  z: number;
  /** Identity colour; null draws a faint "nobody assigned" placeholder. */
  color: string | null;
  highlighted: boolean;
  faded: boolean;
  colors: SceneColors;
  onHover?: (active: boolean) => void;
  onClick?: () => void;
  /** Starts dragging this person (Manager staffing). */
  onPointerDown?: (event: ThreeEvent<PointerEvent>) => void;
  /** Follows the pointer exactly while dragged instead of walking. */
  follow?: boolean;
}

/** World units per second; a block-to-block walk takes a second or two. */
const WALK_SPEED = 7;

function prefersReducedMotion() {
  return typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

/**
 * A low-poly person built from primitives (no asset): grooms wear a cap, trainers a long coat.
 * They stand in the aisle in front of the stalls they are responsible for. The name tag lives in `SceneLabels`.
 * When their spot changes (e.g. a groom is moved to another block) they walk there instead of jumping.
 */
function StaffFigureComponent({ kind, x, z, color, highlighted, faded, colors, onHover, onClick, onPointerDown, follow }: StaffFigureProps) {
  const geo = sharedGeometries();
  const opacity = color == null ? 0.35 : faded ? 0.4 : 1;
  const cloth = <meshStandardMaterial color={color ?? colors.borderStrong} roughness={0.7} transparent opacity={opacity} />;
  const group = useRef<THREE.Group>(null);
  const invalidate = useThree((state) => state.invalidate);

  // Place the figure on its first frame; afterwards position changes are animated in useFrame.
  useLayoutEffect(() => {
    group.current?.position.set(x, 0, z);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  // The loop only runs on demand: ask for a frame whenever the destination changes.
  useEffect(() => { invalidate(); }, [x, z, follow, invalidate]);

  useFrame((_, delta) => {
    const figure = group.current;
    if (!figure) return;
    const dx = x - figure.position.x;
    const dz = z - figure.position.z;
    const distance = Math.hypot(dx, dz);
    if (distance < 0.01) {
      if (figure.position.y !== 0 || figure.rotation.y !== 0) {
        figure.position.set(x, 0, z);
        figure.rotation.y = 0;
        invalidate();
      }
      return;
    }
    if (follow || prefersReducedMotion()) {
      figure.position.set(x, follow ? 0.4 : 0, z);
    } else {
      const step = Math.min(distance, WALK_SPEED * delta);
      figure.position.x += (dx / distance) * step;
      figure.position.z += (dz / distance) * step;
      // A small step bounce and facing the way they walk.
      figure.position.y = Math.abs(Math.sin(performance.now() / 110)) * 0.12;
      figure.rotation.y = Math.atan2(dx, dz);
    }
    invalidate();
  });

  const hover = (active: boolean) => (event: ThreeEvent<PointerEvent>) => {
    event.stopPropagation();
    onHover?.(active);
  };

  return (
    <group ref={group} scale={highlighted || follow ? 1.12 : 1}>
      <mesh geometry={geo.disc} position={[0, 0.02, 0]} rotation={[-Math.PI / 2, 0, 0]} raycast={() => null}>
        <meshBasicMaterial color={color ?? colors.borderStrong} transparent opacity={highlighted ? 0.55 : 0.2} />
      </mesh>
      <group
        onPointerOver={hover(true)}
        onPointerOut={hover(false)}
        onPointerDown={onPointerDown}
        onClick={(event) => { event.stopPropagation(); onClick?.(); }}
      >
        <mesh geometry={geo.legs} position={[0, 0.42, 0]} castShadow>{cloth}</mesh>
        <mesh geometry={kind === 'trainer' ? geo.coat : geo.body} position={[0, kind === 'trainer' ? 1.1 : 1.02, 0]} castShadow>
          {cloth}
        </mesh>
        <mesh geometry={geo.head} position={[0, 1.66, 0]} castShadow>
          <meshStandardMaterial color={mix(colors.borderStrong, colors.surface, 0.25)} roughness={0.8} transparent opacity={opacity} />
        </mesh>
        {kind === 'groom' && (
          <>
            <mesh geometry={geo.cap} position={[0, 1.82, 0]}>{cloth}</mesh>
            <mesh geometry={geo.brim} position={[0.06, 1.79, 0]}>{cloth}</mesh>
          </>
        )}
      </group>
    </group>
  );
}

export const StaffFigure = memo(StaffFigureComponent);
