package com.rtms.backend.ai.tool.handler.vet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.entity.TreatmentPlan;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.TreatmentPlanRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetActivePrescriptionsTool implements AiToolHandler {

    private static final Logger log = LoggerFactory.getLogger(GetActivePrescriptionsTool.class);

    private final TreatmentPlanRepository treatmentPlanRepository;
    private final CareScheduleRepository careScheduleRepository;
    private final ObjectMapper objectMapper;
    private final AiToolSupport support;

    @Autowired
    public GetActivePrescriptionsTool(@Autowired(required = false) TreatmentPlanRepository treatmentPlanRepository,
                                     CareScheduleRepository careScheduleRepository,
                                     @Autowired(required = false) ObjectMapper objectMapper,
                                     AiToolSupport support) {
        this.treatmentPlanRepository = treatmentPlanRepository;
        this.careScheduleRepository = careScheduleRepository;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getActivePrescriptions";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Horse horse = support.resolveHorse(arguments);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Please provide a horse ID or name");
        }
        List<Map<String, Object>> result = new ArrayList<>();

        if (treatmentPlanRepository != null) {
            List<TreatmentPlan> activePlans = treatmentPlanRepository.findActivePlansByHorseId(horse.getId());
            for (TreatmentPlan plan : activePlans) {
                String instructions = plan.getInstructions();
                if (instructions != null && instructions.trim().startsWith("[")) {
                    try {
                        List<Map<String, Object>> items = objectMapper.readValue(instructions, new TypeReference<List<Map<String, Object>>>() {});
                        for (Map<String, Object> it : items) {
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("horseName", horse.getName());
                            row.put("treatmentPlanName", plan.getTreatmentName());
                            row.put("medicationName", it.getOrDefault("medication_name", "Medication"));
                            row.put("dosage", it.getOrDefault("dosage", "N/A"));
                            row.put("frequency", it.getOrDefault("frequency", "N/A"));
                            row.put("route", it.getOrDefault("route", "N/A"));
                            row.put("duration", it.getOrDefault("duration", 0));
                            row.put("withdrawalDays", it.getOrDefault("withdrawal_days", 0));
                            row.put("administeredByRole", it.getOrDefault("administered_by_role", "VETERINARIAN"));
                            row.put("notes", it.getOrDefault("notes", ""));
                            row.put("startDate", plan.getStartDate() != null ? plan.getStartDate().toString() : "N/A");
                            row.put("endDate", plan.getEndDate() != null ? plan.getEndDate().toString() : "N/A");
                            row.put("status", plan.getStatus() != null ? plan.getStatus() : "ACTIVE");
                            result.add(row);
                        }
                    } catch (Exception e) {
                        log.warn("Unable to parse JSON instructions for TreatmentPlan #{}: {}", plan.getId(), e.getMessage());
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("horseName", horse.getName());
                        row.put("treatmentPlanName", plan.getTreatmentName());
                        row.put("medicationName", plan.getTreatmentName());
                        row.put("dosage", "As directed");
                        row.put("notes", instructions);
                        row.put("startDate", plan.getStartDate() != null ? plan.getStartDate().toString() : "N/A");
                        row.put("endDate", plan.getEndDate() != null ? plan.getEndDate().toString() : "N/A");
                        row.put("status", plan.getStatus() != null ? plan.getStatus() : "ACTIVE");
                        result.add(row);
                    }
                } else if (instructions != null && !instructions.isBlank()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("horseName", horse.getName());
                    row.put("treatmentPlanName", plan.getTreatmentName());
                    row.put("medicationName", plan.getTreatmentName());
                    row.put("dosage", "As directed");
                    row.put("notes", instructions);
                    row.put("startDate", plan.getStartDate() != null ? plan.getStartDate().toString() : "N/A");
                    row.put("endDate", plan.getEndDate() != null ? plan.getEndDate().toString() : "N/A");
                    row.put("status", plan.getStatus() != null ? plan.getStatus() : "ACTIVE");
                    result.add(row);
                }
            }
        }

        if (result.isEmpty()) {
            List<CareSchedule> active = careScheduleRepository.findByHorseIdAndStatusIn(
                    horse.getId(), CareScheduleStatus.BOOKED_OR_ACTIVE
            ).stream()
                    .filter(cs -> cs.getVeterinarianId() == null || cs.getVeterinarianId().equals(currentUser.getUserId()))
                    .toList();
            for (CareSchedule cs : active) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("horseName", horse.getName());
                item.put("treatmentType", cs.getCareType() != null ? cs.getCareType().name() : "N/A");
                item.put("scheduledAt", cs.getScheduledAt() != null ? cs.getScheduledAt().toString() : "N/A");
                item.put("purpose", cs.getPurpose() != null ? cs.getPurpose().name() : "N/A");
                item.put("description", cs.getDescription() != null ? cs.getDescription() : "");
                result.add(item);
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalPrescriptions", result.size());
        response.put("treatmentList", result);
        return response;
    }
}
