import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  zoneForPoint,
  zoneById,
  BODY_ZONES,
  normalizePoint,
  normalizedToWorld,
  worldToNormalized,
  isNormalizedPoint,
  resolveInjuryWorldPosition,
  computeAnatomicalNormal,
  resolveInjuryNormal,
  resolveBodyRegionKey,
  matchBodyRegion,
} from '../injuryZones.ts';

test('normalizePoint maps model coords to 0..1', () => {
  // Center spine (x=0), ground (y=-0.21), mid body
  const n = normalizePoint({ x: 0, y: 219.76, z: 0 });
  assert.ok(Math.abs(n.x - 0.5) < 0.01, 'Centerline is 0.5');
  assert.ok(Math.abs(n.y - 1) < 0.01, 'Top of head is 1');
});

test('zoneForPoint maps 3D model coords to body zones', () => {
  // head: high y
  assert.equal(zoneForPoint({ x: 0, y: 200, z: 100 }), 'head');
  // neck: upper y, front z
  assert.equal(zoneForPoint({ x: 0, y: 160, z: 70 }), 'neck');
  // trunk: mid y, mid z
  assert.equal(zoneForPoint({ x: 10, y: 100, z: 0 }), 'trunk');
  // hindquarters: mid y, rear z (z < 0 in GLB is rear)
  assert.equal(zoneForPoint({ x: 0, y: 100, z: -80 }), 'hindquarters');
  // foreleg: lower y, front z
  assert.equal(zoneForPoint({ x: 10, y: 10, z: 50 }), 'foreleg');
  // hindleg: lower y, rear z
  assert.equal(zoneForPoint({ x: 10, y: 10, z: -60 }), 'hindleg');
});

test('zoneById returns defined zone', () => {
  assert.equal(zoneById('head')?.label, 'Head');
  assert.equal(zoneById('nope'), undefined);
});

test('BODY_ZONES has unique ids', () => {
  const ids = BODY_ZONES.map((z) => z.id);
  assert.equal(new Set(ids).size, ids.length);
});

test('normalizedToWorld maps [0..1] bounding-box coords to 3D world space on the horse', () => {
  // Daenerys neck injury marker: x=0.55 (right of midline), y=0.80 (neck height), z=0.30 (front/neck)
  const daenerysPos = normalizedToWorld({ x: 0.55, y: 0.8, z: 0.3 });
  assert.ok(Math.abs(daenerysPos[0] - -0.0351) < 0.01, 'X is slightly to horse right');
  assert.ok(Math.abs(daenerysPos[1] - 0.62) < 0.02, 'Y is at neck height');
  assert.ok(Math.abs(daenerysPos[2] - 0.5564) < 0.02, 'Z is towards front/neck (+Z)');

  // Muscle injury marker: x=0.68 (right thigh), y=0.35 (thigh height), z=0.72 (rear/hindleg)
  const musclePos = normalizedToWorld({ x: 0.68, y: 0.35, z: 0.72 });
  assert.ok(Math.abs(musclePos[0] - -0.1262) < 0.01, 'X is on right thigh');
  assert.ok(Math.abs(musclePos[1] - -0.46) < 0.02, 'Y is at thigh/gaskin height');
  assert.ok(Math.abs(musclePos[2] - -0.6121) < 0.02, 'Z is towards rear/hindleg (-Z)');
});

test('worldToNormalized roundtrips with normalizedToWorld', () => {
  const initial = { x: 0.6, y: 0.45, z: 0.7 };
  const world = normalizedToWorld(initial);
  const back = worldToNormalized(world);
  assert.ok(Math.abs(back.x - initial.x) < 0.01);
  assert.ok(Math.abs(back.y - initial.y) < 0.01);
  assert.ok(Math.abs(back.z - initial.z) < 0.01);
});

test('isNormalizedPoint detects [0..1] range vs world coords', () => {
  assert.equal(isNormalizedPoint({ x: 0.55, y: 0.8, z: 0.3 }), true);
  assert.equal(isNormalizedPoint({ x: -0.15, y: 0.4, z: -0.3 }), false);
  assert.equal(isNormalizedPoint({ x: 0.1, y: -0.8, z: 0.2 }), false);
});

test('resolveInjuryWorldPosition resolves both normalized seeds and world hits', () => {
  // Legacy DB seed (Daenerys - neck)
  const resolved = resolveInjuryWorldPosition({
    positionX: 0.55,
    positionY: 0.8,
    positionZ: 0.3,
  });
  assert.ok(Math.abs(resolved[0] - -0.0351) < 0.01);
  assert.ok(Math.abs(resolved[1] - 0.62) < 0.02);
  assert.ok(Math.abs(resolved[2] - 0.5564) < 0.02);

  // Modern 3D raycast hit (has normal vector and world coords)
  const hit = resolveInjuryWorldPosition({
    positionX: 0.12,
    positionY: -0.85,
    positionZ: 0.3,
    normalX: 0.5,
    normalY: 0.8,
    normalZ: 0.2,
  });
  assert.equal(hit[0], 0.12);
  assert.equal(hit[1], -0.85);
  assert.equal(hit[2], 0.3);
});

test('computeAnatomicalNormal produces correct outward orientation', () => {
  // Left flank: outward normal points to +X
  const leftFlank = computeAnatomicalNormal([0.28, 0.1, 0.1]);
  assert.ok(leftFlank[0] > 0.5, 'Left flank normal X > 0.5');

  // Right flank: outward normal points to -X
  const rightFlank = computeAnatomicalNormal([-0.28, 0.1, 0.1]);
  assert.ok(rightFlank[0] < -0.5, 'Right flank normal X < -0.5');

  // Spine top: outward normal points upward +Y
  const spine = computeAnatomicalNormal([0, 0.6, 0.1]);
  assert.ok(spine[1] > 0.8, 'Spine normal Y > 0.8');

  // Chest: outward normal points forward +Z (in urgent-horse.glb)
  const chest = computeAnatomicalNormal([0.02, 0.1, 0.9]);
  assert.ok(chest[2] > 0.4, 'Chest normal Z > 0.4');

  // Rump: outward normal points backward -Z (in urgent-horse.glb)
  const rump = computeAnatomicalNormal([0.02, 0.4, -0.9]);
  assert.ok(rump[2] < -0.4, 'Rump normal Z < -0.4');
});

test('resolveInjuryNormal cures degenerate vertical default normals and preserves valid face normals', () => {
  // Degenerate dummy [0, 1, 0] on flank is replaced with lateral normal
  const cured = resolveInjuryNormal([0, 1, 0], [0.25, 0, 0]);
  assert.ok(cured[0] > 0.5, 'Degenerate normal on flank resolved to outward X');

  // Legitimate hit face normal is preserved
  const valid = resolveInjuryNormal([0.8, 0.2, 0.1], [0.25, 0, 0]);
  assert.ok(Math.abs(valid[0] - 0.96) < 0.05, 'Valid face normal preserved');
});


test('resolveBodyRegionKey maps zone and hit coordinate to canonical 8 region keys', () => {
  // Foreleg left vs right (+X is left in GLB)
  assert.equal(resolveBodyRegionKey('foreleg', { x: 10, y: 10, z: 50 }), 'foreleg_left');
  assert.equal(resolveBodyRegionKey('foreleg', { x: -10, y: 10, z: 50 }), 'foreleg_right');

  // Hindleg left vs right
  assert.equal(resolveBodyRegionKey('hindleg', { x: 10, y: 10, z: -60 }), 'hindleg_left');
  assert.equal(resolveBodyRegionKey('hindleg', { x: -10, y: 10, z: -60 }), 'hindleg_right');

  // Head and neck preserve direct keys
  assert.equal(resolveBodyRegionKey('head', { x: 0, y: 200, z: 100 }), 'head');
  assert.equal(resolveBodyRegionKey('neck', { x: 0, y: 160, z: 70 }), 'neck');

  // Trunk/back (high y) vs abdomen (low y)
  assert.equal(resolveBodyRegionKey('trunk', { x: 0, y: 180, z: 0 }), 'back');
  assert.equal(resolveBodyRegionKey('trunk', { x: 0, y: 60, z: 0 }), 'abdomen');
});

test('matchBodyRegion finds matching horse_body_region from catalog', () => {
  const regions = [
    { id: 1, code: 'HEAD', displayName: 'Head', modelNodeKey: 'head' },
    { id: 2, code: 'NECK', displayName: 'Neck', modelNodeKey: 'neck' },
    { id: 3, code: 'BACK', displayName: 'Back', modelNodeKey: 'back' },
    { id: 4, code: 'ABDOMEN', displayName: 'Abdomen', modelNodeKey: 'abdomen' },
    { id: 5, code: 'FORELEG_LEFT', displayName: 'Foreleg (Left)', modelNodeKey: 'foreleg_left' },
    { id: 6, code: 'FORELEG_RIGHT', displayName: 'Foreleg (Right)', modelNodeKey: 'foreleg_right' },
    { id: 7, code: 'HINDLEG_LEFT', displayName: 'Hindleg (Left)', modelNodeKey: 'hindleg_left' },
    { id: 8, code: 'HINDLEG_RIGHT', displayName: 'Hindleg (Right)', modelNodeKey: 'hindleg_right' },
  ];

  const forelegRight = matchBodyRegion(regions, 'foreleg', { x: -10, y: 10, z: 50 });
  assert.equal(forelegRight?.id, 6);
  assert.equal(forelegRight?.code, 'FORELEG_RIGHT');

  const forelegLeft = matchBodyRegion(regions, 'foreleg', { x: 10, y: 10, z: 50 });
  assert.equal(forelegLeft?.id, 5);
  assert.equal(forelegLeft?.code, 'FORELEG_LEFT');

  const back = matchBodyRegion(regions, 'trunk', { x: 0, y: 180, z: 0 });
  assert.equal(back?.id, 3);
  assert.equal(back?.code, 'BACK');
});
