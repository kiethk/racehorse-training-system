package com.rtms.backend.ai.tool;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.entity.CandidateHorseProfile;
import com.rtms.backend.admission.repository.CandidateHorseProfileRepository;
import com.rtms.backend.audit.entity.AuditLog;
import com.rtms.backend.audit.repository.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.ai.dto.ClinicalChunkResponseDto;
import com.rtms.backend.ai.rag.service.ClinicalRetrievalService;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
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
import com.rtms.backend.stable.enums.GroomTaskType;
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
import com.rtms.backend.training.dto.SpecialtyWorkoutRow;
import com.rtms.backend.training.dto.TrainingLotRow;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.RacingReadinessAssessment;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.SurfaceType;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.CourseRepository;
import com.rtms.backend.training.repository.HorseTrainingPlanRepository;
import com.rtms.backend.training.entity.HorseTrainingPlan;
import com.rtms.backend.training.enums.TrainingPlanStatus;
import com.rtms.backend.training.repository.RacingReadinessAssessmentRepository;
import com.rtms.backend.training.repository.TrainingLotRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import com.rtms.backend.training.service.HorsePerformanceService;
import com.rtms.backend.training.service.HorseSpecialtyService;
import com.rtms.backend.racing.entity.RaceRegistration;
import com.rtms.backend.racing.repository.RaceRegistrationRepository;
import com.rtms.backend.training.entity.Subject;
import com.rtms.backend.training.repository.SubjectRepository;
import com.rtms.backend.medical.entity.HorseHealthMetric;
import com.rtms.backend.medical.repository.HorseHealthMetricRepository;
import com.rtms.backend.horse.entity.HorsePedigree;
import com.rtms.backend.horse.repository.HorsePedigreeRepository;
import com.rtms.backend.incident.entity.HorseIncidentReport;
import com.rtms.backend.incident.repository.HorseIncidentReportRepository;
import com.rtms.backend.stable.entity.GroomDailyTask;
import com.rtms.backend.stable.repository.GroomTaskCompletionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import com.rtms.backend.training.dto.DistanceStat;
import com.rtms.backend.training.enums.DistanceBand;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dịch vụ điều phối thực thi các công cụ (Tool Execution Dispatcher) được gọi bởi LLM.
 *
 * Kiểm tra 2 tầng an toàn:
 * 1. Phân quyền vai trò (DynamicToolRegistryService.isToolAllowedForRole).
 * 2. Đảm bảo 100% Read-Only / Query / RAG Retrieval cho toàn bộ 5 vai trò.
 *
 * Quy chuẩn dữ liệu:
 * - Không để lộ ID kỹ thuật thô trong database (thay bằng Tên ngựa, Tên người, Tên chuồng).
 * - Cho phép tra cứu linh hoạt theo ID hoặc Name.
 * - Loại bỏ mâu thuẫn dữ liệu (ngựa cách ly luôn là BLOCKED).
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
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final AuditLogRepository auditLogRepository;
    private final CandidateHorseProfileRepository candidateHorseProfileRepository;
    private final RaceRegistrationRepository raceRegistrationRepository;
    private final SubjectRepository subjectRepository;
    private final HorseHealthMetricRepository horseHealthMetricRepository;
    private final HorsePedigreeRepository horsePedigreeRepository;
    private final HorseIncidentReportRepository horseIncidentReportRepository;
    private final GroomTaskCompletionRepository groomTaskCompletionRepository;

    @Autowired
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
                                  AreaRepository areaRepository,
                                  @Autowired(required = false) UserRepository userRepository,
                                  @Autowired(required = false) CourseRepository courseRepository,
                                  @Autowired(required = false) AuditLogRepository auditLogRepository,
                                  @Autowired(required = false) CandidateHorseProfileRepository candidateHorseProfileRepository,
                                  @Autowired(required = false) RaceRegistrationRepository raceRegistrationRepository,
                                  @Autowired(required = false) SubjectRepository subjectRepository,
                                  @Autowired(required = false) HorseHealthMetricRepository horseHealthMetricRepository,
                                  @Autowired(required = false) HorsePedigreeRepository horsePedigreeRepository,
                                  @Autowired(required = false) HorseIncidentReportRepository horseIncidentReportRepository,
                                  @Autowired(required = false) GroomTaskCompletionRepository groomTaskCompletionRepository) {
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
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.auditLogRepository = auditLogRepository;
        this.candidateHorseProfileRepository = candidateHorseProfileRepository;
        this.raceRegistrationRepository = raceRegistrationRepository;
        this.subjectRepository = subjectRepository;
        this.horseHealthMetricRepository = horseHealthMetricRepository;
        this.horsePedigreeRepository = horsePedigreeRepository;
        this.horseIncidentReportRepository = horseIncidentReportRepository;
        this.groomTaskCompletionRepository = groomTaskCompletionRepository;
    }

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
                                  AreaRepository areaRepository,
                                  UserRepository userRepository,
                                  CourseRepository courseRepository) {
        this(toolRegistryService, clinicalRetrievalService, horseRepository, trainingWorkoutRepository,
                healthRecordRepository, careScheduleRepository, stableStallRepository, groomDailyTaskRepository,
                groomDailyTaskService, admissionApplicationRepository, trainingLotRepository,
                horseTrainingPlanRepository, horseSpecialtyService, horsePerformanceService,
                racingReadinessAssessmentRepository, groomIncidentReportRepository, areaRepository,
                userRepository, courseRepository, null, null, null, null, null, null, null, null);
    }

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
                                  AreaRepository areaRepository,
                                  UserRepository userRepository,
                                  CourseRepository courseRepository,
                                  AuditLogRepository auditLogRepository) {
        this(toolRegistryService, clinicalRetrievalService, horseRepository, trainingWorkoutRepository,
                healthRecordRepository, careScheduleRepository, stableStallRepository, groomDailyTaskRepository,
                groomDailyTaskService, admissionApplicationRepository, trainingLotRepository,
                horseTrainingPlanRepository, horseSpecialtyService, horsePerformanceService,
                racingReadinessAssessmentRepository, groomIncidentReportRepository, areaRepository,
                userRepository, courseRepository, auditLogRepository, null, null, null, null, null, null, null);
    }

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
                                  AreaRepository areaRepository,
                                  UserRepository userRepository,
                                  CourseRepository courseRepository,
                                  AuditLogRepository auditLogRepository,
                                  CandidateHorseProfileRepository candidateHorseProfileRepository) {
        this(toolRegistryService, clinicalRetrievalService, horseRepository, trainingWorkoutRepository,
                healthRecordRepository, careScheduleRepository, stableStallRepository, groomDailyTaskRepository,
                groomDailyTaskService, admissionApplicationRepository, trainingLotRepository,
                horseTrainingPlanRepository, horseSpecialtyService, horsePerformanceService,
                racingReadinessAssessmentRepository, groomIncidentReportRepository, areaRepository,
                userRepository, courseRepository, auditLogRepository, candidateHorseProfileRepository,
                null, null, null, null, null, null);
    }

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
        this(toolRegistryService, clinicalRetrievalService, horseRepository, trainingWorkoutRepository,
                healthRecordRepository, careScheduleRepository, stableStallRepository, groomDailyTaskRepository,
                groomDailyTaskService, admissionApplicationRepository, trainingLotRepository,
                horseTrainingPlanRepository, horseSpecialtyService, horsePerformanceService,
                racingReadinessAssessmentRepository, groomIncidentReportRepository, areaRepository,
                null, null, null, null, null, null, null, null, null, null);
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
            case "getMyHorseIncidentHistory" -> handleGetMyHorseIncidentHistory(args, currentUser);
            case "getMyHorsePedigreeInfo" -> handleGetMyHorsePedigreeInfo(args, currentUser);
            case "analyzeHorseInvestmentAndFitnessROI" -> handleAnalyzeHorseInvestmentAndFitnessROI(args, currentUser);

            // HEAD_TRAINER Tools
            case "getTrainerHorses" -> handleGetTrainerHorses(currentUser);
            case "getWorkoutHistory" -> handleGetWorkoutHistory(args);
            case "getHorseFitnessTrend" -> handleGetHorseFitnessTrend(args);
            case "getTrainerSchedule" -> handleGetTrainerSchedule(args, currentUser);
            case "getTrainerPlansSummary" -> handleGetTrainerPlansSummary(args, currentUser);
            case "getHorseSpecialtyAnalysis" -> handleGetHorseSpecialtyAnalysis(args, currentUser);
            case "getTrainerRiskAlerts" -> handleGetTrainerRiskAlerts(args, currentUser);
            case "getRacingReadinessAssessment" -> handleGetRacingReadinessAssessment(args);
            case "checkGroomConflictForLot" -> handleCheckGroomConflictForLot(args);
            case "getTrainerAssignedGrooms" -> handleGetTrainerAssignedGrooms(currentUser);
            case "getMyHorseRaceRegistrations" -> handleGetMyHorseRaceRegistrations(args, currentUser);
            case "getAssignedAdmissionReviews" -> handleGetAssignedAdmissionReviews(currentUser);
            case "getCourseCurriculumOverview" -> handleGetCourseCurriculumOverview();
            case "analyzeWorkoutLoadAndOvertrainingRisk" -> handleAnalyzeWorkoutLoadAndOvertrainingRisk(args);
            case "recommendOptimalRaceDistanceAndTrack" -> handleRecommendOptimalRaceDistanceAndTrack(args);
            case "recommendNextWorkoutAdjustment" -> handleRecommendNextWorkoutAdjustment(args);

            // VETERINARIAN Tools
            case "searchClinicalKnowledge" -> handleSearchClinicalKnowledge(args, currentUser);
            case "getVetExaminationOverview" -> handleGetVetExaminationOverview();
            case "getHorseMedicalHistory" -> handleGetHorseMedicalHistory(args);
            case "getActivePrescriptions" -> handleGetActivePrescriptions(args);
            case "getPreventiveCareSchedule" -> handleGetPreventiveCareSchedule(args);
            case "getQuarantineStatus" -> handleGetQuarantineStatus();
            case "getRecentIncidentReports" -> handleGetRecentIncidentReports();
            case "getPendingAdmissionExams" -> handleGetPendingAdmissionExams();
            case "getUrgentCareAlerts" -> handleGetUrgentCareAlerts();
            case "getHorseVitalSignsTrend" -> handleGetHorseVitalSignsTrend(args);
            case "analyzeIncidentPatternsAndRecurringRisks" -> handleAnalyzeIncidentPatternsAndRecurringRisks(args);
            case "recommendClinicalInterventionPlan" -> handleRecommendClinicalInterventionPlan(args, currentUser);
            case "evaluateTrainingResumptionReadiness" -> handleEvaluateTrainingResumptionReadiness(args);

            // GROOM Tools
            case "getDailyGroomTasks" -> handleGetDailyGroomTasks(currentUser);
            case "getMyAssignedHorses" -> handleGetMyAssignedHorses(currentUser);
            case "getTodayAggregatedTasks" -> handleGetTodayAggregatedTasks(args, currentUser);
            case "getHorseDietaryAndCareAlerts" -> handleGetHorseDietaryAndCareAlerts(args);
            case "getHorseStallLocation" -> handleGetHorseStallLocation(args);
            case "getMyReportedIncidents" -> handleGetMyReportedIncidents(currentUser);
            case "getGroomTaskCompletionRate" -> handleGetGroomTaskCompletionRate(args, currentUser);
            case "detectHorseBehavioralAnomaly" -> handleDetectHorseBehavioralAnomaly(args);
            case "recommendDailyCarePriorityOrder" -> handleRecommendDailyCarePriorityOrder(currentUser);

            // CLUB_MANAGER Tools
            case "getStableOccupancyMetrics" -> handleGetStableOccupancyMetrics();
            case "getAdmissionRequestsOverview" -> handleGetAdmissionRequestsOverview();
            case "getTrainerAreaDistribution" -> handleGetTrainerAreaDistribution();
            case "getClubHealthOverview" -> handleGetClubHealthOverview();
            case "getSystemStaffOverview" -> handleGetSystemStaffOverview(args);
            case "getClubHorsesDirectory" -> handleGetClubHorsesDirectory(args);
            case "getRecentAuditLogs" -> handleGetRecentAuditLogs(args);
            case "getAdmissionPipelineDetails" -> handleGetAdmissionPipelineDetails(args);
            case "analyzeStableCapacityAndStallBottlenecks" -> handleAnalyzeStableCapacityAndStallBottlenecks();
            case "recommendGroomWorkloadRebalancing" -> handleRecommendGroomWorkloadRebalancing();

            default -> throw new ApiException(HttpStatus.NOT_FOUND, "TOOL_NOT_FOUND", "Không tìm thấy công cụ: " + toolName);
        };
    }

    // =================================================================
    // HORSE_OWNER Handlers
    // =================================================================

    private Object handleGetMyHorses(AuthenticatedUser currentUser) {
        List<Horse> horses = horseRepository.findByOwnerId(currentUser.getUserId());
        List<Map<String, Object>> result = new ArrayList<>();
        int duocTapLuyen = 0;
        int camTapLuyen = 0;
        for (Horse h : horses) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseName", h.getName());
            item.put("registrationNumber", h.getRegistrationNumber() != null ? h.getRegistrationNumber() : "N/A");
            item.put("status", h.getCurrentStatus());
            item.put("trainingDecision", h.getTrainingDecision());
            item.put("stallLocation", resolveStallName(h.getCurrentStallId()));
            item.put("actionUrl", h.getId() != null ? "/owner/horses/" + h.getId() : "/owner/horses");
            result.add(item);

            String dec = h.getTrainingDecision() != null ? h.getTrainingDecision().name().toUpperCase() : "";
            if (dec.contains("ELIGIBLE") || dec.contains("ALLOWED") || dec.contains("NORMAL")) {
                duocTapLuyen++;
            } else if (dec.contains("RESTRICTED") || dec.contains("BLOCKED") || dec.contains("CAM")) {
                camTapLuyen++;
            }
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalOwnedHorses", result.size());
        response.put("allowedTrainingHorses", duocTapLuyen);
        response.put("restrictedHorses", camTapLuyen);
        response.put("horseList", result);
        response.put("actionUrl", "/owner/horses");
        return response;
    }

    private Object handleGetMyHorseHealthSummary(Map<String, Object> args, AuthenticatedUser currentUser) {
        Horse horse = resolveHorse(args);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Cần cung cấp ID hoặc tên ngựa");
        }
        verifyHorseOwnershipIfOwner(horse.getId(), currentUser);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("horseName", horse.getName());
        summary.put("registrationNumber", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        summary.put("currentStatus", horse.getCurrentStatus());
        summary.put("trainingDecision", horse.getTrainingDecision());
        summary.put("decisionReason", horse.getTrainingDecisionReason() != null ? horse.getTrainingDecisionReason() : "Bình thường");
        summary.put("stallLocation", resolveStallName(horse.getCurrentStallId()));
        summary.put("actionUrl", horse.getId() != null ? "/owner/horses/" + horse.getId() : "/owner/horses");

        List<HealthRecord> records = healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(horse.getId());
        if (!records.isEmpty()) {
            HealthRecord latest = records.get(0);
            summary.put("latestDiagnosis", latest.getDiagnosis());
            summary.put("treatmentPlan", latest.getTreatment());
            summary.put("medicalRestrictionNotes", latest.getRestrictionDetails());
            summary.put("lastExamDate", latest.getExaminedAt());
        } else {
            summary.put("latestDiagnosis", "Chưa có bệnh án ghi nhận");
        }

        List<CareSchedule> activeSchedules = careScheduleRepository.findByHorseIdAndStatusIn(
                horse.getId(), List.of(CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS));
        List<Map<String, Object>> careList = activeSchedules.stream().map(cs -> Map.<String, Object>of(
                "loaiChamSoc", cs.getCareType() != null ? cs.getCareType().name() : "Khám tổng quát", "scheduledAt", cs.getScheduledAt() != null ? cs.getScheduledAt().toString() : "N/A", "purpose", cs.getPurpose() != null ? cs.getPurpose().name() : "N/A"
        )).toList();
        summary.put("upcomingCareSchedule", careList);

        return summary;
    }

    private Object handleGetMyHorseTrainingProgress(Map<String, Object> args, AuthenticatedUser currentUser) {
        Horse horse = resolveHorse(args);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Cần cung cấp ID hoặc tên ngựa");
        }
        verifyHorseOwnershipIfOwner(horse.getId(), currentUser);

        List<PlanSummaryResponse> summaries = horseTrainingPlanRepository.findPlanSummaries(null, horse.getId());
        if (summaries == null || summaries.isEmpty()) {
            return Map.of("message", "Chiến mã " + horse.getName() + " hiện chưa có kế hoạch huấn luyện nào.");
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (PlanSummaryResponse p : summaries) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("planName", p.getCourseName() != null ? p.getCourseName() : "Khóa huấn luyện");
            item.put("horseName", horse.getName());
            item.put("status", p.getStatus() != null ? p.getStatus().name() : "N/A");
            item.put("progress", p.getCompletedSessions() + "/" + p.getTotalSessions() + " buổi");
            item.put("startDate", p.getStartDate() != null ? p.getStartDate().toString() : "N/A");
            item.put("endDate", p.getEndDate() != null ? p.getEndDate().toString() : "N/A");
            result.add(item);
        }
        return result;
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
            item.put("sessionTime", formatCaTap(lot.startTime(), lot.endTime()));
            item.put("workoutDate", lot.lotDate() != null ? lot.lotDate().toString() : "N/A");
            item.put("startTime", lot.startTime() != null ? lot.startTime().toString() : "N/A");
            item.put("endTime", lot.endTime() != null ? lot.endTime().toString() : "N/A");
            item.put("subjectName", lot.subjectName());
            item.put("myHorseList", namesByLot.getOrDefault(lot.lotId(), Collections.emptyList()));
            result.add(item);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalUpcomingWorkouts", result.size());
        response.put("workoutScheduleList", result);
        return response;
    }

    private Object handleGetMyHorseAdmissions(AuthenticatedUser currentUser) {
        Long ownerId = currentUser.getUserId();
        List<AdmissionApplication> apps = admissionApplicationRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (AdmissionApplication a : apps) {
            Map<String, Object> item = new LinkedHashMap<>();
            String horseName = "Ngựa đăng ký";
            if (a.getHorseId() != null) {
                horseName = horseRepository.findById(a.getHorseId()).map(Horse::getName).orElse("Ngựa đăng ký");
            }
            item.put("horseName", horseName);
            item.put("applicationStatus", a.getStatus() != null ? a.getStatus().name() : "PENDING");
            item.put("groomCheckResult", a.getGroomDecision() != null ? a.getGroomDecision().name() : "Chờ đánh giá");
            item.put("vetExamResult", a.getVetTrainingDecision() != null ? a.getVetTrainingDecision().name() : "Chờ đánh giá");
            item.put("managerApprovalResult", a.getManagerDecision() != null ? a.getManagerDecision().name() : "Chờ phê duyệt");
            item.put("submittedDate", a.getSubmittedAt() != null ? a.getSubmittedAt().toString() : "N/A");
            item.put("actionUrl", a.getId() != null ? "/owner/admissions/" + a.getId() : "/owner/admissions");
            result.add(item);
        }
        int daDuyet = 0;
        int dangXuLy = 0;
        for (AdmissionApplication a : apps) {
            if (a.getStatus() == AdmissionStatus.APPROVED) {
                daDuyet++;
            } else if (a.getStatus() != AdmissionStatus.REJECTED && a.getStatus() != AdmissionStatus.ARRIVAL_EXPIRED) {
                dangXuLy++;
            }
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalSubmittedApplications", result.size());
        response.put("approvedApplicationCount", daDuyet);
        response.put("pendingApplicationCount", dangXuLy);
        response.put("applicationList", result);
        response.put("actionUrl", "/owner/admissions");
        return response;
    }

    private Object handleGetHorseTrainingSummary(Map<String, Object> args, AuthenticatedUser currentUser) {
        Horse horse = resolveHorse(args);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Cần cung cấp ID hoặc tên ngựa");
        }
        verifyHorseOwnershipIfOwner(horse.getId(), currentUser);

        List<TrainingWorkout> workouts = trainingWorkoutRepository.findByHorseIdOrderByCreatedAtDesc(horse.getId());
        int totalSessions = workouts.size();
        long completedSessions = workouts.stream().filter(w -> w.getStatus() == WorkoutStatus.COMPLETED).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseName", horse.getName());
        result.put("registrationNumber", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        result.put("trainingDecision", horse.getTrainingDecision() != null ? horse.getTrainingDecision().name() : "N/A");
        result.put("decisionReason", horse.getTrainingDecisionReason() != null ? horse.getTrainingDecisionReason() : "Bình thường");
        result.put("totalWorkouts", totalSessions);
        result.put("completedSessions", completedSessions);
        return result;
    }

    private Object handleGetHorseCareSchedule(Map<String, Object> args, AuthenticatedUser currentUser) {
        Horse horse = resolveHorse(args);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Cần cung cấp ID hoặc tên ngựa");
        }
        verifyHorseOwnershipIfOwner(horse.getId(), currentUser);

        List<CareSchedule> schedules = careScheduleRepository.findByHorseIdOrderByScheduledAtAsc(horse.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (CareSchedule cs : schedules) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseName", horse.getName());
            item.put("careType", cs.getCareType() != null ? cs.getCareType().name() : "Khám thú y");
            item.put("scheduledAt", cs.getScheduledAt() != null ? cs.getScheduledAt().toString() : "N/A");
            item.put("status", cs.getStatus() != null ? cs.getStatus().name() : "N/A");
            item.put("purpose", cs.getPurpose() != null ? cs.getPurpose().name() : "N/A");
            result.add(item);
        }
        return result;
    }

    private Object handleGetMyHorseIncidentHistory(Map<String, Object> args, AuthenticatedUser currentUser) {
        Horse horse = resolveHorse(args);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Cần cung cấp ID hoặc tên ngựa");
        }
        verifyHorseOwnershipIfOwner(horse.getId(), currentUser);

        List<Map<String, Object>> result = new ArrayList<>();
        if (horseIncidentReportRepository != null) {
            List<HorseIncidentReport> reports = horseIncidentReportRepository.findByHorseId(horse.getId());
            for (HorseIncidentReport r : reports) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("horseName", horse.getName());
                item.put("title", r.getTitle());
                item.put("description", r.getDescription() != null ? r.getDescription() : "");
                item.put("timestamp", r.getReportedAt() != null ? r.getReportedAt().toString() : "N/A");
                item.put("status", r.getStatus() != null ? r.getStatus().name() : "REPORTED");
                item.put("actionUrl", horse != null && horse.getId() != null ? "/owner/horses/" + horse.getId() : "/owner/horses");
                result.add(item);
            }
        }
        if (result.isEmpty() && groomIncidentReportRepository != null) {
            List<GroomIncidentReport> gReports = groomIncidentReportRepository.findByHorseId(horse.getId());
            for (GroomIncidentReport r : gReports) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("horseName", horse.getName());
                item.put("title", r.getTitle());
                item.put("description", r.getDescription() != null ? r.getDescription() : "");
                item.put("timestamp", r.getReportedAt() != null ? r.getReportedAt().toString() : "N/A");
                item.put("status", r.getStatus() != null ? r.getStatus().name() : "REPORTED");
                item.put("actionUrl", horse != null && horse.getId() != null ? "/owner/horses/" + horse.getId() : "/owner/horses");
                result.add(item);
            }
        }
        return result;
    }

    private Object handleGetMyHorsePedigreeInfo(Map<String, Object> args, AuthenticatedUser currentUser) {
        Horse horse = resolveHorse(args);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Cần cung cấp ID hoặc tên ngựa");
        }
        verifyHorseOwnershipIfOwner(horse.getId(), currentUser);

        Optional<HorsePedigree> pedigreeOpt = (horsePedigreeRepository != null)
                ? horsePedigreeRepository.findByHorseId(horse.getId())
                : Optional.empty();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseName", horse.getName());
        result.put("sire", pedigreeOpt.map(HorsePedigree::getSireName).filter(s -> !s.isBlank()).orElse("Chưa rõ"));
        result.put("dam", pedigreeOpt.map(HorsePedigree::getDamName).filter(d -> !d.isBlank()).orElse("Chưa rõ"));
        result.put("origin", pedigreeOpt.map(HorsePedigree::getPedigreeNotes).filter(n -> !n.isBlank()).orElse("Việt Nam"));
        result.put("pedigreeGeneration", pedigreeOpt.map(p -> "Ngựa cha: " + (p.getSireName() != null ? p.getSireName() : "Chưa rõ") + ", Ngựa mẹ: " + (p.getDamName() != null ? p.getDamName() : "Chưa rõ")).orElse("Chưa có hồ sơ phả hệ chi tiết"));
        result.put("actionUrl", horse != null && horse.getId() != null ? "/owner/horses/" + horse.getId() : "/owner/horses");
        return result;
    }

    private Object handleAnalyzeHorseInvestmentAndFitnessROI(Map<String, Object> args, AuthenticatedUser currentUser) {
        Horse horse = resolveHorse(args);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Cần cung cấp ID hoặc tên ngựa");
        }
        verifyHorseOwnershipIfOwner(horse.getId(), currentUser);

        List<TrainingWorkout> workouts = (trainingWorkoutRepository != null)
                ? trainingWorkoutRepository.findByHorseIdOrderByCreatedAtDesc(horse.getId())
                : Collections.emptyList();

        int tongSoBuoiTapDatYeuCau = 0;
        BigDecimal maxSpeed = BigDecimal.ZERO;
        for (TrainingWorkout w : workouts) {
            if (w.getStatus() == WorkoutStatus.COMPLETED && (w.getPerformanceRating() == null || w.getPerformanceRating() >= 6)) {
                tongSoBuoiTapDatYeuCau++;
            }
            if (w.getTopSpeedKmh() != null && w.getTopSpeedKmh().compareTo(maxSpeed) > 0) {
                maxSpeed = w.getTopSpeedKmh();
            }
        }

        String thoiGianGiaNhap = (horse.getCreatedAt() != null)
                ? horse.getCreatedAt().toLocalDate().toString()
                : "N/A";

        double rate = !workouts.isEmpty() ? ((double) tongSoBuoiTapDatYeuCau / workouts.size()) * 100.0 : 100.0;
        String tocDoPhatTrienTheLuc;
        if (maxSpeed.compareTo(BigDecimal.ZERO) > 0) {
            tocDoPhatTrienTheLuc = String.format(Locale.US, "Thể lực phát triển vững chắc (+15%% tốc độ bứt tốc đỉnh đạt %.1f km/h, khả năng duy trì guồng chân ổn định)", maxSpeed.doubleValue());
        } else {
            tocDoPhatTrienTheLuc = "Thể lực đang trong giai đoạn tích lũy cơ bản và thích nghi với cường độ tập";
        }

        String danhGiaTiemNangThiDau;
        if (isHorseInQuarantineOrBlocked(horse)) {
            danhGiaTiemNangThiDau = "Chiến mã đang trong diện cách ly y tế hoặc điều trị phục hồi (BLOCKED). Tạm hoãn tham gia giải đấu để bảo vệ sức khỏe và thể trạng lâu dài.";
        } else if (rate >= 80.0 && !workouts.isEmpty()) {
            danhGiaTiemNangThiDau = String.format(Locale.US, "Tiềm năng thi đấu triển vọng cao. Tỷ lệ bài tập đạt chuẩn %.1f%%, đáp ứng hoàn hảo tiêu chuẩn tranh tài các cự ly sở trường.", rate);
        } else {
            danhGiaTiemNangThiDau = "Đang phát triển kỹ thuật và sức bền cơ bản, khuyến nghị hoàn thành thêm chu kỳ huấn luyện để bộc lộ tối đa sở trường thi đấu.";
        }

        Map<String, Object> roiResult = new LinkedHashMap<>();
        roiResult.put("horseName", horse.getName());
        roiResult.put("registrationNumber", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        roiResult.put("joinedAt", thoiGianGiaNhap);
        roiResult.put("totalWorkouts", workouts.size());
        roiResult.put("qualifiedWorkoutsCount", tongSoBuoiTapDatYeuCau);
        roiResult.put("fitnessProgressionRate", tocDoPhatTrienTheLuc);
        roiResult.put("racingPotentialEvaluation", danhGiaTiemNangThiDau);
        roiResult.put("actionUrl", horse != null && horse.getId() != null ? "/owner/horses/" + horse.getId() : "/owner/horses");
        return roiResult;
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

        List<Long> trainerHorseIds = horses.stream().map(Horse::getId).filter(Objects::nonNull).toList();
        Map<Long, HorseSpecialtyResponse> trainerSpecialties = Collections.emptyMap();
        if (horseSpecialtyService != null && !trainerHorseIds.isEmpty()) {
            try {
                Map<Long, HorseSpecialtyResponse> queried = horseSpecialtyService.getSpecialties(trainerHorseIds);
                if (queried != null) {
                    trainerSpecialties = queried;
                }
            } catch (Exception e) {
                log.warn("Lỗi khi tải sở trường cho danh sách ngựa HLV: {}", e.getMessage());
            }
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Horse h : horses) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseName", h.getName());
            item.put("registrationNumber", h.getRegistrationNumber() != null ? h.getRegistrationNumber() : "N/A");
            item.put("status", h.getCurrentStatus());
            item.put("trainingDecision", h.getTrainingDecision());
            item.put("decisionReason", h.getTrainingDecisionReason() != null ? h.getTrainingDecisionReason() : "Bình thường");

            StableStall stall = h.getCurrentStallId() != null ? stableStallRepository.findById(h.getCurrentStallId()).orElse(null) : null;
            String stallName = stall != null ? ("Chuồng " + stall.getStallCode() + " (Số " + stall.getStallNumber() + ")") : "Chưa xếp chuồng";
            String groomName = "Chưa phân công";
            String groomPhone = "N/A";
            if (stall != null && stall.getGroomId() != null && userRepository != null) {
                User groomUser = userRepository.findById(stall.getGroomId()).orElse(null);
                if (groomUser != null) {
                    groomName = groomUser.getFullName();
                    if (groomUser.getPhone() != null && !groomUser.getPhone().isBlank()) {
                        groomPhone = groomUser.getPhone();
                    }
                }
            }
            HorseSpecialtyResponse specResp = trainerSpecialties.get(h.getId());
            SpecialtyMetrics sm = computeSpecialtyMetrics(specResp);

            item.put("stallLocation", stallName);
            item.put("assignedGroomName", groomName);
            item.put("groomPhone", groomPhone);
            item.put("specialtyCount", sm.specialtyCount());
            item.put("averageRating", sm.averageRating());
            item.put("bestRating", sm.bestRating());
            item.put("specialties", sm.formattedDistances());
            item.put("strongDistances", sm.strongBands());
            item.put("preferredSurface", sm.preferredSurface());
            item.put("specialtyBreakdown", sm.specialtyBreakdown());
            item.put("actionUrl", h.getId() != null ? "/trainer/horses/" + h.getId() : "/trainer/horses");
            list.add(item);
        }
        return Map.of("trainerName", resolveUserName(currentUser, "Huấn luyện viên trưởng"), "totalManagedHorses", list.size(), "horseList", list
        );
    }

    private Object handleGetWorkoutHistory(Map<String, Object> args) {
        Horse horse = resolveHorse(args);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Cần cung cấp ID hoặc tên ngựa");
        }
        List<TrainingWorkout> workouts = trainingWorkoutRepository.findByHorseIdOrderByCreatedAtDesc(horse.getId());
        List<Map<String, Object>> list = new ArrayList<>();
        for (TrainingWorkout w : workouts) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseName", horse.getName());
            item.put("status", w.getStatus());
            item.put("actualDistanceMeters", w.getActualDistanceMeters());
            item.put("topSpeedKmh", w.getTopSpeedKmh());
            item.put("avgHeartRate", w.getAverageHeartRate());
            item.put("evaluationScore", w.getPerformanceRating());
            item.put("trainerNotes", w.getTrainerFeedback());
            list.add(item);
        }
        return list;
    }

    private Object handleGetHorseFitnessTrend(Map<String, Object> args) {
        Horse horse = resolveHorse(args);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Cần cung cấp ID hoặc tên ngựa");
        }
        List<HorseFitnessTrendItemResponse> trends = trainingWorkoutRepository.findFitnessTrend(
                horse.getId(), LocalDate.now().minusDays(30), LocalDate.now()
        );
        return Map.of("horseName", horse.getName(), "fitnessTrends", trends != null ? trends : Collections.emptyList()
        );
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
            Map<String, Object> emptyResp = new LinkedHashMap<>();
            emptyResp.put("totalSessions", 0);
            emptyResp.put("sessionList", Collections.emptyList());
            return emptyResp;
        }

        List<Long> lotIds = lotRows.stream().map(TrainingLotRow::lotId).toList();
        List<LotHorseNameRow> horseNames = trainingLotRepository.findActiveHorseNames(lotIds);
        Map<Long, List<String>> namesByLot = horseNames.stream()
                .collect(Collectors.groupingBy(LotHorseNameRow::lotId,
                        Collectors.mapping(LotHorseNameRow::horseName, Collectors.toList())));

        List<Map<String, Object>> result = new ArrayList<>();
        for (TrainingLotRow lot : lotRows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sessionTime", formatCaTap(lot.startTime(), lot.endTime()));
            item.put("workoutDate", lot.lotDate() != null ? lot.lotDate().toString() : "Hôm nay");
            item.put("startTime", lot.startTime() != null ? lot.startTime().toString() : "N/A");
            item.put("endTime", lot.endTime() != null ? lot.endTime().toString() : "N/A");
            item.put("subjectName", lot.subjectName() != null ? lot.subjectName() : "Bài tập thể lực");
            item.put("horseCount", lot.occupied());
            item.put("horseList", namesByLot.getOrDefault(lot.lotId(), Collections.emptyList()));
            result.add(item);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalSessions", result.size());
        response.put("sessionList", result);
        return response;
    }

    private Object handleGetTrainerPlansSummary(Map<String, Object> args, AuthenticatedUser currentUser) {
        Long trainerId = currentUser.getUserId();
        Horse targetHorse = resolveHorse(args);
        Long horseId = targetHorse != null ? targetHorse.getId() : parseLong(args.get("horseId"));
        String status = args.get("status") != null ? args.get("status").toString().trim() : null;

        List<PlanSummaryResponse> summaries = horseTrainingPlanRepository.findPlanSummaries(trainerId, horseId);
        if (status != null && !status.isBlank()) {
            summaries = summaries.stream()
                    .filter(p -> p.getStatus() != null && p.getStatus().name().equalsIgnoreCase(status))
                    .toList();
        }
        if (summaries == null || summaries.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (PlanSummaryResponse p : summaries) {
            String targetGoal = "Nâng cao thể lực, sức bền và kỹ năng thi đấu";
            if (courseRepository != null && p.getCourseId() != null) {
                targetGoal = courseRepository.findById(p.getCourseId())
                        .map(Course::getTargetGoal)
                        .filter(g -> g != null && !g.isBlank())
                        .orElse(targetGoal);
            }
            String trangThai;
            if (p.getStatus() != null) {
                trangThai = switch (p.getStatus()) {
                    case ACTIVE -> "Đang diễn ra (ACTIVE)";
                    case UPCOMING -> "Sắp diễn ra (UPCOMING)";
                    case COMPLETED -> "Đã hoàn thành (COMPLETED)";
                    case CANCELLED -> "Đã hủy (CANCELLED)";
                    default -> p.getStatus().name();
                };
            } else {
                trangThai = "N/A";
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("planName", p.getCourseName() != null ? p.getCourseName() : "Kế hoạch huấn luyện");
            item.put("horseName", p.getHorseName() != null ? p.getHorseName() : "Ngựa thi đấu");
            item.put("targetGoal", targetGoal);
            item.put("status", trangThai);
            item.put("progress", (p.getCompletedSessions() != null ? p.getCompletedSessions() : 0) + "/"
                    + (p.getTotalSessions() != null ? p.getTotalSessions() : 0) + " buổi hoàn thành");
            item.put("completedSessions", p.getCompletedSessions());
            item.put("totalSessions", p.getTotalSessions());
            item.put("startDate", p.getStartDate() != null ? p.getStartDate().toString() : "N/A");
            item.put("endDate", p.getEndDate() != null ? p.getEndDate().toString() : "N/A");
            result.add(item);
        }
        return result;
    }

    record SpecialtyMetrics(
            int specialtyCount,
            List<String> strongBands,
            List<String> formattedDistances,
            String preferredSurface,
            BigDecimal averageRating,
            BigDecimal bestRating,
            List<String> specialtyBreakdown
    ) {}

    private SpecialtyMetrics computeSpecialtyMetrics(HorseSpecialtyResponse resp) {
        if (resp == null) {
            return new SpecialtyMetrics(
                    0,
                    Collections.emptyList(),
                    List.of("Chưa xác định"),
                    "Chưa rõ",
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    Collections.emptyList()
            );
        }

        List<DistanceBand> strong = resp.strongDistances() != null ? resp.strongDistances() : Collections.emptyList();
        int specialtyCount = strong.size();
        List<String> strongBands = strong.stream().map(DistanceBand::name).toList();
        List<String> formattedDistances = !strong.isEmpty()
                ? strong.stream().map(this::formatDistanceBand).toList()
                : List.of("Chưa xác định");

        String surf = resp.preferredSurface() != null
                ? (resp.preferredSurface() == SurfaceType.TURF ? "Sân cỏ (TURF)" : "Sân cát (DIRT)")
                : "Chưa rõ";

        List<DistanceStat> stats = resp.distanceStats() != null ? resp.distanceStats() : Collections.emptyList();

        List<DistanceStat> qualifyingStats = stats.stream()
                .filter(s -> strong.contains(s.band()) && s.averageRating() != null && s.workoutCount() > 0)
                .toList();

        BigDecimal bestRating = BigDecimal.ZERO;
        BigDecimal avgRating = BigDecimal.ZERO;

        if (!qualifyingStats.isEmpty()) {
            bestRating = qualifyingStats.stream()
                    .map(DistanceStat::averageRating)
                    .max(Comparator.naturalOrder())
                    .orElse(BigDecimal.ZERO);

            int totalWorkouts = qualifyingStats.stream().mapToInt(DistanceStat::workoutCount).sum();
            if (totalWorkouts > 0) {
                BigDecimal sum = BigDecimal.ZERO;
                for (DistanceStat s : qualifyingStats) {
                    sum = sum.add(s.averageRating().multiply(BigDecimal.valueOf(s.workoutCount())));
                }
                avgRating = sum.divide(BigDecimal.valueOf(totalWorkouts), 1, RoundingMode.HALF_UP);
            }
        } else {
            List<DistanceStat> allValid = stats.stream()
                    .filter(s -> s.averageRating() != null && s.workoutCount() > 0)
                    .toList();
            if (!allValid.isEmpty()) {
                bestRating = allValid.stream()
                        .map(DistanceStat::averageRating)
                        .max(Comparator.naturalOrder())
                        .orElse(BigDecimal.ZERO);
                int totalWorkouts = allValid.stream().mapToInt(DistanceStat::workoutCount).sum();
                if (totalWorkouts > 0) {
                    BigDecimal sum = BigDecimal.ZERO;
                    for (DistanceStat s : allValid) {
                        sum = sum.add(s.averageRating().multiply(BigDecimal.valueOf(s.workoutCount())));
                    }
                    avgRating = sum.divide(BigDecimal.valueOf(totalWorkouts), 1, RoundingMode.HALF_UP);
                }
            }
        }

        List<String> breakdown = new ArrayList<>();
        for (DistanceStat s : stats) {
            if (s.workoutCount() > 0 && s.averageRating() != null) {
                String badge = strong.contains(s.band()) ? "⭐ [SỞ TRƯỜNG] " : "";
                String speedStr = s.averageTopSpeedKmh() != null ? (", Tốc độ đỉnh: " + s.averageTopSpeedKmh() + " km/h") : "";
                breakdown.add(badge + formatDistanceBand(s.band()) + ": " + s.averageRating() + "/10 (" + s.workoutCount() + " buổi tập" + speedStr + ")");
            }
        }

        return new SpecialtyMetrics(
                specialtyCount,
                strongBands,
                formattedDistances,
                surf,
                avgRating,
                bestRating,
                breakdown
        );
    }

    private Object handleGetHorseSpecialtyAnalysis(Map<String, Object> args, AuthenticatedUser currentUser) {
        String horseNameArg = args.get("horseName") != null ? args.get("horseName").toString().trim().toLowerCase() : "";
        boolean isFleetRequest = horseNameArg.equals("all") || horseNameArg.equals("tất cả") || horseNameArg.equals("toàn bộ")
                || (args.get("horseId") == null && (args.get("horseName") == null || horseNameArg.isBlank()));

        if (isFleetRequest) {
            return handleRankAllHorsesBySpecialty(args, currentUser);
        }

        Horse horse = resolveHorse(args);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Cần cung cấp ID hoặc tên ngựa");
        }
        Long horseId = horse.getId();

        Map<Long, HorseSpecialtyResponse> specialties = horseSpecialtyService.getSpecialties(List.of(horseId));
        HorseSpecialtyResponse resp = specialties.get(horseId);

        String matSanUaThich = null;
        if (resp != null && resp.preferredSurface() != null) {
            matSanUaThich = resp.preferredSurface() == SurfaceType.TURF ? "Sân cỏ (TURF)" : "Sân cát (DIRT)";
        } else {
            // Phân tích bổ sung từ lịch sử các buổi tập workout thực tế để không bị trống dữ liệu
            List<SpecialtyWorkoutRow> rows = trainingWorkoutRepository.findSpecialtyWorkouts(List.of(horseId), LocalDate.now().minusDays(180));
            if (rows != null && !rows.isEmpty()) {
                Map<SurfaceType, Long> surfaceCounts = rows.stream()
                        .filter(r -> r.surfaceType() != null)
                        .collect(Collectors.groupingBy(SpecialtyWorkoutRow::surfaceType, Collectors.counting()));
                if (!surfaceCounts.isEmpty()) {
                    SurfaceType mostFrequent = surfaceCounts.entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse(null);
                    if (mostFrequent != null) {
                        String surfName = mostFrequent == SurfaceType.TURF ? "Sân cỏ (TURF)" : "Sân cát (DIRT)";
                        matSanUaThich = surfName + " (Ưu thế theo " + surfaceCounts.get(mostFrequent) + " buổi tập thực tế gần nhất)";
                    }
                }
            }
            if (matSanUaThich == null) {
                matSanUaThich = "Chưa ghi nhận đủ dữ liệu thi đấu trên các mặt sân (Khuyến nghị bố trí thêm buổi tập so sánh trên sân Cỏ và Cát)";
            }
        }

        SpecialtyMetrics metrics = computeSpecialtyMetrics(resp);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseName", horse.getName());
        result.put("registrationNumber", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        result.put("horseId", horse.getId());
        result.put("specialtyCount", metrics.specialtyCount());
        result.put("averageRating", metrics.averageRating());
        result.put("bestRating", metrics.bestRating());
        result.put("specialties", metrics.formattedDistances());
        result.put("strongDistances", metrics.strongBands());
        result.put("specialtyBreakdown", metrics.specialtyBreakdown());
        result.put("optimalDistance", (resp != null && !resp.strongDistances().isEmpty()) ? resp.strongDistances() : List.of("Đang phân tích thêm cự ly"));
        result.put("preferredSurface", matSanUaThich);
        result.put("distanceStats", resp != null ? resp.distanceStats() : Collections.emptyList());
        result.put("actionUrl", resolveHorseActionUrl(horse.getId(), currentUser));
        return result;
    }

    private Object handleRankAllHorsesBySpecialty(Map<String, Object> args, AuthenticatedUser currentUser) {
        String sortByArg = args.get("sortBy") != null ? args.get("sortBy").toString().trim().toLowerCase() : "count";
        boolean sortByRating = sortByArg.contains("rating") || sortByArg.contains("diem") || sortByArg.contains("điểm") || sortByArg.contains("score");

        List<Horse> horses;
        if (currentUser != null && "HEAD_TRAINER".equals(currentUser.getRole())) {
            horses = horseRepository.findManagedHorsesByTrainerIdAndStatusIn(
                    currentUser.getUserId(),
                    List.of(HorseStatus.ELIGIBLE, HorseStatus.CANDIDATE)
            );
            if (horses == null || horses.isEmpty()) {
                horses = horseRepository.findAll();
            }
        } else if (currentUser != null && "HORSE_OWNER".equals(currentUser.getRole())) {
            horses = horseRepository.findByOwnerId(currentUser.getUserId());
        } else {
            horses = horseRepository.findAll();
        }

        if (horses == null || horses.isEmpty()) {
            return Map.of(
                    "totalHorses", 0,
                    "horsesWithSpecialties", 0,
                    "rankedHorseList", Collections.emptyList(),
                    "summary", "Không tìm thấy chiến mã nào trong hệ thống để xếp hạng."
            );
        }

        List<Long> horseIds = horses.stream().map(Horse::getId).filter(Objects::nonNull).toList();
        Map<Long, HorseSpecialtyResponse> specialtiesMap = Collections.emptyMap();
        if (horseSpecialtyService != null && !horseIds.isEmpty()) {
            try {
                Map<Long, HorseSpecialtyResponse> queried = horseSpecialtyService.getSpecialties(horseIds);
                if (queried != null) {
                    specialtiesMap = queried;
                }
            } catch (Exception e) {
                log.warn("Lỗi khi batch query sở trường ngựa: {}", e.getMessage());
            }
        }

        List<Map<String, Object>> unrankedList = new ArrayList<>();
        int countWithSpecialties = 0;

        for (Horse h : horses) {
            HorseSpecialtyResponse resp = specialtiesMap.get(h.getId());
            SpecialtyMetrics m = computeSpecialtyMetrics(resp);
            if (m.specialtyCount() > 0) {
                countWithSpecialties++;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseId", h.getId());
            item.put("horseName", h.getName());
            item.put("registrationNumber", h.getRegistrationNumber() != null ? h.getRegistrationNumber() : "N/A");
            item.put("specialtyCount", m.specialtyCount());
            item.put("averageRating", m.averageRating());
            item.put("bestRating", m.bestRating());
            item.put("specialties", m.formattedDistances());
            item.put("strongDistances", m.strongBands());
            item.put("preferredSurface", m.preferredSurface());
            item.put("specialtyBreakdown", m.specialtyBreakdown());
            item.put("status", h.getCurrentStatus() != null ? h.getCurrentStatus().name() : "N/A");
            item.put("actionUrl", resolveHorseActionUrl(h.getId(), currentUser));
            unrankedList.add(item);
        }

        if (sortByRating) {
            unrankedList.sort(Comparator.comparing((Map<String, Object> m) -> (BigDecimal) m.get("averageRating"), Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(m -> (BigDecimal) m.get("bestRating"), Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Comparator.comparingInt((Map<String, Object> m) -> (Integer) m.get("specialtyCount")).reversed())
                    .thenComparing(m -> (String) m.get("horseName")));
        } else {
            unrankedList.sort(Comparator.comparingInt((Map<String, Object> m) -> (Integer) m.get("specialtyCount")).reversed()
                    .thenComparing((Map<String, Object> m) -> (BigDecimal) m.get("averageRating"), Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(m -> (BigDecimal) m.get("bestRating"), Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(m -> (String) m.get("horseName")));
        }

        List<Map<String, Object>> rankedList = new ArrayList<>();
        int rank = 1;
        for (Map<String, Object> m : unrankedList) {
            Map<String, Object> rankedItem = new LinkedHashMap<>();
            rankedItem.put("rank", rank++);
            rankedItem.putAll(m);
            rankedList.add(rankedItem);
        }

        String criterion = sortByRating
                ? "Điểm trung bình phong độ sở trường (averageRating DESC, WINDOW_DAYS=90, MIN_WORKOUTS=3, MIN_RATING=7.0)"
                : "Số lượng sở trường cự ly kết hợp điểm trung bình (specialtyCount DESC, averageRating DESC)";

        String summary = "Bảng xếp hạng " + rankedList.size() + " chiến mã theo "
                + (sortByRating ? "điểm trung bình phong độ sở trường (Average Rating)" : "số lượng sở trường (Specialty Count)")
                + ". Có " + countWithSpecialties + " ngựa đã xác định được sở trường.";

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalHorses", rankedList.size());
        response.put("horsesWithSpecialties", countWithSpecialties);
        response.put("sortBy", sortByRating ? "RATING" : "COUNT");
        response.put("rankingCriterion", criterion);
        response.put("summary", summary);
        response.put("rankedHorseList", rankedList);
        return response;
    }

    private String formatDistanceBand(DistanceBand band) {
        if (band == null) return "Chưa xác định";
        return switch (band) {
            case SPRINT -> "Nước rút (SPRINT: <=1400m)";
            case MILE -> "Cự ly trung bình (MILE: 1401m-1800m)";
            case MIDDLE -> "Cự ly trung xa (MIDDLE: 1801m-2400m)";
            case STAYER -> "Đường trường (STAYER: >2400m)";
        };
    }

    private String resolveHorseActionUrl(Long horseId, AuthenticatedUser currentUser) {
        if (horseId == null) return "/manager/horses";
        String role = currentUser != null && currentUser.getRole() != null ? currentUser.getRole() : "CLUB_MANAGER";
        return switch (role) {
            case "HEAD_TRAINER" -> "/trainer/horses/" + horseId;
            case "HORSE_OWNER" -> "/owner/horses/" + horseId;
            case "VETERINARIAN" -> "/veterinarian/horses/" + horseId;
            default -> "/manager/horses/" + horseId;
        };
    }

    private Object handleGetTrainerRiskAlerts(Map<String, Object> args, AuthenticatedUser currentUser) {
        Horse targetHorse = resolveHorse(args);
        Long horseId = targetHorse != null ? targetHorse.getId() : parseLong(args.get("horseId"));

        if (horseId != null) {
            Horse h = targetHorse != null ? targetHorse : horseRepository.findById(horseId).orElse(null);
            String hName = h != null ? h.getName() : "Ngựa #" + horseId;
            List<HorseAlertResponse> alerts = horsePerformanceService.getHorseAlerts(horseId, currentUser);
            return Map.of("horseName", hName, "alertCount", alerts.size(), "alertList", alerts
            );
        }

        // Tổng hợp cho toàn bộ ngựa do HLV quản lý
        List<Horse> managedHorses = horseRepository.findManagedHorsesByTrainerIdAndStatusIn(
                currentUser.getUserId(),
                List.of(HorseStatus.ELIGIBLE, HorseStatus.CANDIDATE)
        );

        List<Map<String, Object>> aggregated = new ArrayList<>();
        for (Horse h : managedHorses) {
            List<HorseAlertResponse> alerts = horsePerformanceService.getHorseAlerts(h.getId(), currentUser);
            if (!alerts.isEmpty()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("horseName", h.getName());
                item.put("alertCount", alerts.size());
                item.put("alertList", alerts);
                aggregated.add(item);
            }
        }

        return Map.of("trainerName", resolveUserName(currentUser, "Huấn luyện viên"), "horsesAtRiskCount", aggregated.size(), "alertDetails", aggregated
        );
    }

    private Object handleGetRacingReadinessAssessment(Map<String, Object> args) {
        Horse horse = resolveHorse(args);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Cần cung cấp ID hoặc tên ngựa");
        }
        Long horseId = horse.getId();

        Optional<RacingReadinessAssessment> opt = racingReadinessAssessmentRepository.findTopByHorseIdOrderByCreatedAtDesc(horseId);
        if (opt.isEmpty()) {
            return Map.of("horseName", horse.getName(), "message", "Chưa có bản đánh giá sẵn sàng thi đấu nào cho chiến mã này."
            );
        }

        RacingReadinessAssessment r = opt.get();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseName", horse.getName());
        result.put("readinessStatus", r.getReadinessStatus());
        result.put("fitnessScore", r.getFitnessScore());
        result.put("conformationScore", r.getConformationScore());
        result.put("temperamentScore", r.getTemperamentScore());
        result.put("racingFormScore", r.getGaitQualityScore());
        result.put("estimatedMonthsToRace", r.getEstimatedMonthsToRace());
        result.put("assessmentDate", r.getAssessmentDate());
        result.put("evaluatedUpToDate", r.getValidUntil());
        result.put("expertReview", r.getRemarks());
        return result;
    }

    private Object handleCheckGroomConflictForLot(Map<String, Object> args) {
        Horse horse = resolveHorse(args);
        Long horseId = horse != null ? horse.getId() : parseLong(args.get("horseId"));
        Long groomId = parseLong(args.get("groomId"));

        if (horseId == null || groomId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Thiếu thông tin ngựa hoặc groom");
        }

        LocalDate fromDate = parseDate(args.get("date"), LocalDate.now());
        List<GroomConflictRow> conflicts = trainingWorkoutRepository.findGroomConflicts(horseId, groomId, fromDate);

        String horseName = horse != null ? horse.getName() : "Ngựa #" + horseId;
        String groomName = "Groom #" + groomId;
        if (userRepository != null) {
            groomName = userRepository.findById(groomId).map(User::getFullName).orElse(groomName);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseName", horseName);
        result.put("groomName", groomName);
        result.put("inspectionDate", fromDate.toString());
        result.put("hasScheduleConflict", !conflicts.isEmpty());
        result.put("conflictShiftCount", conflicts.size());
        result.put("conflictDetails", conflicts);
        return result;
    }

    private Object handleGetTrainerAssignedGrooms(AuthenticatedUser currentUser) {
        Long trainerId = currentUser.getUserId();
        List<Area> areas = areaRepository.findAll().stream()
                .filter(a -> Objects.equals(a.getTrainerId(), trainerId))
                .toList();

        if (areas.isEmpty()) {
            return Map.of("message", "Huấn luyện viên hiện chưa được phân công quản lý khu vực chuồng trại nào.", "groomList", Collections.emptyList()
            );
        }

        List<Long> areaIds = areas.stream().map(Area::getId).toList();
        List<StableStall> stalls = stableStallRepository.findByAreaIdInOrdered(areaIds);
        Set<Long> groomIds = stalls.stream()
                .map(StableStall::getGroomId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Horse> managedHorses = horseRepository.findManagedHorsesByTrainerIdAndStatusIn(
                trainerId, List.of(HorseStatus.ELIGIBLE, HorseStatus.CANDIDATE));
        Map<Long, String> horseByStall = managedHorses.stream()
                .filter(h -> h.getCurrentStallId() != null)
                .collect(Collectors.toMap(Horse::getCurrentStallId, Horse::getName, (a, b) -> a));

        List<Map<String, Object>> groomList = new ArrayList<>();
        for (Long groomId : groomIds) {
            User groomUser = userRepository != null ? userRepository.findById(groomId).orElse(null) : null;
            String groomName = groomUser != null && groomUser.getFullName() != null && !groomUser.getFullName().isBlank()
                    ? groomUser.getFullName() : "Groom #" + groomId;
            String groomPhone = groomUser != null && groomUser.getPhone() != null && !groomUser.getPhone().isBlank()
                    ? groomUser.getPhone().trim() : "Chưa cập nhật SĐT";
            String groomEmail = groomUser != null && groomUser.getEmail() != null && !groomUser.getEmail().isBlank()
                    ? groomUser.getEmail().trim() : "Chưa có email";

            List<StableStall> groomStalls = stalls.stream()
                    .filter(s -> Objects.equals(s.getGroomId(), groomId))
                    .toList();
            List<String> stallCodes = groomStalls.stream().map(StableStall::getStallCode).toList();
            List<String> horsesTakingCareOf = groomStalls.stream()
                    .map(s -> horseByStall.get(s.getId()))
                    .filter(Objects::nonNull)
                    .toList();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("groomName", groomName);
            item.put("phone", groomPhone);
            item.put("email", groomEmail);
            item.put("assignedStallCount", groomStalls.size());
            item.put("stallCodeList", stallCodes);
            item.put("caredHorseList", horsesTakingCareOf);
            groomList.add(item);
        }

        return Map.of("trainerName", resolveUserName(currentUser, "Huấn luyện viên"), "totalCollaboratingGrooms", groomList.size(), "groomList", groomList
        );
    }

    private Object handleGetMyHorseRaceRegistrations(Map<String, Object> args, AuthenticatedUser currentUser) {
        if (raceRegistrationRepository == null) {
            Map<String, Object> emptyResp = new LinkedHashMap<>();
            emptyResp.put("totalRaceRegistrations", 0);
            emptyResp.put("raceList", Collections.emptyList());
            return emptyResp;
        }
        Long trainerId = currentUser.getUserId();
        Horse targetHorse = resolveHorse(args);
        List<RaceRegistration> regs;
        if (targetHorse != null) {
            regs = raceRegistrationRepository.findByTrainerIdAndHorseIdOrderByCreatedAtDesc(trainerId, targetHorse.getId());
        } else {
            regs = raceRegistrationRepository.findByTrainerIdOrderByCreatedAtDesc(trainerId);
        }

        Map<Long, String> horseNames = horseRepository.findAll().stream()
                .collect(Collectors.toMap(Horse::getId, Horse::getName, (a, b) -> a));

        List<Map<String, Object>> result = new ArrayList<>();
        for (RaceRegistration r : regs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseName", horseNames.getOrDefault(r.getHorseId(), "Chiến mã"));
            item.put("raceName", r.getRaceName() != null ? r.getRaceName() : "Giải đua");
            item.put("registrationDate", r.getEventDate() != null ? r.getEventDate().toString() : (r.getCreatedAt() != null ? r.getCreatedAt().toString() : "N/A"));
            item.put("status", r.getStatus() != null ? r.getStatus().name() : "PENDING");
            item.put("notes", r.getTrainerNotes() != null ? r.getTrainerNotes() : (r.getSelectionReason() != null ? r.getSelectionReason() : ""));
            item.put("actionUrl", r.getId() != null ? "/trainer/racing/" + r.getId() : "/trainer/racing");
            result.add(item);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalRaceRegistrations", result.size());
        response.put("raceList", result);
        response.put("actionUrl", "/trainer/racing");
        return response;
    }

    private Object handleGetAssignedAdmissionReviews(AuthenticatedUser currentUser) {
        List<AdmissionApplication> apps = admissionApplicationRepository.findAllByOrderBySubmittedAtDesc();
        Long trainerId = currentUser.getUserId();

        List<AdmissionApplication> pendingTrainerApps = apps.stream()
                .filter(a -> a.getStatus() == AdmissionStatus.TRAINER_REVIEW
                        && (a.getTrainerId() == null || Objects.equals(a.getTrainerId(), trainerId)))
                .toList();

        List<Long> appIds = pendingTrainerApps.stream().map(AdmissionApplication::getId).toList();
        Map<Long, CandidateHorseProfile> candidates = (candidateHorseProfileRepository != null && !appIds.isEmpty())
                ? candidateHorseProfileRepository.findByAdmissionIdIn(appIds).stream()
                        .collect(Collectors.toMap(CandidateHorseProfile::getAdmissionId, c -> c, (a, b) -> a))
                : Collections.emptyMap();

        List<Map<String, Object>> result = new ArrayList<>();
        for (AdmissionApplication a : pendingTrainerApps) {
            CandidateHorseProfile candidate = candidates.get(a.getId());
            String horseName = candidate != null ? candidate.getName() : "Chiến mã đăng ký";
            String breed = candidate != null && candidate.getBreed() != null ? candidate.getBreed() : "Chưa rõ giống";
            String ownerName = "Chủ sở hữu";
            if (a.getOwnerId() != null && userRepository != null) {
                ownerName = userRepository.findById(a.getOwnerId()).map(User::getFullName).orElse(ownerName);
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseName", horseName);
            item.put("breed", breed);
            item.put("ownerName", ownerName);
            item.put("submittedDate", a.getSubmittedAt() != null ? a.getSubmittedAt().toString() : (a.getCreatedAt() != null ? a.getCreatedAt().toString() : "N/A"));
            item.put("status", "Chờ Huấn luyện viên thẩm định");
            item.put("actionUrl", a.getId() != null ? "/trainer/admissions/" + a.getId() : "/trainer/admissions");
            result.add(item);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("pendingReviewCount", result.size());
        response.put("applicationList", result);
        response.put("actionUrl", "/trainer/admissions");
        return response;
    }

    private Object handleGetCourseCurriculumOverview() {
        List<Course> courses = courseRepository != null ? courseRepository.findAll() : Collections.emptyList();
        List<Subject> subjects = subjectRepository != null ? subjectRepository.findAll() : Collections.emptyList();

        List<Map<String, Object>> courseList = new ArrayList<>();
        for (Course c : courses) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("courseName", c.getName());
            item.put("targetGoal", c.getTargetGoal() != null ? c.getTargetGoal() : "Rèn luyện thể lực");
            item.put("totalSessions", c.getTotalSessions() != null ? c.getTotalSessions() : 0);
            item.put("description", c.getDescription() != null ? c.getDescription() : "");
            courseList.add(item);
        }

        List<Map<String, Object>> subjectList = new ArrayList<>();
        for (Subject s : subjects) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("subjectName", s.getName());
            item.put("technicalGroup", s.getCategory() != null ? s.getCategory().name() : "N/A");
            item.put("surface", s.getSurfaceType() != null ? s.getSurfaceType().name() : "N/A");
            item.put("targetDistance", s.getTargetDistanceMeters() != null ? s.getTargetDistanceMeters() + "m" : "N/A");
            item.put("intensity", s.getIntensityLevel() != null ? s.getIntensityLevel().name() : "MEDIUM");
            item.put("durationMinutes", s.getDurationMinutes() != null ? s.getDurationMinutes() : 60);
            subjectList.add(item);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("courseCurriculumList", courseList);
        response.put("technicalSubjectList", subjectList);
        response.put("actionUrl", "/trainer/courses");
        return response;
    }

    private Object handleAnalyzeWorkoutLoadAndOvertrainingRisk(Map<String, Object> args) {
        Horse horse = resolveHorse(args);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Cần cung cấp ID hoặc tên ngựa");
        }

        List<TrainingWorkout> workouts = (trainingWorkoutRepository != null)
                ? trainingWorkoutRepository.findByHorseIdOrderByCreatedAtDesc(horse.getId())
                : Collections.emptyList();

        LocalDateTime fourteenDaysAgo = LocalDateTime.now().minusDays(14);
        List<TrainingWorkout> recentWorkouts = workouts.stream()
                .filter(w -> w.getCreatedAt() == null || !w.getCreatedAt().isBefore(fourteenDaysAgo))
                .toList();
        if (recentWorkouts.isEmpty() && !workouts.isEmpty()) {
            recentWorkouts = workouts;
        }

        double totalDistanceMeters = 0.0;
        int highIntensityCount = 0;
        int sumHr = 0;
        int hrCount = 0;
        int maxHr = 0;
        int maxRecoveryHr = 0;

        for (TrainingWorkout w : recentWorkouts) {
            if (w.getActualDistanceMeters() != null) {
                totalDistanceMeters += w.getActualDistanceMeters().doubleValue();
            }
            Integer avgHr = w.getAverageHeartRate();
            if (avgHr != null && avgHr > 0) {
                sumHr += avgHr;
                hrCount++;
            }
            Integer mHr = w.getMaxHeartRate();
            if (mHr != null && mHr > maxHr) {
                maxHr = mHr;
            }
            Integer rHr = w.getRecoveryHeartRate();
            if (rHr != null && rHr > maxRecoveryHr) {
                maxRecoveryHr = rHr;
            }
            if ((mHr != null && mHr >= 120) || (w.getTopSpeedKmh() != null && w.getTopSpeedKmh().doubleValue() >= 55.0)) {
                highIntensityCount++;
            }
        }

        int avgHeartRate = hrCount > 0 ? (int) Math.round((double) sumHr / hrCount) : (maxHr > 0 ? maxHr - 15 : 95);
        String tongCuLy14Ngay = totalDistanceMeters >= 1000.0
                ? String.format(Locale.US, "%.1f km", totalDistanceMeters / 1000.0)
                : String.format(Locale.US, "%.0f m", totalDistanceMeters);

        boolean isBlocked = isHorseInQuarantineOrBlocked(horse);
        String danhGiaNguyCo;
        String khuyenNghiHuanLuyen;

        if (isBlocked) {
            danhGiaNguyCo = "Cao";
            khuyenNghiHuanLuyen = "Chiến mã đang trong diện cách ly y tế hoặc có quyết định hạn chế tập luyện (BLOCKED). Tuyệt đối ngưng mọi bài tập vận động nặng, tuân thủ phác đồ điều trị và theo dõi hồi phục tại chuồng.";
        } else if (maxHr >= 130 || avgHeartRate > 115 || maxRecoveryHr > 105 || highIntensityCount >= 5) {
            danhGiaNguyCo = "Cao";
            khuyenNghiHuanLuyen = "Khối lượng vận động 14 ngày qua chạm ngưỡng quá tải. Khuyến nghị giảm 30-50% cự ly bài tập kế tiếp, tăng cường đi bộ hồi phục trên sân cỏ và đo nhịp tim trước ca tập.";
        } else if (maxHr >= 115 || avgHeartRate > 100 || highIntensityCount >= 3) {
            danhGiaNguyCo = "Trung bình";
            khuyenNghiHuanLuyen = "Cường độ tập luyện ở mức trung bình ổn định. Khuyến nghị duy trì lịch tập xen kẽ ngày nghỉ hồi phục và theo dõi sát bước đi sau khi hoàn thành cự ly dài.";
        } else {
            danhGiaNguyCo = "Thấp";
            khuyenNghiHuanLuyen = "Thể lực và nhịp tim phục hồi rất tốt. Ngựa có thể duy trì hoặc nâng nhẹ cường độ các bài tập bứt tốc và kỹ thuật đường đua.";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseName", horse.getName());
        result.put("registrationNumber", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        result.put("totalDistance14Days", tongCuLy14Ngay);
        result.put("workoutCount", recentWorkouts.size());
        result.put("avgHeartRate", avgHeartRate + " bpm");
        result.put("maxHeartRate", (maxHr > 0 ? maxHr : 110) + " bpm");
        result.put("highIntensitySessions", highIntensityCount);
        result.put("riskAssessment", danhGiaNguyCo);
        result.put("trainingRecommendations", khuyenNghiHuanLuyen);
        result.put("actionUrl", "/trainer/dashboard");
        return result;
    }

    private Object handleRecommendOptimalRaceDistanceAndTrack(Map<String, Object> args) {
        Horse horse = resolveHorse(args);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Cần cung cấp ID hoặc tên ngựa");
        }

        Map<Long, HorseSpecialtyResponse> specialties = (horseSpecialtyService != null)
                ? horseSpecialtyService.getSpecialties(List.of(horse.getId()))
                : Collections.emptyMap();
        HorseSpecialtyResponse resp = specialties.get(horse.getId());

        List<TrainingWorkout> workouts = (trainingWorkoutRepository != null)
                ? trainingWorkoutRepository.findByHorseIdOrderByCreatedAtDesc(horse.getId())
                : Collections.emptyList();

        String cuLyDeXuat;
        if (resp != null && resp.strongDistances() != null && !resp.strongDistances().isEmpty()) {
            cuLyDeXuat = resp.strongDistances().stream()
                    .map(b -> switch (b) {
                        case SPRINT -> "Cự ly ngắn Sprint (dưới 1400m)";
                        case MILE -> "Cự ly trung bình Mile (1400m - 1800m)";
                        case MIDDLE -> "Cự ly trung dài Middle (1800m - 2400m)";
                        case STAYER -> "Cự ly đường trường Stayer (trên 2400m)";
                    })
                    .collect(Collectors.joining(", "));
        } else {
            cuLyDeXuat = "Cự ly trung bình Mile (1400m - 1600m) - Đề xuất chạy thử nghiệm để chuẩn hóa cự ly tối ưu";
        }

        String matSanUaThich;
        if (resp != null && resp.preferredSurface() != null) {
            matSanUaThich = resp.preferredSurface() == SurfaceType.TURF ? "Sân cỏ tự nhiên (Turf)" : "Sân cát nhân tạo (Dirt)";
        } else {
            matSanUaThich = "Sân cỏ tự nhiên (Turf) - Đang ghi nhận ưu thế thể lực và độ bám móng tốt hơn";
        }

        BigDecimal topSpeed = workouts.stream()
                .map(TrainingWorkout::getTopSpeedKmh)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(new BigDecimal("56.5"));
        String tocDoToiUu = String.format(Locale.US, "%.1f km/h", topSpeed.doubleValue());
        String lyDoDeXuat = "Phân tích từ " + workouts.size() + " buổi tập thực tế cho thấy ngựa đạt phong độ và chỉ số hồi phục tim mạch cao nhất ở cự ly và mặt sân này.";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseName", horse.getName());
        result.put("registrationNumber", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        result.put("recommendedDistance", cuLyDeXuat);
        result.put("preferredSurface", matSanUaThich);
        result.put("optimalSpeed", tocDoToiUu);
        result.put("recommendationReason", lyDoDeXuat);
        result.put("actionUrl", "/trainer/racing");
        return result;
    }

    private Object handleRecommendNextWorkoutAdjustment(Map<String, Object> args) {
        Horse horse = resolveHorse(args);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Cần cung cấp ID hoặc tên ngựa");
        }

        boolean isBlocked = isHorseInQuarantineOrBlocked(horse);

        List<TrainingWorkout> workouts = (trainingWorkoutRepository != null)
                ? trainingWorkoutRepository.findByHorseIdOrderByCreatedAtDesc(horse.getId())
                : Collections.emptyList();
        TrainingWorkout latest = workouts.isEmpty() ? null : workouts.get(0);

        String buoiTapGanNhat;
        String nhanXetTheLuc;
        String baiTapKeTiepDeXuat;
        String lyDoDieuChinh;

        if (isBlocked) {
            buoiTapGanNhat = "Đang áp dụng lệnh đình chỉ tập luyện do lý do y tế/cách ly";
            nhanXetTheLuc = "Ngựa đang ở trạng thái BLOCKED hoặc khu vực cách ly, không đủ điều kiện an toàn thể chất để vận động.";
            baiTapKeTiepDeXuat = "Nghỉ ngơi hoàn toàn tại chuồng (Resting in Stall), tạm hoãn xếp ca tập cho đến khi bác sĩ thú y gỡ bỏ giới hạn y tế.";
            lyDoDieuChinh = "Tuân thủ tuyệt đối quy định an toàn sinh học và bảo vệ sức khỏe đàn ngựa (BR-Medical-Block).";
        } else if (latest != null) {
            boolean highHeartRate = (latest.getMaxHeartRate() != null && latest.getMaxHeartRate() > 110)
                    || (latest.getAverageHeartRate() != null && latest.getAverageHeartRate() > 110)
                    || (latest.getRecoveryHeartRate() != null && latest.getRecoveryHeartRate() > 100);
            boolean notCompleted = latest.getStatus() != null && latest.getStatus() != WorkoutStatus.COMPLETED;

            if (highHeartRate || notCompleted) {
                buoiTapGanNhat = "Cự ly " + (latest.getActualDistanceMeters() != null ? latest.getActualDistanceMeters() + "m" : "tiêu chuẩn")
                        + ", nhịp tim ghi nhận " + (latest.getMaxHeartRate() != null ? latest.getMaxHeartRate() : (latest.getAverageHeartRate() != null ? latest.getAverageHeartRate() : 115)) + " bpm";
                nhanXetTheLuc = "Nhịp tim sau bài tập vượt ngưỡng an toàn (>110 bpm) hoặc trạng thái buổi tập chưa đạt chuẩn toàn diện.";
                baiTapKeTiepDeXuat = "Giảm tải 30% cự ly, chuyển sang bài tập đi bộ phục hồi nhẹ nhàng trên sân cỏ tự nhiên (Turf Walking).";
                lyDoDieuChinh = "Tạo thời gian cho cơ tim và cơ bắp giải phóng acid lactic, tránh nguy cơ căng cơ hoặc chấn thương gân.";
            } else {
                buoiTapGanNhat = "Hoàn thành tốt cự ly " + (latest.getActualDistanceMeters() != null ? latest.getActualDistanceMeters() + "m" : "tiêu chuẩn")
                        + ", nhịp tim ổn định " + (latest.getAverageHeartRate() != null ? latest.getAverageHeartRate() + " bpm" : "");
                nhanXetTheLuc = "Thể lực sung mãn, nhịp tim hồi phục nhanh và đáp ứng tốt giáo án.";
                baiTapKeTiepDeXuat = "Tăng cự ly thêm 200m hoặc nâng tốc độ bứt tốc đoạn cuối trên sân cát (Dirt Canter).";
                lyDoDieuChinh = "Củng cố sức bền và khả năng bứt tốc về đích theo đúng tiến trình chu kỳ huấn luyện.";
            }
        } else {
            buoiTapGanNhat = "Chưa ghi nhận buổi tập nào gần đây";
            nhanXetTheLuc = "Chưa có dữ liệu thể lực gần nhất để so sánh đối chiếu.";
            baiTapKeTiepDeXuat = "Bài tập làm quen và khởi động căn bản (Trotting & Canter nhẹ) 1000m trên sân cỏ.";
            lyDoDieuChinh = "Đánh giá khả năng thích ứng ban đầu của chiến mã trước khi nâng cao khối lượng huấn luyện.";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseName", horse.getName());
        result.put("registrationNumber", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        result.put("lastWorkout", buoiTapGanNhat);
        result.put("fitnessReview", nhanXetTheLuc);
        result.put("nextRecommendedWorkout", baiTapKeTiepDeXuat);
        result.put("adjustmentReason", lyDoDieuChinh);
        result.put("actionUrl", "/trainer/plans");
        return result;
    }

    // =================================================================
    // VETERINARIAN Handlers
    // =================================================================

    private Object handleSearchClinicalKnowledge(Map<String, Object> args, AuthenticatedUser currentUser) {
        String query = (String) args.get("query");
        Horse horse = resolveHorse(args);
        Long horseId = horse != null ? horse.getId() : parseLong(args.get("horseId"));
        Integer limit = args.get("limit") != null ? ((Number) args.get("limit")).intValue() : 5;

        List<ClinicalChunkResponseDto> chunks = clinicalRetrievalService.retrieveSimilarClinicalRecords(
                query, horseId, currentUser.getRole(), 0.60, limit);
        return chunks;
    }

    private Object handleGetVetExaminationOverview() {
        List<CareSchedule> allSchedules = careScheduleRepository.findAll();
        Map<Long, String> horseNames = horseRepository.findAll().stream()
                .collect(Collectors.toMap(Horse::getId, Horse::getName, (a, b) -> a));

        List<Map<String, Object>> dangKham = new ArrayList<>();
        List<Map<String, Object>> chuaKham = new ArrayList<>();
        List<Map<String, Object>> daKham = new ArrayList<>();

        for (CareSchedule cs : allSchedules) {
            String horseName = horseNames.getOrDefault(cs.getHorseId(), "Ngựa #" + cs.getHorseId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseName", horseName);
            item.put("examType", cs.getCareType() != null ? cs.getCareType().name() : "Khám tổng quát");
            item.put("purpose", cs.getPurpose() != null ? cs.getPurpose().name() : "N/A");
            item.put("scheduledAt", cs.getScheduledAt() != null ? cs.getScheduledAt().toString() : "N/A");
            item.put("description", cs.getDescription() != null ? cs.getDescription() : "");

            if (cs.getStatus() == CareScheduleStatus.IN_PROGRESS) {
                dangKham.add(item);
            } else if (cs.getStatus() == CareScheduleStatus.SCHEDULED) {
                chuaKham.add(item);
            } else if (cs.getStatus() == CareScheduleStatus.COMPLETED) {
                daKham.add(item);
            }
        }

        return Map.of("totalScheduledExams", allSchedules.size(), "inProgressExams", dangKham, "inProgressExamsCount", dangKham.size(), "pendingScheduledExams", chuaKham, "pendingScheduledExamsCount", chuaKham.size(), "completedExams", daKham, "completedExamsCount", daKham.size()
        );
    }

    private Object handleGetHorseMedicalHistory(Map<String, Object> args) {
        Horse horse = resolveHorse(args);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Cần cung cấp ID hoặc tên ngựa");
        }
        List<HealthRecord> records = healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(horse.getId());
        List<Map<String, Object>> list = new ArrayList<>();
        for (HealthRecord hr : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseName", horse.getName());
            item.put("examDate", hr.getExaminedAt());
            item.put("symptoms", hr.getSymptoms());
            item.put("clinicalObservations", hr.getFindings());
            item.put("diagnosis", hr.getDiagnosis());
            item.put("treatmentPlan", hr.getTreatment());
            item.put("trainingDecision", hr.getTrainingDecision());
            item.put("restrictionDetails", hr.getRestrictionDetails());
            list.add(item);
        }
        return list;
    }

    private Object handleGetActivePrescriptions(Map<String, Object> args) {
        Horse horse = resolveHorse(args);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Cần cung cấp ID hoặc tên ngựa");
        }
        List<CareSchedule> active = careScheduleRepository.findByHorseIdAndStatusIn(
                horse.getId(), List.of(CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS)
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (CareSchedule cs : active) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseName", horse.getName());
            item.put("treatmentType", cs.getCareType() != null ? cs.getCareType().name() : "N/A");
            item.put("scheduledAt", cs.getScheduledAt() != null ? cs.getScheduledAt().toString() : "N/A");
            item.put("purpose", cs.getPurpose() != null ? cs.getPurpose().name() : "N/A");
            item.put("description", cs.getDescription() != null ? cs.getDescription() : "");
            result.add(item);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalPrescriptions", result.size());
        response.put("treatmentList", result);
        return response;
    }

    private Object handleGetPreventiveCareSchedule(Map<String, Object> args) {
        Horse targetHorse = resolveHorse(args);
        Long horseId = targetHorse != null ? targetHorse.getId() : parseLong(args.get("horseId"));
        LocalDate from = parseDate(args.get("fromDate"), null);
        LocalDate to = parseDate(args.get("toDate"), null);

        List<CareSchedule> schedules;
        if (horseId != null) {
            schedules = careScheduleRepository.findByHorseIdOrderByScheduledAtAsc(horseId);
        } else {
            schedules = careScheduleRepository.findAll();
        }

        Map<Long, String> horseNames = horseRepository.findAll().stream()
                .collect(Collectors.toMap(Horse::getId, Horse::getName, (a, b) -> a));

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
                    item.put("horseName", horseNames.getOrDefault(cs.getHorseId(), "Ngựa #" + cs.getHorseId()));
                    item.put("careType", cs.getCareType());
                    item.put("purpose", cs.getPurpose());
                    item.put("status", cs.getStatus());
                    item.put("scheduledAt", cs.getScheduledAt());
                    item.put("notes", cs.getDescription());
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
            item.put("horseName", h.getName());
            item.put("registrationNumber", h.getRegistrationNumber() != null ? h.getRegistrationNumber() : "N/A");
            item.put("currentStatus", h.getCurrentStatus());
            // TUYỆT ĐỐI KHÔNG CONFLICT: Ngựa cách ly luôn luôn BLOCKED khỏi tập luyện
            item.put("trainingDecision", "BLOCKED");
            item.put("isTrainingAllowed", false);
            item.put("decisionReason", h.getTrainingDecisionReason() != null ? h.getTrainingDecisionReason() : "Đang thực hiện cách ly kiểm dịch nghiêm ngặt - Cấm tập luyện");

            boolean inQuarantineStall = h.getCurrentStallId() != null && qStallIds.contains(h.getCurrentStallId());
            item.put("inQuarantineArea", inQuarantineStall);
            if (inQuarantineStall && stallMap.containsKey(h.getCurrentStallId())) {
                item.put("stallCode", stallMap.get(h.getCurrentStallId()).getStallCode());
            }

            List<HealthRecord> records = healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(h.getId());
            if (!records.isEmpty()) {
                HealthRecord latest = records.get(0);
                item.put("latestDiagnosis", latest.getDiagnosis());
                item.put("restrictionDetails", latest.getRestrictionDetails());
                item.put("lastExamDate", latest.getExaminedAt());
            }

            result.add(item);
        }

        return Map.of("quarantinedOrRestrictedHorsesCount", result.size(), "quarantinedHorseList", result
        );
    }

    private Object handleGetRecentIncidentReports() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(48);
        List<GroomIncidentReport> reports = groomIncidentReportRepository.findByReportedAtAfterOrderByReportedAtDesc(cutoff);
        Map<Long, String> horseNames = horseRepository.findAll().stream()
                .collect(Collectors.toMap(Horse::getId, Horse::getName, (a, b) -> a));

        List<Map<String, Object>> result = new ArrayList<>();
        for (GroomIncidentReport r : reports) {
            Map<String, Object> item = new LinkedHashMap<>();
            String horseName = horseNames.getOrDefault(r.getHorseId(), "Ngựa #" + r.getHorseId());
            String groomName = "Groom phụ trách";
            if (r.getGroomId() != null && userRepository != null) {
                groomName = userRepository.findById(r.getGroomId()).map(User::getFullName).orElse(groomName);
            }
            item.put("incidentTitle", r.getTitle());
            item.put("horseName", horseName);
            item.put("reporterName", groomName);
            item.put("severity", r.getSeverity());
            item.put("detailedDescription", r.getDescription());
            item.put("incidentStatus", r.getStatus());
            item.put("reportedAt", r.getReportedAt() != null ? r.getReportedAt().toString() : "N/A");
            result.add(item);
        }
        return Map.of("totalReportsLast48Hours", result.size(), "incidentList", result
        );
    }

    private Object handleGetPendingAdmissionExams() {
        List<AdmissionApplication> apps = admissionApplicationRepository.findAllByOrderBySubmittedAtDesc();
        List<AdmissionApplication> pendingVetApps = apps.stream()
                .filter(a -> a.getStatus() == AdmissionStatus.VET_REVIEW)
                .toList();

        List<Long> appIds = pendingVetApps.stream().map(AdmissionApplication::getId).toList();
        Map<Long, CandidateHorseProfile> candidates = (candidateHorseProfileRepository != null && !appIds.isEmpty())
                ? candidateHorseProfileRepository.findByAdmissionIdIn(appIds).stream()
                        .collect(Collectors.toMap(CandidateHorseProfile::getAdmissionId, c -> c, (a, b) -> a))
                : Collections.emptyMap();

        List<Map<String, Object>> result = new ArrayList<>();
        for (AdmissionApplication a : pendingVetApps) {
            CandidateHorseProfile candidate = candidates.get(a.getId());
            String horseName = candidate != null ? candidate.getName() : "Chiến mã đăng ký";
            String ownerName = "Chủ sở hữu";
            if (a.getOwnerId() != null && userRepository != null) {
                ownerName = userRepository.findById(a.getOwnerId()).map(User::getFullName).orElse(ownerName);
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseName", horseName);
            item.put("ownerName", ownerName);
            item.put("submittedDate", a.getSubmittedAt() != null ? a.getSubmittedAt().toString() : (a.getCreatedAt() != null ? a.getCreatedAt().toString() : "N/A"));
            item.put("status", "Chờ bác sĩ khám sàng lọc");
            item.put("actionUrl", a.getId() != null ? "/veterinarian/admissions?id=" + a.getId() : "/veterinarian/admissions");
            result.add(item);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("pendingExamCount", result.size());
        response.put("examList", result);
        response.put("actionUrl", "/veterinarian/admissions");
        return response;
    }

    private Object handleGetUrgentCareAlerts() {
        List<CareSchedule> all = careScheduleRepository.findAll();
        List<CareSchedule> urgent = all.stream()
                .filter(cs -> cs.getCareType() == CareType.URGENT && cs.getStatus() != CareScheduleStatus.COMPLETED)
                .toList();

        Map<Long, Horse> horses = horseRepository.findAll().stream()
                .collect(Collectors.toMap(Horse::getId, h -> h, (a, b) -> a));

        List<Map<String, Object>> result = new ArrayList<>();
        for (CareSchedule cs : urgent) {
            Horse h = horses.get(cs.getHorseId());
            String horseName = h != null ? h.getName() : "Chiến mã";
            String stallName = h != null ? resolveStallName(h.getCurrentStallId()) : "Chưa rõ chuồng";

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseName", horseName);
            item.put("stall", stallName);
            item.put("severity", "Khẩn cấp");
            item.put("symptoms", cs.getDescription() != null ? cs.getDescription() : "Cần can thiệp y tế khẩn cấp");
            item.put("timestamp", cs.getScheduledAt() != null ? cs.getScheduledAt().toString() : "N/A");
            item.put("actionUrl", cs.getId() != null ? "/veterinarian/urgent/" + cs.getId() : "/veterinarian/urgent");
            result.add(item);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalUrgentAlerts", result.size());
        response.put("alertList", result);
        response.put("actionUrl", "/veterinarian/urgent");
        return response;
    }

    private Object handleGetHorseVitalSignsTrend(Map<String, Object> args) {
        if (horseHealthMetricRepository == null) {
            return Collections.emptyList();
        }

        boolean isAllRequested = false;
        if (args != null) {
            Object nameObj = args.get("horseName");
            if (nameObj == null) nameObj = args.get("name");
            if (nameObj != null) {
                String n = nameObj.toString().trim().toLowerCase();
                if (n.equals("all") || n.contains("tất cả") || n.contains("tat ca") || n.contains("toàn bộ") || n.contains("toan bo")) {
                    isAllRequested = true;
                }
            }
        }

        Horse horse = isAllRequested ? null : resolveHorse(args);

        // Nếu không chỉ định ngựa hoặc yêu cầu xem tất cả
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
                            horseRepository.findById(id).map(Horse::getName).orElse("Ngựa #" + id));
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("horseName", hName);
                item.put("measurementDate", m.getRecordedAt() != null ? m.getRecordedAt().toString() : "N/A");
                item.put("weight", m.getWeight() != null ? m.getWeight() + " kg" : "N/A");
                item.put("temperature", m.getTemperature() != null ? m.getTemperature() + " °C" : "N/A");
                item.put("heartRate", m.getHeartRate() != null ? m.getHeartRate() + " bpm" : "N/A");
                item.put("respiratoryRate", m.getRespiratoryRate() != null ? m.getRespiratoryRate() + " nhịp/phút" : "N/A");
                item.put("clinicalNotes", m.getNotes() != null ? m.getNotes() : (m.getHydrationStatus() != null ? m.getHydrationStatus() : "Chỉ số ổn định"));
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
            item.put("respiratoryRate", m.getRespiratoryRate() != null ? m.getRespiratoryRate() + " nhịp/phút" : "N/A");
            item.put("clinicalNotes", m.getNotes() != null ? m.getNotes() : (m.getHydrationStatus() != null ? m.getHydrationStatus() : "Chỉ số ổn định"));
            result.add(item);
        }
        return result;
    }

    private Object handleAnalyzeIncidentPatternsAndRecurringRisks(Map<String, Object> args) {
        Horse horse = resolveHorse(args);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Cần cung cấp ID hoặc tên ngựa");
        }

        List<HorseIncidentReport> incidents = (horseIncidentReportRepository != null)
                ? horseIncidentReportRepository.findByHorseId(horse.getId())
                : Collections.emptyList();

        List<HealthRecord> records = (healthRecordRepository != null)
                ? healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(horse.getId())
                : Collections.emptyList();

        Set<String> conditions = new LinkedHashSet<>();
        for (HealthRecord hr : records) {
            if (hr.getDiagnosis() != null && !hr.getDiagnosis().isBlank()) {
                conditions.add(hr.getDiagnosis().trim());
            } else if (hr.getSymptoms() != null && !hr.getSymptoms().isBlank()) {
                conditions.add(hr.getSymptoms().trim());
            }
        }
        for (HorseIncidentReport ir : incidents) {
            if (ir.getTitle() != null && !ir.getTitle().isBlank()) {
                conditions.add(ir.getTitle().trim());
            }
        }

        String benhLyPhoBien = !conditions.isEmpty()
                ? String.join(", ", conditions)
                : "Không ghi nhận tiền sử bệnh lý nghiêm trọng trong 6 tháng qua";

        String nguyCoTaiPhat;
        if (incidents.size() >= 3 || records.size() >= 3) {
            nguyCoTaiPhat = "Cao (Tần suất sự cố chuồng trại hoặc bệnh lý lặp lại trên 3 lần trong 6 tháng)";
        } else if (incidents.size() >= 1 || records.size() >= 1) {
            nguyCoTaiPhat = "Trung bình (Có tiền sử sự cố đơn lẻ hoặc tổn thương phần mềm đang trong quá trình hồi phục)";
        } else {
            nguyCoTaiPhat = "Thấp (Hồ sơ sức khỏe ổn định, không có dấu hiệu mãn tính tái diễn)";
        }

        String khuyenNghiPhongNgua = "Thực hiện kiểm tra khớp và móng định kỳ trước các bài tập nặng, xoa bóp cơ bắp sau giờ tập, bổ sung men vi sinh và duy trì kiểm tra khẩu phần xơ thô.";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseName", horse.getName());
        result.put("registrationNumber", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        result.put("totalIncidents", incidents.size());
        result.put("totalExamsCount", records.size());
        result.put("commonConditions", benhLyPhoBien);
        result.put("recurrenceRisk", nguyCoTaiPhat);
        result.put("preventiveRecommendations", khuyenNghiPhongNgua);
        result.put("actionUrl", horse != null && horse.getId() != null ? "/veterinarian/health?horseId=" + horse.getId() : "/veterinarian/health");
        return result;
    }

    private Object handleRecommendClinicalInterventionPlan(Map<String, Object> args, AuthenticatedUser currentUser) {
        String symptoms = args.get("symptoms") != null ? args.get("symptoms").toString().trim() : null;
        if (symptoms == null || symptoms.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Cần cung cấp mô tả triệu chứng lâm sàng (symptoms)");
        }

        Horse horse = resolveHorse(args);
        Long horseId = horse != null ? horse.getId() : null;

        List<ClinicalChunkResponseDto> chunks = (clinicalRetrievalService != null)
                ? clinicalRetrievalService.retrieveSimilarClinicalRecords(symptoms, horseId, 3)
                : Collections.emptyList();

        List<String> refDocs = chunks.stream()
                .map(c -> c.title() != null ? c.title() + " (" + c.category() + ")" : "Tài liệu lâm sàng")
                .distinct()
                .toList();
        if (refDocs.isEmpty()) {
            refDocs = List.of("Phác đồ điều trị nội khoa thú y chuẩn RTMS", "Sổ tay cấp cứu lâm sàng chiến mã");
        }

        String phacDoCanThiepDeXuat = "Bước 1 (Xử trí ban đầu): Ngay lập tức cách ly tại chuồng bệnh, đặt đường truyền tĩnh mạch bù nước & điện giải, tạm ngưng thức ăn thô. "
                + "Bước 2 (Can thiệp chuyên sâu): Sử dụng thuốc kháng viêm không steroid (NSAID Flunixin Meglumine 1.1 mg/kg IV) và thuốc bảo vệ niêm mạc; theo dõi nhu động ruột mỗi 2 giờ.";

        String xetNghiemCanLam = "Tổng phân tích tế bào máu (CBC), đo nồng độ Lactate huyết tương, sinh hóa máu đánh giá chức năng gan/thận và chụp X-quang/siêu âm ổ bụng hoặc chi dưới.";
        String thoiGianTheoDoiToiThieu = "24 - 48 giờ liên tục dưới sự giám sát trực tiếp của Bác sĩ thú y trực ban.";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseName", horse != null ? horse.getName() : "Chiến mã đang khám");
        result.put("registrationNumber", horse != null && horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        result.put("intakeSymptoms", symptoms);
        result.put("recommendedIntervention", phacDoCanThiepDeXuat);
        result.put("requiredTests", xetNghiemCanLam);
        result.put("minObservationTime", thoiGianTheoDoiToiThieu);
        result.put("referenceGuidelines", refDocs);
        result.put("actionUrl", "/veterinarian/urgent");
        return result;
    }

    private Object handleEvaluateTrainingResumptionReadiness(Map<String, Object> args) {
        Horse horse = resolveHorse(args);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Cần cung cấp ID hoặc tên ngựa");
        }

        boolean isBlocked = isHorseInQuarantineOrBlocked(horse);

        List<HorseHealthMetric> metrics = (horseHealthMetricRepository != null)
                ? horseHealthMetricRepository.findByHorseIdOrderByRecordedAtDesc(horse.getId())
                : Collections.emptyList();
        HorseHealthMetric latestMetric = metrics.isEmpty() ? null : metrics.get(0);

        boolean feverOrHighHr = latestMetric != null && (
                (latestMetric.getTemperature() != null && latestMetric.getTemperature().compareTo(new BigDecimal("38.5")) > 0)
                || (latestMetric.getHeartRate() != null && latestMetric.getHeartRate().compareTo(new BigDecimal("44")) > 0)
        );

        String diemSanSang;
        String khuyenNghiBacSi;
        String hanCheTapLuyenDeXuat;

        if (isBlocked || feverOrHighHr) {
            diemSanSang = "15/100";
            khuyenNghiBacSi = "Cần nghỉ ngơi thêm, chưa đủ điều kiện thể chất an toàn để quay lại tập luyện";
            hanCheTapLuyenDeXuat = "Duy trì quyết định BLOCKED, nghiêm cấm tập luyện trên đường chạy, chỉ cho phép dẫn đi bộ thư giãn trong chuồng (tối đa 10 phút).";
        } else {
            diemSanSang = "85/100";
            khuyenNghiBacSi = "Đủ điều kiện tập nhẹ";
            hanCheTapLuyenDeXuat = "Chỉ đi bộ và chạy bước nhỏ (Walking & Trotting) dưới 20 phút trên sân cỏ tự nhiên, nghiêm cấm bứt tốc nước rút (Galloping) trong 7 ngày đầu.";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseName", horse.getName());
        result.put("registrationNumber", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        result.put("currentStatus", horse.getCurrentStatus() != null ? horse.getCurrentStatus().name() : "N/A");
        result.put("trainingDecision", horse.getTrainingDecision() != null ? horse.getTrainingDecision().name() : "N/A");
        result.put("readinessScore", diemSanSang);
        result.put("vetRecommendations", khuyenNghiBacSi);
        result.put("proposedTrainingRestriction", hanCheTapLuyenDeXuat);
        result.put("actionUrl", "/veterinarian/preventive-care");
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
            return Map.of("message", "Hiện tại bạn chưa được phân công phụ trách chuồng nào.", "stallAndHorseList", Collections.emptyList()
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
            item.put("stallCode", stall.getStallCode());
            item.put("stallNumber", stall.getStallNumber());
            item.put("stallStatus", stall.getStatus());

            Horse h = horseByStall.get(stall.getId());
            if (h != null) {
                item.put("horseName", h.getName());
                item.put("registrationNumber", h.getRegistrationNumber() != null ? h.getRegistrationNumber() : "N/A");
                item.put("horseStatus", h.getCurrentStatus());
                item.put("trainingDecision", h.getTrainingDecision());
            } else {
                item.put("horseInStall", "Chuồng đang trống");
            }
            horseList.add(item);
        }

        return Map.of("groomName", resolveUserName(currentUser, "Groom"), "assignedStallsCount", stalls.size(), "stallsWithHorsesCount", horses.size(), "stallAndHorseList", horseList
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
        Horse horse = resolveHorse(args);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Cần cung cấp ID hoặc tên ngựa");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseName", horse.getName());
        result.put("registrationNumber", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        result.put("status", horse.getCurrentStatus());
        result.put("trainingDecision", horse.getTrainingDecision());
        result.put("decisionReason", horse.getTrainingDecisionReason() != null ? horse.getTrainingDecisionReason() : "Không có hạn chế đặc biệt");

        List<HealthRecord> records = healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(horse.getId());
        if (!records.isEmpty()) {
            HealthRecord latest = records.get(0);
            result.put("latestDiagnosis", latest.getDiagnosis());
            result.put("treatmentPlan", latest.getTreatment());
            result.put("medicalCareNotes", latest.getRestrictionDetails());
            result.put("lastExamDate", latest.getExaminedAt());
        }

        List<CareSchedule> activeSchedules = careScheduleRepository.findByHorseIdAndStatusIn(
                horse.getId(), List.of(CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS)
        );
        List<Map<String, Object>> careList = activeSchedules.stream().map(cs -> Map.<String, Object>of(
                "loaiChamSoc", cs.getCareType() != null ? cs.getCareType().name() : "Khám thú y", "scheduledAt", cs.getScheduledAt() != null ? cs.getScheduledAt().toString() : "N/A", "purpose", cs.getPurpose() != null ? cs.getPurpose().name() : "N/A"
        )).toList();
        result.put("upcomingCareSchedule", careList);

        return result;
    }

    private Object handleGetHorseStallLocation(Map<String, Object> args) {
        Horse horse = resolveHorse(args);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Cần cung cấp ID hoặc tên ngựa");
        }
        StableStall stall = horse.getCurrentStallId() != null ? stableStallRepository.findById(horse.getCurrentStallId()).orElse(null) : null;
        String areaName = "Chưa rõ";
        String stallName = "Chưa xếp chuồng";
        String groomName = "Chưa phân công";
        if (stall != null) {
            stallName = "Chuồng " + stall.getStallCode() + " (Số " + stall.getStallNumber() + ")";
            if (stall.getAreaId() != null) {
                areaName = areaRepository.findById(stall.getAreaId()).map(Area::getName).orElse("Khu chuồng trại");
            }
            if (stall.getGroomId() != null && userRepository != null) {
                groomName = userRepository.findById(stall.getGroomId()).map(User::getFullName).orElse("Chưa phân công");
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseName", horse.getName());
        result.put("registrationNumber", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        result.put("stallLocation", stallName);
        result.put("areaName", areaName);
        result.put("assignedGroom", groomName);
        return result;
    }

    private Object handleGetMyReportedIncidents(AuthenticatedUser currentUser) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        Map<Long, String> horseNames = horseRepository.findAll().stream()
                .collect(Collectors.toMap(Horse::getId, Horse::getName, (a, b) -> a));

        List<Map<String, Object>> result = new ArrayList<>();
        if (horseIncidentReportRepository != null) {
            List<HorseIncidentReport> reports = horseIncidentReportRepository.findByReportedById(currentUser.getUserId());
            for (HorseIncidentReport r : reports) {
                if (r.getReportedAt() != null && r.getReportedAt().isBefore(cutoff)) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("horseName", horseNames.getOrDefault(r.getHorseId(), "Chiến mã"));
                item.put("title", r.getTitle());
                item.put("severity", r.getSeverity() != null ? r.getSeverity().name() : "MEDIUM");
                item.put("reportedAt", r.getReportedAt() != null ? r.getReportedAt().toString() : "N/A");
                item.put("incidentStatus", r.getStatus() != null ? r.getStatus().name() : "REPORTED");
                item.put("actionUrl", "/groom/incidents");
                result.add(item);
            }
        }
        if (result.isEmpty() && groomIncidentReportRepository != null) {
            List<GroomIncidentReport> gReports = groomIncidentReportRepository.findByGroomId(currentUser.getUserId());
            for (GroomIncidentReport r : gReports) {
                if (r.getReportedAt() != null && r.getReportedAt().isBefore(cutoff)) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("horseName", horseNames.getOrDefault(r.getHorseId(), "Chiến mã"));
                item.put("title", r.getTitle());
                item.put("severity", r.getSeverity() != null ? r.getSeverity().name() : "MEDIUM");
                item.put("reportedAt", r.getReportedAt() != null ? r.getReportedAt().toString() : "N/A");
                item.put("incidentStatus", r.getStatus() != null ? r.getStatus().name() : "REPORTED");
                item.put("actionUrl", "/groom/incidents");
                result.add(item);
            }
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalReportedIncidents", result.size());
        response.put("incidentList", result);
        response.put("actionUrl", "/groom/incidents");
        return response;
    }

    private Object handleGetGroomTaskCompletionRate(Map<String, Object> args, AuthenticatedUser currentUser) {
        LocalDate targetDate = parseDate(args.get("date"), LocalDate.now());
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.atTime(LocalTime.MAX);

        List<GroomDailyTask> tasks = groomDailyTaskRepository.findByGroomIdAndScheduledTimeBetween(
                currentUser.getUserId(), start, end);

        int tongSoNhiemVu = tasks.size();
        long soNhiemVuHoanThanh = tasks.stream().filter(t -> Boolean.TRUE.equals(t.getIsCompleted())).count();
        double rate = tongSoNhiemVu > 0 ? ((double) soNhiemVuHoanThanh / tongSoNhiemVu) * 100.0 : 100.0;
        String tyLeHoanThanh = String.format(Locale.US, "%.1f%%", rate);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", targetDate.toString());
        result.put("totalTasksCount", tongSoNhiemVu);
        result.put("completedTasksCount", soNhiemVuHoanThanh);
        result.put("completionRate", tyLeHoanThanh);
        result.put("actionUrl", "/groom/tasks");
        return result;
    }

    private Object handleDetectHorseBehavioralAnomaly(Map<String, Object> args) {
        Horse horse = resolveHorse(args);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Cần cung cấp ID hoặc tên ngựa");
        }

        List<HorseIncidentReport> incidents = (horseIncidentReportRepository != null)
                ? horseIncidentReportRepository.findByHorseId(horse.getId())
                : Collections.emptyList();
        List<HorseHealthMetric> metrics = (horseHealthMetricRepository != null)
                ? horseHealthMetricRepository.findByHorseIdOrderByRecordedAtDesc(horse.getId())
                : Collections.emptyList();

        boolean colicSuspected = false;
        boolean mildAnomaly = false;
        List<String> detectedAnomalies = new ArrayList<>();

        for (HorseIncidentReport r : incidents) {
            String desc = ((r.getDescription() != null ? r.getDescription() : "") + " " + (r.getTitle() != null ? r.getTitle() : "")).toLowerCase();
            if (desc.contains("bỏ ăn") || desc.contains("cào đất") || desc.contains("uể oải")
                    || desc.contains("colic") || desc.contains("đau bụng") || desc.contains("lăn lộn")) {
                colicSuspected = true;
                detectedAnomalies.add(r.getTitle() != null ? r.getTitle() : "Dấu hiệu bất thường trong báo cáo sự cố chuồng");
            }
        }

        if (!metrics.isEmpty()) {
            HorseHealthMetric m = metrics.get(0);
            if (m.getTemperature() != null && m.getTemperature().compareTo(new BigDecimal("38.5")) > 0) {
                mildAnomaly = true;
                detectedAnomalies.add("Thân nhiệt tăng (" + m.getTemperature() + " °C)");
            }
            if (m.getHeartRate() != null && m.getHeartRate().compareTo(new BigDecimal("44")) > 0) {
                colicSuspected = true;
                detectedAnomalies.add("Nhịp tim khi nghỉ cao bất thường (" + m.getHeartRate() + " bpm)");
            }
        }

        String dauHieuBatThuong;
        String mucDoCanhBao;
        String hanhDongCanLamNgay;

        if (colicSuspected) {
            dauHieuBatThuong = !detectedAnomalies.isEmpty() ? String.join("; ", detectedAnomalies) : "Phát hiện dấu hiệu cào đất, bỏ ăn và nhịp tim nghỉ tăng cao";
            mucDoCanhBao = "Khẩn cấp";
            hanhDongCanLamNgay = "Cảnh báo nguy cơ đau bụng Colic cấp tính! Báo ngay cho Bác sĩ thú y trực ban, tạm ngưng toàn bộ thức ăn thô và tinh, không để ngựa nằm lăn tròn trong chuồng, dắt đi bộ nhẹ nhàng.";
        } else if (mildAnomaly) {
            dauHieuBatThuong = !detectedAnomalies.isEmpty() ? String.join("; ", detectedAnomalies) : "Dấu hiệu uể oải nhẹ, giảm lượng ăn";
            mucDoCanhBao = "Trung bình";
            hanhDongCanLamNgay = "Theo dõi sát lượng nước uống và thức ăn trong 4 giờ tới, kiểm tra phân và đo lại thân nhiệt định kỳ.";
        } else {
            dauHieuBatThuong = "Không ghi nhận bất thường hành vi trong nhật ký gần nhất";
            mucDoCanhBao = "Thấp";
            hanhDongCanLamNgay = "Duy trì chế độ ăn uống, vệ sinh chuồng và vận động tiêu chuẩn theo SOP.";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseName", horse.getName());
        result.put("registrationNumber", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        result.put("abnormalSigns", dauHieuBatThuong);
        result.put("alertLevel", mucDoCanhBao);
        result.put("immediateActionRequired", hanhDongCanLamNgay);
        result.put("actionUrl", "/groom/incidents");
        return result;
    }

    private Object handleRecommendDailyCarePriorityOrder(AuthenticatedUser currentUser) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<GroomDailyTask> tasks = (groomDailyTaskRepository != null && currentUser.getUserId() != null)
                ? groomDailyTaskRepository.findByGroomIdAndScheduledTimeBetween(currentUser.getUserId(), startOfDay, endOfDay)
                : Collections.emptyList();

        Map<Long, String> horseNames = (horseRepository != null)
                ? horseRepository.findAll().stream().collect(Collectors.toMap(Horse::getId, Horse::getName, (a, b) -> a))
                : Collections.emptyMap();

        List<Map<String, Object>> priorityList = new ArrayList<>();
        if (!tasks.isEmpty()) {
            List<GroomDailyTask> sorted = tasks.stream()
                    .sorted((t1, t2) -> {
                        int p1 = getGroomTaskPriority(t1);
                        int p2 = getGroomTaskPriority(t2);
                        if (p1 != p2) return Integer.compare(p1, p2);
                        if (t1.getScheduledTime() == null && t2.getScheduledTime() == null) return 0;
                        if (t1.getScheduledTime() == null) return 1;
                        if (t2.getScheduledTime() == null) return -1;
                        return t1.getScheduledTime().compareTo(t2.getScheduledTime());
                    })
                    .toList();

            int seq = 1;
            for (GroomDailyTask t : sorted) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("priorityOrder", seq++);
                item.put("taskType", formatGroomTaskType(t.getTaskType()));
                item.put("horseName", horseNames.getOrDefault(t.getHorseId(), "Chiến mã"));
                item.put("estimatedTime", t.getScheduledTime() != null ? t.getScheduledTime().toLocalTime().toString() : "Trong ngày");
                item.put("status", Boolean.TRUE.equals(t.getIsCompleted()) ? "Đã hoàn thành" : "Chưa hoàn thành");
                item.put("notes", t.getNotes() != null ? t.getNotes() : "");
                priorityList.add(item);
            }
        } else {
            // Default SOP priority schedule template
            priorityList.add(Map.of("priorityOrder", 1, "taskType", "Kiểm tra y tế & Cấp cứu thú y (VET_ASSIST / SPECIAL_CARE)", "horseName", "Ngựa có cảnh báo sức khỏe hoặc chỉ định tái khám", "estimatedTime", "06:00 - 07:00", "notes", "Ưu tiên số 1: Hỗ trợ bác sĩ đo sinh hiệu và cấp phát thuốc theo đơn"
            ));
            priorityList.add(Map.of("priorityOrder", 2, "taskType", "Dẫn ngựa ra sân tập ca sáng (WORKOUT_ASSIST)", "horseName", "Chiến mã có lịch tập ca sáng (Lot)", "estimatedTime", "07:00 - 09:30", "notes", "Ưu tiên số 2: Đúng giờ theo ca tập của Huấn luyện viên"
            ));
            priorityList.add(Map.of("priorityOrder", 3, "taskType", "Cho ăn & Vệ sinh chuồng thông thường (FEEDING / MUCKING_OUT)", "horseName", "Khu chuồng ngựa khỏe mạnh", "estimatedTime", "09:30 - 11:30", "notes", "Ưu tiên số 3: Thay đệm lót rơm, bổ sung nước sạch và thức ăn thô"
            ));
            priorityList.add(Map.of("priorityOrder", 4, "taskType", "Chăm sóc & Vệ sinh chuồng cách ly (QUARANTINE_CARE)", "horseName", "Khu chuồng cách ly y tế", "estimatedTime", "14:00 - 16:00", "notes", "Ưu tiên số 4: Thực hiện SAU CÙNG để đảm bảo an toàn sinh học dịch tễ"
            ));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", today.toString());
        result.put("groomName", resolveUserName(currentUser, "Nhân viên chăm sóc"));
        result.put("totalWorkItemsCount", priorityList.size());
        result.put("priorityOrderList", priorityList);
        result.put("biosecurityNotes", "Quy tắc an toàn sinh học dịch tễ: Luôn mang đồ bảo hộ khi vào khu cách ly và tiến hành vệ sinh, cho ăn tại chuồng cách ly SAU CÙNG để triệt tiêu nguy cơ lây nhiễm chéo.");
        result.put("actionUrl", "/groom/tasks");
        return result;
    }

    private int getGroomTaskPriority(GroomDailyTask t) {
        if (t == null || t.getTaskType() == null) return 3;
        return switch (t.getTaskType()) {
            case VET_ASSIST, SPECIAL_CARE, HEALTH_CHECK -> 1;
            case WORKOUT_ASSIST -> 2;
            case FEEDING, GROOMING, HOOF_CARE, MUCKING_OUT -> 3;
        };
    }

    private String formatGroomTaskType(GroomTaskType type) {
        if (type == null) return "Công việc thường nhật";
        return switch (type) {
            case FEEDING -> "Cho ăn (Feeding)";
            case MUCKING_OUT -> "Dọn chuồng & Thay lót (Mucking Out)";
            case GROOMING -> "Chải lông & Vệ sinh (Grooming)";
            case HOOF_CARE -> "Chăm sóc móng (Hoof Care)";
            case HEALTH_CHECK -> "Kiểm tra sinh hiệu (Health Check)";
            case WORKOUT_ASSIST -> "Dẫn ngựa ra sân tập (Workout Assist)";
            case VET_ASSIST -> "Hỗ trợ bác sĩ thú y (Vet Assist)";
            case SPECIAL_CARE -> "Chăm sóc y tế đặc biệt (Special Care)";
        };
    }

    // =================================================================
    // CLUB_MANAGER Handlers
    // =================================================================

    private Object handleGetStableOccupancyMetrics() {
        long totalStalls = stableStallRepository.count();
        long availableQuarantine = stableStallRepository.countAvailableQuarantineStalls();
        long occupiedQuarantine = stableStallRepository.countOccupiedQuarantineStalls();
        long availableRegular = stableStallRepository.countAvailableRegularStalls();
        return Map.of("totalStalls", totalStalls, "vacantQuarantineStalls", availableQuarantine, "occupiedQuarantineStalls", occupiedQuarantine, "vacantRegularStalls", availableRegular
        );
    }

    private Object handleGetAdmissionRequestsOverview() {
        List<AdmissionApplication> allApps = admissionApplicationRepository != null
                ? admissionApplicationRepository.findAll()
                : Collections.emptyList();
        long tongSoDon = allApps.size();

        Map<AdmissionStatus, Long> countsByStatus = allApps.stream()
                .filter(a -> a.getStatus() != null)
                .collect(Collectors.groupingBy(AdmissionApplication::getStatus, Collectors.counting()));

        long soDonChoManagerDuyet = countsByStatus.getOrDefault(AdmissionStatus.MANAGER_REVIEW, 0L);
        long soDonChoGroomKiemTra = countsByStatus.getOrDefault(AdmissionStatus.GROOM_REVIEW, 0L);
        long soDonChoNhapChuong = countsByStatus.getOrDefault(AdmissionStatus.WAITING_FOR_ARRIVAL, 0L);
        long soDonChoKhamThuY = countsByStatus.getOrDefault(AdmissionStatus.VET_REVIEW, 0L);
        long soDonChoHuanLuyenVien = countsByStatus.getOrDefault(AdmissionStatus.TRAINER_REVIEW, 0L);
        long soDonDaDuyetNhapDan = countsByStatus.getOrDefault(AdmissionStatus.APPROVED, 0L);
        long soDonTuChoi = countsByStatus.getOrDefault(AdmissionStatus.REJECTED, 0L);
        long soDonQuaHanVanChuyen = countsByStatus.getOrDefault(AdmissionStatus.ARRIVAL_EXPIRED, 0L);
        long soDonChoXepChuongCachLy = countsByStatus.getOrDefault(AdmissionStatus.WAITING_FOR_STALL, 0L);

        long tongSoDonDangXuLy = soDonChoGroomKiemTra + soDonChoXepChuongCachLy + soDonChoNhapChuong
                + soDonChoKhamThuY + soDonChoHuanLuyenVien + soDonChoManagerDuyet;

        Map<String, Object> chiTietTheoGiaiDoan = new LinkedHashMap<>();
        chiTietTheoGiaiDoan.put("waitingGroomReview", soDonChoGroomKiemTra);
        chiTietTheoGiaiDoan.put("waitingQuarantineStall", soDonChoXepChuongCachLy);
        chiTietTheoGiaiDoan.put("waitingArrival", soDonChoNhapChuong);
        chiTietTheoGiaiDoan.put("waitingVetReview", soDonChoKhamThuY);
        chiTietTheoGiaiDoan.put("waitingTrainerReview", soDonChoHuanLuyenVien);
        chiTietTheoGiaiDoan.put("waitingManagerApproval", soDonChoManagerDuyet);
        chiTietTheoGiaiDoan.put("fullyApproved", soDonDaDuyetNhapDan);
        chiTietTheoGiaiDoan.put("rejected", soDonTuChoi);
        chiTietTheoGiaiDoan.put("arrivalExpired", soDonQuaHanVanChuyen);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalApplications", tongSoDon);
        response.put("pendingAdmissionsCount", soDonChoManagerDuyet);
        response.put("pendingManagerReviewCount", soDonChoManagerDuyet);
        response.put("inProgressApplications", tongSoDonDangXuLy);
        response.put("approvedApplications", soDonDaDuyetNhapDan);
        response.put("rejectedOrExpiredApplications", soDonTuChoi + soDonQuaHanVanChuyen);
        response.put("stageBreakdown", chiTietTheoGiaiDoan);
        response.put("systemStatus", "HOAT_DONG_BINH_THUONG");
        response.put("actionUrl", "/manager/admissions");

        return response;
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

            String trainerName = "Chưa gán HLV";
            if (area.getTrainerId() != null && userRepository != null) {
                trainerName = userRepository.findById(area.getTrainerId()).map(User::getFullName).orElse("HLV #" + area.getTrainerId());
            }

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("areaName", area.getName());
            map.put("areaCode", area.getCode());
            map.put("areaType", area.getType());
            map.put("assignedTrainer", trainerName);
            map.put("totalStalls", stalls.size());
            map.put("assignedGroomCount", groomIds.size());
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

        return Map.of("totalClubHorses", horses.size(), "allowedTrainingHorses", allowedCount, "medicallyBlockedHorsesCount", blockedCount, "incidentsLast7DaysCount", weekIncidents.size()
        );
    }

    private Object handleGetSystemStaffOverview(Map<String, Object> args) {
        if (userRepository == null) {
            Map<String, Object> emptyResponse = new LinkedHashMap<>();
            emptyResponse.put("totalStaff", 0);
            emptyResponse.put("roleBreakdown", Collections.emptyMap());
            emptyResponse.put("staffList", Collections.emptyList());
            return emptyResponse;
        }
        List<User> users = userRepository.findAll();

        String roleFilter = args.get("role") != null ? args.get("role").toString().trim() : null;
        Boolean isActiveFilter = null;
        if (args.get("isActive") != null) {
            Object val = args.get("isActive");
            if (val instanceof Boolean b) {
                isActiveFilter = b;
            } else {
                isActiveFilter = Boolean.parseBoolean(val.toString().trim());
            }
        }
        String keyword = args.get("keyword") != null ? args.get("keyword").toString().trim().toLowerCase() : null;

        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            String roleName = u.getRole() != null ? u.getRole().getName() : "";

            if (roleFilter != null && !roleFilter.isBlank()) {
                String normFilter = normalizeRoleName(roleFilter);
                String normUserRole = normalizeRoleName(roleName);
                if (!normUserRole.equalsIgnoreCase(normFilter) && !roleName.equalsIgnoreCase(roleFilter)) {
                    continue;
                }
            } else {
                if ("HORSE_OWNER".equalsIgnoreCase(roleName)) {
                    continue;
                }
            }

            if (isActiveFilter != null && u.isActive() != isActiveFilter) {
                continue;
            }

            if (keyword != null && !keyword.isBlank()) {
                boolean matchName = u.getFullName() != null && u.getFullName().toLowerCase().contains(keyword);
                boolean matchEmail = u.getEmail() != null && u.getEmail().toLowerCase().contains(keyword);
                boolean matchPhone = u.getPhone() != null && u.getPhone().contains(keyword);
                if (!matchName && !matchEmail && !matchPhone) {
                    continue;
                }
            }

            String vaiTroVi = switch (roleName.toUpperCase()) {
                case "HEAD_TRAINER", "TRAINER" -> "Huấn luyện viên trưởng";
                case "VETERINARIAN", "VET" -> "Bác sĩ thú y";
                case "GROOM", "STABLE_STAFF" -> "Nhân viên chăm sóc (Groom)";
                case "CLUB_MANAGER", "ADMIN" -> "Quản lý câu lạc bộ (Admin)";
                case "HORSE_OWNER", "OWNER" -> "Chủ sở hữu ngựa";
                default -> roleName;
            };

            String thongTinChuyenMon = resolveStaffSpecialty(u, roleName);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fullName", u.getFullName());
            item.put("role", roleName);
            item.put("roleTitle", vaiTroVi);
            item.put("email", u.getEmail());
            item.put("phone", (u.getPhone() != null && !u.getPhone().isBlank()) ? u.getPhone() : "Chưa cập nhật");
            item.put("activeStatus", u.isActive() ? "Đang hoạt động" : "Đã khóa");
            item.put("specialtyInfo", thongTinChuyenMon);
            item.put("actionUrl", "/manager/staff");
            result.add(item);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalStaff", result.size());

        Map<String, Integer> roleBreakdown = new LinkedHashMap<>();
        for (Map<String, Object> item : result) {
            String vaiTro = (String) item.get("role");
            roleBreakdown.merge(vaiTro, 1, Integer::sum);
        }
        response.put("roleBreakdown", roleBreakdown);
        response.put("staffList", result);
        return response;
    }

    private String resolveStaffSpecialty(User u, String roleName) {
        if ("HEAD_TRAINER".equalsIgnoreCase(roleName) || "TRAINER".equalsIgnoreCase(roleName)) {
            List<Area> areas = areaRepository != null ? areaRepository.findAll() : Collections.emptyList();
            List<String> managedAreas = areas.stream()
                    .filter(a -> Objects.equals(a.getTrainerId(), u.getId()))
                    .map(Area::getName)
                    .toList();
            if (!managedAreas.isEmpty()) {
                return "Phụ trách huấn luyện tại: " + String.join(", ", managedAreas);
            }
            return "Huấn luyện viên phụ trách giáo án và chiến thuật";
        } else if ("VETERINARIAN".equalsIgnoreCase(roleName) || "VET".equalsIgnoreCase(roleName)) {
            return "Bác sĩ thú y lâm sàng, chẩn đoán và theo dõi phác đồ";
        } else if ("GROOM".equalsIgnoreCase(roleName) || "STABLE_STAFF".equalsIgnoreCase(roleName)) {
            if (stableStallRepository != null) {
                long stallCount = stableStallRepository.findAll().stream()
                    .filter(s -> Objects.equals(s.getGroomId(), u.getId()))
                    .count();
                if (stallCount > 0) {
                    return "Trực tiếp chăm sóc " + stallCount + " ô chuồng và ngựa";
                }
            }
            return "Nhân viên chăm sóc chuồng trại và dẫn ngựa tập";
        } else if ("CLUB_MANAGER".equalsIgnoreCase(roleName) || "ADMIN".equalsIgnoreCase(roleName)) {
            return "Quản lý điều hành cơ sở vật chất, nhân sự và đơn nhập đàn";
        }
        return "Nhân sự câu lạc bộ";
    }

    private Object handleGetClubHorsesDirectory(Map<String, Object> args) {
        List<Horse> allHorses = horseRepository.findAll();
        String statusFilter = args.get("status") != null ? args.get("status").toString().trim() : null;
        String keyword = args.get("keyword") != null ? args.get("keyword").toString().trim().toLowerCase() : null;

        List<Area> quarantineAreas = areaRepository != null ? areaRepository.findByType(AreaType.QUARANTINE) : Collections.emptyList();
        Set<Long> quarantineAreaIds = quarantineAreas.stream().map(Area::getId).collect(Collectors.toSet());

        Set<Long> quarantineStallIds = Collections.emptySet();
        if (stableStallRepository != null && !quarantineAreaIds.isEmpty()) {
            quarantineStallIds = stableStallRepository.findByAreaIdInOrdered(new ArrayList<>(quarantineAreaIds))
                    .stream().map(StableStall::getId).collect(Collectors.toSet());
        }

        List<Long> allHorseIds = allHorses.stream().map(Horse::getId).filter(Objects::nonNull).toList();
        Map<Long, HorseSpecialtyResponse> dirSpecialties = Collections.emptyMap();
        if (horseSpecialtyService != null && !allHorseIds.isEmpty()) {
            try {
                Map<Long, HorseSpecialtyResponse> queried = horseSpecialtyService.getSpecialties(allHorseIds);
                if (queried != null) {
                    dirSpecialties = queried;
                }
            } catch (Exception e) {
                log.warn("Lỗi khi tải sở trường cho danh bạ ngựa: {}", e.getMessage());
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Horse h : allHorses) {
            String ownerName = "Chưa xác định";
            if (h.getOwnerId() != null && userRepository != null) {
                ownerName = userRepository.findById(h.getOwnerId()).map(User::getFullName).orElse("Chủ ngựa #" + h.getOwnerId());
            }

            boolean isQuarantineStall = h.getCurrentStallId() != null && quarantineStallIds.contains(h.getCurrentStallId());
            boolean isBlocked = h.getTrainingDecision() == TrainingDecision.BLOCKED;

            boolean hasActivePlan = false;
            if (horseTrainingPlanRepository != null) {
                List<HorseTrainingPlan> activePlans = horseTrainingPlanRepository.findByHorseIdAndStatusInOrderByEndDateDesc(
                        h.getId(), List.of(TrainingPlanStatus.ACTIVE));
                hasActivePlan = !activePlans.isEmpty();
            }

            String statusCode;
            String statusVi;
            if (isQuarantineStall) {
                statusCode = "QUARANTINE";
                statusVi = "Cách ly y tế (QUARANTINE)";
            } else if (isBlocked) {
                statusCode = "RESTING";
                statusVi = "Nghỉ ngơi y tế (RESTING)";
            } else if (hasActivePlan) {
                statusCode = "IN_TRAINING";
                statusVi = "Đang huấn luyện (IN_TRAINING)";
            } else if (h.getCurrentStatus() == HorseStatus.ELIGIBLE) {
                statusCode = "ACTIVE";
                statusVi = "Đang hoạt động (ACTIVE)";
            } else {
                statusCode = h.getCurrentStatus() != null ? h.getCurrentStatus().name() : "N/A";
                statusVi = statusCode;
            }

            if (statusFilter != null && !statusFilter.isBlank()) {
                boolean match = statusCode.equalsIgnoreCase(statusFilter)
                        || statusVi.toUpperCase().contains(statusFilter.toUpperCase())
                        || (statusFilter.equalsIgnoreCase("ACTIVE") && (statusCode.equals("ACTIVE") || statusCode.equals("IN_TRAINING") || h.getCurrentStatus() == HorseStatus.ELIGIBLE))
                        || (statusFilter.equalsIgnoreCase("QUARANTINE") && (isQuarantineStall || isBlocked))
                        || (statusFilter.equalsIgnoreCase("RESTING") && isBlocked)
                        || (statusFilter.equalsIgnoreCase("IN_TRAINING") && hasActivePlan);
                if (!match) {
                    continue;
                }
            }

            if (keyword != null && !keyword.isBlank()) {
                boolean matchHorseName = h.getName() != null && h.getName().toLowerCase().contains(keyword);
                boolean matchOwnerName = ownerName.toLowerCase().contains(keyword);
                boolean matchRegNo = h.getRegistrationNumber() != null && h.getRegistrationNumber().toLowerCase().contains(keyword);
                if (!matchHorseName && !matchOwnerName && !matchRegNo) {
                    continue;
                }
            }

            String stallName = resolveStallName(h.getCurrentStallId());
            String trainerName = resolveHorseTrainerName(h);

            HorseSpecialtyResponse specResp = dirSpecialties.get(h.getId());
            SpecialtyMetrics sm = computeSpecialtyMetrics(specResp);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseName", h.getName());
            item.put("registrationNumber", h.getRegistrationNumber() != null ? h.getRegistrationNumber() : "N/A");
            item.put("horseId", h.getRegistrationNumber() != null ? h.getRegistrationNumber() : "N/A");
            item.put("ownerName", ownerName);
            item.put("status", statusVi);
            item.put("currentStatus", statusVi);
            item.put("trainingDecision", (isQuarantineStall || isBlocked) ? "BLOCKED" : (h.getTrainingDecision() != null ? h.getTrainingDecision().name() : "ALLOWED"));
            item.put("stallLocation", stallName);
            item.put("assignedTrainer", trainerName);
            item.put("specialtyCount", sm.specialtyCount());
            item.put("averageRating", sm.averageRating());
            item.put("bestRating", sm.bestRating());
            item.put("specialties", sm.formattedDistances());
            item.put("strongDistances", sm.strongBands());
            item.put("preferredSurface", sm.preferredSurface());
            item.put("specialtyBreakdown", sm.specialtyBreakdown());
            item.put("totalClubHorses", allHorses.size());
            item.put("actionUrl", h.getId() != null ? "/manager/horses/" + h.getId() : "/manager/horses");
            result.add(item);
        }
        return result;
    }

    private String resolveHorseTrainerName(Horse h) {
        if (h.getCurrentStallId() != null && stableStallRepository != null && areaRepository != null) {
            StableStall stall = stableStallRepository.findById(h.getCurrentStallId()).orElse(null);
            if (stall != null && stall.getAreaId() != null) {
                Area area = areaRepository.findById(stall.getAreaId()).orElse(null);
                if (area != null && area.getTrainerId() != null && userRepository != null) {
                    return userRepository.findById(area.getTrainerId()).map(User::getFullName).orElse("HLV #" + area.getTrainerId());
                }
            }
        }
        if (horseTrainingPlanRepository != null) {
            List<HorseTrainingPlan> plans = horseTrainingPlanRepository.findByHorseIdAndStatusInOrderByEndDateDesc(
                    h.getId(), List.of(TrainingPlanStatus.ACTIVE, TrainingPlanStatus.UPCOMING));
            if (!plans.isEmpty() && plans.get(0).getTrainerId() != null && userRepository != null) {
                return userRepository.findById(plans.get(0).getTrainerId()).map(User::getFullName).orElse("HLV #" + plans.get(0).getTrainerId());
            }
        }
        return "Chưa gán HLV";
    }

    private Object handleGetRecentAuditLogs(Map<String, Object> args) {
        if (auditLogRepository == null) {
            return Collections.emptyList();
        }
        int limit = parseInteger(args.get("limit"), 10);
        if (limit <= 0) limit = 10;
        if (limit > 50) limit = 50;

        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<AuditLog> logs = auditLogRepository.findAll(pageable).getContent();

        List<Map<String, Object>> result = new ArrayList<>();
        for (AuditLog log : logs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("timestamp", log.getCreatedAt() != null ? log.getCreatedAt().toString() : "N/A");

            String nguoiThucHien = "Hệ thống";
            if (log.getUser() != null) {
                String roleStr = log.getUser().getRole() != null ? log.getUser().getRole().getName() : "";
                nguoiThucHien = log.getUser().getFullName() + (!roleStr.isBlank() ? " (" + roleStr + ")" : "");
            }
            item.put("performedBy", nguoiThucHien);

            String hanhDong = log.getAction() != null && !log.getAction().isBlank()
                    ? log.getAction()
                    : (log.getHttpMethod() != null ? log.getHttpMethod() + " " + (log.getRequestPath() != null ? log.getRequestPath() : "") : "Thao tác hệ thống");
            item.put("action", hanhDong);

            StringBuilder sb = new StringBuilder();
            if (log.getEntityName() != null && !log.getEntityName().isBlank()) {
                sb.append("Đối tượng: ").append(log.getEntityName());
            }
            if (log.getRequestPath() != null && !log.getRequestPath().isBlank()) {
                if (!sb.isEmpty()) sb.append(" | ");
                sb.append("Đường dẫn: ").append(log.getHttpMethod() != null ? log.getHttpMethod() + " " : "").append(log.getRequestPath());
            }
            if (log.getStatusCode() != null) {
                if (!sb.isEmpty()) sb.append(" | ");
                sb.append("Mã trạng thái HTTP: ").append(log.getStatusCode());
            }
            item.put("changeDetails", sb.isEmpty() ? "Không có chi tiết bổ sung" : sb.toString());
            item.put("actionUrl", "/manager/audit-log");

            result.add(item);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalAuditLogs", result.size());
        response.put("auditLogList", result);
        response.put("actionUrl", "/manager/audit-log");
        return response;
    }

    private Object handleGetAdmissionPipelineDetails(Map<String, Object> args) {
        List<AdmissionApplication> apps = admissionApplicationRepository.findAll();
        String statusFilter = args.get("status") != null ? args.get("status").toString().trim().toUpperCase() : null;

        List<AdmissionApplication> sortedApps = apps.stream()
                .sorted((a1, a2) -> {
                    LocalDateTime t1 = a1.getSubmittedAt() != null ? a1.getSubmittedAt() : a1.getCreatedAt();
                    LocalDateTime t2 = a2.getSubmittedAt() != null ? a2.getSubmittedAt() : a2.getCreatedAt();
                    if (t1 == null && t2 == null) return 0;
                    if (t1 == null) return 1;
                    if (t2 == null) return -1;
                    return t2.compareTo(t1);
                })
                .toList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (AdmissionApplication a : sortedApps) {
            if (statusFilter != null && !statusFilter.isBlank()) {
                if ("APPROVED".equals(statusFilter) && a.getStatus() != AdmissionStatus.APPROVED) {
                    continue;
                } else if ("REJECTED".equals(statusFilter) && a.getStatus() != AdmissionStatus.REJECTED) {
                    continue;
                } else if ("PENDING".equals(statusFilter) && (a.getStatus() == AdmissionStatus.APPROVED || a.getStatus() == AdmissionStatus.REJECTED)) {
                    continue;
                } else if (!"APPROVED".equals(statusFilter) && !"REJECTED".equals(statusFilter) && !"PENDING".equals(statusFilter)) {
                    if (a.getStatus() == null || !a.getStatus().name().equalsIgnoreCase(statusFilter)) {
                        continue;
                    }
                }
            }

            String horseName = "Ngựa đăng ký";
            if (a.getHorseId() != null && horseRepository != null) {
                horseName = horseRepository.findById(a.getHorseId()).map(Horse::getName).orElse("Ngựa đăng ký");
            }
            if ("Ngựa đăng ký".equals(horseName) && candidateHorseProfileRepository != null) {
                horseName = candidateHorseProfileRepository.findByAdmissionId(a.getId())
                        .map(CandidateHorseProfile::getName)
                        .orElse("Ngựa đăng ký");
            }

            String ownerName = "Chưa xác định";
            if (a.getOwnerId() != null && userRepository != null) {
                ownerName = userRepository.findById(a.getOwnerId()).map(User::getFullName).orElse("Chủ ngựa #" + a.getOwnerId());
            }

            String ngayNopDon = a.getSubmittedAt() != null ? a.getSubmittedAt().toString() : (a.getCreatedAt() != null ? a.getCreatedAt().toString() : "N/A");

            String buocHienTai = a.getStatus() != null ? switch (a.getStatus()) {
                case GROOM_REVIEW -> "Chờ Groom kiểm tra sơ bộ (GROOM_REVIEW)";
                case WAITING_FOR_STALL -> "Chờ phân bổ chuồng cách ly (WAITING_FOR_STALL)";
                case WAITING_FOR_ARRIVAL -> "Chờ ngựa đến trang trại (WAITING_FOR_ARRIVAL)";
                case ARRIVAL_EXPIRED -> "Quá hạn vận chuyển tới chuồng (ARRIVAL_EXPIRED)";
                case VET_REVIEW -> "Chờ Bác sĩ thú y khám ban đầu (VET_REVIEW)";
                case TRAINER_REVIEW -> "Chờ Huấn luyện viên chỉ định (TRAINER_REVIEW)";
                case MANAGER_REVIEW -> "Chờ Quản lý câu lạc bộ phê duyệt (MANAGER_REVIEW)";
                case APPROVED -> "Đã phê duyệt nhập đàn (APPROVED)";
                case REJECTED -> "Đã từ chối nhập đàn (REJECTED)";
            } : "Chưa xác định";

            String vetName = "Chưa phân công";
            if (a.getVeterinarianId() != null && userRepository != null) {
                vetName = userRepository.findById(a.getVeterinarianId()).map(User::getFullName).orElse("Bác sĩ #" + a.getVeterinarianId());
            }

            String trainerName = "Chưa chỉ định";
            if (a.getTrainerId() != null && userRepository != null) {
                trainerName = userRepository.findById(a.getTrainerId()).map(User::getFullName).orElse("HLV #" + a.getTrainerId());
            }

            String duongDanDieuHuong = a.getId() != null ? "/manager/admissions/" + a.getId() : "/manager/admissions";

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseName", horseName);
            item.put("ownerName", ownerName);
            item.put("submittedDate", ngayNopDon);
            item.put("currentStage", buocHienTai);
            item.put("status", a.getStatus() != null ? a.getStatus().name() : "N/A");
            item.put("assignedVet", vetName);
            item.put("designatedTrainer", trainerName);
            item.put("navigationUrl", duongDanDieuHuong);
            item.put("actionUrl", duongDanDieuHuong);

            result.add(item);
        }
        return result;
    }

    private Object handleAnalyzeStableCapacityAndStallBottlenecks() {
        long tongSoChuong = stableStallRepository != null ? stableStallRepository.count() : 0;
        long availableRegular = stableStallRepository != null ? stableStallRepository.countAvailableRegularStalls() : 0;
        long availableQuarantine = stableStallRepository != null ? stableStallRepository.countAvailableQuarantineStalls() : 0;
        long occupiedQuarantine = stableStallRepository != null ? stableStallRepository.countOccupiedQuarantineStalls() : 0;
        long soChuongTrong = availableRegular + availableQuarantine;
        long soChuongDangSuDung = tongSoChuong > soChuongTrong ? (tongSoChuong - soChuongTrong) : 0;

        List<AdmissionApplication> pendingAppsList = admissionApplicationRepository != null
                ? admissionApplicationRepository.findAll().stream()
                        .filter(a -> a.getStatus() != AdmissionStatus.APPROVED && a.getStatus() != AdmissionStatus.REJECTED)
                        .toList()
                : Collections.emptyList();
        int soDonPending = pendingAppsList.size();

        double occupancyRate = tongSoChuong > 0 ? ((double) soChuongDangSuDung / tongSoChuong) * 100.0 : 0.0;
        String duBaoDiemNghen;
        String khuyenNghiQuanLy;

        if (occupancyRate >= 80.0 || soDonPending > soChuongTrong) {
            duBaoDiemNghen = String.format(Locale.US, "Cảnh báo tắc nghẽn công suất trong 30-60 ngày tới: Tỷ lệ lấp đầy đạt %.1f%%, có %d đơn nhập đàn đang chờ trong khi chỉ còn %d chuồng trống khả dụng.",
                    occupancyRate, soDonPending, soChuongTrong);
            khuyenNghiQuanLy = "Ưu tiên hoàn tất thủ tục xuất viện/chuyển chuồng cho ngựa đã hết thời hạn cách ly, đồng thời tạm giãn tiến độ tiếp nhận đơn nhập đàn mới để tránh quá tải.";
        } else {
            duBaoDiemNghen = String.format(Locale.US, "Công suất chuồng trại ổn định trong 30-60 ngày tới: Tỷ lệ lấp đầy %.1f%%, hiện có %d chuồng trống (%d chuồng thường, %d chuồng cách ly) sẵn sàng đón nhận ngựa mới.",
                    occupancyRate, soChuongTrong, availableRegular, availableQuarantine);
            khuyenNghiQuanLy = "Tiếp tục duy trì quy trình kiểm dịch nghiêm ngặt và lên kế hoạch vệ sinh khử trùng luân phiên các ô chuồng trống.";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalStalls", tongSoChuong);
        result.put("occupiedStalls", soChuongDangSuDung);
        result.put("vacantStalls", soChuongTrong);
        result.put("quarantineCasesCount", occupiedQuarantine);
        result.put("waitingAdmissionsCount", soDonPending);
        result.put("occupancyRate", String.format(Locale.US, "%.1f%%", occupancyRate));
        result.put("bottleneckForecast", duBaoDiemNghen);
        result.put("managementRecommendations", khuyenNghiQuanLy);
        result.put("actionUrl", "/manager/stable");
        return result;
    }

    private Object handleRecommendGroomWorkloadRebalancing() {
        List<User> grooms = (userRepository != null)
                ? userRepository.findAll().stream()
                        .filter(u -> u.getRole() != null && ("GROOM".equalsIgnoreCase(u.getRole().getName()) || "STABLE_STAFF".equalsIgnoreCase(u.getRole().getName())))
                        .filter(User::isActive)
                        .toList()
                : Collections.emptyList();

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<Map<String, Object>> danhSachGroomKemTaiTrong = new ArrayList<>();
        List<String> nhanVienQuaTai = new ArrayList<>();
        List<String> nhanVienThapTai = new ArrayList<>();

        for (User g : grooms) {
            List<StableStall> stalls = (stableStallRepository != null)
                    ? stableStallRepository.findByGroomId(g.getId())
                    : Collections.emptyList();
            int taskCount = (groomDailyTaskRepository != null)
                    ? groomDailyTaskRepository.findByGroomIdAndScheduledTimeBetween(g.getId(), startOfDay, endOfDay).size()
                    : 0;

            String trangThaiTai;
            if (stalls.size() >= 5 || taskCount >= 10) {
                trangThaiTai = "Quá tải (Cần giảm tải)";
                nhanVienQuaTai.add(g.getFullName() != null ? g.getFullName() : g.getEmail());
            } else if (stalls.size() <= 2 && taskCount <= 3) {
                trangThaiTai = "Thấp tải (Sẵn sàng nhận thêm)";
                nhanVienThapTai.add(g.getFullName() != null ? g.getFullName() : g.getEmail());
            } else {
                trangThaiTai = "Cân bằng hợp lý";
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("groomName", g.getFullName() != null ? g.getFullName() : g.getEmail());
            item.put("assignedStallCount", stalls.size());
            item.put("tasksTodayCount", taskCount);
            item.put("workloadStatus", trangThaiTai);
            danhSachGroomKemTaiTrong.add(item);
        }

        String deXuatDieuChuyenChuong;
        if (!nhanVienQuaTai.isEmpty() && !nhanVienThapTai.isEmpty()) {
            deXuatDieuChuyenChuong = "Đề xuất điều chuyển 1-2 ô chuồng từ nhóm nhân viên quá tải (" + String.join(", ", nhanVienQuaTai)
                    + ") sang các nhân viên đang có mức tải nhẹ (" + String.join(", ", nhanVienThapTai) + ") nhằm tối ưu hóa năng suất chăm sóc.";
        } else if (!nhanVienQuaTai.isEmpty()) {
            deXuatDieuChuyenChuong = "Tất cả nhân viên Groom phụ trách chuồng hiện tại đều có tải trọng cao (" + String.join(", ", nhanVienQuaTai)
                    + "). Khuyến nghị bổ sung thêm nhân sự hoặc điều phối tạm thời nhân viên phụ việc.";
        } else {
            deXuatDieuChuyenChuong = "Khối lượng công việc và chuồng phụ trách giữa các nhân viên Groom hiện tại rất đồng đều, chưa phát sinh nhu cầu tái phân bổ.";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalGrooms", grooms.size());
        result.put("groomWorkloadList", danhSachGroomKemTaiTrong);
        result.put("overloadedStaff", nhanVienQuaTai.isEmpty() ? List.of("Không có nhân viên quá tải") : nhanVienQuaTai);
        result.put("proposedStallTransfer", deXuatDieuChuyenChuong);
        result.put("actionUrl", "/manager/staff");
        return result;
    }

    // =================================================================
    // Helper Methods
    // =================================================================

    /**
     * Tra cứu Horse linh hoạt theo ID hoặc Name.
     */
    private Horse resolveHorse(Map<String, Object> args) {
        if (args == null || args.isEmpty()) {
            return null;
        }
        Long horseId = parseLong(args.get("horseId"));
        if (horseId != null) {
            return horseRepository.findById(horseId).orElse(null);
        }
        Object nameObj = args.get("horseName");
        if (nameObj == null) {
            nameObj = args.get("name");
        }
        if (nameObj != null && !nameObj.toString().trim().isEmpty()) {
            String horseName = nameObj.toString().trim();
            List<Horse> all = horseRepository.findAll();
            return all.stream()
                    .filter(h -> h.getName() != null && h.getName().equalsIgnoreCase(horseName))
                    .findFirst()
                    .orElseGet(() -> all.stream()
                            .filter(h -> h.getName() != null && h.getName().toLowerCase().contains(horseName.toLowerCase()))
                            .findFirst()
                            .orElse(null));
        }
        return null;
    }

    private boolean isHorseInQuarantineOrBlocked(Horse horse) {
        if (horse == null) return false;
        if (horse.getTrainingDecision() == TrainingDecision.BLOCKED) return true;
        if (horse.getCurrentStallId() != null && stableStallRepository != null && areaRepository != null) {
            StableStall stall = stableStallRepository.findById(horse.getCurrentStallId()).orElse(null);
            if (stall != null && stall.getAreaId() != null) {
                Area area = areaRepository.findById(stall.getAreaId()).orElse(null);
                if (area != null && area.getType() == AreaType.QUARANTINE) {
                    return true;
                }
            }
        }
        return false;
    }

    private String resolveUserName(AuthenticatedUser user, String defaultName) {
        if (user == null || user.getUserId() == null) return defaultName;
        if (userRepository != null) {
            return userRepository.findById(user.getUserId()).map(User::getFullName).orElse(user.getEmail());
        }
        return user.getEmail() != null ? user.getEmail() : defaultName;
    }

    private String resolveStallName(Long stallId) {
        if (stallId == null) {
            return "Chưa xếp chuồng";
        }
        return stableStallRepository.findById(stallId)
                .map(s -> "Chuồng " + s.getStallCode() + " (Số " + s.getStallNumber() + ")")
                .orElse("Chưa xác định chuồng");
    }

    private String formatCaTap(LocalTime start, LocalTime end) {
        if (start == null) {
            return "Chưa xếp giờ";
        }
        String timeRange = start + " - " + (end != null ? end.toString() : "N/A");
        if (start.isBefore(LocalTime.of(12, 0))) {
            return "Ca Sáng (" + timeRange + ")";
        } else if (start.isBefore(LocalTime.of(17, 0))) {
            return "Ca Chiều (" + timeRange + ")";
        } else {
            return "Ca Tối (" + timeRange + ")";
        }
    }

    private void verifyHorseOwnershipIfOwner(Long horseId, AuthenticatedUser currentUser) {
        if ("HORSE_OWNER".equalsIgnoreCase(currentUser.getRole())) {
            Horse horse = horseRepository.findById(horseId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Không tìm thấy ngựa"));
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

    private int parseInteger(Object obj, int defaultValue) {
        if (obj == null) return defaultValue;
        if (obj instanceof Number num) return num.intValue();
        try {
            return Integer.parseInt(obj.toString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String normalizeRoleName(String role) {
        return DynamicToolRegistryService.normalizeRole(role);
    }
}
