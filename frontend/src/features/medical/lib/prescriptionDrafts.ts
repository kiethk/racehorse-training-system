import type { PrescriptionFormItem, PrescriptionItemRequest } from '../types';

export function toPrescriptionRequests(items: PrescriptionFormItem[]): PrescriptionItemRequest[] {
  return items
    .filter((item) => (item.medicationName ?? '').trim().length > 0)
    .map((item) => ({
      medicationName: (item.medicationName ?? '').trim(),
      dosage: (item.dosage ?? '').trim(),
      route: item.route,
      frequency: (item.frequency ?? '').trim(),
      startDate: item.startDate,
      endDate: item.endDate,
      instructions: (item.instructions ?? '').trim(),
    }));
}

export function computeSuggestedRecheckDate(
  prescriptionEndDates: string[],
  injuryRecoveryDates: string[],
): string | undefined {
  const dates: string[] = [...(injuryRecoveryDates ?? []).filter(Boolean)];
  dates.push(...(prescriptionEndDates ?? []).filter(Boolean));

  const allDates = dates.sort();
  return allDates.length > 0 ? allDates[allDates.length - 1] : undefined;
}
