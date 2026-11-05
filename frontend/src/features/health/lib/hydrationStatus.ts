/** The clinical hydration status options, shared by every vitals form and display. */

export type HydrationStatusValue = 'NORMAL' | 'MILD_DEHYDRATION' | 'MODERATE_DEHYDRATION' | 'SEVERE_DEHYDRATION' | 'ABNORMAL_CRT';

export const HYDRATION_STATUS_OPTIONS: HydrationStatusValue[] = [
  'NORMAL',
  'MILD_DEHYDRATION',
  'MODERATE_DEHYDRATION',
  'SEVERE_DEHYDRATION',
  'ABNORMAL_CRT',
];

const LABELS: Record<HydrationStatusValue, string> = {
  NORMAL: 'Normal',
  MILD_DEHYDRATION: 'Mild dehydration',
  MODERATE_DEHYDRATION: 'Moderate dehydration',
  SEVERE_DEHYDRATION: 'Severe dehydration',
  ABNORMAL_CRT: 'Abnormal mucous membranes / CRT',
};

export const HYDRATION_STATUS_LABELS = LABELS;

/** Display label for a stored hydration status; tolerates legacy free-text values. */
export function hydrationStatusLabel(value: string | null | undefined): string | null {
  if (!value) return null;
  const asEnum = value.toUpperCase() as HydrationStatusValue;
  return LABELS[asEnum] ?? value;
}
