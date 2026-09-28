'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';

import { useAuth } from '@/context/AuthContext';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { trainerAdmissionsApi } from '../services/trainerAdmissionService';
import type { AdmissionSummaryResponse, AdmissionStatus } from '../types';
import { TrainerReviewPanel } from './TrainerReviewPanel';

type Tab = 'PENDING' | 'REVIEWED';

const STATUS_LABEL: Partial<Record<AdmissionStatus, string>> = {
  GROOM_REVIEW: 'Chờ chăm sóc viên',
  WAITING_FOR_STALL: 'Chờ xếp chuồng',
  VET_REVIEW: 'Chờ thú y',
  TRAINER_REVIEW: 'Chờ bạn đánh giá',
  MANAGER_REVIEW: 'Chờ quản lý duyệt',
  APPROVED: 'Đã tiếp nhận',
  REJECTED: 'Đã từ chối',
};

export function TrainerQueueList() {
  const { user } = useAuth();
  const [admissions, setAdmissions] = useState<AdmissionSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [tab, setTab] = useState<Tab>('PENDING');

  const loadQueue = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      setAdmissions(await trainerAdmissionsApi.getAll());
    } catch (err) {
      console.error('Không tải được danh sách tiếp nhận:', err);
      setError('Không tải được danh sách hồ sơ. Kiểm tra kết nối rồi thử lại.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadQueue();
  }, [loadQueue]);

  const pending = useMemo(
    () => admissions.filter((a) => a.status === 'TRAINER_REVIEW'),
    [admissions],
  );
  /**
   * Hồ sơ do CHÍNH Huấn luyện viên đang đăng nhập đánh giá.
   *
   * Lọc theo trainerId chứ không theo status, vì hai lý do:
   *   - Nhiều Trainer cùng làm việc: lọc theo status sẽ cho thấy lẫn hồ sơ
   *     của nhau, do status chỉ nói "đã qua bước Trainer", không nói ai duyệt.
   *   - Bắt được cả hồ sơ bị Quản lý TỪ CHỐI sau khi mình đã đánh giá —
   *     trường hợp mà lọc theo status bỏ sót, vì REJECTED cũng có thể do
   *     Groom hoặc Thú y đặt từ trước khi tới bước Trainer.
   */
  const reviewed = useMemo(
    () =>
      admissions.filter(
        (a) => a.trainerReviewedAt !== null && a.trainerId === user?.userId,
      ),
    [admissions, user?.userId],
  );
  const shown = tab === 'PENDING' ? pending : reviewed;

  /**
   * Hồ sơ đang mở ở cột phải — SUY RA lúc render, không đồng bộ bằng effect.
   *
   * Đổi tab làm lựa chọn cũ không còn trong danh sách, khi đó tự rơi về hồ sơ
   * đầu tiên. Cách này tránh hẳn việc gọi setState trong effect, vốn gây thêm
   * một lượt render thừa và bị lint chặn.
   */
  const activeId =
    selectedId !== null && shown.some((a) => a.admissionId === selectedId)
      ? selectedId
      : shown[0]?.admissionId ?? null;

  if (loading) return <ListSkeleton rows={5} />;

  if (error) {
    return (
      <Panel padded>
        <EmptyState icon="alert-triangle" title="Lỗi tải dữ liệu" description={error} />
      </Panel>
    );
  }

  return (
    <div>
      <div className="mb-4">
        <h1 className="text-[18px] font-semibold tracking-tight text-[var(--color-text-primary)]">
          Tiếp nhận chiến mã
        </h1>
        <p className="text-[12px] text-[var(--color-text-secondary)]">
          Đánh giá tiềm năng thi đấu của ngựa ứng viên đang trong khu cách ly.
        </p>
      </div>

      <div className="grid gap-4 lg:grid-cols-[320px_1fr]">
        {/* ---------- Cột trái: hàng đợi ---------- */}
        <Panel padded>
          <div className="flex gap-1.5">
            {([
              ['PENDING', `Chờ đánh giá (${pending.length})`],
              ['REVIEWED', `Đã đánh giá (${reviewed.length})`],
            ] as [Tab, string][]).map(([key, label]) => (
              <button
                key={key}
                type="button"
                onClick={() => setTab(key)}
                className={`flex-1 rounded-[var(--radius-md)] px-2 py-1.5 text-[11px] font-medium transition ${
                  tab === key
                    ? 'bg-[var(--color-primary)] text-[var(--color-text-inverse)]'
                    : 'bg-[var(--color-surface-muted)] text-[var(--color-text-secondary)] hover:bg-[var(--color-surface)]'
                }`}
              >
                {label}
              </button>
            ))}
          </div>

          {shown.length === 0 ? (
            <EmptyState
              icon="clipboard"
              title={tab === 'PENDING' ? 'Không có hồ sơ nào' : 'Chưa đánh giá hồ sơ nào'}
              description={
                tab === 'PENDING'
                  ? 'Chưa có chiến mã nào chờ bạn đánh giá.'
                  : 'Các hồ sơ bạn đã đánh giá sẽ hiện ở đây.'
              }
            />
          ) : (
            <ul className="mt-3 space-y-2">
              {shown.map((item) => {
                const active = activeId === item.admissionId;
                return (
                  <li key={item.admissionId}>
                    <button
                      type="button"
                      onClick={() => setSelectedId(item.admissionId)}
                      className={`w-full rounded-[var(--radius-md)] border p-3 text-left transition ${
                        active
                          ? 'border-[var(--color-primary)] bg-[var(--color-primary-subtle)]'
                          : 'border-[var(--color-border)] hover:bg-[var(--color-surface-muted)]'
                      }`}
                    >
                      <div className="text-[13px] font-semibold text-[var(--color-text-primary)]">
                        {item.candidateName}
                      </div>
                      <div className="text-[11px] text-[var(--color-text-muted)]">
                        {item.breed || 'Chưa rõ giống'}
                      </div>
                      <div className="mt-1.5 flex flex-wrap gap-1">
                        {item.quarantineStallCode ? (
                          <Pill tone="isolated" icon="shield" size="sm">
                            Chuồng {item.quarantineStallCode}
                          </Pill>
                        ) : (
                          <Pill tone="neutral" size="sm">
                            Chưa xếp chuồng
                          </Pill>
                        )}
                        {/* Tab "đã đánh giá" gộp nhiều trạng thái nên phải nói rõ hồ sơ đang ở đâu */}
                        {tab === 'REVIEWED' && (
                          <Pill
                            tone={
                              item.status === 'APPROVED'
                                ? 'success'
                                : item.status === 'REJECTED'
                                ? 'danger'
                                : 'info'
                            }
                            size="sm"
                          >
                            {STATUS_LABEL[item.status] ?? item.status}
                          </Pill>
                        )}
                      </div>

                      {tab === 'REVIEWED' && item.trainerReviewedAt && (
                        <div className="mt-1 text-[11px] text-[var(--color-text-muted)]">
                          Bạn đánh giá ngày{' '}
                          {new Date(item.trainerReviewedAt).toLocaleDateString('vi-VN')}
                        </div>
                      )}
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </Panel>

        {/* ---------- Cột phải: hồ sơ + form ---------- */}
        {activeId ? (
          <TrainerReviewPanel admissionId={activeId} onSubmitted={loadQueue} />
        ) : (
          <Panel padded>
            <EmptyState
              icon="search"
              title="Chọn một hồ sơ"
              description="Chọn đơn ở cột trái để xem chi tiết và đánh giá."
            />
          </Panel>
        )}
      </div>
    </div>
  );
}