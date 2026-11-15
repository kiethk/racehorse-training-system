/** Minutes since midnight of a backend time such as "06:30" or "06:30:00". */
export function minutesOf(time: string): number {
  const [hours, minutes] = time.split(':').map(Number);
  return hours * 60 + minutes;
}

/** "HH:mm" for a number of minutes since midnight. */
export function clockOf(minutes: number): string {
  return `${String(Math.floor(minutes / 60)).padStart(2, '0')}:${String(minutes % 60).padStart(2, '0')}`;
}

/** Every start time, on the step, at which a session of `duration` minutes still ends inside the window. */
export function sessionStartOptions(windowStart: string, windowEnd: string, duration: number, step: number): string[] {
  const options: string[] = [];
  for (let minute = minutesOf(windowStart); minute + duration <= minutesOf(windowEnd); minute += step) {
    options.push(clockOf(minute));
  }
  return options;
}

/** The start nearest to `minutes` on the step, kept so that the whole session stays inside the window. */
export function snapSessionStart(
  minutes: number,
  duration: number,
  step: number,
  windowStart: string,
  windowEnd: string,
): number {
  const from = minutesOf(windowStart);
  const latest = minutesOf(windowEnd) - duration;
  const snapped = from + Math.round((minutes - from) / step) * step;
  return Math.min(Math.max(snapped, from), latest);
}

export interface StripPosition {
  leftPercent: number;
  widthPercent: number;
}

/** Where a session sits on a strip that spans the window, as percentages of its width. */
export function stripPosition(start: string, end: string, windowStart: string, windowEnd: string): StripPosition {
  const from = minutesOf(windowStart);
  const length = minutesOf(windowEnd) - from || 1;
  const leftPercent = Math.max(0, ((minutesOf(start) - from) / length) * 100);
  const widthPercent = Math.min(100 - leftPercent, ((minutesOf(end) - minutesOf(start)) / length) * 100);
  return { leftPercent, widthPercent: Math.max(0, widthPercent) };
}

export interface HourMark {
  label: string;
  percent: number;
}

/** One mark per full hour inside the window, for the ruler above the strip. */
export function hourMarks(windowStart: string, windowEnd: string): HourMark[] {
  const from = minutesOf(windowStart);
  const to = minutesOf(windowEnd);
  const length = to - from || 1;
  const marks: HourMark[] = [];
  for (let minute = Math.ceil(from / 60) * 60; minute <= to; minute += 60) {
    marks.push({
      label: `${String(minute / 60).padStart(2, '0')}:00`,
      percent: ((minute - from) / length) * 100,
    });
  }
  return marks;
}

export interface LanedSessionLike {
  id: number;
  startTime: string;
  endTime: string;
}

export interface Lanes {
  /** The lane (row) of each session, counted from 0. */
  laneOf: Map<number, number>;
  /** How many rows are needed so that no two sessions in a row overlap; at least 1. */
  laneCount: number;
}

/**
 * Puts sessions into rows so that none overlaps another in its row. A Trainer's own sessions never overlap, but a view that mixes
 * several Trainers (an Owner or a Groom with horses in different groups) can have sessions at the same time; each one then gets a row
 * of its own instead of being drawn on top of another. Greedy by start time, so the fewest rows are used.
 */
export function assignLanes(sessions: LanedSessionLike[]): Lanes {
  const sorted = [...sessions].sort(
    (a, b) => minutesOf(a.startTime) - minutesOf(b.startTime) || minutesOf(a.endTime) - minutesOf(b.endTime) || a.id - b.id,
  );
  const laneEnds: number[] = [];
  const laneOf = new Map<number, number>();
  for (const session of sorted) {
    const start = minutesOf(session.startTime);
    let lane = laneEnds.findIndex((end) => end <= start);
    if (lane === -1) {
      lane = laneEnds.length;
      laneEnds.push(0);
    }
    laneEnds[lane] = minutesOf(session.endTime);
    laneOf.set(session.id, lane);
  }
  return { laneOf, laneCount: Math.max(laneEnds.length, 1) };
}

export interface TrainerLanedSession extends LanedSessionLike {
  trainerId: number;
  trainerName: string | null;
}

/** The rows that belong to one Trainer, so their name can be written beside them. */
export interface TrainerRow {
  trainerId: number;
  trainerName: string;
  firstLane: number;
  laneCount: number;
}

export interface TrainerLanes {
  laneOf: Map<number, number>;
  rows: TrainerRow[];
  /** Every row of every Trainer; at least 1. */
  laneCount: number;
}

/**
 * Groups sessions by Trainer, one band of rows each in name order. Inside a band the sessions are put into rows by assignLanes, so a
 * Trainer (whose own sessions never overlap) gets exactly one row, and two Trainers who train at the same time never share one.
 */
export function assignTrainerLanes(sessions: TrainerLanedSession[]): TrainerLanes {
  const byTrainer = new Map<number, TrainerLanedSession[]>();
  for (const session of sessions) {
    byTrainer.set(session.trainerId, [...(byTrainer.get(session.trainerId) ?? []), session]);
  }
  const trainers = [...byTrainer.entries()]
    .map(([trainerId, own]) => ({ trainerId, trainerName: own[0].trainerName ?? 'Trainer', own }))
    .sort((a, b) => a.trainerName.localeCompare(b.trainerName, undefined, { sensitivity: 'base' }) || a.trainerId - b.trainerId);

  const laneOf = new Map<number, number>();
  const rows: TrainerRow[] = [];
  let offset = 0;
  for (const trainer of trainers) {
    const { laneOf: local, laneCount } = assignLanes(trainer.own);
    for (const [id, lane] of local) laneOf.set(id, offset + lane);
    rows.push({ trainerId: trainer.trainerId, trainerName: trainer.trainerName, firstLane: offset, laneCount });
    offset += laneCount;
  }
  return { laneOf, rows, laneCount: Math.max(offset, 1) };
}
