package com.rtms.backend.ai.tool.handler.trainer;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.config.TrainingLimits;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.projection.PastWorkoutRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
public class AnalyzeWorkoutLoadAndOvertrainingRiskTool implements AiToolHandler {

    private final AiToolSupport support;

    @Autowired
    public AnalyzeWorkoutLoadAndOvertrainingRiskTool(AiToolSupport support) {
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "analyzeWorkoutLoadAndOvertrainingRisk";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Horse horse = support.resolveHorse(arguments);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "A horse ID or name must be provided");
        }

        LocalDate fourteenDaysAgo = LocalDate.now().minusDays(14);
        List<TrainingWorkout> recentWorkouts = support.pastWorkouts(horse.getId()).stream()
                .filter(row -> row.workout().getStatus() == WorkoutStatus.RAN && !row.sessionDate().isBefore(fourteenDaysAgo))
                .map(PastWorkoutRow::workout)
                .toList();

        double totalDistanceMeters = 0.0;
        int highIntensityCount = 0;
        int sumHr = 0;
        int hrCount = 0;
        int maxHr = 0;
        int maxRecoveryHr = 0;

        for (TrainingWorkout w : recentWorkouts) {
            if (w.getActualDistanceMeters() != null) {
                totalDistanceMeters += w.getActualDistanceMeters().doubleValue();
            }
            Integer avgHr = w.getAverageHeartRate();
            if (avgHr != null && avgHr > 0) {
                sumHr += avgHr;
                hrCount++;
            }
            Integer mHr = w.getMaxHeartRate();
            if (mHr != null && mHr > maxHr) {
                maxHr = mHr;
            }
            Integer rHr = w.getRecoveryHeartRate();
            if (rHr != null && rHr > maxRecoveryHr) {
                maxRecoveryHr = rHr;
            }
            if ((mHr != null && mHr >= AiToolSupport.HIGH_INTENSITY_SHARE * TrainingLimits.MAX_HEART_RATE_LIMIT)
                    || (w.getTopSpeedKmh() != null && w.getTopSpeedKmh().doubleValue() >= 55.0)) {
                highIntensityCount++;
            }
        }

        int avgHeartRate = hrCount > 0 ? (int) Math.round((double) sumHr / hrCount) : 0;
        String totalDistanceLast14Days = totalDistanceMeters >= 1000.0
                ? String.format(Locale.US, "%.1f km", totalDistanceMeters / 1000.0)
                : String.format(Locale.US, "%.0f m", totalDistanceMeters);

        boolean isBlocked = support.isHorseInQuarantineOrBlocked(horse);
        String riskAssessment;
        String khuyenNghiHuanLuyen;

        if (isBlocked) {
            riskAssessment = "High";
            khuyenNghiHuanLuyen = "The horse is under medical quarantine or subject to a training restriction (BLOCKED). Stop all strenuous exercise, follow the treatment plan, and monitor recovery in the stall.";
        } else if (support.overLimit(maxHr, avgHeartRate, maxRecoveryHr, 1.0) || highIntensityCount >= 5) {
            riskAssessment = "High";
            khuyenNghiHuanLuyen = "The workload over the past 14 days has reached the overload threshold. Reduce the distance of the next exercise by 30-50%, add recovery walks on grass, and measure heart rate before the workout.";
        } else if (support.overLimit(maxHr, avgHeartRate, maxRecoveryHr, AiToolSupport.HIGH_INTENSITY_SHARE) || highIntensityCount >= 3) {
            riskAssessment = "Medium";
            khuyenNghiHuanLuyen = "Training intensity is at a stable, moderate level. Maintain a schedule alternating workouts with recovery days, and closely monitor the horse's gait after completing long distances.";
        } else {
            riskAssessment = "Low";
            khuyenNghiHuanLuyen = "Fitness and heart-rate recovery are excellent. The horse can maintain or slightly increase the intensity of sprint and track-technique exercises.";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseName", horse.getName());
        result.put("registrationNumber", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        result.put("totalDistance14Days", totalDistanceLast14Days);
        result.put("workoutCount", recentWorkouts.size());
        result.put("avgHeartRate", avgHeartRate > 0 ? avgHeartRate + " bpm" : "N/A");
        result.put("maxHeartRate", maxHr > 0 ? maxHr + " bpm" : "N/A");
        result.put("highIntensitySessions", highIntensityCount);
        result.put("riskAssessment", riskAssessment);
        result.put("trainingRecommendations", khuyenNghiHuanLuyen);
        result.put("actionUrl", "/trainer/dashboard");
        return result;
    }
}
