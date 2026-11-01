/** Shown in place of a group whose row no longer exists. */
export const REMOVED_GROUP_LABEL = 'Removed group';

/** "1 horse", "2 horses". */
export function plural(n: number, noun: string): string {
  return `${n} ${noun}${n === 1 ? '' : 's'}`;
}

/** Name of a group as shown wherever past work is listed: a Disbanded group keeps its name with a mark. */
export function groupLabel(name: string | null, disbanded: boolean): string {
  if (name === null) return REMOVED_GROUP_LABEL;
  return disbanded ? `${name} (Disbanded)` : name;
}

/** Plans and not-yet-started Sessions that disbanding a group will cancel. */
export interface DisbandImpact {
  plans: number;
  sessions: number;
  /** Whether the group has any plan at all, finished ones included; without one the group is removed entirely. */
  hasHistory: boolean;
}

/** Confirmation text for disbanding: says what is cancelled (when anything is) and that the horses leave. */
export function disbandMessage(groupName: string, impact: DisbandImpact): string {
  const horses = 'Its horses leave the group.';
  if (!impact.hasHistory) return `Disband ${groupName}? This group has no history and will be removed. ${horses}`;
  if (impact.plans === 0) return `Disband ${groupName}? ${horses}`;
  return `Disband ${groupName}? ${plural(impact.plans, 'plan')} and ${plural(impact.sessions, 'session')} not yet started will be cancelled. ${horses}`;
}

/** A CANCELLED plan, or the plan of a Disbanded group, only shows its history: no cancel or reschedule actions. */
export function isViewOnlyPlan(status: string, groupDisbanded: boolean): boolean {
  return groupDisbanded || status === 'CANCELLED';
}
