'use client';

import { FormField } from '@/components/ui/FormField';
import type { Dispatch, SetStateAction } from 'react';
import { ChoiceInput, Select, Textarea } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { SegmentedControl } from '@/components/ui/SegmentedControl';
import {
  bodySystems,
  findingLevelLabels,
  normalSystemFindings,
  symptomOptions,
  type BodySystemKey,
  type ExamMode,
  type FindingLevel,
} from './constants';

export interface FindingsSectionProps {
  isUrgent: boolean;
  isFormDisabled: boolean;
  physicalExamConfirmed: boolean;
  setPhysicalExamConfirmed: Dispatch<SetStateAction<boolean>>;
  fieldErrors: Record<string, string>;
  setFieldErrors: Dispatch<SetStateAction<Record<string, string>>>;
  quarantineStallCode?: string | null;
  // urgent
  symptoms: string;
  setSymptoms: Dispatch<SetStateAction<string>>;
  findings: string;
  setFindings: Dispatch<SetStateAction<string>>;
  // standard
  examMode: ExamMode;
  setExamMode: Dispatch<SetStateAction<ExamMode>>;
  systemFindings: Record<BodySystemKey, FindingLevel>;
  setSystemFindings: Dispatch<SetStateAction<Record<BodySystemKey, FindingLevel>>>;
  selectedSymptoms: string[];
  setSelectedSymptoms: Dispatch<SetStateAction<string[]>>;
  structuredDiagnosis: string;
  setStructuredDiagnosis: Dispatch<SetStateAction<string>>;
}

export function FindingsSection(props: FindingsSectionProps) {
  const {
    isUrgent, isFormDisabled, physicalExamConfirmed, setPhysicalExamConfirmed,
    fieldErrors, setFieldErrors, quarantineStallCode,
    symptoms, setSymptoms, findings, setFindings,
    examMode, setExamMode, systemFindings, setSystemFindings,
    selectedSymptoms, setSelectedSymptoms, structuredDiagnosis, setStructuredDiagnosis,
  } = props;

  return (
    <fieldset disabled={isFormDisabled} className="min-w-0 rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-[var(--shadow-panel)] space-y-4">
      <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-2">
        <h3 className="text-sm font-semibold tracking-tight text-[var(--color-text-primary)]">
          2. Clinical Findings &amp; Examination
        </h3>
        <span className="text-xs font-medium text-[var(--color-danger)]">* Required</span>
      </div>

      <div className="flex items-center justify-between rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] px-3 py-2">
        <label className="flex cursor-pointer items-center gap-2">
          <ChoiceInput
            type="checkbox"
            checked={physicalExamConfirmed}
            onChange={(e) => {
              setPhysicalExamConfirmed(e.target.checked);
              if (fieldErrors.physicalExamConfirmed) {
                setFieldErrors((errs) => ({ ...errs, physicalExamConfirmed: '' }));
              }
            }}
          />
          <span className="text-xs font-semibold text-[var(--color-text-primary)]">
            Physical Exam Conducted In-Person
          </span>
        </label>
        <span className="text-xs text-[var(--color-text-muted)]">
          Quarantine facility: {quarantineStallCode || 'Stall verified'}
        </span>
      </div>
      {fieldErrors.physicalExamConfirmed && (
        <p className="text-xs font-medium text-[var(--color-danger)]">
          {fieldErrors.physicalExamConfirmed}
        </p>
      )}

      {isUrgent ? (
        <div className="grid gap-3 sm:grid-cols-2">
          <div>
            <FormField label="Urgent Presenting Symptoms" htmlFor="field-symptoms" required>
              <Textarea
                id="field-symptoms"
                rows={3}
                value={symptoms}
                onChange={(e) => setSymptoms(e.target.value)}
                placeholder="Describe onset, symptoms, and urgency..."
              />
            </FormField>
            {fieldErrors.symptoms && (
              <p className="text-xs font-medium text-[var(--color-danger)]">{fieldErrors.symptoms}</p>
            )}
          </div>
          <div>
            <FormField label="Urgent Physical Findings" htmlFor="field-findings" required>
              <Textarea
                id="field-findings"
                rows={3}
                value={findings}
                onChange={(e) => setFindings(e.target.value)}
                placeholder="Document trauma, distress, or acute signs..."
              />
            </FormField>
            {fieldErrors.findings && (
              <p className="text-xs font-medium text-[var(--color-danger)]">{fieldErrors.findings}</p>
            )}
          </div>
        </div>
      ) : (
        <div className="space-y-3">
          <div className="space-y-1.5">
            <label className="block text-xs font-semibold text-[var(--color-text-secondary)]">
              Clinical Status Assessment
            </label>
            <div>
              <SegmentedControl
                label="Clinical status assessment"
                value={examMode}
                onChange={(mode) => {
                  setExamMode(mode as ExamMode);
                  if (mode === 'NORMAL') {
                    setSystemFindings({ ...normalSystemFindings });
                    setSelectedSymptoms(['No symptoms observed']);
                    setStructuredDiagnosis('Clinically healthy');
                    setFieldErrors((errs) => ({ ...errs, systemFindings: '' }));
                  } else {
                    setSelectedSymptoms([]);
                    setStructuredDiagnosis('Minor clinical condition');
                  }
                }}
                options={[
                  { value: 'NORMAL', label: 'Normal', icon: 'check' },
                  { value: 'ABNORMAL', label: 'Abnormal', icon: 'alert-triangle' },
                ]}
              />
            </div>
          </div>

          {examMode === 'ABNORMAL' && (
            <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3.5 space-y-4">
              <div>
                <div className="flex items-center justify-between">
                  <h4 className="text-xs font-bold text-[var(--color-text-primary)]">8 Body Systems Checklist</h4>
                  <Button type="button" variant="link" size="sm" onClick={() => setSystemFindings({ ...normalSystemFindings })}>
                    Reset all to normal
                  </Button>
                </div>
                <div className="mt-2 grid grid-cols-2 gap-2 sm:grid-cols-4">
                  {bodySystems.map(({ key, label }) => {
                    const isAbnormal = systemFindings[key] !== 'NORMAL';
                    return (
                      <label key={key} className="space-y-1">
                        <span className="block text-xs font-medium text-[var(--color-text-secondary)]">{label}</span>
                        <Select
                          value={systemFindings[key]}
                          onChange={(e) => {
                            setSystemFindings((curr) => ({ ...curr, [key]: e.target.value as FindingLevel }));
                            setFieldErrors((errs) => ({ ...errs, systemFindings: '' }));
                          }}
                          className={`w-full rounded-[var(--radius-sm)] px-2 py-1 text-xs outline-none border transition-colors ${
                            isAbnormal
                              ? 'border-[var(--color-warning)] bg-[var(--color-warning-soft)] text-[var(--color-warning)] font-semibold'
                              : 'border-[var(--color-border)] bg-[var(--color-surface)] text-[var(--color-text-primary)]'
                          }`}
                        >
                          {(Object.entries(findingLevelLabels) as Array<[FindingLevel, string]>).map(([val, optLabel]) => (
                            <option key={val} value={val}>{optLabel}</option>
                          ))}
                        </Select>
                      </label>
                    );
                  })}
                </div>
                {fieldErrors.systemFindings && (
                  <p className="mt-1.5 text-xs font-medium text-[var(--color-danger)]">{fieldErrors.systemFindings}</p>
                )}
              </div>

              <div className="border-t border-[var(--color-border)] pt-3">
                <span className="block text-xs font-bold text-[var(--color-text-primary)] mb-2">Observed Symptoms Checklist</span>
                <div className="flex flex-wrap gap-1.5">
                  {symptomOptions.map((symptom) => {
                    const selected = selectedSymptoms.includes(symptom);
                    return (
                      <Button
                        key={symptom}
                        size="sm"
                        variant="secondary"
                        aria-pressed={selected}
                        onClick={() =>
                          setSelectedSymptoms((curr) => {
                            if (symptom === 'No symptoms observed') return ['No symptoms observed'];
                            const withoutNone = curr.filter((s) => s !== 'No symptoms observed');
                            return selected ? withoutNone.filter((s) => s !== symptom) : [...withoutNone, symptom];
                          })
                        }
                        className={`rounded-full border px-2.5 py-1 text-xs font-medium transition-colors ${
                          selected
                            ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)] text-[var(--color-primary)] font-semibold'
                            : 'border-[var(--color-border)] bg-[var(--color-surface)] text-[var(--color-text-secondary)] hover:border-[var(--color-border-strong)]'
                        }`}
                      >
                        {selected ? '✓ ' : ''}
                        {symptom}
                      </Button>
                    );
                  })}
                </div>
              </div>
            </div>
          )}
        </div>
      )}
    </fieldset>
  );
}