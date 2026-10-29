package com.rtms.backend.training.service;

import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.notification.dto.HorseAlertResponse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.DashboardHorseResponse;
import com.rtms.backend.training.dto.DashboardHorseRow;
import com.rtms.backend.training.dto.DashboardPlanRow;
import com.rtms.backend.training.dto.HorseFitnessTrendItemResponse;
import com.rtms.backend.training.dto.HorseScheduleItemResponse;
import com.rtms.backend.training.dto.HorseSpecialtyResponse;
import com.rtms.backend.training.dto.PerformanceSessionRow;
import com.rtms.backend.training.dto.PerformanceTrendResponse;
import com.rtms.backend.training.dto.RecentWorkoutRow;
import com.rtms.backend.training.enums.LotStatus;
import com.rtms.backend.training.enums.TrainingPlanStatus;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.HorseTrainingPlanRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HorsePerformanceServiceTest {

    @Mock
    private TrainingWorkoutRepository workoutRepository;

    @Mock
    private HorseTrainingPlanRepository planRepository;

    @Mock
    private HorseRepository horseRepository;

    @Mock
    private HorseSpecialtyService specialtyService;

    @Mock
    private com.rtms.backend.stable.repository.StableStallRepository stallRepository;

    private HorsePerformanceService service;

    private final AuthenticatedUser manager = new AuthenticatedUser(1L, "manager@example.com", "CLUB_MANAGER");

    @BeforeEach
    void setUp() {
        // Bộ kiểm quyền chạy thật trên repository giả, nên các test phân quyền cũ giữ nguyên.
        // (The access check runs for real on mocked repositories, so the existing authorization tests stay valid.)
        service = new HorsePerformanceService(workoutRepository, planRepository,
                new HorseViewAccessService(horseRepository, planRepository, stallRepository), specialtyService,
                new HorseScopeService(planRepository));
    }

    private Horse horse(Long id, Long ownerId) {
        Horse horse = new Horse();
        horse.setId(id);
        horse.setName("Horse " + id);
        horse.setOwnerId(ownerId);
        return horse;
    }

    private RecentWorkoutRow workout(Long horseId, LocalDate date, Integer maxHr, Integer recoveryHr, Integer rating) {
        return new RecentWorkoutRow(horseId, date, null, maxHr, recoveryHr, rating);
    }

    // =================================================================
    // Fitness trend
    // =================================================================

    @Test
    @DisplayName("7.1: the fitness trend is read with wide date bounds when from/to are empty")
    void testGetFitnessTrend_Success() {
        Long horseId = 1L;
        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse(horseId, 5L)));
        List<HorseFitnessTrendItemResponse> rows = List.of(
                new HorseFitnessTrendItemResponse(101L, LocalDate.of(2026, 9, 1), "Gallop", null, null, null, null, 135, null, null, 7),
                new HorseFitnessTrendItemResponse(102L, LocalDate.of(2026, 9, 3), "Gallop", null, null, null, null, 145, null, null, 9));
        when(workoutRepository.findFitnessTrend(eq(horseId), any(LocalDate.class), any(LocalDate.class))).thenReturn(rows);

        List<HorseFitnessTrendItemResponse> result = service.getFitnessTrend(horseId, null, null, manager);

        assertEquals(2, result.size());
        assertEquals(LocalDate.of(2026, 9, 1), result.get(0).getDate());
        assertEquals(9, result.get(1).getPerformanceRating());

        ArgumentCaptor<LocalDate> from = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> to = ArgumentCaptor.forClass(LocalDate.class);
        verify(workoutRepository).findFitnessTrend(eq(horseId), from.capture(), to.capture());
        assertEquals(LocalDate.of(1900, 1, 1), from.getValue());
        assertEquals(LocalDate.of(2999, 12, 31), to.getValue());
    }

    @Test
    @DisplayName("Performance trend: the last 12 weeks up to today, one value per horse per week")
    void testGetPerformanceTrend_Last12Weeks() {
        AuthenticatedUser trainer = new AuthenticatedUser(9L, "trainer@example.com", "HEAD_TRAINER");
        LocalDate today = LocalDate.now();
        LocalDate firstWeek = PerformanceTrendCalculator.weekStartOf(today).minusWeeks(11);
        LocalDate monday = PerformanceTrendCalculator.weekStartOf(today);
        when(planRepository.findDashboardHorses(9L)).thenReturn(List.of(
                new DashboardHorseRow(50L, "Wind God", "Thoroughbred", "A-01")));
        when(workoutRepository.findPerformanceSessions(List.of(50L), firstWeek, today)).thenReturn(List.of(
                new PerformanceSessionRow(50L, monday, 8, null, null),
                new PerformanceSessionRow(50L, monday, 6, null, null)));

        PerformanceTrendResponse trend = service.getPerformanceTrend(trainer);

        assertEquals(firstWeek, trend.fromDate());
        assertEquals(today, trend.toDate());
        assertEquals(1, trend.weeks().size());
        assertEquals(monday, trend.weeks().get(0).weekStart());
        assertEquals(7.0, trend.weeks().get(0).averageRating());
    }

    @Test
    @DisplayName("A role with no horses gets an empty performance chart and no query")
    void testGetPerformanceTrend_NoHorses() {
        AuthenticatedUser owner = new AuthenticatedUser(5L, "owner@example.com", "HORSE_OWNER");
        when(planRepository.findDashboardHorsesOfOwner(5L)).thenReturn(List.of());

        assertTrue(service.getPerformanceTrend(owner).weeks().isEmpty());
        verify(workoutRepository, never()).findPerformanceSessions(any(), any(), any());
    }

    @Test
    @DisplayName("An owner cannot read the fitness trend of someone else's horse")
    void testGetFitnessTrend_OtherOwner_Denied() {
        AuthenticatedUser owner = new AuthenticatedUser(5L, "owner@example.com", "HORSE_OWNER");
        when(horseRepository.findById(1L)).thenReturn(Optional.of(horse(1L, 99L)));

        assertThrows(AccessDeniedException.class, () -> service.getFitnessTrend(1L, null, null, owner));
        verifyNoInteractions(workoutRepository);
    }

    @Test
    @DisplayName("An owner can read the fitness trend of their own horse")
    void testGetFitnessTrend_OwnHorse_Allowed() {
        AuthenticatedUser owner = new AuthenticatedUser(5L, "owner@example.com", "HORSE_OWNER");
        when(horseRepository.findById(1L)).thenReturn(Optional.of(horse(1L, 5L)));
        when(workoutRepository.findFitnessTrend(eq(1L), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());

        assertTrue(service.getFitnessTrend(1L, null, null, owner).isEmpty());
    }

    @Test
    @DisplayName("A head trainer can only read horses in the areas they manage")
    void testGetFitnessTrend_TrainerOutsideArea_Denied() {
        AuthenticatedUser trainer = new AuthenticatedUser(9L, "trainer@example.com", "HEAD_TRAINER");
        when(horseRepository.findById(1L)).thenReturn(Optional.of(horse(1L, 5L)));
        when(planRepository.existsHorseInTrainerArea(1L, 9L)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.getFitnessTrend(1L, null, null, trainer));
        verifyNoInteractions(workoutRepository);
    }

    @Test
    @DisplayName("The schedule of a horse is read for the requested days, cancelled sessions included")
    void testGetHorseSchedule_ReturnsTheRequestedRange() {
        LocalDate from = LocalDate.of(2026, 10, 27);
        LocalDate to = LocalDate.of(2026, 11, 2);
        List<HorseScheduleItemResponse> rows = List.of(new HorseScheduleItemResponse(
                7L, from, java.time.LocalTime.of(6, 0), java.time.LocalTime.of(7, 0), "Gallop",
                LotStatus.SCHEDULED, WorkoutStatus.CANCELLED));
        when(horseRepository.findById(1L)).thenReturn(Optional.of(horse(1L, 5L)));
        when(workoutRepository.findHorseSchedule(1L, from, to)).thenReturn(rows);

        assertEquals(rows, service.getHorseSchedule(1L, from, to, manager));
    }

    @Test
    @DisplayName("A schedule range that ends before it starts is rejected")
    void testGetHorseSchedule_BackwardsRange_Rejected() {
        when(horseRepository.findById(1L)).thenReturn(Optional.of(horse(1L, 5L)));

        assertThrows(IllegalArgumentException.class,
                () -> service.getHorseSchedule(1L, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 10, 27), manager));
        verifyNoInteractions(workoutRepository);
    }

    @Test
    @DisplayName("An owner cannot read the schedule of someone else's horse")
    void testGetHorseSchedule_OtherOwner_Denied() {
        AuthenticatedUser owner = new AuthenticatedUser(5L, "owner@example.com", "HORSE_OWNER");
        when(horseRepository.findById(1L)).thenReturn(Optional.of(horse(1L, 99L)));

        assertThrows(AccessDeniedException.class,
                () -> service.getHorseSchedule(1L, LocalDate.of(2026, 10, 27), LocalDate.of(2026, 11, 2), owner));
        verifyNoInteractions(workoutRepository);
    }

    @Test
    @DisplayName("An unknown horse is not found")
    void testGetFitnessTrend_UnknownHorse() {
        when(horseRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.getFitnessTrend(404L, null, null, manager));
    }

    // =================================================================
    // Alerts
    // =================================================================

    @Test
    @DisplayName("7.2: raises alerts for max heart rate > 220, recovery > 100, and falling performance")
    void testGetHorseAlerts_MultipleRulesTriggered() {
        Long horseId = 2L;
        LocalDate today = LocalDate.now();
        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse(horseId, 5L)));

        // Newest first: max HR 225 (>220), recovery 110 (>100), rating falling 8 -> 6 -> 4
        when(workoutRepository.findRecentCompletedWorkouts(eq(List.of(horseId)), any(LocalDate.class)))
                .thenReturn(List.of(
                        workout(horseId, today.minusDays(1), 225, 110, 4),
                        workout(horseId, today.minusDays(3), 200, 90, 6),
                        workout(horseId, today.minusDays(5), 195, 85, 8)));

        List<HorseAlertResponse> alerts = service.getHorseAlerts(horseId, manager);

        assertEquals(3, alerts.size());
        assertTrue(alerts.stream().anyMatch(a -> "MAX_HEART_RATE_EXCEEDED".equals(a.getRuleCode())),
                "Max heart rate alert expected");
        assertTrue(alerts.stream().anyMatch(a -> "POOR_RECOVERY_HEART_RATE".equals(a.getRuleCode())),
                "Poor recovery alert expected");
        assertTrue(alerts.stream().anyMatch(a -> "CONSECUTIVE_PERFORMANCE_DROP".equals(a.getRuleCode())),
                "Consecutive performance drop alert expected");
    }

    @Test
    @DisplayName("No alert when every value is inside its safe range")
    void testGetHorseAlerts_NothingTriggered() {
        Long horseId = 2L;
        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse(horseId, 5L)));
        when(workoutRepository.findRecentCompletedWorkouts(eq(List.of(horseId)), any(LocalDate.class)))
                .thenReturn(List.of(
                        workout(horseId, LocalDate.now().minusDays(1), 200, 90, 8),
                        workout(horseId, LocalDate.now().minusDays(3), 205, 95, 7),
                        workout(horseId, LocalDate.now().minusDays(5), 198, 88, 7)));

        assertTrue(service.getHorseAlerts(horseId, manager).isEmpty());
    }

    @Test
    @DisplayName("The performance-drop alert needs 3 rated workouts that fall strictly")
    void testGetHorseAlerts_PerformanceDropNeedsThreeFallingRatings() {
        Long horseId = 2L;
        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse(horseId, 5L)));
        // 6 -> 6 -> 8 is not strictly falling; only two ratings in the second case
        when(workoutRepository.findRecentCompletedWorkouts(eq(List.of(horseId)), any(LocalDate.class)))
                .thenReturn(List.of(
                        workout(horseId, LocalDate.now().minusDays(1), 150, 60, 6),
                        workout(horseId, LocalDate.now().minusDays(2), 150, 60, 6),
                        workout(horseId, LocalDate.now().minusDays(3), 150, 60, 8)));

        assertTrue(service.getHorseAlerts(horseId, manager).isEmpty());
    }

    // =================================================================
    // Dashboard
    // =================================================================

    @Test
    @DisplayName("7.3: trainer dashboard with plan progress, ratings and alerts, loaded with a constant number of queries")
    void testGetTrainerDashboard_Success() {
        Long trainerId = 9L;
        AuthenticatedUser trainer = new AuthenticatedUser(trainerId, "trainer@example.com", "HEAD_TRAINER");
        LocalDate today = LocalDate.now();

        when(planRepository.findDashboardHorses(trainerId)).thenReturn(List.of(
                new DashboardHorseRow(50L, "Wind God", "Thoroughbred", "A-01"),
                new DashboardHorseRow(51L, "Idle Horse", "Arabian", "A-02")));
        when(planRepository.findDashboardPlans(List.of(50L, 51L))).thenReturn(List.of(
                new DashboardPlanRow(700L, 50L, TrainingPlanStatus.COMPLETED, today.minusDays(60), today.minusDays(30),
                        "Old course", 10, 10L, 0L),
                new DashboardPlanRow(701L, 50L, TrainingPlanStatus.ACTIVE, today.minusDays(5), today.plusDays(20),
                        "Sprint course", 10, 1L, 0L)));
        when(workoutRepository.findRecentCompletedWorkouts(eq(List.of(50L, 51L)), any(LocalDate.class)))
                .thenReturn(List.of(
                        workout(50L, today, null, null, 8),
                        workout(50L, today.minusDays(2), null, null, 6)));
        HorseSpecialtyResponse noData = new HorseSpecialtyResponse(3, List.of(), null, List.of());
        when(specialtyService.getSpecialties(List.of(50L, 51L))).thenReturn(Map.of(50L, noData, 51L, noData));

        List<DashboardHorseResponse> dashboard = service.getDashboard(trainer);

        assertEquals(2, dashboard.size());

        DashboardHorseResponse active = dashboard.get(0);
        assertEquals("Wind God", active.getHorseName());
        assertEquals("A-01", active.getStallCode());
        assertEquals("ACTIVE", active.getPlanStatus(), "The ACTIVE plan wins over the COMPLETED one");
        assertEquals("Sprint course", active.getCourseName());
        assertEquals(1, active.getCompletedSessions());
        assertEquals(10, active.getTotalSessions());
        assertEquals(10.0, active.getProgressPercent());
        assertEquals(8, active.getLatestPerformanceRating());
        assertEquals(7.0, active.getAvgPerformanceRating30d());
        assertEquals(0, active.getAlertsCount());
        assertSame(noData, active.getSpecialty(), "The specialty comes from the shared specialty service");

        DashboardHorseResponse idle = dashboard.get(1);
        assertEquals("NONE", idle.getPlanStatus());
        assertEquals("No plan yet", idle.getCourseName());
        assertEquals(0.0, idle.getProgressPercent());
        assertEquals(0, idle.getAlertsCount(), "No workouts, no alerts");
        assertEquals(List.of(), idle.getAlertTitles());

        // Cố định số truy vấn dù có bao nhiêu ngựa (a fixed number of queries no matter how many horses)
        verify(planRepository, times(1)).findDashboardHorses(trainerId);
        verify(planRepository, times(1)).findDashboardPlans(List.of(50L, 51L));
        verify(workoutRepository, times(1)).findRecentCompletedWorkouts(any(), any());
        verify(specialtyService, times(1)).getSpecialties(any());
    }

    @Test
    @DisplayName("Dashboard plan: chosen by date, not by creation order")
    void testPickCurrentPlanUsesDates() {
        LocalDate today = LocalDate.now();
        // Created first (lower id) but starts later; created second but starts sooner.
        DashboardPlanRow later = new DashboardPlanRow(1L, 7L, TrainingPlanStatus.UPCOMING, today.plusDays(20),
                today.plusDays(40), "Later course", 6, 0L, 0L);
        DashboardPlanRow sooner = new DashboardPlanRow(2L, 7L, TrainingPlanStatus.UPCOMING, today.plusDays(3),
                today.plusDays(15), "Sooner course", 6, 0L, 0L);
        assertSame(sooner, HorsePerformanceService.pickCurrentPlan(List.of(later, sooner)),
                "The upcoming plan that starts first wins");

        DashboardPlanRow active = new DashboardPlanRow(3L, 7L, TrainingPlanStatus.ACTIVE, today.minusDays(2),
                today.plusDays(1), "Running course", 6, 2L, 0L);
        assertSame(active, HorsePerformanceService.pickCurrentPlan(List.of(later, sooner, active)),
                "A running plan wins over upcoming ones");

        DashboardPlanRow olderDone = new DashboardPlanRow(4L, 7L, TrainingPlanStatus.COMPLETED, today.minusDays(60),
                today.minusDays(40), "Older course", 6, 6L, 0L);
        DashboardPlanRow recentDone = new DashboardPlanRow(5L, 7L, TrainingPlanStatus.COMPLETED, today.minusDays(30),
                today.minusDays(5), "Recent course", 6, 6L, 0L);
        assertSame(recentDone, HorsePerformanceService.pickCurrentPlan(List.of(olderDone, recentDone)),
                "Without running or upcoming plans, the one that ended most recently wins");

        assertNull(HorsePerformanceService.pickCurrentPlan(List.of()));
    }

    @Test
    @DisplayName("Scope: an owner sees own horses, a trainer the area horses, the manager every horse")
    void testDashboardScopeFollowsTheRole() {
        DashboardHorseRow own = new DashboardHorseRow(1L, "Own", null, null);
        DashboardHorseRow area = new DashboardHorseRow(2L, "Area", null, "A-01");
        DashboardHorseRow farm = new DashboardHorseRow(3L, "Farm", null, "B-01");
        when(planRepository.findDashboardHorsesOfOwner(5L)).thenReturn(List.of(own));
        when(planRepository.findDashboardHorses(9L)).thenReturn(List.of(area));
        when(planRepository.findAllDashboardHorses()).thenReturn(List.of(own, area, farm));
        HorseScopeService scope = new HorseScopeService(planRepository);

        assertEquals(List.of(own), scope.horsesOf(new AuthenticatedUser(5L, "o@example.com", "HORSE_OWNER")));
        assertEquals(List.of(area), scope.horsesOf(new AuthenticatedUser(9L, "t@example.com", "HEAD_TRAINER")));
        assertEquals(List.of(1L, 2L, 3L), scope.horseIdsOf(new AuthenticatedUser(1L, "m@example.com", "CLUB_MANAGER")));
        assertEquals(List.of(1L, 2L, 3L), scope.horseIdsOf(new AuthenticatedUser(2L, "v@example.com", "VETERINARIAN")));
    }

    @Test
    @DisplayName("An owner dashboard is built from the owner's horses only")
    void testGetDashboard_OwnerScope() {
        AuthenticatedUser owner = new AuthenticatedUser(5L, "owner@example.com", "HORSE_OWNER");
        when(planRepository.findDashboardHorsesOfOwner(5L)).thenReturn(List.of(
                new DashboardHorseRow(60L, "Mine", "Arabian", null)));
        when(planRepository.findDashboardPlans(List.of(60L))).thenReturn(List.of());
        when(workoutRepository.findRecentCompletedWorkouts(eq(List.of(60L)), any(LocalDate.class))).thenReturn(List.of());
        when(specialtyService.getSpecialties(List.of(60L))).thenReturn(Map.of());

        List<DashboardHorseResponse> dashboard = service.getDashboard(owner);

        assertEquals(1, dashboard.size());
        assertEquals("Mine", dashboard.get(0).getHorseName());
        verify(planRepository, never()).findDashboardHorses(any());
        verify(planRepository, never()).findAllDashboardHorses();
    }

    @Test
    @DisplayName("Dashboard progress leaves cancelled sessions out, like the Plan page")
    void testDashboardProgressIgnoresCancelledSessions() {
        AuthenticatedUser owner = new AuthenticatedUser(5L, "owner@example.com", "HORSE_OWNER");
        LocalDate today = LocalDate.now();
        when(planRepository.findDashboardHorsesOfOwner(5L)).thenReturn(List.of(
                new DashboardHorseRow(60L, "Mine", "Arabian", null)));
        // 6-session course, 2 cancelled, 4 held: the course is done in full.
        when(planRepository.findDashboardPlans(List.of(60L))).thenReturn(List.of(
                new DashboardPlanRow(800L, 60L, TrainingPlanStatus.COMPLETED, today.minusDays(20), today.minusDays(2),
                        "Six sessions", 6, 4L, 2L)));
        when(workoutRepository.findRecentCompletedWorkouts(eq(List.of(60L)), any(LocalDate.class))).thenReturn(List.of());
        when(specialtyService.getSpecialties(List.of(60L))).thenReturn(Map.of());

        DashboardHorseResponse row = service.getDashboard(owner).get(0);

        assertEquals(4, row.getCompletedSessions());
        assertEquals(2, row.getCancelledSessions());
        assertEquals(100.0, row.getProgressPercent());
    }

    @Test
    @DisplayName("A plan cancelled part-way keeps its cancelled sessions in the progress: 3 held of 6 is 50%")
    void testDashboardProgressOfCancelledPlan() {
        AuthenticatedUser owner = new AuthenticatedUser(5L, "owner@example.com", "HORSE_OWNER");
        LocalDate today = LocalDate.now();
        when(planRepository.findDashboardHorsesOfOwner(5L)).thenReturn(List.of(
                new DashboardHorseRow(60L, "Mine", "Arabian", null)));
        when(planRepository.findDashboardPlans(List.of(60L))).thenReturn(List.of(
                new DashboardPlanRow(801L, 60L, TrainingPlanStatus.CANCELLED, today.minusDays(10), today.plusDays(5),
                        "Six sessions", 6, 3L, 3L)));
        when(workoutRepository.findRecentCompletedWorkouts(eq(List.of(60L)), any(LocalDate.class))).thenReturn(List.of());
        when(specialtyService.getSpecialties(List.of(60L))).thenReturn(Map.of());

        assertEquals(50.0, service.getDashboard(owner).get(0).getProgressPercent());
    }

    @Test
    @DisplayName("A trainer with no horses gets an empty dashboard and no further queries")
    void testGetTrainerDashboard_NoHorses() {
        AuthenticatedUser trainer = new AuthenticatedUser(9L, "trainer@example.com", "HEAD_TRAINER");
        when(planRepository.findDashboardHorses(9L)).thenReturn(List.of());

        assertTrue(service.getDashboard(trainer).isEmpty());
        verifyNoInteractions(workoutRepository);
        verify(planRepository, never()).findDashboardPlans(any());
    }
}
