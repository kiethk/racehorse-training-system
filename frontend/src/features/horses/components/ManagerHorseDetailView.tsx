'use client';

import { useEffect, useState } from 'react';
import { Button, Notice } from '@/components/ui';
import { HorseProfileView } from '@/features/horses/shared/components/HorseProfileView';
import type { Horse } from '@/features/stable/types';
import type { HorseHealthMetric, HorsePedigree, HorseSpecialty } from '../types';
import type { HorseAlert } from '@/features/training/types';
import { horseService } from '../services/horseService';
import { trainingService } from '@/features/training/services/trainingService';
import { ChangeHorseStatusModal } from './ChangeHorseStatusModal';
import { StableTeamTab } from './StableTeamTab';
import { HistoryTab } from './HistoryTab';
import { HorseAttendanceList } from '@/features/training/components/horses/HorseAttendanceList';
import { HorseFitnessTrendView } from '@/features/training/components/horses/HorseFitnessTrendView';
import { HorseScheduleView } from '@/features/training/components/schedule/HorseScheduleView';
import { toIsoDate } from '@/features/training/lib/dateRange';
import { HorsePedigreeCard } from './HorsePedigreeCard';
import { HorseSpecialtySection } from '../shared/components/HorseSpecialtySection';
import { HorseHealthSummary } from './HorseHealthSummary';

interface ManagerHorseDetailViewProps {
  horseId: number;
}

/**
 * 360-degree comprehensive horse detail view for Club Manager.
 * Combines Overview & Pedigree, Training schedule, Course runs, Fitness & Trends, Stable & Team,
 * Medical & Vet Care, Specialty, and Status History into an executive command view.
 * The training tabs are the same screens the Trainer and Owner see.
 */
export function ManagerHorseDetailView({ horseId }: ManagerHorseDetailViewProps) {
  // Status transition state
  const [modalHorse, setModalHorse] = useState<Horse | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [onSuccessCallback, setOnSuccessCallback] = useState<(() => void) | null>(null);
  const today = toIsoDate(new Date());

  // Cross-actor operational data
  const [specialty, setSpecialty] = useState<HorseSpecialty | null>(null);
  const [pedigree, setPedigree] = useState<HorsePedigree | null>(null);
  const [alerts, setAlerts] = useState<HorseAlert[]>([]);
  const [healthMetrics, setHealthMetrics] = useState<HorseHealthMetric[]>([]);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [preventiveCare, setPreventiveCare] = useState<any[]>([]);

  useEffect(() => {
    let active = true;

    Promise.all([
      horseService.getSpecialty(horseId).catch(() => null),
      horseService.getPedigree(horseId).catch(() => null),
      trainingService.getHorseAlerts(horseId).catch(() => []),
      horseService.getHealthMetrics(horseId).catch(() => []),
      horseService.getPreventiveCare(horseId).catch(() => []),
    ]).then(([specialtyData, pedigreeData, alertData, metricsData, preventiveData]) => {
      if (!active) return;
      setSpecialty(specialtyData);
      setPedigree(pedigreeData);
      setAlerts(alertData);
      setHealthMetrics(metricsData);
      setPreventiveCare(preventiveData);
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
            id: 'training-schedule',
            label: 'Training schedule',
            content: (
              <div className="rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4">
                <HorseScheduleView target={{ horseId: horse.id, name: horse.name, horse }} startDate={today} hideHeader />
              </div>
            ),
          },
          {
            id: 'course-runs',
            label: 'Course runs',
            content: <HorseAttendanceList horseId={horse.id} />,
          },
          {
            id: 'fitness-trends',
            label: 'Fitness & Trends',
            content: <HorseFitnessTrendView horseId={horse.id} hideHeader />,
          },
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
            id: 'specialty',
            label: 'Specialty',
            content: <HorseSpecialtySection specialty={specialty} />,
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
