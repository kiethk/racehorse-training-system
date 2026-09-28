export type StallStatus = 'AVAILABLE' | 'OCCUPIED' | 'MAINTENANCE';
export type AreaType = 'REGULAR' | 'QUARANTINE';
export type HorseStatus =
  | 'CANDIDATE' | 'ELIGIBLE' | 'MONITORING'
  | 'INJURED' | 'QUARANTINED' | 'REJECTED';

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
}

export interface UserSummary {
  id: number;
  fullName: string;
  email: string;
  role: string;
}
