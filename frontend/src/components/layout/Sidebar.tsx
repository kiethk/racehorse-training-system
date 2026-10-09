'use client';

import Link from 'next/link';
import type { NavSection } from '@/config/navigation';
import { BrandLogo } from '@/components/ui/BrandLogo';
import { cn } from '@/lib/cn';
import { getRoleRoute } from '@/lib/roleRoute';
import type { Role } from '@/types/auth';
import { SidebarNav } from './SidebarNav';
import { UserMenu } from './UserMenu';

export function Sidebar({
  sections,
  role,
  collapsed,
}: {
  sections: NavSection[];
  role: Role;
  collapsed: boolean;
}) {
  return (
    <aside
      className={cn(
        'sticky top-0 z-[var(--z-sidebar)] flex h-dvh shrink-0 flex-col border-r border-[var(--color-border)] bg-[var(--color-surface)]',
        'transition-[width] duration-[var(--duration-base)] ease-[var(--ease-out)]',
        collapsed ? 'w-[var(--sidebar-width-collapsed)]' : 'w-[var(--sidebar-width)]',
      )}
    >
      <Link
        href={getRoleRoute(role)}
        aria-label="RTMS home"
        className={cn(
          'flex h-[var(--topbar-height)] shrink-0 items-center gap-2.5 border-b border-[var(--color-border)] px-4 outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-[var(--color-focus)]',
          collapsed && 'justify-center px-0',
        )}
      >
        <BrandLogo className="h-8 w-8 shrink-0" />
        {!collapsed && (
          <span className="leading-tight">
            <span className="block text-[15px] font-semibold tracking-tight text-[var(--color-text-primary)]">
              RTMS
            </span>
            <span className="block text-[11px] text-[var(--color-text-muted)]">Racehorse Training</span>
          </span>
        )}
      </Link>

      <SidebarNav sections={sections} role={role} collapsed={collapsed} />

      <div className="shrink-0 border-t border-[var(--color-border)] p-3">
        <UserMenu collapsed={collapsed} />
      </div>
    </aside>
  );
}
