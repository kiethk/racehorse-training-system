export type StallStatus = 'AVAILABLE' | 'OCCUPIED' | 'MAINTENANCE';
export type AreaType = 'REGULAR' | 'QUARANTINE';
/** A horse's lifecycle in the club. Health is not tracked here — see trainingDecision. */
export type HorseStatus = 'CANDIDATE' | 'ELIGIBLE' | 'REJECTED';

/** Whether the vet allows training or temporarily blocks it. Can train = ELIGIBLE and ALLOWED. */
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
  /** Why the vet blocked training; null when ALLOWED. */
  trainingDecisionReason?: string | null;
  registrationNumber?: string | null;
}

export interface UserSummary {
  id: number;
  fullName: string;
  email: string;
  role: string;
}
