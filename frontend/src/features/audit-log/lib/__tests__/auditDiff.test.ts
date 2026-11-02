import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { parseAuditDetails, resolveEntityUrl, resolveLogModule } from '../auditDiff.ts';

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

  it('resolves universal entity redirection URLs according to system rules', () => {
    assert.equal(resolveEntityUrl('AdmissionApplication', 99), '/manager/admissions/99');
    assert.equal(resolveEntityUrl('Horse', 42), '/manager/horses/42');
    assert.equal(resolveEntityUrl('StableStall', 5), '/manager/stable');
    assert.equal(resolveEntityUrl('Area', 2), '/manager/stable');
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
});

