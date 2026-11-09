package com.rtms.backend.medical.analytics.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.medical.analytics.dto.BioSpcSeriesResponse;
import com.rtms.backend.medical.analytics.dto.BioSpcSeriesResponse.BioSpcPointDto;
import com.rtms.backend.medical.analytics.util.BioSpcCalculator;
import com.rtms.backend.medical.analytics.util.BioSpcCalculator.RawPoint;
import com.rtms.backend.medical.analytics.util.BioSpcCalculator.SpcAnalysisResult;
import com.rtms.backend.medical.entity.HorseHealthMetric;
import com.rtms.backend.medical.repository.HorseHealthMetricRepository;
import com.rtms.backend.notification.entity.Notification;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.notification.service.NotificationTypes;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.service.TrainingAccessPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class BioSpcAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(BioSpcAnalyticsService.class);

    private final HorseHealthMetricRepository metricRepository;
    private final HorseRepository horseRepository;
    private final TrainingAccessPolicy accessPolicy;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public BioSpcAnalyticsService(
            HorseHealthMetricRepository metricRepository,
            HorseRepository horseRepository,
            TrainingAccessPolicy accessPolicy,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.metricRepository = metricRepository;
        this.horseRepository = horseRepository;
        this.accessPolicy = accessPolicy;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public BioSpcSeriesResponse getBioSpcSeries(
            Long horseId,
            String metricType,
            AuthenticatedUser currentUser
    ) {
        if (currentUser != null) {
            accessPolicy.assertCanViewHorse(horseId, currentUser);
        }

        Horse horse = horseRepository.findById(horseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found with ID: " + horseId));

        String normalizedMetric = (metricType != null && metricType.equalsIgnoreCase("RESTING_HEART_RATE"))
                ? "RESTING_HEART_RATE"
                : "TEMPERATURE";

        List<HorseHealthMetric> metrics = metricRepository.findByHorseIdOrderByRecordedAtDesc(horseId);

        // Sort ascending chronologically
        List<RawPoint> rawPoints = new ArrayList<>();
        for (HorseHealthMetric m : metrics) {
            if ("TEMPERATURE".equals(normalizedMetric)) {
                if (m.getTemperature() != null) {
                    rawPoints.add(new RawPoint(m.getId(), m.getRecordedAt(), m.getTemperature().doubleValue()));
                }
            } else {
                if (m.getHeartRate() != null) {
                    rawPoints.add(new RawPoint(m.getId(), m.getRecordedAt(), m.getHeartRate().doubleValue()));
                }
            }
        }
        rawPoints.sort(Comparator.comparing(RawPoint::recordedAt));

        SpcAnalysisResult analysis = BioSpcCalculator.analyzeSeries(rawPoints, normalizedMetric);

        double specMin = BioSpcCalculator.isTemperature(normalizedMetric)
                ? BioSpcCalculator.SPECIES_TEMP_MIN
                : BioSpcCalculator.SPECIES_HR_MIN;
        double specMax = BioSpcCalculator.isTemperature(normalizedMetric)
                ? BioSpcCalculator.SPECIES_TEMP_MAX
                : BioSpcCalculator.SPECIES_HR_MAX;

        // Check if latest point triggered an anomaly to dispatch notification
        List<BioSpcPointDto> analyzedPoints = analysis.analyzedPoints();
        if (!analyzedPoints.isEmpty()) {
            BioSpcPointDto latestPoint = analyzedPoints.get(analyzedPoints.size() - 1);
            if (latestPoint.isSpecialCause()) {
                double val = latestPoint.value();
                boolean violatesLimits = (analysis.ucl() != null && val >= analysis.ucl() - 1e-4)
                        || (analysis.lcl() != null && val <= analysis.lcl() + 1e-4);

                if (violatesLimits) {
                    dispatchAnomalyNotification(horse, normalizedMetric, latestPoint, analysis);
                }
            }
        }

        return new BioSpcSeriesResponse(
                horse.getId(),
                horse.getName(),
                normalizedMetric,
                analysis.mean(),
                analysis.stdDev(),
                analysis.baselineActive() ? rawPoints.size() : analysis.baselineSampleCount(),
                analysis.baselineActive(),
                analysis.ucl(),
                analysis.lcl(),
                analysis.uwl(),
                analysis.lwl(),
                specMin,
                specMax,
                analyzedPoints
        );
    }

    private void dispatchAnomalyNotification(
            Horse horse,
            String metricType,
            BioSpcPointDto point,
            SpcAnalysisResult analysis
    ) {
        try {
            List<User> vets = userRepository.findActiveVeterinarians();
            String title = "Bio-SPC Anomaly Detected: " + horse.getName();
            String limitDesc = analysis.baselineActive() ? "3-sigma limits" : "species physiological limits";
            String message = String.format(
                    Locale.US,
                    "Special cause variation detected for horse %s on metric %s: recorded value %.2f is outside %s [%.2f, %.2f].",
                    horse.getName(),
                    metricType,
                    point.value(),
                    limitDesc,
                    analysis.lcl(),
                    analysis.ucl()
            );

            for (User vet : vets) {
                String dedupKey = Notification.buildDeduplicationKey(
                        vet.getId(),
                        NotificationTypes.REFERENCE_HORSE,
                        horse.getId(),
                        NotificationTypes.BIO_SPC_ANOMALY_DETECTED + "_" + point.id()
                );

                notificationService.createNotification(
                        vet.getId(),
                        title,
                        message,
                        NotificationTypes.BIO_SPC_ANOMALY_DETECTED,
                        NotificationTypes.REFERENCE_HORSE,
                        horse.getId(),
                        dedupKey
                );
            }
        } catch (Exception ex) {
            log.warn("Failed to dispatch Bio-SPC anomaly notification for horse #{}: {}", horse.getId(), ex.getMessage());
        }
    }
}
