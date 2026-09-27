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
                areaRepository
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
}
