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
import com.rtms.backend.training.repository.CourseRunRepository;
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

    private final CourseRunRepository courseRunRepository;
    private final TrainingSessionRepository sessionRepository;
    private final TrainingWorkoutRepository workoutRepository;
    private final TrainingGroupRepository groupRepository;
    private final CourseRepository courseRepository;
    private final SessionSettlementService settlement;
    private final HorseRepository horseRepository;
    private final SessionSummaryAssembler assembler;
    private final CourseRunScheduler scheduler;
    private final Clock clock;

    public CourseRunServiceImpl(CourseRunRepository courseRunRepository,
                                TrainingSessionRepository sessionRepository,
                                TrainingWorkoutRepository workoutRepository,
                                TrainingGroupRepository groupRepository,
                                CourseRepository courseRepository,
                                SessionSettlementService settlement,
                                HorseRepository horseRepository,
                                SessionSummaryAssembler assembler,
                                CourseRunScheduler scheduler,
                                Clock clock) {
        this.courseRunRepository = courseRunRepository;
        this.sessionRepository = sessionRepository;
        this.workoutRepository = workoutRepository;
        this.groupRepository = groupRepository;
        this.courseRepository = courseRepository;
        this.settlement = settlement;
        this.horseRepository = horseRepository;
        this.assembler = assembler;
        this.scheduler = scheduler;
        this.clock = clock;
    }

    // =================================================================
    // XEM TRƯỚC (Preview)
    // =================================================================

    @Override
    @Transactional(readOnly = true)
    public CourseRunPreviewResponse previewCourseRun(CourseRunRequest request, AuthenticatedUser currentUser) {
        CourseRunScheduler.Context context = scheduler.prepare(request, currentUser);
        CourseRunScheduler.Schedule schedule = scheduler.buildSchedule(context, request.startDate(), currentUser.getUserId());
        List<CourseRun> overlapping = scheduler.overlappingCourseRuns(context.group().getId(), schedule);
        Map<Long, String> courseNames = courseNames(overlapping.stream().map(CourseRun::getCourseId).toList());

        List<RunningCourseRunRow> busy = courseRunRepository.findByGroupIdAndStatusIn(context.group().getId(), CourseRunStatus.UNFINISHED).stream()
                .map(courseRun -> new RunningCourseRunRow(courseRun.getId(), courseNames.getOrDefault(courseRun.getCourseId(), ""),
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

        CourseRunScheduler.Context context = scheduler.prepare(request, currentUser);
        CourseRunScheduler.Schedule schedule = scheduler.buildSchedule(context, request.startDate(), trainerId);

        List<CourseRun> overlapping = scheduler.overlappingCourseRuns(context.group().getId(), schedule);
        if (!overlapping.isEmpty()) {
            throw scheduler.courseRunOverlap(overlapping);
        }
        if (!schedule.fullDates().isEmpty()) {
            throw scheduler.scheduleFull(schedule.fullDates());
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

    private CourseRun saveCourseRun(CourseRunScheduler.Context context, CourseRunScheduler.Schedule schedule, Long trainerId) {
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

    private List<TrainingSession> saveSessions(CourseRun courseRun, CourseRunScheduler.Schedule schedule, Long trainerId) {
        List<TrainingSession> sessions = new ArrayList<>();
        for (CourseRunScheduler.PlannedSession planned : schedule.sessions()) {
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
        List<CourseRun> courseRuns = Roles.HEAD_TRAINER.equals(currentUser.getRole())
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
        if (!CourseRunStatus.UNFINISHED.contains(courseRun.getStatus())) {
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
            case Roles.HEAD_TRAINER -> courseRun.getTrainerId().equals(user.getUserId());
            case Roles.CLUB_MANAGER -> true;
            default -> false;
        };
    }

    private CourseRunDetailResponse detail(CourseRun courseRun) {
        CourseRunSummaryResponse summary = summaries(List.of(courseRun)).get(0);
        List<TrainingSession> sessions = sessionRepository.findByCourseRunIdOrderBySessionDateAscStartTimeAsc(courseRun.getId());
        Map<Long, List<TrainingWorkout>> workoutsBySession = workoutRepository
                .findBySessionIdIn(sessions.stream().map(TrainingSession::getId).toList()).stream()
                .collect(Collectors.groupingBy(TrainingWorkout::getSessionId));
        List<SessionSummaryResponse> items = assembler.summariesForTrainer(sessions, workoutsBySession);
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
    // TIỆN ÍCH (Helpers)
    // =================================================================

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private Map<Long, String> courseNames(Collection<Long> courseIds) {
        return courseRepository.findAllById(new LinkedHashSet<>(courseIds)).stream()
                .collect(Collectors.toMap(Course::getId, Course::getName));
    }

}
