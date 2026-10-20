export interface EquineWorkoutMetrics {
  actualDistanceMeters?: number | null;
  actualDurationMinutes?: number | null;
  topSpeedKmh?: number | null;
  averageSpeedKmh?: number | null;
  averageHeartRate?: number | null;
  maxHeartRate?: number | null;
  recoveryHeartRate?: number | null;
}

/**
 * Calculates a 1-10 performance rating based on IoT equine telemetry metrics.
 *
 * Grounded in Equine Exercise Physiology:
 * 1. Speed & Consistency (35%): Sustained speed relative to benchmark (55 km/h) & average-to-top speed preservation.
 * 2. Heart Rate Recovery (40%): Cardiovascular fitness via recovery drop ((maxHR - recHR) / maxHR).
 * 3. Work Efficiency & Pacing (25%): Cardiac economy (speed / heart rate) & continuity density.
 */
export function calculatePerformanceRating(metrics: EquineWorkoutMetrics): number {
  const d = Number(metrics.actualDistanceMeters ?? 0);
  const t = Number(metrics.actualDurationMinutes ?? 0);
  const vTop = Math.max(0.1, Number(metrics.topSpeedKmh ?? 0));
  const vAvg = Math.max(0.1, Number(metrics.averageSpeedKmh ?? 0));
  const hrAvg = Math.max(30, Number(metrics.averageHeartRate ?? 0));
  const hrMax = Math.max(hrAvg, Number(metrics.maxHeartRate ?? hrAvg));
  const hrRec = Math.max(30, Number(metrics.recoveryHeartRate ?? 0));

  if (d <= 0 || t <= 0 || vTop <= 0 || vAvg <= 0 || hrAvg <= 30 || hrMax <= 30 || hrRec <= 30) {
    return 8;
  }

  // 1. Speed & Consistency (35%)
  const sSpeed = Math.min(10, (vAvg / 55) * 10);
  const sConsistency = Math.min(10, (vAvg / vTop) * 10);
  const pSpeed = 0.60 * sSpeed + 0.40 * sConsistency;

  // 2. Heart Rate Recovery (40%)
  const rRec = (hrMax - hrRec) / hrMax;
  let pCardio = 1.0 + ((rRec - 0.20) / 0.35) * 9.0;
  pCardio = Math.max(1, Math.min(10, pCardio));

  // 3. Work Efficiency & Pacing (25%)
  const economy = (vAvg / hrAvg) * 100;
  const sEconomy = Math.max(1, Math.min(10, (economy / 28) * 10));

  const vCalc = t > 0 ? (d / 1000) / (t / 60) : vAvg;
  const sDensity = Math.min(10, Math.max(1, (vCalc / vAvg) * 10));
  const pEfficiency = 0.60 * sEconomy + 0.40 * sDensity;

  const composite = 0.35 * pSpeed + 0.40 * pCardio + 0.25 * pEfficiency;
  const rounded = Math.round(composite);
  return Math.max(1, Math.min(10, rounded));
}
