import { test } from 'node:test';
import assert from 'node:assert/strict';
import { normalizeDueItem, unwrapArray, unwrapData } from '../../care/lib/unwrapResponse.ts';

test('unwrapArray: extracts array directly from array input', () => {
  const input = [{ id: 1 }, { id: 2 }];
  assert.deepEqual(unwrapArray(input), [{ id: 1 }, { id: 2 }]);
});

test('unwrapArray: extracts array from ApiResponse shape { data: [...] }', () => {
  const input = { data: [{ id: 1 }, { id: 2 }] };
  assert.deepEqual(unwrapArray(input), [{ id: 1 }, { id: 2 }]);
});

test('unwrapArray: extracts array from nested page shape { data: { content: [...] } }', () => {
  const input = { data: { content: [{ id: 10 }] } };
  assert.deepEqual(unwrapArray(input), [{ id: 10 }]);
});

test('unwrapArray: extracts array from direct page shape { content: [...] }', () => {
  const input = { content: [{ id: 20 }] };
  assert.deepEqual(unwrapArray(input), [{ id: 20 }]);
});

test('unwrapArray: safely returns empty array for null, undefined, or unexpected types', () => {
  assert.deepEqual(unwrapArray(null), []);
  assert.deepEqual(unwrapArray(undefined), []);
  assert.deepEqual(unwrapArray({}), []);
  assert.deepEqual(unwrapArray({ data: null }), []);
  assert.deepEqual(unwrapArray('error string'), []);
});

test('unwrapData: unwraps data property or falls back to object', () => {
  assert.deepEqual(unwrapData({ data: { count: 5 } }), { count: 5 });
  assert.deepEqual(unwrapData({ count: 5 }), { count: 5 });
});

test('normalizeDueItem: preserves nested protocol item intact', () => {
  const nested = {
    horseId: 1,
    horseName: 'Alpha',
    horseStatus: 'ELIGIBLE',
    protocol: {
      horseId: 1,
      protocolId: 10,
      code: 'EI',
      name: 'Equine Influenza',
      category: 'VACCINE',
      mandatory: true,
      lastDoseOn: null,
      nextDueOn: null,
      status: 'MISSING',
    },
  };
  assert.deepEqual(normalizeDueItem(nested), nested);
});

test('normalizeDueItem: converts flat protocol properties into nested protocol structure', () => {
  const flat = {
    horseId: 2,
    horseName: 'Bravo',
    horseStatus: 'CANDIDATE',
    protocolId: 20,
    code: 'TET',
    name: 'Tetanus',
    category: 'VACCINE',
    mandatory: true,
    lastDoseOn: '2026-10-01',
    nextDueOn: '2026-10-20',
    status: 'DUE_SOON',
  };
  const normalized = normalizeDueItem(flat);
  assert.equal(normalized.horseId, 2);
  assert.equal(normalized.protocol.protocolId, 20);
  assert.equal(normalized.protocol.status, 'DUE_SOON');
  assert.equal(normalized.protocol.name, 'Tetanus');
});
