import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { validateStaffCreation } from '../lib/creation.ts';
import type { StaffCreationRequest } from '../types/index';
const valid: StaffCreationRequest = { role: 'GROOM', fullName: 'Alex Carter', email: 'alex@rtms.com', password: 'Test@2026' };
describe('Staff creation validation', () => {
  it('accepts a Groom without professional credentials', () => assert.deepEqual(validateStaffCreation(valid), {}));
  it('rejects invalid email and blank required fields', () => {
    const errors = validateStaffCreation({ ...valid, fullName: ' ', email: 'invalid', password: ' ' });
    assert.ok(errors.fullName); assert.ok(errors.email); assert.ok(errors.password);
  });
  it('accepts the phone boundary and rejects oversized input', () => {
    assert.equal(validateStaffCreation({ ...valid, phone: '1'.repeat(20) }).phone, undefined);
    assert.ok(validateStaffCreation({ ...valid, phone: '1'.repeat(21) }).phone);
  });
  it('checks UTF-8 password bytes rather than character count alone', () => {
    assert.equal(validateStaffCreation({ ...valid, password: 'é'.repeat(36) }).password, undefined);
    assert.ok(validateStaffCreation({ ...valid, password: 'é'.repeat(37) }).password);
  });
  it('requires only the credentials of the selected role', () => {
    assert.ok(validateStaffCreation({ ...valid, role: 'HEAD_TRAINER' }).certificationNumber);
    assert.ok(validateStaffCreation({ ...valid, role: 'VETERINARIAN' }).licenseNumber);
    assert.deepEqual(validateStaffCreation({ ...valid, role: 'HEAD_TRAINER', certificationNumber: 'TR-1' }), {});
  });
  it('rejects future issue dates and accepts today', () => {
    assert.ok(validateStaffCreation({ ...valid, role: 'VETERINARIAN', licenseNumber: 'VT-1', licenseIssuedDate: '2026-10-19' }, '2026-10-18').licenseIssuedDate);
    assert.equal(validateStaffCreation({ ...valid, role: 'HEAD_TRAINER', certificationNumber: 'TR-1', certificationIssuedDate: '2026-10-18' }, '2026-10-18').certificationIssuedDate, undefined);
  });
});
