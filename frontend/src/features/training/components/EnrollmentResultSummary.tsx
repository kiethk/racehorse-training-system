'use client';

import { useMemo } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/Button';
import { Panel, SectionTitle } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import type { Horse } from '@/features/stable/types';
import type { HorseTrainingPlanDetailResponse, PlanWorkoutItemResponse } from '../types';

/**
 * Màn tóm tắt sau khi ghi danh nhóm.
 *
 * VÌ SAO CẦN:
 * Ghi danh 7 con sẽ tạo 7 kế hoạch. Trước đây frontend nhảy thẳng tới
 * kế hoạch đầu tiên, vứt đi 6 kết quả còn lại — trong khi backend đã trả
 * về đầy đủ cả 7 kèm lotId từng buổi.
 *
 * Quan trọng hơn: nhóm có thể bị TÁCH sang nhiều lot vì hai lý do
 * (vượt sức chứa BR-10, hoặc hai con cùng Groom BR-09). Đó là hành vi
 * ĐÚNG, nhưng nếu không nói ra thì Trainer sẽ tưởng cơ chế ghép nhóm hỏng.
 * Màn này chính là chỗ demo cơ chế lot rõ nhất.
 */
export function EnrollmentResultSummary({
  plans,
  horses,
  onDone,
}: {
  plans: HorseTrainingPlanDetailResponse[];
  horses: Horse[];
  onDone: () => void;
}) {
  const horseNameById = useMemo(
    () => new Map(horses.map((h) => [h.id, h.name] as const)),
    [horses],
  );

  /**
   * Gom buổi tập của BUỔI ĐẦU TIÊN theo lot.
   *
   * Chỉ lấy buổi đầu vì đó đã đủ để thấy nhóm bị tách hay không — các buổi
   * sau lặp lại cùng cấu trúc (cùng nhóm, cùng Groom, cùng sức chứa).
   * Hiện cả 12 buổi sẽ thành một bức tường số liệu không ai đọc.
   */
  const firstSessionByLot = useMemo(() => {
    const byLot = new Map<number, { item: PlanWorkoutItemResponse; horseId: number }[]>();

    plans.forEach((p) => {
      const first = p.workouts[0];
      if (!first) return;
      const bucket = byLot.get(first.lotId) ?? [];
      bucket.push({ item: first, horseId: p.plan.horseId });
      byLot.set(first.lotId, bucket);
    });

    return [...byLot.entries()].sort(
      (a, b) => (a[1][0].item.startTime > b[1][0].item.startTime ? 1 : -1),
    );
  }, [plans]);

  const splitIntoMultipleLots = firstSessionByLot.length > 1;
  const firstDate = plans[0]?.workouts[0]?.lotDate;

  return (
    <div className="space-y-4">
      <Panel padded>
        <div className="flex items-start gap-3">
          <span className="text-[22px] leading-none">✅</span>
          <div>
            <SectionTitle>Đã tạo {plans.length} kế hoạch huấn luyện</SectionTitle>
            <p className="mt-1 text-[12px] text-[var(--color-text-secondary)]">
              Mỗi chiến mã có một kế hoạch riêng để ghi nhận chỉ số và nhận xét.
            </p>
          </div>
        </div>
      </Panel>

      <Panel padded>
        <SectionTitle>
          Buổi tập đầu tiên{firstDate ? ` — ${firstDate}` : ''}
        </SectionTitle>

        <div className="mt-3 space-y-2">
          {firstSessionByLot.map(([lotId, entries]) => {
            const sample = entries[0].item;
            return (
              <div
                key={lotId}
                className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-3"
              >
                <div className="flex flex-wrap items-center gap-2 text-[12px]">
                  <Pill tone="info" size="sm">Lot #{lotId}</Pill>
                  <strong>{sample.startTime} – {sample.endTime}</strong>
                  <span className="text-[var(--color-text-secondary)]">
                    {sample.subjectName}
                  </span>
                  <Pill
                    tone={
                      sample.lotOccupancy && sample.lotOccupancy >= 6 ? 'warning' : 'success'
                    }
                    size="sm"
                  >
                    {sample.lotOccupancy ?? entries.length} chiến mã
                  </Pill>
                </div>
                <div className="mt-1.5 text-[11px] text-[var(--color-text-secondary)]">
                  {entries
                    .map((e) => horseNameById.get(e.horseId) ?? `#${e.horseId}`)
                    .join(' · ')}
                </div>
              </div>
            );
          })}
        </div>

        {splitIntoMultipleLots && (
          <div className="mt-3 rounded-[var(--radius-md)] bg-[var(--color-info-soft)] p-3 text-[11px] text-[var(--color-info)]">
            Nhóm được chia thành {firstSessionByLot.length} buổi tập khác giờ, do
            mỗi buổi chỉ nhận tối đa 6 chiến mã và mỗi Groom chỉ dắt được một con
            mỗi buổi.
          </div>
        )}
      </Panel>

      <Panel padded>
        <SectionTitle>Danh sách kế hoạch vừa tạo</SectionTitle>
        <ul className="mt-2 space-y-1.5">
          {plans.map((p) => (
            <li
              key={p.plan.id}
              className="flex flex-wrap items-center justify-between gap-2 rounded-[var(--radius-md)] border border-[var(--color-border)] p-2.5 text-[12px]"
            >
              <div>
                <strong className="text-[var(--color-text-primary)]">
                  {horseNameById.get(p.plan.horseId) ?? `Chiến mã #${p.plan.horseId}`}
                </strong>
                <span className="ml-2 text-[var(--color-text-muted)]">
                  {p.workouts.length} buổi · {p.plan.startDate} → {p.plan.endDate}
                </span>
                <Pill
                  tone={p.plan.status === 'ACTIVE' ? 'success' : 'info'}
                  size="sm"
                >
                  {p.plan.status}
                </Pill>
              </div>
              <Link
                href={`/trainer/plans/${p.plan.id}`}
                className="text-[var(--color-primary)] underline"
              >
                Xem chi tiết
              </Link>
            </li>
          ))}
        </ul>

        <div className="mt-4 flex gap-2">
          <Button variant="primary" onClick={onDone}>
            Về danh sách khoá học
          </Button>
        </div>
      </Panel>
    </div>
  );
}
