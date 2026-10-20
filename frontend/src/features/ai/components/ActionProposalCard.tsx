'use client';

import { useState, useEffect } from 'react';
import { Icon } from '@/components/ui/Icon';
import type { ActionProposalDto, ProposalLocalStatus } from '../types';

interface ActionProposalCardProps {
  proposal: ActionProposalDto;
  onCommit: (actionId: number, actionToken: string) => Promise<boolean>;
  onReject?: (actionId: number) => void;
  disabled?: boolean;
}

export function ActionProposalCard({
  proposal,
  onCommit,
  onReject,
  disabled = false,
}: ActionProposalCardProps) {
  const [timeLeft, setTimeLeft] = useState<number>(proposal.expiresInSeconds || 300);
  const [localStatus, setLocalStatus] = useState<ProposalLocalStatus>('PENDING');

  useEffect(() => {
    if (localStatus !== 'PENDING') return;

    const timer = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          setLocalStatus('EXPIRED');
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [localStatus]);

  const formatCountdown = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const handleApprove = async () => {
    if (localStatus !== 'PENDING' || timeLeft <= 0 || disabled) return;
    setLocalStatus('COMMITTING');
    try {
      const ok = await onCommit(proposal.actionId, proposal.actionToken);
      if (ok) {
        setLocalStatus('COMMITTED');
      } else {
        setLocalStatus('PENDING');
      }
    } catch {
      setLocalStatus('PENDING');
    }
  };

  const handleReject = () => {
    if (localStatus !== 'PENDING' || disabled) return;
    setLocalStatus('REJECTED');
    onReject?.(proposal.actionId);
  };

  // Liệt kê các key thay đổi giữa currentValues và proposedValues
  const proposedKeys = Object.keys(proposal.proposedValues || {});

  const renderValue = (val: unknown) => {
    if (val === null || val === undefined) return <span className="italic text-neutral-400">trống</span>;
    if (typeof val === 'boolean') return val ? 'Có' : 'Không';
    if (typeof val === 'object') return JSON.stringify(val);
    return String(val);
  };

  return (
    <div className="my-2.5 overflow-hidden rounded-xl border border-amber-500/30 bg-amber-500/5 text-xs shadow-sm transition-all dark:border-amber-500/20 dark:bg-amber-950/20">
      {/* Header Thẻ Đề xuất */}
      <div className="flex items-center justify-between border-b border-amber-500/20 bg-amber-500/10 px-3 py-2 text-[var(--color-text-primary)]">
        <div className="flex items-center gap-1.5 font-medium">
          <Icon name="shield" size={14} className="text-amber-600 dark:text-amber-400" />
          <span className="truncate">{proposal.title || 'Đề xuất thay đổi'}</span>
          <span className="rounded bg-amber-500/20 px-1 py-0.5 text-[10px] text-amber-800 dark:text-amber-200">
            #{proposal.targetEntityId}
          </span>
        </div>

        {/* Trạng thái & TTL Countdown */}
        <div>
          {localStatus === 'PENDING' && (
            <span className="inline-flex items-center gap-1 rounded-full bg-amber-500/20 px-2 py-0.5 text-[11px] font-medium text-amber-700 dark:text-amber-300">
              <Icon name="clock" size={11} />
              {formatCountdown(timeLeft)}
            </span>
          )}
          {localStatus === 'COMMITTING' && (
            <span className="inline-flex items-center gap-1 rounded-full bg-blue-500/20 px-2 py-0.5 text-[11px] font-medium text-blue-700 dark:text-blue-300">
              <Icon name="refresh" size={11} className="animate-spin" />
              Đang áp dụng
            </span>
          )}
          {localStatus === 'COMMITTED' && (
            <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/20 px-2 py-0.5 text-[11px] font-medium text-emerald-700 dark:text-emerald-300">
              <Icon name="check" size={11} />
              Đã duyệt
            </span>
          )}
          {localStatus === 'REJECTED' && (
            <span className="inline-flex items-center gap-1 rounded-full bg-neutral-500/20 px-2 py-0.5 text-[11px] font-medium text-neutral-600 dark:text-neutral-400">
              <Icon name="x" size={11} />
              Đã từ chối
            </span>
          )}
          {localStatus === 'EXPIRED' && (
            <span className="inline-flex items-center gap-1 rounded-full bg-rose-500/20 px-2 py-0.5 text-[11px] font-medium text-rose-700 dark:text-rose-300">
              <Icon name="alert-triangle" size={11} />
              Hết hạn
            </span>
          )}
        </div>
      </div>

      {/* Nội dung lý do */}
      {proposal.reason && (
        <div className="border-b border-amber-500/10 px-3 py-1.5 text-neutral-600 dark:text-neutral-300">
          <p className="line-clamp-2 italic text-[11px]">&quot;{proposal.reason}&quot;</p>
        </div>
      )}

      {/* Bảng so sánh Diff (Old vs New) */}
      <div className="overflow-x-auto p-2">
        <table className="w-full text-left">
          <thead>
            <tr className="border-b border-amber-500/15 text-[10px] uppercase tracking-wider text-neutral-500 dark:text-neutral-400">
              <th className="pb-1 font-medium">Trường</th>
              <th className="pb-1 font-medium">Hiện tại</th>
              <th className="pb-1 font-medium text-emerald-600 dark:text-emerald-400">Đề xuất mới</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-amber-500/10 text-[11px]">
            {proposedKeys.map((key) => {
              const currentVal = proposal.currentValues?.[key];
              const newVal = proposal.proposedValues?.[key];
              return (
                <tr key={key} className="hover:bg-amber-500/5">
                  <td className="py-1 font-mono font-medium text-neutral-700 dark:text-neutral-300">
                    {key}
                  </td>
                  <td className="py-1 text-neutral-500 line-through dark:text-neutral-400">
                    {renderValue(currentVal)}
                  </td>
                  <td className="py-1 font-medium text-emerald-700 dark:text-emerald-300">
                    {renderValue(newVal)}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Nút Phê duyệt / Từ chối */}
      {localStatus === 'PENDING' && (
        <div className="flex items-center justify-end gap-2 border-t border-amber-500/15 bg-white/40 px-3 py-2 dark:bg-neutral-900/40">
          <button
            type="button"
            onClick={handleReject}
            disabled={disabled}
            className="flex items-center gap-1 rounded-lg px-2.5 py-1 text-[11px] font-medium text-neutral-600 transition-colors hover:bg-neutral-200/60 disabled:opacity-50 dark:text-neutral-300 dark:hover:bg-neutral-800"
          >
            <Icon name="x" size={12} />
            Từ chối
          </button>
          <button
            type="button"
            onClick={handleApprove}
            disabled={disabled || timeLeft <= 0}
            className="flex items-center gap-1 rounded-lg bg-emerald-600 px-3 py-1 text-[11px] font-medium text-white shadow-xs transition-colors hover:bg-emerald-700 active:scale-95 disabled:opacity-50 dark:bg-emerald-700 dark:hover:bg-emerald-600"
          >
            <Icon name="check" size={12} />
            Phê duyệt thay đổi
          </button>
        </div>
      )}
    </div>
  );
}
