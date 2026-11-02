import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { exportAuditLogsToCsv } from '../auditExport.ts';
import type { AuditLogItem } from '../../types';

describe('Audit Log CSV Export', () => {
  const sampleLogs: AuditLogItem[] = [
    {
      id: 1,
      actorUserId: 10,
      actorName: 'John Doe',
      actorEmail: 'john@example.com',
      actorRole: 'CLUB_MANAGER',
      httpMethod: 'POST',
      requestPath: '/api/manager/admissions/1/approve',
      statusCode: 200,
      action: 'MANAGER_ADMISSION_APPROVAL',
      entityName: 'AdmissionApplication',
      entityId: 101,
      module: 'ADMISSIONS',
      details: JSON.stringify({
        summary: 'Approved admission with "Special" notes, and allocated stall A-01',
        reason: 'Passed all exams',
        module: 'ADMISSIONS',
        before: { status: 'PENDING' },
        after: { status: 'APPROVED' }
      }),
      success: true,
      createdAt: '2026-11-02T10:00:00Z',
    },
    {
      id: 2,
      actorUserId: null,
      actorName: null,
      actorEmail: null,
      actorRole: null,
      httpMethod: 'GET',
      requestPath: '/api/health',
      statusCode: 200,
      action: null,
      entityName: null,
      entityId: null,
      module: 'SYSTEM',
      details: 'Health check OK',
      success: true,
      createdAt: '2026-11-02T10:05:00Z',
    }
  ];

  it('generates the standard CSV header row', () => {
    const csv = exportAuditLogsToCsv([]);
    const lines = csv.trim().split('\r\n');
    assert.equal(lines[0], '"Timestamp","Actor","Role","Module","Action","Target Entity","Target ID","Summary / Details"');
  });

  it('extracts summary from JSON payload and escapes embedded quotes and commas', () => {
    const csv = exportAuditLogsToCsv([sampleLogs[0]]);
    const lines = csv.trim().split('\r\n');
    assert.equal(lines.length, 2);
    assert.ok(lines[1].includes('"John Doe"'));
    assert.ok(lines[1].includes('"CLUB_MANAGER"'));
    assert.ok(lines[1].includes('"ADMISSIONS"'));
    assert.ok(lines[1].includes('"MANAGER_ADMISSION_APPROVAL"'));
    assert.ok(lines[1].includes('"101"'));
    // Escaped double quotes inside summary
    assert.ok(lines[1].includes('Approved admission with ""Special"" notes, and allocated stall A-01'));
  });

  it('falls back to raw details when details is not JSON or does not have summary', () => {
    const csv = exportAuditLogsToCsv([sampleLogs[1]]);
    const lines = csv.trim().split('\r\n');
    assert.equal(lines.length, 2);
    assert.ok(lines[1].includes('"System"'));
    assert.ok(lines[1].includes('"Health check OK"'));
  });
});

