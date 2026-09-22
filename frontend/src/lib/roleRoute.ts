import type { Role } from '@/types/auth';

/**
 * Maps every backend Role to its role-specific landing page.
 * Used by login redirect, RoleGuard, and the root page redirect.
 */
export const ROLE_ROUTES: Record<Role, string> = {
  HORSE_OWNER: '/owner',
  GROOM: '/groom',
  VETERINARIAN: '/veterinarian',
  HEAD_TRAINER: '/trainer',
  CLUB_MANAGER: '/manager',
};

export function getRoleRoute(role: Role): string {
  return ROLE_ROUTES[role] ?? '/login';
}

export const ROLE_LABELS: Record<Role, string> = {
  HORSE_OWNER: 'Horse Owner',
  GROOM: 'Groom',
  VETERINARIAN: 'Veterinarian',
  HEAD_TRAINER: 'Head Trainer',
  CLUB_MANAGER: 'Club Manager',
};
