package com.rtms.backend.training.dto;

import com.rtms.backend.training.entity.TrainingWorkout;

import java.math.BigDecimal;

/**
 * Workout của một chiến mã trong một buổi của nhóm; status RAN nghĩa là đã ghi kết quả.
 * (One horse's Workout in a group Session; status RAN means results were recorded.)
 */
public record SessionWorkoutResponse(Long id,
                                     Long sessionId,
                                     Long horseId,
                                     String status,
                                     BigDecimal actualDistanceMeters,
                                     BigDecimal actualDurationMinutes,
                                     BigDecimal topSpeedKmh,
                                     BigDecimal averageSpeedKmh,
                                     Integer averageHeartRate,
                                     Integer maxHeartRate,
                                     Integer recoveryHeartRate,
                                     Integer performanceRating,
                                     String trainerFeedback) {

    public static SessionWorkoutResponse of(TrainingWorkout workout) {
        return new SessionWorkoutResponse(workout.getId(), workout.getSessionId(), workout.getHorseId(),
                workout.getStatus().name(),
                workout.getActualDistanceMeters(), workout.getActualDurationMinutes(), workout.getTopSpeedKmh(),
                workout.getAverageSpeedKmh(), workout.getAverageHeartRate(), workout.getMaxHeartRate(),
                workout.getRecoveryHeartRate(), workout.getPerformanceRating(), workout.getTrainerFeedback());
    }
}
