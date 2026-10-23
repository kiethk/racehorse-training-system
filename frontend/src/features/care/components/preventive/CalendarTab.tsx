'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Button,
  EmptyState,
  IconButton,
  ListSkeleton,
  Notice,
  Panel,
  Pill,
  SectionTitle,
  SegmentedControl,
} from '@/components/ui';
import { useAuth } from '@/context/AuthContext';
import { horseService } from '@/features/horses/services/horseService';
import { displayError, formatDate, formatEnumLabel } from '@/lib/display';
import { careService } from '../../services/careService';
import type { CareScheduleResponse } from '../../types';
import { CareRecordDetailModal } from '../CareRecordDetailModal';
import { RescheduleModal } from './RescheduleModal';
import { addDaysIso, todayIso } from './preventiveDisplay';

type View = 'week' | 'month';
const OPEN_STATUSES = new Set(['REQUESTED', 'SCHEDULED', 'IN_PROGRESS']);

/** The day an appointment falls on: its booked slot, or the requested date while no vet is assigned. */
const appointmentDay = (schedule: CareScheduleResponse) =>
  (schedule.scheduledAt ?? schedule.requestedAt ?? '').slice(0, 10);

function periodOf(view: View, anchor: string): { start: string; end: string; label: string } {
  const date = new Date(`${anchor}T00:00:00`);
  if (view === 'week') {
    const mondayOffset = (date.getDay() + 6) % 7;
    const start = addDaysIso(anchor, -mondayOffset);
    const end = addDaysIso(start, 6);
    return { start, end, label: `${formatDate(start)} – ${formatDate(end)}` };
  }
  const start = `${anchor.slice(0, 7)}-01`;
  const next = new Date(date.getFullYear(), date.getMonth() + 1, 1);
  const end = addDaysIso(`${next.getFullYear()}-${String(next.getMonth() + 1).padStart(2, '0')}-01`, -1);
  return { start, end, label: formatDate(start, { month: 'long', year: 'numeric' }) };
}

export function CalendarTab() {
  const { user } = useAuth();
  const [view, setView] = useState<View>('week');
  const [anchor, setAnchor] = useState(todayIso());
  const [schedules, setSchedules] = useState<CareScheduleResponse[]>([]);
  const [horseNames, setHorseNames] = useState<Map<number, string>>(new Map());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [moving, setMoving] = useState<CareScheduleResponse | null>(null);
  const [detailId, setDetailId] = useState<number | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [page, horses] = await Promise.all([
        careService.listSchedules({ careType: 'ROUTINE', size: 500 }),
        horseService.listAll().catch(() => []),
      ]);
      setSchedules(page.content.filter((s) => OPEN_STATUSES.has(s.status)));
      setHorseNames(new Map(horses.map((h) => [h.id, h.name])));
    } catch (cause) {
      setError(displayError(cause, 'Could not load appointments.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const period = periodOf(view, anchor);
  const days = useMemo(() => {
    const byDay = new Map<string, CareScheduleResponse[]>();
    schedules
      .filter((s) => {
        const day = appointmentDay(s);
        return day >= period.start && day <= period.end;
      })
      .sort((a, b) => (a.scheduledAt ?? a.requestedAt ?? '').localeCompare(b.scheduledAt ?? b.requestedAt ?? ''))
      .forEach((s) => {
        const day = appointmentDay(s);
        byDay.set(day, [...(byDay.get(day) ?? []), s]);
      });
    return [...byDay.entries()];
  }, [schedules, period.start, period.end]);

  const horseName = (id: number) => horseNames.get(id) ?? `Horse #${id}`;
  const step = (direction: 1 | -1) => {
    if (view === 'week') setAnchor(addDaysIso(anchor, 7 * direction));
    else {
      const date = new Date(`${anchor}T00:00:00`);
      const target = new Date(date.getFullYear(), date.getMonth() + direction, 1);
      setAnchor(`${target.getFullYear()}-${String(target.getMonth() + 1).padStart(2, '0')}-01`);
    }
  };

  // The backend checks this too; the button is hidden when the request would be refused.
  const canMove = (s: CareScheduleResponse) =>
    user?.role === 'CLUB_MANAGER'
      ? s.status === 'REQUESTED' || s.status === 'SCHEDULED'
      : user?.role === 'VETERINARIAN' && s.status === 'SCHEDULED' && s.veterinarianId === user.userId;

  return (
    <div className="space-y-4">
      <Panel padded className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <IconButton icon="chevron-left" label="Previous period" onClick={() => step(-1)} />
          <Button variant="secondary" size="sm" onClick={() => setAnchor(todayIso())}>Today</Button>
          <IconButton icon="chevron-right" label="Next period" onClick={() => step(1)} />
          <span className="ml-2 text-sm font-semibold text-[var(--color-text-primary)]">{period.label}</span>
        </div>
        <div className="flex items-center gap-2">
          <SegmentedControl<View>
            label="Calendar view"
            value={view}
            onChange={setView}
            options={[{ value: 'week', label: 'Week' }, { value: 'month', label: 'Month' }]}
          />
          <Button variant="secondary" size="sm" icon="refresh" onClick={load} disabled={loading}>Refresh</Button>
        </div>
      </Panel>

      {error && <Notice tone="error" title="Could not load" onDismiss={() => setError(null)}>{error}</Notice>}

      {loading ? (
        <ListSkeleton />
      ) : days.length === 0 ? (
        <Panel padded>
          <EmptyState icon="calendar" title="No appointments" description="No routine or preventive visits fall in this period." />
        </Panel>
      ) : (
        days.map(([day, items]) => (
          <Panel key={day} padded className="space-y-3">
            <div className="flex items-center justify-between">
              <SectionTitle>{formatDate(day, { weekday: 'long', day: '2-digit', month: 'short', year: 'numeric' })}</SectionTitle>
              <span className="text-xs text-[var(--color-text-muted)]">{items.length} visit{items.length === 1 ? '' : 's'}</span>
            </div>
            <ul className="divide-y divide-[var(--color-border)]">
              {items.map((s) => (
                <li key={s.id} className="flex flex-wrap items-center justify-between gap-3 py-2.5">
                  <div className="min-w-0 space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="w-12 text-sm tabular-nums text-[var(--color-text-secondary)]">
                        {s.scheduledAt ? s.scheduledAt.slice(11, 16) : '—'}
                      </span>
                      <span className="font-semibold text-[var(--color-text-primary)]">{horseName(s.horseId)}</span>
                      <Pill tone="neutral" size="sm">
                        {s.purpose === 'PREVENTIVE' ? 'Vaccination' : 'Follow-up'}
                      </Pill>
                      <Pill tone={s.status === 'REQUESTED' ? 'warning' : 'success'} size="sm">
                        {s.status === 'REQUESTED' ? 'Awaiting vet' : formatEnumLabel(s.status)}
                      </Pill>
                      {s.rescheduleReason && (
                        <Pill tone="neutral" size="sm">Moved · {formatEnumLabel(s.rescheduleReason)}</Pill>
                      )}
                    </div>
                    {s.description && (
                      <p className="line-clamp-1 pl-14 text-xs text-[var(--color-text-muted)]">{s.description}</p>
                    )}
                  </div>
                  <div className="flex gap-1.5">
                    {canMove(s) && (
                      <Button variant="secondary" size="sm" icon="calendar" onClick={() => setMoving(s)}>Move</Button>
                    )}
                    <Button variant="tertiary" size="sm" onClick={() => setDetailId(s.id)}>Details</Button>
                  </div>
                </li>
              ))}
            </ul>
          </Panel>
        ))
      )}

      <RescheduleModal
        schedule={moving}
        horseName={moving ? horseName(moving.horseId) : ''}
        onClose={() => setMoving(null)}
        onRescheduled={load}
      />
      <CareRecordDetailModal
        scheduleId={detailId}
        open={detailId != null}
        onClose={() => setDetailId(null)}
        onScheduleUpdated={load}
      />
    </div>
  );
}
