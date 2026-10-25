import type { NotificationItem } from '../types';

export const CURSOR_SEPARATOR = '_';
export const NOTIFICATIONS_STATE_VERSION = 1;
export const MAX_NOTIFICATIONS = 200;

export interface PersistedNotificationState {
  v: number;
  items: NotificationItem[];
  nextPageCursor: string | null;
  hasMore: boolean;
  lastSeenCursor: string;
}

/** Build the backend cursor for an item: "createdAt_id". */
export function cursorFromItem(item: NotificationItem): string {
  return `${item.createdAt}${CURSOR_SEPARATOR}${item.id}`;
}

/**
 * Merge two notification lists, deduplicating by id (incoming wins),
 * sorting newest-first by createdAt then id, and capping the result.
 */
export function mergeNotifications(
  prev: NotificationItem[],
  incoming: NotificationItem[],
  cap: number,
): NotificationItem[] {
  const map = new Map<number, NotificationItem>();
  for (const item of prev) map.set(item.id, item);
  for (const item of incoming) map.set(item.id, item);
  const merged = Array.from(map.values());
  merged.sort((a, b) => b.createdAt.localeCompare(a.createdAt) || b.id - a.id);
  return merged.slice(0, Math.max(0, cap));
}

function storageKey(userId: string | number): string {
  return `rtms_notifications_state_${userId}`;
}

/** Read persisted notification state from localStorage. Wipes invalid data. */
export function readPersistedNotifications(userId: string | number | null): PersistedNotificationState | null {
  if (!userId || typeof window === 'undefined') return null;
  const key = storageKey(userId);
  try {
    const raw = window.localStorage.getItem(key);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as unknown;
    if (
      !parsed ||
      typeof parsed !== 'object' ||
      !('v' in parsed) ||
      (parsed as { v: unknown }).v !== NOTIFICATIONS_STATE_VERSION ||
      !('items' in parsed) ||
      !Array.isArray((parsed as { items: unknown }).items) ||
      !('nextPageCursor' in parsed) ||
      !('hasMore' in parsed) ||
      !('lastSeenCursor' in parsed)
    ) {
      window.localStorage.removeItem(key);
      return null;
    }
    return parsed as PersistedNotificationState;
  } catch {
    try {
      window.localStorage.removeItem(key);
    } catch {
      // Ignore cleanup failure.
    }
    return null;
  }
}

/** Persist notification state to localStorage. */
export function writePersistedNotifications(userId: string | number, state: PersistedNotificationState): void {
  if (typeof window === 'undefined') return;
  try {
    window.localStorage.setItem(storageKey(userId), JSON.stringify(state));
  } catch {
    // Ignore quota/write errors.
  }
}
