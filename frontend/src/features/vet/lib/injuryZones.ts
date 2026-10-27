// Body-zone map for the urgent-horse model.
// Coordinates are NORMALIZED to the model's bounding box (0..1 per axis),
// which is more stable than raw world coords. Boundaries are calibrated
// against the GLB bbox (min/max from the mesh POSITION accessor).
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
 * Maps a local 3D point (or normalized point) to standard body zone:
 * - head: high y
 * - neck: upper-mid y, front z
 * - foreleg: lower y, front z
 * - hindleg: lower y, rear z
 * - hindquarters: rear z
 * - trunk: mid body
 */
export function zoneForPoint(p: { x: number; y: number; z: number }): string {
  const n = normalizePoint(p);
  if (n.y > 0.8) return 'head';
  if (n.y > 0.6) return 'neck';
  if (n.y < 0.35) return n.z < 0.55 ? 'foreleg' : 'hindleg';
  return n.z > 0.65 ? 'hindquarters' : 'trunk';
}

export function zoneById(id: string): BodyZone | undefined {
  return BODY_ZONES.find((z) => z.id === id);
}
