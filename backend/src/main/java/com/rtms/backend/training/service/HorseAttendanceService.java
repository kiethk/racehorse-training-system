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
 * Lịch sử Course Run của một chiến mã, suy ra từ chính các Workout của nó (Workout, rồi Session, rồi Course Run, gom theo kế hoạch),
 * nên ngựa đổi nhóm bao nhiêu lần cũng có đủ lịch sử, mỗi kế hoạch một dòng.
 * (A horse's Course Run history, derived from its own Workouts (Workout, then Session, then Course Run, grouped by course run), so a horse
 * that changed groups keeps its whole history, one line per course run.)
 *
 * Đã học (X) là số Workout RAN của ngựa (đã ghi kết quả). Tổng (Y) là MỌI buổi của chính kế hoạch lúc tạo,
 * kể cả buổi bị hủy, buổi trước khi ngựa vào và buổi mà Workout riêng của ngựa bị hủy, nên con số cho thấy khóa cần bao nhiêu buổi.
 * Không lấy từ Course nên sửa Course về sau không làm đổi lịch sử cũ.
 * (Attended (X) is the horse's Workouts that Ran (results recorded). Total (Y) is EVERY Session the
 * course run was created with, cancelled ones included, Sessions before the horse joined and Sessions where only its own Workout was
 * cancelled, so the number shows how many Sessions the course needs. Never taken from the Course, so editing a Course later does not
 * rewrite old history.)
 */
@Service
public class HorseAttendanceService {

    private final TrainingWorkoutRepository workoutRepository;
    private final TrainingSessionRepository sessionRepository;
    private final CourseRunRepository courseRunRepository;
    private final TrainingGroupRepository groupRepository;
    private final CourseRepository courseRepository;
    private final TrainingAccessPolicy accessPolicy;
    private final SessionSettlementService settlement;

    public HorseAttendanceService(TrainingWorkoutRepository workoutRepository,
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

    /** Mới nhất trước. (Newest first.) */
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
            // Ngựa không còn Workout nào (đã bị hủy hết) thì không hiện khóa này. (A course run where all the horse's Workouts were cancelled is not listed.)
            if (scheduled + tookPlace == 0) {
                continue;
            }

            String effectiveStatus;
            if (courseRun.getStatus() == CourseRunStatus.CANCELLED) {
                effectiveStatus = CourseRunStatus.CANCELLED.name();
            } else if (courseRun.getStatus() == CourseRunStatus.COMPLETED) {
                effectiveStatus = CourseRunStatus.COMPLETED.name();
            } else if (scheduled > 0) {
                effectiveStatus = courseRun.getStatus().name();
            } else {
                effectiveStatus = CourseRunStatus.COMPLETED.name();
            }

            Map<SessionStatus, Long> byStatus = counts.getOrDefault(courseRun.getId(), Map.of());
            // Cancelled Sessions stay in the total: the total is what the course run was created with.
            int total = byStatus.values().stream().mapToInt(Long::intValue).sum();
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
