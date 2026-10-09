'use client';

import React, { createContext, useContext, useEffect, useState, useCallback, useMemo, useRef } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import { notificationApi } from '../services/api';
import type { NotificationItem } from '../types';
import { getNotificationHref } from '../lib/notificationRoute';
import { AssignmentNotificationModal } from '../components/AssignmentNotificationModal';

interface NotificationContextValue {
  notifications: NotificationItem[];
  unreadCount: number;
  refreshNotifications: () => Promise<void>;
  markAsRead: (id: number) => Promise<void>;
  markAllAsRead: () => Promise<void>;
}

const NotificationContext = createContext<NotificationContextValue | null>(null);

export function NotificationProvider({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  const userIdentity = user?.userId ?? 'anonymous';

  return (
    <NotificationProviderForUser key={userIdentity}>
      {children}
    </NotificationProviderForUser>
  );
}

function NotificationProviderForUser({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  const router = useRouter();

  const userId = user?.userId ?? null;
  const storageKey = `rtms_dismissed_notifications_${userId ?? 'anon'}`;

  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [activeModalNotification, setActiveModalNotification] = useState<NotificationItem | null>(null);
  const [temporarilyDismissedIds, setTemporarilyDismissedIds] = useState<Set<number>>(() => {
    if (typeof window === 'undefined') return new Set();
    try {
      const raw = sessionStorage.getItem(`rtms_dismissed_notifications_${userId ?? 'anon'}`);
      if (raw) {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed)) {
          return new Set<number>(parsed.map(Number));
        }
      }
    } catch {
      // Ignore sessionStorage read errors
    }
    return new Set<number>();
  });
  const [modalActionError, setModalActionError] = useState<string | null>(null);
  const [modalActionPending, setModalActionPending] = useState(false);
  const requestGeneration = useRef(0);

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

      const pendingAssignments = unread
        .filter(
          (n) =>
          (n.notificationType === 'ADMISSION_VET_ASSIGNED' ||
            n.notificationType === 'ADMISSION_TRAINER_ASSIGNED') &&
          !temporarilyDismissedIds.has(n.id)
        )
        .sort((a, b) => a.createdAt.localeCompare(b.createdAt) || a.id - b.id);

      setActiveModalNotification((current) => {
        if (current && unread.some((item) => item.id === current.id)) return current;
        return pendingAssignments[0] ?? null;
      });
    } catch {
      // Ignore background fetch error
    }
  }, [userId, temporarilyDismissedIds]);

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

  const markAsRead = useCallback(async (id: number) => {
    const wasUnread =
      notifications.some((item) => item.id === id && !item.read) ||
      (activeModalNotification?.id === id && !activeModalNotification.read);
    await notificationApi.markAsRead(id);
    setNotifications((prev) =>
      prev.map((item) => (item.id === id ? { ...item, read: true } : item))
    );
    if (wasUnread) setUnreadCount((prev) => Math.max(0, prev - 1));
    setActiveModalNotification((current) => (current?.id === id ? null : current));
    setModalActionError(null);
  }, [activeModalNotification, notifications]);

  const markAllAsRead = useCallback(async () => {
    try {
      await notificationApi.markAllAsRead();
      setNotifications((prev) => prev.map((item) => ({ ...item, read: true })));
      setUnreadCount(0);
      setActiveModalNotification(null);
    } catch (cause) {
      throw cause;
    }
  }, []);

  const handleViewDetails = useCallback(
    async (notification: NotificationItem) => {
      setModalActionPending(true);
      setModalActionError(null);
      try {
        await markAsRead(notification.id);
        await refreshNotifications();

        const href = getNotificationHref(notification, user?.role);
        if (href) router.push(href);
      } catch (cause) {
        setModalActionError(cause instanceof Error ? cause.message : 'Could not mark this notification as read. Please retry.');
      } finally {
        setModalActionPending(false);
      }
    },
    [markAsRead, refreshNotifications, router, user]
  );

  const handleDismissRead = useCallback(
    async (notification: NotificationItem) => {
      setModalActionPending(true);
      setModalActionError(null);
      try {
        await markAsRead(notification.id);
        await refreshNotifications();
      } catch (cause) {
        setModalActionError(cause instanceof Error ? cause.message : 'Could not mark this notification as read. Please retry.');
      } finally {
        setModalActionPending(false);
      }
    },
    [markAsRead, refreshNotifications]
  );

  const handleDismissTemporary = useCallback(() => {
    if (activeModalNotification) {
      const dismissedId = activeModalNotification.id;
      setTemporarilyDismissedIds((prev) => {
        const next = new Set(prev).add(dismissedId);
        try {
          if (typeof window !== 'undefined') {
            sessionStorage.setItem(storageKey, JSON.stringify(Array.from(next)));
          }
        } catch {
          // Ignore sessionStorage write errors
        }
        return next;
      });
    }
    setActiveModalNotification(null);
    setModalActionError(null);
  }, [activeModalNotification, storageKey]);

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
      <AssignmentNotificationModal
        notification={activeModalNotification}
        onViewDetails={handleViewDetails}
        onDismissRead={handleDismissRead}
        onDismissTemporary={handleDismissTemporary}
        actionError={modalActionError}
        actionPending={modalActionPending}
      />
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
