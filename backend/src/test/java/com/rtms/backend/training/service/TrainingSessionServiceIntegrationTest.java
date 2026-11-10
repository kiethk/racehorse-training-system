package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.security.Roles;
import com.rtms.backend.training.dto.CreateTrainingGroupRequest;
import com.rtms.backend.training.dto.CourseRunDetailResponse;
import com.rtms.backend.training.dto.CourseRunRequest;
import com.rtms.backend.training.dto.CourseRunSessionResponse;
import com.rtms.backend.training.dto.TrainingGroupResponse;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.CourseRun;
import com.rtms.backend.training.entity.TrainingSession;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.SessionStatus;
import com.rtms.backend.training.enums.TrainingDay;
import com.rtms.backend.training.enums.CourseRunStatus;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.CourseRunRepository;
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
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dời giờ, hủy buổi, hủy Workout của một chiến mã và hủy cả khóa. Mọi thao tác theo một luật: chỉ khi buổi CHƯA bắt đầu. Đồng hồ
 * test được đặt bằng tay, dữ liệu nằm trong một transaction rồi rollback.
 * (Moving, cancelling a Session, cancelling one horse's Workout and dropping the whole course. Every operation follows one rule: only
 * before the Session has STARTED. The test clock is set by hand; data lives in one transaction that rolls back.)
 */
@SpringBootTest
@Import({TestClockConfig.class, TrainingFixtures.class})
@Transactional
class TrainingSessionServiceIntegrationTest {

    private static final List<TrainingDay> MON_WED_FRI =
            List.of(TrainingDay.MONDAY, TrainingDay.WEDNESDAY, TrainingDay.FRIDAY);

    @Autowired private TrainingSessionService service;
    @Autowired private CourseRunService courseRunService;
    @Autowired private TrainingGroupService groupService;
    @Autowired private SessionSettlementService settlement;
    @Autowired private TrainingFixtures fixtures;
    @Autowired private MutableClock clock;
    @Autowired private CourseRunRepository courseRuns;
    @Autowired private TrainingSessionRepository sessions;
    @Autowired private TrainingWorkoutRepository workouts;
    @Autowired private JdbcTemplate jdbc;

    private AuthenticatedUser trainer;
    private User trainerUser;
    private TrainingGroupResponse group;
    private Course course;
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
        group = groupService.createGroup(new CreateTrainingGroupRequest("Nhom 1",
                List.of(fixtures.horse("Thunder").getId(), fixtures.horse("Blaze").getId())), trainer);
        course = fixtures.course(trainerUser, "Sprint", 3, fixtures.exercise(trainerUser, "S1", 30));
        // Sessions: Monday, Wednesday and Friday at 06:00 to 06:30.
        courseRun = courseRunService.createCourseRun(new CourseRunRequest(group.id(), course.getId(), monday, MON_WED_FRI), trainer);
    }

    @AfterEach
    void restoreClock() {
        clock.reset();
    }

    // -------------------------------------------------------------- reschedule

    @Test
    void aSessionIsMovedToAnotherQuarterHourKeepingItsLength() {
        CourseRunSessionResponse moved = service.reschedule(sessionId(0), LocalTime.of(7, 15), trainer);

        assertEquals(LocalTime.of(7, 15), moved.startTime());
        assertEquals(LocalTime.of(7, 45), moved.endTime());
    }

    @Test
    void aTimeOffTheQuarterHourIsRefused() {
        ApiException error = assertThrows(ApiException.class, () -> service.reschedule(sessionId(0), LocalTime.of(7, 10), trainer));
        assertEquals("INVALID_START_TIME", error.getErrorCode());
    }

    @Test
    void aTimeOutsideTheTrainingWindowIsRefused() {
        ApiException error = assertThrows(ApiException.class, () -> service.reschedule(sessionId(0), LocalTime.of(9, 45), trainer));
        assertEquals("OUTSIDE_TRAINING_WINDOW", error.getErrorCode());
    }

    @Test
    void aTimeThatOverlapsAnotherSessionOfTheTrainerIsRefused() {
        User secondUser = trainerUser;
        TrainingGroupResponse other = groupService.createGroup(new CreateTrainingGroupRequest("Nhom 2",
                List.of(fixtures.horse("Echo").getId())), trainer);
        Course second = fixtures.course(secondUser, "Stamina", 3, fixtures.exercise(secondUser, "T1", 60));
        courseRunService.createCourseRun(new CourseRunRequest(other.id(), second.getId(), monday, MON_WED_FRI), trainer);
        // The second group runs 06:30 to 07:30 on Monday; moving the first group to 07:00 would collide.

        ApiException error = assertThrows(ApiException.class, () -> service.reschedule(sessionId(0), LocalTime.of(7, 0), trainer));
        assertEquals("SESSION_OVERLAP", error.getErrorCode());
    }

    @Test
    void aSessionThatHasStartedCannotBeMoved() {
        clock.set(monday.atTime(6, 10));

        ApiException error = assertThrows(ApiException.class, () -> service.reschedule(sessionId(0), LocalTime.of(7, 0), trainer));
        assertEquals("SESSION_ALREADY_STARTED", error.getErrorCode());
    }

    // ----------------------------------------------------------- cancel session

    @Test
    void cancellingASessionCancelsItsWorkoutsAndKeepsTheCourseRunOpen() {
        CourseRunSessionResponse cancelled = service.cancel(sessionId(1), trainer);

        assertEquals("CANCELLED", cancelled.status());
        assertTrue(workoutsOf(1).stream().allMatch(workout -> workout.getStatus() == WorkoutStatus.CANCELLED));
        assertEquals(CourseRunStatus.UPCOMING, courseRunStatus());
        assertEquals(courseRun.courseRun().endDate(), courseRuns.findById(courseRun.courseRun().id()).orElseThrow().getEndDate());
    }

    @Test
    void aSessionThatHasStartedOrEndedCannotBeCancelled() {
        clock.set(monday.atTime(6, 10));
        ApiException started = assertThrows(ApiException.class, () -> service.cancel(sessionId(0), trainer));
        assertEquals("SESSION_ALREADY_STARTED", started.getErrorCode());

        clock.set(monday.atTime(12, 0));
        ApiException ended = assertThrows(ApiException.class, () -> service.cancel(sessionId(0), trainer));
        assertEquals("SESSION_ALREADY_STARTED", ended.getErrorCode());
    }

    @Test
    void aCancelledSessionCannotBeCancelledAgain() {
        service.cancel(sessionId(1), trainer);

        ApiException error = assertThrows(ApiException.class, () -> service.cancel(sessionId(1), trainer));
        assertEquals("SESSION_CANCELLED", error.getErrorCode());
    }

    @Test
    void cancellingTheLastSessionToRunClosesTheCourseRunByTheRule() {
        clock.set(monday.atTime(12, 0));
        settlement.settle();                       // the first Session is Held
        service.cancel(sessionId(1), trainer);
        service.cancel(sessionId(2), trainer);

        assertEquals(CourseRunStatus.COMPLETED, courseRunStatus());
    }

    @Test
    void cancellingEverySessionBeforeAnyWasHeldClosesTheCourseRunAsCancelled() {
        service.cancel(sessionId(0), trainer);
        service.cancel(sessionId(1), trainer);
        service.cancel(sessionId(2), trainer);

        assertEquals(CourseRunStatus.CANCELLED, courseRunStatus());
    }

    // ----------------------------------------------------------- cancel workout

    @Test
    void cancellingOneHorsesWorkoutKeepsTheRestOfTheSessionAndTheHorseInTheGroup() {
        TrainingWorkout thunder = workoutsOf(1).get(0);

        service.cancelWorkout(thunder.getId(), trainer);

        assertEquals(WorkoutStatus.CANCELLED, workouts.findById(thunder.getId()).orElseThrow().getStatus());
        assertEquals(WorkoutStatus.SCHEDULED, workoutsOf(1).get(1).getStatus());
        assertEquals(SessionStatus.SCHEDULED, sessions.findById(sessionId(1)).orElseThrow().getStatus());
        assertEquals(2, groupService.getGroups(trainer).get(0).members().size());
        // The horse still has its Workouts in the other Sessions.
        assertEquals(WorkoutStatus.SCHEDULED, workoutOf(thunder.getHorseId(), 0).getStatus());
        assertEquals(WorkoutStatus.SCHEDULED, workoutOf(thunder.getHorseId(), 2).getStatus());
    }

    @Test
    void cancellingTheLastWorkoutOfASessionCancelsTheSession() {
        service.cancelWorkout(workoutsOf(1).get(0).getId(), trainer);
        service.cancelWorkout(workoutsOf(1).get(1).getId(), trainer);

        assertEquals(SessionStatus.CANCELLED, sessions.findById(sessionId(1)).orElseThrow().getStatus());
    }

    @Test
    void aWorkoutOfASessionThatHasStartedCannotBeCancelled() {
        clock.set(monday.atTime(6, 10));
        Long workoutId = workoutsOf(0).get(0).getId();

        ApiException error = assertThrows(ApiException.class, () -> service.cancelWorkout(workoutId, trainer));
        assertEquals("SESSION_ALREADY_STARTED", error.getErrorCode());
    }

    // -------------------------------------------------------------- cancel course run

    @Test
    void cancellingTheCourseRunCancelsEverySessionAndReleasesItsDates() {
        CourseRunDetailResponse cancelled = courseRunService.cancelCourseRun(courseRun.courseRun().id(), trainer);

        assertEquals("CANCELLED", cancelled.courseRun().status());
        assertTrue(cancelled.sessions().stream().allMatch(session -> session.status().equals("CANCELLED")));

        // The dates are free again: the same course can be assigned over them at once.
        CourseRunDetailResponse again = courseRunService.createCourseRun(
                new CourseRunRequest(group.id(), course.getId(), monday, MON_WED_FRI), trainer);
        assertEquals("UPCOMING", again.courseRun().status());
    }

    @Test
    void cancellingTheCourseRunAfterSomeSessionsWereHeldIsStillCancelled() {
        clock.set(monday.plusDays(2).atTime(12, 0));
        settlement.settle();

        CourseRunDetailResponse cancelled = courseRunService.cancelCourseRun(courseRun.courseRun().id(), trainer);

        assertEquals("CANCELLED", cancelled.courseRun().status());
        assertEquals(2, cancelled.courseRun().sessionsHeld());
        assertEquals(1, cancelled.courseRun().sessionsCancelled());
    }

    @Test
    void cancellingTheCourseRunWhileASessionIsRunningLetsThatSessionFinish() {
        clock.set(monday.plusDays(2).atTime(6, 10));        // the second Session (Wednesday) is running

        CourseRunDetailResponse cancelled = courseRunService.cancelCourseRun(courseRun.courseRun().id(), trainer);
        assertEquals("CANCELLED", cancelled.courseRun().status());
        assertEquals(1, cancelled.courseRun().sessionsCancelled());

        clock.set(monday.plusDays(2).atTime(7, 0));
        settlement.settle();
        TrainingSession running = sessions.findById(sessionId(1)).orElseThrow();
        assertEquals(SessionStatus.HELD, running.getStatus());
        assertEquals(CourseRunStatus.CANCELLED, courseRunStatus());
    }

    @Test
    void aClosedCourseRunCannotBeCancelledAgain() {
        courseRunService.cancelCourseRun(courseRun.courseRun().id(), trainer);

        ApiException error = assertThrows(ApiException.class, () -> courseRunService.cancelCourseRun(courseRun.courseRun().id(), trainer));
        assertEquals("COURSE_RUN_ALREADY_CLOSED", error.getErrorCode());
    }

    @Test
    void anotherTrainerCannotCancelTheCourseRun() {
        AuthenticatedUser other = fixtures.principal(fixtures.user(Roles.HEAD_TRAINER, "other"));

        ApiException error = assertThrows(ApiException.class, () -> courseRunService.cancelCourseRun(courseRun.courseRun().id(), other));
        assertEquals("COURSE_RUN_NOT_FOUND", error.getErrorCode());
    }

    // ------------------------------------------------------------------ helpers

    private Long sessionId(int index) {
        return sessions.findByCourseRunIdOrderBySessionDateAscStartTimeAsc(courseRun.courseRun().id()).get(index).getId();
    }

    private CourseRunStatus courseRunStatus() {
        CourseRun found = courseRuns.findById(courseRun.courseRun().id()).orElseThrow();
        return found.getStatus();
    }

    private List<TrainingWorkout> workoutsOf(int sessionIndex) {
        Long id = sessionId(sessionIndex);
        return workouts.findAll().stream()
                .filter(workout -> id.equals(workout.getSessionId()))
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .toList();
    }

    private TrainingWorkout workoutOf(Long horseId, int sessionIndex) {
        return workoutsOf(sessionIndex).stream().filter(workout -> workout.getHorseId().equals(horseId)).findFirst().orElseThrow();
    }
}
