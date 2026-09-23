package com.rtms.backend.service;

import com.rtms.backend.dto.*;
import com.rtms.backend.entity.*;
import com.rtms.backend.enums.TrainingDay;
import com.rtms.backend.enums.TrainingPlanStatus;
import com.rtms.backend.enums.WorkoutStatus;
import com.rtms.backend.repository.*;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

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
                                    AreaRepository areaRepository) {
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
    }

    public List<HorseTrainingPlan> getAllPlans() {
        return planRepository.findAll();
    }

    public List<HorseTrainingPlan> getPlansByHorse(Long horseId) {
        return planRepository.findByHorseId(horseId);
    }

    public HorseTrainingPlanDetailResponse getPlanById(Long id) {
        HorseTrainingPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kế hoạch #" + id));
        return new HorseTrainingPlanDetailResponse(plan, buildWorkoutItems(id));
    }

    /** Gom workout + lot + tên bài tập thành danh sách hiển thị. */
    private List<PlanWorkoutItemResponse> buildWorkoutItems(Long planId) {
        List<PlanWorkoutItemResponse> items = new ArrayList<>();
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

        // Buổi cuối cùng của khoá -> plan tự chuyển COMPLETED
        long remaining = workoutRepository.countByPlanIdAndStatusNot(
                plan.getId(), WorkoutStatus.COMPLETED);
        if (remaining == 0 && plan.getStatus() == TrainingPlanStatus.ACTIVE) {
            plan.setStatus(TrainingPlanStatus.COMPLETED);
            planRepository.save(plan);
        }

        TrainingLot lot = lotRepository.findById(saved.getLotId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lot của buổi tập"));
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

            String note = upcoming
                    ? String.format("Đồng bộ hoàn toàn — chung trọn %d/%d buổi",
                                    sharedSessions, totalSessions)
                    : String.format("Chung %d/%d buổi, %d buổi cuối tập riêng",
                                    sharedSessions, totalSessions, totalSessions - sharedSessions);

            result.add(new JoinableCohortResponse(
                    status, courseId, course.getName(), suggested, days,
                    horseCount, sharedSessions, totalSessions, waitDays, note));
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
}