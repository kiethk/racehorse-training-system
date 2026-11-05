import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { disbandMessage, groupLabel, isViewOnlyPlan, leaveMessage } from '../lib/groupLifecycle.ts';

describe('leaveMessage', () => {
  it('warns that upcoming workouts are cancelled when the group has a course', () => {
    assert.equal(
      leaveMessage('Thunder', { name: 'Nhom 1', currentPlanStatus: 'ACTIVE' }),
      'Remove Thunder from Nhom 1? Its workouts in sessions not yet started will be cancelled.',
    );
  });

  it('names both groups for a move', () => {
    assert.equal(
      leaveMessage('Thunder', { name: 'Nhom 1', currentPlanStatus: 'UPCOMING' }, { name: 'Nhom 2' }),
      'Move Thunder from Nhom 1 to Nhom 2? Its workouts in sessions not yet started will be cancelled.',
    );
  });

  it('only asks when the group has no course', () => {
    assert.equal(leaveMessage('Thunder', { name: 'Nhom 1', currentPlanStatus: null }), 'Remove Thunder from Nhom 1?');
  });
});

describe('disbandMessage', () => {
  it('only says the horses leave when there is no unfinished plan', () => {
    assert.equal(disbandMessage('Nhom 1', { plans: 0, sessions: 0, hasHistory: true }), 'Disband Nhom 1? Its horses leave the group.');
  });

  it('counts the plans and sessions that will be cancelled', () => {
    assert.equal(
      disbandMessage('Nhom 1', { plans: 2, sessions: 7, hasHistory: true }),
      'Disband Nhom 1? 2 plans and 7 sessions not yet started will be cancelled. Its horses leave the group.',
    );
  });

  it('says a group without any plan is removed entirely', () => {
    assert.equal(
      disbandMessage('Nhom 1', { plans: 0, sessions: 0, hasHistory: false }),
      'Disband Nhom 1? This group has no history and will be removed. Its horses leave the group.',
    );
  });

  it('uses the singular for one plan and one session', () => {
    assert.ok(disbandMessage('A', { plans: 1, sessions: 1, hasHistory: true }).includes('1 plan and 1 session not yet started'));
  });
});

describe('groupLabel', () => {
  it('marks a Disbanded group and keeps live names plain', () => {
    assert.equal(groupLabel('Nhom 1', true), 'Nhom 1 (Disbanded)');
    assert.equal(groupLabel('Nhom 1', false), 'Nhom 1');
    assert.equal(groupLabel(null, false), 'Removed group');
  });
});

describe('isViewOnlyPlan', () => {
  it('is view only for a cancelled plan or a disbanded group', () => {
    assert.equal(isViewOnlyPlan('CANCELLED', false), true);
    assert.equal(isViewOnlyPlan('ACTIVE', true), true);
    assert.equal(isViewOnlyPlan('ACTIVE', false), false);
  });
});
