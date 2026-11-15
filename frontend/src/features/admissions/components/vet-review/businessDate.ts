const BUSINESS_TIME_ZONE = 'Asia/Ho_Chi_Minh';

/** ISO date (yyyy-MM-dd) in the farm's business timezone, shifted by `dayOffset` days. */
export function businessDateFromToday(dayOffset: number) {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: BUSINESS_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date());
  const year = Number(parts.find((part) => part.type === 'year')?.value);
  const month = Number(parts.find((part) => part.type === 'month')?.value);
  const day = Number(parts.find((part) => part.type === 'day')?.value);
  const target = new Date(Date.UTC(year, month - 1, day + dayOffset));

  return [
    target.getUTCFullYear(),
    String(target.getUTCMonth() + 1).padStart(2, '0'),
    String(target.getUTCDate()).padStart(2, '0'),
  ].join('-');
}

export function tomorrowBusinessDate() {
  return businessDateFromToday(1);
}
