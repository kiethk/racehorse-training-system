'use client';

import { Icon } from '@/components/ui/Icon';
import { Tooltip } from '@/components/ui/Tooltip';
import { NotificationBell } from './NotificationBell';

export function TopBar({
  title,
  sidebarCollapsed,
  canToggleSidebar,
  onToggleSidebar,
}: {
  title: string;
  sidebarCollapsed: boolean;
  canToggleSidebar: boolean;
  onToggleSidebar: () => void;
}) {
  const toggleLabel = sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar';

  return (
    <header className="sticky top-0 z-[var(--z-sidebar)] flex h-[var(--topbar-height)] shrink-0 items-center gap-3 border-b border-[var(--color-border)] bg-[var(--color-surface)] px-4">
      {canToggleSidebar && (
        <Tooltip content={toggleLabel} side="bottom">
          <button
            type="button"
            onClick={onToggleSidebar}
            aria-label={toggleLabel}
            aria-expanded={!sidebarCollapsed}
            className="flex h-9 w-9 items-center justify-center rounded-[var(--radius-md)] text-[var(--color-text-secondary)] outline-none transition-colors hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-text-primary)] focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
          >
            <Icon name="panel-left" size={18} />
          </button>
        </Tooltip>
      )}
      <p className="min-w-0 truncate text-sm font-semibold text-[var(--color-text-primary)]">{title}</p>
      <div className="ml-auto flex shrink-0 items-center gap-1.5">
        <NotificationBell />
      </div>
    </header>
  );
}
