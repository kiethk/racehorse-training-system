package com.rtms.backend.training.service;

import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CompleteWorkoutRequest;
import com.rtms.backend.training.entity.TrainingSession;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.ExerciseRepository;
import com.rtms.backend.training.repository.TrainingSessionRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ghi kết quả, sửa kết quả và "Không chạy" phải đọc Workout bằng khóa dòng, để hai yêu cầu cùng lúc nối đuôi nhau chứ không ghi đè nhau.
 * (Recording a result, editing it and "Did not run" must read the Workout with a row lock, so two requests at the same moment run one
 * after the other instead of overwriting each other.)
 */
class TrainingWorkoutWriteLockTest {

    private static final long WORKOUT_ID = 7L;
    private static final long TRAINER_ID = 3L;

    private TrainingWorkoutRepository workouts;
    private TrainingWorkoutService service;
    private AuthenticatedUser trainer;

    @BeforeEach
    void setUp() {
        workouts = mock(TrainingWorkoutRepository.class);
        TrainingSessionRepository sessions = mock(TrainingSessionRepository.class);
        service = new TrainingWorkoutService(workouts, sessions, mock(ExerciseRepository.class),
                mock(WorkoutResultSimulationService.class), mock(SessionSettlementService.class), mock(SessionRules.class));

        TrainingSession session = new TrainingSession();
        session.setId(11L);
        session.setTrainerId(TRAINER_ID);
        when(sessions.findById(11L)).thenReturn(Optional.of(session));

        TrainingWorkout workout = new TrainingWorkout();
        workout.setId(WORKOUT_ID);
        workout.setSessionId(11L);
        workout.setStatus(WorkoutStatus.HELD);
        when(workouts.findByIdForUpdate(WORKOUT_ID)).thenReturn(Optional.of(workout));
        when(workouts.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));

        trainer = mock(AuthenticatedUser.class);
        when(trainer.getUserId()).thenReturn(TRAINER_ID);
    }

    @Test
    @DisplayName("recording a result locks the workout row")
    void recordResultLocksTheRow() {
        service.recordResult(WORKOUT_ID, validResult(), trainer);

        verify(workouts).findByIdForUpdate(WORKOUT_ID);
        verify(workouts, never()).findById(WORKOUT_ID);
    }

    @Test
    @DisplayName("did not run locks the workout row")
    void didNotRunLocksTheRow() {
        service.markDidNotRun(WORKOUT_ID, trainer);

        verify(workouts).findByIdForUpdate(WORKOUT_ID);
        verify(workouts, never()).findById(WORKOUT_ID);
    }

    @Test
    @DisplayName("editing a result locks the workout row")
    void updateResultLocksTheRow() {
        TrainingWorkout ran = new TrainingWorkout();
        ran.setId(WORKOUT_ID);
        ran.setSessionId(11L);
        ran.setStatus(WorkoutStatus.RAN);
        when(workouts.findByIdForUpdate(WORKOUT_ID)).thenReturn(Optional.of(ran));

        service.updateResult(WORKOUT_ID, validResult(), trainer);

        verify(workouts).findByIdForUpdate(WORKOUT_ID);
        verify(workouts, never()).findById(WORKOUT_ID);
    }

    private static CompleteWorkoutRequest validResult() {
        CompleteWorkoutRequest request = new CompleteWorkoutRequest();
        request.setActualDistanceMeters(new BigDecimal("1600"));
        request.setActualDurationMinutes(new BigDecimal("6"));
        request.setTopSpeedKmh(new BigDecimal("55"));
        request.setAverageSpeedKmh(new BigDecimal("45"));
        request.setAverageHeartRate(160);
        request.setMaxHeartRate(195);
        request.setRecoveryHeartRate(80);
        return request;
    }
}
