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
  imageUrl?: string | null;
  dateOfBirth: string; // ISO date string
  submittedAt: string; // ISO datetime string
  quarantineStallId: number | null;
  quarantineStallCode: string | null;
  /** Huấn luyện viên đã đánh giá hồ sơ này. null = chưa ai đánh giá. */
  trainerId: number | null;
  trainerName?: string | null;
  /** Thời điểm đánh giá, dạng ISO. null = chưa đánh giá. */
  trainerReviewedAt: string | null;
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
  careScheduleId?: number | null;
  horseId: number;
  veterinarianId: number;
  examinedAt: string;
  symptoms: string | null;
  findings: string | null;
  diagnosis: string | null;
  treatment: string | null;
  vetDecision: string | null;
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
  vetTrainingDecision?: string | null;
  vetFeedback: string | null;
  vetReviewedAt: string | null;

  trainerId: number | null;
  trainerName?: string | null;
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
  initialExamSchedule: {
    scheduleId: number;
    careType: string;
    status: string;
    veterinarianId: number | null;
    scheduledDate: string | null;
    scheduledAt: string | null;
  } | null;
  capacity: {
    availableQuarantineStalls: number;
    availableRegularStalls: number;
    occupiedQuarantineStalls: number;
    admissionCapacityAvailable: boolean;
    blockingReason: 'NO_QUARANTINE_STALL' | 'REGULAR_RESERVE_INSUFFICIENT' | null;
  };
}

// Care Schedule Types
export type CareScheduleStatus =
  | 'REQUESTED'
  | 'SCHEDULED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED';

export type CareType = 'INITIAL' | 'ROUTINE' | 'URGENT';

export interface HorseSummary {
  id: number;
  name: string;
  breed: string | null;
  registrationNumber: string | null;
  trainingStatus: TrainingDecision;
}

export interface UrgentAssignmentAlert {
  eventId: number;
  scheduleId: number;
  incidentId: number;
  veterinarianId: number;
  horseId: number;
  horseName: string;
  stableLocation: string | null;
  stallCode: string | null;
  reportedById: number;
  reportedByName: string | null;
  reportedAt: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  title: string;
  description: string;
  imageUrl: string | null;
  trainingStatus: TrainingDecision;
  status: CareScheduleStatus;
  scheduledAt: string | null;
  assignedAt: string;
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
}

export interface CareScheduleDetail {
  schedule: CareSchedule;
  horse: HorseSummary | null;
  veterinarian: { id: number; fullName: string; email: string } | null;
  healthRecord: {
    id: number;
    findings: string;
    diagnosis: string;
    treatment: string | null;
    trainingDecision: TrainingDecision;
    restrictionDetails: string | null;
  } | null;
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
  idempotencyKey: string;
  sourceScheduleId?: number | null;
  horseId?: number | null;
  admissionId?: number | null;
  careType: CareType;
  scheduledDate: string;
  description?: string;
  notes?: string;
}

export interface CompleteCareScheduleRequest {
  findings: string;
  diagnosis: string;
  symptoms?: string;
  treatment?: string;
  trainingDecision: TrainingDecision;
  restrictionDetails?: string;
  notes?: string;
  metrics?: HorseHealthMetricRequest[];
  nextSchedule?: CreateNextScheduleRequest | null;
}

export interface VetAdmissionQueueItem {
  admissionId: number;
  ownerId: number;
  ownerName: string | null;
  candidateName: string;
  breed: string;
  imageUrl?: string | null;
  dateOfBirth: string | null;
  admissionStatus: AdmissionStatus;
  submittedAt: string;
  quarantineStallId: number | null;
  quarantineStallCode: string | null;
  horseId: number | null;
  trainerId: number | null;
  trainerName: string | null;
  careSchedule: CareSchedule | null;
}

export interface VetQueueSummary {
  total: number;
  awaiting: number;
  inProgress: number;
}

export type VetQueueSummaryResponse = VetQueueSummary;

export interface VetQueueFilters {
  search?: string;
  pill?: 'ALL' | 'AWAITING' | 'IN_PROGRESS';
  admissionStatus?: AdmissionStatus | 'ALL';
  scheduleStatus?: CareScheduleStatus | 'ALL';
  careType?: CareType | 'ALL';
  priority?: 'ALL' | 'URGENT' | 'NORMAL';
  page?: number;
  size?: number;
}

export type TrainingDecision = 'ALLOWED' | 'RESTRICTED' | 'BLOCKED';

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
  decision?: string;
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
  nextSchedule?: CreateNextScheduleRequest | null;
}

export interface VetReviewResponse {
  admissionId: number;
  status: AdmissionStatus;
  veterinarianId: number;
  /** @deprecated Use trainingDecision instead */
  decision: string | null;
  trainingDecision: TrainingDecision;
  restrictionDetails: string | null;
  feedback: string | null;
  reviewedAt: string;
  horseId: number;
  horseStatus: string;
  quarantineStallId: number | null;
  quarantineStallCode: string | null;
  initialExamStatus: string;
  healthRecordId: number | null;
  vetExamId?: number;
  careScheduleId: number;
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
