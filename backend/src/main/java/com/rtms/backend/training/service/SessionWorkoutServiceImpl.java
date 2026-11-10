package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CompleteWorkoutRequest;
import com.rtms.backend.training.dto.SessionWorkoutResponse;
import com.rtms.backend.training.dto.WorkoutResultPreviewResponse;
import com.rtms.backend.training.entity.Exercise;
import com.rtms.backend.training.entity.TrainingSession;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.ExerciseRepository;
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
    private final ExerciseRepository exerciseRepository;
    private final WorkoutResultSimulationService simulationService;
    private final SessionSettlementService settlement;

    public SessionWorkoutServiceImpl(TrainingWorkoutRepository workoutRepository,
                                     TrainingSessionRepository sessionRepository,
                                     ExerciseRepository exerciseRepository,
                                     WorkoutResultSimulationService simulationService,
                                     SessionSettlementService settlement) {
        this.workoutRepository = workoutRepository;
        this.sessionRepository = sessionRepository;
        this.exerciseRepository = exerciseRepository;
        this.simulationService = simulationService;
        this.settlement = settlement;
    }

    @Override
    @Transactional
    public WorkoutResultPreviewResponse previewResult(Long workoutId, AuthenticatedUser currentUser) {
        settlement.settle();
        TrainingWorkout workout = findOwnWorkout(workoutId, currentUser);
        assertTookPlace(workout);
        TrainingSession session = findSession(workout);
        Exercise exercise = exerciseRepository.findById(session.getExerciseId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EXERCISE_NOT_FOUND", "Exercise of this workout not found!"));
        return simulationService.generate(exercise);
    }

    @Override
    @Transactional
    public SessionWorkoutResponse recordResult(Long workoutId, CompleteWorkoutRequest request, AuthenticatedUser currentUser) {
        settlement.settle();
        TrainingWorkout workout = findOwnWorkout(workoutId, currentUser);
        assertTookPlace(workout);
        if (workout.getStatus() == WorkoutStatus.RAN) {
            throw new ApiException(HttpStatus.CONFLICT, "WORKOUT_ALREADY_RECORDED",
                    "Results were already recorded for this workout. Edit them instead!");
        }
        WorkoutResults.validate(request);
        WorkoutResults.apply(workout, request);
        workout.setStatus(WorkoutStatus.RAN);
        return SessionWorkoutResponse.of(workoutRepository.saveAndFlush(workout));
    }

    @Override
    @Transactional
    public SessionWorkoutResponse updateResult(Long workoutId, CompleteWorkoutRequest request, AuthenticatedUser currentUser) {
        settlement.settle();
        TrainingWorkout workout = findOwnWorkout(workoutId, currentUser);
        assertTookPlace(workout);
        if (workout.getStatus() != WorkoutStatus.RAN) {
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
        assertTookPlace(workout);
        if (workout.getStatus() == WorkoutStatus.RAN) {
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

    /** Workout của buổi đã diễn ra và chưa bị hủy: HELD hoặc RAN. (A Workout of a Session that took place and was not cancelled: HELD or RAN.) */
    private void assertTookPlace(TrainingWorkout workout) {
        if (workout.getStatus() == WorkoutStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "WORKOUT_CANCELLED",
                    "This workout was cancelled, so results cannot be recorded!");
        }
        if (workout.getStatus() == WorkoutStatus.SCHEDULED) {
            throw new ApiException(HttpStatus.CONFLICT, "WORKOUT_NOT_ENDED",
                    "Results can be recorded only after the session has ended!");
        }
    }
}
