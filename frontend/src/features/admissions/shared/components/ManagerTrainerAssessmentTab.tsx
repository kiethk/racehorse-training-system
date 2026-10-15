'use client';

import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { trainerAdmissionsApi } from '../../services/trainerAdmissionService';
import type { RacingReadinessAssessment } from '../../types/trainer';
import { TrainerAssessmentSection } from './TrainerAssessmentSection';

interface ManagerTrainerAssessmentTabProps {
  admissionId: number;
  trainerName?: string | null;
}

type LoadState = 'loading' | 'error' | 'done';

/**
 * Lazy-loaded Trainer Assessment tab for Manager view.
 * Only fetches when mounted. Handles loading, error + retry, and empty state.
 * Reuses trainerAdmissionsApi.getView(admissionId) without inventing new endpoints.
 */
export function ManagerTrainerAssessmentTab({
  admissionId,
  trainerName,
}: ManagerTrainerAssessmentTabProps) {
  const [state, setState] = useState<LoadState>('loading');
  const [error, setError] = useState<string | null>(null);
  const [assessment, setAssessment] = useState<RacingReadinessAssessment | null>(null);
  const [retryKey, setRetryKey] = useState(0);

  useEffect(() => {
    let active = true;

    async function loadAssessment() {
      setState('loading');
      setError(null);
      try {
        const viewData = await trainerAdmissionsApi.getView(admissionId);
        if (active) {
          setAssessment(viewData.existingAssessment);
          setState('done');
        }
      } catch (err) {
        if (active) {
          setError(
            err instanceof Error ? err.message : 'Unable to load trainer assessment.',
          );
          setState('error');
        }
      }
    }

    void loadAssessment();

    return () => {
      active = false;
    };
  }, [admissionId, retryKey]);

  if (state === 'loading') {
    return (
      <div className="p-4">
        <ListSkeleton rows={4} />
      </div>
    );
  }

  if (state === 'error') {
    return (
      <EmptyState
        icon="alert-triangle"
        title="Unable to load trainer assessment"
        description={error ?? 'Assessment details could not be loaded.'}
        action={
          <Button size="sm" icon="refresh" onClick={() => setRetryKey((k) => k + 1)}>
            Retry
          </Button>
        }
      />
    );
  }

  if (!assessment) {
    return (
      <EmptyState
        icon="target"
        title="No trainer assessment yet"
        description="The assigned Head Trainer has not submitted a readiness evaluation for this candidate."
      />
    );
  }

  return (
    <TrainerAssessmentSection
      assessment={assessment}
      trainerName={trainerName}
    />
  );
}
