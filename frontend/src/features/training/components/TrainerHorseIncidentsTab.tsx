'use client';

import { useCallback, useEffect, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { Icon } from '@/components/ui/Icon';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { incidentApi } from '@/features/groom/services/incidentService';
import type { IncidentReport, IncidentSeverity, IncidentStatus } from '@/features/groom/types';
import { ReportIncidentDialog } from './ReportIncidentDialog';

const STATUS_MAP: Record<
  IncidentStatus,
  { tone: 'warning' | 'info' | 'success' | 'neutral'; label: string }
> = {
  REPORTED: { tone: 'warning', label: 'Đã báo cáo — Chờ BSTY xử lý' },
  IN_REVIEW: { tone: 'info', label: 'BSTY đang khám / xử lý' },
  RESOLVED: { tone: 'success', label: 'Đã xử lý (Resolved)' },
  DISMISSED: { tone: 'neutral', label: 'Không có vấn đề (Dismissed)' },
};

const SEVERITY_MAP: Record<IncidentSeverity, { tone: 'neutral' | 'info' | 'warning' | 'danger'; label: string }> = {
  LOW: { tone: 'neutral', label: 'Nhẹ' },
  MEDIUM: { tone: 'info', label: 'Trung bình' },
  HIGH: { tone: 'warning', label: 'Nghiêm trọng' },
  CRITICAL: { tone: 'danger', label: 'Khẩn cấp' },
};

export function TrainerHorseIncidentsTab({ horseId, horseName }: { horseId: number; horseName?: string }) {
  const [reports, setReports] = useState<IncidentReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isReportOpen, setIsReportOpen] = useState(false);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const list = await incidentApi.list({ horseId });
      setReports(list);
    } catch (err) {
      console.error('Không thể tải danh sách sự cố của chiến mã:', err);
      setError('Không thể tải danh sách báo cáo sự cố của chiến mã này.');
    } finally {
      setLoading(false);
    }
  }, [horseId]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 className="text-base font-semibold text-[var(--color-text-primary)]">
            Báo cáo sự cố & An toàn tập luyện
          </h3>
          <p className="text-xs text-[var(--color-text-secondary)]">
            Lịch sử các sự cố trên sân tập hoặc chuồng nuôi và tiến độ xử lý của Bác sĩ Thú y.
          </p>
        </div>
        <Button
          variant="destructive"
          size="sm"
          icon="alert-triangle"
          onClick={() => setIsReportOpen(true)}
        >
          + Báo cáo sự cố tới BSTY
        </Button>
      </div>

      {error && (
        <div className="rounded-[var(--radius-sm)] border border-red-200 bg-red-50 p-3 text-xs text-red-700">
          {error}
        </div>
      )}

      {loading ? (
        <Panel padded>
          <ListSkeleton rows={3} />
        </Panel>
      ) : reports.length === 0 ? (
        <Panel padded>
          <EmptyState
            icon="check"
            title="Không có báo cáo sự cố nào"
            description="Chiến mã chưa ghi nhận bất kỳ sự cố chấn thương, thể lực bất thường hay vấn đề sức khỏe nào."
          />
        </Panel>
      ) : (
        <div className="space-y-3">
          {reports.map((report) => {
            const statusConfig = STATUS_MAP[report.status] ?? { tone: 'neutral' as const, label: report.status };
            const severityConfig = SEVERITY_MAP[report.severity] ?? { tone: 'neutral' as const, label: report.severity };
            const imgPath = incidentApi.imageSrc(report);

            return (
              <Panel key={report.id} padded className="space-y-3">
                <div className="flex flex-wrap items-start justify-between gap-2 border-b border-[var(--color-border)] pb-2">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-semibold text-[var(--color-text-primary)]">{report.title}</span>
                      <Pill tone={severityConfig.tone} size="sm">{severityConfig.label}</Pill>
                    </div>
                    <p className="text-xs text-[var(--color-text-muted)]">
                      Báo cáo lúc {report.reportedAt ? new Date(report.reportedAt).toLocaleString('vi-VN') : '—'}
                      {report.reportedByName && (
                        <span> • Người báo: <strong>{report.reportedByName}</strong></span>
                      )}
                    </p>
                  </div>
                  <Pill tone={statusConfig.tone} size="sm">{statusConfig.label}</Pill>
                </div>

                <p className="text-xs leading-relaxed text-[var(--color-text-secondary)] whitespace-pre-line">
                  {report.description}
                </p>

                {imgPath && (
                  <div className="pt-2">
                    <p className="mb-1 text-xs font-medium text-[var(--color-text-secondary)]">Ảnh đính kèm:</p>
                    <div className="relative max-h-56 max-w-sm overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-muted)]">
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img
                        src={imgPath}
                        alt="Ảnh đính kèm sự cố"
                        className="max-h-56 max-w-sm object-cover"
                        loading="lazy"
                      />
                    </div>
                  </div>
                )}

                {report.handlerNote && (
                  <div className="rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] p-3 text-xs">
                    <span className="font-semibold text-[var(--color-text-primary)]">Ghi chú của BSTY: </span>
                    <span className="text-[var(--color-text-secondary)]">{report.handlerNote}</span>
                  </div>
                )}
              </Panel>
            );
          })}
        </div>
      )}

      <ReportIncidentDialog
        horseId={horseId}
        horseName={horseName}
        open={isReportOpen}
        onClose={() => setIsReportOpen(false)}
        onSuccess={loadData}
      />
    </div>
  );
}
