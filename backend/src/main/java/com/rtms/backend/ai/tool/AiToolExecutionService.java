package com.rtms.backend.ai.tool;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.ai.dto.ClinicalChunkResponseDto;
import com.rtms.backend.ai.rag.service.ClinicalRetrievalService;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.enums.CareSchedulePurpose;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.notification.dto.HorseAlertResponse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.GroomIncidentReport;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.enums.AreaType;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.GroomDailyTaskRepository;
import com.rtms.backend.stable.repository.GroomIncidentReportRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.stable.service.GroomDailyTaskService;
import com.rtms.backend.training.dto.GroomConflictRow;
import com.rtms.backend.training.dto.HorseFitnessTrendItemResponse;
import com.rtms.backend.training.dto.HorseSpecialtyResponse;
import com.rtms.backend.training.dto.LotHorseNameRow;
import com.rtms.backend.training.dto.PlanSummaryResponse;
import com.rtms.backend.training.dto.TrainingLotRow;
import com.rtms.backend.training.entity.RacingReadinessAssessment;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.HorseTrainingPlanRepository;
import com.rtms.backend.training.repository.RacingReadinessAssessmentRepository;
import com.rtms.backend.training.repository.TrainingLotRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import com.rtms.backend.training.service.HorsePerformanceService;
import com.rtms.backend.training.service.HorseSpecialtyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dịch vụ điều phối thực thi các công cụ (Tool Execution Dispatcher) được gọi bởi LLM.
 *
 * Kiểm tra 2 tầng an toàn:
 * 1. Phân quyền vai trò (DynamicToolRegistryService.isToolAllowedForRole).
 * 2. Đảm bảo 100% Read-Only / Query / RAG Retrieval cho toàn bộ 5 vai trò.
 */
@Service
public class AiToolExecutionService {

    private static final Logger log = LoggerFactory.getLogger(AiToolExecutionService.class);

    private final DynamicToolRegistryService toolRegistryService;
    private final ClinicalRetrievalService clinicalRetrievalService;
    private final HorseRepository horseRepository;
    private final TrainingWorkoutRepository trainingWorkoutRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final CareScheduleRepository careScheduleRepository;
    private final StableStallRepository stableStallRepository;
    private final GroomDailyTaskRepository groomDailyTaskRepository;
    private final GroomDailyTaskService groomDailyTaskService;
    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final TrainingLotRepository trainingLotRepository;
    private final HorseTrainingPlanRepository horseTrainingPlanRepository;
    private final HorseSpecialtyService horseSpecialtyService;
    private final HorsePerformanceService horsePerformanceService;
    private final RacingReadinessAssessmentRepository racingReadinessAssessmentRepository;
    private final GroomIncidentReportRepository groomIncidentReportRepository;
    private final AreaRepository areaRepository;

    public AiToolExecutionService(DynamicToolRegistryService toolRegistryService,
                                  ClinicalRetrievalService clinicalRetrievalService,
                                  HorseRepository horseRepository,
                                  TrainingWorkoutRepository trainingWorkoutRepository,
                                  HealthRecordRepository healthRecordRepository,
                                  CareScheduleRepository careScheduleRepository,
                                  StableStallRepository stableStallRepository,
                                  GroomDailyTaskRepository groomDailyTaskRepository,
                                  GroomDailyTaskService groomDailyTaskService,
                                  AdmissionApplicationRepository admissionApplicationRepository,
                                  TrainingLotRepository trainingLotRepository,
                                  HorseTrainingPlanRepository horseTrainingPlanRepository,
                                  HorseSpecialtyService horseSpecialtyService,
                                  HorsePerformanceService horsePerformanceService,
                                  RacingReadinessAssessmentRepository racingReadinessAssessmentRepository,
                                  GroomIncidentReportRepository groomIncidentReportRepository,
                                  AreaRepository areaRepository) {
        this.toolRegistryService = toolRegistryService;
        this.clinicalRetrievalService = clinicalRetrievalService;
        this.horseRepository = horseRepository;
        this.trainingWorkoutRepository = trainingWorkoutRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.careScheduleRepository = careScheduleRepository;
        this.stableStallRepository = stableStallRepository;
        this.groomDailyTaskRepository = groomDailyTaskRepository;
        this.groomDailyTaskService = groomDailyTaskService;
        this.admissionApplicationRepository = admissionApplicationRepository;
        this.trainingLotRepository = trainingLotRepository;
        this.horseTrainingPlanRepository = horseTrainingPlanRepository;
        this.horseSpecialtyService = horseSpecialtyService;
        this.horsePerformanceService = horsePerformanceService;
        this.racingReadinessAssessmentRepository = racingReadinessAssessmentRepository;
        this.groomIncidentReportRepository = groomIncidentReportRepository;
        this.areaRepository = areaRepository;
    }

    /**
     * Thực thi một công cụ theo yêu cầu của LLM.
     */
    @Transactional(readOnly = true)
    public Object executeTool(String toolName, Map<String, Object> arguments, AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Người dùng chưa được xác thực");
        }

        // 1. Kiểm tra thẩm quyền gọi tool theo vai trò
        if (!toolRegistryService.isToolAllowedForRole(toolName, currentUser.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "TOOL_FORBIDDEN",
                    "Vai trò [" + currentUser.getRole() + "] không có thẩm quyền thực thi công cụ [" + toolName + "]");
        }

        Map<String, Object> args = arguments != null ? arguments : Collections.emptyMap();
        log.info("AiToolExecutionService: User [{}] ({}) gọi tool [{}] với args {}",
                currentUser.getUserId(), currentUser.getRole(), toolName, args);

        return switch (toolName) {
            // HORSE_OWNER Tools
            case "getMyHorses" -> handleGetMyHorses(currentUser);
            case "getMyHorseHealthSummary" -> handleGetMyHorseHealthSummary(args, currentUser);
            case "getMyHorseTrainingProgress" -> handleGetMyHorseTrainingProgress(args, currentUser);
            case "getMyHorseUpcomingSchedule" -> handleGetMyHorseUpcomingSchedule(args, currentUser);
            case "getMyHorseAdmissions" -> handleGetMyHorseAdmissions(currentUser);
            case "getHorseTrainingSummary" -> handleGetHorseTrainingSummary(args, currentUser);
            case "getHorseCareSchedule" -> handleGetHorseCareSchedule(args, currentUser);

            // HEAD_TRAINER Tools
            case "getTrainerHorses" -> handleGetTrainerHorses(currentUser);
            case "getWorkoutHistory" -> handleGetWorkoutHistory(args);
            case "getHorseFitnessTrend" -> handleGetHorseFitnessTrend(args);
            case "getTrainerSchedule" -> handleGetTrainerSchedule(args, currentUser);
            case "getTrainerPlansSummary" -> handleGetTrainerPlansSummary(args, currentUser);
            case "getHorseSpecialtyAnalysis" -> handleGetHorseSpecialtyAnalysis(args);
            case "getTrainerRiskAlerts" -> handleGetTrainerRiskAlerts(args, currentUser);
            case "getRacingReadinessAssessment" -> handleGetRacingReadinessAssessment(args);
            case "checkGroomConflictForLot" -> handleCheckGroomConflictForLot(args);

            // VETERINARIAN Tools
            case "searchClinicalKnowledge" -> handleSearchClinicalKnowledge(args, currentUser);
            case "getHorseMedicalHistory" -> handleGetHorseMedicalHistory(args);
            case "getActivePrescriptions" -> handleGetActivePrescriptions(args);
            case "getPreventiveCareSchedule" -> handleGetPreventiveCareSchedule(args);
            case "getQuarantineStatus" -> handleGetQuarantineStatus();
            case "getRecentIncidentReports" -> handleGetRecentIncidentReports();

            // GROOM Tools
            case "getDailyGroomTasks" -> handleGetDailyGroomTasks(currentUser);
            case "getMyAssignedHorses" -> handleGetMyAssignedHorses(currentUser);
            case "getTodayAggregatedTasks" -> handleGetTodayAggregatedTasks(args, currentUser);
            case "getHorseDietaryAndCareAlerts" -> handleGetHorseDietaryAndCareAlerts(args);
            case "checkStallSuppliesStatus" -> handleCheckStallSuppliesStatus(args);
            case "getHorseStallLocation" -> handleGetHorseStallLocation(args);

            // CLUB_MANAGER Tools
            case "getStableOccupancyMetrics" -> handleGetStableOccupancyMetrics();
            case "getAdmissionRequestsOverview" -> handleGetAdmissionRequestsOverview();
            case "getTrainerAreaDistribution" -> handleGetTrainerAreaDistribution();
            case "getClubHealthOverview" -> handleGetClubHealthOverview();
            case "getUpcomingRaces", "getUpcomingRacesAndCompetitions" -> handleGetUpcomingRaces();

            default -> throw new ApiException(HttpStatus.NOT_FOUND, "TOOL_NOT_FOUND", "Không tìm thấy công cụ: " + toolName);
        };
    }

    // =================================================================
    // HORSE_OWNER Handlers
    // =================================================================

    private Object handleGetMyHorses(AuthenticatedUser currentUser) {
        List<Horse> horses = horseRepository.findByOwnerId(currentUser.getUserId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Horse h : horses) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseId", h.getId());
            item.put("name", h.getName());
            item.put("registrationNumber", h.getRegistrationNumber());
            item.put("currentStatus", h.getCurrentStatus());
            item.put("trainingDecision", h.getTrainingDecision());
            item.put("currentStallId", h.getCurrentStallId());
            result.add(item);
        }
        return result;
    }

    private Object handleGetMyHorseHealthSummary(Map<String, Object> args, AuthenticatedUser currentUser) {
        Long horseId = parseLong(args.get("horseId"));
        if (horseId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Thiếu horseId");
        }
        verifyHorseOwnershipIfOwner(horseId, currentUser);

        Horse horse = horseRepository.findById(horseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Không tìm thấy ngựa #" + horseId));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("horseId", horse.getId());
        summary.put("horseName", horse.getName());
        summary.put("currentStatus", horse.getCurrentStatus());
        summary.put("trainingDecision", horse.getTrainingDecision());
        summary.put("trainingDecisionReason", horse.getTrainingDecisionReason() != null ? horse.getTrainingDecisionReason() : "N/A");

        List<HealthRecord> records = healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(horseId);
        if (!records.isEmpty()) {
            HealthRecord latest = records.get(0);
            summary.put("latestDiagnosis", latest.getDiagnosis());
            summary.put("latestTreatment", latest.getTreatment());
            summary.put("restrictionDetails", latest.getRestrictionDetails());
            summary.put("lastExaminedAt", latest.getExaminedAt());
        } else {
            summary.put("latestDiagnosis", "Chưa có bệnh án ghi nhận");
        }

        List<CareSchedule> activeSchedules = careScheduleRepository.findByHorseIdAndStatusIn(
                horseId, List.of(CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS));
        List<Map<String, Object>> careList = activeSchedules.stream().map(cs -> Map.<String, Object>of(
                "scheduleId", cs.getId(),
                "careType", cs.getCareType(),
                "scheduledAt", cs.getScheduledAt() != null ? cs.getScheduledAt().toString() : "N/A",
                "purpose", cs.getPurpose() != null ? cs.getPurpose().name() : "N/A"
        )).toList();
        summary.put("activeCareSchedules", careList);

        return summary;
    }

    private Object handleGetMyHorseTrainingProgress(Map<String, Object> args, AuthenticatedUser currentUser) {
        Long horseId = parseLong(args.get("horseId"));
        if (horseId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Thiếu horseId");
        }
        verifyHorseOwnershipIfOwner(horseId, currentUser);

        List<PlanSummaryResponse> summaries = horseTrainingPlanRepository.findPlanSummaries(null, horseId);
        return summaries != null ? summaries : Collections.emptyList();
    }

    private Object handleGetMyHorseUpcomingSchedule(Map<String, Object> args, AuthenticatedUser currentUser) {
        Long ownerId = currentUser.getUserId();
        LocalDate from = parseDate(args.get("fromDate"), LocalDate.now());
        LocalDate to = parseDate(args.get("toDate"), LocalDate.now().plusDays(14));

        List<TrainingLotRow> lotRows = trainingLotRepository.findOwnerLotRows(ownerId, from, to);
        if (lotRows.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> lotIds = lotRows.stream().map(TrainingLotRow::lotId).toList();
        List<LotHorseNameRow> horseNames = trainingLotRepository.findActiveHorseNamesOfOwner(lotIds, ownerId);
        Map<Long, List<String>> namesByLot = horseNames.stream()
                .collect(Collectors.groupingBy(LotHorseNameRow::lotId,
                        Collectors.mapping(LotHorseNameRow::horseName, Collectors.toList())));

        List<Map<String, Object>> result = new ArrayList<>();
        for (TrainingLotRow lot : lotRows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("lotId", lot.lotId());
            item.put("lotDate", lot.lotDate() != null ? lot.lotDate().toString() : "N/A");
            item.put("startTime", lot.startTime() != null ? lot.startTime().toString() : "N/A");
            item.put("endTime", lot.endTime() != null ? lot.endTime().toString() : "N/A");
            item.put("subjectName", lot.subjectName());
            item.put("myHorseNames", namesByLot.getOrDefault(lot.lotId(), Collections.emptyList()));
            result.add(item);
        }
        return result;
    }

    private Object handleGetMyHorseAdmissions(AuthenticatedUser currentUser) {
        Long ownerId = currentUser.getUserId();
        List<AdmissionApplication> apps = admissionApplicationRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (AdmissionApplication a : apps) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("admissionId", a.getId());
            item.put("status", a.getStatus());
            item.put("horseId", a.getHorseId());
            item.put("groomDecision", a.getGroomDecision());
            item.put("vetTrainingDecision", a.getVetTrainingDecision());
            item.put("managerDecision", a.getManagerDecision());
            item.put("submittedAt", a.getSubmittedAt());
            item.put("createdAt", a.getCreatedAt());
            result.add(item);
        }
        return result;
    }

    private Object handleGetHorseTrainingSummary(Map<String, Object> args, AuthenticatedUser currentUser) {
        Long horseId = parseLong(args.get("horseId"));
        if (horseId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Thiếu horseId");
        }
        verifyHorseOwnershipIfOwner(horseId, currentUser);

        Horse horse = horseRepository.findById(horseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Không tìm thấy ngựa #" + horseId));

        List<TrainingWorkout> workouts = trainingWorkoutRepository.findByHorseIdOrderByCreatedAtDesc(horseId);
        int totalSessions = workouts.size();
        long completedSessions = workouts.stream().filter(w -> w.getStatus() == WorkoutStatus.COMPLETED).count();

        return Map.of(
                "horseId", horse.getId(),
                "name", horse.getName(),
                "trainingDecision", horse.getTrainingDecision(),
                "trainingDecisionReason", horse.getTrainingDecisionReason() != null ? horse.getTrainingDecisionReason() : "N/A",
                "totalWorkouts", totalSessions,
                "completedWorkouts", completedSessions
        );
    }

    private Object handleGetHorseCareSchedule(Map<String, Object> args, AuthenticatedUser currentUser) {
        Long horseId = parseLong(args.get("horseId"));
        if (horseId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Thiếu horseId");
        }
        verifyHorseOwnershipIfOwner(horseId, currentUser);

        List<CareSchedule> schedules = careScheduleRepository.findByHorseIdOrderByScheduledAtAsc(horseId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (CareSchedule cs : schedules) {
            result.add(Map.of(
                    "scheduleId", cs.getId(),
                    "careType", cs.getCareType(),
                    "scheduledAt", cs.getScheduledAt() != null ? cs.getScheduledAt().toString() : "N/A",
                    "status", cs.getStatus(),
                    "purpose", cs.getPurpose() != null ? cs.getPurpose().name() : "N/A"
            ));
        }
        return result;
    }

    // =================================================================
    // HEAD_TRAINER Handlers
    // =================================================================

    private Object handleGetTrainerHorses(AuthenticatedUser currentUser) {
        Long trainerId = currentUser != null ? currentUser.getUserId() : null;
        if (trainerId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Không xác định được danh tính Huấn luyện viên");
        }

        List<Horse> horses = horseRepository.findManagedHorsesByTrainerIdAndStatusIn(
                trainerId,
                List.of(HorseStatus.ELIGIBLE, HorseStatus.CANDIDATE)
        );
        List<Map<String, Object>> list = new ArrayList<>();
        for (Horse h : horses) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseId", h.getId());
            item.put("name", h.getName());
            item.put("status", h.getCurrentStatus());
            item.put("trainingDecision", h.getTrainingDecision());
            item.put("trainingDecisionReason", h.getTrainingDecisionReason() != null ? h.getTrainingDecisionReason() : "N/A");
            item.put("stallId", h.getCurrentStallId() != null ? h.getCurrentStallId() : "Chưa có chuồng");
            list.add(item);
        }
        return Map.of(
                "trainerId", trainerId,
                "totalManagedHorses", list.size(),
                "horses", list
        );
    }

    private Object handleGetWorkoutHistory(Map<String, Object> args) {
        Long horseId = parseLong(args.get("horseId"));
        if (horseId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Thiếu horseId");
        }
        List<TrainingWorkout> workouts = trainingWorkoutRepository.findByHorseIdOrderByCreatedAtDesc(horseId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (TrainingWorkout w : workouts) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("workoutId", w.getId());
            item.put("status", w.getStatus());
            item.put("actualDistanceMeters", w.getActualDistanceMeters());
            item.put("topSpeedKmh", w.getTopSpeedKmh());
            item.put("averageHeartRate", w.getAverageHeartRate());
            item.put("performanceRating", w.getPerformanceRating());
            item.put("trainerFeedback", w.getTrainerFeedback());
            list.add(item);
        }
        return list;
    }

    private Object handleGetHorseFitnessTrend(Map<String, Object> args) {
        Long horseId = parseLong(args.get("horseId"));
        if (horseId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Thiếu horseId");
        }
        List<HorseFitnessTrendItemResponse> trends = trainingWorkoutRepository.findFitnessTrend(
                horseId, LocalDate.now().minusDays(30), LocalDate.now()
        );
        return trends != null ? trends : Collections.emptyList();
    }

    private Object handleGetTrainerSchedule(Map<String, Object> args, AuthenticatedUser currentUser) {
        Long trainerId = currentUser.getUserId();
        LocalDate from;
        LocalDate to;

        if (args.containsKey("date") && args.get("date") != null && !args.get("date").toString().isBlank()) {
            from = parseDate(args.get("date"), LocalDate.now());
            to = from;
        } else {
            from = parseDate(args.get("fromDate"), LocalDate.now());
            to = parseDate(args.get("toDate"), from);
        }

        List<TrainingLotRow> lotRows = trainingLotRepository.findTrainerLotRows(trainerId, from, to, false);
        if (lotRows.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> lotIds = lotRows.stream().map(TrainingLotRow::lotId).toList();
        List<LotHorseNameRow> horseNames = trainingLotRepository.findActiveHorseNames(lotIds);
        Map<Long, List<String>> namesByLot = horseNames.stream()
                .collect(Collectors.groupingBy(LotHorseNameRow::lotId,
                        Collectors.mapping(LotHorseNameRow::horseName, Collectors.toList())));

        List<Map<String, Object>> result = new ArrayList<>();
        for (TrainingLotRow lot : lotRows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("lotId", lot.lotId());
            item.put("lotDate", lot.lotDate() != null ? lot.lotDate().toString() : "N/A");
            item.put("startTime", lot.startTime() != null ? lot.startTime().toString() : "N/A");
            item.put("endTime", lot.endTime() != null ? lot.endTime().toString() : "N/A");
            item.put("subjectName", lot.subjectName());
            item.put("horseCount", lot.occupied());
            item.put("horseNames", namesByLot.getOrDefault(lot.lotId(), Collections.emptyList()));
            result.add(item);
        }
        return result;
    }

    private Object handleGetTrainerPlansSummary(Map<String, Object> args, AuthenticatedUser currentUser) {
        Long trainerId = currentUser.getUserId();
        Long horseId = parseLong(args.get("horseId"));
        String status = args.get("status") != null ? args.get("status").toString().trim() : null;

        List<PlanSummaryResponse> summaries = horseTrainingPlanRepository.findPlanSummaries(trainerId, horseId);
        if (status != null && !status.isBlank()) {
            summaries = summaries.stream()
                    .filter(p -> p.getStatus() != null && p.getStatus().name().equalsIgnoreCase(status))
                    .toList();
        }
        return summaries != null ? summaries : Collections.emptyList();
    }

    private Object handleGetHorseSpecialtyAnalysis(Map<String, Object> args) {
        Long horseId = parseLong(args.get("horseId"));
        if (horseId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Thiếu horseId");
        }

        Map<Long, HorseSpecialtyResponse> specialties = horseSpecialtyService.getSpecialties(List.of(horseId));
        HorseSpecialtyResponse resp = specialties.get(horseId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseId", horseId);
        result.put("optimalDistances", resp != null ? resp.strongDistances() : Collections.emptyList());
        result.put("preferredSurface", resp != null ? resp.preferredSurface() : null);
        result.put("distanceStats", resp != null ? resp.distanceStats() : Collections.emptyList());
        result.put("minWorkoutsPerGroup", resp != null ? resp.minWorkoutsPerGroup() : 3);
        return result;
    }

    private Object handleGetTrainerRiskAlerts(Map<String, Object> args, AuthenticatedUser currentUser) {
        Long horseId = parseLong(args.get("horseId"));
        if (horseId != null) {
            List<HorseAlertResponse> alerts = horsePerformanceService.getHorseAlerts(horseId, currentUser);
            return Map.of(
                    "horseId", horseId,
                    "alertsCount", alerts.size(),
                    "alerts", alerts
            );
        }

        // Nếu không truyền horseId, tổng hợp cho toàn bộ ngựa do HLV quản lý
        List<Horse> managedHorses = horseRepository.findManagedHorsesByTrainerIdAndStatusIn(
                currentUser.getUserId(),
                List.of(HorseStatus.ELIGIBLE, HorseStatus.CANDIDATE)
        );

        List<Map<String, Object>> aggregated = new ArrayList<>();
        for (Horse h : managedHorses) {
            List<HorseAlertResponse> alerts = horsePerformanceService.getHorseAlerts(h.getId(), currentUser);
            if (!alerts.isEmpty()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("horseId", h.getId());
                item.put("horseName", h.getName());
                item.put("alertsCount", alerts.size());
                item.put("alerts", alerts);
                aggregated.add(item);
            }
        }

        return Map.of(
                "trainerId", currentUser.getUserId(),
                "horsesWithRiskAlertsCount", aggregated.size(),
                "riskAlerts", aggregated
        );
    }

    private Object handleGetRacingReadinessAssessment(Map<String, Object> args) {
        Long horseId = parseLong(args.get("horseId"));
        if (horseId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Thiếu horseId");
        }

        Optional<RacingReadinessAssessment> opt = racingReadinessAssessmentRepository.findTopByHorseIdOrderByCreatedAtDesc(horseId);
        if (opt.isEmpty()) {
            return Map.of(
                    "horseId", horseId,
                    "message", "Chưa có bản đánh giá sẵn sàng thi đấu nào cho ngựa này."
            );
        }

        RacingReadinessAssessment r = opt.get();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assessmentId", r.getId());
        result.put("horseId", r.getHorseId());
        result.put("readinessStatus", r.getReadinessStatus());
        result.put("fitnessScore", r.getFitnessScore());
        result.put("conformationScore", r.getConformationScore());
        result.put("temperamentScore", r.getTemperamentScore());
        result.put("gaitQualityScore", r.getGaitQualityScore());
        result.put("estimatedMonthsToRace", r.getEstimatedMonthsToRace());
        result.put("assessmentDate", r.getAssessmentDate());
        result.put("validUntil", r.getValidUntil());
        result.put("remarks", r.getRemarks());
        result.put("trainerId", r.getTrainerId());
        result.put("admissionId", r.getAdmissionId());
        result.put("createdAt", r.getCreatedAt());
        return result;
    }

    private Object handleCheckGroomConflictForLot(Map<String, Object> args) {
        Long horseId = parseLong(args.get("horseId"));
        Long groomId = parseLong(args.get("groomId"));
        if (horseId == null || groomId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Thiếu horseId hoặc groomId");
        }

        LocalDate fromDate = parseDate(args.get("date"), LocalDate.now());
        List<GroomConflictRow> conflicts = trainingWorkoutRepository.findGroomConflicts(horseId, groomId, fromDate);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseId", horseId);
        result.put("groomId", groomId);
        result.put("checkedFromDate", fromDate.toString());
        result.put("hasConflict", !conflicts.isEmpty());
        result.put("conflictCount", conflicts.size());
        result.put("conflicts", conflicts);
        return result;
    }

    // =================================================================
    // VETERINARIAN Handlers
    // =================================================================

    private Object handleSearchClinicalKnowledge(Map<String, Object> args, AuthenticatedUser currentUser) {
        String query = (String) args.get("query");
        Long horseId = parseLong(args.get("horseId"));
        Integer limit = args.get("limit") != null ? ((Number) args.get("limit")).intValue() : 5;

        List<ClinicalChunkResponseDto> chunks = clinicalRetrievalService.retrieveSimilarClinicalRecords(
                query, horseId, currentUser.getRole(), 0.60, limit);
        return chunks;
    }

    private Object handleGetHorseMedicalHistory(Map<String, Object> args) {
        Long horseId = parseLong(args.get("horseId"));
        if (horseId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Thiếu horseId");
        }
        List<HealthRecord> records = healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(horseId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (HealthRecord hr : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("recordId", hr.getId());
            item.put("examinedAt", hr.getExaminedAt());
            item.put("symptoms", hr.getSymptoms());
            item.put("findings", hr.getFindings());
            item.put("diagnosis", hr.getDiagnosis());
            item.put("treatment", hr.getTreatment());
            item.put("trainingDecision", hr.getTrainingDecision());
            item.put("restrictionDetails", hr.getRestrictionDetails());
            list.add(item);
        }
        return list;
    }

    private Object handleGetActivePrescriptions(Map<String, Object> args) {
        Long horseId = parseLong(args.get("horseId"));
        if (horseId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Thiếu horseId");
        }
        List<CareSchedule> active = careScheduleRepository.findByHorseIdAndStatusIn(
                horseId, List.of(CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS)
        );
        return active.stream().map(cs -> Map.of(
                "careScheduleId", cs.getId(),
                "careType", cs.getCareType(),
                "scheduledAt", cs.getScheduledAt(),
                "purpose", cs.getPurpose() != null ? cs.getPurpose().name() : "N/A"
        )).toList();
    }

    private Object handleGetPreventiveCareSchedule(Map<String, Object> args) {
        Long horseId = parseLong(args.get("horseId"));
        LocalDate from = parseDate(args.get("fromDate"), null);
        LocalDate to = parseDate(args.get("toDate"), null);

        List<CareSchedule> schedules;
        if (horseId != null) {
            schedules = careScheduleRepository.findByHorseIdOrderByScheduledAtAsc(horseId);
        } else {
            schedules = careScheduleRepository.findAll();
        }

        List<Map<String, Object>> result = schedules.stream()
                .filter(cs -> cs.getPurpose() == CareSchedulePurpose.PREVENTIVE || cs.getCareType() == CareType.ROUTINE)
                .filter(cs -> {
                    if (cs.getScheduledAt() == null) return false;
                    LocalDate d = cs.getScheduledAt().toLocalDate();
                    if (from != null && d.isBefore(from)) return false;
                    if (to != null && d.isAfter(to)) return false;
                    return true;
                })
                .sorted(Comparator.comparing(CareSchedule::getScheduledAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(cs -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("scheduleId", cs.getId());
                    item.put("horseId", cs.getHorseId());
                    item.put("careType", cs.getCareType());
                    item.put("purpose", cs.getPurpose());
                    item.put("status", cs.getStatus());
                    item.put("scheduledAt", cs.getScheduledAt());
                    item.put("description", cs.getDescription());
                    return item;
                })
                .toList();

        return result;
    }

    private Object handleGetQuarantineStatus() {
        List<Area> quarantineAreas = areaRepository.findByType(AreaType.QUARANTINE);
        List<Long> qAreaIds = quarantineAreas.stream().map(Area::getId).toList();
        List<StableStall> qStalls = qAreaIds.isEmpty() ? Collections.emptyList()
                : stableStallRepository.findByAreaIdInOrdered(qAreaIds);
        List<Long> qStallIds = qStalls.stream().map(StableStall::getId).toList();

        Map<Long, StableStall> stallMap = qStalls.stream()
                .collect(Collectors.toMap(StableStall::getId, s -> s, (a, b) -> a));

        List<Horse> horsesInQStalls = qStallIds.isEmpty() ? Collections.emptyList()
                : horseRepository.findByCurrentStallIdIn(qStallIds);

        List<Horse> allHorses = horseRepository.findAll();
        List<Horse> blockedHorses = allHorses.stream()
                .filter(h -> h.getTrainingDecision() == TrainingDecision.BLOCKED)
                .toList();

        Map<Long, Horse> uniqueHorses = new LinkedHashMap<>();
        for (Horse h : horsesInQStalls) {
            uniqueHorses.put(h.getId(), h);
        }
        for (Horse h : blockedHorses) {
            uniqueHorses.put(h.getId(), h);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Horse h : uniqueHorses.values()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseId", h.getId());
            item.put("horseName", h.getName());
            item.put("registrationNumber", h.getRegistrationNumber());
            item.put("currentStatus", h.getCurrentStatus());
            item.put("trainingDecision", h.getTrainingDecision());
            item.put("trainingDecisionReason", h.getTrainingDecisionReason());
            item.put("currentStallId", h.getCurrentStallId());

            boolean inQuarantineStall = h.getCurrentStallId() != null && qStallIds.contains(h.getCurrentStallId());
            item.put("isInQuarantineStall", inQuarantineStall);
            if (inQuarantineStall && stallMap.containsKey(h.getCurrentStallId())) {
                item.put("stallCode", stallMap.get(h.getCurrentStallId()).getStallCode());
            }

            List<HealthRecord> records = healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(h.getId());
            if (!records.isEmpty()) {
                HealthRecord latest = records.get(0);
                item.put("latestDiagnosis", latest.getDiagnosis());
                item.put("restrictionDetails", latest.getRestrictionDetails());
                item.put("lastExaminedAt", latest.getExaminedAt());
            }

            result.add(item);
        }

        return Map.of(
                "totalQuarantinedOrBlockedHorses", result.size(),
                "horses", result
        );
    }

    private Object handleGetRecentIncidentReports() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(48);
        List<GroomIncidentReport> reports = groomIncidentReportRepository.findByReportedAtAfterOrderByReportedAtDesc(cutoff);

        List<Map<String, Object>> result = new ArrayList<>();
        for (GroomIncidentReport r : reports) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("incidentId", r.getId());
            item.put("horseId", r.getHorseId());
            item.put("groomId", r.getGroomId());
            item.put("title", r.getTitle());
            item.put("description", r.getDescription());
            item.put("severity", r.getSeverity());
            item.put("status", r.getStatus());
            item.put("reportedAt", r.getReportedAt());
            result.add(item);
        }
        return result;
    }

    // =================================================================
    // GROOM Handlers
    // =================================================================

    private Object handleGetDailyGroomTasks(AuthenticatedUser currentUser) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
        return groomDailyTaskRepository.findByScheduledTimeBetween(startOfDay, endOfDay);
    }

    private Object handleGetMyAssignedHorses(AuthenticatedUser currentUser) {
        Long groomId = currentUser.getUserId();
        List<StableStall> stalls = stableStallRepository.findByGroomId(groomId);
        if (stalls.isEmpty()) {
            return Map.of(
                    "groomId", groomId,
                    "message", "Hiện tại bạn chưa được phân công phụ trách chuồng nào.",
                    "assignedHorses", Collections.emptyList()
            );
        }

        List<Long> stallIds = stalls.stream().map(StableStall::getId).toList();
        List<Horse> horses = horseRepository.findByCurrentStallIdIn(stallIds);
        Map<Long, Horse> horseByStall = horses.stream()
                .filter(h -> h.getCurrentStallId() != null)
                .collect(Collectors.toMap(Horse::getCurrentStallId, h -> h, (a, b) -> a));

        List<Map<String, Object>> horseList = new ArrayList<>();
        for (StableStall stall : stalls) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("stallId", stall.getId());
            item.put("stallCode", stall.getStallCode());
            item.put("stallNumber", stall.getStallNumber());
            item.put("areaId", stall.getAreaId());
            item.put("stallStatus", stall.getStatus());

            Horse h = horseByStall.get(stall.getId());
            if (h != null) {
                item.put("horseId", h.getId());
                item.put("horseName", h.getName());
                item.put("registrationNumber", h.getRegistrationNumber());
                item.put("currentStatus", h.getCurrentStatus());
                item.put("trainingDecision", h.getTrainingDecision());
            } else {
                item.put("horse", "Chuồng đang trống");
            }
            horseList.add(item);
        }

        return Map.of(
                "groomId", groomId,
                "totalAssignedStalls", stalls.size(),
                "occupiedStalls", horses.size(),
                "assignedHorses", horseList
        );
    }

    private Object handleGetTodayAggregatedTasks(Map<String, Object> args, AuthenticatedUser currentUser) {
        LocalDate targetDate = LocalDate.now();
        if (args.containsKey("date") && args.get("date") != null) {
            try {
                targetDate = LocalDate.parse(args.get("date").toString().trim());
            } catch (Exception e) {
                log.warn("Ngày không hợp lệ [{}], dùng ngày hiện tại", args.get("date"));
            }
        }
        return groomDailyTaskService.getTodayAggregatedTasks(currentUser.getUserId(), targetDate, currentUser);
    }

    private Object handleGetHorseDietaryAndCareAlerts(Map<String, Object> args) {
        Long horseId = parseLong(args.get("horseId"));
        if (horseId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Thiếu horseId");
        }
        Horse horse = horseRepository.findById(horseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Không tìm thấy ngựa #" + horseId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseId", horse.getId());
        result.put("horseName", horse.getName());
        result.put("currentStatus", horse.getCurrentStatus());
        result.put("trainingDecision", horse.getTrainingDecision());
        result.put("trainingDecisionReason", horse.getTrainingDecisionReason() != null ? horse.getTrainingDecisionReason() : "Không có hạn chế đặc biệt");

        List<HealthRecord> records = healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(horseId);
        if (!records.isEmpty()) {
            HealthRecord latest = records.get(0);
            result.put("latestDiagnosis", latest.getDiagnosis());
            result.put("latestTreatment", latest.getTreatment());
            result.put("restrictionDetails", latest.getRestrictionDetails());
            result.put("lastExaminedAt", latest.getExaminedAt());
        }

        List<CareSchedule> activeSchedules = careScheduleRepository.findByHorseIdAndStatusIn(
                horseId, List.of(CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS)
        );
        List<Map<String, Object>> careList = activeSchedules.stream().map(cs -> Map.<String, Object>of(
                "scheduleId", cs.getId(),
                "careType", cs.getCareType(),
                "scheduledAt", cs.getScheduledAt(),
                "purpose", cs.getPurpose() != null ? cs.getPurpose().name() : "N/A"
        )).toList();
        result.put("upcomingCareSchedules", careList);

        return result;
    }

    private Object handleCheckStallSuppliesStatus(Map<String, Object> args) {
        Long stallId = parseLong(args.get("stallId"));
        if (stallId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Thiếu stallId");
        }
        StableStall stall = stableStallRepository.findById(stallId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STALL_NOT_FOUND", "Không tìm thấy chuồng #" + stallId));

        Optional<Horse> horseOpt = horseRepository.findByCurrentStallId(stallId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stallId", stall.getId());
        result.put("stallCode", stall.getStallCode());
        result.put("stallNumber", stall.getStallNumber());
        result.put("areaId", stall.getAreaId());
        result.put("status", stall.getStatus());
        result.put("assignedGroomId", stall.getGroomId() != null ? stall.getGroomId() : "Chưa phân công");
        result.put("isOccupied", horseOpt.isPresent());

        horseOpt.ifPresent(h -> {
            result.put("horseId", h.getId());
            result.put("horseName", h.getName());
            result.put("registrationNumber", h.getRegistrationNumber());
        });

        return result;
    }

    private Object handleGetHorseStallLocation(Map<String, Object> args) {
        Long horseId = parseLong(args.get("horseId"));
        if (horseId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Thiếu horseId");
        }
        Horse horse = horseRepository.findById(horseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Không tìm thấy ngựa #" + horseId));
        return Map.of(
                "horseId", horse.getId(),
                "name", horse.getName(),
                "currentStallId", horse.getCurrentStallId() != null ? horse.getCurrentStallId() : "Chưa xếp chuồng"
        );
    }

    // =================================================================
    // CLUB_MANAGER Handlers
    // =================================================================

    private Object handleGetStableOccupancyMetrics() {
        long totalStalls = stableStallRepository.count();
        long availableQuarantine = stableStallRepository.countAvailableQuarantineStalls();
        long occupiedQuarantine = stableStallRepository.countOccupiedQuarantineStalls();
        long availableRegular = stableStallRepository.countAvailableRegularStalls();
        return Map.of(
                "totalStalls", totalStalls,
                "availableQuarantineStalls", availableQuarantine,
                "occupiedQuarantineStalls", occupiedQuarantine,
                "availableRegularStalls", availableRegular
        );
    }

    private Object handleGetAdmissionRequestsOverview() {
        int pendingCount = admissionApplicationRepository.findByStatus(AdmissionStatus.MANAGER_REVIEW).size();
        return Map.of(
                "pendingAdmissionsCount", pendingCount,
                "status", "ACTIVE"
        );
    }

    private Object handleGetTrainerAreaDistribution() {
        List<Area> areas = areaRepository.findAll();
        List<StableStall> allStalls = stableStallRepository.findAll();

        Map<Long, List<StableStall>> stallsByArea = allStalls.stream()
                .collect(Collectors.groupingBy(StableStall::getAreaId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Area area : areas) {
            List<StableStall> stalls = stallsByArea.getOrDefault(area.getId(), Collections.emptyList());
            Set<Long> groomIds = stalls.stream()
                    .map(StableStall::getGroomId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("areaId", area.getId());
            map.put("areaCode", area.getCode());
            map.put("areaName", area.getName());
            map.put("areaType", area.getType());
            map.put("trainerId", area.getTrainerId() != null ? area.getTrainerId() : "Chưa gán HLV");
            map.put("totalStalls", stalls.size());
            map.put("assignedGroomsCount", groomIds.size());
            map.put("assignedGroomIds", groomIds);
            result.add(map);
        }
        return result;
    }

    private Object handleGetClubHealthOverview() {
        List<Horse> horses = horseRepository.findAll();
        long allowedCount = horses.stream().filter(h -> h.getTrainingDecision() == TrainingDecision.ALLOWED).count();
        long blockedCount = horses.stream().filter(h -> h.getTrainingDecision() == TrainingDecision.BLOCKED).count();

        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<GroomIncidentReport> weekIncidents = groomIncidentReportRepository.findByReportedAtAfterOrderByReportedAtDesc(weekAgo);

        return Map.of(
                "totalHorses", horses.size(),
                "allowedHorses", allowedCount,
                "blockedHorses", blockedCount,
                "weeklyIncidentsCount", weekIncidents.size()
        );
    }

    private Object handleGetUpcomingRaces() {
        return Map.of("message", "Lịch trình tập luyện và giải đua ngựa sắp diễn ra đang được cập nhật.");
    }

    // =================================================================
    // Helper Methods
    // =================================================================

    private void verifyHorseOwnershipIfOwner(Long horseId, AuthenticatedUser currentUser) {
        if ("HORSE_OWNER".equalsIgnoreCase(currentUser.getRole())) {
            Horse horse = horseRepository.findById(horseId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Không tìm thấy ngựa #" + horseId));
            if (!Objects.equals(horse.getOwnerId(), currentUser.getUserId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN_HORSE", "Bạn không sở hữu con ngựa này.");
            }
        }
    }

    private Long parseLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number num) return num.longValue();
        try {
            return Long.valueOf(obj.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseDate(Object obj, LocalDate defaultDate) {
        if (obj == null || obj.toString().isBlank()) return defaultDate;
        try {
            return LocalDate.parse(obj.toString().trim());
        } catch (Exception e) {
            return defaultDate;
        }
    }
}
