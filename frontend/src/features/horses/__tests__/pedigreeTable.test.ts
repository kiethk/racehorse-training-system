import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { formatHorseSex } from '../lib/sex.ts';
import type { Horse } from '@/features/stable/types/index.ts';
import type { HorsePedigree } from '../types/index.ts';

describe('HorseOverviewPedigreeTable data helpers', () => {
  it('formats horse sex correctly for pedigree display', () => {
    assert.equal(formatHorseSex('MALE'), 'Male');
    assert.equal(formatHorseSex('FEMALE'), 'Female');
    assert.equal(formatHorseSex(null), 'Not recorded');
    assert.equal(formatHorseSex(undefined), 'Not recorded');
  });

  it('correctly maps pedigree fallback registration and ancestry', () => {
    const mockHorse: Horse = {
      id: 42,
      name: 'Thunderbolt',
      breed: 'Thoroughbred',
      sex: 'MALE',
      dateOfBirth: '2022-03-15',
      currentStatus: 'ELIGIBLE',
      currentStallId: 5,
      ownerId: 2,
      registrationNumber: null,
      registryName: null,
    };

    const mockPedigree: HorsePedigree = {
      id: 10,
      horse: {
        id: 42,
        name: 'Thunderbolt',
        breed: 'Thoroughbred',
        dateOfBirth: '2022-03-15',
        currentStatus: 'ELIGIBLE',
      },
      sire: {
        id: 1,
        name: 'Storm Cat',
        breed: 'Thoroughbred',
        dateOfBirth: '2010-01-01',
        currentStatus: 'RETIRED',
      },
      dam: null,
      damName: 'Lady Wind',
      damRegistrationNumber: '528003201209999',
      registrationNumber: '528003202200042',
      registryName: 'Weatherbys',
      pedigreeNotes: 'Grade 1 championship lineage',
    };

    // Registration number falls back to pedigree when horse.registrationNumber is null
    const regNo = mockHorse.registrationNumber || mockPedigree.registrationNumber || 'Not registered';
    assert.equal(regNo, '528003202200042');

    // Registry name falls back to pedigree when horse.registryName is null
    const registry = mockHorse.registryName || mockPedigree.registryName || 'Not recorded';
    assert.equal(registry, 'Weatherbys');

    // Sire resolves to in-system sire
    const sireName = mockPedigree.sire?.name || mockPedigree.sireName || 'Not recorded';
    assert.equal(sireName, 'Storm Cat');

    // Dam resolves to external dam
    const damName = mockPedigree.dam?.name || mockPedigree.damName || 'Not recorded';
    assert.equal(damName, 'Lady Wind');
    assert.equal(mockPedigree.damRegistrationNumber, '528003201209999');
  });
});
