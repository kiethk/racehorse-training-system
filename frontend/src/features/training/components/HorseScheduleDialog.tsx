'use client';

import { useEffect, useMemo, useState } from 'react';
import { HorseAvatar, IconButton, LinkButton, Modal, Notice, Pill, Skeleton, StatusBadge } from '@/components/ui';
import { cn } from '@/lib/cn';
import { displayError, formatDate, formatEnumLabel, formatTime } from '@/lib/display';
import type { Horse } from '@/features/stable/types';
import { toIsoDate, shiftIsoDate } from '../lib/dateRange';
import { groupByDate, scheduleDates, SCHEDULE_DAYS, sessionState } from '../lib/horseSchedule';
import { SESSION_STATE } from '../lib/status';
import { trainingService } from '../services/trainingService';
import type { HorseScheduleItem } from '../types';

interface HorseScheduleDialogProps {
  /** The horse to show; null keeps the dialog closed. `horse` is its profile when known, otherwise only its name. */
  target: { horseId: number; name: string; horse?: Horse } | null;
  /** First day of the week the dialog opens on. */
  startDate: string;
  onClose: () => void;
}

function dayHeading(isoDate: string) {
  const [year, month, day] = isoDate.split('-').map(Number);
  const date = new Date(year, month - 1, day);
  return {
    weekday: formatDate(date, { weekday: 'short' }),
    day: formatDate(date, { day: '2-digit', month: 'short' }),
  };
}

/** The horse's basics and its sessions for seven days at a time, one column per day, with arrows for the week before or after. */
export function HorseScheduleDialog({ target, startDate, onClose }: HorseScheduleDialogProps) {
  return (
    <Modal
      open={target !== null}
      onClose={onClose}
      size="xl"
      title={target?.name ?? ''}
    >
      {/* Remounting per horse resets the week to the day the page is showing. */}
      {target && <ScheduleBody key={target.horseId} target={target} startDate={startDate} />}
    </Modal>
  );
}

function ScheduleBody({
  target,
  startDate,
}: {
  target: { horseId: number; name: string; horse?: Horse };
  startDate: string;
}) {
  const { horseId, name, horse } = target;
  const [weekStart, setWeekStart] = useState(startDate);
  const [items, setItems] = useState<HorseScheduleItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let current = true;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoading(true);
    setError(null);
    trainingService
      .getHorseSchedule(horseId, weekStart, shiftIsoDate(weekStart, SCHEDULE_DAYS - 1))
      .then((result) => current && setItems(result))
      .catch((cause) => current && setError(displayError(cause, 'Unable to load this horse schedule.')))
      .finally(() => current && setLoading(false));
    return () => {
      current = false;
    };
  }, [horseId, weekStart]);

  const dates = useMemo(() => scheduleDates(weekStart), [weekStart]);
  const byDate = useMemo(() => groupByDate(items), [items]);
  const today = toIsoDate(new Date());
  const now = new Date();

  const rangeLabel = `${formatDate(dates[0], { day: '2-digit', month: 'short' })} – ${formatDate(dates[SCHEDULE_DAYS - 1])}`;
  const about = [
    horse?.breed,
    horse?.sex && formatEnumLabel(horse.sex),
    horse?.dateOfBirth && `Born ${formatDate(horse.dateOfBirth)}`,
    horse?.registrationNumber && `Reg. ${horse.registrationNumber}`,
  ].filter(Boolean).join(' · ');

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex min-w-0 items-center gap-3">
          <HorseAvatar name={name} image={horse?.imageUrl} size={48} rounded="md" />
          <div className="min-w-0 space-y-1">
            <div className="flex flex-wrap items-center gap-2">
              {horse && <StatusBadge status={horse.currentStatus} />}
              {horse?.trainingDecision === 'BLOCKED' && <Pill tone="danger" icon="lock" size="sm">Training blocked</Pill>}
            </div>
            {about && <p className="text-xs text-[var(--color-text-secondary)]">{about}</p>}
          </div>
        </div>
        <LinkButton href={`/trainer/horses/${horseId}`} icon="horse" size="sm">Open horse</LinkButton>
      </div>

      <div className="flex items-center justify-between gap-2">
        <IconButton icon="chevron-left" label="Previous 7 days" onClick={() => setWeekStart((d) => shiftIsoDate(d, -SCHEDULE_DAYS))} />
        <p className="text-sm font-semibold text-[var(--color-text-primary)]">{rangeLabel}</p>
        <IconButton icon="chevron-right" label="Next 7 days" onClick={() => setWeekStart((d) => shiftIsoDate(d, SCHEDULE_DAYS))} />
      </div>

      {error && <Notice tone="error">{error}</Notice>}

      <div className="grid grid-cols-7 gap-2">
        {dates.map((date) => {
          const heading = dayHeading(date);
          const sessions = byDate.get(date) ?? [];
          return (
            <div
              key={date}
              className={cn(
                'min-w-0 space-y-2 rounded-[var(--radius-md)] border p-2',
                date === today
                  ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)]/30'
                  : 'border-[var(--color-border)] bg-[var(--color-surface-subtle)]',
              )}
            >
              <div className="border-b border-[var(--color-border)] pb-1.5 text-center">
                <p className="text-xs font-semibold text-[var(--color-text-primary)]">{heading.weekday}</p>
                <p className="text-xs text-[var(--color-text-muted)]">{heading.day}</p>
              </div>
              {loading ? (
                <Skeleton className="h-16 w-full" />
              ) : sessions.length === 0 ? (
                <p className="py-3 text-center text-xs text-[var(--color-text-muted)]">—</p>
              ) : (
                sessions.map((session) => {
                  const state = SESSION_STATE[sessionState(session, now)];
                  return (
                    <div
                      key={session.lotId}
                      className="space-y-1 rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] p-2"
                    >
                      <p className="font-metric text-xs font-semibold text-[var(--color-text-primary)]">
                        {formatTime(session.startTime)}–{formatTime(session.endTime)}
                      </p>
                      <p className="line-clamp-2 break-words text-xs text-[var(--color-text-secondary)]" title={session.subjectName}>
                        {session.subjectName}
                      </p>
                      <Pill tone={state.tone} size="sm">{state.label}</Pill>
                    </div>
                  );
                })
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
