package com.rtms.backend.training.service;

import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.HorseAttendanceResponse;
import com.rtms.backend.training.repository.projection.CourseRunSessionCountRow;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.CourseRun;
import com.rtms.backend.training.entity.TrainingGroup;
import com.rtms.backend.training.entity.TrainingSession;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.SessionStatus;
import com.rtms.backend.training.enums.CourseRunStatus;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.CourseRepository;
import com.rtms.backend.training.repository.CourseRunRepository;
import com.rtms.backend.training.repository.TrainingGroupRepository;
import com.rtms.backend.training.repository.TrainingSessionRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cài đặt HorseAttendanceService. Đã học (X) là số Workout RAN của ngựa (đã ghi kết quả). Tổng (Y) là các buổi chưa bị
 * hủy của chính kế hoạch (kể cả buổi trước khi ngựa vào và buổi mà Workout riêng của ngựa bị hủy), không lấy từ Course nên sửa Course
 * về sau không làm đổi lịch sử cũ.
 * (Implements HorseAttendanceService. Attended (X) is the horse's Workouts that Ran (results recorded). Total (Y) is
 * the course run's own Sessions that were not cancelled (including Sessions before the horse joined and Sessions where only its own
 * Workout was cancelled), never taken from the Course, so editing a Course later does not rewrite old history.)
 */
@Service
public class HorseAttendanceServiceImpl implements HorseAttendanceService {

    private final TrainingWorkoutRepository workoutRepository;
    private final TrainingSessionRepository sessionRepository;
    private final CourseRunRepository courseRunRepository;
    private final TrainingGroupRepository groupRepository;
    private final CourseRepository courseRepository;
    private final TrainingAccessPolicy accessPolicy;
    private final SessionSettlementService settlement;

    public HorseAttendanceServiceImpl(TrainingWorkoutRepository workoutRepository,
                                      TrainingSessionRepository sessionRepository,
                                      CourseRunRepository courseRunRepository,
                                      TrainingGroupRepository groupRepository,
                                      CourseRepository courseRepository,
                                      TrainingAccessPolicy accessPolicy,
                                      SessionSettlementService settlement) {
        this.workoutRepository = workoutRepository;
        this.sessionRepository = sessionRepository;
        this.courseRunRepository = courseRunRepository;
        this.groupRepository = groupRepository;
        this.courseRepository = courseRepository;
        this.accessPolicy = accessPolicy;
        this.settlement = settlement;
    }

    @Override
    @Transactional
    public List<HorseAttendanceResponse> getHistory(Long horseId, AuthenticatedUser currentUser) {
        accessPolicy.assertCanViewHorse(horseId, currentUser);
        settlement.settle();

        List<TrainingWorkout> workouts = workoutRepository.findByHorseIdAndSessionIdIsNotNull(horseId);
        if (workouts.isEmpty()) {
            return List.of();
        }
        Map<Long, TrainingSession> sessionsById = sessionRepository
                .findAllById(workouts.stream().map(TrainingWorkout::getSessionId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(TrainingSession::getId, Function.identity()));
        Map<Long, List<TrainingWorkout>> workoutsByCourseRun = workouts.stream()
                .collect(Collectors.groupingBy(workout -> sessionsById.get(workout.getSessionId()).getCourseRunId()));

        Map<Long, CourseRun> courseRunsById = courseRunRepository.findAllById(workoutsByCourseRun.keySet()).stream()
                .collect(Collectors.toMap(CourseRun::getId, Function.identity()));
        Map<Long, TrainingGroup> groupsById = groupRepository
                .findAllById(courseRunsById.values().stream().map(CourseRun::getGroupId).filter(Objects::nonNull).toList()).stream()
                .collect(Collectors.toMap(TrainingGroup::getId, Function.identity()));
        Map<Long, String> courseNames = courseRepository
                .findAllById(courseRunsById.values().stream().map(CourseRun::getCourseId).toList()).stream()
                .collect(Collectors.toMap(Course::getId, Course::getName));
        Map<Long, Map<SessionStatus, Long>> counts = sessionRepository.countByCourseRunAndStatus(workoutsByCourseRun.keySet()).stream()
                .collect(Collectors.groupingBy(CourseRunSessionCountRow::courseRunId,
                        Collectors.toMap(CourseRunSessionCountRow::status, CourseRunSessionCountRow::count)));

        List<HorseAttendanceResponse> history = new ArrayList<>();
        for (Map.Entry<Long, List<TrainingWorkout>> entry : workoutsByCourseRun.entrySet()) {
            CourseRun courseRun = courseRunsById.get(entry.getKey());
            List<TrainingWorkout> courseRunWorkouts = entry.getValue();
            int attended = (int) courseRunWorkouts.stream().filter(workout -> workout.getStatus() == WorkoutStatus.RAN).count();
            long tookPlace = attended + courseRunWorkouts.stream().filter(workout -> workout.getStatus() == WorkoutStatus.HELD).count();
            long scheduled = courseRunWorkouts.stream().filter(workout -> workout.getStatus() == WorkoutStatus.SCHEDULED).count();

            String effectiveStatus;
            if (courseRun.getStatus() == CourseRunStatus.CANCELLED) {
                effectiveStatus = CourseRunStatus.CANCELLED.name();
            } else if (courseRun.getStatus() == CourseRunStatus.COMPLETED) {
                effectiveStatus = CourseRunStatus.COMPLETED.name();
            } else if (scheduled > 0) {
                effectiveStatus = courseRun.getStatus().name();
            } else if (tookPlace > 0) {
                effectiveStatus = CourseRunStatus.COMPLETED.name();
            } else {
                effectiveStatus = CourseRunStatus.CANCELLED.name();
            }

            Map<SessionStatus, Long> byStatus = counts.getOrDefault(courseRun.getId(), Map.of());
            int total = byStatus.getOrDefault(SessionStatus.HELD, 0L).intValue()
                    + byStatus.getOrDefault(SessionStatus.SCHEDULED, 0L).intValue();
            LocalDate joinedOn = courseRunWorkouts.stream()
                    .map(workout -> sessionsById.get(workout.getSessionId()).getSessionDate())
                    .min(Comparator.naturalOrder()).orElse(courseRun.getStartDate());
            history.add(new HorseAttendanceResponse(courseRun.getId(),
                    courseRun.getGroupId() == null ? null : groupsById.get(courseRun.getGroupId()).getName(),
                    courseRun.getGroupId() != null && groupsById.get(courseRun.getGroupId()).isDisbanded(),
                    courseNames.get(courseRun.getCourseId()), effectiveStatus, courseRun.getStartDate(), courseRun.getEndDate(),
                    attended, total, joinedOn));
        }
        history.sort(Comparator.comparing(HorseAttendanceResponse::startDate).reversed());
        return history;
    }
}
