import type { CourseRunStatus } from '../types';

/** Records get ever-growing ids, so a bigger id was created later: newest first. */
export function newestFirst<T extends { id: number }>(left: T, right: T): number {
  return right.id - left.id;
}

/** Course runs in progress first, then the ones still to start, then finished ones, and cancelled course runs last. */
const COURSE_RUN_STATUS_RANK: Record<CourseRunStatus, number> = { ACTIVE: 0, UPCOMING: 1, COMPLETED: 2, CANCELLED: 3 };

/** Course runs of the same status come newest first. */
export function courseRunOrder(
  left: { status: CourseRunStatus; courseRunId: number },
  right: { status: CourseRunStatus; courseRunId: number },
): number {
  return COURSE_RUN_STATUS_RANK[left.status] - COURSE_RUN_STATUS_RANK[right.status] || right.courseRunId - left.courseRunId;
}
