package com.rtms.backend.ai.tool.handler.vet;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.medical.entity.HorseHealthMetric;
import com.rtms.backend.medical.repository.HorseHealthMetricRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class EvaluateTrainingResumptionReadinessTool implements AiToolHandler {

    private final HorseHealthMetricRepository horseHealthMetricRepository;
    private final AiToolSupport support;

    @Autowired
    public EvaluateTrainingResumptionReadinessTool(@Autowired(required = false) HorseHealthMetricRepository horseHealthMetricRepository,
                                                   AiToolSupport support) {
        this.horseHealthMetricRepository = horseHealthMetricRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "evaluateTrainingResumptionReadiness";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Horse horse = support.resolveHorse(arguments);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Please provide a horse ID or name");
        }

        boolean isBlocked = support.isHorseInQuarantineOrBlocked(horse);

        List<HorseHealthMetric> metrics = (horseHealthMetricRepository != null)
                ? horseHealthMetricRepository.findByHorseIdOrderByRecordedAtDesc(horse.getId())
                : Collections.emptyList();
        HorseHealthMetric latestMetric = metrics.isEmpty() ? null : metrics.get(0);

        boolean feverOrHighHr = latestMetric != null && (
                (latestMetric.getTemperature() != null && latestMetric.getTemperature().compareTo(new BigDecimal("38.5")) > 0)
                || (latestMetric.getHeartRate() != null && latestMetric.getHeartRate().compareTo(new BigDecimal("44")) > 0)
        );

        String readinessScore;
        String khuyenNghiBacSi;
        String recommendedTrainingRestriction;

        if (isBlocked || feverOrHighHr) {
            readinessScore = "15/100";
            khuyenNghiBacSi = "Further rest is needed; the horse is not yet physically fit to safely resume training";
            recommendedTrainingRestriction = "Maintain the BLOCKED decision; training on the track is strictly prohibited. Only relaxed walking in the stall is allowed (up to 10 minutes).";
        } else {
            readinessScore = "85/100";
            khuyenNghiBacSi = "Fit for light exercise";
            recommendedTrainingRestriction = "Walking and light trotting (Walking & Trotting) only, for under 20 minutes on natural grass; sprinting at a gallop (Galloping) is strictly prohibited during the first 7 days.";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseName", horse.getName());
        result.put("registrationNumber", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        result.put("currentStatus", horse.getCurrentStatus() != null ? horse.getCurrentStatus().name() : "N/A");
        result.put("trainingDecision", horse.getTrainingDecision() != null ? horse.getTrainingDecision().name() : "N/A");
        result.put("readinessScore", readinessScore);
        result.put("vetRecommendations", khuyenNghiBacSi);
        result.put("proposedTrainingRestriction", recommendedTrainingRestriction);
        result.put("actionUrl", "/veterinarian/preventive-care");
        return result;
    }
}
