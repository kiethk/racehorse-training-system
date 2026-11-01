package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CompleteWorkoutRequest;
import com.rtms.backend.training.dto.CreateTrainingGroupRequest;
import com.rtms.backend.training.dto.GroupPlanDetailResponse;
import com.rtms.backend.training.dto.GroupPlanRequest;
import com.rtms.backend.training.dto.SessionWorkoutResponse;
import com.rtms.backend.training.dto.TrainingGroupResponse;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.TrainingSession;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.SessionStatus;
import com.rtms.backend.training.enums.TrainingDay;
import com.rtms.backend.training.enums.TrainingPlanStatus;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.GroupPlanRepository;
import com.rtms.backend.training.repository.TrainingSessionRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import com.rtms.backend.training.support.MutableClock;
import com.rtms.backend.training.support.TestClockConfig;
import com.rtms.backend.training.support.TrainingFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ghi kết quả của Workout trong buổi của nhóm: chỉ sau khi buổi hết giờ, không hạn chót, không đổi bất kỳ trạng thái nào, và lựa chọn
 * "Không chạy". Đồng hồ test được đặt bằng tay, dữ liệu nằm trong một transaction rồi rollback.
 * (Recording the results of a Workout in a group Session: only after the Session ended, no deadline, no status ever changes, and the
 * "Did not run" option. The test clock is set by hand; data lives in one transaction that rolls back.)
 */
@SpringBootTest
@Import({TestClockConfig.class, TrainingFixtures.class})
@Transactional
class SessionWorkoutServiceIntegrationTest {

    private static final List<TrainingDay> MON_WED_FRI =
            List.of(TrainingDay.MONDAY, TrainingDay.WEDNESDAY, TrainingDay.FRIDAY);

    @Autowired private SessionWorkoutService service;
    @Autowired private GroupPlanService planService;
    @Autowired private TrainingGroupService groupService;
    @Autowired private TrainingFixtures fixtures;
    @Autowired private MutableClock clock;
    @Autowired private GroupPlanRepository plans;
    @Autowired private TrainingSessionRepository sessions;
    @Autowired private TrainingWorkoutRepository workouts;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private AuthenticatedUser trainer;
    private GroupPlanDetailResponse plan;
    private LocalDate monday;

    @BeforeEach
    void buildFixture() {
        clock.reset();
        jdbc.execute("SET CONSTRAINTS ALL IMMEDIATE");
        User trainerUser = fixtures.trainer();
        trainer = fixtures.principal(trainerUser);

        monday = LocalDate.now().plusDays(14);
        while (monday.getDayOfWeek() != DayOfWeek.MONDAY) {
            monday = monday.plusDays(1);
        }
        TrainingGroupResponse group = groupService.createGroup(new CreateTrainingGroupRequest("Nhom 1",
                List.of(fixtures.horse("Thunder").getId(), fixtures.horse("Blaze").getId())), trainer);
        Course course = fixtures.course(trainerUser, "Sprint", 3, fixtures.subject(trainerUser, "S1", 30));
        plan = planService.createPlan(new GroupPlanRequest(group.id(), course.getId(), monday, MON_WED_FRI), trainer);
    }

    @AfterEach
    void restoreClock() {
        clock.reset();
    }

    // ------------------------------------------------------------------ timing

    @Test
    void resultsCannotBeRecordedBeforeTheSessionHasEnded() {
        clock.set(monday.atTime(6, 15));
        Long workoutId = firstWorkoutOfSession(0).getId();

        ApiException error = assertThrows(ApiException.class, () -> service.recordResult(workoutId, validResult(), trainer));
        assertEquals("WORKOUT_NOT_ENDED", error.getErrorCode());
        assertThrows(ApiException.class, () -> service.previewResult(workoutId, trainer));
    }

    @Test
    void resultsAreRecordedOnAHeldWorkoutWithoutChangingAnyStatus() {
        clock.set(monday.atTime(12, 0));
        Long workoutId = firstWorkoutOfSession(0).getId();

        SessionWorkoutResponse recorded = service.recordResult(workoutId, validResult(), trainer);

        assertEquals("HELD", recorded.status());
        assertTrue(recorded.hasResult());
        assertNotNull(recorded.performanceRating());
        assertEquals(SessionStatus.HELD, sessions.findById(recorded.sessionId()).orElseThrow().getStatus());
        assertEquals(TrainingPlanStatus.ACTIVE, plans.findById(plan.plan().id()).orElseThrow().getStatus());
    }

    @Test
    void resultsCanStillBeRecordedLongAfterThePlanClosed() {
        clock.set(monday.plusDays(60).atTime(12, 0));
        Long workoutId = firstWorkoutOfSession(0).getId();

        SessionWorkoutResponse recorded = service.recordResult(workoutId, validResult(), trainer);

        assertTrue(recorded.hasResult());
        assertEquals(TrainingPlanStatus.COMPLETED, plans.findById(plan.plan().id()).orElseThrow().getStatus());
    }

    @Test
    void aShortDistanceForAnInjuredHorseIsAccepted() {
        clock.set(monday.atTime(12, 0));
        CompleteWorkoutRequest request = validResult();
        request.setActualDistanceMeters(BigDecimal.valueOf(60));
        request.setActualDurationMinutes(BigDecimal.valueOf(4));

        SessionWorkoutResponse recorded = service.recordResult(firstWorkoutOfSession(0).getId(), request, trainer);

        assertEquals(0, BigDecimal.valueOf(60).compareTo(recorded.actualDistanceMeters()));
    }

    // ------------------------------------------------------------ record, edit

    @Test
    void recordingTwiceIsRefusedButEditingIsAllowed() {
        clock.set(monday.atTime(12, 0));
        Long workoutId = firstWorkoutOfSession(0).getId();
        service.recordResult(workoutId, validResult(), trainer);

        ApiException error = assertThrows(ApiException.class, () -> service.recordResult(workoutId, validResult(), trainer));
        assertEquals("WORKOUT_ALREADY_RECORDED", error.getErrorCode());

        CompleteWorkoutRequest edit = validResult();
        edit.setTrainerFeedback("Smoother than last week");
        assertEquals("Smoother than last week", service.updateResult(workoutId, edit, trainer).trainerFeedback());
    }

    @Test
    void editingBeforeAnythingWasRecordedIsRefused() {
        clock.set(monday.atTime(12, 0));
        Long workoutId = firstWorkoutOfSession(0).getId();

        ApiException error = assertThrows(ApiException.class, () -> service.updateResult(workoutId, validResult(), trainer));
        assertEquals("WORKOUT_NOT_RECORDED", error.getErrorCode());
    }

    @Test
    void everyMissingReadingIsRefusedWithAMessageNamingThatField() {
        clock.set(monday.atTime(12, 0));
        Long workoutId = firstWorkoutOfSession(0).getId();
        java.util.Map<String, java.util.function.Consumer<CompleteWorkoutRequest>> clears = new java.util.LinkedHashMap<>();
        clears.put("Actual distance", r -> r.setActualDistanceMeters(null));
        clears.put("Actual duration", r -> r.setActualDurationMinutes(null));
        clears.put("Top speed", r -> r.setTopSpeedKmh(null));
        clears.put("Average speed", r -> r.setAverageSpeedKmh(null));
        clears.put("Average heart rate", r -> r.setAverageHeartRate(null));
        clears.put("Maximum heart rate", r -> r.setMaxHeartRate(null));
        clears.put("Recovery heart rate", r -> r.setRecoveryHeartRate(null));

        clears.forEach((label, clear) -> {
            CompleteWorkoutRequest request = validResult();
            clear.accept(request);
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> service.recordResult(workoutId, request, trainer));
            assertEquals(label + " is required", error.getMessage());
        });
        assertFalse(workouts.findById(workoutId).orElseThrow().getActualDistanceMeters() != null);
    }

    // ------------------------------------------------------------------ rating

    @Test
    void theServerAlwaysComputesTheRatingFromTheReadings() {
        clock.set(monday.atTime(12, 0));
        Long workoutId = firstWorkoutOfSession(0).getId();

        SessionWorkoutResponse recorded = service.recordResult(workoutId, validResult(), trainer);

        assertEquals(9, recorded.performanceRating());
    }

    @Test
    void editingReadingsRecomputesTheRating() {
        clock.set(monday.atTime(12, 0));
        Long workoutId = firstWorkoutOfSession(0).getId();
        service.recordResult(workoutId, validResult(), trainer);
        CompleteWorkoutRequest edit = validResult();
        edit.setRecoveryHeartRate(190);
        edit.setAverageSpeedKmh(BigDecimal.valueOf(20));

        SessionWorkoutResponse edited = service.updateResult(workoutId, edit, trainer);

        assertEquals(WorkoutPerformanceCalculator.calculate(edit.getActualDistanceMeters(), edit.getActualDurationMinutes(),
                edit.getTopSpeedKmh(), edit.getAverageSpeedKmh(), edit.getAverageHeartRate(), edit.getMaxHeartRate(),
                edit.getRecoveryHeartRate()), edited.performanceRating());
        assertTrue(edited.performanceRating() < 9);
    }

    @Test
    void aRatingSentByTheClientIsRejectedAsAnUnsupportedField() {
        String json = """
                {"actualDistanceMeters":800,"actualDurationMinutes":30,"topSpeedKmh":55,"averageSpeedKmh":48,
                 "averageHeartRate":150,"maxHeartRate":200,"recoveryHeartRate":80,"performanceRating":1}""";

        assertThrows(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class,
                () -> objectMapper.readValue(json, CompleteWorkoutRequest.class));
    }

    // -------------------------------------------------------------- did not run

    @Test
    void aHorseThatDidNotRunBecomesCancelledAndItsSessionMatesAreUntouched() {
        clock.set(monday.atTime(12, 0));
        TrainingWorkout thunder = firstWorkoutOfSession(0);
        TrainingWorkout other = workoutsOfSession(0).get(1);

        SessionWorkoutResponse response = service.markDidNotRun(thunder.getId(), trainer);

        assertEquals("CANCELLED", response.status());
        assertEquals(WorkoutStatus.HELD, workouts.findById(other.getId()).orElseThrow().getStatus());
        ApiException error = assertThrows(ApiException.class,
                () -> service.recordResult(thunder.getId(), validResult(), trainer));
        assertEquals("WORKOUT_CANCELLED", error.getErrorCode());
    }

    @Test
    void aHorseWithRecordedResultsCannotBeMarkedAsNotRunning() {
        clock.set(monday.atTime(12, 0));
        Long workoutId = firstWorkoutOfSession(0).getId();
        service.recordResult(workoutId, validResult(), trainer);

        ApiException error = assertThrows(ApiException.class, () -> service.markDidNotRun(workoutId, trainer));
        assertEquals("WORKOUT_ALREADY_RECORDED", error.getErrorCode());
    }

    @Test
    void aHorseCannotBeMarkedAsNotRunningBeforeItsSessionEnded() {
        clock.set(monday.minusDays(1).atTime(12, 0));
        Long workoutId = firstWorkoutOfSession(0).getId();

        ApiException error = assertThrows(ApiException.class, () -> service.markDidNotRun(workoutId, trainer));
        assertEquals("WORKOUT_NOT_ENDED", error.getErrorCode());
    }

    // ------------------------------------------------------------------ access

    @Test
    void anotherTrainerDoesNotFindTheWorkout() {
        clock.set(monday.atTime(12, 0));
        Long workoutId = firstWorkoutOfSession(0).getId();
        User other = fixtures.user("HEAD_TRAINER", "other");
        AuthenticatedUser otherPrincipal = fixtures.principal(other);

        ApiException error = assertThrows(ApiException.class,
                () -> service.recordResult(workoutId, validResult(), otherPrincipal));
        assertEquals("WORKOUT_NOT_FOUND", error.getErrorCode());
        assertFalse(workouts.findById(workoutId).orElseThrow().getActualDistanceMeters() != null);
    }

    // ------------------------------------------------------------------ helpers

    private TrainingWorkout firstWorkoutOfSession(int sessionIndex) {
        return workoutsOfSession(sessionIndex).get(0);
    }

    private List<TrainingWorkout> workoutsOfSession(int sessionIndex) {
        TrainingSession session = sessions.findByGroupPlanIdOrderBySessionDateAscStartTimeAsc(plan.plan().id()).get(sessionIndex);
        return workouts.findAll().stream()
                .filter(workout -> session.getId().equals(workout.getSessionId()))
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .toList();
    }

    private CompleteWorkoutRequest validResult() {
        CompleteWorkoutRequest request = new CompleteWorkoutRequest();
        request.setActualDistanceMeters(BigDecimal.valueOf(800));
        request.setActualDurationMinutes(BigDecimal.valueOf(30));
        request.setTopSpeedKmh(BigDecimal.valueOf(55));
        request.setAverageSpeedKmh(BigDecimal.valueOf(48));
        request.setAverageHeartRate(150);
        request.setMaxHeartRate(200);
        request.setRecoveryHeartRate(80);
        return request;
    }
}
