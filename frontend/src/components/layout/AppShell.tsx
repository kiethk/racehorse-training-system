'use client';

import type { ReactNode } from 'react';
import { usePathname } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import { getNavigationForRole, isNavItemActive } from '@/config/navigation';
import { Sidebar } from './Sidebar';
import { TopBar } from './TopBar';
import { useSidebarState } from './useSidebarState';

/** Persistent frame for every signed-in page: left sidebar, top bar, content. */
export function AppShell({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const pathname = usePathname();
  const { collapsed, canToggle, toggle } = useSidebarState();

  if (!user) return null;

  const sections = getNavigationForRole(user.role);
  const activeItem = sections
    .flatMap((section) => section.items)
    .find((item) => isNavItemActive(pathname, item, user.role));

  return (
    <div className="flex min-h-dvh min-w-0 bg-[var(--color-background)]">
      <Sidebar sections={sections} role={user.role} collapsed={collapsed} />
      <div className="flex min-w-0 flex-1 flex-col">
        <TopBar
          title={activeItem?.label ?? 'RTMS'}
          sidebarCollapsed={collapsed}
          canToggleSidebar={canToggle}
          onToggleSidebar={toggle}
        />
        <main className="flex min-h-0 min-w-0 flex-1 flex-col">{children}</main>
      </div>
    </div>
  );
}
