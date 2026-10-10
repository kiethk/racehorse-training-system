'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { groomApi } from '../services/groomService';
import type { TodayTaskItem } from '../types';
import { displayError } from '@/lib/display';
import { toast } from '@/lib/toast';

export function TodayChecklist() {
  const [tasks, setTasks] = useState<TodayTaskItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [completingId, setCompletingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const loadTasks = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await groomApi.getTodayTasks();
      setTasks(data);
    } catch (err) {
      console.error("Unable to load today's tasks:", err);
      setError("Unable to load today's tasks. Please try again later.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadTasks();
  }, [loadTasks]);

  async function handleGenerateRoutine() {
    setGenerating(true);
    try {
      await groomApi.generateRoutine();
      await loadTasks();
    } catch (err) {
      toast.error(displayError(err, 'Failed to generate routine tasks.'));
    } finally {
      setGenerating(false);
    }
  }

  async function handleComplete(refId: number) {
    setCompletingId(refId);
    try {
      await groomApi.completeTask(refId);
      await loadTasks();
    } catch (err) {
      toast.error(displayError(err, 'Failed to complete the task.'));
    } finally {
      setCompletingId(null);
    }
  }

  // Sort tasks by their time of day.
  const sortedTasks = useMemo(() => {
    return [...tasks].sort((a, b) => a.startTime.localeCompare(b.startTime));
  }, [tasks]);

  const stats = useMemo(() => {
    const total = tasks.length;
    const completed = tasks.filter((t) => t.status === 'COMPLETED').length;
    return { total, completed };
  }, [tasks]);

  if (loading) return <ListSkeleton rows={5} />;

  if (error) {
    return (
      <Panel padded>
        <EmptyState icon="alert-triangle" title="Unable to load data" description={error} />
      </Panel>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold text-[var(--color-text-primary)]">
            Today&apos;s Task Checklist
          </h1>
          <p className="text-xs text-[var(--color-text-secondary)]">
            Daily care SOPs, workouts, and veterinary follow-ups for your assigned stalls.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="secondary"
            size="sm"
            onClick={handleGenerateRoutine}
            disabled={generating}
          >
            {generating ? 'Generating routine tasks...' : 'Generate routine tasks'}
          </Button>
          <Button variant="secondary" size="sm" onClick={() => loadTasks()}>
            Refresh
          </Button>
        </div>
      </div>

      {/* Daily progress summary */}
      <Panel padded>
        <div className="flex items-center justify-between text-sm">
          <span className="font-semibold text-[var(--color-text-primary)]">
            Today&apos;s progress: {stats.completed}/{stats.total} items completed
          </span>
          <span className="text-xs text-[var(--color-text-muted)]">
            {stats.total > 0 ? Math.round((stats.completed / stats.total) * 100) : 0}%
          </span>
        </div>
        <div className="mt-2 h-2 w-full rounded-full bg-[var(--color-surface-muted)] overflow-hidden">
          <div
            className="h-full rounded-full bg-[var(--color-primary)] transition-all duration-300"
            style={{
              width: `${stats.total > 0 ? Math.round((stats.completed / stats.total) * 100) : 0}%`,
            }}
          />
        </div>
      </Panel>

      {/* Tasks ordered by time */}
      {sortedTasks.length === 0 ? (
        <Panel padded>
          <EmptyState
            icon="clipboard"
            title="No tasks scheduled for today"
            description="Select 'Generate routine tasks' above to create feeding, stall cleaning, and grooming schedules."
          />
        </Panel>
      ) : (
        <div className="space-y-2">
          {sortedTasks.map((task, idx) => {
            const isCompleted = task.status === 'COMPLETED';
            return (
              <div
                key={`${task.source}-${task.refId}-${idx}`}
                className={`flex flex-wrap items-center justify-between gap-3 rounded-[var(--radius-md)] border p-3 text-sm transition ${
                  isCompleted
                    ? 'border-[var(--color-border)] bg-[var(--color-surface-muted)] opacity-75'
                    : 'border-[var(--color-border-strong)] bg-[var(--color-surface)] shadow-[var(--shadow-panel)]'
                }`}
              >
                <div className="flex items-start gap-3">
                  {/* Time */}
                  <div className="font-metric text-xs font-bold text-[var(--color-text-primary)] w-16 pt-0.5">
                    {task.startTime.substring(0, 5)}
                    {task.endTime && `–${task.endTime.substring(0, 5)}`}
                  </div>

                  {/* Task details */}
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-[var(--color-text-primary)]">
                        {task.title}
                      </span>
                      <Pill
                        tone={
                          task.source === 'SOP'
                            ? 'primary'
                            : task.source === 'WORKOUT'
                            ? 'warning'
                            : 'info'
                        }
                        size="sm"
                      >
                        {task.source}
                      </Pill>
                    </div>

                    <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-[var(--color-text-secondary)]">
                      <span>Horse: <strong>{task.horseName}</strong></span>
                      {task.note && <span>• {task.note}</span>}
                    </div>
                  </div>
                </div>

                {/* Actions depend on whether the task is actionable. */}
                <div className="flex items-center gap-2">
                  {task.actionable && !isCompleted ? (
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={() => handleComplete(task.refId)}
                      disabled={completingId === task.refId}
                    >
                      {completingId === task.refId ? 'Saving...' : 'Complete'}
                    </Button>
                  ) : isCompleted ? (
                    <Pill tone="success" size="sm">
                      Completed
                    </Pill>
                  ) : task.source === 'WORKOUT' ? (
                    <span className="text-xs text-[var(--color-text-muted)] italic">
                      (Managed by Trainer)
                    </span>
                  ) : (
                    <span className="text-xs text-[var(--color-text-muted)] italic">
                      (Managed by Veterinary)
                    </span>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
