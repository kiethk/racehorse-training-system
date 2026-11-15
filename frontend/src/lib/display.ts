import { getSynchronizedNow } from './clockSync.ts';

const vietnameseText = /[\u00C0-\u024F]/;

/** Keep backend exception details when they are already English; otherwise show a safe UI fallback. */
export function displayError(error: unknown, fallback: string): string {
  if (!(error instanceof Error) || !error.message.trim()) return fallback;
  return vietnameseText.test(error.message) ? fallback : error.message;
}

export function formatDate(value: string | Date | null | undefined, options?: Intl.DateTimeFormatOptions): string {
  if (!value) return '—';
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return new Intl.DateTimeFormat('en-GB', options ?? { day: '2-digit', month: '2-digit', year: 'numeric' }).format(date);
}

export function formatDateTime(value: string | Date | null | undefined): string {
  return formatDate(value, { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

/** Trims a backend time such as "06:30:00" to "06:30". */
export function formatTime(value: string | null | undefined): string {
  return value ? value.slice(0, 5) : '—';
}

/** Formats a clock label (minutes since midnight, Date, ISO timestamp, or HH:mm string) to "HH:mm". */
export function formatClockLabel(value: string | Date | number | null | undefined): string {
  if (value === null || value === undefined) return '—';
  if (typeof value === 'number') {
    const hours = Math.floor(value / 60).toString().padStart(2, '0');
    const mins = Math.floor(value % 60).toString().padStart(2, '0');
    return `${hours}:${mins}`;
  }
  if (typeof value === 'string' && !value.includes('T') && value.includes(':')) {
    return value.slice(0, 5);
  }
  const date = value instanceof Date ? value : new Date(value);
  if (!Number.isNaN(date.getTime())) {
    return new Intl.DateTimeFormat('en-GB', { hour: '2-digit', minute: '2-digit' }).format(date);
  }
  return '—';
}

/** Turns a backend enum such as `HEAD_TRAINER` into "Head trainer". */
export function formatEnumLabel(value: string | null | undefined): string {
  if (!value) return '—';
  const words = value.replaceAll('_', ' ').toLowerCase();
  return words.charAt(0).toUpperCase() + words.slice(1);
}

export function formatNumber(value: number | null | undefined, fractionDigits = 0): string {
  if (value === null || value === undefined || Number.isNaN(value)) return '—';
  return new Intl.NumberFormat('en-GB', {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  }).format(value);
}

/**
 * Returns a date string formatted as "YYYY-MM-DD" in the business timezone (Asia/Ho_Chi_Minh).
 * Avoids UTC offset issues where toISOString() yields yesterday's date prior to 07:00 AM VN.
 */
export function formatLocalDateIso(value?: string | Date): string {
  const date = value ? (value instanceof Date ? value : new Date(value)) : getSynchronizedNow();
  if (Number.isNaN(date.getTime())) return '';
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh' }).format(date);
}
