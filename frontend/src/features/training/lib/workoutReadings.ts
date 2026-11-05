import type { CompleteWorkoutRequest } from '../types';

/** The seven readings a Trainer copies from the IoT device. */
export const READING_FIELDS = [
  { key: 'actualDistanceMeters', label: 'Distance', unit: 'm', min: 0.1, max: 100000, step: '0.1' },
  { key: 'actualDurationMinutes', label: 'Duration', unit: 'min', min: 0.1, max: 1440, step: '0.1' },
  { key: 'topSpeedKmh', label: 'Top speed', unit: 'km/h', min: 0.1, max: 100, step: '0.1' },
  { key: 'averageSpeedKmh', label: 'Average speed', unit: 'km/h', min: 0.1, max: 100, step: '0.1' },
  { key: 'averageHeartRate', label: 'Average heart rate', unit: 'bpm', min: 30, max: 240, step: '1', integer: true },
  { key: 'maxHeartRate', label: 'Max heart rate', unit: 'bpm', min: 30, max: 240, step: '1', integer: true },
  { key: 'recoveryHeartRate', label: 'Recovery heart rate', unit: 'bpm', min: 30, max: 240, step: '1', integer: true },
] as const;

export type ReadingKey = (typeof READING_FIELDS)[number]['key'];
export type ReadingInputs = Record<ReadingKey, string>;
export type ReadingErrors = Partial<Record<ReadingKey | 'feedback', string>>;

export const EMPTY_READINGS: ReadingInputs = {
  actualDistanceMeters: '',
  actualDurationMinutes: '',
  topSpeedKmh: '',
  averageSpeedKmh: '',
  averageHeartRate: '',
  maxHeartRate: '',
  recoveryHeartRate: '',
};

/** Checks each reading against the backend rules and reports the problem under its own field. */
export function validateReadings(inputs: ReadingInputs): ReadingErrors {
  const errors: ReadingErrors = {};
  for (const field of READING_FIELDS) {
    const raw = inputs[field.key].trim();
    if (raw === '') {
      errors[field.key] = `${field.label} is required`;
      continue;
    }
    const value = Number(raw);
    if (!Number.isFinite(value)) errors[field.key] = `${field.label} must be a number`;
    else if ('integer' in field && !Number.isInteger(value)) errors[field.key] = `${field.label} must be a whole number`;
    else if (value < field.min) errors[field.key] = `${field.label} must be at least ${field.min} ${field.unit}`;
    else if (value > field.max) errors[field.key] = `${field.label} cannot exceed ${field.max} ${field.unit}`;
  }
  if (!errors.averageSpeedKmh && !errors.topSpeedKmh && Number(inputs.averageSpeedKmh) > Number(inputs.topSpeedKmh)) {
    errors.averageSpeedKmh = 'Average speed cannot exceed top speed';
  }
  if (!errors.averageHeartRate && !errors.maxHeartRate && Number(inputs.averageHeartRate) > Number(inputs.maxHeartRate)) {
    errors.averageHeartRate = 'Average heart rate cannot exceed max heart rate';
  }
  return errors;
}

export function readingsToInputs(source: Record<ReadingKey, number | string>): ReadingInputs {
  const inputs = { ...EMPTY_READINGS };
  for (const field of READING_FIELDS) inputs[field.key] = String(source[field.key]);
  return inputs;
}

export function readingsToRequest(inputs: ReadingInputs, feedback: string): CompleteWorkoutRequest {
  return {
    actualDistanceMeters: Number(inputs.actualDistanceMeters),
    actualDurationMinutes: Number(inputs.actualDurationMinutes),
    topSpeedKmh: Number(inputs.topSpeedKmh),
    averageSpeedKmh: Number(inputs.averageSpeedKmh),
    averageHeartRate: Number(inputs.averageHeartRate),
    maxHeartRate: Number(inputs.maxHeartRate),
    recoveryHeartRate: Number(inputs.recoveryHeartRate),
    trainerFeedback: feedback.trim() || null,
  };
}
