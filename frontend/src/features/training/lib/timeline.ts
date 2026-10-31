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
