import type { PlanStatus } from '../types';

/** Records get ever-growing ids, so a bigger id was created later: newest first. */
export function newestFirst<T extends { id: number }>(left: T, right: T): number {
  return right.id - left.id;
}

/** Plans in progress first, then the ones still to start, then finished ones, and cancelled plans last. */
const PLAN_STATUS_RANK: Record<PlanStatus, number> = { ACTIVE: 0, UPCOMING: 1, COMPLETED: 2, CANCELLED: 3 };

/** Plans of the same status come newest first. */
export function planOrder(
  left: { status: PlanStatus; planId: number },
  right: { status: PlanStatus; planId: number },
): number {
  return PLAN_STATUS_RANK[left.status] - PLAN_STATUS_RANK[right.status] || right.planId - left.planId;
}
