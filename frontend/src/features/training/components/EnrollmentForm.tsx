'use client';

import { ChoiceInput, Input, Select } from '@/components/ui/Input';
import { FormField } from '@/components/ui/FormField';
import { useCallback, useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { stableApi } from '@/features/stable/services/stableService';
import type { Horse } from '@/features/stable/types';
import { trainingApi } from '../services/trainingService';
import type {
  Course,
  JoinableCohortResponse,
  TrainingDay,
  CreateHorseTrainingPlanRequest,
  HorseTrainingPlanDetailResponse,
} from '../types';
import { EnrollmentResultSummary } from './EnrollmentResultSummary';

const DAYS_OF_WEEK: { value: TrainingDay; label: string }[] = [
  { value: 'MONDAY', label: 'Monday' },
  { value: 'TUESDAY', label: 'Tuesday' },
  { value: 'WEDNESDAY', label: 'Wednesday' },
  { value: 'THURSDAY', label: 'Thursday' },
  { value: 'FRIDAY', label: 'Friday' },
  { value: 'SATURDAY', label: 'Saturday' },
  { value: 'SUNDAY', label: 'Sunday' },
];

export function EnrollmentForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const initialCourseId = searchParams.get('courseId');

  const [courses, setCourses] = useState<Course[]>([]);
  const [horses, setHorses] = useState<Horse[]>([]);
  const [cohorts, setCohorts] = useState<JoinableCohortResponse[]>([]);
  /** Enrollment result — when non-null the form is replaced by the summary screen. */
  const [result, setResult] = useState<HorseTrainingPlanDetailResponse[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [cohortsLoading, setCohortsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Form inputs
  const [courseId, setCourseId] = useState<number | ''>(
    initialCourseId ? Number(initialCourseId) : '',
  );
  const [selectedHorseIds, setSelectedHorseIds] = useState<number[]>([]);
  const [startDate, setStartDate] = useState<string>(
    // eslint-disable-next-line react-hooks/purity
    new Date(Date.now() + 86400000).toISOString().split('T')[0], // Defaults to tomorrow
  );
  const [trainingDays, setTrainingDays] = useState<TrainingDay[]>([
    'MONDAY',
    'WEDNESDAY',
    'FRIDAY',
  ]);
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  // 1. Load courses and horses (only horses in the Trainer's block with status ELIGIBLE)
  const loadInitial = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [crs, hrs] = await Promise.all([
        trainingApi.getCourses(),
        stableApi.getHorses({ mine: true, status: 'ELIGIBLE' }),
      ]);
      setCourses(crs);
      setHorses(hrs);
      if (!initialCourseId && crs.length > 0) {
        setCourseId(crs[0].id);
      }
    } catch (err) {
      console.error('Failed to load enrollment data:', err);
      setError('Unable to load courses or eligible horses.');
    } finally {
      setLoading(false);
    }
  }, [initialCourseId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadInitial();
  }, [loadInitial]);

  // 2. Call joinable-cohorts automatically once a course is selected
  useEffect(() => {
    if (!courseId) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setCohorts([]);
      return;
    }
    setCohortsLoading(true);
    trainingApi
      .getJoinableCohorts(Number(courseId))
      .then((data) => setCohorts(data))
      .catch((err) => {
      console.warn('Unable to load group suggestions:', err);
        setCohorts([]);
      })
      .finally(() => setCohortsLoading(false));
  }, [courseId]);

  function toggleHorse(id: number) {
    setSelectedHorseIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id],
    );
  }

  function toggleDay(day: TrainingDay) {
    setTrainingDays((prev) =>
      prev.includes(day)
        ? prev.length > 1
          ? prev.filter((d) => d !== day)
          : prev
        : [...prev, day],
    );
  }

  function applyCohortSuggestion(cohort: JoinableCohortResponse) {
    setStartDate(cohort.suggestedStartDate);
    setTrainingDays(Array.from(cohort.trainingDays));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!courseId) {
      setSubmitError('Select a course.');
      return;
    }
    if (selectedHorseIds.length === 0) {
      setSubmitError('Select at least one horse for the training group.');
      return;
    }
    if (trainingDays.length === 0) {
      setSubmitError('Select at least one training day.');
      return;
    }

    setSubmitError(null);
    setSubmitting(true);
    try {
      const payload: CreateHorseTrainingPlanRequest = {
        courseId: Number(courseId),
        startDate,
        trainingDays,
        notes: notes.trim() || undefined,
        horses: selectedHorseIds.map((horseId) => ({ horseId })),
      };

      const result = await trainingApi.createPlan(payload);
      if (result && result.length > 0) {
        // Do NOT jump straight to the first plan: a group enrollment creates N plans,
        // and doing that throws away N-1 results. Show the summary so the Trainer sees how many
        // lots the group was placed into and why.
        setResult(result);
      } else {
        router.push('/trainer/courses');
      }
    } catch (err) {
      // 4. The "golden-hour slots are full" error must show the backend's detailed figures verbatim
      setSubmitError(err instanceof Error ? err.message : 'Unable to create training plans.');
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <ListSkeleton rows={5} />;

  if (error) {
    return (
      <Panel padded>
      <EmptyState icon="alert-triangle" title="Unable to load enrollment data" description={error} />
      </Panel>
    );
  }

  // Enrollment done -> replace the whole form with the result summary
  if (result) {
    return (
      <EnrollmentResultSummary
        plans={result}
        horses={horses}
        onDone={() => router.push('/trainer/courses')}
      />
    );
  }

  return (
    <div className="mx-auto w-full max-w-4xl space-y-6">
      <div>
        <h1 className="text-xl font-semibold text-[var(--color-text-primary)]">Group Training Enrollment</h1>
        <p className="text-xs text-[var(--color-text-secondary)]">
          Select multiple horses and the system will schedule them in shared workouts.
        </p>
      </div>

      {submitError && (
        <div className="rounded-[var(--radius-md)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] p-4 text-sm text-[var(--color-danger)] leading-relaxed">
            <div className="mb-1 font-semibold">Unable to create the training plan:</div>
          <div>{submitError}</div>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Step 1: choose a course */}
        <Panel padded>
          <h2 className="text-base font-semibold text-[var(--color-text-primary)] mb-3">
          1. Select a training course
          </h2>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <Select
                aria-label="Training course"
                value={courseId}
                onChange={(e) => setCourseId(e.target.value ? Number(e.target.value) : '')}
              >
                {courses.map((c) => (
                  <option key={c.id} value={c.id}>
              {c.name} ({c.totalSessions} sessions)
                  </option>
                ))}
              </Select>
            </div>
            {courseId && (
              <div className="text-xs text-[var(--color-text-secondary)] flex items-center">
                {courses.find((c) => c.id === courseId)?.targetGoal && (
                  <span>🎯 {courses.find((c) => c.id === courseId)?.targetGoal}</span>
                )}
              </div>
            )}
          </div>

          {/* Synchronized group suggestions (Joinable Cohorts) */}
          <div className="mt-4 pt-3 border-t border-[var(--color-border)]">
            <div className="text-xs font-semibold text-[var(--color-text-primary)] flex items-center gap-1.5">
            <span>Suggested groups to join</span>
            {cohortsLoading && <span className="text-xs text-[var(--color-text-muted)]">(Searching…)</span>}
            </div>

            {cohorts.length === 0 ? (
              <p className="mt-1 text-xs text-[var(--color-text-muted)] italic">
              No groups are open for this course. The group you create will be the first.
              </p>
            ) : (
              <div className="mt-2 space-y-2">
                {cohorts.map((cohort, idx) => (
                  <div
                    key={idx}
                    className="flex flex-wrap items-center justify-between gap-3 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-3 text-xs"
                  >
                    <div>
                      <div className="font-semibold text-[var(--color-text-primary)] flex items-center gap-2">
                        <span>Group &quot;{cohort.courseName}&quot;</span>
                        <Pill tone={cohort.cohortStatus === 'ACTIVE' ? 'success' : 'info'} size="sm">
                          {cohort.cohortStatus} ({cohort.horseCount} con)
                        </Pill>
                      </div>
                      <div className="mt-1 text-xs text-[var(--color-text-secondary)]">
              {cohort.note} • Starts {cohort.suggestedStartDate} (in {cohort.waitDays} days)
                      </div>
                      {/*
            Synchronized scheduling does not guarantee the same lot. Once a group is full, additional horses will be assigned to another lot in the same training window, which is limited to 240 minutes.
                      */}
                      {cohort.horseCount >= cohort.lotCapacity && (
                        <div className="mt-1 text-xs text-[var(--color-warning)]">
            This group is at capacity ({cohort.lotCapacity} horses). Additional horses will train in another session that day.
                        </div>
                      )}
                    </div>
                    <Button
                      variant="secondary"
                      size="sm"
                      type="button"
                      onClick={() => applyCohortSuggestion(cohort)}
                    >
            Use this group schedule
                    </Button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </Panel>

        {/* Step 2: choose one or more horses */}
        <Panel padded>
          <div className="flex items-center justify-between mb-3">
            <div>
              <h2 className="text-base font-semibold text-[var(--color-text-primary)]">
          2. Select horses for the group ({selectedHorseIds.length} selected)
              </h2>
              <p className="text-xs text-[var(--color-text-secondary)]">
          Only eligible horses assigned to your area are shown.
              </p>
            </div>
            {horses.length > 0 && (
              <Button
                variant="secondary"
                size="sm"
                type="button"
                onClick={() =>
                  setSelectedHorseIds(
                    selectedHorseIds.length === horses.length ? [] : horses.map((h) => h.id),
                  )
                }
              >
                {selectedHorseIds.length === horses.length ? 'Clear selection' : 'Select all'}
              </Button>
            )}
          </div>

          {horses.length === 0 ? (
            <p className="text-xs text-[var(--color-text-muted)] italic">
                No eligible horses are assigned to your area. Complete admission or assign horses to stalls first.
            </p>
          ) : (
            <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 md:grid-cols-3">
              {horses.map((horse) => {
                const isSelected = selectedHorseIds.includes(horse.id);
                return (
                  <label
                    key={horse.id}
                    className={`flex items-center gap-2.5 rounded-[var(--radius-md)] border p-2.5 text-xs cursor-pointer transition ${
                      isSelected
                        ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)] font-medium text-[var(--color-primary)]'
                        : 'border-[var(--color-border)] hover:bg-[var(--color-surface-muted)]'
                    }`}
                  >
                    <ChoiceInput
                      type="checkbox"
                      checked={isSelected}
                      onChange={() => toggleHorse(horse.id)}
                    />
                    <div className="truncate">
                      <div className="font-semibold text-[var(--color-text-primary)] truncate">
                        {horse.name}
                      </div>
                      <div className="text-xs text-[var(--color-text-muted)]">
                  {horse.breed || 'Breed unknown'}
                      </div>
                    </div>
                  </label>
                );
              })}
            </div>
          )}
        </Panel>

        {/* Step 3: start date and training weekdays */}
        <Panel padded>
          <h2 className="text-base font-semibold text-[var(--color-text-primary)] mb-3">
          3. Schedule &amp; Training Days
          </h2>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <FormField label="Start date" required>
                <Input
                  type="date"
                  required
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                />
              </FormField>
              <span className="text-xs text-[var(--color-text-muted)]">
            The group’s first session will start on this date.
              </span>
            </div>

            <div>
              <label className="block text-xs font-medium text-[var(--color-text-primary)]">
            Training days *
              </label>
              <div className="mt-2 flex flex-wrap gap-1.5">
                {DAYS_OF_WEEK.map((d) => {
                  const isChecked = trainingDays.includes(d.value);
                  return (
                    <Button
                      key={d.value}
                      size="sm"
                      variant={isChecked ? 'primary' : 'secondary'}
                      className="rounded-full"
                      aria-pressed={isChecked}
                      onClick={() => toggleDay(d.value)}
                    >
                      {d.label}
                    </Button>
                  );
                })}
              </div>
            </div>
          </div>

          <div className="mt-4">
            <FormField label="Plan notes">
              <Input
                type="text"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
              placeholder="e.g. Three horses preparing for the autumn racing season…"
              />
            </FormField>
          </div>
        </Panel>

        <div className="flex items-center justify-end gap-3 pt-2">
          <Button variant="secondary" type="button" onClick={() => router.back()}>
            Back
          </Button>
          <Button
            variant="primary"
            type="submit"
            disabled={submitting || selectedHorseIds.length === 0}
          >
            {submitting ? 'Scheduling workouts…' : `Enroll ${selectedHorseIds.length} horses`}
          </Button>
        </div>
      </form>
    </div>
  );
}
