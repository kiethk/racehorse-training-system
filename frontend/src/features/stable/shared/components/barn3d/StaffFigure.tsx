'use client';

import type { ThreeEvent } from '@react-three/fiber';
import { memo } from 'react';
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
}

/**
 * A low-poly person built from primitives (no asset): grooms wear a cap, trainers a long coat.
 * They stand in the aisle in front of the stalls they are responsible for. The name tag lives in `SceneLabels`.
 */
function StaffFigureComponent({ kind, x, z, color, highlighted, faded, colors, onHover, onClick }: StaffFigureProps) {
  const geo = sharedGeometries();
  const opacity = color == null ? 0.35 : faded ? 0.4 : 1;
  const cloth = <meshStandardMaterial color={color ?? colors.borderStrong} roughness={0.7} transparent opacity={opacity} />;

  const hover = (active: boolean) => (event: ThreeEvent<PointerEvent>) => {
    event.stopPropagation();
    onHover?.(active);
  };

  return (
    <group position={[x, 0, z]} scale={highlighted ? 1.12 : 1}>
      <mesh geometry={geo.disc} position={[0, 0.02, 0]} rotation={[-Math.PI / 2, 0, 0]} raycast={() => null}>
        <meshBasicMaterial color={color ?? colors.borderStrong} transparent opacity={highlighted ? 0.55 : 0.2} />
      </mesh>
      <group
        onPointerOver={hover(true)}
        onPointerOut={hover(false)}
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
