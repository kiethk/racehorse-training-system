'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { BackLink, Button, EmptyState, ListSkeleton, Notice, PageHeader, Panel, ScreenLayout } from '@/components/ui';
import { displayError } from '@/lib/display';
import { toIsoDate, tomorrowIso } from '../lib/dateRange';
import { usePlanPreview } from '../hooks/usePlanPreview';
import { trainingService } from '../services/trainingService';
import type {
  Course,
  CreateHorseTrainingPlanRequest,
  HorseTrainingPlanDetailResponse,
  JoinableCohortResponse,
  TrainingDay,
} from '../types';
import { CourseStep } from './enrollment/CourseStep';
import { GroupScheduleNotice } from './enrollment/GroupScheduleNotice';
import { HorseStep } from './enrollment/HorseStep';
import { ScheduleStep } from './enrollment/ScheduleStep';
import { EnrollmentResultSummary } from './EnrollmentResultSummary';

const DEFAULT_DAYS: TrainingDay[] = ['MONDAY', 'WEDNESDAY', 'FRIDAY'];

/**
 * Enrolls a group of horses in a course. The schedule is chosen before the horses, because whether a horse is free
 * depends on the schedule; the backend preview then marks every horse as free, busy or unable to train. The backend
 * still decides lots, grooms and capacity, and checks everything again when the plans are created.
 */
export function EnrollmentForm() {
  const router = useRouter();
  const initialCourseId = useSearchParams().get('courseId');

  const [courses, setCourses] = useState<Course[]>([]);
  const [cohorts, setCohorts] = useState<JoinableCohortResponse[]>([]);
  const [cohortsLoading, setCohortsLoading] = useState(false);
  /** Enrollment result: when set, the form is replaced by the summary screen. */
  const [result, setResult] = useState<HorseTrainingPlanDetailResponse[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [courseId, setCourseId] = useState<number | ''>(initialCourseId ? Number(initialCourseId) : '');
  const [selectedHorseIds, setSelectedHorseIds] = useState<number[]>([]);
  const [startDate, setStartDate] = useState(() => tomorrowIso());
  const [trainingDays, setTrainingDays] = useState<TrainingDay[]>(DEFAULT_DAYS);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const today = toIsoDate(new Date());
  const { preview, error: previewError, initialLoading, scheduleStale, pending } =
    usePlanPreview({ courseId, startDate, trainingDays, selectedHorseIds });

  const horses = useMemo(() => preview?.horses ?? [], [preview]);
  // Judged only on a preview of the current schedule: an old answer may call a horse busy for dates no longer chosen.
  const selectedBusy = useMemo(
    () => (scheduleStale ? [] : horses.filter((h) => h.availability === 'BUSY' && selectedHorseIds.includes(h.horseId))),
    [horses, scheduleStale, selectedHorseIds],
  );

  const loadCourses = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const courseList = await trainingService.getCourses();
      setCourses(courseList);
      if (!initialCourseId && courseList.length > 0) setCourseId(courseList[0].id);
    } catch (cause) {
      setLoadError(displayError(cause, 'Unable to load courses.'));
    } finally {
      setLoading(false);
    }
  }, [initialCourseId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadCourses();
  }, [loadCourses]);

  // Groups already forming for the chosen course.
  useEffect(() => {
    if (!courseId) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setCohorts([]);
      return;
    }
    let active = true;
    setCohortsLoading(true);
    trainingService
      .getJoinableCohorts(courseId)
      .then((data) => active && setCohorts(data))
      .catch(() => active && setCohorts([]))
      .finally(() => active && setCohortsLoading(false));
    return () => {
      active = false;
    };
  }, [courseId]);

  function toggleHorse(horseId: number) {
    setSelectedHorseIds((current) =>
      current.includes(horseId) ? current.filter((id) => id !== horseId) : [...current, horseId],
    );
  }

  function toggleDay(day: TrainingDay) {
    setTrainingDays((current) => {
      if (!current.includes(day)) return [...current, day];
      return current.length > 1 ? current.filter((d) => d !== day) : current;
    });
  }

  function applyCohortSchedule(cohort: JoinableCohortResponse) {
    setStartDate(cohort.suggestedStartDate);
    setTrainingDays([...cohort.trainingDays]);
  }

  function dropBusyHorses() {
    const busyIds = new Set(selectedBusy.map((horse) => horse.horseId));
    setSelectedHorseIds((current) => current.filter((id) => !busyIds.has(id)));
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!courseId) {
      setSubmitError('Select a course.');
      return;
    }
    if (selectedHorseIds.length === 0) {
      setSubmitError('Select at least one horse for the training group.');
      return;
    }

    setSubmitError(null);
    setSubmitting(true);
    try {
      const request: CreateHorseTrainingPlanRequest = {
        courseId,
        startDate,
        trainingDays,
        horses: selectedHorseIds.map((horseId) => ({ horseId })),
      };
      const plans = await trainingService.createPlan(request);
      if (plans.length > 0) {
        // Show every plan of the group, not just the first: a group can be split over several lots and the
        // trainer needs to see how many and why.
        setResult(plans);
      } else {
        router.push('/trainer/courses');
      }
    } catch (cause) {
      // The backend's own message is kept: for example the golden-hour notice carries the exact minutes left.
      setSubmitError(displayError(cause, 'Unable to create training plans.'));
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <Panel padded><ListSkeleton rows={5} /></Panel>;

  if (loadError) {
    return (
      <Panel padded>
        <EmptyState
          icon="alert-triangle"
          title="Unable to load enrollment data"
          description={loadError}
          action={<Button size="sm" icon="refresh" onClick={loadCourses}>Try again</Button>}
        />
      </Panel>
    );
  }

  if (result) {
    const horseNames = horses.map((horse) => ({ id: horse.horseId, name: horse.horseName }));
    return <EnrollmentResultSummary plans={result} horses={horseNames} onDone={() => router.push('/trainer/courses')} />;
  }

  const blockedReason =
    selectedHorseIds.length === 0 ? 'Select at least one horse.'
      : startDate < today ? 'The start date cannot be in the past.'
        : selectedBusy.length > 0 ? 'Some selected horses are busy in this period.'
          : preview && !scheduleStale && preview.scheduleFullDates.length > 0 ? 'Your schedule is full on some training days.'
            : null;

  return (
    <ScreenLayout variant="form">
      <BackLink onClick={() => router.back()}>Back</BackLink>
      <PageHeader
        title="Group training enrollment"
        breadcrumbs={[{ label: 'Training', href: '/trainer/courses' }, { label: 'Enroll horses' }]}
      />

      {submitError && <Notice tone="error" title="Unable to create the training plan">{submitError}</Notice>}

      <form onSubmit={handleSubmit} className="space-y-5">
        <CourseStep
          courses={courses}
          courseId={courseId}
          onCourseChange={setCourseId}
          cohorts={cohorts}
          cohortsLoading={cohortsLoading}
          onUseCohort={applyCohortSchedule}
        />
        <ScheduleStep
          startDate={startDate}
          minDate={today}
          onStartDateChange={setStartDate}
          trainingDays={trainingDays}
          onToggleDay={toggleDay}
          preview={preview}
          stale={scheduleStale}
        />
        <HorseStep
          horses={horses}
          loading={initialLoading}
          stale={scheduleStale}
          error={previewError}
          scheduleReady={courseId !== '' && startDate >= today}
          selectedIds={selectedHorseIds}
          onToggle={toggleHorse}
          onSelect={setSelectedHorseIds}
        />
        <GroupScheduleNotice
          busyHorses={selectedBusy}
          groupEarliestStart={preview?.groupEarliestStart ?? null}
          pending={pending}
          onUseDate={setStartDate}
          onDropBusy={dropBusyHorses}
        />

        <div className="flex items-center justify-end gap-3">
          {blockedReason && selectedHorseIds.length > 0 && (
            <span className="text-xs text-[var(--color-text-muted)]">{blockedReason}</span>
          )}
          <Button type="submit" variant="primary" loading={submitting} disabled={blockedReason !== null}>
            {submitting ? 'Scheduling workouts…' : `Enroll ${selectedHorseIds.length} horses`}
          </Button>
        </div>
      </form>
    </ScreenLayout>
  );
}
