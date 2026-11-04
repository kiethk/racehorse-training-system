"use client";

import { Panel, SectionTitle } from "@/components/ui/Panel";
import { Pill } from "@/components/ui/StatusBadge";
import { Button } from "@/components/ui/Button";
import { HorseAvatar } from "@/components/ui/HorseAvatar";
import { EmptyState, ListSkeleton } from "@/components/ui/states";
import type { CareSchedule } from "@/features/admissions/types";
import type { Horse } from "@/features/stable/types";
import { getCareTypeTone } from "@/features/schedule/utils/timelineUtils";

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
  onStartExam: (schedule: CareSchedule) => void;
  onViewFindings: (schedule: CareSchedule) => void;
  onBookRoutine: () => void;
}

/** Left column of the vet dashboard: today's clinical agenda (08:00 – 17:00). */
export function VetTodayAgenda({
  loading,
  appointments,
  horseMap,
  stallMap,
  onStartExam,
  onViewFindings,
  onBookRoutine,
}: VetTodayAgendaProps) {
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
            description="No clinical appointments scheduled for today's working day (08:00 – 17:00)."
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
              const timeStr = formatAppointmentTime(schedule.scheduledAt);
              const duration = schedule.durationMinutes ?? 30;
              const tone = getCareTypeTone(schedule.careType, schedule.purpose, schedule.sourceIncidentId);
              // Whole-row click mirrors the status button, so compute the action once for mouse + keyboard.
              const openRow = () =>
                schedule.status === "COMPLETED"
                  ? onViewFindings(schedule)
                  : onStartExam(schedule);

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
                        <span className="font-medium text-[var(--color-text-primary)]">
                          {timeStr}
                        </span>
                        <span className="text-[var(--color-text-muted)]">
                          ({duration}m)
                        </span>
                        <span className="text-[var(--color-border-strong)]">
                          ·
                        </span>
                        <span>
                          {stallCode
                            ? `Stall: ${stallCode}`
                            : "Stall: Unassigned"}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="flex flex-wrap items-center gap-2 self-end sm:self-center">
                    <Pill tone={tone.pillTone} icon={tone.icon} size="sm">
                      {tone.label}
                    </Pill>

                    {schedule.status === "PAUSED" && (
                      <Pill tone="warning" size="sm">
                        Paused
                      </Pill>
                    )}

                    {schedule.overdue && (
                      <Pill tone="warning" icon="clock" size="sm">
                        Overdue
                      </Pill>
                    )}

                    {schedule.status === "PAUSED" ? (
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={(event) => {
                          event.stopPropagation();
                          onStartExam(schedule);
                        }}
                      >
                        Continue (Paused)
                      </Button>
                    ) : schedule.status === "IN_PROGRESS" || schedule.status === "DELAYED" ? (
                      <Button
                        variant="primary"
                        size="sm"
                        onClick={(event) => {
                          event.stopPropagation();
                          onStartExam(schedule);
                        }}
                      >
                        Resume Exam
                      </Button>
                    ) : schedule.status === "COMPLETED" ? (
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={(event) => {
                          event.stopPropagation();
                          onViewFindings(schedule);
                        }}
                      >
                        View Findings
                      </Button>
                    ) : (
                      <Button
                        variant="primary"
                        size="sm"
                        onClick={(event) => {
                          event.stopPropagation();
                          onStartExam(schedule);
                        }}
                      >
                        Start Exam
                      </Button>
                    )}
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
