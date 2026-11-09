package com.rtms.backend.analytics.service;

import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.analytics.dto.manager.ManagerEdaOverviewResponse;
import com.rtms.backend.analytics.util.AdmissionFunnelEdaCalculator;
import com.rtms.backend.analytics.util.OccupancyEdaCalculator;
import com.rtms.backend.analytics.util.TrainerEdaCalculator;
import com.rtms.backend.analytics.util.VetRolloverEdaCalculator;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ManagerEdaAnalyticsService {

    private final CareScheduleRepository careScheduleRepository;
    private final TrainingWorkoutRepository trainingWorkoutRepository;
    private final StableStallRepository stableStallRepository;
    private final AreaRepository areaRepository;
    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final UserRepository userRepository;

    public ManagerEdaAnalyticsService(
            CareScheduleRepository careScheduleRepository,
            TrainingWorkoutRepository trainingWorkoutRepository,
            StableStallRepository stableStallRepository,
            AreaRepository areaRepository,
            AdmissionApplicationRepository admissionApplicationRepository,
            UserRepository userRepository
    ) {
        this.careScheduleRepository = careScheduleRepository;
        this.trainingWorkoutRepository = trainingWorkoutRepository;
        this.stableStallRepository = stableStallRepository;
        this.areaRepository = areaRepository;
        this.admissionApplicationRepository = admissionApplicationRepository;
        this.userRepository = userRepository;
    }

    public ManagerEdaOverviewResponse getOverview(Integer daysParam) {
        int days = daysParam != null ? Math.max(7, Math.min(daysParam, 365)) : 90;
        LocalDate sinceDate = LocalDate.now().minusDays(days);
        LocalDateTime sinceDateTime = sinceDate.atStartOfDay();

        // Cached user map for display names
        Map<Long, String> userNameMap = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, User::getFullName, (existing, replacement) -> existing));

        // 1. Vet Examination Delays & Rollover Analysis
        var schedules = careScheduleRepository.findAssignedSchedulesSince(sinceDate, sinceDateTime);
        var vetDelays = VetRolloverEdaCalculator.calculate(schedules, userNameMap);

        // 2. Trainer Evaluation & Fitness Progression
        var trainerRows = trainingWorkoutRepository.findTrainerWorkoutsSince(sinceDate);
        var trainerEvaluation = TrainerEdaCalculator.calculate(trainerRows, userNameMap);

        // 3. Facility Occupancy & Stall Stress Index
        var areas = areaRepository.findAll();
        var projections = stableStallRepository.countStallsByAreaAndStatus();
        long awaitingArrival = admissionApplicationRepository.countByStatus(AdmissionStatus.WAITING_FOR_ARRIVAL);
        var stallOccupancy = OccupancyEdaCalculator.calculate(areas, projections, awaitingArrival);

        // 4. Admission & Herd Intake Funnel
        var admissions = admissionApplicationRepository.findAdmissionsSince(sinceDateTime);
        var admissionFunnel = AdmissionFunnelEdaCalculator.calculate(admissions);

        return new ManagerEdaOverviewResponse(
                days,
                vetDelays,
                trainerEvaluation,
                stallOccupancy,
                admissionFunnel
        );
    }
}
