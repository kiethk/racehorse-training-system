'use client';

import { EmptyState, Panel, Pill } from '@/components/ui';
import { formatDateTime, formatEnumLabel } from '@/lib/display';
import type { HealthRecord } from '../types';

function Detail({ label, value }: { label: string; value: string | null }) {
  if (!value) return null;
  return (
    <p className="text-sm text-[var(--color-text-secondary)]">
      <span className="text-[var(--color-text-muted)]">{label}: </span>
      {value}
    </p>
  );
}

/**
 * Health history of one horse: every recorded examination from intake to the most
 * recent one, most recent first. A chronological list of records, not a clock axis.
 */
export function HorseHealthHistory({ records }: { records: HealthRecord[] }) {
  if (records.length === 0) {
    return (
      <Panel padded>
        <EmptyState
          icon="file-text"
          title="No health history yet"
          description="No examination has been recorded for this horse since it entered the stable."
        />
      </Panel>
    );
  }

  return (
    <Panel padded className="space-y-3">
      <div>
        <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">Health history</h3>
        <p className="text-xs text-[var(--color-text-muted)]">
          Every recorded examination for this horse, most recent first.
        </p>
      </div>

      <ol className="space-y-3">
        {records.map((record) => (
          <li key={record.id} className="flex gap-3">
            <span
              aria-hidden="true"
              className="mt-1.5 h-2.5 w-2.5 shrink-0 rounded-full bg-[var(--color-primary)]"
            />
            <div className="min-w-0 flex-1 space-y-1 border-b border-[var(--color-border)] pb-3">
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-sm font-semibold text-[var(--color-text-primary)]">
                  {formatDateTime(record.examinedAt)}
                </span>
                <Pill tone="neutral" size="sm">
                  {formatEnumLabel(record.recordType)}
                </Pill>
                {record.trainingDecision && (
                  <Pill tone={record.trainingDecision === 'ALLOWED' ? 'success' : 'danger'} size="sm">
                    {record.trainingDecision}
                  </Pill>
                )}
              </div>

              <p className="text-sm font-medium text-[var(--color-text-primary)]">
                {record.diagnosis || 'No diagnosis recorded'}
              </p>
              <Detail label="Findings" value={record.findings} />
              <Detail label="Symptoms" value={record.symptoms} />
              <Detail label="Treatment" value={record.treatment} />
              <Detail label="Restriction" value={record.restrictionDetails} />
              <Detail label="Product or service" value={record.productOrService} />
              <Detail label="Notes" value={record.notes} />
            </div>
          </li>
        ))}
      </ol>
    </Panel>
  );
}
