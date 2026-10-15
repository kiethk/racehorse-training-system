'use client';

import React, { useEffect, useState, useMemo } from 'react';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { Panel } from '@/components/ui/Panel';
import { Button } from '@/components/ui/Button';
import { Pill } from '@/components/ui/StatusBadge';
import { Icon } from '@/components/ui/Icon';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { careApi } from '@/features/care/services/careApi';
import type { CareTaskItem, CareTasksSummary } from '@/features/care/types';
import { RoutineCheckDialog } from '@/features/care/components/RoutineCheckDialog';
import { RoutineBatchModal } from '@/features/care/components/RoutineBatchModal';

export default function RoutineCarePage() {
  const [items, setItems] = useState<CareTaskItem[]>([]);
  const [summary, setSummary] = useState<CareTasksSummary>({
    overdueCount: 0,
    todayCount: 0,
    thisWeekCount: 0,
    complianceRate: 100,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [urgencyFilter, setUrgencyFilter] = useState<'ALL' | 'OVERDUE' | 'TODAY' | 'UPCOMING'>('ALL');
  const [search, setSearch] = useState('');

  // Selection
  const [selectedIds, setSelectedIds] = useState<number[]>([]);

  // Dialogs
  const [activeCheckItem, setActiveCheckItem] = useState<CareTaskItem | null>(null);
  const [batchModalOpen, setBatchModalOpen] = useState(false);

  useEffect(() => {
    let active = true;
    const fetchData = async () => {
      try {
        setLoading(true);
        setError(null);
        const res = await careApi.getCareTasks({
          type: 'ROUTINE',
          search: search.trim() || undefined,
        });
        if (active) {
          setItems(res.items || []);
          setSummary(res.summary);
        }
      } catch (err) {
        if (active) {
          setError(err instanceof Error ? err.message : 'Không thể tải danh sách ca khám định kỳ');
        }
      } finally {
        if (active) setLoading(false);
      }
    };

    fetchData();
    return () => {
      active = false;
    };
  }, [search]);

  const handleRefresh = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await careApi.getCareTasks({
        type: 'ROUTINE',
        search: search.trim() || undefined,
      });
      setItems(res.items || []);
      setSummary(res.summary);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể tải danh sách ca khám định kỳ');
    } finally {
      setLoading(false);
    }
  };

  // Filtered items
  const filteredItems = useMemo(() => {
    return items.filter((item) => {
      if (urgencyFilter !== 'ALL' && item.urgency !== urgencyFilter) {
        return false;
      }
      return true;
    });
  }, [items, urgencyFilter]);

  // Handle select all
  const handleSelectAll = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.checked) {
      setSelectedIds(filteredItems.map((i) => i.id));
    } else {
      setSelectedIds([]);
    }
  };

  const handleToggleSelect = (id: number) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((i) => i !== id) : [...prev, id]
    );
  };

  // Find next item in queue for sequential flow
  const currentItemIndex = activeCheckItem
    ? filteredItems.findIndex((i) => i.id === activeCheckItem.id)
    : -1;
  const hasNextItem = currentItemIndex >= 0 && currentItemIndex < filteredItems.length - 1;

  const handleNextItem = () => {
    if (hasNextItem) {
      setActiveCheckItem(filteredItems[currentItemIndex + 1]);
    } else {
      setActiveCheckItem(null);
    }
  };

  return (
    <RoleGuard allowedRoles={['VETERINARIAN']}>
      <AppShell>
        <PageContainer>
          <div className="space-y-6">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
              <div>
                <div className="flex items-center gap-3">
                  <h1 className="text-2xl font-bold tracking-tight text-[var(--color-text-primary)]">
                    Khám định kỳ & Tái khám (Routine Care)
                  </h1>
                  <Pill tone="info">ROUTINE</Pill>
                </div>
                <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
                  Khám nhanh lặp lại, đo sinh hiệu định kỳ và đánh giá phục hồi sau điều trị.
                </p>
              </div>

              <div className="flex items-center gap-2">
                <Button variant="secondary" icon="refresh" onClick={handleRefresh} loading={loading}>
                  Làm mới
                </Button>
              </div>
            </div>

            {/* Metrics cards */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
              <Panel padded className="border-l-4 border-l-red-500">
                <div className="text-xs font-medium text-[var(--color-text-secondary)]">Quá hạn khám</div>
                <div className="mt-1 text-2xl font-bold text-red-600 dark:text-red-400">
                  {summary.overdueCount}
                </div>
              </Panel>

              <Panel padded className="border-l-4 border-l-amber-500">
                <div className="text-xs font-medium text-[var(--color-text-secondary)]">Lịch hẹn hôm nay</div>
                <div className="mt-1 text-2xl font-bold text-amber-600 dark:text-amber-400">
                  {summary.todayCount}
                </div>
              </Panel>

              <Panel padded className="border-l-4 border-l-blue-500">
                <div className="text-xs font-medium text-[var(--color-text-secondary)]">Sắp tới (7 ngày)</div>
                <div className="mt-1 text-2xl font-bold text-blue-600 dark:text-blue-400">
                  {summary.thisWeekCount}
                </div>
              </Panel>

              <Panel padded className="border-l-4 border-l-emerald-500">
                <div className="text-xs font-medium text-[var(--color-text-secondary)]">Tỷ lệ tuân thủ</div>
                <div className="mt-1 text-2xl font-bold text-emerald-600 dark:text-emerald-400">
                  {summary.complianceRate}%
                </div>
              </Panel>
            </div>

            {/* Filter toolbar */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-[var(--color-border)] pb-3">
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setUrgencyFilter('ALL')}
                  className={`px-3 py-1.5 text-xs font-medium rounded-[var(--radius-sm)] transition-colors ${
                    urgencyFilter === 'ALL'
                      ? 'bg-[var(--color-primary)] text-white'
                      : 'text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]'
                  }`}
                >
                  Tất cả ({items.length})
                </button>
                <button
                  type="button"
                  onClick={() => setUrgencyFilter('OVERDUE')}
                  className={`px-3 py-1.5 text-xs font-medium rounded-[var(--radius-sm)] transition-colors ${
                    urgencyFilter === 'OVERDUE'
                      ? 'bg-red-600 text-white'
                      : 'text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]'
                  }`}
                >
                  Quá hạn ({summary.overdueCount})
                </button>
                <button
                  type="button"
                  onClick={() => setUrgencyFilter('TODAY')}
                  className={`px-3 py-1.5 text-xs font-medium rounded-[var(--radius-sm)] transition-colors ${
                    urgencyFilter === 'TODAY'
                      ? 'bg-amber-600 text-white'
                      : 'text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]'
                  }`}
                >
                  Hôm nay ({summary.todayCount})
                </button>
                <button
                  type="button"
                  onClick={() => setUrgencyFilter('UPCOMING')}
                  className={`px-3 py-1.5 text-xs font-medium rounded-[var(--radius-sm)] transition-colors ${
                    urgencyFilter === 'UPCOMING'
                      ? 'bg-blue-600 text-white'
                      : 'text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]'
                  }`}
                >
                  Tuần này ({summary.thisWeekCount})
                </button>
              </div>

              <div className="flex items-center gap-2">
                <input
                  type="text"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder="Tìm theo tên ngựa hoặc chuồng..."
                  className="rounded-[var(--radius-sm)] border border-[var(--color-border)] px-3 py-1.5 text-xs bg-[var(--color-surface)] text-[var(--color-text-primary)] focus:outline-none focus:border-[var(--color-primary)] w-64"
                />
              </div>
            </div>

            {/* Error banner */}
            {error && (
              <div className="rounded-[var(--radius-sm)] border border-red-200 bg-red-50 p-4 text-xs text-red-700 dark:border-red-900/50 dark:bg-red-950/20 dark:text-red-300">
                {error}
              </div>
            )}

            {/* Table / List */}
            {loading && items.length === 0 ? (
              <Panel padded>
                <ListSkeleton rows={5} />
              </Panel>
            ) : filteredItems.length === 0 ? (
              <Panel padded>
                <EmptyState
                  icon="check"
                  title="Không có ca khám định kỳ nào cần xử lý"
                  description="Mọi chiến mã đều đã được đo sinh hiệu và kiểm tra đầy đủ."
                />
              </Panel>
            ) : (
              <Panel className="overflow-hidden border border-[var(--color-border)]">
                <div className="overflow-x-auto">
                  <table className="w-full text-left border-collapse text-xs">
                    <thead>
                      <tr className="bg-[var(--color-surface-subtle)] border-b border-[var(--color-border)] text-[var(--color-text-secondary)] font-semibold">
                        <th className="p-3 w-10 text-center">
                          <input
                            type="checkbox"
                            checked={selectedIds.length === filteredItems.length && filteredItems.length > 0}
                            onChange={handleSelectAll}
                            className="accent-[var(--color-primary)]"
                          />
                        </th>
                        <th className="p-3">Chiến mã & Vị trí</th>
                        <th className="p-3">Lịch hẹn</th>
                        <th className="p-3">Mức độ ưu tiên</th>
                        <th className="p-3">Mô tả nhiệm vụ</th>
                        <th className="p-3 text-right">Thao tác</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[var(--color-border)] bg-[var(--color-surface)]">
                      {filteredItems.map((item) => (
                        <tr
                          key={item.id}
                          className={`hover:bg-[var(--color-surface-subtle)]/60 transition-colors ${
                            selectedIds.includes(item.id) ? 'bg-blue-50/40 dark:bg-blue-950/20' : ''
                          }`}
                        >
                          <td className="p-3 text-center">
                            <input
                              type="checkbox"
                              checked={selectedIds.includes(item.id)}
                              onChange={() => handleToggleSelect(item.id)}
                              className="accent-[var(--color-primary)]"
                            />
                          </td>
                          <td className="p-3 font-semibold text-[var(--color-text-primary)]">
                            <div className="flex items-center gap-1.5">
                              <Icon name="horse" size={14} className="text-[var(--color-text-muted)]" />
                              <span>{item.horseName}</span>
                            </div>
                            <span className="block text-[11px] font-normal text-[var(--color-text-muted)] mt-0.5">
                              #{item.horseId} {item.stallCode ? `· Chuồng ${item.stallCode}` : ''}
                            </span>
                          </td>
                          <td className="p-3 text-[var(--color-text-secondary)]">
                            {item.scheduledDate
                              ? new Date(item.scheduledDate).toLocaleString('vi-VN', {
                                  hour: '2-digit',
                                  minute: '2-digit',
                                  day: '2-digit',
                                  month: '2-digit',
                                })
                              : 'Chưa xếp giờ'}
                          </td>
                          <td className="p-3">
                            {item.urgency === 'OVERDUE' && <Pill tone="danger">Quá hạn</Pill>}
                            {item.urgency === 'TODAY' && <Pill tone="warning">Hôm nay</Pill>}
                            {item.urgency === 'UPCOMING' && <Pill tone="info">Sắp tới</Pill>}
                          </td>
                          <td className="p-3 text-[var(--color-text-secondary)] max-w-xs truncate">
                            {item.description || 'Khám lâm sàng & đo sinh hiệu định kỳ'}
                          </td>
                          <td className="p-3 text-right">
                            <Button
                              variant="primary"
                              size="sm"
                              icon="clipboard"
                              onClick={() => setActiveCheckItem(item)}
                            >
                              Khám ngay
                            </Button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </Panel>
            )}

            {/* Floating Selection Action Bar */}
            {selectedIds.length > 0 && (
              <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-40 flex items-center gap-4 rounded-full border border-[var(--color-border)] bg-[var(--color-surface)] px-6 py-3 shadow-xl backdrop-blur-md">
                <span className="text-xs font-semibold text-[var(--color-text-primary)]">
                  Đã chọn <strong>{selectedIds.length}</strong> chiến mã
                </span>
                <div className="h-4 w-px bg-[var(--color-border)]" />
                <Button
                  variant="primary"
                  size="sm"
                  icon="users"
                  onClick={() => setBatchModalOpen(true)}
                >
                  Khám hàng loạt theo lô
                </Button>
                <Button
                  variant="tertiary"
                  size="sm"
                  onClick={() => setSelectedIds([])}
                >
                  Bỏ chọn
                </Button>
              </div>
            )}
          </div>

          {/* Dialog Khám nhanh 1 con (Pattern 3) */}
          {activeCheckItem && (
            <RoutineCheckDialog
              scheduleId={activeCheckItem.id}
              horseId={activeCheckItem.horseId}
              horseName={activeCheckItem.horseName}
              stallCode={activeCheckItem.stallCode}
              open={Boolean(activeCheckItem)}
              onOpenChange={(open) => {
                if (!open) setActiveCheckItem(null);
              }}
              onSuccess={() => {
                handleRefresh();
              }}
              onNext={handleNextItem}
              hasNext={hasNextItem}
            />
          )}

          {/* Modal Khám hàng loạt (Batch) */}
          {batchModalOpen && (
            <RoutineBatchModal
              open={batchModalOpen}
              onOpenChange={setBatchModalOpen}
              items={filteredItems.filter((i) => selectedIds.includes(i.id))}
              onSuccess={() => {
                setSelectedIds([]);
                handleRefresh();
              }}
            />
          )}
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
