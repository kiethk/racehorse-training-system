package com.rtms.backend.ai.tool.handler.vet;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.enums.AreaType;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.enums.TrainingDecision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class GetQuarantineStatusTool implements AiToolHandler {

    private final AreaRepository areaRepository;
    private final StableStallRepository stableStallRepository;
    private final HorseRepository horseRepository;
    private final HealthRecordRepository healthRecordRepository;

    @Autowired
    public GetQuarantineStatusTool(AreaRepository areaRepository,
                                  StableStallRepository stableStallRepository,
                                  HorseRepository horseRepository,
                                  HealthRecordRepository healthRecordRepository) {
        this.areaRepository = areaRepository;
        this.stableStallRepository = stableStallRepository;
        this.horseRepository = horseRepository;
        this.healthRecordRepository = healthRecordRepository;
    }

    @Override
    public String getToolName() {
        return "getQuarantineStatus";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        List<Area> quarantineAreas = areaRepository.findByType(AreaType.QUARANTINE);
        List<Long> qAreaIds = quarantineAreas.stream().map(Area::getId).toList();
        List<StableStall> qStalls = qAreaIds.isEmpty() ? Collections.emptyList()
                : stableStallRepository.findByAreaIdInOrdered(qAreaIds);
        List<Long> qStallIds = qStalls.stream().map(StableStall::getId).toList();

        Map<Long, StableStall> stallMap = qStalls.stream()
                .collect(Collectors.toMap(StableStall::getId, s -> s, (a, b) -> a));

        List<Horse> horsesInQStalls = qStallIds.isEmpty() ? Collections.emptyList()
                : horseRepository.findByCurrentStallIdIn(qStallIds);

        List<Horse> allHorses = horseRepository.findAll();
        List<Horse> blockedHorses = allHorses.stream()
                .filter(h -> h.getTrainingDecision() == TrainingDecision.BLOCKED)
                .toList();

        Map<Long, Horse> uniqueHorses = new LinkedHashMap<>();
        for (Horse h : horsesInQStalls) {
            uniqueHorses.put(h.getId(), h);
        }
        for (Horse h : blockedHorses) {
            uniqueHorses.put(h.getId(), h);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Horse h : uniqueHorses.values()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseName", h.getName());
            item.put("registrationNumber", h.getRegistrationNumber() != null ? h.getRegistrationNumber() : "N/A");
            item.put("currentStatus", h.getCurrentStatus());
            item.put("trainingDecision", "BLOCKED");
            item.put("isTrainingAllowed", false);
            item.put("decisionReason", h.getTrainingDecisionReason() != null ? h.getTrainingDecisionReason() : "Under strict quarantine - training prohibited");

            boolean inQuarantineStall = h.getCurrentStallId() != null && qStallIds.contains(h.getCurrentStallId());
            item.put("inQuarantineArea", inQuarantineStall);
            if (inQuarantineStall && stallMap.containsKey(h.getCurrentStallId())) {
                item.put("stallCode", stallMap.get(h.getCurrentStallId()).getStallCode());
            }

            List<HealthRecord> records = healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(h.getId());
            if (!records.isEmpty()) {
                HealthRecord latest = records.get(0);
                item.put("latestDiagnosis", latest.getDiagnosis());
                item.put("restrictionDetails", latest.getRestrictionDetails());
                item.put("lastExamDate", latest.getExaminedAt());
            }

            result.add(item);
        }

        return Map.of(
                "quarantinedOrRestrictedHorsesCount", result.size(),
                "quarantinedHorseList", result
        );
    }
}
