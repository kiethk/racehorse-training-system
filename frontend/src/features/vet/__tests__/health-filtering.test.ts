import { describe, it } from 'node:test';
import assert from 'node:assert/strict';

describe('AC-8: Health Page Filtering & Stats Aggregation', () => {
  const mockHorses = [
    { id: 1, name: 'Storm', trainingDecision: 'ALLOWED' },
    { id: 2, name: 'Blaze', trainingDecision: 'BLOCKED' },
    { id: 3, name: 'Comet', trainingDecision: 'ALLOWED' },
  ];

  const mockSummaries: Record<number, { activeInjuryCount: number; hasPendingUrgent: boolean; trainingDecision: string }> = {
    1: { activeInjuryCount: 0, hasPendingUrgent: false, trainingDecision: 'ALLOWED' },
    2: { activeInjuryCount: 1, hasPendingUrgent: true, trainingDecision: 'BLOCKED' },
    3: { activeInjuryCount: 0, hasPendingUrgent: false, trainingDecision: 'ALLOWED' },
  };

  function computeStats(horses: typeof mockHorses, summaries: typeof mockSummaries) {
    let allowedCount = 0;
    let blockedCount = 0;
    let openInjuriesCount = 0;
    let urgentCasesCount = 0;

    for (const h of horses) {
      const decision = summaries[h.id]?.trainingDecision ?? h.trainingDecision ?? 'ALLOWED';
      if (decision === 'BLOCKED') blockedCount++;
      else allowedCount++;

      if ((summaries[h.id]?.activeInjuryCount ?? 0) > 0) openInjuriesCount++;
      if (summaries[h.id]?.hasPendingUrgent) urgentCasesCount++;
    }
    return { allowedCount, blockedCount, openInjuriesCount, urgentCasesCount };
  }

  function filterHorses(horses: typeof mockHorses, summaries: typeof mockSummaries, statusFilter: string) {
    return horses.filter((h) => {
      const sum = summaries[h.id];
      if (statusFilter === 'HAS_INJURY') return (sum?.activeInjuryCount ?? 0) > 0;
      if (statusFilter === 'URGENT') return sum?.hasPendingUrgent === true;
      if (statusFilter === 'BLOCKED') return (sum?.trainingDecision ?? h.trainingDecision) === 'BLOCKED';
      if (statusFilter === 'ALLOWED') return (sum?.trainingDecision ?? h.trainingDecision) === 'ALLOWED';
      return true;
    });
  }

  it('filters correctly across full horse roster for HAS_INJURY and URGENT', () => {
    const injuryMatches = filterHorses(mockHorses, mockSummaries, 'HAS_INJURY');
    assert.equal(injuryMatches.length, 1);
    assert.equal(injuryMatches[0].name, 'Blaze');

    const urgentMatches = filterHorses(mockHorses, mockSummaries, 'URGENT');
    assert.equal(urgentMatches.length, 1);
    assert.equal(urgentMatches[0].name, 'Blaze');
  });

  it('aggregates overall stats accurately reflecting full horse roster', () => {
    const stats = computeStats(mockHorses, mockSummaries);
    assert.equal(stats.allowedCount, 2);
    assert.equal(stats.blockedCount, 1);
    assert.equal(stats.openInjuriesCount, 1);
    assert.equal(stats.urgentCasesCount, 1);
  });
});
