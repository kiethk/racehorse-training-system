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
