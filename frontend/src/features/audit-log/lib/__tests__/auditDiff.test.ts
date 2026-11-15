import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { parseAuditDetails, resolveEntityUrl, resolveLogModule, resolveValueLink } from '../auditDiff.ts';

describe('Audit Diff Parser', () => {
  it('parses structured JSON with before and after states and detects changes', () => {
    const rawJson = JSON.stringify({
      version: 1,
      module: 'HORSES',
      summary: 'Status changed from ACTIVE to RESTING',
      reason: 'Scheduled rest period',
      targetUrl: '/manager/horses/42',
      before: {
        status: 'ACTIVE',
        healthStatus: 'HEALTHY',
        stallId: 10
      },
      after: {
        status: 'RESTING',
        healthStatus: 'HEALTHY',
        stallId: 10
      }
    });

    const result = parseAuditDetails(rawJson, 'Horse', 42);
    assert.equal(result.isJson, true);
    assert.equal(result.summary, 'Status changed from ACTIVE to RESTING');
    assert.equal(result.reason, 'Scheduled rest period');
    assert.equal(result.module, 'HORSES');
    assert.equal(result.targetUrl, '/manager/horses/42');
    assert.equal(result.diffs.length, 3);

    const statusDiff = result.diffs.find((d) => d.field === 'status');
    assert.ok(statusDiff);
    assert.equal(statusDiff.beforeValue, 'ACTIVE');
    assert.equal(statusDiff.afterValue, 'RESTING');
    assert.equal(statusDiff.isChanged, true);

    const healthDiff = result.diffs.find((d) => d.field === 'healthStatus');
    assert.ok(healthDiff);
    assert.equal(healthDiff.beforeValue, 'HEALTHY');
    assert.equal(healthDiff.afterValue, 'HEALTHY');
    assert.equal(healthDiff.isChanged, false);
  });

  it('handles non-JSON plain text gracefully', () => {
    const result = parseAuditDetails('User logged in from IP 127.0.0.1', 'User', 5);
    assert.equal(result.isJson, false);
    assert.equal(result.raw, 'User logged in from IP 127.0.0.1');
    assert.equal(result.diffs.length, 0);
    assert.equal(result.summary, undefined);
    assert.equal(result.targetUrl, '/manager/accounts?userId=5');
  });

  it('parses plain text failure with reason gracefully', () => {
    const result = parseAuditDetails('Manager created a new account (Attempt failed) - Reason: Certification number already in use', 'User', null);
    assert.equal(result.isJson, false);
    assert.equal(result.summary, 'Manager created a new account (Attempt failed)');
    assert.equal(result.reason, 'Certification number already in use');
    assert.equal(result.diffs.length, 0);
  });

  it('parses structured JSON failure without diffs and preserves reason and summary', () => {
    const json = JSON.stringify({
      summary: 'Manager created a new account (Attempt failed)',
      reason: 'Email already in use',
      module: 'STAFF'
    });
    const result = parseAuditDetails(json, 'User', null);
    assert.equal(result.isJson, true);
    assert.equal(result.summary, 'Manager created a new account (Attempt failed)');
    assert.equal(result.reason, 'Email already in use');
    assert.equal(result.raw, 'Manager created a new account (Attempt failed)');
    assert.equal(result.diffs.length, 0);
  });

  it('resolves universal entity redirection URLs according to system rules', () => {
    assert.equal(resolveEntityUrl('AdmissionApplication', 99), '/manager/admissions/99');
    assert.equal(resolveEntityUrl('Horse', 42), '/manager/horses/42');
    assert.equal(resolveEntityUrl('StableStall', 5), '/manager/stable?stallId=5');
    assert.equal(resolveEntityUrl('Area', 2), '/manager/stable?areaId=2');
    assert.equal(resolveEntityUrl('User', 10), '/manager/accounts?userId=10');
    assert.equal(resolveEntityUrl('STAFF', 12), '/manager/accounts?userId=12');
    assert.equal(resolveEntityUrl('Unknown', 1), null);
    assert.equal(resolveEntityUrl(null, null), null);
  });

  it('resolves module accurately using fallback heuristics when module is empty or SYSTEM', () => {
    assert.equal(resolveLogModule('ADMISSIONS', null, null, null), 'ADMISSIONS');
    assert.equal(resolveLogModule('SYSTEM', 'Horse', null, null), 'HORSES');
    assert.equal(resolveLogModule('SYSTEM', null, '/api/horses/10', null), 'HORSES');
    assert.equal(resolveLogModule(null, null, '/api/admissions/4', null), 'ADMISSIONS');
    assert.equal(resolveLogModule(null, null, '/api/stalls/occupancy', null), 'STABLE');
    assert.equal(resolveLogModule(null, null, null, 'CARE_SCHEDULE_RESCHEDULED'), 'MEDICAL');
    assert.equal(resolveLogModule(null, null, null, null), 'SYSTEM');
  });

  it('resolves value entity links for trainers, staff, stalls, and horses in changes diffs', () => {
    assert.equal(resolveValueLink('assignedTrainer', 'Head Trainer (#2)'), '/manager/accounts?userId=2');
    assert.equal(resolveValueLink('assignedTrainer', 'Head Trainer'), '/manager/accounts?search=Head%20Trainer');
    assert.equal(resolveValueLink('assignedVet', 'Dr. Sarah Jenkins'), '/manager/accounts?search=Dr.%20Sarah%20Jenkins');
    assert.equal(resolveValueLink('stallCode', 'A-04'), '/manager/stable?stallCode=A-04');
    assert.equal(resolveValueLink('occupantHorse', 'Thunder Spirit (#1)'), '/manager/horses/1');
    assert.equal(resolveValueLink('status', 'ELIGIBLE'), null);
    assert.equal(resolveValueLink('reason', 'None'), null);
    assert.equal(resolveValueLink('field', ''), null);
  });
});

