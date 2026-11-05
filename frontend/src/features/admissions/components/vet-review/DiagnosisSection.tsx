'use client';

import { Button } from '@/components/ui/Button';
import { Input, Select, Textarea } from '@/components/ui/Input';
import { diagnosisOptions } from './constants';
import type { Dispatch, SetStateAction } from 'react';

export interface DiagnosisSectionProps {
  isUrgent: boolean;
  isFormDisabled: boolean;
  // urgent
  diagnosis: string;
  setDiagnosis: Dispatch<SetStateAction<string>>;
  // standard
  examMode: 'NORMAL' | 'ABNORMAL';
  setExamMode: Dispatch<SetStateAction<'NORMAL' | 'ABNORMAL'>>;
  structuredDiagnosis: string;
  setStructuredDiagnosis: Dispatch<SetStateAction<string>>;
  abnormalFindings: string;
  setAbnormalFindings: Dispatch<SetStateAction<string>>;
  fieldErrors: Record<string, string>;
  setFieldErrors: Dispatch<SetStateAction<Record<string, string>>>;
}

export function DiagnosisSection(props: DiagnosisSectionProps) {
  const {
    isUrgent, isFormDisabled, diagnosis, setDiagnosis,
    examMode, setExamMode, structuredDiagnosis, setStructuredDiagnosis,
    abnormalFindings, setAbnormalFindings, fieldErrors, setFieldErrors,
  } = props;

  function switchMode(mode: 'NORMAL' | 'ABNORMAL') {
    if (mode === examMode) return;
    setExamMode(mode);
    if (mode === 'ABNORMAL' && structuredDiagnosis === 'Clinically healthy') {
      setStructuredDiagnosis('Minor clinical condition');
    }
    if (mode === 'NORMAL') {
      setStructuredDiagnosis('Clinically healthy');
      setAbnormalFindings('');
    }
  }

  return (
    <div className="min-w-0 rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-[var(--shadow-panel)] space-y-4">
      <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-2">
        <h3 className="text-sm font-semibold tracking-tight text-[var(--color-text-primary)]">
          2. Clinical Assessment &amp; Diagnosis <span className="text-[var(--color-danger)]">*</span>
        </h3>
      </div>
      {isUrgent ? (
        <>
          <Input
            id="field-assessment-diag"
            type="text"
            value={diagnosis}
            onChange={(e) => {
              setDiagnosis(e.target.value);
              if (fieldErrors.diagnosis) setFieldErrors((errs) => ({ ...errs, diagnosis: '' }));
            }}
            placeholder="Working diagnosis..."
          />
          {fieldErrors.diagnosis && (
            <p className="mt-1 text-xs font-medium text-[var(--color-danger)]">{fieldErrors.diagnosis}</p>
          )}
        </>
      ) : (
        <div className="space-y-2">
          <div className="flex gap-2">
            <Button
              type="button"
              size="sm"
              icon="check"
              variant={examMode === 'NORMAL' ? 'primary' : 'tertiary'}
              disabled={isFormDisabled}
              onClick={() => switchMode('NORMAL')}
            >
              Clinically healthy
            </Button>
            <Button
              type="button"
              size="sm"
              icon="alert-triangle"
              variant={examMode === 'ABNORMAL' ? 'primary' : 'tertiary'}
              disabled={isFormDisabled}
              onClick={() => switchMode('ABNORMAL')}
            >
              Abnormal findings
            </Button>
          </div>
          {examMode === 'ABNORMAL' && (
            <>
              <div>
                <label htmlFor="field-abnormal-findings" className="mb-1 block text-xs font-semibold text-[var(--color-text-primary)]">
                  Examination findings <span className="text-[var(--color-danger)]">*</span>
                </label>
                <Textarea
                  id="field-abnormal-findings"
                  rows={3}
                  value={abnormalFindings}
                  onChange={(e) => {
                    setAbnormalFindings(e.target.value);
                    if (fieldErrors.abnormalFindings) setFieldErrors((errs) => ({ ...errs, abnormalFindings: '' }));
                  }}
                  placeholder="e.g. Mild SDFT strain right fore, heat on palpation, lameness 2/5 at trot…"
                />
                {fieldErrors.abnormalFindings && (
                  <p className="mt-1 text-xs font-medium text-[var(--color-danger)]">{fieldErrors.abnormalFindings}</p>
                )}
              </div>
              <div>
                <label htmlFor="field-assessment-diag" className="mb-1 block text-xs font-semibold text-[var(--color-text-primary)]">
                  Clinical diagnosis <span className="text-[var(--color-danger)]">*</span>
                </label>
                <Select
                  id="field-assessment-diag"
                  value={structuredDiagnosis}
                  onChange={(e) => setStructuredDiagnosis(e.target.value)}
                >
                  {diagnosisOptions
                    .filter((d) => d !== 'Clinically healthy')
                    .map((opt) => (
                      <option key={opt} value={opt}>{opt}</option>
                    ))}
                </Select>
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
}
