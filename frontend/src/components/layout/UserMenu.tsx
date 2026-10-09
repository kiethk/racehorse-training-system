'use client';

import { useRouter } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import { Avatar } from '@/components/ui/Avatar';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/DropdownMenu';
import { Icon } from '@/components/ui/Icon';
import { cn } from '@/lib/cn';
import { ROLE_LABELS } from '@/lib/roleRoute';

export function UserMenu({ collapsed }: { collapsed: boolean }) {
  const { user, logout } = useAuth();
  const router = useRouter();

  if (!user) return null;

  const handleLogout = async () => {
    await logout();
    router.replace('/login');
  };

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        aria-label="Account menu"
        className={cn(
          'flex w-full items-center gap-2.5 rounded-[var(--radius-md)] p-1.5 text-left outline-none transition-colors',
          'hover:bg-[var(--color-surface-muted)] focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]',
          'data-[state=open]:bg-[var(--color-surface-muted)]',
          collapsed && 'justify-center',
        )}
      >
        <Avatar name={user.fullName} />
        {!collapsed && (
          <>
            <span className="min-w-0 flex-1 leading-tight">
              <span className="block truncate text-xs font-medium text-[var(--color-text-primary)]">
                {user.fullName}
              </span>
              <span className="block truncate text-[11px] text-[var(--color-text-muted)]">
                {ROLE_LABELS[user.role]}
              </span>
            </span>
            <Icon name="more" size={16} className="shrink-0 text-[var(--color-text-muted)]" />
          </>
        )}
      </DropdownMenuTrigger>

      <DropdownMenuContent side={collapsed ? 'right' : 'top'} align={collapsed ? 'end' : 'start'} className="w-56">
        <DropdownMenuLabel>
          <p className="text-xs font-semibold text-[var(--color-text-primary)]">{user.fullName}</p>
          <p className="mt-0.5 truncate text-[11px] font-normal text-[var(--color-text-muted)]">{user.email}</p>
          <p className="mt-1 text-[11px] font-normal text-[var(--color-text-muted)]">{ROLE_LABELS[user.role]}</p>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem tone="danger" onSelect={() => void handleLogout()}>
          <Icon name="log-out" size={14} />
          Sign out
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
