package com.rtms.backend.ai.tool.handler.groom;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetHorseDietaryAndCareAlertsTool implements AiToolHandler {

    private final HealthRecordRepository healthRecordRepository;
    private final CareScheduleRepository careScheduleRepository;
    private final AiToolSupport support;

    @Autowired
    public GetHorseDietaryAndCareAlertsTool(HealthRecordRepository healthRecordRepository,
                                           CareScheduleRepository careScheduleRepository,
                                           AiToolSupport support) {
        this.healthRecordRepository = healthRecordRepository;
        this.careScheduleRepository = careScheduleRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getHorseDietaryAndCareAlerts";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Horse horse = support.resolveHorse(arguments);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Please provide a horse ID or name");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseName", horse.getName());
        result.put("registrationNumber", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        result.put("status", horse.getCurrentStatus());
        result.put("trainingDecision", horse.getTrainingDecision());
        result.put("decisionReason", horse.getTrainingDecisionReason() != null ? horse.getTrainingDecisionReason() : "No special restrictions");

        List<HealthRecord> records = healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(horse.getId());
        if (!records.isEmpty()) {
            HealthRecord latest = records.get(0);
            result.put("latestDiagnosis", latest.getDiagnosis());
            result.put("treatmentPlan", latest.getTreatment());
            result.put("medicalCareNotes", latest.getRestrictionDetails());
            result.put("lastExamDate", latest.getExaminedAt());
        }

        List<CareSchedule> activeSchedules = careScheduleRepository.findByHorseIdAndStatusIn(
                horse.getId(), CareScheduleStatus.BOOKED_OR_ACTIVE);
        List<Map<String, Object>> careList = activeSchedules.stream().map(cs -> Map.<String, Object>of(
                "loaiChamSoc", cs.getCareType() != null ? cs.getCareType().name() : "Veterinary examination",
                "scheduledAt", cs.getScheduledAt() != null ? cs.getScheduledAt().toString() : "N/A",
                "purpose", cs.getPurpose() != null ? cs.getPurpose().name() : "N/A"
        )).toList();
        result.put("upcomingCareSchedule", careList);

        return result;
    }
}
