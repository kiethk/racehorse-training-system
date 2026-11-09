package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.security.Roles;
import com.rtms.backend.training.dto.CreateTrainingGroupRequest;
import com.rtms.backend.training.dto.CourseRunDetailResponse;
import com.rtms.backend.training.dto.CourseRunRequest;
import com.rtms.backend.training.dto.CourseRunSummaryResponse;
import com.rtms.backend.training.dto.CourseRunHorseResponse;
import com.rtms.backend.training.dto.SessionHorseResponse;
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
 * (The horses of a course run with Attendance and the horses of each Session, derived from Workouts and never from current membership.)
 */
@SpringBootTest
@Import({TestClockConfig.class, TrainingFixtures.class})
@Transactional
class CourseRunHorsesIntegrationTest {

    private static final List<TrainingDay> MON_WED_FRI =
            List.of(TrainingDay.MONDAY, TrainingDay.WEDNESDAY, TrainingDay.FRIDAY);

    @Autowired private TrainingGroupService groupService;
    @Autowired private CourseRunService courseRunService;
    @Autowired private TrainingSessionService sessionService;
    @Autowired private TrainingWorkoutService workoutService;
    @Autowired private SessionSettlementService settlement;
    @Autowired private TrainingFixtures fixtures;
    @Autowired private MutableClock clock;
    @Autowired private JdbcTemplate jdbc;

    private User trainerUser;
    private AuthenticatedUser trainer;
    private Horse thunder;
    private Horse blaze;
    private TrainingGroupResponse group;
    private CourseRunDetailResponse courseRun;
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
        Course course = fixtures.course(trainerUser, "Sprint", 3, fixtures.exercise(trainerUser, "S1", 30));
        courseRun = courseRunService.createCourseRun(new CourseRunRequest(group.id(), course.getId(), monday, MON_WED_FRI), trainer);
    }

    @AfterEach
    void restoreClock() {
        clock.reset();
    }

    @Test
    void everyHorseOfTheCourseRunShowsItsAttendanceAsRanOverSessionsNotCancelled() {
        clock.set(monday.plusDays(2).atTime(12, 0));   // Monday and Wednesday are over
        settlement.settle();
        fixtures.recordHeldResults(thunder, blaze);

        Map<String, CourseRunHorseResponse> byName = courseRunHorses();

        assertEquals(2, byName.size());
        assertEquals(2, byName.get("Thunder").attended());
        assertEquals(3, byName.get("Thunder").total());
        assertEquals(2, byName.get("Blaze").attended());
    }

    @Test
    void aHorseMarkedAsNotRunningDoesNotCountInAttendance() {
        clock.set(monday.atTime(12, 0));
        settlement.settle();
        Long thunderMonday = sessionHorse(courseRun.sessions().get(0).id(), "Thunder").workoutId();

        workoutService.markDidNotRun(thunderMonday, trainer);
        fixtures.recordHeldResults(blaze);

        assertEquals(0, courseRunHorses().get("Thunder").attended());
        assertEquals(1, courseRunHorses().get("Blaze").attended());
    }

    @Test
    void aHeldWorkoutWithoutResultsDoesNotCountInAttendance() {
        clock.set(monday.atTime(12, 0));
        settlement.settle();

        assertEquals(0, courseRunHorses().get("Thunder").attended());
        assertEquals("HELD", sessionHorse(courseRun.sessions().get(0).id(), "Thunder").status());
    }

    @Test
    void sessionHorsesShowOutcomeToRunRanCancelledAndDidNotRun() {
        clock.set(monday.atTime(12, 0));
        settlement.settle();
        fixtures.recordHeldResults(blaze);
        workoutService.markDidNotRun(sessionHorse(courseRun.sessions().get(0).id(), "Thunder").workoutId(), trainer);
        groupService.removeHorse(group.id(), blaze.getId(), trainer);

        assertEquals("CANCELLED", sessionHorse(courseRun.sessions().get(0).id(), "Thunder").status());
        assertEquals("RAN", sessionHorse(courseRun.sessions().get(0).id(), "Blaze").status());
        assertEquals("SCHEDULED", sessionHorse(courseRun.sessions().get(1).id(), "Thunder").status());
        assertEquals("CANCELLED", sessionHorse(courseRun.sessions().get(1).id(), "Blaze").status());
    }

    @Test
    void aHorseThatJoinedMidCourseIsOnlyInSessionsFromItsJoiningDay() {
        Horse flame = fixtures.horse("Flame");
        clock.set(monday.atTime(12, 0));
        settlement.settle();
        groupService.addHorse(group.id(), flame.getId(), trainer);

        assertFalse(horsesOfSession(0).contains("Flame"));
        assertTrue(horsesOfSession(1).contains("Flame"));
        assertEquals(0, courseRunHorses().get("Flame").attended());
        assertEquals(3, courseRunHorses().get("Flame").total());
    }

    @Test
    void listsStayCompleteWhenTheGroupHasNoMembersLeft() {
        clock.set(monday.plusDays(4).atTime(12, 0));   // whole course is over
        settlement.settle();
        fixtures.recordHeldResults(thunder);
        groupService.removeHorse(group.id(), thunder.getId(), trainer);
        groupService.removeHorse(group.id(), blaze.getId(), trainer);

        assertEquals(List.of("Blaze", "Thunder"), horsesOfSession(0));
        assertEquals(3, courseRunHorses().get("Thunder").attended());
        assertEquals(2, courseRunHorses().size());
    }

    @Test
    void anotherTrainerDoesNotFindTheCourseRunOrItsSessions() {
        AuthenticatedUser other = fixtures.principal(fixtures.user(Roles.HEAD_TRAINER, "other"));

        assertEquals("COURSE_RUN_NOT_FOUND", assertThrows(ApiException.class,
                () -> courseRunService.getCourseRunHorses(courseRun.courseRun().id(), other)).getErrorCode());
        assertEquals("SESSION_NOT_FOUND", assertThrows(ApiException.class,
                () -> sessionService.getSession(courseRun.sessions().get(0).id(), other)).getErrorCode());
    }

    @Test
    void theHorseFilterKeepsTheCourseRunsWhereTheHorseHasAWorkoutEvenCancelledOnes() {
        Horse echo = fixtures.horse("Echo");
        TrainingGroupResponse other = groupService.createGroup(new CreateTrainingGroupRequest("Nhom 2", List.of(echo.getId())), trainer);
        Course course = fixtures.course(trainerUser, "Other", 1, fixtures.exercise(trainerUser, "S2", 30));
        CourseRunDetailResponse otherCourseRun = courseRunService.createCourseRun(
                new CourseRunRequest(other.id(), course.getId(), monday.plusDays(28), MON_WED_FRI), trainer);
        clock.set(monday.atTime(12, 0));
        settlement.settle();
        groupService.removeHorse(group.id(), blaze.getId(), trainer);   // Blaze's later Workouts are cancelled

        assertEquals(List.of(courseRun.courseRun().id()), courseRunIds(blaze.getId()));
        assertEquals(List.of(otherCourseRun.courseRun().id()), courseRunIds(echo.getId()));
        assertEquals(2, courseRunService.getCourseRuns(null, null, trainer).size());
        assertTrue(courseRunIds(fixtures.horse("Idle").getId()).isEmpty());
    }

    // ------------------------------------------------------------------ helpers

    private List<Long> courseRunIds(Long horseId) {
        return courseRunService.getCourseRuns(null, horseId, trainer).stream().map(CourseRunSummaryResponse::id).toList();
    }

    private Map<String, CourseRunHorseResponse> courseRunHorses() {
        return courseRunService.getCourseRunHorses(courseRun.courseRun().id(), trainer).stream()
                .collect(Collectors.toMap(CourseRunHorseResponse::horseName, Function.identity()));
    }

    private List<SessionHorseResponse> sessionHorses(Long sessionId) {
        return sessionService.getSession(sessionId, trainer).horses();
    }

    private List<String> horsesOfSession(int index) {
        return sessionHorses(courseRun.sessions().get(index).id()).stream().map(SessionHorseResponse::horseName).toList();
    }

    private SessionHorseResponse sessionHorse(Long sessionId, String name) {
        return sessionHorses(sessionId).stream().filter(h -> h.horseName().equals(name)).findFirst().orElseThrow();
    }
}
