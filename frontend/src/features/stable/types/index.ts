export type StallStatus = 'AVAILABLE' | 'OCCUPIED' | 'MAINTENANCE';
export type AreaType = 'REGULAR' | 'QUARANTINE';
/** Vòng đời ngựa trong CLB. Sức khỏe không nằm ở đây — xem trainingDecision. */
export type HorseStatus = 'CANDIDATE' | 'ELIGIBLE' | 'REJECTED';

/** Thú y cho tập hay tạm chặn. Tập được = ELIGIBLE và ALLOWED. */
export type TrainingDecision = 'ALLOWED' | 'BLOCKED';

export interface Area {
  id: number;
  code: string;          // "A", "B", "Q"
  name: string;
  type: AreaType;
  trainerId: number | null;
}

export interface StableStall {
  id: number;
  areaId: number;
  groomId: number | null;
  stallNumber: number;
  stallCode: string;     // "A1", "Q3"
  status: StallStatus;
}

export interface Horse {
  id: number;
  name: string;
  breed: string | null;
  dateOfBirth: string | null;
  currentStatus: HorseStatus;
  currentStallId: number | null;
  ownerId: number | null;
  trainingDecision?: TrainingDecision;
  /** Lý do Thú y chặn tập; null khi ALLOWED. */
  trainingDecisionReason?: string | null;
  registrationNumber?: string | null;
}

export interface UserSummary {
  id: number;
  fullName: string;
  email: string;
  role: string;
}
