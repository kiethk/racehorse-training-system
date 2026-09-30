export type Role = 'HEAD_TRAINER' | 'VETERINARIAN' | 'GROOM' | 'HORSE_OWNER' | 'CLUB_MANAGER';

export interface AuthUser {
  userId: number;
  fullName: string;
  email: string;
  role: Role;
  profile?: unknown; // Generic profile for now, can be typed later
}

export interface LoginRequest {
  email: string;
  password?: string;
}

export interface OwnerRegistrationRequest {
  fullName: string;
  email: string;
  password: string;
  phone?: string;
  address?: string;
}

export interface OwnerRegistrationResponse {
  userId: number;
  fullName: string;
  email: string;
  role: string;
}
