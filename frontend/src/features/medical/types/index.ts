import type { Dispatch, SetStateAction } from 'react';

export type MedicationRoute = 'ORAL' | 'TOPICAL' | 'FEED_ADDITIVE';

export interface PrescriptionItemRequest {
  medicationName: string;
  dosage: string;
  route: MedicationRoute;
  frequency: string;
  startDate: string;
  endDate: string;
  instructions: string;
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

export interface PrescriptionSectionProps {
  isFormDisabled: boolean;
  prescriptions: PrescriptionFormItem[];
  setPrescriptions: Dispatch<SetStateAction<PrescriptionFormItem[]>>;
  generalCareInstructions: string;
  setGeneralCareInstructions: Dispatch<SetStateAction<string>>;
  fieldErrors: Record<string, string>;
  setFieldErrors: Dispatch<SetStateAction<Record<string, string>>>;
}

export interface MedicationCatalogItem {
  name: string;
  standardDosage?: string;
  standardFrequency?: string;
  standardRoute?: MedicationRoute;
  category?: string;
  description?: string;
}
