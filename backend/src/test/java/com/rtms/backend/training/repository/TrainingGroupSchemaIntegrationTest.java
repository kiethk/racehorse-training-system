package com.rtms.backend.training.repository;

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
import com.rtms.backend.training.entity.GroupPlan;
import com.rtms.backend.training.entity.Subject;
import com.rtms.backend.training.entity.TrainingGroup;
import com.rtms.backend.training.entity.TrainingGroupMember;
import com.rtms.backend.training.entity.TrainingSession;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.IntensityLevel;
import com.rtms.backend.training.enums.SessionStatus;
import com.rtms.backend.training.enums.SubjectCategory;
import com.rtms.backend.training.enums.SurfaceType;
import com.rtms.backend.training.enums.TrainingDay;
import com.rtms.backend.training.enums.TrainingPlanStatus;
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
    @Autowired private SubjectRepository subjects;
    @Autowired private CourseRepository courses;
    @Autowired private TrainingGroupRepository groups;
    @Autowired private TrainingGroupMemberRepository members;
    @Autowired private GroupPlanRepository plans;
    @Autowired private TrainingSessionRepository sessions;
    @Autowired private TrainingWorkoutRepository workouts;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;

    private User trainer;
    private Area area;
    private Subject subject;
    private Course course;
    private TrainingGroup group;
    private int stallNumber = 800;

    @BeforeEach
    void buildFixture() {
        trainer = user("HEAD_TRAINER", "trainer");
        area = areas.findAll().stream().filter(a -> "A".equals(a.getCode())).findFirst().orElseThrow();

        subject = new Subject();
        subject.setTrainerId(trainer.getId());
        subject.setCategory(SubjectCategory.SPEED);
        subject.setName("Group IT Sprint " + System.nanoTime());
        subject.setSurfaceType(SurfaceType.TURF);
        subject.setTargetDistanceMeters(BigDecimal.valueOf(800));
        subject.setIntensityLevel(IntensityLevel.HIGH);
        subject.setDurationMinutes(60);
        subject = subjects.saveAndFlush(subject);

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
            addMember(group, horse(), user("GROOM", "groom"));
        }

        assertEquals(6, members.countByGroupIdAndLeftAtIsNull(group.getId()));
        assertThrows(DataIntegrityViolationException.class,
                () -> addMember(group, horse(), user("GROOM", "groom")));
    }

    @Test
    void twoHorsesOfTheSameGroomCanShareAGroup() {
        User groom = user("GROOM", "shared-groom");
        addMember(group, horse(), groom);

        assertDoesNotThrow(() -> addMember(group, horse(), groom));
    }

    @Test
    void theSameGroomCanLeadHorsesInDifferentGroups() {
        User groom = user("GROOM", "shared-groom");
        TrainingGroup other = group("Nhóm 2");
        addMember(group, horse(), groom);

        assertDoesNotThrow(() -> addMember(other, horse(), groom));
    }

    @Test
    void aHorseCannotBeInTwoGroups() {
        Horse horse = horse();
        TrainingGroup other = group("Nhóm 2");
        addMember(group, horse, user("GROOM", "groom"));

        assertThrows(DataIntegrityViolationException.class, () -> addMember(other, horse, user("GROOM", "groom")));
    }

    @Test
    void aTrainerCannotHaveTwoGroupsWithTheSameName() {
        assertThrows(DataIntegrityViolationException.class, () -> group("Nhóm 1"));
    }

    // ------------------------------------------------------------------- plans

    @Test
    void unfinishedPlansOfOneGroupCannotOverlapInDates() {
        LocalDate start = LocalDate.now().plusDays(10);
        plan(group, start, start.plus(java.time.Period.ofDays(12)), TrainingPlanStatus.UPCOMING);

        assertThrows(DataIntegrityViolationException.class,
                () -> plan(group, start.plusDays(5), start.plusDays(20), TrainingPlanStatus.UPCOMING));
    }

    @Test
    void aPlanQueuedAfterTheRunningOneIsAccepted() {
        LocalDate start = LocalDate.now().plusDays(10);
        plan(group, start, start.plusDays(12), TrainingPlanStatus.ACTIVE);

        assertDoesNotThrow(() -> plan(group, start.plusDays(13), start.plusDays(25), TrainingPlanStatus.UPCOMING));
    }

    @Test
    void aClosedPlanReleasesItsDates() {
        LocalDate start = LocalDate.now().plusDays(10);
        plan(group, start, start.plusDays(12), TrainingPlanStatus.CANCELLED);

        assertDoesNotThrow(() -> plan(group, start.plusDays(2), start.plusDays(9), TrainingPlanStatus.UPCOMING));
    }

    @Test
    void differentGroupsMayRunOnTheSameDays() {
        LocalDate start = LocalDate.now().plusDays(10);
        plan(group, start, start.plusDays(12), TrainingPlanStatus.UPCOMING);

        assertDoesNotThrow(() -> plan(group("Nhóm 2"), start, start.plusDays(12), TrainingPlanStatus.UPCOMING));
    }

    // ---------------------------------------------------------------- sessions

    @Test
    void sessionsOfOneTrainerCannotOverlapInTime() {
        GroupPlan first = plan(group, LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), TrainingPlanStatus.UPCOMING);
        GroupPlan second = plan(group("Nhóm 2"), LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), TrainingPlanStatus.UPCOMING);
        LocalDate day = LocalDate.now().plusDays(10);
        session(first, day, LocalTime.of(6, 0), LocalTime.of(7, 0), SessionStatus.SCHEDULED);

        assertThrows(DataIntegrityViolationException.class,
                () -> session(second, day, LocalTime.of(6, 30), LocalTime.of(7, 30), SessionStatus.SCHEDULED));
    }

    @Test
    void backToBackSessionsOfOneTrainerAreAccepted() {
        GroupPlan first = plan(group, LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), TrainingPlanStatus.UPCOMING);
        GroupPlan second = plan(group("Nhóm 2"), LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), TrainingPlanStatus.UPCOMING);
        LocalDate day = LocalDate.now().plusDays(10);
        session(first, day, LocalTime.of(6, 0), LocalTime.of(7, 0), SessionStatus.SCHEDULED);

        assertDoesNotThrow(() -> session(second, day, LocalTime.of(7, 0), LocalTime.of(8, 0), SessionStatus.SCHEDULED));
    }

    @Test
    void aCancelledSessionFreesItsSlot() {
        GroupPlan first = plan(group, LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), TrainingPlanStatus.UPCOMING);
        GroupPlan second = plan(group("Nhóm 2"), LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), TrainingPlanStatus.UPCOMING);
        LocalDate day = LocalDate.now().plusDays(10);
        session(first, day, LocalTime.of(6, 0), LocalTime.of(7, 0), SessionStatus.CANCELLED);

        assertDoesNotThrow(() -> session(second, day, LocalTime.of(6, 0), LocalTime.of(7, 0), SessionStatus.SCHEDULED));
    }

    @Test
    void sessionsMustStayInsideTheGoldenWindow() {
        GroupPlan plan = plan(group, LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), TrainingPlanStatus.UPCOMING);
        LocalDate day = LocalDate.now().plusDays(10);

        assertThrows(DataIntegrityViolationException.class,
                () -> session(plan, day, LocalTime.of(5, 45), LocalTime.of(6, 45), SessionStatus.SCHEDULED));
    }

    @Test
    void sessionsMustEndByTenInTheMorning() {
        GroupPlan plan = plan(group, LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), TrainingPlanStatus.UPCOMING);
        LocalDate day = LocalDate.now().plusDays(10);

        assertThrows(DataIntegrityViolationException.class,
                () -> session(plan, day, LocalTime.of(9, 30), LocalTime.of(10, 15), SessionStatus.SCHEDULED));
    }

    @Test
    void sessionsMustStartOnAQuarterHour() {
        GroupPlan plan = plan(group, LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), TrainingPlanStatus.UPCOMING);
        LocalDate day = LocalDate.now().plusDays(10);

        assertThrows(DataIntegrityViolationException.class,
                () -> session(plan, day, LocalTime.of(6, 10), LocalTime.of(7, 10), SessionStatus.SCHEDULED));
    }

    // ---------------------------------------------------------------- workouts

    @Test
    void aWorkoutBelongsToExactlyOneModel() {
        GroupPlan plan = plan(group, LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), TrainingPlanStatus.UPCOMING);
        TrainingSession session = session(plan, LocalDate.now().plusDays(10), LocalTime.of(6, 0), LocalTime.of(7, 0), SessionStatus.SCHEDULED);
        Horse horse = horse();

        TrainingWorkout neither = new TrainingWorkout();
        neither.setHorseId(horse.getId());
        neither.setStatus(WorkoutStatus.SCHEDULED);
        assertThrows(DataIntegrityViolationException.class, () -> workouts.saveAndFlush(neither));
    }

    @Test
    void aSessionWorkoutIsAccepted() {
        GroupPlan plan = plan(group, LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), TrainingPlanStatus.UPCOMING);
        TrainingSession session = session(plan, LocalDate.now().plusDays(10), LocalTime.of(6, 0), LocalTime.of(7, 0), SessionStatus.SCHEDULED);

        assertDoesNotThrow(() -> workouts.saveAndFlush(sessionWorkout(session, horse())));
    }

    @Test
    void aHorseHasOneActiveWorkoutPerSession() {
        GroupPlan plan = plan(group, LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), TrainingPlanStatus.UPCOMING);
        TrainingSession session = session(plan, LocalDate.now().plusDays(10), LocalTime.of(6, 0), LocalTime.of(7, 0), SessionStatus.SCHEDULED);
        Horse horse = horse();
        workouts.saveAndFlush(sessionWorkout(session, horse));

        assertThrows(DataIntegrityViolationException.class, () -> workouts.saveAndFlush(sessionWorkout(session, horse)));
    }

    @Test
    void aWorkoutThatIsNotRanHoldsNoResult() {
        GroupPlan plan = plan(group, LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), TrainingPlanStatus.UPCOMING);
        TrainingSession session = session(plan, LocalDate.now().plusDays(10), LocalTime.of(6, 0), LocalTime.of(7, 0), SessionStatus.SCHEDULED);

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
        GroupPlan plan = plan(group, LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), TrainingPlanStatus.UPCOMING);
        TrainingSession session = session(plan, LocalDate.now().plusDays(10), LocalTime.of(6, 0), LocalTime.of(7, 0), SessionStatus.SCHEDULED);

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
                  ('TRAINING_GROUP_VIEW','TRAINING_GROUP_MANAGE','GROUP_PLAN_CREATE','SESSION_VIEW','SESSION_UPDATE')
                """, String.class);
        List<String> groomAuthorities = jdbc.queryForList("""
                SELECT p.code FROM permissions p
                JOIN role_permissions rp ON rp.permission_id = p.id
                JOIN roles r ON r.id = rp.role_id
                WHERE r.name = 'GROOM' AND p.code IN ('TRAINING_GROUP_VIEW','TRAINING_GROUP_MANAGE','GROUP_PLAN_VIEW',
                  'GROUP_PLAN_LIST','GROUP_PLAN_CREATE','SESSION_VIEW','SESSION_UPDATE')
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
        User owner = user("HORSE_OWNER", "owner");
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

    private GroupPlan plan(TrainingGroup target, LocalDate start, LocalDate end, TrainingPlanStatus status) {
        GroupPlan plan = new GroupPlan();
        plan.setGroupId(target.getId());
        plan.setCourseId(course.getId());
        plan.setTrainerId(trainer.getId());
        plan.setStartDate(start);
        plan.setEndDate(end);
        Set<TrainingDay> days = new LinkedHashSet<>(List.of(TrainingDay.MONDAY, TrainingDay.WEDNESDAY));
        plan.setTrainingDays(days);
        plan.setStatus(status);
        return plans.saveAndFlush(plan);
    }

    private TrainingSession session(GroupPlan plan, LocalDate day, LocalTime start, LocalTime end, SessionStatus status) {
        TrainingSession session = new TrainingSession();
        session.setGroupPlanId(plan.getId());
        session.setSubjectId(subject.getId());
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
