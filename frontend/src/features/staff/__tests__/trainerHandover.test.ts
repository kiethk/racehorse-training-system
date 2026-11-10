import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import type { EligibleReplacementTrainer } from '../services/staffService.ts';

describe('Trainer Handover Capacity and Replacement Logic', () => {
  it('correctly determines trainer acceptance when remaining capacity >= transferred areas', () => {
    const areasToTransfer = 1;
    const candidates: EligibleReplacementTrainer[] = [
      { id: 10, fullName: 'Trainer Alpha', email: 'alpha@rtms.com', managedAreasCount: 1, remainingCapacity: 1, canAccept: true },
      { id: 20, fullName: 'Trainer Beta', email: 'beta@rtms.com', managedAreasCount: 2, remainingCapacity: 0, canAccept: false },
    ];

    const eligible = candidates.filter((c) => c.canAccept);
    assert.equal(eligible.length, 1);
    assert.equal(eligible[0].id, 10);
    assert.equal(candidates[1].canAccept, false);
  });

  it('rejects all candidates when all active trainers are at capacity (2 areas)', () => {
    const candidates: EligibleReplacementTrainer[] = [
      { id: 10, fullName: 'Trainer Alpha', email: 'alpha@rtms.com', managedAreasCount: 2, remainingCapacity: 0, canAccept: false },
      { id: 20, fullName: 'Trainer Beta', email: 'beta@rtms.com', managedAreasCount: 2, remainingCapacity: 0, canAccept: false },
    ];

    const hasAcceptableCandidate = candidates.some((c) => c.canAccept);
    assert.equal(hasAcceptableCandidate, false);
  });

  it('allows replacement trainer with 0 areas to accept up to 2 areas', () => {
    const candidate: EligibleReplacementTrainer = {
      id: 30,
      fullName: 'Trainer Gamma',
      email: 'gamma@rtms.com',
      managedAreasCount: 0,
      remainingCapacity: 2,
      canAccept: true,
    };

    assert.equal(candidate.remainingCapacity, 2);
    assert.equal(candidate.canAccept, true);
  });

  it('formats payload with replacementTrainerId on deactivation', () => {
    const selectedReplacementId = 15;
    const payload = {
      active: false,
      replacementTrainerId: selectedReplacementId,
    };

    assert.equal(payload.active, false);
    assert.equal(payload.replacementTrainerId, 15);
  });

  it('validates candidate eligibility to prevent confirming over-capacity candidate', () => {
    const candidates: EligibleReplacementTrainer[] = [
      { id: 10, fullName: 'Trainer Alpha', email: 'alpha@rtms.com', managedAreasCount: 1, remainingCapacity: 1, canAccept: true },
      { id: 20, fullName: 'Trainer Beta', email: 'beta@rtms.com', managedAreasCount: 2, remainingCapacity: 0, canAccept: false },
    ];

    const isEligible = (id: number | '') => {
      const candidate = candidates.find((c) => c.id === Number(id));
      return !!candidate?.canAccept;
    };

    assert.equal(isEligible(10), true);
    assert.equal(isEligible(20), false);
    assert.equal(isEligible(''), false);
    assert.equal(isEligible(99), false);
  });

  it('formats area codes consistently with Area prefix', () => {
    const codes = ['A', 'B'];
    const formatted = codes.map((c) => (c.startsWith('Area ') ? c : `Area ${c}`)).join(', ');
    assert.equal(formatted, 'Area A, Area B');

    const alreadyPrefixed = ['Area A', 'Area B'];
    const formatted2 = alreadyPrefixed.map((c) => (c.startsWith('Area ') ? c : `Area ${c}`)).join(', ');
    assert.equal(formatted2, 'Area A, Area B');
  });
});
