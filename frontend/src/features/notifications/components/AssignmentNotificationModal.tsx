'use client';

import React, { useEffect } from 'react';
import { Icon } from '@/components/ui/Icon';
import { Button } from '@/components/ui/Button';
import type { NotificationItem } from '../types';

interface AssignmentNotificationModalProps {
  notification: NotificationItem | null;
  onViewDetails: (notification: NotificationItem) => void;
  onDismissRead: (notification: NotificationItem) => void;
  onDismissTemporary: () => void;
  actionError?: string | null;
  actionPending?: boolean;
}

export function AssignmentNotificationModal({
  notification,
  onViewDetails,
  onDismissRead,
  onDismissTemporary,
  actionError,
  actionPending = false,
}: AssignmentNotificationModalProps) {
  useEffect(() => {
    if (!notification) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onDismissTemporary();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [notification, onDismissTemporary]);

  if (!notification) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="notification-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-[2px] animate-fadeIn"
    >
      <div className="relative w-full max-w-md bg-white dark:bg-neutral-900 rounded-xl shadow-2xl border border-neutral-200 dark:border-neutral-800 p-6 overflow-hidden">
        {/* Close icon button for temporary dismiss */}
        <button
          type="button"
          onClick={onDismissTemporary}
          className="absolute top-4 right-4 text-neutral-400 hover:text-neutral-600 dark:hover:text-neutral-200 transition-colors p-1 rounded-md"
          aria-label="Close"
        >
          <Icon name="x" className="w-5 h-5" />
        </button>

        <div className="flex items-start gap-4">
          <div className="p-3 bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400 rounded-xl shrink-0">
            <Icon name="clipboard" className="w-6 h-6" />
          </div>

          <div className="flex-1 pr-6">
            <span className="inline-block px-2 py-0.5 text-xs font-semibold rounded bg-blue-100 dark:bg-blue-900/50 text-blue-800 dark:text-blue-300 mb-2">
              Assignment Notification
            </span>
            <h3
              id="notification-modal-title"
              className="text-base font-bold text-neutral-900 dark:text-neutral-100"
            >
              {notification.title || 'New horse assignment'}
            </h3>
            <p className="mt-2 text-sm text-neutral-600 dark:text-neutral-300 leading-relaxed">
              {notification.message}
            </p>
          </div>
        </div>

        <div className="mt-6 flex items-center justify-end gap-3 pt-4 border-t border-neutral-100 dark:border-neutral-800">
          <Button
            variant="secondary"
            size="sm"
            onClick={() => onDismissRead(notification)}
            disabled={actionPending}
          >
            Understood
          </Button>
          <Button
            variant="primary"
            size="sm"
            onClick={() => onViewDetails(notification)}
            loading={actionPending}
            className="flex items-center gap-1.5"
          >
            <span>View details</span>
            <Icon name="chevron-right" className="w-4 h-4" />
          </Button>
        </div>
        {actionError && (
          <p role="alert" className="mt-3 text-right text-xs font-medium text-[var(--color-danger)]">
            {actionError}
          </p>
        )}
      </div>
    </div>
  );
}
