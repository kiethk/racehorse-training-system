package com.rtms.backend.training.repository;

import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.dto.DashboardHorseRow;
import com.rtms.backend.training.dto.DashboardPlanRow;
import com.rtms.backend.training.dto.GroomWorkoutRow;
import com.rtms.backend.training.dto.HorseFitnessTrendItemResponse;
import com.rtms.backend.training.dto.HorseScheduleItemResponse;
import com.rtms.backend.training.dto.PerformanceSessionRow;
import com.rtms.backend.training.dto.PastWorkoutRow;
import com.rtms.backend.training.dto.PlanSummaryResponse;
import com.rtms.backend.training.dto.RecentWorkoutRow;
import com.rtms.backend.training.dto.SessionHorseNameRow;
import com.rtms.backend.training.dto.SessionRow;
import com.rtms.backend.training.dto.SpecialtyWorkoutRow;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.GroupPlan;
import com.rtms.backend.training.entity.Subject;
import com.rtms.backend.training.entity.TrainingGroup;
import com.rtms.backend.training.entity.TrainingSession;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.SessionStatus;
import com.rtms.backend.training.enums.TrainingDay;
import com.rtms.backend.training.enums.TrainingPlanStatus;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.support.TrainingFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Chạy các câu JPQL JOIN/projection của mô hình nhóm trên PostgreSQL thật. Test Mockito không bắt được lỗi ở đây (constructor expression
 * sai kiểu, GROUP BY thiếu cột, tham số null...), nên phải chạy thật. Mọi dữ liệu nằm trong một transaction rồi rollback.
 * (Runs the JPQL JOIN/projection queries of the group model on a real PostgreSQL. Mockito tests cannot catch problems here (a mistyped
 * constructor expression, a missing GROUP BY column, null parameters...), so they must run for real. All data lives in one transaction
 * that rolls back.)
 */
@SpringBootTest
@Import(TrainingFixtures.class)
@Transactional
class TrainingSessionQueriesIntegrationTest {

    @Autowired private TrainingFixtures fixtures;
    @Autowired private TrainingGroupRepository groups;
    @Autowired private GroupPlanRepository plans;
    @Autowired private TrainingSessionRepository sessions;
    @Autowired private TrainingWorkoutRepository workouts;
    @Autowired private StableStallRepository stalls;
    @Autowired private JdbcTemplate jdbc;

    private final LocalDate today = LocalDate.now();

    private User trainer;
    private Horse thunder;      // owner one, groom one
    private Horse blaze;        // owner two, groom two
    private User groomOne;
    private User groomTwo;
    private GroupPlan plan;
    private TrainingSession held;           // today-3, Held, both horses have results
    private TrainingSession heldNoResult;   // today-2, Held, nobody recorded anything
    private TrainingSession ahead;          // today+2, Scheduled, both horses
    private TrainingSession cancelled;      // today+3, cancelled as a whole
    private Subject sprint;
    private Subject recovery;

    @BeforeEach
    void buildFixture() {
        jdbc.execute("SET CONSTRAINTS ALL IMMEDIATE");
        trainer = fixtures.trainer();
        thunder = fixtures.horse("Thunder");
        blaze = fixtures.horse("Blaze");
        groomOne = userById(stalls.findById(thunder.getCurrentStallId()).orElseThrow().getGroomId());
        groomTwo = userById(stalls.findById(blaze.getCurrentStallId()).orElseThrow().getGroomId());

        sprint = fixtures.subject(trainer, "Sprint", 60);
        recovery = fixtures.subject(trainer, "Walk", 30);
        recovery.setCategory(com.rtms.backend.training.enums.SubjectCategory.RECOVERY);
        recovery = fixtures.save(recovery);
        Course course = fixtures.course(trainer, "Sprint Base", 4, sprint);

        TrainingGroup group = new TrainingGroup();
        group.setTrainerId(trainer.getId());
        group.setName("Nhom 1");
        group = groups.saveAndFlush(group);

        plan = new GroupPlan();
        plan.setGroupId(group.getId());
        plan.setCourseId(course.getId());
        plan.setTrainerId(trainer.getId());
        plan.setStartDate(today.minusDays(3));
        plan.setEndDate(today.plusDays(3));
        plan.setTrainingDays(new LinkedHashSet<>(List.of(TrainingDay.MONDAY)));
        plan.setStatus(TrainingPlanStatus.ACTIVE);
        plan = plans.saveAndFlush(plan);

        held = session(sprint, today.minusDays(3), 6, SessionStatus.HELD);
        heldNoResult = session(recovery, today.minusDays(2), 6, SessionStatus.HELD);
        ahead = session(sprint, today.plusDays(2), 6, SessionStatus.SCHEDULED);
        cancelled = session(sprint, today.plusDays(3), 6, SessionStatus.CANCELLED);

        result(workout(held, thunder, groomOne, WorkoutStatus.HELD), 8, "800");
        result(workout(held, blaze, groomTwo, WorkoutStatus.HELD), 6, "800");
        workout(heldNoResult, thunder, groomOne, WorkoutStatus.HELD);
        workout(heldNoResult, blaze, groomTwo, WorkoutStatus.HELD);
        workout(ahead, thunder, groomOne, WorkoutStatus.SCHEDULED);
        workout(ahead, blaze, groomTwo, WorkoutStatus.SCHEDULED);
        workout(cancelled, thunder, groomOne, WorkoutStatus.CANCELLED);
        workout(cancelled, blaze, groomTwo, WorkoutStatus.CANCELLED);
    }

    // ------------------------------------------------------------- performance

    @Test
    void fitnessTrendListsOnlyWorkoutsThatRan() {
        List<HorseFitnessTrendItemResponse> trend = workouts.findFitnessTrend(thunder.getId(), today.minusDays(30), today);

        assertEquals(1, trend.size());
        assertEquals(today.minusDays(3), trend.get(0).getDate());
        assertEquals("Sprint", trend.get(0).getSubjectName());
    }

    @Test
    void performanceSessionsCoverAllRequestedHorses() {
        List<PerformanceSessionRow> rows = workouts.findPerformanceSessions(
                List.of(thunder.getId(), blaze.getId()), today.minusDays(30), today);

        assertEquals(2, rows.size());
        assertTrue(rows.stream().allMatch(row -> row.date().equals(today.minusDays(3))));
    }

    @Test
    void recentWorkoutsAreNewestFirstPerHorse() {
        List<RecentWorkoutRow> rows = workouts.findRecentCompletedWorkouts(
                List.of(thunder.getId(), blaze.getId()), today.minusDays(30));

        assertEquals(2, rows.size());
        assertEquals(thunder.getId(), rows.get(0).horseId());
    }

    @Test
    void specialtyRowsSkipWorkoutsWithoutResultsAndRecoveryWork() {
        List<SpecialtyWorkoutRow> rows = workouts.findSpecialtyWorkouts(
                List.of(thunder.getId(), blaze.getId()), today.minusDays(30));

        assertEquals(2, rows.size());
    }

    // ---------------------------------------------------------------- schedules

    @Test
    void aHorseScheduleIncludesCancelledSessionsInOrder() {
        List<HorseScheduleItemResponse> schedule = workouts.findHorseSchedule(thunder.getId(), today.minusDays(10), today.plusDays(10));

        assertEquals(4, schedule.size());
        assertEquals(today.minusDays(3), schedule.get(0).sessionDate());
        assertEquals(SessionStatus.CANCELLED, schedule.get(3).sessionStatus());
        assertEquals(WorkoutStatus.CANCELLED, schedule.get(3).workoutStatus());
    }

    @Test
    void aGroomDaySkipsCancelledWorkouts() {
        List<GroomWorkoutRow> day = workouts.findGroomWorkoutRows(groomOne.getId(), today.plusDays(2));
        List<GroomWorkoutRow> cancelledDay = workouts.findGroomWorkoutRows(groomOne.getId(), today.plusDays(3));

        assertEquals(1, day.size());
        assertEquals("Thunder", day.get(0).horseName());
        assertEquals("Sprint", day.get(0).subjectName());
        assertTrue(cancelledDay.isEmpty());
    }

    @Test
    void aWorkoutIsFoundOnlyByItsOwnTrainer() {
        TrainingWorkout any = workouts.findBySessionId(held.getId()).get(0);
        User other = fixtures.user("HEAD_TRAINER", "other");

        assertTrue(workouts.findByIdAndTrainerId(any.getId(), trainer.getId()).isPresent());
        assertFalse(workouts.findByIdAndTrainerId(any.getId(), other.getId()).isPresent());
    }

    @Test
    void pastWorkoutsListOnlySessionsThatTookPlaceNewestFirstWithTheirGroup() {
        List<PastWorkoutRow> past = workouts.findPastWorkouts(thunder.getId(), today);

        assertEquals(List.of(today.minusDays(2), today.minusDays(3)), past.stream().map(PastWorkoutRow::sessionDate).toList());
        assertEquals(List.of(WorkoutStatus.HELD, WorkoutStatus.RAN), past.stream().map(row -> row.workout().getStatus()).toList());
        assertNotNull(past.get(0).subjectName());
    }

    // ------------------------------------------------------------- plan summaries

    @Test
    void planSummariesCountEachHorsesOwnWorkoutsThatRanAndTheWholeCancelledSessions() {
        List<PlanSummaryResponse> summaries = plans.findPlanSummaries(trainer.getId(), null);

        assertEquals(2, summaries.size());
        PlanSummaryResponse mine = summaries.stream().filter(row -> row.getHorseId().equals(thunder.getId())).findFirst().orElseThrow();
        assertEquals("Thunder", mine.getHorseName());
        assertEquals("Sprint Base", mine.getCourseName());
        assertEquals(4, mine.getTotalSessions());
        assertEquals(1, mine.getCompletedSessions());     // the Held Workout without a result does not count
        assertEquals(1, mine.getCancelledSessions());
    }

    @Test
    void planSummariesCanBeNarrowedToOneHorse() {
        List<PlanSummaryResponse> summaries = plans.findPlanSummaries(null, blaze.getId());

        assertEquals(1, summaries.size());
        assertEquals(blaze.getId(), summaries.get(0).getHorseId());
    }

    @Test
    void dashboardPlansCarryTheSameCounters() {
        List<DashboardPlanRow> rows = plans.findDashboardPlans(List.of(thunder.getId(), blaze.getId()));

        assertEquals(2, rows.size());
        assertTrue(rows.stream().allMatch(row -> row.totalSessions() == 4 && row.completedSessions() == 1 && row.cancelledSessions() == 1));
    }

    @Test
    void horsesInTrainingCountEachHorseOnce() {
        long before = plans.countHorsesInTraining();
        assertTrue(before >= 2);
    }

    @Test
    void aTrainersDashboardHorsesAndAreaCheck() {
        List<DashboardHorseRow> horses = plans.findDashboardHorses(trainer.getId());
        User other = fixtures.user("HEAD_TRAINER", "other");

        // Area A of the shared database already houses other horses, so only check that ours are in the Trainer's scope.
        assertTrue(horses.stream().map(DashboardHorseRow::horseId).toList().containsAll(List.of(thunder.getId(), blaze.getId())));
        assertTrue(plans.existsHorseInTrainerArea(thunder.getId(), trainer.getId()));
        assertFalse(plans.existsHorseInTrainerArea(thunder.getId(), other.getId()));
    }

    @Test
    void aHorsesUnfinishedPlansAreFoundThroughItsWorkouts() {
        List<GroupPlan> found = plans.findPlansOfHorse(thunder.getId(), List.of(TrainingPlanStatus.ACTIVE, TrainingPlanStatus.UPCOMING));

        assertEquals(1, found.size());
        assertEquals(plan.getId(), found.get(0).getId());
    }

    // ------------------------------------------------------------ session rows

    @Test
    void sessionRowsHideCancelledSessionsUnlessAsked() {
        List<SessionRow> visible = sessions.findTrainerSessionRows(trainer.getId(), today.minusDays(10), today.plusDays(10), false);
        List<SessionRow> all = sessions.findTrainerSessionRows(trainer.getId(), today.minusDays(10), today.plusDays(10), true);

        assertEquals(3, visible.size());
        assertEquals(4, all.size());
        assertEquals(2L, visible.get(0).occupied());
        assertEquals("Sprint", visible.get(0).subjectName());
    }

    @Test
    void anOwnerSeesOnlySessionsWithAHorseOfTheirs() {
        List<SessionRow> rows = sessions.findOwnerSessionRows(thunder.getOwnerId(), today.minusDays(10), today.plusDays(10));
        List<SessionRow> none = sessions.findOwnerSessionRows(fixtures.user("HORSE_OWNER", "stranger").getId(), today.minusDays(10), today.plusDays(10));

        assertEquals(3, rows.size());
        assertTrue(none.isEmpty());
    }

    @Test
    void horseNamesOfSessionsCanBeNarrowedToAnOwner() {
        List<SessionHorseNameRow> everyone = sessions.findActiveHorseNames(List.of(held.getId()));
        List<SessionHorseNameRow> mine = sessions.findActiveHorseNamesOfOwner(List.of(held.getId()), thunder.getOwnerId());

        assertEquals(2, everyone.size());
        assertEquals(1, mine.size());
        assertEquals("Thunder", mine.get(0).horseName());
    }

    // ------------------------------------------------------------------ helpers

    private User userById(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private TrainingSession session(Subject subject, LocalDate date, int hour, SessionStatus status) {
        TrainingSession session = new TrainingSession();
        session.setGroupPlanId(plan.getId());
        session.setSubjectId(subject.getId());
        session.setTrainerId(trainer.getId());
        session.setSessionDate(date);
        session.setStartTime(LocalTime.of(hour, 0));
        session.setEndTime(LocalTime.of(hour + 1, 0));
        session.setStatus(status);
        return sessions.saveAndFlush(session);
    }

    private TrainingWorkout workout(TrainingSession session, Horse horse, User groom, WorkoutStatus status) {
        TrainingWorkout workout = new TrainingWorkout();
        workout.setSessionId(session.getId());
        workout.setHorseId(horse.getId());
        workout.setAssignedToId(groom.getId());
        workout.setStatus(status);
        return workouts.saveAndFlush(workout);
    }

    private TrainingWorkout result(TrainingWorkout workout, int rating, String distance) {
        workout.setActualDistanceMeters(new BigDecimal(distance));
        workout.setActualDurationMinutes(new BigDecimal("30"));
        workout.setTopSpeedKmh(new BigDecimal("55"));
        workout.setAverageSpeedKmh(new BigDecimal("48"));
        workout.setAverageHeartRate(150);
        workout.setMaxHeartRate(200);
        workout.setRecoveryHeartRate(80);
        workout.setPerformanceRating(rating);
        workout.setStatus(WorkoutStatus.RAN);
        return workouts.saveAndFlush(workout);
    }
}
