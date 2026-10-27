package com.rtms.backend.training.service;

import com.rtms.backend.training.entity.HorseTrainingPlan;
import com.rtms.backend.training.enums.TrainingPlanStatus;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.HorseTrainingPlanRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanCompletionServiceTest {

    @Mock
    private TrainingWorkoutRepository workoutRepository;

    @Mock
    private HorseTrainingPlanRepository planRepository;

    private PlanCompletionService service;

    @BeforeEach
    void setUp() {
        service = new PlanCompletionService(workoutRepository, planRepository);
    }

    private HorseTrainingPlan plan(long id, TrainingPlanStatus status) {
        HorseTrainingPlan plan = new HorseTrainingPlan();
        plan.setId(id);
        plan.setStatus(status);
        return plan;
    }

    @Test
    @DisplayName("Every session cancelled before any was held: the plan is CANCELLED")
    void allCancelledBecomesCancelled() {
        HorseTrainingPlan plan = plan(1L, TrainingPlanStatus.UPCOMING);
        when(workoutRepository.countByPlanIdAndStatusNotIn(eq(1L), anyList())).thenReturn(0L);
        when(workoutRepository.countByPlanIdAndStatus(1L, WorkoutStatus.COMPLETED)).thenReturn(0L);

        service.closeIfFinished(plan);

        assertEquals(TrainingPlanStatus.CANCELLED, plan.getStatus());
    }

    @Test
    @DisplayName("The rest cancelled after some sessions were held: the plan is COMPLETED")
    void restCancelledAfterSomeHeldBecomesCompleted() {
        HorseTrainingPlan plan = plan(2L, TrainingPlanStatus.ACTIVE);
        when(workoutRepository.countByPlanIdAndStatusNotIn(eq(2L), anyList())).thenReturn(0L);
        when(workoutRepository.countByPlanIdAndStatus(2L, WorkoutStatus.COMPLETED)).thenReturn(1L);

        service.closeIfFinished(plan);

        assertEquals(TrainingPlanStatus.COMPLETED, plan.getStatus());
    }

    @Test
    @DisplayName("A session is still scheduled: the plan is left as it is")
    void scheduledSessionLeftKeepsStatus() {
        HorseTrainingPlan plan = plan(3L, TrainingPlanStatus.UPCOMING);
        when(workoutRepository.countByPlanIdAndStatusNotIn(eq(3L), anyList())).thenReturn(2L);

        service.closeIfFinished(plan);

        assertEquals(TrainingPlanStatus.UPCOMING, plan.getStatus());
        verify(workoutRepository, never()).countByPlanIdAndStatus(anyLong(), eq(WorkoutStatus.COMPLETED));
    }

    @Test
    @DisplayName("A plan already closed is never reopened or changed")
    void closedPlanIsUntouched() {
        HorseTrainingPlan blocked = plan(4L, TrainingPlanStatus.CANCELLED);
        HorseTrainingPlan done = plan(5L, TrainingPlanStatus.COMPLETED);

        service.closeIfFinished(blocked);
        service.closeIfFinished(done);

        assertEquals(TrainingPlanStatus.CANCELLED, blocked.getStatus());
        assertEquals(TrainingPlanStatus.COMPLETED, done.getStatus());
        verifyNoInteractions(workoutRepository);
    }

    @Test
    @DisplayName("Several plans by id are loaded, closed and saved once")
    void closeFinishedPlansLoadsAndSaves() {
        HorseTrainingPlan plan = plan(6L, TrainingPlanStatus.UPCOMING);
        when(planRepository.findAllById(Set.of(6L))).thenReturn(List.of(plan));
        when(workoutRepository.countByPlanIdAndStatusNotIn(eq(6L), anyList())).thenReturn(0L);
        when(workoutRepository.countByPlanIdAndStatus(6L, WorkoutStatus.COMPLETED)).thenReturn(0L);

        service.closeFinishedPlans(Set.of(6L));

        assertEquals(TrainingPlanStatus.CANCELLED, plan.getStatus());
        verify(planRepository).saveAll(List.of(plan));
    }

    @Test
    @DisplayName("No plan id: nothing is loaded")
    void noPlanIdDoesNothing() {
        service.closeFinishedPlans(Set.of());

        verify(planRepository, never()).findAllById(anyCollection());
    }
}
