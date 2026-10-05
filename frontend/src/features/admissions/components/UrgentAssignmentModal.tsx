'use client';

import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { Pill } from '@/components/ui/StatusBadge';
import type { UrgentAssignmentAlert } from '../types';
import { admissionsApi } from '../services/api';

interface Props {
  alert: UrgentAssignmentAlert | null;
  onOpenCase: (alert: UrgentAssignmentAlert) => void;
}

function formatDateTime(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(date);
}

export function UrgentAssignmentModal({ alert, onOpenCase }: Props) {
  if (!alert) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/65 backdrop-blur-sm" aria-hidden="true" />
      <div
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="urgent-assignment-title"
        aria-describedby="urgent-assignment-description"
        className="relative flex max-h-[92vh] w-full max-w-2xl flex-col overflow-hidden rounded-[var(--radius-lg)] border-2 border-[var(--color-danger)] bg-[var(--color-surface)] shadow-2xl shadow-black/30"
      >
        <header className="border-b border-[var(--color-danger)]/30 bg-[var(--color-danger-soft)] px-6 py-5">
          <div className="flex items-start gap-3">
            <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-[var(--color-danger)] text-white">
              <Icon name="alert-triangle" size={22} />
            </span>
            <div className="min-w-0 flex-1">
              <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-[var(--color-danger)]">
                Phân công tự động · Không cần xác nhận
              </p>
              <h2 id="urgent-assignment-title" className="mt-1 text-[19px] font-bold text-[var(--color-text-primary)]">
                CA KHẨN CẤP ĐÃ ĐƯỢC PHÂN CÔNG
              </h2>
              <p id="urgent-assignment-description" className="mt-1 text-[12px] text-[var(--color-text-secondary)]">
                Hệ thống đã phân công bạn trực tiếp cho ca này. Horse đang bị BLOCKED khỏi training.
              </p>
            </div>
            <Pill tone="danger">{alert.severity}</Pill>
          </div>
        </header>

        <div className="min-h-0 flex-1 space-y-4 overflow-y-auto p-6">
          <dl className="grid gap-4 rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] p-4 text-[12px] sm:grid-cols-2">
            <div>
              <dt className="text-[10px] font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">Horse</dt>
              <dd className="mt-1 font-bold text-[var(--color-text-primary)]">{alert.horseName} · #{alert.horseId}</dd>
            </div>
            <div>
              <dt className="text-[10px] font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">Chuồng / vị trí</dt>
              <dd className="mt-1 font-semibold text-[var(--color-text-primary)]">
                {[alert.stallCode, alert.stableLocation].filter(Boolean).join(' · ') || 'Được mô tả trong báo cáo'}
              </dd>
            </div>
            <div>
              <dt className="text-[10px] font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">Người báo</dt>
              <dd className="mt-1 font-semibold text-[var(--color-text-primary)]">
                {alert.reportedByName || 'Không rõ tên'} · #{alert.reportedById}
              </dd>
            </div>
            <div>
              <dt className="text-[10px] font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">Thời điểm báo</dt>
              <dd className="mt-1 font-semibold text-[var(--color-text-primary)]">{formatDateTime(alert.reportedAt)}</dd>
            </div>
            <div className="sm:col-span-2">
              <dt className="text-[10px] font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">Training status</dt>
              <dd className="mt-1 font-bold text-[var(--color-danger)]">{alert.trainingStatus} — Không được training</dd>
            </div>
          </dl>

          <section className="rounded-[var(--radius-md)] border border-[var(--color-border)] p-4">
            <h3 className="font-bold text-[var(--color-text-primary)]">{alert.title}</h3>
            <p className="mt-2 whitespace-pre-wrap break-words text-[13px] leading-relaxed text-[var(--color-text-secondary)]">
              {alert.description}
            </p>
          </section>

          {alert.imageUrl && (
            <a
              href={admissionsApi.assetUrl(alert.imageUrl)}
              target="_blank"
              rel="noreferrer"
              className="block overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)]"
              aria-label="Mở ảnh báo cáo kích thước lớn"
            >
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={admissionsApi.assetUrl(alert.imageUrl)} alt="Ảnh ca khẩn cấp" className="max-h-72 w-full object-contain" />
            </a>
          )}
        </div>

        <footer className="border-t border-[var(--color-border)] bg-[var(--color-surface-subtle)] px-6 py-4">
          <Button variant="destructive" icon="chevron-right" className="w-full" onClick={() => onOpenCase(alert)}>
            Mở ca khẩn cấp
          </Button>
        </footer>
      </div>
    </div>
  );
}
