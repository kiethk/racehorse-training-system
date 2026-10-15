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
      setPerformanceRating(workout.performanceRating ?? 8);
      setActualDistanceMeters(Number(workout.actualDistanceMeters ?? 1000));
      setActualDurationMinutes(Number(workout.actualDurationMinutes ?? 60));
      setTopSpeedKmh(Number(workout.topSpeedKmh ?? 50));
      setAverageSpeedKmh(Number(workout.averageSpeedKmh ?? 40));
      setAverageHeartRate(Number(workout.averageHeartRate ?? 140));
      setMaxHeartRate(Number(workout.maxHeartRate ?? 180));
      setRecoveryHeartRate(Number(workout.recoveryHeartRate ?? 90));
      setTrainerFeedback(workout.trainerFeedback ?? "");
    } else if (preview) {
      setPerformanceRating(8);
      setActualDistanceMeters(Number(preview.actualDistanceMeters ?? 1000));
      setActualDurationMinutes(Number(preview.actualDurationMinutes ?? 60));
      setTopSpeedKmh(Number(preview.topSpeedKmh ?? 50));
      setAverageSpeedKmh(Number(preview.averageSpeedKmh ?? 40));
      setAverageHeartRate(Number(preview.averageHeartRate ?? 140));
      setMaxHeartRate(Number(preview.maxHeartRate ?? 180));
      setRecoveryHeartRate(Number(preview.recoveryHeartRate ?? 90));
      setTrainerFeedback("");
    }
    setError(null);
  }, [open, isEdit, workout, preview]);

  if (!open || !workout) return null;
  const target = workout;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (performanceRating < 1 || performanceRating > 10) {
      setError("Performance rating must be between 1 and 10.");
      return;
    }
    if (averageSpeedKmh > topSpeedKmh) {
      setError("Average speed cannot exceed top speed.");
      return;
    }
    if (averageHeartRate > maxHeartRate) {
      setError("Average heart rate cannot exceed maximum heart rate.");
      return;
    }

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
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div>
            <FormField label="Performance rating (1–10)" required>
              <Input
                type="number"
                min="1"
                max="10"
                required
                value={performanceRating}
                onChange={(e) => setPerformanceRating(Number(e.target.value))}
              />
            </FormField>
          </div>

          <div>
            <FormField label="Actual distance (metres)" required>
              <Input
                type="number"
                step="10"
                min="1"
                required
                value={actualDistanceMeters}
                onChange={(e) =>
                  setActualDistanceMeters(Number(e.target.value))
                }
              />
            </FormField>
          </div>

          <div>
            <FormField label="Actual duration (minutes)" required>
              <Input
                type="number"
                step="0.1"
                min="0.1"
                required
                value={actualDurationMinutes}
                onChange={(e) =>
                  setActualDurationMinutes(Number(e.target.value))
                }
              />
            </FormField>
          </div>

          <div>
            <FormField label="Top speed (km/h)" required>
              <Input
                type="number"
                step="0.1"
                min="1"
                required
                value={topSpeedKmh}
                onChange={(e) => setTopSpeedKmh(Number(e.target.value))}
              />
            </FormField>
          </div>

          <div>
            <FormField label="Average speed (km/h)" required>
              <Input
                type="number"
                step="0.1"
                min="1"
                required
                value={averageSpeedKmh}
                onChange={(e) => setAverageSpeedKmh(Number(e.target.value))}
              />
            </FormField>
          </div>

          <div>
            <FormField label="Average heart rate (bpm)" required>
              <Input
                type="number"
                min="30"
                max="240"
                required
                value={averageHeartRate}
                onChange={(e) => setAverageHeartRate(Number(e.target.value))}
              />
            </FormField>
          </div>

          <div>
            <FormField label="Maximum heart rate (bpm)" required>
              <Input
                type="number"
                min="30"
                max="240"
                required
                value={maxHeartRate}
                onChange={(e) => setMaxHeartRate(Number(e.target.value))}
              />
            </FormField>
          </div>

          <div>
            <FormField label="Recovery heart rate (bpm)" required>
              <Input
                type="number"
                min="30"
                max="240"
                required
                value={recoveryHeartRate}
                onChange={(e) => setRecoveryHeartRate(Number(e.target.value))}
              />
            </FormField>
          </div>
        </div>

        <div>
          <FormField label="Trainer feedback">
            <Textarea
              rows={3}
              value={trainerFeedback}
              onChange={(e) => setTrainerFeedback(e.target.value)}
              placeholder="Notes about gait, acceleration, or endurance…"
            />
          </FormField>
        </div>
      </form>
    </Modal>
  );
}
