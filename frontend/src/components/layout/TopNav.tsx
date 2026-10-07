'use client';

import { useState, useRef, useEffect } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import { useNotifications } from '@/features/notifications/context/NotificationContext';
import { getNavigationForRole } from '@/config/navigation';
import { BrandLogo } from '@/components/ui/BrandLogo';
import { Icon } from '@/components/ui/Icon';
import { ROLE_LABELS, getRoleRoute } from '@/lib/roleRoute';
import type { NotificationItem } from '@/features/notifications/types';

export function TopNav() {
  const { user, logout } = useAuth();
  const { notifications, unreadCount, markAsRead, markAllAsRead } = useNotifications();
  const router = useRouter();
  const pathname = usePathname();
  const [accountOpen, setAccountOpen] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const notifRef = useRef<HTMLDivElement>(null);
  const accountRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (notifRef.current && !notifRef.current.contains(event.target as Node)) {
        setNotificationsOpen(false);
      }
      if (accountRef.current && !accountRef.current.contains(event.target as Node)) {
        setAccountOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  if (!user) return null;

  const navItems = getNavigationForRole(user.role);
  
  // Simple initials generator
  const initials = user.fullName
    .split(' ')
    .map((n) => n[0])
    .join('')
    .substring(0, 2)
    .toUpperCase();

  const handleLogout = async () => {
    await logout();
    router.replace('/login');
  };

  const handleViewNotification = async (item: NotificationItem) => {
    if (!item.read) {
      try {
        await markAsRead(item.id);
      } catch {
        // ignore background markAsRead error
      }
    }
    setNotificationsOpen(false);

    const refId = item.referenceId;
    if (item.referenceType === 'CARE_SCHEDULE' && refId && user.role === 'VETERINARIAN') {
      router.push(`/veterinarian/admissions?scheduleId=${refId}`);
    } else if (item.notificationType === 'ADMISSION_VET_ASSIGNED' && refId) {
      router.push(`/veterinarian/admissions?id=${refId}`);
    } else if (item.notificationType === 'ADMISSION_TRAINER_ASSIGNED' && refId) {
      router.push(`/trainer/admissions/${refId}`);
    } else if (item.referenceType === 'ADMISSION' && refId) {
      if (user.role === 'VETERINARIAN') {
        router.push(`/veterinarian/admissions?id=${refId}`);
      } else if (user.role === 'HEAD_TRAINER') {
        router.push(`/trainer/admissions/${refId}`);
      }
    }
  };

  const handleMarkItemRead = async (e: React.MouseEvent, id: number) => {
    e.stopPropagation();
    try {
      await markAsRead(id);
    } catch {
      // ignore
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await markAllAsRead();
    } catch {
      // ignore
    }
  };

  return (
    <header className="sticky top-0 z-30 flex h-14 shrink-0 items-center gap-6 border-b border-[var(--color-border)] bg-[var(--color-surface)] px-4">
      {/* Brand */}
      <div className="flex items-center gap-2">
        <BrandLogo className="h-7 w-7" />
        <span className="text-[15px] font-semibold tracking-tight text-[var(--color-text-primary)]">
          RTMS
        </span>
      </div>

      {/* Desktop Navigation */}
      <nav className="hidden items-center gap-0.5 md:flex" aria-label="Primary">
        {navItems.map((item) => {
          const dashboardHref = getRoleRoute(user.role);
          const isActive = item.href && (
            item.href === dashboardHref
              ? pathname === dashboardHref
              : pathname.startsWith(item.href)
          );
          
          if (item.href) {
            return (
              <Link
                key={item.id}
                href={item.href}
                aria-current={isActive ? 'page' : undefined}
                className={
                  'relative flex h-14 items-center px-3 text-[13px] font-medium outline-none transition-colors focus-visible:ring-2 focus-visible:ring-[var(--color-focus)] ' +
                  (isActive
                    ? 'text-[var(--color-primary)]'
                    : 'text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]')
                }
              >
                {item.label}
                {isActive && (
                  <span className="absolute inset-x-2 bottom-0 h-0.5 rounded-full bg-[var(--color-primary)]" />
                )}
              </Link>
            );
          }

          // Disabled item
          return (
            <span
              key={item.id}
              className="relative flex h-14 items-center px-3 text-[13px] font-medium text-[var(--color-text-muted)] cursor-not-allowed"
              title="Not implemented yet"
            >
              {item.label}
            </span>
          );
        })}
      </nav>

      {/* Mobile Navigation Dropdown */}
      <div className="relative md:hidden ml-auto flex-1">
        <span className="sr-only">Current module</span>
        <select
          aria-label="Current module"
          value={pathname}
          onChange={(event) => {
            if (event.target.value) {
              router.push(event.target.value);
            }
          }}
          className="h-8 w-full max-w-[150px] appearance-none rounded-[var(--radius-xs)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-2.5 pr-6 text-[12px] font-medium text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
        >
          {navItems.map((item) => (
            <option key={item.id} value={item.href || ''} disabled={!item.href}>
              {item.label}
            </option>
          ))}
        </select>
        <Icon
          name="chevron-down"
          size={13}
          className="pointer-events-none absolute right-1.5 top-2.5 text-[var(--color-text-muted)]"
        />
      </div>

      {/* User / Notification Controls */}
      <div className="ml-auto flex items-center gap-1.5 md:ml-auto md:flex-none">
        {/* Notification Bell */}
        <div className="relative" ref={notifRef}>
          <button
            type="button"
            onClick={() => {
              setNotificationsOpen((prev) => !prev);
              setAccountOpen(false);
            }}
            className="relative flex h-8 w-8 items-center justify-center rounded-[var(--radius-sm)] text-[var(--color-text-secondary)] outline-none hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-text-primary)] focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
            aria-label="Notifications"
            aria-expanded={notificationsOpen}
          >
            <Icon name="bell" size={17} />
            {unreadCount > 0 && (
              <span className="absolute -top-0.5 -right-0.5 flex h-4 min-w-[16px] items-center justify-center rounded-full bg-[var(--color-danger)] px-1 text-[10px] font-bold leading-none text-white shadow-sm">
                {unreadCount > 99 ? '99+' : unreadCount}
              </span>
            )}
          </button>

          {notificationsOpen && (
            <div className="absolute right-0 top-10 z-40 w-80 sm:w-96 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] shadow-xl shadow-black/10 overflow-hidden">
              <div className="flex items-center justify-between border-b border-[var(--color-border)] px-3.5 py-2.5">
                <div className="flex items-center gap-2">
                  <span className="text-[13px] font-semibold text-[var(--color-text-primary)]">
                    Notifications
                  </span>
                  {unreadCount > 0 && (
                    <span className="rounded-full bg-[var(--color-primary-soft)] px-2 py-0.5 text-[10px] font-semibold text-[var(--color-primary)]">
                      {unreadCount} unread
                    </span>
                  )}
                </div>
                <button
                  type="button"
                  onClick={handleMarkAllRead}
                  disabled={unreadCount === 0}
                  className="text-[11px] font-medium text-[var(--color-primary)] hover:underline disabled:opacity-40 disabled:cursor-not-allowed disabled:no-underline"
                >
                  Mark all as read
                </button>
              </div>

              <div className="max-h-80 overflow-y-auto divide-y divide-[var(--color-border)]">
                {notifications.length === 0 ? (
                  <div className="p-6 text-center text-[12px] text-[var(--color-text-muted)]">
                    No notifications
                  </div>
                ) : (
                  notifications.map((item) => (
                    <div
                      key={item.id}
                      onClick={() => void handleViewNotification(item)}
                      className={`group flex items-start gap-3 p-3 text-left transition-colors cursor-pointer hover:bg-[var(--color-surface-muted)] ${
                        !item.read ? 'bg-[var(--color-primary-soft)]/20' : ''
                      }`}
                    >
                      <span
                        className={`mt-1.5 h-2 w-2 shrink-0 rounded-full ${
                          !item.read ? 'bg-[var(--color-primary)]' : 'bg-transparent'
                        }`}
                        aria-hidden="true"
                      />
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center justify-between gap-2">
                          <p
                            className={`text-[12px] leading-snug truncate ${
                              !item.read
                                ? 'font-semibold text-[var(--color-text-primary)]'
                                : 'font-medium text-[var(--color-text-secondary)]'
                            }`}
                          >
                            {item.title}
                          </p>
                          <span className="shrink-0 text-[10px] text-[var(--color-text-muted)]">
                            {formatNotificationTime(item.createdAt)}
                          </span>
                        </div>
                        <p className="mt-0.5 line-clamp-2 text-[11px] text-[var(--color-text-muted)] leading-relaxed">
                          {item.message}
                        </p>
                      </div>
                      <div className="shrink-0 flex items-center gap-1">
                        {!item.read && (
                          <button
                            type="button"
                            title="Mark as read"
                            aria-label="Mark as read"
                            onClick={(e) => void handleMarkItemRead(e, item.id)}
                            className="rounded p-1 text-[var(--color-text-muted)] hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-text-primary)]"
                          >
                            <Icon name="check" size={13} />
                          </button>
                        )}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}
        </div>

        {/* User / Account Menu */}
        <div className="relative" ref={accountRef}>
          <button
            type="button"
            onClick={() => setAccountOpen(!accountOpen)}
            className="ml-1 flex items-center gap-2 rounded-[var(--radius-sm)] py-1 pl-1 pr-2 outline-none hover:bg-[var(--color-surface-muted)] focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
            aria-label="Account menu"
            aria-expanded={accountOpen}
          >
            <span className="flex h-7 w-7 items-center justify-center rounded-full bg-[var(--color-primary-soft)] text-[11px] font-semibold text-[var(--color-primary)]">
              {initials}
            </span>
            <span className="hidden text-left leading-tight lg:block">
              <span className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                {user.fullName}
              </span>
              <span className="block text-[10px] text-[var(--color-text-muted)]">
                {ROLE_LABELS[user.role]}
              </span>
            </span>
            <Icon
              name="chevron-down"
              size={14}
              className="hidden text-[var(--color-text-muted)] lg:block"
            />
          </button>

          {accountOpen && (
            <div className="absolute right-0 top-10 z-40 w-56 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-2 shadow-xl shadow-black/10">
              <div className="border-b border-[var(--color-border)] px-2 pb-2">
                <p className="text-[12px] font-semibold text-[var(--color-text-primary)]">
                  Account
                </p>
                <p className="mt-0.5 truncate text-[11px] text-[var(--color-text-muted)]">
                  {user.email}
                </p>
                <p className="mt-1 text-[11px] text-[var(--color-text-muted)]">
                  {ROLE_LABELS[user.role]}
                </p>
              </div>
              <button
                type="button"
                onClick={handleLogout}
                className="mt-1 flex w-full items-center gap-2 rounded-[var(--radius-sm)] px-2 py-2 text-left text-[12px] font-medium text-[var(--color-danger)] hover:bg-[var(--color-danger-soft)]"
              >
                <Icon name="arrow-left" size={14} />
                Sign out
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}

function formatNotificationTime(isoString: string): string {
  try {
    const date = new Date(isoString);
    if (Number.isNaN(date.getTime())) return '';
    return date.toLocaleDateString(undefined, {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return '';
  }
}
