export type MedicationRoute = 'ORAL' | 'TOPICAL' | 'FEED_ADDITIVE';

export interface PrescriptionItemRequest {
  medicationName: string;
  dosage: string;
  route: MedicationRoute;
  frequency: string;
  startDate: string;
  endDate: string;
  instructions?: string;
}

export interface PrescriptionFormItem {
  id: string;
  medicationName: string;
  dosage: string;
  route: MedicationRoute;
  frequency: string;
  startDate: string;
  endDate: string;
  instructions: string;
}

export function toPrescriptionRequests(items: PrescriptionFormItem[]): PrescriptionItemRequest[] {
  return items
    .filter((item) => item.medicationName.trim().length > 0)
    .map((item) => ({
      medicationName: item.medicationName.trim(),
      dosage: item.dosage.trim(),
      route: item.route,
      frequency: item.frequency.trim(),
      startDate: item.startDate,
      endDate: item.endDate,
      instructions: item.instructions.trim() || undefined,
    }));
}

export function computeSuggestedRecheckDate(
  prescriptionEndDates: string[],
  injuryRecoveryDates: string[],
): string | undefined {
  const allDates = [...prescriptionEndDates, ...injuryRecoveryDates].filter(Boolean).sort();
  return allDates.length > 0 ? allDates[allDates.length - 1] : undefined;
}
