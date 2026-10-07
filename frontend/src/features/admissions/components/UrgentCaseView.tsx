'use client';

import { useEffect, useState, type FormEvent } from 'react';
import { ApiError } from '@/services/api';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { admissionsApi } from '../services/api';
import type {
  CompleteCareScheduleRequest,
  HorseHealthMetricRequest,
  TrainingDecision,
  UrgentAssignmentAlert,
} from '../types';

function formatDateTime(value: string | null) {
  if (!value) return 'Chưa xác định';
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(date);
}

export function UrgentCaseView({ scheduleId }: { scheduleId: number }) {
  const [urgentCase, setUrgentCase] = useState<UrgentAssignmentAlert | null>(null);
  const [loading, setLoading] = useState(true);
  const [starting, setStarting] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [submitError, setSubmitError] = useState('');
  const [completedData, setCompletedData] = useState<CompleteCareScheduleRequest | null>(null);

  // Form states
  const [symptoms, setSymptoms] = useState('');
  const [findings, setFindings] = useState('');
  const [diagnosis, setDiagnosis] = useState('');
  const [treatment, setTreatment] = useState('');
  const [trainingDecision, setTrainingDecision] = useState<TrainingDecision>('BLOCKED');
  const [restrictionDetails, setRestrictionDetails] = useState('');
  const [notes, setNotes] = useState('');
  const [temperature, setTemperature] = useState('');
  const [heartRate, setHeartRate] = useState('');
  const [respiratoryRate, setRespiratoryRate] = useState('');
  const [weight, setWeight] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    let active = true;
    admissionsApi
      .getUrgentCase(scheduleId)
      .then((data) => {
        if (!active) return;
        setUrgentCase(data);
        if (data.description) {
          setSymptoms((prev) => prev || data.description);
        }
      })
      .catch((cause: unknown) => {
        if (!active) return;
        setError(cause instanceof ApiError ? cause.message : 'Không thể tải ca khẩn cấp.');
      })
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [scheduleId]);

  async function startExam() {
    if (!urgentCase || urgentCase.status !== 'SCHEDULED') return;
    setStarting(true);
    setError('');
    try {
      const schedule = await admissionsApi.startCareSchedule(urgentCase.scheduleId);
      setUrgentCase((current) => (current ? { ...current, status: schedule.status } : current));
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Không thể bắt đầu khám.');
    } finally {
      setStarting(false);
    }
  }

  function validateForm(): boolean {
    const errors: Record<string, string> = {};

    if (!symptoms.trim()) {
      errors.symptoms = 'Triệu chứng (symptoms) là bắt buộc đối với ca khám khẩn cấp.';
    }
    if (!findings.trim()) {
      errors.findings = 'Kết quả khám thực thể (findings) là bắt buộc.';
    }
    if (!diagnosis.trim()) {
      errors.diagnosis = 'Chẩn đoán lâm sàng (diagnosis) là bắt buộc.';
    }
    if (trainingDecision === 'BLOCKED') {
      if (!restrictionDetails.trim()) {
        errors.restrictionDetails = 'Lý do khóa tập luyện là bắt buộc khi quyết định BLOCKED.';
      }
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!urgentCase) return;

    if (!validateForm()) {
      setSubmitError('Vui lòng kiểm tra lại các trường thông tin bắt buộc.');
      return;
    }

    setSubmitting(true);
    setSubmitError('');

    const metrics: HorseHealthMetricRequest = {};
    if (temperature.trim()) metrics.temperature = Number(temperature);
    if (heartRate.trim()) metrics.heartRate = Number(heartRate);
    if (respiratoryRate.trim()) metrics.respiratoryRate = Number(respiratoryRate);
    if (weight.trim()) metrics.weight = Number(weight);

    const payload: CompleteCareScheduleRequest = {
      symptoms: symptoms.trim(),
      findings: findings.trim(),
      diagnosis: diagnosis.trim(),
      treatment: treatment.trim() || undefined,
      trainingDecision,
      restrictionDetails: restrictionDetails.trim() || undefined,
      notes: notes.trim() || undefined,
      metrics: Object.keys(metrics).length > 0 ? [metrics] : undefined,
    };

    try {
      await admissionsApi.completeCareSchedule(urgentCase.scheduleId, payload);
      setCompletedData(payload);
      setUrgentCase((current) => (current ? { ...current, status: 'COMPLETED' } : current));
    } catch (cause) {
      setSubmitError(
        cause instanceof ApiError
          ? cause.message
          : 'Có lỗi xảy ra khi hoàn tất ca khám. Dữ liệu đã nhập được giữ nguyên, vui lòng thử lại.',
      );
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return <Panel className="p-8 text-sm text-[var(--color-text-secondary)]">Đang tải ca khẩn cấp…</Panel>;
  }
  if (!urgentCase) {
    return (
      <Panel className="border-[var(--color-danger)] p-8 text-sm text-[var(--color-danger)]">
        {error || 'Không tìm thấy ca khẩn cấp.'}
      </Panel>
    );
  }

  return (
    <div className="mx-auto w-full max-w-4xl space-y-5">
      <Panel className="overflow-hidden border-2 border-[var(--color-danger)] p-0">
        <header className="flex flex-wrap items-start gap-4 border-b border-[var(--color-danger)]/30 bg-[var(--color-danger-soft)] p-6">
          <span className="flex h-12 w-12 items-center justify-center rounded-full bg-[var(--color-danger)] text-white">
            <Icon name="alert-triangle" size={24} />
          </span>
          <div className="min-w-0 flex-1">
            <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-[var(--color-danger)]">
              Ca khẩn cấp #{urgentCase.scheduleId}
            </p>
            <h1 className="mt-1 text-2xl font-bold text-[var(--color-text-primary)]">{urgentCase.title}</h1>
            <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
              Bạn đã được hệ thống phân công trực tiếp cho ca này.
            </p>
          </div>
          <div className="flex gap-2">
            <Pill tone="danger">{urgentCase.severity}</Pill>
            <Pill tone={urgentCase.status === 'COMPLETED' ? 'success' : 'info'}>{urgentCase.status}</Pill>
          </div>
        </header>

        <div className="space-y-5 p-6">
          <dl className="grid gap-4 rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] p-4 text-sm sm:grid-cols-2">
            <div>
              <dt className="text-[10px] font-semibold uppercase text-[var(--color-text-muted)]">Horse</dt>
              <dd className="mt-1 font-bold">
                {urgentCase.horseName} · #{urgentCase.horseId}
              </dd>
            </div>
            <div>
              <dt className="text-[10px] font-semibold uppercase text-[var(--color-text-muted)]">Chuồng / vị trí</dt>
              <dd className="mt-1 font-semibold">
                {[urgentCase.stallCode, urgentCase.stableLocation].filter(Boolean).join(' · ') || 'Chưa cập nhật'}
              </dd>
            </div>
            <div>
              <dt className="text-[10px] font-semibold uppercase text-[var(--color-text-muted)]">Người báo</dt>
              <dd className="mt-1 font-semibold">
                {urgentCase.reportedByName || 'Không rõ tên'} · #{urgentCase.reportedById}
              </dd>
            </div>
            <div>
              <dt className="text-[10px] font-semibold uppercase text-[var(--color-text-muted)]">Thời điểm báo</dt>
              <dd className="mt-1 font-semibold">{formatDateTime(urgentCase.reportedAt)}</dd>
            </div>
            <div>
              <dt className="text-[10px] font-semibold uppercase text-[var(--color-text-muted)]">
                Thời điểm phân công
              </dt>
              <dd className="mt-1 font-semibold">{formatDateTime(urgentCase.assignedAt)}</dd>
            </div>
            <div>
              <dt className="text-[10px] font-semibold uppercase text-[var(--color-text-muted)]">Training status</dt>
              <dd className="mt-1 font-bold text-[var(--color-danger)]">
                {urgentCase.trainingStatus} — Không được training
              </dd>
            </div>
          </dl>

          <section className="rounded-[var(--radius-md)] border border-[var(--color-border)] p-4">
            <h2 className="text-sm font-bold text-[var(--color-text-primary)]">Triệu chứng / mô tả ban đầu</h2>
            <p className="mt-2 whitespace-pre-wrap break-words text-sm leading-6 text-[var(--color-text-secondary)]">
              {urgentCase.description}
            </p>
          </section>

          {urgentCase.imageUrl && (
            <a
              href={admissionsApi.assetUrl(urgentCase.imageUrl)}
              target="_blank"
              rel="noreferrer"
              className="block overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)]"
            >
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={admissionsApi.assetUrl(urgentCase.imageUrl)}
                alt="Ảnh báo cáo ca khẩn cấp"
                className="max-h-[28rem] w-full object-contain"
              />
            </a>
          )}

          {error && (
            <p role="alert" className="text-sm font-medium text-[var(--color-danger)]">
              {error}
            </p>
          )}

          {/* Workflow Transitions */}
          {urgentCase.status === 'SCHEDULED' && (
            <div className="pt-2">
              <Button variant="destructive" icon="activity" disabled={starting} onClick={startExam}>
                {starting ? 'Đang bắt đầu…' : 'Bắt đầu khám'}
              </Button>
            </div>
          )}

          {urgentCase.status === 'IN_PROGRESS' && (
            <form onSubmit={handleSubmit} className="space-y-5 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-6">
              <div className="border-b border-[var(--color-border)] pb-3">
                <h3 className="text-lg font-bold text-[var(--color-text-primary)]">
                  Phiếu hoàn tất khám khẩn cấp
                </h3>
                <p className="text-xs text-[var(--color-text-secondary)]">
                  Nhập thông tin đánh giá lâm sàng và quyết định huấn luyện y tế cho ca khẩn cấp.
                </p>
              </div>

              {submitError && (
                <div role="alert" className="rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] p-3 text-sm text-[var(--color-danger)]">
                  {submitError}
                </div>
              )}

              {/* Vitals Telemetry */}
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-[var(--color-text-secondary)] mb-2">
                  1. Sinh hiệu lâm sàng (Vitals)
                </label>
                <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                  <div>
                    <label htmlFor="vital-temp" className="block text-xs font-medium text-[var(--color-text-primary)]">
                      Nhiệt độ (°C)
                    </label>
                    <input
                      id="vital-temp"
                      type="number"
                      step="0.1"
                      placeholder="38.0"
                      value={temperature}
                      onChange={(e) => setTemperature(e.target.value)}
                      className="mt-1 w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-2.5 py-1.5 text-xs outline-none focus:border-[var(--color-primary)]"
                    />
                  </div>
                  <div>
                    <label htmlFor="vital-hr" className="block text-xs font-medium text-[var(--color-text-primary)]">
                      Nhịp tim (bpm)
                    </label>
                    <input
                      id="vital-hr"
                      type="number"
                      step="1"
                      placeholder="36"
                      value={heartRate}
                      onChange={(e) => setHeartRate(e.target.value)}
                      className="mt-1 w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-2.5 py-1.5 text-xs outline-none focus:border-[var(--color-primary)]"
                    />
                  </div>
                  <div>
                    <label htmlFor="vital-rr" className="block text-xs font-medium text-[var(--color-text-primary)]">
                      Nhịp thở (rpm)
                    </label>
                    <input
                      id="vital-rr"
                      type="number"
                      step="1"
                      placeholder="12"
                      value={respiratoryRate}
                      onChange={(e) => setRespiratoryRate(e.target.value)}
                      className="mt-1 w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-2.5 py-1.5 text-xs outline-none focus:border-[var(--color-primary)]"
                    />
                  </div>
                  <div>
                    <label htmlFor="vital-wt" className="block text-xs font-medium text-[var(--color-text-primary)]">
                      Cân nặng (kg)
                    </label>
                    <input
                      id="vital-wt"
                      type="number"
                      step="0.5"
                      placeholder="500"
                      value={weight}
                      onChange={(e) => setWeight(e.target.value)}
                      className="mt-1 w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-2.5 py-1.5 text-xs outline-none focus:border-[var(--color-primary)]"
                    />
                  </div>
                </div>
              </div>

              {/* Clinical Details */}
              <div className="space-y-4">
                <label className="block text-xs font-bold uppercase tracking-wider text-[var(--color-text-secondary)]">
                  2. Khám và chẩn đoán
                </label>

                <div>
                  <label htmlFor="urgent-symptoms" className="block text-xs font-semibold text-[var(--color-text-primary)]">
                    Triệu chứng khẩn cấp quan sát được <span className="text-[var(--color-danger)]">*</span>
                  </label>
                  <textarea
                    id="urgent-symptoms"
                    rows={2}
                    value={symptoms}
                    onChange={(e) => {
                      setSymptoms(e.target.value);
                      if (fieldErrors.symptoms) setFieldErrors((prev) => ({ ...prev, symptoms: '' }));
                    }}
                    placeholder="Mô tả triệu chứng, biểu hiện bệnh/chấn thương của ngựa..."
                    className="mt-1 w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] p-2.5 text-xs outline-none focus:border-[var(--color-primary)]"
                  />
                  {fieldErrors.symptoms && (
                    <p className="mt-1 text-xs text-[var(--color-danger)]">{fieldErrors.symptoms}</p>
                  )}
                </div>

                <div>
                  <label htmlFor="urgent-findings" className="block text-xs font-semibold text-[var(--color-text-primary)]">
                    Kết quả khám thực thể (Findings) <span className="text-[var(--color-danger)]">*</span>
                  </label>
                  <textarea
                    id="urgent-findings"
                    rows={3}
                    value={findings}
                    onChange={(e) => {
                      setFindings(e.target.value);
                      if (fieldErrors.findings) setFieldErrors((prev) => ({ ...prev, findings: '' }));
                    }}
                    placeholder="Ghi nhận cụ thể tình trạng cơ thể, vết thương, vị trí đau..."
                    className="mt-1 w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] p-2.5 text-xs outline-none focus:border-[var(--color-primary)]"
                  />
                  {fieldErrors.findings && (
                    <p className="mt-1 text-xs text-[var(--color-danger)]">{fieldErrors.findings}</p>
                  )}
                </div>

                <div className="grid gap-4 sm:grid-cols-2">
                  <div>
                    <label htmlFor="urgent-diagnosis" className="block text-xs font-semibold text-[var(--color-text-primary)]">
                      Chẩn đoán (Diagnosis) <span className="text-[var(--color-danger)]">*</span>
                    </label>
                    <input
                      id="urgent-diagnosis"
                      type="text"
                      value={diagnosis}
                      onChange={(e) => {
                        setDiagnosis(e.target.value);
                        if (fieldErrors.diagnosis) setFieldErrors((prev) => ({ ...prev, diagnosis: '' }));
                      }}
                      placeholder="Chẩn đoán xác định hoặc dự kiến..."
                      className="mt-1 w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-2.5 py-1.5 text-xs outline-none focus:border-[var(--color-primary)]"
                    />
                    {fieldErrors.diagnosis && (
                      <p className="mt-1 text-xs text-[var(--color-danger)]">{fieldErrors.diagnosis}</p>
                    )}
                  </div>

                  <div>
                    <label htmlFor="urgent-treatment" className="block text-xs font-semibold text-[var(--color-text-primary)]">
                      Phác đồ điều trị / sơ cứu (Treatment)
                    </label>
                    <input
                      id="urgent-treatment"
                      type="text"
                      value={treatment}
                      onChange={(e) => setTreatment(e.target.value)}
                      placeholder="Thuốc, băng bó, chỉ định chăm sóc..."
                      className="mt-1 w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-2.5 py-1.5 text-xs outline-none focus:border-[var(--color-primary)]"
                    />
                  </div>
                </div>

                <div>
                  <label htmlFor="urgent-notes" className="block text-xs font-semibold text-[var(--color-text-primary)]">
                    Ghi chú thêm (Notes)
                  </label>
                  <textarea
                    id="urgent-notes"
                    rows={2}
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    placeholder="Lưu ý chăm sóc, theo dõi thêm..."
                    className="mt-1 w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] p-2.5 text-xs outline-none focus:border-[var(--color-primary)]"
                  />
                </div>
              </div>

              {/* Training Clearance Decision */}
              <div className="space-y-3 border-t border-[var(--color-border)] pt-4">
                <label className="block text-xs font-bold uppercase tracking-wider text-[var(--color-text-secondary)]">
                  3. Quyết định huấn luyện (Training Decision) <span className="text-[var(--color-danger)]">*</span>
                </label>
                <div className="grid gap-3 sm:grid-cols-2">
                  {[
                    { value: 'ALLOWED' as const, label: 'ALLOWED', desc: 'Cho phép tập luyện bình thường' },
                    { value: 'BLOCKED' as const, label: 'BLOCKED', desc: 'Khóa kế hoạch huấn luyện, nghỉ tại chuồng' },
                  ].map((option) => (
                    <label
                      key={option.value}
                      className={`flex cursor-pointer flex-col rounded-[var(--radius-md)] border p-3 transition-colors ${
                        trainingDecision === option.value
                          ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)]'
                          : 'border-[var(--color-border)] bg-[var(--color-surface)] hover:bg-[var(--color-surface-muted)]'
                      }`}
                    >
                      <div className="flex items-center gap-2">
                        <input
                          type="radio"
                          name="trainingDecision"
                          value={option.value}
                          checked={trainingDecision === option.value}
                          onChange={() => {
                            setTrainingDecision(option.value);
                            if (option.value === 'ALLOWED') {
                              setFieldErrors((prev) => ({ ...prev, restrictionDetails: '' }));
                            }
                          }}
                          className="accent-[var(--color-primary)]"
                        />
                        <span className="text-xs font-bold">{option.label}</span>
                      </div>
                      <p className="mt-1 text-[11px] text-[var(--color-text-secondary)]">{option.desc}</p>
                    </label>
                  ))}
                </div>

                {trainingDecision === 'BLOCKED' && (
                  <div className="mt-3">
                    <label htmlFor="restriction-details" className="block text-xs font-semibold text-[var(--color-danger)]">
                      Lý do khóa kế hoạch huấn luyện <span className="text-[var(--color-danger)]">*</span>
                    </label>
                    <textarea
                      id="restriction-details"
                      rows={2}
                      value={restrictionDetails}
                      onChange={(e) => {
                        setRestrictionDetails(e.target.value);
                        if (fieldErrors.restrictionDetails) {
                          setFieldErrors((prev) => ({ ...prev, restrictionDetails: '' }));
                        }
                      }}
                      placeholder="Mô tả cụ thể chế độ hạn chế (ví dụ: chỉ dắt đi bộ 15 phút, nghỉ chuồng tuyệt đối...)"
                      className="mt-1 w-full rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-[var(--color-surface)] p-2.5 text-xs outline-none focus:ring-1 focus:ring-[var(--color-danger)]"
                    />
                    {fieldErrors.restrictionDetails && (
                      <p className="mt-1 text-xs text-[var(--color-danger)]">{fieldErrors.restrictionDetails}</p>
                    )}
                  </div>
                )}
              </div>

              <div className="border-t border-[var(--color-border)] pt-4">
                <Button type="submit" variant="primary" loading={submitting} icon="check" className="w-full sm:w-auto">
                  Hoàn tất ca khám khẩn cấp
                </Button>
              </div>
            </form>
          )}

          {urgentCase.status === 'COMPLETED' && (
            <div className="space-y-4 rounded-[var(--radius-md)] border border-[var(--color-success)] bg-[var(--color-success-soft)] p-6">
              <div className="flex items-center gap-3 text-[var(--color-success)]">
                <Icon name="check" size={24} />
                <h3 className="text-base font-bold">Ca khám khẩn cấp đã hoàn tất thành công!</h3>
              </div>
              <p className="text-xs text-[var(--color-text-secondary)]">
                Hồ sơ bệnh án và quyết định huấn luyện đã được ghi nhận vào hệ thống.
              </p>
              {completedData && (
                <dl className="grid gap-3 rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 text-xs sm:grid-cols-2">
                  <div>
                    <dt className="font-semibold text-[var(--color-text-muted)]">Chẩn đoán</dt>
                    <dd className="mt-1 font-bold text-[var(--color-text-primary)]">{completedData.diagnosis}</dd>
                  </div>
                  <div>
                    <dt className="font-semibold text-[var(--color-text-muted)]">Quyết định huấn luyện</dt>
                    <dd className="mt-1 font-bold text-[var(--color-danger)]">{completedData.trainingDecision}</dd>
                  </div>
                  <div className="sm:col-span-2">
                    <dt className="font-semibold text-[var(--color-text-muted)]">Kết quả khám</dt>
                    <dd className="mt-1 text-[var(--color-text-secondary)]">{completedData.findings}</dd>
                  </div>
                  {completedData.restrictionDetails && (
                    <div className="sm:col-span-2">
                      <dt className="font-semibold text-[var(--color-text-muted)]">Chi tiết hạn chế</dt>
                      <dd className="mt-1 text-[var(--color-danger)]">{completedData.restrictionDetails}</dd>
                    </div>
                  )}
                </dl>
              )}
            </div>
          )}
        </div>
      </Panel>
    </div>
  );
}
