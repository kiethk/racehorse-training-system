package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.security.Roles;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.dto.CompleteWorkoutRequest;
import com.rtms.backend.training.dto.CreateTrainingGroupRequest;
import com.rtms.backend.training.dto.CourseRunDetailResponse;
import com.rtms.backend.training.dto.CourseRunRequest;
import com.rtms.backend.training.dto.ScheduleSessionResponse;
import com.rtms.backend.training.dto.SessionDetailResponse;
import com.rtms.backend.training.dto.TrainingGroupResponse;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.TrainingDay;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lịch các buổi của nhóm theo khoảng ngày: mỗi vai chỉ thấy phần của mình, nhãn "cần ghi kết quả", bộ lọc nhóm và bộ lọc thiếu kết quả,
 * dùng chung cho lịch hôm nay và lịch tuần. Đồng hồ test được đặt bằng tay, dữ liệu nằm trong một transaction rồi rollback.
 * (The schedule of group Sessions over a date range: each role sees only its own part, the "needs result" count, the group filter and
 * the missing-results filter, shared by Today and Week. The test clock is set by hand; data lives in one transaction that rolls back.)
 */
@SpringBootTest
@Import({TestClockConfig.class, TrainingFixtures.class})
@Transactional
class SessionScheduleServiceIntegrationTest {

    private static final List<TrainingDay> MON_WED_FRI =
            List.of(TrainingDay.MONDAY, TrainingDay.WEDNESDAY, TrainingDay.FRIDAY);

    @Autowired private SessionScheduleService schedule;
    @Autowired private SessionWorkoutService workoutService;
    @Autowired private CourseRunService courseRunService;
    @Autowired private TrainingGroupService groupService;
    @Autowired private TrainingFixtures fixtures;
    @Autowired private MutableClock clock;
    @Autowired private TrainingSessionRepository sessions;
    @Autowired private TrainingWorkoutRepository workouts;
    @Autowired private StableStallRepository stalls;
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
    void aTrainerSeesTheirOwnSessionsOverARange() {
        List<ScheduleSessionResponse> week = schedule.getSessions(monday, monday.plusDays(6), null, false, trainer);

        assertEquals(3, week.size());
        assertEquals("Nhom 1", week.get(0).groupName());
        assertEquals("Sprint", week.get(0).courseName());
        assertEquals("S1", week.get(0).exerciseName());
        assertEquals(2, week.get(0).horseCount());
        assertEquals(0, week.get(0).needsResultCount());
    }

    @Test
    void todayIsTheSameQueryOverOneDay() {
        List<ScheduleSessionResponse> today = schedule.getSessions(monday, monday, null, false, trainer);

        assertEquals(1, today.size());
        assertEquals(monday, today.get(0).date());
    }

    @Test
    void anotherTrainerSeesNothing() {
        AuthenticatedUser other = fixtures.principal(fixtures.user(Roles.HEAD_TRAINER, "other"));

        assertTrue(schedule.getSessions(monday, monday.plusDays(6), null, false, other).isEmpty());
        ApiException error = assertThrows(ApiException.class,
                () -> schedule.getSession(courseRun.sessions().get(0).id(), other));
        assertEquals("SESSION_NOT_FOUND", error.getErrorCode());
    }

    @Test
    void theGroupFilterKeepsOnlyThatGroup() {
        TrainingGroupResponse other = groupService.createGroup(
                new CreateTrainingGroupRequest("Nhom 2", List.of(fixtures.horse("Echo").getId())), trainer);
        Course second = fixtures.course(trainerUser, "Stamina", 3, fixtures.exercise(trainerUser, "T1", 45));
        courseRunService.createCourseRun(new CourseRunRequest(other.id(), second.getId(), monday, MON_WED_FRI), trainer);

        assertEquals(6, schedule.getSessions(monday, monday.plusDays(6), null, false, trainer).size());
        List<ScheduleSessionResponse> onlySecond = schedule.getSessions(monday, monday.plusDays(6), other.id(), false, trainer);
        assertEquals(3, onlySecond.size());
        assertTrue(onlySecond.stream().allMatch(session -> session.groupName().equals("Nhom 2")));
    }

    @Test
    void aHeldSessionShowsHowManyHorsesStillNeedAResult() {
        clock.set(monday.atTime(12, 0));

        ScheduleSessionResponse first = schedule.getSessions(monday, monday, null, false, trainer).get(0);

        assertEquals("HELD", first.status());
        assertEquals(2, first.needsResultCount());

        Long thunderWorkout = workoutOf(thunder, 0).getId();
        workoutService.recordResult(thunderWorkout, validResult(), trainer);
        assertEquals(1, schedule.getSessions(monday, monday, null, false, trainer).get(0).needsResultCount());
    }

    @Test
    void theNeedsResultFilterKeepsOnlySessionsWithMissingResults() {
        clock.set(monday.plusDays(2).atTime(12, 0));         // the first two Sessions are over
        workoutService.recordResult(workoutOf(thunder, 0).getId(), validResult(), trainer);
        workoutService.recordResult(workoutOf(blaze, 0).getId(), validResult(), trainer);

        List<ScheduleSessionResponse> missing = schedule.getSessions(monday, monday.plusDays(6), null, true, trainer);

        assertEquals(1, missing.size());
        assertEquals(monday.plusDays(2), missing.get(0).date());
    }

    @Test
    void aGroomSeesOnlySessionsWithAHorseTheyLeadAndOnlyThatHorse() {
        Long thunderGroomId = stalls.findById(thunder.getCurrentStallId()).orElseThrow().getGroomId();
        AuthenticatedUser groom = new AuthenticatedUser(thunderGroomId, "g@test.invalid", Roles.GROOM);

        List<ScheduleSessionResponse> mine = schedule.getSessions(monday, monday.plusDays(6), null, false, groom);
        SessionDetailResponse detail = schedule.getSession(mine.get(0).id(), groom);

        assertEquals(3, mine.size());
        assertEquals(1, mine.get(0).horseCount());
        assertEquals(1, detail.horses().size());
        assertEquals("Thunder", detail.horses().get(0).horseName());
    }

    @Test
    void anOwnerSeesOnlyTheirOwnHorse() {
        AuthenticatedUser owner = new AuthenticatedUser(blaze.getOwnerId(), "o@test.invalid", Roles.HORSE_OWNER);

        SessionDetailResponse detail = schedule.getSession(courseRun.sessions().get(0).id(), owner);

        assertEquals(1, detail.horses().size());
        assertEquals("Blaze", detail.horses().get(0).horseName());
    }

    @Test
    void aGroomWithNoHorseInTheSessionDoesNotFindIt() {
        AuthenticatedUser stranger = new AuthenticatedUser(fixtures.user(Roles.GROOM, "stranger").getId(), "s@test.invalid", Roles.GROOM);

        assertTrue(schedule.getSessions(monday, monday.plusDays(6), null, false, stranger).isEmpty());
        assertThrows(ApiException.class, () -> schedule.getSession(courseRun.sessions().get(0).id(), stranger));
    }

    @Test
    void aMistypedHugeRangeIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> schedule.getSessions(monday, monday.plusDays(1000), null, false, trainer));
        assertThrows(IllegalArgumentException.class, () -> schedule.getSessions(monday, monday.minusDays(1), null, false, trainer));
    }

    // ------------------------------------------------------------------ helpers

    private TrainingWorkout workoutOf(Horse horse, int sessionIndex) {
        Long sessionId = sessions.findByCourseRunIdOrderBySessionDateAscStartTimeAsc(courseRun.courseRun().id()).get(sessionIndex).getId();
        return workouts.findBySessionId(sessionId).stream()
                .filter(workout -> workout.getHorseId().equals(horse.getId())).findFirst().orElseThrow();
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
