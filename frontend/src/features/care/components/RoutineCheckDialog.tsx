'use client';

import React, { useEffect, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { VitalsInput } from '@/components/ui/VitalsInput';
import { careApi } from '../services/careApi';
import type { LatestVitalsResponse, TrainingDecision } from '../types';

interface RoutineCheckDialogProps {
  scheduleId: number;
  horseId: number;
  horseName: string;
  stallCode?: string | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: () => void;
  onNext?: () => void;
  hasNext?: boolean;
}

export function RoutineCheckDialog({
  scheduleId,
  horseId,
  horseName,
  stallCode,
  open,
  onOpenChange,
  onSuccess,
  onNext,
  hasNext = false,
}: RoutineCheckDialogProps) {
  if (!open) return null;

  return (
    <RoutineCheckDialogInner
      scheduleId={scheduleId}
      horseId={horseId}
      horseName={horseName}
      stallCode={stallCode}
      onClose={() => onOpenChange(false)}
      onSuccess={onSuccess}
      onNext={onNext}
      hasNext={hasNext}
    />
  );
}

function RoutineCheckDialogInner({
  scheduleId,
  horseId,
  horseName,
  stallCode,
  onClose,
  onSuccess,
  onNext,
  hasNext,
}: {
  scheduleId: number;
  horseId: number;
  horseName: string;
  stallCode?: string | null;
  onClose: () => void;
  onSuccess?: () => void;
  onNext?: () => void;
  hasNext: boolean;
}) {
  const [latestVitals, setLatestVitals] = useState<LatestVitalsResponse | null>(null);

  // Form states
  const [heartRate, setHeartRate] = useState<string>('');
  const [temperature, setTemperature] = useState<string>('');
  const [respiratoryRate, setRespiratoryRate] = useState<string>('');
  const [weight, setWeight] = useState<string>('');
  const [bcs, setBcs] = useState<string>('');

  const [isAbnormal, setIsAbnormal] = useState(false);
  const [findings, setFindings] = useState('Khám lâm sàng bình thường');
  const [trainingDecision, setTrainingDecision] = useState<TrainingDecision>('ALLOWED');
  const [restrictionDetails, setRestrictionDetails] = useState('');
  const [nextScheduleDate, setNextScheduleDate] = useState('');
  const [notes, setNotes] = useState('');

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    const fetchVitals = async () => {
      try {
        const data = await careApi.getLatestVitals(horseId);
        if (active) {
          setLatestVitals(data);
          // Prefill default weight from previous if available
          if (data.weight) {
            setWeight(String(data.weight));
          }
          if (data.bodyConditionScore) {
            setBcs(String(data.bodyConditionScore));
          }
        }
      } catch {
        // Fallback silently if no previous vitals
      }
    };

    fetchVitals();
    return () => {
      active = false;
    };
  }, [horseId]);

  const handleSubmit = async (advanceNext = false) => {
    setError(null);

    // Validation
    if (trainingDecision === 'BLOCKED') {
      if (!restrictionDetails.trim()) {
        setError('Bắt buộc nhập lý do khóa tập luyện (restriction details)');
        return;
      }
      if (!nextScheduleDate) {
        setError('Bắt buộc chọn ngày hẹn tái khám khi quyết định BLOCKED');
        return;
      }
    }

    try {
      setSubmitting(true);

      const metricsList = [];
      if (heartRate || temperature || respiratoryRate || weight || bcs) {
        metricsList.push({
          heartRate: heartRate ? Number(heartRate) : undefined,
          temperature: temperature ? Number(temperature) : undefined,
          respiratoryRate: respiratoryRate ? Number(respiratoryRate) : undefined,
          weight: weight ? Number(weight) : undefined,
          bodyConditionScore: bcs ? Number(bcs) : undefined,
          notes: notes.trim() || undefined,
        });
      }

      await careApi.completeSchedule(scheduleId, {
        findings: findings.trim() || 'Khám lâm sàng định kỳ',
        diagnosis: isAbnormal ? 'Phát hiện bất thường cần theo dõi' : 'Bình thường',
        trainingDecision,
        restrictionDetails: trainingDecision === 'BLOCKED' ? restrictionDetails.trim() : undefined,
        notes: notes.trim() || undefined,
        metrics: metricsList,
        nextSchedule:
          trainingDecision === 'BLOCKED' && nextScheduleDate
            ? {
                idempotencyKey: `next-${scheduleId}-${Date.now()}`,
                horseId,
                careType: 'ROUTINE',
                scheduledDate: nextScheduleDate,
                description: 'Tái khám sau khám định kỳ bị khóa tập',
              }
            : null,
      });

      if (onSuccess) onSuccess();

      if (advanceNext && onNext) {
        onNext();
      } else {
        onClose();
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Lỗi khi lưu kết quả khám');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
      <div className="relative w-full max-w-2xl rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-[var(--color-border)] px-6 py-4 bg-[var(--color-surface-subtle)]">
          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-base text-[var(--color-text-primary)]">
                Khám định kỳ: {horseName}
              </span>
              <span className="text-xs font-mono px-2 py-0.5 rounded bg-[var(--color-surface)] border border-[var(--color-border)] text-[var(--color-text-secondary)]">
                #{horseId} {stallCode ? `· ${stallCode}` : ''}
              </span>
              <span className="text-xs font-semibold px-2 py-0.5 rounded bg-blue-100 dark:bg-blue-900/40 text-blue-700 dark:text-blue-300">
                ROUTINE
              </span>
            </div>
            {latestVitals?.recordedAt && (
              <p className="text-xs text-[var(--color-text-muted)] mt-0.5">
                Lần khám gần nhất: {new Date(latestVitals.recordedAt).toLocaleDateString('vi-VN')}
              </p>
            )}
          </div>

          <button
            type="button"
            onClick={onClose}
            className="rounded-full p-1.5 text-[var(--color-text-muted)] hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-text-primary)] transition-colors"
          >
            <Icon name="x" size={18} />
          </button>
        </div>

        {/* Content */}
        <div className="overflow-y-auto p-6 space-y-6">
          {error && (
            <div className="rounded-[var(--radius-sm)] border border-red-200 bg-red-50 p-3 text-xs text-red-700 dark:border-red-800 dark:bg-red-950/50 dark:text-red-300">
              {error}
            </div>
          )}

          {/* Sinh hiệu */}
          <div className="space-y-3">
            <h3 className="text-xs font-bold uppercase tracking-wider text-[var(--color-text-secondary)]">
              1. Sinh hiệu lâm sàng
            </h3>

            <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
              <VitalsInput
                label="Nhịp tim"
                value={heartRate}
                onChange={setHeartRate}
                previousValue={latestVitals?.heartRate ? Number(latestVitals.heartRate) : null}
                threshold={{
                  warningMin: latestVitals?.thresholds?.heartRate?.warningMin ?? 28,
                  warningMax: latestVitals?.thresholds?.heartRate?.warningMax ?? 44,
                  dangerMin: latestVitals?.thresholds?.heartRate?.dangerMin ?? 24,
                  dangerMax: latestVitals?.thresholds?.heartRate?.dangerMax ?? 60,
                  unit: latestVitals?.thresholds?.heartRate?.unit || 'bpm',
                }}
                placeholder="36"
              />
              <VitalsInput
                label="Thân nhiệt"
                value={temperature}
                onChange={setTemperature}
                previousValue={latestVitals?.temperature ? Number(latestVitals.temperature) : null}
                threshold={{
                  warningMin: latestVitals?.thresholds?.temperature?.warningMin ?? 37.2,
                  warningMax: latestVitals?.thresholds?.temperature?.warningMax ?? 38.3,
                  dangerMin: latestVitals?.thresholds?.temperature?.dangerMin ?? 36.5,
                  dangerMax: latestVitals?.thresholds?.temperature?.dangerMax ?? 39.0,
                  unit: latestVitals?.thresholds?.temperature?.unit || '°C',
                }}
                placeholder="38.0"
              />
              <VitalsInput
                label="Nhịp thở"
                value={respiratoryRate}
                onChange={setRespiratoryRate}
                previousValue={latestVitals?.respiratoryRate ? Number(latestVitals.respiratoryRate) : null}
                threshold={{
                  warningMin: latestVitals?.thresholds?.respiratoryRate?.warningMin ?? 8,
                  warningMax: latestVitals?.thresholds?.respiratoryRate?.warningMax ?? 16,
                  dangerMin: latestVitals?.thresholds?.respiratoryRate?.dangerMin ?? 6,
                  dangerMax: latestVitals?.thresholds?.respiratoryRate?.dangerMax ?? 24,
                  unit: latestVitals?.thresholds?.respiratoryRate?.unit || 'rpm',
                }}
                placeholder="12"
              />
              <VitalsInput
                label="Cân nặng"
                value={weight}
                onChange={setWeight}
                previousValue={latestVitals?.weight ? Number(latestVitals.weight) : null}
                threshold={{ unit: 'kg' }}
                placeholder="480"
              />
              <VitalsInput
                label="BCS (1-9)"
                value={bcs}
                onChange={setBcs}
                previousValue={latestVitals?.bodyConditionScore ? Number(latestVitals.bodyConditionScore) : null}
                threshold={{ warningMin: 4, warningMax: 6, dangerMin: 2, dangerMax: 8, unit: '/9' }}
                placeholder="5"
              />
            </div>
          </div>

          {/* Đánh giá lâm sàng */}
          <div className="space-y-3 border-t border-[var(--color-border)] pt-4">
            <h3 className="text-xs font-bold uppercase tracking-wider text-[var(--color-text-secondary)]">
              2. Đánh giá & Ghi chú
            </h3>

            <div className="flex items-center gap-4">
              <label className="flex items-center gap-2 text-xs font-medium cursor-pointer">
                <input
                  type="radio"
                  name="clinicalState"
                  checked={!isAbnormal}
                  onChange={() => {
                    setIsAbnormal(false);
                    setFindings('Khám lâm sàng bình thường');
                  }}
                  className="accent-[var(--color-primary)]"
                />
                <span className="text-emerald-700 dark:text-emerald-400 font-semibold">Bình thường</span>
              </label>

              <label className="flex items-center gap-2 text-xs font-medium cursor-pointer">
                <input
                  type="radio"
                  name="clinicalState"
                  checked={isAbnormal}
                  onChange={() => {
                    setIsAbnormal(true);
                    setFindings('');
                  }}
                  className="accent-amber-500"
                />
                <span className="text-amber-700 dark:text-amber-400 font-semibold">Bất thường</span>
              </label>
            </div>

            {isAbnormal && (
              <div>
                <label className="block text-xs font-medium text-[var(--color-text-primary)] mb-1">
                  Mô tả dấu hiệu bất thường
                </label>
                <input
                  type="text"
                  value={findings}
                  onChange={(e) => setFindings(e.target.value)}
                  placeholder="Ví dụ: Nhịp thở nhanh sau vận động nhẹ, mắt lừ đừ..."
                  className="w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] px-3 py-1.5 text-xs bg-[var(--color-surface)] text-[var(--color-text-primary)] focus:outline-none focus:border-[var(--color-primary)]"
                />
              </div>
            )}

            <div>
              <label className="block text-xs font-medium text-[var(--color-text-primary)] mb-1">
                Ghi chú thêm của Bác sĩ
              </label>
              <textarea
                rows={2}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Ghi chú theo dõi, chế độ ăn hoặc dặn dò Groom..."
                className="w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] px-3 py-1.5 text-xs bg-[var(--color-surface)] text-[var(--color-text-primary)] focus:outline-none focus:border-[var(--color-primary)]"
              />
            </div>
          </div>

          {/* Quyết định huấn luyện */}
          <div className="space-y-3 border-t border-[var(--color-border)] pt-4">
            <h3 className="text-xs font-bold uppercase tracking-wider text-[var(--color-text-secondary)]">
              3. Quyết định huấn luyện
            </h3>

            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => setTrainingDecision('ALLOWED')}
                className={`p-3 rounded-[var(--radius-sm)] border text-left transition-all ${
                  trainingDecision === 'ALLOWED'
                    ? 'border-emerald-500 bg-emerald-50 dark:bg-emerald-950/30 text-emerald-900 dark:text-emerald-200 ring-2 ring-emerald-500/20'
                    : 'border-[var(--color-border)] bg-[var(--color-surface)] text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-subtle)]'
                }`}
              >
                <div className="flex items-center gap-2 font-bold text-xs">
                  <span className="w-2.5 h-2.5 rounded-full bg-emerald-500" />
                  ALLOWED (Cho phép tập)
                </div>
                <p className="text-[11px] text-[var(--color-text-muted)] mt-1">
                  Chiến mã khỏe mạnh, mở khóa tập luyện bình thường.
                </p>
              </button>

              <button
                type="button"
                onClick={() => setTrainingDecision('BLOCKED')}
                className={`p-3 rounded-[var(--radius-sm)] border text-left transition-all ${
                  trainingDecision === 'BLOCKED'
                    ? 'border-red-500 bg-red-50 dark:bg-red-950/30 text-red-900 dark:text-red-200 ring-2 ring-red-500/20'
                    : 'border-[var(--color-border)] bg-[var(--color-surface)] text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-subtle)]'
                }`}
              >
                <div className="flex items-center gap-2 font-bold text-xs">
                  <span className="w-2.5 h-2.5 rounded-full bg-red-500" />
                  BLOCKED (Khóa tập luyện)
                </div>
                <p className="text-[11px] text-[var(--color-text-muted)] mt-1">
                  Phát hiện vấn đề sức khỏe, tạm ngưng huấn luyện và hẹn tái khám.
                </p>
              </button>
            </div>

            {trainingDecision === 'BLOCKED' && (
              <div className="rounded-[var(--radius-sm)] border border-red-200 bg-red-50/60 p-4 space-y-3 dark:border-red-900/50 dark:bg-red-950/20">
                <div>
                  <label className="block text-xs font-semibold text-red-900 dark:text-red-300 mb-1">
                    Lý do khóa tập luyện <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    value={restrictionDetails}
                    onChange={(e) => setRestrictionDetails(e.target.value)}
                    placeholder="Ví dụ: Nhịp tim cao, nghi mỏi cơ quá độ; cần nghỉ dưỡng 3 ngày..."
                    className="w-full rounded-[var(--radius-sm)] border border-red-300 px-3 py-1.5 text-xs bg-white dark:bg-neutral-900 text-[var(--color-text-primary)] focus:outline-none focus:ring-1 focus:ring-red-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-red-900 dark:text-red-300 mb-1">
                    Ngày hẹn tái khám (nextSchedule) <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="date"
                    value={nextScheduleDate}
                    onChange={(e) => setNextScheduleDate(e.target.value)}
                    min={new Date().toISOString().split('T')[0]}
                    className="rounded-[var(--radius-sm)] border border-red-300 px-3 py-1.5 text-xs bg-white dark:bg-neutral-900 text-[var(--color-text-primary)] focus:outline-none focus:ring-1 focus:ring-red-500"
                  />
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between border-t border-[var(--color-border)] px-6 py-3 bg-[var(--color-surface-subtle)]">
          <Button variant="tertiary" onClick={onClose} disabled={submitting}>
            Hủy bỏ
          </Button>

          <div className="flex items-center gap-2">
            {hasNext && (
              <Button
                variant="secondary"
                icon="chevron-right"
                onClick={() => handleSubmit(true)}
                loading={submitting}
              >
                Lưu & Khám tiếp
              </Button>
            )}

            <Button
              variant="primary"
              icon="check"
              onClick={() => handleSubmit(false)}
              loading={submitting}
            >
              Hoàn tất ca khám
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
