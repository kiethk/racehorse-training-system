package com.rtms.backend.horse.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.dto.HorseStatusChangeRequest;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.entity.GroomDailyTask;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.enums.StallStatus;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.GroomDailyTaskRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.entity.HorseTrainingPlan;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.enums.TrainingPlanStatus;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.HorseTrainingPlanRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import com.rtms.backend.training.service.TrainingWorkoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HorseStatusChangeTest {

    @Mock
    private HorseRepository horseRepository;
    @Mock
    private StableStallRepository stableStallRepository;
    @Mock
    private TrainingWorkoutService workoutService;
    @Mock
    private AreaRepository areaRepository;
    @Mock
    private TrainingWorkoutRepository workoutRepository;
    @Mock
    private HorseTrainingPlanRepository planRepository;
    @Mock
    private CareScheduleRepository careScheduleRepository;
    @Mock
    private GroomDailyTaskRepository groomDailyTaskRepository;
    @Mock
    private NotificationService notificationService;

    private HorseService horseService;

    private AuthenticatedUser managerUser;
    private AuthenticatedUser groomUser;

    @BeforeEach
    void setUp() {
        horseService = new HorseService(
                horseRepository,
                stableStallRepository,
                workoutService,
                areaRepository,
                workoutRepository,
                planRepository,
                careScheduleRepository,
                groomDailyTaskRepository,
                notificationService
        );

        managerUser = new AuthenticatedUser(1L, "admin@rtms.com", "CLUB_MANAGER");
        groomUser = new AuthenticatedUser(10L, "groom@rtms.com", "GROOM");
    }

    @Test
    @DisplayName("Manager changes ELIGIBLE horse to INACTIVE with full cascades")
    void managerChangesEligibleHorseToInactiveWithFullCascades() {
        Long horseId = 32L;
        Long stallId = 100L;
        Long ownerId = 55L;

        Horse horse = new Horse();
        horse.setId(horseId);
        horse.setName("BV KAROL");
        horse.setCurrentStatus(HorseStatus.ELIGIBLE);
        horse.setCurrentStallId(stallId);
        horse.setOwnerId(ownerId);
        horse.setTrainingDecision(TrainingDecision.ALLOWED);

        StableStall stall = new StableStall();
        stall.setId(stallId);
        stall.setStatus(StallStatus.OCCUPIED);

        TrainingWorkout workout = new TrainingWorkout();
        workout.setId(201L);
        workout.setHorseId(horseId);
        workout.setStatus(WorkoutStatus.SCHEDULED);

        HorseTrainingPlan plan = new HorseTrainingPlan();
        plan.setId(301L);
        plan.setHorseId(horseId);
        plan.setStatus(TrainingPlanStatus.ACTIVE);

        CareSchedule care = new CareSchedule();
        care.setId(401L);
        care.setHorseId(horseId);
        care.setStatus(CareScheduleStatus.SCHEDULED);

        GroomDailyTask groomTask = new GroomDailyTask();
        groomTask.setId(501L);
        groomTask.setHorseId(horseId);
        groomTask.setIsCompleted(false);

        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findById(stallId)).thenReturn(Optional.of(stall));
        when(workoutRepository.findFutureScheduledByHorsesForUpdate(eq(List.of(horseId)), any(LocalDate.class)))
                .thenReturn(List.of(workout));
        when(planRepository.findByHorseIdAndStatusInOrderByEndDateDesc(eq(horseId), anyList()))
                .thenReturn(List.of(plan));
        when(careScheduleRepository.findByHorseIdAndStatusIn(eq(horseId), anyList()))
                .thenReturn(List.of(care));
        when(groomDailyTaskRepository.findByHorseIdAndIsCompletedFalse(horseId))
                .thenReturn(List.of(groomTask));
        when(horseRepository.save(any(Horse.class))).thenAnswer(inv -> inv.getArgument(0));

        HorseStatusChangeRequest request = new HorseStatusChangeRequest(HorseStatus.INACTIVE, "Passed away from natural causes");
        Horse updated = horseService.changeHorseStatus(horseId, request, managerUser);

        // Assert Horse updates
        assertEquals(HorseStatus.INACTIVE, updated.getCurrentStatus());
        assertEquals("Passed away from natural causes", updated.getStatusReason());
        assertNull(updated.getCurrentStallId());
        assertEquals(TrainingDecision.BLOCKED, updated.getTrainingDecision());
        assertEquals("Passed away from natural causes", updated.getTrainingDecisionReason());

        // Assert Stall vacated
        assertEquals(StallStatus.AVAILABLE, stall.getStatus());
        verify(stableStallRepository).save(stall);

        // Assert Workout cancelled
        assertEquals(WorkoutStatus.CANCELLED, workout.getStatus());
        verify(workoutRepository).saveAll(List.of(workout));

        // Assert Plan cancelled
        assertEquals(TrainingPlanStatus.CANCELLED, plan.getStatus());
        verify(planRepository).saveAll(List.of(plan));

        // Assert Care cancelled
        assertEquals(CareScheduleStatus.CANCELLED, care.getStatus());
        assertTrue(care.getCancelReason().contains("Horse marked INACTIVE"));
        verify(careScheduleRepository).saveAll(List.of(care));

        // Assert Groom tasks deleted
        verify(groomDailyTaskRepository).deleteAll(List.of(groomTask));

        // Assert Owner notified
        verify(notificationService).createNotification(
                eq(ownerId),
                contains("BV KAROL"),
                contains("INACTIVE"),
                eq("HORSE_STATUS_CHANGE"),
                eq("HORSE"),
                eq(horseId),
                isNull()
        );
    }

    @Test
    @DisplayName("Manager can restore INACTIVE horse back to ELIGIBLE with automatic stall assignment")
    void managerCanRestoreInactiveHorseToEligible() {
        Long horseId = 32L;
        Horse horse = new Horse();
        horse.setId(horseId);
        horse.setCurrentStatus(HorseStatus.INACTIVE);
        horse.setStatusReason("Old departure reason");
        horse.setTrainingDecision(TrainingDecision.BLOCKED);

        StableStall stall = new StableStall();
        stall.setId(105L);
        stall.setGroomId(10L);
        stall.setStatus(StallStatus.AVAILABLE);

        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findFairAvailableStallForUpdate("REGULAR")).thenReturn(Optional.of(stall));
        when(horseRepository.save(any(Horse.class))).thenAnswer(inv -> inv.getArgument(0));

        HorseStatusChangeRequest request = new HorseStatusChangeRequest(HorseStatus.ELIGIBLE, "Horse returned to training");
        Horse updated = horseService.changeHorseStatus(horseId, request, managerUser);

        assertEquals(HorseStatus.ELIGIBLE, updated.getCurrentStatus());
        assertEquals("Horse returned to training", updated.getStatusReason());
        assertEquals(TrainingDecision.ALLOWED, updated.getTrainingDecision());
        assertNull(updated.getTrainingDecisionReason());
        assertEquals(105L, updated.getCurrentStallId());
        assertEquals(StallStatus.OCCUPIED, stall.getStatus());
        verify(workoutService).reassignFutureWorkoutsToGroom(horseId, 10L);
    }

    @Test
    @DisplayName("Restoring horse fails if no regular stall is available")
    void restoreFailsWhenNoRegularStallAvailable() {
        Long horseId = 32L;
        Horse horse = new Horse();
        horse.setId(horseId);
        horse.setCurrentStatus(HorseStatus.INACTIVE);

        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findFairAvailableStallForUpdate("REGULAR")).thenReturn(Optional.empty());

        HorseStatusChangeRequest request = new HorseStatusChangeRequest(HorseStatus.ELIGIBLE, "Horse returned to training");
        ApiException ex = assertThrows(ApiException.class, () ->
                horseService.changeHorseStatus(horseId, request, managerUser));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("NO_AVAILABLE_STALL", ex.getErrorCode());
    }

    @Test
    @DisplayName("Groom can report ELIGIBLE horse as INACTIVE if assigned to that stall")
    void groomCanReportEligibleHorseAsInactiveForAssignedStall() {
        Long horseId = 32L;
        Long stallId = 100L;
        Horse horse = new Horse();
        horse.setId(horseId);
        horse.setCurrentStatus(HorseStatus.ELIGIBLE);
        horse.setCurrentStallId(stallId);

        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));
        when(stableStallRepository.existsByIdAndGroomId(stallId, groomUser.getUserId())).thenReturn(true);
        when(horseRepository.save(any(Horse.class))).thenAnswer(inv -> inv.getArgument(0));

        HorseStatusChangeRequest request = new HorseStatusChangeRequest(HorseStatus.INACTIVE, "Groom reported emergency transfer");
        Horse updated = horseService.changeHorseStatus(horseId, request, groomUser);

        assertEquals(HorseStatus.INACTIVE, updated.getCurrentStatus());
    }

    @Test
    @DisplayName("Groom CANNOT report horse as INACTIVE if not assigned to that stall")
    void groomCannotReportHorseInUnassignedStall() {
        Long horseId = 32L;
        Long stallId = 100L;
        Horse horse = new Horse();
        horse.setId(horseId);
        horse.setCurrentStatus(HorseStatus.ELIGIBLE);
        horse.setCurrentStallId(stallId);

        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));
        when(stableStallRepository.existsByIdAndGroomId(stallId, groomUser.getUserId())).thenReturn(false);

        HorseStatusChangeRequest request = new HorseStatusChangeRequest(HorseStatus.INACTIVE, "Groom reported emergency");
        ApiException ex = assertThrows(ApiException.class, () ->
                horseService.changeHorseStatus(horseId, request, groomUser));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("You are only permitted to update horses assigned to your stalls"));
    }

    @Test
    @DisplayName("Groom CANNOT restore INACTIVE horse to ELIGIBLE")
    void groomCannotRestoreInactiveHorseToEligible() {
        Long horseId = 32L;
        Horse horse = new Horse();
        horse.setId(horseId);
        horse.setCurrentStatus(HorseStatus.INACTIVE);

        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));

        HorseStatusChangeRequest request = new HorseStatusChangeRequest(HorseStatus.ELIGIBLE, "Restore");
        ApiException ex = assertThrows(ApiException.class, () ->
                horseService.changeHorseStatus(horseId, request, groomUser));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("Grooms are only permitted to report a horse as INACTIVE"));
    }

    @Test
    @DisplayName("Cannot change status between CANDIDATE or REJECTED")
    void cannotChangeStatusFromCandidateOrRejected() {
        Long horseId = 32L;
        Horse horse = new Horse();
        horse.setId(horseId);
        horse.setCurrentStatus(HorseStatus.CANDIDATE);

        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));

        HorseStatusChangeRequest request = new HorseStatusChangeRequest(HorseStatus.INACTIVE, "Trying to bypass admission");
        ApiException ex = assertThrows(ApiException.class, () ->
                horseService.changeHorseStatus(horseId, request, managerUser));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("admission process"));
    }

    @Test
    @DisplayName("Status change resolves enum reason code to human readable text and stores code")
    void statusChangeResolvesEnumReasonCode() {
        Long horseId = 32L;
        Horse horse = new Horse();
        horse.setId(horseId);
        horse.setName("BV KAROL");
        horse.setCurrentStatus(HorseStatus.ELIGIBLE);

        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));
        when(horseRepository.save(any(Horse.class))).thenAnswer(inv -> inv.getArgument(0));

        HorseStatusChangeRequest request = new HorseStatusChangeRequest(
                HorseStatus.INACTIVE, "RETIRED", null, null);
        Horse updated = horseService.changeHorseStatus(horseId, request, managerUser);

        assertEquals(HorseStatus.INACTIVE, updated.getCurrentStatus());
        assertEquals("RETIRED", updated.getStatusReasonCode());
        assertEquals("Retired from training & competition", updated.getStatusReason());
    }
}
