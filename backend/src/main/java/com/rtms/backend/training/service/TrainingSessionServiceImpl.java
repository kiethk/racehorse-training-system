package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.config.FarmSchedulePolicy;
import com.rtms.backend.config.TrainingLimits;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.security.Roles;
import com.rtms.backend.training.dto.SessionDetailResponse;
import com.rtms.backend.training.dto.SessionHorseResponse;
import com.rtms.backend.training.dto.SessionSummaryResponse;
import com.rtms.backend.training.dto.SessionWorkoutResponse;
import com.rtms.backend.training.entity.TrainingSession;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.SessionStatus;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.TrainingSessionRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import com.rtms.backend.training.service.SessionSummaryAssembler.Lookups;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Cài đặt TrainingSessionService bằng một số truy vấn cố định, không phụ thuộc số buổi. Mỗi thao tác bắt đầu bằng settle() để trạng
 * thái luôn khớp đồng hồ; dời giờ và hủy buổi còn khóa lịch của Trainer để hai request đồng thời không cùng chiếm một khe.
 * (Implements TrainingSessionService with a fixed number of queries, independent of the number of Sessions. Every operation starts
 * with settle() so statuses always match the clock; move and cancel also take the Trainer's schedule lock so two simultaneous
 * requests never take the same slot.)
 */
@Service
public class TrainingSessionServiceImpl implements TrainingSessionService {

    private final TrainingSessionRepository sessionRepository;
    private final TrainingWorkoutRepository workoutRepository;
    private final HorseRepository horseRepository;
    private final UserRepository userRepository;
    private final SessionSettlementService settlement;
    private final SessionSummaryAssembler assembler;
    private final SessionRules rules;
    private final Clock clock;

    public TrainingSessionServiceImpl(TrainingSessionRepository sessionRepository,
                                      TrainingWorkoutRepository workoutRepository,
                                      HorseRepository horseRepository,
                                      UserRepository userRepository,
                                      SessionSettlementService settlement,
                                      SessionSummaryAssembler assembler,
                                      SessionRules rules,
                                      Clock clock) {
        this.sessionRepository = sessionRepository;
        this.workoutRepository = workoutRepository;
        this.horseRepository = horseRepository;
        this.userRepository = userRepository;
        this.settlement = settlement;
        this.assembler = assembler;
        this.rules = rules;
        this.clock = clock;
    }

    // =================================================================
    // ĐỌC (Reading)
    // =================================================================

    @Override
    @Transactional
    public List<SessionSummaryResponse> getSessions(LocalDate from, LocalDate to, Long groupId, boolean needsResultOnly,
                                                    AuthenticatedUser currentUser) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException("A valid date range is required!");
        }
        if (from.plusDays(TrainingLimits.MAX_SCHEDULE_RANGE_DAYS).isBefore(to)) {
            throw new IllegalArgumentException("The date range cannot be longer than " + TrainingLimits.MAX_SCHEDULE_RANGE_DAYS + " days!");
        }
        settlement.settle();

        List<TrainingSession> sessions = visibleSessions(from, to, currentUser);
        if (sessions.isEmpty()) {
            return List.of();
        }
        Map<Long, List<TrainingWorkout>> workoutsBySession = workoutRepository
                .findBySessionIdIn(sessions.stream().map(TrainingSession::getId).toList()).stream()
                .collect(Collectors.groupingBy(TrainingWorkout::getSessionId));
        Lookups lookups = assembler.lookups(sessions, workoutsBySession.values().stream().flatMap(List::stream).toList());
        Predicate<TrainingWorkout> visible = visibleWorkouts(currentUser, workoutsBySession.values());
        boolean showGroup = knowsGroups(currentUser);

        return sessions.stream()
                .filter(session -> groupId == null || groupId.equals(lookups.courseRuns().get(session.getCourseRunId()).getGroupId()))
                .map(session -> assembler.summary(session, workoutsBySession.getOrDefault(session.getId(), List.of()), visible, lookups, showGroup))
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
            case Roles.HEAD_TRAINER -> session.getTrainerId().equals(currentUser.getUserId());
            case Roles.CLUB_MANAGER -> true;
            default -> workouts.stream().anyMatch(visible);
        };
        if (!allowed) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Session not found!");
        }

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
                        workout.getStatus().name(), workout.getPerformanceRating(),
                        workout.getStatus() == WorkoutStatus.RAN ? SessionWorkoutResponse.of(workout) : null))
                .toList();
        return new SessionDetailResponse(
                assembler.summary(session, workouts, visible, assembler.lookups(List.of(session), workouts), knowsGroups(currentUser)), horses);
    }

    /** Chỉ Trainer và Quản lý biết đến Training Group; Owner và Groom chỉ biết ngựa. (Only a Trainer and a Manager know Training Groups; an Owner and a Groom know horses.) */
    private static boolean knowsGroups(AuthenticatedUser user) {
        return Roles.HEAD_TRAINER.equals(user.getRole()) || Roles.CLUB_MANAGER.equals(user.getRole());
    }

    /** Các buổi người xem được phép thấy: Trainer của mình, Groom có ngựa mình dắt, Owner có ngựa của mình, Quản lý tất cả. */
    private List<TrainingSession> visibleSessions(LocalDate from, LocalDate to, AuthenticatedUser user) {
        return switch (user.getRole()) {
            case Roles.HEAD_TRAINER -> sessionRepository
                    .findByTrainerIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(user.getUserId(), from, to);
            case Roles.GROOM -> sessionRepository.findOfGroom(user.getUserId(), from, to);
            case Roles.HORSE_OWNER -> sessionRepository.findOfOwner(user.getUserId(), from, to);
            case Roles.CLUB_MANAGER -> sessionRepository.findBySessionDateBetweenOrderBySessionDateAscStartTimeAsc(from, to);
            default -> List.of();
        };
    }

    /** Workout người xem được phép thấy: Groom chỉ ngựa mình dắt, Owner chỉ ngựa của mình, Trainer và Quản lý tất cả. */
    private Predicate<TrainingWorkout> visibleWorkouts(AuthenticatedUser user, Collection<List<TrainingWorkout>> all) {
        return switch (user.getRole()) {
            case Roles.GROOM -> workout -> user.getUserId().equals(workout.getAssignedToId());
            case Roles.HORSE_OWNER -> {
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
    // GHI (Writing)
    // =================================================================

    @Override
    @Transactional
    public SessionSummaryResponse reschedule(Long sessionId, LocalTime newStartTime, AuthenticatedUser currentUser) {
        sessionRepository.lockSchedule(ScheduleLocks.of(currentUser.getUserId()));
        settlement.settle();
        TrainingSession session = rules.findOwnSession(sessionId, currentUser);
        rules.assertNotStarted(session);

        if (newStartTime == null) {
            throw new IllegalArgumentException("The new start time must not be empty!");
        }
        if (!FarmSchedulePolicy.isOnSessionStartStep(newStartTime)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_START_TIME",
                    "A session starts at minute 00, 15, 30 or 45!");
        }
        LocalTime newEnd = newStartTime.plus(Duration.between(session.getStartTime(), session.getEndTime()));
        if (!FarmSchedulePolicy.isWithinGoldenHours(newStartTime, newEnd) || newEnd.isBefore(newStartTime)) {
            throw new ApiException(HttpStatus.CONFLICT, "OUTSIDE_TRAINING_WINDOW", String.format(
                    "A session must fit inside the training window (%s–%s)!",
                    FarmSchedulePolicy.GOLDEN_HOURS_START, FarmSchedulePolicy.GOLDEN_HOURS_END));
        }
        if (LocalDateTime.of(session.getSessionDate(), newStartTime).isBefore(LocalDateTime.now(clock))) {
            throw new ApiException(HttpStatus.CONFLICT, "START_IN_THE_PAST", "That time has already passed!");
        }
        boolean overlaps = sessionRepository
                .findByTrainerIdAndSessionDateAndStatusNotOrderByStartTimeAsc(
                        currentUser.getUserId(), session.getSessionDate(), SessionStatus.CANCELLED).stream()
                .filter(other -> !other.getId().equals(session.getId()))
                .anyMatch(other -> newStartTime.isBefore(other.getEndTime()) && newEnd.isAfter(other.getStartTime()));
        if (overlaps) {
            throw new ApiException(HttpStatus.CONFLICT, "SESSION_OVERLAP",
                    "That time overlaps another session of yours!");
        }

        session.setStartTime(newStartTime);
        session.setEndTime(newEnd);
        try {
            sessionRepository.saveAndFlush(session);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "SESSION_OVERLAP", "That time overlaps another session of yours!");
        }
        return trainerSummary(session);
    }

    @Override
    @Transactional
    public SessionSummaryResponse cancel(Long sessionId, AuthenticatedUser currentUser) {
        sessionRepository.lockSchedule(ScheduleLocks.of(currentUser.getUserId()));
        settlement.settle();
        TrainingSession session = rules.findOwnSession(sessionId, currentUser);
        rules.assertNotStarted(session);

        session.setStatus(SessionStatus.CANCELLED);
        sessionRepository.saveAndFlush(session);
        workoutRepository.cancelWorkoutsOfCancelledSessions();
        settlement.settle();
        return trainerSummary(sessionRepository.findById(sessionId).orElseThrow());
    }

    private SessionSummaryResponse trainerSummary(TrainingSession session) {
        return assembler.summaryForTrainer(session, workoutRepository.findBySessionId(session.getId()));
    }

    private Map<Long, String> userNames(Collection<Long> ids) {
        Set<Long> present = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        return userRepository.findAllById(present).stream().collect(Collectors.toMap(User::getId, User::getFullName));
    }
}
