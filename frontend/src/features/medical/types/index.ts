import type { Dispatch, SetStateAction } from 'react';

export type MedicationRoute = 'ORAL' | 'TOPICAL' | 'FEED_ADDITIVE';

/** Request shape kept for the API: the examination form no longer sends prescriptions. */
export interface PrescriptionItemRequest {
  medicationName: string;
  dosage: string;
  route: MedicationRoute;
  frequency: string;
  startDate: string;
  endDate: string;
  instructions: string;
}

export interface PrescriptionSectionProps {
  isFormDisabled: boolean;
  generalCareInstructions: string;
  setGeneralCareInstructions: Dispatch<SetStateAction<string>>;
  fieldErrors: Record<string, string>;
  setFieldErrors: Dispatch<SetStateAction<Record<string, string>>>;
}
