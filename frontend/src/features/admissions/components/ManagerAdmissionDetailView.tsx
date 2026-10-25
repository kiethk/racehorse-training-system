'use client';

import { useEffect, useState, useCallback } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/Button';
import { Notice } from '@/components/ui/Notice';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { Tabs, type TabItem } from '@/components/ui/Tabs';
import { admissionsApi } from '../services/api';
import { formatDateTime, formatDate, formatEnumLabel } from '@/lib/display';
import { formatHorseSex } from '@/features/horses/lib/sex';
import { Pill } from '@/components/ui/StatusBadge';
import type { AdmissionDetailResponse } from '../types';

import { AdmissionDetailHeader } from '../shared/components/AdmissionDetailHeader';
import { AdmissionPipeline } from '../shared/components/AdmissionPipeline';
import { AdmissionInfoSection, AdmissionSideCard, InfoGroupTitle, InfoRow } from '../shared/components/AdmissionInfoSection';
import { AdmissionStatusBadge } from '../shared/components/AdmissionStatusBadge';
import { AdmissionDocumentsSection } from '../shared/components/AdmissionDocumentsSection';
import { AdmissionDetailLayout } from '../shared/components/AdmissionDetailLayout';
import { ManagerTrainerAssessmentTab } from '../shared/components/ManagerTrainerAssessmentTab';
import { MedicalFindingsSection } from '../shared/components/MedicalFindingsSection';
import { ManagerFinalReviewPanel } from './ManagerFinalReviewPanel';

function datetime(value: string | null) {
  return value ? formatDateTime(value) : 'Not recorded';
}

function calculateAge(dateOfBirth: string | null | undefined): string | null {
  if (!dateOfBirth) return null;
  const dob = new Date(dateOfBirth);
  if (Number.isNaN(dob.getTime())) return null;
  const now = new Date();
  let years = now.getFullYear() - dob.getFullYear();
  const m = now.getMonth() - dob.getMonth();
  if (m < 0 || (m === 0 && now.getDate() < dob.getDate())) {
    years--;
  }
  if (years < 1) {
    const months = Math.max(0, (now.getFullYear() - dob.getFullYear()) * 12 + now.getMonth() - dob.getMonth());
    return `${months} mo`;
  }
  return `${years} yo`;
}

function calculateDaysInQuarantine(arrivedAt: string | null | undefined): number | null {
  if (!arrivedAt) return null;
  const arrival = new Date(arrivedAt);
  if (Number.isNaN(arrival.getTime())) return null;
  const diffMs = Date.now() - arrival.getTime();
  return Math.max(0, Math.floor(diffMs / (1000 * 60 * 60 * 24)));
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
  // Use only this admission's vet decision — never infer from arbitrary health records.
  const currentTrainingDecision =
    detail.vetTrainingDecision ?? detail.initialVetExam?.trainingDecision ?? null;
  // The admission passed the vet stage but the system has not found an eligible Trainer to assign yet.
  const trainerUnassigned = detail.status === 'TRAINER_REVIEW' && detail.trainerId == null;
  const age = calculateAge(candidate.dateOfBirth);
  const daysInQuarantine = calculateDaysInQuarantine(detail.arrivedAt);
  
  const TABS: TabItem[] = [
    { id: 'overview', label: 'Overview' },
    { id: 'documents', label: 'Documents', count: detail.documents.length },
    { id: 'medical', label: 'Medical Findings' },
    { id: 'trainer-assessment', label: 'Trainer Assessment' },
    { id: 'history', label: 'Review History' },
  ];

  const renderOverview = () => (
    <div className="space-y-5">
      {/* 1. Candidate Identification & Pedigree */}
      <AdmissionInfoSection title="Candidate Identification & Pedigree">
        <InfoRow
          label="Date of Birth"
          value={
            candidate.dateOfBirth
              ? `${formatDate(candidate.dateOfBirth)}${age ? ` (${age})` : ''}`
              : 'Not recorded'
          }
        />
        <InfoRow label="Breed" value={candidate.breed || 'Not provided'} />
        <InfoRow label="Sex" value={formatHorseSex(candidate.sex)} />
        <InfoRow label="Registry Name" value={candidate.registryName} />
        <InfoRow label="Registration No." value={candidate.registrationNumber} />
        <div className="space-y-3 border-t border-[var(--color-border)] pt-3">
          <InfoRow
            label="Sire"
            value={
              candidate.sireName
                ? `${candidate.sireName}${candidate.sireRegistrationNumber ? ` (#${candidate.sireRegistrationNumber})` : ''}`
                : null
            }
          />
          <InfoRow
            label="Dam"
            value={
              candidate.damName
                ? `${candidate.damName}${candidate.damRegistrationNumber ? ` (#${candidate.damRegistrationNumber})` : ''}`
                : null
            }
          />
          {candidate.pedigreeNotes && (
            <div className="mt-2 text-xs text-[var(--color-text-secondary)] italic">
              &quot;{candidate.pedigreeNotes}&quot;
            </div>
          )}
        </div>
      </AdmissionInfoSection>

      {/* 2. Owner & Contact Information */}
      <AdmissionInfoSection title="Owner & Intake Details">
        <InfoRow
          label="Owner"
          value={
            detail.ownerName
              ? `${detail.ownerName}${detail.ownerId != null ? ` (#${detail.ownerId})` : ''}`
              : detail.ownerId != null
                ? `#${detail.ownerId}`
                : '—'
          }
        />
        <InfoRow label="Owner Email" value={detail.ownerEmail} />
        <InfoRow label="Owner Phone" value={detail.ownerPhone} />
        <div className="space-y-3 border-t border-[var(--color-border)] pt-3">
          <InfoRow label="Submitted At" value={datetime(detail.submittedAt)} />
          <InfoRow label="Quarantine Stall" value={detail.quarantineStallCode} />
          <InfoRow
            label="Arrival Status"
            value={
              detail.arrivedAt
                ? `Arrived on ${datetime(detail.arrivedAt)}${daysInQuarantine != null ? ` (${daysInQuarantine} days in quarantine)` : ''}`
                : detail.arrivalDeadlineAt
                  ? `Pending arrival (Deadline: ${datetime(detail.arrivalDeadlineAt)})`
                  : 'Not arrived'
            }
          />
        </div>
      </AdmissionInfoSection>

      {/* 3. Executive Assessment Highlights */}
      <AdmissionInfoSection title="Executive Assessment Highlights">
        <InfoRow
          label="Vet Clearance"
          value={
            currentTrainingDecision ? (
              <Pill tone={currentTrainingDecision === 'ALLOWED' ? 'success' : 'danger'} size="sm">
                {formatEnumLabel(currentTrainingDecision)}
              </Pill>
            ) : (
              <span className="italic text-[var(--color-text-muted)]">Pending vet examination</span>
            )
          }
        />
        <InfoRow
          label="Examining Vet"
          value={
            detail.veterinarianName ??
            detail.initialVetExam?.veterinarianName ??
            (detail.veterinarianId ? `#${detail.veterinarianId}` : null)
          }
        />
        {(detail.vetFeedback || detail.initialVetExam?.diagnosis) && (
          <InfoRow
            label="Vet Diagnosis / Notes"
            value={detail.vetFeedback || detail.initialVetExam?.diagnosis}
          />
        )}
        <div className="space-y-3 border-t border-[var(--color-border)] pt-3">
          <InfoRow
            label="Trainer Readiness"
            value={
              detail.trainerFeedback ? (
                'Assessment submitted'
              ) : detail.trainerId ? (
                <span className="italic text-[var(--color-text-muted)]">Awaiting trainer evaluation</span>
              ) : (
                <span className="italic text-[var(--color-text-muted)]">Pending trainer assignment</span>
              )
            }
          />
          <InfoRow
            label="Assigned Trainer"
            value={detail.trainerName ?? (detail.trainerId ? `#${detail.trainerId}` : null)}
          />
          {detail.trainerFeedback && (
            <InfoRow label="Trainer Notes" value={detail.trainerFeedback} />
          )}
        </div>
      </AdmissionInfoSection>
    </div>
  );

  const renderDocuments = () => (
    <AdmissionDocumentsSection documents={detail.documents} assetUrl={admissionsApi.assetUrl} />
  );

  const renderMedical = () => {
    if (!detail.initialVetExam) {
      if (detail.vetReviewedAt) {
        return (
          <EmptyState
            icon="alert-triangle"
            title="Examination details unavailable"
            action={<Button size="sm" icon="refresh" onClick={() => void load()}>Retry</Button>}
          />
        );
      }
      return (
        <EmptyState
          icon="stethoscope"
          title="No examination recorded"
          description="The veterinarian has not yet completed the initial admission exam for this horse."
        />
      );
    }
    return <MedicalFindingsSection exam={detail.initialVetExam} />;
  };

  const renderHistory = () => (
    <AdmissionInfoSection title="Review History">
      <div className="space-y-5">
        {detail.groomReviewedAt && (
          <div className="space-y-2">
            <InfoGroupTitle>Groom Review</InfoGroupTitle>
            <InfoRow
              label="Reviewed by"
              value={detail.groomName ?? (detail.groomId ? `#${detail.groomId}` : 'Assigned Groom')}
            />
            <InfoRow label="Decision" value={detail.groomDecision} />
            <InfoRow label="Reviewed At" value={datetime(detail.groomReviewedAt)} />
            {detail.groomFeedback && <InfoRow label="Feedback" value={detail.groomFeedback} />}
          </div>
        )}
        
        {detail.vetReviewedAt && (
          <div className="space-y-2 pt-4 border-t border-[var(--color-border)]">
            <InfoGroupTitle>Veterinarian Review</InfoGroupTitle>
            <InfoRow
              label="Examined by"
              value={
                detail.veterinarianName ??
                detail.initialVetExam?.veterinarianName ??
                (detail.veterinarianId ? `#${detail.veterinarianId}` : 'Assigned Veterinarian')
              }
            />
            <InfoRow label="Training Decision" value={currentTrainingDecision} />
            <InfoRow label="Reviewed At" value={datetime(detail.vetReviewedAt)} />
            {detail.vetFeedback && <InfoRow label="Feedback" value={detail.vetFeedback} />}
          </div>
        )}
        
        {detail.trainerReviewedAt && (
          <div className="space-y-2 pt-4 border-t border-[var(--color-border)]">
            <InfoGroupTitle>Trainer Readiness Assessment</InfoGroupTitle>
            <InfoRow
              label="Assessed by"
              value={detail.trainerName ?? (detail.trainerId ? `#${detail.trainerId}` : 'Assigned Trainer')}
            />
            <InfoRow label="Assessed At" value={datetime(detail.trainerReviewedAt)} />
            {detail.trainerFeedback && <InfoRow label="Assessment Notes" value={detail.trainerFeedback} />}
          </div>
        )}

        {detail.managerReviewedAt && (
          <div className="space-y-2 pt-4 border-t border-[var(--color-border)]">
            <InfoGroupTitle>Manager Decision</InfoGroupTitle>
            <InfoRow
              label="Decided by"
              value={detail.managerName ?? (detail.managerId ? `#${detail.managerId}` : 'Manager')}
            />
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
              {activeTab === 'trainer-assessment' && (
                <ManagerTrainerAssessmentTab
                  admissionId={admissionId}
                  trainerName={detail.trainerName}
                />
              )}
              {activeTab === 'history' && renderHistory()}
            </div>
          </div>
        )}
        sidebar={(
          <>
            <AdmissionSideCard title="Admission at a glance">
              <InfoRow label="Current status" value={<AdmissionStatusBadge status={detail.status} size="sm" />} />
              {detail.quarantineStallCode && <InfoRow label="Quarantine stall" value={detail.quarantineStallCode} />}
              <InfoRow
                label="Assigned Groom"
                value={detail.groomName ?? (detail.groomId ? `#${detail.groomId}` : 'Unassigned')}
              />
              <InfoRow
                label="Examining Vet"
                value={
                  detail.veterinarianName ??
                  detail.initialVetExam?.veterinarianName ??
                  (detail.veterinarianId ? `#${detail.veterinarianId}` : 'Pending assignment')
                }
              />
              {currentTrainingDecision && (
                <InfoRow
                  label="Vet clearance"
                  value={
                    <Pill tone={currentTrainingDecision === 'ALLOWED' ? 'success' : 'danger'} size="sm">
                      {formatEnumLabel(currentTrainingDecision)}
                    </Pill>
                  }
                />
              )}
              <InfoRow
                label="Head Trainer"
                value={detail.trainerName ?? (detail.trainerId ? `#${detail.trainerId}` : 'Unassigned')}
              />
              {detail.arrivedAt && (
                <InfoRow
                  label="Quarantine stay"
                  value={
                    daysInQuarantine != null
                      ? `${daysInQuarantine} days (${formatDate(detail.arrivedAt)})`
                      : formatDate(detail.arrivedAt)
                  }
                />
              )}
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

