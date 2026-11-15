package com.rtms.backend.analytics.service;

import com.rtms.backend.analytics.dto.EdaCorrelationSummary;
import com.rtms.backend.analytics.dto.EdaDistributionStats;
import com.rtms.backend.analytics.dto.FleetEdaResponse;
import com.rtms.backend.analytics.dto.FleetHorseEdaPoint;
import com.rtms.backend.analytics.dto.FleetWorkoutSummaryRow;
import com.rtms.backend.analytics.dto.IndividualHorseEdaResponse;
import com.rtms.backend.analytics.dto.IndividualWorkoutEdaPoint;
import com.rtms.backend.analytics.util.EdaStatisticsCalculator;
import com.rtms.backend.analytics.util.EdaStatisticsCalculator.CorrelationResult;
import com.rtms.backend.analytics.util.EdaStatisticsCalculator.DistributionStats;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.repository.projection.DashboardHorseRow;
import com.rtms.backend.training.dto.HorseFitnessTrendItemResponse;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import com.rtms.backend.training.service.TrainingAccessPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dịch vụ xử lý và tổng hợp dữ liệu cho module EDA (Exploratory Data Analysis),
 * trích xuất thống kê mô tả và phân tích tương quan hai chiều hiệu suất - sức khỏe.
 */
@Service
public class HorseEdaAnalyticsService {

    private final TrainingWorkoutRepository workoutRepository;
    private final TrainingAccessPolicy accessPolicy;
    private final HorseRepository horseRepository;

    public HorseEdaAnalyticsService(
            TrainingWorkoutRepository workoutRepository,
            TrainingAccessPolicy accessPolicy,
            HorseRepository horseRepository
    ) {
        this.workoutRepository = workoutRepository;
        this.accessPolicy = accessPolicy;
        this.horseRepository = horseRepository;
    }

    /**
     * Phân tích EDA cho toàn bộ đàn ngựa trong phạm vi quyền hạn của người dùng.
     */
    @Transactional(readOnly = true)
    public FleetEdaResponse getFleetEdaAnalytics(AuthenticatedUser currentUser, Integer days) {
        int windowDays = (days != null && days > 0) ? days : 180;
        LocalDate since = LocalDate.now().minusDays(windowDays);

        List<DashboardHorseRow> accessibleHorses = accessPolicy.horsesOf(currentUser);
        if (accessibleHorses == null || accessibleHorses.isEmpty()) {
            return emptyFleetResponse();
        }

        List<Long> horseIds = accessibleHorses.stream().map(DashboardHorseRow::horseId).toList();
        List<FleetWorkoutSummaryRow> summaries = workoutRepository.findFleetWorkoutSummaries(horseIds, since);

        if (summaries == null || summaries.isEmpty()) {
            return emptyFleetResponse();
        }

        Map<Long, FleetWorkoutSummaryRow> summaryByHorse = summaries.stream()
                .collect(Collectors.toMap(FleetWorkoutSummaryRow::horseId, s -> s));

        List<Double> speeds = new ArrayList<>();
        List<Double> recoveryHrs = new ArrayList<>();

        for (FleetWorkoutSummaryRow s : summaries) {
            if (s.avgSpeedKmh() != null && s.avgRecoveryHeartRate() != null) {
                speeds.add(round1(s.avgSpeedKmh()));
                recoveryHrs.add(round1(s.avgRecoveryHeartRate()));
            }
        }

        DistributionStats speedDist = EdaStatisticsCalculator.computeDistribution(speeds);
        DistributionStats recoveryHrDist = EdaStatisticsCalculator.computeDistribution(recoveryHrs);
        CorrelationResult corrRes = EdaStatisticsCalculator.computeCorrelation(speeds, recoveryHrs);

        List<FleetHorseEdaPoint> points = new ArrayList<>();
        for (DashboardHorseRow horse : accessibleHorses) {
            FleetWorkoutSummaryRow s = summaryByHorse.get(horse.horseId());
            if (s != null && s.avgSpeedKmh() != null && s.avgRecoveryHeartRate() != null) {
                double spd = round1(s.avgSpeedKmh());
                double rec = round1(s.avgRecoveryHeartRate());
                Double maxHr = s.maxHeartRate() != null ? s.maxHeartRate().doubleValue() : null;

                boolean isSpeedOutlier = speedDist.outliers().contains(spd);
                boolean isHrOutlier = recoveryHrDist.outliers().contains(rec);
                boolean isOutlier = isSpeedOutlier || isHrOutlier;

                String reason = buildOutlierReason(isSpeedOutlier, isHrOutlier, spd, rec, speedDist, recoveryHrDist);

                points.add(new FleetHorseEdaPoint(
                        horse.horseId(),
                        horse.horseName(),
                        horse.breed(),
                        horse.stallCode(),
                        "N/A",
                        spd,
                        rec,
                        maxHr,
                        isOutlier,
                        reason
                ));
            }
        }

        return new FleetEdaResponse(
                EdaDistributionStats.from(speedDist),
                EdaDistributionStats.from(recoveryHrDist),
                EdaCorrelationSummary.from("averageSpeedKmh", "recoveryHeartRateBpm", corrRes),
                points
        );
    }

    /**
     * Phân tích EDA chi tiết cho một chiến mã theo chuỗi các buổi tập đã hoàn thành.
     */
    @Transactional(readOnly = true)
    public IndividualHorseEdaResponse getIndividualEdaAnalytics(Long horseId, AuthenticatedUser currentUser, Integer days) {
        accessPolicy.assertCanViewHorse(horseId, currentUser);

        Horse horse = horseRepository.findById(horseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found with ID: " + horseId));

        int windowDays = (days != null && days > 0) ? days : 365;
        LocalDate from = LocalDate.now().minusDays(windowDays);
        LocalDate to = LocalDate.now();

        List<HorseFitnessTrendItemResponse> workouts = workoutRepository.findFitnessTrend(horseId, from, to);

        if (workouts == null || workouts.isEmpty()) {
            return emptyIndividualResponse(horseId, horse.getName(), horse.getBreed());
        }

        List<Double> speeds = new ArrayList<>();
        List<Double> recoveryHrs = new ArrayList<>();

        for (HorseFitnessTrendItemResponse w : workouts) {
            if (w.getAverageSpeedKmh() != null && w.getRecoveryHeartRate() != null) {
                speeds.add(round1(w.getAverageSpeedKmh().doubleValue()));
                recoveryHrs.add(round1(w.getRecoveryHeartRate().doubleValue()));
            }
        }

        DistributionStats speedDist = EdaStatisticsCalculator.computeDistribution(speeds);
        DistributionStats recoveryHrDist = EdaStatisticsCalculator.computeDistribution(recoveryHrs);
        CorrelationResult corrRes = EdaStatisticsCalculator.computeCorrelation(speeds, recoveryHrs);

        List<IndividualWorkoutEdaPoint> points = new ArrayList<>();
        for (HorseFitnessTrendItemResponse w : workouts) {
            if (w.getAverageSpeedKmh() != null && w.getRecoveryHeartRate() != null) {
                double spd = round1(w.getAverageSpeedKmh().doubleValue());
                double rec = round1(w.getRecoveryHeartRate().doubleValue());
                Double maxHr = w.getMaxHeartRate() != null ? w.getMaxHeartRate().doubleValue() : null;
                Double dist = w.getDistanceMeters() != null ? w.getDistanceMeters().doubleValue() : null;

                boolean isSpeedOutlier = speedDist.outliers().contains(spd);
                boolean isHrOutlier = recoveryHrDist.outliers().contains(rec);
                boolean isOutlier = isSpeedOutlier || isHrOutlier;

                String reason = buildOutlierReason(isSpeedOutlier, isHrOutlier, spd, rec, speedDist, recoveryHrDist);

                points.add(new IndividualWorkoutEdaPoint(
                        w.getWorkoutId(),
                        w.getDate() != null ? w.getDate().toString() : "N/A",
                        w.getExerciseName() != null ? w.getExerciseName() : "Workout",
                        spd,
                        rec,
                        maxHr,
                        dist,
                        isOutlier,
                        reason
                ));
            }
        }

        return new IndividualHorseEdaResponse(
                horseId,
                horse.getName(),
                horse.getBreed(),
                EdaDistributionStats.from(speedDist),
                EdaDistributionStats.from(recoveryHrDist),
                EdaCorrelationSummary.from("speedKmh", "recoveryHeartRateBpm", corrRes),
                points
        );
    }

    private String buildOutlierReason(boolean isSpeedOutlier, boolean isHrOutlier, double spd, double rec,
                                      DistributionStats speedDist, DistributionStats hrDist) {
        if (!isSpeedOutlier && !isHrOutlier) return null;
        List<String> reasons = new ArrayList<>();
        if (isSpeedOutlier) {
            if (spd > speedDist.q3()) {
                reasons.add(String.format(Locale.US, "Above typical workout speed (%.1f km/h)", spd));
            } else {
                reasons.add(String.format(Locale.US, "Below typical workout speed (%.1f km/h)", spd));
            }
        }
        if (isHrOutlier) {
            if (rec > hrDist.q3()) {
                reasons.add(String.format(Locale.US, "Elevated recovery heart rate (%.0f bpm)", rec));
            } else {
                reasons.add(String.format(Locale.US, "Optimal cardio recovery (%.0f bpm)", rec));
            }
        }
        return String.join(" | ", reasons);
    }

    private FleetEdaResponse emptyFleetResponse() {
        DistributionStats emptyDist = EdaStatisticsCalculator.computeDistribution(Collections.emptyList());
        CorrelationResult emptyCorr = EdaStatisticsCalculator.computeCorrelation(Collections.emptyList(), Collections.emptyList());
        return new FleetEdaResponse(
                EdaDistributionStats.from(emptyDist),
                EdaDistributionStats.from(emptyDist),
                EdaCorrelationSummary.from("averageSpeedKmh", "recoveryHeartRateBpm", emptyCorr),
                Collections.emptyList()
        );
    }

    private IndividualHorseEdaResponse emptyIndividualResponse(Long horseId, String horseName, String breed) {
        DistributionStats emptyDist = EdaStatisticsCalculator.computeDistribution(Collections.emptyList());
        CorrelationResult emptyCorr = EdaStatisticsCalculator.computeCorrelation(Collections.emptyList(), Collections.emptyList());
        return new IndividualHorseEdaResponse(
                horseId,
                horseName,
                breed,
                EdaDistributionStats.from(emptyDist),
                EdaDistributionStats.from(emptyDist),
                EdaCorrelationSummary.from("speedKmh", "recoveryHeartRateBpm", emptyCorr),
                Collections.emptyList()
        );
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
