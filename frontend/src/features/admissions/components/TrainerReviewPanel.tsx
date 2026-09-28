'use client';

import { useCallback, useEffect, useState } from 'react';

import { Button } from '@/components/ui/Button';
import { FieldLabel, Panel, SectionTitle } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { DetailSkeleton, EmptyState } from '@/components/ui/states';
import { trainerAdmissionsApi } from '../services/trainerAdmissionService';
import type {
  RacingReadinessStatus,
  TrainerAdmissionView,
  TrainerReviewRequest,
} from '../types/trainer';

const READINESS_OPTIONS: {
  value: RacingReadinessStatus;
  label: string;
  hint: string;
}[] = [
  {
    value: 'READY',
    label: 'Sẵn sàng thi đấu',
    hint: 'Hiếm gặp với ngựa mới nhập — chỉ chọn khi thật sự đã có nền tảng',
  },
  {
    value: 'NEEDS_MORE_TRAINING',
    label: 'Cần huấn luyện thêm',
    hint: 'Lựa chọn thường gặp nhất với ngựa mới',
  },
  {
    value: 'UNSUITABLE',
    label: 'Không phù hợp',
    hint: 'Kênh duy nhất để báo Quản lý rằng không nên nhận con này',
  },
];

const MIN_REMARKS = 20;

export function TrainerReviewPanel({
  admissionId,
  onSubmitted,
}: {
  admissionId: number;
  onSubmitted: () => void;
}) {
  const [view, setView] = useState<TrainerAdmissionView | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const [form, setForm] = useState<TrainerReviewRequest>({
    readinessStatus: 'NEEDS_MORE_TRAINING',
    conformationScore: null,
    temperamentScore: null,
    gaitQualityScore: null,
    estimatedMonthsToRace: null,
    remarks: '',
  });

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setLoadError(null);
      setFormError(null);
      setView(await trainerAdmissionsApi.getView(admissionId));
    } catch (err) {
      console.error('Không tải được hồ sơ:', err);
      setLoadError('Không tải được hồ sơ ứng viên.');
    } finally {
      setLoading(false);
    }
  }, [admissionId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  if (loading) {
    return (
      <Panel padded>
        <DetailSkeleton />
      </Panel>
    );
  }

  if (loadError || !view) {
    return (
      <Panel padded>
        <EmptyState
          icon="alert-triangle"
          title="Lỗi"
          description={loadError ?? 'Không có dữ liệu.'}
        />
      </Panel>
    );
  }

  const { admission, horse, healthRecords, existingAssessment } = view;
  const readOnly = existingAssessment !== null;
  const horseMissing = horse === null;

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
      await trainerAdmissionsApi.submitReview(admissionId, { ...form, remarks });
      await load();    // nạp lại -> existingAssessment khác null -> form khoá
      onSubmitted();   // báo danh sách bỏ đơn này khỏi hàng đợi
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Nộp đánh giá thất bại.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Panel padded>
      {/* ============ HỒ SƠ ỨNG VIÊN ============ */}
      <SectionTitle>Hồ sơ ứng viên</SectionTitle>
      <div className="mt-2 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        <Field label="Tên" value={admission.candidate?.name} />
        <Field label="Giống" value={admission.candidate?.breed} />
        <Field label="Ngày sinh" value={admission.candidate?.dateOfBirth} />
        <Field label="Số đăng ký (UELN)" value={admission.candidate?.registrationNumber} />
        <Field label="Cha (sire)" value={admission.candidate?.sireName} />
        <Field label="Mẹ (dam)" value={admission.candidate?.damName} />
      </div>

      <div className="mt-3 flex flex-wrap items-center gap-2">
        <Pill tone="isolated" icon="shield">
          Khu cách ly — chuồng {admission.quarantineStallCode ?? 'chưa xếp'}
        </Pill>
        {horse && (
          <Pill tone="info" icon="horse">
            Hồ sơ ngựa #{horse.id} · {horse.currentStatus}
          </Pill>
        )}
      </div>

      {horseMissing && (
        <p className="mt-3 rounded-[var(--radius-md)] bg-[var(--color-warning-soft)] p-2 text-[12px] text-[var(--color-warning)]">
          Đơn này chưa gắn hồ sơ chiến mã. Bước Groom phải tạo hồ sơ và xếp chuồng
          cách ly trước khi Huấn luyện viên đánh giá được.
        </p>
      )}

      {/* ============ GIẤY TỜ ============ */}
      <div className="mt-5">
        <SectionTitle>Giấy tờ kèm theo</SectionTitle>
      </div>
      {admission.documents.length === 0 ? (
        <p className="mt-1 text-[12px] text-[var(--color-text-muted)]">
          Không có giấy tờ nào.
        </p>
      ) : (
        <ul className="mt-2 space-y-1">
          {admission.documents.map((doc) => (
            <li key={doc.id} className="text-[12px]">
              <a
                className="text-[var(--color-primary)] underline"
                href={trainerAdmissionsApi.documentFileUrl(admissionId, doc.id)}
                target="_blank"
                rel="noopener noreferrer"
              >
                {doc.documentType}
              </a>
              {doc.note && (
                <span className="text-[var(--color-text-muted)]"> — {doc.note}</span>
              )}
            </li>
          ))}
        </ul>
      )}

      {/* ============ DỮ LIỆU THÚ Y ============ */}
      <div className="mt-5">
        <SectionTitle>Dữ liệu thú y</SectionTitle>
      </div>
      {healthRecords.length === 0 ? (
        <p className="mt-1 text-[12px] text-[var(--color-text-muted)]">
          Chưa có dữ liệu khám. Module Thú y đang được xây dựng.
        </p>
      ) : (
        <p className="mt-1 text-[12px]">{healthRecords.length} bản ghi khám</p>
      )}

      {/* ============ ĐÁNH GIÁ ============ */}
      <div className="mt-5">
        <SectionTitle>
          {readOnly ? 'Đánh giá đã nộp' : 'Đánh giá của Huấn luyện viên'}
        </SectionTitle>
      </div>

      {readOnly && existingAssessment && (
        <div className="mt-2 space-y-1 rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] p-3 text-[12px]">
          <Row label="Mức sẵn sàng" value={existingAssessment.readinessStatus} />
          <Row label="Dáng vóc" value={fmtScore(existingAssessment.conformationScore)} />
          <Row label="Tính nết" value={fmtScore(existingAssessment.temperamentScore)} />
          <Row label="Bước đi" value={fmtScore(existingAssessment.gaitQualityScore)} />
          <Row
            label="Ước tính"
            value={
              existingAssessment.estimatedMonthsToRace != null
                ? `${existingAssessment.estimatedMonthsToRace} tháng`
                : '—'
            }
          />
          <Row label="Ngày đánh giá" value={existingAssessment.assessmentDate} />
          <div className="mt-2 whitespace-pre-wrap text-[var(--color-text-secondary)]">
            {existingAssessment.remarks}
          </div>
        </div>
      )}

      {!readOnly && (
        <div className="mt-3 space-y-4">
          {/* --- Mức sẵn sàng --- */}
          <div>
            <FieldLabel>Mức độ sẵn sàng thi đấu</FieldLabel>
            <div className="mt-1.5 space-y-1.5">
              {READINESS_OPTIONS.map((opt) => (
                <label key={opt.value} className="flex items-start gap-2">
                  <input
                    type="radio"
                    name="readiness"
                    className="mt-1"
                    checked={form.readinessStatus === opt.value}
                    onChange={() =>
                      setForm((f) => ({ ...f, readinessStatus: opt.value }))
                    }
                  />
                  <span>
                    <span className="text-[13px] text-[var(--color-text-primary)]">
                      {opt.label}
                    </span>
                    <span className="block text-[11px] text-[var(--color-text-muted)]">
                      {opt.hint}
                    </span>
                  </span>
                </label>
              ))}
            </div>
          </div>

          {/* --- Ba điểm quan sát --- */}
          <div className="grid gap-3 sm:grid-cols-3">
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
              className="mt-1 w-32 rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-2 py-1 text-[13px]"
              value={form.estimatedMonthsToRace ?? ''}
              onChange={(e) => setNum('estimatedMonthsToRace')(e.target.value)}
            />
          </div>

          {/* --- Nhận xét --- */}
          <div>
            <FieldLabel>Nhận xét chuyên môn</FieldLabel>
            <textarea
              rows={5}
              placeholder="Dáng vóc cân đối, vai dốc tốt. Tính nết bình tĩnh khi dắt tay. Bước đi đều nhưng chân sau hơi ngắn sải..."
              className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-2 py-1.5 text-[13px]"
              value={form.remarks ?? ''}
              onChange={(e) => setForm((f) => ({ ...f, remarks: e.target.value }))}
            />
            <div className="mt-1 text-[11px] text-[var(--color-text-muted)]">
              {form.remarks?.trim().length ?? 0}/{MIN_REMARKS} ký tự tối thiểu
            </div>
          </div>

          {/* --- Giải thích vì sao không có ô thể lực --- */}
          <p className="rounded-[var(--radius-md)] bg-[var(--color-warning-soft)] p-2 text-[11px] text-[var(--color-warning)]">
            Điểm thể lực để trống — ngựa đang cách ly nên không đưa ra đường chạy
            chung để đo được. Chỉ số này sẽ chấm ở lần đánh giá định kỳ sau khi
            ngựa chính thức nhập trại.
          </p>

          {formError && (
            <p className="rounded-[var(--radius-md)] bg-[var(--color-danger-soft)] p-2 text-[12px] text-[var(--color-danger)]">
              {formError}
            </p>
          )}

          <div className="flex items-center gap-3">
            <Button
              variant="primary"
              disabled={submitting || horseMissing}
              onClick={handleSubmit}
            >
              {submitting ? 'Đang nộp...' : 'Nộp đánh giá'}
            </Button>
            <span className="text-[11px] text-[var(--color-text-muted)]">
              Nộp xong đơn tự chuyển sang bước Quản lý duyệt. Không sửa lại được.
            </span>
          </div>
        </div>
      )}
    </Panel>
  );
}

/* ---------------- Thành phần phụ ---------------- */

function fmtScore(v: number | null): string {
  return v == null ? '—' : `${v}/10`;
}

function Field({ label, value }: { label: string; value?: string | null }) {
  return (
    <div>
      <FieldLabel>{label}</FieldLabel>
      <div className="text-[13px] text-[var(--color-text-primary)]">{value || '—'}</div>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-4">
      <span className="text-[var(--color-text-muted)]">{label}</span>
      <span className="text-[var(--color-text-primary)]">{value}</span>
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