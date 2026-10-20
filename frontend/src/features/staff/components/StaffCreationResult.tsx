'use client';
import { Button, LinkButton, Modal, Pill } from '@/components/ui';
import { formatEnumLabel } from '@/lib/display';
import type { StaffCreationResponse } from '../types';

export function StaffCreationResult({ result, onClose }: { result: StaffCreationResponse; onClose: () => void }) {
  const hasAllocation = result.role === 'GROOM' || result.role === 'HEAD_TRAINER';
  const assigned = result.role === 'GROOM' ? result.assignmentStatus === 'ASSIGNED' : !!result.assignedAreaCodes?.length;
  return <Modal open title="Staff created" onClose={onClose} footer={<>
    {hasAllocation && <LinkButton href="/manager/stable" onClick={onClose} variant="secondary">Open Stable</LinkButton>}
    <Button variant="primary" onClick={onClose}>Done</Button>
  </>}>
    <div className="space-y-5">
      <div className="space-y-1.5"><p className="text-lg font-semibold">{result.fullName}</p>
        <p className="break-all text-sm text-[var(--color-text-secondary)]">{result.email}</p>
        <Pill tone="neutral">{formatEnumLabel(result.role)}</Pill></div>
      {hasAllocation && <div className="space-y-4 border-t border-[var(--color-border)] pt-4">
        <div className="flex items-center justify-between"><p className="text-sm font-medium">Assignment</p>
          <Pill tone={assigned ? 'success' : 'warning'}>{assigned ? 'Assigned' : 'Unassigned'}</Pill></div>
        {!assigned ? <p className="text-sm text-[var(--color-text-secondary)]">
          {result.role === 'GROOM' ? 'No available three-stall block.' : 'No available regular areas.'}
        </p> : result.role === 'HEAD_TRAINER' ? <div className="flex flex-wrap gap-2">
          {result.assignedAreaCodes?.map(code => <Pill key={code} tone="primary">Area {code}</Pill>)}
        </div> : <dl className="space-y-3 text-sm">
          <div className="flex justify-between gap-4"><dt className="text-[var(--color-text-secondary)]">Area</dt><dd>{result.assignedAreaCode}</dd></div>
          {result.assignedAreaType === 'QUARANTINE' ? <div className="flex justify-between gap-4"><dt className="text-[var(--color-text-secondary)]">Type</dt><dd>Quarantine</dd></div>
            : <div className="flex justify-between gap-4"><dt className="text-[var(--color-text-secondary)]">Trainer</dt><dd>{result.trainerName}</dd></div>}
          <div className="flex items-start justify-between gap-4"><dt className="text-[var(--color-text-secondary)]">Stalls</dt><dd className="flex flex-wrap justify-end gap-1.5">
            {result.assignedStallCodes?.map(code => <Pill key={code} tone="primary">{code}</Pill>)}
          </dd></div>
        </dl>}
      </div>}
    </div>
  </Modal>;
}
