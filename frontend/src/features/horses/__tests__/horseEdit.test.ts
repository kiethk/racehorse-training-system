import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import {
  buildSlotStates,
  diffFields,
  HORSE_DOCUMENT_TYPES,
  uelnFormatError,
  validateDocumentFile,
} from '../lib/horseEdit.ts';
import type { HorseDocumentSlot, HorseEditFields } from '../types/index.ts';

const fields = (over: Partial<HorseEditFields> = {}): HorseEditFields => ({
  name: 'Mercury', breed: 'Arabian', sireName: null, sireRegistrationNumber: null,
  damName: null, damRegistrationNumber: null, ...over,
});

const slot = (documentType: HorseDocumentSlot['documentType'], fileUrl: string | null): HorseDocumentSlot => ({
  documentType, source: fileUrl ? 'ADMISSION' : null, documentId: fileUrl ? 1 : null,
  originalFileName: fileUrl ? 'file.pdf' : null, fileUrl, updatedAt: null,
});

describe('Horse document slots', () => {
  it('gives every document type a slot, empty when there is no file', () => {
    const states = buildSlotStates([slot('HORSE_PHOTO', '/api/x')], {});
    assert.equal(states.length, HORSE_DOCUMENT_TYPES.length);
    assert.equal(states[0].kind, 'current');
    assert.ok(states.slice(1).every((s) => s.kind === 'empty'));
  });

  it('shows a chosen file as pending and remembers what it replaces', () => {
    const existing = slot('HORSE_PHOTO', '/api/x');
    const [photo, registration] = buildSlotStates([existing], {
      HORSE_PHOTO: { name: 'new.png' },
      REGISTRATION_DOCUMENT: { name: 'papers.pdf' },
    });
    assert.deepEqual(photo, { kind: 'pending', type: 'HORSE_PHOTO', fileName: 'new.png', replaces: existing });
    assert.equal(registration.kind, 'pending');
    assert.equal(registration.kind === 'pending' && registration.replaces, null);
  });
});

describe('Horse edit diff', () => {
  it('lists only changed fields, ignoring whitespace and UELN case', () => {
    const changes = diffFields(fields(), fields({ name: ' Mercury ', sireRegistrationNumber: '999999sir000001' }));
    assert.deepEqual(changes, [{ field: 'sireRegistrationNumber', label: 'Sire UELN', from: null, to: '999999SIR000001' }]);
  });

  it('reports nothing when the form is unchanged', () => {
    assert.deepEqual(diffFields(fields(), fields({ breed: 'Arabian  ' })), []);
  });
});

describe('Horse edit validation', () => {
  it('accepts both UELN formats and flags anything else', () => {
    assert.equal(uelnFormatError('250001AB1234567'), null);
    assert.equal(uelnFormatError('FRA123456789012'), null);
    assert.equal(uelnFormatError(''), null);
    assert.notEqual(uelnFormatError('ABC'), null);
  });

  it('mirrors the server file rules', () => {
    assert.equal(validateDocumentFile('REGISTRATION_DOCUMENT', { type: 'application/pdf', size: 1 }), null);
    assert.notEqual(validateDocumentFile('HORSE_PHOTO', { type: 'application/pdf', size: 1 }), null);
    assert.notEqual(validateDocumentFile('HEALTH_CERTIFICATE', { type: 'text/plain', size: 1 }), null);
    assert.notEqual(validateDocumentFile('HEALTH_CERTIFICATE', { type: 'image/png', size: 11 * 1024 * 1024 }), null);
  });
});
