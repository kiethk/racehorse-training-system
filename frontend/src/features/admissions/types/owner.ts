import type { AdmissionStatus } from './index';

export interface ApiResult<T> {
  success: boolean;
  data: T;
  message: string;
}

export interface CreateOwnerAdmissionRequest {
  name: string;
  breed?: string;
  dateOfBirth?: string;
  registrationNumber?: string;
  registryName?: string;
  sireName?: string;
  sireRegistrationNumber?: string;
  damName?: string;
  damRegistrationNumber?: string;
  pedigreeNotes?: string;
}

export type AdmissionDocumentType =
  | 'HORSE_PHOTO' | 'REGISTRATION_DOCUMENT' | 'PEDIGREE_CERTIFICATE'
  | 'VACCINATION_RECORD' | 'DEWORMING_RECORD' | 'HEALTH_CERTIFICATE'
  | 'PREVIOUS_MEDICAL_RECORD' | 'PREVIOUS_INJURY_RECORD';

export interface AdmissionDocument {
  id: number;
  documentType: AdmissionDocumentType;
  fileUrl: string;
  recordDate: string | null;
  note: string | null;
  uploadedAt: string;
  medical: boolean;
}
export interface AdmissionSummaryResponse {
  admissionId: number;
  status: AdmissionStatus;
  candidateName: string;
  breed: string | null;
  dateOfBirth: string | null;
  submittedAt: string;
  quarantineStallId: number | null;
}

export interface OwnerAdmissionDetail {
  admissionId: number;
  status: AdmissionStatus;
  submittedAt: string;
  candidate: CreateOwnerAdmissionRequest;
  documents: AdmissionDocument[];
  groomFeedback: string | null;
  vetFeedback: string | null;
  trainerFeedback: string | null;
  managerFeedback: string | null;
}
