'use client';

import { useMemo } from 'react';
import { Button, Icon, Modal, Notice, Pill } from '@/components/ui';
import { formatEnumLabel } from '@/lib/display';
import { isAdministeredByGroom, parseTreatmentInstructions } from '../lib/treatmentInstructions';

export interface MedicationInstructionModalProps {
  isOpen?: boolean;
  open?: boolean;
  onClose: () => void;
  horseName: string;
  stallCode: string;
  diagnosis?: string;
  veterinarianName?: string;
  instructions: string;
  endDate?: string | null;
}

export function MedicationInstructionModal({
  isOpen,
  open,
  onClose,
  horseName,
  stallCode,
  diagnosis,
  veterinarianName,
  instructions,
  endDate,
}: MedicationInstructionModalProps) {
  const isVisible = open ?? isOpen ?? false;

  const parsed = useMemo(
    () => parseTreatmentInstructions(instructions),
    [instructions],
  );

  if (!isVisible) return null;

  return (
    <Modal
      open={isVisible}
      onClose={onClose}
      title={
        <div className="flex items-center gap-2">
          <Icon name="pill" size={20} className="text-[var(--color-warning)]" />
          <span>Medication Administration Instructions</span>
        </div>
      }
      
      size="lg"
      footer={
        <div className="flex justify-end">
          <Button variant="secondary" onClick={onClose}>
            Close
          </Button>
        </div>
      }
    >
      <div className="space-y-4">
        {/* Context metadata summary */}
        <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3.5 text-xs text-[var(--color-text-secondary)] space-y-1.5">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <span className="font-semibold text-[var(--color-text-primary)]">Horse: </span>
              {horseName} (Stall {stallCode})
            </div>
            {endDate && (
              <div>
                <span className="font-semibold text-[var(--color-text-primary)]">Treatment Until: </span>
                {endDate}
              </div>
            )}
          </div>
          {(diagnosis || veterinarianName) && (
            <div className="flex flex-wrap items-center justify-between gap-2 pt-1 border-t border-[var(--color-border)]/60">
              {diagnosis && (
                <div>
                  <span className="font-semibold text-[var(--color-text-primary)]">Diagnosis: </span>
                  {diagnosis}
                </div>
              )}
              {veterinarianName && (
                <div>
                  <span className="font-semibold text-[var(--color-text-primary)]">Prescribing Vet: </span>
                  {veterinarianName}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Content: Structured JSON Table vs Legacy Plain-Text Notice */}
        {parsed.isJson && parsed.items.length > 0 ? (
          <div className="space-y-3">
            <div className="flex items-center justify-between gap-2 text-xs text-[var(--color-text-muted)]">
              <span className="font-medium text-[var(--color-text-primary)]">Prescribed Medications</span>
              <div className="flex items-center gap-2">
                <span className="inline-flex items-center gap-1">
                  <span className="inline-block h-2 w-2 rounded-full bg-[var(--color-success)]" />
                  Groom Administered
                </span>
                <span className="inline-flex items-center gap-1">
                  <span className="inline-block h-2 w-2 rounded-full bg-[var(--color-warning)]" />
                  Vet Administered Only
                </span>
              </div>
            </div>

            {/* Custom Grid Table using pure CSS and tokens (no raw <table>) */}
            <div className="rounded-lg border border-[var(--color-border)] overflow-hidden">
              {/* Table Header */}
              <div className="grid grid-cols-12 gap-3 bg-[var(--color-surface-subtle)] px-3.5 py-2.5 text-xs font-semibold text-[var(--color-text-secondary)] border-b border-[var(--color-border)]">
                <div className="col-span-4">Medication &amp; Route</div>
                <div className="col-span-3">Dosage &amp; Frequency</div>
                <div className="col-span-2">Duration</div>
                <div className="col-span-3">Special Instructions</div>
              </div>

              {/* Table Rows */}
              <div className="divide-y divide-[var(--color-border)] bg-[var(--color-surface)]">
                {parsed.items.map((item, idx) => {
                  const groomRole = isAdministeredByGroom(item.route);
                  return (
                    <div
                      key={`${item.medication_name}-${idx}`}
                      className="grid grid-cols-12 gap-3 px-3.5 py-3 text-xs items-start"
                    >
                      {/* Medication & Route */}
                      <div className="col-span-4 space-y-1">
                        <p className="font-semibold text-[var(--color-text-primary)]">
                          {item.medication_name}
                        </p>
                        <div>
                          {groomRole ? (
                            <Pill size="sm" tone="success">
                              Groom: {formatEnumLabel(item.route)}
                            </Pill>
                          ) : (
                            <Pill size="sm" tone="warning">
                              Vet Only: {formatEnumLabel(item.route)}
                            </Pill>
                          )}
                        </div>
                      </div>

                      {/* Dosage & Frequency */}
                      <div className="col-span-3 space-y-0.5">
                        <p className="font-medium text-[var(--color-text-primary)]">
                          {item.dosage || '—'}
                        </p>
                        <p className="text-[var(--color-text-muted)]">
                          {item.frequency || '—'}
                        </p>
                      </div>

                      {/* Duration */}
                      <div className="col-span-2 font-medium text-[var(--color-text-primary)]">
                        {item.duration > 0
                          ? `${item.duration} ${item.duration === 1 ? 'day' : 'days'}`
                          : '—'}
                      </div>

                      {/* Special Instructions */}
                      <div className="col-span-3 text-[var(--color-text-secondary)] leading-relaxed italic">
                        {item.notes || 'No special notes.'}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        ) : (
          <Notice tone="info" title="Clinical Treatment Instructions">
            {parsed.rawText || 'No specific administration instructions recorded.'}
          </Notice>
        )}
      </div>
    </Modal>
  );
}

