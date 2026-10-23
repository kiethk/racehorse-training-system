'use client';

import React, { createContext, useContext, useEffect, useState, useCallback, useMemo, useRef } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import { toast } from '@/lib/toast';
import { notificationApi } from '../services/api';
import type { NotificationItem } from '../types';
import { getNotificationHref } from '../lib/notificationRoute';

interface NotificationContextValue {
  notifications: NotificationItem[];
  unreadCount: number;
  refreshNotifications: () => Promise<void>;
  markAsRead: (id: number) => Promise<void>;
  markAllAsRead: () => Promise<void>;
}

const NotificationContext = createContext<NotificationContextValue | null>(null);

const ASSIGNMENT_TYPES = new Set(['ADMISSION_VET_ASSIGNED', 'ADMISSION_TRAINER_ASSIGNED']);
/** Long enough to read the message and reach the View button. */
const ACTION_TOAST_MS = 8000;

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
    // Ignore sessionStorage read errors
    return new Set<number>();
  }
}

function NotificationProviderForUser({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  const router = useRouter();

  const userId = user?.userId ?? null;
  const storageKey = `rtms_dismissed_notifications_${userId ?? 'anon'}`;

  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const openNotificationRef = useRef<(notification: NotificationItem) => Promise<void>>(() => Promise.resolve());
  // A ref, not state: toasting must not re-render, and concurrent polls must see the latest set.
  const toastedIds = useRef<Set<number> | null>(null);
  const requestGeneration = useRef(0);

  const markAsRead = useCallback(async (id: number) => {
    // An assignment opened from a toast may not be in the 10 most recent items; the next poll reconciles the count.
    await notificationApi.markAsRead(id);
    setNotifications((prev) => {
      const wasUnread = prev.some((item) => item.id === id && !item.read);
      if (wasUnread) setUnreadCount((count) => Math.max(0, count - 1));
      return prev.map((item) => (item.id === id ? { ...item, read: true } : item));
    });
  }, []);

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
      if (toastedIds.current === null) toastedIds.current = readToastedIds(storageKey);
      const seen = toastedIds.current;
      const fresh = unread
        .filter((n) => ASSIGNMENT_TYPES.has(n.notificationType) && !seen.has(n.id))
        .sort((a, b) => a.createdAt.localeCompare(b.createdAt) || a.id - b.id);
      if (fresh.length === 0) return;

      fresh.forEach((n) => seen.add(n.id));
      try {
        sessionStorage.setItem(storageKey, JSON.stringify(Array.from(seen)));
      } catch {
        // Ignore sessionStorage write errors
      }

      if (fresh.length === 1) {
        const [only] = fresh;
        toast.info(only.title || 'New horse assignment', only.message, {
          action: { label: 'View', onClick: () => void openNotificationRef.current(only) },
          duration: ACTION_TOAST_MS,
        });
      } else {
        toast.info(`You have ${fresh.length} new horse assignments`, 'Open the notification bell to review them.');
      }
    },
    [storageKey]
  );

  const refreshNotifications = useCallback(async () => {
    if (!userId) return;
    const generation = requestGeneration.current;
    try {
      const [count, historyPage] = await Promise.all([
        notificationApi.getUnreadCount(),
        notificationApi.getNotifications('ALL', 0, 10),
      ]);
      const unread: NotificationItem[] = [];
      let unreadPageNumber = 0;
      let unreadTotalPages = 1;
      do {
        const unreadPage = await notificationApi.getNotifications('UNREAD', unreadPageNumber, 50);
        unread.push(...unreadPage.content);
        unreadTotalPages = unreadPage.totalPages;
        unreadPageNumber += 1;
      } while (unreadPageNumber < unreadTotalPages);

      if (generation !== requestGeneration.current) return;
      setUnreadCount(count);
      setNotifications(historyPage.content);
      announceNewAssignments(unread);
    } catch {
      // Ignore background fetch error
    }
  }, [userId, announceNewAssignments]);

  useEffect(() => () => {
    requestGeneration.current += 1;
  }, []);

  // Initial, focus-triggered, and periodic polling.
  useEffect(() => {
    if (!userId) return;
    const initialTimer = window.setTimeout(() => void refreshNotifications(), 0);
    const interval = setInterval(refreshNotifications, 30000);
    const handleFocus = () => void refreshNotifications();
    window.addEventListener('focus', handleFocus);
    return () => {
      clearInterval(interval);
      window.clearTimeout(initialTimer);
      window.removeEventListener('focus', handleFocus);
    };
  }, [userId, refreshNotifications]);

  const markAllAsRead = useCallback(async () => {
    await notificationApi.markAllAsRead();
    setNotifications((prev) => prev.map((item) => ({ ...item, read: true })));
    setUnreadCount(0);
  }, []);

  const contextValue = useMemo(
    () => ({
      notifications,
      unreadCount,
      refreshNotifications,
      markAsRead,
      markAllAsRead,
    }),
    [notifications, unreadCount, refreshNotifications, markAsRead, markAllAsRead]
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
