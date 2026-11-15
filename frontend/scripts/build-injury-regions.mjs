/**
 * Builds per-triangle injury region maps for every mesh of the horse model.
 *
 * Why: clicks used to normalize the hit point against THREE.Box3.setFromObject(scene). That box
 * includes mane/strand nodes with translations (~7% drift), so head clicks landed in the neck band,
 * back clicks in the abdomen band, etc. Now every mesh gets its own region string, assigned per
 * triangle from the dominant skin bone (JOINTS_0/WEIGHTS_0), so borders follow the skeleton and no
 * camera angle, runtime bounding box or eyeball/saddle click can shift the answer.
 *
 * Bones are named anatomically: head/jaw, neck_*, upperarm|lowerarm|hand_l|r,
 * upperleg|lowerleg|foot_l|r, spine|pelvis, tail. Helper/corrective bones fall back to the
 * coordinate bands from horseAnatomyBlueprint (see --bands for the legacy-only output).
 *
 * Usage:
 *   node scripts/build-injury-regions.mjs            # writes injuryRegionMap.json
 *   node scripts/build-injury-regions.mjs --grid     # + ASCII side view of the body mesh
 *   node scripts/build-injury-regions.mjs --bands    # compare with the old coordinate bands
 */
import fs from 'node:fs';
import path from 'node:path';

const ROOT = path.resolve(import.meta.dirname, '..');
const MODEL = path.join(ROOT, 'public/models/urgent-horse-v2.glb');
const OUT = path.join(ROOT, 'src/features/vet/lib/injuryRegionMap.json');

// --- blueprint bands (mirror of horseAnatomyBlueprint.ts, fallback only) -----
const B = {
  headYMin: 0.86,
  neckYMin: 0.66,
  neckZMax: 0.46,
  backYMin: 0.62,
  trunkYMin: 0.55,
  spineNormalY: 0.15,
  abdomenZMin: 0.42,
  abdomenZMax: 0.72,
  frontShoulderYMax: 0.52,
  legYMax: 0.44,
  forelegZMax: 0.55,
};

const CODES = {
  head: '0',
  neck: '1',
  back: '2',
  abdomen: '3',
  foreleg_left: '4',
  foreleg_right: '5',
  hindleg_left: '6',
  hindleg_right: '7',
  eye: '8',
  tail: '9',
};

function zoneOf(n, normal) {
  if (n.y > B.headYMin) return 'head';
  if (n.y > B.neckYMin && n.z < B.neckZMax) return 'neck';
  if (n.y > B.backYMin) return 'back';
  const legZone = (z) => (z < B.forelegZMax ? 'foreleg' : 'hindleg');
  if (n.y < B.legYMax) return legZone(n.z);
  if (n.z > B.abdomenZMax) return 'hindleg';
  if (n.z < B.abdomenZMin && n.y < B.frontShoulderYMax) return 'foreleg';
  if (n.y > B.trunkYMin) {
    if (normal) {
      const len = Math.hypot(normal[0], normal[1], normal[2]) || 1;
      return normal[1] / len > B.spineNormalY ? 'back' : 'abdomen';
    }
    return 'back';
  }
  return 'abdomen';
}

/** Skeleton-driven class. Returns null for helper bones so the caller can fall back. */
function classFromBone(name, normal) {
  if (/(^|_)(head|jaw)/.test(name)) return 'head';
  if (/neck/.test(name)) return 'neck';
  const side = /_l(_|$)/.test(name) ? 'left' : /_r(_|$)/.test(name) ? 'right' : null;
  if (/upperarm|lowerarm|hand/.test(name)) return side ? `foreleg_${side}` : null;
  if (/upperleg|lowerleg|foot/.test(name)) return side ? `hindleg_${side}` : null;
  // Tail bones sit on the croup; the tail itself is its own region.
  if (/tail/.test(name)) return 'tail';
  if (/spine|pelvis|root|shoulder|rivet/.test(name)) {
    const len = Math.hypot(normal[0], normal[1], normal[2]) || 1;
    return normal[1] / len > 0.2 ? 'back' : 'abdomen';
  }
  return null;
}

// --- GLB parsing -----------------------------------------------------------
const file = fs.readFileSync(MODEL);
const jsonLen = file.readUInt32LE(12);
const json = JSON.parse(file.subarray(20, 20 + jsonLen).toString('utf8'));
const bin = file.subarray(20 + jsonLen + 8);

const COMPONENT = { 5120: 1, 5121: 1, 5122: 2, 5123: 2, 5125: 4, 5126: 4 };
const dv = new DataView(bin.buffer, bin.byteOffset);

function readAccessor(accessorIndex, components) {
  const acc = json.accessors[accessorIndex];
  const view = json.bufferViews[acc.bufferView];
  const bytes = COMPONENT[acc.componentType];
  const stride = view.byteStride ?? bytes * components;
  const base = (view.byteOffset ?? 0) + (acc.byteOffset ?? 0);
  const out = new Float32Array(acc.count * components);
  for (let i = 0; i < acc.count; i++) {
    for (let c = 0; c < components; c++) {
      const off = base + i * stride + c * bytes;
      // KHR_mesh_quantization keeps min/max in the same quantized units as the stored integers,
      // so raw values are used here (dequantizing would break the band math).
      if (acc.componentType === 5126) out[i * components + c] = dv.getFloat32(off, true);
      else if (acc.componentType === 5122) out[i * components + c] = dv.getInt16(off, true);
      else if (acc.componentType === 5123) out[i * components + c] = dv.getUint16(off, true);
      else if (acc.componentType === 5121) out[i * components + c] = dv.getUint8(off);
      else if (acc.componentType === 5125) out[i * components + c] = dv.getUint32(off, true);
      else throw new Error('unsupported componentType ' + acc.componentType);
    }
  }
  return { values: out, acc };
}

function readIndices(accessorIndex) {
  return readAccessor(accessorIndex, 1).values;
}

const boneNames = json.skins[0].joints.map((i) => (json.nodes[i].name || '').toLowerCase());
// Anatomy frame = body mesh bounds; every mesh shares the same node space.
const bodyBounds = json.accessors[json.meshes[0].primitives[0].attributes.POSITION];
const { min, max } = bodyBounds;
const bw = max[0] - min[0];
const bh = max[1] - min[1];
const bd = max[2] - min[2];

const useBands = process.argv.includes('--bands');
const showGrid = process.argv.includes('--grid');
const ROWS = 20;
const COLS = 20;
const GLYPH = { head: 'H', neck: 'N', back: 'B', abdomen: 'A', foreleg_left: 'F', foreleg_right: 'F', hindleg_left: 'G', hindleg_right: 'G', eye: 'E', tail: 'T' };
const grid = Array.from({ length: ROWS }, () => Array.from({ length: COLS }, () => ({})));

const meshMaps = [];

json.meshes.forEach((mesh, meshIndex) => {
  const primitive = mesh.primitives[0];
  const pos = readAccessor(primitive.attributes.POSITION, 3);
  const idx = readIndices(primitive.indices);
  const joints = readAccessor(primitive.attributes.JOINTS_0, 4);
  const weights = readAccessor(primitive.attributes.WEIGHTS_0, 4);
  const weightScale = json.accessors[primitive.attributes.WEIGHTS_0].componentType === 5121 ? 255 : 1;
  const triCount = idx.length / 3;
  let region = '';

  for (let t = 0; t < triCount; t++) {
    const ia = idx[t * 3];
    const ib = idx[t * 3 + 1];
    const ic = idx[t * 3 + 2];
    const cx = (pos.values[ia * 3] + pos.values[ib * 3] + pos.values[ic * 3]) / 3;
    const cy = (pos.values[ia * 3 + 1] + pos.values[ib * 3 + 1] + pos.values[ic * 3 + 1]) / 3;
    const cz = (pos.values[ia * 3 + 2] + pos.values[ib * 3 + 2] + pos.values[ic * 3 + 2]) / 3;

    // Anatomy frame: +X is Left, +Y is up, +Z is front (nose).
    const n = { x: 1 - (cx - min[0]) / bw, y: (cy - min[1]) / bh, z: (max[2] - cz) / bd };

    const ax = pos.values[ib * 3] - pos.values[ia * 3];
    const ay = pos.values[ib * 3 + 1] - pos.values[ia * 3 + 1];
    const az = pos.values[ib * 3 + 2] - pos.values[ia * 3 + 2];
    const bx = pos.values[ic * 3] - pos.values[ia * 3];
    const by = pos.values[ic * 3 + 1] - pos.values[ia * 3 + 1];
    const bz = pos.values[ic * 3 + 2] - pos.values[ia * 3 + 2];
    const normal = [ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx];

    let key;
    if (/_eye_/i.test(mesh.name)) {
      // The eye mesh is a small dedicated object; every triangle of it is the eye region.
      key = 'eye';
    } else if (useBands) {
      const zone = zoneOf(n, normal);
      key = zone === 'foreleg' || zone === 'hindleg'
        ? `${zone}_${n.x <= 0.5 ? 'left' : 'right'}`
        : zone;
    } else {
      const jointWeights = {};
      for (const vertex of [ia, ib, ic]) {
        for (let k = 0; k < 4; k++) {
          const jointIndex = joints.values[vertex * 4 + k];
          const weight = weights.values[vertex * 4 + k] / weightScale;
          if (weight > 0) jointWeights[jointIndex] = (jointWeights[jointIndex] ?? 0) + weight;
        }
      }
      const dominant = Object.entries(jointWeights).sort((a, b) => b[1] - a[1])[0];
      key = dominant ? classFromBone(boneNames[Number(dominant[0])], normal) : null;
      if (!key) {
        // Helper/corrective bones carry no anatomical meaning: fall back to the coordinate bands.
        const zone = zoneOf(n, normal);
        key = zone === 'foreleg' || zone === 'hindleg'
          ? `${zone}_${n.x <= 0.5 ? 'left' : 'right'}`
          : zone;
      }
    }
    region += CODES[key];

    if (meshIndex === 0) {
      const row = Math.min(ROWS - 1, Math.max(0, Math.floor((1 - n.y) * ROWS)));
      const col = Math.min(COLS - 1, Math.max(0, Math.floor(n.z * COLS)));
      grid[row][col][GLYPH[key]] = (grid[row][col][GLYPH[key]] ?? 0) + 1;
    }
  }

  meshMaps.push({ mesh: mesh.name, triangles: triCount, region });
});

fs.writeFileSync(
  OUT,
  JSON.stringify({
    model: 'urgent-horse-v2.glb',
    codes: Object.fromEntries(Object.entries(CODES).map(([k, v]) => [v, k])),
    meshes: meshMaps,
  }),
);

console.log('meshes', meshMaps.map((m) => `${m.mesh}:${m.triangles}`).join(' '));
console.log('wrote', path.relative(ROOT, OUT), 'bytes', fs.statSync(OUT).size);

if (showGrid) {
  console.log('\nBODY SIDE VIEW: rows = height 1.0 (top) → 0.0, cols = nose (left) → tail (right)\n');
  console.log('       z:  ' + Array.from({ length: COLS }, (_, i) => (i % 5 === 0 ? String((i / COLS).toFixed(2)).slice(1) : ' ')).join(''));
  grid.forEach((row, r) => {
    const y = (1 - (r + 0.5) / ROWS).toFixed(2);
    const cells = row.map((cell) => {
      const best = Object.entries(cell).sort((a, b) => b[1] - a[1])[0];
      return best ? best[0] : '.';
    });
    console.log(` y ${y} ` + cells.join(''));
  });
  console.log('\n H=head N=neck B=back A=abdomen F=foreleg G=hindleg .=no mesh');
}
