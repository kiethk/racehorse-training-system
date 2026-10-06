'use client';

import { useEffect, useState, useCallback } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/Button';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { Tabs, type TabItem } from '@/components/ui/Tabs';
import { Icon } from '@/components/ui/Icon';
import { admissionsApi } from '../services/api';
import type { AdmissionDetailResponse } from '../types';

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
  const [activeTab, setActiveTab] = useState<string>('overview');

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
  
  const TABS: TabItem[] = [
    { id: 'overview', label: 'Overview' },
    { id: 'documents', label: 'Documents', count: detail.documents.length },
    { id: 'medical', label: 'Medical Findings' },
    { id: 'history', label: 'Review History' },
  ];

  const renderOverview = () => (
    <div className="space-y-6">
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
      
      {/* Short Trainer/Vet Assessment Summary if available */}
      {(detail.trainerFeedback || detail.vetDecision) && (
        <AdmissionInfoSection title="Assessment Summary">
          {detail.vetDecision && <InfoRow label="Vet Decision" value={detail.vetDecision} />}
          {detail.trainerFeedback && <InfoRow label="Trainer Assessment" value={detail.trainerFeedback} />}
        </AdmissionInfoSection>
      )}
    </div>
  );

  const renderDocuments = () => (
    <AdmissionDocumentsSection documents={detail.documents} assetUrl={admissionsApi.assetUrl} />
  );

  const renderMedical = () => (
    <div className="space-y-6">
      <AdmissionInfoSection title="Health & Veterinary">
        <InfoRow label="Vet Decision" value={detail.vetDecision} />
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
              <li key={hr.id} className="bg-[var(--color-surface)] border border-[var(--color-border)] p-4 rounded-[var(--radius-md)]">
                <div className="flex justify-between mb-2">
                  <span className="font-semibold text-[var(--color-text-primary)]">{hr.recordType}</span>
                  <span className="text-xs text-[var(--color-text-muted)]">{date(hr.examinedAt)}</span>
                </div>
                {hr.trainingDecision && <div className="text-sm mt-1"><span className="text-[var(--color-text-muted)]">Training:</span> {hr.trainingDecision}</div>}
                {hr.restrictionDetails && <div className="text-sm mt-1"><span className="text-[var(--color-text-muted)]">Restrictions:</span> {hr.restrictionDetails}</div>}
                {hr.diagnosis && <div className="text-sm mt-1"><span className="text-[var(--color-text-muted)]">Diagnosis:</span> {hr.diagnosis}</div>}
                {hr.followUpDate && <div className="text-sm mt-1"><span className="text-[var(--color-text-muted)]">Follow-up:</span> {date(hr.followUpDate)}</div>}
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
            <h4 className="text-xs font-bold text-[var(--color-text-secondary)] uppercase tracking-wider">Groom Review</h4>
            <InfoRow label="Decision" value={detail.groomDecision} />
            <InfoRow label="Reviewed At" value={datetime(detail.groomReviewedAt)} />
            {detail.groomFeedback && <InfoRow label="Feedback" value={detail.groomFeedback} />}
          </div>
        )}
        
        {detail.vetReviewedAt && (
          <div className="space-y-2 pt-4 border-t border-[var(--color-border)]">
            <h4 className="text-xs font-bold text-[var(--color-text-secondary)] uppercase tracking-wider">Veterinarian Review</h4>
            <InfoRow label="Decision" value={detail.vetDecision} />
            <InfoRow label="Reviewed At" value={datetime(detail.vetReviewedAt)} />
            {detail.vetFeedback && <InfoRow label="Feedback" value={detail.vetFeedback} />}
          </div>
        )}
        
        {detail.trainerReviewedAt && (
          <div className="space-y-2 pt-4 border-t border-[var(--color-border)]">
            <h4 className="text-xs font-bold text-[var(--color-text-secondary)] uppercase tracking-wider">Trainer Readiness Assessment</h4>
            <InfoRow label="Assessed At" value={datetime(detail.trainerReviewedAt)} />
            {detail.trainerFeedback && <InfoRow label="Assessment Notes" value={detail.trainerFeedback} />}
          </div>
        )}

        {detail.managerReviewedAt && (
          <div className="space-y-2 pt-4 border-t border-[var(--color-border)]">
            <h4 className="text-xs font-bold text-[var(--color-text-secondary)] uppercase tracking-wider">Manager Decision</h4>
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
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Link href={returnTo} className="inline-flex items-center gap-2 text-sm font-medium text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]">
          <Icon name="arrow-left" size={15} /> Back to admissions
        </Link>
      </div>

      {error && <div role="alert" className="border-l-2 border-[var(--color-danger)] bg-[var(--color-danger-soft)] px-4 py-3 text-sm text-[var(--color-text-primary)]">{error}</div>}

      <div className="rounded-[var(--radius-lg)] overflow-hidden border border-[var(--color-border)] shadow-sm bg-[var(--color-surface)]">
        <AdmissionDetailHeader detail={detail} horsePhotoUrl={horsePhoto ? admissionsApi.assetUrl(horsePhoto.fileUrl) : undefined} />
        
        <div className="border-t border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-6">
          <AdmissionPipeline detail={detail} />
          
          <div className="mt-8 flex flex-col lg:flex-row gap-8 items-start">
            {/* Main Content (Tabs) */}
            <div className="w-full lg:flex-[3] min-w-0">
              <div className="mb-5">
                <Tabs tabs={TABS} active={activeTab} onChange={setActiveTab} />
              </div>
              <div className="animate-in fade-in slide-in-from-bottom-2 duration-300">
                {activeTab === 'overview' && renderOverview()}
                {activeTab === 'documents' && renderDocuments()}
                {activeTab === 'medical' && renderMedical()}
                {activeTab === 'history' && renderHistory()}
              </div>
            </div>

            {/* Right Summary/Action Column */}
            <div className="w-full lg:flex-[1] space-y-5 lg:sticky lg:top-6">
              {/* Admission at a glance */}
              <div className="bg-[var(--color-surface)] border border-[var(--color-border)] rounded-[var(--radius-md)] p-5 space-y-4">
                <h3 className="text-sm font-bold text-[var(--color-text-primary)]">Admission at a glance</h3>
                <div className="space-y-3 text-[13px]">
                  <div className="flex justify-between items-center gap-4">
                    <span className="text-[var(--color-text-secondary)] whitespace-nowrap">Current Status</span>
                    <span className="font-medium text-[var(--color-text-primary)] text-right">{detail.status}</span>
                  </div>
                  {detail.quarantineStallCode && (
                    <div className="flex justify-between items-center gap-4">
                      <span className="text-[var(--color-text-secondary)] whitespace-nowrap">Quarantine Stall</span>
                      <span className="font-medium text-[var(--color-text-primary)] text-right">{detail.quarantineStallCode}</span>
                    </div>
                  )}
                  {detail.healthRecords && detail.healthRecords.length > 0 && (
                    <div className="flex justify-between items-center gap-4">
                      <span className="text-[var(--color-text-secondary)] whitespace-nowrap">Training</span>
                      <span className="font-medium text-[var(--color-text-primary)] text-right">
                        {detail.healthRecords[0].trainingDecision || 'Unknown'}
                      </span>
                    </div>
                  )}
                </div>
              </div>

              {/* Manager Final Review / Decision Panel */}
              {detail.status === 'MANAGER_REVIEW' && (
                <div className="bg-[var(--color-surface)] border border-[var(--color-border)] rounded-[var(--radius-md)] p-5">
                  <h3 className="text-sm font-bold text-[var(--color-text-primary)] mb-4">Final Admission Decision</h3>
                  <ManagerFinalReviewPanel detailData={detail} onSuccess={load} />
                  
                  <div className="mt-4 pt-4 border-t border-[var(--color-border)] space-y-2">
                    <p className="text-[11px] text-[var(--color-text-muted)]">
                      <span className="font-semibold block text-[var(--color-text-secondary)]">If Approved:</span>
                      Horse is allocated a regular stall (or auto-assigned) and becomes eligible. Quarantine stall is released.
                    </p>
                    <p className="text-[11px] text-[var(--color-text-muted)]">
                      <span className="font-semibold block text-[var(--color-text-secondary)]">If Rejected:</span>
                      Admission is terminated. Quarantine stall is released.
                    </p>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

