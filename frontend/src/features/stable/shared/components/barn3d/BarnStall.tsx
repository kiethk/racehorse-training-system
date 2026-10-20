'use client';

import type { ThreeEvent } from '@react-three/fiber';
import { memo } from 'react';
import { MATERIAL_COLORS, STALL_DEPTH, STALL_WIDTH, mix, sharedGeometries, type SceneColors } from './sceneResources';

export type DropState = 'valid' | 'invalid' | null;

interface BarnStallProps {
  stallId: number;
  x: number;
  z: number;
  row: 0 | 1;
  /** Resolved colour of the stall's state (occupied, blocked, reserved, empty); shown on the gate plaque. */
  toneColor: string;
  /** Identity colour of the stall's groom; paints the gate's top rail so a block's three gates match its groom. */
  groomColor: string | null;
  selected: boolean;
  hovered: boolean;
  dimmed: boolean;
  dropState: DropState;
  colors: SceneColors;
  onSelect: (stallId: number) => void;
  onHover: (stallId: number | null) => void;
}

/**
 * The parts of one stall that react to data and interaction: the straw floor (click target), the gate rail in
 * the groom's colour, the status plaque, and selection and drop feedback. Static fittings live in `BarnStructure`.
 */
function BarnStallComponent(props: BarnStallProps) {
  const { stallId, x, z, row, toneColor, groomColor, selected, hovered, dimmed, dropState, colors } = props;
  const geo = sharedGeometries();

  // Material props are updated in place by R3F: a state change recolours the stall without a new shader.
  const dropColor = dropState === 'valid' ? colors.success : dropState === 'invalid' ? colors.danger : null;
  const floorColor = dropColor ? mix(dropColor, MATERIAL_COLORS.straw, 0.4)
    : selected ? mix(colors.primary, MATERIAL_COLORS.straw, 0.55)
    : hovered ? mix(MATERIAL_COLORS.straw, colors.surface, 0.3)
    : MATERIAL_COLORS.straw;
  const outlineOpacity = selected || dropState ? 0.95 : hovered ? 0.4 : 0;
  // Gate sits on the aisle side: +z for the top row, -z for the bottom row.
  const front = row === 0 ? STALL_DEPTH / 2 : -STALL_DEPTH / 2;
  const outward = row === 0 ? 1 : -1;

  const select = (event: ThreeEvent<MouseEvent>) => {
    event.stopPropagation();
    props.onSelect(stallId);
  };

  return (
    <group position={[x, 0, z]}>
      <mesh
        geometry={geo.stallFloor}
        position={[0, 0.04, 0]}
        receiveShadow
        onClick={select}
        onPointerOver={(event) => { event.stopPropagation(); props.onHover(stallId); }}
        onPointerOut={() => props.onHover(null)}
      >
        <meshStandardMaterial color={floorColor} roughness={1} transparent opacity={dimmed ? 0.55 : 1} />
      </mesh>
      {outlineOpacity > 0 && (
        <>
          <mesh geometry={geo.stallOutline} position={[0, 0.095, 0]} raycast={() => null}>
            <meshBasicMaterial color={dropColor ?? colors.primary} transparent opacity={outlineOpacity} />
          </mesh>
          <mesh geometry={geo.bedding} position={[0, 0.115, 0]} raycast={() => null}>
            <meshStandardMaterial color={floorColor} roughness={1} />
          </mesh>
        </>
      )}
      <mesh geometry={geo.gateRail} position={[-STALL_WIDTH / 4, 0.9, front]} castShadow raycast={() => null}>
        <meshStandardMaterial color={groomColor ?? MATERIAL_COLORS.woodDark} roughness={0.6} />
      </mesh>
      <mesh geometry={geo.plaque} position={[-STALL_WIDTH / 4, 0.55, front + outward * 0.055]} raycast={() => null}>
        <meshBasicMaterial color={toneColor} />
      </mesh>
    </group>
  );
}

export const BarnStall = memo(BarnStallComponent);
