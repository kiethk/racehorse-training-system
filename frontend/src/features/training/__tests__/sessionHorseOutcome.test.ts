import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { sessionHorseOutcome } from '../lib/status.ts';

describe('sessionHorseOutcome', () => {
  it('shows a horse that has not run yet, and one that still needs its result', () => {
    assert.equal(sessionHorseOutcome('SCHEDULED', 'SCHEDULED'), 'TO_RUN');
    assert.equal(sessionHorseOutcome('HELD', 'HELD'), 'NEEDS_RESULT');
  });

  it('shows a horse whose result was recorded as ran', () => {
    assert.equal(sessionHorseOutcome('RAN', 'HELD'), 'RAN');
  });

  it('calls a cancelled Workout "Did not run" only when its Session took place', () => {
    assert.equal(sessionHorseOutcome('CANCELLED', 'HELD'), 'DID_NOT_RUN');
    assert.equal(sessionHorseOutcome('CANCELLED', 'SCHEDULED'), 'CANCELLED');
    assert.equal(sessionHorseOutcome('CANCELLED', 'CANCELLED'), 'CANCELLED');
  });
});
