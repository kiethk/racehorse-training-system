package com.rtms.backend.ai.tool.handler.manager;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.enums.AreaType;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.dto.HorseSpecialtyResponse;
import com.rtms.backend.training.entity.CourseRun;
import com.rtms.backend.training.enums.CourseRunStatus;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.repository.CourseRunRepository;
import com.rtms.backend.training.service.HorseSpecialtyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class GetClubHorsesDirectoryTool implements AiToolHandler {

    private static final Logger log = LoggerFactory.getLogger(GetClubHorsesDirectoryTool.class);

    private final HorseRepository horseRepository;
    private final AreaRepository areaRepository;
    private final StableStallRepository stableStallRepository;
    private final UserRepository userRepository;
    private final CourseRunRepository courseRunRepository;
    private final HorseSpecialtyService horseSpecialtyService;
    private final AiToolSupport support;

    @Autowired
    public GetClubHorsesDirectoryTool(HorseRepository horseRepository,
                                      AreaRepository areaRepository,
                                      StableStallRepository stableStallRepository,
                                      @Autowired(required = false) UserRepository userRepository,
                                      @Autowired(required = false) CourseRunRepository courseRunRepository,
                                      HorseSpecialtyService horseSpecialtyService,
                                      AiToolSupport support) {
        this.horseRepository = horseRepository;
        this.areaRepository = areaRepository;
        this.stableStallRepository = stableStallRepository;
        this.userRepository = userRepository;
        this.courseRunRepository = courseRunRepository;
        this.horseSpecialtyService = horseSpecialtyService;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getClubHorsesDirectory";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        List<Horse> allHorses = horseRepository.findAll();
        String statusFilter = arguments.get("status") != null ? arguments.get("status").toString().trim() : null;
        String keyword = arguments.get("keyword") != null ? arguments.get("keyword").toString().trim().toLowerCase() : null;

        List<Area> quarantineAreas = areaRepository != null ? areaRepository.findByType(AreaType.QUARANTINE) : Collections.emptyList();
        Set<Long> quarantineAreaIds = quarantineAreas.stream().map(Area::getId).collect(Collectors.toSet());

        Set<Long> quarantineStallIds = Collections.emptySet();
        if (stableStallRepository != null && !quarantineAreaIds.isEmpty()) {
            quarantineStallIds = stableStallRepository.findByAreaIdInOrdered(new ArrayList<>(quarantineAreaIds))
                    .stream().map(StableStall::getId).collect(Collectors.toSet());
        }

        List<Long> allHorseIds = allHorses.stream().map(Horse::getId).filter(Objects::nonNull).toList();
        Map<Long, HorseSpecialtyResponse> dirSpecialties = Collections.emptyMap();
        if (horseSpecialtyService != null && !allHorseIds.isEmpty()) {
            try {
                Map<Long, HorseSpecialtyResponse> queried = horseSpecialtyService.getSpecialties(allHorseIds);
                if (queried != null) {
                    dirSpecialties = queried;
                }
            } catch (Exception e) {
                log.warn("Failed to load specialties for the horse directory: {}", e.getMessage());
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Horse h : allHorses) {
            String ownerName = "Unknown";
            if (h.getOwnerId() != null && userRepository != null) {
                ownerName = userRepository.findById(h.getOwnerId()).map(User::getFullName).orElse("Owner #" + h.getOwnerId());
            }

            boolean isQuarantineStall = h.getCurrentStallId() != null && quarantineStallIds.contains(h.getCurrentStallId());
            boolean isBlocked = h.getTrainingDecision() == TrainingDecision.BLOCKED;

            boolean hasActivePlan = false;
            if (courseRunRepository != null) {
                List<CourseRun> activePlans = courseRunRepository.findCourseRunsOfHorse(
                        h.getId(), List.of(CourseRunStatus.ACTIVE));
                hasActivePlan = !activePlans.isEmpty();
            }

            String statusCode;
            String statusLabel;
            if (isQuarantineStall) {
                statusCode = "QUARANTINE";
                statusLabel = "Medical quarantine (QUARANTINE)";
            } else if (isBlocked) {
                statusCode = "RESTING";
                statusLabel = "Medical rest (RESTING)";
            } else if (hasActivePlan) {
                statusCode = "IN_TRAINING";
                statusLabel = "In training (IN_TRAINING)";
            } else if (h.getCurrentStatus() == HorseStatus.ELIGIBLE) {
                statusCode = "ACTIVE";
                statusLabel = "Active (ACTIVE)";
            } else {
                statusCode = h.getCurrentStatus() != null ? h.getCurrentStatus().name() : "N/A";
                statusLabel = statusCode;
            }

            if (statusFilter != null && !statusFilter.isBlank()) {
                boolean match = statusCode.equalsIgnoreCase(statusFilter)
                        || statusLabel.toUpperCase().contains(statusFilter.toUpperCase())
                        || (statusFilter.equalsIgnoreCase("ACTIVE") && (statusCode.equals("ACTIVE") || statusCode.equals("IN_TRAINING") || h.getCurrentStatus() == HorseStatus.ELIGIBLE))
                        || (statusFilter.equalsIgnoreCase("QUARANTINE") && (isQuarantineStall || isBlocked))
                        || (statusFilter.equalsIgnoreCase("RESTING") && isBlocked)
                        || (statusFilter.equalsIgnoreCase("IN_TRAINING") && hasActivePlan);
                if (!match) {
                    continue;
                }
            }

            if (keyword != null && !keyword.isBlank()) {
                boolean matchHorseName = h.getName() != null && h.getName().toLowerCase().contains(keyword);
                boolean matchOwnerName = ownerName.toLowerCase().contains(keyword);
                boolean matchRegNo = h.getRegistrationNumber() != null && h.getRegistrationNumber().toLowerCase().contains(keyword);
                if (!matchHorseName && !matchOwnerName && !matchRegNo) {
                    continue;
                }
            }

            String stallName = support.resolveStallName(h.getCurrentStallId());
            String trainerName = support.resolveHorseTrainerName(h);

            HorseSpecialtyResponse specResp = dirSpecialties.get(h.getId());
            AiToolSupport.SpecialtyMetrics sm = support.computeSpecialtyMetrics(specResp);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseId", h.getId());
            item.put("horseName", h.getName());
            item.put("registrationNumber", h.getRegistrationNumber() != null ? h.getRegistrationNumber() : "N/A");
            item.put("breed", h.getBreed() != null ? h.getBreed() : "N/A");
            item.put("sex", h.getSex() != null ? h.getSex().name() : "N/A");
            item.put("dateOfBirth", h.getDateOfBirth() != null ? h.getDateOfBirth().toString() : "N/A");
            item.put("trainingDecisionReason", h.getTrainingDecisionReason() != null ? h.getTrainingDecisionReason() : "Normal");
            item.put("decisionReason", h.getTrainingDecisionReason() != null ? h.getTrainingDecisionReason() : "Normal");
            item.put("registryName", h.getRegistryName() != null ? h.getRegistryName() : "N/A");
            item.put("ownerName", ownerName);
            item.put("status", statusLabel);
            item.put("currentStatus", statusLabel);
            item.put("trainingDecision", (isQuarantineStall || isBlocked) ? "BLOCKED" : (h.getTrainingDecision() != null ? h.getTrainingDecision().name() : "ALLOWED"));
            item.put("stallLocation", stallName);
            item.put("assignedTrainer", trainerName);
            item.put("specialtyCount", sm.specialtyCount());
            item.put("averageRating", sm.averageRating());
            item.put("bestRating", sm.bestRating());
            item.put("specialties", sm.formattedDistances());
            item.put("strongDistances", sm.strongBands());
            item.put("preferredSurface", sm.preferredSurface());
            item.put("specialtyBreakdown", sm.specialtyBreakdown());
            item.put("totalClubHorses", allHorses.size());
            item.put("actionUrl", h.getId() != null ? "/manager/horses/" + h.getId() : "/manager/horses");
            result.add(item);
        }
        return result;
    }
}
