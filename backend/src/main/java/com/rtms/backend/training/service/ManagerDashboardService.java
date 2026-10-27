package com.rtms.backend.training.service;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.entity.CandidateHorseProfile;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.admission.repository.CandidateHorseProfileRepository;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.incident.entity.HorseIncidentReport;
import com.rtms.backend.incident.enums.IncidentSeverity;
import com.rtms.backend.incident.enums.IncidentStatus;
import com.rtms.backend.incident.repository.HorseIncidentReportRepository;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.enums.StallStatus;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.dto.ManagerDashboardSummaryResponse;
import com.rtms.backend.training.dto.ManagerDashboardSummaryResponse.*;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.enums.TrainingPlanStatus;
import com.rtms.backend.training.repository.HorseTrainingPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ManagerDashboardService {

    private final StableStallRepository stableStallRepository;
    private final AreaRepository areaRepository;
    private final HorseRepository horseRepository;
    private final HorseTrainingPlanRepository horseTrainingPlanRepository;
    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final CandidateHorseProfileRepository candidateHorseProfileRepository;
    private final HorseIncidentReportRepository horseIncidentReportRepository;
    private final UserRepository userRepository;

    public ManagerDashboardService(
            StableStallRepository stableStallRepository,
            AreaRepository areaRepository,
            HorseRepository horseRepository,
            HorseTrainingPlanRepository horseTrainingPlanRepository,
            AdmissionApplicationRepository admissionApplicationRepository,
            CandidateHorseProfileRepository candidateHorseProfileRepository,
            HorseIncidentReportRepository horseIncidentReportRepository,
            UserRepository userRepository) {
        this.stableStallRepository = stableStallRepository;
        this.areaRepository = areaRepository;
        this.horseRepository = horseRepository;
        this.horseTrainingPlanRepository = horseTrainingPlanRepository;
        this.admissionApplicationRepository = admissionApplicationRepository;
        this.candidateHorseProfileRepository = candidateHorseProfileRepository;
        this.horseIncidentReportRepository = horseIncidentReportRepository;
        this.userRepository = userRepository;
    }

    public ManagerDashboardSummaryResponse getSummary() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Stall & Barn Breakdown via Aggregation (avoid N+1)
        List<Area> areas = areaRepository.findAll();
        List<StableStallRepository.AreaStallCountProjection> areaStallCounts =
                stableStallRepository.countStallsByAreaAndStatus();

        // Map: areaId -> (StallStatus -> Count)
        Map<Long, Map<StallStatus, Long>> areaStatusCountMap = new HashMap<>();
        for (var row : areaStallCounts) {
            areaStatusCountMap
                    .computeIfAbsent(row.getAreaId(), k -> new EnumMap<>(StallStatus.class))
                    .put(row.getStatus(), row.getCount());
        }

        List<BarnOccupancyDto> barnOccupancies = new ArrayList<>();
        long totalStalls = 0;
        long totalOccupied = 0;
        long totalAvailable = 0;
        long totalReserved = 0;

        for (Area area : areas) {
            Map<StallStatus, Long> counts = areaStatusCountMap.getOrDefault(area.getId(), Collections.emptyMap());
            long occupied = counts.getOrDefault(StallStatus.OCCUPIED, 0L);
            long available = counts.getOrDefault(StallStatus.AVAILABLE, 0L);
            long reserved = counts.getOrDefault(StallStatus.RESERVED, 0L);
            long areaTotal = occupied + available + reserved;

            totalStalls += areaTotal;
            totalOccupied += occupied;
            totalAvailable += available;
            totalReserved += reserved;

            double areaOccupancyRate = areaTotal > 0
                    ? ((double) (occupied + reserved) / areaTotal) * 100.0
                    : 0.0;

            barnOccupancies.add(new BarnOccupancyDto(
                    area.getId(),
                    area.getCode(),
                    area.getName(),
                    area.getType() != null ? area.getType().name() : "REGULAR",
                    areaTotal,
                    occupied,
                    available,
                    reserved,
                    Math.round(areaOccupancyRate * 10.0) / 10.0
            ));
        }

        // Sort barns: Quarantine first, then by code
        barnOccupancies.sort(Comparator.comparing((BarnOccupancyDto b) -> !"QUARANTINE".equalsIgnoreCase(b.areaType()))
                .thenComparing(BarnOccupancyDto::areaCode));

        double overallOccupancyRate = totalStalls > 0
                ? ((double) (totalOccupied + totalReserved) / totalStalls) * 100.0
                : 0.0;

        StallSummary stallSummary = new StallSummary(
                totalStalls,
                totalOccupied,
                totalAvailable,
                totalReserved,
                Math.round(overallOccupancyRate * 10.0) / 10.0
        );

        // 2. Fleet Summary
        long totalHorses = horseRepository.count();
        long activeEligible = horseRepository.countByCurrentStatus(HorseStatus.ELIGIBLE);
        long inTraining = horseTrainingPlanRepository.countByStatus(TrainingPlanStatus.ACTIVE);
        long medicalBlocked = horseRepository.countByTrainingDecision(TrainingDecision.BLOCKED);
        long inactive = horseRepository.countByCurrentStatus(HorseStatus.INACTIVE);

        FleetSummary fleetSummary = new FleetSummary(
                totalHorses,
                activeEligible,
                inTraining,
                medicalBlocked,
                inactive
        );

        // 3. Admission Pipeline & Actions
        long managerReviewCount = admissionApplicationRepository.countByStatus(AdmissionStatus.MANAGER_REVIEW);
        long waitingArrivalCount = admissionApplicationRepository.countByStatus(AdmissionStatus.WAITING_FOR_ARRIVAL);
        long groomReviewCount = admissionApplicationRepository.countByStatus(AdmissionStatus.GROOM_REVIEW);
        long vetReviewCount = admissionApplicationRepository.countByStatus(AdmissionStatus.VET_REVIEW);
        long trainerReviewCount = admissionApplicationRepository.countByStatus(AdmissionStatus.TRAINER_REVIEW);
        long totalPending = managerReviewCount + waitingArrivalCount + groomReviewCount + vetReviewCount + trainerReviewCount;

        // Find admissions waiting for Manager review
        List<AdmissionApplication> managerReviewApps =
                admissionApplicationRepository.findByStatusOrderBySubmittedAtDesc(AdmissionStatus.MANAGER_REVIEW);

        // Find admissions with arrival deadline within 48h or already overdue
        List<AdmissionApplication> urgentArrivalApps =
                admissionApplicationRepository.findByStatusAndArrivalDeadlineAtBetweenOrderByArrivalDeadlineAtAsc(
                        AdmissionStatus.WAITING_FOR_ARRIVAL,
                        now.minusDays(14),
                        now.plusHours(48)
                );

        long arrivalDeadlineAlertCount = urgentArrivalApps.size();

        AdmissionSummary admissionSummary = new AdmissionSummary(
                totalPending,
                managerReviewCount,
                waitingArrivalCount,
                groomReviewCount,
                arrivalDeadlineAlertCount
        );

        // Combine actionable admissions (Manager review first, then urgent arrivals)
        List<AdmissionApplication> actionableAdmissions = new ArrayList<>(managerReviewApps);
        for (AdmissionApplication app : urgentArrivalApps) {
            if (!actionableAdmissions.contains(app)) {
                actionableAdmissions.add(app);
            }
        }

        // Batch resolve candidate horses and owners (zero N+1)
        List<Long> admissionIds = actionableAdmissions.stream().map(AdmissionApplication::getId).toList();
        Map<Long, CandidateHorseProfile> candidateMap = candidateHorseProfileRepository.findByAdmissionIdIn(admissionIds)
                .stream().collect(Collectors.toMap(CandidateHorseProfile::getAdmissionId, c -> c, (a, b) -> a));

        Set<Long> ownerIds = actionableAdmissions.stream().map(AdmissionApplication::getOwnerId).collect(Collectors.toSet());
        Map<Long, User> ownerMap = userRepository.findAllById(ownerIds)
                .stream().collect(Collectors.toMap(User::getId, u -> u));

        List<PendingAdmissionActionDto> pendingAdmissions = actionableAdmissions.stream().map(app -> {
            CandidateHorseProfile candidate = candidateMap.get(app.getId());
            User owner = ownerMap.get(app.getOwnerId());
            boolean isArrivalAlert = app.getStatus() == AdmissionStatus.WAITING_FOR_ARRIVAL &&
                    app.getArrivalDeadlineAt() != null &&
                    (app.getArrivalDeadlineAt().isBefore(now) || app.getArrivalDeadlineAt().isBefore(now.plusHours(48)));

            return new PendingAdmissionActionDto(
                    app.getId(),
                    candidate != null ? candidate.getName() : "Unknown Horse",
                    candidate != null ? candidate.getBreed() : "—",
                    app.getOwnerId(),
                    owner != null ? owner.getFullName() : "Owner #" + app.getOwnerId(),
                    app.getStatus().name(),
                    app.getSubmittedAt(),
                    app.getArrivalDeadlineAt(),
                    isArrivalAlert
            );
        }).toList();

        // 4. Urgent Incidents: status in (REPORTED, IN_REVIEW) and severity in (HIGH, CRITICAL)
        List<IncidentStatus> openStatuses = List.of(IncidentStatus.REPORTED, IncidentStatus.IN_REVIEW);
        List<IncidentSeverity> urgentSeverities = List.of(IncidentSeverity.HIGH, IncidentSeverity.CRITICAL);

        long openIncidents = horseIncidentReportRepository.countByStatusIn(openStatuses);
        long criticalUrgentIncidents = horseIncidentReportRepository.countByStatusInAndSeverityIn(openStatuses, urgentSeverities);

        IncidentSummary incidentSummary = new IncidentSummary(
                openIncidents,
                criticalUrgentIncidents
        );

        List<HorseIncidentReport> urgentIncidentEntities =
                horseIncidentReportRepository.findUrgentIncidents(openStatuses, urgentSeverities);

        // Batch resolve horses and reporters
        Set<Long> incidentHorseIds = urgentIncidentEntities.stream().map(HorseIncidentReport::getHorseId).collect(Collectors.toSet());
        Map<Long, Horse> horseMap = horseRepository.findAllById(incidentHorseIds)
                .stream().collect(Collectors.toMap(Horse::getId, h -> h));

        Set<Long> reporterIds = urgentIncidentEntities.stream().map(HorseIncidentReport::getReportedById).collect(Collectors.toSet());
        Map<Long, User> reporterMap = userRepository.findAllById(reporterIds)
                .stream().collect(Collectors.toMap(User::getId, u -> u));

        List<UrgentIncidentActionDto> urgentIncidents = urgentIncidentEntities.stream().map(inc -> {
            Horse horse = horseMap.get(inc.getHorseId());
            User reporter = reporterMap.get(inc.getReportedById());
            return new UrgentIncidentActionDto(
                    inc.getId(),
                    inc.getHorseId(),
                    horse != null ? horse.getName() : "Horse #" + inc.getHorseId(),
                    inc.getTitle(),
                    inc.getDescription(),
                    inc.getSeverity() != null ? inc.getSeverity().name() : "HIGH",
                    inc.getStatus().name(),
                    inc.getReportedAt(),
                    inc.getReportedById(),
                    reporter != null ? reporter.getFullName() : "Staff #" + inc.getReportedById()
            );
        }).toList();

        return new ManagerDashboardSummaryResponse(
                stallSummary,
                fleetSummary,
                admissionSummary,
                incidentSummary,
                barnOccupancies,
                pendingAdmissions,
                urgentIncidents
        );
    }
}
