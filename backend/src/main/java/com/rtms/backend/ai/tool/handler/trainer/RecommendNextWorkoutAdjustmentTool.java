package com.rtms.backend.ai.tool.handler.trainer;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.config.TrainingLimits;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.entity.TrainingWorkout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RecommendNextWorkoutAdjustmentTool implements AiToolHandler {

    private final AiToolSupport support;

    @Autowired
    public RecommendNextWorkoutAdjustmentTool(AiToolSupport support) {
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "recommendNextWorkoutAdjustment";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Horse horse = support.resolveHorse(arguments);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "A horse ID or name must be provided");
        }

        boolean isBlocked = support.isHorseInQuarantineOrBlocked(horse);

        List<TrainingWorkout> workouts = support.ranWorkouts(horse.getId());
        TrainingWorkout latest = workouts.isEmpty() ? null : workouts.get(0);

        String latestWorkout;
        String fitnessNotes;
        String recommendedNextWorkout;
        String adjustmentReason;

        if (isBlocked) {
            latestWorkout = "Training suspension in effect for medical reasons/quarantine";
            fitnessNotes = "The horse is BLOCKED or in quarantine and is not physically safe to exercise.";
            recommendedNextWorkout = "Complete rest in the stall (Resting in Stall); postpone scheduling workouts until the veterinarian lifts the medical restriction.";
            adjustmentReason = "Strictly follow biosecurity protocols and protect the health of the herd (BR-Medical-Block).";
        } else if (latest != null) {
            boolean highHeartRate = support.overLimit(
                    latest.getMaxHeartRate() != null ? latest.getMaxHeartRate() : 0,
                    latest.getAverageHeartRate() != null ? latest.getAverageHeartRate() : 0,
                    latest.getRecoveryHeartRate() != null ? latest.getRecoveryHeartRate() : 0, 1.0);

            if (highHeartRate) {
                latestWorkout = "Distance " + (latest.getActualDistanceMeters() != null ? latest.getActualDistanceMeters() + "m" : "standard")
                        + ", maximum heart rate " + latest.getMaxHeartRate() + " bpm, recovery " + latest.getRecoveryHeartRate() + " bpm";
                fitnessNotes = "Heart rate exceeds the system safety limits (maximum " + TrainingLimits.MAX_HEART_RATE_LIMIT
                        + ", average " + TrainingLimits.AVERAGE_HEART_RATE_LIMIT + ", recovery "
                        + TrainingLimits.RECOVERY_HEART_RATE_LIMIT + " bpm).";
                recommendedNextWorkout = "Reduce the distance by 30% and switch to a gentle recovery walk on natural grass (Turf Walking).";
                adjustmentReason = "Allow the heart and muscles to clear lactic acid, reducing the risk of muscle strain or tendon injury.";
            } else {
                latestWorkout = "Successfully completed distance " + (latest.getActualDistanceMeters() != null ? latest.getActualDistanceMeters() + "m" : "standard")
                        + ", with a steady heart rate of " + (latest.getAverageHeartRate() != null ? latest.getAverageHeartRate() + " bpm" : "");
                fitnessNotes = "Excellent fitness, rapid heart-rate recovery, and a good response to the training plan.";
                recommendedNextWorkout = "Increase the distance by 200m or raise the final sprint speed on the dirt track (Dirt Canter).";
                adjustmentReason = "Build endurance and finishing speed in line with the training cycle.";
            }
        } else {
            latestWorkout = "No recent workouts recorded";
            fitnessNotes = "No recent fitness data is available for comparison.";
            recommendedNextWorkout = "A familiarization and basic warm-up exercise (Trotting & light Canter) over 1000m on grass.";
            adjustmentReason = "Assess the horse's initial adaptability before increasing training volume.";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseName", horse.getName());
        result.put("registrationNumber", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        result.put("lastWorkout", latestWorkout);
        result.put("fitnessReview", fitnessNotes);
        result.put("nextRecommendedWorkout", recommendedNextWorkout);
        result.put("adjustmentReason", adjustmentReason);
        result.put("actionUrl", "/trainer/plans");
        return result;
    }
}
