import type { HorseSex } from '@/types/horse';

export const HORSE_SEX_OPTIONS = [
  { value: 'MALE', label: 'Male' },
  { value: 'FEMALE', label: 'Female' },
] as const;

export function isHorseSex(value: unknown): value is HorseSex {
  return value === 'MALE' || value === 'FEMALE';
}

export function formatHorseSex(value: HorseSex | null | undefined): string {
  return HORSE_SEX_OPTIONS.find(option => option.value === value)?.label ?? 'Not recorded';
}
