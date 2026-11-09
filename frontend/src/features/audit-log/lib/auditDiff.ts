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

export function resolveValueLink(field: string, value: string): string | null {
  if (!value || value === 'None' || value === 'null' || value === 'undefined') {
    return null;
  }

  const f = field.toLowerCase();

  // 1. Trainer / Staff / Vet / Groom / User references
  if (
    f.includes('trainer') ||
    f.includes('vet') ||
    f.includes('groom') ||
    f.includes('user') ||
    f.includes('owner') ||
    f.includes('actor') ||
    f.includes('staff')
  ) {
    const idMatch = value.match(/#(\d+)/);
    if (idMatch) {
      return `/manager/accounts?userId=${idMatch[1]}`;
    }
    return `/manager/accounts?search=${encodeURIComponent(value.trim())}`;
  }

  // 2. Horse references
  if (f.includes('horse') || f.includes('occupant')) {
    const idMatch = value.match(/#(\d+)/);
    if (idMatch) {
      return `/manager/horses/${idMatch[1]}`;
    }
    return `/manager/horses`;
  }

  // 3. Stall / Barn / Area references
  if (f.includes('stall') || f.includes('barn') || f.includes('area')) {
    return `/manager/stable`;
  }

  // 4. Admission references
  if (f.includes('admission')) {
    const idMatch = value.match(/#(\d+)/);
    if (idMatch) {
      return `/manager/admissions/${idMatch[1]}`;
    }
    return `/manager/admissions`;
  }

  return null;
}

export function resolveLogModule(
  module?: string | null,
  entityName?: string | null,
  requestPath?: string | null,
  action?: string | null
): string {
  if (module && module !== 'SYSTEM') {
    return module.toUpperCase();
  }

  // 1. Entity name check
  if (entityName && entityName.toUpperCase() !== 'SYSTEM') {
    const e = entityName.toUpperCase();
    if (e.includes('ADMISSION')) return 'ADMISSIONS';
    if (e.includes('CARE') || e.includes('HEALTH') || e.includes('MEDICAL') || e.includes('TREATMENT') || e.includes('INCIDENT')) return 'MEDICAL';
    if (e.includes('STABLE') || e.includes('STALL') || e.includes('AREA')) return 'STABLE';
    if (e.includes('HORSE')) return 'HORSES';
    if (e.includes('USER') || e.includes('STAFF') || e.includes('ACCOUNT')) return 'STAFF';
    if (e.includes('TRAINING') || e.includes('COURSE') || e.includes('SUBJECT') || e.includes('EXERCISE') || e.includes('LOT') || e.includes('WORKOUT')) return 'TRAINING';
  }

  // 2. Request path check
  if (requestPath) {
    const p = requestPath.toLowerCase();
    if (p.includes('/admissions')) return 'ADMISSIONS';
    if (p.includes('/care-schedules') || p.includes('/health-records') || p.includes('/health-metrics') || p.includes('/preventive') || p.includes('/treatment-plans') || p.includes('/incidents') || p.includes('/vet/')) return 'MEDICAL';
    if (p.includes('/stalls') || p.includes('/areas') || p.includes('/groom-daily-tasks')) return 'STABLE';
    if (p.includes('/horses')) return 'HORSES';
    if (p.includes('/staff') || p.includes('/users') || p.includes('/accounts')) return 'STAFF';
    if (p.includes('/training-plans') || p.includes('/courses') || p.includes('/subjects') || p.includes('/exercises') || p.includes('/course-runs') || p.includes('/group-plans') || p.includes('/lots') || p.includes('/workouts')) return 'TRAINING';
  }

  // 3. Action check
  if (action) {
    const a = action.toUpperCase();
    if (a.includes('ADMISSION')) return 'ADMISSIONS';
    if (a.includes('CARE') || a.includes('HEALTH') || a.includes('VET') || a.includes('TREATMENT')) return 'MEDICAL';
    if (a.includes('STALL') || a.includes('AREA') || a.includes('STABLE')) return 'STABLE';
    if (a.includes('HORSE')) return 'HORSES';
    if (a.includes('STAFF') || a.includes('USER') || a.includes('ACCOUNT')) return 'STAFF';
    if (a.includes('TRAINING') || a.includes('WORKOUT') || a.includes('COURSE') || a.includes('LOT')) return 'TRAINING';
  }

  return 'SYSTEM';
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

