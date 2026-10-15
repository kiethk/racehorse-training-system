package com.rtms.backend.service;

import com.rtms.backend.dto.*;
import com.rtms.backend.entity.*;
import com.rtms.backend.enums.*;
import com.rtms.backend.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoutineCareServiceTest {

    @Mock private CareScheduleRepository careScheduleRepository;
    @Mock private HorseRepository horseRepository;
    @Mock private HealthRecordRepository healthRecordRepository;
    @Mock private HorseHealthMetricRepository metricRepository;
    @Mock private AdmissionApplicationRepository admissionRepository;
    @Mock private StableStallRepository stallRepository;
    @Mock private UserRepository userRepository;
    @Mock private GroomIncidentReportRepository incidentReportRepository;
    @Mock private VeterinarianProfileRepository veterinarianProfileRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private EntityManager entityManager;
    @Mock private NotificationService notificationService;
    @Mock private TrainingDecisionService trainingDecisionService;

    private CareScheduleService careScheduleService;

    @BeforeEach
    void setUp() {
        careScheduleService = new CareScheduleService(
                careScheduleRepository,
                horseRepository,
                healthRecordRepository,
                metricRepository,
                admissionRepository,
                stallRepository,
                userRepository,
                incidentReportRepository,
                veterinarianProfileRepository,
                auditLogRepository,
                eventPublisher,
                entityManager,
                notificationService,
                trainingDecisionService
        );
        ReflectionTestUtils.setField(careScheduleService, "self", careScheduleService);
    }

    @Test
    @DisplayName("getLatestVitals trả về metric mới nhất kèm dải ngưỡng chuẩn")
    void testGetLatestVitals() {
        Long horseId = 100L;
        HorseHealthMetric m = new HorseHealthMetric();
        m.setHorseId(horseId);
        m.setHeartRate(new BigDecimal("38"));
        m.setTemperature(new BigDecimal("37.8"));
        m.setRespiratoryRate(new BigDecimal("12"));
        m.setWeight(new BigDecimal("490.5"));
        m.setBodyConditionScore(new BigDecimal("5"));
        m.setRecordedAt(LocalDateTime.now().minusDays(1));

        when(metricRepository.findByHorseIdOrderByRecordedAtDesc(horseId)).thenReturn(List.of(m));

        LatestVitalsResponse resp = careScheduleService.getLatestVitals(horseId);

        assertNotNull(resp);
        assertEquals(horseId, resp.getHorseId());
        assertEquals(new BigDecimal("38"), resp.getHeartRate());
        assertEquals(new BigDecimal("37.8"), resp.getTemperature());
        assertNotNull(resp.getThresholds());
        assertTrue(resp.getThresholds().containsKey("heartRate"));
        assertEquals("bpm", resp.getThresholds().get("heartRate").getUnit());
    }

    @Test
    @DisplayName("batchComplete xử lý độc lập từng dòng: lỗi 1 dòng không làm sập các dòng khác")
    void testBatchComplete() {
        Long vetId = 99L;

        // Schedule 1: Success
        CareSchedule s1 = new CareSchedule();
        s1.setId(1L);
        s1.setHorseId(10L);
        s1.setVeterinarianId(vetId);
        s1.setStatus(CareScheduleStatus.IN_PROGRESS);
        s1.setCareType(CareType.ROUTINE);

        Horse h1 = new Horse();
        h1.setId(10L);
        h1.setName("Thunder");
        h1.setTrainingDecision(TrainingDecision.ALLOWED);

        when(careScheduleRepository.findById(1L)).thenReturn(Optional.of(s1));
        when(careScheduleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(s1));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(h1));
        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));
        when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(i -> {
            HealthRecord rec = i.getArgument(0);
            rec.setId(500L);
            return rec;
        });

        // Schedule 2: Not found
        when(careScheduleRepository.findById(2L)).thenReturn(Optional.empty());
        when(careScheduleRepository.findByIdForUpdate(2L)).thenReturn(Optional.empty());

        BatchCareScheduleRowRequest r1 = new BatchCareScheduleRowRequest();
        r1.setScheduleId(1L);
        r1.setHeartRate(new BigDecimal("36"));
        r1.setTrainingDecision(TrainingDecision.ALLOWED);

        BatchCareScheduleRowRequest r2 = new BatchCareScheduleRowRequest();
        r2.setScheduleId(2L);

        BatchCompleteCareScheduleRequest req = new BatchCompleteCareScheduleRequest(List.of(r1, r2));

        BatchCareScheduleResultResponse resp = careScheduleService.batchComplete(req, vetId);

        assertEquals(2, resp.getTotal());
        assertEquals(1, resp.getSuccessCount());
        assertEquals(1, resp.getFailedCount());
        assertEquals("SUCCESS", resp.getResults().get(0).getStatus());
        assertEquals("FAILED", resp.getResults().get(1).getStatus());
    }

    @Test
    @DisplayName("BR-TRN-05: Khám Routine chọn ALLOWED nhưng còn ca URGENT mở thì không được mở khóa tập luyện")
    void testRoutineWithActiveUrgentBlocker() {
        Long vetId = 99L;
        Long horseId = 15L;

        CareSchedule routineSchedule = new CareSchedule();
        routineSchedule.setId(101L);
        routineSchedule.setHorseId(horseId);
        routineSchedule.setVeterinarianId(vetId);
        routineSchedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        routineSchedule.setCareType(CareType.ROUTINE);

        Horse horse = new Horse();
        horse.setId(horseId);
        horse.setName("Flash");
        horse.setTrainingDecision(TrainingDecision.BLOCKED);

        when(careScheduleRepository.findById(101L)).thenReturn(Optional.of(routineSchedule));
        when(careScheduleRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(routineSchedule));
        when(horseRepository.findByIdForUpdate(horseId)).thenReturn(Optional.of(horse));
        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));
        when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(i -> {
            HealthRecord rec = i.getArgument(0);
            rec.setId(600L);
            return rec;
        });

        // Còn ca URGENT mở
        when(careScheduleRepository.existsByHorseIdAndCareTypeAndStatusIn(
                eq(horseId), eq(CareType.URGENT), anyCollection())).thenReturn(true);

        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
        req.setFindings("Mắt sáng, đi đứng tốt");
        req.setDiagnosis("Bình thường");
        req.setTrainingDecision(TrainingDecision.ALLOWED); // Vet muốn mở khóa ALLOWED

        careScheduleService.completeCareSchedule(101L, req, vetId);

        // Xác nhận trainingDecisionService.allow() KHÔNG được gọi vì còn Urgent khóa
        verify(trainingDecisionService, never()).allow(any(Horse.class));
    }

    @Test
    @DisplayName("getTaskBoard tổng hợp danh sách công việc và thống kê Quá hạn/Hôm nay/Tuần này")
    void testGetTaskBoard() {
        CareSchedule cs = new CareSchedule();
        cs.setId(201L);
        cs.setHorseId(50L);
        cs.setCareType(CareType.ROUTINE);
        cs.setStatus(CareScheduleStatus.SCHEDULED);
        cs.setScheduledAt(LocalDateTime.now().minusHours(2)); // Overdue
        cs.setDescription("Khám định kỳ");

        Horse horse = new Horse();
        horse.setId(50L);
        horse.setName("Shadow");

        when(careScheduleRepository.findTaskBoard(eq(CareType.ROUTINE), any(), any()))
                .thenReturn(List.of(cs));
        when(horseRepository.findAllById(anySet())).thenReturn(List.of(horse));
        when(careScheduleRepository.countByStatusAndCompletedAtGreaterThanEqual(any(), any()))
                .thenReturn(9L);

        CareTaskBoardResponse board = careScheduleService.getTaskBoard(CareType.ROUTINE, null, null, null);

        assertNotNull(board);
        assertEquals(1, board.getItems().size());
        assertEquals("OVERDUE", board.getItems().get(0).getUrgency());
        assertEquals("Shadow", board.getItems().get(0).getHorseName());
        assertEquals(1, board.getSummary().getOverdueCount());
    }
}
