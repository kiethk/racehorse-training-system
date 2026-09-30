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
