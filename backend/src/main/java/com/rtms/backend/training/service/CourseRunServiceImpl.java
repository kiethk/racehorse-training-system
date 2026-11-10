package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.config.FarmSchedulePolicy;
import com.rtms.backend.config.TrainingLimits;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CourseRunDetailResponse;
import com.rtms.backend.training.dto.CourseRunPreviewResponse;
import com.rtms.backend.training.dto.CourseRunRequest;
import com.rtms.backend.training.dto.CourseRunSummaryResponse;
import com.rtms.backend.training.dto.CourseRunHorseResponse;
import com.rtms.backend.training.dto.CourseRunOverlapResponse;
import com.rtms.backend.training.dto.CourseRunSessionCountRow;
import com.rtms.backend.training.dto.CourseRunSessionResponse;
import com.rtms.backend.training.dto.PlannedSessionResponse;
import com.rtms.backend.training.dto.RunningCourseRunRow;
import com.rtms.backend.training.dto.SessionCountRow;
import com.rtms.backend.training.dto.SessionHorseOutcomeResponse;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.CourseRun;
import com.rtms.backend.training.entity.Exercise;
import com.rtms.backend.training.entity.TrainingGroup;
import com.rtms.backend.training.entity.TrainingGroupMember;
import com.rtms.backend.training.entity.TrainingSession;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.SessionStatus;
import com.rtms.backend.training.enums.TrainingDay;
import com.rtms.backend.training.enums.CourseRunStatus;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.CourseRepository;
import com.rtms.backend.training.repository.CourseExerciseRepository;
import com.rtms.backend.training.repository.ExerciseRepository;
import com.rtms.backend.training.repository.CourseRunRepository;
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
 * Cài đặt CourseRunService. Mỗi ngày tập được xếp giờ riêng bằng first-fit nên một bài dài chỉ ảnh hưởng đúng ngày của nó.
 * (Implementation of CourseRunService. Each training day is timed on its own with first-fit, so a long Exercise only affects its own
 * day.)
 */
@Service
public class CourseRunServiceImpl implements CourseRunService {

    private static final List<CourseRunStatus> UNFINISHED = List.of(CourseRunStatus.UPCOMING, CourseRunStatus.ACTIVE);
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CourseRunRepository courseRunRepository;
    private final TrainingSessionRepository sessionRepository;
    private final TrainingWorkoutRepository workoutRepository;
    private final TrainingGroupRepository groupRepository;
    private final TrainingGroupMemberRepository memberRepository;
    private final CourseRepository courseRepository;
    private final CourseExerciseRepository courseExerciseRepository;
    private final ExerciseRepository exerciseRepository;
    private final SessionSettlementService settlement;
    private final HorseRepository horseRepository;
    private final Clock clock;

    public CourseRunServiceImpl(CourseRunRepository courseRunRepository,
                                TrainingSessionRepository sessionRepository,
                                TrainingWorkoutRepository workoutRepository,
                                TrainingGroupRepository groupRepository,
                                TrainingGroupMemberRepository memberRepository,
                                CourseRepository courseRepository,
                                CourseExerciseRepository courseExerciseRepository,
                                ExerciseRepository exerciseRepository,
                                SessionSettlementService settlement,
                                HorseRepository horseRepository,
                                Clock clock) {
        this.courseRunRepository = courseRunRepository;
        this.sessionRepository = sessionRepository;
        this.workoutRepository = workoutRepository;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.courseRepository = courseRepository;
        this.courseExerciseRepository = courseExerciseRepository;
        this.exerciseRepository = exerciseRepository;
        this.settlement = settlement;
        this.horseRepository = horseRepository;
        this.clock = clock;
    }

    /** Mọi thứ cần để tính lịch, đã kiểm hợp lệ. (Everything needed to calculate a schedule, already validated.) */
    private record Context(TrainingGroup group, Course course, List<Exercise> exercises,
                           List<TrainingGroupMember> members, Set<TrainingDay> days) {
    }

    /** Một buổi sắp được xếp; slot rỗng khi ngày đó hết khe. (A Session about to be placed; slot is null when the day has none.) */
    private record PlannedSession(LocalDate date, Exercise exercise, LocalTime slot) {
    }

    private record Schedule(LocalDate startDate, LocalDate endDate, List<PlannedSession> sessions, List<LocalDate> fullDates) {
    }

    // =================================================================
    // XEM TRƯỚC (Preview)
    // =================================================================

    @Override
    @Transactional(readOnly = true)
    public CourseRunPreviewResponse previewCourseRun(CourseRunRequest request, AuthenticatedUser currentUser) {
        Context context = prepare(request, currentUser);
        Schedule schedule = buildSchedule(context, request.startDate(), currentUser.getUserId());
        List<CourseRun> overlapping = overlappingCourseRuns(context.group().getId(), schedule);
        Map<Long, String> courseNames = courseNames(overlapping.stream().map(CourseRun::getCourseId).toList());

        List<RunningCourseRunRow> busy = courseRunRepository.findByGroupIdAndStatusIn(context.group().getId(), UNFINISHED).stream()
                .map(courseRun -> new RunningCourseRunRow(courseRun.getId(), null, courseNames.getOrDefault(courseRun.getCourseId(), ""),
                        courseRun.getStartDate(), courseRun.getEndDate(), courseRun.getStatus()))
                .toList();
        LocalDate earliest = TrainingCalendar.earliestFreeStart(
                request.startDate(), context.days(), context.course().getTotalSessions(), busy);

        List<PlannedSessionResponse> sessions = schedule.sessions().stream()
                .map(planned -> new PlannedSessionResponse(planned.date(), planned.exercise().getName(),
                        planned.exercise().getDurationMinutes(), planned.slot(),
                        planned.slot() == null ? null : planned.slot().plusMinutes(planned.exercise().getDurationMinutes())))
                .toList();
        List<CourseRunOverlapResponse> overlaps = overlapping.stream()
                .map(courseRun -> new CourseRunOverlapResponse(courseRun.getId(), courseNames.getOrDefault(courseRun.getCourseId(), ""),
                        courseRun.getStartDate(), courseRun.getEndDate(), courseRun.getStatus().name()))
                .toList();

        return new CourseRunPreviewResponse(schedule.startDate(), schedule.endDate(), context.members().size(), sessions,
                schedule.fullDates(), overlaps, earliest, overlaps.isEmpty() && schedule.fullDates().isEmpty());
    }

    // =================================================================
    // TẠO (Create)
    // =================================================================

    @Override
    @Transactional
    public CourseRunDetailResponse createCourseRun(CourseRunRequest request, AuthenticatedUser currentUser) {
        Long trainerId = currentUser.getUserId();

        // Khóa lịch của Trainer TRƯỚC khi đọc bất cứ thứ gì: hai request đồng thời (bấm đúp, hai tab) xếp hàng ở đây, nên request
        // sau thấy course run của request trước và báo trùng lịch thay vì cùng tạo course run chồng nhau.
        // (Lock the trainer schedule BEFORE reading anything: two simultaneous requests (a double click, two tabs) queue here, so
        // the second sees the course runs of the first and reports a clash instead of both creating overlapping course runs.)
        sessionRepository.lockSchedule(ScheduleLocks.of(trainerId));

        Context context = prepare(request, currentUser);
        Schedule schedule = buildSchedule(context, request.startDate(), trainerId);

        List<CourseRun> overlapping = overlappingCourseRuns(context.group().getId(), schedule);
        if (!overlapping.isEmpty()) {
            throw courseRunOverlap(overlapping);
        }
        if (!schedule.fullDates().isEmpty()) {
            throw scheduleFull(schedule.fullDates());
        }

        try {
            CourseRun courseRun = saveCourseRun(context, schedule, trainerId);
            List<TrainingSession> sessions = saveSessions(courseRun, schedule, trainerId);
            saveWorkouts(sessions, context.members());
            return detail(courseRun);
        } catch (DataIntegrityViolationException e) {
            // Khóa lịch đã chặn hai request đồng thời; đây là lớp chặn cuối của DB cho chồng ngày hoặc chồng giờ.
            // (The schedule lock already stops two simultaneous requests; this is the last DB guard for a date or time overlap.)
            throw new ApiException(HttpStatus.CONFLICT, "COURSE_RUN_CONFLICT",
                    "The schedule changed at the same time. Reload and try again!");
        }
    }

    private CourseRun saveCourseRun(Context context, Schedule schedule, Long trainerId) {
        CourseRun courseRun = new CourseRun();
        courseRun.setGroupId(context.group().getId());
        courseRun.setCourseId(context.course().getId());
        courseRun.setTrainerId(trainerId);
        courseRun.setStartDate(schedule.startDate());
        courseRun.setEndDate(schedule.endDate());
        courseRun.setTrainingDays(context.days());
        courseRun.setStatus(schedule.startDate().isAfter(today()) ? CourseRunStatus.UPCOMING : CourseRunStatus.ACTIVE);
        return courseRunRepository.saveAndFlush(courseRun);
    }

    private List<TrainingSession> saveSessions(CourseRun courseRun, Schedule schedule, Long trainerId) {
        List<TrainingSession> sessions = new ArrayList<>();
        for (PlannedSession planned : schedule.sessions()) {
            TrainingSession session = new TrainingSession();
            session.setCourseRunId(courseRun.getId());
            session.setExerciseId(planned.exercise().getId());
            session.setTrainerId(trainerId);
            session.setSessionDate(planned.date());
            session.setStartTime(planned.slot());
            session.setEndTime(planned.slot().plusMinutes(planned.exercise().getDurationMinutes()));
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
    public List<CourseRunSummaryResponse> getCourseRuns(Long groupId, Long horseId, AuthenticatedUser currentUser) {
        settlement.settle();
        List<CourseRun> courseRuns = "HEAD_TRAINER".equals(currentUser.getRole())
                ? courseRunRepository.findByTrainerIdOrderByStartDateDesc(currentUser.getUserId())
                : courseRunRepository.findAllByOrderByStartDateDesc();
        if (groupId != null) {
            courseRuns = courseRuns.stream().filter(courseRun -> groupId.equals(courseRun.getGroupId())).toList();
        }
        if (horseId != null) {
            // A course run matches when the horse has any Workout in one of its Sessions, cancelled ones included.
            Set<Long> sessionIds = workoutRepository.findByHorseIdAndSessionIdIsNotNull(horseId).stream()
                    .map(TrainingWorkout::getSessionId).collect(Collectors.toSet());
            Set<Long> courseRunIds = sessionRepository.findAllById(sessionIds).stream()
                    .map(TrainingSession::getCourseRunId).collect(Collectors.toSet());
            courseRuns = courseRuns.stream().filter(courseRun -> courseRunIds.contains(courseRun.getId())).toList();
        }
        return summaries(courseRuns);
    }

    @Override
    @Transactional
    public CourseRunDetailResponse getCourseRun(Long courseRunId, AuthenticatedUser currentUser) {
        settlement.settle();
        CourseRun courseRun = courseRunRepository.findById(courseRunId)
                .filter(found -> canSee(found, currentUser))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COURSE_RUN_NOT_FOUND", "Course run not found!"));
        return detail(courseRun);
    }

    @Override
    @Transactional
    public List<CourseRunHorseResponse> getCourseRunHorses(Long courseRunId, AuthenticatedUser currentUser) {
        settlement.settle();
        CourseRun courseRun = visibleCourseRun(courseRunId, currentUser);
        List<TrainingSession> sessions = sessionRepository.findByCourseRunIdOrderBySessionDateAscStartTimeAsc(courseRun.getId());
        int total = (int) sessions.stream().filter(session -> session.getStatus() != SessionStatus.CANCELLED).count();
        List<TrainingWorkout> workouts = workoutRepository.findBySessionIdIn(sessions.stream().map(TrainingSession::getId).toList());
        Map<Long, String> names = horseNames(workouts);
        Map<Long, Long> attended = workouts.stream().filter(workout -> workout.getStatus() == WorkoutStatus.RAN)
                .collect(Collectors.groupingBy(TrainingWorkout::getHorseId, Collectors.counting()));
        return workouts.stream().map(TrainingWorkout::getHorseId).distinct()
                .map(horseId -> new CourseRunHorseResponse(horseId, names.get(horseId), attended.getOrDefault(horseId, 0L).intValue(), total))
                .sorted(Comparator.comparing(CourseRunHorseResponse::horseName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional
    public List<SessionHorseOutcomeResponse> getSessionHorses(Long courseRunId, Long sessionId, AuthenticatedUser currentUser) {
        settlement.settle();
        visibleCourseRun(courseRunId, currentUser);
        TrainingSession session = sessionRepository.findById(sessionId)
                .filter(found -> courseRunId.equals(found.getCourseRunId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Session not found!"));
        List<TrainingWorkout> workouts = workoutRepository.findBySessionId(session.getId());
        Map<Long, String> names = horseNames(workouts);
        return workouts.stream()
                .map(workout -> new SessionHorseOutcomeResponse(workout.getId(), workout.getHorseId(), names.get(workout.getHorseId()),
                        outcome(session, workout)))
                .sorted(Comparator.comparing(SessionHorseOutcomeResponse::horseName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Workout hủy trong buổi đã diễn ra là "Did not run"; hủy trước đó là "Cancelled". (A cancelled Workout in a Session that took place is "Did not run"; otherwise "Cancelled".) */
    private String outcome(TrainingSession session, TrainingWorkout workout) {
        return switch (workout.getStatus()) {
            case SCHEDULED -> "TO_RUN";
            case HELD -> "NEEDS_RESULT";
            case RAN -> "RAN";
            case CANCELLED -> session.getStatus() == SessionStatus.HELD ? "DID_NOT_RUN" : "CANCELLED";
        };
    }

    private Map<Long, String> horseNames(List<TrainingWorkout> workouts) {
        return horseRepository.findAllById(workouts.stream().map(TrainingWorkout::getHorseId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Horse::getId, Horse::getName));
    }

    private CourseRun visibleCourseRun(Long courseRunId, AuthenticatedUser currentUser) {
        return courseRunRepository.findById(courseRunId)
                .filter(found -> canSee(found, currentUser))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COURSE_RUN_NOT_FOUND", "Course run not found!"));
    }

    @Override
    @Transactional
    public CourseRunDetailResponse cancelCourseRun(Long courseRunId, AuthenticatedUser currentUser) {
        sessionRepository.lockSchedule(ScheduleLocks.of(currentUser.getUserId()));
        settlement.settle();
        CourseRun courseRun = courseRunRepository.findById(courseRunId)
                .filter(found -> found.getTrainerId().equals(currentUser.getUserId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COURSE_RUN_NOT_FOUND", "Course run not found!"));
        if (!UNFINISHED.contains(courseRun.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "COURSE_RUN_ALREADY_CLOSED", "This course run is already closed!");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        sessionRepository.cancelNotStartedOfCourseRun(courseRunId, now.toLocalDate(), now.toLocalTime());
        workoutRepository.cancelWorkoutsOfCancelledSessions();

        // The bulk updates cleared the persistence context, so the course run is read again before it is closed.
        CourseRun cancelled = courseRunRepository.findById(courseRunId).orElseThrow();
        cancelled.setStatus(CourseRunStatus.CANCELLED);
        return detail(courseRunRepository.saveAndFlush(cancelled));
    }

    /** Trainer chỉ thấy kế hoạch của mình; Quản lý thấy tất cả; vai khác báo "không tìm thấy". (A Trainer sees own course runs, a Manager all, others "not found".) */
    private boolean canSee(CourseRun courseRun, AuthenticatedUser user) {
        return switch (user.getRole()) {
            case "HEAD_TRAINER" -> courseRun.getTrainerId().equals(user.getUserId());
            case "CLUB_MANAGER" -> true;
            default -> false;
        };
    }

    private CourseRunDetailResponse detail(CourseRun courseRun) {
        CourseRunSummaryResponse summary = summaries(List.of(courseRun)).get(0);
        List<TrainingSession> sessions = sessionRepository.findByCourseRunIdOrderBySessionDateAscStartTimeAsc(courseRun.getId());
        Map<Long, Long> horseCounts = workoutRepository
                .countActiveBySession(sessions.stream().map(TrainingSession::getId).toList()).stream()
                .collect(Collectors.toMap(SessionCountRow::sessionId, SessionCountRow::count));
        Map<Long, String> exerciseNames = exerciseNames(sessions);

        List<CourseRunSessionResponse> items = sessions.stream()
                .map(session -> new CourseRunSessionResponse(session.getId(), session.getSessionDate(), session.getStartTime(),
                        session.getEndTime(), exerciseNames.get(session.getExerciseId()), session.getStatus().name(),
                        horseCounts.getOrDefault(session.getId(), 0L).intValue()))
                .toList();
        return new CourseRunDetailResponse(summary, new ArrayList<>(courseRun.getTrainingDays()), items);
    }

    private List<CourseRunSummaryResponse> summaries(List<CourseRun> courseRuns) {
        if (courseRuns.isEmpty()) {
            return List.of();
        }
        List<Long> courseRunIds = courseRuns.stream().map(CourseRun::getId).toList();
        Map<Long, TrainingGroup> groupsById = groupRepository
                .findAllById(courseRuns.stream().map(CourseRun::getGroupId).filter(java.util.Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(TrainingGroup::getId, java.util.function.Function.identity()));
        Map<Long, String> courseNames = courseNames(courseRuns.stream().map(CourseRun::getCourseId).toList());
        Map<Long, Map<SessionStatus, Long>> counts = sessionRepository.countByCourseRunAndStatus(courseRunIds).stream()
                .collect(Collectors.groupingBy(CourseRunSessionCountRow::courseRunId,
                        Collectors.toMap(CourseRunSessionCountRow::status, CourseRunSessionCountRow::count)));

        return courseRuns.stream().map(courseRun -> {
            Map<SessionStatus, Long> byStatus = counts.getOrDefault(courseRun.getId(), Map.of());
            int held = byStatus.getOrDefault(SessionStatus.HELD, 0L).intValue();
            int cancelled = byStatus.getOrDefault(SessionStatus.CANCELLED, 0L).intValue();
            int scheduled = byStatus.getOrDefault(SessionStatus.SCHEDULED, 0L).intValue();
            return new CourseRunSummaryResponse(courseRun.getId(), courseRun.getGroupId(),
                    courseRun.getGroupId() == null ? null : groupsById.get(courseRun.getGroupId()).getName(),
                    courseRun.getGroupId() != null && groupsById.get(courseRun.getGroupId()).isDisbanded(), courseRun.getCourseId(),
                    courseNames.get(courseRun.getCourseId()), courseRun.getStartDate(), courseRun.getEndDate(), courseRun.getStatus().name(),
                    held, cancelled, held + cancelled + scheduled);
        }).toList();
    }

    // =================================================================
    // KIỂM HỢP LỆ VÀ TÍNH LỊCH (Validation and the schedule calculation)
    // =================================================================

    private Context prepare(CourseRunRequest request, AuthenticatedUser currentUser) {
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

        List<Exercise> exercises = courseExerciseRepository.findExercisesOfCourseInOrder(course.getId());
        if (exercises.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "COURSE_HAS_NO_EXERCISES",
                    "This course has no exercises yet, so it cannot be assigned to a group!");
        }
        List<TrainingGroupMember> members = memberRepository.findByGroupIdAndLeftAtIsNullOrderByJoinedAtAsc(group.getId());
        if (members.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "GROUP_EMPTY",
                    "Group '" + group.getName() + "' has no horses yet. Add horses before assigning a course!");
        }
        Set<TrainingDay> days = TrainingCalendar.requireWeekdays(new LinkedHashSet<>(request.trainingDays()));
        return new Context(group, course, exercises, members, days);
    }

    private void validateDates(CourseRunRequest request) {
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
     * (The ONE schedule calculation for preview and create: the real start date, the end date, and the Exercise and time of each
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
                // Xoay vòng bài tập: exercises[i % n] (Rotate the Exercises: exercises[i % n])
                Exercise exercise = context.exercises().get(index % context.exercises().size());
                LocalTime slot = SessionSlotFinder.firstFreeSlot(busyByDate.getOrDefault(cursor, List.of()),
                        exercise.getDurationMinutes(), SessionSlotFinder.earliestStart(cursor, now));
                planned.add(new PlannedSession(cursor, exercise, slot));
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

    /** Các kế hoạch chưa đóng của nhóm có khoảng ngày giao với lịch mới. (Unfinished course runs of the group whose dates meet the new schedule.) */
    private List<CourseRun> overlappingCourseRuns(Long groupId, Schedule schedule) {
        return courseRunRepository.findByGroupIdAndStatusIn(groupId, UNFINISHED).stream()
                .filter(courseRun -> !courseRun.getStartDate().isAfter(schedule.endDate()) && !courseRun.getEndDate().isBefore(schedule.startDate()))
                .sorted(Comparator.comparing(CourseRun::getStartDate))
                .toList();
    }

    private ApiException courseRunOverlap(List<CourseRun> overlapping) {
        Map<Long, String> names = courseNames(overlapping.stream().map(CourseRun::getCourseId).toList());
        String described = overlapping.stream()
                .map(courseRun -> String.format("'%s' (%s to %s)", names.get(courseRun.getCourseId()),
                        courseRun.getStartDate().format(DAY_FORMAT), courseRun.getEndDate().format(DAY_FORMAT)))
                .collect(Collectors.joining(", "));
        return new ApiException(HttpStatus.CONFLICT, "COURSE_RUN_OVERLAP",
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

    private Map<Long, String> exerciseNames(List<TrainingSession> sessions) {
        Set<Long> exerciseIds = sessions.stream().map(TrainingSession::getExerciseId).collect(Collectors.toSet());
        if (exerciseIds.isEmpty()) {
            return Map.of();
        }
        return exerciseRepository.findAllById(exerciseIds).stream()
                .collect(Collectors.toMap(Exercise::getId, Exercise::getName));
    }
}
