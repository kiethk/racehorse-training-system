import type { Horse } from '@/features/stable/types';
import type { InjuryRecord, HorseHealthSummary } from '../types';

/** Health categories a veterinarian can filter the stable list by. */
export type HealthCategory = 'ALL' | 'CLEARED' | 'BLOCKED' | 'INJURED' | 'URGENT';

export const HEALTH_CATEGORY_OPTIONS: { value: HealthCategory; label: string }[] = [
  { value: 'ALL', label: 'All horses' },
  { value: 'CLEARED', label: 'Cleared' },
  { value: 'BLOCKED', label: 'Blocked' },
  { value: 'INJURED', label: 'Injured' },
  { value: 'URGENT', label: 'Urgent' },
];

/** One horse in the stable health list, with everything the list needs to show. */
export interface HealthOverviewRow {
  horseId: number;
  name: string;
  breed: string | null;
  registrationNumber: string | null;
  stallCode: string;
  trainingDecision: 'ALLOWED' | 'BLOCKED';
  trainingDecisionReason: string | null;
  activeInjuryCount: number;
  hasPendingUrgent: boolean;
  lastExaminedAt: string | null;
}

/**
 * Merges the stable horses with their health summary and injuries.
 * The summary wins over the horse row because the backend owns the training decision.
 */
export function buildOverviewRows(
  horses: Horse[],
  summaries: Record<number, HorseHealthSummary>,
  injuriesByHorse: Record<number, InjuryRecord[]>,
  stallCodes: Record<number, string>,
): HealthOverviewRow[] {
  return horses.map((horse) => {
    const summary = summaries[horse.id];
    const activeInjuries = (injuriesByHorse[horse.id] ?? []).filter((injury) => injury.status === 'ACTIVE');
    return {
      horseId: horse.id,
      name: horse.name,
      breed: horse.breed,
      registrationNumber: horse.registrationNumber ?? null,
      stallCode: (horse.currentStallId && stallCodes[horse.currentStallId]) || 'Unassigned',
      trainingDecision: summary?.trainingDecision ?? horse.trainingDecision ?? 'ALLOWED',
      trainingDecisionReason: summary?.trainingDecisionReason ?? horse.trainingDecisionReason ?? null,
      activeInjuryCount: activeInjuries.length > 0 ? activeInjuries.length : (summary?.activeInjuryCount ?? 0),
      hasPendingUrgent: summary?.hasPendingUrgent ?? false,
      lastExaminedAt: summary?.lastExaminedAt ?? null,
    };
  });
}

export function isInCategory(row: HealthOverviewRow, category: HealthCategory): boolean {
  switch (category) {
    case 'CLEARED':
      return row.trainingDecision === 'ALLOWED' && row.activeInjuryCount === 0;
    case 'BLOCKED':
      return row.trainingDecision === 'BLOCKED';
    case 'INJURED':
      return row.activeInjuryCount > 0;
    case 'URGENT':
      return row.hasPendingUrgent;
    default:
      return true;
  }
}

export function countByCategory(rows: HealthOverviewRow[]): Record<HealthCategory, number> {
  const counts: Record<HealthCategory, number> = { ALL: rows.length, CLEARED: 0, BLOCKED: 0, INJURED: 0, URGENT: 0 };
  for (const row of rows) {
    for (const category of ['CLEARED', 'BLOCKED', 'INJURED', 'URGENT'] as const) {
      if (isInCategory(row, category)) counts[category] += 1;
    }
  }
  return counts;
}

/** Matches name, registration number, breed, stall code or the horse id. */
export function matchesQuery(row: HealthOverviewRow, query: string): boolean {
  const needle = query.trim().toLocaleLowerCase('en-GB');
  if (!needle) return true;
  return [
    row.name,
    row.registrationNumber,
    row.breed,
    row.stallCode,
    String(row.horseId),
  ]
    .filter((value): value is string => Boolean(value))
    .some((value) => value.toLocaleLowerCase('en-GB').includes(needle));
}

export function filterOverviewRows(
  rows: HealthOverviewRow[],
  query: string,
  category: HealthCategory,
): HealthOverviewRow[] {
  return rows.filter((row) => isInCategory(row, category) && matchesQuery(row, query));
}

/** Page slice for a client-side list. Out-of-range pages collapse to the first page. */
export function paginate<T>(rows: T[], page: number, pageSize: number): T[] {
  const pageCount = Math.max(1, Math.ceil(rows.length / pageSize));
  const current = Math.min(Math.max(page, 0), pageCount - 1);
  return rows.slice(current * pageSize, (current + 1) * pageSize);
}
