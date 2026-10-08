package com.rtms.backend.dto;

import com.rtms.backend.entity.HorseHealthMetric;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HorseHealthMetricResponse(Long id, Long horseId, Long healthRecordId, LocalDateTime recordedAt,
        BigDecimal heartRate, BigDecimal temperature, BigDecimal weight, BigDecimal respiratoryRate,
        String hydrationStatus, BigDecimal bodyConditionScore, String notes) {
    public static HorseHealthMetricResponse from(HorseHealthMetric metric) {
        return new HorseHealthMetricResponse(metric.getId(), metric.getHorseId(), metric.getHealthRecordId(),
                metric.getRecordedAt(), metric.getHeartRate(), metric.getTemperature(), metric.getWeight(),
                metric.getRespiratoryRate(), metric.getHydrationStatus(), metric.getBodyConditionScore(), metric.getNotes());
    }
}
