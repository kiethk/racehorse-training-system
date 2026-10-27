import type { Role } from '@/types/auth';

export interface AccountSummary {
  userId: number;
  fullName: string;
  email: string;
  phone?: string;
  address?: string;
  role: Role | string;
  active: boolean;
  createdAt: string;
  profileSummary?: string;
  ownedHorsesCount?: number;
  assignmentStatus?: string;
  assignedAreaCode?: string | null;
  assignedAreaType?: 'REGULAR' | 'QUARANTINE' | null;
  assignedStallCodes?: string[] | null;
  trainerName?: string | null;
  managedAreaCodes?: string[] | null;
}

export interface AccountCreationRequest {
  fullName: string;
  email: string;
  password: string;
  phone?: string;
  address?: string;
  role: Role | string;
  // Veterinarian specific
  licenseNumber?: string;
  licenseIssuedDate?: string;
  specialization?: string;
  // Head Trainer specific
  certificationNumber?: string;
  certificationIssuedDate?: string;
  // Groom specific
  trainerId?: number;
}

export interface AccountCreationResponse {
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

export interface OwnedHorseSummary {
  id: number;
  name: string;
  registrationNumber: string;
  currentStatus: string;
  stableLocation?: string;
}

export interface AccountDetailResponse {
  id: number;
  fullName: string;
  email: string;
  phone?: string;
  address?: string;
  role: string;
  active: boolean;
  createdAt: string;
  profile?: Record<string, unknown>;
  ownedHorses?: OwnedHorseSummary[];
  assignmentStatus?: string;
  assignedAreaId?: number | null;
  assignedAreaCode?: string | null;
  assignedAreaName?: string | null;
  assignedAreaType?: 'REGULAR' | 'QUARANTINE' | null;
  assignedStallCodes?: string[] | null;
  trainerId?: number | null;
  trainerName?: string | null;
  managedAreaCodes?: string[] | null;
}

export interface AccountUpdateRequest {
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
