'use client';

import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { Button } from '@/components/ui/Button';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { Notice } from '@/components/ui/Notice';
import { ownerAdmissionApi, type OwnerAdmissionDetail } from '../services/ownerApi';
import { admissionsApi } from '../services/api';
import { AdmissionDetailLayout } from '../shared/components/AdmissionDetailLayout';
import { AdmissionDetailHeader } from '../shared/components/AdmissionDetailHeader';
import { AdmissionPipeline } from '../shared/components/AdmissionPipeline';
import { AdmissionInfoSection, AdmissionSideCard, InfoRow } from '../shared/components/AdmissionInfoSection';
import { AdmissionDetailTabs } from '../shared/components/AdmissionDetailTabs';
import { AdmissionDocumentsSection } from '../shared/components/AdmissionDocumentsSection';
import { AdmissionDocumentUploads } from './AdmissionDocumentUploads';

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
  const locked = detail.status !== 'GROOM_REVIEW';
  const activeStage = currentReviewStage(detail.status);
  const feedback = reviewFeedback.flatMap(({ label, field }) => {
    const value = detail[field]?.trim();
    return value ? [{ label, value }] : [];
  });
  const feedbackValue = (field: typeof reviewFeedback[number]['field'], stage: number) => {
    const value = detail[field];
    if (value?.trim()) return value;
    return activeStage !== null && stage > activeStage ? 'Not yet in review' : 'No feedback recorded';
  };
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
                  <AdmissionInfoSection title="Review feedback">
                    <InfoRow label="Groom" value={feedbackValue('groomFeedback', 0)} />
                    <InfoRow label="Veterinarian" value={feedbackValue('vetFeedback', 2)} />
                    <InfoRow label="Trainer" value={feedbackValue('trainerFeedback', 3)} />
                    <InfoRow label="Manager" value={feedbackValue('managerFeedback', 4)} />
                  </AdmissionInfoSection>
                ),
              },
            ]}
          />
        )}
        sidebar={locked ? undefined : (
          <AdmissionSideCard title="Additional supporting documents">
            <p className="text-[var(--color-text-secondary)]">You can add documents while your application is in Groom review.</p>
            <AdmissionDocumentUploads admissionId={admissionId} documents={detail.documents} locked={locked}
              onUploaded={async () => { setDetail(await ownerAdmissionApi.detail(admissionId)); }} />
          </AdmissionSideCard>
        )}
      />
    </div>
  );
}
