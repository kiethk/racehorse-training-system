import type { TodayTaskItem } from '../types/index.ts';

/** What one completion action recorded: shown once per block, whichever horses it covered. */
export interface CompletionEvidence {
  completionId: number;
  note: string | null;
  photoUrls: string[];
  /** Horses the action covered, in the block's order. */
  horseNames: string[];
}

/** Routine tasks of one duty at one time, for every horse that has it. */
export interface TaskBlock {
  kind: 'block';
  key: string;
  startTime: string;
  title: string;
  tasks: TodayTaskItem[];
  pending: TodayTaskItem[];
  done: TodayTaskItem[];
  evidence: CompletionEvidence[];
}

/** A workout or vet visit: managed by someone else, shown as its own row. */
export interface SingleTask {
  kind: 'single';
  key: string;
  startTime: string;
  task: TodayTaskItem;
}

export type ChecklistEntry = TaskBlock | SingleTask;

/**
 * Turns the day's items into what the checklist draws. Routine tasks that share a start time and a duty
 * become one block, so a Groom with three horses sees "05:00 Feeding" once instead of three times.
 * Workouts and vet visits stay single rows. Entries are ordered by time, blocks before rows at the same time.
 */
export function groupTasks(items: TodayTaskItem[]): ChecklistEntry[] {
  const blocks = new Map<string, TodayTaskItem[]>();
  const singles: SingleTask[] = [];

  for (const item of items) {
    if (item.source === 'SOP' && item.actionable) {
      const key = `${item.startTime}|${item.title}`;
      blocks.set(key, [...(blocks.get(key) ?? []), item]);
    } else {
      singles.push({ kind: 'single', key: `${item.source}-${item.refId}`, startTime: item.startTime, task: item });
    }
  }

  const blockEntries: TaskBlock[] = [...blocks.entries()].map(([key, tasks]) => {
    const sorted = [...tasks].sort((a, b) => a.horseName.localeCompare(b.horseName));
    return {
      kind: 'block',
      key,
      startTime: sorted[0].startTime,
      title: sorted[0].title,
      tasks: sorted,
      pending: sorted.filter((task) => task.status !== 'COMPLETED'),
      done: sorted.filter((task) => task.status === 'COMPLETED'),
      evidence: evidenceOf(sorted),
    };
  });

  return [...blockEntries, ...singles].sort(
    (a, b) => a.startTime.localeCompare(b.startTime) || (a.kind === b.kind ? 0 : a.kind === 'block' ? -1 : 1),
  );
}

/** The distinct completions of a block that recorded a note or photos. */
function evidenceOf(tasks: TodayTaskItem[]): CompletionEvidence[] {
  const byId = new Map<number, CompletionEvidence>();
  for (const task of tasks) {
    if (task.completionId == null) continue;
    const existing = byId.get(task.completionId);
    if (existing) {
      existing.horseNames.push(task.horseName);
      continue;
    }
    byId.set(task.completionId, {
      completionId: task.completionId,
      note: task.completionNote ?? null,
      photoUrls: task.completionPhotoUrls ?? [],
      horseNames: [task.horseName],
    });
  }
  return [...byId.values()].filter((evidence) => evidence.note || evidence.photoUrls.length > 0);
}
