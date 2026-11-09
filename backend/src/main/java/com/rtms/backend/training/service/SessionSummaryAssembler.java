package com.rtms.backend.training.service;

import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.training.dto.SessionSummaryResponse;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.CourseRun;
import com.rtms.backend.training.entity.Exercise;
import com.rtms.backend.training.entity.TrainingGroup;
import com.rtms.backend.training.entity.TrainingSession;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.CourseRepository;
import com.rtms.backend.training.repository.CourseRunRepository;
import com.rtms.backend.training.repository.ExerciseRepository;
import com.rtms.backend.training.repository.TrainingGroupRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Dựng SessionSummaryResponse bằng một số truy vấn cố định, không phụ thuộc số buổi. Lịch, chi tiết buổi, đổi giờ, hủy buổi và chi
 * tiết Course Run đều dựng một buổi theo CÙNG một cách, nên một buổi luôn hiện cùng một hình dạng ở mọi màn hình.
 * (Builds SessionSummaryResponse with a fixed number of queries, independent of the number of Sessions. The schedule, the Session
 * detail, reschedule, cancel and the Course Run detail all build a Session the SAME way, so a Session has one shape on every screen.)
 */
@Component
class SessionSummaryAssembler {

    /** Những gì cần tra thêm cho một nhóm buổi. (What is looked up for a set of Sessions.) */
    record Lookups(Map<Long, CourseRun> courseRuns, Map<Long, TrainingGroup> groups, Map<Long, String> courseNames,
                   Map<Long, String> exerciseNames, Map<Long, String> trainerNames, Map<Long, String> horseNames) {
    }

    private final CourseRunRepository courseRunRepository;
    private final TrainingGroupRepository groupRepository;
    private final CourseRepository courseRepository;
    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final HorseRepository horseRepository;

    SessionSummaryAssembler(CourseRunRepository courseRunRepository,
                            TrainingGroupRepository groupRepository,
                            CourseRepository courseRepository,
                            ExerciseRepository exerciseRepository,
                            UserRepository userRepository,
                            HorseRepository horseRepository) {
        this.courseRunRepository = courseRunRepository;
        this.groupRepository = groupRepository;
        this.courseRepository = courseRepository;
        this.exerciseRepository = exerciseRepository;
        this.userRepository = userRepository;
        this.horseRepository = horseRepository;
    }

    Lookups lookups(Collection<TrainingSession> sessions, Collection<TrainingWorkout> workouts) {
        Map<Long, CourseRun> courseRuns = courseRunRepository
                .findAllById(sessions.stream().map(TrainingSession::getCourseRunId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(CourseRun::getId, Function.identity()));
        Map<Long, TrainingGroup> groups = groupRepository
                .findAllById(courseRuns.values().stream().map(CourseRun::getGroupId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(TrainingGroup::getId, Function.identity()));
        Map<Long, String> courseNames = courseRepository
                .findAllById(courseRuns.values().stream().map(CourseRun::getCourseId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Course::getId, Course::getName));
        Map<Long, String> exerciseNames = exerciseRepository
                .findAllById(sessions.stream().map(TrainingSession::getExerciseId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Exercise::getId, Exercise::getName));
        Map<Long, String> trainerNames = userRepository
                .findAllById(sessions.stream().map(TrainingSession::getTrainerId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(User::getId, User::getFullName));
        Map<Long, String> horseNames = horseRepository
                .findAllById(workouts.stream().map(TrainingWorkout::getHorseId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Horse::getId, Horse::getName));
        return new Lookups(courseRuns, groups, courseNames, exerciseNames, trainerNames, horseNames);
    }

    /** Một buổi, chỉ đếm các Workout người xem được phép thấy. (One Session, counting only the Workouts the viewer may see.) */
    SessionSummaryResponse summary(TrainingSession session, List<TrainingWorkout> workouts,
                                   Predicate<TrainingWorkout> visible, Lookups lookups, boolean showGroup) {
        CourseRun courseRun = lookups.courseRuns().get(session.getCourseRunId());
        List<TrainingWorkout> mine = workouts.stream().filter(visible).toList();
        int horseCount = (int) mine.stream().filter(workout -> workout.getStatus() != WorkoutStatus.CANCELLED).count();
        int needsResult = (int) mine.stream().filter(workout -> workout.getStatus() == WorkoutStatus.HELD).count();
        TrainingGroup group = !showGroup || courseRun.getGroupId() == null ? null : lookups.groups().get(courseRun.getGroupId());
        List<String> horseNames = mine.stream().filter(workout -> workout.getStatus() != WorkoutStatus.CANCELLED)
                .map(workout -> lookups.horseNames().get(workout.getHorseId()))
                .filter(java.util.Objects::nonNull).distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        return new SessionSummaryResponse(session.getId(), courseRun.getId(), showGroup ? courseRun.getGroupId() : null,
                group == null ? null : group.getName(), group != null && group.isDisbanded(),
                lookups.courseNames().get(courseRun.getCourseId()), lookups.exerciseNames().get(session.getExerciseId()),
                session.getSessionDate(), session.getStartTime(), session.getEndTime(), session.getStatus().name(),
                horseCount, needsResult, session.getTrainerId(), lookups.trainerNames().get(session.getTrainerId()), horseNames);
    }

    /** Một buổi của Trainer (thấy mọi Workout). (One Session for a Trainer, who sees every Workout.) */
    SessionSummaryResponse summaryForTrainer(TrainingSession session, List<TrainingWorkout> workouts) {
        return summary(session, workouts, workout -> true, lookups(List.of(session), workouts), true);
    }

    /** Các buổi của một Course Run, theo thứ tự đã có. (The Sessions of one Course Run, in the given order.) */
    List<SessionSummaryResponse> summariesForTrainer(List<TrainingSession> sessions,
                                                     Map<Long, List<TrainingWorkout>> workoutsBySession) {
        if (sessions.isEmpty()) {
            return List.of();
        }
        Lookups lookups = lookups(sessions, workoutsBySession.values().stream().flatMap(List::stream).toList());
        return sessions.stream()
                .map(session -> summary(session, workoutsBySession.getOrDefault(session.getId(), List.of()),
                        workout -> true, lookups, true))
                .toList();
    }
}
