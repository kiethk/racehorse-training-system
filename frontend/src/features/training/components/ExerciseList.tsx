'use client';

import { useCallback, useEffect, useState } from 'react';
import { Button, EmptyState, ListSkeleton, Modal, PageSection, Panel, Pill } from '@/components/ui';
import { displayError, formatNumber } from '@/lib/display';
import { toast } from '@/lib/toast';
import { categoryLabel, intensityOption, surfaceLabel } from '../lib/exerciseOptions';
import { newestFirst } from '../lib/listOrder';
import { trainingService } from '../services/trainingService';
import type { CreateExerciseRequest, Exercise } from '../types';
import { ExerciseForm } from './ExerciseForm';

function ExerciseCard({ exercise, onOpen }: { exercise: Exercise; onOpen: () => void }) {
  const intensity = intensityOption(exercise.intensityLevel);
  return (
    <Panel padded className="flex h-full flex-col justify-between gap-3">
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <h3 className="text-base font-semibold text-[var(--color-text-primary)]">{exercise.name}</h3>
          <p className="text-xs text-[var(--color-text-muted)]">{categoryLabel(exercise.category)}</p>
        </div>
        <Pill tone={intensity.tone} size="sm">{intensity.label}</Pill>
      </div>
      <div className="border-t border-[var(--color-border)] pt-3">
        <Button size="sm" onClick={onOpen}>View details</Button>
      </div>
    </Panel>
  );
}

function ExerciseDetailModal({ exercise, onClose }: { exercise: Exercise | null; onClose: () => void }) {
  return (
    <Modal
      open={exercise !== null}
      onClose={onClose}
      size="sm"
      title={exercise?.name ?? ''}
      description={exercise ? categoryLabel(exercise.category) : undefined}
      footer={<Button onClick={onClose}>Close</Button>}
    >
      {exercise && (
        <div className="space-y-3 text-sm">
          {exercise.description && <p className="text-[var(--color-text-secondary)]">{exercise.description}</p>}
          <dl className="grid grid-cols-2 gap-x-4 gap-y-2">
            <dt className="text-[var(--color-text-muted)]">Intensity</dt>
            <dd><Pill tone={intensityOption(exercise.intensityLevel).tone} size="sm">{intensityOption(exercise.intensityLevel).label}</Pill></dd>
            <dt className="text-[var(--color-text-muted)]">Duration</dt>
            <dd className="font-medium">{exercise.durationMinutes} min</dd>
            <dt className="text-[var(--color-text-muted)]">Distance</dt>
            <dd className="font-medium">{formatNumber(exercise.targetDistanceMeters)} m</dd>
            <dt className="text-[var(--color-text-muted)]">Surface</dt>
            <dd className="font-medium">{surfaceLabel(exercise.surfaceType)}</dd>
          </dl>
        </div>
      )}
    </Modal>
  );
}

export function ExerciseList() {
  const [exercises, setExercises] = useState<Exercise[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [detail, setDetail] = useState<Exercise | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setExercises([...(await trainingService.getExercises())].sort(newestFirst));
    } catch (cause) {
      setError(displayError(cause, 'Unable to load the exercise library.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  async function createExercise(request: CreateExerciseRequest) {
    setFormError(null);
    setSubmitting(true);
    try {
      await trainingService.createExercise(request);
      toast.success('Exercise created.');
      setShowForm(false);
      await load();
    } catch (cause) {
      setFormError(displayError(cause, 'Unable to create the exercise.'));
    } finally {
      setSubmitting(false);
    }
  }

  if (loading && exercises.length === 0) return <Panel padded><ListSkeleton rows={4} /></Panel>;

  if (error) {
    return (
      <Panel padded>
        <EmptyState
          icon="alert-triangle"
          title="Unable to load the exercise library"
          description={error}
          action={<Button size="sm" icon="refresh" onClick={load}>Try again</Button>}
        />
      </Panel>
    );
  }

  return (
    <PageSection
      title={`Exercise library (${exercises.length})`}
      actions={
        <Button variant="primary" size="sm" onClick={() => setShowForm((open) => !open)}>
          {showForm ? 'Close form' : 'Create exercise'}
        </Button>
      }
    >
      {showForm && (
        <ExerciseForm submitting={submitting} error={formError} onSubmit={createExercise} onCancel={() => setShowForm(false)} />
      )}

      {exercises.length === 0 ? (
        <Panel padded>
          <EmptyState icon="clipboard" title="No exercises yet" description="Create your first exercise to start building a training course." />
        </Panel>
      ) : (
        <div className="grid grid-cols-1 gap-3 md:grid-cols-2 lg:grid-cols-3">
          {exercises.map((exercise) => <ExerciseCard key={exercise.id} exercise={exercise} onOpen={() => setDetail(exercise)} />)}
        </div>
      )}

      <ExerciseDetailModal exercise={detail} onClose={() => setDetail(null)} />
    </PageSection>
  );
}
