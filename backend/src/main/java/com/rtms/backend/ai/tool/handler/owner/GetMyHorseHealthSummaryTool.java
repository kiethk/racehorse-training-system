package com.rtms.backend.ai.tool.handler.owner;

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
public class GetMyHorseHealthSummaryTool implements AiToolHandler {

    private final HealthRecordRepository healthRecordRepository;
    private final CareScheduleRepository careScheduleRepository;
    private final AiToolSupport support;

    @Autowired
    public GetMyHorseHealthSummaryTool(HealthRecordRepository healthRecordRepository,
                                       CareScheduleRepository careScheduleRepository,
                                       AiToolSupport support) {
        this.healthRecordRepository = healthRecordRepository;
        this.careScheduleRepository = careScheduleRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getMyHorseHealthSummary";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Horse horse = support.resolveHorse(arguments);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Please provide a horse ID or name");
        }
        support.verifyHorseOwnershipIfOwner(horse.getId(), currentUser);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("horseName", horse.getName());
        summary.put("registrationNumber", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        summary.put("currentStatus", horse.getCurrentStatus());
        summary.put("trainingDecision", horse.getTrainingDecision());
        summary.put("decisionReason", horse.getTrainingDecisionReason() != null ? horse.getTrainingDecisionReason() : "Normal");
        summary.put("stallLocation", support.resolveStallName(horse.getCurrentStallId()));
        summary.put("actionUrl", horse.getId() != null ? "/owner/horses/" + horse.getId() : "/owner/horses");

        List<HealthRecord> records = healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(horse.getId());
        if (!records.isEmpty()) {
            HealthRecord latest = records.get(0);
            summary.put("latestDiagnosis", latest.getDiagnosis());
            summary.put("treatmentPlan", latest.getTreatment());
            summary.put("medicalRestrictionNotes", latest.getRestrictionDetails());
            summary.put("lastExamDate", latest.getExaminedAt());
        } else {
            summary.put("latestDiagnosis", "No medical records have been recorded");
        }

        List<CareSchedule> activeSchedules = careScheduleRepository.findByHorseIdAndStatusIn(
                horse.getId(), CareScheduleStatus.BOOKED_OR_ACTIVE);
        List<Map<String, Object>> careList = activeSchedules.stream().map(cs -> Map.<String, Object>of(
                "loaiChamSoc", cs.getCareType() != null ? cs.getCareType().name() : "General examination",
                "scheduledAt", cs.getScheduledAt() != null ? cs.getScheduledAt().toString() : "N/A",
                "purpose", cs.getPurpose() != null ? cs.getPurpose().name() : "N/A"
        )).toList();
        summary.put("upcomingCareSchedule", careList);

        return summary;
    }
}
