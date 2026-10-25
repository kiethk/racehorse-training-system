'use client';

import { ChoiceInput, Select, Textarea } from '@/components/ui/Input';
import { Pill } from '@/components/ui/StatusBadge';
import { trainingDecisions, restrictionOptions } from './constants';
import type { TrainingDecision as TD } from '../../types';
import type { Dispatch, SetStateAction } from 'react';

export interface TrainingDecisionSectionProps {
  isUrgent: boolean;
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
}

export function TrainingDecisionSection(props: TrainingDecisionSectionProps) {
  const {
    isUrgent, isFormDisabled, trainingDecision, setTrainingDecision,
    restrictionDetails, setRestrictionDetails, fieldErrors, setFieldErrors,
    followUpDate, setFollowUpDate, businessDateFromToday,
  } = props;

  return (
    <div className="space-y-2">
      <label className="block text-xs font-bold uppercase tracking-wider text-[var(--color-text-secondary)]">
        Training Clearance
      </label>
      <div className="space-y-2">
        {trainingDecisions.map((opt) => {
          const isSelected = trainingDecision === opt.value;
          return (
            <label
              key={opt.value}
              className={`flex cursor-pointer items-start gap-2.5 rounded-[var(--radius-md)] border p-2.5 transition-all ${
                isSelected
                  ? `${opt.activeBorder} ${opt.activeBg}`
                  : 'border-[var(--color-border)] bg-[var(--color-surface)] hover:bg-[var(--color-surface-subtle)]'
              }`}
            >
              <ChoiceInput
                type="radio"
                name="trainingDecision"
                value={opt.value}
                checked={isSelected}
                onChange={() => {
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
              </div>
            </label>
          );
        })}
      </div>

      {trainingDecision === 'BLOCKED' && (
        <div className="rounded-[var(--radius-md)] border border-[var(--color-warning)] bg-[var(--color-warning-soft)] p-3 space-y-1.5 animate-in fade-in duration-200">
          <label htmlFor="field-restriction-details" className="block text-xs font-bold text-[var(--color-warning)]">
            Mandatory Restriction Protocol <span className="text-[var(--color-danger)]">*</span>
          </label>
          <p className="text-xs text-[var(--color-text-secondary)]">
            Shown to the Head Trainer when planning. The horse rests until the follow-up exam below.
          </p>
          {isUrgent ? (
            <Textarea
              id="field-restriction-details"
              rows={2}
              value={restrictionDetails}
              onChange={(e) => setRestrictionDetails(e.target.value)}
              placeholder="Describe restriction rules..."
            />
          ) : (
            <Select
              id="field-restriction-details"
              value={restrictionDetails}
              onChange={(e) => {
                setRestrictionDetails(e.target.value);
                if (fieldErrors.restrictionDetails) {
                  setFieldErrors((errs) => ({ ...errs, restrictionDetails: '' }));
                }
              }}
            >
              <option value="">Select restriction protocol</option>
              {restrictionOptions.map((opt) => (
                <option key={opt} value={opt}>{opt}</option>
              ))}
            </Select>
          )}
          {fieldErrors.restrictionDetails && (
            <p className="text-xs font-medium text-[var(--color-danger)]">{fieldErrors.restrictionDetails}</p>
          )}
        </div>
      )}
    </div>
  );
}