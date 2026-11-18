package com.rtms.backend.ai.tool.handler.trainer;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.projection.PastWorkoutRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetWorkoutHistoryTool implements AiToolHandler {

    private final UserRepository userRepository;
    private final AiToolSupport support;

    @Autowired
    public GetWorkoutHistoryTool(@Autowired(required = false) UserRepository userRepository,
                                 AiToolSupport support) {
        this.userRepository = userRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getWorkoutHistory";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Horse horse = support.resolveHorse(arguments);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "A horse ID or name must be provided");
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (PastWorkoutRow row : support.pastWorkouts(horse.getId()).stream().limit(AiToolSupport.WORKOUT_HISTORY_LIMIT).toList()) {
            TrainingWorkout w = row.workout();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseName", horse.getName());
            item.put("workoutDate", row.sessionDate().toString());
            item.put("sessionTime", support.formatSessionWindow(row.startTime(), row.endTime()));
            item.put("groupName", support.groupLabel(row.groupName(), row.groupDisbanded()));
            item.put("exerciseName", row.exerciseName());
            item.put("outcome", support.outcome(row));
            if (w.getStatus() == WorkoutStatus.RAN) {
                item.put("actualDistanceMeters", w.getActualDistanceMeters());
                item.put("actualDurationMinutes", w.getActualDurationMinutes());
                item.put("topSpeedKmh", w.getTopSpeedKmh());
                item.put("averageSpeedKmh", w.getAverageSpeedKmh());
                item.put("avgHeartRate", w.getAverageHeartRate());
                item.put("maxHeartRate", w.getMaxHeartRate());
                item.put("recoveryHeartRate", w.getRecoveryHeartRate());
                item.put("evaluationScore", w.getPerformanceRating());
                String groomAssigned = "Unassigned";
                if (w.getAssignedToId() != null && userRepository != null) {
                    groomAssigned = userRepository.findById(w.getAssignedToId()).map(User::getFullName).orElse("Groom #" + w.getAssignedToId());
                }
                item.put("assignedGroomName", groomAssigned);
                item.put("videoUrl", w.getVideoUrl() != null ? w.getVideoUrl() : "");
                item.put("trainerNotes", w.getTrainerFeedback());
            }
            list.add(item);
        }
        return list;
    }
}
