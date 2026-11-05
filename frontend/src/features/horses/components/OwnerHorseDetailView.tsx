'use client';

import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import {
  Button,
  ConfirmDialog,
  EmptyState,
  HorseAvatar,
  Icon,
  Notice,
  PageHeader,
  Panel,
  Pill,
  ScreenLayout,
  Spinner,
  StatusBadge,
  Tabs,
  type TabItem,
} from '@/components/ui';
import { horseService } from '../services/horseService';
import { trainingService } from '@/features/training/services/trainingService';
import { racingService } from '@/features/racing/services/racingService';
import { HorseSpecialtyBadges } from '../shared/components/HorseSpecialtyBadges';
import { HorseOverviewPedigreeTable } from './HorseOverviewPedigreeTable';
import { HorseHealthSummary } from './HorseHealthSummary';
import { HorseRacingHistory } from './HorseRacingHistory';
import { HorseAttendanceList } from '@/features/training/components/HorseAttendanceList';
import { HorseFitnessTrendView } from '@/features/training/components/HorseFitnessTrendView';
import { HorseScheduleView } from '@/features/training/components/HorseScheduleDialog';
import { toIsoDate } from '@/features/training/lib/dateRange';
import type { Horse } from '@/features/stable/types';
import type { HorseDocumentSlot, HorseEditRequest, HorseHealthMetric, HorsePedigree, HorseSpecialty } from '../types';
import { horseEditRequestService } from '../services/horseEditRequestService';
import { DocumentSlotGrid } from './DocumentSlotGrid';
import { EditHorseRequestModal, initialFields } from './EditHorseRequestModal';
import { OwnerHorseChangeHistory } from './OwnerHorseChangeHistory';
import { normalizeFields } from '../lib/horseEdit';
import type { HorseDocumentType } from '../types';
import { toast } from '@/lib/toast';
import type { HorseAlert } from '@/features/training/types';
import type { RaceRegistrationResponse } from '@/features/racing/types';
import { displayError, formatDate } from '@/lib/display';

const tabs: TabItem[] = [
  { id: 'overview', label: 'Overview & Pedigree', icon: 'file-text' },
  { id: 'training-schedule', label: 'Training schedule', icon: 'calendar' },
  { id: 'training-plans', label: 'Training plans', icon: 'clipboard' },
  { id: 'fitness-trends', label: 'Fitness & Trends', icon: 'activity' },
  { id: 'health', label: 'Health & Care', icon: 'heart-pulse' },
  { id: 'racing', label: 'Racing History', icon: 'flag' },
];

export function OwnerHorseDetailView({ horseId }: { horseId: number }) {
  const today = toIsoDate(new Date());
  const [activeTab, setActiveTab] = useState('overview');

  const [horse, setHorse] = useState<Horse | null>(null);
  const [specialty, setSpecialty] = useState<HorseSpecialty | null>(null);
  const [pedigree, setPedigree] = useState<HorsePedigree | null>(null);

  // Training & alerts data
  const [alerts, setAlerts] = useState<HorseAlert[]>([]);

  // Health data
  const [healthMetrics, setHealthMetrics] = useState<HorseHealthMetric[]>([]);
  const [preventiveCare, setPreventiveCare] = useState<any[]>([]);

  // Racing data
  const [raceRegistrations, setRaceRegistrations] = useState<RaceRegistrationResponse[]>([]);

  // Documents and change requests
  const [documentSlots, setDocumentSlots] = useState<HorseDocumentSlot[]>([]);
  const [latestRequest, setLatestRequest] = useState<HorseEditRequest | null>(null);
  const [changeHistory, setChangeHistory] = useState<HorseEditRequest[]>([]);
  const [editing, setEditing] = useState(false);
  const [confirmCancel, setConfirmCancel] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  // Documents picked directly on the page (no popup), sent as a documents-only change request.
  const [pageFiles, setPageFiles] = useState<Partial<Record<HorseDocumentType, File>>>({});
  const [submittingFiles, setSubmittingFiles] = useState(false);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadAll = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [
        horseData,
        specialtyData,
        pedigreeData,
        alertData,
        metricsData,
        preventiveData,
        racingData,
        slotData,
        requestData,
        historyData,
      ] = await Promise.all([
        horseService.getById(horseId),
        horseService.getSpecialty(horseId).catch(() => null),
        horseService.getPedigree(horseId).catch(() => null),
        trainingService.getHorseAlerts(horseId).catch(() => []),
        horseService.getHealthMetrics(horseId).catch(() => []),
        horseService.getPreventiveCare(horseId).catch(() => []),
        racingService.listMine(horseId).catch(() => []),
        horseEditRequestService.documents(horseId).catch(() => []),
        horseEditRequestService.latest(horseId).catch(() => null),
        horseEditRequestService.listForOwner(horseId).catch(() => []),
      ]);

      setHorse(horseData);
      setSpecialty(specialtyData);
      setPedigree(pedigreeData);
      setAlerts(alertData);
      setHealthMetrics(metricsData);
      setPreventiveCare(preventiveData);
      setRaceRegistrations(racingData);
      setDocumentSlots(slotData);
      setLatestRequest(requestData);
      setChangeHistory(historyData);
    } catch (cause) {
      setError(displayError(cause, 'Unable to load horse details.'));
    } finally {
      setLoading(false);
    }
  }, [horseId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadAll();
  }, [loadAll]);

  const cancelRequest = async () => {
    if (!latestRequest) return;
    setCancelling(true);
    try {
      const cancelled = await horseEditRequestService.cancel(horseId, latestRequest.id);
      setLatestRequest(cancelled);
      setChangeHistory((current) => current.map((request) => request.id === cancelled.id ? cancelled : request));
      setConfirmCancel(false);
      toast.success('Change request cancelled');
    } catch (cause) {
      const message = displayError(cause, 'Unable to cancel the change request.');
      setConfirmCancel(false);
      toast.error('Change request not cancelled', message);
    } finally {
      setCancelling(false);
    }
  };

  const submitPageFiles = async () => {
    if (!horse) return;
    setSubmittingFiles(true);
    try {
      const uploads = (Object.entries(pageFiles) as [HorseDocumentType, File][])
        .map(([documentType, file]) => ({ documentType, file }));
      // Details are sent unchanged, so only the documents are up for review.
      const request = await horseEditRequestService.submit(horse.id, normalizeFields(initialFields(horse, pedigree)), uploads);
      setLatestRequest(request);
      setChangeHistory((current) => [request, ...current]);
      setPageFiles({});
      toast.success('Documents sent for review');
    } catch (cause) {
      toast.error('Documents not sent', displayError(cause, 'Unable to submit the documents.'));
    } finally {
      setSubmittingFiles(false);
    }
  };

  if (loading) {
    return (
      <ScreenLayout variant="detail">
        <Panel padded className="flex h-64 items-center justify-center">
          <Spinner />
        </Panel>
      </ScreenLayout>
    );
  }

  if (error || !horse) {
    return (
      <ScreenLayout variant="detail">
        <Panel padded>
          <EmptyState
            icon="alert-triangle"
            title="Unable to open horse details"
            description={error || 'Horse not found.'}
            action={
              <Link href="/owner/horses">
                <Button size="sm" icon="arrow-left">Back to My Horses</Button>
              </Link>
            }
          />
        </Panel>
      </ScreenLayout>
    );
  }

  const pendingRequest = latestRequest?.status === 'PENDING';
  const canEdit = horse.currentStatus === 'ELIGIBLE' && !pendingRequest;
  const editBlockedReason = horse.currentStatus !== 'ELIGIBLE'
    ? 'Details and documents can be edited once the horse is eligible.'
    : pendingRequest ? 'A change request is already waiting for groom review.' : null;

  return (
    <ScreenLayout variant="detail">
      {/* Top Breadcrumb Navigation */}
      <div className="flex items-center gap-2 text-[12px] text-[var(--color-text-muted)]">
        <Link
          href="/owner/horses"
          className="hover:text-[var(--color-primary)] flex items-center gap-1 transition-colors"
        >
          <Icon name="arrow-left" size={12} />
          My Horses
        </Link>
        <span>/</span>
        <span className="text-[var(--color-text-primary)] font-medium">{horse.name}</span>
      </div>

      {/* Horse Header Card */}
      <Panel padded className="space-y-4">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-4">
            <HorseAvatar name={horse.name} image={horse.imageUrl} size={64} rounded="md" />
            <div>
              <div className="flex flex-wrap items-center gap-2">
                <h1 className="text-xl font-bold tracking-tight text-[var(--color-text-primary)]">
                  {horse.name}
                </h1>
                <StatusBadge status={horse.currentStatus} size="sm" />
              </div>
              <p className="text-[13px] text-[var(--color-text-muted)] mt-0.5">
                {horse.breed || 'Breed not specified'} · Born: {formatDate(horse.dateOfBirth)}
              </p>
            </div>
          </div>

          <div className="flex flex-col items-start sm:items-end gap-1.5">
            <span className="text-[11px] font-semibold text-[var(--color-text-muted)] uppercase tracking-wider">
              Stall Assignment
            </span>
            <span className="text-[13px] font-medium text-[var(--color-text-primary)]">
              {horse.currentStallId ? `Stall #${horse.currentStallId}` : 'Not assigned to a stall'}
            </span>
            <Button
              size="sm"
              variant="primary"
              icon="file-text"
              disabled={!canEdit}
              title={editBlockedReason ?? undefined}
              onClick={() => setEditing(true)}
            >
              Edit horse profile
            </Button>
            {editBlockedReason && (
              <span className="max-w-[16rem] text-[11px] text-[var(--color-text-muted)] sm:text-right">{editBlockedReason}</span>
            )}
          </div>
        </div>

        {/* Racing Specialty Section */}
        <div className="pt-3 border-t border-[var(--color-border-subtle)] flex flex-wrap items-center gap-2">
          <span className="text-[12px] font-medium text-[var(--color-text-muted)] flex items-center gap-1">
            <Icon name="star" size={14} className="text-[var(--color-primary)]" />
            Racing Specialty:
          </span>
          <HorseSpecialtyBadges specialty={specialty} size="md" />
        </div>

        {pendingRequest && latestRequest && (
          <Notice tone="info" title="Changes awaiting groom review">
            <span className="block">
              Submitted on {formatDate(latestRequest.createdAt)}. The horse keeps its current details until the groom approves.
            </span>
            <Button size="sm" variant="secondary" className="mt-2" onClick={() => setConfirmCancel(true)}>Cancel request</Button>
          </Notice>
        )}
        {latestRequest?.status === 'REJECTED' && (
          <Notice tone="warning" title="Your last change request was rejected">
            {latestRequest.reviewFeedback || 'No reason was given.'}
          </Notice>
        )}
        {latestRequest?.status === 'APPROVED' && (
          <Notice tone="success" title="Your last change request was approved" />
        )}

        {/* Veterinary Training Restriction Notice */}
        {horse.trainingDecision === 'BLOCKED' && (
          <Notice tone="error" title="Training is Currently Suspended">
            {horse.trainingDecisionReason || 'This horse has been placed on medical hold by veterinary staff.'}
          </Notice>
        )}
      </Panel>

      {/* Tab Navigation Strip */}
      <Tabs tabs={tabs} active={activeTab} onChange={setActiveTab} />

      {/* Tab 1: Overview & Pedigree */}
      {activeTab === 'overview' && (
        <div className="space-y-4">
          <HorseOverviewPedigreeTable horse={horse} pedigree={pedigree} />

          <Panel padded>
            <h3 className="mb-1 text-[14px] font-semibold text-[var(--color-text-primary)] flex items-center gap-2">
              <Icon name="file-text" size={16} className="text-[var(--color-primary)]" />
              Documents
            </h3>
            <p className="mb-3 text-xs text-[var(--color-text-muted)]">
              {editBlockedReason ?? 'Replace a document or add a missing one. Changes apply after the groom approves them.'}
            </p>
            <DocumentSlotGrid
              slots={documentSlots}
              chosen={pageFiles}
              onChoose={canEdit ? (type, file) => setPageFiles((prev) => {
                const next = { ...prev };
                if (file) next[type] = file; else delete next[type];
                return next;
              }) : undefined}
            />
            {Object.keys(pageFiles).length > 0 && (
              <div className="mt-3 flex flex-wrap items-center justify-between gap-2 rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] p-3">
                <span className="text-sm text-[var(--color-text-secondary)]">
                  {Object.keys(pageFiles).length} document change(s) ready. The groom reviews them before they apply.
                </span>
                <div className="flex gap-2">
                  <Button size="sm" variant="tertiary" disabled={submittingFiles} onClick={() => setPageFiles({})}>Discard</Button>
                  <Button size="sm" variant="primary" loading={submittingFiles} onClick={() => void submitPageFiles()}>
                    Submit for review
                  </Button>
                </div>
              </div>
            )}
          </Panel>
          <OwnerHorseChangeHistory requests={changeHistory} />
        </div>
      )}

      {/* Tab 2: Training schedule */}
      {activeTab === 'training-schedule' && (
        <div className="rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4">
          <HorseScheduleView target={{ horseId: horse.id, name: horse.name, horse }} startDate={today} hideHeader />
        </div>
      )}

      {/* Tab 3: Training plans */}
      {activeTab === 'training-plans' && (
        <HorseAttendanceList horseId={horse.id} />
      )}

      {/* Tab 4: Fitness & Trends */}
      {activeTab === 'fitness-trends' && (
        <HorseFitnessTrendView horseId={horse.id} hideHeader />
      )}

      {/* Tab 5: Health & Care */}
      {activeTab === 'health' && (
        <HorseHealthSummary
          alerts={alerts}
          metrics={healthMetrics}
          preventiveCare={preventiveCare}
        />
      )}

      {/* Tab 6: Racing History */}
      {activeTab === 'racing' && (
        <HorseRacingHistory registrations={raceRegistrations} />
      )}

      {editing && (
        <EditHorseRequestModal
          open
          horse={horse}
          pedigree={pedigree}
          slots={documentSlots}
          onClose={() => setEditing(false)}
          onSubmitted={(request) => {
            setEditing(false);
            setLatestRequest(request);
            setChangeHistory((current) => [request, ...current]);
          }}
        />
      )}

      <ConfirmDialog
        open={confirmCancel}
        title="Cancel this change request?"
        description="The groom will no longer see it, and the files you uploaded with it are discarded."
        confirmLabel="Cancel request"
        cancelLabel="Keep request"
        tone="danger"
        loading={cancelling}
        onConfirm={() => void cancelRequest()}
        onCancel={() => { if (!cancelling) setConfirmCancel(false); }}
      />
    </ScreenLayout>
  );
}
