'use client';

import { useEffect, useState, useCallback } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/Button';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { admissionsApi } from '../services/api';
import { trainerAdmissionsApi } from '../services/trainerAdmissionService';
import type { TrainerAdmissionView } from '../types/trainer';
import { formatDate, formatDateTime } from '@/lib/display';

import { AdmissionDetailLayout } from '../shared/components/AdmissionDetailLayout';
import { AdmissionDetailHeader } from '../shared/components/AdmissionDetailHeader';
import { AdmissionPipeline } from '../shared/components/AdmissionPipeline';
import { AdmissionInfoSection, InfoGroupTitle, InfoRow } from '../shared/components/AdmissionInfoSection';
import { AdmissionDetailTabs } from '../shared/components/AdmissionDetailTabs';
import { Notice } from '@/components/ui/Notice';
import { AdmissionDocumentsSection } from '../shared/components/AdmissionDocumentsSection';
import { TrainerReviewActionPanel } from './TrainerReviewActionPanel';

function date(value: string | null) {
  return value ? formatDate(value) : 'Not recorded';
}

function datetime(value: string | null) {
  return value ? formatDateTime(value) : 'Not recorded';
}

interface TrainerAdmissionDetailViewProps {
  admissionId: number;
  returnTo: string;
}

export function TrainerAdmissionDetailView({ admissionId, returnTo }: TrainerAdmissionDetailViewProps) {
  const [view, setView] = useState<TrainerAdmissionView | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await trainerAdmissionsApi.getView(admissionId);
      setView(data);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Failed to load admission application.');
    } finally {
      setLoading(false);
    }
  }, [admissionId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void load();
  }, [load]);

  if (loading) {
    return (
      <div className="p-6">
        <ListSkeleton rows={8} />
      </div>
    );
  }

  if (error && !view) {
    return (
      <EmptyState
        icon="alert-triangle"
        title="Cannot load admission details"
        description={error}
        action={<Button size="sm" onClick={() => void load()}>Try Again</Button>}
      />
    );
  }

  const detail = view?.admission;
  const candidate = detail?.candidate;

  if (!detail || !candidate) {
    return (
      <EmptyState
        title="Admission application not found"
        description="The admission application does not exist or has been removed."
        action={
          <Link className="text-sm font-medium text-[var(--color-primary)]" href={returnTo}>
            Back to admissions list
          </Link>
        }
      />
    );
  }

  const horse = view.horse;
  const healthRecords = view.healthRecords;
  const healthMetrics = view.healthMetrics;
  const horsePhoto = detail.documents.find((doc) => doc.documentType === 'HORSE_PHOTO');

  // 1. Pedigree & Quarantine Housing
  const candidateSection = (
    <AdmissionInfoSection title="Pedigree & Housing">
      <InfoRow label="Registry Name" value={candidate.registryName} />
      <InfoRow label="UELN Number" value={candidate.registrationNumber} />
      <InfoRow label="Sire" value={candidate.sireName} />
      <InfoRow label="Dam" value={candidate.damName} />
      <div className="pt-2 border-t border-[var(--color-border)] space-y-3">
        <InfoRow
          label="Quarantine Stall"
          value={detail.quarantineStallCode ? `Stall ${detail.quarantineStallCode}` : 'Unassigned'}
        />
        <InfoRow
          label="Horse Profile"
          value={horse ? `#${horse.id} · ${horse.currentStatus}` : 'Not created yet'}
        />
        {candidate.pedigreeNotes && (
          <div className="mt-2 text-[var(--color-text-secondary)] italic">
            &quot;{candidate.pedigreeNotes}&quot;
          </div>
        )}
      </div>
    </AdmissionInfoSection>
  );

  // 2. Veterinary Examination & Vitals
  const healthSection = (
    <AdmissionInfoSection title="Veterinary Examination & Vitals">
      <InfoRow
        label="Veterinary Training Decision"
        value={detail.vetTrainingDecision ?? healthRecords.find((record) => record.trainingDecision)?.trainingDecision ?? 'Pending Decision'}
      />
      {detail.vetFeedback && (
        <div className="pt-2 border-t border-[var(--color-border)]">
          <span className="text-[var(--color-text-muted)] block mb-1 font-medium">Veterinarian&apos;s Remarks:</span>
          <p className="text-[var(--color-text-primary)]">{detail.vetFeedback}</p>
        </div>
      )}

      {/* Latest Vitals */}
      {healthMetrics && healthMetrics.length > 0 && (
        <div className="pt-2 border-t border-[var(--color-border)] space-y-1.5">
          <span className="text-[var(--color-text-muted)] block font-medium">
            Latest Vital Signs ({formatDate(healthMetrics[0].recordedAt)}):
          </span>
          <div className="grid grid-cols-2 gap-x-4 gap-y-2 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3">
            <div>Temperature: <strong>{healthMetrics[0].temperature != null ? `${healthMetrics[0].temperature} °C` : '—'}</strong></div>
            <div>Heart Rate: <strong>{healthMetrics[0].heartRate != null ? `${healthMetrics[0].heartRate} bpm` : '—'}</strong></div>
            <div>Respiratory Rate: <strong>{healthMetrics[0].respiratoryRate != null ? `${healthMetrics[0].respiratoryRate} bpm` : '—'}</strong></div>
            <div>Weight: <strong>{healthMetrics[0].weight != null ? `${healthMetrics[0].weight} kg` : '—'}</strong></div>
            <div>Hydration: <strong>{healthMetrics[0].hydrationStatus || '—'}</strong></div>
            <div>BCS Score: <strong>{healthMetrics[0].bodyConditionScore != null ? `${healthMetrics[0].bodyConditionScore}/9` : '—'}</strong></div>
          </div>
        </div>
      )}

      {/* Clinical Exam Records */}
      <div className="pt-2 border-t border-[var(--color-border)]">
        <span className="text-[var(--color-text-muted)] block mb-2 font-medium">Clinical Examination History</span>
        {healthRecords && healthRecords.length > 0 ? (
          <ul className="space-y-2">
            {healthRecords.map((hr) => (
              <li key={hr.id} className="space-y-1 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3">
                <div className="flex justify-between font-medium">
                  <span>{hr.recordType || 'Admission Exam'}</span>
                  <span className="text-xs text-[var(--color-text-muted)]">{date(hr.examinedAt)}</span>
                </div>
                {hr.trainingDecision && <div className="font-semibold">Training Decision: {hr.trainingDecision}</div>}
                {hr.restrictionDetails && <div>Medical Restrictions: {hr.restrictionDetails}</div>}
                {hr.diagnosis && <div>Diagnosis: {hr.diagnosis}</div>}
                {hr.symptoms && <div className="text-[var(--color-text-secondary)]">Symptoms: {hr.symptoms}</div>}
                {hr.treatment && <div className="text-[var(--color-text-secondary)]">Treatment: {hr.treatment}</div>}
                {hr.notes && <div className="text-[var(--color-text-secondary)] italic">Notes: {hr.notes}</div>}
              </li>
            ))}
          </ul>
        ) : (
          <span className="text-[var(--color-text-muted)] italic">No clinical examination records found.</span>
        )}
      </div>
    </AdmissionInfoSection>
  );

  // 3. Review History Across Pipeline
  const reviewHistorySection = (
    <AdmissionInfoSection title="Review History">
      <div className="space-y-4">
        {detail.groomReviewedAt ? (
          <div className="space-y-2">
            <InfoGroupTitle>Groom</InfoGroupTitle>
            <InfoRow label="Decision" value={detail.groomDecision} />
            <InfoRow label="Date & Time" value={datetime(detail.groomReviewedAt)} />
            <InfoRow label="Feedback" value={detail.groomFeedback} />
          </div>
        ) : null}

        {detail.vetReviewedAt ? (
          <div className="space-y-2 border-t border-[var(--color-border)] pt-4">
            <InfoGroupTitle>Veterinarian</InfoGroupTitle>
            <InfoRow
              label="Training Decision"
              value={detail.vetTrainingDecision ?? healthRecords.find((record) => record.trainingDecision)?.trainingDecision}
            />
            <InfoRow label="Date & Time" value={datetime(detail.vetReviewedAt)} />
            <InfoRow label="Feedback" value={detail.vetFeedback} />
          </div>
        ) : null}

        {detail.trainerReviewedAt ? (
          <div className="space-y-2 border-t border-[var(--color-border)] pt-4">
            <InfoGroupTitle>Head Trainer</InfoGroupTitle>
            <InfoRow label="Date & Time" value={datetime(detail.trainerReviewedAt)} />
            <InfoRow label="Feedback" value={detail.trainerFeedback} />
          </div>
        ) : null}

        {detail.managerReviewedAt ? (
          <div className="space-y-2 border-t border-[var(--color-border)] pt-4">
            <InfoGroupTitle>Club Manager</InfoGroupTitle>
            <InfoRow label="Decision" value={detail.managerDecision} />
            <InfoRow label="Date & Time" value={datetime(detail.managerReviewedAt)} />
            <InfoRow label="Feedback" value={detail.managerFeedback} />
          </div>
        ) : null}

        {!detail.groomReviewedAt && !detail.vetReviewedAt && !detail.trainerReviewedAt && (
          <span className="text-[var(--color-text-muted)] italic">No previous review records.</span>
        )}
      </div>
    </AdmissionInfoSection>
  );

  return (
    <>
      {error && <Notice tone="error" className="mb-4">{error}</Notice>}
      <AdmissionDetailLayout
        returnTo={returnTo}
        header={
          <AdmissionDetailHeader
            detail={detail}
            simplifiedStatus
            horsePhotoUrl={horsePhoto ? admissionsApi.assetUrl(horsePhoto.fileUrl) : undefined}
          />
        }
        pipeline={<AdmissionPipeline detail={detail} />}
        content={(
          <AdmissionDetailTabs
            tabs={[
              { id: 'overview', label: 'Overview', content: candidateSection },
              {
                id: 'documents',
                label: 'Documents',
                count: detail.documents.length,
                content: <AdmissionDocumentsSection documents={detail.documents} assetUrl={admissionsApi.assetUrl} />,
              },
              { id: 'medical', label: 'Medical Findings', content: healthSection },
              { id: 'history', label: 'Review History', content: reviewHistorySection },
            ]}
          />
        )}
        actions={<TrainerReviewActionPanel view={view} onSuccess={load} />}
      />
    </>
  );
}
