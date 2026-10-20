package com.rtms.backend.training.service;

import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class TrainingWorkoutBatchAssignmentTest {
    private final TrainingWorkoutRepository repository = mock(TrainingWorkoutRepository.class);
    private final TrainingWorkoutService service = new TrainingWorkoutService(repository,
            mock(HorseTrainingPlanRepository.class), mock(TrainingLotRepository.class),
            mock(SubjectRepository.class), mock(TrainingLotService.class), mock(WorkoutResultSimulationService.class));

    private TrainingWorkout workout(long horse, long groom, WorkoutStatus status) {
        var w = new TrainingWorkout(); w.setId(horse); w.setHorseId(horse); w.setLotId(1L);
        w.setAssignedToId(groom); w.setStatus(status); return w;
    }

    @Test void sameLotSwapUsesFinalAssignmentsAndRetainsCompletedHistory() {
        var first = workout(1, 200, WorkoutStatus.SCHEDULED);
        var second = workout(2, 201, WorkoutStatus.SCHEDULED);
        var historical = workout(3, 202, WorkoutStatus.COMPLETED);
        when(repository.findFutureScheduledByHorsesForUpdate(anyCollection(), any())).thenReturn(List.of(first, second));
        when(repository.findNonCancelledByLotsForUpdate(anyCollection())).thenReturn(List.of(first, second, historical));
        assertEquals(2, service.reassignFutureWorkoutsToGrooms(Map.of(1L, 201L, 2L, 200L)));
        assertEquals(201L, first.getAssignedToId()); assertEquals(200L, second.getAssignedToId());
        assertEquals(202L, historical.getAssignedToId());
    }

    @Test void genuineFinalConflictDoesNotMutateAnyWorkout() {
        var first = workout(1, 200, WorkoutStatus.SCHEDULED);
        var second = workout(2, 201, WorkoutStatus.SCHEDULED);
        var third = workout(3, 202, WorkoutStatus.SCHEDULED);
        when(repository.findFutureScheduledByHorsesForUpdate(anyCollection(), any())).thenReturn(List.of(first, second));
        when(repository.findNonCancelledByLotsForUpdate(anyCollection())).thenReturn(List.of(first, second, third));
        assertThrows(IllegalStateException.class, () -> service.reassignFutureWorkoutsToGrooms(Map.of(1L, 203L, 2L, 202L)));
        assertEquals(200L, first.getAssignedToId()); assertEquals(201L, second.getAssignedToId());
        verify(repository, never()).saveAll(any());
    }

    @Test void completedPeerIsNotHypotheticallyReassignedDuringConflictCheck() {
        var first = workout(1, 200, WorkoutStatus.SCHEDULED);
        var history = workout(2, 201, WorkoutStatus.COMPLETED);
        when(repository.findFutureScheduledByHorsesForUpdate(anyCollection(), any())).thenReturn(List.of(first));
        when(repository.findNonCancelledByLotsForUpdate(anyCollection())).thenReturn(List.of(first, history));
        assertThrows(IllegalStateException.class, () -> service.reassignFutureWorkoutsToGrooms(Map.of(1L, 201L, 2L, 200L)));
        assertEquals(201L, history.getAssignedToId());
    }

    @Test void noFutureWorkoutsNeedsNoPeerQuery() {
        when(repository.findFutureScheduledByHorsesForUpdate(anyCollection(), any())).thenReturn(List.of());
        assertEquals(0, service.reassignFutureWorkoutsToGrooms(Map.of(1L, 200L)));
        verify(repository, never()).findNonCancelledByLotsForUpdate(anyCollection());
    }
}
