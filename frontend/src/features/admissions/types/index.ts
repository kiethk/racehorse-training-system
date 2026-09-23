export type AdmissionStatus =
  | 'SUBMITTED'
  | 'GROOM_REVIEW'
  | 'WAITING_FOR_STALL'
  | 'VET_REVIEW'
  | 'TRAINER_REVIEW'
  | 'MANAGER_REVIEW'
  | 'ADDITIONAL_INFORMATION_REQUIRED'
  | 'APPROVED'
  | 'REJECTED';

export interface AdmissionSummaryResponse {
  admissionId: number;
  status: AdmissionStatus;
  candidateName: string;
  breed: string;
  dateOfBirth: string; // ISO date string
  submittedAt: string; // ISO datetime string
  quarantineStallId: number | null;
}
