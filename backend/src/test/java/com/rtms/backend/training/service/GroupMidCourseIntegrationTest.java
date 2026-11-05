package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CreateTrainingGroupRequest;
import com.rtms.backend.training.dto.GroupPlanDetailResponse;
import com.rtms.backend.training.dto.GroupPlanRequest;
import com.rtms.backend.training.dto.HorseAttendanceResponse;
import com.rtms.backend.training.dto.TrainingGroupResponse;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.TrainingSession;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.SessionStatus;
import com.rtms.backend.training.enums.TrainingDay;
import com.rtms.backend.training.enums.TrainingPlanStatus;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.GroupPlanRepository;
import com.rtms.backend.training.repository.TrainingGroupMemberRepository;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ngựa vào, ra và chuyển nhóm giữa khóa, ngựa bị Thú y chặn tập, và lịch sử Attendance suy từ Workout. Đồng hồ test được đặt bằng
 * tay, dữ liệu nằm trong một transaction rồi rollback.
 * (Horses joining, leaving and moving between groups mid-course, a horse blocked by the Vet, and the Attendance history derived from
 * Workouts. The test clock is set by hand; data lives in one transaction that rolls back.)
 */
@SpringBootTest
@Import({TestClockConfig.class, TrainingFixtures.class})
@Transactional
class GroupMidCourseIntegrationTest {

    private static final List<TrainingDay> MON_WED_FRI =
            List.of(TrainingDay.MONDAY, TrainingDay.WEDNESDAY, TrainingDay.FRIDAY);

    @Autowired private TrainingGroupService groupService;
    @Autowired private GroupPlanService planService;
    @Autowired private SessionSettlementService settlement;
    @Autowired private HorseAttendanceService attendanceService;
    @Autowired private TrainingDecisionService decisionService;
    @Autowired private GroupWorkoutSyncService groupSync;
    @Autowired private TrainingFixtures fixtures;
    @Autowired private MutableClock clock;
    @Autowired private HorseRepository horses;
    @Autowired private GroupPlanRepository plans;
    @Autowired private TrainingSessionRepository sessions;
    @Autowired private TrainingWorkoutRepository workouts;
    @Autowired private TrainingGroupMemberRepository members;
    @Autowired private JdbcTemplate jdbc;

    private User trainerUser;
    private AuthenticatedUser trainer;
    private Horse thunder;
    private Horse blaze;
    private TrainingGroupResponse group;
    private Course course;
    private GroupPlanDetailResponse plan;
    private LocalDate monday;

    @BeforeEach
    void buildFixture() {
        clock.reset();
        jdbc.execute("SET CONSTRAINTS ALL IMMEDIATE");
        trainerUser = fixtures.trainer();
        trainer = fixtures.principal(trainerUser);

        monday = LocalDate.now().plusDays(14);
        while (monday.getDayOfWeek() != DayOfWeek.MONDAY) {
            monday = monday.plusDays(1);
        }
        thunder = fixtures.horse("Thunder");
        blaze = fixtures.horse("Blaze");
        group = groupService.createGroup(new CreateTrainingGroupRequest("Nhom 1", List.of(thunder.getId(), blaze.getId())), trainer);
        course = fixtures.course(trainerUser, "Sprint", 3, fixtures.subject(trainerUser, "S1", 30));
        // Sessions: Monday, Wednesday and Friday at 06:00 to 06:30.
        plan = planService.createPlan(new GroupPlanRequest(group.id(), course.getId(), monday, MON_WED_FRI), trainer);
    }

    @AfterEach
    void restoreClock() {
        clock.reset();
    }

    // -------------------------------------------------------------------- join

    @Test
    void aHorseJoiningBeforeTheCourseStartsGetsAWorkoutInEverySession() {
        Horse flame = fixtures.horse("Flame");

        groupService.addHorse(group.id(), flame.getId(), trainer);

        assertEquals(3, workoutsOf(flame).size());
    }

    @Test
    void aHorseJoiningMidCourseGetsWorkoutsOnlyForSessionsThatHaveNotStarted() {
        Horse flame = fixtures.horse("Flame");
        clock.set(monday.atTime(12, 0));            // the first Session is over

        groupService.addHorse(group.id(), flame.getId(), trainer);

        assertEquals(2, workoutsOf(flame).size());
        assertTrue(workoutsOf(flame).stream().allMatch(workout -> workout.getStatus() == WorkoutStatus.SCHEDULED));
    }

    @Test
    void aSessionThatIsRunningIsNotOfferedToANewHorse() {
        Horse flame = fixtures.horse("Flame");
        clock.set(monday.atTime(6, 15));

        groupService.addHorse(group.id(), flame.getId(), trainer);

        assertEquals(2, workoutsOf(flame).size());
    }

    @Test
    void aGroupWhoseCourseHasNoSessionLeftToAttendTakesNoNewHorse() {
        Course single = fixtures.course(trainerUser, "Single", 1, fixtures.subject(trainerUser, "One", 30));
        TrainingGroupResponse other = groupService.createGroup(
                new CreateTrainingGroupRequest("Nhom 2", List.of(fixtures.horse("Echo").getId())), trainer);
        planService.createPlan(new GroupPlanRequest(other.id(), single.getId(), monday.plusDays(7), MON_WED_FRI), trainer);
        Horse flame = fixtures.horse("Flame");
        clock.set(monday.plusDays(7).atTime(6, 10));  // its only Session is running

        ApiException error = assertThrows(ApiException.class, () -> groupService.addHorse(other.id(), flame.getId(), trainer));
        assertEquals("GROUP_PLAN_NO_SESSION_LEFT", error.getErrorCode());
    }

    // ------------------------------------------------------------------- leave

    @Test
    void aHorseThatLeavesLosesOnlyItsFutureWorkoutsAndTheGroupCarriesOn() {
        clock.set(monday.atTime(12, 0));
        settlement.settle();

        groupService.removeHorse(group.id(), thunder.getId(), trainer);

        assertEquals(WorkoutStatus.HELD, workoutsOf(thunder).get(0).getStatus());
        assertEquals(List.of(WorkoutStatus.CANCELLED, WorkoutStatus.CANCELLED),
                workoutsOf(thunder).subList(1, 3).stream().map(TrainingWorkout::getStatus).toList());
        assertTrue(workoutsOf(blaze).stream().noneMatch(workout -> workout.getStatus() == WorkoutStatus.CANCELLED));
        assertEquals(TrainingPlanStatus.ACTIVE, plans.findById(plan.plan().id()).orElseThrow().getStatus());
        assertTrue(members.findByHorseIdAndLeftAtIsNull(thunder.getId()).isEmpty());
    }

    @Test
    void whenEveryHorseLeavesTheRemainingSessionsAreCancelledAndThePlanCloses() {
        clock.set(monday.atTime(12, 0));
        settlement.settle();

        groupService.removeHorse(group.id(), thunder.getId(), trainer);
        groupService.removeHorse(group.id(), blaze.getId(), trainer);

        List<TrainingSession> all = sessions.findByGroupPlanIdOrderBySessionDateAscStartTimeAsc(plan.plan().id());
        assertEquals(SessionStatus.HELD, all.get(0).getStatus());
        assertEquals(SessionStatus.CANCELLED, all.get(1).getStatus());
        assertEquals(SessionStatus.CANCELLED, all.get(2).getStatus());
        assertEquals(TrainingPlanStatus.COMPLETED, plans.findById(plan.plan().id()).orElseThrow().getStatus());
    }

    @Test
    void whenEveryHorseLeavesBeforeAnySessionTheDatesAreReleased() {
        groupService.removeHorse(group.id(), thunder.getId(), trainer);
        groupService.removeHorse(group.id(), blaze.getId(), trainer);

        assertEquals(TrainingPlanStatus.CANCELLED, plans.findById(plan.plan().id()).orElseThrow().getStatus());
    }

    // -------------------------------------------------------------------- move

    @Test
    void aMovedHorseLeavesTheOldPlanAndTakesTheNewGroupsRemainingSessions() {
        Course stamina = fixtures.course(trainerUser, "Stamina", 3, fixtures.subject(trainerUser, "T1", 45));
        TrainingGroupResponse other = groupService.createGroup(
                new CreateTrainingGroupRequest("Nhom 2", List.of(fixtures.horse("Echo").getId())), trainer);
        GroupPlanDetailResponse secondPlan = planService.createPlan(
                new GroupPlanRequest(other.id(), stamina.getId(), monday, MON_WED_FRI), trainer);
        clock.set(monday.atTime(12, 0));
        settlement.settle();

        groupService.moveHorse(other.id(), thunder.getId(), trainer);

        List<TrainingWorkout> mine = workoutsOf(thunder);
        // 3 in the old plan, and 2 in the new one: its first Session is already over when the horse arrives.
        assertEquals(5, mine.size());
        assertEquals(1, mine.stream().filter(workout -> workout.getStatus() == WorkoutStatus.HELD).count());
        assertEquals(2, mine.stream().filter(workout -> workout.getStatus() == WorkoutStatus.CANCELLED).count());
        assertEquals(2, mine.stream().filter(workout -> workout.getStatus() == WorkoutStatus.SCHEDULED).count());
        assertEquals(other.id(), members.findByHorseIdAndLeftAtIsNull(thunder.getId()).orElseThrow().getGroupId());
        assertEquals(TrainingPlanStatus.ACTIVE, plans.findById(secondPlan.plan().id()).orElseThrow().getStatus());
    }

    // --------------------------------------------------------------- Vet block

    @Test
    void aHorseBlockedByTheVetLeavesItsGroupAndTheOthersKeepTraining() {
        decisionService.block(horses.findById(thunder.getId()).orElseThrow(), "Tendon strain");

        assertTrue(members.findByHorseIdAndLeftAtIsNull(thunder.getId()).isEmpty());
        assertTrue(workoutsOf(thunder).stream().allMatch(workout -> workout.getStatus() == WorkoutStatus.CANCELLED));
        assertTrue(workoutsOf(blaze).stream().allMatch(workout -> workout.getStatus() == WorkoutStatus.SCHEDULED));
        assertEquals(1, members.countByGroupIdAndLeftAtIsNull(group.id()));
    }

    @Test
    void aClearedHorseCanJoinAGroupAgain() {
        decisionService.block(horses.findById(thunder.getId()).orElseThrow(), "Tendon strain");
        decisionService.allow(horses.findById(thunder.getId()).orElseThrow());

        groupService.addHorse(group.id(), thunder.getId(), trainer);

        assertEquals(2, members.countByGroupIdAndLeftAtIsNull(group.id()));
    }

    @Test
    void aHorseThatAlreadyMovedOnceCanStillBeBlocked() {
        TrainingGroupResponse other = groupService.createGroup(new CreateTrainingGroupRequest("Nhom 2", List.of()), trainer);
        groupService.moveHorse(other.id(), thunder.getId(), trainer);

        decisionService.block(horses.findById(thunder.getId()).orElseThrow(), "Tendon strain");

        assertTrue(members.findByHorseIdAndLeftAtIsNull(thunder.getId()).isEmpty());
        assertEquals(0, members.countByGroupIdAndLeftAtIsNull(other.id()));
    }

    @Test
    void aHorseThatLeftDoesNotBlockItsGroomFromTakingAnotherHorseInTheGroup() {
        Long thunderGroom = group.members().stream()
                .filter(member -> member.horseId().equals(thunder.getId())).findFirst().orElseThrow().groomId();
        groupService.removeHorse(group.id(), thunder.getId(), trainer);

        groupSync.reassignFutureWorkoutsToGroom(blaze.getId(), thunderGroom);

        assertEquals(thunderGroom, members.findByHorseIdAndLeftAtIsNull(blaze.getId()).orElseThrow().getGroomId());
    }

    // -------------------------------------------------------------- attendance

    @Test
    void attendanceCountsWorkoutsThatRanOverTheSessionsThatWereNotCancelled() {
        clock.set(monday.plusDays(2).atTime(12, 0));       // two Sessions are over, the third is ahead
        settlement.settle();
        fixtures.recordHeldResults(thunder);

        HorseAttendanceResponse line = attendanceService.getHistory(thunder.getId(), trainer).get(0);

        assertEquals(2, line.attended());
        assertEquals(3, line.total());
        assertEquals(monday, line.joinedOn());
        assertEquals("Nhom 1", line.groupName());
        assertEquals("Sprint", line.courseName());
    }

    @Test
    void aLateJoinerAttendsFewerSessionsOutOfTheWholeCourse() {
        Horse flame = fixtures.horse("Flame");
        clock.set(monday.atTime(12, 0));
        groupService.addHorse(group.id(), flame.getId(), trainer);
        clock.set(monday.plusDays(4).atTime(12, 0));        // everything is over
        settlement.settle();
        fixtures.recordHeldResults(flame);

        HorseAttendanceResponse line = attendanceService.getHistory(flame.getId(), trainer).get(0);

        assertEquals(2, line.attended());
        assertEquals(3, line.total());
        assertEquals(monday.plusDays(2), line.joinedOn());
    }

    @Test
    void aHorseThatChangedGroupsShowsOneLinePerPlan() {
        Course stamina = fixtures.course(trainerUser, "Stamina", 3, fixtures.subject(trainerUser, "T1", 45));
        TrainingGroupResponse other = groupService.createGroup(
                new CreateTrainingGroupRequest("Nhom 2", List.of(fixtures.horse("Echo").getId())), trainer);
        planService.createPlan(new GroupPlanRequest(other.id(), stamina.getId(), monday, MON_WED_FRI), trainer);
        clock.set(monday.atTime(12, 0));
        settlement.settle();
        groupService.moveHorse(other.id(), thunder.getId(), trainer);
        clock.set(monday.plusDays(4).atTime(12, 0));
        settlement.settle();
        fixtures.recordHeldResults(thunder);

        List<HorseAttendanceResponse> history = attendanceService.getHistory(thunder.getId(), trainer);

        assertEquals(2, history.size());
        HorseAttendanceResponse sprint = history.stream().filter(line -> line.courseName().equals("Sprint")).findFirst().orElseThrow();
        HorseAttendanceResponse staminaLine = history.stream().filter(line -> line.courseName().equals("Stamina")).findFirst().orElseThrow();
        assertEquals(1, sprint.attended());
        assertEquals(3, sprint.total());
        assertEquals(2, staminaLine.attended());
        assertEquals(3, staminaLine.total());
        assertFalse(sprint.groupName().equals(staminaLine.groupName()));
    }

    @Test
    void aSessionCancelledAsAWholeLeavesTheDenominatorButAHorsesOwnCancelledWorkoutDoesNot() {
        List<TrainingSession> all = sessions.findByGroupPlanIdOrderBySessionDateAscStartTimeAsc(plan.plan().id());
        all.get(1).setStatus(SessionStatus.CANCELLED);
        sessions.saveAndFlush(all.get(1));
        workouts.cancelWorkoutsOfCancelledSessions();
        clock.set(monday.plusDays(4).atTime(12, 0));
        settlement.settle();
        fixtures.recordHeldResults(thunder);

        HorseAttendanceResponse line = attendanceService.getHistory(thunder.getId(), trainer).get(0);

        assertEquals(2, line.attended());
        assertEquals(2, line.total());
    }

    // ------------------------------------------------------------------ helpers

    private List<TrainingWorkout> workoutsOf(Horse horse) {
        return workouts.findByHorseIdAndSessionIdIsNotNull(horse.getId()).stream()
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .toList();
    }
}
