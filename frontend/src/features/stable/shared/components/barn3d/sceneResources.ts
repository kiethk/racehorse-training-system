import { useMemo } from 'react';
import * as THREE from 'three';
import type { AreaLayout } from '../../lib/stallLayout';

/** Barn dimensions in world units (roughly metres). */
export const STALL_WIDTH = 3.75;
export const STALL_DEPTH = 4.25;
export const STALL_PITCH = 4.25;
/** Distance from the aisle centre line to the middle of each stall row. */
export const ROW_Z = 3.92;
export const AISLE_WIDTH = 3.55;
/** Extra floor at the aisle entrance (left end) where the area's trainer stands. */
export const ENTRANCE = 3;
export const HORSE_MODEL_URL = '/models/low-poly-horse.glb';

export function stallPosition(column: number, row: 0 | 1, columns: number): [number, number] {
  return [(column - (columns - 1) / 2) * STALL_PITCH, row === 0 ? -ROW_Z : ROW_Z];
}

/** Aisle spot in front of the middle of a block, close to its own row so the two rows' grooms never stand together. */
export function blockStandPosition(block: AreaLayout['blocks'][number], columns: number): [number, number] {
  const [x] = stallPosition((block.firstColumn + block.lastColumn) / 2, block.row, columns);
  return [x, block.row === 0 ? -1.15 : 1.15];
}

export function barnSize(columns: number) {
  return { width: Math.max(columns, 1) * STALL_PITCH + ENTRANCE + 1.2, depth: ROW_Z * 2 + STALL_DEPTH + 1.2 };
}

export const ENTRANCE_X = (columns: number) => -(Math.max(columns, 1) * STALL_PITCH) / 2 - ENTRANCE / 2;

/**
 * Colours for the scene's UI-like parts (status, selection, people), read from the design tokens in
 * globals.css. WebGL cannot read CSS variables, so the values are resolved once when the scene mounts.
 */
const TOKENS = {
  background: '--color-surface-subtle',
  surface: '--color-surface',
  surfaceMuted: '--color-surface-muted',
  border: '--color-border',
  borderStrong: '--color-border-strong',
  textSecondary: '--color-text-secondary',
  textMuted: '--color-text-muted',
  primary: '--color-primary',
  success: '--color-success',
  danger: '--color-danger',
  info: '--color-info',
} as const;

/** Module accents give each groom an identity colour. Order matters: neighbours get the earliest, most distinct ones. */
export const STAFF_TOKENS = [
  '--color-training',
  '--color-racing',
  '--color-finance',
  '--color-medical',
  '--color-grooming',
  '--color-isolated',
  '--color-info',
] as const;

/**
 * Physical materials of the barn. These describe real-world surfaces (wood, straw, concrete), not UI, so they
 * are the one place in the stable feature with literal colours; everything that carries meaning uses tokens.
 */
export const MATERIAL_COLORS = {
  ground: '#cfd8c4',
  slab: '#c9c3b8',
  aisle: '#b9b3a8',
  aisleJoint: '#a59f94',
  woodDark: '#7a5236',
  wood: '#9a6a45',
  woodLight: '#b98a5e',
  metal: '#6e7378',
  straw: '#d9bf86',
  strawDark: '#c2a061',
  hay: '#c9b06a',
  water: '#7fa6b8',
  foliage: '#7f9c6b',
  trunk: '#6b4f3a',
  lamp: '#fff3c4',
} as const;

export type SceneColors = Record<keyof typeof TOKENS, string> & { staff: string[] };

export function readToken(name: string, fallback = '#888888') {
  if (typeof document === 'undefined') return fallback;
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback;
}

export function useSceneColors(): SceneColors {
  return useMemo(() => {
    const entries = Object.entries(TOKENS).map(([key, token]) => [key, readToken(token)]);
    return { ...Object.fromEntries(entries), staff: STAFF_TOKENS.map((token) => readToken(token)) } as SceneColors;
  }, []);
}

/** `amount` of the way from `color` towards `toward`; used for soft tints. */
export function mix(color: string, toward: string, amount: number) {
  return `#${new THREE.Color(color).lerp(new THREE.Color(toward), amount).getHexString()}`;
}

/** Initials for a name tag: "Nguyen Van An" → "NA". */
export function initials(name: string) {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '?';
  return (parts.length === 1 ? parts[0].slice(0, 2) : parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

/**
 * Geometry shared by every stall and figure. Created once per page load and intentionally never disposed:
 * re-opening the 3D view reuses it instead of re-uploading buffers.
 */
let geometries: ReturnType<typeof createGeometries> | null = null;

function createGeometries() {
  return {
    stallFloor: new THREE.BoxGeometry(STALL_WIDTH, 0.08, STALL_DEPTH),
    // Sits just above the floor; only its rim shows past the bedding, which reads as an outline.
    stallOutline: new THREE.BoxGeometry(STALL_WIDTH - 0.04, 0.03, STALL_DEPTH - 0.04),
    bedding: new THREE.BoxGeometry(STALL_WIDTH - 0.5, 0.032, STALL_DEPTH - 0.5),
    partitionPanel: new THREE.BoxGeometry(0.1, 0.9, STALL_DEPTH),
    partitionBar: new THREE.CylinderGeometry(0.018, 0.018, 0.55, 6),
    topRail: new THREE.BoxGeometry(0.08, 0.06, STALL_DEPTH),
    post: new THREE.BoxGeometry(0.16, 1.6, 0.16),
    gatePanel: new THREE.BoxGeometry(1.5, 0.75, 0.07),
    gateBar: new THREE.CylinderGeometry(0.016, 0.016, 0.5, 6),
    gateRail: new THREE.BoxGeometry(1.5, 0.07, 0.09),
    plaque: new THREE.BoxGeometry(0.42, 0.2, 0.03),
    feeder: new THREE.BoxGeometry(0.9, 0.36, 0.44),
    water: new THREE.CylinderGeometry(0.25, 0.21, 0.26, 16),
    straw: new THREE.BoxGeometry(0.42, 0.025, 0.04),
    hayBale: new THREE.BoxGeometry(1.1, 0.5, 0.6),
    lampPost: new THREE.CylinderGeometry(0.05, 0.06, 2.6, 8),
    lamp: new THREE.SphereGeometry(0.16, 12, 10),
    aisleJoint: new THREE.BoxGeometry(0.03, 0.01, AISLE_WIDTH - 0.3),
    trunk: new THREE.CylinderGeometry(0.12, 0.16, 1.1, 8),
    crown: new THREE.ConeGeometry(0.9, 2, 9),
    ring: new THREE.RingGeometry(0.95, 1.2, 40),
    disc: new THREE.CircleGeometry(0.45, 28),
    legs: new THREE.CapsuleGeometry(0.17, 0.5, 4, 10),
    body: new THREE.CapsuleGeometry(0.27, 0.45, 4, 12),
    coat: new THREE.ConeGeometry(0.4, 0.8, 14),
    head: new THREE.SphereGeometry(0.22, 18, 14),
    cap: new THREE.CylinderGeometry(0.24, 0.24, 0.08, 16),
    brim: new THREE.CylinderGeometry(0.34, 0.34, 0.025, 18),
  };
}

export function sharedGeometries() {
  geometries ??= createGeometries();
  return geometries;
}
