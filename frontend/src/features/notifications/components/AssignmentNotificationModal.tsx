'use client';

import { Button, Icon, Modal, Notice, Pill } from '@/components/ui';
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
  if (!notification) return null;

  return (
    <Modal
      open
      // Closing (Escape, backdrop, X) only hides the notification for this session; it stays unread.
      onClose={onDismissTemporary}
      size="sm"
      title={notification.title || 'New horse assignment'}
      footer={(
        <>
          <Button variant="secondary" onClick={() => onDismissRead(notification)} disabled={actionPending}>
            Understood
          </Button>
          <Button
            variant="primary"
            iconRight="chevron-right"
            onClick={() => onViewDetails(notification)}
            loading={actionPending}
          >
            View details
          </Button>
        </>
      )}
    >
      <div className="flex items-start gap-3">
        <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-[var(--radius-md)] bg-[var(--color-info-soft)] text-[var(--color-info)]">
          <Icon name="clipboard" size={20} />
        </span>
        <div className="min-w-0 space-y-2">
          <Pill tone="info" size="sm">Assignment notification</Pill>
          <p className="text-sm leading-relaxed text-[var(--color-text-secondary)]">{notification.message}</p>
        </div>
      </div>
      {actionError && <Notice tone="error" className="mt-4">{actionError}</Notice>}
    </Modal>
  );
}
