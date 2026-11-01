import type { GroupPlanSummary, TrainingGroup } from '../types';

/** One group on the Plans page: its name, current horses and its plans in display order. */
export interface PlanBlock {
  groupId: number | null;
  name: string;
  disbanded: boolean;
  horseCount: number;
  plans: GroupPlanSummary[];
}

const isFinished = (plan: GroupPlanSummary) => plan.status === 'COMPLETED' || plan.status === 'CANCELLED';

/** Unfinished plans first, then finished ones; newest first inside each part (a bigger id was created later). */
function planOrder(left: GroupPlanSummary, right: GroupPlanSummary): number {
  return Number(isFinished(left)) - Number(isFinished(right)) || right.id - left.id;
}

/**
 * One block per group: live groups first in the order given (even with no plan), Disbanded groups last, newest plan first.
 * A Disbanded group is not in `liveGroups`, so its block comes from its plans; it has no horses.
 */
export function groupPlansByGroup(liveGroups: TrainingGroup[], plans: GroupPlanSummary[]): PlanBlock[] {
  const live: PlanBlock[] = liveGroups.map((group) => ({
    groupId: group.id, name: group.name, disbanded: false, horseCount: group.members.length,
    plans: plans.filter((plan) => plan.groupId === group.id).sort(planOrder),
  }));
  const liveIds = new Set(liveGroups.map((group) => group.id));

  const closed = new Map<number | null, PlanBlock>();
  for (const plan of plans) {
    if (plan.groupId !== null && liveIds.has(plan.groupId)) continue;
    const block = closed.get(plan.groupId) ?? {
      groupId: plan.groupId, name: plan.groupName ?? 'Deleted group', disbanded: true, horseCount: 0, plans: [],
    };
    block.plans.push(plan);
    closed.set(plan.groupId, block);
  }
  const disbanded = [...closed.values()].map((block) => ({ ...block, plans: block.plans.sort(planOrder) }));
  disbanded.sort((left, right) => right.plans[0].id - left.plans[0].id);

  return [...live, ...disbanded];
}
