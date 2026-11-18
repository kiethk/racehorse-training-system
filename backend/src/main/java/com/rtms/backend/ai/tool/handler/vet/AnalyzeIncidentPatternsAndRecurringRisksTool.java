package com.rtms.backend.ai.tool.handler.vet;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.incident.entity.HorseIncidentReport;
import com.rtms.backend.incident.repository.HorseIncidentReportRepository;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AnalyzeIncidentPatternsAndRecurringRisksTool implements AiToolHandler {

    private final HorseIncidentReportRepository horseIncidentReportRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final AiToolSupport support;

    @Autowired
    public AnalyzeIncidentPatternsAndRecurringRisksTool(@Autowired(required = false) HorseIncidentReportRepository horseIncidentReportRepository,
                                                        @Autowired(required = false) HealthRecordRepository healthRecordRepository,
                                                        AiToolSupport support) {
        this.horseIncidentReportRepository = horseIncidentReportRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "analyzeIncidentPatternsAndRecurringRisks";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Horse horse = support.resolveHorse(arguments);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Please provide a horse ID or name");
        }

        List<HorseIncidentReport> incidents = (horseIncidentReportRepository != null)
                ? horseIncidentReportRepository.findByHorseId(horse.getId())
                : Collections.emptyList();

        List<HealthRecord> records = (healthRecordRepository != null)
                ? healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(horse.getId())
                : Collections.emptyList();

        Set<String> conditions = new LinkedHashSet<>();
        for (HealthRecord hr : records) {
            if (hr.getDiagnosis() != null && !hr.getDiagnosis().isBlank()) {
                conditions.add(hr.getDiagnosis().trim());
            } else if (hr.getFindings() != null && !hr.getFindings().isBlank()) {
                conditions.add(hr.getFindings().trim());
            }
        }
        for (HorseIncidentReport ir : incidents) {
            if (ir.getTitle() != null && !ir.getTitle().isBlank()) {
                conditions.add(ir.getTitle().trim());
            }
        }

        String commonConditions = !conditions.isEmpty()
                ? String.join(", ", conditions)
                : "No serious medical history recorded in the past 6 months";

        String recurrenceRisk;
        if (incidents.size() >= 3 || records.size() >= 3) {
            recurrenceRisk = "High (3 or more recurring stable-related incidents or medical conditions within 6 months)";
        } else if (incidents.size() >= 1 || records.size() >= 1) {
            recurrenceRisk = "Moderate (A single incident in the medical history or a soft-tissue injury that is still healing)";
        } else {
            recurrenceRisk = "Low (Stable health records with no signs of recurring chronic conditions)";
        }

        String preventionRecommendation = "Check joints and hooves regularly before strenuous exercise, massage muscles after training, add probiotics, and continue monitoring roughage intake.";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseName", horse.getName());
        result.put("registrationNumber", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        result.put("totalIncidents", incidents.size());
        result.put("totalExamsCount", records.size());
        result.put("commonConditions", commonConditions);
        result.put("recurrenceRisk", recurrenceRisk);
        result.put("preventiveRecommendations", preventionRecommendation);
        result.put("actionUrl", horse.getId() != null ? "/veterinarian/health?horseId=" + horse.getId() : "/veterinarian/health");
        return result;
    }
}
