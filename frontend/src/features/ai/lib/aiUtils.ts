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
 * Checks whether two rectangular areas intersect.
 */
export function checkRectsOverlap(
  r1: { x: number; y: number; width: number; height: number },
  r2: { x: number; y: number; width: number; height: number }
): boolean {
  return !(
    r1.x + r1.width <= r2.x ||
    r1.x >= r2.x + r2.width ||
    r1.y + r1.height <= r2.y ||
    r1.y >= r2.y + r2.height
  );
}

/**
 * Calculates popup position relative to the floating action button (FAB),
 * ensuring the popup appears beside or above without covering the button.
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

  const fabRect = { x: fab.x, y: fab.y, width: fabSize, height: fabSize };

  // Candidate 1: Try placing above the FAB (ideal when FAB is near bottom corner)
  const candidateAboveX =
    fab.x + fabSize / 2 > viewportWidth / 2
      ? fab.x + fabSize - width
      : fab.x;
  const candidateAboveY = fab.y - height - gap;

  const clampedAbove = clampDialogPosition(
    { x: candidateAboveX, y: candidateAboveY },
    width,
    height,
    viewportWidth,
    viewportHeight,
    gap
  );

  // If candidateAbove fits above without overlapping the FAB, use it
  if (!checkRectsOverlap({ ...clampedAbove, width, height }, fabRect)) {
    return clampedAbove;
  }

  // Candidate 2: Place side-by-side so the popup never covers the FAB
  const spaceLeft = fab.x - gap;
  const spaceRight = viewportWidth - (fab.x + fabSize) - gap;

  let sideX: number;
  if (spaceLeft >= width) {
    // Prefer left side if it has enough space
    sideX = fab.x - width - gap;
  } else if (spaceRight >= width) {
    // Or right side if it fits
    sideX = fab.x + fabSize + gap;
  } else if (spaceLeft >= spaceRight) {
    sideX = fab.x - width - gap;
  } else {
    sideX = fab.x + fabSize + gap;
  }

  // Vertically align centered with the FAB, clamped safely to viewport
  const idealY = fab.y + fabSize / 2 - height / 2;
  const clampedSideY = Math.min(
    Math.max(gap, idealY),
    Math.max(gap, viewportHeight - height - gap)
  );

  return clampDialogPosition(
    { x: sideX, y: clampedSideY },
    width,
    height,
    viewportWidth,
    viewportHeight,
    gap
  );
}
