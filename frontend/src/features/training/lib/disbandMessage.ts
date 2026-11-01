/** Name of a group as shown wherever past work is listed: a Disbanded group keeps its name with a mark. */
export function groupLabel(name: string | null, disbanded: boolean): string {
  if (name === null) return 'Deleted group';
  return disbanded ? `${name} (Disbanded)` : name;
}

/** Plans and not-yet-started Sessions that disbanding a group will cancel. */
export interface DisbandImpact {
  plans: number;
  sessions: number;
}

const count = (n: number, noun: string) => `${n} ${noun}${n === 1 ? '' : 's'}`;

/** Confirmation text for disbanding: says what is cancelled (when anything is) and that the horses leave. */
export function disbandMessage(groupName: string, impact: DisbandImpact): string {
  const horses = 'Its horses leave the group.';
  if (impact.plans === 0) return `Disband ${groupName}? ${horses}`;
  return `Disband ${groupName}? ${count(impact.plans, 'plan')} and ${count(impact.sessions, 'session')} not yet started will be cancelled. ${horses}`;
}

/** A CANCELLED plan, or the plan of a Disbanded group, only shows its history: no cancel or reschedule actions. */
export function isViewOnlyPlan(status: string, groupDisbanded: boolean): boolean {
  return groupDisbanded || status === 'CANCELLED';
}
