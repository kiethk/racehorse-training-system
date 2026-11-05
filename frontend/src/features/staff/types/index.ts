export interface StaffSummary {
  userId: number;
  fullName: string;
  email: string;
  phone?: string;
  address?: string;
  role: string;
  active: boolean;
  createdAt: string;
  profileSummary?: string;
}

export interface StaffCreationRequest {
  fullName: string;
  email: string;
  password: string;
  phone?: string;
  address?: string;
  role: 'GROOM' | 'VETERINARIAN' | 'HEAD_TRAINER';
  licenseNumber?: string;
  licenseIssuedDate?: string;
  specialization?: string;
  certificationNumber?: string;
  certificationIssuedDate?: string;
}

/** Result of POST /api/staff — carries auto-assignment info from backend. */
export interface StaffCreationResponse {
  userId: number;
  fullName: string;
  email: string;
  phone?: string;
  address?: string;
  role: string;
  active: boolean;
  createdAt: string;
  profileSummary?: string;
  // HEAD_TRAINER assignment
  assignedAreaIds?: number[] | null;
  assignedAreaCodes?: string[] | null;
  // GROOM assignment
  trainerId?: number | null;
  trainerName?: string | null;
  assignedAreaId?: number | null;
  assignedAreaCode?: string | null;
  assignedAreaType?: 'REGULAR' | 'QUARANTINE' | null;
  assignedStallIds?: number[] | null;
  assignedStallCodes?: string[] | null;
  noStallBlockAvailable?: boolean;
  assignmentStatus?: string;
}

export interface StaffDetailResponse {
  id: number;
  fullName: string;
  email: string;
  phone?: string;
  address?: string;
  role: string;
  active: boolean;
  createdAt: string;
  profile?: Record<string, unknown>;
}

export interface StaffUpdateRequest {
  fullName?: string;
  phone?: string;
  address?: string;
  // Vet
  licenseNumber?: string;
  licenseIssuedDate?: string;
  specialization?: string;
  // Trainer
  certificationNumber?: string;
  certificationIssuedDate?: string;
  // Groom
  trainerId?: number | null;
  trainerIdProvided?: boolean;
}
