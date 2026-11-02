export const SNOOZE_STORAGE_KEY = 'vet:urgent:p1:snoozed';
export const DISMISSED_SESSION_KEY = 'vet:urgent:p1:dismissed';
export const DEFAULT_SNOOZE_MS = 10 * 60 * 1000; // 10 minutes

/**
 * Returns true if an urgent P1 overlay should be suppressed for a schedule ID.
 */
export function isOverlaySuppressed(
  scheduleId: number,
  pathname: string,
  snoozedUntil: number | undefined,
  isDismissed: boolean,
  now: number = Date.now(),
): boolean {
  // 1. Suppress if vet is already viewing this exact urgent case workspace
  if (pathname === `/veterinarian/urgent/${scheduleId}`) {
    return true;
  }

  // 2. Suppress if currently snoozed and snooze period has not expired
  if (snoozedUntil !== undefined && snoozedUntil > now) {
    return true;
  }

  // 3. Suppress if user explicitly dismissed this case in current session
  if (isDismissed) {
    return true;
  }

  return false;
}

/**
 * Calculates future timestamp for snoozing an alert.
 */
export function getNextSnoozeExpiry(now: number = Date.now(), snoozeMs: number = DEFAULT_SNOOZE_MS): number {
  return now + snoozeMs;
}

/**
 * Checks whether emergency audio beep is permitted based on user motion/sound preferences.
 */
export function shouldPlayEmergencySound(prefersReducedMotion: boolean): boolean {
  return !prefersReducedMotion;
}

export interface CandidateAlert {
  scheduleId: number;
  reportedAt: string;
}

/**
 * Finds the top candidate alert to show in the P1 overlay from the active alert queue.
 */
export function selectP1OverlayAlert<T extends CandidateAlert>(
  alerts: T[],
  pathname: string,
  snoozedMap: Map<number, number>,
  dismissedSet: Set<number>,
  now: number = Date.now(),
): T | null {
  const eligible = alerts.filter((alert) => {
    const snoozedUntil = snoozedMap.get(alert.scheduleId);
    const dismissed = dismissedSet.has(alert.scheduleId);
    return !isOverlaySuppressed(alert.scheduleId, pathname, snoozedUntil, dismissed, now);
  });

  if (eligible.length === 0) return null;

  // Pick the oldest reported case first
  return [...eligible].sort(
    (a, b) => new Date(a.reportedAt).getTime() - new Date(b.reportedAt).getTime(),
  )[0];
}
