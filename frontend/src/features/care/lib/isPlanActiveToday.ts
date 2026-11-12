import { formatLocalDateIso } from '../../../lib/display.ts';
import type { TreatmentPlan } from '../types';

export function isPlanActiveToday(plan: TreatmentPlan, today = formatLocalDateIso()): boolean {
  return plan.status === 'ACTIVE' && (!plan.endDate || plan.endDate >= today);
}
