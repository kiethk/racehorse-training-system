/**
 * AI Assistant & Quick Prompts Utilities
 */

import type { Role } from '../../../types/auth.ts';
import { ROLE_LABELS } from '../../../lib/roleRoute.ts';

export const PROMPTS_BY_ROLE: Record<Role, string[]> = {
  HEAD_TRAINER: [
    "Today's workout session schedule",
    'Training plan progress & milestones',
    'Injury risk & recovery heart rate alerts',
    'Horse distance & track preferences',
    'Assigned grooms on duty',
  ],
  VETERINARIAN: [
    'Pending & active clinical exam queue',
    'Clinical case literature lookup via RAG',
    'Preventive care & vaccination schedule',
    'Medical quarantine horse list',
    'Stable incident reports in past 48h',
  ],
  GROOM: [
    "Today's care task checklist",
    'My assigned stalls and horses',
    'Feed intake & activity warnings',
    'Stall assignments & caretaker directory',
  ],
  CLUB_MANAGER: [
    'Staff directory & roster overview',
    'Barn & stall occupancy chart',
    'Pending admission applications',
    'Recent compliance & audit log',
  ],
  HORSE_OWNER: [
    'My racehorses directory',
    'Upcoming workout schedule',
    'Health summary & recent vet exams',
    'Training course progress & fitness',
  ],
};

/**
 * Returns user-friendly display name for user role (instead of raw enum like HEAD_TRAINER)
 */
export function getRoleDisplayName(role?: Role | string | null): string {
  if (!role) return 'AI Assistant';
  return (ROLE_LABELS as Record<string, string>)[role] || role;
}

export function getQuickPromptsByRole(role?: Role | string | null): string[] {
  if (!role) return PROMPTS_BY_ROLE.HORSE_OWNER;
  const norm = role.toUpperCase();
  if (norm === 'HEAD_TRAINER' || norm === 'TRAINER') {
    return PROMPTS_BY_ROLE.HEAD_TRAINER;
  }
  if (norm === 'VETERINARIAN' || norm === 'VET') {
    return PROMPTS_BY_ROLE.VETERINARIAN;
  }
  if (norm === 'GROOM' || norm === 'STABLE_STAFF') {
    return PROMPTS_BY_ROLE.GROOM;
  }
  if (norm === 'CLUB_MANAGER' || norm === 'RACING_MANAGER' || norm === 'ADMIN') {
    return PROMPTS_BY_ROLE.CLUB_MANAGER;
  }
  if (norm === 'HORSE_OWNER' || norm === 'OWNER') {
    return PROMPTS_BY_ROLE.HORSE_OWNER;
  }
  return PROMPTS_BY_ROLE.HORSE_OWNER;
}

export interface Coordinates {
  x: number;
  y: number;
}

/**
 * Clamps floating dialog position within viewport boundaries with safety margin.
 */
export function clampDialogPosition(
  pos: Coordinates,
  width: number,
  height: number,
  viewportWidth: number,
  viewportHeight: number,
  minMargin = 8
): Coordinates {
  const effectiveWidth = Math.min(width, Math.max(0, viewportWidth - minMargin * 2));
  const effectiveHeight = Math.min(height, Math.max(0, viewportHeight - minMargin * 2));

  const maxX = Math.max(minMargin, viewportWidth - effectiveWidth - minMargin);
  const maxY = Math.max(minMargin, viewportHeight - effectiveHeight - minMargin);

  return {
    x: Math.min(Math.max(minMargin, Math.round(pos.x)), Math.round(maxX)),
    y: Math.min(Math.max(minMargin, Math.round(pos.y)), Math.round(maxY)),
  };
}

/**
 * Calculates popup position relative to the floating action button (FAB).
 */
export function getPopupPositionFromFab(
  fab: Coordinates,
  isExpanded: boolean,
  viewportWidth: number,
  viewportHeight: number
): Coordinates {
  const width = isExpanded ? 800 : 380;
  const height = isExpanded ? 720 : 560;
  const fabSize = 56;
  const gap = 12;

  let y = fab.y - height - gap;
  if (y < gap) {
    y = fab.y + fabSize + gap;
  }

  let x: number;
  if (fab.x + fabSize / 2 > viewportWidth / 2) {
    x = fab.x + fabSize - width;
  } else {
    x = fab.x;
  }

  return clampDialogPosition({ x, y }, width, height, viewportWidth, viewportHeight, gap);
}


