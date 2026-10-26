'use client';

import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { Icon, Panel, Pill } from '@/components/ui';
import { formatDate, formatEnumLabel } from '@/lib/display';

interface SchedulePatientHeaderProps {
  scheduleId: number;
  status: string;
  careType?: string | null;
  purpose?: string | null;
  scheduledAt?: string | null;
  horseName?: string | null;
  horseBreed?: string | null;
  veterinarianName?: string | null;
}

const STATUS_TONE: Record<string, 'success' | 'warning' | 'info' | 'danger' | 'neutral'> = {
  COMPLETED: 'success',
  IN_PROGRESS: 'info',
  SCHEDULED: 'warning',
  REQUESTED: 'warning',
  CANCELLED: 'danger',
};

/** Patient header shown above the examination form — the Admission-style block. */
export function SchedulePatientHeader({
  scheduleId,
  status,
  careType,
  purpose,
  scheduledAt,
  horseName,
  horseBreed,
  veterinarianName,
}: SchedulePatientHeaderProps) {
  return (
    <Panel padded className="border-l-4 border-l-[var(--color-medical)]">
      <div className="flex items-start gap-3.5">
        <HorseAvatar name={horseName || `Horse #${scheduleId}`} image={null} size={50} rounded="md" />
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-xl font-bold tracking-tight text-[var(--color-text-primary)]">
              {horseName || `Horse #${scheduleId}`}
            </h1>
            {horseBreed && (
              <span className="text-xs text-[var(--color-text-muted)] font-medium">({horseBreed})</span>
            )}
            <Pill tone={STATUS_TONE[status] ?? 'neutral'} size="sm">
              {status === 'IN_PROGRESS' ? 'In Progress' : formatEnumLabel(status)}
            </Pill>
          </div>
          <div className="mt-1 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-[var(--color-text-secondary)]">
            {scheduledAt && (
              <span className="inline-flex items-center gap-1">
                <Icon name="calendar" size={12} />
                {formatDate(scheduledAt)}
              </span>
            )}
            {careType && (
              <span>
                {formatEnumLabel(careType)} Care
              </span>
            )}
            {purpose && (
              <span>
                {formatEnumLabel(purpose)}
              </span>
            )}
            <span className="font-mono text-[11px]">Schedule #{scheduleId}</span>
            {veterinarianName && (
              <span>
                Veterinarian: <strong>{veterinarianName}</strong>
              </span>
            )}
          </div>
        </div>
      </div>
    </Panel>
  );
}
