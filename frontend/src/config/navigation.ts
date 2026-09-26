import type { Role } from '@/types/auth';
import { getRoleRoute } from '@/lib/roleRoute';

export interface NavItem {
  id: string;
  label: string;
  href?: string; // If undefined, it acts as a disabled/placeholder item
}

/**
 * Returns the visible navigation items for a specific role.
 * Only implemented features have a valid href.
 */
export function getNavigationForRole(role: Role): NavItem[] {
  const dashboardHref = getRoleRoute(role);

  switch (role) {
    case 'CLUB_MANAGER':
      return [
        { id: 'dashboard', label: 'Dashboard', href: dashboardHref },
        { id: 'management', label: 'Management', href: '/manager/management' },
        { id: 'horses', label: 'Horses' },
        { id: 'stable', label: 'Stable' },
        { id: 'reports', label: 'Reports' },
      ];
    case 'HEAD_TRAINER':
      return [
        { id: 'dashboard', label: 'Dashboard', href: dashboardHref },
        { id: 'admissions', label: 'Admissions', href: '/trainer/admissions' },
        { id: 'stable', label: 'Stable', href: '/trainer/stable' },
        { id: 'training', label: 'Training', href: '/trainer/courses' },
        { id: 'schedule', label: 'Schedule', href: '/trainer/schedule' },
        { id: 'horses', label: 'Horses' },
        { id: 'racing', label: 'Racing' },
      ];
    case 'VETERINARIAN':
      return [
        { id: 'dashboard', label: 'Dashboard', href: dashboardHref },
        { id: 'admissions', label: 'Admissions' },
        { id: 'health', label: 'Health' },
        { id: 'preventive-care', label: 'Preventive Care' },
      ];
    case 'GROOM':
      return [
        { id: 'dashboard', label: 'Dashboard', href: dashboardHref },
        { id: 'care-tasks', label: 'Daily Tasks', href: '/groom/tasks' },
        { id: 'incidents', label: 'Incidents', href: '/groom/incidents' },
        { id: 'admissions', label: 'Admissions' },
        { id: 'stable', label: 'Stable' },
      ];
    case 'HORSE_OWNER':
      return [
        { id: 'dashboard', label: 'Dashboard', href: dashboardHref },
        { id: 'my-horses', label: 'My Horses' },
        { id: 'admissions', label: 'Admissions', href: '/owner/admissions' },
        { id: 'racing', label: 'Racing' },
      ];
    default:
      return [];
  }
}
