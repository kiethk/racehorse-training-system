'use client';

import { useCallback, useEffect, useState } from 'react';

import { Button } from '@/components/ui/Button';
import { FieldLabel, Panel, SectionTitle } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { DetailSkeleton, EmptyState } from '@/components/ui/states';
import { admissionsApi } from '../services/api';
import { trainerAdmissionsApi } from '../services/trainerAdmissionService';
import type {
  RacingReadinessStatus,
  TrainerAdmissionView,
  TrainerReviewRequest,
} from '../types/trainer';

/**
 * Nhãn hiển thị cho người dùng — chỉ ghi ĐIỀU HỌ CẦN BIẾT ĐỂ CHỌN,
 * không giải thích hệ thống hoạt động thế nào.
 */
const READINESS_OPTIONS: {
  value: RacingReadinessStatus;
  label: string;
}[] = [
  { value: 'READY', label: 'Sẵn sàng thi đấu' },
  { value: 'NEEDS_MORE_TRAINING', label: 'Cần huấn luyện thêm' },
  { value: 'UNSUITABLE', label: 'Không phù hợp' },
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
  /** Vừa nộp xong — hiện băng xác nhận để người dùng biết thao tác đã thành công. */
  const [submitted, setSubmitted] = useState(false);

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

  const { admission, horse, healthRecords, healthMetrics, existingAssessment } = view;
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
      setSubmitted(true);   // báo thành công cho người dùng
      await load();         // nạp lại -> existingAssessment khác null -> form khoá
      onSubmitted();        // báo danh sách bỏ đơn này khỏi hàng đợi
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Nộp đánh giá thất bại.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Panel padded>
      {submitted && (
        <div className="mb-4 flex items-start gap-2 rounded-[var(--radius-md)] bg-[var(--color-success-soft)] p-3 text-[12px] text-[var(--color-success)]">
          <span>✅</span>
          <div>
            <strong>Đã gửi đánh giá thành công.</strong>
            <div className="mt-0.5">
              Hồ sơ đã chuyển sang Quản lý câu lạc bộ xem xét.
            </div>
          </div>
        </div>
      )}

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
          Chưa thể đánh giá: hồ sơ chiến mã chưa được lập.
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
        <ul className="mt-2 space-y-1.5">
          {admission.documents.map((doc) => {
            const fileHref = doc.fileUrl
              ? admissionsApi.assetUrl(doc.fileUrl)
              : trainerAdmissionsApi.documentFileUrl(admissionId, doc.id);
            return (
              <li key={doc.id} className="text-[12px] flex items-center gap-1.5">
                <a
                  className="text-[var(--color-primary)] font-medium underline hover:opacity-80"
                  href={fileHref}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  {doc.documentType}
                </a>
                {doc.originalFileName && (
                  <span className="text-[var(--color-text-muted)]">({doc.originalFileName})</span>
                )}
                {doc.note && (
                  <span className="text-[var(--color-text-muted)]"> — {doc.note}</span>
                )}
              </li>
            );
          })}
        </ul>
      )}

      {/* ============ DỮ LIỆU THÚ Y ============ */}
      <div className="mt-5">
        <SectionTitle>Dữ liệu thú y</SectionTitle>
      </div>
      <div className="mt-2 space-y-3">
        {/* Kết luận từ Bác sĩ thú y trong quy trình duyệt đơn */}
        {admission.vetDecision ? (
          <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-3 text-[12px] space-y-1.5">
            <div className="flex items-center justify-between font-medium">
              <span className="text-[var(--color-text-primary)]">
                Đánh giá tổng quan của Thú y
                {admission.vetReviewedAt && (
                  <span className="text-[var(--color-text-muted)] font-normal">
                    {' '}· {new Date(admission.vetReviewedAt).toLocaleDateString('vi-VN')}
                  </span>
                )}
              </span>
              <span
                className={`rounded px-1.5 py-0.5 text-[11px] font-semibold ${
                  admission.vetDecision === 'APPROVED'
                    ? 'bg-emerald-100 text-emerald-800'
                    : admission.vetDecision === 'RECHECK_REQUIRED'
                    ? 'bg-amber-100 text-amber-800'
                    : 'bg-rose-100 text-rose-800'
                }`}
              >
                {admission.vetDecision === 'APPROVED'
                  ? 'ĐẠT'
                  : admission.vetDecision === 'RECHECK_REQUIRED'
                  ? 'CẦN KHÁM LẠI'
                  : 'TỪ CHỐI'}
              </span>
            </div>
            {admission.vetFeedback && (
              <Row label="Nhận xét" value={admission.vetFeedback} />
            )}
          </div>
        ) : (
          healthRecords.length === 0 && healthMetrics.length === 0 && (
            <p className="mt-1 text-[12px] text-[var(--color-text-muted)]">
              Chưa có dữ liệu khám từ bác sĩ thú y.
            </p>
          )
        )}
          {/* Chỉ số sinh hiệu gần nhất */}
          {healthMetrics.length > 0 && (
            <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-3 text-[12px]">
              <div className="font-semibold text-[var(--color-text-primary)] mb-2">
                Chỉ số sinh hiệu gần nhất ({new Date(healthMetrics[0].recordedAt).toLocaleDateString('vi-VN')})
              </div>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                <div>
                  <span className="text-[var(--color-text-muted)]">Thân nhiệt:</span>{' '}
                  <span className="font-medium">{healthMetrics[0].temperature != null ? `${healthMetrics[0].temperature} °C` : '—'}</span>
                </div>
                <div>
                  <span className="text-[var(--color-text-muted)]">Nhịp tim:</span>{' '}
                  <span className="font-medium">{healthMetrics[0].heartRate != null ? `${healthMetrics[0].heartRate} bpm` : '—'}</span>
                </div>
                <div>
                  <span className="text-[var(--color-text-muted)]">Nhịp thở:</span>{' '}
                  <span className="font-medium">{healthMetrics[0].respiratoryRate != null ? `${healthMetrics[0].respiratoryRate} bpm` : '—'}</span>
                </div>
                <div>
                  <span className="text-[var(--color-text-muted)]">Cân nặng:</span>{' '}
                  <span className="font-medium">{healthMetrics[0].weight != null ? `${healthMetrics[0].weight} kg` : '—'}</span>
                </div>
                <div>
                  <span className="text-[var(--color-text-muted)]">Bù nước:</span>{' '}
                  <span className="font-medium">{healthMetrics[0].hydrationStatus || '—'}</span>
                </div>
                <div>
                  <span className="text-[var(--color-text-muted)]">Điểm thể trạng:</span>{' '}
                  <span className="font-medium">{healthMetrics[0].bodyConditionScore != null ? `${healthMetrics[0].bodyConditionScore}/9` : '—'}</span>
                </div>
              </div>
            </div>
          )}

          {/* Bản ghi khám lâm sàng */}
          {healthRecords.length > 0 && (
            <div className="space-y-2">
              <div className="text-[12px] font-semibold text-[var(--color-text-primary)]">
                Bản ghi khám lâm sàng ({healthRecords.length})
              </div>
              {healthRecords.map((hr) => (
                <div
                  key={hr.id}
                  className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-3 text-[12px] space-y-1.5"
                >
                  <div className="flex items-center justify-between font-medium">
                    <span>
                      {hr.recordType || 'Khám nhập học'} · {new Date(hr.examinedAt).toLocaleDateString('vi-VN')}
                    </span>
                    {hr.vetDecision && (
                      <span
                        className={`rounded px-1.5 py-0.5 text-[11px] font-semibold ${
                          hr.vetDecision === 'APPROVED'
                            ? 'bg-emerald-100 text-emerald-800'
                            : hr.vetDecision === 'RECHECK_REQUIRED'
                            ? 'bg-amber-100 text-amber-800'
                            : 'bg-rose-100 text-rose-800'
                        }`}
                      >
                        {hr.vetDecision === 'APPROVED'
                          ? 'ĐẠT'
                          : hr.vetDecision === 'RECHECK_REQUIRED'
                          ? 'CẦN KHÁM LẠI'
                          : 'TỪ CHỐI'}
                      </span>
                    )}
                  </div>
                  {hr.diagnosis && <Row label="Chẩn đoán" value={hr.diagnosis} />}
                  {hr.symptoms && <Row label="Triệu chứng" value={hr.symptoms} />}
                  {hr.treatment && <Row label="Điều trị" value={hr.treatment} />}
                  {hr.notes && <Row label="Ghi chú" value={hr.notes} />}
                </div>
              ))}
            </div>
          )}
        </div>

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
                  <span className="text-[13px] text-[var(--color-text-primary)]">
                    {opt.label}
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

          {/*
            Ngựa đang cách ly nên chưa đo được thể lực. Người dùng chỉ cần biết
            "sẽ chấm sau", không cần biết lý do kỹ thuật.
          */}
          <p className="text-[11px] text-[var(--color-text-muted)]">
            Điểm thể lực sẽ được chấm ở lần đánh giá định kỳ sau khi chiến mã nhập trại.
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
              Sau khi nộp sẽ không sửa lại được.
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