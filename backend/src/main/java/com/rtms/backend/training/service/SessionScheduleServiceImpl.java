package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.ScheduleSessionResponse;
import com.rtms.backend.training.dto.SessionDetailResponse;
import com.rtms.backend.training.dto.SessionHorseResponse;
import com.rtms.backend.training.dto.SessionWorkoutResponse;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.GroupPlan;
import com.rtms.backend.training.entity.Subject;
import com.rtms.backend.training.entity.TrainingGroup;
import com.rtms.backend.training.entity.TrainingSession;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.CourseRepository;
import com.rtms.backend.training.repository.GroupPlanRepository;
import com.rtms.backend.training.repository.SubjectRepository;
import com.rtms.backend.training.repository.TrainingGroupRepository;
import com.rtms.backend.training.repository.TrainingSessionRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Cài đặt SessionScheduleService bằng một số truy vấn cố định, không phụ thuộc số buổi. Mỗi lần đọc bắt đầu bằng settle() để trạng
 * thái luôn khớp đồng hồ.
 * (Implements SessionScheduleService with a fixed number of queries, independent of the number of Sessions. Every read starts with
 * settle() so statuses always match the clock.)
 */
@Service
public class SessionScheduleServiceImpl implements SessionScheduleService {

    /** Tối đa số ngày một lần đọc, để một khoảng gõ nhầm không kéo cả bảng. (Most days one read may span, so a mistyped range cannot pull the whole table.) */
    private static final int MAX_RANGE_DAYS = 93;

    private final TrainingSessionRepository sessionRepository;
    private final TrainingWorkoutRepository workoutRepository;
    private final GroupPlanRepository planRepository;
    private final TrainingGroupRepository groupRepository;
    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final HorseRepository horseRepository;
    private final UserRepository userRepository;
    private final SessionSettlementService settlement;

    public SessionScheduleServiceImpl(TrainingSessionRepository sessionRepository,
                                      TrainingWorkoutRepository workoutRepository,
                                      GroupPlanRepository planRepository,
                                      TrainingGroupRepository groupRepository,
                                      CourseRepository courseRepository,
                                      SubjectRepository subjectRepository,
                                      HorseRepository horseRepository,
                                      UserRepository userRepository,
                                      SessionSettlementService settlement) {
        this.sessionRepository = sessionRepository;
        this.workoutRepository = workoutRepository;
        this.planRepository = planRepository;
        this.groupRepository = groupRepository;
        this.courseRepository = courseRepository;
        this.subjectRepository = subjectRepository;
        this.horseRepository = horseRepository;
        this.userRepository = userRepository;
        this.settlement = settlement;
    }

    @Override
    @Transactional
    public List<ScheduleSessionResponse> getSessions(LocalDate from, LocalDate to, Long groupId, boolean needsResultOnly,
                                                     AuthenticatedUser currentUser) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException("A valid date range is required!");
        }
        if (from.plusDays(MAX_RANGE_DAYS).isBefore(to)) {
            throw new IllegalArgumentException("The date range cannot be longer than " + MAX_RANGE_DAYS + " days!");
        }
        settlement.settle();

        List<TrainingSession> sessions = visibleSessions(from, to, currentUser);
        if (sessions.isEmpty()) {
            return List.of();
        }
        Map<Long, List<TrainingWorkout>> workoutsBySession = workoutRepository
                .findBySessionIdIn(sessions.stream().map(TrainingSession::getId).toList()).stream()
                .collect(Collectors.groupingBy(TrainingWorkout::getSessionId));
        Lookups lookups = lookups(sessions);
        Predicate<TrainingWorkout> visible = visibleWorkouts(currentUser, workoutsBySession.values());

        return sessions.stream()
                .filter(session -> groupId == null || groupId.equals(lookups.plans().get(session.getGroupPlanId()).getGroupId()))
                .map(session -> toSummary(session, workoutsBySession.getOrDefault(session.getId(), List.of()), visible, lookups))
                .filter(summary -> !needsResultOnly || summary.needsResultCount() > 0)
                .toList();
    }

    @Override
    @Transactional
    public SessionDetailResponse getSession(Long sessionId, AuthenticatedUser currentUser) {
        settlement.settle();
        TrainingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Session not found!"));
        List<TrainingWorkout> workouts = workoutRepository.findBySessionId(sessionId);
        Predicate<TrainingWorkout> visible = visibleWorkouts(currentUser, List.of(workouts));
        boolean allowed = switch (currentUser.getRole()) {
            case "HEAD_TRAINER" -> session.getTrainerId().equals(currentUser.getUserId());
            case "CLUB_MANAGER" -> true;
            default -> workouts.stream().anyMatch(visible);
        };
        if (!allowed) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Session not found!");
        }

        Lookups lookups = lookups(List.of(session));
        List<TrainingWorkout> shown = workouts.stream().filter(visible).toList();
        Map<Long, String> horseNames = horseRepository
                .findAllById(shown.stream().map(TrainingWorkout::getHorseId).toList()).stream()
                .collect(Collectors.toMap(Horse::getId, Horse::getName));
        Map<Long, String> groomNames = userNames(shown.stream().map(TrainingWorkout::getAssignedToId).toList());

        List<SessionHorseResponse> horses = shown.stream()
                .sorted((a, b) -> horseNames.getOrDefault(a.getHorseId(), "").compareToIgnoreCase(horseNames.getOrDefault(b.getHorseId(), "")))
                .map(workout -> new SessionHorseResponse(workout.getId(), workout.getHorseId(),
                        horseNames.get(workout.getHorseId()),
                        workout.getAssignedToId() == null ? null : groomNames.get(workout.getAssignedToId()),
                        workout.getStatus().name(), workout.getActualDistanceMeters() != null, workout.getPerformanceRating(),
                        workout.getActualDistanceMeters() != null ? SessionWorkoutResponse.of(workout) : null))
                .toList();
        return new SessionDetailResponse(toSummary(session, workouts, visible, lookups), horses);
    }

    // =================================================================
    // QUYỀN XEM (Who sees what)
    // =================================================================

    private List<TrainingSession> visibleSessions(LocalDate from, LocalDate to, AuthenticatedUser user) {
        return switch (user.getRole()) {
            case "HEAD_TRAINER" -> sessionRepository
                    .findByTrainerIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(user.getUserId(), from, to);
            case "GROOM" -> sessionRepository.findOfGroom(user.getUserId(), from, to);
            case "HORSE_OWNER" -> sessionRepository.findOfOwner(user.getUserId(), from, to);
            case "CLUB_MANAGER" -> sessionRepository.findBySessionDateBetweenOrderBySessionDateAscStartTimeAsc(from, to);
            default -> List.of();
        };
    }

    /** Workout người xem được phép thấy: Groom chỉ ngựa mình dắt, Owner chỉ ngựa của mình, Trainer và Quản lý tất cả. */
    private Predicate<TrainingWorkout> visibleWorkouts(AuthenticatedUser user, Collection<List<TrainingWorkout>> all) {
        return switch (user.getRole()) {
            case "GROOM" -> workout -> user.getUserId().equals(workout.getAssignedToId());
            case "HORSE_OWNER" -> {
                Set<Long> mine = horseRepository
                        .findAllById(all.stream().flatMap(List::stream).map(TrainingWorkout::getHorseId).collect(Collectors.toSet()))
                        .stream().filter(horse -> user.getUserId().equals(horse.getOwnerId()))
                        .map(Horse::getId).collect(Collectors.toSet());
                yield workout -> mine.contains(workout.getHorseId());
            }
            default -> workout -> true;
        };
    }

    // =================================================================
    // DỰNG PHẢN HỒI (Building responses)
    // =================================================================

    private record Lookups(Map<Long, GroupPlan> plans, Map<Long, TrainingGroup> groups, Map<Long, String> courseNames,
                           Map<Long, String> subjectNames) {
    }

    private Lookups lookups(Collection<TrainingSession> sessions) {
        Map<Long, GroupPlan> plans = planRepository
                .findAllById(sessions.stream().map(TrainingSession::getGroupPlanId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(GroupPlan::getId, Function.identity()));
        Map<Long, TrainingGroup> groups = groupRepository
                .findAllById(plans.values().stream().map(GroupPlan::getGroupId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(TrainingGroup::getId, Function.identity()));
        Map<Long, String> courseNames = courseRepository
                .findAllById(plans.values().stream().map(GroupPlan::getCourseId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Course::getId, Course::getName));
        Map<Long, String> subjectNames = subjectRepository
                .findAllById(sessions.stream().map(TrainingSession::getSubjectId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Subject::getId, Subject::getName));
        return new Lookups(plans, groups, courseNames, subjectNames);
    }

    private ScheduleSessionResponse toSummary(TrainingSession session, List<TrainingWorkout> workouts,
                                              Predicate<TrainingWorkout> visible, Lookups lookups) {
        GroupPlan plan = lookups.plans().get(session.getGroupPlanId());
        List<TrainingWorkout> mine = workouts.stream().filter(visible).toList();
        int horseCount = (int) mine.stream().filter(workout -> workout.getStatus() != WorkoutStatus.CANCELLED).count();
        int needsResult = (int) mine.stream()
                .filter(workout -> workout.getStatus() == WorkoutStatus.HELD && workout.getActualDistanceMeters() == null).count();
        return new ScheduleSessionResponse(session.getId(), plan.getId(), plan.getGroupId(),
                plan.getGroupId() == null ? null : lookups.groups().get(plan.getGroupId()).getName(),
                plan.getGroupId() != null && lookups.groups().get(plan.getGroupId()).isDisbanded(),
                lookups.courseNames().get(plan.getCourseId()), lookups.subjectNames().get(session.getSubjectId()),
                session.getSessionDate(), session.getStartTime(), session.getEndTime(), session.getStatus().name(),
                horseCount, needsResult);
    }

    private Map<Long, String> userNames(Collection<Long> ids) {
        Set<Long> present = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        return userRepository.findAllById(present).stream().collect(Collectors.toMap(User::getId, User::getFullName));
    }
}
