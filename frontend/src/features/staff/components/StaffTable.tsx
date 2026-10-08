import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { DataTable, type DataTableColumn } from '@/components/ui/DataTable';
import { Pill } from '@/components/ui/StatusBadge';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import type { StaffSummary } from '../types';

interface StaffTableProps {
  staff: StaffSummary[];
  onToggleStatus: (userId: number, currentStatus: boolean) => Promise<void>;
  loadingActionId: number | null;
  onRowClick?: (userId: number) => void;
}

export function StaffTable({ staff, onToggleStatus, loadingActionId, onRowClick }: StaffTableProps) {
  const [confirmToggle, setConfirmToggle] = useState<StaffSummary | null>(null);

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
        title="Deactivate Staff Account"
        description={<>Are you sure you want to deactivate <strong>{confirmToggle?.fullName}</strong>? They will no longer be able to sign in.</>}
        confirmLabel="Deactivate Account"
        tone="danger"
        loading={loadingActionId === confirmToggle?.userId}
        onConfirm={async () => {
          if (confirmToggle) {
            await onToggleStatus(confirmToggle.userId, confirmToggle.active);
            setConfirmToggle(null);
          }
        }}
        onCancel={() => setConfirmToggle(null)}
      />
    </>
  );
}
