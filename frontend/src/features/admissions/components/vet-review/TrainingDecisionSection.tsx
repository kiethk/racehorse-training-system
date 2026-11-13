'use client';

import { ChoiceInput, Textarea } from '@/components/ui/Input';
import { Pill } from '@/components/ui/StatusBadge';
import { Notice } from '@/components/ui/Notice';
import { trainingDecisions } from './constants';
import type { TrainingDecision as TD } from '../../types';
import type { Dispatch, SetStateAction } from 'react';

export interface TrainingDecisionSectionProps {
  isFormDisabled: boolean;
  trainingDecision: TD;
  setTrainingDecision: Dispatch<SetStateAction<TD>>;
  restrictionDetails: string;
  setRestrictionDetails: Dispatch<SetStateAction<string>>;
  fieldErrors: Record<string, string>;
  setFieldErrors: Dispatch<SetStateAction<Record<string, string>>>;
  followUpDate: string;
  setFollowUpDate: Dispatch<SetStateAction<string>>;
  businessDateFromToday: (offset: number) => string;
  /** Markers placed in this exam session forced BLOCKED, so ALLOWED is locked. */
  allowLocked?: boolean;
  markerNotice?: string | null;
  onLockedAllowAttempt?: () => void;
}

export function TrainingDecisionSection(props: TrainingDecisionSectionProps) {
  const {
    isFormDisabled, trainingDecision, setTrainingDecision,
    restrictionDetails, setRestrictionDetails, fieldErrors, setFieldErrors,
    followUpDate, setFollowUpDate, businessDateFromToday,
    allowLocked = false, markerNotice = null, onLockedAllowAttempt,
  } = props;

  return (
    <div className="space-y-2">
      <label className="block text-xs font-bold uppercase tracking-wider text-[var(--color-text-secondary)]">
        Training Clearance
      </label>
      <div className="space-y-2">
        {trainingDecisions.map((opt) => {
          const isSelected = trainingDecision === opt.value;
          const isLockedAllow = opt.value === 'ALLOWED' && allowLocked;
          return (
            <label
              key={opt.value}
              className={`flex items-start gap-2.5 rounded-[var(--radius-md)] border p-2.5 transition-all ${
                isSelected
                  ? `${opt.activeBorder} ${opt.activeBg}`
                  : 'border-[var(--color-border)] bg-[var(--color-surface)] hover:bg-[var(--color-surface-subtle)]'
              } ${isLockedAllow ? 'cursor-not-allowed opacity-60' : 'cursor-pointer'}`}
            >
              <ChoiceInput
                type="radio"
                name="trainingDecision"
                value={opt.value}
                checked={isSelected}
                onChange={() => {
                  if (isLockedAllow) {
                    onLockedAllowAttempt?.();
                    return;
                  }
                  setTrainingDecision(opt.value as TD);
                  if (opt.value === 'BLOCKED' && !followUpDate) {
                    setFollowUpDate(businessDateFromToday(7));
                  }
                  if (fieldErrors.restrictionDetails) {
                    setFieldErrors((errs) => ({ ...errs, restrictionDetails: '' }));
                  }
                }}
              />
              <div className="min-w-0 flex-1">
                <div className="flex items-center justify-between gap-1">
                  <span className="text-xs font-bold text-[var(--color-text-primary)]">{opt.title}</span>
                  <Pill tone={opt.tone} size="sm" icon={opt.icon}>{opt.value}</Pill>
                </div>
                {isLockedAllow && (
                  <p className="mt-1 text-xs text-[var(--color-text-secondary)]">
                    Marked injuries on the 3D model &mdash; remove markers to allow training.
                  </p>
                )}
              </div>
            </label>
          );
        })}
      </div>

      {markerNotice && (
        <Notice tone="info">{markerNotice}</Notice>
      )}

      {trainingDecision === 'BLOCKED' && (
        <div className="rounded-[var(--radius-md)] border border-[var(--color-warning)] bg-[var(--color-warning-soft)] p-3 space-y-2 animate-in fade-in duration-200">
          <div className="flex items-center justify-between">
            <label htmlFor="field-restriction-details" className="block text-xs font-bold text-[var(--color-warning)]">
              Assessment note <span className="text-[var(--color-danger)]">*</span>
            </label>
            
          </div>
          {/* Quick-Chips for common clinical restrictions */}
          <div className="flex flex-wrap gap-1.5">
            {[
              { label: 'Stall Rest', text: 'Strict box rest for 7 days; cold hosing twice daily.' },
              { label: 'Hand Walking', text: 'Box rest with controlled hand-walking 15 mins daily only.' },
              { label: 'Light Trot Only', text: 'Light walking/trotting under saddle; no canter or galloping.' },
              { label: 'Medical Isolation', text: 'Quarantine isolation with daily temperature and health monitoring.' },
              { label: 'Paddock Turnout', text: 'Individual paddock turnout permitted; avoid strenuous work.' },
            ].map((preset) => (
              <button
                key={preset.label}
                type="button"
                onClick={() => {
                  setRestrictionDetails(preset.text);
                  if (fieldErrors.restrictionDetails) {
                    setFieldErrors((errs) => ({ ...errs, restrictionDetails: '' }));
                  }
                }}
                className="rounded-full border border-[var(--color-border)] bg-[var(--color-surface)] px-2.5 py-1 text-[11px] font-medium text-[var(--color-text-primary)] shadow-sm hover:border-[var(--color-warning)] hover:bg-[var(--color-surface-hover)] active:scale-[0.97] transition-all"
              >
                + {preset.label}
              </button>
            ))}
          </div>
          <Textarea
            id="field-restriction-details"
            rows={3}
            value={restrictionDetails}
            onChange={(e) => {
              setRestrictionDetails(e.target.value);
              if (fieldErrors.restrictionDetails) {
                setFieldErrors((errs) => ({ ...errs, restrictionDetails: '' }));
              }
            }}
            placeholder="e.g. Tendon strain: box rest with hand-walking twice daily, recheck before trotting…"
          />
          {fieldErrors.restrictionDetails && (
            <p className="text-xs font-medium text-[var(--color-danger)]">{fieldErrors.restrictionDetails}</p>
          )}
        </div>
      )}
    </div>
  );
}