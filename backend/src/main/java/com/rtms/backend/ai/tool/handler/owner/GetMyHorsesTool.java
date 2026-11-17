package com.rtms.backend.ai.tool.handler.owner;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetMyHorsesTool implements AiToolHandler {

    private final HorseRepository horseRepository;
    private final AiToolSupport support;

    @Autowired
    public GetMyHorsesTool(HorseRepository horseRepository, AiToolSupport support) {
        this.horseRepository = horseRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getMyHorses";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        List<Horse> horses = horseRepository.findByOwnerId(currentUser.getUserId());
        List<Map<String, Object>> result = new ArrayList<>();
        int duocTapLuyen = 0;
        int restrictedFromTraining = 0;
        for (Horse h : horses) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseId", h.getId());
            item.put("horseName", h.getName());
            item.put("registrationNumber", h.getRegistrationNumber() != null ? h.getRegistrationNumber() : "N/A");
            item.put("breed", h.getBreed() != null ? h.getBreed() : "N/A");
            item.put("sex", h.getSex() != null ? h.getSex().name() : "N/A");
            item.put("dateOfBirth", h.getDateOfBirth() != null ? h.getDateOfBirth().toString() : "N/A");
            item.put("status", h.getCurrentStatus());
            item.put("trainingDecision", h.getTrainingDecision());
            item.put("trainingDecisionReason", h.getTrainingDecisionReason() != null ? h.getTrainingDecisionReason() : "Normal");
            item.put("registryName", h.getRegistryName() != null ? h.getRegistryName() : "N/A");
            item.put("stallLocation", support.resolveStallName(h.getCurrentStallId()));
            item.put("actionUrl", h.getId() != null ? "/owner/horses/" + h.getId() : "/owner/horses");
            result.add(item);

            String dec = h.getTrainingDecision() != null ? h.getTrainingDecision().name().toUpperCase() : "";
            if (dec.contains("ELIGIBLE") || dec.contains("ALLOWED") || dec.contains("NORMAL")) {
                duocTapLuyen++;
            } else if (dec.contains("RESTRICTED") || dec.contains("BLOCKED") || dec.contains("CAM")) {
                restrictedFromTraining++;
            }
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalOwnedHorses", result.size());
        response.put("allowedTrainingHorses", duocTapLuyen);
        response.put("restrictedHorses", restrictedFromTraining);
        if (result.isEmpty()) {
            response.put("message", "You currently have no horses registered under your ownership in the club.");
        }
        response.put("horseList", result);
        response.put("actionUrl", "/owner/horses");
        return response;
    }
}
