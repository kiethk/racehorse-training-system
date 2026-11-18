package com.rtms.backend.ai.tool.handler.groom;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetHorseStallLocationTool implements AiToolHandler {

    private final StableStallRepository stableStallRepository;
    private final AreaRepository areaRepository;
    private final UserRepository userRepository;
    private final AiToolSupport support;

    @Autowired
    public GetHorseStallLocationTool(StableStallRepository stableStallRepository,
                                     AreaRepository areaRepository,
                                     @Autowired(required = false) UserRepository userRepository,
                                     AiToolSupport support) {
        this.stableStallRepository = stableStallRepository;
        this.areaRepository = areaRepository;
        this.userRepository = userRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getHorseStallLocation";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Horse horse = support.resolveHorse(arguments);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Please provide a horse ID or name");
        }
        StableStall stall = horse.getCurrentStallId() != null ? stableStallRepository.findById(horse.getCurrentStallId()).orElse(null) : null;
        String areaName = "Unknown";
        String stallName = "Not assigned to a stall";
        String groomName = "Unassigned";
        if (stall != null) {
            stallName = "Stall " + stall.getStallCode() + " (No. " + stall.getStallNumber() + ")";
            if (stall.getAreaId() != null) {
                areaName = areaRepository.findById(stall.getAreaId()).map(Area::getName).orElse("Stable area");
            }
            if (stall.getGroomId() != null && userRepository != null) {
                groomName = userRepository.findById(stall.getGroomId()).map(User::getFullName).orElse("Unassigned");
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseName", horse.getName());
        result.put("registrationNumber", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        result.put("stallLocation", stallName);
        result.put("areaName", areaName);
        result.put("assignedGroom", groomName);
        return result;
    }
}
