'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { groomApi } from '../services/groomService';
import { groupTasks, type TaskBlock as TaskBlockData } from '../lib/groupTasks';
import type { TodayTaskItem } from '../types';
import { CompleteTaskDialog } from './CompleteTaskDialog';
import { TaskBlock } from './TaskBlock';
import { TaskRow } from './TaskRow';
import { displayError } from '@/lib/display';
import { toast } from '@/lib/toast';

export function TodayChecklist() {
  const [tasks, setTasks] = useState<TodayTaskItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  /** The block whose completion dialog is open. */
  const [completingBlock, setCompletingBlock] = useState<TaskBlockData | null>(null);
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

  // Routine tasks of one duty at one time become one block; workouts and vet visits stay single rows.
  const entries = useMemo(() => groupTasks(tasks), [tasks]);

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
      {entries.length === 0 ? (
        <Panel padded>
          <EmptyState
            icon="clipboard"
            title="No tasks scheduled for today"
            description="Select 'Generate routine tasks' above to create feeding, stall cleaning, and grooming schedules."
          />
        </Panel>
      ) : (
        <div className="space-y-2">
          {entries.map((entry) =>
            entry.kind === 'block'
              ? <TaskBlock key={entry.key} block={entry} onComplete={setCompletingBlock} />
              : <TaskRow key={entry.key} task={entry.task} />,
          )}
        </div>
      )}

      <CompleteTaskDialog
        block={completingBlock}
        onClose={() => setCompletingBlock(null)}
        onCompleted={async () => {
          toast.success('Tasks completed');
          await loadTasks();
        }}
      />
    </div>
  );
}
