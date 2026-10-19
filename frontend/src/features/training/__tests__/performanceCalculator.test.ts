import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { calculatePerformanceRating } from '../lib/performanceCalculator.ts';

describe('calculatePerformanceRating', () => {
  it('rates high-performing workouts with excellent speed and quick recovery as 8-10', () => {
    const rating = calculatePerformanceRating({
      actualDistanceMeters: 1200,
      actualDurationMinutes: 1.6,
      topSpeedKmh: 52,
      averageSpeedKmh: 45,
      averageHeartRate: 158,
      maxHeartRate: 192,
      recoveryHeartRate: 82,
    });
    assert.ok(rating >= 8 && rating <= 10, `Expected 8..10, got ${rating}`);
  });

  it('rates moderate standard workouts with standard recovery as 6-8', () => {
    const rating = calculatePerformanceRating({
      actualDistanceMeters: 1000,
      actualDurationMinutes: 2.0,
      topSpeedKmh: 38,
      averageSpeedKmh: 30,
      averageHeartRate: 155,
      maxHeartRate: 180,
      recoveryHeartRate: 105,
    });
    assert.ok(rating >= 6 && rating <= 8, `Expected 6..8, got ${rating}`);
  });

  it('rates struggling workouts with elevated heart rate and poor recovery as <= 5', () => {
    const rating = calculatePerformanceRating({
      actualDistanceMeters: 800,
      actualDurationMinutes: 2.5,
      topSpeedKmh: 32,
      averageSpeedKmh: 20,
      averageHeartRate: 170,
      maxHeartRate: 190,
      recoveryHeartRate: 145,
    });
    assert.ok(rating <= 5, `Expected <= 5, got ${rating}`);
  });

  it('falls back to 8 when input metrics are missing or zero', () => {
    const rating = calculatePerformanceRating({});
    assert.equal(rating, 8);
  });
});
