import { Icon } from '@/components/ui';
import type { Horse } from '@/features/stable/types';
import { cn } from '@/lib/cn';

/** Displays the veterinary decision only; does not infer admission or scheduling eligibility. */
export function HorseTrainingClearance({ horse }: { horse: Horse }) {
  const blocked = horse.trainingDecision === 'BLOCKED';
  const allowed = horse.trainingDecision === 'ALLOWED';

  return (
    <div className="min-w-0 space-y-2 border-t border-[var(--color-border)] pt-4 lg:border-l lg:border-t-0 lg:pl-6 lg:pt-0">
      <p className="text-xs font-medium text-[var(--color-text-muted)]">Veterinary clearance</p>
      <div className="flex items-center gap-2.5">
        <span className={cn(
          'flex h-9 w-9 shrink-0 items-center justify-center rounded-full',
          blocked ? 'bg-[var(--color-danger-soft)] text-[var(--color-danger)]'
            : allowed ? 'bg-[var(--color-success-soft)] text-[var(--color-success)]'
              : 'bg-[var(--color-surface-muted)] text-[var(--color-text-muted)]',
        )}>
          <Icon name={blocked ? 'lock' : allowed ? 'check' : 'stethoscope'} size={18} />
        </span>
        <p className="text-sm font-semibold text-[var(--color-text-primary)]">
          {blocked ? 'Training blocked' : allowed ? 'Allowed by vet' : 'Not recorded'}
        </p>
      </div>
      {blocked && horse.trainingDecisionReason?.trim() && (
        <p className="break-words text-xs leading-relaxed text-[var(--color-text-secondary)]">
          {horse.trainingDecisionReason.trim()}
        </p>
      )}
    </div>
  );
}
