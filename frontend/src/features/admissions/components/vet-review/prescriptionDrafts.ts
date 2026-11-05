export type MedicationRoute = 'ORAL' | 'TOPICAL' | 'INJECTION_IV' | 'INJECTION_IM' | 'FEED_ADDITIVE';

export interface PrescriptionItemRequest {
  medicationId: number;
  dosage: string;
  route: MedicationRoute;
  frequency: string;
  startDate: string;
  endDate: string;
  withdrawalDays: number;
  instructions?: string;
}

export interface PrescriptionFormItem {
  id: string;
  medicationId: number | null;
  dosage: string;
  route: MedicationRoute;
  frequency: string;
  startDate: string;
  endDate: string;
  withdrawalDays: number;
  instructions: string;
}

export function toPrescriptionRequests(items: PrescriptionFormItem[]): PrescriptionItemRequest[] {
  return items
    .filter((item) => item.medicationId != null)
    .map((item) => ({
      medicationId: item.medicationId!,
      dosage: item.dosage.trim(),
      route: item.route,
      frequency: item.frequency.trim(),
      startDate: item.startDate,
      endDate: item.endDate,
      withdrawalDays: item.withdrawalDays ?? 0,
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
