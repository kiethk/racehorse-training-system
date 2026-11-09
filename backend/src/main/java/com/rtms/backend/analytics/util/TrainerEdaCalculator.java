package com.rtms.backend.analytics.util;

import com.rtms.backend.analytics.dto.manager.TrainerEvaluationEdaDto;
import com.rtms.backend.analytics.dto.manager.TrainerPerformanceDto;
import com.rtms.backend.analytics.dto.manager.TrainerWorkoutRow;
import com.rtms.backend.training.enums.WorkoutStatus;

import java.util.*;
import java.util.stream.Collectors;

public final class TrainerEdaCalculator {

    private TrainerEdaCalculator() {}

    public static TrainerEvaluationEdaDto calculate(List<TrainerWorkoutRow> rows, Map<Long, String> trainerNames) {
        if (rows == null || rows.isEmpty()) {
            return new TrainerEvaluationEdaDto(0.0, 0, 0, Collections.emptyList());
        }

        int totalScheduled = rows.size();
        int totalCompleted = (int) rows.stream().filter(r -> r.status() == WorkoutStatus.RAN).count();
        double overallAdherence = totalScheduled > 0
                ? Math.round(((double) totalCompleted / totalScheduled * 100.0) * 10.0) / 10.0
                : 0.0;

        Map<Long, List<TrainerWorkoutRow>> byTrainer = rows.stream()
                .filter(r -> r.trainerId() != null)
                .collect(Collectors.groupingBy(TrainerWorkoutRow::trainerId));

        List<TrainerPerformanceDto> trainerDtos = new ArrayList<>();

        for (Map.Entry<Long, List<TrainerWorkoutRow>> entry : byTrainer.entrySet()) {
            Long trainerId = entry.getKey();
            List<TrainerWorkoutRow> trainerRows = entry.getValue();

            int scheduled = trainerRows.size();
            int completed = (int) trainerRows.stream().filter(r -> r.status() == WorkoutStatus.RAN).count();
            int held = (int) trainerRows.stream().filter(r -> r.status() == WorkoutStatus.HELD).count();
            int cancelled = (int) trainerRows.stream().filter(r -> r.status() == WorkoutStatus.CANCELLED).count();
            double adherence = scheduled > 0 ? Math.round(((double) completed / scheduled * 100.0) * 10.0) / 10.0 : 0.0;

            List<TrainerWorkoutRow> ranRows = trainerRows.stream()
                    .filter(r -> r.status() == WorkoutStatus.RAN)
                    .toList();

            double avgRating = ranRows.stream()
                    .filter(r -> r.performanceRating() != null)
                    .mapToInt(TrainerWorkoutRow::performanceRating)
                    .average()
                    .orElse(0.0);

            double avgSpeed = ranRows.stream()
                    .filter(r -> r.averageSpeedKmh() != null)
                    .mapToDouble(TrainerWorkoutRow::averageSpeedKmh)
                    .average()
                    .orElse(0.0);

            double avgRecoveryHr = ranRows.stream()
                    .filter(r -> r.recoveryHeartRate() != null)
                    .mapToInt(TrainerWorkoutRow::recoveryHeartRate)
                    .average()
                    .orElse(0.0);

            // Compute speed & recovery delta per horse
            Map<Long, List<TrainerWorkoutRow>> byHorse = ranRows.stream()
                    .filter(r -> r.horseId() != null && r.sessionDate() != null)
                    .collect(Collectors.groupingBy(TrainerWorkoutRow::horseId));

            List<Double> horseSpeedGains = new ArrayList<>();
            List<Double> horseHrGains = new ArrayList<>();

            for (List<TrainerWorkoutRow> horseRows : byHorse.values()) {
                if (horseRows.size() >= 2) {
                    List<TrainerWorkoutRow> sorted = horseRows.stream()
                            .sorted(Comparator.comparing(TrainerWorkoutRow::sessionDate))
                            .toList();
                    TrainerWorkoutRow first = sorted.get(0);
                    TrainerWorkoutRow last = sorted.get(sorted.size() - 1);

                    if (first.averageSpeedKmh() != null && last.averageSpeedKmh() != null) {
                        horseSpeedGains.add(last.averageSpeedKmh() - first.averageSpeedKmh());
                    }
                    if (first.recoveryHeartRate() != null && last.recoveryHeartRate() != null) {
                        horseHrGains.add((double) (first.recoveryHeartRate() - last.recoveryHeartRate()));
                    }
                }
            }

            double meanSpeedGain = horseSpeedGains.isEmpty() ? 0.0
                    : Math.round(horseSpeedGains.stream().mapToDouble(Double::doubleValue).average().orElse(0.0) * 100.0) / 100.0;
            double meanHrGain = horseHrGains.isEmpty() ? 0.0
                    : Math.round(horseHrGains.stream().mapToDouble(Double::doubleValue).average().orElse(0.0) * 100.0) / 100.0;

            trainerDtos.add(new TrainerPerformanceDto(
                    trainerId,
                    trainerNames.getOrDefault(trainerId, "Trainer #" + trainerId),
                    scheduled,
                    completed,
                    held,
                    cancelled,
                    adherence,
                    Math.round(avgRating * 10.0) / 10.0,
                    Math.round(avgSpeed * 10.0) / 10.0,
                    Math.round(avgRecoveryHr * 10.0) / 10.0,
                    meanSpeedGain,
                    meanHrGain
            ));
        }

        trainerDtos.sort(Comparator.comparingDouble(TrainerPerformanceDto::adherenceRate).reversed());

        return new TrainerEvaluationEdaDto(
                overallAdherence,
                totalScheduled,
                totalCompleted,
                trainerDtos
        );
    }
}
