package com.rtms.backend.analytics.util;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.analytics.dto.manager.AdmissionFunnelEdaDto;
import com.rtms.backend.analytics.dto.manager.AdmissionStageLatenciesDto;
import com.rtms.backend.analytics.dto.manager.StageFunnelPointDto;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public final class AdmissionFunnelEdaCalculator {

    private AdmissionFunnelEdaCalculator() {}

    public static AdmissionFunnelEdaDto calculate(List<AdmissionApplication> applications) {
        if (applications == null || applications.isEmpty()) {
            return new AdmissionFunnelEdaDto(
                    0, 0, 0, 0, 0, 0.0, 0.0, "NONE",
                    new AdmissionStageLatenciesDto(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
                    Collections.emptyList()
            );
        }

        long total = applications.size();
        long approved = applications.stream().filter(a -> a.getStatus() == AdmissionStatus.APPROVED).count();
        long rejected = applications.stream().filter(a -> a.getStatus() == AdmissionStatus.REJECTED).count();
        long expired = applications.stream().filter(a -> a.getStatus() == AdmissionStatus.ARRIVAL_EXPIRED).count();
        long inProgress = Math.max(0, total - approved - rejected - expired);

        double approvalRate = total > 0 ? Math.round(((double) approved / total * 100.0) * 10.0) / 10.0 : 0.0;
        double rejectionRate = total > 0 ? Math.round(((double) rejected / total * 100.0) * 10.0) / 10.0 : 0.0;

        // Funnel Step Counts
        long countSubmitted = applications.stream().filter(a -> a.getSubmittedAt() != null).count();
        long countArrived = applications.stream().filter(a -> a.getArrivedAt() != null).count();
        long countGroom = applications.stream().filter(a -> a.getGroomReviewedAt() != null).count();
        long countVet = applications.stream().filter(a -> a.getVetReviewedAt() != null).count();
        long countTrainer = applications.stream().filter(a -> a.getTrainerReviewedAt() != null).count();
        long countManager = applications.stream().filter(a -> a.getManagerReviewedAt() != null).count();

        List<StageFunnelPointDto> stageFunnel = new ArrayList<>();
        stageFunnel.add(new StageFunnelPointDto("Submitted", countSubmitted, 100.0));
        stageFunnel.add(new StageFunnelPointDto("Arrived at Facility", countArrived, calcPct(countArrived, countSubmitted)));
        stageFunnel.add(new StageFunnelPointDto("Groom Checked", countGroom, calcPct(countGroom, countSubmitted)));
        stageFunnel.add(new StageFunnelPointDto("Vet Examined", countVet, calcPct(countVet, countSubmitted)));
        stageFunnel.add(new StageFunnelPointDto("Trainer Evaluated", countTrainer, calcPct(countTrainer, countSubmitted)));
        stageFunnel.add(new StageFunnelPointDto("Manager Cleared", approved, calcPct(approved, countSubmitted)));

        // Stage Latencies (in hours)
        List<Double> arrivalLatencies = new ArrayList<>();
        List<Double> groomLatencies = new ArrayList<>();
        List<Double> vetLatencies = new ArrayList<>();
        List<Double> trainerLatencies = new ArrayList<>();
        List<Double> managerLatencies = new ArrayList<>();
        List<Double> endToEndLatencies = new ArrayList<>();

        for (AdmissionApplication a : applications) {
            LocalDateTime sub = a.getSubmittedAt();
            LocalDateTime arr = a.getArrivedAt();
            LocalDateTime grm = a.getGroomReviewedAt();
            LocalDateTime vet = a.getVetReviewedAt();
            LocalDateTime trn = a.getTrainerReviewedAt();
            LocalDateTime mgr = a.getManagerReviewedAt();

            if (sub != null && arr != null && !arr.isBefore(sub)) {
                arrivalLatencies.add(toHours(sub, arr));
            }
            if (arr != null && grm != null && !grm.isBefore(arr)) {
                groomLatencies.add(toHours(arr, grm));
            }
            if (grm != null && vet != null && !vet.isBefore(grm)) {
                vetLatencies.add(toHours(grm, vet));
            }
            if (vet != null && trn != null && !trn.isBefore(vet)) {
                trainerLatencies.add(toHours(vet, trn));
            }
            if (trn != null && mgr != null && !mgr.isBefore(trn)) {
                managerLatencies.add(toHours(trn, mgr));
            }
            if (sub != null && mgr != null && !mgr.isBefore(sub)) {
                endToEndLatencies.add(toHours(sub, mgr));
            }
        }

        double medArrival = median(arrivalLatencies);
        double medGroom = median(groomLatencies);
        double medVet = median(vetLatencies);
        double medTrainer = median(trainerLatencies);
        double medManager = median(managerLatencies);
        double medEndToEnd = median(endToEndLatencies);

        // Identify bottleneck stage
        Map<String, Double> stageMedianMap = new LinkedHashMap<>();
        stageMedianMap.put("ARRIVAL", medArrival);
        stageMedianMap.put("GROOM_REVIEW", medGroom);
        stageMedianMap.put("VET_REVIEW", medVet);
        stageMedianMap.put("TRAINER_REVIEW", medTrainer);
        stageMedianMap.put("MANAGER_REVIEW", medManager);

        String bottleneck = "NONE";
        double maxLatency = 0.0;
        for (Map.Entry<String, Double> entry : stageMedianMap.entrySet()) {
            if (entry.getValue() > maxLatency && entry.getValue() > 0.0) {
                maxLatency = entry.getValue();
                bottleneck = entry.getKey();
            }
        }

        AdmissionStageLatenciesDto latenciesDto = new AdmissionStageLatenciesDto(
                medArrival,
                medGroom,
                medVet,
                medTrainer,
                medManager,
                medEndToEnd
        );

        return new AdmissionFunnelEdaDto(
                total,
                approved,
                rejected,
                expired,
                inProgress,
                approvalRate,
                rejectionRate,
                bottleneck,
                latenciesDto,
                stageFunnel
        );
    }

    private static double calcPct(long part, long total) {
        if (total <= 0) return 0.0;
        return Math.round(((double) part / total * 100.0) * 10.0) / 10.0;
    }

    private static double toHours(LocalDateTime start, LocalDateTime end) {
        return Math.max(0.0, Math.round((Duration.between(start, end).toMinutes() / 60.0) * 10.0) / 10.0);
    }

    private static double median(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int size = sorted.size();
        if (size % 2 == 1) {
            return sorted.get(size / 2);
        } else {
            return Math.round(((sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0) * 10.0) / 10.0;
        }
    }
}
