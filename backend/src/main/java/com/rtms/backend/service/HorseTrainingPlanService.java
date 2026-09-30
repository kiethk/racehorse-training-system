package com.rtms.backend.service;

import com.rtms.backend.config.FarmSchedulePolicy;
import com.rtms.backend.dto.*;
import com.rtms.backend.entity.*;
import com.rtms.backend.enums.LotStatus;
import com.rtms.backend.enums.TrainingDay;
import com.rtms.backend.enums.TrainingPlanStatus;
import com.rtms.backend.enums.WorkoutStatus;
import com.rtms.backend.repository.*;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HorseTrainingPlanService {

    private final HorseTrainingPlanRepository planRepository;
    private final TrainingWorkoutRepository workoutRepository;
    private final CourseRepository courseRepository;
    private final CourseSubjectRepository courseSubjectRepository;
    private final SubjectRepository subjectRepository;
    private final HorseRepository horseRepository;
    private final InjuryRecordService injuryRecordService;

    // ===== THÊM MỚI (V43) =====
    private final TrainingLotService lotService;
    private final TrainingLotRepository lotRepository;
    private final StableStallRepository stableStallRepository;
    private final AreaRepository areaRepository;
    private final GroomIncidentReportRepository incidentReportRepository;

    /**
     * CỐ Ý chỉ có MỘT constructor.
     *
     * Trước đây có thêm một bản rút gọn uỷ quyền xuống đây với
     * incidentReportRepository = null, để khỏi phải sửa file test. Nhưng bản đó
     * tạo ra một service THIẾU CHỨC NĂNG mà không báo lỗi: luật cảnh báo "sự cố
     * lặp lại" bị bỏ qua im lặng. Một constructor dựng được object hỏng là thứ
     * phải tránh — thà để trình biên dịch bắt lỗi còn hơn.
     *
     * Chỉ có một constructor nên Spring tự chọn, không cần @Autowired.
     */
    public HorseTrainingPlanService(HorseTrainingPlanRepository planRepository,
                                    TrainingWorkoutRepository workoutRepository,
                                    CourseRepository courseRepository,
                                    CourseSubjectRepository courseSubjectRepository,
                                    SubjectRepository subjectRepository,
                                    HorseRepository horseRepository,
                                    InjuryRecordService injuryRecordService,
                                    TrainingLotService lotService,
                                    TrainingLotRepository lotRepository,
                                    StableStallRepository stableStallRepository,
                                    AreaRepository areaRepository,
                                    GroomIncidentReportRepository incidentReportRepository) {
        this.planRepository = planRepository;
        this.workoutRepository = workoutRepository;
        this.courseRepository = courseRepository;
        this.courseSubjectRepository = courseSubjectRepository;
        this.subjectRepository = subjectRepository;
        this.horseRepository = horseRepository;
        this.injuryRecordService = injuryRecordService;
        this.lotService = lotService;
        this.lotRepository = lotRepository;
        this.stableStallRepository = stableStallRepository;
        this.areaRepository = areaRepository;
        this.incidentReportRepository = incidentReportRepository;
    }

    /**
     * Danh sách kế hoạch cho màn hình tra cứu.
     *
     * HEAD_TRAINER chỉ thấy kế hoạch của chính mình — giống cách GET /api/lots
     * đang làm. Các vai khác thấy tất cả, hoặc lọc theo horseId.
     */
    public List<PlanSummaryResponse> getPlanSummaries(Long horseId, AuthenticatedUser currentUser) {
        List<HorseTrainingPlan> plans;

        if (horseId != null) {
            plans = planRepository.findByHorseId(horseId);
        } else if ("HEAD_TRAINER".equalsIgnoreCase(currentUser.getRole())) {
            plans = planRepository.findByTrainerIdAndStatusIn(
                    currentUser.getUserId(), List.of(TrainingPlanStatus.values()));
        } else {
            plans = planRepository.findAll();
        }

        return toSummaries(plans);
    }

    /**
     * Ghép tên ngựa, tên khoá và ba con số tiến độ vào danh sách kế hoạch.
     *
     * Ba truy vấn gom, KHÔNG phải ba truy vấn mỗi dòng: nạp một lượt toàn bộ
     * ngựa, khoá và số buổi theo trạng thái. Với 20 kế hoạch thì đây là
     * 3 truy vấn thay vì 60.
     */
    private List<PlanSummaryResponse> toSummaries(List<HorseTrainingPlan> plans) {
        if (plans.isEmpty()) {
            return List.of();
        }

        Set<Long> horseIds = new HashSet<>();
        Set<Long> courseIds = new HashSet<>();
        Set<Long> planIds = new HashSet<>();
        plans.forEach(p -> {
            horseIds.add(p.getHorseId());
            courseIds.add(p.getCourseId());
            planIds.add(p.getId());
        });

        Map<Long, String> horseNames = new HashMap<>();
        horseRepository.findAllById(horseIds).forEach(h -> horseNames.put(h.getId(), h.getName()));

        Map<Long, String> courseNames = new HashMap<>();
        courseRepository.findAllById(courseIds).forEach(c -> courseNames.put(c.getId(), c.getName()));

        // planId -> (trạng thái -> số buổi)
        Map<Long, Map<WorkoutStatus, Integer>> countsByPlan = new HashMap<>();
        for (Object[] row : workoutRepository.countByPlanIdsGroupedByStatus(planIds)) {
            countsByPlan
                    .computeIfAbsent((Long) row[0], k -> new EnumMap<>(WorkoutStatus.class))
                    .put((WorkoutStatus) row[1], ((Number) row[2]).intValue());
        }

        List<PlanSummaryResponse> result = new ArrayList<>();
        for (HorseTrainingPlan p : plans) {
            Map<WorkoutStatus, Integer> counts =
                    countsByPlan.getOrDefault(p.getId(), Map.of());
            int total = counts.values().stream().mapToInt(Integer::intValue).sum();

            result.add(new PlanSummaryResponse(
                    p.getId(),
                    p.getHorseId(),
                    horseNames.getOrDefault(p.getHorseId(), "#" + p.getHorseId()),
                    p.getCourseId(),
                    courseNames.getOrDefault(p.getCourseId(), "#" + p.getCourseId()),
                    p.getStartDate(),
                    p.getEndDate(),
                    p.getStatus(),
                    total,
                    counts.getOrDefault(WorkoutStatus.COMPLETED, 0),
                    counts.getOrDefault(WorkoutStatus.CANCELLED, 0)));
        }

        // Kế hoạch mới nhất lên đầu
        result.sort(Comparator.comparing(PlanSummaryResponse::getStartDate).reversed());
        return result;
    }

    public HorseTrainingPlanDetailResponse getPlanById(Long id) {
        HorseTrainingPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kế hoạch #" + id));

        // Tên ngựa và tên khoá: entity chỉ mang id dạng số, phải ghép ở đây.
        String horseName = horseRepository.findById(plan.getHorseId())
                .map(Horse::getName)
                .orElse("#" + plan.getHorseId());
        String courseName = courseRepository.findById(plan.getCourseId())
                .map(Course::getName)
                .orElse("#" + plan.getCourseId());

        return new HorseTrainingPlanDetailResponse(
                plan, buildWorkoutItems(id), horseName, courseName);
    }

    /** Gom workout + lot + tên bài tập thành danh sách hiển thị. */
    private List<PlanWorkoutItemResponse> buildWorkoutItems(Long planId) {
        List<PlanWorkoutItemResponse> items = new ArrayList<>();
        Set<Long> lotIds = new LinkedHashSet<>();

        for (Object[] row : workoutRepository.findByPlanIdWithLot(planId)) {
            TrainingWorkout w = (TrainingWorkout) row[0];
            TrainingLot lot = (TrainingLot) row[1];
            Subject subject = subjectRepository.findById(lot.getSubjectId()).orElse(null);
            PlanWorkoutItemResponse item = new PlanWorkoutItemResponse(
                    w, lot, subject != null ? subject.getName() : "#" + lot.getSubjectId());
            if (subject != null) {
                item.setWorkoutType(subject.getWorkoutType().name());
            }
            items.add(item);
            lotIds.add(lot.getId());
        }

        // Số ngựa mỗi lot — MỘT truy vấn gom cho cả kế hoạch, không phải
        // mỗi buổi một truy vấn. Nhờ đó trang chi tiết thấy được cơ chế ghép
        // nhóm đang hoạt động ("chung lot với 5 con khác").
        if (!lotIds.isEmpty()) {
            Map<Long, Integer> occupancyByLot = new HashMap<>();
            for (Object[] row : workoutRepository.countActiveByLotIds(lotIds)) {
                occupancyByLot.put((Long) row[0], ((Number) row[1]).intValue());
            }
            items.forEach(i -> i.setLotOccupancy(occupancyByLot.get(i.getLotId())));
        }

        return items;
    }

    /**
     * GHI DANH THEO NHÓM + SINH LOT + SINH BUỔI TẬP.
     *
     * Trả về danh sách plan (mỗi chiến mã một plan riêng — dữ liệu KHÔNG bị
     * gộp, chỉ thao tác tạo được gộp). Đề tài yêu cầu "giáo án cho từng con
     * ngựa", và mỗi con vẫn có hồ sơ, chỉ số đo, nhận xét riêng.
     */
    @Transactional
    public List<HorseTrainingPlanDetailResponse> createPlan(CreateHorseTrainingPlanRequest request,
                                                            AuthenticatedUser currentUser) {
        Long trainerId = currentUser.getUserId();

        // =============================================================
        // 0. VALIDATE ĐẦU VÀO
        // =============================================================
        if (request.getHorses() == null || request.getHorses().isEmpty()) {
            throw new IllegalArgumentException("Phải ghi danh ít nhất 1 chiến mã!");
        }
        if (request.getCourseId() == null) {
            throw new IllegalArgumentException("Phải chọn khoá huấn luyện!");
        }
        if (request.getStartDate() == null) {
            throw new IllegalArgumentException("Ngày bắt đầu không được để trống!");
        }
        // KHÔNG mặc định ngầm T2-4-6 như bản cũ: giá trị này giờ được LƯU vào DB
        // và trở thành dữ liệu thật của hồ sơ, không nên để hệ thống đoán hộ.
        if (request.getTrainingDays() == null || request.getTrainingDays().isEmpty()) {
            throw new IllegalArgumentException(
                    "Phải chọn ít nhất 1 thứ trong tuần để tập (ví dụ MONDAY, WEDNESDAY, FRIDAY)!");
        }

        Set<TrainingDay> selectedDays = new LinkedHashSet<>(request.getTrainingDays());

        // =============================================================
        // 1. KHOÁ HỌC + DANH SÁCH BÀI TẬP THEO THỨ TỰ
        // =============================================================
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy khoá huấn luyện #" + request.getCourseId()));

        List<CourseSubject> courseSubjects =
                courseSubjectRepository.findByCourseIdOrderByOrderIndexAsc(course.getId());
        if (courseSubjects.isEmpty()) {
            throw new IllegalStateException("Khoá học này chưa có bài tập nào, không thể gán cho ngựa!");
        }

        // Nạp sẵn Subject để không query lại trong vòng lặp sinh lịch
        List<Subject> orderedSubjects = courseSubjects.stream()
                .map(cs -> subjectRepository.findById(cs.getSubjectId())
                        .orElseThrow(() -> new RuntimeException(
                                "Không tìm thấy bài tập #" + cs.getSubjectId())))
                .toList();

        int totalNeeded = course.getTotalSessions();

        // =============================================================
        // 2. CHUẨN HOÁ & KIỂM TRA TỪNG CHIẾN MÃ
        // =============================================================
        List<EnrolledHorse> enrolled = new ArrayList<>();
        Set<Long> seenHorseIds = new HashSet<>();

        for (HorseEnrollmentRequest e : request.getHorses()) {
            if (e.getHorseId() == null) {
                throw new IllegalArgumentException("Thiếu horseId trong danh sách ghi danh!");
            }
            if (!seenHorseIds.add(e.getHorseId())) {
                throw new IllegalArgumentException(
                        "Chiến mã #" + e.getHorseId() + " bị ghi danh trùng trong cùng một yêu cầu!");
            }

            Horse horse = horseRepository.findById(e.getHorseId())
                    .orElseThrow(() -> new RuntimeException(
                            "Không tìm thấy chiến mã #" + e.getHorseId()));

            // ---- BR-05: KHOÁ HUẤN LUYỆN ----
            TrainingLockStatusResponse lock =
                    injuryRecordService.getTrainingLockStatus(horse.getId());
            if (lock.isLocked()) {
                throw new IllegalStateException(String.format(
                        "Chiến mã '%s' đang bị KHOÁ HUẤN LUYỆN (trạng thái: %s). "
                      + "Không thể tạo kế hoạch mới!",
                        horse.getName(), lock.getCurrentStatus()));
            }

            Long groomId = resolveGroom(horse, e.getGroomId(), trainerId);
            enrolled.add(new EnrolledHorse(horse, groomId));
        }

        // =============================================================
        // 3. NGÀY KẾT THÚC DỰ KIẾN
        // =============================================================
        LocalDate calculatedEndDate =
                calculateEndDate(request.getStartDate(), selectedDays, totalNeeded);

        // =============================================================
        // 4. BR-01 — KHÔNG CHỒNG KHOẢNG NGÀY VỚI PLAN ĐANG CHẠY
        // =============================================================
        for (EnrolledHorse en : enrolled) {
            List<HorseTrainingPlan> ongoing =
                    planRepository.findByHorseIdAndStatusInOrderByEndDateDesc(
                            en.horse().getId(),
                            List.of(TrainingPlanStatus.ACTIVE, TrainingPlanStatus.UPCOMING));

            for (HorseTrainingPlan existing : ongoing) {
                boolean overlapping = !request.getStartDate().isAfter(existing.getEndDate())
                                   && !calculatedEndDate.isBefore(existing.getStartDate());
                if (overlapping) {
                    throw new IllegalStateException(String.format(
                            "Xung đột lịch: chiến mã '%s' đã có kế hoạch (%s) từ %s đến %s, "
                          + "trùng với thời gian dự kiến %s đến %s!",
                            en.horse().getName(), existing.getStatus(),
                            existing.getStartDate(), existing.getEndDate(),
                            request.getStartDate(), calculatedEndDate));
                }
            }
        }

        // =============================================================
        // 5. TẠO PLAN CHO TỪNG CON
        // =============================================================
        Map<Long, HorseTrainingPlan> plansByHorse = new LinkedHashMap<>();

        for (EnrolledHorse en : enrolled) {
            HorseTrainingPlan plan = new HorseTrainingPlan();
            plan.setHorseId(en.horse().getId());
            plan.setCourseId(course.getId());
            plan.setTrainerId(trainerId);
            plan.setStartDate(request.getStartDate());
            plan.setEndDate(calculatedEndDate);
            plan.setTrainingDays(selectedDays);        // KHUÔN MẪU
            plan.setGroomId(en.groomId());             // KHUÔN MẪU
            plan.setStatus(request.getStartDate().isAfter(LocalDate.now())
                    ? TrainingPlanStatus.UPCOMING
                    : TrainingPlanStatus.ACTIVE);
            plan.setNotes(request.getNotes());

            plansByHorse.put(en.horse().getId(), planRepository.saveAndFlush(plan));
        }

        // =============================================================
        // 6. SINH LOT + BUỔI TẬP
        //
        // findOrCreateLot nằm BÊN TRONG vòng lặp ngựa (không phải bên ngoài).
        // Nhờ vậy khi một lot đầy giữa chừng hoặc vướng BR-09, con tiếp theo
        // tự động tràn sang lot thứ hai mà không cần xử lý đặc biệt nào.
        // Nhóm ĐƯỢC PHÉP tách sang lot khác — giá trị của cơ chế nhóm là
        // "cùng bài vào cùng ngày", không phải "luôn đứng chung một lot".
        // =============================================================
        Map<Long, List<PlanWorkoutItemResponse>> workoutsByHorse = new LinkedHashMap<>();
        enrolled.forEach(en -> workoutsByHorse.put(en.horse().getId(), new ArrayList<>()));

        LocalDate cursor = request.getStartDate();
        int sessionIndex = 0;

        while (sessionIndex < totalNeeded) {
            TrainingDay day = TrainingDay.valueOf(cursor.getDayOfWeek().name());

            if (selectedDays.contains(day)) {
                // Xoay vòng bài tập: courseSubjects[i % n]
                Subject subject = orderedSubjects.get(sessionIndex % orderedSubjects.size());

                for (EnrolledHorse en : enrolled) {
                    TrainingLot lot = lotService.findOrCreateLot(
                            trainerId, cursor, subject, en.groomId());

                    TrainingWorkout workout = new TrainingWorkout();
                    workout.setPlanId(plansByHorse.get(en.horse().getId()).getId());
                    workout.setLotId(lot.getId());
                    workout.setHorseId(en.horse().getId());
                    workout.setAssignedToId(en.groomId());
                    workout.setStatus(WorkoutStatus.SCHEDULED);

                    // saveAndFlush BẮT BUỘC: findOrCreateLot của con NGỰA KẾ TIẾP
                    // sẽ đếm sức chứa và kiểm tra BR-09 bằng câu query — nếu
                    // workout này chưa được flush xuống DB thì nó đếm thiếu.
                    TrainingWorkout saved = workoutRepository.saveAndFlush(workout);

                    PlanWorkoutItemResponse item =
                            new PlanWorkoutItemResponse(saved, lot, subject.getName());
                    item.setWorkoutType(subject.getWorkoutType().name());
                    workoutsByHorse.get(en.horse().getId()).add(item);
                }
                sessionIndex++;
            }

            if (sessionIndex < totalNeeded) {
                cursor = cursor.plusDays(1);
            }
        }

        // =============================================================
        // 7. TRẢ KẾT QUẢ
        // =============================================================
        List<HorseTrainingPlanDetailResponse> result = new ArrayList<>();
        for (EnrolledHorse en : enrolled) {
            result.add(new HorseTrainingPlanDetailResponse(
                    plansByHorse.get(en.horse().getId()),
                    workoutsByHorse.get(en.horse().getId())));
        }
        return result;
    }

    /**
     * Chuyển các kế hoạch đã tới ngày bắt đầu từ UPCOMING sang ACTIVE.
     *
     * createPlan đặt UPCOMING cho kế hoạch có startDate ở tương lai, nhưng
     * trước đây KHÔNG có gì chuyển nó sang ACTIVE khi ngày đó tới — nhãn
     * trạng thái đứng yên mãi ở "sắp diễn ra" dù khoá đang chạy.
     *
     * Hàm này để job nửa đêm gọi. Nó chỉ lo phần HIỂN THỊ đúng trạng thái:
     * việc đóng khoá đã được completeWorkout tự xử lý, không phụ thuộc job
     * này chạy hay không (phòng trường hợp server tắt đúng đêm đó).
     *
     * @return số kế hoạch đã chuyển
     */
    @Transactional
    public int activateStartedPlans(LocalDate asOf) {
        List<HorseTrainingPlan> due = planRepository
                .findByStatusAndStartDateLessThanEqual(TrainingPlanStatus.UPCOMING, asOf);

        if (due.isEmpty()) {
            return 0;
        }

        due.forEach(p -> p.setStatus(TrainingPlanStatus.ACTIVE));
        planRepository.saveAll(due);
        return due.size();
    }

    @Transactional
    public HorseTrainingPlan updatePlanStatus(Long id, UpdatePlanStatusRequest request) {
        HorseTrainingPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Training plan not found with id: " + id));

        plan.setStatus(request.getStatus());
        return planRepository.save(plan);
    }

    // =================================================================
    // HÀM PHỤ TRỢ
    // =================================================================

    /** Bản ghi tạm: chiến mã + Groom đã phân giải. */
    private record EnrolledHorse(Horse horse, Long groomId) {}

    /**
     * Phân giải Groom cho một chiến mã, đồng thời kiểm tra quyền sở hữu.
     *
     * Chuỗi quan hệ (không cần thêm cột nào vào bảng horses):
     *   Horse.currentStallId -> StableStall.areaId -> Area.trainerId
     *   Horse.currentStallId -> StableStall.groomId
     */
    private Long resolveGroom(Horse horse, Long overrideGroomId, Long trainerId) {
        if (horse.getCurrentStallId() == null) {
            throw new IllegalStateException(String.format(
                    "Chiến mã '%s' chưa được xếp chuồng. Hãy gán chuồng "
                  + "(PUT /api/horses/%d/assign-stall) trước khi lập kế hoạch huấn luyện!",
                    horse.getName(), horse.getId()));
        }

        StableStall stall = stableStallRepository.findById(horse.getCurrentStallId())
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy chuồng #" + horse.getCurrentStallId()));

        // ---- Trainer chỉ lập kế hoạch cho ngựa trong khu của mình ----
        Area area = areaRepository.findById(stall.getAreaId())
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy khu vực #" + stall.getAreaId()));

        if (area.getTrainerId() == null) {
            throw new IllegalStateException(String.format(
                    "Khu vực '%s' chưa được phân công cho Trainer nào. "
                  + "Liên hệ Quản lý câu lạc bộ để gán khu vực!", area.getCode()));
        }
        if (!area.getTrainerId().equals(trainerId)) {
            throw new org.springframework.security.access.AccessDeniedException(String.format(
                    "Chiến mã '%s' đang ở khu vực '%s' không do bạn phụ trách!",
                    horse.getName(), area.getCode()));
        }

        // ---- Groom: suy từ chuồng, cho phép ghi đè ----
        Long groomId = (overrideGroomId != null) ? overrideGroomId : stall.getGroomId();
        if (groomId == null) {
            throw new IllegalStateException(String.format(
                    "Chuồng '%s' của chiến mã '%s' chưa được gán Groom. "
                  + "Hãy gán Groom (PUT /api/stalls/%d/assign-groom) trước!",
                    stall.getStallCode(), horse.getName(), stall.getId()));
        }
        return groomId;
    }

    /**
     * Mô phỏng tiến trình để tìm ngày diễn ra buổi tập cuối cùng.
     * Tách thành hàm thuần để dùng lại được ở cả BR-01 và API gợi ý nhóm,
     * và để viết unit test không cần DB.
     */
    static LocalDate calculateEndDate(LocalDate startDate,
                                      Set<TrainingDay> selectedDays,
                                      int totalSessions) {
        LocalDate cursor = startDate;
        int count = 0;
        while (count < totalSessions) {
            if (selectedDays.contains(TrainingDay.valueOf(cursor.getDayOfWeek().name()))) {
                count++;
            }
            if (count < totalSessions) {
                cursor = cursor.plusDays(1);
            }
        }
        return cursor;
    }

    // =================================================================
    // ĐÓNG BUỔI TẬP (TRAINER GHI NHẬN KẾT QUẢ)
    // =================================================================

    /**
     * Trainer đóng một buổi tập và ghi nhận chỉ số chuyên môn.
     *
     * Phân biệt rõ với việc Groom tick hoàn thành:
     *   - groom_daily_tasks.isCompleted : Groom bấm, chỉ là "có làm / chưa làm"
     *   - training_workouts.status      : Trainer bấm, kèm chỉ số + nhận xét
     * Đề tài phân công rõ: "Đánh giá phong độ, ghi nhận chỉ số buổi tập và đưa
     * ra nhận xét chuyên môn sau mỗi buổi tập" là việc của Head Trainer.
     */
    @Transactional
    public PlanWorkoutItemResponse completeWorkout(Long workoutId,
                                                   CompleteWorkoutRequest request,
                                                   AuthenticatedUser currentUser) {
        TrainingWorkout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi tập #" + workoutId));

        HorseTrainingPlan plan = planRepository.findById(workout.getPlanId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kế hoạch của buổi tập này"));

        if (!plan.getTrainerId().equals(currentUser.getUserId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Chỉ Trainer phụ trách kế hoạch này mới được ghi nhận kết quả buổi tập!");
        }
        if (workout.getStatus() == WorkoutStatus.CANCELLED) {
            throw new IllegalStateException("Buổi tập đã bị huỷ, không thể ghi nhận kết quả!");
        }
        if (request.getPerformanceRating() != null
                && (request.getPerformanceRating() < 1 || request.getPerformanceRating() > 10)) {
            throw new IllegalArgumentException("Điểm phong độ phải từ 1 đến 10!");
        }

        workout.setActualDistanceMeters(request.getActualDistanceMeters());
        workout.setActualDurationMinutes(request.getActualDurationMinutes());
        workout.setTopSpeedKmh(request.getTopSpeedKmh());
        workout.setAverageSpeedKmh(request.getAverageSpeedKmh());
        workout.setAverageHeartRate(request.getAverageHeartRate());
        workout.setMaxHeartRate(request.getMaxHeartRate());
        workout.setRecoveryHeartRate(request.getRecoveryHeartRate());
        workout.setPerformanceRating(request.getPerformanceRating());
        workout.setTrainerFeedback(request.getTrainerFeedback());
        workout.setVideoUrl(request.getVideoUrl());
        workout.setStatus(WorkoutStatus.COMPLETED);

        TrainingWorkout saved = workoutRepository.saveAndFlush(workout);

        // Hoàn thành được một buổi tập nghĩa là khoá ĐÃ BẮT ĐẦU.
        //
        // Vì sao chuyển trạng thái NGAY TẠI ĐÂY thay vì chỉ trông vào job nửa
        // đêm: nếu server tắt đúng đêm đó thì plan kẹt ở UPCOMING vĩnh viễn,
        // và điều kiện đóng khoá bên dưới không bao giờ đúng. Đây từng là lỗi
        // thật — mọi kế hoạch tạo cho ngày tương lai đều không thể hoàn thành.
        if (plan.getStatus() == TrainingPlanStatus.UPCOMING) {
            plan.setStatus(TrainingPlanStatus.ACTIVE);
        }

        // Buổi cuối cùng của khoá -> plan tự chuyển COMPLETED.
        //
        // CANCELLED được coi là "đã giải quyết xong", không nằm trong remaining.
        // Nhờ vậy khoá 12 buổi bị huỷ 1 vì mưa vẫn đóng được ở 11 buổi, thay vì
        // treo mãi. Giao diện cũng trừ mẫu số tương ứng -> hiện 11/11 (100%).
        //
        // KHÔNG kiểm "status == ACTIVE" nữa: điều kiện đó khiến kế hoạch
        // UPCOMING không bao giờ đóng được. Chỉ cần nó chưa đóng và chưa huỷ.
        long remaining = workoutRepository.countByPlanIdAndStatusNotIn(
                plan.getId(), List.of(WorkoutStatus.COMPLETED, WorkoutStatus.CANCELLED));
        if (remaining == 0
                && plan.getStatus() != TrainingPlanStatus.COMPLETED
                && plan.getStatus() != TrainingPlanStatus.CANCELLED) {
            plan.setStatus(TrainingPlanStatus.COMPLETED);
        }

        planRepository.save(plan);

        TrainingLot lot = lotRepository.findById(saved.getLotId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lot của buổi tập"));

        // Khi toàn bộ buổi tập trong lot đã hoàn thành (hoặc bị huỷ), lot tự chuyển sang COMPLETED.
        long remainingInLot = workoutRepository.countByLotIdAndStatusNotIn(
                lot.getId(), List.of(WorkoutStatus.COMPLETED, WorkoutStatus.CANCELLED));
        if (remainingInLot == 0
                && lot.getStatus() != LotStatus.COMPLETED
                && lot.getStatus() != LotStatus.CANCELLED) {
            lot.setStatus(LotStatus.COMPLETED);
            lotRepository.save(lot);
        }

        Subject subject = subjectRepository.findById(lot.getSubjectId()).orElse(null);

        PlanWorkoutItemResponse item = new PlanWorkoutItemResponse(
                saved, lot, subject != null ? subject.getName() : "#" + lot.getSubjectId());
        if (subject != null) {
            item.setWorkoutType(subject.getWorkoutType().name());
        }
        return item;
    }

    // =================================================================
    // GỢI Ý NHÓM ĐỂ GHI DANH CHUNG
    // =================================================================

    /**
     * Liệt kê các nhóm có thể ghi danh chung cho một khoá.
     *
     * CƠ SỞ TOÁN HỌC: bài tập của buổi thứ i là courseSubjects[i % n]. Nếu hai
     * con cùng trainingDays thì mỗi ngày tập cả hai đều tăng i lên 1, nên hiệu
     * (i_A - i_B) KHÔNG BAO GIỜ đổi. Do đó:
     *
     *      Đồng pha một lần  =>  đồng pha vĩnh viễn.
     *
     * Con mới luôn bắt đầu ở i = 0, nên điều kiện chỉ còn là:
     * i_A ≡ 0 (mod n) vào đúng ngày con mới bắt đầu — tức ngày nhóm A quay về
     * bài orderIndex = 1.
     *
     * LƯU Ý: đây KHÔNG phải "nhập giữa chừng". Con mới vẫn học từ buổi 0, đủ
     * totalSessions, không bỏ bài nào. Ta chỉ CHỜ vòng xoay của nhóm quay về
     * vạch xuất phát rồi cho nhập cuộc từ đó. Nhờ vậy không phát sinh khái
     * niệm "học bù" / "miễn bài" / "khoá rút gọn".
     *
     * GIÁ PHẢI TRẢ: con mới phải chờ tối đa (n - 1) buổi tập. Hệ thống chỉ
     * GỢI Ý, Trainer nhìn waitDays đối chiếu sharedSessions rồi tự quyết.
     */
    public List<JoinableCohortResponse> getJoinableCohorts(Long courseId,
                                                            AuthenticatedUser currentUser) {
        Long trainerId = currentUser.getUserId();

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khoá #" + courseId));

        int subjectCount = courseSubjectRepository
                .findByCourseIdOrderByOrderIndexAsc(courseId).size();
        if (subjectCount == 0) {
            return List.of();
        }

        int totalSessions = course.getTotalSessions();
        LocalDate today = LocalDate.now();
        List<JoinableCohortResponse> result = new ArrayList<>();

        for (Object[] row : planRepository.findJoinableCohorts(trainerId, courseId)) {
            LocalDate cohortStart = (LocalDate) row[1];
            @SuppressWarnings("unchecked")
            Set<TrainingDay> days = (Set<TrainingDay>) row[2];
            int horseCount = ((Number) row[3]).intValue();

            if (days == null || days.isEmpty()) {
                continue;   // plan cũ trước V43 chưa có training_days
            }

            boolean upcoming = cohortStart.isAfter(today);
            LocalDate suggested;
            int sharedSessions;
            String status;

            if (upcoming) {
                // Nhóm chưa khởi động -> nhập luôn, đồng bộ TRỌN khoá
                status = "UPCOMING";
                suggested = cohortStart;
                sharedSessions = totalSessions;
            } else {
                status = "ACTIVE";
                AlignedPoint point = nextAlignedPoint(
                        cohortStart, days, subjectCount, totalSessions, today);
                if (point == null) {
                    continue;   // khoá sắp hết, không còn điểm đồng pha
                }
                suggested = point.date();
                sharedSessions = totalSessions - point.sessionIndex();
            }

            long waitDays = java.time.temporal.ChronoUnit.DAYS.between(today, suggested);

            // Câu này hiển thị thẳng cho người dùng nên viết bằng từ thường,
            // không dùng thuật ngữ nội bộ như "đồng pha" hay "lot".
            //
            // Lưu ý kỹ thuật cho người bảo trì: sharedSessions là số buổi CÙNG
            // NGÀY CÙNG BÀI, không bảo đảm xếp chung được — nhóm vẫn bị tách
            // nếu vượt sức chứa (BR-10) hoặc hai con cùng Groom (BR-09).
            String note = upcoming
                    ? String.format("Tập chung trọn %d/%d buổi", sharedSessions, totalSessions)
                    : String.format("Tập chung %d/%d buổi, %d buổi cuối tách riêng",
                                    sharedSessions, totalSessions, totalSessions - sharedSessions);

            result.add(new JoinableCohortResponse(
                    status, courseId, course.getName(), suggested, days,
                    horseCount, sharedSessions, totalSessions, waitDays,
                    FarmSchedulePolicy.DEFAULT_LOT_CAPACITY, note));
        }

        // Nhóm chung được nhiều buổi nhất lên đầu
        result.sort(Comparator.comparing(JoinableCohortResponse::getSharedSessions).reversed());
        return result;
    }

    private record AlignedPoint(LocalDate date, int sessionIndex) {}

    /**
     * Tìm ngày gần nhất (>= minDate) mà nhóm quay về bài đầu khoá (i % n == 0).
     * Hàm thuần, không đụng DB — dễ viết unit test.
     */
    static AlignedPoint nextAlignedPoint(LocalDate cohortStart,
                                         Set<TrainingDay> days,
                                         int subjectCount,
                                         int totalSessions,
                                         LocalDate minDate) {
        LocalDate cursor = cohortStart;
        int index = 0;
        while (index < totalSessions) {
            if (days.contains(TrainingDay.valueOf(cursor.getDayOfWeek().name()))) {
                if (index % subjectCount == 0 && !cursor.isBefore(minDate)) {
                    return new AlignedPoint(cursor, index);
                }
                index++;
            }
            cursor = cursor.plusDays(1);
        }
        return null;
    }

    // =================================================================
    // CASCADE KHI NGỰA CHẤN THƯƠNG
    // =================================================================

    /**
     * HUỶ TOÀN BỘ HUẤN LUYỆN TƯƠNG LAI CỦA MỘT CHIẾN MÃ.
     *
     * Gọi khi Bác sĩ thú y chuyển ngựa sang trạng thái khoá huấn luyện
     * (INJURED / QUARANTINED / MONITORING / REJECTED / CANDIDATE).
     *
     * Theo thiết kế đã chốt:
     *   1. Huỷ mọi buổi tập SCHEDULED từ hôm nay trở đi  -> TỰ ĐỘNG
     *   2. Lot nào rỗng sau khi huỷ thì huỷ luôn lot     -> trả lại khe giờ
     *   3. Plan chuyển CANCELLED (không phải PAUSED), vì sau chấn thương
     *      thường thay bằng khoá hồi phục chứ không tập tiếp khoá cũ.
     *
     * @return số buổi tập đã huỷ
     */
    @Transactional
    public int cancelFutureTrainingForHorse(Long horseId, String reason) {
        List<TrainingWorkout> future =
                workoutRepository.findFutureScheduledByHorse(horseId, LocalDate.now());

        Set<Long> touchedLotIds = new HashSet<>();
        for (TrainingWorkout w : future) {
            w.setStatus(WorkoutStatus.CANCELLED);
            workoutRepository.save(w);
            touchedLotIds.add(w.getLotId());
        }
        workoutRepository.flush();   // để cancelLotIfEmpty đếm đúng

        // Lot rỗng -> huỷ, trả khe giờ cho ngựa khác.
        // Không làm bước này thì khung giờ vàng bị chiếm bởi "lot ma".
        touchedLotIds.forEach(lotService::cancelLotIfEmpty);

        // Plan đang chạy -> CANCELLED
        List<HorseTrainingPlan> plans = planRepository.findByHorseIdAndStatusInOrderByEndDateDesc(
                horseId, List.of(TrainingPlanStatus.ACTIVE, TrainingPlanStatus.UPCOMING));
        for (HorseTrainingPlan p : plans) {
            p.setStatus(TrainingPlanStatus.CANCELLED);
            String suffix = "[Huỷ tự động] " + (reason != null ? reason : "Ngựa bị khoá huấn luyện");
            p.setNotes(p.getNotes() == null ? suffix : p.getNotes() + " | " + suffix);
            planRepository.save(p);
        }

        return future.size();
    }

    // =================================================================
    // ĐỒNG BỘ GROOM KHI ĐỔI CHUỒNG / ĐỔI NGƯỜI PHỤ TRÁCH
    // =================================================================

    /**
     * Chuyển mọi buổi tập CHƯA diễn ra của một chiến mã sang Groom mới.
     *
     * VÌ SAO CẦN HÀM NÀY:
     * workout.assignedToId là ẢNH CHỤP, lấy từ stall.groomId đúng một lần lúc
     * tạo kế hoạch. Đổi Groom của chuồng, hoặc chuyển ngựa sang chuồng khác,
     * đều KHÔNG tự cập nhật các workout đã sinh. Ba hậu quả:
     *
     *   1. Groom cũ vẫn thấy việc dắt con ngựa mình không còn chăm
     *   2. Groom mới KHÔNG thấy việc đáng lẽ của mình
     *   3. Nếu Groom mới đã có con khác trong cùng lot thì BR-09 bị vi phạm
     *      NGOÀI ĐỜI — một người phải dắt 2 con cùng lúc — trong khi dữ liệu
     *      vẫn trông hợp lệ vì workout chưa được cập nhật. Không query nào
     *      phát hiện được.
     *
     * @param newGroomId Groom mới. Truyền null khi gỡ Groom khỏi chuồng hoặc
     *                   gỡ ngựa khỏi chuồng — buổi tập thành "chưa phân công"
     *                   để Trainer thấy mà xử lý, thay vì treo ở người không
     *                   còn liên quan.
     * @return số buổi tập đã chuyển
     * @throws IllegalStateException nếu bất kỳ lot nào vi phạm BR-09
     */
    @Transactional
    public int reassignFutureWorkoutsToGroom(Long horseId, Long newGroomId) {

        List<TrainingWorkout> future =
                workoutRepository.findFutureScheduledByHorse(horseId, LocalDate.now());

        if (future.isEmpty()) {
            return 0;   // ngựa chưa có kế hoạch nào -> không có gì để chuyển
        }

        // ---------- BƯỚC 1: KIỂM TRA BR-09 ----------
        //
        // Khối if này CHỈ bọc phần kiểm tra. Bước ghi bên dưới LUÔN chạy.
        // Bỏ qua kiểm tra khi newGroomId == null vì "không ai phụ trách" thì
        // không thể trùng với ai — và đó chính là trạng thái "chưa phân công"
        // mà ta muốn ghi xuống.
        //
        // Cố ý duyệt HẾT rồi mới quyết định, thay vì ném lỗi ngay ở lot đầu:
        //   - Người dùng thấy đủ các buổi bị vướng trong một lần, không phải
        //     sửa từng cái rồi thử lại
        //   - Không xen kẽ việc kiểm tra với việc ghi
        if (newGroomId != null) {
            List<String> conflicts = new ArrayList<>();

            for (TrainingWorkout w : future) {
                boolean busy = workoutRepository
                        .existsByLotIdAndAssignedToIdAndHorseIdNotAndStatusNot(
                                w.getLotId(), newGroomId, horseId, WorkoutStatus.CANCELLED);
                if (busy) {
                    // Chỉ truy vấn dựng thông báo KHI ĐÃ BIẾT CHẮC có vướng.
                    conflicts.add(describeGroomConflict(w.getLotId(), newGroomId, horseId));
                }
            }

            if (!conflicts.isEmpty()) {
                throw new IllegalStateException(String.format(
                        "Không giao được chiến mã này cho Groom đó: một Groom không dắt "
                      + "được 2 chiến mã trong cùng một lot (BR-09).%n%s%n"
                      + "Hãy dời lot sang khung giờ khác, hoặc chọn Groom khác.",
                        String.join(System.lineSeparator(), conflicts)));
            }
        }

        // ---------- BƯỚC 2: GHI ----------
        //
        // findFutureScheduledByHorse chỉ trả buổi SCHEDULED và lot_date >= hôm nay.
        // Buổi đã COMPLETED giữ nguyên Groom cũ — đó là LỊCH SỬ, ghi nhận ai
        // thực sự đã dắt hôm đó, không được sửa lại.
        future.forEach(w -> w.setAssignedToId(newGroomId));
        workoutRepository.saveAll(future);

        return future.size();
    }

    /** Dựng câu mô tả một lot bị vướng BR-09. Chỉ gọi khi đã biết chắc có vướng. */
    private String describeGroomConflict(Long lotId, Long newGroomId, Long movingHorseId) {
        TrainingLot lot = lotRepository.findById(lotId).orElse(null);

        String otherHorse = workoutRepository
                .findByLotIdAndStatusNot(lotId, WorkoutStatus.CANCELLED).stream()
                .filter(x -> newGroomId.equals(x.getAssignedToId())
                          && !movingHorseId.equals(x.getHorseId()))
                .findFirst()
                .map(x -> horseRepository.findById(x.getHorseId())
                        .map(Horse::getName)
                        .orElse("#" + x.getHorseId()))
                .orElse("(không xác định)");

        return lot == null
                ? String.format("  · lot #%d: Groom này đang dắt '%s'", lotId, otherHorse)
                : String.format("  · %s lúc %s (lot #%d): Groom này đang dắt '%s'",
                        lot.getLotDate(), lot.getStartTime(), lotId, otherHorse);
    }

    // =================================================================
    // ĐỢT 7 — TIẾN ĐỘ, BIỂU ĐỒ THỂ LỰC & CẢNH BÁO
    // =================================================================

    /**
     * Lấy chuỗi thời gian các buổi tập đã hoàn thành của một chiến mã (FE-7.2).
     */
    public List<HorseFitnessTrendItemResponse> getFitnessTrend(Long horseId, LocalDate fromDate, LocalDate toDate) {
        // Bỏ trống thì dùng mốc bao trùm, để câu truy vấn luôn có BETWEEN cụ thể.
        // Cách này tránh phải viết "(:from IS NULL OR ...)" trong JPQL — kiểu đó
        // hay vướng lỗi ép kiểu tham số null trên PostgreSQL.
        LocalDate from = fromDate != null ? fromDate : LocalDate.of(1900, 1, 1);
        LocalDate to = toDate != null ? toDate : LocalDate.of(2999, 12, 31);

        List<Object[]> rows = workoutRepository.findCompletedWorkoutsWithLotAsc(horseId, from, to);
        List<HorseFitnessTrendItemResponse> items = new ArrayList<>();

        for (Object[] row : rows) {
            TrainingWorkout w = (TrainingWorkout) row[0];
            TrainingLot lot = (TrainingLot) row[1];
            Subject subject = subjectRepository.findById(lot.getSubjectId()).orElse(null);

            HorseFitnessTrendItemResponse item = new HorseFitnessTrendItemResponse();
            item.setWorkoutId(w.getId());
            item.setDate(lot.getLotDate());
            item.setSubjectName(subject != null ? subject.getName() : "Bài tập #" + lot.getSubjectId());
            item.setDistanceMeters(w.getActualDistanceMeters());
            item.setActualDurationMinutes(w.getActualDurationMinutes());
            item.setAverageSpeedKmh(w.getAverageSpeedKmh());
            item.setTopSpeedKmh(w.getTopSpeedKmh());
            item.setAverageHeartRate(w.getAverageHeartRate());
            item.setMaxHeartRate(w.getMaxHeartRate());
            item.setRecoveryHeartRate(w.getRecoveryHeartRate());
            item.setPerformanceRating(w.getPerformanceRating());
            items.add(item);
        }
        return items;
    }

    /**
     * Bộ 5 luật cảnh báo nguy cơ chấn thương và suy giảm thể lực.
     * Tính toán động theo dữ liệu thời gian thực, không lưu bảng.
     */
    public List<HorseAlertResponse> getHorseAlerts(Long horseId) {
        List<HorseAlertResponse> alerts = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);

        List<Object[]> allCompleted = workoutRepository.findCompletedWorkoutsWithLotDesc(horseId);
        List<Object[]> recentWorkoutsWithLot = allCompleted.stream()
                .filter(r -> !((TrainingLot) r[1]).getLotDate().isBefore(thirtyDaysAgo))
                .toList();

        // Luật 1: Nhịp tim tối đa vượt ngưỡng (maxHeartRate > 220)
        for (Object[] row : recentWorkoutsWithLot) {
            TrainingWorkout w = (TrainingWorkout) row[0];
            TrainingLot l = (TrainingLot) row[1];
            if (w.getMaxHeartRate() != null && w.getMaxHeartRate() > 220) {
                alerts.add(new HorseAlertResponse(
                        "MAX_HEART_RATE_EXCEEDED",
                        "Nhịp tim tối đa vượt ngưỡng an toàn",
                        String.format("Ghi nhận nhịp tim tối đa %d bpm (ngưỡng an toàn <= 220 bpm) vào ngày %s.",
                                w.getMaxHeartRate(), l.getLotDate()),
                        "DANGER",
                        l.getLotDate().atTime(l.getEndTime() != null ? l.getEndTime() : LocalTime.of(10, 0)),
                        w.getMaxHeartRate().doubleValue(),
                        220.0
                ));
                break; // Chỉ báo mốc gần nhất
            }
        }

        // Luật 2: Hồi phục tim kém (recoveryHeartRate > 100)
        for (Object[] row : recentWorkoutsWithLot) {
            TrainingWorkout w = (TrainingWorkout) row[0];
            TrainingLot l = (TrainingLot) row[1];
            if (w.getRecoveryHeartRate() != null && w.getRecoveryHeartRate() > 100) {
                alerts.add(new HorseAlertResponse(
                        "POOR_RECOVERY_HEART_RATE",
                        "Chỉ số hồi phục tim kém",
                        String.format("Nhịp tim hồi phục sau buổi tập cao bất thường (%d bpm, ngưỡng an toàn <= 100 bpm) vào ngày %s.",
                                w.getRecoveryHeartRate(), l.getLotDate()),
                        "DANGER",
                        l.getLotDate().atTime(l.getEndTime() != null ? l.getEndTime() : LocalTime.of(10, 0)),
                        w.getRecoveryHeartRate().doubleValue(),
                        100.0
                ));
                break;
            }
        }

        // Luật 3: Phong độ tụt liên tục (performanceRating giảm 3 buổi liên tiếp)
        List<TrainingWorkout> ratedWorkouts = recentWorkoutsWithLot.stream()
                .map(r -> (TrainingWorkout) r[0])
                .filter(w -> w.getPerformanceRating() != null)
                .toList();

        if (ratedWorkouts.size() >= 3) {
            int r0 = ratedWorkouts.get(0).getPerformanceRating(); // Buổi mới nhất
            int r1 = ratedWorkouts.get(1).getPerformanceRating(); // Buổi thứ 2
            int r2 = ratedWorkouts.get(2).getPerformanceRating(); // Buổi thứ 3 (cũ nhất trong bộ 3)
            if (r0 < r1 && r1 < r2) {
                alerts.add(new HorseAlertResponse(
                        "CONSECUTIVE_PERFORMANCE_DROP",
                        "Phong độ suy giảm liên tiếp",
                        String.format("Điểm phong độ giảm liên tiếp qua 3 buổi tập gần nhất (%d → %d → %d).",
                                r2, r1, r0),
                        "WARNING",
                        LocalDateTime.now(),
                        (double) r0,
                        (double) r2
                ));
            }
        }

        // ĐÃ BỎ — Luật "khối lượng tăng đột biến" (tổng cự ly tuần này > 1.5 × tuần trước).
        //
        // Lý do: khoá huấn luyện XOAY VÒNG bài tập (orderedSubjects[i % n]), mà
        // mỗi bài có cự ly rất khác nhau — chạy bền 1200m so với đi bộ thả lỏng.
        // Nên tổng cự ly theo tuần dao động mạnh chỉ vì tuần đó rơi vào bài nào,
        // chứ không phản ánh việc tăng tải thật. Ngưỡng 1.5× sẽ kêu vì lịch xoay
        // bài, tạo báo động giả liên tục và làm người dùng mất tin vào cảnh báo.
        //
        // Muốn đo quá tải cho đúng thì phải so sánh TRONG CÙNG MỘT BÀI TẬP, hoặc
        // dùng chỉ số chuẩn hoá theo cường độ — vượt phạm vi đồ án.

        // Luật 4: Sự cố lặp lại (>= 2 báo cáo sự cố trong 14 ngày qua từ Groom)
        //
        // Không còn kiểm null: constructor giờ bắt buộc truyền repository này,
        // nên nếu thiếu thì Spring báo lỗi ngay lúc khởi động chứ không để luật
        // cảnh báo bị bỏ qua âm thầm lúc chạy.
        LocalDateTime fourteenDaysAgoTime = today.minusDays(14).atStartOfDay();
        long incidentCount = incidentReportRepository.countByHorseIdAndReportedAtAfter(horseId, fourteenDaysAgoTime);
        if (incidentCount >= 2) {
            alerts.add(new HorseAlertResponse(
                    "REPEATED_INCIDENTS",
                    "Cảnh báo sự cố sức khỏe lặp lại",
                    String.format("Chiến mã có %d sự cố được Groom ghi nhận trong 14 ngày qua. Cần Thú y kiểm tra chuyên sâu.",
                            incidentCount),
                    "DANGER",
                    LocalDateTime.now(),
                    (double) incidentCount,
                    2.0
            ));
        }

        return alerts;
    }

    /**
     * Bảng tiến độ và thể lực toàn khu của Trainer (FE-7.1).
     */
    public List<TrainerDashboardHorseResponse> getTrainerDashboard(AuthenticatedUser currentUser) {
        Long trainerId = currentUser.getUserId();

        // 1. Lấy tất cả khu vực do Trainer này phụ trách
        Set<Long> myAreaIds = areaRepository.findAll().stream()
                .filter(a -> trainerId.equals(a.getTrainerId()))
                .map(Area::getId)
                .collect(Collectors.toSet());

        if (myAreaIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Lấy tất cả chuồng thuộc các khu vực đó
        List<StableStall> stalls = stableStallRepository.findAll().stream()
                .filter(s -> myAreaIds.contains(s.getAreaId()))
                .toList();

        Map<Long, StableStall> stallMap = stalls.stream()
                .collect(Collectors.toMap(StableStall::getId, s -> s, (s1, s2) -> s1));

        Set<Long> stallIds = stallMap.keySet();

        // 3. Lấy tất cả chiến mã đang ở trong các chuồng đó
        List<Horse> horses = horseRepository.findAll().stream()
                .filter(h -> h.getCurrentStallId() != null && stallIds.contains(h.getCurrentStallId()))
                .toList();

        if (horses.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. Lấy tất cả kế hoạch huấn luyện của Trainer
        List<HorseTrainingPlan> allPlans = planRepository.findAll().stream()
                .filter(p -> trainerId.equals(p.getTrainerId()))
                .toList();

        Map<Long, List<HorseTrainingPlan>> plansByHorse = allPlans.stream()
                .collect(Collectors.groupingBy(HorseTrainingPlan::getHorseId));

        Map<Long, Course> courseMap = courseRepository.findAll().stream()
                .collect(Collectors.toMap(Course::getId, c -> c, (c1, c2) -> c1));

        // Số buổi đã hoàn thành của MỌI kế hoạch — MỘT truy vấn gom.
        // Trước đây gọi findByPlanIdOrderByIdAsc trong vòng lặp qua từng con
        // ngựa: 11 con là 11 truy vấn thừa, và tăng tuyến tính theo quy mô trại.
        Map<Long, Integer> completedByPlan = new HashMap<>();
        Set<Long> planIds = allPlans.stream()
                .map(HorseTrainingPlan::getId)
                .collect(Collectors.toSet());
        if (!planIds.isEmpty()) {
            for (Object[] row : workoutRepository.countByPlanIdsGroupedByStatus(planIds)) {
                if (row[1] == WorkoutStatus.COMPLETED) {
                    completedByPlan.put((Long) row[0], ((Number) row[2]).intValue());
                }
            }
        }

        List<TrainerDashboardHorseResponse> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);

        for (Horse horse : horses) {
            TrainerDashboardHorseResponse resp = new TrainerDashboardHorseResponse();
            resp.setHorseId(horse.getId());
            resp.setHorseName(horse.getName());
            resp.setBreed(horse.getBreed());

            StableStall stall = stallMap.get(horse.getCurrentStallId());
            resp.setStallCode(stall != null ? stall.getStallCode() : "—");

            // Chọn kế hoạch ưu tiên: ACTIVE -> UPCOMING -> COMPLETED mới nhất
            List<HorseTrainingPlan> horsePlans = plansByHorse.getOrDefault(horse.getId(), Collections.emptyList());
            HorseTrainingPlan currentPlan = horsePlans.stream()
                    .filter(p -> p.getStatus() == TrainingPlanStatus.ACTIVE)
                    .findFirst()
                    .orElseGet(() -> horsePlans.stream()
                            .filter(p -> p.getStatus() == TrainingPlanStatus.UPCOMING)
                            .findFirst()
                            .orElseGet(() -> horsePlans.isEmpty() ? null : horsePlans.get(0)));

            if (currentPlan != null) {
                resp.setPlanId(currentPlan.getId());
                resp.setPlanStatus(currentPlan.getStatus().name());
                resp.setStartDate(currentPlan.getStartDate());
                resp.setEndDate(currentPlan.getEndDate());

                Course course = courseMap.get(currentPlan.getCourseId());
                resp.setCourseName(course != null ? course.getName() : "Khoá #" + currentPlan.getCourseId());

                int total = course != null && course.getTotalSessions() != null ? course.getTotalSessions() : 0;
                // Số buổi đã hoàn thành lấy từ bản đồ đếm gom một lượt bên trên,
                // KHÔNG truy vấn lại trong vòng lặp.
                long completedExact = completedByPlan.getOrDefault(currentPlan.getId(), 0);

                resp.setCompletedSessions((int) completedExact);
                resp.setTotalSessions(total > 0 ? total : (int) completedExact);
                double percent = total > 0 ? ((double) completedExact / total) * 100.0 : 0.0;
                resp.setProgressPercent(Math.round(percent * 10.0) / 10.0);
            } else {
                resp.setPlanStatus("NONE");
                resp.setCourseName("Chưa có kế hoạch");
                resp.setCompletedSessions(0);
                resp.setTotalSessions(0);
                resp.setProgressPercent(0.0);
            }

            // Tính điểm phong độ từ các completed workouts của ngựa
            List<Object[]> completedWorkouts = workoutRepository.findCompletedWorkoutsWithLotDesc(horse.getId()).stream()
                    .filter(r -> !((TrainingLot) r[1]).getLotDate().isBefore(thirtyDaysAgo))
                    .toList();
            if (!completedWorkouts.isEmpty()) {
                TrainingWorkout latestW = (TrainingWorkout) completedWorkouts.get(0)[0];
                resp.setLatestPerformanceRating(latestW.getPerformanceRating());

                double avg = completedWorkouts.stream()
                        .map(r -> (TrainingWorkout) r[0])
                        .filter(w -> w.getPerformanceRating() != null)
                        .mapToInt(TrainingWorkout::getPerformanceRating)
                        .average()
                        .orElse(0.0);
                resp.setAvgPerformanceRating30d(Math.round(avg * 10.0) / 10.0);
            }

            // Tính cảnh báo
            List<HorseAlertResponse> horseAlerts = getHorseAlerts(horse.getId());
            resp.setAlertsCount(horseAlerts.size());
            resp.setAlertTitles(horseAlerts.stream().map(HorseAlertResponse::getTitle).toList());

            result.add(resp);
        }

        return result;
    }
}