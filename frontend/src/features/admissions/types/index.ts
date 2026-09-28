export type AdmissionStatus =
  | 'GROOM_REVIEW'
  | 'WAITING_FOR_STALL'
  | 'VET_REVIEW'
  | 'TRAINER_REVIEW'
  | 'MANAGER_REVIEW'
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
  quarantineStallCode: string | null;
  imageUrl?: string | null;
}

export interface AdmissionDocument {
  id: number;
  documentType: string;
  fileUrl: string;
  recordDate: string | null;
  note: string | null;
  uploadedAt: string;
  originalFileName: string | null;
}

export interface HealthRecord {
  id: number;
  horseId: number;
  veterinarianId: number;
  examinedAt: string;
  symptoms: string | null;
  findings: string | null;
  diagnosis: string | null;
  recordType: string;
  productOrService: string | null;
  notes: string | null;
  followUpDate: string | null;
}

export interface StableStall {
  id: number;
  areaId: number;
  groomId: number | null;
  stallNumber: number;
  stallCode: string;
  status: string;
}

export interface AdmissionDetailResponse {
  admissionId: number;
  ownerId: number;
  ownerName: string | null;
  status: AdmissionStatus;
  quarantineStallId: number | null;
  quarantineStallCode: string | null;
  
  candidate: {
    id: number;
    name: string;
    breed: string;
    dateOfBirth: string;
    registrationNumber: string | null;
    registryName: string | null;
    sireName: string | null;
    damName: string | null;
    pedigreeNotes: string | null;
  } | null;

  documents: AdmissionDocument[];

  groomId: number | null;
  groomDecision: string | null;
  groomFeedback: string | null;
  groomReviewedAt: string | null;

  veterinarianId: number | null;
  vetDecision: string | null;
  vetFeedback: string | null;
  vetReviewedAt: string | null;

  trainerId: number | null;
  trainerFeedback: string | null;
  trainerReviewedAt: string | null;

  managerId: number | null;
  managerDecision: string | null;
  managerFeedback: string | null;
  managerReviewedAt: string | null;

  horseId: number | null;
  submittedAt: string;

  availableRegularStalls: StableStall[];
  healthRecords: HealthRecord[];
  capacity: {
    availableQuarantineStalls: number;
    availableRegularStalls: number;
    occupiedQuarantineStalls: number;
    admissionCapacityAvailable: boolean;
    blockingReason: 'NO_QUARANTINE_STALL' | 'REGULAR_RESERVE_INSUFFICIENT' | null;
  };
}

export interface GroomQueueFilters {
  candidateName: string;
  status: AdmissionStatus | '';
  submittedFrom: string;
  submittedTo: string;
  page: number;
}

export interface GroomQueueResponse {
  content: AdmissionSummaryResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface GroomReviewRequest {
  decision: 'APPROVED' | 'REJECTED';
  feedback: string;
}

export interface ManagerReviewRequest {
  decision: 'APPROVED' | 'REJECTED';
  feedback?: string;
  stallId?: number | null;
}
