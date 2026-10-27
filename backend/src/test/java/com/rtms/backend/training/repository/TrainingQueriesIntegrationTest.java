package com.rtms.backend.training.repository;

import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.RoleRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.dto.CourseSubjectResponse;
import com.rtms.backend.training.dto.DashboardHorseRow;
import com.rtms.backend.training.dto.DashboardPlanRow;
import com.rtms.backend.training.dto.GroomConflictRow;
import com.rtms.backend.training.dto.GroomWorkoutRow;
import com.rtms.backend.training.dto.HorseFitnessTrendItemResponse;
import com.rtms.backend.training.dto.HorseStallInfo;
import com.rtms.backend.training.dto.JoinableCohortRow;
import com.rtms.backend.training.dto.LotHorseNameRow;
import com.rtms.backend.training.dto.PerformanceSessionRow;
import com.rtms.backend.training.dto.PlanNamesRow;
import com.rtms.backend.training.dto.PlanSummaryResponse;
import com.rtms.backend.training.dto.PlanWorkoutItemResponse;
import com.rtms.backend.training.dto.RecentWorkoutRow;
import com.rtms.backend.training.dto.RunningPlanRow;
import com.rtms.backend.training.dto.SpecialtyWorkoutRow;
import com.rtms.backend.training.dto.TrainingLotRow;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.CourseSubject;
import com.rtms.backend.training.entity.HorseTrainingPlan;
import com.rtms.backend.training.entity.Subject;
import com.rtms.backend.training.entity.TrainingLot;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.IntensityLevel;
import com.rtms.backend.training.enums.LotStatus;
import com.rtms.backend.training.enums.SubjectCategory;
import com.rtms.backend.training.enums.SurfaceType;
import com.rtms.backend.training.enums.TrainingDay;
import com.rtms.backend.training.enums.TrainingPlanStatus;
import com.rtms.backend.training.enums.WorkoutStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Chạy các câu JPQL JOIN/projection MỚI trên PostgreSQL thật. Test Mockito không bắt được lỗi ở đây (constructor
 * expression sai kiểu, GROUP BY thiếu cột, tham số null...), nên phải chạy thật.
 * (Runs the NEW JPQL JOIN/projection queries against a real PostgreSQL. Mockito tests cannot catch problems
 * here (a mistyped constructor expression, a missing GROUP BY column, null parameters...), so they must run
 * for real.)
 *
 * Mọi dữ liệu được tạo trong một transaction rồi rollback, nên không để lại gì trong DB.
 * (All data is created inside one transaction and rolled back, so nothing is left in the DB.)
 */
@SpringBootTest
@Transactional
class TrainingQueriesIntegrationTest {

    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private AreaRepository areas;
    @Autowired private StableStallRepository stalls;
    @Autowired private HorseRepository horses;
    @Autowired private SubjectRepository subjects;
    @Autowired private CourseRepository courses;
    @Autowired private CourseSubjectRepository courseSubjects;
    @Autowired private HorseTrainingPlanRepository plans;
    @Autowired private TrainingLotRepository lots;
    @Autowired private TrainingWorkoutRepository workouts;

    private final LocalDate today = LocalDate.now();

    private User trainer;
    private User groom;
    private User owner;
    private User otherOwner;
    private Horse horse1;      // owned by `owner`, in an area managed by `trainer`
    private Horse horse2;      // owned by `otherOwner`, in the same area
    private Subject subject;
    private Course course;
    private HorseTrainingPlan plan;
    private TrainingLot todayLot;
    private TrainingWorkout workout1;   // horse1, SCHEDULED, today
    private TrainingWorkout workout2;   // horse2, SCHEDULED, today, same lot
    private TrainingWorkout done;       // horse1, COMPLETED, yesterday

    @BeforeEach
    void buildFixture() {
        // Own users instead of seed accounts: the local demo seed renames trainer@/vet@/groom@rtms.com,
        // and the test transaction rolls these back anyway.
        trainer = user("HEAD_TRAINER", "trainer");
        groom = user("GROOM", "groom");
        owner = user("HORSE_OWNER", "owner");
        otherOwner = user("HORSE_OWNER", "other-owner");

        Area area = areas.findAll().stream().filter(a -> "A".equals(a.getCode())).findFirst().orElseThrow();
        area.setTrainerId(trainer.getId());
        areas.saveAndFlush(area);

        StableStall stall1 = stall(area, 901, "IT-901");
        StableStall stall2 = stall(area, 902, "IT-902");
        horse1 = horse("IT Horse One", owner, stall1);
        horse2 = horse("IT Horse Two", otherOwner, stall2);

        subject = new Subject();
        subject.setTrainerId(trainer.getId());
        subject.setCategory(SubjectCategory.SPEED);
        subject.setName("IT Sprint");
        subject.setSurfaceType(SurfaceType.TURF);
        subject.setTargetDistanceMeters(BigDecimal.valueOf(800));
        subject.setIntensityLevel(IntensityLevel.HIGH);
        subject.setDurationMinutes(60);
        subject = subjects.saveAndFlush(subject);

        course = new Course();
        course.setName("IT Course " + System.nanoTime());
        course.setTotalSessions(4);
        course.setCreatedById(trainer.getId());
        course.setTrainerId(trainer.getId());
        course = courses.saveAndFlush(course);

        CourseSubject link = new CourseSubject();
        link.setCourseId(course.getId());
        link.setSubjectId(subject.getId());
        link.setOrderIndex(1);
        courseSubjects.saveAndFlush(link);

        plan = new HorseTrainingPlan();
        plan.setHorseId(horse1.getId());
        plan.setCourseId(course.getId());
        plan.setTrainerId(trainer.getId());
        plan.setStartDate(today.minusDays(2));
        plan.setEndDate(today.plusDays(10));
        plan.setTrainingDays(new LinkedHashSet<>(List.of(TrainingDay.MONDAY, TrainingDay.WEDNESDAY)));
        plan.setGroomId(groom.getId());
        plan.setStatus(TrainingPlanStatus.ACTIVE);
        plan = plans.saveAndFlush(plan);

        todayLot = lot(today, LocalTime.of(6, 0), LocalTime.of(7, 0));
        TrainingLot yesterdayLot = lot(today.minusDays(1), LocalTime.of(6, 0), LocalTime.of(7, 0));

        workout1 = workout(todayLot, horse1, WorkoutStatus.SCHEDULED);
        workout2 = workout(todayLot, horse2, WorkoutStatus.SCHEDULED);
        done = workout(yesterdayLot, horse1, WorkoutStatus.COMPLETED);
        done.setMaxHeartRate(230);
        done.setPerformanceRating(8);
        done.setActualDistanceMeters(BigDecimal.valueOf(800));
        workouts.saveAndFlush(done);
    }

    private User user(String role, String label) {
        User user = new User();
        user.setFullName("IT " + label);
        user.setEmail("it-" + label + "-" + System.nanoTime() + "@test.invalid");
        user.setPasswordHash("not-used");
        user.setRole(roles.findByName(role).orElseThrow());
        user.setActive(true);
        return users.saveAndFlush(user);
    }

    private StableStall stall(Area area, int number, String code) {
        StableStall stall = new StableStall();
        stall.setAreaId(area.getId());
        stall.setGroomId(groom.getId());
        stall.setStallNumber(number);
        stall.setStallCode(code);
        return stalls.saveAndFlush(stall);
    }

    private Horse horse(String name, User horseOwner, StableStall stall) {
        Horse horse = new Horse();
        horse.setName(name);
        horse.setOwnerId(horseOwner.getId());
        horse.setCurrentStallId(stall.getId());
        return horses.saveAndFlush(horse);
    }

    private TrainingLot lot(LocalDate date, LocalTime start, LocalTime end) {
        TrainingLot lot = new TrainingLot();
        lot.setTrainerId(trainer.getId());
        lot.setSubjectId(subject.getId());
        lot.setLotDate(date);
        lot.setStartTime(start);
        lot.setEndTime(end);
        lot.setMaxCapacity(6);
        lot.setStatus(LotStatus.SCHEDULED);
        return lots.saveAndFlush(lot);
    }

    private TrainingWorkout workout(TrainingLot lot, Horse horse, WorkoutStatus status) {
        TrainingWorkout workout = new TrainingWorkout();
        workout.setPlanId(plan.getId());
        workout.setLotId(lot.getId());
        workout.setHorseId(horse.getId());
        workout.setAssignedToId(groom.getId());
        workout.setStatus(status);
        if (status == WorkoutStatus.COMPLETED) {
            // The database requires a full result on a COMPLETED workout (chk_workouts_completed_has_result).
            workout.setActualDistanceMeters(BigDecimal.valueOf(800));
            workout.setActualDurationMinutes(BigDecimal.valueOf(40));
            workout.setTopSpeedKmh(BigDecimal.valueOf(60));
            workout.setAverageSpeedKmh(BigDecimal.valueOf(50));
            workout.setAverageHeartRate(140);
            workout.setMaxHeartRate(180);
            workout.setRecoveryHeartRate(90);
            workout.setPerformanceRating(8);
        }
        return workouts.saveAndFlush(workout);
    }

    // =================================================================
    // Plans
    // =================================================================

    @Test
    void planSummariesJoinNamesAndCountWorkoutsPerStatus() {
        List<PlanSummaryResponse> mine = plans.findPlanSummaries(trainer.getId(), null);

        PlanSummaryResponse row = mine.stream().filter(p -> p.getPlanId().equals(plan.getId())).findFirst().orElseThrow();
        assertEquals("IT Horse One", row.getHorseName());
        assertEquals(course.getName(), row.getCourseName());
        assertEquals(TrainingPlanStatus.ACTIVE, row.getStatus());
        // The fixture puts 2 scheduled + 1 completed workout under the same plan
        assertEquals(3, row.getTotalSessions());
        assertEquals(1, row.getCompletedSessions());
        assertEquals(0, row.getCancelledSessions());
    }

    @Test
    void planSummariesAcceptNullAndNonNullFilters() {
        // trainer + horse
        assertEquals(1, plans.findPlanSummaries(trainer.getId(), horse1.getId()).stream()
                .filter(p -> p.getPlanId().equals(plan.getId())).count());
        // another horse has no plan of this trainer
        assertTrue(plans.findPlanSummaries(trainer.getId(), horse2.getId()).isEmpty());
        // every trainer, one horse
        assertEquals(1, plans.findPlanSummaries(null, horse1.getId()).size());
        // someone else's trainer id
        assertTrue(plans.findPlanSummaries(groom.getId(), null).isEmpty());
    }

    @Test
    void planNamesAndOwnership() {
        Optional<PlanNamesRow> names = plans.findPlanNames(plan.getId());
        assertTrue(names.isPresent());
        assertEquals("IT Horse One", names.get().horseName());
        assertEquals(course.getName(), names.get().courseName());

        assertTrue(plans.existsPlanOfOwner(plan.getId(), owner.getId()));
        assertFalse(plans.existsPlanOfOwner(plan.getId(), otherOwner.getId()));
    }

    @Test
    void stallInfoComesFromOneJoin() {
        List<HorseStallInfo> info = plans.findStallInfoByHorseIds(List.of(horse1.getId(), horse2.getId()));

        assertEquals(2, info.size());
        HorseStallInfo first = info.stream().filter(i -> i.horseId().equals(horse1.getId())).findFirst().orElseThrow();
        assertEquals("IT-901", first.stallCode());
        assertEquals(groom.getId(), first.stallGroomId());
        assertEquals("A", first.areaCode());
        assertEquals(trainer.getId(), first.areaTrainerId());
    }

    @Test
    void runningPlansCarryCourseNameAndSkipFinishedOnes() {
        List<RunningPlanRow> running = plans.findRunningPlans(List.of(horse1.getId()), today.plusDays(5));
        assertEquals(1, running.size());
        assertEquals(plan.getId(), running.get(0).planId());
        assertEquals(course.getName(), running.get(0).courseName());
        assertEquals(TrainingPlanStatus.ACTIVE, running.get(0).status());

        assertTrue(plans.findRunningPlans(List.of(horse1.getId()), today.plusDays(11)).isEmpty(),
                "The plan ends before the given date");
        assertTrue(plans.findRunningPlans(List.of(horse2.getId()), today).isEmpty(), "Another horse");

        plan.setStatus(TrainingPlanStatus.COMPLETED);
        plans.saveAndFlush(plan);
        assertTrue(plans.findRunningPlans(List.of(horse1.getId()), today).isEmpty(), "Only ACTIVE and UPCOMING");
    }

    @Test
    void enrollableHorsesAreTheAdmittedHorsesOfTheTrainerAreas() {
        horse2.setCurrentStatus(HorseStatus.CANDIDATE);
        horses.saveAndFlush(horse2);

        List<Horse> enrollable = plans.findEnrollableHorses(trainer.getId());

        assertTrue(enrollable.stream().anyMatch(h -> h.getId().equals(horse1.getId())));
        assertFalse(enrollable.stream().anyMatch(h -> h.getId().equals(horse2.getId())), "Not admitted yet");
        assertTrue(plans.findEnrollableHorses(groom.getId()).isEmpty(), "Not a trainer of any area");
    }

    @Test
    void joinableCohortsGroupByStartDateAndTrainingDays() {
        List<JoinableCohortRow> cohorts = plans.findJoinableCohorts(trainer.getId(), course.getId());

        assertEquals(1, cohorts.size());
        assertEquals(plan.getStartDate(), cohorts.get(0).startDate());
        assertEquals(Set.of(TrainingDay.MONDAY, TrainingDay.WEDNESDAY), cohorts.get(0).trainingDays());
        assertEquals(1L, cohorts.get(0).horseCount());
    }

    @Test
    void dashboardQueries() {
        List<DashboardHorseRow> dashboardHorses = plans.findDashboardHorses(trainer.getId());
        assertTrue(dashboardHorses.stream().anyMatch(h -> h.horseId().equals(horse1.getId())
                && "IT-901".equals(h.stallCode())));
        assertTrue(dashboardHorses.stream().anyMatch(h -> h.horseId().equals(horse2.getId())));

        List<DashboardPlanRow> dashboardPlans = plans.findDashboardPlans(List.of(horse1.getId(), horse2.getId()));
        DashboardPlanRow row = dashboardPlans.stream().filter(p -> p.planId().equals(plan.getId())).findFirst().orElseThrow();
        assertEquals(course.getName(), row.courseName());
        assertEquals(4, row.totalSessions());
        assertEquals(1L, row.completedSessions());

        assertTrue(plans.existsHorseInTrainerArea(horse1.getId(), trainer.getId()));
        assertFalse(plans.existsHorseInTrainerArea(horse1.getId(), groom.getId()));
    }

    @Test
    void dashboardHorsesOfAnOwnerAndOfTheWholeFarm() {
        Horse unhoused = new Horse();
        unhoused.setName("IT Unhoused");
        unhoused.setOwnerId(owner.getId());
        unhoused = horses.saveAndFlush(unhoused);

        List<DashboardHorseRow> ownerHorses = plans.findDashboardHorsesOfOwner(owner.getId());
        assertEquals(2, ownerHorses.size(), "Only the horses of this owner");
        assertTrue(ownerHorses.stream().anyMatch(h -> h.horseId().equals(horse1.getId()) && "IT-901".equals(h.stallCode())));
        final Long unhousedId = unhoused.getId();
        assertTrue(ownerHorses.stream().anyMatch(h -> h.horseId().equals(unhousedId) && h.stallCode() == null),
                "A horse without a stall is still listed, without a stall code");

        List<DashboardHorseRow> farm = plans.findAllDashboardHorses();
        assertTrue(farm.stream().anyMatch(h -> h.horseId().equals(horse1.getId())));
        assertTrue(farm.stream().anyMatch(h -> h.horseId().equals(horse2.getId())), "Horses of other owners too");
    }

    // =================================================================
    // Workouts
    // =================================================================

    @Test
    void planWorkoutItemsCarryLotAndOccupancy() {
        List<PlanWorkoutItemResponse> items = workouts.findPlanWorkoutItems(plan.getId());

        // The fixture puts all 3 workouts under the same plan id
        assertEquals(3, items.size());
        PlanWorkoutItemResponse scheduled = items.stream()
                .filter(i -> i.getWorkoutId().equals(workout1.getId())).findFirst().orElseThrow();
        assertEquals("IT Sprint", scheduled.getSubjectName());
        assertEquals(todayLot.getId(), scheduled.getLotId());
        assertEquals(today, scheduled.getLotDate());
        assertEquals(2, scheduled.getLotOccupancy(), "horse 1 and horse 2 share today's lot");
        assertEquals("SCHEDULED", scheduled.getStatus());
        // Ordered by lot date: yesterday's completed workout comes first
        assertEquals(done.getId(), items.get(0).getWorkoutId());
    }

    @Test
    void fitnessTrendAndRecentWorkoutsReadOnlyCompletedOnes() {
        List<HorseFitnessTrendItemResponse> trend =
                workouts.findFitnessTrend(horse1.getId(), today.minusDays(30), today);
        assertEquals(1, trend.size());
        assertEquals("IT Sprint", trend.get(0).getSubjectName());
        assertEquals(today.minusDays(1), trend.get(0).getDate());
        assertEquals(8, trend.get(0).getPerformanceRating());

        assertTrue(workouts.findFitnessTrend(horse1.getId(), today, today).isEmpty(), "Outside the range");

        List<RecentWorkoutRow> recent = workouts.findRecentCompletedWorkouts(
                List.of(horse1.getId(), horse2.getId()), today.minusDays(30));
        assertEquals(1, recent.size());
        assertEquals(230, recent.get(0).maxHeartRate());
        assertEquals(LocalTime.of(7, 0), recent.get(0).lotEndTime());
    }

    @Test
    void performanceSessionsCarryTheValuesOfEveryCompletedSession() {
        TrainingLot yesterdayLot = lots.findById(done.getLotId()).orElseThrow();
        TrainingWorkout second = workout(yesterdayLot, horse2, WorkoutStatus.COMPLETED);
        second.setPerformanceRating(6);
        second.setMaxHeartRate(190);
        second.setAverageSpeedKmh(BigDecimal.valueOf(40));
        workouts.saveAndFlush(second);

        List<PerformanceSessionRow> sessions =
                workouts.findPerformanceSessions(List.of(horse1.getId(), horse2.getId()), today.minusDays(29), today);

        // Today only has scheduled sessions, so only yesterday's two completed ones come back.
        assertEquals(2, sessions.size());
        PerformanceSessionRow other = sessions.stream().filter(s -> s.horseId().equals(horse2.getId())).findFirst().orElseThrow();
        assertEquals(today.minusDays(1), other.date());
        assertEquals(6, other.rating());
        assertEquals(190, other.maxHeartRate());
        assertEquals(0, BigDecimal.valueOf(40).compareTo(other.averageSpeedKmh()));

        assertEquals(1, workouts.findPerformanceSessions(List.of(horse1.getId()), today.minusDays(29), today).size(),
                "Only the sessions of the given horses count");
        assertTrue(workouts.findPerformanceSessions(List.of(-1L), today.minusDays(29), today).isEmpty());
        assertTrue(workouts.findPerformanceSessions(List.of(horse1.getId()), today, today).isEmpty(), "Outside the range");
    }

    @Test
    void groomWorkoutRowsAndConflicts() {
        List<GroomWorkoutRow> rows = workouts.findGroomWorkoutRows(groom.getId(), today);
        assertEquals(2, rows.size());
        assertTrue(rows.stream().anyMatch(r -> "IT Horse One".equals(r.horseName())
                && "IT Sprint".equals(r.subjectName()) && LocalTime.of(6, 0).equals(r.startTime())));

        // Horse 1 would clash with the groom in today's lot, because the groom already leads horse 2 there
        List<GroomConflictRow> conflicts = workouts.findGroomConflicts(horse1.getId(), groom.getId(), today);
        assertEquals(1, conflicts.size());
        assertEquals("IT Horse Two", conflicts.get(0).otherHorseName());
        assertEquals(todayLot.getId(), conflicts.get(0).lotId());

        assertTrue(workouts.findGroomConflicts(horse1.getId(), trainer.getId(), today).isEmpty());
    }

    @Test
    void ownerHasWorkoutInLotOnlyForOwnHorses() {
        assertTrue(workouts.existsActiveWorkoutOfOwnerInLot(todayLot.getId(), owner.getId()));
        assertTrue(workouts.existsActiveWorkoutOfOwnerInLot(todayLot.getId(), otherOwner.getId()));
        assertFalse(workouts.existsActiveWorkoutOfOwnerInLot(todayLot.getId(), groom.getId()));
    }

    @Test
    void notYetHeldWorkoutsOfAPlanSkipLotsThatHaveEnded() {
        LocalDate today = todayLot.getLotDate();
        TrainingWorkout tomorrow = workout(lot(today.plusDays(1), LocalTime.of(6, 0), LocalTime.of(7, 0)),
                horse1, WorkoutStatus.SCHEDULED);
        TrainingWorkout missedYesterday = workout(lot(today.minusDays(1), LocalTime.of(8, 0), LocalTime.of(9, 0)),
                horse1, WorkoutStatus.SCHEDULED);

        // Before today's lot ends (06:00-07:00): today's and tomorrow's sessions are still to come.
        List<Long> early = workouts.findNotYetHeldByPlan(plan.getId(), today, LocalTime.of(6, 30)).stream()
                .map(TrainingWorkout::getId).toList();
        assertTrue(early.containsAll(List.of(workout1.getId(), workout2.getId(), tomorrow.getId())));
        assertFalse(early.contains(missedYesterday.getId()), "A past session without a result is kept to record");
        assertFalse(early.contains(done.getId()), "A completed session is never cancelled");

        // After today's lot has ended only tomorrow's session is left.
        assertEquals(List.of(tomorrow.getId()),
                workouts.findNotYetHeldByPlan(plan.getId(), today, LocalTime.of(7, 0)).stream()
                        .map(TrainingWorkout::getId).toList());

        // The same rule per horse (training block, groom change): this morning's ended lot is not touched.
        assertEquals(List.of(tomorrow.getId()),
                workouts.findNotYetHeldByHorse(horse1.getId(), today, LocalTime.of(9, 0)).stream()
                        .map(TrainingWorkout::getId).toList());
    }

    // =================================================================
    // Lots
    // =================================================================

    @Test
    void lotRowsPerRole() {
        List<TrainingLotRow> trainerRows = lots.findTrainerLotRows(trainer.getId(), today, today, false);
        assertEquals(1, trainerRows.size());
        TrainingLotRow row = trainerRows.get(0);
        assertEquals("IT Sprint", row.subjectName());
        assertEquals(60, row.durationMinutes());
        assertEquals(2L, row.occupied());
        assertEquals(trainer.getId(), row.trainerId());

        assertEquals(1, lots.findGroomLotRows(groom.getId(), today, today).size());
        assertEquals(1, lots.findOwnerLotRows(owner.getId(), today, today).size());
        assertTrue(lots.findOwnerLotRows(groom.getId(), today, today).isEmpty(), "Not an owner of any horse");
        assertTrue(lots.findOwnerLotRows(owner.getId(), today.plusDays(1), today.plusDays(3)).isEmpty());

        assertTrue(lots.findLotRowById(todayLot.getId()).isPresent());
        assertTrue(lots.findLotRowById(-1L).isEmpty());
    }

    @Test
    void cancelledLotsAreHiddenUnlessRequested() {
        todayLot.setStatus(LotStatus.CANCELLED);
        lots.saveAndFlush(todayLot);

        assertTrue(lots.findTrainerLotRows(trainer.getId(), today, today, false).isEmpty());
        assertEquals(1, lots.findTrainerLotRows(trainer.getId(), today, today, true).size());
        assertTrue(lots.findGroomLotRows(groom.getId(), today, today).isEmpty());
        assertTrue(lots.findOwnerLotRows(owner.getId(), today, today).isEmpty());
    }

    @Test
    void horseNamesOfOwnerExcludeOtherOwnersHorses() {
        List<Long> lotIds = List.of(todayLot.getId());

        List<LotHorseNameRow> all = lots.findActiveHorseNames(lotIds);
        assertEquals(List.of("IT Horse One", "IT Horse Two"), all.stream().map(LotHorseNameRow::horseName).toList());

        List<LotHorseNameRow> mine = lots.findActiveHorseNamesOfOwner(lotIds, owner.getId());
        assertEquals(List.of("IT Horse One"), mine.stream().map(LotHorseNameRow::horseName).toList());
    }

    // =================================================================
    // Courses
    // =================================================================

    @Test
    void courseSubjectsAreJoinedInOrder() {
        List<Subject> ordered = courseSubjects.findSubjectsOfCourseInOrder(course.getId());
        assertEquals(1, ordered.size());
        assertEquals("IT Sprint", ordered.get(0).getName());
        assertEquals(SubjectCategory.SPEED, ordered.get(0).getCategory());

        List<CourseSubjectResponse> responses = courseSubjects.findCourseSubjectResponses(course.getId());
        assertEquals(1, responses.size());
        assertEquals("IT Sprint", responses.get(0).getSubjectName());
        assertEquals(60, responses.get(0).getDurationMinutes());
        assertEquals(1, responses.get(0).getOrderIndex());

        assertEquals(1L, courseSubjects.countByCourseId(course.getId()));
    }

    @Test
    void subjectsCanBeFilteredByCategory() {
        assertTrue(subjects.findByCategory(SubjectCategory.SPEED).stream()
                .anyMatch(s -> s.getId().equals(subject.getId())));
        assertTrue(subjects.findByCategory(SubjectCategory.RECOVERY).stream()
                .noneMatch(s -> s.getId().equals(subject.getId())));
    }

    // =================================================================
    // Specialty
    // =================================================================

    private TrainingWorkout completedWorkout(TrainingLot lot, Horse horse, Integer rating, BigDecimal actualDistance) {
        TrainingWorkout workout = workout(lot, horse, WorkoutStatus.COMPLETED);
        workout.setPerformanceRating(rating);
        if (actualDistance != null) {
            workout.setActualDistanceMeters(actualDistance);
        }
        return workouts.saveAndFlush(workout);
    }

    private Subject subjectOf(SubjectCategory category, SurfaceType surface, BigDecimal targetDistance) {
        Subject other = new Subject();
        other.setCategory(category);
        other.setName("IT " + category + " " + System.nanoTime());
        other.setSurfaceType(surface);
        other.setTargetDistanceMeters(targetDistance);
        other.setIntensityLevel(IntensityLevel.MEDIUM);
        other.setDurationMinutes(45);
        return subjects.saveAndFlush(other);
    }

    private TrainingLot lotOf(Subject lotSubject, LocalDate date) {
        TrainingLot lot = new TrainingLot();
        lot.setTrainerId(trainer.getId());
        lot.setSubjectId(lotSubject.getId());
        lot.setLotDate(date);
        lot.setStartTime(LocalTime.of(8, 0));
        lot.setEndTime(LocalTime.of(9, 0));
        lot.setMaxCapacity(6);
        lot.setStatus(LotStatus.SCHEDULED);
        return lots.saveAndFlush(lot);
    }

    @Test
    void specialtyWorkoutsJoinTheSurfaceAndUseTheRecordedDistance() {
        Subject endurance = subjectOf(SubjectCategory.ENDURANCE, SurfaceType.DIRT, BigDecimal.valueOf(1600));
        completedWorkout(lotOf(endurance, today.minusDays(3)), horse1, 7, BigDecimal.valueOf(1500));

        List<SpecialtyWorkoutRow> rows = workouts.findSpecialtyWorkouts(List.of(horse1.getId()), today.minusDays(90));

        assertEquals(2, rows.size(), "The fixture's completed workout plus the one just added");
        SpecialtyWorkoutRow fixtureRow = rows.stream()
                .filter(r -> r.surfaceType() == SurfaceType.TURF).findFirst().orElseThrow();
        assertEquals(0, BigDecimal.valueOf(800).compareTo(fixtureRow.distanceMeters()));
        assertEquals(8, fixtureRow.performanceRating());
        SpecialtyWorkoutRow fallbackRow = rows.stream()
                .filter(r -> r.surfaceType() == SurfaceType.DIRT).findFirst().orElseThrow();
        assertEquals(0, BigDecimal.valueOf(1500).compareTo(fallbackRow.distanceMeters()));
        assertEquals(horse1.getId(), fallbackRow.horseId());
    }

    @Test
    void specialtyWorkoutsLeaveOutRecoveryUnfinishedAndOldOnes() {
        Subject recovery = subjectOf(SubjectCategory.RECOVERY, SurfaceType.SYNTHETIC, BigDecimal.valueOf(500));
        Subject speed = subjectOf(SubjectCategory.SPEED, SurfaceType.TURF, BigDecimal.valueOf(1000));
        completedWorkout(lotOf(recovery, today.minusDays(2)), horse1, 9, null);          // recovery category
        workout(lotOf(speed, today.minusDays(4)), horse1, WorkoutStatus.SCHEDULED);       // not completed
        completedWorkout(lotOf(speed, today.minusDays(120)), horse1, 9, null);           // before the window
        completedWorkout(lotOf(speed, today.minusDays(5)), horse2, 9, null);             // another horse

        List<SpecialtyWorkoutRow> rows = workouts.findSpecialtyWorkouts(List.of(horse1.getId()), today.minusDays(90));

        assertEquals(1, rows.size(), "Only the fixture's completed, non-recovery, in-window workout is left");
        assertEquals(SurfaceType.TURF, rows.get(0).surfaceType());
        assertEquals(0, BigDecimal.valueOf(800).compareTo(rows.get(0).distanceMeters()));
    }

    @Test
    void specialtyWorkoutsOfManyHorsesComeInOneQuery() {
        Subject speed = subjectOf(SubjectCategory.SPEED, SurfaceType.TURF, BigDecimal.valueOf(1000));
        completedWorkout(lotOf(speed, today.minusDays(5)), horse2, 6, null);

        List<SpecialtyWorkoutRow> rows = workouts.findSpecialtyWorkouts(
                List.of(horse1.getId(), horse2.getId()), today.minusDays(90));

        assertEquals(2, rows.size());
        assertTrue(rows.stream().anyMatch(r -> r.horseId().equals(horse1.getId())));
        assertTrue(rows.stream().anyMatch(r -> r.horseId().equals(horse2.getId())));
    }
}
