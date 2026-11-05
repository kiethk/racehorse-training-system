import { Pill } from '@/components/ui/StatusBadge';
import { formatEnumLabel, formatDateTime } from '@/lib/display';
import { AdmissionInfoSection, InfoRow } from './AdmissionInfoSection';
import type { RacingReadinessAssessment, RacingReadinessStatus } from '../../types/trainer';

function readinessTone(status: RacingReadinessStatus): 'success' | 'warning' | 'danger' {
  if (status === 'READY') return 'success';
  if (status === 'NEEDS_MORE_TRAINING') return 'warning';
  return 'danger';
}

export function TrainerReadinessBadge({
  status,
  size = 'md',
}: {
  status: RacingReadinessStatus;
  size?: 'sm' | 'md';
}) {
  return (
    <Pill tone={readinessTone(status)} size={size}>
      {formatEnumLabel(status)}
    </Pill>
  );
}

function formatScore(score: number | null | undefined): string {
  if (score === null || score === undefined) return '—';
  return `${score}/10`;
}

interface TrainerAssessmentSectionProps {
  assessment: RacingReadinessAssessment;
  trainerName?: string | null;
}

/**
 * Pure presentation component for Trainer Assessment in admission details.
 * Reusable across Manager, Vet, or Trainer read-only views.
 */
export function TrainerAssessmentSection({
  assessment,
  trainerName,
}: TrainerAssessmentSectionProps) {
  const months = assessment.estimatedMonthsToRace;

  return (
    <div className="space-y-5">
      <AdmissionInfoSection title="Readiness & Evaluation">
        <InfoRow
          label="Readiness status"
          value={<TrainerReadinessBadge status={assessment.readinessStatus} size="sm" />}
        />
        <InfoRow
          label="Estimated time to race"
          value={months != null ? `${months} ${months === 1 ? 'month' : 'months'}` : '—'}
        />
        <InfoRow
          label="Assessed by"
          value={trainerName ?? (assessment.trainerId != null ? `#${assessment.trainerId}` : '—')}
        />
        <InfoRow
          label="Assessment date"
          value={formatDateTime(assessment.assessmentDate || assessment.createdAt)}
        />
      </AdmissionInfoSection>

      <AdmissionInfoSection title="Evaluation Scores">
        <InfoRow
          label="Conformation score"
          value={formatScore(assessment.conformationScore)}
        />
        <InfoRow
          label="Temperament score"
          value={formatScore(assessment.temperamentScore)}
        />
        <InfoRow
          label="Gait quality score"
          value={formatScore(assessment.gaitQualityScore)}
        />
      </AdmissionInfoSection>

      {assessment.remarks && (
        <AdmissionInfoSection title="Professional Remarks">
          <p className="whitespace-pre-wrap text-sm text-[var(--color-text-primary)] leading-relaxed">
            {assessment.remarks}
          </p>
        </AdmissionInfoSection>
      )}
    </div>
  );
}
