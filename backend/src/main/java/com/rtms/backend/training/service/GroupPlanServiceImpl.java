package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.config.FarmSchedulePolicy;
import com.rtms.backend.config.TrainingLimits;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.GroupPlanDetailResponse;
import com.rtms.backend.training.dto.GroupPlanPreviewResponse;
import com.rtms.backend.training.dto.GroupPlanRequest;
import com.rtms.backend.training.dto.GroupPlanSummaryResponse;
import com.rtms.backend.training.dto.PlanOverlapResponse;
import com.rtms.backend.training.dto.PlanSessionCountRow;
import com.rtms.backend.training.dto.PlanSessionResponse;
import com.rtms.backend.training.dto.PlannedSessionResponse;
import com.rtms.backend.training.dto.RunningPlanRow;
import com.rtms.backend.training.dto.SessionCountRow;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.GroupPlan;
import com.rtms.backend.training.entity.Subject;
import com.rtms.backend.training.entity.TrainingGroup;
import com.rtms.backend.training.entity.TrainingGroupMember;
import com.rtms.backend.training.entity.TrainingSession;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.SessionStatus;
import com.rtms.backend.training.enums.TrainingDay;
import com.rtms.backend.training.enums.TrainingPlanStatus;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.CourseRepository;
import com.rtms.backend.training.repository.CourseSubjectRepository;
import com.rtms.backend.training.repository.SubjectRepository;
import com.rtms.backend.training.repository.GroupPlanRepository;
import com.rtms.backend.training.repository.TrainingGroupMemberRepository;
import com.rtms.backend.training.repository.TrainingGroupRepository;
import com.rtms.backend.training.repository.TrainingSessionRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import com.rtms.backend.training.service.SessionSlotFinder.Busy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cài đặt GroupPlanService. Mỗi ngày tập được xếp giờ riêng bằng first-fit nên một bài dài chỉ ảnh hưởng đúng ngày của nó.
 * (Implementation of GroupPlanService. Each training day is timed on its own with first-fit, so a long Subject only affects its own
 * day.)
 */
@Service
public class GroupPlanServiceImpl implements GroupPlanService {

    private static final List<TrainingPlanStatus> UNFINISHED = List.of(TrainingPlanStatus.UPCOMING, TrainingPlanStatus.ACTIVE);
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final GroupPlanRepository planRepository;
    private final TrainingSessionRepository sessionRepository;
    private final TrainingWorkoutRepository workoutRepository;
    private final TrainingGroupRepository groupRepository;
    private final TrainingGroupMemberRepository memberRepository;
    private final CourseRepository courseRepository;
    private final CourseSubjectRepository courseSubjectRepository;
    private final SubjectRepository subjectRepository;
    private final SessionSettlementService settlement;
    private final Clock clock;

    public GroupPlanServiceImpl(GroupPlanRepository planRepository,
                                TrainingSessionRepository sessionRepository,
                                TrainingWorkoutRepository workoutRepository,
                                TrainingGroupRepository groupRepository,
                                TrainingGroupMemberRepository memberRepository,
                                CourseRepository courseRepository,
                                CourseSubjectRepository courseSubjectRepository,
                                SubjectRepository subjectRepository,
                                SessionSettlementService settlement,
                                Clock clock) {
        this.planRepository = planRepository;
        this.sessionRepository = sessionRepository;
        this.workoutRepository = workoutRepository;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.courseRepository = courseRepository;
        this.courseSubjectRepository = courseSubjectRepository;
        this.subjectRepository = subjectRepository;
        this.settlement = settlement;
        this.clock = clock;
    }

    /** Mọi thứ cần để tính lịch, đã kiểm hợp lệ. (Everything needed to calculate a schedule, already validated.) */
    private record Context(TrainingGroup group, Course course, List<Subject> subjects,
                           List<TrainingGroupMember> members, Set<TrainingDay> days) {
    }

    /** Một buổi sắp được xếp; slot rỗng khi ngày đó hết khe. (A Session about to be placed; slot is null when the day has none.) */
    private record PlannedSession(LocalDate date, Subject subject, LocalTime slot) {
    }

    private record Schedule(LocalDate startDate, LocalDate endDate, List<PlannedSession> sessions, List<LocalDate> fullDates) {
    }

    // =================================================================
    // XEM TRƯỚC (Preview)
    // =================================================================

    @Override
    @Transactional(readOnly = true)
    public GroupPlanPreviewResponse previewPlan(GroupPlanRequest request, AuthenticatedUser currentUser) {
        Context context = prepare(request, currentUser);
        Schedule schedule = buildSchedule(context, request.startDate(), currentUser.getUserId());
        List<GroupPlan> overlapping = overlappingPlans(context.group().getId(), schedule);
        Map<Long, String> courseNames = courseNames(overlapping.stream().map(GroupPlan::getCourseId).toList());

        List<RunningPlanRow> busy = planRepository.findByGroupIdAndStatusIn(context.group().getId(), UNFINISHED).stream()
                .map(plan -> new RunningPlanRow(plan.getId(), null, courseNames.getOrDefault(plan.getCourseId(), ""),
                        plan.getStartDate(), plan.getEndDate(), plan.getStatus()))
                .toList();
        LocalDate earliest = TrainingCalendar.earliestFreeStart(
                request.startDate(), context.days(), context.course().getTotalSessions(), busy);

        List<PlannedSessionResponse> sessions = schedule.sessions().stream()
                .map(planned -> new PlannedSessionResponse(planned.date(), planned.subject().getName(),
                        planned.subject().getDurationMinutes(), planned.slot(),
                        planned.slot() == null ? null : planned.slot().plusMinutes(planned.subject().getDurationMinutes())))
                .toList();
        List<PlanOverlapResponse> overlaps = overlapping.stream()
                .map(plan -> new PlanOverlapResponse(plan.getId(), courseNames.getOrDefault(plan.getCourseId(), ""),
                        plan.getStartDate(), plan.getEndDate(), plan.getStatus().name()))
                .toList();

        return new GroupPlanPreviewResponse(schedule.startDate(), schedule.endDate(), context.members().size(), sessions,
                schedule.fullDates(), overlaps, earliest, overlaps.isEmpty() && schedule.fullDates().isEmpty());
    }

    // =================================================================
    // TẠO (Create)
    // =================================================================

    @Override
    @Transactional
    public GroupPlanDetailResponse createPlan(GroupPlanRequest request, AuthenticatedUser currentUser) {
        Long trainerId = currentUser.getUserId();

        // Khóa lịch của Trainer TRƯỚC khi đọc bất cứ thứ gì: hai request đồng thời (bấm đúp, hai tab) xếp hàng ở đây, nên request
        // sau thấy plan của request trước và báo trùng lịch thay vì cùng tạo plan chồng nhau.
        // (Lock the trainer schedule BEFORE reading anything: two simultaneous requests (a double click, two tabs) queue here, so
        // the second sees the plans of the first and reports a clash instead of both creating overlapping plans.)
        sessionRepository.lockSchedule(ScheduleLocks.of(trainerId));

        Context context = prepare(request, currentUser);
        Schedule schedule = buildSchedule(context, request.startDate(), trainerId);

        List<GroupPlan> overlapping = overlappingPlans(context.group().getId(), schedule);
        if (!overlapping.isEmpty()) {
            throw planOverlap(overlapping);
        }
        if (!schedule.fullDates().isEmpty()) {
            throw scheduleFull(schedule.fullDates());
        }

        try {
            GroupPlan plan = savePlan(context, schedule, trainerId);
            List<TrainingSession> sessions = saveSessions(plan, schedule, trainerId);
            saveWorkouts(sessions, context.members());
            return detail(plan);
        } catch (DataIntegrityViolationException e) {
            // Khóa lịch đã chặn hai request đồng thời; đây là lớp chặn cuối của DB cho chồng ngày hoặc chồng giờ.
            // (The schedule lock already stops two simultaneous requests; this is the last DB guard for a date or time overlap.)
            throw new ApiException(HttpStatus.CONFLICT, "PLAN_CONFLICT",
                    "The schedule changed at the same time. Reload and try again!");
        }
    }

    private GroupPlan savePlan(Context context, Schedule schedule, Long trainerId) {
        GroupPlan plan = new GroupPlan();
        plan.setGroupId(context.group().getId());
        plan.setCourseId(context.course().getId());
        plan.setTrainerId(trainerId);
        plan.setStartDate(schedule.startDate());
        plan.setEndDate(schedule.endDate());
        plan.setTrainingDays(context.days());
        plan.setStatus(schedule.startDate().isAfter(today()) ? TrainingPlanStatus.UPCOMING : TrainingPlanStatus.ACTIVE);
        return planRepository.saveAndFlush(plan);
    }

    private List<TrainingSession> saveSessions(GroupPlan plan, Schedule schedule, Long trainerId) {
        List<TrainingSession> sessions = new ArrayList<>();
        for (PlannedSession planned : schedule.sessions()) {
            TrainingSession session = new TrainingSession();
            session.setGroupPlanId(plan.getId());
            session.setSubjectId(planned.subject().getId());
            session.setTrainerId(trainerId);
            session.setSessionDate(planned.date());
            session.setStartTime(planned.slot());
            session.setEndTime(planned.slot().plusMinutes(planned.subject().getDurationMinutes()));
            session.setStatus(SessionStatus.SCHEDULED);
            sessions.add(session);
        }
        return sessionRepository.saveAllAndFlush(sessions);
    }

    /** Mỗi thành viên có một Workout trong mỗi buổi; Groom lấy từ ảnh chụp lúc vào nhóm. (Each member gets a Workout in each Session.) */
    private void saveWorkouts(List<TrainingSession> sessions, List<TrainingGroupMember> members) {
        List<TrainingWorkout> workouts = new ArrayList<>();
        for (TrainingSession session : sessions) {
            for (TrainingGroupMember member : members) {
                TrainingWorkout workout = new TrainingWorkout();
                workout.setSessionId(session.getId());
                workout.setHorseId(member.getHorseId());
                workout.setAssignedToId(member.getGroomId());
                workout.setStatus(WorkoutStatus.SCHEDULED);
                workouts.add(workout);
            }
        }
        workoutRepository.saveAllAndFlush(workouts);
    }

    // =================================================================
    // XEM (Reading)
    // =================================================================

    @Override
    @Transactional
    public List<GroupPlanSummaryResponse> getPlans(Long groupId, AuthenticatedUser currentUser) {
        settlement.settle();
        List<GroupPlan> plans = "HEAD_TRAINER".equals(currentUser.getRole())
                ? planRepository.findByTrainerIdOrderByStartDateDesc(currentUser.getUserId())
                : planRepository.findAllByOrderByStartDateDesc();
        if (groupId != null) {
            plans = plans.stream().filter(plan -> groupId.equals(plan.getGroupId())).toList();
        }
        return summaries(plans);
    }

    @Override
    @Transactional
    public GroupPlanDetailResponse getPlan(Long planId, AuthenticatedUser currentUser) {
        settlement.settle();
        GroupPlan plan = planRepository.findById(planId)
                .filter(found -> canSee(found, currentUser))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "GROUP_PLAN_NOT_FOUND", "Plan not found!"));
        return detail(plan);
    }

    @Override
    @Transactional
    public GroupPlanDetailResponse cancelPlan(Long planId, AuthenticatedUser currentUser) {
        sessionRepository.lockSchedule(ScheduleLocks.of(currentUser.getUserId()));
        settlement.settle();
        GroupPlan plan = planRepository.findById(planId)
                .filter(found -> found.getTrainerId().equals(currentUser.getUserId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "GROUP_PLAN_NOT_FOUND", "Plan not found!"));
        if (!UNFINISHED.contains(plan.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "PLAN_ALREADY_CLOSED", "This plan is already closed!");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        sessionRepository.cancelNotStartedOfPlan(planId, now.toLocalDate(), now.toLocalTime());
        workoutRepository.cancelWorkoutsOfCancelledSessions();

        // The bulk updates cleared the persistence context, so the plan is read again before it is closed.
        GroupPlan cancelled = planRepository.findById(planId).orElseThrow();
        cancelled.setStatus(TrainingPlanStatus.CANCELLED);
        return detail(planRepository.saveAndFlush(cancelled));
    }

    /** Trainer chỉ thấy kế hoạch của mình; Quản lý thấy tất cả; vai khác báo "không tìm thấy". (A Trainer sees own plans, a Manager all, others "not found".) */
    private boolean canSee(GroupPlan plan, AuthenticatedUser user) {
        return switch (user.getRole()) {
            case "HEAD_TRAINER" -> plan.getTrainerId().equals(user.getUserId());
            case "CLUB_MANAGER" -> true;
            default -> false;
        };
    }

    private GroupPlanDetailResponse detail(GroupPlan plan) {
        GroupPlanSummaryResponse summary = summaries(List.of(plan)).get(0);
        List<TrainingSession> sessions = sessionRepository.findByGroupPlanIdOrderBySessionDateAscStartTimeAsc(plan.getId());
        Map<Long, Long> horseCounts = workoutRepository
                .countActiveBySession(sessions.stream().map(TrainingSession::getId).toList()).stream()
                .collect(Collectors.toMap(SessionCountRow::sessionId, SessionCountRow::count));
        Map<Long, String> subjectNames = subjectNames(sessions);

        List<PlanSessionResponse> items = sessions.stream()
                .map(session -> new PlanSessionResponse(session.getId(), session.getSessionDate(), session.getStartTime(),
                        session.getEndTime(), subjectNames.get(session.getSubjectId()), session.getStatus().name(),
                        horseCounts.getOrDefault(session.getId(), 0L).intValue()))
                .toList();
        return new GroupPlanDetailResponse(summary, new ArrayList<>(plan.getTrainingDays()), items);
    }

    private List<GroupPlanSummaryResponse> summaries(List<GroupPlan> plans) {
        if (plans.isEmpty()) {
            return List.of();
        }
        List<Long> planIds = plans.stream().map(GroupPlan::getId).toList();
        Map<Long, TrainingGroup> groupsById = groupRepository
                .findAllById(plans.stream().map(GroupPlan::getGroupId).filter(java.util.Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(TrainingGroup::getId, java.util.function.Function.identity()));
        Map<Long, String> courseNames = courseNames(plans.stream().map(GroupPlan::getCourseId).toList());
        Map<Long, Map<SessionStatus, Long>> counts = sessionRepository.countByPlanAndStatus(planIds).stream()
                .collect(Collectors.groupingBy(PlanSessionCountRow::planId,
                        Collectors.toMap(PlanSessionCountRow::status, PlanSessionCountRow::count)));

        return plans.stream().map(plan -> {
            Map<SessionStatus, Long> byStatus = counts.getOrDefault(plan.getId(), Map.of());
            int held = byStatus.getOrDefault(SessionStatus.HELD, 0L).intValue();
            int cancelled = byStatus.getOrDefault(SessionStatus.CANCELLED, 0L).intValue();
            int scheduled = byStatus.getOrDefault(SessionStatus.SCHEDULED, 0L).intValue();
            return new GroupPlanSummaryResponse(plan.getId(), plan.getGroupId(),
                    plan.getGroupId() == null ? null : groupsById.get(plan.getGroupId()).getName(),
                    plan.getGroupId() != null && groupsById.get(plan.getGroupId()).isDisbanded(), plan.getCourseId(),
                    courseNames.get(plan.getCourseId()), plan.getStartDate(), plan.getEndDate(), plan.getStatus().name(),
                    held, cancelled, held + cancelled + scheduled);
        }).toList();
    }

    // =================================================================
    // KIỂM HỢP LỆ VÀ TÍNH LỊCH (Validation and the schedule calculation)
    // =================================================================

    private Context prepare(GroupPlanRequest request, AuthenticatedUser currentUser) {
        Long trainerId = currentUser.getUserId();
        validateDates(request);

        TrainingGroup group = groupRepository.findById(request.groupId())
                .filter(found -> found.getTrainerId().equals(trainerId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND", "Group not found!"));
        if (group.isDisbanded()) {
            throw new ApiException(HttpStatus.CONFLICT, "GROUP_DISBANDED", "Group '" + group.getName() + "' is disbanded!");
        }
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND", "Training course not found!"));
        if (course.getTrainerId() != null && !course.getTrainerId().equals(trainerId)) {
            throw new IllegalArgumentException("You can only assign your own training courses!");
        }
        assertCourseCanBePlanned(course);

        List<Subject> subjects = courseSubjectRepository.findSubjectsOfCourseInOrder(course.getId());
        if (subjects.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "COURSE_HAS_NO_SUBJECTS",
                    "This course has no subjects yet, so it cannot be assigned to a group!");
        }
        List<TrainingGroupMember> members = memberRepository.findByGroupIdOrderByJoinedAtAsc(group.getId());
        if (members.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "GROUP_EMPTY",
                    "Group '" + group.getName() + "' has no horses yet. Add horses before assigning a course!");
        }
        Set<TrainingDay> days = TrainingCalendar.requireWeekdays(new LinkedHashSet<>(request.trainingDays()));
        return new Context(group, course, subjects, members, days);
    }

    private void validateDates(GroupPlanRequest request) {
        LocalDate today = today();
        if (request.startDate().isBefore(today)) {
            throw new IllegalArgumentException("Start date cannot be in the past!");
        }
        if (request.startDate().isAfter(today.plusDays(TrainingLimits.MAX_START_HORIZON_DAYS))) {
            throw new IllegalArgumentException(
                    "Start date cannot be more than " + TrainingLimits.MAX_START_HORIZON_DAYS + " days from today!");
        }
    }

    /** Khóa phải có số buổi trong giới hạn. (A course must have a session count within the limits.) */
    private void assertCourseCanBePlanned(Course course) {
        Integer sessions = course.getTotalSessions();
        if (sessions == null || sessions < TrainingLimits.MIN_COURSE_SESSIONS || sessions > TrainingLimits.MAX_COURSE_SESSIONS) {
            throw new ApiException(HttpStatus.CONFLICT, "COURSE_INVALID_SESSIONS", String.format(
                    "Course '%s' has %s sessions; a course needs between %d and %d sessions to be assigned.",
                    course.getName(), sessions, TrainingLimits.MIN_COURSE_SESSIONS, TrainingLimits.MAX_COURSE_SESSIONS));
        }
    }

    /**
     * Phép tính lịch DUY NHẤT cho xem trước và tạo: ngày bắt đầu thật, ngày kết thúc, bài và giờ của từng buổi. Một truy vấn lấy
     * mọi buổi đã có của Trainer trong khoảng ngày của khóa, rồi mỗi ngày quét khe trống riêng.
     * (The ONE schedule calculation for preview and create: the real start date, the end date, and the Subject and time of each
     * Session. One query loads the Trainer's existing Sessions over the course range, then each day scans its own free slots.)
     */
    private Schedule buildSchedule(Context context, LocalDate requestedStart, Long trainerId) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate start = TrainingCalendar.firstSessionOnOrAfter(requestedStart, context.days());
        if (start.equals(now.toLocalDate()) && !now.toLocalTime().isBefore(FarmSchedulePolicy.GOLDEN_HOURS_END)) {
            throw new ApiException(HttpStatus.CONFLICT, "TRAINING_WINDOW_OVER", String.format(
                    "Today's training window (%s–%s) is over. Choose a later start date.",
                    FarmSchedulePolicy.GOLDEN_HOURS_START, FarmSchedulePolicy.GOLDEN_HOURS_END));
        }
        int total = context.course().getTotalSessions();
        LocalDate end = TrainingCalendar.endDate(start, context.days(), total);

        Map<LocalDate, List<Busy>> busyByDate = sessionRepository
                .findByTrainerIdAndSessionDateBetweenAndStatusNot(trainerId, start, end, SessionStatus.CANCELLED).stream()
                .collect(Collectors.groupingBy(TrainingSession::getSessionDate,
                        Collectors.mapping(session -> new Busy(session.getStartTime(), session.getEndTime()),
                                Collectors.toList())));

        List<PlannedSession> planned = new ArrayList<>();
        List<LocalDate> fullDates = new ArrayList<>();
        LocalDate cursor = start;
        int index = 0;
        while (index < total) {
            if (TrainingCalendar.isTrainingDay(cursor, context.days())) {
                // Xoay vòng bài tập: subjects[i % n] (Rotate the Subjects: subjects[i % n])
                Subject subject = context.subjects().get(index % context.subjects().size());
                LocalTime slot = SessionSlotFinder.firstFreeSlot(busyByDate.getOrDefault(cursor, List.of()),
                        subject.getDurationMinutes(), SessionSlotFinder.earliestStart(cursor, now));
                planned.add(new PlannedSession(cursor, subject, slot));
                if (slot == null) {
                    fullDates.add(cursor);
                }
                index++;
            }
            if (index < total) {
                cursor = cursor.plusDays(1);
            }
        }
        return new Schedule(start, end, planned, fullDates);
    }

    /** Các kế hoạch chưa đóng của nhóm có khoảng ngày giao với lịch mới. (Unfinished plans of the group whose dates meet the new schedule.) */
    private List<GroupPlan> overlappingPlans(Long groupId, Schedule schedule) {
        return planRepository.findByGroupIdAndStatusIn(groupId, UNFINISHED).stream()
                .filter(plan -> !plan.getStartDate().isAfter(schedule.endDate()) && !plan.getEndDate().isBefore(schedule.startDate()))
                .sorted(Comparator.comparing(GroupPlan::getStartDate))
                .toList();
    }

    private ApiException planOverlap(List<GroupPlan> overlapping) {
        Map<Long, String> names = courseNames(overlapping.stream().map(GroupPlan::getCourseId).toList());
        String described = overlapping.stream()
                .map(plan -> String.format("'%s' (%s to %s)", names.get(plan.getCourseId()),
                        plan.getStartDate().format(DAY_FORMAT), plan.getEndDate().format(DAY_FORMAT)))
                .collect(Collectors.joining(", "));
        return new ApiException(HttpStatus.CONFLICT, "PLAN_OVERLAP",
                "This group is already busy with " + described + ". Choose a start date after it ends!");
    }

    private ApiException scheduleFull(List<LocalDate> fullDates) {
        String days = fullDates.stream().map(day -> day.format(DAY_FORMAT)).collect(Collectors.joining(", "));
        return new ApiException(HttpStatus.CONFLICT, "SCHEDULE_FULL",
                "Your schedule is full on " + days + ". Choose other dates or weekdays!");
    }

    // =================================================================
    // TIỆN ÍCH (Helpers)
    // =================================================================

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private Map<Long, String> courseNames(Collection<Long> courseIds) {
        return courseRepository.findAllById(new LinkedHashSet<>(courseIds)).stream()
                .collect(Collectors.toMap(Course::getId, Course::getName));
    }

    private Map<Long, String> subjectNames(List<TrainingSession> sessions) {
        Set<Long> subjectIds = sessions.stream().map(TrainingSession::getSubjectId).collect(Collectors.toSet());
        if (subjectIds.isEmpty()) {
            return Map.of();
        }
        return subjectRepository.findAllById(subjectIds).stream()
                .collect(Collectors.toMap(Subject::getId, Subject::getName));
    }
}
