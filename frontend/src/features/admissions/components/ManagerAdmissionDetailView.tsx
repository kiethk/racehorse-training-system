'use client';

import { useEffect, useState, useCallback } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/Button';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { admissionsApi } from '../services/api';
import type { AdmissionDetailResponse } from '../types';

import { AdmissionDetailLayout } from '../shared/components/AdmissionDetailLayout';
import { AdmissionDetailHeader } from '../shared/components/AdmissionDetailHeader';
import { AdmissionPipeline } from '../shared/components/AdmissionPipeline';
import { AdmissionInfoSection, InfoRow } from '../shared/components/AdmissionInfoSection';
import { AdmissionDocumentsSection } from '../shared/components/AdmissionDocumentsSection';
import { ManagerFinalReviewPanel } from './ManagerFinalReviewPanel';

function date(value: string | null) {
  return value ? new Date(value).toLocaleDateString() : 'Not recorded';
}

function datetime(value: string | null) {
  return value ? new Date(value).toLocaleString('en-GB', { dateStyle: 'short', timeStyle: 'short' }) : 'Not recorded';
}

interface ManagerAdmissionDetailViewProps {
  admissionId: number;
  returnTo: string;
}

export function ManagerAdmissionDetailView({ admissionId, returnTo }: ManagerAdmissionDetailViewProps) {
  const [detail, setDetail] = useState<AdmissionDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setDetail(await admissionsApi.getAdmissionDetail(admissionId));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to load this application.');
    } finally {
      setLoading(false);
    }
  }, [admissionId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void load();
  }, [load]);

  if (loading) return <div className="p-6"><ListSkeleton rows={8} /></div>;
  if (error && !detail) return <EmptyState icon="alert-triangle" title="Unable to load application" description={error} action={<Button size="sm" onClick={() => void load()}>Retry</Button>} />;
  if (!detail?.candidate) return <EmptyState title="Application not found" description="This admission record is unavailable." action={<Link className="text-sm font-medium text-[var(--color-primary)]" href={returnTo}>Back to applications</Link>} />;

  const candidate = detail.candidate;
  const horsePhoto = detail.documents.find((doc) => doc.documentType === 'HORSE_PHOTO');
  
  const candidateSection = (
    <AdmissionInfoSection title="Pedigree & Registration">
      <InfoRow label="Registry Name" value={candidate.registryName} />
      <InfoRow label="Registration No." value={candidate.registrationNumber} />
      <div className="pt-2 border-t border-[var(--color-border)] space-y-3">
        <InfoRow label="Sire" value={candidate.sireName} />
        <InfoRow label="Dam" value={candidate.damName} />
        {candidate.pedigreeNotes && (
          <div className="mt-2 text-[var(--color-text-secondary)] italic">
            &quot;{candidate.pedigreeNotes}&quot;
          </div>
        )}
      </div>
    </AdmissionInfoSection>
  );

  const healthSection = (
    <AdmissionInfoSection title="Health Screening">
      <InfoRow label="Vet Decision" value={detail.vetDecision} />
      {detail.vetFeedback && (
        <div>
          <span className="text-[var(--color-text-muted)] block mb-1">Feedback:</span>
          <p className="text-[var(--color-text-primary)]">{detail.vetFeedback}</p>
        </div>
      )}
      <div className="pt-2 border-t border-[var(--color-border)]">
        <span className="text-[var(--color-text-muted)] block mb-2 font-medium">Health Records</span>
        {detail.healthRecords && detail.healthRecords.length > 0 ? (
          <ul className="space-y-2">
            {detail.healthRecords.map(hr => (
              <li key={hr.id} className="bg-[var(--color-surface-muted)] p-2 rounded">
                <div className="flex justify-between mb-1">
                  <span className="font-semibold">{hr.recordType}</span>
                  <span className="text-[10px] text-[var(--color-text-muted)]">{date(hr.examinedAt)}</span>
                </div>
                {hr.trainingDecision && <div className="text-[11px] font-semibold">Training: {hr.trainingDecision}</div>}
                {hr.restrictionDetails && <div className="text-[11px]">Restrictions: {hr.restrictionDetails}</div>}
                {hr.followUpDate && <div className="text-[11px]">Follow-up: {date(hr.followUpDate)}</div>}
                {hr.diagnosis && <div className="text-[11px]">Diag: {hr.diagnosis}</div>}
                {hr.notes && <div className="text-[11px] text-[var(--color-text-secondary)] mt-1">{hr.notes}</div>}
              </li>
            ))}
          </ul>
        ) : (
          <span className="text-[var(--color-text-muted)] italic">No health records available.</span>
        )}
      </div>
    </AdmissionInfoSection>
  );
  
  const reviewHistorySection = (
    <AdmissionInfoSection title="Review History">
      <div className="space-y-4">
        {detail.groomReviewedAt ? (
          <div className="space-y-1">
            <h4 className="text-[11px] font-semibold text-[var(--color-text-secondary)] uppercase tracking-wider">Groom Review</h4>
            <InfoRow label="Decision" value={detail.groomDecision} />
            <InfoRow label="Reviewed At" value={datetime(detail.groomReviewedAt)} />
            <InfoRow label="Feedback" value={detail.groomFeedback} />
          </div>
        ) : null}
        
        {detail.vetReviewedAt ? (
          <div className="space-y-1 pt-3 border-t border-[var(--color-border)]">
            <h4 className="text-[11px] font-semibold text-[var(--color-text-secondary)] uppercase tracking-wider">Veterinarian Review</h4>
            <InfoRow label="Decision" value={detail.vetDecision} />
            <InfoRow label="Reviewed At" value={datetime(detail.vetReviewedAt)} />
            <InfoRow label="Feedback" value={detail.vetFeedback} />
          </div>
        ) : null}
        
        {detail.trainerReviewedAt ? (
          <div className="space-y-1 pt-3 border-t border-[var(--color-border)]">
            <h4 className="text-[11px] font-semibold text-[var(--color-text-secondary)] uppercase tracking-wider">Trainer Review</h4>
            <InfoRow label="Reviewed At" value={datetime(detail.trainerReviewedAt)} />
            <InfoRow label="Feedback" value={detail.trainerFeedback} />
          </div>
        ) : null}
        
        {!detail.groomReviewedAt && !detail.vetReviewedAt && !detail.trainerReviewedAt && (
          <span className="text-[var(--color-text-muted)] italic">No prior reviews recorded.</span>
        )}
      </div>
    </AdmissionInfoSection>
  );

  return (
    <>
      {error && <div role="alert" className="mb-4 border-l-2 border-[var(--color-danger)] bg-[var(--color-danger-soft)] px-4 py-3 text-sm text-[var(--color-text-primary)]">{error}</div>}
      <AdmissionDetailLayout
        returnTo={returnTo}
        header={<AdmissionDetailHeader detail={detail} horsePhotoUrl={horsePhoto ? admissionsApi.assetUrl(horsePhoto.fileUrl) : undefined} />}
        pipeline={<AdmissionPipeline detail={detail} />}
        sections={[
          candidateSection,
          healthSection,
          <AdmissionDocumentsSection key="docs" documents={detail.documents} assetUrl={admissionsApi.assetUrl} />,
          reviewHistorySection,
        ]}
        actions={<ManagerFinalReviewPanel detailData={detail} onSuccess={load} />}
      />
    </>
  );
}
