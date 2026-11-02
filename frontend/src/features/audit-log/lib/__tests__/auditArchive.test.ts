import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import type { AuditArchiveStatus, AuditPurgeRequest } from '../../types.ts';

describe('Audit Archive & Purge Data Types and Invariants', () => {
  it('correctly validates AuditArchiveStatus requiresMaintenance flag', () => {
    const statusWithAgedLogs: AuditArchiveStatus = {
      totalCount: 1500,
      agedCount: 120,
      oldestLogDate: '2026-09-01T10:00:00Z',
      requiresMaintenance: true,
    };
    assert.equal(statusWithAgedLogs.requiresMaintenance, true);
    assert.equal(statusWithAgedLogs.agedCount > 0, true);

    const cleanStatus: AuditArchiveStatus = {
      totalCount: 200,
      agedCount: 0,
      oldestLogDate: null,
      requiresMaintenance: false,
    };
    assert.equal(cleanStatus.requiresMaintenance, false);
    assert.equal(cleanStatus.agedCount, 0);
  });

  it('constructs valid AuditPurgeRequest cutoff ranges', () => {
    const now = new Date('2026-11-02T12:00:00Z');
    const thirtyDaysAgo = new Date(now);
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

    const request: AuditPurgeRequest = {
      beforeDate: thirtyDaysAgo.toISOString(),
      reason: 'Routine quarterly cleanup',
    };

    assert.ok(request.beforeDate);
    assert.equal(request.reason, 'Routine quarterly cleanup');
    assert.equal(new Date(request.beforeDate!).getTime() < now.getTime(), true);
  });
});
