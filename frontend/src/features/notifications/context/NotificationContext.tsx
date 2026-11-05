'use client';

import React, { createContext, useContext, useEffect, useState, useCallback, useMemo, useRef } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import { toast } from '@/lib/toast';
import { notificationApi } from '../services/api';
import type { NotificationItem, NotificationFeedResponse } from '../types';
import { getNotificationHref } from '../lib/notificationRoute';
import {
  cursorFromItem,
  mergeNotifications,
  readPersistedNotifications,
  writePersistedNotifications,
  MAX_NOTIFICATIONS,
} from '../lib/notificationState';

interface NotificationContextValue {
  notifications: NotificationItem[];
  unreadCount: number;
  hasMore: boolean;
  refreshNotifications: () => Promise<void>;
  loadOlder: () => Promise<void>;
  markAsRead: (id: number) => Promise<void>;
  markAllAsRead: () => Promise<void>;
}

const NotificationContext = createContext<NotificationContextValue | null>(null);

const ASSIGNMENT_TYPES = new Set([
  'ADMISSION_VET_ASSIGNED',
  'ADMISSION_TRAINER_ASSIGNED',
  'ADMISSION_NEW_FOR_GROOM',
  'ADMISSION_GROOM_ASSIGNED',
  'HORSE_EDIT_REQUESTED',
  'HORSE_EDIT_REVIEWED',
]);
/** Long enough to read the message and reach the View button. */
const ACTION_TOAST_MS = 8000;
const POLL_MS = 30000;

export function NotificationProvider({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  const userIdentity = user?.userId ?? 'anonymous';

  return (
    <NotificationProviderForUser key={userIdentity}>
      {children}
    </NotificationProviderForUser>
  );
}

/** Assignment notifications already announced in this browser session, so polling never repeats a toast. */
function readToastedIds(storageKey: string): Set<number> {
  if (typeof window === 'undefined') return new Set();
  try {
    const raw = sessionStorage.getItem(storageKey);
    const parsed = raw ? JSON.parse(raw) : null;
    return Array.isArray(parsed) ? new Set<number>(parsed.map(Number)) : new Set<number>();
  } catch {
    // Ignore sessionStorage read errors.
    return new Set<number>();
  }
}

function NotificationProviderForUser({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  const router = useRouter();

  const userId = user?.userId ?? null;
  const toastedStorageKey = `rtms_dismissed_notifications_${userId ?? 'anon'}`;

  const restored = userId ? readPersistedNotifications(userId) : null;

  const [items, setItems] = useState<NotificationItem[]>(restored?.items ?? []);
  const [nextPageCursor, setNextPageCursor] = useState<string | null>(restored?.nextPageCursor ?? null);
  const [hasMore, setHasMore] = useState<boolean>(restored?.hasMore ?? false);
  const [lastSeenCursor, setLastSeenCursor] = useState<string>(restored?.lastSeenCursor ?? '');
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const openNotificationRef = useRef<(notification: NotificationItem) => Promise<void>>(() => Promise.resolve());
  const itemsRef = useRef<NotificationItem[]>(restored?.items ?? []);
  const nextPageCursorRef = useRef<string | null>(restored?.nextPageCursor ?? null);
  const hasMoreRef = useRef<boolean>(restored?.hasMore ?? false);
  const lastSeenCursorRef = useRef<string>(restored?.lastSeenCursor ?? '');
  const toastedIds = useRef<Set<number> | null>(null);
  const requestGeneration = useRef(0);

  useEffect(() => { itemsRef.current = items; }, [items]);
  useEffect(() => { nextPageCursorRef.current = nextPageCursor; }, [nextPageCursor]);
  useEffect(() => { hasMoreRef.current = hasMore; }, [hasMore]);
  useEffect(() => { lastSeenCursorRef.current = lastSeenCursor; }, [lastSeenCursor]);

  const persist = useCallback((nextItems: NotificationItem[]) => {
    if (!userId) return;
    const watermark = nextItems.length > 0 ? cursorFromItem(nextItems[0]) : lastSeenCursorRef.current;
    writePersistedNotifications(userId, {
      v: 1,
      items: nextItems,
      nextPageCursor: nextPageCursorRef.current,
      hasMore: hasMoreRef.current,
      lastSeenCursor: watermark,
    });
  }, [userId]);

  const mergeAndPersist = useCallback((incoming: NotificationItem[], nextCursor: string | null, more: boolean) => {
    const merged = mergeNotifications(itemsRef.current, incoming, MAX_NOTIFICATIONS);
    itemsRef.current = merged;

    const newWatermark = merged.length > 0 ? cursorFromItem(merged[0]) : lastSeenCursorRef.current;
    if (newWatermark !== lastSeenCursorRef.current) {
      lastSeenCursorRef.current = newWatermark;
      setLastSeenCursor(newWatermark);
    }

    setItems(merged);
    setNextPageCursor(nextCursor);
    setHasMore(more);
    nextPageCursorRef.current = nextCursor;
    hasMoreRef.current = more;
    persist(merged);
  }, [persist]);

  const markAsRead = useCallback(async (id: number) => {
    const wasUnread = itemsRef.current.some((item) => item.id === id && !item.read);
    const updated = await notificationApi.markAsRead(id);
    const next = itemsRef.current.map((item) => (item.id === id ? updated : item));
    itemsRef.current = next;
    setItems(next);
    if (wasUnread) setUnreadCount((prev) => Math.max(0, prev - 1));
    persist(next);
  }, [persist]);

  const userRole = user?.role;
  const openNotification = useCallback(
    async (notification: NotificationItem) => {
      try {
        await markAsRead(notification.id);
      } catch {
        // Best effort, like the bell: still take the user to the item.
      }
      const href = getNotificationHref(notification, userRole);
      if (href) router.push(href);
    },
    [markAsRead, router, userRole]
  );

  useEffect(() => {
    openNotificationRef.current = openNotification;
  }, [openNotification]);

  const announceNewAssignments = useCallback(
    (unread: NotificationItem[]) => {
      if (toastedIds.current === null) toastedIds.current = readToastedIds(toastedStorageKey);
      const seen = toastedIds.current;
      const fresh = unread
        .filter((n) => ASSIGNMENT_TYPES.has(n.notificationType) && !seen.has(n.id))
        .sort((a, b) => a.createdAt.localeCompare(b.createdAt) || a.id - b.id);
      if (fresh.length === 0) return;

      fresh.forEach((n) => seen.add(n.id));
      try {
        sessionStorage.setItem(toastedStorageKey, JSON.stringify(Array.from(seen)));
      } catch {
        // Ignore sessionStorage write errors.
      }

      if (fresh.length === 1) {
        const [only] = fresh;
        toast.info(only.title || 'New horse assignment', only.message, {
          action: { label: 'View', onClick: () => void openNotificationRef.current(only) },
          duration: ACTION_TOAST_MS,
        });
      } else {
        toast.info(`You have ${fresh.length} new notifications`, 'Open the notification bell to review them.');
      }
    },
    [toastedStorageKey]
  );

  const poll = useCallback(async () => {
    if (!userId) return;
    const generation = requestGeneration.current;
    try {
      const [count, delta]: [number, NotificationFeedResponse] = await Promise.all([
        notificationApi.getUnreadCount(),
        lastSeenCursorRef.current
          ? notificationApi.getNotificationsSince(lastSeenCursorRef.current)
          : notificationApi.getNotificationFeed(),
      ]);
      if (generation !== requestGeneration.current) return;

      setUnreadCount(count);
      if (lastSeenCursorRef.current) {
        mergeAndPersist(delta.content, nextPageCursorRef.current, hasMoreRef.current);
      } else {
        mergeAndPersist(delta.content, delta.nextCursor, delta.hasMore);
      }
      if (delta.content.length > 0) {
        announceNewAssignments(delta.content.filter((n) => !n.read));
      }
    } catch {
      // Ignore background fetch errors.
    }
  }, [userId, mergeAndPersist, announceNewAssignments]);

  const refreshNotifications = useCallback(async () => {
    if (!userId) return;
    const generation = requestGeneration.current;
    try {
      const [count, page]: [number, NotificationFeedResponse] = await Promise.all([
        notificationApi.getUnreadCount(),
        notificationApi.getNotificationFeed(),
      ]);
      if (generation !== requestGeneration.current) return;
      setUnreadCount(count);
      mergeAndPersist(page.content, page.nextCursor, page.hasMore);
    } catch {
      // Ignore background fetch errors.
    }
  }, [userId, mergeAndPersist]);

  const loadOlder = useCallback(async () => {
    if (!userId || !nextPageCursorRef.current) return;
    const generation = requestGeneration.current;
    try {
      const page = await notificationApi.getNotificationFeed(undefined, nextPageCursorRef.current);
      if (generation !== requestGeneration.current) return;
      mergeAndPersist(page.content, page.nextCursor, page.hasMore);
    } catch {
      // Ignore background fetch errors.
    }
  }, [userId, mergeAndPersist]);

  const markAllAsRead = useCallback(async () => {
    await notificationApi.markAllAsRead();
    const next = itemsRef.current.map((item) => ({ ...item, read: true }));
    itemsRef.current = next;
    setItems(next);
    setUnreadCount(0);
    persist(next);
  }, [persist]);

  // On mount, start from the persisted watermark if available; otherwise load the first page.
  useEffect(() => {
    if (!userId) return;
    if (lastSeenCursorRef.current) {
      void poll();
    } else {
      void refreshNotifications();
    }
  }, [userId, poll, refreshNotifications]);

  // Focus-triggered and periodic polling. The mount effect performs the initial load.
  useEffect(() => {
    if (!userId) return;
    let interval: ReturnType<typeof setInterval> | null = setInterval(poll, POLL_MS);
    const handleVisibility = () => {
      if (document.visibilityState === 'hidden') {
        if (interval) { clearInterval(interval); interval = null; }
      } else {
        if (!interval) interval = setInterval(poll, POLL_MS);
        void poll();
      }
    };
    const handleFocus = () => void poll();
    document.addEventListener('visibilitychange', handleVisibility);
    window.addEventListener('focus', handleFocus);
    return () => {
      if (interval) clearInterval(interval);
      document.removeEventListener('visibilitychange', handleVisibility);
      window.removeEventListener('focus', handleFocus);
    };
  }, [userId, poll]);

  // Cancel in-flight requests when the provider unmounts (e.g. user switch).
  useEffect(() => () => {
    requestGeneration.current += 1;
  }, []);

  const contextValue = useMemo(
    () => ({
      notifications: items,
      unreadCount,
      hasMore,
      refreshNotifications,
      loadOlder,
      markAsRead,
      markAllAsRead,
    }),
    [items, unreadCount, hasMore, refreshNotifications, loadOlder, markAsRead, markAllAsRead]
  );

  return (
    <NotificationContext.Provider value={contextValue}>
      {children}
    </NotificationContext.Provider>
  );
}

export function useNotifications() {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotifications must be used within a NotificationProvider');
  }
  return context;
}
