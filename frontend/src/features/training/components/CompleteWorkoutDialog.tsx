"use client";

import { useEffect, useState } from "react";
import {
  Button,
  FormField,
  Input,
  Modal,
  Notice,
  Textarea,
} from "@/components/ui";
import { displayError, formatDate, formatTime } from "@/lib/display";
import { toast } from "@/lib/toast";
import { calculatePerformanceRating } from "../lib/performanceCalculator";
import { RatingPill } from "./RatingPill";
import { trainingService } from "../services/trainingService";
import type {
  CompleteWorkoutRequest,
  PlanWorkoutItemResponse,
  WorkoutResultPreviewResponse,
} from "../types";

const FORM_ID = "complete-workout-form";

interface CompleteWorkoutDialogProps {
  workout: PlanWorkoutItemResponse | null;
  preview: WorkoutResultPreviewResponse | null;
  isEdit?: boolean;
  open: boolean;
  onClose: () => void;
  onSuccess: () => Promise<void> | void;
}

export function CompleteWorkoutDialog({
  workout,
  preview,
  isEdit = false,
  open,
  onClose,
  onSuccess,
}: CompleteWorkoutDialogProps) {
  const [performanceRating, setPerformanceRating] = useState<number>(8);
  const [actualDistanceMeters, setActualDistanceMeters] =
    useState<number>(1000);
  const [actualDurationMinutes, setActualDurationMinutes] =
    useState<number>(60);
  const [topSpeedKmh, setTopSpeedKmh] = useState<number>(50);
  const [averageSpeedKmh, setAverageSpeedKmh] = useState<number>(40);
  const [averageHeartRate, setAverageHeartRate] = useState<number>(140);
  const [maxHeartRate, setMaxHeartRate] = useState<number>(180);
  const [recoveryHeartRate, setRecoveryHeartRate] = useState<number>(90);
  const [trainerFeedback, setTrainerFeedback] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Sync state when dialog opens with workout or preview
  useEffect(() => {
    if (!open) return;
    if (isEdit && workout) {
      const dist = Number(workout.actualDistanceMeters ?? 1000);
      const dur = Number(workout.actualDurationMinutes ?? 60);
      const top = Number(workout.topSpeedKmh ?? 50);
      const avg = Number(workout.averageSpeedKmh ?? 40);
      const hrAvg = Number(workout.averageHeartRate ?? 140);
      const hrMax = Number(workout.maxHeartRate ?? 180);
      const hrRec = Number(workout.recoveryHeartRate ?? 90);

      const calculated = calculatePerformanceRating({
        actualDistanceMeters: dist,
        actualDurationMinutes: dur,
        topSpeedKmh: top,
        averageSpeedKmh: avg,
        averageHeartRate: hrAvg,
        maxHeartRate: hrMax,
        recoveryHeartRate: hrRec,
      });

      setActualDistanceMeters(dist);
      setActualDurationMinutes(dur);
      setTopSpeedKmh(top);
      setAverageSpeedKmh(avg);
      setAverageHeartRate(hrAvg);
      setMaxHeartRate(hrMax);
      setRecoveryHeartRate(hrRec);
      setPerformanceRating(workout.performanceRating ?? calculated);
      setTrainerFeedback(workout.trainerFeedback ?? "");
    } else if (preview) {
      const dist = Number(preview.actualDistanceMeters ?? 1000);
      const dur = Number(preview.actualDurationMinutes ?? 60);
      const top = Number(preview.topSpeedKmh ?? 50);
      const avg = Number(preview.averageSpeedKmh ?? 40);
      const hrAvg = Number(preview.averageHeartRate ?? 140);
      const hrMax = Number(preview.maxHeartRate ?? 180);
      const hrRec = Number(preview.recoveryHeartRate ?? 90);

      const calculated = preview.performanceRating ?? calculatePerformanceRating({
        actualDistanceMeters: dist,
        actualDurationMinutes: dur,
        topSpeedKmh: top,
        averageSpeedKmh: avg,
        averageHeartRate: hrAvg,
        maxHeartRate: hrMax,
        recoveryHeartRate: hrRec,
      });

      setActualDistanceMeters(dist);
      setActualDurationMinutes(dur);
      setTopSpeedKmh(top);
      setAverageSpeedKmh(avg);
      setAverageHeartRate(hrAvg);
      setMaxHeartRate(hrMax);
      setRecoveryHeartRate(hrRec);
      setPerformanceRating(calculated);
      setTrainerFeedback("");
    }
    setError(null);
  }, [open, isEdit, workout, preview]);

  if (!open || !workout) return null;
  const target = workout;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const payload: CompleteWorkoutRequest = {
        actualDistanceMeters,
        actualDurationMinutes,
        topSpeedKmh,
        averageSpeedKmh,
        averageHeartRate,
        maxHeartRate,
        recoveryHeartRate,
        performanceRating: Number(performanceRating),
        trainerFeedback: trainerFeedback.trim() || null,
      };

      if (isEdit) {
        await trainingService.updateWorkoutResult(target.workoutId, payload);
        toast.success("Workout result updated.");
      } else {
        await trainingService.completeWorkout(target.workoutId, payload);
        toast.success("Workout result recorded.");
      }
      await onSuccess();
      onClose();
    } catch (err) {
      setError(
        displayError(
          err,
          isEdit
            ? "Unable to update workout results."
            : "Unable to complete the workout.",
        ),
      );
    } finally {
      setSubmitting(false);
    }
  }

  const dialogTitle = isEdit
    ? `Edit workout result #${workout.workoutId}`
    : `Complete workout result #${workout.workoutId}`;

  const submitLabel = isEdit ? "Update" : "Record";

  return (
    <Modal
      open
      onClose={onClose}
      dismissible={!submitting}
      title={dialogTitle}
      description={`${workout.subjectName} · ${formatDate(workout.lotDate)} (${formatTime(workout.startTime)} – ${formatTime(workout.endTime)})`}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button
            variant="primary"
            type="submit"
            form={FORM_ID}
            loading={submitting}
          >
            {submitLabel}
          </Button>
        </>
      }
    >
      <form id={FORM_ID} onSubmit={handleSubmit} className="space-y-4">
        {error && <Notice tone="error">{error}</Notice>}

        {/* Performance Rating Score Banner */}
        <div className="flex items-center justify-between rounded-lg border border-[var(--color-border)] bg-[var(--color-card)] p-3.5">
          <div>
            <div className="text-xs font-medium uppercase tracking-wider text-[var(--color-text-muted)]">
              Calculated Performance Rating
            </div>
            <div className="mt-1 flex items-baseline gap-2">
              <span className="text-2xl font-bold text-[var(--color-text)]">
                {performanceRating}
              </span>
              <span className="text-xs text-[var(--color-text-muted)]">/ 10</span>
            </div>
          </div>
          <div className="flex flex-col items-end gap-1">
            <RatingPill rating={performanceRating} size="md" />
            <span className="text-[11px] text-[var(--color-text-muted)]">
              Speed · Cardio · Efficiency
            </span>
          </div>
        </div>

        {/* Telemetry Metrics Grid (Read-only) */}
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div>
            <FormField label="Actual distance (metres)">
              <Input
                type="number"
                readOnly
                value={actualDistanceMeters}
                className="bg-[var(--color-bg-subtle)] text-[var(--color-text-secondary)] cursor-not-allowed select-none"
              />
            </FormField>
          </div>

          <div>
            <FormField label="Actual duration (minutes)">
              <Input
                type="number"
                readOnly
                value={actualDurationMinutes}
                className="bg-[var(--color-bg-subtle)] text-[var(--color-text-secondary)] cursor-not-allowed select-none"
              />
            </FormField>
          </div>

          <div>
            <FormField label="Top speed (km/h)">
              <Input
                type="number"
                readOnly
                value={topSpeedKmh}
                className="bg-[var(--color-bg-subtle)] text-[var(--color-text-secondary)] cursor-not-allowed select-none"
              />
            </FormField>
          </div>

          <div>
            <FormField label="Average speed (km/h)">
              <Input
                type="number"
                readOnly
                value={averageSpeedKmh}
                className="bg-[var(--color-bg-subtle)] text-[var(--color-text-secondary)] cursor-not-allowed select-none"
              />
            </FormField>
          </div>

          <div>
            <FormField label="Average heart rate (bpm)">
              <Input
                type="number"
                readOnly
                value={averageHeartRate}
                className="bg-[var(--color-bg-subtle)] text-[var(--color-text-secondary)] cursor-not-allowed select-none"
              />
            </FormField>
          </div>

          <div>
            <FormField label="Maximum heart rate (bpm)">
              <Input
                type="number"
                readOnly
                value={maxHeartRate}
                className="bg-[var(--color-bg-subtle)] text-[var(--color-text-secondary)] cursor-not-allowed select-none"
              />
            </FormField>
          </div>

          <div className="sm:col-span-2">
            <FormField label="Recovery heart rate (bpm)">
              <Input
                type="number"
                readOnly
                value={recoveryHeartRate}
                className="bg-[var(--color-bg-subtle)] text-[var(--color-text-secondary)] cursor-not-allowed select-none"
              />
            </FormField>
          </div>
        </div>

        {/* Trainer Feedback (Only Editable Input) */}
        <div>
          <FormField label="Trainer feedback">
            <Textarea
              rows={3}
              value={trainerFeedback}
              onChange={(e) => setTrainerFeedback(e.target.value)}
              placeholder="Notes about gait, acceleration, endurance, or recovery attitude…"
              autoFocus
            />
          </FormField>
        </div>
      </form>
    </Modal>
  );
}
