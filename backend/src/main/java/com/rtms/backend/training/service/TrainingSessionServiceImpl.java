package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.config.FarmSchedulePolicy;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.PlanSessionResponse;
import com.rtms.backend.training.dto.SessionWorkoutResponse;
import com.rtms.backend.training.entity.Subject;
import com.rtms.backend.training.entity.TrainingSession;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.SessionStatus;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.SubjectRepository;
import com.rtms.backend.training.repository.TrainingSessionRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Cài đặt TrainingSessionService. Sau mỗi thay đổi gọi settle() để kế hoạch tự đóng khi không còn buổi nào chờ chạy.
 * (Implements TrainingSessionService. After every change settle() runs so a plan closes by itself once no Session is left to run.)
 */
@Service
public class TrainingSessionServiceImpl implements TrainingSessionService {

    private final TrainingSessionRepository sessionRepository;
    private final TrainingWorkoutRepository workoutRepository;
    private final SubjectRepository subjectRepository;
    private final SessionSettlementService settlement;
    private final Clock clock;

    public TrainingSessionServiceImpl(TrainingSessionRepository sessionRepository,
                                      TrainingWorkoutRepository workoutRepository,
                                      SubjectRepository subjectRepository,
                                      SessionSettlementService settlement,
                                      Clock clock) {
        this.sessionRepository = sessionRepository;
        this.workoutRepository = workoutRepository;
        this.subjectRepository = subjectRepository;
        this.settlement = settlement;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PlanSessionResponse reschedule(Long sessionId, LocalTime newStartTime, AuthenticatedUser currentUser) {
        sessionRepository.lockSchedule(ScheduleLocks.of(currentUser.getUserId()));
        settlement.settle();
        TrainingSession session = findOwnSession(sessionId, currentUser);
        assertNotStarted(session);

        if (newStartTime == null) {
            throw new IllegalArgumentException("The new start time must not be empty!");
        }
        if (!FarmSchedulePolicy.isOnLotStartStep(newStartTime)) {
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
        return toResponse(session);
    }

    @Override
    @Transactional
    public PlanSessionResponse cancel(Long sessionId, AuthenticatedUser currentUser) {
        sessionRepository.lockSchedule(ScheduleLocks.of(currentUser.getUserId()));
        settlement.settle();
        TrainingSession session = findOwnSession(sessionId, currentUser);
        assertNotStarted(session);

        session.setStatus(SessionStatus.CANCELLED);
        sessionRepository.saveAndFlush(session);
        workoutRepository.cancelWorkoutsOfCancelledSessions();
        settlement.settle();
        return toResponse(sessionRepository.findById(sessionId).orElseThrow());
    }

    @Override
    @Transactional
    public SessionWorkoutResponse cancelWorkout(Long workoutId, AuthenticatedUser currentUser) {
        sessionRepository.lockSchedule(ScheduleLocks.of(currentUser.getUserId()));
        settlement.settle();
        TrainingWorkout workout = workoutRepository.findById(workoutId)
                .filter(found -> found.getSessionId() != null)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WORKOUT_NOT_FOUND", "Workout not found!"));
        TrainingSession session = findOwnSession(workout.getSessionId(), currentUser);
        assertNotStarted(session);
        if (workout.getStatus() != WorkoutStatus.SCHEDULED) {
            throw new ApiException(HttpStatus.CONFLICT, "WORKOUT_NOT_SCHEDULED", "Only a scheduled workout can be cancelled!");
        }

        workout.setStatus(WorkoutStatus.CANCELLED);
        workoutRepository.saveAndFlush(workout);

        // Buổi không còn Workout nào hiệu lực thì tự hủy. (A Session with no effective Workout left cancels itself.)
        if (workoutRepository.countBySessionIdAndStatusNot(session.getId(), WorkoutStatus.CANCELLED) == 0) {
            session.setStatus(SessionStatus.CANCELLED);
            sessionRepository.saveAndFlush(session);
            settlement.settle();
        }
        return SessionWorkoutResponse.of(workoutRepository.findById(workoutId).orElseThrow());
    }

    // =================================================================
    // TIỆN ÍCH (Helpers)
    // =================================================================

    /** Buổi của Trainer này; của người khác báo "không tìm thấy" để không lộ nó tồn tại. */
    private TrainingSession findOwnSession(Long sessionId, AuthenticatedUser currentUser) {
        return sessionRepository.findById(sessionId)
                .filter(found -> found.getTrainerId().equals(currentUser.getUserId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Session not found!"));
    }

    /** Luật chung: chỉ buổi còn SCHEDULED và CHƯA bắt đầu mới đổi được. (The shared rule: only a SCHEDULED Session that has NOT started can change.) */
    private void assertNotStarted(TrainingSession session) {
        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "SESSION_CANCELLED", "This session was already cancelled!");
        }
        if (session.getStatus() == SessionStatus.HELD || !LocalDateTime.now(clock).isBefore(session.startsAt())) {
            throw new ApiException(HttpStatus.CONFLICT, "SESSION_ALREADY_STARTED",
                    "This session has already started, so it can no longer be changed!");
        }
    }

    private PlanSessionResponse toResponse(TrainingSession session) {
        Subject subject = subjectRepository.findById(session.getSubjectId()).orElse(null);
        long horses = workoutRepository.countBySessionIdAndStatusNot(session.getId(), WorkoutStatus.CANCELLED);
        return new PlanSessionResponse(session.getId(), session.getSessionDate(), session.getStartTime(), session.getEndTime(),
                subject == null ? null : subject.getName(), session.getStatus().name(), (int) horses);
    }
}
