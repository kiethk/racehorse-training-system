import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { filterAreas } from '../shared/lib/areaSearch.ts';
import type { Area } from '../types';

const area = (id: number, code: string, name: string, type: Area['type'] = 'REGULAR'): Area =>
  ({ id, code, name, type, trainerId: null });

const areas = [
  area(1, 'A', 'Regular Area A'),
  area(2, 'B', 'North Barn'),
  area(3, 'K', 'Regular Area K'),
  area(4, 'Q-A', 'Quarantine A', 'QUARANTINE'),
];

const codes = (query: string) => filterAreas(areas, query).map((group) => [group.type, group.areas.map((a) => a.code)]);

describe('Area search', () => {
  it('returns every area, regular before quarantine, when the query is empty', () => {
    assert.deepEqual(codes('  '), [['REGULAR', ['A', 'B', 'K']], ['QUARANTINE', ['Q-A']]]);
  });

  it('matches the code or the name, ignoring case', () => {
    assert.deepEqual(codes('north'), [['REGULAR', ['B']]]);
    assert.deepEqual(codes('q-a'), [['QUARANTINE', ['Q-A']]]);
    assert.deepEqual(codes('K'), [['REGULAR', ['K']]]);
  });

  it('drops empty groups and returns nothing when nothing matches', () => {
    assert.deepEqual(codes('regular'), [['REGULAR', ['A', 'K']]]);
    assert.deepEqual(codes('zzz'), []);
  });
});
