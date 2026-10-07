import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Pill } from '@/components/ui/StatusBadge';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow, TableShell } from '@/components/ui/Table';
import type { StaffSummary } from '../types';

interface StaffTableProps {
  staff: StaffSummary[];
  onToggleStatus: (userId: number, currentStatus: boolean) => Promise<void>;
  loadingActionId: number | null;
  onRowClick?: (userId: number) => void;
}

export function StaffTable({ staff, onToggleStatus, loadingActionId, onRowClick }: StaffTableProps) {
  const [confirmToggle, setConfirmToggle] = useState<StaffSummary | null>(null);

  if (staff.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] py-20 text-center">
        <p className="text-[14px] font-medium text-[var(--color-text-primary)]">No staff members found.</p>
        <p className="mt-1 text-[13px] text-[var(--color-text-secondary)]">Try adjusting your filters or search query.</p>
      </div>
    );
  }

  function handleToggle(staffMember: StaffSummary) {
    if (staffMember.active) {
      // Ask for confirmation to deactivate
      setConfirmToggle(staffMember);
    } else {
      // Activate immediately without confirmation
      onToggleStatus(staffMember.userId, staffMember.active);
    }
  }

  return (
    <>
      <TableShell>
        <Table>
          <TableHeader>
            <tr>
              <TableHead>Name / Email</TableHead>
              <TableHead>Role</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Profile Summary</TableHead>
              <TableHead align="right">Actions</TableHead>
            </tr>
          </TableHeader>
          <TableBody>
            {staff.map((s) => (
              <TableRow
                key={s.userId} 
                className="cursor-pointer"
                onClick={() => onRowClick?.(s.userId)}
              >
                <TableCell>
                  <div className="font-medium text-[var(--color-text-primary)]">{s.fullName}</div>
                  <div className="text-[12px] text-[var(--color-text-muted)]">{s.email}</div>
                </TableCell>
                <TableCell>
                  <span className="inline-flex items-center rounded-full bg-[var(--color-border-strong)]/30 px-2.5 py-0.5 text-[11px] font-medium text-[var(--color-text-secondary)]">
                    {s.role.replace('_', ' ')}
                  </span>
                </TableCell>
                <TableCell>
                  <Pill tone={s.active ? 'success' : 'neutral'}>
                    {s.active ? 'Active' : 'Inactive'}
                  </Pill>
                </TableCell>
                <TableCell className="text-[12px] text-[var(--color-text-secondary)]">
                  {s.profileSummary || '-'}
                </TableCell>
                <TableCell align="right">
                  <Button
                    size="sm"
                    variant={s.active ? 'destructive' : 'primary'}
                    loading={loadingActionId === s.userId}
                    onClick={(e) => {
                      e.stopPropagation();
                      handleToggle(s);
                    }}
                  >
                    {s.active ? 'Deactivate' : 'Activate'}
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableShell>

      <ConfirmDialog
        open={!!confirmToggle}
        title="Deactivate Staff Account"
        description={
          <>
            Are you sure you want to deactivate <strong>{confirmToggle?.fullName}</strong>? 
            They will no longer be able to log in to the system.
          </>
        }
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
