package com.rtms.backend.training.service;

import com.rtms.backend.training.dto.SessionSummaryResponse;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.config.FarmSchedulePolicy;
import com.rtms.backend.config.TrainingLimits;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.security.Roles;
import com.rtms.backend.training.dto.CourseRunDetailResponse;
import com.rtms.backend.training.dto.CourseRunPreviewResponse;
import com.rtms.backend.training.dto.CourseRunRequest;
import com.rtms.backend.training.dto.CourseRunSummaryResponse;
import com.rtms.backend.training.dto.CourseRunHorseResponse;
import com.rtms.backend.training.dto.CourseRunOverlapResponse;
import com.rtms.backend.training.repository.projection.CourseRunSessionCountRow;
import com.rtms.backend.training.dto.PlannedSessionResponse;
import com.rtms.backend.training.repository.projection.RunningCourseRunRow;
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
import org.springframework.stereotype.Component;
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
 * Phép tính lịch DUY NHẤT cho xem trước và tạo Course Run: kiểm hợp lệ, ngày bắt đầu thật, ngày kết thúc, bài và giờ của từng buổi. Một truy
 * vấn lấy mọi buổi đã có của Trainer trong khoảng ngày của khóa, rồi mỗi ngày quét khe trống riêng nên một bài dài chỉ ảnh hưởng đúng
 * ngày của nó. Không ghi gì vào DB.
 * (The ONE schedule calculation for previewing and creating a Course Run: validation, the real start date, the end date, and the
 * Exercise and time of each Session. One query loads the Trainer's existing Sessions over the course range, then each day scans its
 * own free slots, so a long Exercise only affects its own day. Writes nothing.)
 */
@Component
class CourseRunScheduler {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CourseRunRepository courseRunRepository;
    private final TrainingSessionRepository sessionRepository;
    private final TrainingGroupRepository groupRepository;
    private final TrainingGroupMemberRepository memberRepository;
    private final CourseRepository courseRepository;
    private final CourseExerciseRepository courseExerciseRepository;
    private final Clock clock;

    CourseRunScheduler(CourseRunRepository courseRunRepository,
                       TrainingSessionRepository sessionRepository,
                       TrainingGroupRepository groupRepository,
                       TrainingGroupMemberRepository memberRepository,
                       CourseRepository courseRepository,
                       CourseExerciseRepository courseExerciseRepository,
                       Clock clock) {
        this.courseRunRepository = courseRunRepository;
        this.sessionRepository = sessionRepository;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.courseRepository = courseRepository;
        this.courseExerciseRepository = courseExerciseRepository;
        this.clock = clock;
    }

    /** Mọi thứ cần để tính lịch, đã kiểm hợp lệ. (Everything needed to calculate a schedule, already validated.) */
    record Context(TrainingGroup group, Course course, List<Exercise> exercises,
                           List<TrainingGroupMember> members, Set<TrainingDay> days) {
    }

    /** Một buổi sắp được xếp; slot rỗng khi ngày đó hết khe. (A Session about to be placed; slot is null when the day has none.) */
    record PlannedSession(LocalDate date, Exercise exercise, LocalTime slot) {
    }

    record Schedule(LocalDate startDate, LocalDate endDate, List<PlannedSession> sessions, List<LocalDate> fullDates) {
    }

    // =================================================================
    // KIỂM HỢP LỆ VÀ TÍNH LỊCH (Validation and the schedule calculation)
    // =================================================================

    Context prepare(CourseRunRequest request, AuthenticatedUser currentUser) {
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
    Schedule buildSchedule(Context context, LocalDate requestedStart, Long trainerId) {
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
    List<CourseRun> overlappingCourseRuns(Long groupId, Schedule schedule) {
        return courseRunRepository.findByGroupIdAndStatusIn(groupId, CourseRunStatus.UNFINISHED).stream()
                .filter(courseRun -> !courseRun.getStartDate().isAfter(schedule.endDate()) && !courseRun.getEndDate().isBefore(schedule.startDate()))
                .sorted(Comparator.comparing(CourseRun::getStartDate))
                .toList();
    }

    ApiException courseRunOverlap(List<CourseRun> overlapping) {
        Map<Long, String> names = courseNames(overlapping.stream().map(CourseRun::getCourseId).toList());
        String described = overlapping.stream()
                .map(courseRun -> String.format("'%s' (%s to %s)", names.get(courseRun.getCourseId()),
                        courseRun.getStartDate().format(DAY_FORMAT), courseRun.getEndDate().format(DAY_FORMAT)))
                .collect(Collectors.joining(", "));
        return new ApiException(HttpStatus.CONFLICT, "COURSE_RUN_OVERLAP",
                "This group is already busy with " + described + ". Choose a start date after it ends!");
    }

    ApiException scheduleFull(List<LocalDate> fullDates) {
        String days = fullDates.stream().map(day -> day.format(DAY_FORMAT)).collect(Collectors.joining(", "));
        return new ApiException(HttpStatus.CONFLICT, "SCHEDULE_FULL",
                "Your schedule is full on " + days + ". Choose other dates or weekdays!");
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private Map<Long, String> courseNames(Collection<Long> courseIds) {
        return courseRepository.findAllById(new LinkedHashSet<>(courseIds)).stream()
                .collect(Collectors.toMap(Course::getId, Course::getName));
    }
}
