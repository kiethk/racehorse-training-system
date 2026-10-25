package com.rtms.backend.training.service;

import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.config.FarmSchedulePolicy;
import com.rtms.backend.training.dto.CreateHorseTrainingPlanRequest;
import com.rtms.backend.training.dto.HorseEnrollmentRequest;
import com.rtms.backend.training.dto.HorsePlanAvailability;
import com.rtms.backend.training.dto.HorseStallInfo;
import com.rtms.backend.training.dto.HorseTrainingPlanDetailResponse;
import com.rtms.backend.training.dto.JoinableCohortResponse;
import com.rtms.backend.training.dto.JoinableCohortRow;
import com.rtms.backend.training.dto.PlanNamesRow;
import com.rtms.backend.training.dto.PlanPreviewRequest;
import com.rtms.backend.training.dto.PlanPreviewResponse;
import com.rtms.backend.training.dto.PlanSummaryResponse;
import com.rtms.backend.training.dto.PlanWorkoutItemResponse;
import com.rtms.backend.training.dto.RunningPlanRow;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.HorseTrainingPlan;
import com.rtms.backend.training.entity.Subject;
import com.rtms.backend.training.enums.PlanAvailability;
import com.rtms.backend.training.enums.TrainingDay;
import com.rtms.backend.training.enums.TrainingPlanStatus;
import com.rtms.backend.training.repository.CourseRepository;
import com.rtms.backend.training.repository.CourseSubjectRepository;
import com.rtms.backend.training.repository.HorseTrainingPlanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Kế hoạch huấn luyện: xem danh sách và chi tiết, lập kế hoạch theo nhóm, gợi ý nhóm ghi danh chung.
 * (Training plans: list and detail views, creating plans for a group, suggesting cohorts to join.)
 *
 * Phần buổi tập nằm ở TrainingWorkoutService; phần theo dõi thể lực nằm ở HorsePerformanceService.
 * (Workouts live in TrainingWorkoutService; fitness tracking lives in HorsePerformanceService.)
 */
@Service
public class HorseTrainingPlanService {

    private final HorseTrainingPlanRepository planRepository;
    private final CourseRepository courseRepository;
    private final CourseSubjectRepository courseSubjectRepository;
    private final HorseRepository horseRepository;
    private final TrainingDecisionService trainingDecisionService;
    private final TrainingWorkoutService workoutService;

    /**
     * CỐ Ý chỉ có MỘT constructor: một constructor dựng được object thiếu chức năng mà không báo lỗi là thứ
     * phải tránh, thà để trình biên dịch bắt lỗi.
     * (INTENTIONALLY a single constructor: a constructor that builds a half-working object without any error
     * must be avoided; better let the compiler catch the mistake.)
     */
    public HorseTrainingPlanService(HorseTrainingPlanRepository planRepository,
                                    CourseRepository courseRepository,
                                    CourseSubjectRepository courseSubjectRepository,
                                    HorseRepository horseRepository,
                                    TrainingDecisionService trainingDecisionService,
                                    TrainingWorkoutService workoutService) {
        this.planRepository = planRepository;
        this.courseRepository = courseRepository;
        this.courseSubjectRepository = courseSubjectRepository;
        this.horseRepository = horseRepository;
        this.trainingDecisionService = trainingDecisionService;
        this.workoutService = workoutService;
    }

    // =================================================================
    // XEM KẾ HOẠCH (Viewing plans)
    // =================================================================

    /**
     * Danh sách kế hoạch cho màn hình tra cứu, MỘT câu SQL kèm tên ngựa, tên khóa và tiến độ.
     * (Plan list for the lookup screen, ONE SQL statement with horse name, course name and progress.)
     *
     * HEAD_TRAINER chỉ thấy kế hoạch của chính mình, kể cả khi lọc theo horseId; vai khác thấy tất cả.
     * (A HEAD_TRAINER only sees their own plans, even when filtering by horseId; other roles see everything.)
     */
    @Transactional(readOnly = true)
    public List<PlanSummaryResponse> getPlanSummaries(Long horseId, AuthenticatedUser currentUser) {
        Long trainerId = "HEAD_TRAINER".equalsIgnoreCase(currentUser.getRole()) ? currentUser.getUserId() : null;
        return planRepository.findPlanSummaries(trainerId, horseId);
    }

    /**
     * Chi tiết một kế hoạch. Owner chỉ xem kế hoạch của ngựa mình; Trainer chỉ xem kế hoạch do mình lập.
     * Không đủ quyền thì báo "không tìm thấy" để không lộ kế hoạch có tồn tại.
     * (Detail of one plan. An owner only sees plans of their own horses; a trainer only plans they created.
     * Without access the plan is reported as "not found" so its existence is not revealed.)
     */
    @Transactional(readOnly = true)
    public HorseTrainingPlanDetailResponse getPlanById(Long id, AuthenticatedUser currentUser) {
        HorseTrainingPlan plan = planRepository.findById(id).orElseThrow(() -> planNotFound(id));
        assertCanViewPlan(plan, currentUser);

        PlanNamesRow names = planRepository.findPlanNames(id).orElseThrow(() -> planNotFound(id));
        return new HorseTrainingPlanDetailResponse(
                plan, workoutService.buildWorkoutItems(id), names.horseName(), names.courseName());
    }

    private void assertCanViewPlan(HorseTrainingPlan plan, AuthenticatedUser currentUser) {
        boolean allowed = switch (currentUser.getRole()) {
            case "HORSE_OWNER" -> planRepository.existsPlanOfOwner(plan.getId(), currentUser.getUserId());
            case "HEAD_TRAINER" -> plan.getTrainerId().equals(currentUser.getUserId());
            default -> true;
        };
        if (!allowed) {
            throw planNotFound(plan.getId());
        }
    }

    private RuntimeException planNotFound(Long id) {
        return new RuntimeException("Training plan #" + id + " not found");
    }

    // =================================================================
    // LẬP KẾ HOẠCH (Creating plans)
    // =================================================================

    /**
     * GHI DANH THEO NHÓM + SINH LOT + SINH BUỔI TẬP.
     * (GROUP ENROLLMENT + GENERATING LOTS + GENERATING WORKOUTS.)
     *
     * Trả về danh sách plan, mỗi chiến mã một plan riêng: dữ liệu KHÔNG gộp, chỉ thao tác tạo được gộp.
     * Đề tài yêu cầu "giáo án cho từng con ngựa", và mỗi con vẫn có hồ sơ, chỉ số, nhận xét riêng.
     * (Returns one plan per horse: the data is NOT merged, only the creation step is. The project requires a
     * "plan for each horse", and each horse keeps its own record, metrics and feedback.)
     */
    @Transactional
    public List<HorseTrainingPlanDetailResponse> createPlan(CreateHorseTrainingPlanRequest request,
                                                            AuthenticatedUser currentUser) {
        Long trainerId = currentUser.getUserId();
        validateCreatePlanRequest(request);

        Set<TrainingDay> selectedDays = new LinkedHashSet<>(request.getTrainingDays());
        Course course = findOwnCourse(request.getCourseId(), trainerId);
        List<Subject> orderedSubjects = loadOrderedSubjects(course);
        List<EnrolledHorse> enrolled = enrollHorses(request.getHorses(), trainerId);

        // Cùng cách tính với previewPlan, nên ngày Trainer thấy trên màn hình đúng là ngày được lưu.
        // (The same calculation as previewPlan, so the dates the trainer saw on screen are the dates stored.)
        LocalDate startDate = TrainingCalendar.firstSessionOnOrAfter(request.getStartDate(), selectedDays);
        LocalDate endDate = TrainingCalendar.endDate(startDate, selectedDays, course.getTotalSessions());
        assertNoOverlappingPlans(enrolled, startDate, endDate);

        Map<Long, HorseTrainingPlan> plansByHorse =
                savePlans(request, course, trainerId, selectedDays, startDate, endDate, enrolled);
        Map<Long, List<PlanWorkoutItemResponse>> workoutsByHorse = workoutService.createWorkouts(
                new TrainingWorkoutService.PlanSchedule(trainerId, startDate, selectedDays,
                        course.getTotalSessions(), orderedSubjects),
                enrolled, plansByHorse);

        return buildPlanResponses(enrolled, plansByHorse, workoutsByHorse);
    }

    private void validateCreatePlanRequest(CreateHorseTrainingPlanRequest request) {
        if (request.getHorses() == null || request.getHorses().isEmpty()) {
            throw new IllegalArgumentException("At least 1 horse must be enrolled!");
        }
        validateSchedule(request.getCourseId(), request.getStartDate(), request.getTrainingDays());
    }

    /** Ba trường đồng bộ, dùng chung cho tạo và xem trước. (The three sync fields, shared by create and preview.) */
    private void validateSchedule(Long courseId, LocalDate startDate, List<TrainingDay> trainingDays) {
        if (courseId == null) {
            throw new IllegalArgumentException("A training course must be selected!");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("Start date must not be empty!");
        }
        // Lịch trong quá khứ sinh ra lot và buổi tập cho những ngày đã qua.
        // (A past schedule creates lots and workouts for days already gone.)
        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past!");
        }
        // KHÔNG mặc định ngầm T2-4-6: giá trị này được LƯU vào DB và trở thành dữ liệu thật của hồ sơ.
        // (NO silent Mon-Wed-Fri default: this value is STORED in the DB and becomes real record data.)
        if (trainingDays == null || trainingDays.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least 1 weekday must be selected for training (for example MONDAY, WEDNESDAY, FRIDAY)!");
        }
    }

    private Course findCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Training course #" + courseId + " not found"));
    }

    /** Khóa của chính Trainer này hoặc khóa dùng chung. (A course of this trainer, or a shared one.) */
    private Course findOwnCourse(Long courseId, Long trainerId) {
        Course course = findCourse(courseId);
        if (course.getTrainerId() != null && !course.getTrainerId().equals(trainerId)) {
            throw new IllegalArgumentException("You can only enroll horses into your own training courses!");
        }
        return course;
    }

    /** Các bài tập của khóa theo đúng thứ tự, bằng một câu JOIN. (The course subjects in order, with one JOIN.) */
    private List<Subject> loadOrderedSubjects(Course course) {
        List<Subject> subjects = courseSubjectRepository.findSubjectsOfCourseInOrder(course.getId());
        if (subjects.isEmpty()) {
            throw new IllegalStateException("This course has no subjects yet, so it cannot be assigned to horses!");
        }
        return subjects;
    }

    /**
     * Chuẩn hóa và kiểm tra từng chiến mã: tồn tại, không trùng, được phép tập (BR-05), có Groom.
     * Ngựa, chuồng và khu được nạp bằng 2 truy vấn cho cả nhóm thay vì 3 truy vấn mỗi con.
     * (Normalizes and validates each horse: exists, no duplicate, allowed to train (BR-05), has a groom.
     * Horses, stalls and areas are loaded with 2 queries for the whole group instead of 3 per horse.)
     */
    private List<EnrolledHorse> enrollHorses(List<HorseEnrollmentRequest> requests, Long trainerId) {
        Set<Long> horseIds = new LinkedHashSet<>();
        for (HorseEnrollmentRequest e : requests) {
            if (e.getHorseId() == null) {
                throw new IllegalArgumentException("Missing horseId in the enrollment list!");
            }
            if (!horseIds.add(e.getHorseId())) {
                throw new IllegalArgumentException(
                        "Horse #" + e.getHorseId() + " is enrolled more than once in the same request!");
            }
        }

        Map<Long, Horse> horsesById = horseRepository.findAllById(horseIds).stream()
                .collect(Collectors.toMap(Horse::getId, Function.identity()));
        Map<Long, HorseStallInfo> stallInfoByHorse = planRepository.findStallInfoByHorseIds(horseIds).stream()
                .collect(Collectors.toMap(HorseStallInfo::horseId, Function.identity()));

        List<EnrolledHorse> enrolled = new ArrayList<>();
        for (HorseEnrollmentRequest e : requests) {
            Horse horse = horsesById.get(e.getHorseId());
            if (horse == null) {
                throw new RuntimeException("Horse #" + e.getHorseId() + " not found");
            }

            // BR-05: ngựa phải được nhận vào và không bị Thú y chặn tập; lỗi nêu rõ lý do và ngày khám lại.
            // (BR-05: the horse must be admitted and not blocked by the vet; the error states the reason and
            // the re-examination date.)
            trainingDecisionService.assertCanTrain(horse);

            Long groomId = resolveGroom(horse, stallInfoByHorse.get(horse.getId()), e.getGroomId(), trainerId);
            enrolled.add(new EnrolledHorse(horse, groomId));
        }
        return enrolled;
    }

    /**
     * Phân giải Groom cho một chiến mã, đồng thời kiểm tra quyền: Trainer chỉ lập kế hoạch cho ngựa trong khu
     * của mình. Chuỗi quan hệ: Horse.currentStallId -> StableStall.areaId -> Area.trainerId, và
     * StableStall.groomId.
     * (Resolves the groom of a horse and checks permission: a trainer only plans horses in their own area.
     * Relation chain: Horse.currentStallId -> StableStall.areaId -> Area.trainerId, and StableStall.groomId.)
     */
    private Long resolveGroom(Horse horse, HorseStallInfo stall, Long overrideGroomId, Long trainerId) {
        if (stall == null) {
            throw new IllegalStateException(String.format(
                    "Horse '%s' has not been assigned to a stall. Assign a stall "
                  + "(PUT /api/horses/%d/assign-stall) before creating a training plan!",
                    horse.getName(), horse.getId()));
        }
        if (stall.areaTrainerId() == null) {
            throw new IllegalStateException(String.format(
                    "Area '%s' is not assigned to any trainer. Contact the club manager to assign the area!",
                    stall.areaCode()));
        }
        if (!stall.areaTrainerId().equals(trainerId)) {
            throw new AccessDeniedException(String.format(
                    "Horse '%s' is in area '%s', which you do not manage!",
                    horse.getName(), stall.areaCode()));
        }

        // Groom suy từ chuồng, cho phép ghi đè. Từ V63 việc phân công Groom cho chuồng là của Quản lý, nên
        // thông báo không hướng người dùng tới hành động họ không có quyền làm.
        // (The groom comes from the stall and may be overridden. Since V63 assigning a groom to a stall is the
        // manager job, so the message does not send the user to an action they cannot perform.)
        Long groomId = (overrideGroomId != null) ? overrideGroomId : stall.stallGroomId();
        if (groomId == null) {
            throw new IllegalStateException(String.format(
                    "Stall '%s' of horse '%s' has no groom assigned. Contact the club manager to assign one, "
                  + "or choose a groom directly for the workouts when creating the plan!",
                    stall.stallCode(), horse.getName()));
        }
        return groomId;
    }

    /**
     * BR-01: không chồng khoảng ngày với kế hoạch đang chạy hoặc sắp chạy. Một truy vấn cho cả nhóm, và báo MỌI con bị
     * vướng cùng MỌI kế hoạch bị chồng trong một lần: báo từng cái một khiến Trainer dời ngày ra sau cái này rồi lại
     * đụng cái kế tiếp.
     * (BR-01: no date overlap with a running or upcoming plan. One query for the whole group, and EVERY clashing horse
     * with EVERY overlapped plan is reported at once: reporting one at a time makes the trainer move past one plan
     * only to hit the next.)
     */
    private void assertNoOverlappingPlans(List<EnrolledHorse> enrolled, LocalDate startDate, LocalDate endDate) {
        List<Long> horseIds = enrolled.stream().map(en -> en.horse().getId()).toList();
        Map<Long, List<RunningPlanRow>> conflictsByHorse = planRepository.findRunningPlans(horseIds, startDate).stream()
                .filter(plan -> plan.overlaps(startDate, endDate))
                .collect(Collectors.groupingBy(RunningPlanRow::horseId));
        if (conflictsByHorse.isEmpty()) {
            return;
        }

        String details = enrolled.stream()
                .filter(en -> conflictsByHorse.containsKey(en.horse().getId()))
                .map(en -> en.horse().getName() + ": " + describeConflicts(conflictsByHorse.get(en.horse().getId())))
                .collect(Collectors.joining("; "));
        throw new ApiException(HttpStatus.CONFLICT, "PLAN_SCHEDULE_CONFLICT", String.format(
                "Schedule conflict: the planned period %s to %s overlaps existing plans. %s.",
                startDate, endDate, details));
    }

    private static String describeConflicts(List<RunningPlanRow> conflicts) {
        return conflicts.stream()
                .map(plan -> String.format("%s (%s, %s to %s)",
                        plan.courseName(), plan.status(), plan.startDate(), plan.endDate()))
                .collect(Collectors.joining(", "));
    }

    /** Tạo plan cho từng con, lưu một lượt. (Creates a plan for each horse, saved in one batch.) */
    private Map<Long, HorseTrainingPlan> savePlans(CreateHorseTrainingPlanRequest request, Course course,
                                                   Long trainerId, Set<TrainingDay> selectedDays,
                                                   LocalDate startDate, LocalDate endDate,
                                                   List<EnrolledHorse> enrolled) {
        TrainingPlanStatus status = startDate.isAfter(LocalDate.now())
                ? TrainingPlanStatus.UPCOMING
                : TrainingPlanStatus.ACTIVE;

        List<HorseTrainingPlan> plans = new ArrayList<>();
        for (EnrolledHorse en : enrolled) {
            HorseTrainingPlan plan = new HorseTrainingPlan();
            plan.setHorseId(en.horse().getId());
            plan.setCourseId(course.getId());
            plan.setTrainerId(trainerId);
            plan.setStartDate(startDate);
            plan.setEndDate(endDate);
            plan.setTrainingDays(selectedDays);        // KHUÔN MẪU (TEMPLATE)
            plan.setGroomId(en.groomId());             // KHUÔN MẪU (TEMPLATE)
            plan.setStatus(status);
            plans.add(plan);
        }

        Map<Long, HorseTrainingPlan> plansByHorse = new LinkedHashMap<>();
        for (HorseTrainingPlan saved : planRepository.saveAllAndFlush(plans)) {
            plansByHorse.put(saved.getHorseId(), saved);
        }
        return plansByHorse;
    }

    private List<HorseTrainingPlanDetailResponse> buildPlanResponses(
            List<EnrolledHorse> enrolled,
            Map<Long, HorseTrainingPlan> plansByHorse,
            Map<Long, List<PlanWorkoutItemResponse>> workoutsByHorse) {
        List<HorseTrainingPlanDetailResponse> result = new ArrayList<>();
        for (EnrolledHorse en : enrolled) {
            Long horseId = en.horse().getId();
            result.add(new HorseTrainingPlanDetailResponse(
                    plansByHorse.get(horseId), workoutsByHorse.get(horseId)));
        }
        return result;
    }

    // =================================================================
    // XEM TRƯỚC KẾ HOẠCH (Previewing a plan)
    // =================================================================

    /**
     * Cho Trainer thấy hậu quả TRƯỚC khi bấm tạo: khóa chạy từ ngày nào tới ngày nào, từng con trong khu rảnh hay bận
     * (bận vì kế hoạch nào, rảnh lại từ ngày nào), và ngày sớm nhất cả nhóm đang chọn cùng rảnh. Không ghi gì xuống
     * DB; createPlan vẫn kiểm lại lần cuối vì hai Trainer có thể thao tác cùng lúc.
     * (Shows the trainer the outcome BEFORE creating: from which date to which date the course runs, whether each horse
     * of the area is free or busy (busy with which plan, free again from when), and the earliest date the selected
     * group is free together. Nothing is written; createPlan still checks once more because two trainers may act at
     * the same time.)
     *
     * Số truy vấn là HẰNG SỐ, không phụ thuộc số ngựa: ngựa trong khu, chuồng, kế hoạch đang chạy, cộng một truy vấn
     * lịch khám lại cho mỗi con đang bị chặn tập.
     * (The number of queries is CONSTANT, independent of the horse count: area horses, stalls, running plans, plus one
     * re-examination lookup per horse that is blocked from training.)
     */
    @Transactional(readOnly = true)
    public PlanPreviewResponse previewPlan(PlanPreviewRequest request, AuthenticatedUser currentUser) {
        Long trainerId = currentUser.getUserId();
        validateSchedule(request.getCourseId(), request.getStartDate(), request.getTrainingDays());

        Set<TrainingDay> days = new LinkedHashSet<>(request.getTrainingDays());
        int totalSessions = findOwnCourse(request.getCourseId(), trainerId).getTotalSessions();
        LocalDate startDate = TrainingCalendar.firstSessionOnOrAfter(request.getStartDate(), days);
        LocalDate endDate = TrainingCalendar.endDate(startDate, days, totalSessions);
        PlanWindow window = new PlanWindow(days, totalSessions, startDate, endDate);

        List<Horse> horses = planRepository.findEnrollableHorses(trainerId);
        if (horses.isEmpty()) {
            return new PlanPreviewResponse(startDate, endDate, totalSessions, null, List.of());
        }

        List<Long> horseIds = horses.stream().map(Horse::getId).toList();
        Map<Long, HorseStallInfo> stallByHorse = planRepository.findStallInfoByHorseIds(horseIds).stream()
                .collect(Collectors.toMap(HorseStallInfo::horseId, Function.identity()));
        Map<Long, List<RunningPlanRow>> plansByHorse = planRepository.findRunningPlans(horseIds, startDate).stream()
                .collect(Collectors.groupingBy(RunningPlanRow::horseId));

        List<HorsePlanAvailability> availability = horses.stream()
                .map(horse -> availabilityOf(horse, stallByHorse.get(horse.getId()),
                        plansByHorse.getOrDefault(horse.getId(), List.of()), window))
                .toList();
        LocalDate groupEarliestStart =
                groupEarliestStart(request.getSelectedHorseIds(), availability, plansByHorse, window);
        return new PlanPreviewResponse(startDate, endDate, totalSessions, groupEarliestStart, availability);
    }

    /** Lịch đang xem trước, gói lại để khỏi truyền bốn tham số rời. (The previewed schedule, bundled instead of four loose parameters.) */
    private record PlanWindow(Set<TrainingDay> days, int totalSessions, LocalDate startDate, LocalDate endDate) {
    }

    /**
     * Thứ tự xét giống thứ tự createPlan báo lỗi: bị chặn tập, rồi thiếu Groom, rồi trùng lịch.
     * (Checked in the same order createPlan fails: blocked, then no groom, then overlapping plans.)
     */
    private HorsePlanAvailability availabilityOf(Horse horse, HorseStallInfo stall,
                                                 List<RunningPlanRow> runningPlans, PlanWindow window) {
        String stallCode = stall == null ? null : stall.stallCode();
        if (!horse.canTrain()) {
            return new HorsePlanAvailability(horse.getId(), horse.getName(), horse.getBreed(), stallCode,
                    PlanAvailability.BLOCKED, "Blocked by the vet: " + trainingDecisionService.describeRest(horse),
                    List.of(), null);
        }
        if (stall == null || stall.stallGroomId() == null) {
            return new HorsePlanAvailability(horse.getId(), horse.getName(), horse.getBreed(), stallCode,
                    PlanAvailability.NO_GROOM,
                    "Stall " + stallCode + " has no groom assigned. Contact the club manager.",
                    List.of(), null);
        }

        List<RunningPlanRow> conflicts = runningPlans.stream()
                .filter(plan -> plan.overlaps(window.startDate(), window.endDate()))
                .toList();
        if (conflicts.isEmpty()) {
            return new HorsePlanAvailability(horse.getId(), horse.getName(), horse.getBreed(), stallCode,
                    PlanAvailability.AVAILABLE, null, List.of(), null);
        }
        LocalDate earliest = TrainingCalendar.earliestFreeStart(
                window.startDate(), window.days(), window.totalSessions(), runningPlans);
        return new HorsePlanAvailability(horse.getId(), horse.getName(), horse.getBreed(), stallCode,
                PlanAvailability.BUSY, "Already enrolled in a plan during this period", conflicts, earliest);
    }

    /**
     * Ngày sớm nhất mọi con đang chọn cùng rảnh TRỌN khóa: gộp kế hoạch bận của cả nhóm rồi tìm như với một con. Con
     * bị chặn tập hay thiếu Groom không chọn được nên bị bỏ qua; null khi nhóm không còn con nào ghi danh được.
     * (The earliest date every selected horse is free for the WHOLE course: the busy plans of the group are merged and
     * searched as for one horse. Blocked or groomless horses cannot be selected and are skipped; null when no
     * enrollable horse is left in the group.)
     */
    private LocalDate groupEarliestStart(List<Long> selectedHorseIds, List<HorsePlanAvailability> availability,
                                         Map<Long, List<RunningPlanRow>> plansByHorse, PlanWindow window) {
        if (selectedHorseIds == null || selectedHorseIds.isEmpty()) {
            return null;
        }
        Set<Long> selected = new HashSet<>(selectedHorseIds);
        List<Long> group = availability.stream()
                .filter(horse -> selected.contains(horse.horseId()))
                .filter(horse -> horse.availability() == PlanAvailability.AVAILABLE
                        || horse.availability() == PlanAvailability.BUSY)
                .map(HorsePlanAvailability::horseId)
                .toList();
        if (group.isEmpty()) {
            return null;
        }
        List<RunningPlanRow> busy = group.stream()
                .flatMap(horseId -> plansByHorse.getOrDefault(horseId, List.of()).stream())
                .toList();
        return TrainingCalendar.earliestFreeStart(window.startDate(), window.days(), window.totalSessions(), busy);
    }

    // =================================================================
    // CHUYỂN TRẠNG THÁI THEO NGÀY (Date-driven status change)
    // =================================================================

    /**
     * Chuyển các kế hoạch đã tới ngày bắt đầu từ UPCOMING sang ACTIVE. Job nửa đêm gọi hàm này; nó chỉ lo
     * phần HIỂN THỊ đúng trạng thái, việc đóng khóa do completeWorkout tự xử lý.
     * (Moves plans whose start date has arrived from UPCOMING to ACTIVE. The midnight job calls this; it only
     * keeps the DISPLAYED status right, while closing the course is handled by completeWorkout.)
     *
     * @return số kế hoạch đã chuyển (number of plans moved)
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

    // =================================================================
    // GỢI Ý NHÓM ĐỂ GHI DANH CHUNG (Suggesting cohorts to enroll together)
    // =================================================================

    /**
     * Liệt kê các nhóm có thể ghi danh chung cho một khóa.
     * (Lists the cohorts that can be enrolled together for a course.)
     *
     * CƠ SỞ TOÁN HỌC: bài của buổi thứ i là courseSubjects[i % n]. Hai con cùng trainingDays thì mỗi ngày tập cả
     * hai cùng tăng i lên 1, nên hiệu (i_A - i_B) KHÔNG BAO GIỜ đổi: đồng pha một lần là đồng pha vĩnh viễn. Con
     * mới luôn bắt đầu ở i = 0, nên điều kiện chỉ còn là i_A ≡ 0 (mod n) vào đúng ngày con mới bắt đầu.
     * (MATH BASIS: the subject of session i is courseSubjects[i % n]. Two horses with the same trainingDays both
     * advance i by 1 on each training day, so the difference (i_A - i_B) NEVER changes: in phase once means in
     * phase forever. A new horse always starts at i = 0, so the only condition is i_A ≡ 0 (mod n) on the day the
     * new horse starts.)
     *
     * Đây KHÔNG phải "nhập giữa chừng": con mới vẫn học từ buổi 0, đủ totalSessions; ta chỉ CHỜ vòng xoay của
     * nhóm quay về vạch xuất phát. Giá phải trả: con mới chờ tối đa (n - 1) buổi, hệ thống chỉ GỢI Ý.
     * (This is NOT "joining mid-way": the new horse still studies from session 0, all totalSessions; we only
     * WAIT for the group rotation to return to the start. The price: the new horse waits at most (n - 1)
     * sessions, and the system only SUGGESTS.)
     */
    @Transactional(readOnly = true)
    public List<JoinableCohortResponse> getJoinableCohorts(Long courseId, AuthenticatedUser currentUser) {
        Course course = findCourse(courseId);

        int subjectCount = (int) courseSubjectRepository.countByCourseId(courseId);
        if (subjectCount == 0) {
            return List.of();
        }

        LocalDate today = LocalDate.now();
        List<JoinableCohortResponse> result = new ArrayList<>();
        for (JoinableCohortRow row : planRepository.findJoinableCohorts(currentUser.getUserId(), courseId)) {
            toJoinableCohort(row, course, subjectCount, today).ifPresent(result::add);
        }

        // Nhóm chung được nhiều buổi nhất lên đầu (The cohort sharing the most sessions comes first)
        result.sort(Comparator.comparing(JoinableCohortResponse::getSharedSessions).reversed());
        return result;
    }

    /** Dựng một gợi ý nhóm, hoặc rỗng nếu nhóm không còn điểm đồng pha. (Builds one suggestion, or empty if the cohort has no aligned point left.) */
    private Optional<JoinableCohortResponse> toJoinableCohort(JoinableCohortRow row, Course course,
                                                              int subjectCount, LocalDate today) {
        Set<TrainingDay> days = row.trainingDays();
        if (days == null || days.isEmpty()) {
            return Optional.empty();   // plan cũ trước V43 chưa có training_days (old plan from before V43)
        }

        int totalSessions = course.getTotalSessions();
        boolean upcoming = row.startDate().isAfter(today);
        LocalDate suggested;
        int sharedSessions;

        if (upcoming) {
            // Nhóm chưa khởi động: nhập luôn, đồng bộ TRỌN khóa (a cohort not started yet: join now, in sync for the WHOLE course)
            suggested = row.startDate();
            sharedSessions = totalSessions;
        } else {
            AlignedPoint point = nextAlignedPoint(row.startDate(), days, subjectCount, totalSessions, today);
            if (point == null) {
                return Optional.empty();   // khóa sắp hết, không còn điểm đồng pha (course almost over, no aligned point)
            }
            suggested = point.date();
            sharedSessions = totalSessions - point.sessionIndex();
        }

        long waitDays = ChronoUnit.DAYS.between(today, suggested);
        return Optional.of(new JoinableCohortResponse(
                upcoming ? "UPCOMING" : "ACTIVE", suggested, days, row.horseCount().intValue(),
                sharedSessions, totalSessions, waitDays, FarmSchedulePolicy.DEFAULT_LOT_CAPACITY));
    }

    private record AlignedPoint(LocalDate date, int sessionIndex) {}

    /**
     * Tìm ngày gần nhất (>= minDate) mà nhóm quay về bài đầu khóa (i % n == 0). Hàm thuần, không đụng DB.
     * (Finds the nearest date (>= minDate) on which the cohort returns to the first subject (i % n == 0).
     * A pure function that does not touch the DB.)
     */
    static AlignedPoint nextAlignedPoint(LocalDate cohortStart,
                                         Set<TrainingDay> days,
                                         int subjectCount,
                                         int totalSessions,
                                         LocalDate minDate) {
        LocalDate cursor = cohortStart;
        int index = 0;
        while (index < totalSessions) {
            if (TrainingCalendar.isTrainingDay(cursor, days)) {
                if (index % subjectCount == 0 && !cursor.isBefore(minDate)) {
                    return new AlignedPoint(cursor, index);
                }
                index++;
            }
            cursor = cursor.plusDays(1);
        }
        return null;
    }
}
