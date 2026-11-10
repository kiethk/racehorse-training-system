package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.RoleRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.security.Roles;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.dto.CreateTrainingGroupRequest;
import com.rtms.backend.training.dto.CourseRunDetailResponse;
import com.rtms.backend.training.dto.CourseRunPreviewResponse;
import com.rtms.backend.training.dto.CourseRunRequest;
import com.rtms.backend.training.dto.CourseRunSummaryResponse;
import com.rtms.backend.training.dto.CourseRunSessionResponse;
import com.rtms.backend.training.dto.TrainingGroupResponse;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.CourseExercise;
import com.rtms.backend.training.entity.Exercise;
import com.rtms.backend.training.enums.IntensityLevel;
import com.rtms.backend.training.enums.ExerciseCategory;
import com.rtms.backend.training.enums.SurfaceType;
import com.rtms.backend.training.enums.TrainingDay;
import com.rtms.backend.training.repository.CourseRepository;
import com.rtms.backend.training.repository.CourseExerciseRepository;
import com.rtms.backend.training.repository.CourseRunRepository;
import com.rtms.backend.training.repository.ExerciseRepository;
import com.rtms.backend.training.repository.TrainingSessionRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Giao khóa cho nhóm qua service trên PostgreSQL thật: ngày bắt đầu, xoay vòng bài, giờ từng buổi (first-fit theo ngày),
 * chồng ngày, ngày hết khe và Workout của mọi thành viên. Mọi ngày dùng trong test cách hôm nay hai tuần để kết quả không phụ thuộc
 * giờ chạy test. Mọi dữ liệu nằm trong một transaction rồi rollback.
 * (Assigning a course to a group through the service on a real PostgreSQL: start date, Exercise rotation, the time of each Session
 * (first-fit per day), date overlap, full days and every member's Workouts. Every date is two weeks away so the result does not
 * depend on when the test runs. All data lives in one transaction that rolls back.)
 */
@SpringBootTest
@Transactional
class CourseRunServiceIntegrationTest {

    private static final List<TrainingDay> MON_WED_FRI =
            List.of(TrainingDay.MONDAY, TrainingDay.WEDNESDAY, TrainingDay.FRIDAY);

    @Autowired private CourseRunService service;
    @Autowired private TrainingGroupService groupService;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private AreaRepository areas;
    @Autowired private StableStallRepository stalls;
    @Autowired private HorseRepository horses;
    @Autowired private ExerciseRepository exercises;
    @Autowired private CourseRepository courses;
    @Autowired private CourseExerciseRepository courseExercises;
    @Autowired private CourseRunRepository courseRuns;
    @Autowired private TrainingSessionRepository sessions;
    @Autowired private TrainingWorkoutRepository workouts;
    @Autowired private JdbcTemplate jdbc;

    private User trainer;
    private AuthenticatedUser trainerPrincipal;
    private Area area;
    private LocalDate monday;
    private int stallNumber = 600;

    @BeforeEach
    void buildFixture() {
        trainer = user(Roles.HEAD_TRAINER, "trainer");
        trainerPrincipal = new AuthenticatedUser(trainer.getId(), trainer.getEmail(), Roles.HEAD_TRAINER);
        area = areas.findAll().stream().filter(a -> "A".equals(a.getCode())).findFirst().orElseThrow();
        area.setTrainerId(trainer.getId());
        areas.saveAndFlush(area);
        jdbc.execute("SET CONSTRAINTS ALL IMMEDIATE");

        monday = LocalDate.now().plusDays(14);
        while (monday.getDayOfWeek() != DayOfWeek.MONDAY) {
            monday = monday.plusDays(1);
        }
    }

    // ------------------------------------------------------------------ create

    @Test
    void aCourseRunGetsRotatingExercisesFirstFitTimesAndWorkoutsForEveryMember() {
        TrainingGroupResponse group = groupOf("Nhom 1", 3);
        Course course = course("Sprint Base", 6, exercise("S1", 30), exercise("S2", 45), exercise("S3", 60));

        // Tuesday is not a training day, so the course run starts on Wednesday.
        CourseRunDetailResponse courseRun = service.createCourseRun(
                new CourseRunRequest(group.id(), course.getId(), monday.plusDays(1), MON_WED_FRI), trainerPrincipal);

        assertEquals(monday.plusDays(2), courseRun.courseRun().startDate());
        assertEquals(monday.plusDays(2).plusDays(12), courseRun.courseRun().endDate());
        assertEquals("UPCOMING", courseRun.courseRun().status());
        assertEquals(List.of("S1", "S2", "S3", "S1", "S2", "S3"),
                courseRun.sessions().stream().map(CourseRunSessionResponse::exerciseName).toList());
        assertTrue(courseRun.sessions().stream().allMatch(session -> session.startTime().equals(LocalTime.of(6, 0))));
        assertTrue(courseRun.sessions().stream().allMatch(session -> session.horseCount() == 3));
        assertEquals(6, courseRun.courseRun().sessionsTotal());
        assertEquals(18, workouts.count() - existingWorkouts);
    }

    @Test
    void previewShowsTheSameTimesAsCreateAndWritesNothing() {
        TrainingGroupResponse group = groupOf("Nhom 1", 2);
        Course course = course("Sprint Base", 3, exercise("S1", 30), exercise("S2", 45));
        CourseRunRequest request = new CourseRunRequest(group.id(), course.getId(), monday, MON_WED_FRI);
        long courseRunsBefore = courseRuns.count();

        CourseRunPreviewResponse preview = service.previewCourseRun(request, trainerPrincipal);

        assertEquals(courseRunsBefore, courseRuns.count());
        assertTrue(preview.canCreate());
        assertEquals(monday, preview.startDate());
        assertEquals(monday.plusDays(4), preview.endDate());
        assertEquals(2, preview.memberCount());

        CourseRunDetailResponse created = service.createCourseRun(request, trainerPrincipal);
        assertEquals(preview.sessions().stream().map(session -> session.startTime()).toList(),
                created.sessions().stream().map(CourseRunSessionResponse::startTime).toList());
    }

    @Test
    void aSecondGroupIsPlacedAfterTheFirstOneOnTheSameDay() {
        TrainingGroupResponse first = groupOf("Nhom 1", 2);
        TrainingGroupResponse second = groupOf("Nhom 2", 2);
        Course sprint = course("Sprint", 2, exercise("S1", 30));
        Course stamina = course("Stamina", 2, exercise("T1", 90));

        service.createCourseRun(new CourseRunRequest(first.id(), sprint.getId(), monday, MON_WED_FRI), trainerPrincipal);
        CourseRunDetailResponse courseRun = service.createCourseRun(
                new CourseRunRequest(second.id(), stamina.getId(), monday, MON_WED_FRI), trainerPrincipal);

        assertEquals(LocalTime.of(6, 30), courseRun.sessions().get(0).startTime());
        assertEquals(LocalTime.of(8, 0), courseRun.sessions().get(0).endTime());
    }

    @Test
    void aLongExerciseOnlyShiftsTheDayItBelongsTo() {
        TrainingGroupResponse first = groupOf("Nhom 1", 2);
        TrainingGroupResponse second = groupOf("Nhom 2", 2);
        Course mixed = course("Mixed", 2, exercise("Long", 120), exercise("Short", 30));
        Course steady = course("Steady", 2, exercise("Hour", 60));

        service.createCourseRun(new CourseRunRequest(first.id(), mixed.getId(), monday, MON_WED_FRI), trainerPrincipal);
        CourseRunDetailResponse courseRun = service.createCourseRun(
                new CourseRunRequest(second.id(), steady.getId(), monday, MON_WED_FRI), trainerPrincipal);

        // Day 1 follows the 120-minute Exercise, day 2 only the 30-minute one: the times differ by day.
        assertEquals(LocalTime.of(8, 0), courseRun.sessions().get(0).startTime());
        assertEquals(LocalTime.of(6, 30), courseRun.sessions().get(1).startTime());
    }

    // ------------------------------------------------------------- overlap, full

    @Test
    void unfinishedCourseRunsOfOneGroupCannotOverlapButTheNextCourseCanBeQueued() {
        TrainingGroupResponse group = groupOf("Nhom 1", 2);
        Course course = course("Sprint", 6, exercise("S1", 30));
        CourseRunDetailResponse first = service.createCourseRun(
                new CourseRunRequest(group.id(), course.getId(), monday, MON_WED_FRI), trainerPrincipal);

        ApiException overlap = assertThrows(ApiException.class, () -> service.createCourseRun(
                new CourseRunRequest(group.id(), course.getId(), first.courseRun().endDate().minusDays(2), MON_WED_FRI), trainerPrincipal));
        assertEquals("COURSE_RUN_OVERLAP", overlap.getErrorCode());

        CourseRunDetailResponse queued = service.createCourseRun(
                new CourseRunRequest(group.id(), course.getId(), first.courseRun().endDate().plusDays(1), MON_WED_FRI), trainerPrincipal);
        assertEquals("UPCOMING", queued.courseRun().status());
    }

    @Test
    void previewNamesTheOverlappingCourseRunAndTheEarliestFreeStart() {
        TrainingGroupResponse group = groupOf("Nhom 1", 2);
        Course course = course("Sprint", 6, exercise("S1", 30));
        CourseRunDetailResponse first = service.createCourseRun(
                new CourseRunRequest(group.id(), course.getId(), monday, MON_WED_FRI), trainerPrincipal);

        CourseRunPreviewResponse preview = service.previewCourseRun(
                new CourseRunRequest(group.id(), course.getId(), monday.plusDays(2), MON_WED_FRI), trainerPrincipal);

        assertFalse(preview.canCreate());
        assertEquals(1, preview.overlaps().size());
        assertEquals("Sprint", preview.overlaps().get(0).courseName());
        assertTrue(preview.earliestFreeStart().isAfter(first.courseRun().endDate()));
    }

    @Test
    void everyFullDayIsReportedTogetherAndNothingIsSaved() {
        TrainingGroupResponse blocker = groupOf("Nhom 1", 1);
        TrainingGroupResponse group = groupOf("Nhom 2", 1);
        Course allMorning = course("All morning", 3, exercise("Marathon", 240));
        Course hour = course("Hour", 3, exercise("Hour", 60));
        service.createCourseRun(new CourseRunRequest(blocker.id(), allMorning.getId(), monday, MON_WED_FRI), trainerPrincipal);
        long courseRunsBefore = courseRuns.count();
        CourseRunRequest request = new CourseRunRequest(group.id(), hour.getId(), monday, MON_WED_FRI);

        CourseRunPreviewResponse preview = service.previewCourseRun(request, trainerPrincipal);
        ApiException full = assertThrows(ApiException.class, () -> service.createCourseRun(request, trainerPrincipal));

        assertFalse(preview.canCreate());
        assertEquals(List.of(monday, monday.plusDays(2), monday.plusDays(4)), preview.fullDates());
        assertEquals("SCHEDULE_FULL", full.getErrorCode());
        assertEquals(courseRunsBefore, courseRuns.count());
    }

    // -------------------------------------------------------------- validation

    @Test
    void aGroupWithoutHorsesIsRefused() {
        TrainingGroupResponse group = groupOf("Nhom 1", 0);
        Course course = course("Sprint", 2, exercise("S1", 30));

        ApiException error = assertThrows(ApiException.class, () -> service.createCourseRun(
                new CourseRunRequest(group.id(), course.getId(), monday, MON_WED_FRI), trainerPrincipal));
        assertEquals("GROUP_EMPTY", error.getErrorCode());
    }

    @Test
    void aStartDateInThePastIsRefused() {
        TrainingGroupResponse group = groupOf("Nhom 1", 1);
        Course course = course("Sprint", 2, exercise("S1", 30));

        assertThrows(IllegalArgumentException.class, () -> service.createCourseRun(
                new CourseRunRequest(group.id(), course.getId(), LocalDate.now().minusDays(1), MON_WED_FRI), trainerPrincipal));
    }

    @Test
    void aCourseWithoutExercisesIsRefused() {
        TrainingGroupResponse group = groupOf("Nhom 1", 1);
        Course empty = course("Empty", 2);

        ApiException error = assertThrows(ApiException.class, () -> service.createCourseRun(
                new CourseRunRequest(group.id(), empty.getId(), monday, MON_WED_FRI), trainerPrincipal));
        assertEquals("COURSE_HAS_NO_EXERCISES", error.getErrorCode());
    }

    @Test
    void anotherTrainersGroupIsNotFound() {
        TrainingGroupResponse group = groupOf("Nhom 1", 1);
        Course course = course("Sprint", 2, exercise("S1", 30));
        User other = user(Roles.HEAD_TRAINER, "other");
        AuthenticatedUser otherPrincipal = new AuthenticatedUser(other.getId(), other.getEmail(), Roles.HEAD_TRAINER);

        ApiException error = assertThrows(ApiException.class, () -> service.createCourseRun(
                new CourseRunRequest(group.id(), course.getId(), monday, MON_WED_FRI), otherPrincipal));
        assertEquals("GROUP_NOT_FOUND", error.getErrorCode());
    }

    // ----------------------------------------------------------------- reading

    @Test
    void theListShowsTheOwnCourseRunsAndTheDetailIsPrivateToItsTrainer() {
        TrainingGroupResponse group = groupOf("Nhom 1", 1);
        Course course = course("Sprint", 3, exercise("S1", 30));
        CourseRunDetailResponse created = service.createCourseRun(
                new CourseRunRequest(group.id(), course.getId(), monday, MON_WED_FRI), trainerPrincipal);
        User other = user(Roles.HEAD_TRAINER, "other");
        AuthenticatedUser otherPrincipal = new AuthenticatedUser(other.getId(), other.getEmail(), Roles.HEAD_TRAINER);

        List<CourseRunSummaryResponse> own = service.getCourseRuns(group.id(), null, trainerPrincipal);

        assertEquals(1, own.size());
        assertEquals("Nhom 1", own.get(0).groupName());
        assertEquals("Sprint", own.get(0).courseName());
        assertEquals(3, own.get(0).sessionsTotal());
        assertTrue(service.getCourseRuns(null, null, otherPrincipal).isEmpty());
        ApiException error = assertThrows(ApiException.class, () -> service.getCourseRun(created.courseRun().id(), otherPrincipal));
        assertEquals("COURSE_RUN_NOT_FOUND", error.getErrorCode());
    }

    // ------------------------------------------------------------------ helpers

    private long existingWorkouts;

    @BeforeEach
    void countExistingWorkouts() {
        existingWorkouts = workouts.count();
    }

    private TrainingGroupResponse groupOf(String name, int horseCount) {
        List<Long> horseIds = new java.util.ArrayList<>();
        for (int i = 0; i < horseCount; i++) {
            horseIds.add(horse("H" + System.nanoTime(), user(Roles.GROOM, "groom")).getId());
        }
        return groupService.createGroup(new CreateTrainingGroupRequest(name, horseIds), trainerPrincipal);
    }

    private Exercise exercise(String name, int minutes) {
        Exercise exercise = new Exercise();
        exercise.setTrainerId(trainer.getId());
        exercise.setCategory(ExerciseCategory.SPEED);
        exercise.setName(name);
        exercise.setSurfaceType(SurfaceType.TURF);
        exercise.setTargetDistanceMeters(BigDecimal.valueOf(800));
        exercise.setIntensityLevel(IntensityLevel.HIGH);
        exercise.setDurationMinutes(minutes);
        return exercises.saveAndFlush(exercise);
    }

    private Course course(String name, int totalSessions, Exercise... orderedExercises) {
        Course course = new Course();
        course.setName(name);
        course.setTotalSessions(totalSessions);
        course.setCreatedById(trainer.getId());
        course.setTrainerId(trainer.getId());
        course = courses.saveAndFlush(course);
        int order = 1;
        for (Exercise exercise : orderedExercises) {
            CourseExercise link = new CourseExercise();
            link.setCourseId(course.getId());
            link.setExerciseId(exercise.getId());
            link.setOrderIndex(order++);
            courseExercises.saveAndFlush(link);
        }
        return course;
    }

    private User user(String role, String label) {
        User user = new User();
        user.setFullName(label);
        user.setEmail("gp-" + label + "-" + System.nanoTime() + "@test.invalid");
        user.setPasswordHash("not-used");
        user.setRole(roles.findByName(role).orElseThrow());
        user.setActive(true);
        return users.saveAndFlush(user);
    }

    private Horse horse(String name, User groom) {
        User owner = user(Roles.HORSE_OWNER, "owner");
        StableStall stall = new StableStall();
        stall.setAreaId(area.getId());
        stall.setGroomId(groom.getId());
        stall.setStallNumber(stallNumber);
        stall.setStallCode("GP-" + stallNumber + "-" + System.nanoTime());
        stallNumber++;
        stall = stalls.saveAndFlush(stall);
        Horse horse = new Horse();
        horse.setName(name);
        horse.setOwnerId(owner.getId());
        horse.setCurrentStallId(stall.getId());
        return horses.saveAndFlush(horse);
    }
}
