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
import { useTrainingRules } from '@/features/training/hooks/useTrainingRules';
import { HorseSpecialtyBadges } from '../shared/components/HorseSpecialtyBadges';
import { HorseOverviewPedigreeTable } from './HorseOverviewPedigreeTable';
import { HorseHealthSummary } from './HorseHealthSummary';
import { HorseRacingHistory } from './HorseRacingHistory';
import { FitnessTrendChart } from '@/features/training/components/FitnessTrendChart';
import { HorseMetricsRow } from '@/features/training/components/HorseMetricsRow';
import { WorkoutHistoryTable } from '@/features/training/components/WorkoutHistoryTable';
import type { Horse } from '@/features/stable/types';
import type { HorseDocumentSlot, HorseEditRequest, HorseHealthMetric, HorsePedigree, HorseSpecialty } from '../types';
import { horseEditRequestService } from '../services/horseEditRequestService';
import { DocumentSlotGrid } from './DocumentSlotGrid';
import { EditHorseRequestModal, initialFields } from './EditHorseRequestModal';
import { normalizeFields } from '../lib/horseEdit';
import type { HorseDocumentType } from '../types';
import { toast } from '@/lib/toast';
import type { HorseAlert, HorseFitnessTrendItem } from '@/features/training/types';
import type { RaceRegistrationResponse } from '@/features/racing/types';
import { displayError, formatDate } from '@/lib/display';

const tabs: TabItem[] = [
  { id: 'overview', label: 'Overview & Pedigree', icon: 'file-text' },
  { id: 'training', label: 'Training & Fitness', icon: 'activity' },
  { id: 'health', label: 'Health & Care', icon: 'heart-pulse' },
  { id: 'racing', label: 'Racing History', icon: 'flag' },
];

export function OwnerHorseDetailView({ horseId }: { horseId: number }) {
  const rules = useTrainingRules();
  const [activeTab, setActiveTab] = useState('overview');

  const [horse, setHorse] = useState<Horse | null>(null);
  const [specialty, setSpecialty] = useState<HorseSpecialty | null>(null);
  const [pedigree, setPedigree] = useState<HorsePedigree | null>(null);

  // Training data
  const [trend, setTrend] = useState<HorseFitnessTrendItem[]>([]);
  const [alerts, setAlerts] = useState<HorseAlert[]>([]);

  // Health data
  const [healthMetrics, setHealthMetrics] = useState<HorseHealthMetric[]>([]);
  const [preventiveCare, setPreventiveCare] = useState<any[]>([]);

  // Racing data
  const [raceRegistrations, setRaceRegistrations] = useState<RaceRegistrationResponse[]>([]);

  // Documents and change requests
  const [documentSlots, setDocumentSlots] = useState<HorseDocumentSlot[]>([]);
  const [latestRequest, setLatestRequest] = useState<HorseEditRequest | null>(null);
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
        trendData,
        alertData,
        metricsData,
        preventiveData,
        racingData,
        slotData,
        requestData,
      ] = await Promise.all([
        horseService.getById(horseId),
        horseService.getSpecialty(horseId).catch(() => null),
        horseService.getPedigree(horseId).catch(() => null),
        trainingService.getFitnessTrend(horseId).catch(() => []),
        trainingService.getHorseAlerts(horseId).catch(() => []),
        horseService.getHealthMetrics(horseId).catch(() => []),
        horseService.getPreventiveCare(horseId).catch(() => []),
        racingService.listMine(horseId).catch(() => []),
        horseEditRequestService.documents(horseId).catch(() => []),
        horseEditRequestService.latest(horseId).catch(() => null),
      ]);

      setHorse(horseData);
      setSpecialty(specialtyData);
      setPedigree(pedigreeData);
      setTrend(trendData);
      setAlerts(alertData);
      setHealthMetrics(metricsData);
      setPreventiveCare(preventiveData);
      setRaceRegistrations(racingData);
      setDocumentSlots(slotData);
      setLatestRequest(requestData);
    } catch (cause) {
      setError(displayError(cause, 'Unable to load horse details.'));
    } finally {
      setLoading(false);
    }
  }, [horseId]);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  const cancelRequest = async () => {
    if (!latestRequest) return;
    setCancelling(true);
    try {
      setLatestRequest(await horseEditRequestService.cancel(horseId, latestRequest.id));
      setConfirmCancel(false);
      toast.success('Change request cancelled', 'You can submit a new one at any time.');
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
      setPageFiles({});
      toast.success('Documents sent for review', 'They replace the current ones once the groom approves.');
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
        </div>      )}

      {/* Tab 2: Training & Fitness */}
      {activeTab === 'training' && (
        <div className="space-y-6">
          {/* Key Metric Numbers */}
          <HorseMetricsRow trend={trend} rules={rules} />

          {/* Charts (Heart rate, speed, performance rating) */}
          <FitnessTrendChart trend={trend} rules={rules} />

          {/* Completed Workouts Table */}
          <WorkoutHistoryTable trend={trend} rules={rules} loading={false} />
        </div>
      )}

      {/* Tab 3: Health & Care */}
      {activeTab === 'health' && (
        <HorseHealthSummary
          alerts={alerts}
          metrics={healthMetrics}
          preventiveCare={preventiveCare}
        />
      )}

      {/* Tab 4: Racing History */}
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
