import { Button, Notice } from '@/components/ui';
import { formatDate } from '@/lib/display';
import type { HorsePlanAvailability } from '../../types';

/**
 * Shown when selected horses are busy in the chosen period. Instead of letting the trainer move the date by trial
 * and error, it offers the earliest date on which the WHOLE selected group is free for the whole course, or to drop
 * the busy horses and keep the date.
 */
export function GroupScheduleNotice({
  busyHorses,
  groupEarliestStart,
  pending,
  onUseDate,
  onDropBusy,
}: {
  busyHorses: HorsePlanAvailability[];
  groupEarliestStart: string | null;
  /** The suggestion is being recomputed for the latest selection. */
  pending: boolean;
  onUseDate: (date: string) => void;
  onDropBusy: () => void;
}) {
  if (busyHorses.length === 0) return null;
  const names = busyHorses.map((horse) => horse.horseName).join(', ');

  return (
    <Notice tone="warning" title={`${busyHorses.length} selected ${busyHorses.length === 1 ? 'horse is' : 'horses are'} busy in this period`}>
      <div className="space-y-2">
        <p>{names} already {busyHorses.length === 1 ? 'has' : 'have'} a plan that overlaps the dates above.</p>
        <div className="flex flex-wrap gap-2">
          {groupEarliestStart && (
            <Button size="sm" type="button" variant="primary" disabled={pending} onClick={() => onUseDate(groupEarliestStart)}>
              Start on {formatDate(groupEarliestStart)}, when all selected horses are free
            </Button>
          )}
          <Button size="sm" type="button" onClick={onDropBusy}>
            Keep the date, remove busy horses
          </Button>
        </div>
      </div>
    </Notice>
  );
}
