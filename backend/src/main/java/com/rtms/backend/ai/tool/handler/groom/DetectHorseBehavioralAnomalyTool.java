package com.rtms.backend.ai.tool.handler.groom;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.incident.entity.HorseIncidentReport;
import com.rtms.backend.incident.repository.HorseIncidentReportRepository;
import com.rtms.backend.medical.entity.HorseHealthMetric;
import com.rtms.backend.medical.repository.HorseHealthMetricRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class DetectHorseBehavioralAnomalyTool implements AiToolHandler {

    private final HorseIncidentReportRepository horseIncidentReportRepository;
    private final HorseHealthMetricRepository horseHealthMetricRepository;
    private final AiToolSupport support;

    @Autowired
    public DetectHorseBehavioralAnomalyTool(@Autowired(required = false) HorseIncidentReportRepository horseIncidentReportRepository,
                                           @Autowired(required = false) HorseHealthMetricRepository horseHealthMetricRepository,
                                           AiToolSupport support) {
        this.horseIncidentReportRepository = horseIncidentReportRepository;
        this.horseHealthMetricRepository = horseHealthMetricRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "detectHorseBehavioralAnomaly";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Horse horse = support.resolveHorse(arguments);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Please provide a horse ID or name");
        }

        List<HorseIncidentReport> incidents = (horseIncidentReportRepository != null)
                ? horseIncidentReportRepository.findByHorseId(horse.getId()).stream()
                        .filter(r -> currentUser.getUserId().equals(r.getReportedById()))
                        .toList()
                : Collections.emptyList();
        List<HorseHealthMetric> metrics = (horseHealthMetricRepository != null)
                ? horseHealthMetricRepository.findByHorseIdOrderByRecordedAtDesc(horse.getId())
                : Collections.emptyList();

        boolean colicSuspected = false;
        boolean mildAnomaly = false;
        List<String> detectedAnomalies = new ArrayList<>();

        for (HorseIncidentReport r : incidents) {
            String desc = ((r.getDescription() != null ? r.getDescription() : "") + " " + (r.getTitle() != null ? r.getTitle() : "")).toLowerCase();
            if (desc.contains("refusing to eat") || desc.contains("pawing the ground") || desc.contains("lethargic")
                    || desc.contains("colic") || desc.contains("abdominal pain") || desc.contains("rolling")) {
                colicSuspected = true;
                detectedAnomalies.add(r.getTitle() != null ? r.getTitle() : "Abnormal signs in the stall incident report");
            }
        }

        if (!metrics.isEmpty()) {
            HorseHealthMetric m = metrics.get(0);
            if (m.getTemperature() != null && m.getTemperature().compareTo(new BigDecimal("38.5")) > 0) {
                mildAnomaly = true;
                detectedAnomalies.add("Elevated body temperature (" + m.getTemperature() + " °C)");
            }
            if (m.getHeartRate() != null && m.getHeartRate().compareTo(new BigDecimal("44")) > 0) {
                colicSuspected = true;
                detectedAnomalies.add("Abnormally high resting heart rate (" + m.getHeartRate() + " bpm)");
            }
        }

        String abnormalSigns;
        String alertLevel;
        String immediateActions;

        if (colicSuspected) {
            abnormalSigns = !detectedAnomalies.isEmpty() ? String.join(", ", detectedAnomalies) : "Signs of pawing the ground, refusing to eat, and an elevated resting heart rate detected";
            alertLevel = "Emergency";
            immediateActions = "Possible acute colic! Notify the on-call veterinarian immediately. Withhold all forage and concentrate feed, prevent the horse from rolling in the stall, and walk it gently.";
        } else if (mildAnomaly) {
            abnormalSigns = !detectedAnomalies.isEmpty() ? String.join(", ", detectedAnomalies) : "Mild lethargy and reduced food intake";
            alertLevel = "Moderate";
            immediateActions = "Closely monitor water and food intake for the next 4 hours, check the manure, and take the horse's temperature periodically.";
        } else {
            abnormalSigns = "No behavioral abnormalities recorded in the latest log";
            alertLevel = "Low";
            immediateActions = "Maintain the standard feeding, stall-cleaning, and exercise routines in accordance with the SOP.";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseName", horse.getName());
        result.put("registrationNumber", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        result.put("abnormalSigns", abnormalSigns);
        result.put("alertLevel", alertLevel);
        result.put("immediateActionRequired", immediateActions);
        result.put("actionUrl", "/groom/incidents");
        return result;
    }
}
