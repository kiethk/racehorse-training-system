import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { disbandMessage, groupLabel } from '../lib/disbandMessage.ts';

describe('disbandMessage', () => {
  it('only says the horses leave when there is no unfinished plan', () => {
    assert.equal(disbandMessage('Nhom 1', { plans: 0, sessions: 0 }), 'Disband Nhom 1? Its horses leave the group.');
  });

  it('counts the plans and sessions that will be cancelled', () => {
    assert.equal(
      disbandMessage('Nhom 1', { plans: 2, sessions: 7 }),
      'Disband Nhom 1? 2 plans and 7 sessions not yet started will be cancelled. Its horses leave the group.',
    );
  });

  it('uses the singular for one plan and one session', () => {
    assert.ok(disbandMessage('A', { plans: 1, sessions: 1 }).includes('1 plan and 1 session not yet started'));
  });
});

describe('groupLabel', () => {
  it('marks a Disbanded group and keeps live names plain', () => {
    assert.equal(groupLabel('Nhom 1', true), 'Nhom 1 (Disbanded)');
    assert.equal(groupLabel('Nhom 1', false), 'Nhom 1');
    assert.equal(groupLabel(null, false), 'Deleted group');
  });
});
