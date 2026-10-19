'use client';

import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { incidentApi } from '@/features/groom/services/incidentService';
import type { IncidentSeverity } from '@/features/groom/types';

interface ReportIncidentDialogProps {
  horseId: number;
  horseName?: string;
  initialTitle?: string;
  initialDescription?: string;
  initialSeverity?: IncidentSeverity;
  open: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

const SEVERITY_OPTIONS: { value: IncidentSeverity; label: string }[] = [
  { value: 'LOW', label: 'Thấp (Low) — Cần theo dõi, chưa cần can thiệp khẩn cấp' },
  { value: 'MEDIUM', label: 'Trung bình (Medium) — Cần Bác sĩ Thú y khám trong ngày' },
  { value: 'HIGH', label: 'Cao (High) — Yêu cầu Bác sĩ Thú y kiểm tra gấp' },
  { value: 'CRITICAL', label: 'Khẩn cấp (Critical) — Nguy cấp, can thiệp y tế tức thời' },
];

export function ReportIncidentDialog({
  horseId,
  horseName,
  initialTitle = '',
  initialDescription = '',
  initialSeverity = 'HIGH',
  open,
  onClose,
  onSuccess,
}: ReportIncidentDialogProps) {
  const [title, setTitle] = useState(initialTitle);
  const [description, setDescription] = useState(initialDescription);
  const [severity, setSeverity] = useState<IncidentSeverity>(initialSeverity);
  const [file, setFile] = useState<File | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      setTitle(initialTitle);
      setDescription(initialDescription);
      setSeverity(initialSeverity);
      setFile(null);
      setError(null);
    }
  }, [open, initialTitle, initialDescription, initialSeverity]);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !submitting) {
        e.stopImmediatePropagation();
        e.preventDefault();
        onClose();
      }
    };
    window.addEventListener('keydown', onKey, true);
    return () => window.removeEventListener('keydown', onKey, true);
  }, [open, onClose, submitting]);

  if (!open) return null;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!title.trim()) {
      setError('Vui lòng nhập tiêu đề sự cố.');
      return;
    }
    if (!description.trim()) {
      setError('Vui lòng nhập mô tả chi tiết sự cố.');
      return;
    }

    setError(null);
    setSubmitting(true);
    try {
      const created = await incidentApi.create({
        horseId,
        title: title.trim(),
        description: description.trim(),
        severity,
      });

      if (file) {
        try {
          await incidentApi.uploadImage(created.id, file);
        } catch (imgErr) {
          console.error('Không thể tải ảnh đính kèm:', imgErr);
        }
      }

      onSuccess?.();
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Không thể gửi báo cáo sự cố.';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div
        className="absolute inset-0 bg-black/40 backdrop-blur-sm"
        onClick={() => !submitting && onClose()}
        aria-hidden="true"
      />
      <div
        role="dialog"
        aria-modal="true"
        className="relative w-full max-w-lg rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-6 shadow-xl"
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 className="text-base font-semibold text-[var(--color-text-primary)]">
              Báo cáo sự cố sức khỏe cho Bác sĩ Thú y
            </h2>
            <p className="mt-1 text-xs text-[var(--color-text-secondary)]">
              Gửi báo cáo sự cố cho {horseName ?? `Chiến mã #${horseId}`}. Hệ thống sẽ lập tức kích hoạt khóa an toàn (BLOCKED) và phát cảnh báo realtime trực tiếp tới Bác sĩ Thú y trực ban.
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={submitting}
            className="rounded p-1 text-[var(--color-text-muted)] hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-text-primary)]"
          >
            <Icon name="x" size={16} />
          </button>
        </div>

        <form id="report-incident-form" onSubmit={handleSubmit} className="mt-4 space-y-4">
          {error && (
            <div className="rounded-[var(--radius-sm)] border border-red-200 bg-red-50 p-3 text-xs text-red-700">
              {error}
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-[var(--color-text-primary)] mb-1">
              Tiêu đề sự cố <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={title}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => setTitle(e.target.value)}
              placeholder="VD: Nhịp tim vượt ngưỡng an toàn trong buổi tập trên sân"
              required
              className="w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-2 text-xs text-[var(--color-text-primary)] placeholder:text-[var(--color-text-muted)] focus:border-[var(--color-primary)] focus:outline-none"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-[var(--color-text-primary)] mb-1">
              Mức độ nghiêm trọng <span className="text-red-500">*</span>
            </label>
            <select
              value={severity}
              onChange={(e: React.ChangeEvent<HTMLSelectElement>) => setSeverity(e.target.value as IncidentSeverity)}
              className="w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-2 text-xs text-[var(--color-text-primary)] focus:border-[var(--color-primary)] focus:outline-none"
            >
              {SEVERITY_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-[var(--color-text-primary)] mb-1">
              Mô tả chi tiết / Triệu chứng quan sát <span className="text-red-500">*</span>
            </label>
            <textarea
              rows={4}
              value={description}
              onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setDescription(e.target.value)}
              placeholder="Mô tả triệu chứng, nhịp tim tăng vọt, dáng đi khập khiễng hoặc hành vi bất thường của ngựa khi tập luyện..."
              required
              className="w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-2 text-xs text-[var(--color-text-primary)] placeholder:text-[var(--color-text-muted)] focus:border-[var(--color-primary)] focus:outline-none"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-[var(--color-text-primary)] mb-1">
              Đính kèm hình ảnh / Bằng chứng (tùy chọn)
            </label>
            <input
              type="file"
              accept="image/png,image/jpeg,image/webp"
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                const selected = e.target.files?.[0] ?? null;
                if (selected && selected.size > 10 * 1024 * 1024) {
                  setError('Hình ảnh không được vượt quá 10 MB.');
                  return;
                }
                setFile(selected);
              }}
              className="w-full text-xs text-[var(--color-text-secondary)] file:mr-2 file:rounded file:border-0 file:bg-[var(--color-surface-muted)] file:px-2.5 file:py-1 file:text-xs file:font-medium file:text-[var(--color-text-primary)]"
            />
          </div>

          <div className="flex justify-end gap-2 pt-3 border-t border-[var(--color-border)]">
            <Button variant="secondary" onClick={onClose} disabled={submitting}>
              Hủy
            </Button>
            <Button variant="destructive" type="submit" loading={submitting}>
              Gửi báo cáo sự cố
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
