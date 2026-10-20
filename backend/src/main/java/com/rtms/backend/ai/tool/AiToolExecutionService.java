package com.rtms.backend.ai.tool;

import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.ai.dto.ActionProposalDto;
import com.rtms.backend.ai.dto.ClinicalChunkResponseDto;
import com.rtms.backend.ai.hitl.service.ActionProposalService;
import com.rtms.backend.ai.rag.service.ClinicalRetrievalService;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.repository.GroomDailyTaskRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.dto.HorseFitnessTrendItemResponse;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Dịch vụ điều phối thực thi các công cụ (Tool Execution Dispatcher) được gọi bởi LLM.
 *
 * Kiểm tra 2 tầng an toàn:
 * 1. Phân quyền vai trò (DynamicToolRegistryService.isToolAllowedForRole).
 * 2. Cấm triệt để HORSE_OWNER đối với các thao tác ghi / đề xuất mutation (Read-Only 100%).
 */
@Service
public class AiToolExecutionService {

    private static final Logger log = LoggerFactory.getLogger(AiToolExecutionService.class);

    private final DynamicToolRegistryService toolRegistryService;
    private final ActionProposalService actionProposalService;
    private final ClinicalRetrievalService clinicalRetrievalService;
    private final HorseRepository horseRepository;
    private final TrainingWorkoutRepository trainingWorkoutRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final CareScheduleRepository careScheduleRepository;
    private final StableStallRepository stableStallRepository;
    private final GroomDailyTaskRepository groomDailyTaskRepository;
    private final AdmissionApplicationRepository admissionApplicationRepository;

    public AiToolExecutionService(DynamicToolRegistryService toolRegistryService,
                                  ActionProposalService actionProposalService,
                                  ClinicalRetrievalService clinicalRetrievalService,
                                  HorseRepository horseRepository,
                                  TrainingWorkoutRepository trainingWorkoutRepository,
                                  HealthRecordRepository healthRecordRepository,
                                  CareScheduleRepository careScheduleRepository,
                                  StableStallRepository stableStallRepository,
                                  GroomDailyTaskRepository groomDailyTaskRepository,
                                  AdmissionApplicationRepository admissionApplicationRepository) {
        this.toolRegistryService = toolRegistryService;
        this.actionProposalService = actionProposalService;
        this.clinicalRetrievalService = clinicalRetrievalService;
        this.horseRepository = horseRepository;
        this.trainingWorkoutRepository = trainingWorkoutRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.careScheduleRepository = careScheduleRepository;
        this.stableStallRepository = stableStallRepository;
        this.groomDailyTaskRepository = groomDailyTaskRepository;
        this.admissionApplicationRepository = admissionApplicationRepository;
    }

    /**
     * Thực thi một công cụ theo yêu cầu của LLM.
     */
    @Transactional
    public Object executeTool(String toolName, Map<String, Object> arguments, AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Người dùng chưa được xác thực");
        }

        // 1. Kiểm tra thẩm quyền gọi tool theo vai trò
        if (!toolRegistryService.isToolAllowedForRole(toolName, currentUser.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "TOOL_FORBIDDEN",
                    "Vai trò [" + currentUser.getRole() + "] không có thẩm quyền thực thi công cụ [" + toolName + "]");
        }

        // 2. Chốt chặn HORSE_OWNER Read-Only
        if ("HORSE_OWNER".equalsIgnoreCase(currentUser.getRole()) && toolName.toLowerCase().startsWith("propose")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "OWNER_READ_ONLY",
                    "Chủ ngựa chỉ có quyền xem dữ liệu, không được phép thực thi thao tác đề xuất biến đổi.");
        }

        Map<String, Object> args = arguments != null ? arguments : Collections.emptyMap();
        log.info("AiToolExecutionService: User [{}] ({}) gọi tool [{}] với args {}",
                currentUser.getUserId(), currentUser.getRole(), toolName, args);

        return switch (toolName) {
            // HORSE_OWNER Tools
            case "getMyHorses" -> handleGetMyHorses(currentUser);
            case "getHorseTrainingSummary" -> handleGetHorseTrainingSummary(args, currentUser);
            case "getHorseCareSchedule" -> handleGetHorseCareSchedule(args, currentUser);

            // HEAD_TRAINER Tools
            case "getTrainerHorses" -> handleGetTrainerHorses(currentUser);
            case "getWorkoutHistory" -> handleGetWorkoutHistory(args);
            case "getHorseFitnessTrend" -> handleGetHorseFitnessTrend(args);
            case "proposeEditWorkoutMetric" -> handleProposeEditWorkoutMetric(args, currentUser);

            // VETERINARIAN Tools
            case "searchClinicalKnowledge" -> handleSearchClinicalKnowledge(args, currentUser);
            case "getHorseMedicalHistory" -> handleGetHorseMedicalHistory(args);
            case "getActivePrescriptions" -> handleGetActivePrescriptions(args);
            case "proposeUpdateHorseMedicalRestriction" -> handleProposeUpdateHorseMedicalRestriction(args, currentUser);

            // GROOM Tools
            case "getDailyGroomTasks" -> handleGetDailyGroomTasks(currentUser);
            case "getHorseStallLocation" -> handleGetHorseStallLocation(args);

            // CLUB_MANAGER Tools
            case "getStableOccupancyMetrics" -> handleGetStableOccupancyMetrics();
            case "getUpcomingRaces" -> handleGetUpcomingRaces();
            case "getAdmissionRequestsOverview" -> handleGetAdmissionRequestsOverview();

            default -> throw new ApiException(HttpStatus.NOT_FOUND, "TOOL_NOT_FOUND", "Không tìm thấy công cụ: " + toolName);
        };
    }

    // --- HORSE_OWNER Handlers ---
    private Object handleGetMyHorses(AuthenticatedUser currentUser) {
        List<Horse> horses = horseRepository.findByOwnerId(currentUser.getUserId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Horse h : horses) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseId", h.getId());
            item.put("name", h.getName());
            item.put("registrationNumber", h.getRegistrationNumber());
            item.put("sex", h.getSex() != null ? h.getSex().name() : null);
            item.put("status", h.getCurrentStatus() != null ? h.getCurrentStatus().name() : null);
            item.put("stallId", h.getCurrentStallId());
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
        List<HorseFitnessTrendItemResponse> workouts = trainingWorkoutRepository.findFitnessTrend(
                horseId, LocalDate.now().minusDays(30), LocalDate.now());
        return Map.of(
                "horseId", horseId,
                "recentCompletedWorkoutsCount", workouts.size(),
                "recentWorkouts", workouts.stream().limit(5).toList()
        );
    }

    private Object handleGetHorseCareSchedule(Map<String, Object> args, AuthenticatedUser currentUser) {
        Long horseId = parseLong(args.get("horseId"));
        if (horseId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Thiếu horseId");
        }
        verifyHorseOwnershipIfOwner(horseId, currentUser);
        List<CareSchedule> schedules = careScheduleRepository.findByHorseIdOrderByScheduledAtAsc(horseId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (CareSchedule cs : schedules.stream().limit(10).toList()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("scheduleId", cs.getId());
            m.put("careType", cs.getCareType() != null ? cs.getCareType().name() : null);
            m.put("scheduledAt", cs.getScheduledAt());
            m.put("status", cs.getStatus() != null ? cs.getStatus().name() : null);
            list.add(m);
        }
        return list;
    }

    // --- HEAD_TRAINER Handlers ---
    private Object handleGetTrainerHorses(AuthenticatedUser currentUser) {
        List<Horse> horses = horseRepository.findAll();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Horse h : horses.stream().limit(15).toList()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("horseId", h.getId());
            m.put("name", h.getName());
            m.put("registrationNumber", h.getRegistrationNumber());
            m.put("status", h.getCurrentStatus() != null ? h.getCurrentStatus().name() : null);
            list.add(m);
        }
        return list;
    }

    private Object handleGetWorkoutHistory(Map<String, Object> args) {
        Long horseId = parseLong(args.get("horseId"));
        if (horseId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Thiếu horseId");
        }
        int limit = args.get("limit") != null ? ((Number) args.get("limit")).intValue() : 5;
        List<HorseFitnessTrendItemResponse> workouts = trainingWorkoutRepository.findFitnessTrend(
                horseId, LocalDate.now().minusMonths(3), LocalDate.now());
        return workouts.stream().limit(limit).toList();
    }

    private Object handleGetHorseFitnessTrend(Map<String, Object> args) {
        Long horseId = parseLong(args.get("horseId"));
        if (horseId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Thiếu horseId");
        }
        return trainingWorkoutRepository.findFitnessTrend(
                horseId, LocalDate.now().minusMonths(6), LocalDate.now());
    }

    private Object handleProposeEditWorkoutMetric(Map<String, Object> args, AuthenticatedUser currentUser) {
        Long workoutId = parseLong(args.get("workoutId"));
        String reason = (String) args.get("reason");
        if (workoutId == null || reason == null || reason.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Thiếu workoutId hoặc reason");
        }

        TrainingWorkout workout = trainingWorkoutRepository.findById(workoutId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WORKOUT_NOT_FOUND", "Không tìm thấy buổi tập #" + workoutId));

        Map<String, Object> currentValues = new LinkedHashMap<>();
        currentValues.put("actualDistanceMeters", workout.getActualDistanceMeters());
        currentValues.put("topSpeedKmh", workout.getTopSpeedKmh());
        currentValues.put("averageHeartRate", workout.getAverageHeartRate());
        currentValues.put("trainerFeedback", workout.getTrainerFeedback());

        Map<String, Object> proposedValues = new LinkedHashMap<>();
        if (args.containsKey("actualDistanceMeters")) proposedValues.put("actualDistanceMeters", args.get("actualDistanceMeters"));
        if (args.containsKey("topSpeedKmh")) proposedValues.put("topSpeedKmh", args.get("topSpeedKmh"));
        if (args.containsKey("averageHeartRate")) proposedValues.put("averageHeartRate", args.get("averageHeartRate"));
        if (args.containsKey("trainerFeedback")) proposedValues.put("trainerFeedback", args.get("trainerFeedback"));

        ActionProposalDto proposal = actionProposalService.createProposal(
                currentUser.getUserId(),
                "HEAD_TRAINER",
                "proposeEditWorkoutMetric",
                "Đề xuất sửa đổi thông số buổi tập #" + workoutId,
                String.valueOf(workoutId),
                currentValues,
                proposedValues,
                reason
        );

        return Map.of(
                "actionType", "HITL_PROPOSAL_CREATED",
                "actionToken", proposal.actionToken(),
                "actionId", proposal.actionId(),
                "expiresInSeconds", proposal.expiresInSeconds(),
                "title", proposal.title(),
                "proposedValues", proposedValues,
                "message", "Đã tạo bản nháp đề xuất. Vui lòng bấm Xác nhận trên giao diện để áp dụng."
        );
    }

    // --- VETERINARIAN Handlers ---
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
        for (HealthRecord hr : records.stream().limit(10).toList()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("recordId", hr.getId());
            m.put("examinedAt", hr.getExaminedAt());
            m.put("diagnosis", hr.getDiagnosis());
            m.put("notes", hr.getNotes());
            list.add(m);
        }
        return list;
    }

    private Object handleGetActivePrescriptions(Map<String, Object> args) {
        Long horseId = parseLong(args.get("horseId"));
        if (horseId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Thiếu horseId");
        }
        return Map.of("horseId", horseId, "message", "Hệ thống đang tích hợp chi tiết đơn thuốc qua TreatmentPlan.");
    }

    private Object handleProposeUpdateHorseMedicalRestriction(Map<String, Object> args, AuthenticatedUser currentUser) {
        Long horseId = parseLong(args.get("horseId"));
        String restrictionType = (String) args.get("restrictionType");
        String reason = (String) args.get("reason");
        String medicalNotes = (String) args.get("medicalNotes");

        if (horseId == null || restrictionType == null || reason == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Thiếu thông tin horseId, restrictionType hoặc reason");
        }

        Horse horse = horseRepository.findById(horseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Không tìm thấy ngựa #" + horseId));

        Map<String, Object> currentValues = Map.of("currentStatus", horse.getCurrentStatus() != null ? horse.getCurrentStatus().name() : "UNKNOWN");
        Map<String, Object> proposedValues = Map.of(
                "restrictionType", restrictionType,
                "medicalNotes", medicalNotes != null ? medicalNotes : ""
        );

        ActionProposalDto proposal = actionProposalService.createProposal(
                currentUser.getUserId(),
                "VETERINARIAN",
                "proposeUpdateHorseMedicalRestriction",
                "Đề xuất áp dụng hạn chế y tế cho ngựa " + horse.getName() + " (#" + horseId + ")",
                String.valueOf(horseId),
                currentValues,
                proposedValues,
                reason
        );

        return Map.of(
                "actionType", "HITL_PROPOSAL_CREATED",
                "actionToken", proposal.actionToken(),
                "actionId", proposal.actionId(),
                "expiresInSeconds", proposal.expiresInSeconds(),
                "title", proposal.title(),
                "proposedValues", proposedValues,
                "message", "Đã tạo đề xuất hạn chế y tế. Bác sĩ cần bấm phê duyệt để kích hoạt."
        );
    }

    // --- GROOM Handlers ---
    private Object handleGetDailyGroomTasks(AuthenticatedUser currentUser) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
        return groomDailyTaskRepository.findByScheduledTimeBetween(startOfDay, endOfDay);
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

    // --- CLUB_MANAGER Handlers ---
    private Object handleGetStableOccupancyMetrics() {
        long totalStalls = stableStallRepository.count();
        long availableQuarantine = stableStallRepository.countAvailableQuarantineStalls();
        return Map.of(
                "totalStalls", totalStalls,
                "availableQuarantineStalls", availableQuarantine
        );
    }

    private Object handleGetUpcomingRaces() {
        return Map.of("message", "Lịch trình tập luyện và giải đấu sắp tới đang được cập nhật.");
    }

    private Object handleGetAdmissionRequestsOverview() {
        int pendingCount = admissionApplicationRepository.findByStatus(AdmissionStatus.MANAGER_REVIEW).size();
        return Map.of(
                "pendingAdmissionsCount", pendingCount,
                "status", "ACTIVE"
        );
    }

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
            return Long.valueOf(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
