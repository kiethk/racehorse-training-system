import { Button, Icon, Pill, ProtectedImage } from '@/components/ui';
import { cn } from '@/lib/cn';
import type { TaskBlock as TaskBlockData } from '../lib/groupTasks';

/**
 * One routine duty at one time for every horse that has it, completed in one action. A block of one horse
 * looks the same, so the checklist stays uniform.
 */
export function TaskBlock({ block, onComplete }: { block: TaskBlockData; onComplete: (block: TaskBlockData) => void }) {
  const finished = block.pending.length === 0;

  return (
    <div
      className={cn(
        'rounded-[var(--radius-md)] border p-3 text-sm transition-colors duration-[var(--duration-base)]',
        finished
          ? 'border-[var(--color-border)] bg-[var(--color-surface-muted)]'
          : 'border-[var(--color-border-strong)] bg-[var(--color-surface)] shadow-[var(--shadow-panel)]',
      )}
    >
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex min-w-0 items-start gap-3">
          <div className="font-metric w-16 pt-0.5 text-xs font-bold text-[var(--color-text-primary)]">
            {block.startTime.substring(0, 5)}
          </div>
          <div className="min-w-0">
            <div className="flex items-center gap-2">
              <span className="font-semibold text-[var(--color-text-primary)]">{block.title}</span>
              <Pill tone="primary" size="sm">SOP</Pill>
              <span className="text-xs text-[var(--color-text-muted)]">
                {block.done.length}/{block.tasks.length} done
              </span>
            </div>
            <ul className="mt-2 flex flex-wrap gap-1.5" aria-label="Horses">
              {block.tasks.map((task) => {
                const done = task.status === 'COMPLETED';
                return (
                  <li
                    key={task.refId}
                    className={cn(
                      'inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-xs',
                      done
                        ? 'border-[var(--color-success)] bg-[var(--color-success-soft)] text-[var(--color-success)]'
                        : 'border-[var(--color-border)] text-[var(--color-text-secondary)]',
                    )}
                  >
                    {done && <Icon name="check" size={12} />}
                    {task.horseName}
                  </li>
                );
              })}
            </ul>
          </div>
        </div>

        {finished ? (
          <Pill tone="success" size="sm">Completed</Pill>
        ) : (
          <Button variant="primary" size="sm" icon="check" onClick={() => onComplete(block)}>
            {block.pending.length > 1 ? `Complete all (${block.pending.length})` : 'Complete'}
          </Button>
        )}
      </div>

      {block.evidence.length > 0 && (
        <div className="mt-3 space-y-3 border-t border-[var(--color-border)] pt-3 sm:ml-[4.75rem]">
          {block.evidence.map((evidence) => (
            <div key={evidence.completionId} className="space-y-2">
              {block.evidence.length > 1 && (
                <p className="text-xs text-[var(--color-text-muted)]">{evidence.horseNames.join(', ')}</p>
              )}
              {evidence.note && (
                <p className="whitespace-pre-wrap break-words text-xs text-[var(--color-text-primary)]">
                  <span className="font-medium">Note: </span>
                  {evidence.note}
                </p>
              )}
              {evidence.photoUrls.length > 0 && (
                <ul className="flex flex-wrap gap-2" aria-label="Photos">
                  {evidence.photoUrls.map((url, index) => (
                    <li key={url} className="w-28">
                      <ProtectedImage
                        src={url}
                        alt={`Photo ${index + 1} for ${block.title} at ${block.startTime.substring(0, 5)}`}
                        openOnClick
                        className="h-20 w-full object-cover"
                      />
                    </li>
                  ))}
                </ul>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
