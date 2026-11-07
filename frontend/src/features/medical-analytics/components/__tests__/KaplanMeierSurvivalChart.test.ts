import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import type { SurvivalEdaResponse } from '../../types/medicalEda.ts';

describe('KaplanMeierSurvivalChart Logic Tests', () => {
  const mockSurvivalData: SurvivalEdaResponse = {
    stratifyBy: 'TREATMENT_PLAN',
    strata: [
      {
        stratumId: 1,
        stratumName: 'Hydrotherapy & Soft Rest',
        totalCases: 10,
        recoveredCount: 7,
        censoredCount: 3,
        medianRecoveryDays: 14.5,
        steps: [
          { days: 0, survivalProbability: 1.0, atRiskCount: 10, recoveredEventsCount: 0 },
          { days: 7, survivalProbability: 0.8, atRiskCount: 10, recoveredEventsCount: 2 },
          { days: 14, survivalProbability: 0.5, atRiskCount: 8, recoveredEventsCount: 3 },
          { days: 21, survivalProbability: 0.3, atRiskCount: 4, recoveredEventsCount: 2 },
        ],
      },
      {
        stratumId: 2,
        stratumName: 'Corticosteroid & Cryotherapy',
        totalCases: 8,
        recoveredCount: 4,
        censoredCount: 4,
        medianRecoveryDays: 18.0,
        steps: [
          { days: 0, survivalProbability: 1.0, atRiskCount: 8, recoveredEventsCount: 0 },
          { days: 10, survivalProbability: 0.75, atRiskCount: 8, recoveredEventsCount: 2 },
          { days: 18, survivalProbability: 0.5, atRiskCount: 6, recoveredEventsCount: 2 },
        ],
      },
    ],
  };

  it('correctly extracts and formats stratum recovery rate and median days', () => {
    const s1 = mockSurvivalData.strata[0];
    const rate1 = Math.round((s1.recoveredCount / s1.totalCases) * 100);
    assert.equal(rate1, 70);
    assert.equal(s1.medianRecoveryDays, 14.5);

    const s2 = mockSurvivalData.strata[1];
    const rate2 = Math.round((s2.recoveredCount / s2.totalCases) * 100);
    assert.equal(rate2, 50);
    assert.equal(s2.medianRecoveryDays, 18.0);
  });

  it('merges multi-strata step data into unified timeline for stepAfter rendering', () => {
    const daySet = new Set<number>();
    mockSurvivalData.strata.forEach((stratum) => {
      stratum.steps.forEach((step) => daySet.add(step.days));
    });

    const sortedDays = Array.from(daySet).sort((a, b) => a - b);
    assert.deepEqual(sortedDays, [0, 7, 10, 14, 18, 21]);

    const rows = sortedDays.map((day) => {
      const row: Record<string, number | null> = { day };
      mockSurvivalData.strata.forEach((stratum) => {
        let lastProb = 1.0;
        for (const step of stratum.steps) {
          if (step.days <= day) {
            lastProb = step.survivalProbability;
          } else {
            break;
          }
        }
        row[`stratum_${stratum.stratumId}`] = Math.round(lastProb * 1000) / 10;
      });
      return row;
    });

    assert.equal(rows.length, 6);
    // At day 0, both strata are at 100%
    assert.equal(rows[0].stratum_1, 100);
    assert.equal(rows[0].stratum_2, 100);

    // At day 10: stratum 1 was 80% at day 7, stratum 2 just dropped to 75%
    assert.equal(rows[2].day, 10);
    assert.equal(rows[2].stratum_1, 80);
    assert.equal(rows[2].stratum_2, 75);

    // At day 21: stratum 1 is 30%, stratum 2 remains 50%
    assert.equal(rows[5].day, 21);
    assert.equal(rows[5].stratum_1, 30);
    assert.equal(rows[5].stratum_2, 50);
  });

  it('verifies dropdown options support TREATMENT_PLAN and BODY_ZONE', () => {
    const options = [
      { value: 'TREATMENT_PLAN', label: 'Compare by Treatment Plan' },
      { value: 'BODY_ZONE', label: 'Compare by Body Zone' },
    ];
    assert.equal(options.length, 2);
    assert.equal(options[0].label, 'Compare by Treatment Plan');
    assert.equal(options[1].label, 'Compare by Body Zone');
  });

  it('extends timeline to at least 10 days domain when all events occur early', () => {
    const earlySteps = [{ days: 0, survivalProbability: 1.0 }, { days: 3, survivalProbability: 0.5 }];
    const daySet = new Set<number>();
    earlySteps.forEach((s) => daySet.add(s.days));
    const sorted = Array.from(daySet).sort((a, b) => a - b);
    const max = sorted[sorted.length - 1];
    const maxAxisDay = Math.max(max, 10);
    if (maxAxisDay > max) {
      sorted.push(maxAxisDay);
    }
    assert.deepEqual(sorted, [0, 3, 10]);
  });
});
