'use client';

import { useEffect, useMemo, useState } from 'react';
import type { TreatmentPlan } from '../types';
import { treatmentPlanService } from '@/services/treatmentPlanService';
import { isPlanActiveToday } from './isPlanActiveToday';

interface ActiveTreatmentPlansState {
  key: string;
  plans: Map<number, TreatmentPlan>;
  loading: boolean;
}

export function useActiveTreatmentPlans(horseIds: number[]) {
  const ids = useMemo(() => [...new Set(horseIds)].sort((a, b) => a - b), [horseIds]);
  const key = ids.join(',');
  const [state, setState] = useState<ActiveTreatmentPlansState>({ key: '', plans: new Map(), loading: true });

  useEffect(() => {
    let active = true;
    if (ids.length === 0) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setState({ key, plans: new Map(), loading: false });
      return () => { active = false; };
    }
    setState((current) => ({ key, plans: current.key === key ? current.plans : new Map(), loading: true }));
    Promise.all(ids.map(async (id) => [id, await treatmentPlanService.getActiveByHorseId(id)] as const))
      .then((results) => {
        if (!active) return;
        const plans = new Map<number, TreatmentPlan>();
        for (const [id, plan] of results) {
          if (plan && isPlanActiveToday(plan)) plans.set(id, plan);
        }
        setState({ key, plans, loading: false });
      });
    return () => { active = false; };
  }, [ids, key]);

  return state.key === key ? state : { plans: new Map<number, TreatmentPlan>(), loading: true };
}
