'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { FieldLabel, Panel, SectionTitle } from '@/components/ui/Panel';
import { trainerAdmissionsApi } from '../services/trainerAdmissionService';
import type {
  RacingReadinessStatus,
  TrainerAdmissionView,
  TrainerReviewRequest,
} from '../types/trainer';

const READINESS_OPTIONS: {
  value: RacingReadinessStatus;
  label: string;
}[] = [
  { value: 'READY', label: 'Sẵn sàng thi đấu' },
  { value: 'NEEDS_MORE_TRAINING', label: 'Cần huấn luyện thêm' },
  { value: 'UNSUITABLE', label: 'Không phù hợp' },
];

const MIN_REMARKS = 20;

function fmtScore(score: number | null | undefined): string {
  return score != null ? `${score}/10` : '—';
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-4">
      <span className="text-[var(--color-text-muted)]">{label}</span>
      <span className="font-medium text-[var(--color-text-primary)]">{value}</span>
    </div>
  );
}

function ScoreInput({
  label,
  value,
  onChange,
}: {
  label: string;
  value: number | null | undefined;
  onChange: (raw: string) => void;
}) {
  return (
    <div>
      <FieldLabel>{label} (0–10)</FieldLabel>
      <input
        type="number"
        min={0}
        max={10}
        step={0.5}
        className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-2 py-1 text-[13px]"
        value={value ?? ''}
        onChange={(e) => onChange(e.target.value)}
      />
    </div>
  );
}

interface TrainerReviewActionPanelProps {
  view: TrainerAdmissionView;
  onSuccess: () => void;
}

export function TrainerReviewActionPanel({ view, onSuccess }: TrainerReviewActionPanelProps) {
  const { admission, horse, existingAssessment } = view;
  const readOnly = existingAssessment !== null;
  const horseMissing = horse === null;
  const canReview = admission.status === 'TRAINER_REVIEW' && !readOnly;

  const [form, setForm] = useState<TrainerReviewRequest>({
    readinessStatus: 'NEEDS_MORE_TRAINING',
    conformationScore: null,
    temperamentScore: null,
    gaitQualityScore: null,
    estimatedMonthsToRace: null,
    remarks: '',
  });

  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [submittedNotice, setSubmittedNotice] = useState(false);

  const setNum =
    (key: keyof TrainerReviewRequest) =>
    (raw: string) =>
      setForm((f) => ({ ...f, [key]: raw === '' ? null : Number(raw) }));

  async function handleSubmit() {
    setFormError(null);

    const remarks = form.remarks?.trim() ?? '';
    if (remarks.length < MIN_REMARKS) {
      setFormError(
        `Nhận xét chuyên môn phải có ít nhất ${MIN_REMARKS} ký tự (hiện ${remarks.length}).`,
      );
      return;
    }

    try {
      setSubmitting(true);
      await trainerAdmissionsApi.submitReview(admission.admissionId, {
        ...form,
        remarks,
      });
      setSubmittedNotice(true);
      onSuccess();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Gửi đánh giá thất bại.';
      setFormError(msg);
    } finally {
      setSubmitting(false);
    }
  }

  // Đã đánh giá rồi -> Hiển thị kết quả đánh giá (chỉ đọc)
  if (readOnly && existingAssessment) {
    return (
      <Panel padded className="bg-[var(--color-surface)]">
        <SectionTitle>Đánh giá của Huấn luyện viên (Đã nộp)</SectionTitle>
        <div className="mt-3 space-y-2 rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] p-4 text-[12px]">
          <Row label="Mức độ sẵn sàng" value={existingAssessment.readinessStatus} />
          <Row label="Điểm dáng vóc" value={fmtScore(existingAssessment.conformationScore)} />
          <Row label="Điểm tính nết" value={fmtScore(existingAssessment.temperamentScore)} />
          <Row label="Điểm bước đi" value={fmtScore(existingAssessment.gaitQualityScore)} />
          <Row
            label="Ước tính thời gian thi đấu"
            value={
              existingAssessment.estimatedMonthsToRace != null
                ? `${existingAssessment.estimatedMonthsToRace} tháng`
                : '—'
            }
          />
          <Row label="Ngày đánh giá" value={existingAssessment.assessmentDate} />
          {existingAssessment.remarks && (
            <div className="mt-3 border-t border-[var(--color-border)] pt-2">
              <span className="block text-[var(--color-text-muted)] mb-1">Nhận xét chuyên môn:</span>
              <p className="whitespace-pre-wrap text-[var(--color-text-primary)]">
                {existingAssessment.remarks}
              </p>
            </div>
          )}
        </div>
      </Panel>
    );
  }

  // Chưa đến lượt hoặc không ở trạng thái TRAINER_REVIEW
  if (!canReview) {
    return (
      <Panel padded className="bg-[var(--color-surface)]">
        <SectionTitle>Đánh giá của Huấn luyện viên</SectionTitle>
        <p className="mt-2 text-[12px] text-[var(--color-text-muted)] italic">
          Hồ sơ hiện không ở trạng thái chờ Huấn luyện viên đánh giá (Trạng thái hiện tại: {admission.status}).
        </p>
      </Panel>
    );
  }

  // Đang ở TRAINER_REVIEW nhưng thiếu hồ sơ ngựa (Groom chưa tạo)
  if (horseMissing) {
    return (
      <Panel padded className="bg-[var(--color-surface)]">
        <SectionTitle>Đánh giá của Huấn luyện viên</SectionTitle>
        <div className="mt-2 rounded-[var(--radius-md)] bg-[var(--color-warning-soft)] p-3 text-[12px] text-[var(--color-warning)]">
          Chưa thể đánh giá: Hồ sơ chiến mã chưa được lập bởi Chăm sóc viên (Groom).
        </div>
      </Panel>
    );
  }

  return (
    <Panel padded className="bg-[var(--color-surface)]">
      <SectionTitle>Thẩm định Tiềm năng Thi đấu (Racing Readiness)</SectionTitle>

      {submittedNotice && (
        <div className="mt-3 rounded-[var(--radius-md)] bg-[var(--color-success-soft)] p-3 text-[12px] text-[var(--color-success)]">
          Đánh giá của bạn đã được gửi thành công và chuyển tiếp sang Quản lý duyệt.
        </div>
      )}

      <div className="mt-4 space-y-4">
        {/* --- Mức sẵn sàng --- */}
        <div>
          <FieldLabel>Mức độ sẵn sàng thi đấu</FieldLabel>
          <div className="mt-1.5 flex flex-wrap gap-4">
            {READINESS_OPTIONS.map((opt) => (
              <label key={opt.value} className="flex items-center gap-2 cursor-pointer">
                <input
                  type="radio"
                  name="readiness"
                  className="mt-0.5"
                  checked={form.readinessStatus === opt.value}
                  onChange={() => setForm((f) => ({ ...f, readinessStatus: opt.value }))}
                />
                <span className="text-[13px] text-[var(--color-text-primary)] font-medium">
                  {opt.label}
                </span>
              </label>
            ))}
          </div>
        </div>

        {/* --- Ba điểm quan sát --- */}
        <div className="grid gap-4 sm:grid-cols-3">
          <ScoreInput
            label="Dáng vóc"
            value={form.conformationScore}
            onChange={setNum('conformationScore')}
          />
          <ScoreInput
            label="Tính nết"
            value={form.temperamentScore}
            onChange={setNum('temperamentScore')}
          />
          <ScoreInput
            label="Bước đi"
            value={form.gaitQualityScore}
            onChange={setNum('gaitQualityScore')}
          />
        </div>

        {/* --- Ước tính thời gian --- */}
        <div>
          <FieldLabel>Ước tính số tháng nữa đủ điều kiện đăng ký giải</FieldLabel>
          <input
            type="number"
            min={0}
            max={60}
            placeholder="Số tháng"
            className="mt-1 w-40 rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px]"
            value={form.estimatedMonthsToRace ?? ''}
            onChange={(e) => setNum('estimatedMonthsToRace')(e.target.value)}
          />
        </div>

        {/* --- Nhận xét --- */}
        <div>
          <FieldLabel>Nhận xét chuyên môn (tối thiểu 20 ký tự)</FieldLabel>
          <textarea
            rows={4}
            placeholder="Dáng vóc cân đối, cơ bắp phát triển tốt. Tính nết điềm tĩnh khi tiếp xúc. Bước đi đều, sải chân dài..."
            className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px]"
            value={form.remarks ?? ''}
            onChange={(e) => setForm((f) => ({ ...f, remarks: e.target.value }))}
          />
          <div className="mt-1 text-[11px] text-[var(--color-text-muted)]">
            {form.remarks?.trim().length ?? 0}/{MIN_REMARKS} ký tự tối thiểu
          </div>
        </div>

        <p className="text-[11px] text-[var(--color-text-muted)] italic">
          * Điểm thể lực sẽ được đo lường và đánh giá định kỳ sau khi chiến mã kết thúc cách ly và nhập chuồng chính thức.
        </p>

        {formError && (
          <div className="rounded-[var(--radius-md)] bg-[var(--color-danger-soft)] p-3 text-[12px] text-[var(--color-danger)]">
            {formError}
          </div>
        )}

        <div className="flex items-center gap-3 pt-2">
          <Button
            variant="primary"
            disabled={submitting}
            onClick={handleSubmit}
          >
            {submitting ? 'Đang nộp...' : 'Nộp đánh giá'}
          </Button>
          <span className="text-[11px] text-[var(--color-text-muted)]">
            Sau khi nộp, hồ sơ sẽ được chuyển tới Quản lý câu lạc bộ.
          </span>
        </div>
      </div>
    </Panel>
  );
}
