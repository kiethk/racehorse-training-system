package com.rtms.backend.ai.tool.handler.trainer;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.entity.RacingReadinessAssessment;
import com.rtms.backend.training.repository.RacingReadinessAssessmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetRacingReadinessAssessmentTool implements AiToolHandler {

    private final RacingReadinessAssessmentRepository racingReadinessAssessmentRepository;
    private final UserRepository userRepository;
    private final AiToolSupport support;

    @Autowired
    public GetRacingReadinessAssessmentTool(RacingReadinessAssessmentRepository racingReadinessAssessmentRepository,
                                            @Autowired(required = false) UserRepository userRepository,
                                            AiToolSupport support) {
        this.racingReadinessAssessmentRepository = racingReadinessAssessmentRepository;
        this.userRepository = userRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getRacingReadinessAssessment";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Horse horse = support.resolveHorse(arguments);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "A horse ID or name must be provided");
        }
        Long horseId = horse.getId();

        Optional<RacingReadinessAssessment> opt = racingReadinessAssessmentRepository.findTopByHorseIdOrderByCreatedAtDesc(horseId);
        if (opt.isEmpty()) {
            return Map.of("horseName", horse.getName(), "message", "No racing-readiness assessment is available for this horse.");
        }

        RacingReadinessAssessment r = opt.get();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseId", horse.getId());
        result.put("horseName", horse.getName());
        result.put("readinessStatus", r.getReadinessStatus());
        result.put("conformationScore", r.getConformationScore());
        result.put("temperamentScore", r.getTemperamentScore());
        result.put("racingFormScore", r.getGaitQualityScore());
        result.put("estimatedMonthsToRace", r.getEstimatedMonthsToRace());
        result.put("assessmentDate", r.getAssessmentDate());
        result.put("expertReview", r.getRemarks());
        result.put("trainerId", r.getTrainerId());
        String readinessTrainer = "Unknown";
        if (r.getTrainerId() != null && userRepository != null) {
            readinessTrainer = userRepository.findById(r.getTrainerId()).map(User::getFullName).orElse("Trainer #" + r.getTrainerId());
        }
        result.put("trainerName", readinessTrainer);
        result.put("admissionId", r.getAdmissionId());
        return result;
    }
}
