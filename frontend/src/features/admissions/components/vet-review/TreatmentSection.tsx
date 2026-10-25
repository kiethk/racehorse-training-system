'use client';

import { FormField } from '@/components/ui/FormField';
import { Textarea } from '@/components/ui/Input';
import type { Dispatch, SetStateAction } from 'react';

export interface TreatmentSectionProps {
  isFormDisabled: boolean;
  treatment: string;
  setTreatment: Dispatch<SetStateAction<string>>;
  notes: string;
  setNotes: Dispatch<SetStateAction<string>>;
}

export function TreatmentSection(props: TreatmentSectionProps) {
  const { isFormDisabled, treatment, setTreatment, notes, setNotes } = props;
  return (
    <>
      <div>
        <FormField label="Treatment &amp; Care Protocol (Optional)" htmlFor="field-treatment">
          <Textarea
            id="field-treatment"
            rows={2}
            value={treatment}
            onChange={(e) => setTreatment(e.target.value)}
            placeholder="Enter prescribed medications, bandages, or therapy..."
          />
        </FormField>
      </div>
      <div>
        <FormField label="Veterinarian Notes &amp; Stable Directives (Optional)" htmlFor="field-notes">
          <Textarea
            id="field-notes"
            rows={2}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Internal notes, groom handling directives, or dietary recommendations..."
          />
        </FormField>
      </div>
    </>
  );
}