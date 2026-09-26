'use client';

import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { Icon } from '@/components/ui/Icon';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { incidentApi } from '../services/incidentService';
import { stableApi } from '@/features/stable/services/stableService';
import type { IncidentReport, IncidentSeverity, IncidentStatus } from '../types';
import type { Horse } from '@/features/stable/types';

const STATUS_MAP: Record<IncidentStatus, { tone: 'warning' | 'info' | 'success'; label: string }> = {
  REPORTED: { tone: 'warning', label: 'Đã gửi, chờ Thú y' },
  ACKNOWLEDGED: { tone: 'info', label: 'Thú y đã tiếp nhận' },
  RESOLVED: { tone: 'success', label: 'Đã xử lý xong' },
};

const SEVERITY_MAP: Record<IncidentSeverity, { tone: 'neutral' | 'info' | 'warning' | 'danger'; label: string }> = {
  LOW: { tone: 'neutral', label: 'Nhẹ' },
  MEDIUM: { tone: 'info', label: 'Trung bình' },
  HIGH: { tone: 'warning', label: 'Nặng' },
  CRITICAL: { tone: 'danger', label: 'Nguy kịch' },
};

export function IncidentList() {
  const [reports, setReports] = useState<IncidentReport[]>([]);
  const [horses, setHorses] = useState<Horse[]>([]);
  const [statusFilter, setStatusFilter] = useState<'ALL' | IncidentStatus>('ALL');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [incidentList, horseList] = await Promise.all([
        incidentApi.list(statusFilter === 'ALL' ? undefined : statusFilter),
        stableApi.getHorses().catch(() => [] as Horse[]),
      ]);
      setReports(incidentList);
      setHorses(horseList);
    } catch (err) {
      console.error('Lỗi khi nạp danh sách sự cố:', err);
      setError('Không tải được danh sách sự cố. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadData();
  }, [loadData]);

  const horseMap = new Map<number, Horse>();
  for (const h of horses) {
    horseMap.set(h.id, h);
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-[18px] font-semibold text-[var(--color-text-primary)]">
            Báo cáo sự cố đột xuất
          </h1>
          <p className="text-[12px] text-[var(--color-text-secondary)]">
            Theo dõi tình trạng sức khỏe bất thường của chiến mã và phản hồi xử lý từ Bác sĩ Thú y.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button variant="secondary" size="sm" onClick={loadData} disabled={loading}>
            <Icon name="refresh" size={14} /> Làm mới
          </Button>
          <Link href="/groom/incidents/new">
            <Button variant="primary" size="sm">
              <Icon name="plus" size={14} /> Báo cáo sự cố mới
            </Button>
          </Link>
        </div>
      </div>

      {/* Bộ lọc trạng thái */}
      <div className="flex flex-wrap items-center gap-2 border-b border-[var(--color-border)] pb-3">
        {(
          [
            { id: 'ALL', label: 'Tất cả' },
            { id: 'REPORTED', label: 'Chờ Thú y' },
            { id: 'ACKNOWLEDGED', label: 'Đã tiếp nhận' },
            { id: 'RESOLVED', label: 'Đã xử lý xong' },
          ] as const
        ).map((tab) => (
          <button
            key={tab.id}
            type="button"
            onClick={() => setStatusFilter(tab.id)}
            className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${
              statusFilter === tab.id
                ? 'bg-[var(--color-primary)] text-white'
                : 'bg-[var(--color-bg-secondary)] text-[var(--color-text-secondary)] hover:bg-[var(--color-border)] hover:text-[var(--color-text-primary)]'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {loading ? (
        <ListSkeleton rows={4} />
      ) : error ? (
        <Panel padded>
          <EmptyState icon="alert-triangle" title="Lỗi nạp dữ liệu" description={error} />
        </Panel>
      ) : reports.length === 0 ? (
        <Panel padded>
          <EmptyState
            icon="shield"
            title="Chưa có báo cáo sự cố nào"
            description={
              statusFilter === 'ALL'
                ? 'Hiện tại chưa ghi nhận sự cố chấn thương nào cho các chiến mã.'
                : 'Không có báo cáo sự cố nào phù hợp với bộ lọc hiện tại.'
            }
          />
        </Panel>
      ) : (
        <div className="space-y-4">
          {reports.map((report) => {
            const horse = horseMap.get(report.horseId);
            const statusCfg = STATUS_MAP[report.status] || { tone: 'neutral', label: report.status };
            const severityCfg = SEVERITY_MAP[report.severity] || { tone: 'neutral', label: report.severity };
            const imgSrc = incidentApi.imageSrc(report);

            return (
              <Panel key={report.id} padded>
                <div className="space-y-3">
                  <div className="flex flex-wrap items-start justify-between gap-2 border-b border-[var(--color-border-subtle)] pb-2.5">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-semibold text-[var(--color-text-muted)]">
                          #{report.id}
                        </span>
                        <h2 className="text-sm font-semibold text-[var(--color-text-primary)]">
                          {report.title}
                        </h2>
                      </div>
                      <div className="mt-1 flex flex-wrap items-center gap-3 text-xs text-[var(--color-text-secondary)]">
                        <span className="font-medium text-[var(--color-text-primary)]">
                          Chiến mã: {horse ? horse.name : report.horseName || `Mã #${report.horseId}`}
                        </span>
                        <span>•</span>
                        <span>
                          Gửi lúc: {report.reportedAt ? new Date(report.reportedAt).toLocaleString('vi-VN') : '—'}
                        </span>
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      <Pill tone={severityCfg.tone} size="sm">
                        {severityCfg.label}
                      </Pill>
                      <Pill tone={statusCfg.tone} size="sm">
                        {statusCfg.label}
                      </Pill>
                    </div>
                  </div>

                  {/* Nội dung mô tả */}
                  <div className="text-xs text-[var(--color-text-secondary)] whitespace-pre-wrap leading-relaxed">
                    {report.description}
                  </div>

                  {/* Hình ảnh sự cố */}
                  {imgSrc && (
                    <div className="pt-1">
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img
                        src={imgSrc}
                        alt={`Ảnh sự cố: ${report.title}`}
                        className="max-h-56 max-w-sm rounded-[var(--radius-md)] border border-[var(--color-border)] object-cover shadow-sm"
                        loading="lazy"
                      />
                    </div>
                  )}

                  {/* Kết luận Thú y nếu có */}
                  {report.handlerNote && (
                    <div className="rounded-[var(--radius-sm)] border border-emerald-200 bg-emerald-50/50 p-3 text-xs">
                      <div className="flex items-center gap-1.5 font-medium text-emerald-900">
                        <Icon name="stethoscope" size={14} />
                        <span>
                          Kết luận & hướng dẫn của Thú y{' '}
                          {report.handledAt ? `(${new Date(report.handledAt).toLocaleString('vi-VN')})` : ''}:
                        </span>
                      </div>
                      <p className="mt-1.5 text-emerald-800 whitespace-pre-wrap">
                        {report.handlerNote}
                      </p>
                    </div>
                  )}
                </div>
              </Panel>
            );
          })}
        </div>
      )}
    </div>
  );
}
