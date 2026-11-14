"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { PageHeader } from "@/components/ui/PageHeader";
import { MetricCard } from "@/components/ui/MetricCard";
import { Button, IconButton } from "@/components/ui/Button";
import { Notice } from "@/components/ui/Notice";
import { ScreenLayout } from "@/components/ui/ScreenLayout";
import { useAuth } from "@/context/AuthContext";
import { admissionsApi } from "@/features/admissions/services/api";
import { stableApi } from "@/features/stable/services/stableService";
import { CreateRoutineModal } from "@/features/care/shared/components/CreateRoutineModal";
import { HealthTrendCard } from "@/features/dashboard/components/HealthTrendCard";
import type {
  CareSchedule,
  VetQueueSummary,
} from "@/features/admissions/types";
import type { Horse, StableStall } from "@/features/stable/types";
import {
  filterActiveEmergencies,
  filterBlockedHorses,
  filterTodayAppointments,
  getTodayLocalDate,
} from "../lib/dashboardUtils";
import { VetTodayAgenda } from "./dashboard/VetTodayAgenda";
import { VetImmediateAttention } from "./dashboard/VetImmediateAttention";
import { VetClearanceWatchlist } from "./dashboard/VetClearanceWatchlist";
import { VetExamActivityCard } from "./dashboard/VetExamActivityCard";
import { KaplanMeierSurvivalChart } from "@/features/medical-analytics";
import { useSynchronizedClock } from "@/lib/clockSync";

export function VetDashboardView() {
  const router = useRouter();
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [summary, setSummary] = useState<VetQueueSummary | null>(null);
  const [todaySchedules, setTodaySchedules] = useState<CareSchedule[]>([]);
  const [urgentSchedules, setUrgentSchedules] = useState<CareSchedule[]>([]);
  const [horses, setHorses] = useState<Horse[]>([]);
  const [stalls, setStalls] = useState<StableStall[]>([]);

  const [isRoutineModalOpen, setIsRoutineModalOpen] = useState(false);

  const currentClock = useSynchronizedClock();
  const todayLocal = useMemo(() => {
    const year = currentClock.getFullYear();
    const month = String(currentClock.getMonth() + 1).padStart(2, "0");
    const day = String(currentClock.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  }, [currentClock]);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      // The vet's command center reflects only their own clinical work;
      // the BLOCKED watchlist stays stable-wide (a horse state, not an assignment).
      const [summaryRes, todayRes, urgentRes, horsesRes, stallsRes] =
        await Promise.all([
          admissionsApi.getVetQueueSummary(),
          admissionsApi.getCareSchedules({ date: todayLocal, size: 50, veterinarianId: user?.userId }),
          admissionsApi.getCareSchedules({ careType: "URGENT", size: 50, veterinarianId: user?.userId }),
          stableApi.getHorses(),
          stableApi.getStalls(),
        ]);

      setSummary(summaryRes);
      setTodaySchedules(todayRes.content);
      setUrgentSchedules(urgentRes.content);
      setHorses(horsesRes);
      setStalls(stallsRes);
    } catch (err: unknown) {
      const msg =
        err instanceof Error
          ? err.message
          : "Failed to load veterinary dashboard data";
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, [todayLocal, user]);

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

  const handleStartExam = (schedule: CareSchedule) => {
    if (schedule.careType === "INITIAL") {
      router.push(`/veterinarian/admissions?scheduleId=${schedule.id}`);
    } else if (schedule.purpose === "PREVENTIVE") {
      router.push(`/veterinarian/preventive-care?horseId=${schedule.horseId}`);
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
    <ScreenLayout variant="dashboard">
      {/* TIER 1: Header & Quick Actions */}
      <PageHeader
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

      {/* TIER 3: Two-Column Operational Workspace — agenda and emergencies */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
        <VetTodayAgenda
          loading={loading}
          appointments={todayAppointments}
          horseMap={horseMap}
          stallMap={stallMap}
          onStartExam={handleStartExam}
          onViewFindings={handleViewFindings}
          onBookRoutine={() => setIsRoutineModalOpen(true)}
        />

        <div className="lg:col-span-5">
          <VetImmediateAttention
            loading={loading}
            emergencies={activeEmergencies}
            horseMap={horseMap}
            stallMap={stallMap}
          />
        </div>
      </div>

      {/* TIER 4: Training Clearance Watchlist — full-width horizontal band */}
      <VetClearanceWatchlist
        loading={loading}
        blockedHorses={blockedHorses}
        horseMap={horseMap}
      />

      {/* TIER 5: Trends & Survival EDA Analysis */}
      <VetExamActivityCard />
      <HealthTrendCard hideCaption />
      <KaplanMeierSurvivalChart />

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
    </ScreenLayout>
  );
}
