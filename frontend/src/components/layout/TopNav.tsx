'use client';

import { useState } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import { getNavigationForRole } from '@/config/navigation';
import { BrandLogo } from '@/components/ui/BrandLogo';
import { Icon } from '@/components/ui/Icon';
import { ROLE_LABELS } from '@/lib/roleRoute';

export function TopNav() {
  const { user, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const [accountOpen, setAccountOpen] = useState(false);

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
          const isActive = item.href && pathname.startsWith(item.href);
          
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

      {/* User / Account Menu */}
      <div className="ml-auto flex items-center gap-1 md:ml-auto md:flex-none">
        <div className="relative">
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
