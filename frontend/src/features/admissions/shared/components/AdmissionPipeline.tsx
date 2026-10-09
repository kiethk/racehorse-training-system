import { Panel, SectionTitle } from '@/components/ui/Panel';
import { formatDate } from '@/lib/display';
import type { AdmissionDetailResponse } from '../../types';

function PipelineStep({ label, isDone, isActive, note }: { label: string; isDone: boolean; isActive: boolean; note?: string }) {
  if (isActive) return (
    <div className="rounded-[var(--radius-sm)] border border-[var(--color-primary)] bg-[var(--color-primary-soft)] px-3 py-2 font-medium text-[var(--color-primary)]">
      <span className="mr-1.5">●</span>{label}
      {note && <span className="ml-1.5 text-xs font-normal opacity-75">({note})</span>}
    </div>
  );
  if (isDone) return <div className="rounded-[var(--radius-sm)] border border-[var(--color-success)] bg-[var(--color-success-soft)] px-3 py-2 text-[var(--color-success)]"><span className="mr-1.5">✓</span>{label}</div>;
  return <div className="rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] px-3 py-2 text-[var(--color-text-muted)]"><span className="mr-1.5 opacity-40">-</span>{label}</div>;
}

type PipelineDetail = Pick<AdmissionDetailResponse, 'status'> & Partial<Pick<AdmissionDetailResponse,
  'groomReviewedAt' | 'quarantineStallCode' | 'arrivalDeadlineAt' | 'arrivedAt' | 'vetReviewedAt' | 'trainerReviewedAt' | 'managerReviewedAt'>>;

export function AdmissionPipeline({ detail }: { detail: PipelineDetail }) {
  const isRejected = detail.status === 'REJECTED';
  const isApproved = detail.status === 'APPROVED';
  // Owner responses omit timestamps. Infer only transitions proven by the current status.
  const stages = ['GROOM_REVIEW', 'WAITING_FOR_STALL', 'WAITING_FOR_ARRIVAL', 'ARRIVAL_EXPIRED', 'VET_REVIEW', 'TRAINER_REVIEW', 'MANAGER_REVIEW', 'APPROVED'];
  const stage = stages.indexOf(detail.status);
  const done = (value: string | null | undefined, threshold: number) => value === undefined ? stage >= threshold : Boolean(value);
  return (
    <Panel padded>
      <SectionTitle>Review pipeline</SectionTitle>
      {isRejected && (
        <p role="status" className="mt-3 rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] px-3 py-2 text-sm font-medium text-[var(--color-danger)]">
          This application has been rejected.
        </p>
      )}
      {isApproved && (
        <p role="status" className="mt-3 rounded-[var(--radius-sm)] border border-[var(--color-success)] bg-[var(--color-success-soft)] px-3 py-2 text-sm font-medium text-[var(--color-success)]">
          This application has been approved.
        </p>
      )}
      <div className="mt-4 flex flex-wrap gap-2 text-xs">
        <PipelineStep label="Groom Review" isDone={done(detail.groomReviewedAt, 1)} isActive={detail.status === 'GROOM_REVIEW'} />
        <PipelineStep label="Waiting for Stall" isDone={done(detail.quarantineStallCode, 2)} isActive={detail.status === 'WAITING_FOR_STALL'} />
        <PipelineStep label="Waiting for Arrival" isDone={done(detail.arrivedAt, 3)} isActive={detail.status === 'WAITING_FOR_ARRIVAL'} note={detail.arrivalDeadlineAt ? `by ${formatDate(detail.arrivalDeadlineAt)}` : undefined} />
        <PipelineStep label="Arrival Expired" isDone={detail.status === 'ARRIVAL_EXPIRED'} isActive={detail.status === 'ARRIVAL_EXPIRED'} />
        <PipelineStep label="Vet Review" isDone={done(detail.vetReviewedAt, 5)} isActive={detail.status === 'VET_REVIEW'} />
        <PipelineStep label="Trainer Review" isDone={done(detail.trainerReviewedAt, 6)} isActive={detail.status === 'TRAINER_REVIEW'} />
        <PipelineStep label="Manager Review" isDone={done(detail.managerReviewedAt, 7)} isActive={detail.status === 'MANAGER_REVIEW'} />
      </div>
    </Panel>
  );
}
