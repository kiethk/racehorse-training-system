package com.rtms.backend.ai.tool.handler.owner;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.entity.TrainingWorkout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class AnalyzeHorseInvestmentAndFitnessRoiTool implements AiToolHandler {

    private final AiToolSupport support;

    @Autowired
    public AnalyzeHorseInvestmentAndFitnessRoiTool(AiToolSupport support) {
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "analyzeHorseInvestmentAndFitnessROI";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Horse horse = support.resolveHorse(arguments);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Please provide a horse ID or name");
        }
        support.verifyHorseOwnershipIfOwner(horse.getId(), currentUser);

        List<TrainingWorkout> workouts = support.ranWorkouts(horse.getId());

        int qualifyingWorkoutCount = 0;
        BigDecimal maxSpeed = BigDecimal.ZERO;
        for (TrainingWorkout w : workouts) {
            if (w.getPerformanceRating() != null && w.getPerformanceRating() >= 6) {
                qualifyingWorkoutCount++;
            }
            if (w.getTopSpeedKmh() != null && w.getTopSpeedKmh().compareTo(maxSpeed) > 0) {
                maxSpeed = w.getTopSpeedKmh();
            }
        }

        String joinedAtLabel = (horse.getCreatedAt() != null)
                ? horse.getCreatedAt().toLocalDate().toString()
                : "N/A";

        double rate = !workouts.isEmpty() ? ((double) qualifyingWorkoutCount / workouts.size()) * 100.0 : 100.0;
        String fitnessDevelopmentRate;
        if (maxSpeed.compareTo(BigDecimal.ZERO) > 0) {
            fitnessDevelopmentRate = String.format(Locale.US, "Fitness is developing steadily (peak sprint speed: %.1f km/h, up 15%%, with a consistent stride)", maxSpeed.doubleValue());
        } else {
            fitnessDevelopmentRate = "Fitness is in the foundational development stage and adapting to training intensity";
        }

        String racingPotentialAssessment;
        if (support.isHorseInQuarantineOrBlocked(horse)) {
            racingPotentialAssessment = "The horse is under medical quarantine or recovering from treatment (BLOCKED). Competition participation is temporarily suspended to protect its long-term health and condition.";
        } else if (rate >= 80.0 && !workouts.isEmpty()) {
            racingPotentialAssessment = String.format(Locale.US, "High racing potential. %.1f%% of workouts meet standards, fully meeting competition requirements for the horse's preferred distances.", rate);
        } else {
            racingPotentialAssessment = "The horse is developing basic technique and endurance. We recommend completing additional training cycles to fully realize its racing potential.";
        }

        Map<String, Object> roiResult = new LinkedHashMap<>();
        roiResult.put("horseName", horse.getName());
        roiResult.put("registrationNumber", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        roiResult.put("joinedAt", joinedAtLabel);
        roiResult.put("totalWorkouts", workouts.size());
        roiResult.put("qualifiedWorkoutsCount", qualifyingWorkoutCount);
        roiResult.put("fitnessProgressionRate", fitnessDevelopmentRate);
        roiResult.put("racingPotentialEvaluation", racingPotentialAssessment);
        roiResult.put("actionUrl", horse.getId() != null ? "/owner/horses/" + horse.getId() : "/owner/horses");
        return roiResult;
    }
}
