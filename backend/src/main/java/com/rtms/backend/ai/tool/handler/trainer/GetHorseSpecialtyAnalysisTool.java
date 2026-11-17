package com.rtms.backend.ai.tool.handler.trainer;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.HorseSpecialtyResponse;
import com.rtms.backend.training.enums.SurfaceType;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import com.rtms.backend.training.repository.projection.SpecialtyWorkoutRow;
import com.rtms.backend.training.service.HorseSpecialtyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class GetHorseSpecialtyAnalysisTool implements AiToolHandler {

    private static final Logger log = LoggerFactory.getLogger(GetHorseSpecialtyAnalysisTool.class);

    private final HorseRepository horseRepository;
    private final HorseSpecialtyService horseSpecialtyService;
    private final TrainingWorkoutRepository trainingWorkoutRepository;
    private final AiToolSupport support;

    @Autowired
    public GetHorseSpecialtyAnalysisTool(HorseRepository horseRepository,
                                        HorseSpecialtyService horseSpecialtyService,
                                        TrainingWorkoutRepository trainingWorkoutRepository,
                                        AiToolSupport support) {
        this.horseRepository = horseRepository;
        this.horseSpecialtyService = horseSpecialtyService;
        this.trainingWorkoutRepository = trainingWorkoutRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getHorseSpecialtyAnalysis";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        String horseNameArg = arguments.get("horseName") != null ? arguments.get("horseName").toString().trim().toLowerCase() : "";
        boolean isFleetRequest = horseNameArg.equals("all") || horseNameArg.equals("all horses") || horseNameArg.equals("everything")
                || (arguments.get("horseId") == null && (arguments.get("horseName") == null || horseNameArg.isBlank()));

        if (isFleetRequest) {
            return rankAllHorsesBySpecialty(arguments, currentUser);
        }

        Horse horse = support.resolveHorse(arguments);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "A horse ID or name must be provided");
        }
        Long horseId = horse.getId();

        Map<Long, HorseSpecialtyResponse> specialties = horseSpecialtyService.getSpecialties(List.of(horseId));
        HorseSpecialtyResponse resp = specialties.get(horseId);

        String preferredSurface = null;
        if (resp != null && resp.preferredSurface() != null) {
            preferredSurface = resp.preferredSurface() == SurfaceType.TURF ? "Grass track (TURF)" : "Dirt track (DIRT)";
        } else {
            List<SpecialtyWorkoutRow> rows = trainingWorkoutRepository.findSpecialtyWorkouts(List.of(horseId), LocalDate.now().minusDays(180));
            if (rows != null && !rows.isEmpty()) {
                Map<SurfaceType, Long> surfaceCounts = rows.stream()
                        .filter(r -> r.surfaceType() != null)
                        .collect(Collectors.groupingBy(SpecialtyWorkoutRow::surfaceType, Collectors.counting()));
                if (!surfaceCounts.isEmpty()) {
                    SurfaceType mostFrequent = surfaceCounts.entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse(null);
                    if (mostFrequent != null) {
                        String surfName = mostFrequent == SurfaceType.TURF ? "Grass track (TURF)" : "Dirt track (DIRT)";
                        preferredSurface = surfName + " (preferred based on " + surfaceCounts.get(mostFrequent) + " most recent actual workouts)";
                    }
                }
            }
            if (preferredSurface == null) {
                preferredSurface = "Insufficient performance data recorded across track surfaces (schedule additional comparison workouts on grass and dirt tracks)";
            }
        }

        AiToolSupport.SpecialtyMetrics metrics = support.computeSpecialtyMetrics(resp);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseName", horse.getName());
        result.put("registrationNumber", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        result.put("horseId", horse.getId());
        result.put("specialtyCount", metrics.specialtyCount());
        result.put("averageRating", metrics.averageRating());
        result.put("bestRating", metrics.bestRating());
        result.put("specialties", metrics.formattedDistances());
        result.put("strongDistances", metrics.strongBands());
        result.put("specialtyBreakdown", metrics.specialtyBreakdown());
        result.put("optimalDistance", (resp != null && !resp.strongDistances().isEmpty()) ? resp.strongDistances() : List.of("Analyzing additional distances"));
        result.put("preferredSurface", preferredSurface);
        result.put("distanceStats", resp != null ? resp.distanceStats() : Collections.emptyList());
        result.put("actionUrl", support.resolveHorseActionUrl(horse.getId(), currentUser));
        return result;
    }

    private Object rankAllHorsesBySpecialty(Map<String, Object> args, AuthenticatedUser currentUser) {
        String sortByArg = args.get("sortBy") != null ? args.get("sortBy").toString().trim().toLowerCase() : "count";
        boolean sortByRating = sortByArg.contains("rating") || sortByArg.contains("score");

        List<Horse> horses;
        if (currentUser != null && "HEAD_TRAINER".equals(currentUser.getRole())) {
            horses = horseRepository.findManagedHorsesByTrainerIdAndStatusIn(
                    currentUser.getUserId(),
                    List.of(HorseStatus.ELIGIBLE, HorseStatus.CANDIDATE)
            );
            if (horses == null || horses.isEmpty()) {
                horses = horseRepository.findAll();
            }
        } else if (currentUser != null && "HORSE_OWNER".equals(currentUser.getRole())) {
            horses = horseRepository.findByOwnerId(currentUser.getUserId());
        } else {
            horses = horseRepository.findAll();
        }

        if (horses == null || horses.isEmpty()) {
            return Map.of(
                    "totalHorses", 0,
                    "horsesWithSpecialties", 0,
                    "rankedHorseList", Collections.emptyList(),
                    "summary", "No horses were found in the system to rank."
            );
        }

        List<Long> horseIds = horses.stream().map(Horse::getId).filter(Objects::nonNull).toList();
        Map<Long, HorseSpecialtyResponse> specialtiesMap = Collections.emptyMap();
        if (horseSpecialtyService != null && !horseIds.isEmpty()) {
            try {
                Map<Long, HorseSpecialtyResponse> queried = horseSpecialtyService.getSpecialties(horseIds);
                if (queried != null) {
                    specialtiesMap = queried;
                }
            } catch (Exception e) {
                log.warn("Error querying horse specialties in batch: {}", e.getMessage());
            }
        }

        List<Map<String, Object>> unrankedList = new ArrayList<>();
        int countWithSpecialties = 0;

        for (Horse h : horses) {
            HorseSpecialtyResponse resp = specialtiesMap.get(h.getId());
            AiToolSupport.SpecialtyMetrics m = support.computeSpecialtyMetrics(resp);
            if (m.specialtyCount() > 0) {
                countWithSpecialties++;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseId", h.getId());
            item.put("horseName", h.getName());
            item.put("registrationNumber", h.getRegistrationNumber() != null ? h.getRegistrationNumber() : "N/A");
            item.put("breed", h.getBreed() != null ? h.getBreed() : "N/A");
            item.put("sex", h.getSex() != null ? h.getSex().name() : "N/A");
            item.put("dateOfBirth", h.getDateOfBirth() != null ? h.getDateOfBirth().toString() : "N/A");
            item.put("specialtyCount", m.specialtyCount());
            item.put("averageRating", m.averageRating());
            item.put("bestRating", m.bestRating());
            item.put("specialties", m.formattedDistances());
            item.put("strongDistances", m.strongBands());
            item.put("preferredSurface", m.preferredSurface());
            item.put("specialtyBreakdown", m.specialtyBreakdown());
            item.put("status", h.getCurrentStatus() != null ? h.getCurrentStatus().name() : "N/A");
            item.put("actionUrl", support.resolveHorseActionUrl(h.getId(), currentUser));
            unrankedList.add(item);
        }

        if (sortByRating) {
            unrankedList.sort(Comparator.comparing((Map<String, Object> m) -> (BigDecimal) m.get("averageRating"), Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(m -> (BigDecimal) m.get("bestRating"), Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Comparator.comparingInt((Map<String, Object> m) -> (Integer) m.get("specialtyCount")).reversed())
                    .thenComparing(m -> (String) m.get("horseName")));
        } else {
            unrankedList.sort(Comparator.comparingInt((Map<String, Object> m) -> (Integer) m.get("specialtyCount")).reversed()
                    .thenComparing((Map<String, Object> m) -> (BigDecimal) m.get("averageRating"), Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(m -> (BigDecimal) m.get("bestRating"), Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(m -> (String) m.get("horseName")));
        }

        List<Map<String, Object>> rankedList = new ArrayList<>();
        int rank = 1;
        for (Map<String, Object> m : unrankedList) {
            Map<String, Object> rankedItem = new LinkedHashMap<>();
            rankedItem.put("rank", rank++);
            rankedItem.putAll(m);
            rankedList.add(rankedItem);
        }

        String criterion = sortByRating
                ? "Average specialty rating (averageRating DESC, WINDOW_DAYS=90, MIN_WORKOUTS=3, MIN_RATING=7.0)"
                : "Specialty distance count combined with average rating (specialtyCount DESC, averageRating DESC)";

        String summary = "Ranking " + rankedList.size() + " horses by "
                + (sortByRating ? "average specialty rating (Average Rating)" : "number of specialties (Specialty Count)")
                + ". " + countWithSpecialties + " horses have identified specialties.";

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalHorses", rankedList.size());
        response.put("horsesWithSpecialties", countWithSpecialties);
        response.put("sortBy", sortByRating ? "RATING" : "COUNT");
        response.put("rankingCriterion", criterion);
        response.put("summary", summary);
        response.put("rankedHorseList", rankedList);
        return response;
    }
}
