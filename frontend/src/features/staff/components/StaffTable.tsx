import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/Button';
import { DataTable, type DataTableColumn } from '@/components/ui/DataTable';
import { Pill } from '@/components/ui/StatusBadge';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import type { StaffSummary } from '../types';
import { getEligibleReplacementTrainers, type EligibleReplacementTrainer } from '../services/staffService';

interface StaffTableProps {
  staff: StaffSummary[];
  onToggleStatus: (userId: number, currentStatus: boolean, replacementTrainerId?: number) => Promise<void>;
  loadingActionId: number | null;
  onRowClick?: (userId: number) => void;
}

export function StaffTable({ staff, onToggleStatus, loadingActionId, onRowClick }: StaffTableProps) {
  const [confirmToggle, setConfirmToggle] = useState<StaffSummary | null>(null);
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

  function handleToggle(staffMember: StaffSummary) {
    if (staffMember.active) setConfirmToggle(staffMember);
    else void onToggleStatus(staffMember.userId, staffMember.active);
  }

  const columns: DataTableColumn<StaffSummary>[] = [
    {
      id: 'name',
      header: 'Name / Email',
      sortValue: (staffMember) => staffMember.fullName,
      render: (staffMember) => (
        <div>
          <div className="font-medium text-[var(--color-text-primary)]">{staffMember.fullName}</div>
          <div className="text-xs text-[var(--color-text-muted)]">{staffMember.email}</div>
        </div>
      ),
    },
    {
      id: 'role',
      header: 'Role',
      sortValue: (staffMember) => staffMember.role,
      render: (staffMember) => (
        <span className="inline-flex rounded-full bg-[var(--color-surface-muted)] px-2.5 py-1 text-xs font-medium text-[var(--color-text-secondary)]">
          {staffMember.role.replaceAll('_', ' ')}
        </span>
      ),
    },
    {
      id: 'status',
      header: 'Status',
      sortValue: (staffMember) => staffMember.active ? 1 : 0,
      render: (staffMember) => <Pill tone={staffMember.active ? 'success' : 'neutral'}>{staffMember.active ? 'Active' : 'Inactive'}</Pill>,
    },
    {
      id: 'summary',
      header: 'Profile Summary',
      render: (staffMember) => staffMember.profileSummary || '—',
      className: 'max-w-sm',
    },
    {
      id: 'actions',
      header: 'Actions',
      align: 'right',
      render: (staffMember) => (
        <Button
          size="sm"
          variant={staffMember.active ? 'destructive' : 'primary'}
          loading={loadingActionId === staffMember.userId}
          onClick={(event) => {
            event.stopPropagation();
            handleToggle(staffMember);
          }}
        >
          {staffMember.active ? 'Deactivate' : 'Activate'}
        </Button>
      ),
    },
  ];

  const isTrainerHandover = confirmToggle?.role === 'HEAD_TRAINER' && (confirmToggle.managedAreaCodes?.length ?? 0) > 0;
  const hasAcceptableCandidate = eligibleReplacements.some((t) => t.canAccept);
  const selectedCandidate = eligibleReplacements.find((t) => t.id === Number(selectedReplacementId));
  const isCandidateEligible = !!selectedCandidate?.canAccept;

  return (
    <>
      <DataTable
        rows={staff}
        columns={columns}
        getRowKey={(staffMember) => staffMember.userId}
        onRowClick={onRowClick ? (staffMember) => onRowClick(staffMember.userId) : undefined}
        ariaLabel="Staff members"
        emptyTitle="No staff members found"
        emptyDescription="Try adjusting your filters or search query."
        pageSize={10}
      />
      <ConfirmDialog
        open={!!confirmToggle}
        title={isTrainerHandover ? 'Handover & Deactivate Head Trainer' : 'Deactivate Staff Account'}
        description={
          isTrainerHandover ? (
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
                  <label htmlFor="staff-replacement-trainer-select" className="block text-xs font-semibold text-[var(--color-text-secondary)]">
                    Replacement Head Trainer (Required)
                  </label>
                  <select
                    id="staff-replacement-trainer-select"
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
                Are you sure you want to deactivate <strong>{confirmToggle?.fullName}</strong>? They will no longer be able to sign in.
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
        confirmLabel={isTrainerHandover ? 'Handover & Deactivate' : 'Deactivate Account'}
        confirmDisabled={isTrainerHandover && (loadingReplacements || !hasAcceptableCandidate || !selectedReplacementId || !isCandidateEligible)}
        tone="danger"
        loading={loadingActionId === confirmToggle?.userId}
        onConfirm={async () => {
          if (confirmToggle) {
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
