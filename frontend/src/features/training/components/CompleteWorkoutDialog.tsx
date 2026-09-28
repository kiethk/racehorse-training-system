'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { trainingApi } from '../services/trainingService';
import type { PlanWorkoutItemResponse, CompleteWorkoutRequest } from '../types';

interface CompleteWorkoutDialogProps {
  workout: PlanWorkoutItemResponse | null;
  open: boolean;
  onClose: () => void;
  onSuccess: () => Promise<void> | void;
}

export function CompleteWorkoutDialog({
  workout,
  open,
  onClose,
  onSuccess,
}: CompleteWorkoutDialogProps) {
  const [actualDistanceMeters, setActualDistanceMeters] = useState<number | ''>('');
  const [actualDurationMinutes, setActualDurationMinutes] = useState<number | ''>('');
  const [topSpeedKmh, setTopSpeedKmh] = useState<number | ''>('');
  const [averageSpeedKmh, setAverageSpeedKmh] = useState<number | ''>('');
  const [averageHeartRate, setAverageHeartRate] = useState<number | ''>('');
  const [maxHeartRate, setMaxHeartRate] = useState<number | ''>('');
  const [recoveryHeartRate, setRecoveryHeartRate] = useState<number | ''>('');
  const [performanceRating, setPerformanceRating] = useState<number>(8);
  const [trainerFeedback, setTrainerFeedback] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!open || !workout) return null;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (performanceRating < 1 || performanceRating > 10) {
      setError('Điểm đánh giá thể hiện (performance rating) phải từ 1 đến 10.');
      return;
    }

    setError(null);
    setSubmitting(true);
    try {
      const payload: CompleteWorkoutRequest = {
        actualDistanceMeters: actualDistanceMeters !== '' ? Number(actualDistanceMeters) : null,
        actualDurationMinutes: actualDurationMinutes !== '' ? Number(actualDurationMinutes) : null,
        topSpeedKmh: topSpeedKmh !== '' ? Number(topSpeedKmh) : null,
        averageSpeedKmh: averageSpeedKmh !== '' ? Number(averageSpeedKmh) : null,
        averageHeartRate: averageHeartRate !== '' ? Number(averageHeartRate) : null,
        maxHeartRate: maxHeartRate !== '' ? Number(maxHeartRate) : null,
        recoveryHeartRate: recoveryHeartRate !== '' ? Number(recoveryHeartRate) : null,
        performanceRating: Number(performanceRating),
        trainerFeedback: trainerFeedback.trim() || null,
      };

      if (!workout) return;
      await trainingApi.completeWorkout(workout.workoutId, payload);
      await onSuccess();
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Đóng buổi tập thất bại.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div
        className="absolute inset-0 bg-black/30 backdrop-blur-sm"
        onClick={() => !submitting && onClose()}
        aria-hidden="true"
      />
      <div
        role="dialog"
        aria-modal="true"
        className="relative w-full max-w-lg rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-6 shadow-xl max-h-[90vh] overflow-y-auto"
      >
        <div className="flex items-start justify-between">
          <div>
            <h2 className="text-[16px] font-semibold text-[var(--color-text-primary)]">
              Ghi nhận kết quả buổi tập #{workout.workoutId}
            </h2>
            <p className="text-[12px] text-[var(--color-text-secondary)]">
              {workout.subjectName} • Ngày {workout.lotDate} ({workout.startTime} – {workout.endTime})
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="text-[14px] text-[var(--color-text-muted)] hover:text-[var(--color-text-primary)]"
          >
            ✕
          </button>
        </div>

        {error && (
          <div className="mt-3 rounded-[var(--radius-md)] bg-[var(--color-danger-soft)] p-3 text-[12px] text-[var(--color-danger)]">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                Điểm thể hiện (Rating: 1–10) *
              </label>
              <input
                type="number"
                min="1"
                max="10"
                required
                value={performanceRating}
                onChange={(e) => setPerformanceRating(Number(e.target.value))}
                className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
              />
            </div>

            <div>
              <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                Cự ly thực tế (mét)
              </label>
              <input
                type="number"
                min="0"
                step="10"
                value={actualDistanceMeters}
                onChange={(e) => setActualDistanceMeters(e.target.value ? Number(e.target.value) : '')}
                placeholder="Ví dụ: 1200"
                className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
              />
            </div>

            <div>
              <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                Thời lượng thực tế (phút)
              </label>
              <input
                type="number"
                min="0"
                step="0.1"
                value={actualDurationMinutes}
                onChange={(e) => setActualDurationMinutes(e.target.value ? Number(e.target.value) : '')}
                placeholder="Ví dụ: 4.5"
                className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
              />
            </div>

            <div>
              <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                Tốc độ tối đa (km/h)
              </label>
              <input
                type="number"
                min="0"
                step="0.1"
                value={topSpeedKmh}
                onChange={(e) => setTopSpeedKmh(e.target.value ? Number(e.target.value) : '')}
                placeholder="Ví dụ: 58.5"
                className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
              />
            </div>

            <div>
              <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                Tốc độ trung bình (km/h)
              </label>
              <input
                type="number"
                min="0"
                step="0.1"
                value={averageSpeedKmh}
                onChange={(e) => setAverageSpeedKmh(e.target.value ? Number(e.target.value) : '')}
                placeholder="Ví dụ: 46.2"
                className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
              />
            </div>

            <div>
              <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                Nhịp tim trung bình (bpm)
              </label>
              <input
                type="number"
                min="0"
                value={averageHeartRate}
                onChange={(e) => setAverageHeartRate(e.target.value ? Number(e.target.value) : '')}
                placeholder="Ví dụ: 165"
                className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
              />
            </div>

            <div>
              <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                Nhịp tim tối đa (bpm)
              </label>
              <input
                type="number"
                min="0"
                value={maxHeartRate}
                onChange={(e) => setMaxHeartRate(e.target.value ? Number(e.target.value) : '')}
                placeholder="Ví dụ: 210"
                className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
              />
            </div>

            <div>
              <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                Nhịp tim phục hồi (bpm sau 15p)
              </label>
              <input
                type="number"
                min="0"
                value={recoveryHeartRate}
                onChange={(e) => setRecoveryHeartRate(e.target.value ? Number(e.target.value) : '')}
                placeholder="Ví dụ: 88"
                className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
              />
            </div>
          </div>

          <div>
            <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
              Nhận xét của Trainer
            </label>
            <textarea
              rows={3}
              value={trainerFeedback}
              onChange={(e) => setTrainerFeedback(e.target.value)}
              placeholder="Ghi chú về dáng chạy, độ bứt tốc, sức bền..."
              className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t border-[var(--color-border)]">
            <Button variant="secondary" size="sm" type="button" onClick={onClose} disabled={submitting}>
              Huỷ
            </Button>
            <Button variant="primary" size="sm" type="submit" disabled={submitting}>
              {submitting ? 'Đang lưu...' : 'Hoàn thành buổi tập'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
