package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CreateHorseTrainingPlanRequest;
import com.rtms.backend.training.dto.HorseEnrollmentRequest;
import com.rtms.backend.training.dto.HorseStallInfo;
import com.rtms.backend.training.dto.HorseTrainingPlanDetailResponse;
import com.rtms.backend.training.dto.JoinableCohortResponse;
import com.rtms.backend.training.dto.JoinableCohortRow;
import com.rtms.backend.training.dto.PlanNamesRow;
import com.rtms.backend.training.dto.PlanPreviewRequest;
import com.rtms.backend.training.dto.PlanPreviewResponse;
import com.rtms.backend.training.dto.PlanSummaryResponse;
import com.rtms.backend.training.dto.PlanWorkoutItemResponse;
import com.rtms.backend.training.dto.RunningPlanRow;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.HorseTrainingPlan;
import com.rtms.backend.training.entity.Subject;
import com.rtms.backend.training.enums.PlanAvailability;
import com.rtms.backend.training.enums.TrainingDay;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.enums.TrainingPlanStatus;
import com.rtms.backend.training.repository.CourseRepository;
import com.rtms.backend.training.repository.CourseSubjectRepository;
import com.rtms.backend.training.repository.HorseTrainingPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HorseTrainingPlanServiceTest {

    @Mock
    private HorseTrainingPlanRepository planRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseSubjectRepository courseSubjectRepository;

    @Mock
    private HorseRepository horseRepository;

    @Mock
    private TrainingDecisionService trainingDecisionService;

    @Mock
    private TrainingWorkoutService workoutService;

    private HorseTrainingPlanService planService;

    private final AuthenticatedUser trainerUser = new AuthenticatedUser(9L, "trainer@example.com", "HEAD_TRAINER");

    /** A Monday in the future: start dates in the past are rejected, so fixed calendar dates would go stale. */
    private static final LocalDate NEXT_MONDAY = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

    @BeforeEach
    void setUp() {
        planService = new HorseTrainingPlanService(
                planRepository,
                courseRepository,
                courseSubjectRepository,
                horseRepository,
                trainingDecisionService,
                workoutService
        );
    }

    // =================================================================
    // Helpers
    // =================================================================

    private Horse horse(Long id, String name) {
        Horse horse = new Horse();
        horse.setId(id);
        horse.setName(name);
        return horse;
    }

    private RunningPlanRow runningPlan(Long planId, Long horseId, LocalDate start, LocalDate end) {
        return new RunningPlanRow(planId, horseId, "Course " + planId, start, end, TrainingPlanStatus.ACTIVE);
    }

    private Subject subject(Long id, String name) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setName(name);
        return subject;
    }

    private Course course(Long id, int totalSessions) {
        Course course = new Course();
        course.setId(id);
        course.setName("Course " + id);
        course.setTotalSessions(totalSessions);
        return course;
    }

    private CreateHorseTrainingPlanRequest request(Long courseId, LocalDate startDate, Long... horseIds) {
        CreateHorseTrainingPlanRequest request = new CreateHorseTrainingPlanRequest();
        request.setCourseId(courseId);
        request.setStartDate(startDate);
        request.setTrainingDays(List.of(TrainingDay.MONDAY, TrainingDay.WEDNESDAY));

        List<HorseEnrollmentRequest> enrollments = new ArrayList<>();
        for (Long horseId : horseIds) {
            HorseEnrollmentRequest enrollment = new HorseEnrollmentRequest();
            enrollment.setHorseId(horseId);
            enrollments.add(enrollment);
        }
        request.setHorses(enrollments);
        return request;
    }

    /** Course + subjects loaded, ready for the enrollment step. */
    private void stubCourse(Long courseId, int totalSessions, Subject... subjects) {
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course(courseId, totalSessions)));
        when(courseSubjectRepository.findSubjectsOfCourseInOrder(courseId)).thenReturn(List.of(subjects));
    }

    /** The given horses exist, and stall info is returned for the ones listed in {@code stalls}. */
    private void stubHorses(List<Horse> horses, HorseStallInfo... stalls) {
        when(horseRepository.findAllById(any())).thenReturn(horses);
        when(planRepository.findStallInfoByHorseIds(any())).thenReturn(List.of(stalls));
    }

    // =================================================================
    // createPlan
    // =================================================================

    @Test
    @DisplayName("1. Creates plans for a group of 2 horses and hands the schedule to the workout service")
    void testCreatePlan_Success() {
        LocalDate startDate = LocalDate.now();
        LocalDate secondSession = startDate.plusDays(2);
        Subject warmUp = subject(50L, "Warm-up 30 min");
        stubCourse(100L, 2, warmUp);
        stubHorses(List.of(horse(1L, "White Dragon"), horse(2L, "Red Hare")),
                new HorseStallInfo(1L, "A1", 4L, "A", 9L),
                new HorseStallInfo(2L, "A2", 4L, "A", 9L));
        when(planRepository.findRunningPlans(any(), any())).thenReturn(Collections.emptyList());
        when(planRepository.saveAllAndFlush(anyList())).thenAnswer(inv -> {
            List<HorseTrainingPlan> plans = inv.getArgument(0);
            plans.forEach(p -> p.setId(p.getHorseId() + 1000L));
            return plans;
        });
        when(workoutService.createWorkouts(any(), anyList(), anyMap())).thenAnswer(inv -> {
            List<EnrolledHorse> enrolled = inv.getArgument(1);
            Map<Long, List<PlanWorkoutItemResponse>> workouts = new LinkedHashMap<>();
            enrolled.forEach(en -> workouts.put(en.horse().getId(), new ArrayList<>()));
            return workouts;
        });

        CreateHorseTrainingPlanRequest request = request(100L, startDate, 1L, 2L);
        request.setTrainingDays(List.of(trainingDayOf(startDate), trainingDayOf(secondSession)));

        List<HorseTrainingPlanDetailResponse> responses = planService.createPlan(request, trainerUser);

        assertEquals(2, responses.size(), "One response per horse");
        assertEquals(1L, responses.get(0).getPlan().getHorseId());
        assertEquals(2L, responses.get(1).getPlan().getHorseId());
        assertEquals(TrainingPlanStatus.ACTIVE, responses.get(0).getPlan().getStatus(),
                "A plan that starts today or earlier is ACTIVE");
        // Hai buổi, hôm nay và hai ngày sau (two sessions, today and two days later)
        assertEquals(secondSession, responses.get(0).getPlan().getEndDate());
        assertEquals(4L, responses.get(0).getPlan().getGroomId(), "Groom is taken from the stall");

        ArgumentCaptor<TrainingWorkoutService.PlanSchedule> captor =
                ArgumentCaptor.forClass(TrainingWorkoutService.PlanSchedule.class);
        verify(workoutService).createWorkouts(captor.capture(), anyList(), anyMap());
        assertEquals(9L, captor.getValue().trainerId());
        assertEquals(startDate, captor.getValue().startDate());
        assertEquals(2, captor.getValue().totalSessions());
        assertEquals(List.of(warmUp), captor.getValue().orderedSubjects());
        verify(planRepository, times(1)).saveAllAndFlush(anyList());
    }

    @Test
    @DisplayName("1b. A plan that starts in the future is UPCOMING")
    void testCreatePlan_FutureStart_IsUpcoming() {
        LocalDate future = LocalDate.now().plusDays(30);
        stubCourse(100L, 1, subject(50L, "Warm-up"));
        stubHorses(List.of(horse(1L, "White Dragon")), new HorseStallInfo(1L, "A1", 4L, "A", 9L));
        when(planRepository.findRunningPlans(any(), any())).thenReturn(Collections.emptyList());
        when(planRepository.saveAllAndFlush(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(workoutService.createWorkouts(any(), anyList(), anyMap())).thenReturn(new LinkedHashMap<>());

        CreateHorseTrainingPlanRequest request = request(100L, future, 1L);
        request.setTrainingDays(List.of(TrainingDay.values()));

        List<HorseTrainingPlanDetailResponse> responses = planService.createPlan(request, trainerUser);

        assertEquals(TrainingPlanStatus.UPCOMING, responses.get(0).getPlan().getStatus());
    }

    @Test
    @DisplayName("1c. A groom chosen in the request overrides the stall groom")
    void testCreatePlan_GroomOverride() {
        stubCourse(100L, 1, subject(50L, "Warm-up"));
        stubHorses(List.of(horse(1L, "White Dragon")), new HorseStallInfo(1L, "A1", 4L, "A", 9L));
        when(planRepository.findRunningPlans(any(), any())).thenReturn(Collections.emptyList());
        when(planRepository.saveAllAndFlush(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(workoutService.createWorkouts(any(), anyList(), anyMap())).thenReturn(new LinkedHashMap<>());

        CreateHorseTrainingPlanRequest request = request(100L, LocalDate.now(), 1L);
        request.getHorses().get(0).setGroomId(77L);

        List<HorseTrainingPlanDetailResponse> responses = planService.createPlan(request, trainerUser);

        assertEquals(77L, responses.get(0).getPlan().getGroomId());
    }

    @Test
    @DisplayName("2. Fails when the horse list is empty")
    void testCreatePlan_EmptyHorses_ThrowsIllegalArgument() {
        CreateHorseTrainingPlanRequest req = request(1L, LocalDate.now());
        req.setHorses(Collections.emptyList());

        assertThrows(IllegalArgumentException.class, () -> planService.createPlan(req, trainerUser));
    }

    @Test
    @DisplayName("3. Fails when the course, start date or training days are missing")
    void testCreatePlan_MissingCourseOrDate_ThrowsIllegalArgument() {
        CreateHorseTrainingPlanRequest req = request(null, LocalDate.now(), 1L);
        assertThrows(IllegalArgumentException.class, () -> planService.createPlan(req, trainerUser));

        req.setCourseId(1L);
        req.setStartDate(null);
        assertThrows(IllegalArgumentException.class, () -> planService.createPlan(req, trainerUser));

        req.setStartDate(LocalDate.now());
        req.setTrainingDays(null);
        assertThrows(IllegalArgumentException.class, () -> planService.createPlan(req, trainerUser));

        verifyNoInteractions(courseRepository, planRepository);
    }

    @Test
    @DisplayName("3b. Fails when the course has no subjects")
    void testCreatePlan_CourseWithoutSubjects_ThrowsIllegalState() {
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course(10L, 1)));
        when(courseSubjectRepository.findSubjectsOfCourseInOrder(10L)).thenReturn(List.of());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> planService.createPlan(request(10L, LocalDate.now(), 1L), trainerUser));
        assertTrue(ex.getMessage().contains("no subjects"));
    }

    @Test
    @DisplayName("4. Fails when the same horse is enrolled twice in one request")
    void testCreatePlan_DuplicateHorseInRequest_ThrowsIllegalArgument() {
        stubCourse(10L, 1, subject(1L, "Warm-up"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> planService.createPlan(request(10L, LocalDate.now(), 5L, 5L), trainerUser));
        assertTrue(ex.getMessage().contains("more than once"));
    }

    @Test
    @DisplayName("4b. Fails when an enrolled horse does not exist")
    void testCreatePlan_UnknownHorse_ThrowsNotFound() {
        stubCourse(10L, 1, subject(1L, "Warm-up"));
        stubHorses(List.of());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> planService.createPlan(request(10L, LocalDate.now(), 5L), trainerUser));
        assertTrue(ex.getMessage().contains("Horse #5 not found"));
    }

    @Test
    @DisplayName("5. BR-05: the training decision service can block a plan for an injured horse")
    void testCreatePlan_HorseBlocked_ThrowsIllegalStateException() {
        stubCourse(10L, 1, subject(1L, "Warm-up"));
        Horse injured = horse(7L, "Injured Mare");
        stubHorses(List.of(injured));
        doThrow(new IllegalStateException("Horse Injured Mare is resting"))
                .when(trainingDecisionService).assertCanTrain(injured);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> planService.createPlan(request(10L, LocalDate.now(), 7L), trainerUser));
        assertTrue(ex.getMessage().contains("resting"));
        verify(planRepository, never()).saveAllAndFlush(anyList());
    }

    @Test
    @DisplayName("6. Fails when the horse has not been assigned to a stall")
    void testCreatePlan_HorseNotAssignedToStall_ThrowsIllegalStateException() {
        stubCourse(10L, 1, subject(1L, "Warm-up"));
        stubHorses(List.of(horse(3L, "Free Horse")));   // no stall info row

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> planService.createPlan(request(10L, LocalDate.now(), 3L), trainerUser));
        assertTrue(ex.getMessage().contains("has not been assigned to a stall"));
    }

    @Test
    @DisplayName("7. Trainer permission: blocks a horse in an area managed by another trainer")
    void testCreatePlan_HorseInOtherTrainerArea_ThrowsAccessDenied() {
        stubCourse(10L, 1, subject(1L, "Warm-up"));
        stubHorses(List.of(horse(3L, "Area B Horse")), new HorseStallInfo(3L, "B1", 4L, "B", 999L));

        assertThrows(AccessDeniedException.class,
                () -> planService.createPlan(request(10L, LocalDate.now(), 3L), trainerUser));
    }

    @Test
    @DisplayName("7b. Fails when the area is not assigned to any trainer")
    void testCreatePlan_AreaWithoutTrainer_ThrowsIllegalState() {
        stubCourse(10L, 1, subject(1L, "Warm-up"));
        stubHorses(List.of(horse(3L, "Orphan Horse")), new HorseStallInfo(3L, "C1", 4L, "C", null));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> planService.createPlan(request(10L, LocalDate.now(), 3L), trainerUser));
        assertTrue(ex.getMessage().contains("not assigned to any trainer"));
    }

    @Test
    @DisplayName("7c. Fails when the stall has no groom and the request names none")
    void testCreatePlan_StallWithoutGroom_ThrowsIllegalState() {
        stubCourse(10L, 1, subject(1L, "Warm-up"));
        stubHorses(List.of(horse(3L, "Groomless Horse")), new HorseStallInfo(3L, "A3", null, "A", 9L));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> planService.createPlan(request(10L, LocalDate.now(), 3L), trainerUser));
        assertTrue(ex.getMessage().contains("no groom assigned"));
    }

    @Test
    @DisplayName("8. BR-01: blocks a plan overlapping a running plan with 409 PLAN_SCHEDULE_CONFLICT")
    void testCreatePlan_OverlappingPlan_ThrowsConflict() {
        stubCourse(10L, 2, subject(1L, "Warm-up"));
        stubHorses(List.of(horse(1L, "Horse One")), new HorseStallInfo(1L, "A1", 4L, "A", 9L));
        when(planRepository.findRunningPlans(any(), any())).thenReturn(List.of(
                runningPlan(30L, 1L, NEXT_MONDAY.minusDays(4), NEXT_MONDAY.plusDays(10))));

        ApiException ex = assertThrows(ApiException.class,
                () -> planService.createPlan(request(10L, NEXT_MONDAY, 1L), trainerUser));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("PLAN_SCHEDULE_CONFLICT", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Course 30"));
        verify(planRepository, never()).saveAllAndFlush(anyList());
    }

    @Test
    @DisplayName("8b. BR-01: reports EVERY overlapped plan of EVERY horse in one message, not only the first")
    void testCreatePlan_OverlappingPlans_AllReported() {
        stubCourse(10L, 6, subject(1L, "Warm-up"));   // Mon/Wed x 6 = three weeks
        stubHorses(List.of(horse(1L, "Horse One"), horse(2L, "Horse Two")),
                new HorseStallInfo(1L, "A1", 4L, "A", 9L),
                new HorseStallInfo(2L, "A2", 5L, "A", 9L));
        when(planRepository.findRunningPlans(any(), any())).thenReturn(List.of(
                runningPlan(30L, 1L, NEXT_MONDAY, NEXT_MONDAY.plusDays(3)),
                runningPlan(31L, 1L, NEXT_MONDAY.plusDays(14), NEXT_MONDAY.plusDays(20)),
                runningPlan(32L, 2L, NEXT_MONDAY.plusDays(7), NEXT_MONDAY.plusDays(9)),
                // ends before the new plan starts, so it is not a conflict
                runningPlan(33L, 2L, NEXT_MONDAY.minusDays(1), NEXT_MONDAY.minusDays(1))));

        ApiException ex = assertThrows(ApiException.class,
                () -> planService.createPlan(request(10L, NEXT_MONDAY, 1L, 2L), trainerUser));
        assertTrue(ex.getMessage().contains("Course 30"));
        assertTrue(ex.getMessage().contains("Course 31"));
        assertTrue(ex.getMessage().contains("Course 32"));
        assertFalse(ex.getMessage().contains("Course 33"));
    }

    @Test
    @DisplayName("8c. A start date in the past is rejected before anything is loaded")
    void testCreatePlan_PastStartDate_ThrowsIllegalArgument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> planService.createPlan(request(10L, LocalDate.now().minusDays(1), 1L), trainerUser));
        assertTrue(ex.getMessage().contains("past"));
        verifyNoInteractions(courseRepository, planRepository);
    }

    @Test
    @DisplayName("8d. A start date that is not a training day is moved to the first training day")
    void testCreatePlan_StartMovedToFirstTrainingDay() {
        stubCourse(100L, 1, subject(50L, "Warm-up"));
        stubHorses(List.of(horse(1L, "White Dragon")), new HorseStallInfo(1L, "A1", 4L, "A", 9L));
        when(planRepository.findRunningPlans(any(), any())).thenReturn(Collections.emptyList());
        when(planRepository.saveAllAndFlush(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(workoutService.createWorkouts(any(), anyList(), anyMap())).thenReturn(new LinkedHashMap<>());

        // Tuesday chosen for a Mon/Wed course: the first session is Wednesday
        List<HorseTrainingPlanDetailResponse> responses =
                planService.createPlan(request(100L, NEXT_MONDAY.plusDays(1), 1L), trainerUser);

        assertEquals(NEXT_MONDAY.plusDays(2), responses.get(0).getPlan().getStartDate());
        assertEquals(NEXT_MONDAY.plusDays(2), responses.get(0).getPlan().getEndDate());
    }

    // =================================================================
    // activateStartedPlans
    // =================================================================

    @Test
    @DisplayName("activateStartedPlans: moves plans whose start date has arrived to ACTIVE")
    void testActivateStartedPlans() {
        HorseTrainingPlan p1 = new HorseTrainingPlan();
        p1.setId(1L);
        p1.setStatus(TrainingPlanStatus.UPCOMING);

        HorseTrainingPlan p2 = new HorseTrainingPlan();
        p2.setId(2L);
        p2.setStatus(TrainingPlanStatus.UPCOMING);

        LocalDate today = LocalDate.of(2026, 11, 2);
        when(planRepository.findByStatusAndStartDateLessThanEqual(
                TrainingPlanStatus.UPCOMING, today)).thenReturn(List.of(p1, p2));

        int activated = planService.activateStartedPlans(today);

        assertEquals(2, activated);
        assertEquals(TrainingPlanStatus.ACTIVE, p1.getStatus());
        assertEquals(TrainingPlanStatus.ACTIVE, p2.getStatus());
        verify(planRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("activateStartedPlans: nothing due means nothing is written")
    void testActivateStartedPlans_NothingDue() {
        LocalDate today = LocalDate.of(2026, 11, 2);
        when(planRepository.findByStatusAndStartDateLessThanEqual(
                TrainingPlanStatus.UPCOMING, today)).thenReturn(List.of());

        assertEquals(0, planService.activateStartedPlans(today));
        verify(planRepository, never()).saveAll(anyList());
    }

    // =================================================================
    // Plan list
    // =================================================================

    @Test
    @DisplayName("Plan list: a head trainer is always filtered to their own plans, even with a horseId")
    void testGetPlanSummaries_TrainerSeesOnlyOwnPlans() {
        when(planRepository.findPlanSummaries(9L, 15L)).thenReturn(List.of());

        List<PlanSummaryResponse> result = planService.getPlanSummaries(15L, trainerUser);

        assertTrue(result.isEmpty());
        verify(planRepository).findPlanSummaries(9L, 15L);
    }

    @Test
    @DisplayName("Plan list: a manager sees every trainer's plans")
    void testGetPlanSummaries_ManagerSeesAll() {
        AuthenticatedUser manager = new AuthenticatedUser(1L, "manager@example.com", "CLUB_MANAGER");
        when(planRepository.findPlanSummaries(null, null)).thenReturn(List.of());

        planService.getPlanSummaries(null, manager);

        verify(planRepository).findPlanSummaries(null, null);
    }

    // =================================================================
    // Plan detail and who may open it
    // =================================================================

    private HorseTrainingPlan plan(Long id, Long trainerId) {
        HorseTrainingPlan plan = new HorseTrainingPlan();
        plan.setId(id);
        plan.setHorseId(15L);
        plan.setTrainerId(trainerId);
        return plan;
    }

    @Test
    @DisplayName("Plan detail: the trainer who owns the plan sees names and workouts")
    void testGetPlanById_Trainer_Success() {
        when(planRepository.findById(27L)).thenReturn(Optional.of(plan(27L, 9L)));
        when(planRepository.findPlanNames(27L)).thenReturn(Optional.of(new PlanNamesRow("Wind God", "Sprint course")));
        when(workoutService.buildWorkoutItems(27L)).thenReturn(List.of());

        HorseTrainingPlanDetailResponse res = planService.getPlanById(27L, trainerUser);

        assertEquals("Wind God", res.getHorseName());
        assertEquals("Sprint course", res.getCourseName());
    }

    @Test
    @DisplayName("Plan detail: a trainer cannot open another trainer's plan (reported as not found)")
    void testGetPlanById_OtherTrainer_NotFound() {
        when(planRepository.findById(27L)).thenReturn(Optional.of(plan(27L, 888L)));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> planService.getPlanById(27L, trainerUser));
        assertTrue(ex.getMessage().contains("not found"));
        verifyNoInteractions(workoutService);
    }

    @Test
    @DisplayName("Plan detail: an owner can open a plan of their own horse")
    void testGetPlanById_Owner_OwnHorse_Success() {
        AuthenticatedUser owner = new AuthenticatedUser(5L, "owner@example.com", "HORSE_OWNER");
        when(planRepository.findById(27L)).thenReturn(Optional.of(plan(27L, 9L)));
        when(planRepository.existsPlanOfOwner(27L, 5L)).thenReturn(true);
        when(planRepository.findPlanNames(27L)).thenReturn(Optional.of(new PlanNamesRow("Wind God", "Sprint course")));
        when(workoutService.buildWorkoutItems(27L)).thenReturn(List.of());

        assertNotNull(planService.getPlanById(27L, owner));
    }

    @Test
    @DisplayName("Plan detail: an owner cannot open a plan of someone else's horse")
    void testGetPlanById_Owner_OtherHorse_NotFound() {
        AuthenticatedUser owner = new AuthenticatedUser(5L, "owner@example.com", "HORSE_OWNER");
        when(planRepository.findById(27L)).thenReturn(Optional.of(plan(27L, 9L)));
        when(planRepository.existsPlanOfOwner(27L, 5L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> planService.getPlanById(27L, owner));
        verifyNoInteractions(workoutService);
    }

    @Test
    @DisplayName("Plan detail: a vet can open any plan")
    void testGetPlanById_Vet_AnyPlan() {
        AuthenticatedUser vet = new AuthenticatedUser(3L, "vet@example.com", "VETERINARIAN");
        when(planRepository.findById(27L)).thenReturn(Optional.of(plan(27L, 888L)));
        when(planRepository.findPlanNames(27L)).thenReturn(Optional.of(new PlanNamesRow("Wind God", "Sprint course")));
        when(workoutService.buildWorkoutItems(27L)).thenReturn(List.of());

        assertNotNull(planService.getPlanById(27L, vet));
        verify(planRepository, never()).existsPlanOfOwner(any(), any());
    }

    @Test
    @DisplayName("Plan detail: an unknown plan id is not found")
    void testGetPlanById_Unknown_NotFound() {
        when(planRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> planService.getPlanById(404L, trainerUser));
    }

    // =================================================================
    // Joinable cohorts
    // =================================================================

    @Test
    @DisplayName("Cohorts: a cohort that has not started yet is joined for the whole course")
    void testGetJoinableCohorts_UpcomingCohort() {
        LocalDate start = LocalDate.now().plusDays(10);
        when(courseRepository.findById(5L)).thenReturn(Optional.of(course(5L, 12)));
        when(courseSubjectRepository.countByCourseId(5L)).thenReturn(3L);
        when(planRepository.findJoinableCohorts(9L, 5L)).thenReturn(List.of(
                new JoinableCohortRow(start, Set.of(TrainingDay.MONDAY, TrainingDay.FRIDAY), 4L)));

        List<JoinableCohortResponse> cohorts = planService.getJoinableCohorts(5L, trainerUser);

        assertEquals(1, cohorts.size());
        assertEquals("UPCOMING", cohorts.get(0).getCohortStatus());
        assertEquals(12, cohorts.get(0).getSharedSessions());
        assertEquals(4, cohorts.get(0).getHorseCount());
        assertEquals(start, cohorts.get(0).getSuggestedStartDate());
        assertEquals(12, cohorts.get(0).getTotalSessions());
    }

    @Test
    @DisplayName("Cohorts: a course without subjects has no cohorts")
    void testGetJoinableCohorts_NoSubjects() {
        when(courseRepository.findById(5L)).thenReturn(Optional.of(course(5L, 12)));
        when(courseSubjectRepository.countByCourseId(5L)).thenReturn(0L);

        assertTrue(planService.getJoinableCohorts(5L, trainerUser).isEmpty());
        verify(planRepository, never()).findJoinableCohorts(any(), any());
    }

    @Test
    @DisplayName("Cohorts: an old plan without training days is skipped")
    void testGetJoinableCohorts_SkipsPlansWithoutTrainingDays() {
        when(courseRepository.findById(5L)).thenReturn(Optional.of(course(5L, 12)));
        when(courseSubjectRepository.countByCourseId(5L)).thenReturn(3L);
        when(planRepository.findJoinableCohorts(9L, 5L)).thenReturn(List.of(
                new JoinableCohortRow(LocalDate.now().plusDays(3), Set.of(), 2L)));

        assertTrue(planService.getJoinableCohorts(5L, trainerUser).isEmpty());
    }

    // =================================================================
    // previewPlan
    // =================================================================

    private PlanPreviewRequest previewRequest(LocalDate startDate, Long... selectedHorseIds) {
        PlanPreviewRequest request = new PlanPreviewRequest();
        request.setCourseId(10L);
        request.setStartDate(startDate);
        request.setTrainingDays(List.of(TrainingDay.MONDAY, TrainingDay.WEDNESDAY));
        request.setSelectedHorseIds(List.of(selectedHorseIds));
        return request;
    }

    private void stubPreviewHorses(List<Horse> horses, HorseStallInfo... stalls) {
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course(10L, 4)));   // Mon/Wed x 4 = two weeks
        when(planRepository.findEnrollableHorses(9L)).thenReturn(horses);
        when(planRepository.findStallInfoByHorseIds(any())).thenReturn(List.of(stalls));
    }

    @Test
    @DisplayName("Preview: classifies each horse as available, busy, blocked or without groom")
    void testPreviewPlan_ClassifiesHorses() {
        Horse free = horse(1L, "Free");
        Horse busy = horse(2L, "Busy");
        Horse blocked = horse(3L, "Blocked");
        blocked.setTrainingDecision(TrainingDecision.BLOCKED);
        Horse groomless = horse(4L, "Groomless");
        stubPreviewHorses(List.of(free, busy, blocked, groomless),
                new HorseStallInfo(1L, "A1", 4L, "A", 9L),
                new HorseStallInfo(2L, "A2", 5L, "A", 9L),
                new HorseStallInfo(3L, "A3", 6L, "A", 9L),
                new HorseStallInfo(4L, "A4", null, "A", 9L));
        when(planRepository.findRunningPlans(any(), any())).thenReturn(List.of(
                runningPlan(30L, 2L, NEXT_MONDAY.minusDays(3), NEXT_MONDAY.plusDays(4))));
        when(trainingDecisionService.describeRest(blocked)).thenReturn("Tendon strain (no follow-up exam scheduled)");

        PlanPreviewResponse preview = planService.previewPlan(previewRequest(NEXT_MONDAY), trainerUser);

        assertEquals(NEXT_MONDAY, preview.firstSessionDate());
        assertEquals(NEXT_MONDAY.plusDays(9), preview.endDate(), "Mon, Wed, Mon, Wed");
        assertEquals(PlanAvailability.AVAILABLE, preview.horses().get(0).availability());
        assertEquals(PlanAvailability.BUSY, preview.horses().get(1).availability());
        assertEquals(30L, preview.horses().get(1).conflicts().get(0).planId());
        assertEquals(NEXT_MONDAY.plusDays(7), preview.horses().get(1).earliestStart(),
                "The first Monday after the busy plan ends");
        assertEquals(PlanAvailability.BLOCKED, preview.horses().get(2).availability());
        assertTrue(preview.horses().get(2).reason().contains("Tendon strain"));
        assertEquals(PlanAvailability.NO_GROOM, preview.horses().get(3).availability());
        assertNull(preview.groupEarliestStart(), "No horse is selected");
    }

    @Test
    @DisplayName("Preview: the group date skips a gap that is too short for the course")
    void testPreviewPlan_GroupEarliestStartSkipsShortGap() {
        stubPreviewHorses(List.of(horse(1L, "One"), horse(2L, "Two")),
                new HorseStallInfo(1L, "A1", 4L, "A", 9L),
                new HorseStallInfo(2L, "A2", 5L, "A", 9L));
        // Horse 1 is busy this week; horse 2 is busy from the Wednesday of the following week. The free Monday in
        // between leaves room for one session only, while the course needs four.
        when(planRepository.findRunningPlans(any(), any())).thenReturn(List.of(
                runningPlan(30L, 1L, NEXT_MONDAY, NEXT_MONDAY.plusDays(4)),
                runningPlan(31L, 2L, NEXT_MONDAY.plusDays(9), NEXT_MONDAY.plusDays(18))));

        PlanPreviewResponse preview = planService.previewPlan(previewRequest(NEXT_MONDAY, 1L, 2L), trainerUser);

        assertEquals(NEXT_MONDAY.plusDays(21), preview.groupEarliestStart());
    }

    @Test
    @DisplayName("Preview: rejects a start date in the past like createPlan does")
    void testPreviewPlan_PastStartDate_ThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> planService.previewPlan(previewRequest(LocalDate.now().minusDays(1)), trainerUser));
        verifyNoInteractions(planRepository);
    }

    private static TrainingDay trainingDayOf(LocalDate date) {
        return TrainingDay.valueOf(date.getDayOfWeek().name());
    }
}
