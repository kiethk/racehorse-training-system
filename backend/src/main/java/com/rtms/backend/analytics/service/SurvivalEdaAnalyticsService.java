package com.rtms.backend.analytics.service;

import com.rtms.backend.analytics.dto.medical.SurvivalEdaResponse;
import com.rtms.backend.analytics.dto.medical.SurvivalEdaResponse.SurvivalStratumDto;
import com.rtms.backend.analytics.calculator.KaplanMeierCalculator;
import com.rtms.backend.analytics.calculator.KaplanMeierCalculator.KmAnalysisResult;
import com.rtms.backend.analytics.calculator.KaplanMeierCalculator.SurvivalObservation;
import com.rtms.backend.medical.entity.HorseBodyRegion;
import com.rtms.backend.medical.entity.InjuryRecord;
import com.rtms.backend.medical.repository.HorseBodyRegionRepository;
import com.rtms.backend.medical.repository.InjuryRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SurvivalEdaAnalyticsService {

    private final InjuryRecordRepository injuryRecordRepository;
    private final HorseBodyRegionRepository bodyRegionRepository;

    public SurvivalEdaAnalyticsService(
            InjuryRecordRepository injuryRecordRepository,
            HorseBodyRegionRepository bodyRegionRepository
    ) {
        this.injuryRecordRepository = injuryRecordRepository;
        this.bodyRegionRepository = bodyRegionRepository;
    }

    @Transactional(readOnly = true)
    public SurvivalEdaResponse getSurvivalAnalytics(String stratifyBy) {
        String normalizedStratify = (stratifyBy != null && stratifyBy.equalsIgnoreCase("BODY_ZONE"))
                ? "BODY_ZONE"
                : "SEVERITY";

        List<InjuryRecord> injuries = injuryRecordRepository.findAll();
        if (injuries.isEmpty()) {
            return new SurvivalEdaResponse(normalizedStratify, List.of());
        }

        Map<String, List<SurvivalObservation>> strataMap = new LinkedHashMap<>();

        if ("BODY_ZONE".equals(normalizedStratify)) {
            Map<Long, String> regionNameMap = bodyRegionRepository.findAll().stream()
                    .collect(Collectors.toMap(HorseBodyRegion::getId, HorseBodyRegion::getDisplayName, (a, b) -> a));

            for (InjuryRecord inj : injuries) {
                String stratumName;
                if (inj.getBodyRegionId() != null && regionNameMap.containsKey(inj.getBodyRegionId())) {
                    stratumName = regionNameMap.get(inj.getBodyRegionId());
                } else if (inj.getZoneId() != null && !inj.getZoneId().isBlank()) {
                    stratumName = inj.getZoneId();
                } else {
                    stratumName = "General / Unspecified";
                }

                SurvivalObservation obs = toObservation(inj);
                strataMap.computeIfAbsent(stratumName, k -> new ArrayList<>()).add(obs);
            }
        } else {
            // SEVERITY: Clinical ordering (Mild -> Moderate -> Severe -> Unspecified)
            List<String> canonicalOrder = List.of("Mild Severity", "Moderate Severity", "Severe", "Unspecified Severity");
            for (String label : canonicalOrder) {
                strataMap.put(label, new ArrayList<>());
            }

            for (InjuryRecord inj : injuries) {
                String rawSeverity = inj.getSeverity();
                String stratumName;
                if (rawSeverity != null) {
                    switch (rawSeverity.trim().toUpperCase()) {
                        case "MILD" -> stratumName = "Mild Severity";
                        case "MODERATE" -> stratumName = "Moderate Severity";
                        case "SEVERE" -> stratumName = "Severe";
                        default -> stratumName = "Unspecified Severity";
                    }
                } else {
                    stratumName = "Unspecified Severity";
                }

                SurvivalObservation obs = toObservation(inj);
                strataMap.get(stratumName).add(obs);
            }

            // Remove any predefined stratum that has no cases
            strataMap.entrySet().removeIf(e -> e.getValue().isEmpty());
        }

        List<SurvivalStratumDto> strata = new ArrayList<>();
        long stratumIdCounter = 1;

        for (Map.Entry<String, List<SurvivalObservation>> entry : strataMap.entrySet()) {
            String name = entry.getKey();
            List<SurvivalObservation> obsList = entry.getValue();

            KmAnalysisResult kmResult = KaplanMeierCalculator.calculate(obsList);

            strata.add(new SurvivalStratumDto(
                    stratumIdCounter++,
                    name,
                    kmResult.totalCases(),
                    kmResult.recoveredCount(),
                    kmResult.censoredCount(),
                    kmResult.medianRecoveryDays(),
                    kmResult.steps()
            ));
        }

        return new SurvivalEdaResponse(normalizedStratify, strata);
    }

    private SurvivalObservation toObservation(InjuryRecord inj) {
        LocalDate start = (inj.getDiagnosedAt() != null)
                ? inj.getDiagnosedAt().toLocalDate()
                : (inj.getCreatedAt() != null ? inj.getCreatedAt().toLocalDate() : LocalDate.now().minusDays(1));

        boolean recovered = inj.getRecoveredAt() != null
                || "RESOLVED".equalsIgnoreCase(inj.getStatus())
                || "RECOVERED".equalsIgnoreCase(inj.getStatus());

        LocalDate endDate;
        if (inj.getRecoveredAt() != null) {
            endDate = inj.getRecoveredAt().toLocalDate();
        } else if (recovered && inj.getUpdatedAt() != null) {
            endDate = inj.getUpdatedAt().toLocalDate();
        } else if (recovered && inj.getExpectedRecoveryDate() != null) {
            endDate = inj.getExpectedRecoveryDate();
        } else {
            endDate = LocalDate.now();
        }

        if (endDate.isBefore(start)) {
            endDate = start;
        }

        int days = (int) ChronoUnit.DAYS.between(start, endDate);
        if (days < 1) {
            days = 1;
        }

        return new SurvivalObservation(days, recovered);
    }
}
