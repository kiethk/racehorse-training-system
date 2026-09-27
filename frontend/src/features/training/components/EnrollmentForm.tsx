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
import type { Course, JoinableCohortResponse, TrainingDay, CreateHorseTrainingPlanRequest } from '../types';

const DAYS_OF_WEEK: { value: TrainingDay; label: string }[] = [
  { value: 'MONDAY', label: 'Thứ 2' },
  { value: 'TUESDAY', label: 'Thứ 3' },
  { value: 'WEDNESDAY', label: 'Thứ 4' },
  { value: 'THURSDAY', label: 'Thứ 5' },
  { value: 'FRIDAY', label: 'Thứ 6' },
  { value: 'SATURDAY', label: 'Thứ 7' },
  { value: 'SUNDAY', label: 'Chủ Nhật' },
];

export function EnrollmentForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const initialCourseId = searchParams.get('courseId');

  const [courses, setCourses] = useState<Course[]>([]);
  const [horses, setHorses] = useState<Horse[]>([]);
  const [cohorts, setCohorts] = useState<JoinableCohortResponse[]>([]);
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
      console.error('Lỗi khi nạp dữ liệu ghi danh:', err);
      setError('Không tải được danh sách khóa học hoặc chiến mã đủ điều kiện.');
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
        console.warn('Không tải được gợi ý nhóm:', err);
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
      setSubmitError('Vui lòng chọn khóa học.');
      return;
    }
    if (selectedHorseIds.length === 0) {
      setSubmitError('Vui lòng chọn ít nhất 1 chiến mã vào nhóm huấn luyện.');
      return;
    }
    if (trainingDays.length === 0) {
      setSubmitError('Vui lòng chọn ít nhất 1 ngày tập trong tuần.');
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
        // Chuyển tới kế hoạch đầu tiên vừa tạo
        router.push(`/trainer/plans/${result[0].plan.id}`);
      } else {
        router.push('/trainer/courses');
      }
    } catch (err) {
      // 4. Lỗi "hết khe giờ vàng" phải hiện nguyên văn số liệu chi tiết từ backend
      setSubmitError(err instanceof Error ? err.message : 'Tạo kế hoạch huấn luyện thất bại.');
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <ListSkeleton rows={5} />;

  if (error) {
    return (
      <Panel padded>
        <EmptyState icon="alert-triangle" title="Lỗi dữ liệu" description={error} />
      </Panel>
    );
  }

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-[18px] font-semibold text-[var(--color-text-primary)]">
          Ghi danh huấn luyện theo nhóm
        </h1>
        <p className="text-[12px] text-[var(--color-text-secondary)]">
          Ghi danh nhiều chiến mã cùng lúc để ghép lot tự động, tối ưu hóa khung giờ vàng 06:00 – 10:00.
        </p>
      </div>

      {submitError && (
        <div className="rounded-[var(--radius-md)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] p-4 text-[13px] text-[var(--color-danger)] leading-relaxed">
          <div className="font-semibold mb-1">⚠️ Không thể lập kế hoạch:</div>
          <div>{submitError}</div>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Bước 1: Chọn khóa học */}
        <Panel padded>
          <h2 className="text-[14px] font-semibold text-[var(--color-text-primary)] mb-3">
            1. Chọn khóa học mục tiêu
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
                    {c.name} ({c.totalSessions} buổi)
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
              <span>💡 Gợi ý nhóm có thể ghép chung (Joinable Cohorts)</span>
              {cohortsLoading && <span className="text-[11px] text-[var(--color-text-muted)]">(Đang quét...)</span>}
            </div>

            {cohorts.length === 0 ? (
              <p className="mt-1 text-[11px] text-[var(--color-text-muted)] italic">
                Chưa có nhóm nào đang mở cho khóa này. Nhóm bạn tạo sẽ là nhóm khởi đầu.
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
                        <span>Nhóm &quot;{cohort.courseName}&quot;</span>
                        <Pill tone={cohort.cohortStatus === 'ACTIVE' ? 'success' : 'info'} size="sm">
                          {cohort.cohortStatus} ({cohort.horseCount} con)
                        </Pill>
                      </div>
                      <div className="mt-1 text-[11px] text-[var(--color-text-secondary)]">
                        {cohort.note} • Bắt đầu {cohort.suggestedStartDate} (chờ {cohort.waitDays} ngày)
                        → Chung lot {cohort.sharedSessions}/{cohort.totalSessions} buổi
                      </div>
                    </div>
                    <Button
                      variant="secondary"
                      size="sm"
                      type="button"
                      onClick={() => applyCohortSuggestion(cohort)}
                    >
                      Dùng lịch nhóm này
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
                2. Chọn chiến mã tham gia nhóm ({selectedHorseIds.length} con đã chọn)
              </h2>
              <p className="text-[11px] text-[var(--color-text-secondary)]">
                Chỉ hiển thị các chiến mã trong khu vực của bạn và đạt trạng thái ELIGIBLE.
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
                {selectedHorseIds.length === horses.length ? 'Bỏ chọn hết' : 'Chọn tất cả'}
              </Button>
            )}
          </div>

          {horses.length === 0 ? (
            <p className="text-[12px] text-[var(--color-text-muted)] italic">
              Không có chiến mã nào ở trạng thái ELIGIBLE trong khu của bạn. Vui lòng hoàn tất tiếp nhận hoặc xếp chuồng trước.
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
                        {horse.breed || 'Chưa rõ giống'}
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
            3. Lịch trình &amp; Các ngày tập trong tuần
          </h2>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                Ngày bắt đầu (Start Date) *
              </label>
              <input
                type="date"
                required
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
              />
              <span className="text-[11px] text-[var(--color-text-muted)]">
                Cả nhóm sẽ bắt đầu buổi học đầu tiên từ ngày này.
              </span>
            </div>

            <div>
              <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                Các thứ tập trong tuần (Training Days) *
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
              Ghi chú kế hoạch
            </label>
            <input
              type="text"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Ví dụ: Nhóm 3 ngựa chuẩn bị cho giải mùa thu..."
              className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
          </div>
        </Panel>

        <div className="flex items-center justify-end gap-3 pt-2">
          <Button variant="secondary" type="button" onClick={() => router.back()}>
            Quay lại
          </Button>
          <Button
            variant="primary"
            type="submit"
            disabled={submitting || selectedHorseIds.length === 0}
          >
            {submitting ? 'Đang lên lịch lot...' : `Ghi danh ${selectedHorseIds.length} chiến mã`}
          </Button>
        </div>
      </form>
    </div>
  );
}
