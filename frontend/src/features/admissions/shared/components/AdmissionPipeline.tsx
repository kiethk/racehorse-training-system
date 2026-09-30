import { Panel, SectionTitle } from '@/components/ui/Panel';
import type { AdmissionDetailResponse } from '../../types';

function PipelineStep({ label, isDone, isActive, note }: { label: string; isDone: boolean; isActive: boolean; note?: string }) {
  if (isActive) return (
    <div className="rounded border border-[var(--color-primary)] bg-[var(--color-primary-soft)] px-3 py-2 font-medium text-[var(--color-primary)]">
      <span className="mr-1.5">●</span>{label}
      {note && <span className="ml-1.5 text-[10px] font-normal opacity-75">({note})</span>}
    </div>
  );
  if (isDone) return <div className="rounded border border-[var(--color-success)] bg-[var(--color-success-soft)] px-3 py-2 text-[var(--color-success)]"><span className="mr-1.5">✓</span>{label}</div>;
  return <div className="rounded border border-[var(--color-border)] bg-[var(--color-surface-subtle)] px-3 py-2 text-[var(--color-text-muted)]"><span className="mr-1.5 opacity-40">-</span>{label}</div>;
}

export function AdmissionPipeline({ detail }: { detail: AdmissionDetailResponse }) {
  const isPendingRecheck = detail.status === 'PENDING_RECHECK';
  return (
    <Panel padded className="bg-[var(--color-surface)]">
      <SectionTitle>Review pipeline</SectionTitle>
      <div className="mt-4 flex flex-wrap gap-2 text-[12px]">
        <PipelineStep label="Groom Review" isDone={!!detail.groomReviewedAt} isActive={detail.status === 'GROOM_REVIEW'} />
        <PipelineStep label="Waiting for Stall" isDone={!!detail.quarantineStallCode} isActive={detail.status === 'WAITING_FOR_STALL'} />
        <PipelineStep label="Vet Review" isDone={!!detail.vetReviewedAt && !isPendingRecheck} isActive={detail.status === 'VET_REVIEW' || isPendingRecheck} note={isPendingRecheck ? 'Pending Recheck' : undefined} />
        <PipelineStep label="Trainer Review" isDone={!!detail.trainerReviewedAt} isActive={detail.status === 'TRAINER_REVIEW'} />
        <PipelineStep label="Manager Review" isDone={!!detail.managerReviewedAt} isActive={detail.status === 'MANAGER_REVIEW'} />
      </div>
    </Panel>
  );
}
