package com.rtms.backend.training.service;

import com.rtms.backend.config.TrainingLimits;
import com.rtms.backend.training.dto.CompleteWorkoutRequest;
import com.rtms.backend.training.entity.TrainingWorkout;

import java.math.BigDecimal;

/**
 * Kiểm tra và ghi chỉ số của một buổi tập, dùng chung cho mô hình session cũ và mô hình nhóm để mọi nơi ghi kết quả đều theo MỘT luật.
 * (Validates and applies the measurements of a workout, shared by the old session model and the group model so every place that
 * records a result follows ONE rule.)
 */
public final class WorkoutResults {

    private WorkoutResults() {
        // lớp tiện ích, không cho khởi tạo (utility class)
    }

    public static void validate(CompleteWorkoutRequest request) {
        require(request.getActualDistanceMeters(), "Actual distance");
        require(request.getActualDurationMinutes(), "Actual duration");
        require(request.getTopSpeedKmh(), "Top speed");
        require(request.getAverageSpeedKmh(), "Average speed");
        require(request.getAverageHeartRate(), "Average heart rate");
        require(request.getMaxHeartRate(), "Maximum heart rate");
        require(request.getRecoveryHeartRate(), "Recovery heart rate");
        if (request.getActualDistanceMeters().compareTo(BigDecimal.ZERO) <= 0
                || request.getActualDurationMinutes().compareTo(BigDecimal.ZERO) <= 0
                || request.getTopSpeedKmh().compareTo(BigDecimal.ZERO) <= 0
                || request.getAverageSpeedKmh().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Distance, duration and speed values must be greater than 0!");
        }
        if (request.getAverageSpeedKmh().compareTo(request.getTopSpeedKmh()) > 0) {
            throw new IllegalArgumentException("Average speed cannot exceed top speed!");
        }
        if (request.getAverageHeartRate() < TrainingLimits.MIN_HEART_RATE_BPM || request.getAverageHeartRate() > TrainingLimits.MAX_HEART_RATE_BPM
                || request.getMaxHeartRate() < TrainingLimits.MIN_HEART_RATE_BPM || request.getMaxHeartRate() > TrainingLimits.MAX_HEART_RATE_BPM
                || request.getRecoveryHeartRate() < TrainingLimits.MIN_HEART_RATE_BPM || request.getRecoveryHeartRate() > TrainingLimits.MAX_HEART_RATE_BPM) {
            throw new IllegalArgumentException("Heart rate values must be between " + TrainingLimits.MIN_HEART_RATE_BPM + " and " + TrainingLimits.MAX_HEART_RATE_BPM + " bpm!");
        }
        if (request.getAverageHeartRate() > request.getMaxHeartRate()) {
            throw new IllegalArgumentException("Average heart rate cannot exceed maximum heart rate!");
        }
        if (request.getTrainerFeedback() != null && request.getTrainerFeedback().length() > TrainingLimits.MAX_TRAINER_FEEDBACK_LENGTH) {
            throw new IllegalArgumentException("Trainer feedback cannot exceed " + TrainingLimits.MAX_TRAINER_FEEDBACK_LENGTH + " characters!");
        }
    }

    private static void require(Object value, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " is required");
        }
    }

    public static void apply(TrainingWorkout workout, CompleteWorkoutRequest request) {
        workout.setActualDistanceMeters(request.getActualDistanceMeters());
        workout.setActualDurationMinutes(request.getActualDurationMinutes());
        workout.setTopSpeedKmh(request.getTopSpeedKmh());
        workout.setAverageSpeedKmh(request.getAverageSpeedKmh());
        workout.setAverageHeartRate(request.getAverageHeartRate());
        workout.setMaxHeartRate(request.getMaxHeartRate());
        workout.setRecoveryHeartRate(request.getRecoveryHeartRate());

        int calculatedRating = WorkoutPerformanceCalculator.calculate(
                request.getActualDistanceMeters(),
                request.getActualDurationMinutes(),
                request.getTopSpeedKmh(),
                request.getAverageSpeedKmh(),
                request.getAverageHeartRate(),
                request.getMaxHeartRate(),
                request.getRecoveryHeartRate());
        workout.setPerformanceRating(calculatedRating);
        workout.setTrainerFeedback(request.getTrainerFeedback());
    }
}
