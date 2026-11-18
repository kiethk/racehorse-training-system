package com.rtms.backend.ai.tool.handler.groom;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class GetMyAssignedHorsesTool implements AiToolHandler {

    private final StableStallRepository stableStallRepository;
    private final HorseRepository horseRepository;
    private final AreaRepository areaRepository;
    private final AiToolSupport support;

    @Autowired
    public GetMyAssignedHorsesTool(StableStallRepository stableStallRepository,
                                  HorseRepository horseRepository,
                                  AreaRepository areaRepository,
                                  AiToolSupport support) {
        this.stableStallRepository = stableStallRepository;
        this.horseRepository = horseRepository;
        this.areaRepository = areaRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getMyAssignedHorses";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Long groomId = currentUser.getUserId();
        List<StableStall> stalls = stableStallRepository.findByGroomId(groomId);
        if (stalls.isEmpty()) {
            return Map.of(
                    "message", "You have not been assigned to any stalls at this time.",
                    "assignedStallsCount", 0,
                    "stallsWithHorsesCount", 0,
                    "stallAndHorseList", Collections.emptyList()
            );
        }

        List<Long> stallIds = stalls.stream().map(StableStall::getId).toList();
        List<Horse> horses = horseRepository.findByCurrentStallIdIn(stallIds);
        Map<Long, Horse> horseByStall = horses.stream()
                .filter(h -> h.getCurrentStallId() != null)
                .collect(Collectors.toMap(Horse::getCurrentStallId, h -> h, (a, b) -> a));

        List<Map<String, Object>> horseList = new ArrayList<>();
        for (StableStall stall : stalls) {
            Map<String, Object> item = new LinkedHashMap<>();
            String stallAreaName = "No area assigned";
            if (stall.getAreaId() != null && areaRepository != null) {
                stallAreaName = areaRepository.findById(stall.getAreaId()).map(Area::getName).orElse("Area #" + stall.getAreaId());
            }
            item.put("areaName", stallAreaName);
            item.put("stallCode", stall.getStallCode());
            item.put("stallNumber", stall.getStallNumber());
            item.put("stallStatus", stall.getStatus());

            Horse h = horseByStall.get(stall.getId());
            if (h != null) {
                item.put("horseId", h.getId());
                item.put("horseName", h.getName());
                item.put("registrationNumber", h.getRegistrationNumber() != null ? h.getRegistrationNumber() : "N/A");
                item.put("breed", h.getBreed() != null ? h.getBreed() : "N/A");
                item.put("sex", h.getSex() != null ? h.getSex().name() : "N/A");
                item.put("dateOfBirth", h.getDateOfBirth() != null ? h.getDateOfBirth().toString() : "N/A");
                item.put("horseStatus", h.getCurrentStatus());
                item.put("trainingDecision", h.getTrainingDecision());
                item.put("trainingDecisionReason", h.getTrainingDecisionReason() != null ? h.getTrainingDecisionReason() : "Normal");
            } else {
                item.put("horseInStall", "Stall is currently empty");
            }
            horseList.add(item);
        }

        return Map.of(
                "groomName", support.resolveUserName(currentUser, "Groom"),
                "assignedStallsCount", stalls.size(),
                "stallsWithHorsesCount", horses.size(),
                "stallAndHorseList", horseList
        );
    }
}
