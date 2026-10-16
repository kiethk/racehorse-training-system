import { Pill } from '@/components/ui';
import type { TodayTaskItem } from '../types';

/** A workout or vet visit on the checklist. The Groom sees it but someone else closes it. */
export function TaskRow({ task }: { task: TodayTaskItem }) {
  const isCompleted = task.status === 'COMPLETED';

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-3 text-sm">
      <div className="flex items-start gap-3">
        <div className="font-metric w-16 pt-0.5 text-xs font-bold text-[var(--color-text-primary)]">
          {task.startTime.substring(0, 5)}
          {task.endTime && `–${task.endTime.substring(0, 5)}`}
        </div>
        <div>
          <div className="flex items-center gap-2">
            <span className="font-semibold text-[var(--color-text-primary)]">{task.title}</span>
            <Pill tone={task.source === 'WORKOUT' ? 'warning' : 'info'} size="sm">{task.source}</Pill>
          </div>
          <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-[var(--color-text-secondary)]">
            <span>Horse: <strong>{task.horseName}</strong></span>
            {task.note && <span>• {task.note}</span>}
          </div>
        </div>
      </div>

      {isCompleted ? (
        <Pill tone="success" size="sm">Completed</Pill>
      ) : (
        <span className="text-xs italic text-[var(--color-text-muted)]">
          {task.source === 'WORKOUT' ? '(Managed by Trainer)' : '(Managed by Veterinary)'}
        </span>
      )}
    </div>
  );
}
