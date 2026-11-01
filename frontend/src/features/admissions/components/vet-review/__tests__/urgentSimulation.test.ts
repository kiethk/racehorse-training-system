import test from 'node:test';
import assert from 'node:assert/strict';
import { randomizeUrgentVitals, URGENT_VITAL_PROFILES } from '../urgentSimulation.ts';
import { assessHydrationFromVitals } from '../constants.ts';

test('randomizeUrgentVitals stays inside the published post-exercise profile envelope', () => {
  const minHr = Math.min(...URGENT_VITAL_PROFILES.map((p) => p.heartRate.min));
  const maxHr = Math.max(...URGENT_VITAL_PROFILES.map((p) => p.heartRate.max));
  const minTemp = Math.min(...URGENT_VITAL_PROFILES.map((p) => p.temperature.min));
  const maxTemp = Math.max(...URGENT_VITAL_PROFILES.map((p) => p.temperature.max));
  const minRr = Math.min(...URGENT_VITAL_PROFILES.map((p) => p.respiratoryRate.min));
  const maxRr = Math.max(...URGENT_VITAL_PROFILES.map((p) => p.respiratoryRate.max));

  for (let i = 0; i < 200; i++) {
    const vitals = randomizeUrgentVitals();
    const hr = Number(vitals.heartRate);
    const temp = Number(vitals.temperature);
    const rr = Number(vitals.respiratoryRate);
    assert.ok(hr >= minHr && hr <= maxHr, `HR ${hr} outside ${minHr}-${maxHr}`);
    assert.ok(temp >= minTemp && temp <= maxTemp, `temp ${temp} outside ${minTemp}-${maxTemp}`);
    assert.ok(rr >= minRr && rr <= maxRr, `RR ${rr} outside ${minRr}-${maxRr}`);
  }

  // Across many draws the scanner must sometimes leave the resting band (that was the whole point).
  // Profiles A+B are always above HR 44; profile C (42-58) is above it ~85% of the time.
  const elevated = Array.from({ length: 100 }, () => Number(randomizeUrgentVitals().heartRate)).filter((hr) => hr > 44);
  assert.ok(elevated.length >= 75, 'urgent simulate should rarely return resting-range heart rates');
});

test('assessHydrationFromVitals grades against the app normal/critical limits', () => {
  assert.equal(assessHydrationFromVitals({ heartRate: '', temperature: '', respiratoryRate: '' }), null);

  const normal = assessHydrationFromVitals({ heartRate: '40', temperature: '37.8', respiratoryRate: '12' });
  assert.equal(normal?.status, 'NORMAL');

  // HR 48 is above the 44 normal max but below the 55 critical max: one abnormal flag.
  const mild = assessHydrationFromVitals({ heartRate: '48', temperature: '37.9', respiratoryRate: '14' });
  assert.equal(mild?.status, 'MILD_DEHYDRATION');
  assert.match(mild?.reason ?? '', /HR 48 > 44/);

  // One metric over its critical max.
  const moderate = assessHydrationFromVitals({ heartRate: '60', temperature: '38.0', respiratoryRate: '15' });
  assert.equal(moderate?.status, 'MODERATE_DEHYDRATION');

  // Two metrics over critical maxima.
  const severe = assessHydrationFromVitals({ heartRate: '120', temperature: '39.6', respiratoryRate: '15' });
  assert.equal(severe?.status, 'SEVERE_DEHYDRATION');
  assert.match(severe?.reason ?? '', /HR 120 > 55/);
  assert.match(severe?.reason ?? '', /temp 39\.6 > 38\.8/);
});
