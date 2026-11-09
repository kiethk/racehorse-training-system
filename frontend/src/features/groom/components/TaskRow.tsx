import { Pill } from '@/components/ui';
import { cn } from '@/lib/cn';
import type { TodayTaskItem } from '../types';

/**
 * A Session or vet visit on the checklist, drawn like a routine block: a Session lists every horse the Groom leads in it.
 * The Groom sees it but someone else closes it.
 */
export function TaskRow({ task, horseNames }: { task: TodayTaskItem; horseNames: string[] }) {
  const isCompleted = task.status === 'COMPLETED';

  return (
    <div
      className={cn(
        'rounded-[var(--radius-md)] border p-3 text-sm transition-colors duration-[var(--duration-base)]',
        isCompleted
          ? 'border-[var(--color-border)] bg-[var(--color-surface-muted)]'
          : 'border-[var(--color-border-strong)] bg-[var(--color-surface)] shadow-[var(--shadow-panel)]',
      )}
    >
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex min-w-0 items-start gap-3">
          <div className="font-metric w-16 pt-0.5 text-xs font-bold text-[var(--color-text-primary)]">
            {task.startTime.substring(0, 5)}
            {task.endTime && `–${task.endTime.substring(0, 5)}`}
          </div>
          <div className="min-w-0">
            <div className="flex items-center gap-2">
              <span className="font-semibold text-[var(--color-text-primary)]">{task.title}</span>
              <Pill tone={task.source === 'WORKOUT' ? 'warning' : 'info'} size="sm">{task.source}</Pill>
            </div>
            <ul className="mt-2 flex flex-wrap gap-1.5" aria-label="Horses">
              {horseNames.map((name) => (
                <li
                  key={name}
                  className="inline-flex items-center rounded-full border border-[var(--color-border)] px-2 py-0.5 text-xs text-[var(--color-text-secondary)]"
                >
                  {name}
                </li>
              ))}
            </ul>
            {task.note && <p className="mt-2 text-xs text-[var(--color-text-secondary)]">{task.note}</p>}
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
    </div>
  );
}
