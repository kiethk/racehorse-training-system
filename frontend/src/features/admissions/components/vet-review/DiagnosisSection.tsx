'use client';

import { Input, Select } from '@/components/ui/Input';
import { Icon } from '@/components/ui/Icon';
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
  structuredDiagnosis: string;
  setStructuredDiagnosis: Dispatch<SetStateAction<string>>;
  fieldErrors: Record<string, string>;
}

export function DiagnosisSection(props: DiagnosisSectionProps) {
  const { isUrgent, isFormDisabled, diagnosis, setDiagnosis, examMode, structuredDiagnosis, setStructuredDiagnosis, fieldErrors } = props;

  return (
    <div>
      <label htmlFor="field-assessment-diag" className="mb-1 block text-xs font-semibold text-[var(--color-text-primary)]">
        Clinical Assessment &amp; Diagnosis <span className="text-[var(--color-danger)]">*</span>
      </label>
      {isUrgent ? (
        <Input
          id="field-assessment-diag"
          type="text"
          value={diagnosis}
          onChange={(e) => setDiagnosis(e.target.value)}
          placeholder="Working diagnosis..."
        />
      ) : examMode === 'NORMAL' ? (
        <div className="flex h-9 items-center gap-2 rounded-[var(--radius-sm)] border border-[var(--color-success)] bg-[var(--color-success-soft)] px-3 text-xs font-semibold text-[var(--color-success)]">
          <Icon name="check" size={14} />
          Clinically Healthy
        </div>
      ) : (
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
      )}
      {fieldErrors.diagnosis && (
        <p className="mt-1 text-xs font-medium text-[var(--color-danger)]">{fieldErrors.diagnosis}</p>
      )}
    </div>
  );
}