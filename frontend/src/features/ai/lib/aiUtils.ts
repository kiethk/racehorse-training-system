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
