'use client';

import { useEffect, useState } from 'react';
import { Button, EntityLink, ListSkeleton, Notice } from '@/components/ui';
import { displayError, formatDateTime } from '@/lib/display';
import { admissionsApi } from '../services/api';
import type { GroomAssignmentHistoryItem } from '../types';
import { AdmissionInfoSection, InfoRow } from '../shared/components/AdmissionInfoSection';

const reasons = {
  DECIDED: 'Decided',
  OVERDUE: 'Not decided in 72 hours',
  FULL: 'Stalls full',
  LOST_QUARANTINE: 'No longer a quarantine groom',
  MANUAL: 'Reassigned by manager',
  REOPENED: 'Reopened',
};

export function GroomAssignmentHistory({ admissionId }: { admissionId: number }) {
  const [rows, setRows] = useState<GroomAssignmentHistoryItem[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reload, setReload] = useState(0);
  useEffect(() => {
    let active = true;
    admissionsApi.getGroomAssignments(admissionId)
      .then(data => { if (active) { setRows(data); setError(null); } })
      .catch(cause => { if (active) setError(displayError(cause, 'Unable to load assignment history.')); });
    return () => { active = false; };
  }, [admissionId, reload]);

  return (
    <AdmissionInfoSection title="Assignment history">
      {!rows && !error && <ListSkeleton rows={3} />}
      {error && <Notice tone="error">{error}</Notice>}
      {error && <Button size="sm" onClick={() => { setError(null); setReload(value => value + 1); }}>Retry</Button>}
      {rows?.length === 0 && <span>No assignments recorded</span>}
      {rows?.map(row => (
        <div key={row.id} className="space-y-3 border-b border-[var(--color-border)] pb-3 last:border-0 last:pb-0">
          <InfoRow label="Groom" value={<EntityLink href={`/manager/accounts?userId=${row.groomId}`}>{row.groomName}</EntityLink>} />
          <InfoRow label="Assigned at" value={formatDateTime(row.assignedAt)} />
          <InfoRow label="Ended at" value={row.endedAt ? formatDateTime(row.endedAt) : 'Ongoing'} />
          <InfoRow label="Reason" value={row.endReason ? reasons[row.endReason] : 'Ongoing'} />
          <InfoRow label="Assigned by" value={row.assignedByName ?? 'System'} />
        </div>
      ))}
    </AdmissionInfoSection>
  );
}
