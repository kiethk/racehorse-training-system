package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CreateTrainingGroupRequest;
import com.rtms.backend.training.dto.GroupPlanDetailResponse;
import com.rtms.backend.training.dto.GroupPlanRequest;
import com.rtms.backend.training.dto.GroupPlanSummaryResponse;
import com.rtms.backend.training.dto.PlanHorseResponse;
import com.rtms.backend.training.dto.SessionHorseOutcomeResponse;
import com.rtms.backend.training.dto.TrainingGroupResponse;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.enums.TrainingDay;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Các ngựa của một kế hoạch (kèm Attendance) và các ngựa của từng buổi, suy từ Workout chứ không từ thành viên hiện tại của nhóm.
 * (The horses of a plan with Attendance and the horses of each Session, derived from Workouts and never from current membership.)
 */
@SpringBootTest
@Import({TestClockConfig.class, TrainingFixtures.class})
@Transactional
class GroupPlanHorsesIntegrationTest {

    private static final List<TrainingDay> MON_WED_FRI =
            List.of(TrainingDay.MONDAY, TrainingDay.WEDNESDAY, TrainingDay.FRIDAY);

    @Autowired private TrainingGroupService groupService;
    @Autowired private GroupPlanService planService;
    @Autowired private SessionWorkoutService workoutService;
    @Autowired private SessionSettlementService settlement;
    @Autowired private TrainingFixtures fixtures;
    @Autowired private MutableClock clock;
    @Autowired private JdbcTemplate jdbc;

    private User trainerUser;
    private AuthenticatedUser trainer;
    private Horse thunder;
    private Horse blaze;
    private TrainingGroupResponse group;
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
        Course course = fixtures.course(trainerUser, "Sprint", 3, fixtures.subject(trainerUser, "S1", 30));
        plan = planService.createPlan(new GroupPlanRequest(group.id(), course.getId(), monday, MON_WED_FRI), trainer);
    }

    @AfterEach
    void restoreClock() {
        clock.reset();
    }

    @Test
    void everyHorseOfThePlanShowsItsAttendanceAsRanOverSessionsNotCancelled() {
        clock.set(monday.plusDays(2).atTime(12, 0));   // Monday and Wednesday are over
        settlement.settle();
        fixtures.recordHeldResults(thunder, blaze);

        Map<String, PlanHorseResponse> byName = planHorses();

        assertEquals(2, byName.size());
        assertEquals(2, byName.get("Thunder").attended());
        assertEquals(3, byName.get("Thunder").total());
        assertEquals(2, byName.get("Blaze").attended());
    }

    @Test
    void aHorseMarkedAsNotRunningDoesNotCountInAttendance() {
        clock.set(monday.atTime(12, 0));
        settlement.settle();
        Long thunderMonday = sessionHorse(plan.sessions().get(0).id(), "Thunder").workoutId();

        workoutService.markDidNotRun(thunderMonday, trainer);
        fixtures.recordHeldResults(blaze);

        assertEquals(0, planHorses().get("Thunder").attended());
        assertEquals(1, planHorses().get("Blaze").attended());
    }

    @Test
    void aHeldWorkoutWithoutResultsDoesNotCountInAttendance() {
        clock.set(monday.atTime(12, 0));
        settlement.settle();

        assertEquals(0, planHorses().get("Thunder").attended());
        assertEquals("NEEDS_RESULT", sessionHorse(plan.sessions().get(0).id(), "Thunder").outcome());
    }

    @Test
    void sessionHorsesShowOutcomeToRunRanCancelledAndDidNotRun() {
        clock.set(monday.atTime(12, 0));
        settlement.settle();
        fixtures.recordHeldResults(blaze);
        workoutService.markDidNotRun(sessionHorse(plan.sessions().get(0).id(), "Thunder").workoutId(), trainer);
        groupService.removeHorse(group.id(), blaze.getId(), trainer);

        assertEquals("DID_NOT_RUN", sessionHorse(plan.sessions().get(0).id(), "Thunder").outcome());
        assertEquals("RAN", sessionHorse(plan.sessions().get(0).id(), "Blaze").outcome());
        assertEquals("TO_RUN", sessionHorse(plan.sessions().get(1).id(), "Thunder").outcome());
        assertEquals("CANCELLED", sessionHorse(plan.sessions().get(1).id(), "Blaze").outcome());
    }

    @Test
    void aHorseThatJoinedMidCourseIsOnlyInSessionsFromItsJoiningDay() {
        Horse flame = fixtures.horse("Flame");
        clock.set(monday.atTime(12, 0));
        settlement.settle();
        groupService.addHorse(group.id(), flame.getId(), trainer);

        assertFalse(horsesOfSession(0).contains("Flame"));
        assertTrue(horsesOfSession(1).contains("Flame"));
        assertEquals(0, planHorses().get("Flame").attended());
        assertEquals(3, planHorses().get("Flame").total());
    }

    @Test
    void listsStayCompleteWhenTheGroupHasNoMembersLeft() {
        clock.set(monday.plusDays(4).atTime(12, 0));   // whole course is over
        settlement.settle();
        fixtures.recordHeldResults(thunder);
        groupService.removeHorse(group.id(), thunder.getId(), trainer);
        groupService.removeHorse(group.id(), blaze.getId(), trainer);

        assertEquals(List.of("Blaze", "Thunder"), horsesOfSession(0));
        assertEquals(3, planHorses().get("Thunder").attended());
        assertEquals(2, planHorses().size());
    }

    @Test
    void anotherTrainerDoesNotFindThePlanOrItsSessions() {
        AuthenticatedUser other = fixtures.principal(fixtures.user("HEAD_TRAINER", "other"));

        assertEquals("GROUP_PLAN_NOT_FOUND", assertThrows(ApiException.class,
                () -> planService.getPlanHorses(plan.plan().id(), other)).getErrorCode());
        assertEquals("GROUP_PLAN_NOT_FOUND", assertThrows(ApiException.class,
                () -> planService.getSessionHorses(plan.plan().id(), plan.sessions().get(0).id(), other)).getErrorCode());
    }

    @Test
    void aSessionOfAnotherPlanIsNotFoundThroughThisPlan() {
        Horse echo = fixtures.horse("Echo");
        TrainingGroupResponse other = groupService.createGroup(new CreateTrainingGroupRequest("Nhom 2", List.of(echo.getId())), trainer);
        Course course = fixtures.course(trainerUser, "Other", 1, fixtures.subject(trainerUser, "S2", 30));
        GroupPlanDetailResponse otherPlan = planService.createPlan(
                new GroupPlanRequest(other.id(), course.getId(), monday.plusDays(28), MON_WED_FRI), trainer);

        assertEquals("SESSION_NOT_FOUND", assertThrows(ApiException.class,
                () -> planService.getSessionHorses(plan.plan().id(), otherPlan.sessions().get(0).id(), trainer)).getErrorCode());
    }

    @Test
    void theHorseFilterKeepsThePlansWhereTheHorseHasAWorkoutEvenCancelledOnes() {
        Horse echo = fixtures.horse("Echo");
        TrainingGroupResponse other = groupService.createGroup(new CreateTrainingGroupRequest("Nhom 2", List.of(echo.getId())), trainer);
        Course course = fixtures.course(trainerUser, "Other", 1, fixtures.subject(trainerUser, "S2", 30));
        GroupPlanDetailResponse otherPlan = planService.createPlan(
                new GroupPlanRequest(other.id(), course.getId(), monday.plusDays(28), MON_WED_FRI), trainer);
        clock.set(monday.atTime(12, 0));
        settlement.settle();
        groupService.removeHorse(group.id(), blaze.getId(), trainer);   // Blaze's later Workouts are cancelled

        assertEquals(List.of(plan.plan().id()), planIds(blaze.getId()));
        assertEquals(List.of(otherPlan.plan().id()), planIds(echo.getId()));
        assertEquals(2, planService.getPlans(null, null, trainer).size());
        assertTrue(planIds(fixtures.horse("Idle").getId()).isEmpty());
    }

    // ------------------------------------------------------------------ helpers

    private List<Long> planIds(Long horseId) {
        return planService.getPlans(null, horseId, trainer).stream().map(GroupPlanSummaryResponse::id).toList();
    }

    private Map<String, PlanHorseResponse> planHorses() {
        return planService.getPlanHorses(plan.plan().id(), trainer).stream()
                .collect(Collectors.toMap(PlanHorseResponse::horseName, Function.identity()));
    }

    private List<SessionHorseOutcomeResponse> sessionHorses(Long sessionId) {
        return planService.getSessionHorses(plan.plan().id(), sessionId, trainer);
    }

    private List<String> horsesOfSession(int index) {
        return sessionHorses(plan.sessions().get(index).id()).stream().map(SessionHorseOutcomeResponse::horseName).toList();
    }

    private SessionHorseOutcomeResponse sessionHorse(Long sessionId, String name) {
        return sessionHorses(sessionId).stream().filter(h -> h.horseName().equals(name)).findFirst().orElseThrow();
    }
}
