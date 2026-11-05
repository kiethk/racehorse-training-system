import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import {
  isAdministeredByGroom,
  parseTreatmentInstructions,
} from '../../care/lib/treatmentInstructions.ts';

describe('Treatment Instructions Parser (CP-1)', () => {
  it('correctly parses structured JSON array into prescription items', () => {
    const jsonStr = JSON.stringify([
      {
        medication_name: 'Doxycycline',
        dosage: '10 mg/kg',
        frequency: 'Twice daily',
        route: 'ORAL',
        duration: 7,
        notes: 'Give with morning feed.',
      },
      {
        medication_name: 'Vitamin B Complex',
        dosage: '10 mL',
        frequency: 'Once daily',
        route: 'INJECTION_IM',
        duration: 5,
        notes: 'Vet administration only.',
      },
    ]);

    const result = parseTreatmentInstructions(jsonStr);
    assert.equal(result.isJson, true);
    assert.equal(result.items.length, 2);

    assert.equal(result.items[0].medication_name, 'Doxycycline');
    assert.equal(result.items[0].dosage, '10 mg/kg');
    assert.equal(result.items[0].frequency, 'Twice daily');
    assert.equal(result.items[0].route, 'ORAL');
    assert.equal(result.items[0].duration, 7);
    assert.equal(result.items[0].notes, 'Give with morning feed.');

    assert.equal(result.items[1].medication_name, 'Vitamin B Complex');
    assert.equal(result.items[1].dosage, '10 mL');
    assert.equal(result.items[1].route, 'INJECTION_IM');
    assert.equal(result.items[1].duration, 5);
  });

  it('tolerates camelCase keys such as medicationName', () => {
    const jsonStr = JSON.stringify([
      {
        medicationName: 'Omeprazole Paste',
        dosage: '1 mg/kg',
        frequency: 'Once daily',
        route: 'FEED_ADDITIVE',
        duration: 14,
        notes: 'Stomach protection.',
      },
    ]);

    const result = parseTreatmentInstructions(jsonStr);
    assert.equal(result.isJson, true);
    assert.equal(result.items.length, 1);
    assert.equal(result.items[0].medication_name, 'Omeprazole Paste');
    assert.equal(result.items[0].route, 'FEED_ADDITIVE');
  });

  it('falls back safely to plain-text when instructions is legacy unstructured text', () => {
    const legacyText = 'Box rest for 3 days and cold hosing 20 minutes twice daily.';
    const result = parseTreatmentInstructions(legacyText);

    assert.equal(result.isJson, false);
    assert.equal(result.items.length, 0);
    assert.equal(result.rawText, legacyText);
  });

  it('handles null, undefined, and empty string without throwing', () => {
    assert.deepEqual(parseTreatmentInstructions(null), { isJson: false, items: [], rawText: '' });
    assert.deepEqual(parseTreatmentInstructions(undefined), { isJson: false, items: [], rawText: '' });
    assert.deepEqual(parseTreatmentInstructions('   '), { isJson: false, items: [], rawText: '' });
  });

  it('handles malformed JSON strings gracefully as plain text fallback', () => {
    const brokenJson = '[{"medication_name": "Broken"';
    const result = parseTreatmentInstructions(brokenJson);

    assert.equal(result.isJson, false);
    assert.equal(result.items.length, 0);
    assert.equal(result.rawText, brokenJson);
  });
});

describe('Route Classification (Groom vs Vet)', () => {
  it('identifies groom-administered routes as true', () => {
    assert.equal(isAdministeredByGroom('ORAL'), true);
    assert.equal(isAdministeredByGroom('oral'), true);
    assert.equal(isAdministeredByGroom('FEED_ADDITIVE'), true);
    assert.equal(isAdministeredByGroom('TOPICAL'), true);
  });

  it('identifies vet-administered injection routes as false', () => {
    assert.equal(isAdministeredByGroom('INJECTION_IV'), false);
    assert.equal(isAdministeredByGroom('INJECTION_IM'), false);
    assert.equal(isAdministeredByGroom('IV'), false);
    assert.equal(isAdministeredByGroom('IM'), false);
  });
});

