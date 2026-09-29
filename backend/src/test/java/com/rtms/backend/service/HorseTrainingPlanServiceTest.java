package com.rtms.backend.service;

import com.rtms.backend.dto.*;
import com.rtms.backend.entity.*;
import com.rtms.backend.enums.TrainingDay;
import com.rtms.backend.enums.TrainingPlanStatus;
import com.rtms.backend.enums.WorkoutStatus;
import com.rtms.backend.enums.WorkoutType;
import com.rtms.backend.repository.*;
import com.rtms.backend.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HorseTrainingPlanServiceTest {

    @Mock
    private HorseTrainingPlanRepository planRepository;

    @Mock
    private TrainingWorkoutRepository workoutRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseSubjectRepository courseSubjectRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private HorseRepository horseRepository;

    @Mock
    private InjuryRecordService injuryRecordService;

    @Mock
    private TrainingLotService lotService;

    @Mock
    private TrainingLotRepository lotRepository;

    @Mock
    private StableStallRepository stableStallRepository;

    @Mock
    private AreaRepository areaRepository;

    @Mock
    private GroomIncidentReportRepository incidentReportRepository;

    private HorseTrainingPlanService planService;

    private final AuthenticatedUser trainerUser = new AuthenticatedUser(9L, "trainer@example.com", "HEAD_TRAINER");

    @BeforeEach
    void setUp() {
        planService = new HorseTrainingPlanService(
                planRepository,
                workoutRepository,
                courseRepository,
                courseSubjectRepository,
                subjectRepository,
                horseRepository,
                injuryRecordService,
                lotService,
                lotRepository,
                stableStallRepository,
                areaRepository,
                incidentReportRepository
        );
    }

    @Test
    @DisplayName("1. Lập kế hoạch huấn luyện thành công cho nhóm 2 chiến mã")
    void testCreatePlan_Success() {
        // Arrange
        Long courseId = 100L;
        LocalDate startDate = LocalDate.of(2026, 10, 5); // Monday
        List<TrainingDay> days = List.of(TrainingDay.MONDAY, TrainingDay.WEDNESDAY);

        CreateHorseTrainingPlanRequest request = new CreateHorseTrainingPlanRequest();
        request.setCourseId(courseId);
        request.setStartDate(startDate);
        request.setTrainingDays(days);

        HorseEnrollmentRequest en1 = new HorseEnrollmentRequest();
        en1.setHorseId(1L);
        HorseEnrollmentRequest en2 = new HorseEnrollmentRequest();
        en2.setHorseId(2L);
        request.setHorses(List.of(en1, en2));

        Course course = new Course();
        course.setId(courseId);
        course.setName("Cơ bản 2 buổi");
        course.setTotalSessions(2);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        CourseSubject cs1 = new CourseSubject();
        cs1.setId(10L);
        cs1.setCourseId(courseId);
        cs1.setSubjectId(50L);
        cs1.setOrderIndex(1);
        when(courseSubjectRepository.findByCourseIdOrderByOrderIndexAsc(courseId)).thenReturn(List.of(cs1));

        Subject sub1 = new Subject();
        sub1.setId(50L);
        sub1.setName("Khởi động 30'");
        sub1.setWorkoutType(WorkoutType.REGULAR);
        when(subjectRepository.findById(50L)).thenReturn(Optional.of(sub1));

        // Horse 1
        Horse h1 = new Horse();
        h1.setId(1L);
        h1.setName("Bạch Long Mã");
        h1.setCurrentStallId(10L);
        when(horseRepository.findById(1L)).thenReturn(Optional.of(h1));

        // Horse 2
        Horse h2 = new Horse();
        h2.setId(2L);
        h2.setName("Xích Thố");
        h2.setCurrentStallId(11L);
        when(horseRepository.findById(2L)).thenReturn(Optional.of(h2));

        // Locks
        when(injuryRecordService.getTrainingLockStatus(1L))
                .thenReturn(new TrainingLockStatusResponse(1L, "ELIGIBLE", false));
        when(injuryRecordService.getTrainingLockStatus(2L))
                .thenReturn(new TrainingLockStatusResponse(2L, "ELIGIBLE", false));

        // Stalls & Areas
        StableStall s1 = new StableStall();
        s1.setId(10L);
        s1.setAreaId(200L);
        s1.setGroomId(4L);
        s1.setStallCode("A1");
        when(stableStallRepository.findById(10L)).thenReturn(Optional.of(s1));

        StableStall s2 = new StableStall();
        s2.setId(11L);
        s2.setAreaId(200L);
        s2.setGroomId(4L);
        s2.setStallCode("A2");
        when(stableStallRepository.findById(11L)).thenReturn(Optional.of(s2));

        Area area = new Area();
        area.setId(200L);
        area.setCode("A");
        area.setTrainerId(9L); // Trainer phụ trách
        when(areaRepository.findById(200L)).thenReturn(Optional.of(area));

        // Check ongoing plans -> rỗng
        when(planRepository.findByHorseIdAndStatusInOrderByEndDateDesc(anyLong(), anyList()))
                .thenReturn(Collections.emptyList());

        // Save plan
        when(planRepository.saveAndFlush(any(HorseTrainingPlan.class))).thenAnswer(inv -> {
            HorseTrainingPlan p = inv.getArgument(0);
            p.setId(p.getHorseId() + 1000L);
            return p;
        });

        // Lot
        TrainingLot mockLot = new TrainingLot();
        mockLot.setId(500L);
        mockLot.setLotDate(startDate);
        mockLot.setSubjectId(50L);
        mockLot.setStartTime(LocalTime.of(6, 0));
        mockLot.setEndTime(LocalTime.of(6, 30));
        when(lotService.findOrCreateLot(eq(9L), any(LocalDate.class), eq(sub1), eq(4L)))
                .thenReturn(mockLot);

        // Save workout
        when(workoutRepository.saveAndFlush(any(TrainingWorkout.class))).thenAnswer(inv -> {
            TrainingWorkout w = inv.getArgument(0);
            w.setId(new Random().nextLong(1000, 9999));
            return w;
        });

        // Act
        List<HorseTrainingPlanDetailResponse> responses = planService.createPlan(request, trainerUser);

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size(), "Phải trả về 2 response cho 2 chiến mã");
        assertEquals(1L, responses.get(0).getPlan().getHorseId());
        assertEquals(2L, responses.get(1).getPlan().getHorseId());
        assertEquals(2, responses.get(0).getWorkouts().size(), "Tổng 2 buổi tập");
        verify(planRepository, times(2)).saveAndFlush(any(HorseTrainingPlan.class));
        verify(workoutRepository, times(4)).saveAndFlush(any(TrainingWorkout.class)); // 2 horses * 2 sessions
    }

    @Test
    @DisplayName("2. Kiểm tra lỗi khi danh sách ngựa rỗng")
    void testCreatePlan_EmptyHorses_ThrowsIllegalArgument() {
        CreateHorseTrainingPlanRequest req = new CreateHorseTrainingPlanRequest();
        req.setHorses(Collections.emptyList());
        req.setCourseId(1L);
        req.setStartDate(LocalDate.now());
        req.setTrainingDays(List.of(TrainingDay.MONDAY));

        assertThrows(IllegalArgumentException.class, () -> planService.createPlan(req, trainerUser));
    }

    @Test
    @DisplayName("3. Kiểm tra lỗi khi thiếu khoá học hoặc ngày bắt đầu")
    void testCreatePlan_MissingCourseOrDate_ThrowsIllegalArgument() {
        CreateHorseTrainingPlanRequest req = new CreateHorseTrainingPlanRequest();
        HorseEnrollmentRequest en = new HorseEnrollmentRequest();
        en.setHorseId(1L);
        req.setHorses(List.of(en));

        // Thiếu courseId
        req.setStartDate(LocalDate.now());
        req.setTrainingDays(List.of(TrainingDay.MONDAY));
        assertThrows(IllegalArgumentException.class, () -> planService.createPlan(req, trainerUser));

        // Thiếu startDate
        req.setCourseId(1L);
        req.setStartDate(null);
        assertThrows(IllegalArgumentException.class, () -> planService.createPlan(req, trainerUser));

        // Thiếu trainingDays
        req.setStartDate(LocalDate.now());
        req.setTrainingDays(null);
        assertThrows(IllegalArgumentException.class, () -> planService.createPlan(req, trainerUser));
    }

    @Test
    @DisplayName("4. Kiểm tra lỗi khi ghi danh trùng 1 ngựa trong cùng request")
    void testCreatePlan_DuplicateHorseInRequest_ThrowsIllegalArgument() {
        CreateHorseTrainingPlanRequest req = new CreateHorseTrainingPlanRequest();
        req.setCourseId(10L);
        req.setStartDate(LocalDate.now());
        req.setTrainingDays(List.of(TrainingDay.MONDAY));

        HorseEnrollmentRequest en1 = new HorseEnrollmentRequest();
        en1.setHorseId(5L);
        HorseEnrollmentRequest en2 = new HorseEnrollmentRequest();
        en2.setHorseId(5L); // Trùng
        req.setHorses(List.of(en1, en2));

        Course course = new Course();
        course.setId(10L);
        course.setTotalSessions(1);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        CourseSubject cs = new CourseSubject();
        cs.setSubjectId(1L);
        when(courseSubjectRepository.findByCourseIdOrderByOrderIndexAsc(10L)).thenReturn(List.of(cs));

        Subject sub = new Subject();
        sub.setId(1L);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(sub));

        Horse horse = new Horse();
        horse.setId(5L);
        horse.setName("Chiến Mã 5");
        horse.setCurrentStallId(10L);
        when(horseRepository.findById(5L)).thenReturn(Optional.of(horse));
        when(injuryRecordService.getTrainingLockStatus(5L))
                .thenReturn(new TrainingLockStatusResponse(5L, "ELIGIBLE", false));

        StableStall stall = new StableStall();
        stall.setId(10L);
        stall.setAreaId(1L);
        stall.setGroomId(4L);
        when(stableStallRepository.findById(10L)).thenReturn(Optional.of(stall));

        Area area = new Area();
        area.setId(1L);
        area.setTrainerId(9L);
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> planService.createPlan(req, trainerUser));
        assertTrue(ex.getMessage().contains("bị ghi danh trùng"));
    }

    @Test
    @DisplayName("5. BR-05: Chặn tạo kế hoạch nếu chiến mã đang bị KHOÁ HUẤN LUYỆN (chấn thương)")
    void testCreatePlan_HorseLockedDueToInjury_ThrowsIllegalStateException() {
        CreateHorseTrainingPlanRequest req = new CreateHorseTrainingPlanRequest();
        req.setCourseId(10L);
        req.setStartDate(LocalDate.now());
        req.setTrainingDays(List.of(TrainingDay.MONDAY));

        HorseEnrollmentRequest en = new HorseEnrollmentRequest();
        en.setHorseId(7L);
        req.setHorses(List.of(en));

        Course course = new Course();
        course.setId(10L);
        course.setTotalSessions(1);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        CourseSubject cs = new CourseSubject();
        cs.setSubjectId(1L);
        when(courseSubjectRepository.findByCourseIdOrderByOrderIndexAsc(10L)).thenReturn(List.of(cs));

        Subject sub = new Subject();
        sub.setId(1L);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(sub));

        Horse horse = new Horse();
        horse.setId(7L);
        horse.setName("Thương Mã");
        when(horseRepository.findById(7L)).thenReturn(Optional.of(horse));

        // Khóa do chấn thương
        when(injuryRecordService.getTrainingLockStatus(7L))
                .thenReturn(new TrainingLockStatusResponse(7L, "INJURED", true));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> planService.createPlan(req, trainerUser));
        assertTrue(ex.getMessage().contains("KHOÁ HUẤN LUYỆN"));
    }

    @Test
    @DisplayName("6. Chặn tạo kế hoạch nếu ngựa chưa được xếp chuồng")
    void testCreatePlan_HorseNotAssignedToStall_ThrowsIllegalStateException() {
        CreateHorseTrainingPlanRequest req = new CreateHorseTrainingPlanRequest();
        req.setCourseId(10L);
        req.setStartDate(LocalDate.now());
        req.setTrainingDays(List.of(TrainingDay.MONDAY));

        HorseEnrollmentRequest en = new HorseEnrollmentRequest();
        en.setHorseId(3L);
        req.setHorses(List.of(en));

        Course course = new Course();
        course.setId(10L);
        course.setTotalSessions(1);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        CourseSubject cs = new CourseSubject();
        cs.setSubjectId(1L);
        when(courseSubjectRepository.findByCourseIdOrderByOrderIndexAsc(10L)).thenReturn(List.of(cs));

        Subject sub = new Subject();
        sub.setId(1L);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(sub));

        Horse horse = new Horse();
        horse.setId(3L);
        horse.setName("Ngựa Tự Do");
        horse.setCurrentStallId(null); // Chưa xếp chuồng
        when(horseRepository.findById(3L)).thenReturn(Optional.of(horse));
        when(injuryRecordService.getTrainingLockStatus(3L))
                .thenReturn(new TrainingLockStatusResponse(3L, "ELIGIBLE", false));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> planService.createPlan(req, trainerUser));
        assertTrue(ex.getMessage().contains("chưa được xếp chuồng"));
    }

    @Test
    @DisplayName("7. Quyền hạn Trainer: Chặn nếu chiến mã ở khu vực Trainer khác phụ trách")
    void testCreatePlan_HorseInOtherTrainerArea_ThrowsAccessDenied() {
        CreateHorseTrainingPlanRequest req = new CreateHorseTrainingPlanRequest();
        req.setCourseId(10L);
        req.setStartDate(LocalDate.now());
        req.setTrainingDays(List.of(TrainingDay.MONDAY));

        HorseEnrollmentRequest en = new HorseEnrollmentRequest();
        en.setHorseId(3L);
        req.setHorses(List.of(en));

        Course course = new Course();
        course.setId(10L);
        course.setTotalSessions(1);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        CourseSubject cs = new CourseSubject();
        cs.setSubjectId(1L);
        when(courseSubjectRepository.findByCourseIdOrderByOrderIndexAsc(10L)).thenReturn(List.of(cs));

        Subject sub = new Subject();
        sub.setId(1L);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(sub));

        Horse horse = new Horse();
        horse.setId(3L);
        horse.setName("Ngựa Khu B");
        horse.setCurrentStallId(20L);
        when(horseRepository.findById(3L)).thenReturn(Optional.of(horse));
        when(injuryRecordService.getTrainingLockStatus(3L))
                .thenReturn(new TrainingLockStatusResponse(3L, "ELIGIBLE", false));

        StableStall stall = new StableStall();
        stall.setId(20L);
        stall.setAreaId(88L);
        stall.setGroomId(4L);
        when(stableStallRepository.findById(20L)).thenReturn(Optional.of(stall));

        Area area = new Area();
        area.setId(88L);
        area.setCode("B");
        area.setTrainerId(999L); // Trainer khác (trainerUser id là 9L)
        when(areaRepository.findById(88L)).thenReturn(Optional.of(area));

        assertThrows(AccessDeniedException.class, () -> planService.createPlan(req, trainerUser));
    }

    @Test
    @DisplayName("8. BR-01: Chặn kế hoạch nếu trùng khoảng thời gian với kế hoạch đang chạy (ACTIVE/UPCOMING)")
    void testCreatePlan_OverlappingPlan_ThrowsIllegalStateException() {
        LocalDate start = LocalDate.of(2026, 10, 5); // T2
        CreateHorseTrainingPlanRequest req = new CreateHorseTrainingPlanRequest();
        req.setCourseId(10L);
        req.setStartDate(start);
        req.setTrainingDays(List.of(TrainingDay.MONDAY, TrainingDay.WEDNESDAY));

        HorseEnrollmentRequest en = new HorseEnrollmentRequest();
        en.setHorseId(1L);
        req.setHorses(List.of(en));

        Course course = new Course();
        course.setId(10L);
        course.setTotalSessions(2);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        CourseSubject cs = new CourseSubject();
        cs.setSubjectId(1L);
        when(courseSubjectRepository.findByCourseIdOrderByOrderIndexAsc(10L)).thenReturn(List.of(cs));

        Subject sub = new Subject();
        sub.setId(1L);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(sub));

        Horse horse = new Horse();
        horse.setId(1L);
        horse.setName("Chiến Mã 1");
        horse.setCurrentStallId(10L);
        when(horseRepository.findById(1L)).thenReturn(Optional.of(horse));
        when(injuryRecordService.getTrainingLockStatus(1L))
                .thenReturn(new TrainingLockStatusResponse(1L, "ELIGIBLE", false));

        StableStall stall = new StableStall();
        stall.setId(10L);
        stall.setAreaId(1L);
        stall.setGroomId(4L);
        when(stableStallRepository.findById(10L)).thenReturn(Optional.of(stall));

        Area area = new Area();
        area.setId(1L);
        area.setTrainerId(9L);
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area));

        // Kế hoạch đang chạy từ 2026-10-01 đến 2026-10-15 (trùng với 05/10 - 07/10)
        HorseTrainingPlan existing = new HorseTrainingPlan();
        existing.setStartDate(LocalDate.of(2026, 10, 1));
        existing.setEndDate(LocalDate.of(2026, 10, 15));
        existing.setStatus(TrainingPlanStatus.ACTIVE);
        when(planRepository.findByHorseIdAndStatusInOrderByEndDateDesc(eq(1L), anyList()))
                .thenReturn(List.of(existing));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> planService.createPlan(req, trainerUser));
        assertTrue(ex.getMessage().contains("Xung đột lịch"));
    }

    @Test
    @DisplayName("9. Trainer đóng buổi tập thành công: ghi nhận chỉ số, điểm phong độ và nhận xét")
    void testCompleteWorkout_Success() {
        Long workoutId = 300L;
        CompleteWorkoutRequest req = new CompleteWorkoutRequest();
        req.setPerformanceRating(9);
        req.setActualDistanceMeters(java.math.BigDecimal.valueOf(1200));
        req.setActualDurationMinutes(java.math.BigDecimal.valueOf(45));
        req.setTopSpeedKmh(java.math.BigDecimal.valueOf(58.5));
        req.setAverageHeartRate(140);
        req.setTrainerFeedback("Chiến mã chạy rất ổn định, giữ sức tốt");

        TrainingWorkout workout = new TrainingWorkout();
        workout.setId(workoutId);
        workout.setPlanId(50L);
        workout.setLotId(600L);
        workout.setStatus(WorkoutStatus.SCHEDULED);

        HorseTrainingPlan plan = new HorseTrainingPlan();
        plan.setId(50L);
        plan.setTrainerId(9L); // Cùng Trainer
        plan.setStatus(TrainingPlanStatus.ACTIVE);

        when(workoutRepository.findById(workoutId)).thenReturn(Optional.of(workout));
        when(planRepository.findById(50L)).thenReturn(Optional.of(plan));
        when(workoutRepository.saveAndFlush(any(TrainingWorkout.class))).thenAnswer(inv -> inv.getArgument(0));
        when(workoutRepository.countByPlanIdAndStatusNotIn(eq(50L), anyList())).thenReturn(2L); // Vẫn còn 2 buổi

        TrainingLot lot = new TrainingLot();
        lot.setId(600L);
        lot.setSubjectId(77L);
        when(lotRepository.findById(600L)).thenReturn(Optional.of(lot));

        Subject subject = new Subject();
        subject.setId(77L);
        subject.setName("Bài tập tăng tốc");
        subject.setWorkoutType(WorkoutType.REGULAR);
        when(subjectRepository.findById(77L)).thenReturn(Optional.of(subject));

        // Act
        PlanWorkoutItemResponse response = planService.completeWorkout(workoutId, req, trainerUser);

        // Assert
        assertNotNull(response);
        assertEquals(WorkoutStatus.COMPLETED, workout.getStatus());
        assertEquals(9, workout.getPerformanceRating());
        assertEquals("Chiến mã chạy rất ổn định, giữ sức tốt", workout.getTrainerFeedback());
        assertEquals(java.math.BigDecimal.valueOf(58.5), workout.getTopSpeedKmh());
    }

    @Test
    @DisplayName("10. Trainer khác không được phép đóng buổi tập của Trainer phụ trách")
    void testCompleteWorkout_WrongTrainer_ThrowsAccessDenied() {
        Long workoutId = 300L;
        CompleteWorkoutRequest req = new CompleteWorkoutRequest();
        req.setPerformanceRating(8);

        TrainingWorkout workout = new TrainingWorkout();
        workout.setId(workoutId);
        workout.setPlanId(50L);

        HorseTrainingPlan plan = new HorseTrainingPlan();
        plan.setId(50L);
        plan.setTrainerId(888L); // Khác trainerUser (9L)

        when(workoutRepository.findById(workoutId)).thenReturn(Optional.of(workout));
        when(planRepository.findById(50L)).thenReturn(Optional.of(plan));

        assertThrows(AccessDeniedException.class,
                () -> planService.completeWorkout(workoutId, req, trainerUser));
    }

    @Test
    @DisplayName("11. Không thể đóng buổi tập đã bị huỷ")
    void testCompleteWorkout_CancelledWorkout_ThrowsIllegalStateException() {
        Long workoutId = 300L;
        CompleteWorkoutRequest req = new CompleteWorkoutRequest();

        TrainingWorkout workout = new TrainingWorkout();
        workout.setId(workoutId);
        workout.setPlanId(50L);
        workout.setStatus(WorkoutStatus.CANCELLED);

        HorseTrainingPlan plan = new HorseTrainingPlan();
        plan.setId(50L);
        plan.setTrainerId(9L);

        when(workoutRepository.findById(workoutId)).thenReturn(Optional.of(workout));
        when(planRepository.findById(50L)).thenReturn(Optional.of(plan));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> planService.completeWorkout(workoutId, req, trainerUser));
        assertTrue(ex.getMessage().contains("đã bị huỷ"));
    }

    @Test
    @DisplayName("12. Kiểm tra điểm phong độ không hợp lệ (< 1 hoặc > 10)")
    void testCompleteWorkout_InvalidRating_ThrowsIllegalArgument() {
        Long workoutId = 300L;
        CompleteWorkoutRequest req = new CompleteWorkoutRequest();
        req.setPerformanceRating(15); // > 10

        TrainingWorkout workout = new TrainingWorkout();
        workout.setId(workoutId);
        workout.setPlanId(50L);
        workout.setStatus(WorkoutStatus.SCHEDULED);

        HorseTrainingPlan plan = new HorseTrainingPlan();
        plan.setId(50L);
        plan.setTrainerId(9L);

        when(workoutRepository.findById(workoutId)).thenReturn(Optional.of(workout));
        when(planRepository.findById(50L)).thenReturn(Optional.of(plan));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> planService.completeWorkout(workoutId, req, trainerUser));
        assertTrue(ex.getMessage().contains("từ 1 đến 10"));
    }

    @Test
    @DisplayName("13. Buổi tập cuối cùng hoàn thành -> Kế hoạch tự động chuyển sang COMPLETED")
    void testCompleteWorkout_LastWorkout_AutoCompletesPlan() {
        Long workoutId = 300L;
        CompleteWorkoutRequest req = new CompleteWorkoutRequest();
        req.setPerformanceRating(8);

        TrainingWorkout workout = new TrainingWorkout();
        workout.setId(workoutId);
        workout.setPlanId(50L);
        workout.setLotId(600L);
        workout.setStatus(WorkoutStatus.SCHEDULED);

        HorseTrainingPlan plan = new HorseTrainingPlan();
        plan.setId(50L);
        plan.setTrainerId(9L);
        plan.setStatus(TrainingPlanStatus.ACTIVE);

        when(workoutRepository.findById(workoutId)).thenReturn(Optional.of(workout));
        when(planRepository.findById(50L)).thenReturn(Optional.of(plan));
        when(workoutRepository.saveAndFlush(any(TrainingWorkout.class))).thenAnswer(inv -> inv.getArgument(0));

        // 0 buổi còn lại (buổi này là buổi cuối)
        when(workoutRepository.countByPlanIdAndStatusNotIn(eq(50L), anyList())).thenReturn(0L);

        TrainingLot lot = new TrainingLot();
        lot.setId(600L);
        lot.setSubjectId(77L);
        when(lotRepository.findById(600L)).thenReturn(Optional.of(lot));

        // Act
        planService.completeWorkout(workoutId, req, trainerUser);

        // Assert
        assertEquals(TrainingPlanStatus.COMPLETED, plan.getStatus(),
                "Kế hoạch phải tự động chuyển thành COMPLETED khi buổi cuối hoàn tất");
        verify(planRepository).save(plan);
    }

    // =================================================================
    // reassignFutureWorkoutsToGroom — ĐỒNG BỘ GROOM KHI ĐỔI CHUỒNG
    // =================================================================

    /** Dựng một buổi tập tương lai. */
    private TrainingWorkout futureWorkout(Long id, Long lotId, Long horseId, Long groomId) {
        TrainingWorkout w = new TrainingWorkout();
        w.setId(id);
        w.setLotId(lotId);
        w.setHorseId(horseId);
        w.setAssignedToId(groomId);
        w.setStatus(WorkoutStatus.SCHEDULED);
        return w;
    }

    @Test
    @DisplayName("Ngựa chưa có kế hoạch nào: trả 0, không ghi gì")
    void testReassign_NoFutureWorkouts_ReturnsZero() {
        when(workoutRepository.findFutureScheduledByHorse(eq(77L), any(LocalDate.class)))
                .thenReturn(List.of());

        int moved = planService.reassignFutureWorkoutsToGroom(77L, 5L);

        assertEquals(0, moved);
        verify(workoutRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Không xung đột: mọi buổi tương lai chuyển sang Groom mới")
    void testReassign_NoConflict_UpdatesAll() {
        TrainingWorkout w1 = futureWorkout(101L, 37L, 77L, 4L);
        TrainingWorkout w2 = futureWorkout(102L, 38L, 77L, 4L);

        when(workoutRepository.findFutureScheduledByHorse(eq(77L), any(LocalDate.class)))
                .thenReturn(List.of(w1, w2));
        when(workoutRepository.existsByLotIdAndAssignedToIdAndHorseIdNotAndStatusNot(
                anyLong(), eq(5L), eq(77L), eq(WorkoutStatus.CANCELLED)))
                .thenReturn(false);

        int moved = planService.reassignFutureWorkoutsToGroom(77L, 5L);

        assertEquals(2, moved);
        assertEquals(5L, w1.getAssignedToId());
        assertEquals(5L, w2.getAssignedToId());
        verify(workoutRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("groomId = null: BỎ QUA kiểm tra nhưng VẪN ghi null -> 'chưa phân công'")
    void testReassign_NullGroom_StillWritesNull() {
        TrainingWorkout w1 = futureWorkout(101L, 37L, 77L, 4L);

        when(workoutRepository.findFutureScheduledByHorse(eq(77L), any(LocalDate.class)))
                .thenReturn(List.of(w1));

        int moved = planService.reassignFutureWorkoutsToGroom(77L, null);

        assertEquals(1, moved);
        assertNull(w1.getAssignedToId(),
                "Khối if(newGroomId != null) chỉ bọc bước KIỂM TRA, bước ghi luôn chạy");
        verify(workoutRepository).saveAll(anyList());
        // Không có người thì không thể trùng với ai -> không được gọi kiểm tra
        verify(workoutRepository, never())
                .existsByLotIdAndAssignedToIdAndHorseIdNotAndStatusNot(
                        anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("BR-09: Groom mới đã có con khác trong cùng lot -> ném lỗi, không ghi")
    void testReassign_Br09Conflict_Throws() {
        TrainingWorkout moving = futureWorkout(101L, 37L, 77L, 4L);

        TrainingLot lot = new TrainingLot();
        lot.setId(37L);
        lot.setLotDate(LocalDate.of(2026, 11, 2));
        lot.setStartTime(LocalTime.of(6, 0));

        TrainingWorkout rival = futureWorkout(200L, 37L, 9L, 5L);

        Horse rivalHorse = new Horse();
        rivalHorse.setId(9L);
        rivalHorse.setName("Red Fox");

        when(workoutRepository.findFutureScheduledByHorse(eq(77L), any(LocalDate.class)))
                .thenReturn(List.of(moving));
        when(workoutRepository.existsByLotIdAndAssignedToIdAndHorseIdNotAndStatusNot(
                37L, 5L, 77L, WorkoutStatus.CANCELLED)).thenReturn(true);
        when(lotRepository.findById(37L)).thenReturn(Optional.of(lot));
        when(workoutRepository.findByLotIdAndStatusNot(37L, WorkoutStatus.CANCELLED))
                .thenReturn(List.of(moving, rival));
        when(horseRepository.findById(9L)).thenReturn(Optional.of(rivalHorse));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> planService.reassignFutureWorkoutsToGroom(77L, 5L));

        // Thông báo phải đủ để người dùng tự xử lý: ngày, giờ, lot nào, con nào
        assertTrue(ex.getMessage().contains("BR-09"));
        assertTrue(ex.getMessage().contains("2026-11-02"));
        assertTrue(ex.getMessage().contains("Red Fox"));

        assertEquals(4L, moving.getAssignedToId(), "Vướng lỗi thì không được ghi đè");
        verify(workoutRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("BR-09: gom TẤT CẢ lot bị vướng vào một thông báo, không dừng ở cái đầu")
    void testReassign_MultipleConflicts_ListsAll() {
        TrainingWorkout w1 = futureWorkout(101L, 37L, 77L, 4L);
        TrainingWorkout w2 = futureWorkout(102L, 38L, 77L, 4L);

        TrainingLot lot37 = new TrainingLot();
        lot37.setId(37L);
        lot37.setLotDate(LocalDate.of(2026, 11, 2));
        lot37.setStartTime(LocalTime.of(6, 0));

        TrainingLot lot38 = new TrainingLot();
        lot38.setId(38L);
        lot38.setLotDate(LocalDate.of(2026, 11, 4));
        lot38.setStartTime(LocalTime.of(6, 0));

        Horse rivalHorse = new Horse();
        rivalHorse.setId(9L);
        rivalHorse.setName("Red Fox");

        when(workoutRepository.findFutureScheduledByHorse(eq(77L), any(LocalDate.class)))
                .thenReturn(List.of(w1, w2));
        when(workoutRepository.existsByLotIdAndAssignedToIdAndHorseIdNotAndStatusNot(
                anyLong(), eq(5L), eq(77L), eq(WorkoutStatus.CANCELLED))).thenReturn(true);
        when(lotRepository.findById(37L)).thenReturn(Optional.of(lot37));
        when(lotRepository.findById(38L)).thenReturn(Optional.of(lot38));
        when(workoutRepository.findByLotIdAndStatusNot(anyLong(), eq(WorkoutStatus.CANCELLED)))
                .thenReturn(List.of(futureWorkout(200L, 37L, 9L, 5L)));
        when(horseRepository.findById(9L)).thenReturn(Optional.of(rivalHorse));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> planService.reassignFutureWorkoutsToGroom(77L, 5L));

        assertTrue(ex.getMessage().contains("2026-11-02"));
        assertTrue(ex.getMessage().contains("2026-11-04"),
                "Phải liệt kê đủ để người dùng sửa một lần, không phải sửa từng cái rồi thử lại");
    }

    // =================================================================
    // VÒNG ĐỜI TRẠNG THÁI KẾ HOẠCH
    // =================================================================

    /**
     * Dựng bối cảnh đóng một buổi tập.
     *
     * @param remainingAfter số buổi CÒN LẠI sau khi đóng buổi này.
     *                       0 = đây là buổi cuối của khoá.
     */
    private HorseTrainingPlan setUpWorkoutScenario(TrainingPlanStatus planStatus,
                                                   long remainingAfter) {
        HorseTrainingPlan plan = new HorseTrainingPlan();
        plan.setId(27L);
        plan.setTrainerId(9L);
        plan.setStatus(planStatus);

        TrainingWorkout workout = futureWorkout(101L, 71L, 15L, 11L);
        workout.setPlanId(27L);

        TrainingLot lot = new TrainingLot();
        lot.setId(71L);
        lot.setSubjectId(1L);

        Subject subject = new Subject();
        subject.setId(1L);
        subject.setName("Chạy bền");
        subject.setWorkoutType(WorkoutType.REGULAR);

        when(workoutRepository.findById(101L)).thenReturn(Optional.of(workout));
        when(planRepository.findById(27L)).thenReturn(Optional.of(plan));
        when(workoutRepository.saveAndFlush(any(TrainingWorkout.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(workoutRepository.countByPlanIdAndStatusNotIn(eq(27L), anyList()))
                .thenReturn(remainingAfter);
        when(lotRepository.findById(71L)).thenReturn(Optional.of(lot));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));

        return plan;
    }

    @Test
    @DisplayName("Kế hoạch UPCOMING vẫn đóng được khi hoàn thành buổi cuối")
    void testCompleteWorkout_UpcomingPlan_StillCompletes() {
        HorseTrainingPlan plan = setUpWorkoutScenario(TrainingPlanStatus.UPCOMING, 0L);

        planService.completeWorkout(101L, new CompleteWorkoutRequest(), trainerUser);

        // Trước đây điều kiện "status == ACTIVE" khiến mọi kế hoạch tạo cho
        // ngày tương lai KHÔNG BAO GIỜ đóng được, vì không gì chuyển chúng
        // sang ACTIVE cả.
        assertEquals(TrainingPlanStatus.COMPLETED, plan.getStatus());
        verify(planRepository).save(plan);
    }

    @Test
    @DisplayName("Kế hoạch ACTIVE hoàn thành buổi cuối -> COMPLETED")
    void testCompleteWorkout_ActivePlan_Completes() {
        HorseTrainingPlan plan = setUpWorkoutScenario(TrainingPlanStatus.ACTIVE, 0L);

        planService.completeWorkout(101L, new CompleteWorkoutRequest(), trainerUser);

        assertEquals(TrainingPlanStatus.COMPLETED, plan.getStatus());
    }

    @Test
    @DisplayName("Còn buổi chưa xong: kế hoạch UPCOMING chuyển sang ACTIVE, chưa đóng")
    void testCompleteWorkout_StillRemaining_BecomesActiveNotCompleted() {
        HorseTrainingPlan plan = setUpWorkoutScenario(TrainingPlanStatus.UPCOMING, 5L);

        planService.completeWorkout(101L, new CompleteWorkoutRequest(), trainerUser);

        assertEquals(TrainingPlanStatus.ACTIVE, plan.getStatus(),
                "Đóng được một buổi nghĩa là khoá đã bắt đầu");
    }

    @Test
    @DisplayName("activateStartedPlans: chuyển kế hoạch đã tới ngày bắt đầu sang ACTIVE")
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
    @DisplayName("activateStartedPlans: không có kế hoạch nào tới hạn thì không ghi DB")
    void testActivateStartedPlans_NothingDue() {
        LocalDate today = LocalDate.of(2026, 11, 2);
        when(planRepository.findByStatusAndStartDateLessThanEqual(
                TrainingPlanStatus.UPCOMING, today)).thenReturn(List.of());

        assertEquals(0, planService.activateStartedPlans(today));
        verify(planRepository, never()).saveAll(anyList());
    }

    // =================================================================
    // lotOccupancy — cho thấy cơ chế ghép nhóm đang hoạt động
    // =================================================================

    @Test
    @DisplayName("Chi tiết kế hoạch: mỗi buổi kèm số chiến mã trong lot, đếm bằng MỘT truy vấn")
    void testGetPlanById_FillsLotOccupancy() {
        HorseTrainingPlan plan = new HorseTrainingPlan();
        plan.setId(27L);
        plan.setHorseId(15L);

        TrainingLot lot71 = new TrainingLot();
        lot71.setId(71L);
        lot71.setSubjectId(1L);
        lot71.setLotDate(LocalDate.of(2026, 12, 7));
        lot71.setStartTime(LocalTime.of(6, 0));
        lot71.setEndTime(LocalTime.of(7, 30));

        TrainingLot lot72 = new TrainingLot();
        lot72.setId(72L);
        lot72.setSubjectId(1L);
        lot72.setLotDate(LocalDate.of(2026, 12, 9));
        lot72.setStartTime(LocalTime.of(7, 30));
        lot72.setEndTime(LocalTime.of(9, 0));

        Subject subject = new Subject();
        subject.setId(1L);
        subject.setName("Khởi động & Chạy bền nhịp đều");
        subject.setWorkoutType(WorkoutType.REGULAR);

        when(planRepository.findById(27L)).thenReturn(Optional.of(plan));
        when(workoutRepository.findByPlanIdWithLot(27L)).thenReturn(List.of(
                new Object[]{futureWorkout(101L, 71L, 15L, 11L), lot71},
                new Object[]{futureWorkout(102L, 72L, 15L, 11L), lot72}));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(workoutRepository.countActiveByLotIds(anySet())).thenReturn(List.of(
                new Object[]{71L, 6L},
                new Object[]{72L, 1L}));

        HorseTrainingPlanDetailResponse res = planService.getPlanById(27L);

        assertEquals(6, res.getWorkouts().get(0).getLotOccupancy(),
                "Lot 71 đầy 6 con -> giao diện hiện 'chung với 5 chiến mã khác'");
        assertEquals(1, res.getWorkouts().get(1).getLotOccupancy(),
                "Lot 72 chỉ 1 con -> không hiện chú thích chung lot");

        // MỘT truy vấn gom cho cả kế hoạch, không phải mỗi buổi một truy vấn
        verify(workoutRepository, times(1)).countActiveByLotIds(anySet());
    }

    // =================================================================
    // Nhãn gợi ý nhóm
    // =================================================================

    @Test
    @DisplayName("Chuyển sang chuồng CÙNG Groom: không báo xung đột giả với chính mình")
    void testReassign_SameGroom_NoSelfConflict() {
        TrainingWorkout w1 = futureWorkout(101L, 37L, 77L, 4L);

        when(workoutRepository.findFutureScheduledByHorse(eq(77L), any(LocalDate.class)))
                .thenReturn(List.of(w1));
        // Mệnh đề HorseIdNot loại chính con ngựa đang chuyển ra khỏi phép đếm
        when(workoutRepository.existsByLotIdAndAssignedToIdAndHorseIdNotAndStatusNot(
                37L, 4L, 77L, WorkoutStatus.CANCELLED)).thenReturn(false);

        int moved = planService.reassignFutureWorkoutsToGroom(77L, 4L);

        assertEquals(1, moved);
        assertEquals(4L, w1.getAssignedToId());
    }

    // =================================================================
    // ĐỢT 7 TESTS: TIẾN ĐỘ, BIỂU ĐỒ THỂ LỰC & CẢNH BÁO
    // =================================================================

    @Test
    @DisplayName("Đợt 7.1: Lấy chuỗi dữ liệu thể lực (fitness-trend) theo thời gian tăng dần")
    void testGetFitnessTrend_Success() {
        Long horseId = 1L;
        LocalDate d1 = LocalDate.of(2026, 9, 1);
        LocalDate d2 = LocalDate.of(2026, 9, 3);

        TrainingWorkout w1 = new TrainingWorkout();
        w1.setId(101L);
        w1.setHorseId(horseId);
        w1.setLotId(10L);
        w1.setStatus(WorkoutStatus.COMPLETED);
        w1.setActualDistanceMeters(java.math.BigDecimal.valueOf(1000));
        w1.setAverageSpeedKmh(java.math.BigDecimal.valueOf(40.0));
        w1.setAverageHeartRate(135);
        w1.setPerformanceRating(7);

        TrainingLot l1 = new TrainingLot();
        l1.setId(10L);
        l1.setSubjectId(50L);
        l1.setLotDate(d1);

        TrainingWorkout w2 = new TrainingWorkout();
        w2.setId(102L);
        w2.setHorseId(horseId);
        w2.setLotId(11L);
        w2.setStatus(WorkoutStatus.COMPLETED);
        w2.setActualDistanceMeters(java.math.BigDecimal.valueOf(1200));
        w2.setAverageSpeedKmh(java.math.BigDecimal.valueOf(45.0));
        w2.setAverageHeartRate(145);
        w2.setPerformanceRating(9);

        TrainingLot l2 = new TrainingLot();
        l2.setId(11L);
        l2.setSubjectId(50L);
        l2.setLotDate(d2);

        Subject subject = new Subject();
        subject.setId(50L);
        subject.setName("Phi nước đại");
        when(subjectRepository.findById(50L)).thenReturn(Optional.of(subject));

        // Bộ lọc ngày giờ nằm trong SQL, nên service luôn truyền đủ 3 tham số
        // (bỏ trống from/to thì dùng mốc bao trùm 1900–2999).
        when(workoutRepository.findCompletedWorkoutsWithLotAsc(
                eq(horseId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.<Object[]>of(new Object[]{w1, l1}, new Object[]{w2, l2}));

        List<HorseFitnessTrendItemResponse> result = planService.getFitnessTrend(horseId, null, null);

        assertEquals(2, result.size());
        assertEquals(d1, result.get(0).getDate());
        assertEquals(d2, result.get(1).getDate());
        assertEquals("Phi nước đại", result.get(0).getSubjectName());
        assertEquals(7, result.get(0).getPerformanceRating());
        assertEquals(9, result.get(1).getPerformanceRating());
    }

    @Test
    @DisplayName("Đợt 7.2: Kích hoạt cảnh báo khi nhịp tim max > 220, hồi phục > 100, tụt phong độ, sự cố lặp")
    void testGetHorseAlerts_MultipleRulesTriggered() {
        Long horseId = 2L;
        LocalDate today = LocalDate.now();

        // 3 buổi tập gần nhất: maxHeartRate = 225 (>220), recovery = 110 (>100), phong độ giảm dần 8 -> 6 -> 4
        TrainingWorkout w1 = new TrainingWorkout(); // Mới nhất
        w1.setId(201L);
        w1.setMaxHeartRate(225);
        w1.setRecoveryHeartRate(110);
        w1.setPerformanceRating(4);
        w1.setActualDistanceMeters(java.math.BigDecimal.valueOf(3000));
        TrainingLot l1 = new TrainingLot();
        l1.setLotDate(today.minusDays(1));

        TrainingWorkout w2 = new TrainingWorkout(); // Buổi kế trước
        w2.setId(202L);
        w2.setMaxHeartRate(200);
        w2.setRecoveryHeartRate(90);
        w2.setPerformanceRating(6);
        w2.setActualDistanceMeters(java.math.BigDecimal.valueOf(1000));
        TrainingLot l2 = new TrainingLot();
        l2.setLotDate(today.minusDays(3));

        TrainingWorkout w3 = new TrainingWorkout(); // Buổi trước nữa
        w3.setId(203L);
        w3.setMaxHeartRate(195);
        w3.setRecoveryHeartRate(85);
        w3.setPerformanceRating(8);
        w3.setActualDistanceMeters(java.math.BigDecimal.valueOf(1000));
        TrainingLot l3 = new TrainingLot();
        l3.setLotDate(today.minusDays(5));

        when(workoutRepository.findCompletedWorkoutsWithLotDesc(eq(horseId)))
                .thenReturn(List.<Object[]>of(new Object[]{w1, l1}, new Object[]{w2, l2}, new Object[]{w3, l3}));

        // Sự cố: 2 sự cố trong 14 ngày
        when(incidentReportRepository.countByHorseIdAndReportedAtAfter(eq(horseId), any(LocalDateTime.class)))
                .thenReturn(2L);

        List<HorseAlertResponse> alerts = planService.getHorseAlerts(horseId);

        assertNotNull(alerts);
        assertTrue(alerts.stream().anyMatch(a -> "MAX_HEART_RATE_EXCEEDED".equals(a.getRuleCode())),
                "Phải có cảnh báo nhịp tim tối đa");
        assertTrue(alerts.stream().anyMatch(a -> "POOR_RECOVERY_HEART_RATE".equals(a.getRuleCode())),
                "Phải có cảnh báo hồi phục tim kém");
        assertTrue(alerts.stream().anyMatch(a -> "CONSECUTIVE_PERFORMANCE_DROP".equals(a.getRuleCode())),
                "Phải có cảnh báo phong độ tụt liên tiếp");
        assertTrue(alerts.stream().anyMatch(a -> "REPEATED_INCIDENTS".equals(a.getRuleCode())),
                "Phải có cảnh báo sự cố lặp lại từ Groom");
    }

    @Test
    @DisplayName("Đợt 7.3: Bảng tiến độ Dashboard toàn khu cho Trainer phụ trách")
    void testGetTrainerDashboard_Success() {
        Long trainerId = 9L;
        AuthenticatedUser trainer = new AuthenticatedUser(trainerId, "trainer@example.com", "HEAD_TRAINER");

        Area area = new Area();
        area.setId(10L);
        area.setTrainerId(trainerId);
        when(areaRepository.findAll()).thenReturn(List.of(area));

        StableStall stall = new StableStall();
        stall.setId(100L);
        stall.setAreaId(10L);
        stall.setStallCode("A-01");
        when(stableStallRepository.findAll()).thenReturn(List.of(stall));

        Horse horse = new Horse();
        horse.setId(50L);
        horse.setName("Thần Gió");
        horse.setBreed("Thoroughbred");
        horse.setCurrentStallId(100L);
        when(horseRepository.findAll()).thenReturn(List.of(horse));

        Course course = new Course();
        course.setId(5L);
        course.setName("Khoá bứt tốc");
        course.setTotalSessions(10);
        when(courseRepository.findAll()).thenReturn(List.of(course));

        HorseTrainingPlan plan = new HorseTrainingPlan();
        plan.setId(700L);
        plan.setTrainerId(trainerId);
        plan.setHorseId(50L);
        plan.setCourseId(5L);
        plan.setStatus(TrainingPlanStatus.ACTIVE);
        when(planRepository.findAll()).thenReturn(List.of(plan));

        TrainingWorkout w = new TrainingWorkout();
        w.setId(1L);
        w.setStatus(WorkoutStatus.COMPLETED);
        w.setPerformanceRating(8);

        // Số buổi hoàn thành giờ lấy từ MỘT truy vấn gom cho mọi kế hoạch,
        // thay vì gọi findByPlanIdOrderByIdAsc trong vòng lặp qua từng con ngựa.
        when(workoutRepository.countByPlanIdsGroupedByStatus(anySet()))
                .thenReturn(List.<Object[]>of(new Object[]{700L, WorkoutStatus.COMPLETED, 1L}));

        TrainingLot lot = new TrainingLot();
        lot.setLotDate(LocalDate.now());
        when(workoutRepository.findCompletedWorkoutsWithLotDesc(eq(50L)))
                .thenReturn(List.<Object[]>of(new Object[]{w, lot}));

        List<TrainerDashboardHorseResponse> dashboard = planService.getTrainerDashboard(trainer);

        assertNotNull(dashboard);
        assertEquals(1, dashboard.size());
        TrainerDashboardHorseResponse item = dashboard.get(0);
        assertEquals("Thần Gió", item.getHorseName());
        assertEquals("A-01", item.getStallCode());
        assertEquals("Khoá bứt tốc", item.getCourseName());
        assertEquals(1, item.getCompletedSessions());
        assertEquals(10, item.getTotalSessions());
        assertEquals(10.0, item.getProgressPercent());
        assertEquals(8, item.getLatestPerformanceRating());
    }
}
