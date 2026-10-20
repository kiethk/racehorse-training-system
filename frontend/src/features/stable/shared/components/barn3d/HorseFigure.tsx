'use client';

import { useGLTF } from '@react-three/drei';
import type { ThreeEvent } from '@react-three/fiber';
import { memo, useMemo } from 'react';
import * as THREE from 'three';
import { clone as cloneSkinned } from 'three/examples/jsm/utils/SkeletonUtils.js';
import { HORSE_MODEL_URL, sharedGeometries } from './sceneResources';

/** The model ships with a detached tack mesh; it is hidden once on the source scene. */
const DETACHED_MESH = 'Object_21';
const prepared = new WeakSet<THREE.Object3D>();

function prepare(scene: THREE.Object3D) {
  if (prepared.has(scene)) return scene;
  scene.traverse((object) => {
    if (object.name === DETACHED_MESH) object.visible = false;
    if (object instanceof THREE.Mesh) object.castShadow = true;
  });
  prepared.add(scene);
  return scene;
}

interface HorseFigureProps {
  x: number;
  z: number;
  /** Lift while the horse is being dragged. */
  y?: number;
  /** 0 for the top row (head towards the aisle at +z), 1 for the bottom row. */
  row: 0 | 1;
  /** Groom identity colour; drawn as a ring on the floor around the horse. */
  ringMaterial: THREE.Material | null;
  dimmed: boolean;
  draggable: boolean;
  onPointerDown?: (event: ThreeEvent<PointerEvent>) => void;
}

/**
 * One horse. Every horse clones the same loaded model (a rigged mesh needs its own skeleton) but shares
 * its geometry and materials, so twelve horses cost twelve skeletons, not twelve models.
 */
function HorseFigureComponent({ x, z, y = 0, row, ringMaterial, dimmed, draggable, onPointerDown }: HorseFigureProps) {
  const { scene } = useGLTF(HORSE_MODEL_URL);
  const horse = useMemo(() => cloneSkinned(prepare(scene)), [scene]);
  const geo = sharedGeometries();

  return (
    <group position={[x, y, z]}>
      {ringMaterial && (
        <mesh geometry={geo.ring} material={ringMaterial} position={[0, 0.1 - y, 0]} rotation={[-Math.PI / 2, 0, 0]}
          raycast={() => null} />
      )}
      <group
        position={[0, 0.1, 0]}
        rotation={[0, row === 0 ? 0 : Math.PI, 0]}
        scale={dimmed ? 0.7 : 0.75}
        onPointerDown={draggable ? onPointerDown : undefined}
        onPointerOver={draggable ? (event) => { event.stopPropagation(); } : undefined}
      >
        <primitive object={horse} position={[0, 0, -0.95]} />
      </group>
    </group>
  );
}

export const HorseFigure = memo(HorseFigureComponent);

export function preloadHorseModel() {
  useGLTF.preload(HORSE_MODEL_URL);
}
