'use client';

import { FormField } from '@/components/ui/FormField';
import { Textarea } from '@/components/ui/Input';
import { Icon } from '@/components/ui/Icon';
import type { PrescriptionSectionProps } from '../types';

export function PrescriptionSection(props: PrescriptionSectionProps) {
  const {
    isFormDisabled,
    generalCareInstructions,
    setGeneralCareInstructions,
    fieldErrors,
    setFieldErrors,
  } = props;

  return (
    <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 space-y-4">
      <div className="border-b border-[var(--color-border)] pb-3">
        <h3 className="flex items-center gap-2 text-sm font-semibold text-[var(--color-text-primary)]">
          <Icon name="pill" size={16} className="text-[var(--color-accent)]" />
          4. Prescription &amp; Care Note (Optional)
        </h3>
      </div>

      <FormField
        label="Medication & directions"
        error={fieldErrors.generalCareInstructions}
      >
        <Textarea
          rows={4}
          disabled={isFormDisabled}
          placeholder="e.g. Phenylbutazone 4.4 mg/kg IV once daily for 5 days. Cold hose left forelimb 20 minutes BID; monitor appetite."
          value={generalCareInstructions}
          onChange={(e) => {
            setGeneralCareInstructions(e.target.value);
            if (fieldErrors.generalCareInstructions) {
              setFieldErrors((prev) => {
                const next = { ...prev };
                delete next.generalCareInstructions;
                return next;
              });
            }
          }}
        />
      </FormField>

      <p className="text-xs text-[var(--color-text-secondary)]">
        Injections are performed by the veterinarian only.
      </p>
    </div>
  );
}
