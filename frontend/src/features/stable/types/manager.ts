import type { Area, Horse, StableStall, UserSummary } from './index';

export interface StableSnapshot {
  areas: Area[];
  stalls: StableStall[];
  horses: Horse[];
  trainers: UserSummary[];
  grooms: UserSummary[];
}

export interface AssignmentDraft {
  trainers: Record<number, number | null>;
  grooms: Record<number, number | null>;
}

export type AssignmentTarget =
  | { kind: 'trainer'; areaId: number }
  | { kind: 'groom'; areaId: number; blockNumber: number };

export interface DragStaff {
  kind: 'trainer' | 'groom';
  personId: number;
  source?: AssignmentTarget;
}

export interface AssignmentBatch {
  trainerAssignments: { areaId: number; trainerId: number | null; expectedTrainerId: number | null }[];
  groomAssignments: { areaId: number; blockNumber: number; groomId: number | null; expectedGroomIds: (number | null)[] }[];
}

export interface AssignmentResponse {
  updatedAreas: Area[];
  updatedStalls: StableStall[];
  affectedWorkoutsCount: number;
  message: string;
}

export interface AssignmentChange {
  label: string;
  before: string;
  after: string;
  note?: string;
}
