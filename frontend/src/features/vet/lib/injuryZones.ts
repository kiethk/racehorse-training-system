// Body-zone map for the urgent-horse model.
// Coordinates are NORMALIZED to the model's bounding box (0..1 per axis),
// which is more stable than raw world coords. Boundaries are calibrated
// against the GLB bbox (min/max from the mesh POSITION accessor).
// Region bands live in horseAnatomyBlueprint.ts (user-approved 2D blueprint).
import {
  HORSE_ANATOMY_BLUEPRINT,
  zoneFromBlueprint,
} from './horseAnatomyBlueprint.ts';
export interface BodyZone {
  id: string;
  label: string;
  anchor: { x: number; y: number };
}

export const BODY_ZONES: BodyZone[] = [
  { id: 'head', label: 'Head', anchor: { x: 0.82, y: 0.18 } },
  { id: 'neck', label: 'Neck', anchor: { x: 0.7, y: 0.3 } },
  { id: 'trunk', label: 'Trunk', anchor: { x: 0.5, y: 0.45 } },
  { id: 'hindquarters', label: 'Hindquarters', anchor: { x: 0.28, y: 0.45 } },
  { id: 'foreleg', label: 'Foreleg', anchor: { x: 0.62, y: 0.78 } },
  { id: 'hindleg', label: 'Hindleg', anchor: { x: 0.34, y: 0.78 } },
];

// Model bounding box (from GLB POSITION accessor min/max).
// NOTE: these are raw accessor-space bounds, not the runtime scene bbox — mane/
// strand nodes carry translations, so do not "re-measure" from primitives; the
// runtime box is probed in snapToHorseSurface instead.
export const MODEL_BOUNDS = {
  min: { x: -32.139862, y: -0.213483, z: -112.122093 },
  max: { x: 32.139915, y: 219.761337, z: 142.886353 },
};

const BBOX_HEIGHT = MODEL_BOUNDS.max.y - MODEL_BOUNDS.min.y;
const SCALE = 2.4 / BBOX_HEIGHT;
const CENTER_Z = (MODEL_BOUNDS.min.z + MODEL_BOUNDS.max.z) / 2;

export const HORSE_TRANSFORM = {
  scale: SCALE,
  position: [
    0,
    -MODEL_BOUNDS.min.y * SCALE - 1.3,
    -CENTER_Z * SCALE,
  ] as [number, number, number],
};

/**
 * Normalizes a local GLB point into [0..1] anatomical coordinates:
 * - x: 0 = Left side of horse, 0.5 = Spine/Midline, 1 = Right side of horse
 * - y: 0 = Ground/Hooves, 1 = Top of head/Ears
 * - z: 0 = Nose/Head (Front), 1 = Tail (Rear)
 */
export function normalizePoint(p: { x: number; y: number; z: number }): { x: number; y: number; z: number } {
  // In urgent-horse.glb: +X is Left, -X is Right
  const nx = 0.5 - p.x / (MODEL_BOUNDS.max.x - MODEL_BOUNDS.min.x);
  // In urgent-horse.glb: -Y is ground, +Y is top of head
  const ny = (p.y - MODEL_BOUNDS.min.y) / (MODEL_BOUNDS.max.y - MODEL_BOUNDS.min.y);
  // In urgent-horse.glb: +Z is Front (Nose), -Z is Rear (Tail)
  const nz = (MODEL_BOUNDS.max.z - p.z) / (MODEL_BOUNDS.max.z - MODEL_BOUNDS.min.z);

  return {
    x: Math.min(1, Math.max(0, Number(nx.toFixed(4)))),
    y: Math.min(1, Math.max(0, Number(ny.toFixed(4)))),
    z: Math.min(1, Math.max(0, Number(nz.toFixed(4)))),
  };
}

/**
 * Converts a normalized coordinate [0..1] to world 3D coordinates aligned with HorseModel:
 * - n.x: 0 = Left side, 0.5 = Midline/Spine, 1 = Right side
 * - n.y: 0 = Ground/Hooves, 1 = Top of head/Ears
 * - n.z: 0 = Nose/Head (Front), 1 = Tail (Rear)
 */
export function normalizedToWorld(n: { x: number; y: number; z: number }): [number, number, number] {
  const localX = (0.5 - n.x) * (MODEL_BOUNDS.max.x - MODEL_BOUNDS.min.x);
  const localY = MODEL_BOUNDS.min.y + n.y * (MODEL_BOUNDS.max.y - MODEL_BOUNDS.min.y);
  const localZ = MODEL_BOUNDS.max.z - n.z * (MODEL_BOUNDS.max.z - MODEL_BOUNDS.min.z);

  const scale = HORSE_TRANSFORM.scale;
  const posY = HORSE_TRANSFORM.position[1];
  const posZ = HORSE_TRANSFORM.position[2];

  return [
    Number((localX * scale).toFixed(4)),
    Number((localY * scale + posY).toFixed(4)),
    Number((localZ * scale + posZ).toFixed(4)),
  ];
}

/**
 * Converts a world 3D coordinate to normalized [0..1] bounding box coordinates.
 */
export function worldToNormalized(w: [number, number, number] | { x: number; y: number; z: number }): {
  x: number;
  y: number;
  z: number;
} {
  const wx = Array.isArray(w) ? w[0] : w.x;
  const wy = Array.isArray(w) ? w[1] : w.y;
  const wz = Array.isArray(w) ? w[2] : w.z;

  const scale = HORSE_TRANSFORM.scale;
  const posY = HORSE_TRANSFORM.position[1];
  const posZ = HORSE_TRANSFORM.position[2];

  const localX = wx / scale;
  const localY = (wy - posY) / scale;
  const localZ = (wz - posZ) / scale;

  return normalizePoint({ x: localX, y: localY, z: localZ });
}

/**
 * Checks whether a given (x, y, z) is in normalized [0..1] coordinate space.
 * Horse width spans [-0.35, +0.35] in world space, so x > 0.35 or values strictly
 * in [0, 1] without negative Y (legs) represent normalized bounding box coordinates.
 */
export function isNormalizedPoint(p: { x: number; y: number; z: number } | [number, number, number]): boolean {
  const x = Array.isArray(p) ? p[0] : p.x;
  const y = Array.isArray(p) ? p[1] : p.y;
  const z = Array.isArray(p) ? p[2] : p.z;

  if (x < 0 || x > 1 || y < 0 || y > 1 || z < 0 || z > 1) {
    return false;
  }
  // Any x > 0.35 is impossible on the horse in world coordinates (half-width is ~0.35m),
  // which distinguishes normalized [0..1] seeds (midline at 0.5) from world coords (midline at 0).
  if (x > 0.35) return true;
  return false;
}

/**
 * Resolves injury coordinates into valid Three.js world space coordinates:
 * Handles legacy normalized DB seeds ([0..1]) as well as direct Three.js world clicks.
 */
export function resolveInjuryWorldPosition(injury: {
  positionX?: number | null;
  positionY?: number | null;
  positionZ?: number | null;
  localX?: number | null;
  localY?: number | null;
  localZ?: number | null;
  normalX?: number | null;
  normalY?: number | null;
  normalZ?: number | null;
} | [number, number, number]): [number, number, number] {
  if (Array.isArray(injury)) {
    const [x, y, z] = injury;
    if (x >= 0 && x <= 1 && y >= 0 && y <= 1 && z >= 0 && z <= 1 && x > 0.35) {
      return normalizedToWorld({ x, y, z });
    }
    return [x, y, z];
  }

  if (injury.localX != null && injury.localY != null && injury.localZ != null) {
    return normalizedToWorld({
      x: Number(injury.localX),
      y: Number(injury.localY),
      z: Number(injury.localZ),
    });
  }

  const px = Number(injury.positionX ?? 0);
  const py = Number(injury.positionY ?? 0);
  const pz = Number(injury.positionZ ?? 0);

  // If point is within [0..1] space and either x > 0.35 or normal vector is absent,
  // it is a legacy/seed normalized coordinate that must be mapped to world coordinates.
  if (
    px >= 0 &&
    px <= 1 &&
    py >= 0 &&
    py <= 1 &&
    pz >= 0 &&
    pz <= 1 &&
    (px > 0.35 || injury.normalX == null)
  ) {
    return normalizedToWorld({ x: px, y: py, z: pz });
  }

  return [px, py, pz];
}

/**
 * Computes an outward anatomical surface normal based on 3D world coordinates.
 * The horse is oriented with:
 * - Midline along X = 0 (left: +X, right: -X)
 * - Spine along Z (head towards negative Z, tail towards positive Z)
 * - Standing ground at Y ~ -1.3, back/head up to Y ~ 1.0.
 */
export function computeAnatomicalNormal(pos: [number, number, number]): [number, number, number] {
  const [x, y, z] = pos;

  let nx = 0;
  let ny = 0;
  let nz = 0;

  // Lateral flank/leg outward normal
  if (Math.abs(x) > 0.06) {
    nx = Math.sign(x) * Math.max(Math.abs(x), 0.4);
  }

  // Vertical (spine/withers/neck vs belly)
  if (y > 0.35) {
    ny = 0.5; // withers, crest, saddle, croup face upwards
  } else if (y < -0.4) {
    ny = -0.2; // lower limbs
  }

  // Longitudinal (in urgent-horse.glb: +Z is Front/Nose, -Z is Rear/Tail)
  if (z > 0.4) {
    nz = 0.5; // front (chest, neck, face)
  } else if (z < -0.4) {
    nz = -0.5; // rear (rump, hindquarters)
  }

  // If near midline top of spine
  if (Math.abs(x) <= 0.06 && y > 0.2) {
    ny = 0.85;
  }

  const len = Math.hypot(nx, ny, nz) || 1;
  return [
    Number((nx / len).toFixed(4)),
    Number((ny / len).toFixed(4)),
    Number((nz / len).toFixed(4)),
  ];
}

/**
 * Resolves a reliable outward normal vector for an injury marker.
 * - Detects degenerate or missing normals (e.g. dummy [0, 1, 0] on side markers)
 * - Ensures normal always faces outward from horse anatomy rather than inward
 * - Preserves exact Three.js face normals from user clicks
 */
export function resolveInjuryNormal(
  normal?:
    | [number, number, number]
    | { normalX?: number | null; normalY?: number | null; normalZ?: number | null }
    | null,
  worldPos?: [number, number, number],
): [number, number, number] {
  const pos: [number, number, number] = worldPos || [0, 0, 0];
  const anat = computeAnatomicalNormal(pos);

  let nx: number | undefined;
  let ny: number | undefined;
  let nz: number | undefined;

  if (Array.isArray(normal)) {
    [nx, ny, nz] = normal;
  } else if (normal && typeof normal === 'object') {
    nx = normal.normalX != null ? Number(normal.normalX) : undefined;
    ny = normal.normalY != null ? Number(normal.normalY) : undefined;
    nz = normal.normalZ != null ? Number(normal.normalZ) : undefined;
  }

  if (nx == null || ny == null || nz == null) {
    return anat;
  }

  const len = Math.hypot(nx, ny, nz);
  if (len < 0.05) {
    return anat;
  }

  const unitN: [number, number, number] = [nx / len, ny / len, nz / len];

  // Detect degenerate dummy [0, 1, 0] default normal on side markers (|x| > 0.08)
  const isDefaultUp =
    Math.abs(unitN[0]) < 0.02 && Math.abs(unitN[1] - 1) < 0.02 && Math.abs(unitN[2]) < 0.02;
  if (isDefaultUp && Math.abs(pos[0]) > 0.08) {
    return anat;
  }

  return [
    Number(unitN[0].toFixed(4)),
    Number(unitN[1].toFixed(4)),
    Number(unitN[2].toFixed(4)),
  ];
}

/**
 * Normalizes a scene-local point (glTF scene space, i.e. what the runtime
 * transform of UrgentHorseInjuryViewer maps to) into [0..1] anatomical coords
 * measured against the RUNTIME bounding box of the same transform.
 * urgent-horse.glb applies scale 0.01 on its mesh nodes, so the mesh accessor
 * bounds (MODEL_BOUNDS) are ~100x the scene-space extents and must NOT be used
 * on click points — that mismatch classified nearly every hit as Hindleg.
 * - x: 0 = Left side, 0.5 = Spine/Midline, 1 = Right side (+X is Left in GLB)
 * - y: 0 = Ground/Hooves, 1 = Top of head/Ears (-Y is ground)
 * - z: 0 = Nose/Head (Front), 1 = Tail (Rear) (+Z is Front)
 */
export function normalizeScenePoint(
  p: { x: number; y: number; z: number },
  bounds: {
    min: { x: number; y: number; z: number };
    max: { x: number; y: number; z: number };
  },
): { x: number; y: number; z: number } {
  const w = Math.max(bounds.max.x - bounds.min.x, 1e-6);
  const h = Math.max(bounds.max.y - bounds.min.y, 1e-6);
  const d = Math.max(bounds.max.z - bounds.min.z, 1e-6);
  // +X is Left: leftmost (+maxX) maps to 0, rightmost (minX) maps to 1.
  const nx = 1 - (p.x - bounds.min.x) / w;
  const ny = (p.y - bounds.min.y) / h;
  const nz = (bounds.max.z - p.z) / d;

  return {
    x: Math.min(1, Math.max(0, Number(nx.toFixed(4)))),
    y: Math.min(1, Math.max(0, Number(ny.toFixed(4)))),
    z: Math.min(1, Math.max(0, Number(nz.toFixed(4)))),
  };
}

/**
 * Classifies a normalized [0..1] anatomical point into the geometry zone.
 * Bands come from the 2D anatomy blueprint (horseAnatomyBlueprint.ts) — the
 * user-approved source of truth, anchored to live-measured clicks, not to
 * ad-hoc thresholds.
 */
export function zoneForNormalized(
  n: { x: number; y: number; z: number },
  surfaceNormal?: { x: number; y: number; z: number } | null,
): string {
  return zoneFromBlueprint(n, surfaceNormal);
}

/**
 * Classifies a normalized [0..1] anatomical point into the canonical
 * 8-region key. Legs keep the left/right split on the anatomy frame
 * (n.x <= 0.5 means Left); head/neck/back/abdomen are already region keys.
 */
export function regionKeyForNormalized(
  n: { x: number; y: number; z: number },
  surfaceNormal?: { x: number; y: number; z: number } | null,
): string {
  const zone = zoneFromBlueprint(n, surfaceNormal);
  if (zone === 'foreleg' || zone === 'hindleg') {
    return n.x <= 0.5 ? `${zone}_left` : `${zone}_right`;
  }
  if (zone === 'trunk') {
    // Legacy trunk passthrough: split spine vs belly by the approved normal rule.
    if (surfaceNormal) {
      const len = Math.hypot(surfaceNormal.x, surfaceNormal.y, surfaceNormal.z) || 1;
      return surfaceNormal.y / len > HORSE_ANATOMY_BLUEPRINT.spineNormalY ? 'back' : 'abdomen';
    }
    return n.y > HORSE_ANATOMY_BLUEPRINT.trunkYMin ? 'back' : 'abdomen';
  }
  return zone;
}

/**
 * Maps a local 3D point (or normalized point) to standard body zone:
 * - head: high y
 * - neck: upper-mid y, front z
 * - foreleg: lower y, front z
 * - hindleg: lower y, rear z
 * - hindquarters: rear z
 * - trunk: mid body
 */
export function zoneForPoint(p: { x: number; y: number; z: number }): string {
  return zoneForNormalized(normalizePoint(p));
}

export function zoneById(id: string): BodyZone | undefined {
  return BODY_ZONES.find((z) => z.id === id);
}

// Canonical 8 body-region keys mirror the horse_body_regions catalog
// (modelNodeKey values seeded in V16). Markers store one of these keys in their
// zoneId; legacy records may still carry the 6-zone ids of BODY_ZONES above.
export const BODY_REGION_KEYS = [
  'head',
  'neck',
  'back',
  'abdomen',
  'foreleg_left',
  'foreleg_right',
  'hindleg_left',
  'hindleg_right',
] as const;

export type BodyRegionKey = (typeof BODY_REGION_KEYS)[number];

const REGION_LABELS: Record<BodyRegionKey, string> = {
  head: 'Head',
  neck: 'Neck',
  back: 'Back',
  abdomen: 'Abdomen',
  foreleg_left: 'Foreleg (Left)',
  foreleg_right: 'Foreleg (Right)',
  hindleg_left: 'Hindleg (Left)',
  hindleg_right: 'Hindleg (Right)',
};

export function isBodyRegionKey(value: string | null | undefined): value is BodyRegionKey {
  return value != null && value in REGION_LABELS;
}

/**
 * Display label for a canonical region key; falls back to the legacy
 * 6-zone label so old DB records (trunk, hindquarters, foreleg, ...) still
 * resolve instead of rendering as "Hindleg" guesses.
 */
export function regionKeyLabel(key: string | null | undefined): string | undefined {
  if (!key) return undefined;
  if (isBodyRegionKey(key)) return REGION_LABELS[key];
  return zoneById(key)?.label;
}

/**
 * Geometry zone (BODY_ZONES id) that approximates a region key — used only for
 * 2D-fallback anchoring and other zone-shaped consumers, never for storage.
 */
export function zoneForRegionKey(key: string | null | undefined): string | undefined {
  if (!key) return undefined;
  if (isBodyRegionKey(key)) {
    if (key.startsWith('foreleg')) return 'foreleg';
    if (key.startsWith('hindleg')) return 'hindleg';
    if (key === 'back' || key === 'abdomen') return 'trunk';
    return key;
  }
  return zoneById(key)?.id;
}

/**
 * Full click classification: geometry zone + local model point → canonical
 * 8-region key (e.g. foreleg + x < 0 → 'foreleg_right').
 */
export function regionKeyForPoint(p: { x: number; y: number; z: number }): string {
  return resolveBodyRegionKey(zoneForPoint(p), p);
}

export interface MarkerRegionRef {
  bodyRegionId?: number | null;
  zoneId?: string | null;
}

export interface RegionCatalogItem {
  id: number;
  displayName?: string | null;
  modelNodeKey?: string | null;
  code?: string | null;
}

/**
 * Zone label of a marker, by agreed priority:
 * bodyRegionId (catalog truth) → zoneId (canonical key, then legacy zone) →
 * recomputed from nothing (caller may pass undefined and show a fallback).
 * Invisible: this is display-only; storage keeps bodyRegionId + zoneId.
 */
export function markerZoneLabel(
  bodyRegions: RegionCatalogItem[],
  marker: MarkerRegionRef,
): string | undefined {
  const region = marker.bodyRegionId != null
    ? bodyRegions.find((r) => r.id === marker.bodyRegionId)
    : undefined;
  return region?.displayName ?? regionKeyLabel(marker.zoneId ?? undefined);
}


/**
 * Maps a 3D hit point and zone to the canonical model_node_key of horse_body_regions:
 * - Differentiates left vs right forelegs and hindlegs (+X is left, -X is right)
 * - Differentiates upper trunk (back) vs lower trunk (abdomen)
 */
export function resolveBodyRegionKey(zoneId: string, p: { x: number; y: number; z: number }): string {
  const isLeft = p.x >= 0;
  if (zoneId === 'foreleg') {
    return isLeft ? 'foreleg_left' : 'foreleg_right';
  }
  if (zoneId === 'hindleg') {
    return isLeft ? 'hindleg_left' : 'hindleg_right';
  }
  if (zoneId === 'trunk' || zoneId === 'hindquarters') {
    const n = normalizePoint(p);
    return n.y > HORSE_ANATOMY_BLUEPRINT.trunkYMin ? 'back' : 'abdomen';
  }
  return zoneId;
}

export function matchBodyRegion<T extends { id: number; code: string; modelNodeKey?: string | null; displayName?: string }>(
  bodyRegions: T[],
  zoneId: string,
  p?: { x: number; y: number; z: number },
): T | undefined {
  if (!bodyRegions.length) return undefined;
  const key = p ? resolveBodyRegionKey(zoneId, p) : zoneId;
  return (
    bodyRegions.find((r) => r.modelNodeKey === key) ||
    bodyRegions.find((r) => r.code.toLowerCase() === key.toLowerCase()) ||
    bodyRegions.find((r) => r.modelNodeKey === zoneId) ||
    bodyRegions[0]
  );
}
