package com.rtms.backend.ai.tool;

import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.ai.dto.ActionProposalDto;
import com.rtms.backend.ai.dto.ClinicalChunkResponseDto;
import com.rtms.backend.ai.hitl.service.ActionProposalService;
import com.rtms.backend.ai.rag.service.ClinicalRetrievalService;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.repository.GroomDailyTaskRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.stable.service.GroomDailyTaskService;
import com.rtms.backend.training.dto.HorseFitnessTrendItemResponse;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
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
    private final GroomDailyTaskService groomDailyTaskService;
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
                                  GroomDailyTaskService groomDailyTaskService,
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
        this.groomDailyTaskService = groomDailyTaskService;
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
            case "getMyAssignedHorses" -> handleGetMyAssignedHorses(currentUser);
            case "getTodayAggregatedTasks" -> handleGetTodayAggregatedTasks(args, currentUser);
            case "getHorseDietaryAndCareAlerts" -> handleGetHorseDietaryAndCareAlerts(args);
            case "checkStallSuppliesStatus" -> handleCheckStallSuppliesStatus(args);
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
            item.put("currentStatus", h.getCurrentStatus());
            item.put("trainingDecision", h.getTrainingDecision());
            item.put("currentStallId", h.getCurrentStallId());
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

    // --- HEAD_TRAINER Handlers ---
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

    private Object handleProposeUpdateHorseMedicalRestriction(Map<String, Object> args, AuthenticatedUser currentUser) {
        Long horseId = parseLong(args.get("horseId"));
        String restrictionType = (String) args.get("restrictionType");
        String medicalNotes = (String) args.get("medicalNotes");
        String reason = (String) args.get("reason");

        if (horseId == null || restrictionType == null || reason == null || reason.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Thiếu horseId, restrictionType hoặc reason");
        }

        Horse horse = horseRepository.findById(horseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Không tìm thấy ngựa #" + horseId));

        Map<String, Object> currentValues = new LinkedHashMap<>();
        currentValues.put("trainingDecision", horse.getTrainingDecision());
        currentValues.put("trainingDecisionReason", horse.getTrainingDecisionReason());

        Map<String, Object> proposedValues = new LinkedHashMap<>();
        proposedValues.put("trainingDecision", "BLOCKED");
        proposedValues.put("trainingDecisionReason", String.format("[%s] %s - %s",
                restrictionType, reason, medicalNotes != null ? medicalNotes : ""));

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
