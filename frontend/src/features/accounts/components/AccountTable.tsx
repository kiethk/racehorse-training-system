'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { DataTable, type DataTableColumn } from '@/components/ui/DataTable';
import { Pill } from '@/components/ui/StatusBadge';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { formatEnumLabel } from '@/lib/display';
import type { AccountSummary } from '../types';

interface AccountTableProps {
  accounts: AccountSummary[];
  onToggleStatus: (userId: number, currentStatus: boolean) => Promise<void>;
  loadingActionId: number | null;
  onRowClick?: (userId: number) => void;
  currentUserId?: number;
}

export function AccountTable({
  accounts,
  onToggleStatus,
  loadingActionId,
  onRowClick,
  currentUserId,
}: AccountTableProps) {
  const [confirmToggle, setConfirmToggle] = useState<AccountSummary | null>(null);

  function handleToggle(account: AccountSummary) {
    if (account.userId === currentUserId && account.active) {
      return; // Protected against self-lockout
    }
    if (account.active) {
      setConfirmToggle(account);
    } else {
      void onToggleStatus(account.userId, account.active);
    }
  }

  const columns: DataTableColumn<AccountSummary>[] = [
    {
      id: 'name',
      header: 'Name / Email',
      sortValue: (account) => account.fullName,
      render: (account) => {
        const isSelf = account.userId === currentUserId;
        return (
          <div>
            <div className="flex items-center gap-1.5 font-medium text-[var(--color-text-primary)]">
              <span>{account.fullName}</span>
              {isSelf && (
                <span className="rounded bg-[var(--color-surface-muted)] px-1.5 py-0.5 text-[10px] font-semibold text-[var(--color-primary)]">
                  You
                </span>
              )}
            </div>
            <div className="text-xs text-[var(--color-text-muted)]">{account.email}</div>
          </div>
        );
      },
    },
    {
      id: 'role',
      header: 'Role',
      sortValue: (account) => account.role,
      render: (account) => (
        <span className="text-sm font-medium text-[var(--color-text-primary)]">
          {formatEnumLabel(account.role)}
        </span>
      ),
    },
    {
      id: 'summary',
      header: 'Role Context',
      render: (account) => {
        if (account.role === 'HORSE_OWNER') {
          const count = account.ownedHorsesCount ?? 0;
          return (
            <span className="inline-flex items-center gap-1 text-sm text-[var(--color-text-secondary)]">
              <span className="font-medium text-[var(--color-text-primary)]">{count}</span>
              <span>{count === 1 ? 'horse owned' : 'horses owned'}</span>
            </span>
          );
        }
        if (account.role === 'GROOM' && account.assignedAreaType === 'QUARANTINE') {
          return (
            <span className="inline-flex items-center gap-1.5 text-sm text-[var(--color-text-secondary)]">
              <span className="rounded bg-[var(--color-warning-soft)] px-1.5 py-0.5 text-[10px] font-semibold text-[var(--color-warning)]">
                Quarantine
              </span>
              <span>{account.assignedStallCodes?.join(', ') || 'Facility'}</span>
            </span>
          );
        }
        return (
          <span className="text-sm text-[var(--color-text-secondary)]">
            {account.profileSummary || '—'}
          </span>
        );
      },
      className: 'max-w-xs',
    },
    {
      id: 'status',
      header: 'Status',
      sortValue: (account) => (account.active ? 1 : 0),
      render: (account) => (
        <Pill tone={account.active ? 'success' : 'neutral'}>
          {account.active ? 'Active' : 'Inactive'}
        </Pill>
      ),
    },
    {
      id: 'actions',
      header: 'Actions',
      align: 'right',
      render: (account) => {
        const isSelf = account.userId === currentUserId;
        return (
          <div
            className="flex items-center justify-end gap-2"
            onClick={(event) => event.stopPropagation()}
          >
            {onRowClick && (
              <Button
                size="sm"
                variant="secondary"
                onClick={() => onRowClick(account.userId)}
              >
                View
              </Button>
            )}
            {isSelf && account.active ? (
              <span
                className="inline-block"
                title="You cannot deactivate your own administrator account"
              >
                <Button size="sm" variant="secondary" disabled>
                  Active (Self)
                </Button>
              </span>
            ) : (
              <Button
                size="sm"
                variant={account.active ? 'destructive' : 'primary'}
                loading={loadingActionId === account.userId}
                onClick={() => handleToggle(account)}
              >
                {account.active ? 'Deactivate' : 'Activate'}
              </Button>
            )}
          </div>
        );
      },
    },
  ];

  const getBlockingReason = (account: AccountSummary | null): string | null => {
    if (!account) return null;
    if (account.role === 'GROOM' && account.assignedStallCodes && account.assignedStallCodes.length > 0) {
      return `This Groom is currently assigned to stalls (${account.assignedStallCodes.join(', ')}). All stalls must be unassigned or transferred before deactivation.`;
    }
    if (account.role === 'HEAD_TRAINER' && account.managedAreaCodes && account.managedAreaCodes.length > 0) {
      return `This Head Trainer currently manages regular areas (${account.managedAreaCodes.join(', ')}). All regular areas must be reassigned before deactivation.`;
    }
    return null;
  };

  const blockingReason = getBlockingReason(confirmToggle);
  const isBlocked = !!blockingReason;

  return (
    <>
      <DataTable
        rows={accounts}
        columns={columns}
        getRowKey={(account) => account.userId}
        onRowClick={onRowClick ? (account) => onRowClick(account.userId) : undefined}
        ariaLabel="User accounts table"
        emptyTitle="No accounts found"
        emptyDescription="Try adjusting your search criteria or role filters."
        pageSize={10}
      />
      <ConfirmDialog
        open={!!confirmToggle}
        title={isBlocked ? 'Cannot Deactivate Account' : 'Deactivate User Account'}
        description={
          isBlocked ? (
            <div className="space-y-3">
              <p>
                You cannot deactivate <strong>{confirmToggle?.fullName}</strong> because they still have active assignments:
              </p>
              <div className="p-3 rounded-lg bg-[var(--color-bg-warning-subtle,#fffbeb)] border border-[var(--color-border-warning,#fef3c7)] text-xs text-[var(--color-warning-text,#92400e)] font-medium leading-relaxed">
                ⚠️ {blockingReason}
              </div>
              <p className="text-xs text-[var(--color-text-secondary)]">
                Please reassign or unassign these responsibilities first before deactivating this account.
              </p>
            </div>
          ) : (
            <div className="space-y-2">
              <p>
                Are you sure you want to deactivate <strong>{confirmToggle?.fullName}</strong>?
                They will immediately lose the ability to sign in to the platform.
              </p>
              {confirmToggle?.role === 'HORSE_OWNER' && (confirmToggle.ownedHorsesCount ?? 0) > 0 && (
                <p className="text-xs text-[var(--color-warning)] font-medium">
                  Note: If this Owner has active racehorses or pending admission applications, the system will verify those before deactivating.
                </p>
              )}
            </div>
          )
        }
        confirmLabel={isBlocked ? 'Cannot Deactivate' : 'Deactivate Account'}
        cancelLabel={isBlocked ? 'Close' : 'Cancel'}
        confirmDisabled={isBlocked}
        tone="danger"
        loading={loadingActionId === confirmToggle?.userId}
        onConfirm={async () => {
          if (confirmToggle && !isBlocked) {
            await onToggleStatus(confirmToggle.userId, confirmToggle.active);
            setConfirmToggle(null);
          }
        }}
        onCancel={() => setConfirmToggle(null)}
      />
    </>
  );
}
