export type AdmissionStatus =
  | 'SUBMITTED'
  | 'GROOM_REVIEW'
  | 'WAITING_FOR_STALL'
  | 'VET_REVIEW'
  | 'PENDING_RECHECK'
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
  imageUrl?: string;
}

export interface AdmissionDocument {
  id: number;
  documentType: string;
  fileUrl: string;
  recordDate: string | null;
  note: string | null;
  uploadedAt: string;
}

export interface HealthRecord {
  id: number;
  horseId: number;
  veterinarianId: number;
  examinedAt: string;
  symptoms: string | null;
  findings: string | null;
  diagnosis: string | null;
  treatment: string | null;
  vetDecision: VetDecision | null;
  rejectionReason: string | null;
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
  initialExamSchedule: InitialExamScheduleResponse | null;
}

export interface InitialExamScheduleResponse {
  scheduleId: number;
  careType: string;
  status: string;
  veterinarianId: number | null;
  scheduledDate: string | null;
  scheduledAt: string | null;
}

export type VetDecision = 'APPROVED' | 'RECHECK_REQUIRED' | 'REJECTED';
export type VetExamType = 'URGENT' | 'INITIAL' | 'FOLLOW_UP' | 'ROUTINE';
export type VetExamStatus = 'REQUESTED' | 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface VetExamResponse {
  id: number;
  horseId: number;
  admissionId: number | null;
  examType: VetExamType;
  status: VetExamStatus;
  priority: number;
  reason: string | null;
  createdByUserId: number | null;
  assignedVetId: number | null;
  preferredVetId: number | null;
  requestedForDate: string | null;
  scheduledAt: string | null;
  durationMinutes: number;
  healthRecordId: number | null;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface HorseHealthMetricRequest {
  heartRate?: number;
  temperature?: number;
  weight?: number;
  respiratoryRate?: number;
  hydrationStatus?: string;
  bodyConditionScore?: number;
  notes?: string;
}

export interface HorseHealthMetricResponse extends HorseHealthMetricRequest {
  id: number;
  horseId: number;
  healthRecordId: number;
  recordedAt: string;
}

export interface VetReviewRequest {
  decision: VetDecision;
  feedback?: string;
  physicalExamConfirmed: boolean;
  symptoms?: string;
  findings?: string;
  diagnosis?: string;
  treatment?: string;
  rejectionReason?: string;
  notes?: string;
  followUpDate?: string;
  metrics?: HorseHealthMetricRequest[];
}

export interface VetReviewResponse {
  admissionId: number;
  status: AdmissionStatus;
  veterinarianId: number;
  decision: VetDecision;
  feedback: string | null;
  reviewedAt: string;
  horseId: number;
  horseStatus: string;
  quarantineStallId: number;
  quarantineStallCode: string;
  initialExamStatus: string;
  healthRecordId: number | null;
  vetExamId: number;
}

export interface ManagerReviewRequest {
  decision: 'APPROVED' | 'REJECTED';
  feedback?: string;
  stallId?: number | null;
}
