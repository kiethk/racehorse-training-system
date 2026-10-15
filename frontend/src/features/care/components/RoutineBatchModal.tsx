'use client';

import React, { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { Pill } from '@/components/ui/StatusBadge';
import { careApi } from '../services/careApi';
import type { CareTaskItem, BatchCareScheduleRowResult, TrainingDecision } from '../types';

interface RoutineBatchModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  items: CareTaskItem[];
  onSuccess?: () => void;
}

interface RowState {
  scheduleId: number;
  horseId: number;
  horseName: string;
  stallCode?: string | null;
  heartRate: string;
  temperature: string;
  respiratoryRate: string;
  weight: string;
  bcs: string;
  trainingDecision: TrainingDecision;
}

export function RoutineBatchModal({
  open,
  onOpenChange,
  items,
  onSuccess,
}: RoutineBatchModalProps) {
  if (!open) return null;

  return (
    <RoutineBatchModalInner
      items={items}
      onClose={() => onOpenChange(false)}
      onSuccess={onSuccess}
    />
  );
}

function RoutineBatchModalInner({
  items,
  onClose,
  onSuccess,
}: {
  items: CareTaskItem[];
  onClose: () => void;
  onSuccess?: () => void;
}) {
  const [rows, setRows] = useState<RowState[]>(() =>
    items.map((item) => ({
      scheduleId: item.id,
      horseId: item.horseId,
      horseName: item.horseName,
      stallCode: item.stallCode,
      heartRate: '36',
      temperature: '38.0',
      respiratoryRate: '12',
      weight: '480',
      bcs: '5',
      trainingDecision: 'ALLOWED',
    }))
  );

  const [commonBlockedReason, setCommonBlockedReason] = useState('');
  const [commonNextScheduleDate, setCommonNextScheduleDate] = useState('');

  const [submitting, setSubmitting] = useState(false);
  const [results, setResults] = useState<BatchCareScheduleRowResult[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const hasAnyBlocked = rows.some((r) => r.trainingDecision === 'BLOCKED');

  const updateRow = <K extends keyof RowState>(index: number, field: K, value: RowState[K]) => {
    setRows((prev) => {
      const copy = [...prev];
      copy[index] = { ...copy[index], [field]: value };
      return copy;
    });
  };

  const handleSubmit = async () => {
    setError(null);

    if (hasAnyBlocked) {
      if (!commonBlockedReason.trim()) {
        setError('Có ngựa bị khóa tập (BLOCKED) — Bắt buộc nhập lý do khóa');
        return;
      }
      if (!commonNextScheduleDate) {
        setError('Có ngựa bị khóa tập (BLOCKED) — Bắt buộc chọn ngày hẹn tái khám');
        return;
      }
    }

    try {
      setSubmitting(true);

      const payload = {
        items: rows.map((r) => ({
          scheduleId: r.scheduleId,
          heartRate: r.heartRate ? Number(r.heartRate) : undefined,
          temperature: r.temperature ? Number(r.temperature) : undefined,
          respiratoryRate: r.respiratoryRate ? Number(r.respiratoryRate) : undefined,
          weight: r.weight ? Number(r.weight) : undefined,
          bodyConditionScore: r.bcs ? Number(r.bcs) : undefined,
          trainingDecision: r.trainingDecision,
          restrictionDetails: r.trainingDecision === 'BLOCKED' ? commonBlockedReason.trim() : undefined,
          nextScheduleDate: r.trainingDecision === 'BLOCKED' ? commonNextScheduleDate : undefined,
          notes: 'Khám định kỳ hàng loạt theo lô',
        })),
      };

      const res = await careApi.batchComplete(payload);
      setResults(res.results);

      if (res.failedCount === 0 && onSuccess) {
        onSuccess();
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Lỗi khi gửi kết quả khám hàng loạt');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
      <div className="relative w-full max-w-4xl rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-[var(--color-border)] px-6 py-4 bg-[var(--color-surface-subtle)]">
          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-base text-[var(--color-text-primary)]">
                Khám định kỳ hàng loạt (Batch Routine Care)
              </span>
              <Pill tone="info">{rows.length} chiến mã</Pill>
            </div>
            <p className="text-xs text-[var(--color-text-muted)] mt-0.5">
              Nhập nhanh sinh hiệu theo dạng bảng phím Tab để tối ưu thời gian thao tác.
            </p>
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
        <div className="overflow-y-auto p-6 space-y-4">
          {error && (
            <div className="rounded-[var(--radius-sm)] border border-red-200 bg-red-50 p-3 text-xs text-red-700 dark:border-red-800 dark:bg-red-950/50 dark:text-red-300">
              {error}
            </div>
          )}

          {results ? (
            <div className="space-y-4">
              <div className="rounded-[var(--radius-md)] border border-emerald-200 bg-emerald-50/50 p-4 dark:border-emerald-900/50 dark:bg-emerald-950/20">
                <h4 className="font-bold text-sm text-emerald-800 dark:text-emerald-300">
                  Kết quả lưu hàng loạt
                </h4>
                <p className="text-xs text-emerald-700 dark:text-emerald-400 mt-1">
                  Đã hoàn tất {results.filter((r) => r.status === 'SUCCESS').length} / {results.length} ca khám thành công.
                </p>
              </div>

              <div className="divide-y divide-[var(--color-border)] rounded-[var(--radius-sm)] border border-[var(--color-border)]">
                {results.map((res) => {
                  const item = items.find((i) => i.id === res.scheduleId);
                  return (
                    <div key={res.scheduleId} className="flex items-center justify-between p-3 text-xs">
                      <span className="font-semibold text-[var(--color-text-primary)]">
                        {item?.horseName || `#${res.scheduleId}`}
                      </span>
                      {res.status === 'SUCCESS' ? (
                        <span className="text-emerald-600 font-bold flex items-center gap-1">
                          <Icon name="check" size={14} /> Thành công
                        </span>
                      ) : (
                        <span className="text-red-600 font-bold flex items-center gap-1">
                          <Icon name="alert-triangle" size={14} /> {res.errorMessage || 'Thất bại'}
                        </span>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          ) : (
            <>
              <div className="overflow-x-auto rounded-[var(--radius-sm)] border border-[var(--color-border)]">
                <table className="w-full text-left border-collapse text-xs">
                  <thead>
                    <tr className="bg-[var(--color-surface-subtle)] border-b border-[var(--color-border)] text-[var(--color-text-secondary)] font-semibold">
                      <th className="p-3">Chiến mã & Chuồng</th>
                      <th className="p-3 w-24">HR (bpm)</th>
                      <th className="p-3 w-24">Temp (°C)</th>
                      <th className="p-3 w-24">RR (rpm)</th>
                      <th className="p-3 w-24">Weight (kg)</th>
                      <th className="p-3 w-20">BCS (1-9)</th>
                      <th className="p-3 w-36">Huấn luyện</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[var(--color-border)] bg-[var(--color-surface)]">
                    {rows.map((row, idx) => (
                      <tr key={row.scheduleId} className="hover:bg-[var(--color-surface-subtle)]/50">
                        <td className="p-3 font-semibold text-[var(--color-text-primary)]">
                          {row.horseName}
                          <span className="block text-[11px] font-normal text-[var(--color-text-muted)]">
                            {row.stallCode || `#${row.horseId}`}
                          </span>
                        </td>
                        <td className="p-2">
                          <input
                            type="number"
                            value={row.heartRate}
                            onChange={(e) => updateRow(idx, 'heartRate', e.target.value)}
                            className="w-full rounded border border-[var(--color-border)] px-2 py-1 bg-[var(--color-surface)] text-[var(--color-text-primary)] focus:outline-none focus:border-[var(--color-primary)]"
                          />
                        </td>
                        <td className="p-2">
                          <input
                            type="number"
                            step="0.1"
                            value={row.temperature}
                            onChange={(e) => updateRow(idx, 'temperature', e.target.value)}
                            className="w-full rounded border border-[var(--color-border)] px-2 py-1 bg-[var(--color-surface)] text-[var(--color-text-primary)] focus:outline-none focus:border-[var(--color-primary)]"
                          />
                        </td>
                        <td className="p-2">
                          <input
                            type="number"
                            value={row.respiratoryRate}
                            onChange={(e) => updateRow(idx, 'respiratoryRate', e.target.value)}
                            className="w-full rounded border border-[var(--color-border)] px-2 py-1 bg-[var(--color-surface)] text-[var(--color-text-primary)] focus:outline-none focus:border-[var(--color-primary)]"
                          />
                        </td>
                        <td className="p-2">
                          <input
                            type="number"
                            value={row.weight}
                            onChange={(e) => updateRow(idx, 'weight', e.target.value)}
                            className="w-full rounded border border-[var(--color-border)] px-2 py-1 bg-[var(--color-surface)] text-[var(--color-text-primary)] focus:outline-none focus:border-[var(--color-primary)]"
                          />
                        </td>
                        <td className="p-2">
                          <input
                            type="number"
                            value={row.bcs}
                            onChange={(e) => updateRow(idx, 'bcs', e.target.value)}
                            className="w-full rounded border border-[var(--color-border)] px-2 py-1 bg-[var(--color-surface)] text-[var(--color-text-primary)] focus:outline-none focus:border-[var(--color-primary)]"
                          />
                        </td>
                        <td className="p-2">
                          <select
                            value={row.trainingDecision}
                            onChange={(e) => updateRow(idx, 'trainingDecision', e.target.value as TrainingDecision)}
                            className={`w-full rounded border px-2 py-1 text-xs font-semibold focus:outline-none ${
                              row.trainingDecision === 'ALLOWED'
                                ? 'border-emerald-300 text-emerald-700 bg-emerald-50 dark:bg-emerald-950/40 dark:text-emerald-300'
                                : 'border-red-300 text-red-700 bg-red-50 dark:bg-red-950/40 dark:text-red-300'
                            }`}
                          >
                            <option value="ALLOWED">ALLOWED (Cho tập)</option>
                            <option value="BLOCKED">BLOCKED (Khóa tập)</option>
                          </select>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {hasAnyBlocked && (
                <div className="rounded-[var(--radius-sm)] border border-red-200 bg-red-50/70 p-4 space-y-3 dark:border-red-900/50 dark:bg-red-950/20">
                  <h4 className="font-bold text-xs text-red-900 dark:text-red-300">
                    Thông tin bổ sung cho các chiến mã bị Khóa tập (BLOCKED)
                  </h4>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div>
                      <label className="block text-xs font-medium text-red-900 dark:text-red-300 mb-1">
                        Lý do khóa tập chung <span className="text-red-500">*</span>
                      </label>
                      <input
                        type="text"
                        value={commonBlockedReason}
                        onChange={(e) => setCommonBlockedReason(e.target.value)}
                        placeholder="Ví dụ: Dấu hiệu mệt mỏi, cần theo dõi thêm..."
                        className="w-full rounded border border-red-300 px-3 py-1.5 text-xs bg-white dark:bg-neutral-900 text-[var(--color-text-primary)] focus:outline-none"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-red-900 dark:text-red-300 mb-1">
                        Ngày hẹn tái khám (nextSchedule) <span className="text-red-500">*</span>
                      </label>
                      <input
                        type="date"
                        value={commonNextScheduleDate}
                        onChange={(e) => setCommonNextScheduleDate(e.target.value)}
                        min={new Date().toISOString().split('T')[0]}
                        className="w-full rounded border border-red-300 px-3 py-1.5 text-xs bg-white dark:bg-neutral-900 text-[var(--color-text-primary)] focus:outline-none"
                      />
                    </div>
                  </div>
                </div>
              )}
            </>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between border-t border-[var(--color-border)] px-6 py-3 bg-[var(--color-surface-subtle)]">
          <Button variant="tertiary" onClick={onClose} disabled={submitting}>
            {results ? 'Đóng' : 'Hủy bỏ'}
          </Button>

          {!results && (
            <Button
              variant="primary"
              icon="check"
              onClick={handleSubmit}
              loading={submitting}
            >
              Lưu toàn bộ ({rows.length} ca)
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}
