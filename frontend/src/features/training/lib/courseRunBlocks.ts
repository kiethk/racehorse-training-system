import { REMOVED_GROUP_LABEL } from './groupLifecycle.ts';
import type { CourseRunSummary, TrainingGroup } from '../types';

/** One group on the Course runs page: its name, current horses and its course runs in display order. */
export interface CourseRunBlock {
  groupId: number | null;
  name: string;
  disbanded: boolean;
  horseCount: number;
  courseRuns: CourseRunSummary[];
}

const isFinished = (courseRun: CourseRunSummary) => courseRun.status === 'COMPLETED' || courseRun.status === 'CANCELLED';

/** Unfinished course runs first, then finished ones; newest first inside each part (a bigger id was created later). */
function courseRunOrder(left: CourseRunSummary, right: CourseRunSummary): number {
  return Number(isFinished(left)) - Number(isFinished(right)) || right.id - left.id;
}

/**
 * One block per group: live groups first in the order given (even with no course run), Disbanded groups last, newest course run first.
 * A Disbanded group is not in `liveGroups`, so its block comes from its course runs; it has no horses.
 */
export function courseRunsByGroup(liveGroups: TrainingGroup[], courseRuns: CourseRunSummary[]): CourseRunBlock[] {
  const live: CourseRunBlock[] = liveGroups.map((group) => ({
    groupId: group.id, name: group.name, disbanded: false, horseCount: group.members.length,
    courseRuns: courseRuns.filter((courseRun) => courseRun.groupId === group.id).sort(courseRunOrder),
  }));
  const liveIds = new Set(liveGroups.map((group) => group.id));

  const closed = new Map<number | null, CourseRunBlock>();
  for (const courseRun of courseRuns) {
    if (courseRun.groupId !== null && liveIds.has(courseRun.groupId)) continue;
    const block = closed.get(courseRun.groupId) ?? {
      groupId: courseRun.groupId, name: courseRun.groupName ?? REMOVED_GROUP_LABEL, disbanded: true, horseCount: 0, courseRuns: [],
    };
    block.courseRuns.push(courseRun);
    closed.set(courseRun.groupId, block);
  }
  const disbanded = [...closed.values()].map((block) => ({ ...block, courseRuns: block.courseRuns.sort(courseRunOrder) }));
  disbanded.sort((left, right) => right.courseRuns[0].id - left.courseRuns[0].id);

  return [...live, ...disbanded];
}
