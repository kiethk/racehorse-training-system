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
  regionKeyLabel,
  regionKeyForPoint,
  regionKeyForNormalized,
  zoneForNormalized,
  normalizeScenePoint,
  zoneForRegionKey,
  markerZoneLabel,
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
  // neck: upper y (n.y > 0.66), forward z (n.z < 0.46)
  assert.equal(zoneForPoint({ x: 0, y: 160, z: 70 }), 'neck');
  // mid-body central barrel (0.42 <= n.z <= 0.72, 0.44 <= n.y <= 0.55) stays abdomen
  assert.equal(zoneForPoint({ x: 10, y: 105, z: 0 }), 'abdomen');
  // rear thigh/haunch (n.z > 0.72, n.y <= 0.62) returns hindleg instead of abdomen
  assert.equal(zoneForPoint({ x: 0, y: 100, z: -80 }), 'hindleg');
  // legs below legYMax (0.44): fore left / hind right
  assert.equal(zoneForPoint({ x: 26, y: 10, z: 50 }), 'foreleg');
  assert.equal(zoneForPoint({ x: -26, y: 10, z: -60 }), 'hindleg');
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

test('regionKeyLabel resolves the 8 canonical keys and legacy zone ids', () => {
  assert.equal(regionKeyLabel('head'), 'Head');
  assert.equal(regionKeyLabel('back'), 'Back');
  assert.equal(regionKeyLabel('abdomen'), 'Abdomen');
  assert.equal(regionKeyLabel('foreleg_left'), 'Foreleg (Left)');
  assert.equal(regionKeyLabel('foreleg_right'), 'Foreleg (Right)');
  assert.equal(regionKeyLabel('hindleg_left'), 'Hindleg (Left)');
  assert.equal(regionKeyLabel('hindleg_right'), 'Hindleg (Right)');

  // Legacy 6-zone ids (stored on older DB records) still resolve.
  assert.equal(regionKeyLabel('trunk'), 'Trunk');
  assert.equal(regionKeyLabel('hindquarters'), 'Hindquarters');
  assert.equal(regionKeyLabel('foreleg'), 'Foreleg');
  assert.equal(regionKeyLabel('hindleg'), 'Hindleg');
});

test('regionKeyForPoint classifies both sides of forelegs and hindlegs', () => {
  // +X is left, -X is right in the GLB model (outer x bands = legs)
  assert.equal(regionKeyForPoint({ x: 26, y: 10, z: 50 }), 'foreleg_left');
  assert.equal(regionKeyForPoint({ x: -26, y: 10, z: 50 }), 'foreleg_right');
  assert.equal(regionKeyForPoint({ x: 26, y: 10, z: -60 }), 'hindleg_left');
  assert.equal(regionKeyForPoint({ x: -26, y: 10, z: -60 }), 'hindleg_right');
  // Head and neck preserve direct keys
  assert.equal(regionKeyForPoint({ x: 0, y: 200, z: 100 }), 'head');
  assert.equal(regionKeyForPoint({ x: 0, y: 160, z: 70 }), 'neck');
  // Central barrel (n.y ~ 0.546, n.z ~ 0.56) -> abdomen
  assert.equal(regionKeyForPoint({ x: 0, y: 120, z: 0 }), 'abdomen');
  // Lower limb height (y=90 -> n.y ~ 0.41 < legYMax 0.44) -> hindleg_left (n.z ~ 0.56 >= 0.55)
  assert.equal(regionKeyForPoint({ x: 0, y: 90, z: 0 }), 'hindleg_left');
});

test('zoneForRegionKey maps a region key back to a geometry zone for anchoring', () => {
  assert.equal(zoneForRegionKey('foreleg_left'), 'foreleg');
  assert.equal(zoneForRegionKey('hindleg_right'), 'hindleg');
  assert.equal(zoneForRegionKey('back'), 'trunk');
  assert.equal(zoneForRegionKey('abdomen'), 'trunk');
  assert.equal(zoneForRegionKey('head'), 'head');
  // Legacy zone ids pass through
  assert.equal(zoneForRegionKey('trunk'), 'trunk');
  assert.equal(zoneForRegionKey('hindquarters'), 'hindquarters');
});

test('markerZoneLabel prefers the catalog region over the stored zoneId', () => {
  const regions = [
    { id: 5, code: 'FORELEG_LEFT', displayName: 'Foreleg (Left)', modelNodeKey: 'foreleg_left' },
    { id: 7, code: 'HINDLEG_LEFT', displayName: 'Hindleg (Left)', modelNodeKey: 'hindleg_left' },
  ];

  // bodyRegionId wins even when the legacy zoneId disagrees (the old mislabel bug)
  assert.equal(
    markerZoneLabel(regions, { bodyRegionId: 5, zoneId: 'hindleg' }),
    'Foreleg (Left)',
  );

  // No matching region: fall back to the zoneId label (canonical and legacy)
  assert.equal(markerZoneLabel(regions, { bodyRegionId: 99, zoneId: 'foreleg_right' }), 'Foreleg (Right)');
  assert.equal(markerZoneLabel([], { zoneId: 'trunk' }), 'Trunk');

  // Nothing to resolve: undefined, caller shows its own fallback
  assert.equal(markerZoneLabel(regions, { bodyRegionId: null, zoneId: null }), undefined);
});

// Scene-space bounds of urgent-horse.glb — MEASURED at runtime in the 2026-11-10
// session (skinned model: Box3.setFromObject in UrgentHorseInjuryViewer), not the
// static mesh accessor bounds (which are ~100x larger here because the glTF
// applies scale 0.01 to its mesh nodes).
const RUNTIME_BOUNDS = {
  min: { x: -0.45938312480057364, y: -0.004760892664914936, z: 0.48969310926000587 },
  max: { x: 0.4454562192253908, y: 2.22302160402677, z: 3.1294350601816143 },
};

/** Inverse of normalizeScenePoint: builds a scene-local point from anatomy n. */
function localFromN(n: { x: number; y: number; z: number }) {
  const w = RUNTIME_BOUNDS.max.x - RUNTIME_BOUNDS.min.x;
  const h = RUNTIME_BOUNDS.max.y - RUNTIME_BOUNDS.min.y;
  const d = RUNTIME_BOUNDS.max.z - RUNTIME_BOUNDS.min.z;
  return {
    x: RUNTIME_BOUNDS.min.x + (1 - n.x) * w,
    y: RUNTIME_BOUNDS.min.y + n.y * h,
    z: RUNTIME_BOUNDS.max.z - n.z * d,
  };
}

function classify(n: { x: number; y: number; z: number }) {
  return regionKeyForNormalized(normalizeScenePoint(localFromN(n), RUNTIME_BOUNDS));
}

test('zoneForNormalized keeps the tightened blueprint bands', () => {
  assert.equal(zoneForNormalized({ x: 0.5, y: 0.9, z: 0.5 }), 'head');
  assert.equal(zoneForNormalized({ x: 0.5, y: 0.7, z: 0.4 }), 'neck');
  // Withers / front back behind neckZMax (0.46) is now Back instead of Neck
  assert.equal(zoneForNormalized({ x: 0.5, y: 0.7, z: 0.5 }), 'back');
  // Upper shoulder below neckYMin (0.66) and above backYMin (0.62) is Back instead of Neck
  assert.equal(zoneForNormalized({ x: 0.5, y: 0.64, z: 0.4 }), 'back');
  // Upper trunk on the spine/back side of the split is the Back region key.
  assert.equal(zoneForNormalized({ x: 0.5, y: 0.72, z: 0.8 }), 'back');
  // Central barrel (0.42 <= n.z <= 0.72, 0.44 <= n.y <= 0.55) stays abdomen
  assert.equal(zoneForNormalized({ x: 0.5, y: 0.5, z: 0.58 }), 'abdomen');
  // Rear haunch (n.z > 0.72, n.y <= 0.62) is now hindleg instead of abdomen
  assert.equal(zoneForNormalized({ x: 0.5, y: 0.48, z: 0.8 }), 'hindleg');
  assert.equal(zoneForNormalized({ x: 0.15, y: 0.2, z: 0.3 }), 'foreleg');
  assert.equal(zoneForNormalized({ x: 0.85, y: 0.2, z: 0.8 }), 'hindleg');
});

test('blueprint anchors from live-measured clicks classify with tightened neck & abdomen', () => {
  // Trán 0.944 -> Head; mõm bridge 0.860 -> Head boundary holds
  assert.equal(zoneForNormalized({ x: 0.45, y: 0.944, z: 0.36 }), 'head');
  assert.equal(zoneForNormalized({ x: 0.40, y: 0.8602, z: 0.33 }), 'head');
  // Upper neck/crest 0.810, z 0.363 -> Neck
  assert.equal(regionKeyForNormalized({ x: 0.454, y: 0.8096, z: 0.363 }), 'neck');
  // Saddle top/withers 0.728 z 0.650 -> Back
  assert.equal(
    regionKeyForNormalized({ x: 0.5, y: 0.7276, z: 0.6498 }, { x: 0.03, y: 0.69, z: 0.72 }),
    'back',
  );
  // Lateral upper back (n.y = 0.65 > backYMin 0.62, lateral normal fn.y = 0.05) -> Back (not Abdomen)
  assert.equal(
    regionKeyForNormalized({ x: 0.25, y: 0.65, z: 0.58 }, { x: 0.99, y: 0.05, z: 0 }),
    'back',
  );
  // Mid flank 0.588 z 0.601 belly-facing normal -> Abdomen (central barrel)
  assert.equal(
    regionKeyForNormalized({ x: 0.22, y: 0.5875, z: 0.6006 }, { x: 1, y: -0.14, z: 0.03 }),
    'abdomen',
  );
  // Rear thigh/haunch 0.366 z 0.844 -> Hindleg (was Abdomen)
  assert.equal(zoneForNormalized({ x: 0.69, y: 0.366, z: 0.844 }), 'hindleg');
  // Lower hindleg 0.111 x 0.744 z 0.811 -> Hindleg (Right) (was Abdomen)
  assert.equal(regionKeyForNormalized({ x: 0.744, y: 0.111, z: 0.811 }), 'hindleg_right');
});

test('all 4 legs and front lower shoulder classify as legs at every normal angle', () => {
  // Front lower shoulder/chest (n.z = 0.309 < 0.42, n.y = 0.478 < 0.52) -> Foreleg (Left)
  assert.equal(
    regionKeyForNormalized({ x: 0.33, y: 0.478, z: 0.309 }, { x: 0.68, y: -0.49, z: 0.54 }),
    'foreleg_left',
  );
  // Central barrel at chest height (n.z = 0.52 in [0.42, 0.72], n.y = 0.486 in [0.44, 0.62]) -> Abdomen
  assert.equal(
    regionKeyForNormalized({ x: 0.222, y: 0.4859, z: 0.52 }, { x: 0.9997, y: 0.025, z: 0.0003 }),
    'abdomen',
  );
  // Foreleg shaft: lateral face -> Foreleg (Right)
  assert.equal(
    regionKeyForNormalized({ x: 0.6217, y: 0.2976, z: 0.4033 }, { x: 0.98, y: 0.1, z: 0.2 }),
    'foreleg_right',
  );
  // Foreleg shaft clicked from front/diagonal (|fn.x| <= 0.92) still -> Foreleg (Left)
  assert.equal(
    regionKeyForNormalized({ x: 0.38, y: 0.28, z: 0.40 }, { x: 0.3, y: 0.0, z: 0.95 }),
    'foreleg_left',
  );
  // Hindleg shaft clicked from rear/diagonal (|fn.x| <= 0.92) -> Hindleg (Right)
  assert.equal(
    regionKeyForNormalized({ x: 0.65, y: 0.25, z: 0.78 }, { x: 0.2, y: -0.1, z: -0.97 }),
    'hindleg_right',
  );
});

test('normalizeScenePoint maps scene-local clicks to anatomical 8 regions', () => {
  // Head: trán 0.944 / crest-găm click still resolves to the head band.
  assert.equal(classify({ x: 0.45, y: 0.944, z: 0.36 }), 'head');
  // Neck: upper neck/crest anchor 0.810 / z 0.363 (was Head before blueprint)
  assert.equal(classify({ x: 0.454, y: 0.8096, z: 0.363 }), 'neck');
  // Back: saddle top/withers anchor n.y 0.728 / n.z 0.650 with an up face
  assert.equal(
    regionKeyForNormalized(
      normalizeScenePoint(localFromN({ x: 0.5, y: 0.7276, z: 0.6498 }), RUNTIME_BOUNDS),
      { x: 0.03, y: 0.69, z: 0.72 },
    ),
    'back',
  );
  // Abdomen: mid flank anchor n.y 0.588 / n.z 0.601 belly face
  assert.equal(
    regionKeyForNormalized(
      normalizeScenePoint(localFromN({ x: 0.22, y: 0.5875, z: 0.6006 }), RUNTIME_BOUNDS),
      { x: 1, y: -0.14, z: 0.03 },
    ),
    'abdomen',
  );
  // Foreleg left/hindleg right: outer x bands on the 2D blueprint
  assert.equal(classify({ x: 0.1, y: 0.2, z: 0.5 }), 'foreleg_left');
  assert.equal(classify({ x: 0.9, y: 0.2, z: 0.5 }), 'foreleg_right');
});

test('trunk hits split Back vs Abdomen by surface normal, not only height', () => {
  // Upper-trunk point (n.y > trunkYMin): same position, different faces.
  const withers = { x: 0.5, y: 0.6, z: 0.7 };
  // Face pointing forward/sideways (the surface a vet clicks on the chest)
  assert.equal(regionKeyForNormalized(withers, { x: 0, y: 0.1, z: 1 }), 'abdomen');
  // Face pointing up (the topline under the saddle)
  assert.equal(regionKeyForNormalized(withers, { x: 0, y: 1, z: 0 }), 'back');
  // no normal fallback: the spine-side bucket per the blueprint (topline assumption)
  assert.equal(regionKeyForNormalized({ x: 0.5, y: 0.7, z: 0.8 }), 'back');
  assert.equal(regionKeyForNormalized({ x: 0.5, y: 0.6, z: 0.7 }), 'back');
});
