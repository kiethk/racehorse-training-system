package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.RoleRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.dto.CreateTrainingGroupRequest;
import com.rtms.backend.training.dto.GroupPlanDetailResponse;
import com.rtms.backend.training.dto.GroupPlanPreviewResponse;
import com.rtms.backend.training.dto.GroupPlanRequest;
import com.rtms.backend.training.dto.GroupPlanSummaryResponse;
import com.rtms.backend.training.dto.PlanSessionResponse;
import com.rtms.backend.training.dto.TrainingGroupResponse;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.CourseSubject;
import com.rtms.backend.training.entity.Subject;
import com.rtms.backend.training.enums.IntensityLevel;
import com.rtms.backend.training.enums.SubjectCategory;
import com.rtms.backend.training.enums.SurfaceType;
import com.rtms.backend.training.enums.TrainingDay;
import com.rtms.backend.training.repository.CourseRepository;
import com.rtms.backend.training.repository.CourseSubjectRepository;
import com.rtms.backend.training.repository.GroupPlanRepository;
import com.rtms.backend.training.repository.SubjectRepository;
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
 * (Assigning a course to a group through the service on a real PostgreSQL: start date, Subject rotation, the time of each Session
 * (first-fit per day), date overlap, full days and every member's Workouts. Every date is two weeks away so the result does not
 * depend on when the test runs. All data lives in one transaction that rolls back.)
 */
@SpringBootTest
@Transactional
class GroupPlanServiceIntegrationTest {

    private static final List<TrainingDay> MON_WED_FRI =
            List.of(TrainingDay.MONDAY, TrainingDay.WEDNESDAY, TrainingDay.FRIDAY);

    @Autowired private GroupPlanService service;
    @Autowired private TrainingGroupService groupService;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private AreaRepository areas;
    @Autowired private StableStallRepository stalls;
    @Autowired private HorseRepository horses;
    @Autowired private SubjectRepository subjects;
    @Autowired private CourseRepository courses;
    @Autowired private CourseSubjectRepository courseSubjects;
    @Autowired private GroupPlanRepository plans;
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
        trainer = user("HEAD_TRAINER", "trainer");
        trainerPrincipal = new AuthenticatedUser(trainer.getId(), trainer.getEmail(), "HEAD_TRAINER");
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
    void aGroupPlanGetsRotatingSubjectsFirstFitTimesAndWorkoutsForEveryMember() {
        TrainingGroupResponse group = groupOf("Nhom 1", 3);
        Course course = course("Sprint Base", 6, subject("S1", 30), subject("S2", 45), subject("S3", 60));

        // Tuesday is not a training day, so the plan starts on Wednesday.
        GroupPlanDetailResponse plan = service.createPlan(
                new GroupPlanRequest(group.id(), course.getId(), monday.plusDays(1), MON_WED_FRI), trainerPrincipal);

        assertEquals(monday.plusDays(2), plan.plan().startDate());
        assertEquals(monday.plusDays(2).plusDays(12), plan.plan().endDate());
        assertEquals("UPCOMING", plan.plan().status());
        assertEquals(List.of("S1", "S2", "S3", "S1", "S2", "S3"),
                plan.sessions().stream().map(PlanSessionResponse::subjectName).toList());
        assertTrue(plan.sessions().stream().allMatch(session -> session.startTime().equals(LocalTime.of(6, 0))));
        assertTrue(plan.sessions().stream().allMatch(session -> session.horseCount() == 3));
        assertEquals(6, plan.plan().sessionsTotal());
        assertEquals(18, workouts.count() - existingWorkouts);
    }

    @Test
    void previewShowsTheSameTimesAsCreateAndWritesNothing() {
        TrainingGroupResponse group = groupOf("Nhom 1", 2);
        Course course = course("Sprint Base", 3, subject("S1", 30), subject("S2", 45));
        GroupPlanRequest request = new GroupPlanRequest(group.id(), course.getId(), monday, MON_WED_FRI);
        long plansBefore = plans.count();

        GroupPlanPreviewResponse preview = service.previewPlan(request, trainerPrincipal);

        assertEquals(plansBefore, plans.count());
        assertTrue(preview.canCreate());
        assertEquals(monday, preview.startDate());
        assertEquals(monday.plusDays(4), preview.endDate());
        assertEquals(2, preview.memberCount());

        GroupPlanDetailResponse created = service.createPlan(request, trainerPrincipal);
        assertEquals(preview.sessions().stream().map(session -> session.startTime()).toList(),
                created.sessions().stream().map(PlanSessionResponse::startTime).toList());
    }

    @Test
    void aSecondGroupIsPlacedAfterTheFirstOneOnTheSameDay() {
        TrainingGroupResponse first = groupOf("Nhom 1", 2);
        TrainingGroupResponse second = groupOf("Nhom 2", 2);
        Course sprint = course("Sprint", 2, subject("S1", 30));
        Course stamina = course("Stamina", 2, subject("T1", 90));

        service.createPlan(new GroupPlanRequest(first.id(), sprint.getId(), monday, MON_WED_FRI), trainerPrincipal);
        GroupPlanDetailResponse plan = service.createPlan(
                new GroupPlanRequest(second.id(), stamina.getId(), monday, MON_WED_FRI), trainerPrincipal);

        assertEquals(LocalTime.of(6, 30), plan.sessions().get(0).startTime());
        assertEquals(LocalTime.of(8, 0), plan.sessions().get(0).endTime());
    }

    @Test
    void aLongSubjectOnlyShiftsTheDayItBelongsTo() {
        TrainingGroupResponse first = groupOf("Nhom 1", 2);
        TrainingGroupResponse second = groupOf("Nhom 2", 2);
        Course mixed = course("Mixed", 2, subject("Long", 120), subject("Short", 30));
        Course steady = course("Steady", 2, subject("Hour", 60));

        service.createPlan(new GroupPlanRequest(first.id(), mixed.getId(), monday, MON_WED_FRI), trainerPrincipal);
        GroupPlanDetailResponse plan = service.createPlan(
                new GroupPlanRequest(second.id(), steady.getId(), monday, MON_WED_FRI), trainerPrincipal);

        // Day 1 follows the 120-minute Subject, day 2 only the 30-minute one: the times differ by day.
        assertEquals(LocalTime.of(8, 0), plan.sessions().get(0).startTime());
        assertEquals(LocalTime.of(6, 30), plan.sessions().get(1).startTime());
    }

    // ------------------------------------------------------------- overlap, full

    @Test
    void unfinishedPlansOfOneGroupCannotOverlapButTheNextCourseCanBeQueued() {
        TrainingGroupResponse group = groupOf("Nhom 1", 2);
        Course course = course("Sprint", 6, subject("S1", 30));
        GroupPlanDetailResponse first = service.createPlan(
                new GroupPlanRequest(group.id(), course.getId(), monday, MON_WED_FRI), trainerPrincipal);

        ApiException overlap = assertThrows(ApiException.class, () -> service.createPlan(
                new GroupPlanRequest(group.id(), course.getId(), first.plan().endDate().minusDays(2), MON_WED_FRI), trainerPrincipal));
        assertEquals("PLAN_OVERLAP", overlap.getErrorCode());

        GroupPlanDetailResponse queued = service.createPlan(
                new GroupPlanRequest(group.id(), course.getId(), first.plan().endDate().plusDays(1), MON_WED_FRI), trainerPrincipal);
        assertEquals("UPCOMING", queued.plan().status());
    }

    @Test
    void previewNamesTheOverlappingPlanAndTheEarliestFreeStart() {
        TrainingGroupResponse group = groupOf("Nhom 1", 2);
        Course course = course("Sprint", 6, subject("S1", 30));
        GroupPlanDetailResponse first = service.createPlan(
                new GroupPlanRequest(group.id(), course.getId(), monday, MON_WED_FRI), trainerPrincipal);

        GroupPlanPreviewResponse preview = service.previewPlan(
                new GroupPlanRequest(group.id(), course.getId(), monday.plusDays(2), MON_WED_FRI), trainerPrincipal);

        assertFalse(preview.canCreate());
        assertEquals(1, preview.overlaps().size());
        assertEquals("Sprint", preview.overlaps().get(0).courseName());
        assertTrue(preview.earliestFreeStart().isAfter(first.plan().endDate()));
    }

    @Test
    void everyFullDayIsReportedTogetherAndNothingIsSaved() {
        TrainingGroupResponse blocker = groupOf("Nhom 1", 1);
        TrainingGroupResponse group = groupOf("Nhom 2", 1);
        Course allMorning = course("All morning", 3, subject("Marathon", 240));
        Course hour = course("Hour", 3, subject("Hour", 60));
        service.createPlan(new GroupPlanRequest(blocker.id(), allMorning.getId(), monday, MON_WED_FRI), trainerPrincipal);
        long plansBefore = plans.count();
        GroupPlanRequest request = new GroupPlanRequest(group.id(), hour.getId(), monday, MON_WED_FRI);

        GroupPlanPreviewResponse preview = service.previewPlan(request, trainerPrincipal);
        ApiException full = assertThrows(ApiException.class, () -> service.createPlan(request, trainerPrincipal));

        assertFalse(preview.canCreate());
        assertEquals(List.of(monday, monday.plusDays(2), monday.plusDays(4)), preview.fullDates());
        assertEquals("SCHEDULE_FULL", full.getErrorCode());
        assertEquals(plansBefore, plans.count());
    }

    // -------------------------------------------------------------- validation

    @Test
    void aGroupWithoutHorsesIsRefused() {
        TrainingGroupResponse group = groupOf("Nhom 1", 0);
        Course course = course("Sprint", 2, subject("S1", 30));

        ApiException error = assertThrows(ApiException.class, () -> service.createPlan(
                new GroupPlanRequest(group.id(), course.getId(), monday, MON_WED_FRI), trainerPrincipal));
        assertEquals("GROUP_EMPTY", error.getErrorCode());
    }

    @Test
    void aStartDateInThePastIsRefused() {
        TrainingGroupResponse group = groupOf("Nhom 1", 1);
        Course course = course("Sprint", 2, subject("S1", 30));

        assertThrows(IllegalArgumentException.class, () -> service.createPlan(
                new GroupPlanRequest(group.id(), course.getId(), LocalDate.now().minusDays(1), MON_WED_FRI), trainerPrincipal));
    }

    @Test
    void aCourseWithoutSubjectsIsRefused() {
        TrainingGroupResponse group = groupOf("Nhom 1", 1);
        Course empty = course("Empty", 2);

        ApiException error = assertThrows(ApiException.class, () -> service.createPlan(
                new GroupPlanRequest(group.id(), empty.getId(), monday, MON_WED_FRI), trainerPrincipal));
        assertEquals("COURSE_HAS_NO_SUBJECTS", error.getErrorCode());
    }

    @Test
    void anotherTrainersGroupIsNotFound() {
        TrainingGroupResponse group = groupOf("Nhom 1", 1);
        Course course = course("Sprint", 2, subject("S1", 30));
        User other = user("HEAD_TRAINER", "other");
        AuthenticatedUser otherPrincipal = new AuthenticatedUser(other.getId(), other.getEmail(), "HEAD_TRAINER");

        ApiException error = assertThrows(ApiException.class, () -> service.createPlan(
                new GroupPlanRequest(group.id(), course.getId(), monday, MON_WED_FRI), otherPrincipal));
        assertEquals("GROUP_NOT_FOUND", error.getErrorCode());
    }

    // ----------------------------------------------------------------- reading

    @Test
    void theListShowsTheOwnPlansAndTheDetailIsPrivateToItsTrainer() {
        TrainingGroupResponse group = groupOf("Nhom 1", 1);
        Course course = course("Sprint", 3, subject("S1", 30));
        GroupPlanDetailResponse created = service.createPlan(
                new GroupPlanRequest(group.id(), course.getId(), monday, MON_WED_FRI), trainerPrincipal);
        User other = user("HEAD_TRAINER", "other");
        AuthenticatedUser otherPrincipal = new AuthenticatedUser(other.getId(), other.getEmail(), "HEAD_TRAINER");

        List<GroupPlanSummaryResponse> own = service.getPlans(group.id(), null, trainerPrincipal);

        assertEquals(1, own.size());
        assertEquals("Nhom 1", own.get(0).groupName());
        assertEquals("Sprint", own.get(0).courseName());
        assertEquals(3, own.get(0).sessionsTotal());
        assertTrue(service.getPlans(null, null, otherPrincipal).isEmpty());
        ApiException error = assertThrows(ApiException.class, () -> service.getPlan(created.plan().id(), otherPrincipal));
        assertEquals("GROUP_PLAN_NOT_FOUND", error.getErrorCode());
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
            horseIds.add(horse("H" + System.nanoTime(), user("GROOM", "groom")).getId());
        }
        return groupService.createGroup(new CreateTrainingGroupRequest(name, horseIds), trainerPrincipal);
    }

    private Subject subject(String name, int minutes) {
        Subject subject = new Subject();
        subject.setTrainerId(trainer.getId());
        subject.setCategory(SubjectCategory.SPEED);
        subject.setName(name);
        subject.setSurfaceType(SurfaceType.TURF);
        subject.setTargetDistanceMeters(BigDecimal.valueOf(800));
        subject.setIntensityLevel(IntensityLevel.HIGH);
        subject.setDurationMinutes(minutes);
        return subjects.saveAndFlush(subject);
    }

    private Course course(String name, int totalSessions, Subject... orderedSubjects) {
        Course course = new Course();
        course.setName(name);
        course.setTotalSessions(totalSessions);
        course.setCreatedById(trainer.getId());
        course.setTrainerId(trainer.getId());
        course = courses.saveAndFlush(course);
        int order = 1;
        for (Subject subject : orderedSubjects) {
            CourseSubject link = new CourseSubject();
            link.setCourseId(course.getId());
            link.setSubjectId(subject.getId());
            link.setOrderIndex(order++);
            courseSubjects.saveAndFlush(link);
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
        User owner = user("HORSE_OWNER", "owner");
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
