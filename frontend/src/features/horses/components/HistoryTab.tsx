'use client';

import { useEffect, useState } from 'react';

import { Button, EmptyState, Panel, Skeleton, LinkButton } from '@/components/ui';
import { AdmissionStatusBadge } from '@/features/admissions/shared/components/AdmissionStatusBadge';
import type { HorseAdmissionHistory } from '../types';
import { horseService } from '../services/horseService';
import { formatDateTime, displayError } from '@/lib/display';

interface HistoryTabProps {
  horseId: number;
}

type LoadState = 'loading' | 'error' | 'done';

function MilestoneRow({ label, name, at }: { label: string; name: string | null; at: string | null }) {
  if (!name && !at) return null;
  return (
    <div className="flex items-start gap-3 py-2">
      <div className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-[var(--color-border-strong)]" />
      <div className="min-w-0 space-y-0.5">
        <p className="text-xs font-medium text-[var(--color-text-secondary)]">{label}</p>
        {name && <p className="text-sm text-[var(--color-text-primary)]">{name}</p>}
        {at && <p className="text-xs text-[var(--color-text-muted)]">{formatDateTime(at)}</p>}
      </div>
    </div>
  );
}

function AdmissionHistoryCard({ record }: { record: HorseAdmissionHistory }) {
  return (
    <Panel className="overflow-hidden">
      {/* Header */}
      <div className="flex items-center justify-between gap-3 border-b border-[var(--color-border)] px-5 py-3">
        <div className="flex items-center gap-3">
          <AdmissionStatusBadge status={record.status} size="sm" />
          <span className="text-xs text-[var(--color-text-muted)]">
            #{record.admissionId}
          </span>
        </div>
        <LinkButton
          href={`/manager/admissions/${record.admissionId}`}
          size="sm"
          variant="secondary"
        >
          View
        </LinkButton>
      </div>

      {/* Timeline */}
      <div className="divide-y divide-[var(--color-border)] px-5">
        {record.submittedAt && (
          <MilestoneRow label="Submitted" name={null} at={record.submittedAt} />
        )}
        {record.groomReviewedAt && (
          <MilestoneRow label="Groom review" name={record.groomName} at={record.groomReviewedAt} />
        )}
        {record.arrivedAt && (
          <MilestoneRow label="Horse arrived" name={null} at={record.arrivedAt} />
        )}
        {record.vetReviewedAt && (
          <MilestoneRow label="Vet examination" name={record.vetName} at={record.vetReviewedAt} />
        )}
        {record.trainerReviewedAt && (
          <MilestoneRow label="Trainer review" name={record.trainerName} at={record.trainerReviewedAt} />
        )}
        {record.managerReviewedAt && (
          <MilestoneRow label="Manager review" name={record.managerName} at={record.managerReviewedAt} />
        )}
      </div>
    </Panel>
  );
}

export function HistoryTab({ horseId }: HistoryTabProps) {
  const [state, setState] = useState<LoadState>('loading');
  const [error, setError] = useState<string | null>(null);
  const [records, setRecords] = useState<HorseAdmissionHistory[]>([]);
  const [retryKey, setRetryKey] = useState(0);

  useEffect(() => {
    let active = true;

    async function loadHistory() {
      setState('loading');
      setError(null);
      try {
        const data = await horseService.getAdmissionHistory(horseId);
        if (active) {
          setRecords(data);
          setState('done');
        }
      } catch (err) {
        if (active) {
          setError(displayError(err, 'Unable to load admission history.'));
          setState('error');
        }
      }
    }

    void loadHistory();

    return () => {
      active = false;
    };
  }, [horseId, retryKey]);

  if (state === 'loading') {
    return (
      <div className="space-y-4">
        {[1, 2].map((i) => (
          <Panel key={i} className="space-y-3 p-5">
            <Skeleton className="h-4 w-24" />
            <Skeleton className="h-3 w-48" />
            <Skeleton className="h-3 w-36" />
          </Panel>
        ))}
      </div>
    );
  }

  if (state === 'error') {
    return (
      <Panel padded>
        <EmptyState
          icon="alert-triangle"
          title="Unable to load history"
          description={error ?? 'Admission history could not be loaded.'}
          action={
            <Button size="sm" icon="refresh" onClick={() => setRetryKey((k) => k + 1)}>
              Retry
            </Button>
          }
        />
      </Panel>
    );
  }

  if (records.length === 0) {
    return (
      <Panel padded>
        <EmptyState
          icon="file-text"
          title="No admission history"
          description="No admission applications are linked to this horse."
        />
      </Panel>
    );
  }

  return (
    <div className="space-y-4">
      {records.map((record) => (
        <AdmissionHistoryCard key={record.admissionId} record={record} />
      ))}
    </div>
  );
}
