package com.rtms.backend.ai.tool.handler.manager;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.enums.StallStatus;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class GetTrainerAreaDistributionTool implements AiToolHandler {

    private final AreaRepository areaRepository;
    private final StableStallRepository stableStallRepository;
    private final UserRepository userRepository;

    @Autowired
    public GetTrainerAreaDistributionTool(AreaRepository areaRepository,
                                          StableStallRepository stableStallRepository,
                                          @Autowired(required = false) UserRepository userRepository) {
        this.areaRepository = areaRepository;
        this.stableStallRepository = stableStallRepository;
        this.userRepository = userRepository;
    }

    @Override
    public String getToolName() {
        return "getTrainerAreaDistribution";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        List<Area> areas = areaRepository.findAll();
        List<StableStall> allStalls = stableStallRepository.findAll();

        Map<Long, List<StableStall>> stallsByArea = allStalls.stream()
                .collect(Collectors.groupingBy(StableStall::getAreaId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Area area : areas) {
            List<StableStall> stalls = stallsByArea.getOrDefault(area.getId(), Collections.emptyList());
            Set<Long> groomIds = stalls.stream()
                    .map(StableStall::getGroomId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            String trainerName = "No trainer assigned";
            if (area.getTrainerId() != null && userRepository != null) {
                trainerName = userRepository.findById(area.getTrainerId()).map(User::getFullName).orElse("Trainer #" + area.getTrainerId());
            }

            long occupiedCount = stalls.stream().filter(s -> s.getStatus() == StallStatus.OCCUPIED).count();
            long vacantCount = stalls.size() - occupiedCount;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("areaName", area.getName());
            map.put("areaCode", area.getCode());
            map.put("areaType", area.getType());
            map.put("assignedTrainer", trainerName);
            map.put("totalStalls", stalls.size());
            map.put("occupiedStallsCount", occupiedCount);
            map.put("vacantStallsCount", vacantCount);
            map.put("assignedGroomCount", groomIds.size());
            result.add(map);
        }
        return result;
    }
}
