'use client';

import type { Horse } from '@/features/stable/types';
import { ScheduleWeekCalendar } from './ScheduleWeekCalendar';

/** One horse's week of Sessions, without a dialog around it: shown inline on a horse page or inside HorseScheduleDialog. */
export function HorseScheduleView({
  target,
  startDate,
  hideHeader = false,
  onOpenSession,
}: {
  target: { horseId: number; name: string; horse?: Horse };
  startDate: string;
  hideHeader?: boolean;
  onOpenSession?: (sessionId: number) => void;
}) {
  return (
    <ScheduleWeekCalendar
      mode="horse"
      horseTarget={target}
      weekStart={startDate}
      hideHeader={hideHeader}
      onOpenSession={onOpenSession}
    />
  );
}
