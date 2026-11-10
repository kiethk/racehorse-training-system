package com.rtms.backend.training.service;

import com.rtms.backend.identity.entity.User;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CreateTrainingGroupRequest;
import com.rtms.backend.training.dto.CourseRunDetailResponse;
import com.rtms.backend.training.dto.CourseRunRequest;
import com.rtms.backend.training.dto.TrainingGroupResponse;
import com.rtms.backend.training.entity.Course;
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
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Trạng thái do ĐỒNG HỒ quyết định: buổi hết giờ là HELD, kế hoạch tới ngày bắt đầu là ACTIVE, kế hoạch hết buổi chờ chạy thì đóng,
 * và không điều nào phụ thuộc việc đã ghi kết quả hay chưa. Đồng hồ test được đặt bằng tay, dữ liệu nằm trong một transaction rồi
 * rollback.
 * (Statuses are decided by the CLOCK: an ended Session is HELD, a course run at its start date is ACTIVE, a course run with nothing left to run
 * closes, and none of it depends on results being recorded. The test clock is set by hand; data lives in one transaction that rolls back.)
 */
@SpringBootTest
@Import({TestClockConfig.class, TrainingFixtures.class})
@Transactional
class SessionSettlementIntegrationTest {

    private static final List<TrainingDay> MON_WED_FRI =
            List.of(TrainingDay.MONDAY, TrainingDay.WEDNESDAY, TrainingDay.FRIDAY);

    @Autowired private SessionSettlementService settlement;
    @Autowired private CourseRunService courseRunService;
    @Autowired private TrainingGroupService groupService;
    @Autowired private TrainingFixtures fixtures;
    @Autowired private MutableClock clock;
    @Autowired private CourseRunRepository courseRuns;
    @Autowired private TrainingSessionRepository sessions;
    @Autowired private TrainingWorkoutRepository workouts;
    @Autowired private JdbcTemplate jdbc;

    private AuthenticatedUser trainer;
    private CourseRunDetailResponse courseRun;
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
        Course course = fixtures.course(trainerUser, "Sprint", 3, fixtures.exercise(trainerUser, "S1", 30));
        // Sessions: Monday, Wednesday and Friday at 06:00 to 06:30.
        courseRun = courseRunService.createCourseRun(new CourseRunRequest(group.id(), course.getId(), monday, MON_WED_FRI), trainer);
    }

    @AfterEach
    void restoreClock() {
        clock.reset();
    }

    @Test
    void nothingChangesBeforeTheFirstSessionEnds() {
        clock.set(monday.minusDays(1).atTime(12, 0));

        settlement.settle();

        assertEquals(CourseRunStatus.UPCOMING, courseRunStatus());
        assertTrue(allSessions().stream().allMatch(session -> session.getStatus() == SessionStatus.SCHEDULED));
    }

    @Test
    void aSessionThatHasStartedButNotEndedIsStillScheduled() {
        clock.set(monday.atTime(6, 15));

        settlement.settle();

        assertEquals(SessionStatus.SCHEDULED, allSessions().get(0).getStatus());
        assertEquals(CourseRunStatus.ACTIVE, courseRunStatus());
    }

    @Test
    void settlingInsideAReadOnlyTransactionLeavesItToTheScheduler() {
        clock.set(monday.atTime(12, 0));
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);   // as inside the AI tools
        try {
            settlement.settle();
        } finally {
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
        }

        assertEquals(SessionStatus.SCHEDULED, allSessions().get(0).getStatus());
    }

    @Test
    void aSessionBecomesHeldWithItsWorkoutsWhenItsEndTimePasses() {
        clock.set(monday.atTime(12, 0));

        settlement.settle();

        List<TrainingSession> all = allSessions();
        assertEquals(SessionStatus.HELD, all.get(0).getStatus());
        assertEquals(SessionStatus.SCHEDULED, all.get(1).getStatus());
        assertTrue(workoutsOf(all.get(0)).stream().allMatch(workout -> workout.getStatus() == WorkoutStatus.HELD));
        assertTrue(workoutsOf(all.get(1)).stream().allMatch(workout -> workout.getStatus() == WorkoutStatus.SCHEDULED));
        assertEquals(CourseRunStatus.ACTIVE, courseRunStatus());
    }

    @Test
    void theCourseRunClosesAsCompletedWhenTheLastSessionEndsEvenWithoutAnyRecordedResult() {
        clock.set(monday.plusDays(5).atTime(7, 0));

        settlement.settle();

        assertEquals(CourseRunStatus.COMPLETED, courseRunStatus());
        assertTrue(allSessions().stream().allMatch(session -> session.getStatus() == SessionStatus.HELD));
        assertTrue(allSessions().stream().flatMap(session -> workoutsOf(session).stream())
                .allMatch(workout -> workout.getStatus() == WorkoutStatus.HELD && workout.getActualDistanceMeters() == null));
    }

    @Test
    void aCourseRunWhoseSessionsWereAllCancelledBeforeAnyWasHeldClosesAsCancelled() {
        allSessions().forEach(session -> session.setStatus(SessionStatus.CANCELLED));
        sessions.saveAllAndFlush(allSessions());
        clock.set(monday.minusDays(1).atTime(12, 0));

        settlement.settle();

        assertEquals(CourseRunStatus.CANCELLED, courseRunStatus());
    }

    @Test
    void aCourseRunWithSomeHeldAndTheRestCancelledClosesAsCompleted() {
        List<TrainingSession> all = allSessions();
        all.get(1).setStatus(SessionStatus.CANCELLED);
        all.get(2).setStatus(SessionStatus.CANCELLED);
        sessions.saveAllAndFlush(all);
        clock.set(monday.atTime(12, 0));

        settlement.settle();

        assertEquals(CourseRunStatus.COMPLETED, courseRunStatus());
    }

    @Test
    void settlingTwiceChangesNothingMore() {
        clock.set(monday.plusDays(2).atTime(12, 0));

        settlement.settle();
        CourseRunStatus first = courseRunStatus();
        long held = allSessions().stream().filter(session -> session.getStatus() == SessionStatus.HELD).count();
        settlement.settle();

        assertEquals(first, courseRunStatus());
        assertEquals(held, allSessions().stream().filter(session -> session.getStatus() == SessionStatus.HELD).count());
        assertEquals(2, held);
    }

    @Test
    void readingTheCourseRunListSettlesFirst() {
        clock.set(monday.plusDays(5).atTime(7, 0));

        String status = courseRunService.getCourseRuns(null, null, trainer).get(0).status();

        assertEquals("COMPLETED", status);
    }

    private CourseRunStatus courseRunStatus() {
        return courseRuns.findById(courseRun.courseRun().id()).orElseThrow().getStatus();
    }

    private List<TrainingSession> allSessions() {
        return sessions.findByCourseRunIdOrderBySessionDateAscStartTimeAsc(courseRun.courseRun().id());
    }

    private List<TrainingWorkout> workoutsOf(TrainingSession session) {
        return workouts.findAll().stream().filter(workout -> session.getId().equals(workout.getSessionId())).toList();
    }
}
