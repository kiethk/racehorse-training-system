'use client';

import { Modal } from '@/components/ui';
import type { Horse } from '@/features/stable/types';
import { ScheduleWeekCalendar } from './ScheduleWeekCalendar';

interface HorseScheduleDialogProps {
  /** The horse to show; null keeps the dialog closed. `horse` is its profile when known, otherwise only its name. */
  target: { horseId: number; name: string; horse?: Horse } | null;
  /** First day of the week the dialog opens on. */
  startDate: string;
  onClose: () => void;
  onOpenSession?: (sessionId: number) => void;
}

/** The horse's basics and its sessions for seven days at a time, one column per day, with arrows for the week before or after. */
export function HorseScheduleDialog({ target, startDate, onClose, onOpenSession }: HorseScheduleDialogProps) {
  return (
    <Modal
      open={target !== null}
      onClose={onClose}
      size="xl"
      title={target ? `Training schedule · ${target.name}` : ''}
    >
      {/* Remounting per horse resets the week to the day the page is showing. */}
      {target && (
        <HorseScheduleView
          key={target.horseId}
          target={target}
          startDate={startDate}
          hideHeader
          onOpenSession={onOpenSession}
        />
      )}
    </Modal>
  );
}

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
