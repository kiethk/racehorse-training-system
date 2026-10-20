import type { ReactNode } from 'react';
import { Pill } from '@/components/ui/StatusBadge';
import { formatDateTime } from '@/lib/display';
import { cn } from '@/lib/cn';
import {
  AdmissionInfoSection,
  InfoRow,
} from './AdmissionInfoSection';
import type { InitialVetExamResponse, InitialVetExamMetric } from '../../types';

// ─── Helpers ──────────────────────────────────────────────────────────────────

/** Display a nullable text value; 0 is valid, empty string is not. */
function txt(v: string | null | undefined): ReactNode {
  if (v == null || v === '') return <span className="font-normal italic text-[var(--color-text-muted)]">—</span>;
  return v;
}

/** Display a nullable number; 0 is a valid result (e.g. score=0). */
function num(v: number | null | undefined, unit?: string): ReactNode {
  if (v == null) return <span className="font-normal italic text-[var(--color-text-muted)]">—</span>;
  return unit ? `${v} ${unit}` : String(v);
}

// ─── Decision badge ────────────────────────────────────────────────────────────

function DecisionBadge({ decision }: { decision: 'ALLOWED' | 'BLOCKED' | null }) {
  if (!decision) return <span className="font-normal italic text-[var(--color-text-muted)]">—</span>;
  return (
    <Pill tone={decision === 'ALLOWED' ? 'success' : 'danger'}>
      {decision === 'ALLOWED' ? 'Allowed to Train' : 'Blocked'}
    </Pill>
  );
}

// ─── Vitals row ────────────────────────────────────────────────────────────────

function MetricRow({ metric, index }: { metric: InitialVetExamMetric; index: number }) {
  const hasValues =
    metric.heartRate != null ||
    metric.temperature != null ||
    metric.respiratoryRate != null ||
    metric.weight != null ||
    metric.hydrationStatus != null ||
    metric.bodyConditionScore != null;

  if (!hasValues) return null;

  return (
    <div
      className={cn(
        'rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3 space-y-2',
      )}
    >
      <div className="flex items-center justify-between">
        <span className="text-xs font-semibold text-[var(--color-text-secondary)]">
          Measurement {index + 1}
        </span>
        <span className="text-xs text-[var(--color-text-muted)]">
          {formatDateTime(metric.recordedAt)}
        </span>
      </div>
      <div className="grid grid-cols-2 gap-x-6 gap-y-1 text-sm">
        {metric.temperature != null && (
          <InfoRow label="Temperature" value={num(metric.temperature, '°C')} />
        )}
        {metric.heartRate != null && (
          <InfoRow label="Heart rate" value={num(metric.heartRate, 'bpm')} />
        )}
        {metric.respiratoryRate != null && (
          <InfoRow label="Respiratory rate" value={num(metric.respiratoryRate, 'breaths/min')} />
        )}
        {metric.weight != null && (
          <InfoRow label="Weight" value={num(metric.weight, 'kg')} />
        )}
        {metric.hydrationStatus != null && (
          <InfoRow label="Hydration" value={txt(metric.hydrationStatus)} />
        )}
        {metric.bodyConditionScore != null && (
          <InfoRow label="Body condition score" value={num(metric.bodyConditionScore)} />
        )}
      </div>
      {metric.notes && (
        <p className="text-xs text-[var(--color-text-secondary)] italic">{metric.notes}</p>
      )}
    </div>
  );
}

// ─── Main component ────────────────────────────────────────────────────────────

interface MedicalFindingsSectionProps {
  exam: InitialVetExamResponse;
}

/**
 * Pure read-only presentation of the initial admission vet exam.
 * Shows: vet + decision, clinical notes, and vital-sign measurements.
 */
export function MedicalFindingsSection({ exam }: MedicalFindingsSectionProps) {
  const visibleMetrics = exam.metrics.filter(
    (m) =>
      m.heartRate != null ||
      m.temperature != null ||
      m.respiratoryRate != null ||
      m.weight != null ||
      m.hydrationStatus != null ||
      m.bodyConditionScore != null,
  );

  return (
    <div className="space-y-5">
      {/* ── 1. Examination overview ─────────────────────────────────────── */}
      <AdmissionInfoSection title="Examination Overview">
        <InfoRow label="Examined by" value={txt(exam.veterinarianName)} />
        <InfoRow label="Examined at" value={txt(formatDateTime(exam.examinedAt))} />
        <InfoRow label="Training decision" value={<DecisionBadge decision={exam.trainingDecision} />} />
        {exam.restrictionDetails && (
          <InfoRow label="Restriction details" value={txt(exam.restrictionDetails)} />
        )}
      </AdmissionInfoSection>

      {/* ── 2. Clinical notes ───────────────────────────────────────────── */}
      <AdmissionInfoSection title="Clinical Notes">
        <InfoRow label="Symptoms" value={txt(exam.symptoms)} />
        <InfoRow label="Findings" value={txt(exam.findings)} />
        <InfoRow label="Diagnosis" value={txt(exam.diagnosis)} />
        <InfoRow label="Treatment" value={txt(exam.treatment)} />
        {exam.notes && <InfoRow label="Notes" value={txt(exam.notes)} />}
      </AdmissionInfoSection>

      {/* ── 3. Vital-sign measurements ──────────────────────────────────── */}
      <AdmissionInfoSection
        title="Vital Signs"
        aside={
          visibleMetrics.length > 0 ? (
            <span className="text-xs text-[var(--color-text-muted)]">
              {visibleMetrics.length} measurement{visibleMetrics.length !== 1 ? 's' : ''}
            </span>
          ) : undefined
        }
      >
        {visibleMetrics.length === 0 ? (
          <span className="italic text-[var(--color-text-muted)]">No vital signs recorded.</span>
        ) : (
          <div className="space-y-3">
            {visibleMetrics.map((m, i) => (
              <MetricRow key={m.id} metric={m} index={i} />
            ))}
          </div>
        )}
      </AdmissionInfoSection>
    </div>
  );
}
