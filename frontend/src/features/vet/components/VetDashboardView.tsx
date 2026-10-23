"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { PageHeader } from "@/components/ui/PageHeader";
import { MetricCard } from "@/components/ui/MetricCard";
import { Panel, SectionTitle } from "@/components/ui/Panel";
import { Pill } from "@/components/ui/StatusBadge";
import { Button, LinkButton, IconButton } from "@/components/ui/Button";
import { HorseAvatar } from "@/components/ui/HorseAvatar";
import { Icon } from "@/components/ui/Icon";
import { EmptyState, ListSkeleton } from "@/components/ui/states";
import { Notice } from "@/components/ui/Notice";
import { admissionsApi } from "@/features/admissions/services/api";
import { stableApi } from "@/features/stable/services/stableService";
import { CreateRoutineModal } from "@/features/routine/components/CreateRoutineModal";
import type {
  CareSchedule,
  VetQueueSummary,
} from "@/features/admissions/types";
import type { Horse, StableStall } from "@/features/stable/types";
import {
  filterActiveEmergencies,
  filterBlockedHorses,
  filterTodayAppointments,
} from "../lib/dashboardUtils";

function getTodayLocalDate(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

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

export function VetDashboardView() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [summary, setSummary] = useState<VetQueueSummary | null>(null);
  const [todaySchedules, setTodaySchedules] = useState<CareSchedule[]>([]);
  const [urgentSchedules, setUrgentSchedules] = useState<CareSchedule[]>([]);
  const [horses, setHorses] = useState<Horse[]>([]);
  const [stalls, setStalls] = useState<StableStall[]>([]);

  const [isRoutineModalOpen, setIsRoutineModalOpen] = useState(false);

  const todayLocal = useMemo(() => getTodayLocalDate(), []);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const [summaryRes, todayRes, urgentRes, horsesRes, stallsRes] =
        await Promise.all([
          admissionsApi.getVetQueueSummary().catch((err) => {
            console.error("Failed to load vet queue summary", err);
            return null;
          }),
          admissionsApi
            .getCareSchedules({ date: todayLocal, size: 50 })
            .catch((err) => {
              console.error("Failed to load today schedules", err);
              return { content: [] };
            }),
          admissionsApi
            .getCareSchedules({ careType: "URGENT", size: 50 })
            .catch((err) => {
              console.error("Failed to load urgent schedules", err);
              return { content: [] };
            }),
          stableApi.getHorses().catch((err) => {
            console.error("Failed to load horses", err);
            return [];
          }),
          stableApi.getStalls().catch((err) => {
            console.error("Failed to load stalls", err);
            return [];
          }),
        ]);

      setSummary(summaryRes);
      setTodaySchedules(todayRes?.content ?? []);
      setUrgentSchedules(urgentRes?.content ?? []);
      setHorses(horsesRes ?? []);
      setStalls(stallsRes ?? []);
    } catch (err: unknown) {
      const msg =
        err instanceof Error
          ? err.message
          : "Failed to load veterinary dashboard data";
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, [todayLocal]);

  useEffect(() => {
    let active = true;
    Promise.resolve().then(() => {
      if (active) void loadData();
    });
    return () => {
      active = false;
    };
  }, [loadData]);

  const horseMap = useMemo(
    () => new Map(horses.map((h) => [h.id, h])),
    [horses],
  );
  const stallMap = useMemo(
    () => new Map(stalls.map((s) => [s.id, s.stallCode])),
    [stalls],
  );

  const activeEmergencies = useMemo(
    () => filterActiveEmergencies(urgentSchedules),
    [urgentSchedules],
  );
  const blockedHorses = useMemo(
    () => filterBlockedHorses(horses, stallMap),
    [horses, stallMap],
  );
  const todayAppointments = useMemo(
    () => filterTodayAppointments(todaySchedules, todayLocal),
    [todaySchedules, todayLocal],
  );

  const admissionClearanceCount = summary?.active ?? summary?.awaiting ?? 0;

  const handleStartExam = async (schedule: CareSchedule) => {
    try {
      if (schedule.status === "SCHEDULED") {
        await admissionsApi.startCareSchedule(schedule.id);
      }
    } catch (err: unknown) {
      console.error("Failed to start care schedule:", err);
      const msg =
        err instanceof Error
          ? err.message
          : "Cannot start examination: veterinarian is currently busy with another in-progress schedule";
      setError(msg);
      return;
    }

    if (schedule.careType === "INITIAL") {
      router.push(`/veterinarian/admissions?scheduleId=${schedule.id}`);
    } else if (schedule.careType === "ROUTINE") {
      router.push(`/veterinarian/routine/${schedule.id}`);
    } else if (schedule.careType === "URGENT") {
      router.push(`/veterinarian/urgent/${schedule.id}`);
    } else {
      router.push(`/veterinarian/schedule/${schedule.id}`);
    }
  };

  const handleViewFindings = (schedule: CareSchedule) => {
    if (schedule.careType === "INITIAL") {
      router.push(`/veterinarian/admissions?scheduleId=${schedule.id}`);
    } else if (schedule.careType === "ROUTINE") {
      router.push(`/veterinarian/routine/${schedule.id}`);
    } else if (schedule.careType === "URGENT") {
      router.push(`/veterinarian/urgent/${schedule.id}`);
    } else {
      router.push(`/veterinarian/schedule/${schedule.id}`);
    }
  };

  return (
    <div className="space-y-6">
      {/* TIER 1: Header & Quick Actions */}
      <PageHeader
        eyebrow="Clinical Operations"
        title="Veterinary Command Center"
        actions={
          <div className="flex flex-wrap items-center gap-2">
            <IconButton
              icon="refresh"
              label="Refresh dashboard data"
              variant="secondary"
              onClick={loadData}
              disabled={loading}
            />
            <LinkButton
              href="/veterinarian/preventive-care"
              variant="secondary"
              size="md"
            >
              Vaccine Protocols
            </LinkButton>
            <LinkButton
              href="/veterinarian/health"
              variant="secondary"
              size="md"
            >
              Medical Records
            </LinkButton>
            <Button
              variant="primary"
              size="md"
              icon="plus"
              onClick={() => setIsRoutineModalOpen(true)}
            >
              Schedule Routine
            </Button>
          </div>
        }
      />

      {error && (
        <Notice tone="error" title="Notice">
          <div className="flex items-center justify-between">
            <span>{error}</span>
            <Button variant="secondary" size="sm" onClick={loadData}>
              Retry
            </Button>
          </div>
        </Notice>
      )}

      {/* TIER 2: 4 Real-time Clinical Metric Cards */}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <MetricCard
          label="Active Emergencies"
          value={activeEmergencies.length}
          icon="alert-triangle"
          tone={activeEmergencies.length > 0 ? "danger" : "neutral"}
          onClick={() => router.push("/veterinarian/urgent")}
        />
        <MetricCard
          label="Admission Quarantine"
          value={admissionClearanceCount}
          icon="stethoscope"
          tone={admissionClearanceCount > 0 ? "info" : "neutral"}
          onClick={() => router.push("/veterinarian/admissions")}
        />
        <MetricCard
          label="Today's Schedule"
          value={todayAppointments.length}
          icon="calendar"
          tone="default"
        />
        <MetricCard
          label="Training on Hold"
          value={blockedHorses.length}
          icon="shield"
          tone={blockedHorses.length > 0 ? "warning" : "neutral"}
        />
      </div>

      {/* TIER 3: Two-Column Operational Workspace */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
        {/* Left Column (60%): Today's Agenda */}
        <div className="space-y-4 lg:col-span-7">
          <Panel padded>
            <div className="mb-4 flex items-center justify-between border-b border-[var(--color-border)] pb-3">
              <SectionTitle>Today&apos;s Clinical Agenda</SectionTitle>
              <Pill tone="primary" icon="clock" size="sm">
                13:30 - 15:30
              </Pill>
            </div>

            {loading ? (
              <ListSkeleton rows={4} />
            ) : todayAppointments.length === 0 ? (
              <EmptyState
                icon="calendar"
                title="No clinical appointments today"
                description="No clinical appointments scheduled for today's veterinary window (13:30 – 15:30)."
                action={
                  <Button
                    variant="secondary"
                    size="sm"
                    icon="plus"
                    onClick={() => setIsRoutineModalOpen(true)}
                  >
                    Book Routine Check
                  </Button>
                }
              />
            ) : (
              <div className="divide-y divide-[var(--color-border)]">
                {todayAppointments.map((schedule) => {
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

                  return (
                    <div
                      key={schedule.id}
                      className="flex flex-col gap-3 py-3.5 first:pt-0 last:pb-0 sm:flex-row sm:items-center sm:justify-between"
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
                        {schedule.careType === "URGENT" && (
                          <Pill tone="danger" icon="alert-triangle" size="sm">
                            Urgent
                          </Pill>
                        )}
                        {schedule.careType === "INITIAL" && (
                          <Pill tone="info" icon="stethoscope" size="sm">
                            Initial
                          </Pill>
                        )}
                        {schedule.careType === "ROUTINE" && (
                          <Pill tone="neutral" icon="clock" size="sm">
                            Routine
                          </Pill>
                        )}

                        {schedule.status === "IN_PROGRESS" ? (
                          <Button
                            variant="primary"
                            size="sm"
                            onClick={() => handleStartExam(schedule)}
                          >
                            Resume Exam
                          </Button>
                        ) : schedule.status === "COMPLETED" ? (
                          <Button
                            variant="secondary"
                            size="sm"
                            onClick={() => handleViewFindings(schedule)}
                          >
                            View Findings
                          </Button>
                        ) : (
                          <Button
                            variant="primary"
                            size="sm"
                            onClick={() => handleStartExam(schedule)}
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

        {/* Right Column (40%): Active Emergencies & Medical Hold Watchlist */}
        <div className="space-y-6 lg:col-span-5">
          {/* Card 1: 🚨 Immediate Attention (Active Emergencies) */}
          <Panel
            padded
            tone={activeEmergencies.length > 0 ? "danger" : "default"}
          >
            <div className="mb-3 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <SectionTitle>Immediate Attention</SectionTitle>
                {activeEmergencies.length > 0 && (
                  <Pill tone="danger" size="sm">
                    {activeEmergencies.length} Active
                  </Pill>
                )}
              </div>
              <LinkButton
                href="/veterinarian/urgent"
                variant="tertiary"
                size="sm"
              >
                View All
              </LinkButton>
            </div>

            {loading ? (
              <ListSkeleton rows={2} />
            ) : activeEmergencies.length === 0 ? (
              <div className="flex items-center gap-3 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-3 text-sm text-[var(--color-text-secondary)]">
                <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[var(--color-success-soft)] text-[var(--color-success)]">
                  <Icon name="check" size={16} />
                </span>
                <p className="font-medium text-[var(--color-text-primary)]">
                  No active emergencies
                </p>
              </div>
            ) : (
              <div className="space-y-3">
                {activeEmergencies.map((schedule) => {
                  const horse = horseMap.get(schedule.horseId);
                  const horseName =
                    schedule.candidateName ||
                    horse?.name ||
                    `Horse #${schedule.horseId}`;
                  const stallCode =
                    horse?.currentStallId != null
                      ? stallMap.get(horse.currentStallId)
                      : null;
                  const excerpt =
                    schedule.description ||
                    schedule.notes ||
                    "Emergency examination requested";

                  return (
                    <div
                      key={schedule.id}
                      className="rounded-[var(--radius-md)] border border-[var(--color-danger)] bg-[var(--color-surface)] p-3 text-sm shadow-xs"
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <HorseAvatar
                            name={horseName}
                            image={horse?.imageUrl}
                            size={32}
                            rounded="sm"
                          />
                          <div>
                            <span className="font-semibold text-[var(--color-text-primary)]">
                              {horseName}
                            </span>
                            <span className="ml-2 text-xs text-[var(--color-text-muted)]">
                              {stallCode ? `Stall ${stallCode}` : "Unassigned"}
                            </span>
                          </div>
                        </div>
                        <Pill tone="danger" size="sm">
                          {schedule.status}
                        </Pill>
                      </div>

                      <p className="mt-2 line-clamp-2 text-xs text-[var(--color-text-secondary)]">
                        {excerpt}
                      </p>

                      <div className="mt-3 flex justify-end">
                        <LinkButton
                          href={`/veterinarian/urgent/${schedule.id}`}
                          variant="destructive"
                          size="sm"
                        >
                          Attend Emergency
                        </LinkButton>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </Panel>

          {/* Card 2: ⛔ Training Clearance Watchlist */}
          <Panel padded tone={blockedHorses.length > 0 ? "warning" : "default"}>
            <div className="mb-3 flex items-center gap-2">
              <SectionTitle>Training Clearance Watchlist</SectionTitle>
              {blockedHorses.length > 0 && (
                <Pill tone="warning" size="sm">
                  {blockedHorses.length} Blocked
                </Pill>
              )}
            </div>

            {loading ? (
              <ListSkeleton rows={3} />
            ) : blockedHorses.length === 0 ? (
              <div className="flex items-center gap-3 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-3 text-sm text-[var(--color-text-secondary)]">
                <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[var(--color-success-soft)] text-[var(--color-success)]">
                  <Icon name="check" size={16} />
                </span>
                <p className="font-medium text-[var(--color-text-primary)]">
                  All horses cleared
                </p>
              </div>
            ) : (
              <div className="space-y-3">
                {blockedHorses.map((item) => (
                  <div
                    key={item.id}
                    className="flex flex-col gap-2 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-3 text-sm"
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex items-center gap-2">
                        <HorseAvatar
                          name={item.name}
                          image={horseMap.get(item.id)?.imageUrl}
                          size={32}
                          rounded="sm"
                        />
                        <div>
                          <div className="flex items-center gap-1.5">
                            <span className="font-semibold text-[var(--color-text-primary)]">
                              {item.name}
                            </span>
                            {item.breed && (
                              <span className="text-xs text-[var(--color-text-muted)]">
                                · {item.breed}
                              </span>
                            )}
                          </div>
                          <p className="text-xs text-[var(--color-text-muted)]">
                            Stall: {item.stallCode}
                          </p>
                        </div>
                      </div>
                      <Pill tone="warning" size="sm">
                        BLOCKED
                      </Pill>
                    </div>

                    <div className="rounded-[var(--radius-xs)] bg-[var(--color-warning-soft)] px-2 py-1.5 text-xs text-[var(--color-text-primary)]">
                      <span className="font-medium text-[var(--color-warning)]">
                        Hold Reason:{" "}
                      </span>
                      {item.reason}
                    </div>

                    <div className="flex justify-end">
                      <LinkButton
                        href={`/veterinarian/health?horseId=${item.id}`}
                        variant="tertiary"
                        size="sm"
                      >
                        View Health Profile &rarr;
                      </LinkButton>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Panel>
        </div>
      </div>

      {/* Routine Check Modal */}
      <CreateRoutineModal
        open={isRoutineModalOpen}
        horses={horses}
        onClose={() => setIsRoutineModalOpen(false)}
        onSuccess={() => {
          setIsRoutineModalOpen(false);
          loadData();
        }}
      />
    </div>
  );
}
