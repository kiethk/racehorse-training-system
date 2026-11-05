'use client';

import type { Dispatch, SetStateAction } from 'react';
import { Button } from '@/components/ui/Button';
import { CreatableCombobox, type CreatableComboboxOption } from '@/components/ui/CreatableCombobox';
import { FormField } from '@/components/ui/FormField';
import { Input, Select, Textarea } from '@/components/ui/Input';
import { Icon } from '@/components/ui/Icon';
import type { MedicationRoute, PrescriptionFormItem, PrescriptionSectionProps } from '../types';
import medicationsCatalog from '../data/medications.json';

const ROUTE_OPTIONS: Array<{ value: MedicationRoute; label: string }> = [
  { value: 'ORAL', label: 'Oral (Groom Administered)' },
  { value: 'TOPICAL', label: 'Topical (Groom Administered)' },
  { value: 'FEED_ADDITIVE', label: 'Feed Additive (Groom Administered)' },
  { value: 'INJECTION_IV', label: 'Intravenous — IV (Veterinarian Administered)' },
  { value: 'INJECTION_IM', label: 'Intramuscular — IM (Veterinarian Administered)' },
];

const MEDICATION_OPTIONS: CreatableComboboxOption[] = medicationsCatalog.map((m) => ({
  value: m.name,
  label: `${m.name} (${m.therapeuticClass})`,
}));

export function PrescriptionSection(props: PrescriptionSectionProps) {
  const {
    isFormDisabled,
    prescriptions,
    setPrescriptions,
    generalCareInstructions,
    setGeneralCareInstructions,
    fieldErrors,
    setFieldErrors,
  } = props;

  const handleAddMedication = () => {
    const newItem: PrescriptionFormItem = {
      id: Math.random().toString(36).substring(2, 9),
      medicationName: '',
      dosage: '',
      route: 'ORAL',
      frequency: 'BID',
      duration: 5,
      notes: '',
    };
    setPrescriptions((prev) => [...prev, newItem]);
  };

  const handleRemove = (itemId: string) => {
    setPrescriptions((prev) => prev.filter((p) => p.id !== itemId));
    setFieldErrors((prev) => {
      const next = { ...prev };
      Object.keys(next).forEach((key) => {
        if (key.startsWith(`rx.${itemId}`)) {
          delete next[key];
        }
      });
      return next;
    });
  };

  const handleMedicationNameChange = (itemId: string, name: string) => {
    const med = medicationsCatalog.find((m) => m.name.toLowerCase() === name.toLowerCase());
    setPrescriptions((prev) =>
      prev.map((item) => {
        if (item.id !== itemId) return item;
        return {
          ...item,
          medicationName: name,
          dosage: med && !item.dosage ? med.defaultDosage : item.dosage,
          route: med ? (med.defaultRoute as MedicationRoute) : item.route,
          frequency: med && !item.frequency ? med.defaultFrequency : item.frequency,
        };
      })
    );
    const errorKey = `rx.${itemId}.medicationName`;
    if (fieldErrors[errorKey]) {
      setFieldErrors((prev) => {
        const next = { ...prev };
        delete next[errorKey];
        return next;
      });
    }
  };

  const handleFieldChange = <K extends keyof PrescriptionFormItem>(
    itemId: string,
    field: K,
    value: PrescriptionFormItem[K]
  ) => {
    setPrescriptions((prev) =>
      prev.map((item) => (item.id === itemId ? { ...item, [field]: value } : item))
    );
    const errorKey = `rx.${itemId}.${String(field)}`;
    if (fieldErrors[errorKey]) {
      setFieldErrors((prev) => {
        const next = { ...prev };
        delete next[errorKey];
        return next;
      });
    }
  };

  return (
    <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[var(--color-border)] pb-3">
        <div>
          <h3 className="flex items-center gap-2 text-sm font-semibold text-[var(--color-text-primary)]">
            <Icon name="pill" size={16} className="text-[var(--color-accent)]" />
            4. Prescriptions & Medication Orders (Optional)
          </h3>
        </div>
        <Button
          type="button"
          variant="secondary"
          size="sm"
          icon="plus"
          disabled={isFormDisabled}
          onClick={handleAddMedication}
        >
          Add Medication
        </Button>
      </div>

      {prescriptions.length > 0 && (
        <div className="space-y-4">
          {prescriptions.map((rx, index) => {
            const isVetRoute = rx.route === 'INJECTION_IV' || rx.route === 'INJECTION_IM';

            return (
              <div
                key={rx.id}
                className="rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3.5 space-y-3"
              >
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-[var(--color-text-secondary)] uppercase tracking-wider">
                    Medication #{index + 1}
                  </span>
                  <Button
                    type="button"
                    variant="tertiary"
                    size="sm"
                    disabled={isFormDisabled}
                    onClick={() => handleRemove(rx.id)}
                    className="text-[var(--color-danger)] hover:bg-[var(--color-danger-soft)]/20 h-7 px-2"
                  >
                    <Icon name="x" size={14} className="mr-1" />
                    Remove
                  </Button>
                </div>

                <FormField
                  label="Medication Name"
                  required
                  error={fieldErrors[`rx.${rx.id}.medicationName`]}
                >
                  <CreatableCombobox
                    disabled={isFormDisabled}
                    value={rx.medicationName}
                    onChange={(val) => handleMedicationNameChange(rx.id, val)}
                    options={MEDICATION_OPTIONS}
                    placeholder="Select or type a medication name"
                  />
                </FormField>

                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  <FormField
                    label="Administration Route"
                    required
                    error={fieldErrors[`rx.${rx.id}.route`]}
                  >
                    <Select
                      disabled={isFormDisabled}
                      value={rx.route}
                      onChange={(e) =>
                        handleFieldChange(rx.id, 'route', e.target.value as MedicationRoute)
                      }
                    >
                      {ROUTE_OPTIONS.map((opt) => (
                        <option key={opt.value} value={opt.value}>
                          {opt.label}
                        </option>
                      ))}
                    </Select>
                  </FormField>

                  <FormField label="Dosage" required error={fieldErrors[`rx.${rx.id}.dosage`]}>
                    <Input
                      disabled={isFormDisabled}
                      placeholder="e.g. 1.1 mg/kg or 10ml"
                      value={rx.dosage}
                      onChange={(e) => handleFieldChange(rx.id, 'dosage', e.target.value)}
                    />
                  </FormField>
                </div>

                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  <FormField label="Frequency" required error={fieldErrors[`rx.${rx.id}.frequency`]}>
                    <Input
                      disabled={isFormDisabled}
                      placeholder="e.g. BID (Twice daily), SID, ONCE"
                      value={rx.frequency}
                      onChange={(e) => handleFieldChange(rx.id, 'frequency', e.target.value)}
                    />
                  </FormField>

                  <FormField label="Duration (days)" required error={fieldErrors[`rx.${rx.id}.duration`]}>
                    <Input
                      type="number"
                      min={1}
                      max={365}
                      disabled={isFormDisabled}
                      value={rx.duration}
                      onChange={(e) =>
                        handleFieldChange(rx.id, 'duration', Math.max(1, parseInt(e.target.value || '1', 10)))
                      }
                    />
                    <span className="text-[11px] text-[var(--color-text-muted)] mt-1 block">
                      {isVetRoute ? 'Veterinarian administers' : 'Groom administers'}
                    </span>
                  </FormField>
                </div>

                <FormField
                  label="Notes (for Groom / Handler)"
                  error={fieldErrors[`rx.${rx.id}.notes`]}
                >
                  <Input
                    disabled={isFormDisabled}
                    placeholder="e.g. Administer with morning grain feed; monitor for colic signs"
                    value={rx.notes}
                    onChange={(e) => handleFieldChange(rx.id, 'notes', e.target.value)}
                  />
                </FormField>
              </div>
            );
          })}
        </div>
      )}

      <FormField
        label="General Care Instructions (Overall care directions for Groom & Trainer)"
        error={fieldErrors.generalCareInstructions}
      >
        <Textarea
          rows={3}
          disabled={isFormDisabled}
          placeholder="e.g. Strict stall rest, cold hose lower limbs for 20 minutes BID, monitor water intake, resume light walking after day 3."
          value={generalCareInstructions}
          onChange={(e) => setGeneralCareInstructions(e.target.value)}
        />
      </FormField>
    </div>
  );
}
