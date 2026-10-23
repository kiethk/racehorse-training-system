'use client';

import { useState } from 'react';
import { Button, EmptyState, Panel, Pill } from '@/components/ui';
import { formatDate } from '@/lib/display';
import type { InjuryRecord } from '../types';
import { InjuryCorrectionModal } from './InjuryCorrectionModal';

function severityTone(severity: string): 'danger' | 'warning' | 'info' {
  if (severity === 'CRITICAL' || severity === 'HIGH') return 'danger';
  if (severity === 'MEDIUM') return 'warning';
  return 'info';
}

function statusTone(status: string): 'info' | 'success' | 'neutral' {
  if (status === 'ACTIVE') return 'info';
  if (status === 'RECOVERED') return 'success';
  return 'neutral';
}

/** Every injury of the horse, with the correction dialog the veterinarian needs. */
export function HorseInjuriesPanel({
  injuries,
  horseId,
  onChanged,
}: {
  injuries: InjuryRecord[];
  horseId: number;
  onChanged: () => void;
}) {
  const [selected, setSelected] = useState<InjuryRecord | null>(null);
  const [modalOpen, setModalOpen] = useState(false);

  if (injuries.length === 0) {
    return (
      <Panel padded>
        <EmptyState
          icon="shield"
          title="No injuries on record"
          description="This horse has no injury or condition record."
        />
      </Panel>
    );
  }

  return (
    <Panel padded className="space-y-3">
      <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">
        Injuries ({injuries.length})
      </h3>

      <ul className="space-y-2">
        {injuries.map((injury) => (
          <li
            key={injury.id}
            className="flex flex-col gap-2 rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3 sm:flex-row sm:items-start sm:justify-between"
          >
            <div className="min-w-0 space-y-1">
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-sm font-semibold text-[var(--color-text-primary)]">{injury.injuryType}</span>
                <Pill tone={severityTone(injury.severity)} size="sm">
                  {injury.severity}
                </Pill>
                <Pill tone={statusTone(injury.status)} size="sm">
                  {injury.status}
                </Pill>
              </div>
              {injury.description && (
                <p className="text-sm text-[var(--color-text-secondary)]">{injury.description}</p>
              )}
              <p className="text-xs text-[var(--color-text-muted)]">
                Diagnosed {formatDate(injury.diagnosedAt)}
                {injury.expectedRecoveryDate ? ` · Expected recovery ${formatDate(injury.expectedRecoveryDate)}` : ''}
                {injury.recoveredAt ? ` · Recovered ${formatDate(injury.recoveredAt)}` : ''}
              </p>
            </div>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => {
                setSelected(injury);
                setModalOpen(true);
              }}
            >
              Correct record
            </Button>
          </li>
        ))}
      </ul>

      {selected && (
        <InjuryCorrectionModal
          open={modalOpen}
          onClose={() => setModalOpen(false)}
          horseId={horseId}
          injury={selected}
          onSuccess={onChanged}
        />
      )}
    </Panel>
  );
}
