'use client';

import type { Dispatch, SetStateAction } from 'react';
import { Icon } from '@/components/ui/Icon';
import { Pill } from '@/components/ui/StatusBadge';
import { ChoiceInput } from '@/components/ui/Input';
import { formatDate } from '@/lib/display';
import { cn } from '@/lib/cn';
import type { InjuryRecord } from '@/features/health/types';

export interface ActiveConditionsSectionProps {
  activeInjuries: InjuryRecord[];
  resolvedConditionIds: number[];
  setResolvedConditionIds: Dispatch<SetStateAction<number[]>>;
  isFormDisabled: boolean;
}

export function ActiveConditionsSection({
  activeInjuries,
  resolvedConditionIds,
  setResolvedConditionIds,
  isFormDisabled,
}: ActiveConditionsSectionProps) {
  if (activeInjuries.length === 0) return null;

  const resolvedCount = resolvedConditionIds.length;

  return (
    <div className="rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-[var(--shadow-panel)] space-y-3">
      <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-2.5">
        <div className="flex items-center gap-2">
          <Icon name="activity" size={16} className="text-[var(--color-primary)]" />
          <h3 className="text-sm font-bold text-[var(--color-text-primary)]">
            Active Medical Conditions & Recovery
          </h3>
        </div>
        <span className="text-xs font-medium text-[var(--color-text-muted)]">
          {resolvedCount} marked resolved
        </span>
      </div>

      <p className="text-xs text-[var(--color-text-secondary)]">
        Select conditions that have fully resolved during this examination. Unchecked conditions will remain active.
      </p>

      <div className="space-y-2">
        {activeInjuries.map((inj) => {
          const isResolved = resolvedConditionIds.includes(inj.id);
          const severityTone =
            inj.severity === 'SEVERE' || inj.severity === 'CRITICAL'
              ? 'danger'
              : inj.severity === 'MODERATE'
                ? 'warning'
                : 'neutral';

          return (
            <label
              key={inj.id}
              className={cn(
                'flex items-start justify-between p-3 rounded-[var(--radius-md)] border cursor-pointer transition-colors',
                isResolved
                  ? 'border-[var(--color-success)]/40 bg-[var(--color-success-subtle)]/30'
                  : 'border-[var(--color-border)] bg-[var(--color-surface-subtle)] hover:border-[var(--color-border-strong)]',
              )}
            >
              <div className="space-y-1 pr-3">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-semibold text-[var(--color-text-primary)]">
                    {inj.injuryType}
                  </span>
                  <Pill tone={severityTone} size="sm">
                    {inj.severity}
                  </Pill>
                </div>
                <p className="text-[11px] text-[var(--color-text-muted)]">
                  Diagnosed {formatDate(inj.diagnosedAt)}
                  {inj.expectedRecoveryDate && ` · Expected recovery: ${formatDate(inj.expectedRecoveryDate)}`}
                </p>
                {inj.description && (
                  <p className="text-xs text-[var(--color-text-secondary)] italic">
                    &ldquo;{inj.description}&rdquo;
                  </p>
                )}
              </div>

              <div className="flex items-center gap-2 shrink-0 pt-0.5">
                <span
                  className={cn(
                    'text-xs font-semibold',
                    isResolved
                      ? 'text-[var(--color-success)]'
                      : 'text-[var(--color-text-muted)]',
                  )}
                >
                  {isResolved ? 'Recovered' : 'Active'}
                </span>
                <ChoiceInput
                  type="checkbox"
                  checked={isResolved}
                  disabled={isFormDisabled}
                  onChange={(e) => {
                    const checked = e.target.checked;
                    setResolvedConditionIds((prev) =>
                      checked ? [...prev, inj.id] : prev.filter((id) => id !== inj.id),
                    );
                  }}
                />
              </div>
            </label>
          );
        })}
      </div>
    </div>
  );
}
