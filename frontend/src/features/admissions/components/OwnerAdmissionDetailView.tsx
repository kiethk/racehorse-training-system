'use client';

import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { Button } from '@/components/ui/Button';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { Notice } from '@/components/ui/Notice';
import { Pill } from '@/components/ui/StatusBadge';
import { formatDateTime } from '@/lib/display';
import { ownerAdmissionApi, type OwnerAdmissionDetail } from '../services/ownerApi';
import { admissionsApi } from '../services/api';
import { AdmissionDetailLayout } from '../shared/components/AdmissionDetailLayout';
import { AdmissionDetailHeader } from '../shared/components/AdmissionDetailHeader';
import { AdmissionPipeline } from '../shared/components/AdmissionPipeline';
import { AdmissionInfoSection, AdmissionSideCard, InfoGroupTitle, InfoRow } from '../shared/components/AdmissionInfoSection';
import { EditAdmissionModal } from './EditAdmissionModal';
import { AdmissionDetailTabs } from '../shared/components/AdmissionDetailTabs';
import { AdmissionDocumentsSection } from '../shared/components/AdmissionDocumentsSection';

const reviewFeedback = [
  { label: 'Groom', field: 'groomFeedback', stage: 0 },
  { label: 'Veterinarian', field: 'vetFeedback', stage: 4 },
  { label: 'Trainer', field: 'trainerFeedback', stage: 5 },
  { label: 'Manager', field: 'managerFeedback', stage: 6 },
] as const;

function currentReviewStage(status: OwnerAdmissionDetail['status']): number | null {
  switch (status) {
    case 'GROOM_REVIEW': return 0;
    case 'WAITING_FOR_STALL': return 1;
    case 'WAITING_FOR_ARRIVAL': return 2;
    case 'ARRIVAL_EXPIRED': return 3;
    case 'VET_REVIEW': return 4;
    case 'TRAINER_REVIEW': return 5;
    case 'MANAGER_REVIEW': return 6;
    default: return null;
  }
}

export function OwnerAdmissionDetailView({ admissionId, created }: { admissionId: number; created: boolean }) {
  const { user } = useAuth();
  const [detail, setDetail] = useState<OwnerAdmissionDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(false);
  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setDetail(await ownerAdmissionApi.detail(admissionId));
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
  if (!detail) return <EmptyState title="Application not found" description="This admission record is unavailable." />;

  const candidate = detail.candidate;
  const photo = detail.documents.find(document => document.documentType === 'HORSE_PHOTO');
  const feedback = reviewFeedback.flatMap(({ label, field }) => {
    const value = detail[field]?.trim();
    return value ? [{ label, value }] : [];
  });

  const hasAnyReview = Boolean(
    detail.groomReviewedAt ||
    detail.arrivedAt ||
    detail.arrivalDeadlineAt ||
    detail.vetReviewedAt ||
    detail.trainerReviewedAt ||
    detail.managerReviewedAt ||
    detail.groomFeedback ||
    detail.vetFeedback ||
    detail.trainerFeedback ||
    detail.managerFeedback ||
    detail.groomDecision ||
    detail.vetTrainingDecision ||
    detail.managerDecision
  );
  return (
    <div className="space-y-5">
      {created && <Notice tone="success">Admission and supporting documents submitted for Groom review.</Notice>}
      {error && <Notice tone="error">{error}</Notice>}
      {detail.status === 'REJECTED' && (
        <Notice tone="error" title="Application rejected">
          {feedback.length > 0 ? (
            <div className="mt-1 space-y-1 text-[var(--color-text-primary)]">
              {feedback.map(({ label, value }) => (
                <p key={label} className="whitespace-pre-wrap break-words">
                  <span className="font-medium">{label}:</span> {value}
                </p>
              ))}
            </div>
          ) : (
            <p className="text-[var(--color-text-secondary)]">No review feedback is recorded on this application.</p>
          )}
        </Notice>
      )}
      <AdmissionDetailLayout
        returnTo="/owner/admissions"
        header={<AdmissionDetailHeader detail={{ ...detail, ownerId: user?.userId, ownerName: user?.fullName }} horsePhotoUrl={photo ? admissionsApi.assetUrl(photo.fileUrl) : undefined} />}
        pipeline={<AdmissionPipeline detail={detail} />}
        sidebar={detail.status === 'GROOM_REVIEW' ? (
          <AdmissionSideCard title="Waiting for groom review">
            <p className="text-[var(--color-text-secondary)]">
              You can still change the horse details and replace or add documents until a groom reviews this application.
            </p>
            <Button type="button" variant="primary" icon="file-text" className="w-full" onClick={() => setEditing(true)}>
              Edit application
            </Button>
          </AdmissionSideCard>
        ) : undefined}
        content={(
          <AdmissionDetailTabs
            tabs={[
              {
                id: 'overview',
                label: 'Overview',
                content: (
                  <>
                    <AdmissionInfoSection title="Candidate horse">
                      <InfoRow label="Horse name" value={candidate.name} />
                      <InfoRow label="Breed" value={candidate.breed} />
                      <InfoRow label="Date of birth" value={candidate.dateOfBirth} />
                    </AdmissionInfoSection>
                    <AdmissionInfoSection title="Pedigree & Registration">
                      <InfoRow label="Registry name" value={candidate.registryName} />
                      <InfoRow label="Registration number" value={candidate.registrationNumber} />
                      <InfoRow label="Sire" value={candidate.sireName} />
                      <InfoRow label="Sire UELN" value={candidate.sireRegistrationNumber} />
                      <InfoRow label="Dam" value={candidate.damName} />
                      <InfoRow label="Dam UELN" value={candidate.damRegistrationNumber} />
                      {candidate.pedigreeNotes && <p className="whitespace-pre-wrap break-words text-[var(--color-text-secondary)]">{candidate.pedigreeNotes}</p>}
                    </AdmissionInfoSection>
                  </>
                ),
              },
              {
                id: 'documents',
                label: 'Documents',
                count: detail.documents.length,
                content: <AdmissionDocumentsSection documents={detail.documents} assetUrl={admissionsApi.assetUrl} />,
              },
              {
                id: 'history',
                label: 'Review History',
                content: (
                  <AdmissionInfoSection title="Review History">
                    {!hasAnyReview ? (
                      <p className="text-[var(--color-text-secondary)] italic">
                        Application is currently awaiting review. Review notes and decisions will appear here as the application progresses.
                      </p>
                    ) : (
                      <div className="space-y-6">
                        {/* 1. Groom Review & Arrival */}
                        {(detail.groomReviewedAt || detail.groomFeedback || detail.groomDecision || detail.arrivedAt || detail.arrivalDeadlineAt) && (
                          <div className="space-y-3">
                            <InfoGroupTitle>Quarantine Groom Review</InfoGroupTitle>
                            {detail.groomDecision && (
                              <InfoRow
                                label="Decision"
                                value={
                                  <Pill tone={detail.groomDecision === 'APPROVED' ? 'success' : 'danger'}>
                                    {detail.groomDecision === 'APPROVED' ? 'Accepted for Quarantine' : 'Rejected'}
                                  </Pill>
                                }
                              />
                            )}
                            {detail.groomReviewedAt && (
                              <InfoRow label="Reviewed At" value={formatDateTime(detail.groomReviewedAt)} />
                            )}
                            {detail.arrivalDeadlineAt && (
                              <InfoRow label="Arrival Deadline" value={formatDateTime(detail.arrivalDeadlineAt)} />
                            )}
                            {detail.arrivedAt && (
                              <InfoRow label="Checked In At" value={formatDateTime(detail.arrivedAt)} />
                            )}
                            <InfoRow
                              label="Groom Notes"
                              value={detail.groomFeedback?.trim() || <span className="text-[var(--color-text-muted)] italic">No notes recorded</span>}
                            />
                          </div>
                        )}

                        {/* 2. Veterinarian Review */}
                        {(detail.vetReviewedAt || detail.vetFeedback || detail.vetTrainingDecision) && (
                          <div className="space-y-3 pt-5 border-t border-[var(--color-border)]">
                            <InfoGroupTitle>Veterinarian Clinical Examination</InfoGroupTitle>
                            {detail.vetTrainingDecision && (
                              <InfoRow
                                label="Clinical Conclusion"
                                value={
                                  <Pill
                                    tone={detail.vetTrainingDecision === 'ALLOWED' ? 'success' : 'warning'}
                                  >
                                    {detail.vetTrainingDecision === 'ALLOWED'
                                      ? 'Approved for Training'
                                      : 'Temporarily Blocked from Training'}
                                  </Pill>
                                }
                              />
                            )}
                            {detail.vetReviewedAt && (
                              <InfoRow label="Examined At" value={formatDateTime(detail.vetReviewedAt)} />
                            )}
                            <InfoRow
                              label="Clinical Notes"
                              value={detail.vetFeedback?.trim() || <span className="text-[var(--color-text-muted)] italic">No notes recorded</span>}
                            />
                          </div>
                        )}

                        {/* 3. Trainer Review */}
                        {(detail.trainerReviewedAt || detail.trainerFeedback) && (
                          <div className="space-y-3 pt-5 border-t border-[var(--color-border)]">
                            <InfoGroupTitle>Trainer Assessment</InfoGroupTitle>
                            {detail.trainerReviewedAt && (
                              <InfoRow label="Assessed At" value={formatDateTime(detail.trainerReviewedAt)} />
                            )}
                            <InfoRow
                              label="Assessment Notes"
                              value={detail.trainerFeedback?.trim() || <span className="text-[var(--color-text-muted)] italic">No notes recorded</span>}
                            />
                          </div>
                        )}

                        {/* 4. Manager Review */}
                        {(detail.managerReviewedAt || detail.managerFeedback || detail.managerDecision) && (
                          <div className="space-y-3 pt-5 border-t border-[var(--color-border)]">
                            <InfoGroupTitle>Manager Final Decision</InfoGroupTitle>
                            {detail.managerDecision && (
                              <InfoRow
                                label="Final Decision"
                                value={
                                  <Pill tone={detail.managerDecision === 'APPROVED' ? 'success' : 'danger'}>
                                    {detail.managerDecision === 'APPROVED' ? 'Approved into Club' : 'Rejected'}
                                  </Pill>
                                }
                              />
                            )}
                            {detail.managerReviewedAt && (
                              <InfoRow label="Decided At" value={formatDateTime(detail.managerReviewedAt)} />
                            )}
                            <InfoRow
                              label="Manager Notes"
                              value={detail.managerFeedback?.trim() || <span className="text-[var(--color-text-muted)] italic">No notes recorded</span>}
                            />
                          </div>
                        )}
                      </div>
                    )}
                  </AdmissionInfoSection>
                ),
              },
            ]}
          />
        )}
      />
      {editing && (
        <EditAdmissionModal
          detail={detail}
          onClose={() => setEditing(false)}
          onSaved={(updated) => { setDetail(updated); setEditing(false); }}
        />
      )}
    </div>
  );
}
