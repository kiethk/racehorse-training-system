'use client';

import { FormField } from '@/components/ui/FormField';
import { ChoiceInput, Input, Select } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { formatDate } from '@/lib/display';
import { followUpDescriptionOptions } from './constants';
import { followUpMerge } from './examVariantConfig';
import type { Dispatch, SetStateAction } from 'react';

export interface FollowUpSectionProps {
  isFormDisabled: boolean;
  careType: string;
  schedulePurpose?: 'FOLLOW_UP' | 'PREVENTIVE';
  fromIncident?: boolean;
  waitingFollowUpAt?: string | null;
  followUpRequired: boolean;
  followUpActive: boolean;
  scheduleFollowUp: boolean;
  setScheduleFollowUp: Dispatch<SetStateAction<boolean>>;
  followUpDate: string;
  setFollowUpDate: Dispatch<SetStateAction<string>>;
  followUpDescription: string;
  setFollowUpDescription: Dispatch<SetStateAction<string>>;
  fieldErrors: Record<string, string>;
  setFieldErrors: Dispatch<SetStateAction<Record<string, string>>>;
  businessDateFromToday: (offset: number) => string;
  tomorrowBusinessDate: () => string;
  suggestedRecheckDate?: string;
}

/** Tells the Vet what the chosen date does to the Follow-up already waiting. */
export function FollowUpMergeNotice({ chosenDate, waitingAt }: { chosenDate: string; waitingAt?: string | null }) {
  const merge = followUpMerge(chosenDate, waitingAt);
  if (!merge || !waitingAt) return null;
  return (
    <p className="rounded-[var(--radius-sm)] bg-[var(--color-warning-soft)] p-2 text-xs font-medium text-[var(--color-warning)]">
      {merge === 'MOVED'
        ? `The Follow-up on ${formatDate(waitingAt)} moves to ${formatDate(chosenDate)}.`
        : `This horse already has a Follow-up on ${formatDate(waitingAt)}. That date stays, no new visit is booked.`}
    </p>
  );
}

export function FollowUpSection(props: FollowUpSectionProps) {
  const {
    isFormDisabled, careType, schedulePurpose, fromIncident, waitingFollowUpAt, followUpRequired, followUpActive,
    scheduleFollowUp, setScheduleFollowUp, followUpDate, setFollowUpDate,
    followUpDescription, setFollowUpDescription, fieldErrors, setFieldErrors,
    businessDateFromToday, tomorrowBusinessDate, suggestedRecheckDate,
  } = props;

  return (
    <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3 space-y-2">
      <label className="flex cursor-pointer items-center justify-between">
        <span className="text-xs font-semibold text-[var(--color-text-primary)]">
          Schedule Follow-up Exam
          {followUpRequired && <span className="text-[var(--color-danger)]"> *</span>}
        </span>
        <ChoiceInput
          type="checkbox"
          checked={followUpActive}
          disabled={followUpRequired || isFormDisabled}
          onChange={(e) => {
            const checked = e.target.checked;
            setScheduleFollowUp(checked);
            if (checked && !followUpDate) {
              setFollowUpDate(businessDateFromToday(7));
            }
          }}
        />
      </label>
      {followUpRequired && (
        <p className="text-xs text-[var(--color-text-secondary)]">
          A follow-up schedule is required before submitting.
        </p>
      )}
      {careType === 'ROUTINE' && !followUpRequired && schedulePurpose !== 'PREVENTIVE' && !fromIncident && (
        <p className="text-xs text-[var(--color-text-secondary)]">
          Without a follow-up date the system books the next visit automatically — routine
          follow-ups repeat every 14 days.
        </p>
      )}
      {followUpActive && (
        <div className="space-y-2 border-t border-[var(--color-border)] pt-2 animate-in fade-in duration-200">
          <div>
            <FormField label="Target Date" htmlFor="field-followup-date" required>
              <Input
                id="field-followup-date"
                type="date"
                min={tomorrowBusinessDate()}
                value={followUpDate}
                onChange={(e) => {
                  setFollowUpDate(e.target.value);
                  if (fieldErrors.followUpDate) {
                    setFieldErrors((errs) => ({ ...errs, followUpDate: '' }));
                  }
                }}
              />
            </FormField>
            <div className="mt-1 flex gap-1">
              {[3, 7, 14].map((days) => {
                const iso = businessDateFromToday(days);
                return (
                  <Button type="button" key={days} size="sm" variant="secondary" onClick={() => setFollowUpDate(iso)}>
                    +{days}d
                  </Button>
                );
              })}
            </div>
            {suggestedRecheckDate && (
              <p className="mt-1 flex items-center gap-1.5 text-xs font-medium text-[var(--color-primary)]">
                <span>💡 Suggested follow-up: on or after {formatDate(suggestedRecheckDate)} (active medication schedule)</span>
              </p>
            )}
            <div className="mt-1.5">
              <FollowUpMergeNotice chosenDate={followUpDate} waitingAt={waitingFollowUpAt} />
            </div>
            {fieldErrors.followUpDate && (
              <p className="mt-0.5 text-xs font-medium text-[var(--color-danger)]">{fieldErrors.followUpDate}</p>
            )}
          </div>
          <div>
            <FormField label="Care Procedure" htmlFor="field-followup-desc" required>
              <Select
                id="field-followup-desc"
                value={followUpDescription}
                onChange={(e) => {
                  setFollowUpDescription(e.target.value);
                  if (fieldErrors.followUpDescription) {
                    setFieldErrors((errs) => ({ ...errs, followUpDescription: '' }));
                  }
                }}
              >
                {followUpDescriptionOptions.map((opt) => (
                  <option key={opt} value={opt}>{opt}</option>
                ))}
              </Select>
            </FormField>
            {fieldErrors.followUpDescription && (
              <p className="mt-0.5 text-xs font-medium text-[var(--color-danger)]">{fieldErrors.followUpDescription}</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}