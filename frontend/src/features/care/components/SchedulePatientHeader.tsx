'use client';

import type { ReactNode } from 'react';
import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { Icon, Panel, Pill } from '@/components/ui';
import { formatDate, formatDateTime } from '@/lib/display';
import { getScheduleStatusDisplay } from '@/features/care/lib/scheduleStatusDisplay';

export interface SchedulePatientHeaderProps {
  scheduleId?: number;
  status: string;
  careType?: string | null;
  purpose?: string | null;
  sourceIncidentId?: number | null;
  scheduledAt?: string | null;
  horseName?: string | null;
  horseBreed?: string | null;
  horseId?: number | null;
  horsePhotoUrl?: string | null;
  veterinarianName?: string | null;
  severity?: string | null;
  action?: ReactNode;
}

/** Patient header shown above the examination form — the Admission-style block. */
export function SchedulePatientHeader({
  scheduleId,
  status,
  careType,
  sourceIncidentId,
  scheduledAt,
  horseName,
  horseId,
  horsePhotoUrl,
  veterinarianName,
  severity,
  action,
}: SchedulePatientHeaderProps) {
  const isUrgent = careType === 'URGENT';
  const statusDisplay = getScheduleStatusDisplay(status);

  const careTypeLabel = (() => {
    if (careType === 'INITIAL') return 'Admission Examination';
    if (careType === 'URGENT') return 'Emergency Care';
    if (careType === 'ROUTINE') {
      return sourceIncidentId ? 'Follow-up Care' : 'Routine Care';
    }
    return null;
  })();

  const avatarName = horseName || (scheduleId ? `Horse #${scheduleId}` : 'Unknown');
  const avatarImage = horsePhotoUrl || (horseId ? `/api/horses/${horseId}/photo` : null);

  return (
    <Panel
      padded
      className={`border-l-4 bg-[var(--color-surface)] ${
        isUrgent ? 'border-l-[var(--color-danger)]' : 'border-l-[var(--color-medical)]'
      }`}
    >
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-start gap-3.5">
          <HorseAvatar name={avatarName} image={avatarImage} size={50} rounded="md" />
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="text-xl font-bold tracking-tight text-[var(--color-text-primary)]">
                {avatarName}
              </h1>
              {severity && (
                <Pill tone={severity === 'CRITICAL' ? 'danger' : 'warning'} size="sm">
                  {severity}
                </Pill>
              )}
              <Pill
                tone={isUrgent && status === 'SCHEDULED' ? 'warning' : statusDisplay.tone}
                icon={isUrgent && status === 'SCHEDULED' ? 'clock' : statusDisplay.icon}
                size="sm"
              >
                {isUrgent && status === 'SCHEDULED' ? 'Pending Intake' : statusDisplay.label}
              </Pill>
            </div>
            <div className="mt-1 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-[var(--color-text-secondary)]">
              {scheduledAt && (
                <span className="inline-flex items-center gap-1">
                  <Icon name={isUrgent ? 'clock' : 'calendar'} size={12} />
                  {isUrgent ? formatDateTime(scheduledAt) : formatDate(scheduledAt)}
                </span>
              )}
              {careTypeLabel && <span>{careTypeLabel}</span>}
              {veterinarianName && (
                <span>
                  Vet: <strong>{veterinarianName}</strong>
                </span>
              )}
            </div>
          </div>
        </div>

        {action && (
          <div className="flex items-center gap-2 self-start sm:self-center">
            {action}
          </div>
        )}
      </div>
    </Panel>
  );
}
