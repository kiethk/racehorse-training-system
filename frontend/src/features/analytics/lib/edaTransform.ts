import type { CorrelationStrength } from '../types/eda';

export interface CorrelationToneInfo {
  label: string;
  qualitativeLabel: string;
  tone: 'success' | 'warning' | 'info' | 'neutral' | 'danger';
  description: string;
  tooltip: string;
}

export function formatCorrelationStrength(strength: CorrelationStrength, pearsonR: number): CorrelationToneInfo {
  const sign = pearsonR > 0 ? '+' : '';
  const rFormatted = `${sign}${pearsonR.toFixed(2)}`;

  switch (strength) {
    case 'STRONG_POSITIVE':
      return {
        label: `Strong Positive (${rFormatted})`,
        qualitativeLabel: 'Strong Positive',
        tone: 'info',
        description: 'Higher workout speed correlates with significantly higher recovery heart rate.',
        tooltip: `Pearson r = ${rFormatted}`,
      };
    case 'MODERATE_POSITIVE':
      return {
        label: `Moderate Positive (${rFormatted})`,
        qualitativeLabel: 'Moderate Positive',
        tone: 'neutral',
        description: 'Moderate tendency of increasing recovery heart rate with higher speed.',
        tooltip: `Pearson r = ${rFormatted}`,
      };
    case 'STRONG_NEGATIVE':
      return {
        label: `Strong Negative (${rFormatted})`,
        qualitativeLabel: 'Strong Negative',
        tone: 'success',
        description: 'High speed with low recovery HR indicates exceptional cardiovascular fitness.',
        tooltip: `Pearson r = ${rFormatted}`,
      };
    case 'MODERATE_NEGATIVE':
      return {
        label: `Moderate Negative (${rFormatted})`,
        qualitativeLabel: 'Moderate Negative',
        tone: 'success',
        description: 'Horse maintains solid speed with moderate cardiovascular exertion.',
        tooltip: `Pearson r = ${rFormatted}`,
      };
    case 'WEAK':
      return {
        label: `Weak (${rFormatted})`,
        qualitativeLabel: 'Weak',
        tone: 'neutral',
        description: 'No significant linear correlation observed between speed and recovery HR.',
        tooltip: `Pearson r = ${rFormatted}`,
      };
    case 'NO_VARIANCE':
      return {
        label: 'Zero / Insufficient Variance',
        qualitativeLabel: 'Zero / Insufficient variance',
        tone: 'neutral',
        description: 'Workouts currently have identical speeds or heart rates. Record varied sessions to calculate cardio fitness correlation.',
        tooltip: 'Pearson r = N/A (zero variance)',
      };
    case 'INSUFFICIENT_DATA':
    default:
      return {
        label: 'Insufficient Data',
        qualitativeLabel: 'Insufficient Data',
        tone: 'warning',
        description: 'At least 2 data points with speed and recovery HR are required for analysis.',
        tooltip: 'Pearson r = N/A (insufficient data)',
      };
  }
}

export function calculateTrendLinePoints(
  minX: number,
  maxX: number,
  slope: number,
  intercept: number
): { x: number; y: number }[] {
  if (minX >= maxX || isNaN(slope) || isNaN(intercept)) {
    return [];
  }
  return [
    { x: minX, y: Math.round((slope * minX + intercept) * 10) / 10 },
    { x: maxX, y: Math.round((slope * maxX + intercept) * 10) / 10 },
  ];
}
