'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { DetailSkeleton, EmptyState } from '@/components/ui/states';
import { racingService } from '../services/racingService';
import type { RaceRegistrationResponse, RaceRegistrationStatus } from '../types';

interface TrainerRaceDetailProps {
  id: number;
}

export function TrainerRaceDetail({ id }: TrainerRaceDetailProps) {
  const [data, setData] = useState<RaceRegistrationResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function fetchDetail() {
      try {
        setLoading(true);
        setError(null);
        const res = await racingService.getMine(id);
        setData(res);
      } catch (err) {
        console.error('Lỗi khi tải chi tiết đơn đề cử:', err);
        setError(err instanceof Error ? err.message : 'Không tìm thấy đơn đề cử hoặc bạn không có quyền xem.');
      } finally {
        setLoading(false);
      }
    }
    fetchDetail();
  }, [id]);

  const renderStatusBadge = (status: RaceRegistrationStatus) => {
    switch (status) {
      case 'PENDING':
        return <Pill tone="warning" size="md">Chờ duyệt</Pill>;
      case 'APPROVED':
        return <Pill tone="success" size="md">Đã duyệt</Pill>;
      case 'REJECTED':
        return <Pill tone="danger" size="md">Từ chối</Pill>;
      default:
        return <Pill tone="neutral" size="md">{status}</Pill>;
    }
  };

  const formatDate = (dateStr?: string | null) => {
    if (!dateStr) return '—';
    try {
      const [y, m, d] = dateStr.split('-');
      if (y && m && d) return `${d}/${m}/${y}`;
      return new Date(dateStr).toLocaleDateString('vi-VN');
    } catch {
      return dateStr;
    }
  };

  const formatDateTime = (dtStr?: string | null) => {
    if (!dtStr) return '—';
    try {
      const dt = new Date(dtStr);
      return `${dt.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })} ngày ${dt.toLocaleDateString('vi-VN')}`;
    } catch {
      return dtStr;
    }
  };

  if (loading) {
    return (
      <Panel padded>
        <DetailSkeleton />
      </Panel>
    );
  }

  if (error || !data) {
    return (
      <Panel padded>
        <EmptyState
          icon="alert-triangle"
          title="Không thể hiển thị đơn đề cử"
          description={error || 'Đơn đề cử không tồn tại hoặc đã bị gỡ bỏ.'}
          action={
            <Link href="/trainer/racing">
              <Button variant="secondary" size="sm">
                ← Quay lại danh sách
              </Button>
            </Link>
          }
        />
      </Panel>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-4 border-b border-[var(--color-border)] pb-4">
        <div>
          <Link
            href="/trainer/racing"
            className="inline-flex items-center text-[12px] text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] transition mb-1"
          >
            ← Danh sách đơn đề cử
          </Link>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-[20px] font-bold text-[var(--color-text-primary)]">
              {data.raceName}
            </h1>
            {renderStatusBadge(data.status)}
          </div>
          <p className="mt-0.5 text-[13px] text-[var(--color-text-secondary)]">
            Hạng mục: <strong className="text-[var(--color-text-primary)]">{data.raceCategory}</strong> • Ngày gửi: {formatDateTime(data.createdAt)}
          </p>
        </div>

        <Link href="/trainer/racing">
          <Button variant="secondary">
            Quay lại danh sách
          </Button>
        </Link>
      </div>

      {/* Cảnh báo nguồn tin */}
      <div className="rounded-[var(--radius-md)] border border-[var(--color-info)] bg-[var(--color-info-soft)] p-3 text-[12px] text-[var(--color-info)] flex items-center gap-2">
        <span>ℹ️</span>
        <span>
          <strong>Lưu ý:</strong> Thông tin cuộc đua do Trainer tự tìm hiểu và cung cấp; Ban Quản lý sẽ kiểm chứng trước khi phê duyệt.
        </span>
      </div>

      {/* Khối phản hồi của Quản lý (nếu đã xét duyệt) */}
      {data.status !== 'PENDING' && (
        <div
          className={`rounded-[var(--radius-md)] border p-4 text-[13px] ${
            data.status === 'APPROVED'
              ? 'border-[var(--color-success)] bg-[var(--color-success-soft)] text-[var(--color-success)]'
              : 'border-[var(--color-danger)] bg-[var(--color-danger-soft)] text-[var(--color-danger)]'
          }`}
        >
          <div className="font-bold flex items-center gap-2 mb-1">
            <span>{data.status === 'APPROVED' ? '✅' : '❌'}</span>
            <span>
              Phản hồi từ Ban Quản lý ({data.status === 'APPROVED' ? 'Đồng ý duyệt' : 'Từ chối đề cử'})
            </span>
          </div>
          {data.reviewedAt && (
            <div className="text-[12px] opacity-80 mb-2">
              Thời gian duyệt: {formatDateTime(data.reviewedAt)}
            </div>
          )}
          <div className="rounded bg-[var(--color-surface)]/80 p-3 border border-current/20 text-[var(--color-text-primary)]">
            {data.managerFeedback || 'Không có nhận xét bổ sung.'}
          </div>
        </div>
      )}

      {/* Grid 2 cột: Chiến mã & Thông tin giải */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* Cột 1: Chiến mã */}
        <div className="space-y-6">
          <Panel padded>
            <h2 className="text-[15px] font-semibold text-[var(--color-text-primary)] mb-3 flex items-center gap-2 border-b border-[var(--color-border)] pb-2">
              <span>🏇</span> Chiến mã đề cử
            </h2>

            <div className="space-y-3 text-[13px]">
              <div>
                <span className="text-[var(--color-text-secondary)]">Tên chiến mã:</span>{' '}
                <strong className="text-[var(--color-text-primary)] text-[15px]">
                  {data.horseName || `Ngựa #${data.horseId}`}
                </strong>
              </div>

              {data.horseRegistrationNumber && (
                <div>
                  <span className="text-[var(--color-text-secondary)]">Mã UELN / Số đăng ký:</span>{' '}
                  <span className="font-mono font-medium text-[var(--color-text-primary)]">
                    {data.horseRegistrationNumber}
                  </span>
                </div>
              )}

              <div className="mt-3 pt-3 border-t border-[var(--color-border)]">
                <span className="text-[var(--color-text-secondary)] block font-medium mb-1">
                  Nhận định / Lý do đề cử của Trainer:
                </span>
                <p className="whitespace-pre-wrap rounded bg-[var(--color-surface-muted)] p-3 text-[var(--color-text-primary)] border border-[var(--color-border)]">
                  {data.selectionReason}
                </p>
              </div>
            </div>
          </Panel>

          {/* Cột 1: Thông tin tham khảo */}
          <Panel padded>
            <h2 className="text-[15px] font-semibold text-[var(--color-text-primary)] mb-3 flex items-center gap-2 border-b border-[var(--color-border)] pb-2">
              <span>📋</span> Thông số kỹ thuật & Ghi chú
            </h2>

            <div className="space-y-3 text-[13px]">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <span className="text-[var(--color-text-secondary)] block">Cự ly:</span>
                  <span className="font-medium text-[var(--color-text-primary)]">
                    {data.distanceMeters ? `${data.distanceMeters.toLocaleString()} m` : '—'}
                  </span>
                </div>
                <div>
                  <span className="text-[var(--color-text-secondary)] block">Mặt sân:</span>
                  <span className="font-medium text-[var(--color-text-primary)]">
                    {data.trackType || '—'}
                  </span>
                </div>
              </div>

              {data.prizeDetails && (
                <div className="pt-2 border-t border-[var(--color-border)]">
                  <span className="text-[var(--color-text-secondary)] block font-medium mb-1">
                    Cơ cấu giải thưởng / Tiền thưởng:
                  </span>
                  <p className="whitespace-pre-wrap rounded bg-[var(--color-surface-muted)] p-2.5 text-[var(--color-text-primary)] border border-[var(--color-border)]">
                    {data.prizeDetails}
                  </p>
                </div>
              )}

              {data.trainerNotes && (
                <div className="pt-2 border-t border-[var(--color-border)]">
                  <span className="text-[var(--color-text-secondary)] block font-medium mb-1">
                    Ghi chú / Điều kiện tham dự / Hậu cần:
                  </span>
                  <p className="whitespace-pre-wrap rounded bg-[var(--color-surface-muted)] p-2.5 text-[var(--color-text-primary)] border border-[var(--color-border)]">
                    {data.trainerNotes}
                  </p>
                </div>
              )}
            </div>
          </Panel>
        </div>

        {/* Cột 2: Chi tiết cuộc đua */}
        <div className="space-y-6">
          <Panel padded>
            <h2 className="text-[15px] font-semibold text-[var(--color-text-primary)] mb-3 flex items-center gap-2 border-b border-[var(--color-border)] pb-2">
              <span>🏆</span> Chi tiết cuộc đua / sự kiện
            </h2>

            <div className="space-y-3 text-[13px]">
              <div>
                <span className="text-[var(--color-text-secondary)] block">Tên sự kiện:</span>
                <span className="font-semibold text-[var(--color-text-primary)] text-[14px]">
                  {data.raceName}
                </span>
              </div>

              <div>
                <span className="text-[var(--color-text-secondary)] block">Hạng mục / Chặng:</span>
                <span className="font-medium text-[var(--color-text-primary)]">
                  {data.raceCategory}
                </span>
              </div>

              <div>
                <span className="text-[var(--color-text-secondary)] block">Sân đua & Địa điểm:</span>
                <span className="font-medium text-[var(--color-text-primary)]">
                  📍 {data.location}
                </span>
              </div>

              <div className="grid grid-cols-2 gap-4 pt-2 border-t border-[var(--color-border)]">
                <div>
                  <span className="text-[var(--color-text-secondary)] block">Ngày tổ chức:</span>
                  <span className="font-medium text-[var(--color-text-primary)]">
                    📅 {formatDate(data.eventDate)}
                  </span>
                </div>
                <div>
                  <span className="text-[var(--color-text-secondary)] block">Giờ xuất phát:</span>
                  <span className="font-medium text-[var(--color-text-primary)]">
                    ⏱️ {data.eventTime ? data.eventTime.substring(0, 5) : 'Chưa công bố'}
                  </span>
                </div>
              </div>

              <div className="pt-2 border-t border-[var(--color-border)]">
                <span className="text-[var(--color-text-secondary)] block">Đơn vị tổ chức:</span>
                <span className="font-medium text-[var(--color-text-primary)]">
                  {data.organizer || '—'}
                </span>
              </div>

              <div>
                <span className="text-[var(--color-text-secondary)] block">Hạn nộp hồ sơ thật:</span>
                <span className="font-medium text-[var(--color-text-primary)]">
                  {data.nominationDeadline ? formatDate(data.nominationDeadline) : '—'}
                </span>
              </div>

              {data.sourceUrl && (
                <div className="pt-2 border-t border-[var(--color-border)]">
                  <span className="text-[var(--color-text-secondary)] block font-medium mb-1">
                    Trang thể lệ / Link chính thức:
                  </span>
                  <a
                    href={data.sourceUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-[var(--color-primary)] hover:underline break-all text-[12px] inline-flex items-center gap-1"
                  >
                    <span>🔗 {data.sourceUrl}</span>
                    <span className="text-[10px]">↗</span>
                  </a>
                </div>
              )}
            </div>
          </Panel>
        </div>
      </div>
    </div>
  );
}
