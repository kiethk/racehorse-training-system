'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import { useNotifications } from '@/features/notifications/context/NotificationContext';
import { getNotificationHref } from '@/features/notifications/lib/notificationRoute';
import type { NotificationItem } from '@/features/notifications/types';
import { Icon } from '@/components/ui/Icon';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/Popover';
import { cn } from '@/lib/cn';

export function NotificationBell() {
  const { user } = useAuth();
  const { notifications, unreadCount, markAsRead, markAllAsRead } = useNotifications();
  const router = useRouter();
  const [open, setOpen] = useState(false);

  // Marking as read is best-effort here; the next poll reconciles the list.
  const markReadQuietly = async (id: number) => {
    try {
      await markAsRead(id);
    } catch {
      // ignore
    }
  };

  const handleView = async (item: NotificationItem) => {
    if (!item.read) await markReadQuietly(item.id);
    setOpen(false);
    const href = getNotificationHref(item, user?.role);
    if (href) router.push(href);
  };

  const handleMarkAllRead = async () => {
    try {
      await markAllAsRead();
    } catch {
      // ignore
    }
  };

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger
        aria-label={unreadCount > 0 ? `Notifications, ${unreadCount} unread` : 'Notifications'}
        className="relative flex h-9 w-9 items-center justify-center rounded-[var(--radius-md)] text-[var(--color-text-secondary)] outline-none transition-colors hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-text-primary)] focus-visible:ring-2 focus-visible:ring-[var(--color-focus)] data-[state=open]:bg-[var(--color-surface-muted)]"
      >
        <Icon name="bell" size={18} />
        {unreadCount > 0 && (
          <span
            aria-hidden="true"
            className="absolute right-0.5 top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-[var(--color-danger)] px-1 text-[10px] font-bold leading-none text-[var(--color-text-inverse)] animate-in zoom-in-50 duration-200"
          >
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </PopoverTrigger>

      <PopoverContent className="w-96 overflow-hidden">
        <div className="flex items-center justify-between border-b border-[var(--color-border)] px-3.5 py-2.5">
          <div className="flex items-center gap-2">
            <span className="text-sm font-semibold text-[var(--color-text-primary)]">Notifications</span>
            {unreadCount > 0 && (
              <span className="rounded-full bg-[var(--color-primary-soft)] px-2 py-0.5 text-[10px] font-semibold text-[var(--color-primary)]">
                {unreadCount} unread
              </span>
            )}
          </div>
          <button
            type="button"
            onClick={() => void handleMarkAllRead()}
            disabled={unreadCount === 0}
            className="rounded-[var(--radius-xs)] text-[11px] font-medium text-[var(--color-primary)] outline-none hover:underline focus-visible:ring-2 focus-visible:ring-[var(--color-focus)] disabled:cursor-not-allowed disabled:no-underline disabled:opacity-40"
          >
            Mark all as read
          </button>
        </div>

        {notifications.length === 0 ? (
          <p className="p-6 text-center text-xs text-[var(--color-text-muted)]">No notifications</p>
        ) : (
          <ul className="scroll-slim max-h-80 divide-y divide-[var(--color-border)] overflow-y-auto">
            {notifications.map((item) => (
              <li
                key={item.id}
                className={cn(
                  'flex items-start gap-1 transition-colors hover:bg-[var(--color-surface-muted)]',
                  !item.read && 'bg-[var(--color-primary-subtle)]',
                )}
              >
                <button
                  type="button"
                  onClick={() => void handleView(item)}
                  className="flex min-w-0 flex-1 items-start gap-3 p-3 text-left outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-[var(--color-focus)]"
                >
                  <span
                    aria-hidden="true"
                    className={cn(
                      'mt-1.5 h-2 w-2 shrink-0 rounded-full',
                      item.read ? 'bg-transparent' : 'bg-[var(--color-primary)]',
                    )}
                  />
                  <span className="min-w-0 flex-1">
                    <span className="flex items-center justify-between gap-2">
                      <span
                        className={cn(
                          'truncate text-xs leading-snug',
                          item.read
                            ? 'font-medium text-[var(--color-text-secondary)]'
                            : 'font-semibold text-[var(--color-text-primary)]',
                        )}
                      >
                        {item.title}
                      </span>
                      <span className="shrink-0 text-[10px] text-[var(--color-text-muted)]">
                        {formatNotificationTime(item.createdAt)}
                      </span>
                    </span>
                    <span className="mt-0.5 line-clamp-2 block text-[11px] leading-relaxed text-[var(--color-text-muted)]">
                      {item.message}
                    </span>
                  </span>
                </button>
                {!item.read && (
                  <button
                    type="button"
                    aria-label={`Mark "${item.title}" as read`}
                    onClick={() => void markReadQuietly(item.id)}
                    className="mr-2 mt-2.5 shrink-0 rounded-[var(--radius-xs)] p-1 text-[var(--color-text-muted)] outline-none hover:bg-[var(--color-surface)] hover:text-[var(--color-text-primary)] focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
                  >
                    <Icon name="check" size={13} />
                  </button>
                )}
              </li>
            ))}
          </ul>
        )}
      </PopoverContent>
    </Popover>
  );
}

function formatNotificationTime(isoString: string): string {
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return '';
  return date.toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}
