'use client';

import { useRouter } from 'next/navigation';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Icon, IconButton, Panel, SectionTitle } from '@/components/ui';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { groomApi } from '../services/groomService';
import { groupTasks, type TaskBlock as TaskBlockData } from '../lib/groupTasks';
import type { TodayTaskItem } from '../types';
import { CompleteTaskDialog } from './CompleteTaskDialog';
import { TaskBlock } from './TaskBlock';
import { TaskRow } from './TaskRow';
import { displayError, formatDate } from '@/lib/display';
import { toast } from '@/lib/toast';
import { useActiveTreatmentPlans } from '@/features/care/lib/useActiveTreatmentPlans';
import { MedicationInstructionModal } from '@/features/care/components/MedicationInstructionModal';

/** `horseId` narrows the checklist to one horse (opened from its stall on the Stable screen). */
export function TodayChecklist({ horseId }: { horseId?: number }) {
  const router = useRouter();
  const [allTasks, setTasks] = useState<TodayTaskItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  /** The block whose completion dialog is open. */
  const [completingBlock, setCompletingBlock] = useState<TaskBlockData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selectedHorseId, setSelectedHorseId] = useState<number | null>(null);

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

  const tasks = useMemo(
    () => (horseId != null ? allTasks.filter((task) => task.horseId === horseId) : allTasks),
    [allTasks, horseId],
  );
  const filterHorseName = horseId != null ? allTasks.find((task) => task.horseId === horseId)?.horseName ?? `Horse #${horseId}` : null;

  // Routine tasks of one duty at one time become one block; workouts and vet visits stay single rows.
  const entries = useMemo(() => groupTasks(tasks), [tasks]);
  const horseIds = useMemo(() => [...new Set(tasks.map((task) => task.horseId))], [tasks]);
  const { plans, loading: plansLoading } = useActiveTreatmentPlans(horseIds);
  const taskByHorse = useMemo(() => new Map(tasks.map((task) => [task.horseId, task])), [tasks]);
  const selectedTask = selectedHorseId == null ? null : taskByHorse.get(selectedHorseId) ?? null;
  const selectedPlan = selectedHorseId == null ? null : plans.get(selectedHorseId) ?? null;

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
          {filterHorseName && (
            <span className="mt-2 inline-flex items-center gap-1 rounded-full border border-[var(--color-border)] bg-[var(--color-surface-muted)] py-0.5 pl-3 pr-0.5 text-xs font-medium text-[var(--color-text-primary)]">
              Horse: {filterHorseName}
              <IconButton icon="x" label="Show tasks for every horse" size="sm" variant="tertiary"
                onClick={() => router.replace('/groom/tasks')} />
            </span>
          )}
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

      {/* Horses on an active treatment plan: open the vet's instructions without leaving the checklist */}
      {!plansLoading && plans.size > 0 && (
        <Panel padded tone="warning" className="border-2 bg-[var(--color-warning-soft)]">
          <div className="flex items-center gap-2 text-[var(--color-warning)]">
            <Icon name="pill" size={18} />
            <SectionTitle>Horses on treatment</SectionTitle>
          </div>
          <div className="mt-3 divide-y divide-[var(--color-warning)]">
            {[...plans.entries()].map(([id, plan]) => {
              const task = taskByHorse.get(id);
              if (!task) return null;
              return <div key={id} className="flex flex-wrap items-center justify-between gap-3 py-2 first:pt-0 last:pb-0">
                <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm">
                  <span className="font-medium text-[var(--color-text-primary)]">{task.horseName}</span>
                  {task.stallCode && <span className="text-[var(--color-text-secondary)]">{task.stallCode}</span>}
                  <span className="text-[var(--color-text-secondary)]">{plan.endDate ? `Until ${formatDate(plan.endDate)}` : 'Ongoing'}</span>
                </div>
                <Button type="button" variant="secondary" size="sm" icon="pill" onClick={() => setSelectedHorseId(id)}>View instructions</Button>
              </div>;
            })}
          </div>
        </Panel>
      )}

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
              ? <TaskBlock key={entry.key} block={entry} onComplete={setCompletingBlock} plans={plans} onViewInstructions={setSelectedHorseId} />
              : <TaskRow key={entry.key} task={entry.task} plans={plans} onViewInstructions={setSelectedHorseId} />,
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
      {selectedTask && selectedPlan && <MedicationInstructionModal
        open
        onClose={() => setSelectedHorseId(null)}
        horseName={selectedTask.horseName}
        stallCode={selectedTask.stallCode ?? '—'}
        diagnosis={selectedPlan.diagnosis}
        veterinarianName={selectedPlan.veterinarianName}
        instructions={selectedPlan.instructions}
        endDate={selectedPlan.endDate ? formatDate(selectedPlan.endDate) : null}
      />}
    </div>
  );
}
