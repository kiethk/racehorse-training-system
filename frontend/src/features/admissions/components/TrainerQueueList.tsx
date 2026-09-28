'use client';

import { useCallback, useEffect, useState } from 'react';

import { Panel, SectionTitle } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { trainerAdmissionsApi } from '../services/trainerAdmissionService';
import type { AdmissionSummaryResponse } from '../types';
import { TrainerReviewPanel } from './TrainerReviewPanel';

export function TrainerQueueList() {
  const [queue, setQueue] = useState<AdmissionSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<number | null>(null);

  const loadQueue = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await trainerAdmissionsApi.getQueue();
      setQueue(data);
      // Giữ nguyên lựa chọn nếu đơn đó còn trong hàng đợi, ngược lại chọn đơn đầu.
      setSelectedId((prev) =>
        prev && data.some((a) => a.admissionId === prev)
          ? prev
          : data[0]?.admissionId ?? null,
      );
    } catch (err) {
      console.error('Không tải được hàng đợi tiếp nhận:', err);
      setError('Không tải được danh sách đơn. Kiểm tra kết nối rồi thử lại.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadQueue();
  }, [loadQueue]);

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
          <SectionTitle>Chờ đánh giá ({queue.length})</SectionTitle>

          {queue.length === 0 ? (
            <EmptyState
              icon="clipboard"
              title="Không có đơn nào"
              description="Chưa có hồ sơ nào chuyển tới bước đánh giá của Huấn luyện viên."
            />
          ) : (
            <ul className="mt-3 space-y-2">
              {queue.map((item) => {
                const active = selectedId === item.admissionId;
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
                      <div className="mt-1.5">
                        {item.quarantineStallCode ? (
                          <Pill tone="isolated" icon="shield" size="sm">
                            Chuồng {item.quarantineStallCode}
                          </Pill>
                        ) : (
                          <Pill tone="neutral" size="sm">
                            Chưa xếp chuồng
                          </Pill>
                        )}
                      </div>
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </Panel>

        {/* ---------- Cột phải: hồ sơ + form ---------- */}
        {selectedId ? (
          <TrainerReviewPanel admissionId={selectedId} onSubmitted={loadQueue} />
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