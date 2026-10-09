'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { isNavItemActive, type NavItem, type NavSection } from '@/config/navigation';
import { Icon } from '@/components/ui/Icon';
import { Tooltip } from '@/components/ui/Tooltip';
import { cn } from '@/lib/cn';
import type { Role } from '@/types/auth';

const itemBase =
  'relative flex h-9 items-center gap-3 rounded-[var(--radius-md)] px-3 text-sm font-medium outline-none ' +
  'transition-colors duration-[var(--duration-fast)] focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]';

export function SidebarNav({
  sections,
  role,
  collapsed,
}: {
  sections: NavSection[];
  role: Role;
  collapsed: boolean;
}) {
  const pathname = usePathname();

  return (
    <nav aria-label="Primary" className="scroll-slim flex-1 space-y-4 overflow-y-auto overflow-x-hidden px-3 py-3">
      {sections.map((section) => (
        <div key={section.id}>
          {section.label && (
            <p
              className={cn(
                'mb-1 px-3 text-[11px] font-semibold uppercase tracking-wider text-[var(--color-text-muted)]',
                collapsed && 'sr-only',
              )}
            >
              {section.label}
            </p>
          )}
          <ul className="space-y-0.5">
            {section.items.map((item) => (
              <li key={item.id}>
                <NavLink item={item} active={isNavItemActive(pathname, item, role)} collapsed={collapsed} />
              </li>
            ))}
          </ul>
        </div>
      ))}
    </nav>
  );
}

function NavLink({ item, active, collapsed }: { item: NavItem; active: boolean; collapsed: boolean }) {
  const label = <span className={cn('truncate', collapsed && 'sr-only')}>{item.label}</span>;

  if (!item.href) {
    return (
      <Tooltip content={`${item.label} — coming soon`} side="right">
        <span
          aria-disabled="true"
          className={cn(itemBase, 'cursor-not-allowed text-[var(--color-text-muted)] opacity-60')}
        >
          <Icon name={item.icon} size={18} className="shrink-0" />
          {label}
          {!collapsed && (
            <span className="ml-auto rounded-full bg-[var(--color-surface-muted)] px-1.5 py-0.5 text-[10px] font-medium">
              Soon
            </span>
          )}
        </span>
      </Tooltip>
    );
  }

  return (
    <Tooltip content={item.label} side="right" disabled={!collapsed}>
      <Link
        href={item.href}
        aria-current={active ? 'page' : undefined}
        className={cn(
          itemBase,
          active
            ? 'bg-[var(--color-primary-soft)] text-[var(--color-primary)]'
            : 'text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-text-primary)]',
        )}
      >
        {active && (
          <span
            aria-hidden="true"
            className="absolute inset-y-1.5 left-0 w-[3px] rounded-full bg-[var(--color-primary)] animate-in fade-in zoom-in-50 duration-200"
          />
        )}
        <Icon name={item.icon} size={18} className="shrink-0" />
        {label}
      </Link>
    </Tooltip>
  );
}
