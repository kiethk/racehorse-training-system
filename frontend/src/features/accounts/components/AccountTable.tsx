'use client';

import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/Button';
import { DataTable, type DataTableColumn } from '@/components/ui/DataTable';
import { Pill } from '@/components/ui/StatusBadge';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { formatEnumLabel } from '@/lib/display';
import type { AccountSummary } from '../types';
import { getEligibleReplacementTrainers, type EligibleReplacementTrainer } from '../services/accountService';

interface AccountTableProps {
  accounts: AccountSummary[];
  onToggleStatus: (userId: number, currentStatus: boolean, replacementTrainerId?: number) => Promise<void>;
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
  const [eligibleReplacements, setEligibleReplacements] = useState<EligibleReplacementTrainer[]>([]);
  const [loadingReplacements, setLoadingReplacements] = useState(false);
  const [selectedReplacementId, setSelectedReplacementId] = useState<number | ''>('');

  useEffect(() => {
    if (confirmToggle?.role === 'HEAD_TRAINER' && (confirmToggle.managedAreaCodes?.length ?? 0) > 0) {
      setLoadingReplacements(true);
      setSelectedReplacementId('');
      getEligibleReplacementTrainers(confirmToggle.userId)
        .then((res) => {
          const list = res.data || [];
          setEligibleReplacements(list);
          const firstAcceptable = list.find((t) => t.canAccept);
          if (firstAcceptable) {
            setSelectedReplacementId(firstAcceptable.id);
          }
        })
        .catch(() => {
          setEligibleReplacements([]);
        })
        .finally(() => {
          setLoadingReplacements(false);
        });
    } else {
      setEligibleReplacements([]);
      setSelectedReplacementId('');
      setLoadingReplacements(false);
    }
  }, [confirmToggle]);

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
    if (account.role === 'HORSE_OWNER' && (account.ownedHorsesCount ?? 0) > 0) {
      return `This Owner currently has ${account.ownedHorsesCount} registered horse(s) in the system. All horses must be discharged, transferred, or resolved before deactivation.`;
    }
    return null;
  };

  const blockingReason = getBlockingReason(confirmToggle);
  const isBlocked = !!blockingReason;
  const isTrainerHandover = confirmToggle?.role === 'HEAD_TRAINER' && (confirmToggle.managedAreaCodes?.length ?? 0) > 0;
  const hasAcceptableCandidate = eligibleReplacements.some((t) => t.canAccept);
  const selectedCandidate = eligibleReplacements.find((t) => t.id === Number(selectedReplacementId));
  const isCandidateEligible = !!selectedCandidate?.canAccept;

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
        title={
          isBlocked
            ? 'Cannot Deactivate Account'
            : isTrainerHandover
            ? 'Handover & Deactivate Head Trainer'
            : 'Deactivate User Account'
        }
        description={
          isBlocked ? (
            <div className="space-y-3">
              <p>
                You cannot deactivate <strong>{confirmToggle?.fullName}</strong> because they still have active{' '}
                {confirmToggle?.role === 'HORSE_OWNER' ? 'horses' : 'assignments'}:
              </p>
              <div className="p-3 rounded-lg bg-[var(--color-bg-warning-subtle,#fffbeb)] border border-[var(--color-border-warning,#fef3c7)] text-xs text-[var(--color-warning-text,#92400e)] font-medium leading-relaxed">
                 {blockingReason}
              </div>
              <p className="text-xs text-[var(--color-text-secondary)]">
                Please{' '}
                {confirmToggle?.role === 'HORSE_OWNER'
                  ? 'discharge or transfer all horses'
                  : 'reassign or unassign these responsibilities'}{' '}
                first before deactivating this account.
              </p>
            </div>
          ) : isTrainerHandover ? (
            <div className="space-y-3">
              <p>
                <strong>{confirmToggle?.fullName}</strong> currently manages regular area(s):{' '}
                <strong>{confirmToggle?.managedAreaCodes?.map(c => c.startsWith('Area ') ? c : `Area ${c}`).join(', ')}</strong>.
              </p>
              <p className="text-xs text-[var(--color-text-secondary)]">
                All managed regular areas, subordinate grooms, active training groups, plans, upcoming sessions, and pending admissions must be handed over to an active replacement Head Trainer.
              </p>
              {loadingReplacements ? (
                <div className="p-3 text-xs text-[var(--color-text-muted)] bg-[var(--color-surface-muted)] rounded-md">
                  Loading eligible replacement trainers...
                </div>
              ) : !hasAcceptableCandidate ? (
                <div className="p-3 rounded-lg bg-[var(--color-bg-warning-subtle,#fffbeb)] border border-[var(--color-border-warning,#fef3c7)] text-xs text-[var(--color-warning-text,#92400e)] font-medium leading-relaxed">
                  ⚠️ All active Head Trainers are currently at full capacity (max 2 regular areas each). No trainer has remaining capacity to take over. Please register a new Head Trainer before deactivating this account.
                </div>
              ) : (
                <div className="space-y-1.5 pt-1">
                  <label htmlFor="account-replacement-trainer-select" className="block text-xs font-semibold text-[var(--color-text-secondary)]">
                    Replacement Head Trainer (Required)
                  </label>
                  <select
                    id="account-replacement-trainer-select"
                    value={selectedReplacementId}
                    onChange={(e) => setSelectedReplacementId(e.target.value ? Number(e.target.value) : '')}
                    className="w-full rounded-md border border-[var(--color-border-default,#e2e8f0)] bg-[var(--color-surface,#ffffff)] px-3 py-2 text-xs text-[var(--color-text-primary,#0f172a)] focus:outline-none focus:ring-2 focus:ring-[var(--color-primary-500,#3b82f6)]"
                  >
                    <option value="">-- Select a replacement Head Trainer --</option>
                    {eligibleReplacements.map((t) => (
                      <option key={t.id} value={t.id} disabled={!t.canAccept}>
                        {t.fullName} (Currently managing {t.managedAreasCount}/2 areas{!t.canAccept ? ' - Full capacity' : ''})
                      </option>
                    ))}
                  </select>
                  <p className="text-[11px] text-[var(--color-text-muted)]">
                    Responsibilities will be atomically transferred to the selected trainer upon deactivation.
                  </p>
                </div>
              )}
            </div>
          ) : (
            <div className="space-y-2">
              <p>
                Are you sure you want to deactivate <strong>{confirmToggle?.fullName}</strong>?
                They will immediately lose the ability to sign in to the platform.
              </p>
              {confirmToggle?.role === 'VETERINARIAN' && (
                <p className="text-xs text-[var(--color-text-secondary)] bg-[var(--color-surface-muted)] p-2.5 rounded-md">
                  Any pending or in-progress care schedules will be automatically reassigned to other active veterinarians.
                </p>
              )}
              {confirmToggle?.role === 'HEAD_TRAINER' && (
                <p className="text-xs text-[var(--color-text-secondary)] bg-[var(--color-surface-muted)] p-2.5 rounded-md">
                  Any pending admission applications in review will be automatically reassigned to other active head trainers.
                </p>
              )}
            </div>
          )
        }
        confirmLabel={
          isBlocked
            ? 'Cannot Deactivate'
            : isTrainerHandover
            ? 'Handover & Deactivate'
            : 'Deactivate Account'
        }
        cancelLabel={isBlocked ? 'Close' : 'Cancel'}
        confirmDisabled={isBlocked || (isTrainerHandover && (loadingReplacements || !hasAcceptableCandidate || !selectedReplacementId || !isCandidateEligible))}
        tone="danger"
        loading={loadingActionId === confirmToggle?.userId}
        onConfirm={async () => {
          if (confirmToggle && !isBlocked) {
            if (isTrainerHandover) {
              if (!selectedReplacementId || !isCandidateEligible) return;
              await onToggleStatus(confirmToggle.userId, confirmToggle.active, Number(selectedReplacementId));
            } else {
              await onToggleStatus(confirmToggle.userId, confirmToggle.active);
            }
            setConfirmToggle(null);
          }
        }}
        onCancel={() => setConfirmToggle(null)}
      />
    </>
  );
}
