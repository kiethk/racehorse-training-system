'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { trainingApi } from '../services/trainingService';
import type { TrainingLotResponse } from '../types';
import { RescheduleDialog } from './RescheduleDialog';

const WINDOW_START = 6 * 60; // 06:00 (360')
const WINDOW_END = 10 * 60;  // 10:00 (600')
const WINDOW_LEN = WINDOW_END - WINDOW_START; // 240'

function toMinutes(hhmmss: string): number {
  const [h, m] = hhmmss.split(':').map(Number);
  return h * 60 + m;
}

export function LotTimeline() {
  const [selectedDate, setSelectedDate] = useState<string>(
    new Date().toISOString().split('T')[0],
  );
  const [lots, setLots] = useState<TrainingLotResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Modals
  const [rescheduleLot, setRescheduleLot] = useState<TrainingLotResponse | null>(null);
  const [cancelTargetLot, setCancelTargetLot] = useState<TrainingLotResponse | null>(null);
  const [cancelling, setCancelling] = useState(false);

  const loadLots = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      // Mặc định includeCancelled = false theo Plan 6
      const data = await trainingApi.getLots(selectedDate, selectedDate, false);
      setLots(data);
    } catch (err) {
      console.error('Lỗi khi nạp lịch lot:', err);
      setError('Không tải được danh sách lot của ngày đã chọn.');
    } finally {
      setLoading(false);
    }
  }, [selectedDate]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadLots();
  }, [loadLots]);

  async function handleConfirmCancel() {
    if (!cancelTargetLot) return;
    setCancelling(true);
    try {
      await trainingApi.cancelLot(cancelTargetLot.lotId);
      setCancelTargetLot(null);
      await loadLots();
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Huỷ lot thất bại.');
    } finally {
      setCancelling(false);
    }
  }

  // Sắp xếp lot theo giờ bắt đầu
  const sortedLots = useMemo(() => {
    return [...lots].sort((a, b) => a.startTime.localeCompare(b.startTime));
  }, [lots]);

  // Tính tổng phút đã sử dụng trong khung giờ vàng
  const totalOccupiedMinutes = useMemo(() => {
    return lots.reduce((acc, l) => acc + (l.durationMinutes || 0), 0);
  }, [lots]);

  if (loading) return <ListSkeleton rows={4} />;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-[18px] font-semibold text-[var(--color-text-primary)]">
            Lịch Lot huấn luyện (Vận hành khung giờ vàng)
          </h1>
          <p className="text-[12px] text-[var(--color-text-secondary)]">
            Quan sát trực quan các lot tập từ 06:00 đến 10:00 và nhận diện các khe giờ còn trống.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <label className="text-[12px] font-medium text-[var(--color-text-primary)]">
            Chọn ngày:
          </label>
          <input
            type="date"
            value={selectedDate}
            onChange={(e) => setSelectedDate(e.target.value)}
            className="rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-2.5 py-1 text-[12px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
          />
          <Button variant="secondary" size="sm" onClick={() => loadLots()}>
            Tải lại
          </Button>
        </div>
      </div>

      {error ? (
        <Panel padded>
          <EmptyState icon="alert-triangle" title="Lỗi nạp dữ liệu" description={error} />
        </Panel>
      ) : (
        <>
          {/* Dải thời gian trực quan (Timeline bar) */}
          <Panel padded>
            <div className="flex items-center justify-between mb-3 text-[13px]">
              <span className="font-semibold text-[var(--color-text-primary)]">
                Dải thời gian 06:00 – 10:00 ({selectedDate})
              </span>
              <span className="text-[12px] text-[var(--color-text-secondary)]">
                Đã dùng: <strong>{totalOccupiedMinutes}/240 phút</strong> • Còn trống:{' '}
                <strong>{Math.max(0, 240 - totalOccupiedMinutes)} phút</strong>
              </span>
            </div>

            {/* Thước đo giờ */}
            <div className="relative h-6 text-[10px] text-[var(--color-text-muted)] font-mono border-b border-[var(--color-border)] mb-2">
              <span className="absolute left-0">06:00</span>
              <span className="absolute left-[25%] -translate-x-1/2">07:00</span>
              <span className="absolute left-[50%] -translate-x-1/2">08:00</span>
              <span className="absolute left-[75%] -translate-x-1/2">09:00</span>
              <span className="absolute right-0">10:00</span>
            </div>

            {/* Dải trực quan các Lot */}
            <div className="relative h-16 w-full rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] border border-[var(--color-border)] overflow-hidden">
              {sortedLots.map((lot) => {
                const startMins = toMinutes(lot.startTime);
                const endMins = toMinutes(lot.endTime);
                const leftPercent = Math.max(0, ((startMins - WINDOW_START) / WINDOW_LEN) * 100);
                const widthPercent = Math.min(100 - leftPercent, ((endMins - startMins) / WINDOW_LEN) * 100);

                const tone =
                  lot.remainingSlots === 0
                    ? 'bg-[var(--color-danger-soft)] border-[var(--color-danger)] text-[var(--color-danger)]'
                    : lot.remainingSlots <= 2
                    ? 'bg-[var(--color-warning-soft)] border-[var(--color-warning)] text-[var(--color-warning)]'
                    : 'bg-[var(--color-success-soft)] border-[var(--color-success)] text-[var(--color-success)]';

                return (
                  <div
                    key={lot.lotId}
                    style={{ left: `${leftPercent}%`, width: `${widthPercent}%` }}
                    className={`absolute top-1 bottom-1 rounded border px-2 py-1 flex flex-col justify-center overflow-hidden transition shadow-sm ${tone}`}
                    title={`Lot #${lot.lotId}: ${lot.subjectName} (${lot.startTime} - ${lot.endTime}) - ${lot.occupied}/${lot.maxCapacity} ngựa`}
                  >
                    <div className="text-[11px] font-bold truncate">
                      #{lot.lotId} {lot.subjectName}
                    </div>
                    <div className="text-[10px] truncate">
                      {lot.startTime.substring(0, 5)}–{lot.endTime.substring(0, 5)} ({lot.occupied}/{lot.maxCapacity} ngựa)
                    </div>
                  </div>
                );
              })}
            </div>
          </Panel>

          {/* Danh sách thẻ chi tiết từng Lot */}
          <div className="space-y-3">
            <h2 className="text-[15px] font-semibold text-[var(--color-text-primary)]">
              Chi tiết các Lot trong ngày ({sortedLots.length})
            </h2>

            {sortedLots.length === 0 ? (
              <Panel padded>
                <EmptyState
                  icon="activity"
                  title="Không có lot nào trong ngày"
                  description="Khung giờ vàng 06:00 – 10:00 hoàn toàn trống trong ngày này."
                />
              </Panel>
            ) : (
              <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
                {sortedLots.map((lot) => (
                  <Panel key={lot.lotId} padded>
                    <div className="flex flex-col justify-between h-full">
                      <div>
                        <div className="flex items-start justify-between gap-2">
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="text-[14px] font-bold text-[var(--color-text-primary)]">
                                Lot #{lot.lotId} • {lot.subjectName}
                              </span>
                            </div>
                            <div className="text-[12px] text-[var(--color-text-secondary)] mt-0.5">
                              ⏱️ <strong>{lot.startTime} – {lot.endTime}</strong> ({lot.durationMinutes} phút)
                            </div>
                          </div>

                          <Pill
                            tone={
                              lot.remainingSlots === 0
                                ? 'danger'
                                : lot.remainingSlots <= 2
                                ? 'warning'
                                : 'success'
                            }
                            size="sm"
                          >
                            {lot.occupied}/{lot.maxCapacity} ngựa {lot.remainingSlots === 0 ? '(Đầy)' : ''}
                          </Pill>
                        </div>

                        <div className="mt-3 text-[12px] border-t border-[var(--color-border)] pt-2">
                          <div className="text-[11px] text-[var(--color-text-muted)]">
                            Chiến mã tham gia ({lot.horseNames.length}):
                          </div>
                          <div className="mt-1 flex flex-wrap gap-1.5">
                            {lot.horseNames.length === 0 ? (
                              <span className="text-[11px] text-[var(--color-text-muted)] italic">
                                Không có ngựa
                              </span>
                            ) : (
                              lot.horseNames.map((name, i) => (
                                <span
                                  key={i}
                                  className="rounded bg-[var(--color-surface-muted)] px-2 py-0.5 text-[11px] font-medium text-[var(--color-text-primary)] border border-[var(--color-border)]"
                                >
                                  🏇 {name}
                                </span>
                              ))
                            )}
                          </div>
                        </div>
                      </div>

                      <div className="mt-4 pt-2 border-t border-[var(--color-border)] flex justify-end gap-2">
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => setRescheduleLot(lot)}
                        >
                          Dời giờ
                        </Button>
                        <Button
                          variant="destructive"
                          size="sm"
                          onClick={() => setCancelTargetLot(lot)}
                        >
                          Huỷ lot
                        </Button>
                      </div>
                    </div>
                  </Panel>
                ))}
              </div>
            )}
          </div>
        </>
      )}

      {/* Modal dời giờ */}
      <RescheduleDialog
        lot={rescheduleLot}
        open={rescheduleLot !== null}
        onClose={() => setRescheduleLot(null)}
        onSuccess={loadLots}
      />

      {/* Confirm dialog huỷ lot */}
      <ConfirmDialog
        open={cancelTargetLot !== null}
        title={`Huỷ lot #${cancelTargetLot?.lotId} (${cancelTargetLot?.subjectName})?`}
        description={
          <div>
            Toàn bộ <strong>{cancelTargetLot?.occupied} buổi tập</strong> của các chiến mã trong lot sẽ bị huỷ theo.
            Khe giờ {cancelTargetLot?.startTime} – {cancelTargetLot?.endTime} sẽ được trả lại cho lot khác.
          </div>
        }
        tone="danger"
        confirmLabel="Xác nhận huỷ"
        loading={cancelling}
        onConfirm={handleConfirmCancel}
        onCancel={() => setCancelTargetLot(null)}
      />
    </div>
  );
}
