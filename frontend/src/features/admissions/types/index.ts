export type AdmissionStatus =
  | 'GROOM_REVIEW'
  | 'WAITING_FOR_STALL'
  | 'WAITING_FOR_ARRIVAL'
  | 'VET_REVIEW'
  | 'PENDING_RECHECK'
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
  /** Huấn luyện viên đã đánh giá hồ sơ này. null = chưa ai đánh giá. */
  trainerId: number | null;
  /** Thời điểm đánh giá, dạng ISO. null = chưa đánh giá. */
  trainerReviewedAt: string | null;
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
  treatment: string | null;
  vetDecision: VetDecision | null;
  trainingDecision?: TrainingDecision | null;
  restrictionDetails?: string | null;
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
  arrivalStatus: 'PENDING' | 'CONFIRMED';
  arrivalConfirmedAt: string | null;
  arrivalConfirmedBy: number | null;
  submittedAt: string;

  availableRegularStalls: StableStall[];
  healthRecords: HealthRecord[];
  initialExamSchedule: InitialExamScheduleResponse | null;
  capacity: {
    availableQuarantineStalls: number;
    availableRegularStalls: number;
    occupiedQuarantineStalls: number;
    admissionCapacityAvailable: boolean;
    blockingReason: 'NO_QUARANTINE_STALL' | 'REGULAR_RESERVE_INSUFFICIENT' | null;
  };
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

// Care Schedule and Offer Types
export type CareScheduleStatus =
  | 'REQUESTED'
  | 'AWAITING_VET_CONFIRMATION'
  | 'SCHEDULED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED';

export type CareType = 'INITIAL' | 'ROUTINE' | 'URGENT';

export type VetOfferStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'EXPIRED' | 'RELEASED';

export interface VetOffer {
  id: number;
  scheduleId: number;
  careScheduleId?: number;
  veterinarianId: number;
  status: VetOfferStatus;
  round: number;
  offeredAt: string;
  expiresAt: string;
  proposedScheduledAt: string;
}

export interface PendingVetOfferResponse {
  id: number;
  scheduleId?: number;
  careScheduleId: number;
  admissionId?: number | null;
  horseId: number;
  horseName: string;
  breed?: string;
  careType: CareType;
  durationMinutes: number;
  description?: string | null;
  round: number;
  status: VetOfferStatus;
  offeredAt: string;
  expiresAt: string;
  proposedScheduledAt: string;
}

export interface CareSchedule {
  id: number;
  horseId: number;
  veterinarianId?: number | null;
  admissionId?: number | null;
  candidateName?: string;
  careType: CareType;
  status: CareScheduleStatus;
  assignedVetId?: number | null;
  scheduledDate?: string | null;
  scheduledAt?: string | null;
  notes?: string | null;
  description?: string | null;
  createdAt?: string;
  offers?: VetOffer[];
}

export interface CareScheduleFilters {
  status?: CareScheduleStatus;
  careType?: CareType;
  horseId?: number;
  admissionId?: number;
  vetId?: number;
  veterinarianId?: number;
  page?: number;
  size?: number;
}

export interface CreateNextScheduleRequest {
  horseId?: number | null;
  admissionId?: number | null;
  careType: CareType;
  scheduledDate: string;
  description?: string;
  notes?: string;
}

export interface CompleteCareScheduleRequest {
  findings: string;
  diagnosis?: string;
  treatment?: string;
  trainingDecision: TrainingDecision;
  restrictionDetails?: string;
  rejectAdmission?: boolean;
  rejectionReason?: string;
  notes?: string;
  metrics?: HorseHealthMetricRequest[];
  scheduleFollowUp?: boolean;
  followUpDate?: string;
  followUpDescription?: string;
}

export type TrainingDecision = 'ALLOWED' | 'RESTRICTED' | 'BLOCKED';
export type TrainingStatus = 'ALLOWED' | 'RESTRICTED' | 'BLOCKED';

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
  careScheduleId?: number;
  decision?: VetDecision;
  trainingDecision?: TrainingDecision;
  restrictionDetails?: string;
  rejectAdmission?: boolean;
  feedback?: string;
  physicalExamConfirmed: boolean;
  symptoms?: string;
  findings: string;
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
  trainingDecision?: TrainingDecision;
  trainingStatus?: TrainingStatus;
  restrictionDetails?: string | null;
  feedback: string | null;
  reviewedAt: string;
  horseId: number;
  horseStatus: string;
  quarantineStallId: number;
  quarantineStallCode: string;
  initialExamStatus: string;
  healthRecordId: number | null;
  vetExamId?: number;
  careScheduleId?: number;
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
