import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { EMPTY_READINGS, readingsToRequest, validateReadings } from '../lib/workoutReadings.ts';

const valid = {
  actualDistanceMeters: '800', actualDurationMinutes: '30', topSpeedKmh: '55', averageSpeedKmh: '48',
  averageHeartRate: '150', maxHeartRate: '200', recoveryHeartRate: '80',
};

describe('validateReadings', () => {
  it('reports every empty reading under its own field', () => {
    const errors = validateReadings(EMPTY_READINGS);
    assert.equal(Object.keys(errors).length, 7);
    assert.equal(errors.topSpeedKmh, 'Top speed is required');
  });

  it('accepts a complete set', () => {
    assert.deepEqual(validateReadings(valid), {});
  });

  it('flags out-of-range and inconsistent readings on the right field', () => {
    const errors = validateReadings({ ...valid, maxHeartRate: '300', averageSpeedKmh: '60' });
    assert.equal(errors.maxHeartRate, 'Max heart rate cannot exceed 240 bpm');
    assert.equal(errors.averageSpeedKmh, 'Average speed cannot exceed top speed');
  });
});

describe('readingsToRequest', () => {
  it('carries no rating', () => {
    const request = readingsToRequest(valid, ' ok ');
    assert.equal('performanceRating' in request, false);
    assert.equal(request.trainerFeedback, 'ok');
    assert.equal(request.averageHeartRate, 150);
  });
});
