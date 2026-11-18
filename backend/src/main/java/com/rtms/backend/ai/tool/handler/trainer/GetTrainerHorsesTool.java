package com.rtms.backend.ai.tool.handler.trainer;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.dto.HorseSpecialtyResponse;
import com.rtms.backend.training.service.HorseSpecialtyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetTrainerHorsesTool implements AiToolHandler {

    private static final Logger log = LoggerFactory.getLogger(GetTrainerHorsesTool.class);

    private final HorseRepository horseRepository;
    private final HorseSpecialtyService horseSpecialtyService;
    private final StableStallRepository stableStallRepository;
    private final UserRepository userRepository;
    private final AiToolSupport support;

    @Autowired
    public GetTrainerHorsesTool(HorseRepository horseRepository,
                                HorseSpecialtyService horseSpecialtyService,
                                StableStallRepository stableStallRepository,
                                @Autowired(required = false) UserRepository userRepository,
                                AiToolSupport support) {
        this.horseRepository = horseRepository;
        this.horseSpecialtyService = horseSpecialtyService;
        this.stableStallRepository = stableStallRepository;
        this.userRepository = userRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getTrainerHorses";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Long trainerId = currentUser != null ? currentUser.getUserId() : null;
        if (trainerId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Could not identify the trainer");
        }

        List<Horse> horses = horseRepository.findManagedHorsesByTrainerIdAndStatusIn(
                trainerId,
                List.of(HorseStatus.ELIGIBLE, HorseStatus.CANDIDATE)
        );

        List<Long> trainerHorseIds = horses.stream().map(Horse::getId).filter(Objects::nonNull).toList();
        Map<Long, HorseSpecialtyResponse> trainerSpecialties = Collections.emptyMap();
        if (horseSpecialtyService != null && !trainerHorseIds.isEmpty()) {
            try {
                Map<Long, HorseSpecialtyResponse> queried = horseSpecialtyService.getSpecialties(trainerHorseIds);
                if (queried != null) {
                    trainerSpecialties = queried;
                }
            } catch (Exception e) {
                log.warn("Error loading specialties for the trainer's horse list: {}", e.getMessage());
            }
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Horse h : horses) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseId", h.getId());
            item.put("horseName", h.getName());
            item.put("registrationNumber", h.getRegistrationNumber() != null ? h.getRegistrationNumber() : "N/A");
            item.put("breed", h.getBreed() != null ? h.getBreed() : "N/A");
            item.put("sex", h.getSex() != null ? h.getSex().name() : "N/A");
            item.put("dateOfBirth", h.getDateOfBirth() != null ? h.getDateOfBirth().toString() : "N/A");
            item.put("ownerId", h.getOwnerId());
            String ownerNameOfHorse = "Unknown";
            if (h.getOwnerId() != null && userRepository != null) {
                ownerNameOfHorse = userRepository.findById(h.getOwnerId()).map(User::getFullName).orElse("Horse owner #" + h.getOwnerId());
            }
            item.put("ownerName", ownerNameOfHorse);
            item.put("status", h.getCurrentStatus());
            item.put("trainingDecision", h.getTrainingDecision());
            item.put("decisionReason", h.getTrainingDecisionReason() != null ? h.getTrainingDecisionReason() : "Normal");
            item.put("trainingDecisionReason", h.getTrainingDecisionReason() != null ? h.getTrainingDecisionReason() : "Normal");
            item.put("registryName", h.getRegistryName() != null ? h.getRegistryName() : "N/A");

            StableStall stall = h.getCurrentStallId() != null ? stableStallRepository.findById(h.getCurrentStallId()).orElse(null) : null;
            String stallName = stall != null ? ("Stall " + stall.getStallCode() + " (No. " + stall.getStallNumber() + ")") : "No stall assigned";
            String groomName = "Unassigned";
            String groomPhone = "N/A";
            if (stall != null && stall.getGroomId() != null && userRepository != null) {
                User groomUser = userRepository.findById(stall.getGroomId()).orElse(null);
                if (groomUser != null) {
                    groomName = groomUser.getFullName();
                    if (groomUser.getPhone() != null && !groomUser.getPhone().isBlank()) {
                        groomPhone = groomUser.getPhone();
                    }
                }
            }
            HorseSpecialtyResponse specResp = trainerSpecialties.get(h.getId());
            AiToolSupport.SpecialtyMetrics sm = support.computeSpecialtyMetrics(specResp);

            item.put("stallLocation", stallName);
            item.put("assignedGroomName", groomName);
            item.put("groomPhone", groomPhone);
            item.put("specialtyCount", sm.specialtyCount());
            item.put("averageRating", sm.averageRating());
            item.put("bestRating", sm.bestRating());
            item.put("specialties", sm.formattedDistances());
            item.put("strongDistances", sm.strongBands());
            item.put("preferredSurface", sm.preferredSurface());
            item.put("specialtyBreakdown", sm.specialtyBreakdown());
            item.put("actionUrl", h.getId() != null ? "/trainer/horses/" + h.getId() : "/trainer/horses");
            list.add(item);
        }
        return Map.of(
                "trainerName", support.resolveUserName(currentUser, "Head Trainer"),
                "totalManagedHorses", list.size(),
                "horseList", list
        );
    }
}
