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
import com.rtms.backend.training.repository.RacingReadinessAssessmentRepository;
import com.rtms.backend.training.repository.TrainingLotRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import com.rtms.backend.training.service.HorsePerformanceService;
import com.rtms.backend.training.service.HorseSpecialtyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                                  @Autowired(required = false) CourseRepository courseRepository) {
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
                null, null);
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
            case "getTrainerAssignedGrooms" -> handleGetTrainerAssignedGrooms(currentUser);

            // VETERINARIAN Tools
            case "searchClinicalKnowledge" -> handleSearchClinicalKnowledge(args, currentUser);
            case "getVetExaminationOverview" -> handleGetVetExaminationOverview();
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
            case "getHorseStallLocation" -> handleGetHorseStallLocation(args);

            // CLUB_MANAGER Tools
            case "getStableOccupancyMetrics" -> handleGetStableOccupancyMetrics();
            case "getAdmissionRequestsOverview" -> handleGetAdmissionRequestsOverview();
            case "getTrainerAreaDistribution" -> handleGetTrainerAreaDistribution();
            case "getClubHealthOverview" -> handleGetClubHealthOverview();

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
            item.put("tenNgua", h.getName());
            item.put("maDangKy", h.getRegistrationNumber() != null ? h.getRegistrationNumber() : "N/A");
            item.put("trangThai", h.getCurrentStatus());
            item.put("quyetDinhTapLuyen", h.getTrainingDecision());
            item.put("viTriChuong", resolveStallName(h.getCurrentStallId()));
            result.add(item);
        }
        return result;
    }

    private Object handleGetMyHorseHealthSummary(Map<String, Object> args, AuthenticatedUser currentUser) {
        Horse horse = resolveHorse(args);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Cần cung cấp ID hoặc tên ngựa");
        }
        verifyHorseOwnershipIfOwner(horse.getId(), currentUser);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("tenNgua", horse.getName());
        summary.put("maDangKy", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        summary.put("trangThaiHienTai", horse.getCurrentStatus());
        summary.put("quyetDinhTapLuyen", horse.getTrainingDecision());
        summary.put("lyDoQuyetDinh", horse.getTrainingDecisionReason() != null ? horse.getTrainingDecisionReason() : "Bình thường");
        summary.put("viTriChuong", resolveStallName(horse.getCurrentStallId()));

        List<HealthRecord> records = healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(horse.getId());
        if (!records.isEmpty()) {
            HealthRecord latest = records.get(0);
            summary.put("chanDoanMoiNhat", latest.getDiagnosis());
            summary.put("phacDoDieuTri", latest.getTreatment());
            summary.put("ghiChuHanCheYTe", latest.getRestrictionDetails());
            summary.put("ngayKhamGanNhat", latest.getExaminedAt());
        } else {
            summary.put("chanDoanMoiNhat", "Chưa có bệnh án ghi nhận");
        }

        List<CareSchedule> activeSchedules = careScheduleRepository.findByHorseIdAndStatusIn(
                horse.getId(), List.of(CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS));
        List<Map<String, Object>> careList = activeSchedules.stream().map(cs -> Map.<String, Object>of(
                "loaiChamSoc", cs.getCareType() != null ? cs.getCareType().name() : "Khám tổng quát",
                "thoiGianHen", cs.getScheduledAt() != null ? cs.getScheduledAt().toString() : "N/A",
                "mucDich", cs.getPurpose() != null ? cs.getPurpose().name() : "N/A"
        )).toList();
        summary.put("lichChamSocSapToi", careList);

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
            return Map.of("thongBao", "Chiến mã " + horse.getName() + " hiện chưa có kế hoạch huấn luyện nào.");
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (PlanSummaryResponse p : summaries) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("tenKeHoach", p.getCourseName() != null ? p.getCourseName() : "Khóa huấn luyện");
            item.put("tenNgua", horse.getName());
            item.put("trangThai", p.getStatus() != null ? p.getStatus().name() : "N/A");
            item.put("tienDo", p.getCompletedSessions() + "/" + p.getTotalSessions() + " buổi");
            item.put("ngayBatDau", p.getStartDate() != null ? p.getStartDate().toString() : "N/A");
            item.put("ngayKetThuc", p.getEndDate() != null ? p.getEndDate().toString() : "N/A");
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
            item.put("caTap", formatCaTap(lot.startTime(), lot.endTime()));
            item.put("ngayTap", lot.lotDate() != null ? lot.lotDate().toString() : "N/A");
            item.put("gioBatDau", lot.startTime() != null ? lot.startTime().toString() : "N/A");
            item.put("gioKetThuc", lot.endTime() != null ? lot.endTime().toString() : "N/A");
            item.put("tenBaiTap", lot.subjectName());
            item.put("danhSachNguaCuaBan", namesByLot.getOrDefault(lot.lotId(), Collections.emptyList()));
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
            String horseName = "Ngựa đăng ký";
            if (a.getHorseId() != null) {
                horseName = horseRepository.findById(a.getHorseId()).map(Horse::getName).orElse("Ngựa đăng ký");
            }
            item.put("tenNgua", horseName);
            item.put("trangThaiDon", a.getStatus() != null ? a.getStatus().name() : "PENDING");
            item.put("ketQuaGroomKiemTra", a.getGroomDecision() != null ? a.getGroomDecision().name() : "Chờ đánh giá");
            item.put("ketQuaVetKham", a.getVetTrainingDecision() != null ? a.getVetTrainingDecision().name() : "Chờ đánh giá");
            item.put("quanLyPheDuyet", a.getManagerDecision() != null ? a.getManagerDecision().name() : "Chờ phê duyệt");
            item.put("ngayNopDon", a.getSubmittedAt() != null ? a.getSubmittedAt().toString() : "N/A");
            result.add(item);
        }
        return result;
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
        result.put("tenNgua", horse.getName());
        result.put("maDangKy", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        result.put("quyetDinhTapLuyen", horse.getTrainingDecision() != null ? horse.getTrainingDecision().name() : "N/A");
        result.put("lyDoQuyetDinh", horse.getTrainingDecisionReason() != null ? horse.getTrainingDecisionReason() : "Bình thường");
        result.put("tongSoBuoiTap", totalSessions);
        result.put("soBuoiHoanThanh", completedSessions);
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
            item.put("tenNgua", horse.getName());
            item.put("loaiChamSoc", cs.getCareType() != null ? cs.getCareType().name() : "Khám thú y");
            item.put("thoiGianHen", cs.getScheduledAt() != null ? cs.getScheduledAt().toString() : "N/A");
            item.put("trangThai", cs.getStatus() != null ? cs.getStatus().name() : "N/A");
            item.put("mucDich", cs.getPurpose() != null ? cs.getPurpose().name() : "N/A");
            result.add(item);
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
            item.put("tenNgua", h.getName());
            item.put("maDangKy", h.getRegistrationNumber() != null ? h.getRegistrationNumber() : "N/A");
            item.put("trangThai", h.getCurrentStatus());
            item.put("quyetDinhTapLuyen", h.getTrainingDecision());
            item.put("lyDoQuyetDinh", h.getTrainingDecisionReason() != null ? h.getTrainingDecisionReason() : "Bình thường");

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
            item.put("viTriChuong", stallName);
            item.put("tenGroomPhuTrach", groomName);
            item.put("soDienThoaiGroom", groomPhone);
            list.add(item);
        }
        return Map.of(
                "huanLuyenVien", resolveUserName(currentUser, "Huấn luyện viên trưởng"),
                "tongSoNguaQuanLy", list.size(),
                "danhSachNgua", list
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
            item.put("tenNgua", horse.getName());
            item.put("trangThai", w.getStatus());
            item.put("cuLyThucTeMet", w.getActualDistanceMeters());
            item.put("tocDoDinhKmh", w.getTopSpeedKmh());
            item.put("nhipTimTrungBinh", w.getAverageHeartRate());
            item.put("diemDanhGia", w.getPerformanceRating());
            item.put("nhanXetCuaHLV", w.getTrainerFeedback());
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
        return Map.of(
                "tenNgua", horse.getName(),
                "xuHuongTheLuc", trends != null ? trends : Collections.emptyList()
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
            item.put("caTap", formatCaTap(lot.startTime(), lot.endTime()));
            item.put("ngayTap", lot.lotDate() != null ? lot.lotDate().toString() : "Hôm nay");
            item.put("gioBatDau", lot.startTime() != null ? lot.startTime().toString() : "N/A");
            item.put("gioKetThuc", lot.endTime() != null ? lot.endTime().toString() : "N/A");
            item.put("tenBaiTap", lot.subjectName() != null ? lot.subjectName() : "Bài tập thể lực");
            item.put("soLuongNgua", lot.occupied());
            item.put("danhSachNgua", namesByLot.getOrDefault(lot.lotId(), Collections.emptyList()));
            result.add(item);
        }
        return result;
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
            item.put("tenKeHoach", p.getCourseName() != null ? p.getCourseName() : "Kế hoạch huấn luyện");
            item.put("tenNgua", p.getHorseName() != null ? p.getHorseName() : "Ngựa thi đấu");
            item.put("mucTieu", targetGoal);
            item.put("trangThai", trangThai);
            item.put("tienDo", (p.getCompletedSessions() != null ? p.getCompletedSessions() : 0) + "/"
                    + (p.getTotalSessions() != null ? p.getTotalSessions() : 0) + " buổi hoàn thành");
            item.put("soBuoiHoanThanh", p.getCompletedSessions());
            item.put("tongSoBuoi", p.getTotalSessions());
            item.put("ngayBatDau", p.getStartDate() != null ? p.getStartDate().toString() : "N/A");
            item.put("ngayKetThuc", p.getEndDate() != null ? p.getEndDate().toString() : "N/A");
            result.add(item);
        }
        return result;
    }

    private Object handleGetHorseSpecialtyAnalysis(Map<String, Object> args) {
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

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenNgua", horse.getName());
        result.put("maDangKy", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        result.put("cuLyToiUu", (resp != null && !resp.strongDistances().isEmpty()) ? resp.strongDistances() : List.of("Đang phân tích thêm cự ly"));
        result.put("matSanUaThich", matSanUaThich);
        result.put("thongKeCuLy", resp != null ? resp.distanceStats() : Collections.emptyList());
        return result;
    }

    private Object handleGetTrainerRiskAlerts(Map<String, Object> args, AuthenticatedUser currentUser) {
        Horse targetHorse = resolveHorse(args);
        Long horseId = targetHorse != null ? targetHorse.getId() : parseLong(args.get("horseId"));

        if (horseId != null) {
            Horse h = targetHorse != null ? targetHorse : horseRepository.findById(horseId).orElse(null);
            String hName = h != null ? h.getName() : "Ngựa #" + horseId;
            List<HorseAlertResponse> alerts = horsePerformanceService.getHorseAlerts(horseId, currentUser);
            return Map.of(
                    "tenNgua", hName,
                    "soLuongCanhBao", alerts.size(),
                    "danhSachCanhBao", alerts
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
                item.put("tenNgua", h.getName());
                item.put("soLuongCanhBao", alerts.size());
                item.put("danhSachCanhBao", alerts);
                aggregated.add(item);
            }
        }

        return Map.of(
                "huanLuyenVien", resolveUserName(currentUser, "Huấn luyện viên"),
                "soNguaCoNguyCo", aggregated.size(),
                "chiTietCanhBao", aggregated
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
            return Map.of(
                    "tenNgua", horse.getName(),
                    "thongBao", "Chưa có bản đánh giá sẵn sàng thi đấu nào cho chiến mã này."
            );
        }

        RacingReadinessAssessment r = opt.get();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenNgua", horse.getName());
        result.put("trangThaiSanSang", r.getReadinessStatus());
        result.put("diemTheLuc", r.getFitnessScore());
        result.put("diemNgoaiHinh", r.getConformationScore());
        result.put("diemTinhKhi", r.getTemperamentScore());
        result.put("diemDangChay", r.getGaitQualityScore());
        result.put("soThangUocTinhCoTheThiDau", r.getEstimatedMonthsToRace());
        result.put("ngayDanhGia", r.getAssessmentDate());
        result.put("giaTriDenNgay", r.getValidUntil());
        result.put("nhanXetChuyenMon", r.getRemarks());
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
        result.put("tenNgua", horseName);
        result.put("tenGroom", groomName);
        result.put("ngayKiemTra", fromDate.toString());
        result.put("coXungDotLich", !conflicts.isEmpty());
        result.put("soCaXungDot", conflicts.size());
        result.put("chiTietXungDot", conflicts);
        return result;
    }

    private Object handleGetTrainerAssignedGrooms(AuthenticatedUser currentUser) {
        Long trainerId = currentUser.getUserId();
        List<Area> areas = areaRepository.findAll().stream()
                .filter(a -> Objects.equals(a.getTrainerId(), trainerId))
                .toList();

        if (areas.isEmpty()) {
            return Map.of(
                    "thongBao", "Huấn luyện viên hiện chưa được phân công quản lý khu vực chuồng trại nào.",
                    "danhSachGroom", Collections.emptyList()
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
            String groomName = groomUser != null ? groomUser.getFullName() : "Groom #" + groomId;
            String groomPhone = groomUser != null && groomUser.getPhone() != null ? groomUser.getPhone() : "N/A";
            String groomEmail = groomUser != null ? groomUser.getEmail() : "N/A";

            List<StableStall> groomStalls = stalls.stream()
                    .filter(s -> Objects.equals(s.getGroomId(), groomId))
                    .toList();
            List<String> stallCodes = groomStalls.stream().map(StableStall::getStallCode).toList();
            List<String> horsesTakingCareOf = groomStalls.stream()
                    .map(s -> horseByStall.get(s.getId()))
                    .filter(Objects::nonNull)
                    .toList();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("tenGroom", groomName);
            item.put("soDienThoai", groomPhone);
            item.put("email", groomEmail);
            item.put("soChuongPhuTrach", groomStalls.size());
            item.put("danhSachMaChuong", stallCodes);
            item.put("danhSachNguaDangChamSoc", horsesTakingCareOf);
            groomList.add(item);
        }

        return Map.of(
                "tenHuanLuyenVien", resolveUserName(currentUser, "Huấn luyện viên"),
                "tongSoGroomLamViecCung", groomList.size(),
                "danhSachGroom", groomList
        );
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
            item.put("tenNgua", horseName);
            item.put("loaiKham", cs.getCareType() != null ? cs.getCareType().name() : "Khám tổng quát");
            item.put("mucDich", cs.getPurpose() != null ? cs.getPurpose().name() : "N/A");
            item.put("thoiGianHen", cs.getScheduledAt() != null ? cs.getScheduledAt().toString() : "N/A");
            item.put("moTa", cs.getDescription() != null ? cs.getDescription() : "");

            if (cs.getStatus() == CareScheduleStatus.IN_PROGRESS) {
                dangKham.add(item);
            } else if (cs.getStatus() == CareScheduleStatus.SCHEDULED) {
                chuaKham.add(item);
            } else if (cs.getStatus() == CareScheduleStatus.COMPLETED) {
                daKham.add(item);
            }
        }

        return Map.of(
                "tongSoLichKham", allSchedules.size(),
                "dangKham", dangKham,
                "soCaDangKham", dangKham.size(),
                "chuaKhamChuaDenLich", chuaKham,
                "soCaChuaKham", chuaKham.size(),
                "daKhamHoanThanh", daKham,
                "soCaDaKham", daKham.size()
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
            item.put("tenNgua", horse.getName());
            item.put("ngayKham", hr.getExaminedAt());
            item.put("trieuChung", hr.getSymptoms());
            item.put("ghiNhanLamSang", hr.getFindings());
            item.put("chanDoan", hr.getDiagnosis());
            item.put("phacDoDieuTri", hr.getTreatment());
            item.put("quyetDinhTapLuyen", hr.getTrainingDecision());
            item.put("chiTietHanChe", hr.getRestrictionDetails());
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
            item.put("tenNgua", horse.getName());
            item.put("loaiDieuTri", cs.getCareType() != null ? cs.getCareType().name() : "N/A");
            item.put("thoiGianHen", cs.getScheduledAt() != null ? cs.getScheduledAt().toString() : "N/A");
            item.put("mucDich", cs.getPurpose() != null ? cs.getPurpose().name() : "N/A");
            item.put("moTa", cs.getDescription() != null ? cs.getDescription() : "");
            result.add(item);
        }
        return result;
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
                    item.put("tenNgua", horseNames.getOrDefault(cs.getHorseId(), "Ngựa #" + cs.getHorseId()));
                    item.put("loaiChamSoc", cs.getCareType());
                    item.put("mucDich", cs.getPurpose());
                    item.put("trangThai", cs.getStatus());
                    item.put("thoiGianHen", cs.getScheduledAt());
                    item.put("ghiChu", cs.getDescription());
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
            item.put("tenNgua", h.getName());
            item.put("maDangKy", h.getRegistrationNumber() != null ? h.getRegistrationNumber() : "N/A");
            item.put("trangThaiHienTai", h.getCurrentStatus());
            // TUYỆT ĐỐI KHÔNG CONFLICT: Ngựa cách ly luôn luôn BLOCKED khỏi tập luyện
            item.put("quyetDinhTapLuyen", "BLOCKED");
            item.put("duocPhepTapLuyen", false);
            item.put("lyDoQuyetDinh", h.getTrainingDecisionReason() != null ? h.getTrainingDecisionReason() : "Đang thực hiện cách ly kiểm dịch nghiêm ngặt - Cấm tập luyện");

            boolean inQuarantineStall = h.getCurrentStallId() != null && qStallIds.contains(h.getCurrentStallId());
            item.put("oKhuChuongCachLy", inQuarantineStall);
            if (inQuarantineStall && stallMap.containsKey(h.getCurrentStallId())) {
                item.put("maChuong", stallMap.get(h.getCurrentStallId()).getStallCode());
            }

            List<HealthRecord> records = healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(h.getId());
            if (!records.isEmpty()) {
                HealthRecord latest = records.get(0);
                item.put("chanDoanMoiNhat", latest.getDiagnosis());
                item.put("chiTietHanChe", latest.getRestrictionDetails());
                item.put("ngayKhamGanNhat", latest.getExaminedAt());
            }

            result.add(item);
        }

        return Map.of(
                "tongSoNguaCachLyHoacCamTap", result.size(),
                "danhSachNguaCachLy", result
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
            item.put("tieuDeSuCo", r.getTitle());
            item.put("tenNgua", horseName);
            item.put("nguoiBaoCao", groomName);
            item.put("mucDoNghiemTrong", r.getSeverity());
            item.put("moTaChiTiet", r.getDescription());
            item.put("trangThaiXuLy", r.getStatus());
            item.put("thoiGianBaoCao", r.getReportedAt() != null ? r.getReportedAt().toString() : "N/A");
            result.add(item);
        }
        return Map.of(
                "tongSoBaoCaoTrong48h", result.size(),
                "danhSachSuCo", result
        );
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
                    "thongBao", "Hiện tại bạn chưa được phân công phụ trách chuồng nào.",
                    "danhSachChuongVaNgua", Collections.emptyList()
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
            item.put("maChuong", stall.getStallCode());
            item.put("soThuTuChuong", stall.getStallNumber());
            item.put("trangThaiChuong", stall.getStatus());

            Horse h = horseByStall.get(stall.getId());
            if (h != null) {
                item.put("tenNgua", h.getName());
                item.put("maDangKy", h.getRegistrationNumber() != null ? h.getRegistrationNumber() : "N/A");
                item.put("trangThaiNgua", h.getCurrentStatus());
                item.put("quyetDinhTapLuyen", h.getTrainingDecision());
            } else {
                item.put("nguaTrongChuong", "Chuồng đang trống");
            }
            horseList.add(item);
        }

        return Map.of(
                "tenGroom", resolveUserName(currentUser, "Groom"),
                "tongSoChuongPhuTrach", stalls.size(),
                "soChuongCoNgua", horses.size(),
                "danhSachChuongVaNgua", horseList
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
        result.put("tenNgua", horse.getName());
        result.put("maDangKy", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        result.put("trangThai", horse.getCurrentStatus());
        result.put("quyetDinhTapLuyen", horse.getTrainingDecision());
        result.put("lyDoQuyetDinh", horse.getTrainingDecisionReason() != null ? horse.getTrainingDecisionReason() : "Không có hạn chế đặc biệt");

        List<HealthRecord> records = healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(horse.getId());
        if (!records.isEmpty()) {
            HealthRecord latest = records.get(0);
            result.put("chanDoanMoiNhat", latest.getDiagnosis());
            result.put("phacDoDieuTri", latest.getTreatment());
            result.put("ghiChuChamSocYTe", latest.getRestrictionDetails());
            result.put("ngayKhamGanNhat", latest.getExaminedAt());
        }

        List<CareSchedule> activeSchedules = careScheduleRepository.findByHorseIdAndStatusIn(
                horse.getId(), List.of(CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS)
        );
        List<Map<String, Object>> careList = activeSchedules.stream().map(cs -> Map.<String, Object>of(
                "loaiChamSoc", cs.getCareType() != null ? cs.getCareType().name() : "Khám thú y",
                "thoiGianHen", cs.getScheduledAt() != null ? cs.getScheduledAt().toString() : "N/A",
                "mucDich", cs.getPurpose() != null ? cs.getPurpose().name() : "N/A"
        )).toList();
        result.put("lichChamSocSapToi", careList);

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
        result.put("tenNgua", horse.getName());
        result.put("maDangKy", horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        result.put("viTriChuong", stallName);
        result.put("khuVuc", areaName);
        result.put("groomPhuTrach", groomName);
        return result;
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
                "tongSoChuongTrangTrai", totalStalls,
                "chuongCachLyConTrong", availableQuarantine,
                "chuongCachLyDangSuDung", occupiedQuarantine,
                "chuongThuongConTrong", availableRegular
        );
    }

    private Object handleGetAdmissionRequestsOverview() {
        int pendingCount = admissionApplicationRepository.findByStatus(AdmissionStatus.MANAGER_REVIEW).size();
        return Map.of(
                "soDonDangKyNhapChuongChoDuyet", pendingCount,
                "trangThaiHeThong", "HOAT_DONG_BINH_THUONG"
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

            String trainerName = "Chưa gán HLV";
            if (area.getTrainerId() != null && userRepository != null) {
                trainerName = userRepository.findById(area.getTrainerId()).map(User::getFullName).orElse("HLV #" + area.getTrainerId());
            }

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("tenKhuVuc", area.getName());
            map.put("maKhuVuc", area.getCode());
            map.put("loaiKhuVuc", area.getType());
            map.put("huanLuyenVienPhuTrach", trainerName);
            map.put("tongSoChuong", stalls.size());
            map.put("soGroomPhuTrach", groomIds.size());
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
                "tongSoNguaTrongTrangTrai", horses.size(),
                "soNguaDuocPhepTapLuyen", allowedCount,
                "soNguaBiCamTapLuyenYTe", blockedCount,
                "soSuCoTrong7NgayQua", weekIncidents.size()
        );
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
                .orElse("Chuồng #" + stallId);
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
}
