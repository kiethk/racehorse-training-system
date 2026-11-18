/** Ngày khám lại gợi ý = hạn hồi phục muộn nhất của các chấn thương đang theo dõi. */
export function computeSuggestedRecheckDate(injuryRecoveryDates: string[]): string | undefined {
  const dates: string[] = (injuryRecoveryDates ?? []).filter(Boolean).sort();
  return dates.length > 0 ? dates[dates.length - 1] : undefined;
}
