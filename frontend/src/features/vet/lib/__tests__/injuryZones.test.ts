import { test } from 'node:test';
import assert from 'node:assert/strict';
import { zoneForPoint, zoneById, BODY_ZONES, normalizePoint } from '../injuryZones.ts';

test('normalizePoint clamps to 0..1', () => {
  const n = normalizePoint({ x: 0, y: 219.76, z: 0 });
  assert.ok(Math.abs(n.y - 1) < 0.01);
  const n2 = normalizePoint({ x: -100, y: 0, z: 0 });
  assert.equal(n2.x, 0);
});

test('zoneForPoint maps model coords to zones', () => {
  // head: high y
  assert.equal(zoneForPoint({ x: 0, y: 200, z: 0 }), 'head');
  // neck
  assert.equal(zoneForPoint({ x: 0, y: 160, z: 0 }), 'neck');
  // trunk
  assert.equal(zoneForPoint({ x: 10, y: 100, z: 0 }), 'trunk');
  // hindquarters
  assert.equal(zoneForPoint({ x: -25, y: 100, z: 0 }), 'hindquarters');
  // foreleg
  assert.equal(zoneForPoint({ x: 10, y: 10, z: 0 }), 'foreleg');
  // hindleg
  assert.equal(zoneForPoint({ x: -25, y: 10, z: 0 }), 'hindleg');
});

test('zoneById returns defined zone', () => {
  assert.equal(zoneById('head')?.label, 'Head');
  assert.equal(zoneById('nope'), undefined);
});

test('BODY_ZONES has unique ids', () => {
  const ids = BODY_ZONES.map((z) => z.id);
  assert.equal(new Set(ids).size, ids.length);
});