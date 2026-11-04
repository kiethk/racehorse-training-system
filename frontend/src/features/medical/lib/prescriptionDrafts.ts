import type { PrescriptionFormItem, PrescriptionItemRequest } from '../types';

export function toPrescriptionRequests(items: PrescriptionFormItem[]): PrescriptionItemRequest[] {
  return items
    .filter((item) => (item.medicationName ?? '').trim().length > 0)
    .map((item) => ({
      medicationName: (item.medicationName ?? '').trim(),
      dosage: (item.dosage ?? '').trim(),
      route: item.route,
      frequency: (item.frequency ?? '').trim(),
      duration: item.duration ?? 1,
      notes: (item.notes ?? '').trim() || undefined,
    }));
}

export function computeSuggestedRecheckDate(
  prescriptionDurations: number[],
  injuryRecoveryDates: string[],
): string | undefined {
  const dates: string[] = [...(injuryRecoveryDates ?? []).filter(Boolean)];
  const validDurations = (prescriptionDurations ?? []).filter((d) => typeof d === 'number' && d > 0);

  if (validDurations.length > 0) {
    const today = new Date();
    const maxDuration = Math.max(...validDurations);
    const recheckDate = new Date(today);
    recheckDate.setDate(recheckDate.getDate() + maxDuration);
    dates.push(recheckDate.toISOString().slice(0, 10));
  }

  const allDates = dates.sort();
  return allDates.length > 0 ? allDates[allDates.length - 1] : undefined;
}
