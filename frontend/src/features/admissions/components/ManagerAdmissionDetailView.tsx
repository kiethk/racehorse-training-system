'use client';

import { useEffect, useState, useCallback } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/Button';
import { Notice } from '@/components/ui/Notice';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { Tabs, type TabItem } from '@/components/ui/Tabs';
import { admissionsApi } from '../services/api';
import { formatDate, formatDateTime, formatEnumLabel } from '@/lib/display';
import type { AdmissionDetailResponse } from '../types';

import { AdmissionDetailHeader } from '../shared/components/AdmissionDetailHeader';
import { AdmissionPipeline } from '../shared/components/AdmissionPipeline';
import { AdmissionInfoSection, AdmissionSideCard, InfoGroupTitle, InfoRow } from '../shared/components/AdmissionInfoSection';
import { AdmissionStatusBadge } from '../shared/components/AdmissionStatusBadge';
import { AdmissionDocumentsSection } from '../shared/components/AdmissionDocumentsSection';
import { AdmissionDetailLayout } from '../shared/components/AdmissionDetailLayout';
import { ManagerFinalReviewPanel } from './ManagerFinalReviewPanel';

function date(value: string | null) {
  return value ? formatDate(value) : 'Not recorded';
}

function datetime(value: string | null) {
  return value ? formatDateTime(value) : 'Not recorded';
}

interface ManagerAdmissionDetailViewProps {
  admissionId: number;
  returnTo: string;
}

export function ManagerAdmissionDetailView({ admissionId, returnTo }: ManagerAdmissionDetailViewProps) {
  const [detail, setDetail] = useState<AdmissionDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<string>('overview');
  const [reopening, setReopening] = useState(false);

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

  const reopenExpiredArrival = async () => {
    setReopening(true);
    setError(null);
    try {
      await admissionsApi.reopenExpiredArrival(admissionId);
      await load();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to reopen the arrival window.');
    } finally {
      setReopening(false);
    }
  };

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void load();
  }, [load]);

  if (loading) return <div className="p-6"><ListSkeleton rows={8} /></div>;
  if (error && !detail) return <EmptyState icon="alert-triangle" title="Unable to load application" description={error} action={<Button size="sm" onClick={() => void load()}>Retry</Button>} />;
  if (!detail?.candidate) return <EmptyState title="Application not found" description="This admission record is unavailable." action={<Link className="text-sm font-medium text-[var(--color-primary)]" href={returnTo}>Back to applications</Link>} />;

  const candidate = detail.candidate;
  const horsePhoto = detail.documents.find((doc) => doc.documentType === 'HORSE_PHOTO');
  const currentTrainingDecision =
    detail.vetTrainingDecision ?? detail.healthRecords?.find((record) => record.trainingDecision)?.trainingDecision ?? null;
  // Đơn đã qua Thú y nhưng hệ thống chưa tìm được Trainer đủ điều kiện để giao.
  const trainerUnassigned = detail.status === 'TRAINER_REVIEW' && detail.trainerId == null;
  
  const TABS: TabItem[] = [
    { id: 'overview', label: 'Overview' },
    { id: 'documents', label: 'Documents', count: detail.documents.length },
    { id: 'medical', label: 'Medical Findings' },
    { id: 'history', label: 'Review History' },
  ];

  const renderOverview = () => (
    <div className="space-y-5">
      <AdmissionInfoSection title="Pedigree & Registration">
        <InfoRow label="Registry Name" value={candidate.registryName} />
        <InfoRow label="Registration No." value={candidate.registrationNumber} />
        <div className="space-y-3 border-t border-[var(--color-border)] pt-3">
          <InfoRow label="Sire" value={candidate.sireName} />
          <InfoRow label="Dam" value={candidate.damName} />
          {candidate.pedigreeNotes && (
            <div className="mt-2 text-[var(--color-text-secondary)] italic">
              &quot;{candidate.pedigreeNotes}&quot;
            </div>
          )}
        </div>
      </AdmissionInfoSection>
      
      {/* Short Trainer/Vet Assessment Summary if available */}
      {(detail.trainerFeedback || currentTrainingDecision) && (
        <AdmissionInfoSection title="Assessment Summary">
          {currentTrainingDecision && <InfoRow label="Vet Training Decision" value={currentTrainingDecision} />}
          {detail.trainerFeedback && <InfoRow label="Trainer Assessment" value={detail.trainerFeedback} />}
        </AdmissionInfoSection>
      )}
    </div>
  );

  const renderDocuments = () => (
    <AdmissionDocumentsSection documents={detail.documents} assetUrl={admissionsApi.assetUrl} />
  );

  const renderMedical = () => (
    <div className="space-y-5">
      <AdmissionInfoSection title="Health & Veterinary">
        <InfoRow label="Training Decision" value={currentTrainingDecision} />
        {detail.vetFeedback && (
          <div>
            <span className="text-[var(--color-text-muted)] block mb-1">Feedback:</span>
            <p className="text-[var(--color-text-primary)]">{detail.vetFeedback}</p>
          </div>
        )}
      </AdmissionInfoSection>
      
      <AdmissionInfoSection title="Health Records">
        {detail.healthRecords && detail.healthRecords.length > 0 ? (
          <ul className="space-y-3">
            {detail.healthRecords.map(hr => (
              <li key={hr.id} className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3">
                <div className="flex justify-between mb-2">
                  <span className="font-semibold text-[var(--color-text-primary)]">{hr.recordType}</span>
                  <span className="text-xs text-[var(--color-text-muted)]">{date(hr.examinedAt)}</span>
                </div>
                {hr.trainingDecision && <div className="text-sm mt-1"><span className="text-[var(--color-text-muted)]">Training:</span> {hr.trainingDecision}</div>}
                {hr.restrictionDetails && <div className="text-sm mt-1"><span className="text-[var(--color-text-muted)]">Restrictions:</span> {hr.restrictionDetails}</div>}
                {hr.diagnosis && <div className="text-sm mt-1"><span className="text-[var(--color-text-muted)]">Diagnosis:</span> {hr.diagnosis}</div>}
                {hr.notes && <div className="text-sm text-[var(--color-text-secondary)] mt-2 italic">{hr.notes}</div>}
              </li>
            ))}
          </ul>
        ) : (
          <span className="text-[var(--color-text-muted)] italic">No health records available.</span>
        )}
      </AdmissionInfoSection>
    </div>
  );

  const renderHistory = () => (
    <AdmissionInfoSection title="Review History">
      <div className="space-y-5">
        {detail.groomReviewedAt && (
          <div className="space-y-2">
            <InfoGroupTitle>Groom Review</InfoGroupTitle>
            <InfoRow label="Decision" value={detail.groomDecision} />
            <InfoRow label="Reviewed At" value={datetime(detail.groomReviewedAt)} />
            {detail.groomFeedback && <InfoRow label="Feedback" value={detail.groomFeedback} />}
          </div>
        )}
        
        {detail.vetReviewedAt && (
          <div className="space-y-2 pt-4 border-t border-[var(--color-border)]">
            <InfoGroupTitle>Veterinarian Review</InfoGroupTitle>
            <InfoRow label="Training Decision" value={currentTrainingDecision} />
            <InfoRow label="Reviewed At" value={datetime(detail.vetReviewedAt)} />
            {detail.vetFeedback && <InfoRow label="Feedback" value={detail.vetFeedback} />}
          </div>
        )}
        
        {detail.trainerReviewedAt && (
          <div className="space-y-2 pt-4 border-t border-[var(--color-border)]">
            <InfoGroupTitle>Trainer Readiness Assessment</InfoGroupTitle>
            <InfoRow label="Assessed At" value={datetime(detail.trainerReviewedAt)} />
            {detail.trainerFeedback && <InfoRow label="Assessment Notes" value={detail.trainerFeedback} />}
          </div>
        )}

        {detail.managerReviewedAt && (
          <div className="space-y-2 pt-4 border-t border-[var(--color-border)]">
            <InfoGroupTitle>Manager Decision</InfoGroupTitle>
            <InfoRow label="Decision" value={detail.managerDecision} />
            <InfoRow label="Reviewed At" value={datetime(detail.managerReviewedAt)} />
            {detail.managerFeedback && <InfoRow label="Notes" value={detail.managerFeedback} />}
          </div>
        )}
        
        {!detail.groomReviewedAt && !detail.vetReviewedAt && !detail.trainerReviewedAt && !detail.managerReviewedAt && (
          <span className="text-[var(--color-text-muted)] italic">No prior reviews recorded.</span>
        )}
      </div>
    </AdmissionInfoSection>
  );

  return (
    <div className="space-y-5">
      {error && <Notice tone="error">{error}</Notice>}

      <AdmissionDetailLayout
        returnTo={returnTo}
        header={<AdmissionDetailHeader detail={detail} horsePhotoUrl={horsePhoto ? admissionsApi.assetUrl(horsePhoto.fileUrl) : undefined} />}
        pipeline={<AdmissionPipeline detail={detail} />}
        content={(
          <div className="min-w-0">
            <div className="mb-5">
              <Tabs tabs={TABS} active={activeTab} onChange={setActiveTab} />
            </div>
            <div key={activeTab} className="animate-in fade-in slide-in-from-bottom-1 duration-200">
              {activeTab === 'overview' && renderOverview()}
              {activeTab === 'documents' && renderDocuments()}
              {activeTab === 'medical' && renderMedical()}
              {activeTab === 'history' && renderHistory()}
            </div>
          </div>
        )}
        sidebar={(
          <>
            <AdmissionSideCard title="Admission at a glance">
              <InfoRow label="Current status" value={<AdmissionStatusBadge status={detail.status} size="sm" />} />
              {detail.quarantineStallCode && <InfoRow label="Quarantine stall" value={detail.quarantineStallCode} />}
              {currentTrainingDecision && <InfoRow label="Training" value={formatEnumLabel(currentTrainingDecision)} />}
              {detail.trainerId != null && <InfoRow label="Head Trainer" value={detail.trainerName ?? `#${detail.trainerId}`} />}
              {trainerUnassigned && (
                <Notice tone="warning" title="No Head Trainer assigned yet">
                  No eligible Head Trainer is available. The system retries automatically; check that at
                  least one active Head Trainer has a certification number.
                </Notice>
              )}
            </AdmissionSideCard>

            {detail.status === 'ARRIVAL_EXPIRED' && (
              <AdmissionSideCard title="Arrival window expired" tone="warning">
                <p className="text-[var(--color-text-secondary)]">The quarantine reservation was released. Reopening attempts to reserve an available quarantine stall and starts a new 14-day window.</p>
                <Button type="button" className="w-full" loading={reopening} onClick={() => void reopenExpiredArrival()}>Reopen arrival window</Button>
              </AdmissionSideCard>
            )}

            {detail.status === 'MANAGER_REVIEW' && (
              <AdmissionSideCard title="Final admission decision" tone="primary" className="space-y-4">
                <ManagerFinalReviewPanel detailData={detail} onSuccess={load} />
                <div className="space-y-2 border-t border-[var(--color-border)] pt-3 text-xs text-[var(--color-text-muted)]">
                  <p>
                    <span className="block font-semibold text-[var(--color-text-secondary)]">If approved</span>
                    Horse is allocated a regular stall (or auto-assigned) and becomes eligible. Quarantine stall is released.
                  </p>
                  <p>
                    <span className="block font-semibold text-[var(--color-text-secondary)]">If rejected</span>
                    Admission is terminated. Quarantine stall is released.
                  </p>
                </div>
              </AdmissionSideCard>
            )}
          </>
        )}
      />
    </div>
  );
}

