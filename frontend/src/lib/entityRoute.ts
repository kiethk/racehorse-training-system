import type { Role } from '../types/auth';

export type EntityKind = 'horse' | 'owner' | 'user' | 'admission' | 'race' | 'plan' | 'stall' | 'editRequest';

export function entityHref(kind: EntityKind, id: number | string, role: Role | null | undefined, extra?: { stallCode?: string | null; horseId?: number | null }): string | null {
  if (!role) return null;
  const value = encodeURIComponent(String(id));
  switch (kind) {
    case 'horse':
      if (role === 'CLUB_MANAGER') return `/manager/horses/${value}`;
      if (role === 'HORSE_OWNER') return `/owner/horses/${value}`;
      if (role === 'HEAD_TRAINER') return `/trainer/horses/${value}`;
      if (role === 'VETERINARIAN') return `/veterinarian/records?horseId=${value}`;
      return role === 'GROOM' && extra?.stallCode ? `/groom/stable?stall=${encodeURIComponent(extra.stallCode)}` : null;
    case 'owner':
    case 'user': return role === 'CLUB_MANAGER' ? `/manager/accounts?userId=${value}` : null;
    case 'admission': {
      const paths: Partial<Record<Role, string>> = { CLUB_MANAGER: 'manager', HORSE_OWNER: 'owner', HEAD_TRAINER: 'trainer', GROOM: 'groom' };
      if (role === 'VETERINARIAN') return '/veterinarian/admissions';
      return paths[role] ? `/${paths[role]}/admissions/${value}` : null;
    }
    case 'race': return role === 'HEAD_TRAINER' ? `/trainer/racing/${value}` : null;
    case 'plan': return role === 'HEAD_TRAINER' ? `/trainer/plans/${value}` : null;
    case 'stall': {
      const paths: Partial<Record<Role, string>> = { CLUB_MANAGER: 'manager', HEAD_TRAINER: 'trainer', GROOM: 'groom' };
      return paths[role] ? `/${paths[role]}/stable?stall=${value}` : null;
    }
    case 'editRequest':
      if (role === 'GROOM') return `/groom/horse-edit-requests/${value}`;
      return role === 'HORSE_OWNER' && extra?.horseId != null ? `/owner/horses/${encodeURIComponent(String(extra.horseId))}` : null;
  }
}
