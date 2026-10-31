package com.rtms.backend.training.service;

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
        if (request.getActualDistanceMeters() == null
                || request.getActualDurationMinutes() == null
                || request.getTopSpeedKmh() == null
                || request.getAverageSpeedKmh() == null
                || request.getAverageHeartRate() == null
                || request.getMaxHeartRate() == null
                || request.getRecoveryHeartRate() == null) {
            throw new IllegalArgumentException("All measured results are required!");
        }
        if (request.getPerformanceRating() == null) {
            request.setPerformanceRating(WorkoutPerformanceCalculator.calculate(
                    request.getActualDistanceMeters(),
                    request.getActualDurationMinutes(),
                    request.getTopSpeedKmh(),
                    request.getAverageSpeedKmh(),
                    request.getAverageHeartRate(),
                    request.getMaxHeartRate(),
                    request.getRecoveryHeartRate()));
        }
        if (request.getActualDistanceMeters().compareTo(BigDecimal.ZERO) <= 0
                || request.getActualDurationMinutes().compareTo(BigDecimal.ZERO) <= 0
                || request.getTopSpeedKmh().compareTo(BigDecimal.ZERO) <= 0
                || request.getAverageSpeedKmh().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Distance, duration and speed values must be greater than 0!");
        }
        if (request.getAverageSpeedKmh().compareTo(request.getTopSpeedKmh()) > 0) {
            throw new IllegalArgumentException("Average speed cannot exceed top speed!");
        }
        if (request.getAverageHeartRate() < 30 || request.getAverageHeartRate() > 240
                || request.getMaxHeartRate() < 30 || request.getMaxHeartRate() > 240
                || request.getRecoveryHeartRate() < 30 || request.getRecoveryHeartRate() > 240) {
            throw new IllegalArgumentException("Heart rate values must be between 30 and 240 bpm!");
        }
        if (request.getAverageHeartRate() > request.getMaxHeartRate()) {
            throw new IllegalArgumentException("Average heart rate cannot exceed maximum heart rate!");
        }
        if (request.getPerformanceRating() < 1 || request.getPerformanceRating() > 10) {
            throw new IllegalArgumentException("Performance rating must be between 1 and 10!");
        }
        if (request.getTrainerFeedback() != null && request.getTrainerFeedback().length() > 2000) {
            throw new IllegalArgumentException("Trainer feedback cannot exceed 2000 characters!");
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
