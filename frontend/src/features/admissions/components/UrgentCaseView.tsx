'use client';

import { useEffect, useState } from 'react';
import { ApiError } from '@/services/api';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { admissionsApi } from '../services/api';
import type { UrgentAssignmentAlert } from '../types';

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
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    admissionsApi.getUrgentCase(scheduleId)
      .then((data) => active && setUrgentCase(data))
      .catch((cause: unknown) => {
        if (!active) return;
        setError(cause instanceof ApiError ? cause.message : 'Không thể tải ca khẩn cấp.');
      })
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, [scheduleId]);

  async function startExam() {
    if (!urgentCase || urgentCase.status !== 'SCHEDULED') return;
    setStarting(true);
    setError('');
    try {
      const schedule = await admissionsApi.startCareSchedule(urgentCase.scheduleId);
      setUrgentCase((current) => current ? { ...current, status: schedule.status } : current);
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Không thể bắt đầu khám.');
    } finally {
      setStarting(false);
    }
  }

  if (loading) {
    return <Panel className="p-8 text-sm text-[var(--color-text-secondary)]">Đang tải ca khẩn cấp…</Panel>;
  }
  if (!urgentCase) {
    return <Panel className="border-[var(--color-danger)] p-8 text-sm text-[var(--color-danger)]">{error || 'Không tìm thấy ca khẩn cấp.'}</Panel>;
  }

  return (
    <div className="mx-auto w-full max-w-4xl space-y-5">
      <Panel className="overflow-hidden border-2 border-[var(--color-danger)] p-0">
        <header className="flex flex-wrap items-start gap-4 border-b border-[var(--color-danger)]/30 bg-[var(--color-danger-soft)] p-6">
          <span className="flex h-12 w-12 items-center justify-center rounded-full bg-[var(--color-danger)] text-white">
            <Icon name="alert-triangle" size={24} />
          </span>
          <div className="min-w-0 flex-1">
            <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-[var(--color-danger)]">Ca khẩn cấp #{urgentCase.scheduleId}</p>
            <h1 className="mt-1 text-2xl font-bold text-[var(--color-text-primary)]">{urgentCase.title}</h1>
            <p className="mt-1 text-sm text-[var(--color-text-secondary)]">Bạn đã được hệ thống phân công trực tiếp cho ca này.</p>
          </div>
          <div className="flex gap-2"><Pill tone="danger">{urgentCase.severity}</Pill><Pill tone="info">{urgentCase.status}</Pill></div>
        </header>

        <div className="space-y-5 p-6">
          <dl className="grid gap-4 rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] p-4 text-sm sm:grid-cols-2">
            <div><dt className="text-[10px] font-semibold uppercase text-[var(--color-text-muted)]">Horse</dt><dd className="mt-1 font-bold">{urgentCase.horseName} · #{urgentCase.horseId}</dd></div>
            <div><dt className="text-[10px] font-semibold uppercase text-[var(--color-text-muted)]">Chuồng / vị trí</dt><dd className="mt-1 font-semibold">{[urgentCase.stallCode, urgentCase.stableLocation].filter(Boolean).join(' · ') || 'Chưa cập nhật'}</dd></div>
            <div><dt className="text-[10px] font-semibold uppercase text-[var(--color-text-muted)]">Người báo</dt><dd className="mt-1 font-semibold">{urgentCase.reportedByName || 'Không rõ tên'} · #{urgentCase.reportedById}</dd></div>
            <div><dt className="text-[10px] font-semibold uppercase text-[var(--color-text-muted)]">Thời điểm báo</dt><dd className="mt-1 font-semibold">{formatDateTime(urgentCase.reportedAt)}</dd></div>
            <div><dt className="text-[10px] font-semibold uppercase text-[var(--color-text-muted)]">Thời điểm phân công</dt><dd className="mt-1 font-semibold">{formatDateTime(urgentCase.assignedAt)}</dd></div>
            <div><dt className="text-[10px] font-semibold uppercase text-[var(--color-text-muted)]">Training status</dt><dd className="mt-1 font-bold text-[var(--color-danger)]">{urgentCase.trainingStatus} — Không được training</dd></div>
          </dl>

          <section className="rounded-[var(--radius-md)] border border-[var(--color-border)] p-4">
            <h2 className="text-sm font-bold text-[var(--color-text-primary)]">Triệu chứng / mô tả đầy đủ</h2>
            <p className="mt-2 whitespace-pre-wrap break-words text-sm leading-6 text-[var(--color-text-secondary)]">{urgentCase.description}</p>
          </section>

          {urgentCase.imageUrl && (
            <a href={admissionsApi.assetUrl(urgentCase.imageUrl)} target="_blank" rel="noreferrer" className="block overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)]">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={admissionsApi.assetUrl(urgentCase.imageUrl)} alt="Ảnh báo cáo ca khẩn cấp" className="max-h-[28rem] w-full object-contain" />
            </a>
          )}

          {error && <p role="alert" className="text-sm font-medium text-[var(--color-danger)]">{error}</p>}

          {urgentCase.status === 'SCHEDULED' ? (
            <Button variant="destructive" icon="activity" disabled={starting} onClick={startExam}>
              {starting ? 'Đang bắt đầu…' : 'Bắt đầu khám'}
            </Button>
          ) : (
            <p className="rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] p-4 text-sm font-semibold text-[var(--color-text-primary)]">
              Trạng thái hiện tại: {urgentCase.status}
            </p>
          )}
        </div>
      </Panel>
    </div>
  );
}
