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
  password?: string;
  phone?: string;
  address?: string;
  role: 'GROOM' | 'VETERINARIAN' | 'HEAD_TRAINER';
  licenseNumber?: string;
  licenseIssuedDate?: string;
  specialization?: string;
  certificationNumber?: string;
  certificationIssuedDate?: string;
  trainerId?: number;
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
  assignedAreaIds?: number[];
  assignedAreaCodes?: string[];
  // GROOM assignment
  trainerId?: number;
  trainerName?: string;
  assignedAreaId?: number;
  assignedAreaCode?: string;
  assignedStallIds?: number[];
  assignedStallCodes?: string[];
  noStallBlockAvailable?: boolean;
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
