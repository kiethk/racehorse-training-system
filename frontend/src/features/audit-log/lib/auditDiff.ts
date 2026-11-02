import type { AuditDetailPayload } from '../types';

export interface FieldDiff {
  field: string;
  beforeValue: string;
  afterValue: string;
  isChanged: boolean;
}

export interface ParsedAuditDetail {
  isJson: boolean;
  raw: string;
  summary?: string;
  reason?: string;
  module?: string;
  targetUrl?: string;
  diffs: FieldDiff[];
}

export function resolveEntityUrl(
  entityName?: string | null,
  entityId?: number | null
): string | null {
  if (!entityName) return null;
  const normalized = entityName.toUpperCase();

  if (normalized === 'ADMISSIONAPPLICATION' || normalized === 'ADMISSION') {
    return entityId != null ? `/manager/admissions/${entityId}` : '/manager/admissions';
  }

  if (normalized === 'HORSE') {
    return entityId != null ? `/manager/horses/${entityId}` : '/manager/horses';
  }

  if (normalized === 'STABLESTALL' || normalized === 'STALL' || normalized === 'AREA') {
    return '/manager/stable';
  }

  if (normalized === 'USER' || normalized === 'STAFF') {
    return entityId != null ? `/manager/accounts?userId=${entityId}` : '/manager/accounts';
  }

  return null;
}

function formatValue(val: unknown): string {
  if (val === null || val === undefined) return 'None';
  if (typeof val === 'object') return JSON.stringify(val);
  return String(val);
}

export function parseAuditDetails(
  details: string | null,
  entityName?: string | null,
  entityId?: number | null
): ParsedAuditDetail {
  const rawText = details || '';
  const fallbackUrl = resolveEntityUrl(entityName, entityId) ?? undefined;

  if (!details) {
    return {
      isJson: false,
      raw: '',
      targetUrl: fallbackUrl,
      diffs: [],
    };
  }

  const trimmed = details.trim();
  if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
    try {
      const parsed = JSON.parse(trimmed) as AuditDetailPayload;
      if (parsed && typeof parsed === 'object') {
        const diffs: FieldDiff[] = [];
        const beforeObj = parsed.before || {};
        const afterObj = parsed.after || {};

        const allKeys = Array.from(
          new Set([...Object.keys(beforeObj), ...Object.keys(afterObj)])
        );

        for (const key of allKeys) {
          const beforeVal = beforeObj[key];
          const afterVal = afterObj[key];
          const beforeStr = formatValue(beforeVal);
          const afterStr = formatValue(afterVal);

          diffs.push({
            field: key,
            beforeValue: beforeStr,
            afterValue: afterStr,
            isChanged: beforeStr !== afterStr,
          });
        }

        return {
          isJson: true,
          raw: details,
          summary: parsed.summary,
          reason: parsed.reason,
          module: parsed.module,
          targetUrl: parsed.targetUrl || fallbackUrl,
          diffs,
        };
      }
    } catch {
      // Fall through to plain text
    }
  }

  return {
    isJson: false,
    raw: details,
    targetUrl: fallbackUrl,
    diffs: [],
  };
}
