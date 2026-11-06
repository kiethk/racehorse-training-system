"use client";

import { Panel, SectionTitle } from "@/components/ui/Panel";
import { Pill } from "@/components/ui/StatusBadge";
import { Button } from "@/components/ui/Button";
import { HorseAvatar } from "@/components/ui/HorseAvatar";
import { EmptyState, ListSkeleton } from "@/components/ui/states";
import type { CareSchedule } from "@/features/admissions/types";
import type { Horse } from "@/features/stable/types";
import { getCareTypeTone } from "@/features/schedule/utils/timelineUtils";
import { getScheduleExamAction, getScheduleStatusDisplay } from "@/features/care/lib/scheduleStatusDisplay";

function formatAppointmentTime(scheduledAt?: string | null): string {
  if (!scheduledAt) return "--:--";
  const date = new Date(scheduledAt);
  if (!Number.isNaN(date.getTime())) {
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");
    return `${hours}:${minutes}`;
  }
  return scheduledAt.length >= 16 ? scheduledAt.slice(11, 16) : scheduledAt;
}

interface VetTodayAgendaProps {
  loading: boolean;
  appointments: CareSchedule[];
  horseMap: Map<number, Horse>;
  stallMap: Map<number, string>;
  onStartExam?: (schedule: CareSchedule) => void;
  onExamine?: (schedule: CareSchedule) => void;
  onViewFindings: (schedule: CareSchedule) => void;
  onBookRoutine: () => void;
}

/** Left column of the vet dashboard: today's clinical agenda. */
export function VetTodayAgenda({
  loading,
  appointments,
  horseMap,
  stallMap,
  onStartExam,
  onExamine,
  onViewFindings,
  onBookRoutine,
}: VetTodayAgendaProps) {
  const handleExamAction = onExamine ?? onStartExam ?? (() => {});
  return (
    <div className="space-y-4 lg:col-span-7">
      <Panel padded>
        <div className="mb-4 border-b border-[var(--color-border)] pb-3">
          <SectionTitle>Today&apos;s Clinical Agenda</SectionTitle>
        </div>

        {loading ? (
          <ListSkeleton rows={4} />
        ) : appointments.length === 0 ? (
          <EmptyState
            icon="calendar"
            title="No clinical appointments today"
            action={
              <Button variant="secondary" size="sm" icon="plus" onClick={onBookRoutine}>
                Book Routine Check
              </Button>
            }
          />
        ) : (
          <div className="divide-y divide-[var(--color-border)]">
            {appointments.map((schedule) => {
              const horse = horseMap.get(schedule.horseId);
              const horseName =
                schedule.candidateName ||
                horse?.name ||
                `Horse #${schedule.horseId}`;
              const stallCode =
                horse?.currentStallId != null
                  ? stallMap.get(horse.currentStallId)
                  : null;
              const stallLabel = stallCode
                ? (schedule.careType === 'INITIAL' ? `Quarantine Stall: ${stallCode}` : `Stall: ${stallCode}`)
                : "Stall: Unassigned";
              const tone = getCareTypeTone(schedule.careType, schedule.purpose, schedule.sourceIncidentId);
              const status = getScheduleStatusDisplay(schedule.status);
              const action = getScheduleExamAction(schedule.status);
              // Whole-row click mirrors the action button, so compute once for mouse + keyboard.
              const openRow = () =>
                schedule.status === "COMPLETED"
                  ? onViewFindings(schedule)
                  : handleExamAction(schedule);

              return (
                <div
                  key={schedule.id}
                  role="button"
                  tabIndex={0}
                  onClick={openRow}
                  onKeyDown={(event) => {
                    // Guard: Enter on an inner button bubbles here too — only act when the row itself is focused.
                    if (event.target !== event.currentTarget) return;
                    if (event.key === "Enter" || event.key === " ") {
                      event.preventDefault();
                      openRow();
                    }
                  }}
                  className="flex cursor-pointer flex-col gap-3 rounded-[var(--radius-sm)] py-3.5 transition-colors first:pt-0 last:pb-0 hover:bg-[var(--color-surface-subtle)] focus-visible:outline-2 focus-visible:outline-[var(--color-focus)] sm:flex-row sm:items-center sm:justify-between"
                >
                  <div className="flex items-center gap-3">
                    <HorseAvatar
                      name={horseName}
                      image={horse?.imageUrl}
                      size={44}
                      rounded="md"
                    />
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="truncate font-semibold text-[var(--color-text-primary)]">
                          {horseName}
                        </span>
                        {horse?.breed && (
                          <span className="text-xs text-[var(--color-text-muted)]">
                            · {horse.breed}
                          </span>
                        )}
                      </div>
                      <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-[var(--color-text-secondary)]">
                        {schedule.careType === 'URGENT' && schedule.scheduledAt && (
                          <>
                            <span className="font-semibold text-[var(--color-danger)]">
                              {formatAppointmentTime(schedule.scheduledAt)}
                            </span>
                            <span className="text-[var(--color-border-strong)]">·</span>
                          </>
                        )}
                        <span>{stallLabel}</span>
                      </div>
                    </div>
                  </div>

                  <div className="flex flex-wrap items-center gap-2 self-end sm:self-center">
                    <Pill tone={tone.pillTone} icon={tone.icon} size="sm">
                      {tone.label}
                    </Pill>

                    {schedule.status !== "SCHEDULED" && (
                      <Pill tone={status.tone} icon={status.icon} size="sm">
                        {status.label}
                      </Pill>
                    )}

                    <Button
                      variant={schedule.status === "COMPLETED" ? "secondary" : "primary"}
                      size="sm"
                      icon={action.icon}
                      onClick={(event) => {
                        event.stopPropagation();
                        openRow();
                      }}
                    >
                      {action.label}
                    </Button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </Panel>
    </div>
  );
}
