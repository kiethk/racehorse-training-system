import type { AuditLogItem, AuditDetailPayload } from '../types';
import { resolveLogModule } from './auditDiff.ts';

function escapeCsvField(val: unknown): string {
  if (val == null) return '""';
  const str = String(val).replace(/"/g, '""');
  return `"${str}"`;
}

export function extractAuditSummary(details: string | null): string {
  if (!details) return '';
  const trimmed = details.trim();
  if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
    try {
      const parsed = JSON.parse(trimmed) as AuditDetailPayload;
      if (parsed && typeof parsed === 'object' && parsed.summary) {
        return parsed.summary;
      }
    } catch {
      // Fallback to raw string
    }
  }
  return details;
}

export function exportAuditLogsToCsv(items: AuditLogItem[]): string {
  const headers = [
    'Timestamp',
    'Actor',
    'Role',
    'Module',
    'Action',
    'Target Entity',
    'Target ID',
    'Summary / Details',
  ];

  const rows = items.map((item) => {
    const actor = item.actorName || item.actorEmail || 'System';
    const role = item.actorRole || '-';
    const moduleName = resolveLogModule(item.module, item.entityName, item.requestPath, item.action);
    const action = item.action || `${item.httpMethod} ${item.requestPath}`;
    const entityName = item.entityName || '-';
    const entityId = item.entityId != null ? String(item.entityId) : '-';
    const summary = extractAuditSummary(item.details);

    return [
      escapeCsvField(item.createdAt),
      escapeCsvField(actor),
      escapeCsvField(role),
      escapeCsvField(moduleName),
      escapeCsvField(action),
      escapeCsvField(entityName),
      escapeCsvField(entityId),
      escapeCsvField(summary),
    ].join(',');
  });

  return [headers.map((h) => `"${h}"`).join(','), ...rows].join('\r\n');
}

export function downloadAuditLogsCsv(items: AuditLogItem[], filename = 'audit-logs.csv'): void {
  if (typeof window === 'undefined') return;

  const csvContent = exportAuditLogsToCsv(items);
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);

  const link = document.createElement('a');
  link.setAttribute('href', url);
  link.setAttribute('download', filename);
  link.style.visibility = 'hidden';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

