package com.rtms.backend.ai.tool.handler.trainer;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.HorseFitnessTrendItemResponse;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
public class GetHorseFitnessTrendTool implements AiToolHandler {

    private final TrainingWorkoutRepository trainingWorkoutRepository;
    private final AiToolSupport support;

    @Autowired
    public GetHorseFitnessTrendTool(TrainingWorkoutRepository trainingWorkoutRepository, AiToolSupport support) {
        this.trainingWorkoutRepository = trainingWorkoutRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getHorseFitnessTrend";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Horse horse = support.resolveHorse(arguments);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "A horse ID or name must be provided");
        }
        List<HorseFitnessTrendItemResponse> trends = trainingWorkoutRepository.findFitnessTrend(
                horse.getId(), LocalDate.now().minusDays(30), LocalDate.now()
        );
        return Map.of(
                "horseName", horse.getName(),
                "fitnessTrends", trends != null ? trends : Collections.emptyList()
        );
    }
}
