'use client';

import { useEffect, useState } from 'react';
import { Button, Icon, Notice } from '@/components/ui';
import { HorseProfileView } from '@/features/horses/shared/components/HorseProfileView';
import type { Horse } from '@/features/stable/types';
import type { HorseHealthMetric, HorsePedigree, HorseSpecialty } from '../types';
import type { HorseAlert, HorseFitnessTrendItem } from '@/features/training/types';
import type { RaceRegistrationResponse } from '@/features/racing/types';
import { horseService } from '../services/horseService';
import { trainingService } from '@/features/training/services/trainingService';
import { racingService } from '@/features/racing/services/racingService';
import { useTrainingRules } from '@/features/training/hooks/useTrainingRules';
import { ChangeHorseStatusModal } from './ChangeHorseStatusModal';
import { StableTeamTab } from './StableTeamTab';
import { HistoryTab } from './HistoryTab';
import { HorsePedigreeCard } from './HorsePedigreeCard';
import { HorseSpecialtySection } from '../shared/components/HorseSpecialtySection';
import { HorseMetricsRow } from '@/features/training/components/HorseMetricsRow';
import { FitnessTrendChart } from '@/features/training/components/FitnessTrendChart';
import { WorkoutHistoryTable } from '@/features/training/components/WorkoutHistoryTable';
import { HorseHealthSummary } from './HorseHealthSummary';
import { HorseRacingHistory } from './HorseRacingHistory';

interface ManagerHorseDetailViewProps {
  horseId: number;
}

/**
 * 360-degree comprehensive horse detail view for Club Manager.
 * Combines Overview & Pedigree, Stable & Team, Training & Fitness,
 * Medical & Vet Care, Racing History, and Status History into an executive command view.
 */
export function ManagerHorseDetailView({ horseId }: ManagerHorseDetailViewProps) {
  const rules = useTrainingRules();

  // Status transition state
  const [modalHorse, setModalHorse] = useState<Horse | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [onSuccessCallback, setOnSuccessCallback] = useState<(() => void) | null>(null);

  // Cross-actor operational data
  const [specialty, setSpecialty] = useState<HorseSpecialty | null>(null);
  const [pedigree, setPedigree] = useState<HorsePedigree | null>(null);
  const [trend, setTrend] = useState<HorseFitnessTrendItem[]>([]);
  const [alerts, setAlerts] = useState<HorseAlert[]>([]);
  const [healthMetrics, setHealthMetrics] = useState<HorseHealthMetric[]>([]);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [preventiveCare, setPreventiveCare] = useState<any[]>([]);
  const [raceRegistrations, setRaceRegistrations] = useState<RaceRegistrationResponse[]>([]);
  const [loadingFacets, setLoadingFacets] = useState(true);

  useEffect(() => {
    let active = true;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoadingFacets(true);

    Promise.all([
      horseService.getSpecialty(horseId).catch(() => null),
      horseService.getPedigree(horseId).catch(() => null),
      trainingService.getFitnessTrend(horseId).catch(() => []),
      trainingService.getHorseAlerts(horseId).catch(() => []),
      horseService.getHealthMetrics(horseId).catch(() => []),
      horseService.getPreventiveCare(horseId).catch(() => []),
      racingService.listMine(horseId).catch(() => []),
    ]).then(([specialtyData, pedigreeData, trendData, alertData, metricsData, preventiveData, racingData]) => {
      if (!active) return;
      setSpecialty(specialtyData);
      setPedigree(pedigreeData);
      setTrend(trendData);
      setAlerts(alertData);
      setHealthMetrics(metricsData);
      setPreventiveCare(preventiveData);
      setRaceRegistrations(racingData);
    }).finally(() => {
      if (active) setLoadingFacets(false);
    });

    return () => {
      active = false;
    };
  }, [horseId]);

  return (
    <>
      <HorseProfileView
        horseId={horseId}
        returnTo="/manager/horses"
        backLabel="Back to horses"
        showSpecialty={false}
        headerActions={({ horse, reload }) => {
          const isEligible = horse.currentStatus === 'ELIGIBLE';
          const isInactive = horse.currentStatus === 'INACTIVE';

          if (!isEligible && !isInactive) return null;

          return (
            <Button
              type="button"
              variant={isInactive ? 'primary' : 'secondary'}
              size="sm"
              icon={isInactive ? 'check' : 'alert-triangle'}
              onClick={() => {
                setModalHorse(horse);
                setOnSuccessCallback(() => reload);
                setModalOpen(true);
              }}
            >
              {isInactive ? 'Restore to Active' : 'Change Status'}
            </Button>
          );
        }}
        overviewContent={({ horse }) => (
          <HorsePedigreeCard pedigree={pedigree} horse={horse} />
        )}
        extraTabs={({ horse }) => [
          {
            id: 'stable-team',
            label: 'Stable & Team',
            content: (
              <StableTeamTab
                ownerId={horse.ownerId ?? null}
                currentStallId={horse.currentStallId ?? null}
              />
            ),
          },
          {
            id: 'training',
            label: 'Training & Fitness',
            content: (
              <div className="space-y-6">
                <HorseMetricsRow trend={trend} rules={rules} />
                <FitnessTrendChart trend={trend} rules={rules} />
                <WorkoutHistoryTable trend={trend} rules={rules} loading={loadingFacets} />
              </div>
            ),
          },
          {
            id: 'medical',
            label: 'Medical & Vet Care',
            content: (
              <div className="space-y-5">
                {horse.trainingDecision === 'BLOCKED' && (
                  <Notice tone="error" title="Veterinary Training Suspension Active">
                    {horse.trainingDecisionReason || 'This horse has been placed on medical hold by veterinary staff.'}
                  </Notice>
                )}
                <HorseHealthSummary
                  alerts={alerts}
                  metrics={healthMetrics}
                  preventiveCare={preventiveCare}
                />
              </div>
            ),
          },
          {
            id: 'racing',
            label: 'Racing History',
            content: (
              <div className="space-y-6">
                <HorseSpecialtySection specialty={specialty} />
                <HorseRacingHistory registrations={raceRegistrations} />
              </div>
            ),
          },
          {
            id: 'history',
            label: 'Status History',
            content: <HistoryTab horseId={horse.id} />,
          },
        ]}
      />

      <ChangeHorseStatusModal
        horse={modalHorse}
        open={modalOpen}
        onClose={() => {
          setModalOpen(false);
          setModalHorse(null);
        }}
        onSuccess={() => {
          if (onSuccessCallback) {
            onSuccessCallback();
          }
        }}
      />
    </>
  );
}
