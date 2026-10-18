'use client';

import { useLayoutEffect, useMemo, useRef } from 'react';
import * as THREE from 'three';
import {
  AISLE_WIDTH,
  ENTRANCE,
  MATERIAL_COLORS as M,
  ROW_Z,
  STALL_DEPTH,
  STALL_PITCH,
  STALL_WIDTH,
  barnSize,
  sharedGeometries,
} from './sceneResources';

/** Position plus an optional turn around the vertical axis. */
type Transform = [x: number, y: number, z: number, rotationY?: number];

/** Many copies of one mesh in a single draw call. Transforms only change when the area changes. */
function InstancedParts({ geometry, material, transforms, castShadow = false }: {
  geometry: THREE.BufferGeometry;
  material: THREE.Material;
  transforms: Transform[];
  castShadow?: boolean;
}) {
  const ref = useRef<THREE.InstancedMesh>(null);
  useLayoutEffect(() => {
    const mesh = ref.current;
    if (!mesh) return;
    const object = new THREE.Object3D();
    transforms.forEach(([x, y, z, rotationY = 0], index) => {
      object.position.set(x, y, z);
      object.rotation.set(0, rotationY, 0);
      object.updateMatrix();
      mesh.setMatrixAt(index, object.matrix);
    });
    mesh.instanceMatrix.needsUpdate = true;
    mesh.computeBoundingSphere();
  }, [transforms]);
  if (transforms.length === 0) return null;
  return (
    <instancedMesh
      ref={ref}
      args={[geometry, material, transforms.length]}
      castShadow={castShadow}
      receiveShadow
      // Structure is decoration; clicks belong to the stall floors.
      raycast={() => null}
    />
  );
}

/** Deterministic pseudo-random numbers, so the straw lies the same way on every render and every visit. */
function seeded(seed: number) {
  let value = seed;
  return () => {
    value = (value * 16807) % 2147483647;
    return (value - 1) / 2147483646;
  };
}

/** Front (aisle side) edge of a stall row. */
export function aisleEdgeZ(row: 0 | 1) {
  return row === 0 ? -ROW_Z + STALL_DEPTH / 2 : ROW_Z - STALL_DEPTH / 2;
}

/**
 * Everything that does not change with the data: ground, slab, aisle, partitions with bars, gates, bedding,
 * feeders, hay and lamps. Rebuilt only when the number of stalls changes.
 */
export function BarnStructure({ columns }: { columns: number }) {
  const geo = sharedGeometries();
  const { width, depth } = barnSize(columns);
  const length = Math.max(columns, 1) * STALL_PITCH;
  const entranceX = -length / 2 - ENTRANCE / 2;

  const materials = useMemo(() => ({
    ground: new THREE.MeshStandardMaterial({ color: M.ground, roughness: 1 }),
    slab: new THREE.MeshStandardMaterial({ color: M.slab, roughness: 1 }),
    aisle: new THREE.MeshStandardMaterial({ color: M.aisle, roughness: 0.95 }),
    joint: new THREE.MeshStandardMaterial({ color: M.aisleJoint, roughness: 1 }),
    woodDark: new THREE.MeshStandardMaterial({ color: M.woodDark, roughness: 0.8 }),
    wood: new THREE.MeshStandardMaterial({ color: M.wood, roughness: 0.75 }),
    metal: new THREE.MeshStandardMaterial({ color: M.metal, roughness: 0.4, metalness: 0.7 }),
    straw: new THREE.MeshStandardMaterial({ color: M.strawDark, roughness: 1 }),
    hay: new THREE.MeshStandardMaterial({ color: M.hay, roughness: 1 }),
    water: new THREE.MeshStandardMaterial({ color: M.water, roughness: 0.3, metalness: 0.3 }),
    trunk: new THREE.MeshStandardMaterial({ color: M.trunk, roughness: 1 }),
    foliage: new THREE.MeshStandardMaterial({ color: M.foliage, roughness: 1, flatShading: true }),
    lamp: new THREE.MeshBasicMaterial({ color: M.lamp }),
  }), []);
  useLayoutEffect(() => () => Object.values(materials).forEach((material) => material.dispose()), [materials]);

  const parts = useMemo(() => {
    const t = {
      panels: [] as Transform[], bars: [] as Transform[], rails: [] as Transform[], posts: [] as Transform[],
      gates: [] as Transform[], gateBars: [] as Transform[], feeders: [] as Transform[], waters: [] as Transform[],
      straw: [] as Transform[], joints: [] as Transform[], bales: [] as Transform[], lampPosts: [] as Transform[],
      lamps: [] as Transform[], trunks: [] as Transform[], crowns: [] as Transform[],
    };
    const random = seeded(columns * 97 + 13);
    for (const row of [0, 1] as const) {
      const z = row === 0 ? -ROW_Z : ROW_Z;
      const front = aisleEdgeZ(row);
      const back = z - (front - z);
      const inward = row === 0 ? -1 : 1; // From the aisle towards the back wall.
      for (let i = 0; i <= columns; i += 1) {
        const x = -length / 2 + i * STALL_PITCH;
        t.panels.push([x, 0.5, z]);
        t.rails.push([x, 1.5, z]);
        t.posts.push([x, 0.8, front]);
        for (let b = 0; b < 9; b += 1) t.bars.push([x, 1.2, z - STALL_DEPTH / 2 + 0.25 + b * ((STALL_DEPTH - 0.5) / 8)]);
      }
      for (let i = 0; i < columns; i += 1) {
        const x = -length / 2 + (i + 0.5) * STALL_PITCH;
        // Half-height gate on the aisle side, left half closed so the horse stays visible.
        t.gates.push([x - STALL_WIDTH / 4, 0.48, front]);
        for (let b = 0; b < 4; b += 1) t.gateBars.push([x - STALL_WIDTH / 2 + 0.28 + b * 0.45, 1.1, front]);
        // Against the back wall, inside the stall: step from the back edge towards the aisle.
        t.feeders.push([x - 0.85, 0.26, back - inward * 0.35]);
        t.waters.push([x + 0.85, 0.2, back - inward * 0.35]);
        for (let s = 0; s < 16; s += 1) {
          t.straw.push([x + (random() - 0.5) * (STALL_WIDTH - 0.6), 0.1, z + (random() - 0.5) * (STALL_DEPTH - 0.8), random() * Math.PI]);
        }
      }
    }
    for (let x = -length / 2 + 0.5; x < length / 2; x += 1) t.joints.push([x, 0.012, 0]);
    // Entrance: hay stacked by the door, lamps at both ends of the aisle.
    t.bales.push([entranceX - 0.4, 0.25, -AISLE_WIDTH / 2 - 0.9], [entranceX - 0.4, 0.25, -AISLE_WIDTH / 2 - 1.55],
      [entranceX - 0.4, 0.75, -AISLE_WIDTH / 2 - 1.2, 0.08], [entranceX + 0.2, 0.25, AISLE_WIDTH / 2 + 1.3, Math.PI / 2]);
    for (const x of [entranceX - ENTRANCE / 2 + 0.3, length / 2 + 0.3]) {
      for (const side of [-1, 1]) {
        t.lampPosts.push([x, 1.3, side * (AISLE_WIDTH / 2 + 0.2)]);
        t.lamps.push([x, 2.65, side * (AISLE_WIDTH / 2 + 0.2)]);
      }
    }
    // A few trees behind the barn and at its ends; none in front, where they would hide the stalls.
    const treeRandom = seeded(7);
    for (let i = 0; i < 9; i += 1) {
      const x = -width / 2 - 2 + i * ((width + 4) / 8) + (treeRandom() - 0.5) * 1.5;
      const z = -depth / 2 - 2.2 - treeRandom() * 2.5;
      t.trunks.push([x, 0.45, z]);
      t.crowns.push([x, 1.9, z, treeRandom() * Math.PI]);
    }
    for (const x of [-width / 2 - ENTRANCE / 2 - 3.5, width / 2 + 1.5]) {
      t.trunks.push([x, 0.45, 2]);
      t.crowns.push([x, 1.9, 2, 1]);
    }
    return t;
  }, [columns, length, entranceX, width, depth]);

  return (
    <group>
      <mesh position={[-ENTRANCE / 2, -0.14, 0]} rotation={[-Math.PI / 2, 0, 0]} receiveShadow raycast={() => null}>
        <planeGeometry args={[width + 40, depth + 40]} />
        <primitive object={materials.ground} attach="material" />
      </mesh>
      <mesh position={[-ENTRANCE / 2, -0.06, 0]} receiveShadow raycast={() => null}>
        <boxGeometry args={[width, 0.12, depth]} />
        <primitive object={materials.slab} attach="material" />
      </mesh>
      <mesh position={[-ENTRANCE / 2, 0.005, 0]} rotation={[-Math.PI / 2, 0, 0]} receiveShadow raycast={() => null}>
        <planeGeometry args={[width - 0.6, AISLE_WIDTH]} />
        <primitive object={materials.aisle} attach="material" />
      </mesh>
      {[-1, 1].map((side) => (
        <mesh key={side} position={[0, 0.6, side * (ROW_Z + STALL_DEPTH / 2 + 0.06)]} castShadow receiveShadow raycast={() => null}>
          <boxGeometry args={[length + 0.16, 1.2, 0.14]} />
          <primitive object={materials.woodDark} attach="material" />
        </mesh>
      ))}
      <InstancedParts geometry={geo.aisleJoint} material={materials.joint} transforms={parts.joints} />
      <InstancedParts geometry={geo.partitionPanel} material={materials.wood} transforms={parts.panels} castShadow />
      <InstancedParts geometry={geo.partitionBar} material={materials.metal} transforms={parts.bars} />
      <InstancedParts geometry={geo.topRail} material={materials.woodDark} transforms={parts.rails} />
      <InstancedParts geometry={geo.post} material={materials.woodDark} transforms={parts.posts} castShadow />
      <InstancedParts geometry={geo.gatePanel} material={materials.wood} transforms={parts.gates} castShadow />
      <InstancedParts geometry={geo.gateBar} material={materials.metal} transforms={parts.gateBars} />
      <InstancedParts geometry={geo.straw} material={materials.straw} transforms={parts.straw} />
      <InstancedParts geometry={geo.feeder} material={materials.woodDark} transforms={parts.feeders} />
      <InstancedParts geometry={geo.water} material={materials.water} transforms={parts.waters} />
      <InstancedParts geometry={geo.hayBale} material={materials.hay} transforms={parts.bales} castShadow />
      <InstancedParts geometry={geo.lampPost} material={materials.woodDark} transforms={parts.lampPosts} castShadow />
      <InstancedParts geometry={geo.lamp} material={materials.lamp} transforms={parts.lamps} />
      <InstancedParts geometry={geo.trunk} material={materials.trunk} transforms={parts.trunks} castShadow />
      <InstancedParts geometry={geo.crown} material={materials.foliage} transforms={parts.crowns} castShadow />
    </group>
  );
}
