package com.rtms.backend.ai.tool.handler.vet;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.entity.HorseHealthMetric;
import com.rtms.backend.medical.repository.HorseHealthMetricRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetHorseVitalSignsTrendTool implements AiToolHandler {

    private final HorseHealthMetricRepository horseHealthMetricRepository;
    private final HorseRepository horseRepository;
    private final AiToolSupport support;

    @Autowired
    public GetHorseVitalSignsTrendTool(@Autowired(required = false) HorseHealthMetricRepository horseHealthMetricRepository,
                                       HorseRepository horseRepository,
                                       AiToolSupport support) {
        this.horseHealthMetricRepository = horseHealthMetricRepository;
        this.horseRepository = horseRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getHorseVitalSignsTrend";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        if (horseHealthMetricRepository == null) {
            return Collections.emptyList();
        }

        boolean isAllRequested = false;
        if (arguments != null) {
            Object nameObj = arguments.get("horseName");
            if (nameObj == null) nameObj = arguments.get("name");
            if (nameObj != null) {
                String n = nameObj.toString().trim().toLowerCase();
                if (n.equals("all") || n.contains("all horses") || n.contains("all of them") || n.contains("the entire herd") || n.contains("the whole herd")) {
                    isAllRequested = true;
                }
            }
        }

        Horse horse = isAllRequested ? null : support.resolveHorse(arguments);

        if (horse == null) {
            List<HorseHealthMetric> allMetrics = horseHealthMetricRepository.findAll().stream()
                    .filter(m -> m.getRecordedAt() != null)
                    .sorted(Comparator.comparing(HorseHealthMetric::getRecordedAt).reversed())
                    .limit(20)
                    .toList();

            Map<Long, String> horseNameCache = new HashMap<>();
            List<Map<String, Object>> result = new ArrayList<>();
            for (HorseHealthMetric m : allMetrics) {
                String hName = "N/A";
                if (m.getHorseId() != null && horseRepository != null) {
                    hName = horseNameCache.computeIfAbsent(m.getHorseId(), id ->
                            horseRepository.findById(id).map(Horse::getName).orElse("Horse #" + id));
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("horseName", hName);
                item.put("measurementDate", m.getRecordedAt() != null ? m.getRecordedAt().toString() : "N/A");
                item.put("weight", m.getWeight() != null ? m.getWeight() + " kg" : "N/A");
                item.put("temperature", m.getTemperature() != null ? m.getTemperature() + " °C" : "N/A");
                item.put("heartRate", m.getHeartRate() != null ? m.getHeartRate() + " bpm" : "N/A");
                item.put("respiratoryRate", m.getRespiratoryRate() != null ? m.getRespiratoryRate() + " breaths/min" : "N/A");
                item.put("bodyConditionScore", m.getBodyConditionScore() != null ? m.getBodyConditionScore().toString() : "N/A");
                item.put("hydrationStatus", m.getHydrationStatus() != null ? m.getHydrationStatus().name() : "ADEQUATE");
                item.put("clinicalNotes", m.getNotes() != null ? m.getNotes() : (m.getHydrationStatus() != null ? m.getHydrationStatus().name() : "Stable readings"));
                result.add(item);
            }
            return result;
        }

        List<HorseHealthMetric> metrics = horseHealthMetricRepository.findByHorseIdOrderByRecordedAtDesc(horse.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (HorseHealthMetric m : metrics) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseName", horse.getName());
            item.put("measurementDate", m.getRecordedAt() != null ? m.getRecordedAt().toString() : "N/A");
            item.put("weight", m.getWeight() != null ? m.getWeight() + " kg" : "N/A");
            item.put("temperature", m.getTemperature() != null ? m.getTemperature() + " °C" : "N/A");
            item.put("heartRate", m.getHeartRate() != null ? m.getHeartRate() + " bpm" : "N/A");
            item.put("respiratoryRate", m.getRespiratoryRate() != null ? m.getRespiratoryRate() + " breaths/min" : "N/A");
            item.put("bodyConditionScore", m.getBodyConditionScore() != null ? m.getBodyConditionScore().toString() : "N/A");
            item.put("hydrationStatus", m.getHydrationStatus() != null ? m.getHydrationStatus().name() : "ADEQUATE");
            item.put("clinicalNotes", m.getNotes() != null ? m.getNotes() : (m.getHydrationStatus() != null ? m.getHydrationStatus().name() : "Stable readings"));
            result.add(item);
        }
        return result;
    }
}
