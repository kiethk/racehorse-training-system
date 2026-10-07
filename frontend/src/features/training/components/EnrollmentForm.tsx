'use client';

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
  /** Kết quả ghi danh — khác null thì thay form bằng màn tóm tắt. */
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
    new Date(Date.now() + 86400000).toISOString().split('T')[0], // Mặc định ngày mai
  );
  const [trainingDays, setTrainingDays] = useState<TrainingDay[]>([
    'MONDAY',
    'WEDNESDAY',
    'FRIDAY',
  ]);
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  // 1. Nạp danh sách khóa học và ngựa (chỉ ngựa trong khu của Trainer & trạng thái ELIGIBLE)
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
      setError('Unable to load eligible courses or horses.');
    } finally {
      setLoading(false);
    }
  }, [initialCourseId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadInitial();
  }, [loadInitial]);

  // 2. Tự động gọi joinable-cohorts khi chọn xong khóa học
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
        console.warn('Unable to load cohort suggestions:', err);
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
      setSubmitError('Please select a course.');
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
        // KHÔNG nhảy thẳng tới plan đầu tiên: ghi danh nhóm tạo ra N kế hoạch,
        // làm vậy là vứt N-1 kết quả. Hiện màn tóm tắt để Trainer thấy nhóm
        // được xếp vào mấy lot và vì sao.
        setResult(result);
      } else {
        router.push('/trainer/courses');
      }
    } catch (err) {
      // 4. Lỗi "hết khe giờ vàng" phải hiện nguyên văn số liệu chi tiết từ backend
      setSubmitError(err instanceof Error ? err.message : 'Unable to create the training plan.');
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <ListSkeleton rows={5} />;

  if (error) {
    return (
      <Panel padded>
        <EmptyState icon="alert-triangle" title="Data error" description={error} />
      </Panel>
    );
  }

  // Ghi danh xong -> thay toàn bộ form bằng màn tóm tắt kết quả
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
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-[18px] font-semibold text-[var(--color-text-primary)]">
          Group training enrollment
        </h1>
        <p className="text-[12px] text-[var(--color-text-secondary)]">
          Select multiple horses and let the system schedule shared sessions.
        </p>
      </div>

      {submitError && (
        <div className="rounded-[var(--radius-md)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] p-4 text-[13px] text-[var(--color-danger)] leading-relaxed">
          <div className="font-semibold mb-1">Unable to create plan:</div>
          <div>{submitError}</div>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Bước 1: Chọn khóa học */}
        <Panel padded>
          <h2 className="text-[14px] font-semibold text-[var(--color-text-primary)] mb-3">
            1. Select target course
          </h2>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <select
                value={courseId}
                onChange={(e) => setCourseId(e.target.value ? Number(e.target.value) : '')}
                className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
              >
                {courses.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name} ({c.totalSessions} sessions)
                  </option>
                ))}
              </select>
            </div>
            {courseId && (
              <div className="text-[12px] text-[var(--color-text-secondary)] flex items-center">
                {courses.find((c) => c.id === courseId)?.targetGoal && (
                  <span>🎯 {courses.find((c) => c.id === courseId)?.targetGoal}</span>
                )}
              </div>
            )}
          </div>

          {/* Gợi ý nhóm đồng bộ (Joinable Cohorts) */}
          <div className="mt-4 pt-3 border-t border-[var(--color-border)]">
            <div className="text-[12px] font-semibold text-[var(--color-text-primary)] flex items-center gap-1.5">
              <span>Suggested joinable cohorts</span>
              {cohortsLoading && <span className="text-[11px] text-[var(--color-text-muted)]">(Scanning...)</span>}
            </div>

            {cohorts.length === 0 ? (
              <p className="mt-1 text-[11px] text-[var(--color-text-muted)] italic">
                No open cohorts are available for this course. Your group will start a new cohort.
              </p>
            ) : (
              <div className="mt-2 space-y-2">
                {cohorts.map((cohort, idx) => (
                  <div
                    key={idx}
                    className="flex flex-wrap items-center justify-between gap-3 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-3 text-[12px]"
                  >
                    <div>
                      <div className="font-semibold text-[var(--color-text-primary)] flex items-center gap-2">
                        <span>Cohort &quot;{cohort.courseName}&quot;</span>
                        <Pill tone={cohort.cohortStatus === 'ACTIVE' ? 'success' : 'info'} size="sm">
                          {cohort.cohortStatus} ({cohort.horseCount} con)
                        </Pill>
                      </div>
                      <div className="mt-1 text-[11px] text-[var(--color-text-secondary)]">
                        {cohort.note} • Starts {cohort.suggestedStartDate} (wait {cohort.waitDays} days)
                      </div>
                      {/*
                        Synchronized start does not guarantee a shared lot. Full cohorts move
                        the next horse to another lot in the same 240-minute window.
                      */}
                      {cohort.horseCount >= cohort.lotCapacity && (
                        <div className="mt-1 text-[11px] text-[var(--color-warning)]">
                          Cohort is full at {cohort.lotCapacity} horses. Another horse will
                          train in a different session that day.
                        </div>
                      )}
                    </div>
                    <Button
                      variant="secondary"
                      size="sm"
                      type="button"
                      onClick={() => applyCohortSuggestion(cohort)}
                    >
                      Use this cohort schedule
                    </Button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </Panel>

        {/* Bước 2: Chọn nhiều chiến mã */}
        <Panel padded>
          <div className="flex items-center justify-between mb-3">
            <div>
              <h2 className="text-[14px] font-semibold text-[var(--color-text-primary)]">
                2. Select horses ({selectedHorseIds.length} selected)
              </h2>
              <p className="text-[11px] text-[var(--color-text-secondary)]">
                Only horses in your area with ELIGIBLE status are shown.
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
            <p className="text-[12px] text-[var(--color-text-muted)] italic">
              No ELIGIBLE horses are available in your area. Complete admission or stall assignment first.
            </p>
          ) : (
            <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 md:grid-cols-3">
              {horses.map((horse) => {
                const isSelected = selectedHorseIds.includes(horse.id);
                return (
                  <label
                    key={horse.id}
                    className={`flex items-center gap-2.5 rounded-[var(--radius-md)] border p-2.5 text-[12px] cursor-pointer transition ${
                      isSelected
                        ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)] font-medium text-[var(--color-primary)]'
                        : 'border-[var(--color-border)] hover:bg-[var(--color-surface-muted)]'
                    }`}
                  >
                    <input
                      type="checkbox"
                      checked={isSelected}
                      onChange={() => toggleHorse(horse.id)}
                      className="rounded text-[var(--color-primary)]"
                    />
                    <div className="truncate">
                      <div className="font-semibold text-[var(--color-text-primary)] truncate">
                        {horse.name}
                      </div>
                      <div className="text-[10px] text-[var(--color-text-muted)]">
                        {horse.breed || 'Unknown breed'}
                      </div>
                    </div>
                  </label>
                );
              })}
            </div>
          )}
        </Panel>

        {/* Bước 3: Ngày bắt đầu và các thứ tập trong tuần */}
        <Panel padded>
          <h2 className="text-[14px] font-semibold text-[var(--color-text-primary)] mb-3">
            3. Schedule &amp; training days
          </h2>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                Start date *
              </label>
              <input
                type="date"
                required
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
              />
              <span className="text-[11px] text-[var(--color-text-muted)]">
                The group will start its first session on this date.
              </span>
            </div>

            <div>
              <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                Training days *
              </label>
              <div className="mt-2 flex flex-wrap gap-1.5">
                {DAYS_OF_WEEK.map((d) => {
                  const isChecked = trainingDays.includes(d.value);
                  return (
                    <button
                      key={d.value}
                      type="button"
                      onClick={() => toggleDay(d.value)}
                      className={`rounded-full px-3 py-1 text-[11px] font-medium transition ${
                        isChecked
                          ? 'bg-[var(--color-primary)] text-white'
                          : 'border border-[var(--color-border)] bg-[var(--color-surface)] text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]'
                      }`}
                    >
                      {d.label}
                    </button>
                  );
                })}
              </div>
            </div>
          </div>

          <div className="mt-4">
            <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
              Plan notes
            </label>
            <input
              type="text"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="For example: Three-horse group preparing for the autumn race..."
              className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
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
            {submitting ? 'Scheduling lots...' : `Enroll ${selectedHorseIds.length} horses`}
          </Button>
        </div>
      </form>
    </div>
  );
}
