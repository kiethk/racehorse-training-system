package com.rtms.backend.training.repository;

import com.rtms.backend.security.Roles;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.RoleRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.CourseRun;
import com.rtms.backend.training.entity.Exercise;
import com.rtms.backend.training.entity.TrainingGroup;
import com.rtms.backend.training.entity.TrainingGroupMember;
import com.rtms.backend.training.entity.TrainingSession;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.IntensityLevel;
import com.rtms.backend.training.enums.SessionStatus;
import com.rtms.backend.training.enums.ExerciseCategory;
import com.rtms.backend.training.enums.SurfaceType;
import com.rtms.backend.training.enums.TrainingDay;
import com.rtms.backend.training.enums.CourseRunStatus;
import com.rtms.backend.training.enums.WorkoutStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Các ràng buộc DB của mô hình nhóm huấn luyện, chạy trên PostgreSQL thật và đi thẳng vào repository (không qua service) để
 * chứng minh DB tự chặn dữ liệu sai dù service có sót. Mọi dữ liệu nằm trong một transaction rồi rollback.
 * (The DB guards of the training group model, on a real PostgreSQL and straight through the repositories (not the services) to
 * prove the DB refuses wrong data even if a service misses it. All data lives in one transaction that is rolled back.)
 */
@SpringBootTest
@Transactional
class TrainingGroupSchemaIntegrationTest {

    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private AreaRepository areas;
    @Autowired private StableStallRepository stalls;
    @Autowired private HorseRepository horses;
    @Autowired private ExerciseRepository exercises;
    @Autowired private CourseRepository courses;
    @Autowired private TrainingGroupRepository groups;
    @Autowired private TrainingGroupMemberRepository members;
    @Autowired private CourseRunRepository courseRuns;
    @Autowired private TrainingSessionRepository sessions;
    @Autowired private TrainingWorkoutRepository workouts;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;

    private User trainer;
    private Area area;
    private Exercise exercise;
    private Course course;
    private TrainingGroup group;
    private int stallNumber = 800;

    @BeforeEach
    void buildFixture() {
        trainer = user(Roles.HEAD_TRAINER, "trainer");
        area = areas.findAll().stream().filter(a -> "A".equals(a.getCode())).findFirst().orElseThrow();

        exercise = new Exercise();
        exercise.setTrainerId(trainer.getId());
        exercise.setCategory(ExerciseCategory.SPEED);
        exercise.setName("Group IT Sprint " + System.nanoTime());
        exercise.setSurfaceType(SurfaceType.TURF);
        exercise.setTargetDistanceMeters(BigDecimal.valueOf(800));
        exercise.setIntensityLevel(IntensityLevel.HIGH);
        exercise.setDurationMinutes(60);
        exercise = exercises.saveAndFlush(exercise);

        course = new Course();
        course.setName("Group IT Course " + System.nanoTime());
        course.setTotalSessions(4);
        course.setCreatedById(trainer.getId());
        course.setTrainerId(trainer.getId());
        course = courses.saveAndFlush(course);

        group = group("Nhóm 1");

        // Deferred triggers (the group size) must fire inside the test, not only at a commit that never happens.
        jdbc.execute("SET CONSTRAINTS ALL IMMEDIATE");
    }

    // ----------------------------------------------------------------- members

    @Test
    void sixHorsesFitInAGroupAndTheSeventhIsRefused() {
        for (int i = 0; i < 6; i++) {
            addMember(group, horse(), user(Roles.GROOM, "groom"));
        }

        assertEquals(6, members.countByGroupIdAndLeftAtIsNull(group.getId()));
        assertThrows(DataIntegrityViolationException.class,
                () -> addMember(group, horse(), user(Roles.GROOM, "groom")));
    }

    @Test
    void twoHorsesOfTheSameGroomCanShareAGroup() {
        User groom = user(Roles.GROOM, "shared-groom");
        addMember(group, horse(), groom);

        assertDoesNotThrow(() -> addMember(group, horse(), groom));
    }

    @Test
    void theSameGroomCanLeadHorsesInDifferentGroups() {
        User groom = user(Roles.GROOM, "shared-groom");
        TrainingGroup other = group("Nhóm 2");
        addMember(group, horse(), groom);

        assertDoesNotThrow(() -> addMember(other, horse(), groom));
    }

    @Test
    void aHorseCannotBeInTwoGroups() {
        Horse horse = horse();
        TrainingGroup other = group("Nhóm 2");
        addMember(group, horse, user(Roles.GROOM, "groom"));

        assertThrows(DataIntegrityViolationException.class, () -> addMember(other, horse, user(Roles.GROOM, "groom")));
    }

    @Test
    void aTrainerCannotHaveTwoGroupsWithTheSameName() {
        assertThrows(DataIntegrityViolationException.class, () -> group("Nhóm 1"));
    }

    // ------------------------------------------------------------------- course runs

    @Test
    void unfinishedCourseRunsOfOneGroupCannotOverlapInDates() {
        LocalDate start = LocalDate.now().plusDays(10);
        courseRun(group, start, start.plus(java.time.Period.ofDays(12)), CourseRunStatus.UPCOMING);

        assertThrows(DataIntegrityViolationException.class,
                () -> courseRun(group, start.plusDays(5), start.plusDays(20), CourseRunStatus.UPCOMING));
    }

    @Test
    void aCourseRunQueuedAfterTheRunningOneIsAccepted() {
        LocalDate start = LocalDate.now().plusDays(10);
        courseRun(group, start, start.plusDays(12), CourseRunStatus.ACTIVE);

        assertDoesNotThrow(() -> courseRun(group, start.plusDays(13), start.plusDays(25), CourseRunStatus.UPCOMING));
    }

    @Test
    void aClosedCourseRunReleasesItsDates() {
        LocalDate start = LocalDate.now().plusDays(10);
        courseRun(group, start, start.plusDays(12), CourseRunStatus.CANCELLED);

        assertDoesNotThrow(() -> courseRun(group, start.plusDays(2), start.plusDays(9), CourseRunStatus.UPCOMING));
    }

    @Test
    void differentGroupsMayRunOnTheSameDays() {
        LocalDate start = LocalDate.now().plusDays(10);
        courseRun(group, start, start.plusDays(12), CourseRunStatus.UPCOMING);

        assertDoesNotThrow(() -> courseRun(group("Nhóm 2"), start, start.plusDays(12), CourseRunStatus.UPCOMING));
    }

    // ---------------------------------------------------------------- sessions

    @Test
    void sessionsOfOneTrainerCannotOverlapInTime() {
        CourseRun first = courseRun(group, LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), CourseRunStatus.UPCOMING);
        CourseRun second = courseRun(group("Nhóm 2"), LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), CourseRunStatus.UPCOMING);
        LocalDate day = LocalDate.now().plusDays(10);
        session(first, day, LocalTime.of(6, 0), LocalTime.of(7, 0), SessionStatus.SCHEDULED);

        assertThrows(DataIntegrityViolationException.class,
                () -> session(second, day, LocalTime.of(6, 30), LocalTime.of(7, 30), SessionStatus.SCHEDULED));
    }

    @Test
    void backToBackSessionsOfOneTrainerAreAccepted() {
        CourseRun first = courseRun(group, LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), CourseRunStatus.UPCOMING);
        CourseRun second = courseRun(group("Nhóm 2"), LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), CourseRunStatus.UPCOMING);
        LocalDate day = LocalDate.now().plusDays(10);
        session(first, day, LocalTime.of(6, 0), LocalTime.of(7, 0), SessionStatus.SCHEDULED);

        assertDoesNotThrow(() -> session(second, day, LocalTime.of(7, 0), LocalTime.of(8, 0), SessionStatus.SCHEDULED));
    }

    @Test
    void aCancelledSessionFreesItsSlot() {
        CourseRun first = courseRun(group, LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), CourseRunStatus.UPCOMING);
        CourseRun second = courseRun(group("Nhóm 2"), LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), CourseRunStatus.UPCOMING);
        LocalDate day = LocalDate.now().plusDays(10);
        session(first, day, LocalTime.of(6, 0), LocalTime.of(7, 0), SessionStatus.CANCELLED);

        assertDoesNotThrow(() -> session(second, day, LocalTime.of(6, 0), LocalTime.of(7, 0), SessionStatus.SCHEDULED));
    }

    @Test
    void sessionsMustStayInsideTheGoldenWindow() {
        CourseRun courseRun = courseRun(group, LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), CourseRunStatus.UPCOMING);
        LocalDate day = LocalDate.now().plusDays(10);

        assertThrows(DataIntegrityViolationException.class,
                () -> session(courseRun, day, LocalTime.of(5, 45), LocalTime.of(6, 45), SessionStatus.SCHEDULED));
    }

    @Test
    void sessionsMustEndByTenInTheMorning() {
        CourseRun courseRun = courseRun(group, LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), CourseRunStatus.UPCOMING);
        LocalDate day = LocalDate.now().plusDays(10);

        assertThrows(DataIntegrityViolationException.class,
                () -> session(courseRun, day, LocalTime.of(9, 30), LocalTime.of(10, 15), SessionStatus.SCHEDULED));
    }

    @Test
    void sessionsMustStartOnAQuarterHour() {
        CourseRun courseRun = courseRun(group, LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), CourseRunStatus.UPCOMING);
        LocalDate day = LocalDate.now().plusDays(10);

        assertThrows(DataIntegrityViolationException.class,
                () -> session(courseRun, day, LocalTime.of(6, 10), LocalTime.of(7, 10), SessionStatus.SCHEDULED));
    }

    // ---------------------------------------------------------------- workouts

    @Test
    void aWorkoutBelongsToExactlyOneModel() {
        CourseRun courseRun = courseRun(group, LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), CourseRunStatus.UPCOMING);
        TrainingSession session = session(courseRun, LocalDate.now().plusDays(10), LocalTime.of(6, 0), LocalTime.of(7, 0), SessionStatus.SCHEDULED);
        Horse horse = horse();

        TrainingWorkout neither = new TrainingWorkout();
        neither.setHorseId(horse.getId());
        neither.setStatus(WorkoutStatus.SCHEDULED);
        assertThrows(DataIntegrityViolationException.class, () -> workouts.saveAndFlush(neither));
    }

    @Test
    void aSessionWorkoutIsAccepted() {
        CourseRun courseRun = courseRun(group, LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), CourseRunStatus.UPCOMING);
        TrainingSession session = session(courseRun, LocalDate.now().plusDays(10), LocalTime.of(6, 0), LocalTime.of(7, 0), SessionStatus.SCHEDULED);

        assertDoesNotThrow(() -> workouts.saveAndFlush(sessionWorkout(session, horse())));
    }

    @Test
    void aHorseHasOneActiveWorkoutPerSession() {
        CourseRun courseRun = courseRun(group, LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), CourseRunStatus.UPCOMING);
        TrainingSession session = session(courseRun, LocalDate.now().plusDays(10), LocalTime.of(6, 0), LocalTime.of(7, 0), SessionStatus.SCHEDULED);
        Horse horse = horse();
        workouts.saveAndFlush(sessionWorkout(session, horse));

        assertThrows(DataIntegrityViolationException.class, () -> workouts.saveAndFlush(sessionWorkout(session, horse)));
    }

    @Test
    void aWorkoutThatIsNotRanHoldsNoResult() {
        CourseRun courseRun = courseRun(group, LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), CourseRunStatus.UPCOMING);
        TrainingSession session = session(courseRun, LocalDate.now().plusDays(10), LocalTime.of(6, 0), LocalTime.of(7, 0), SessionStatus.SCHEDULED);

        TrainingWorkout noResult = sessionWorkout(session, horse());
        noResult.setStatus(WorkoutStatus.HELD);
        assertDoesNotThrow(() -> workouts.saveAndFlush(noResult));

        TrainingWorkout partial = sessionWorkout(session, horse());
        partial.setStatus(WorkoutStatus.HELD);
        partial.setActualDistanceMeters(BigDecimal.valueOf(800));
        assertThrows(DataIntegrityViolationException.class, () -> workouts.saveAndFlush(partial));
    }

    @Test
    void aRanWorkoutMustHoldAResult() {
        CourseRun courseRun = courseRun(group, LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), CourseRunStatus.UPCOMING);
        TrainingSession session = session(courseRun, LocalDate.now().plusDays(10), LocalTime.of(6, 0), LocalTime.of(7, 0), SessionStatus.SCHEDULED);

        TrainingWorkout ranWithoutResult = sessionWorkout(session, horse());
        ranWithoutResult.setStatus(WorkoutStatus.RAN);
        assertThrows(DataIntegrityViolationException.class, () -> workouts.saveAndFlush(ranWithoutResult));
    }

    // ------------------------------------------------------------- authorities

    @Test
    void theNewAuthoritiesAreSeededForTheSameRolesAsTheOldOnes() {
        List<String> trainerAuthorities = jdbc.queryForList("""
                SELECT p.code FROM permissions p
                JOIN role_permissions rp ON rp.permission_id = p.id
                JOIN roles r ON r.id = rp.role_id
                WHERE r.name = 'HEAD_TRAINER' AND p.code IN
                  ('TRAINING_GROUP_VIEW','TRAINING_GROUP_MANAGE','COURSE_RUN_MANAGE','SESSION_VIEW','SESSION_UPDATE')
                """, String.class);
        List<String> groomAuthorities = jdbc.queryForList("""
                SELECT p.code FROM permissions p
                JOIN role_permissions rp ON rp.permission_id = p.id
                JOIN roles r ON r.id = rp.role_id
                WHERE r.name = 'GROOM' AND p.code IN ('TRAINING_GROUP_VIEW','TRAINING_GROUP_MANAGE','COURSE_RUN_VIEW',
                  'COURSE_RUN_LIST','COURSE_RUN_MANAGE','SESSION_VIEW','SESSION_UPDATE')
                """, String.class);

        assertEquals(5, trainerAuthorities.size());
        assertEquals(List.of("SESSION_VIEW"), groomAuthorities);
    }

    // ------------------------------------------------------------------ helpers

    private User user(String role, String label) {
        User user = new User();
        user.setFullName("GIT " + label);
        user.setEmail("git-" + label + "-" + System.nanoTime() + "@test.invalid");
        user.setPasswordHash("not-used");
        user.setRole(roles.findByName(role).orElseThrow());
        user.setActive(true);
        return users.saveAndFlush(user);
    }

    private Horse horse() {
        User owner = user(Roles.HORSE_OWNER, "owner");
        StableStall stall = new StableStall();
        stall.setAreaId(area.getId());
        stall.setStallNumber(stallNumber);
        stall.setStallCode("GIT-" + stallNumber + "-" + System.nanoTime());
        stallNumber++;
        stall = stalls.saveAndFlush(stall);
        Horse horse = new Horse();
        horse.setName("GIT Horse " + System.nanoTime());
        horse.setOwnerId(owner.getId());
        horse.setCurrentStallId(stall.getId());
        return horses.saveAndFlush(horse);
    }

    private TrainingGroup group(String name) {
        TrainingGroup trainingGroup = new TrainingGroup();
        trainingGroup.setTrainerId(trainer.getId());
        trainingGroup.setName(name);
        return groups.saveAndFlush(trainingGroup);
    }

    private TrainingGroupMember addMember(TrainingGroup target, Horse horse, User groom) {
        TrainingGroupMember member = new TrainingGroupMember();
        member.setGroupId(target.getId());
        member.setHorseId(horse.getId());
        member.setGroomId(groom.getId());
        return members.saveAndFlush(member);
    }

    private CourseRun courseRun(TrainingGroup target, LocalDate start, LocalDate end, CourseRunStatus status) {
        CourseRun courseRun = new CourseRun();
        courseRun.setGroupId(target.getId());
        courseRun.setCourseId(course.getId());
        courseRun.setTrainerId(trainer.getId());
        courseRun.setStartDate(start);
        courseRun.setEndDate(end);
        Set<TrainingDay> days = new LinkedHashSet<>(List.of(TrainingDay.MONDAY, TrainingDay.WEDNESDAY));
        courseRun.setTrainingDays(days);
        courseRun.setStatus(status);
        return courseRuns.saveAndFlush(courseRun);
    }

    private TrainingSession session(CourseRun courseRun, LocalDate day, LocalTime start, LocalTime end, SessionStatus status) {
        TrainingSession session = new TrainingSession();
        session.setCourseRunId(courseRun.getId());
        session.setExerciseId(exercise.getId());
        session.setTrainerId(trainer.getId());
        session.setSessionDate(day);
        session.setStartTime(start);
        session.setEndTime(end);
        session.setStatus(status);
        return sessions.saveAndFlush(session);
    }

    private TrainingWorkout sessionWorkout(TrainingSession session, Horse horse) {
        TrainingWorkout workout = new TrainingWorkout();
        workout.setSessionId(session.getId());
        workout.setHorseId(horse.getId());
        workout.setStatus(WorkoutStatus.SCHEDULED);
        return workout;
    }
}
