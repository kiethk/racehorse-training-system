package com.rtms.backend.ai.tool.handler.trainer;

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
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class GetTrainerAssignedGroomsTool implements AiToolHandler {

    private final AreaRepository areaRepository;
    private final StableStallRepository stableStallRepository;
    private final HorseRepository horseRepository;
    private final UserRepository userRepository;
    private final AiToolSupport support;

    @Autowired
    public GetTrainerAssignedGroomsTool(AreaRepository areaRepository,
                                       StableStallRepository stableStallRepository,
                                       HorseRepository horseRepository,
                                       @Autowired(required = false) UserRepository userRepository,
                                       AiToolSupport support) {
        this.areaRepository = areaRepository;
        this.stableStallRepository = stableStallRepository;
        this.horseRepository = horseRepository;
        this.userRepository = userRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getTrainerAssignedGrooms";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Long trainerId = currentUser.getUserId();
        List<Area> areas = areaRepository.findAll().stream()
                .filter(a -> Objects.equals(a.getTrainerId(), trainerId))
                .toList();

        if (areas.isEmpty()) {
            return Map.of(
                    "message", "The trainer is not currently assigned to manage any stable areas.",
                    "groomList", Collections.emptyList()
            );
        }

        List<Long> areaIds = areas.stream().map(Area::getId).toList();
        List<StableStall> stalls = stableStallRepository.findByAreaIdInOrdered(areaIds);
        Set<Long> groomIds = stalls.stream()
                .map(StableStall::getGroomId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Horse> managedHorses = horseRepository.findManagedHorsesByTrainerIdAndStatusIn(
                trainerId, List.of(HorseStatus.ELIGIBLE, HorseStatus.CANDIDATE));
        Map<Long, String> horseByStall = managedHorses.stream()
                .filter(h -> h.getCurrentStallId() != null)
                .collect(Collectors.toMap(Horse::getCurrentStallId, Horse::getName, (a, b) -> a));

        List<Map<String, Object>> groomList = new ArrayList<>();
        for (Long groomId : groomIds) {
            User groomUser = userRepository != null ? userRepository.findById(groomId).orElse(null) : null;
            String groomName = groomUser != null && groomUser.getFullName() != null && !groomUser.getFullName().isBlank()
                    ? groomUser.getFullName() : "Groom #" + groomId;
            String groomPhone = groomUser != null && groomUser.getPhone() != null && !groomUser.getPhone().isBlank()
                    ? groomUser.getPhone().trim() : "Phone number not updated";
            String groomEmail = groomUser != null && groomUser.getEmail() != null && !groomUser.getEmail().isBlank()
                    ? groomUser.getEmail().trim() : "No email available";

            List<StableStall> groomStalls = stalls.stream()
                    .filter(s -> Objects.equals(s.getGroomId(), groomId))
                    .toList();
            List<String> stallCodes = groomStalls.stream().map(StableStall::getStallCode).toList();
            List<String> horsesTakingCareOf = groomStalls.stream()
                    .map(s -> horseByStall.get(s.getId()))
                    .filter(Objects::nonNull)
                    .toList();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("groomName", groomName);
            item.put("phone", groomPhone);
            item.put("email", groomEmail);
            item.put("assignedStallCount", groomStalls.size());
            item.put("stallCodeList", stallCodes);
            item.put("caredHorseList", horsesTakingCareOf);
            groomList.add(item);
        }

        return Map.of(
                "trainerName", support.resolveUserName(currentUser, "Trainer"),
                "totalCollaboratingGrooms", groomList.size(),
                "groomList", groomList
        );
    }
}
