// Body-zone map for the urgent-horse model.
// Coordinates are NORMALIZED to the model's bounding box (0..1 per axis),
// which is more stable than raw world coords. Boundaries are calibrated
// against the GLB bbox (min/max from the mesh POSITION accessor).
// shortcut: zone boundaries are height/depth bands; refine with designer if needed.
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
export const MODEL_BOUNDS = {
  min: { x: -32.139862, y: -0.213483, z: -112.122093 },
  max: { x: 32.139915, y: 219.761337, z: 142.886353 },
};

export function normalizePoint(p: { x: number; y: number; z: number }): { x: number; y: number; z: number } {
  const nx = (p.x - MODEL_BOUNDS.min.x) / (MODEL_BOUNDS.max.x - MODEL_BOUNDS.min.x);
  const ny = (p.y - MODEL_BOUNDS.min.y) / (MODEL_BOUNDS.max.y - MODEL_BOUNDS.min.y);
  const nz = (p.z - MODEL_BOUNDS.min.z) / (MODEL_BOUNDS.max.z - MODEL_BOUNDS.min.z);
  return { x: Math.min(1, Math.max(0, nx)), y: Math.min(1, Math.max(0, ny)), z: Math.min(1, Math.max(0, nz)) };
}

export function zoneForPoint(p: { x: number; y: number; z: number }): string {
  const n = normalizePoint(p);
  if (n.y > 0.8) return 'head';
  if (n.y > 0.62) return 'neck';
  if (n.y < 0.32) return n.x > 0.5 ? 'foreleg' : 'hindleg';
  return n.x < 0.4 ? 'hindquarters' : 'trunk';
}

export function zoneById(id: string): BodyZone | undefined {
  return BODY_ZONES.find((z) => z.id === id);
}