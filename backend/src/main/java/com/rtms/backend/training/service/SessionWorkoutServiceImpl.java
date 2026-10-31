package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CompleteWorkoutRequest;
import com.rtms.backend.training.dto.SessionWorkoutResponse;
import com.rtms.backend.training.dto.WorkoutResultPreviewResponse;
import com.rtms.backend.training.entity.Subject;
import com.rtms.backend.training.entity.TrainingSession;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.SubjectRepository;
import com.rtms.backend.training.repository.TrainingSessionRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cài đặt SessionWorkoutService. Mọi thao tác bắt đầu bằng settle() để Workout của buổi đã hết giờ đã là HELD.
 * (Implements SessionWorkoutService. Every operation starts with settle() so the Workouts of an ended Session are already HELD.)
 */
@Service
public class SessionWorkoutServiceImpl implements SessionWorkoutService {

    private final TrainingWorkoutRepository workoutRepository;
    private final TrainingSessionRepository sessionRepository;
    private final SubjectRepository subjectRepository;
    private final WorkoutResultSimulationService simulationService;
    private final SessionSettlementService settlement;

    public SessionWorkoutServiceImpl(TrainingWorkoutRepository workoutRepository,
                                     TrainingSessionRepository sessionRepository,
                                     SubjectRepository subjectRepository,
                                     WorkoutResultSimulationService simulationService,
                                     SessionSettlementService settlement) {
        this.workoutRepository = workoutRepository;
        this.sessionRepository = sessionRepository;
        this.subjectRepository = subjectRepository;
        this.simulationService = simulationService;
        this.settlement = settlement;
    }

    @Override
    @Transactional
    public WorkoutResultPreviewResponse previewResult(Long workoutId, AuthenticatedUser currentUser) {
        settlement.settle();
        TrainingWorkout workout = findOwnWorkout(workoutId, currentUser);
        assertHeld(workout);
        TrainingSession session = findSession(workout);
        Subject subject = subjectRepository.findById(session.getSubjectId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SUBJECT_NOT_FOUND", "Subject of this workout not found!"));
        return simulationService.generate(subject);
    }

    @Override
    @Transactional
    public SessionWorkoutResponse recordResult(Long workoutId, CompleteWorkoutRequest request, AuthenticatedUser currentUser) {
        settlement.settle();
        TrainingWorkout workout = findOwnWorkout(workoutId, currentUser);
        assertHeld(workout);
        if (hasResult(workout)) {
            throw new ApiException(HttpStatus.CONFLICT, "WORKOUT_ALREADY_RECORDED",
                    "Results were already recorded for this workout. Edit them instead!");
        }
        WorkoutResults.validate(request);
        WorkoutResults.apply(workout, request);
        return SessionWorkoutResponse.of(workoutRepository.saveAndFlush(workout));
    }

    @Override
    @Transactional
    public SessionWorkoutResponse updateResult(Long workoutId, CompleteWorkoutRequest request, AuthenticatedUser currentUser) {
        settlement.settle();
        TrainingWorkout workout = findOwnWorkout(workoutId, currentUser);
        assertHeld(workout);
        if (!hasResult(workout)) {
            throw new ApiException(HttpStatus.CONFLICT, "WORKOUT_NOT_RECORDED",
                    "No results were recorded for this workout yet!");
        }
        WorkoutResults.validate(request);
        WorkoutResults.apply(workout, request);
        return SessionWorkoutResponse.of(workoutRepository.saveAndFlush(workout));
    }

    @Override
    @Transactional
    public SessionWorkoutResponse markDidNotRun(Long workoutId, AuthenticatedUser currentUser) {
        settlement.settle();
        TrainingWorkout workout = findOwnWorkout(workoutId, currentUser);
        assertHeld(workout);
        if (hasResult(workout)) {
            throw new ApiException(HttpStatus.CONFLICT, "WORKOUT_ALREADY_RECORDED",
                    "Results were already recorded, so this horse cannot be marked as not running!");
        }
        workout.setStatus(WorkoutStatus.CANCELLED);
        return SessionWorkoutResponse.of(workoutRepository.saveAndFlush(workout));
    }

    // =================================================================
    // TIỆN ÍCH (Helpers)
    // =================================================================

    /** Workout của buổi thuộc Trainer này; của người khác báo "không tìm thấy" để không lộ nó tồn tại. */
    private TrainingWorkout findOwnWorkout(Long workoutId, AuthenticatedUser currentUser) {
        TrainingWorkout workout = workoutRepository.findById(workoutId)
                .filter(found -> found.getSessionId() != null)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WORKOUT_NOT_FOUND", "Workout not found!"));
        TrainingSession session = findSession(workout);
        if (!session.getTrainerId().equals(currentUser.getUserId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "WORKOUT_NOT_FOUND", "Workout not found!");
        }
        return workout;
    }

    private TrainingSession findSession(TrainingWorkout workout) {
        return sessionRepository.findById(workout.getSessionId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WORKOUT_NOT_FOUND", "Workout not found!"));
    }

    private void assertHeld(TrainingWorkout workout) {
        if (workout.getStatus() == WorkoutStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "WORKOUT_CANCELLED",
                    "This workout was cancelled, so results cannot be recorded!");
        }
        if (workout.getStatus() != WorkoutStatus.HELD) {
            throw new ApiException(HttpStatus.CONFLICT, "WORKOUT_NOT_ENDED",
                    "Results can be recorded only after the session has ended!");
        }
    }

    private boolean hasResult(TrainingWorkout workout) {
        return workout.getActualDistanceMeters() != null;
    }
}
