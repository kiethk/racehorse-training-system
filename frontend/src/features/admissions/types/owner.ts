import type { AdmissionStatus } from './index';
import type { HorseSex } from '@/types/horse';

export interface ApiResult<T> {
  success: boolean;
  data: T;
  message: string;
}

export interface CreateOwnerAdmissionRequest {
  name: string;
  sex: HorseSex;
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

export interface AdmissionDocumentUpload {
  file: File;
  documentType: AdmissionDocumentType;
  recordDate?: string;
  note?: string;
}

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

/** Stored legacy snapshots may not have sex, unlike a new submission. */
export type OwnerCandidateSnapshot = Omit<CreateOwnerAdmissionRequest, 'sex'> & {
  sex: HorseSex | null;
};

export interface OwnerAdmissionDetail {
  admissionId: number;
  status: AdmissionStatus;
  submittedAt: string;
  candidate: OwnerCandidateSnapshot;
  documents: AdmissionDocument[];
  groomFeedback: string | null;
  vetFeedback: string | null;
  trainerFeedback: string | null;
  managerFeedback: string | null;
  /** GROOM_REVIEW only: no groom can take a new horse right now. */
  quarantineFull: boolean;
}
