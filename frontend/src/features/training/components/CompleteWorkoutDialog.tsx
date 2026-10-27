"use client";

import { useMemo, useState } from "react";
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
  const [trainerFeedback, setTrainerFeedback] = useState(() => workout?.trainerFeedback ?? "");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const telemetry = useMemo(() => {
    const source = isEdit && workout ? workout : preview;
    const actualDistanceMeters = Number(source?.actualDistanceMeters ?? 1000);
    const actualDurationMinutes = Number(source?.actualDurationMinutes ?? 60);
    const topSpeedKmh = Number(source?.topSpeedKmh ?? 50);
    const averageSpeedKmh = Number(source?.averageSpeedKmh ?? 40);
    const averageHeartRate = Number(source?.averageHeartRate ?? 140);
    const maxHeartRate = Number(source?.maxHeartRate ?? 180);
    const recoveryHeartRate = Number(source?.recoveryHeartRate ?? 90);

    const performanceRating = source?.performanceRating ?? calculatePerformanceRating({
      actualDistanceMeters,
      actualDurationMinutes,
      topSpeedKmh,
      averageSpeedKmh,
      averageHeartRate,
      maxHeartRate,
      recoveryHeartRate,
    });

    return {
      actualDistanceMeters,
      actualDurationMinutes,
      topSpeedKmh,
      averageSpeedKmh,
      averageHeartRate,
      maxHeartRate,
      recoveryHeartRate,
      performanceRating,
    };
  }, [isEdit, workout, preview]);

  if (!open || !workout) return null;
  const target = workout;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const payload: CompleteWorkoutRequest = {
        actualDistanceMeters: telemetry.actualDistanceMeters,
        actualDurationMinutes: telemetry.actualDurationMinutes,
        topSpeedKmh: telemetry.topSpeedKmh,
        averageSpeedKmh: telemetry.averageSpeedKmh,
        averageHeartRate: telemetry.averageHeartRate,
        maxHeartRate: telemetry.maxHeartRate,
        recoveryHeartRate: telemetry.recoveryHeartRate,
        performanceRating: Number(telemetry.performanceRating),
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
                {telemetry.performanceRating}
              </span>
              <span className="text-xs text-[var(--color-text-muted)]">/ 10</span>
            </div>
          </div>
          <div className="flex flex-col items-end gap-1">
            <RatingPill rating={telemetry.performanceRating} size="md" />
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
                value={telemetry.actualDistanceMeters}
                className="bg-[var(--color-bg-subtle)] text-[var(--color-text-secondary)] cursor-not-allowed select-none"
              />
            </FormField>
          </div>

          <div>
            <FormField label="Actual duration (minutes)">
              <Input
                type="number"
                readOnly
                value={telemetry.actualDurationMinutes}
                className="bg-[var(--color-bg-subtle)] text-[var(--color-text-secondary)] cursor-not-allowed select-none"
              />
            </FormField>
          </div>

          <div>
            <FormField label="Top speed (km/h)">
              <Input
                type="number"
                readOnly
                value={telemetry.topSpeedKmh}
                className="bg-[var(--color-bg-subtle)] text-[var(--color-text-secondary)] cursor-not-allowed select-none"
              />
            </FormField>
          </div>

          <div>
            <FormField label="Average speed (km/h)">
              <Input
                type="number"
                readOnly
                value={telemetry.averageSpeedKmh}
                className="bg-[var(--color-bg-subtle)] text-[var(--color-text-secondary)] cursor-not-allowed select-none"
              />
            </FormField>
          </div>

          <div>
            <FormField label="Average heart rate (bpm)">
              <Input
                type="number"
                readOnly
                value={telemetry.averageHeartRate}
                className="bg-[var(--color-bg-subtle)] text-[var(--color-text-secondary)] cursor-not-allowed select-none"
              />
            </FormField>
          </div>

          <div>
            <FormField label="Maximum heart rate (bpm)">
              <Input
                type="number"
                readOnly
                value={telemetry.maxHeartRate}
                className="bg-[var(--color-bg-subtle)] text-[var(--color-text-secondary)] cursor-not-allowed select-none"
              />
            </FormField>
          </div>

          <div className="sm:col-span-2">
            <FormField label="Recovery heart rate (bpm)">
              <Input
                type="number"
                readOnly
                value={telemetry.recoveryHeartRate}
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
