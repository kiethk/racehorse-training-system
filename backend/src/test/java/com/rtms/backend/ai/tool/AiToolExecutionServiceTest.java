package com.rtms.backend.ai.tool;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.ai.dto.ClinicalChunkResponseDto;
import com.rtms.backend.ai.rag.service.ClinicalRetrievalService;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.enums.CareSchedulePurpose;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.notification.dto.HorseAlertResponse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.dto.TodayTaskItemResponse;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.GroomIncidentReport;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.enums.AreaType;
import com.rtms.backend.stable.enums.IncidentSeverity;
import com.rtms.backend.stable.enums.IncidentStatus;
import com.rtms.backend.stable.enums.StallStatus;
import com.rtms.backend.stable.enums.TaskSource;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.GroomDailyTaskRepository;
import com.rtms.backend.stable.repository.GroomIncidentReportRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.stable.service.GroomDailyTaskService;
import com.rtms.backend.training.dto.*;
import com.rtms.backend.training.entity.RacingReadinessAssessment;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.DistanceBand;
import com.rtms.backend.training.enums.LotStatus;
import com.rtms.backend.training.enums.RacingReadinessStatus;
import com.rtms.backend.training.enums.SurfaceType;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.enums.TrainingPlanStatus;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.HorseTrainingPlanRepository;
import com.rtms.backend.training.repository.RacingReadinessAssessmentRepository;
import com.rtms.backend.training.repository.TrainingLotRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import com.rtms.backend.training.service.HorsePerformanceService;
import com.rtms.backend.training.service.HorseSpecialtyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class AiToolExecutionServiceTest {

    private DynamicToolRegistryService toolRegistryService;
    private ClinicalRetrievalService clinicalRetrievalService;
    private HorseRepository horseRepository;
    private TrainingWorkoutRepository trainingWorkoutRepository;
    private HealthRecordRepository healthRecordRepository;
    private CareScheduleRepository careScheduleRepository;
    private StableStallRepository stableStallRepository;
    private GroomDailyTaskRepository groomDailyTaskRepository;
    private GroomDailyTaskService groomDailyTaskService;
    private AdmissionApplicationRepository admissionApplicationRepository;
    private TrainingLotRepository trainingLotRepository;
    private HorseTrainingPlanRepository horseTrainingPlanRepository;
    private HorseSpecialtyService horseSpecialtyService;
    private HorsePerformanceService horsePerformanceService;
    private RacingReadinessAssessmentRepository racingReadinessAssessmentRepository;
    private GroomIncidentReportRepository groomIncidentReportRepository;
    private AreaRepository areaRepository;

    private AiToolExecutionService toolExecutionService;

    @BeforeEach
    void setUp() {
        toolRegistryService = new DynamicToolRegistryService();
        clinicalRetrievalService = Mockito.mock(ClinicalRetrievalService.class);
        horseRepository = Mockito.mock(HorseRepository.class);
        trainingWorkoutRepository = Mockito.mock(TrainingWorkoutRepository.class);
        healthRecordRepository = Mockito.mock(HealthRecordRepository.class);
        careScheduleRepository = Mockito.mock(CareScheduleRepository.class);
        stableStallRepository = Mockito.mock(StableStallRepository.class);
        groomDailyTaskRepository = Mockito.mock(GroomDailyTaskRepository.class);
        groomDailyTaskService = Mockito.mock(GroomDailyTaskService.class);
        admissionApplicationRepository = Mockito.mock(AdmissionApplicationRepository.class);
        trainingLotRepository = Mockito.mock(TrainingLotRepository.class);
        horseTrainingPlanRepository = Mockito.mock(HorseTrainingPlanRepository.class);
        horseSpecialtyService = Mockito.mock(HorseSpecialtyService.class);
        horsePerformanceService = Mockito.mock(HorsePerformanceService.class);
        racingReadinessAssessmentRepository = Mockito.mock(RacingReadinessAssessmentRepository.class);
        groomIncidentReportRepository = Mockito.mock(GroomIncidentReportRepository.class);
        areaRepository = Mockito.mock(AreaRepository.class);

        toolExecutionService = new AiToolExecutionService(
                toolRegistryService,
                clinicalRetrievalService,
                horseRepository,
                trainingWorkoutRepository,
                healthRecordRepository,
                careScheduleRepository,
                stableStallRepository,
                groomDailyTaskRepository,
                groomDailyTaskService,
                admissionApplicationRepository,
                trainingLotRepository,
                horseTrainingPlanRepository,
                horseSpecialtyService,
                horsePerformanceService,
                racingReadinessAssessmentRepository,
                groomIncidentReportRepository,
                areaRepository
        );
    }

    // =================================================================
    // Security & Authorization Tests
    // =================================================================

    @Test
    @DisplayName("Người dùng chưa đăng nhập sẽ nhận HTTP 401 UNAUTHORIZED")
    void testExecuteTool_Unauthenticated() {
        ApiException ex = assertThrows(ApiException.class, () ->
                toolExecutionService.executeTool("getMyHorses", Map.of(), null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("Người dùng gọi tool không nằm trong thẩm quyền vai trò sẽ bị từ chối 403 TOOL_FORBIDDEN")
    void testExecuteTool_DisallowedToolForbidden() {
        AuthenticatedUser groom = new AuthenticatedUser(3L, "groom@test.com", "GROOM");

        ApiException ex = assertThrows(ApiException.class, () ->
                toolExecutionService.executeTool("searchClinicalKnowledge", Map.of("query", "test"), groom));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("TOOL_FORBIDDEN", ex.getErrorCode());
    }

    // =================================================================
    // HEAD_TRAINER Tests
    // =================================================================

    @Test
    @DisplayName("HEAD_TRAINER gọi getTrainerHorses trả về danh sách ngựa quản lý")
    void testTrainer_GetTrainerHorses() {
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@rtms.com", "HEAD_TRAINER");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("RED RUM");
        horse.setCurrentStatus(HorseStatus.ELIGIBLE);
        horse.setTrainingDecision(TrainingDecision.ALLOWED);

        when(horseRepository.findManagedHorsesByTrainerIdAndStatusIn(eq(5L), anyCollection()))
                .thenReturn(List.of(horse));

        Object result = toolExecutionService.executeTool("getTrainerHorses", Map.of(), trainer);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(5L, map.get("trainerId"));
        assertEquals(1, map.get("totalManagedHorses"));
    }

    @Test
    @DisplayName("HEAD_TRAINER gọi getTrainerSchedule tra cứu ca tập TrainingLot")
    void testTrainer_GetTrainerSchedule() {
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@rtms.com", "HEAD_TRAINER");

        TrainingLotRow row = new TrainingLotRow(
                100L, 5L, LocalDate.of(2026, 10, 20),
                LocalTime.of(8, 0), LocalTime.of(9, 0),
                1L, "Sprint 1000m", 60, LotStatus.SCHEDULED, 4, 2L
        );
        when(trainingLotRepository.findTrainerLotRows(eq(5L), any(LocalDate.class), any(LocalDate.class), eq(false)))
                .thenReturn(List.of(row));

        LotHorseNameRow name1 = new LotHorseNameRow(100L, "THUNDER");
        LotHorseNameRow name2 = new LotHorseNameRow(100L, "LIGHTNING");
        when(trainingLotRepository.findActiveHorseNames(anyCollection()))
                .thenReturn(List.of(name1, name2));

        Object result = toolExecutionService.executeTool(
                "getTrainerSchedule",
                Map.of("date", "2026-10-20"),
                trainer
        );

        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals(100L, item.get("lotId"));
        assertEquals("Sprint 1000m", item.get("subjectName"));
        assertEquals(2L, item.get("horseCount"));
        List<?> horseNames = (List<?>) item.get("horseNames");
        assertEquals(2, horseNames.size());
    }

    @Test
    @DisplayName("HEAD_TRAINER gọi getTrainerPlansSummary tra cứu kế hoạch huấn luyện")
    void testTrainer_GetTrainerPlansSummary() {
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@rtms.com", "HEAD_TRAINER");

        PlanSummaryResponse summary = new PlanSummaryResponse(
                1L, 10L, "RED RUM", 2L, "Sprint Course",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1),
                TrainingPlanStatus.ACTIVE, 20L, 10L, 0L
        );
        when(horseTrainingPlanRepository.findPlanSummaries(5L, 10L))
                .thenReturn(List.of(summary));

        Object result = toolExecutionService.executeTool(
                "getTrainerPlansSummary",
                Map.of("horseId", 10L, "status", "ACTIVE"),
                trainer
        );

        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("HEAD_TRAINER gọi getHorseSpecialtyAnalysis phân tích sở trường ngựa")
    void testTrainer_GetHorseSpecialtyAnalysis() {
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@rtms.com", "HEAD_TRAINER");

        HorseSpecialtyResponse specialty = new HorseSpecialtyResponse(
                3,
                List.of(DistanceBand.SPRINT),
                SurfaceType.TURF,
                List.of(new DistanceStat(DistanceBand.SPRINT, 5, new BigDecimal("8.5"), new BigDecimal("62.0")))
        );
        when(horseSpecialtyService.getSpecialties(List.of(10L)))
                .thenReturn(Map.of(10L, specialty));

        Object result = toolExecutionService.executeTool(
                "getHorseSpecialtyAnalysis",
                Map.of("horseId", 10L),
                trainer
        );

        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(10L, map.get("horseId"));
        assertEquals(SurfaceType.TURF, map.get("preferredSurface"));
        List<?> dists = (List<?>) map.get("optimalDistances");
        assertEquals(1, dists.size());
    }

    @Test
    @DisplayName("HEAD_TRAINER gọi getTrainerRiskAlerts phát hiện cảnh báo rủi ro chấn thương")
    void testTrainer_GetTrainerRiskAlerts() {
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@rtms.com", "HEAD_TRAINER");

        HorseAlertResponse alert = new HorseAlertResponse(
                "MAX_HEART_RATE_EXCEEDED",
                "Heart rate limit exceeded",
                "Max heart rate 230 bpm",
                "DANGER",
                LocalDateTime.now(),
                230.0,
                220.0
        );
        when(horsePerformanceService.getHorseAlerts(10L, trainer))
                .thenReturn(List.of(alert));

        Object result = toolExecutionService.executeTool(
                "getTrainerRiskAlerts",
                Map.of("horseId", 10L),
                trainer
        );

        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(10L, map.get("horseId"));
        assertEquals(1, map.get("alertsCount"));
    }

    @Test
    @DisplayName("HEAD_TRAINER gọi getRacingReadinessAssessment lấy đánh giá sẵn sàng thi đấu")
    void testTrainer_GetRacingReadinessAssessment() {
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@rtms.com", "HEAD_TRAINER");

        RacingReadinessAssessment assessment = new RacingReadinessAssessment();
        assessment.setId(1L);
        assessment.setHorseId(10L);
        assessment.setReadinessStatus(RacingReadinessStatus.READY);
        assessment.setFitnessScore(new BigDecimal("9.0"));
        assessment.setAssessmentDate(LocalDate.of(2026, 10, 15));

        when(racingReadinessAssessmentRepository.findTopByHorseIdOrderByCreatedAtDesc(10L))
                .thenReturn(Optional.of(assessment));

        Object result = toolExecutionService.executeTool(
                "getRacingReadinessAssessment",
                Map.of("horseId", 10L),
                trainer
        );

        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(1L, map.get("assessmentId"));
        assertEquals(RacingReadinessStatus.READY, map.get("readinessStatus"));
    }

    @Test
    @DisplayName("HEAD_TRAINER gọi checkGroomConflictForLot kiểm tra xung đột BR-09")
    void testTrainer_CheckGroomConflictForLot() {
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@rtms.com", "HEAD_TRAINER");

        GroomConflictRow conflict = new GroomConflictRow(100L, LocalDate.of(2026, 10, 20), LocalTime.of(8, 0), "OTHER_HORSE");
        when(trainingWorkoutRepository.findGroomConflicts(eq(10L), eq(7L), any(LocalDate.class)))
                .thenReturn(List.of(conflict));

        Object result = toolExecutionService.executeTool(
                "checkGroomConflictForLot",
                Map.of("horseId", 10L, "groomId", 7L, "date", "2026-10-20"),
                trainer
        );

        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(true, map.get("hasConflict"));
        assertEquals(1, map.get("conflictCount"));
    }

    // =================================================================
    // VETERINARIAN Tests
    // =================================================================

    @Test
    @DisplayName("VETERINARIAN gọi searchClinicalKnowledge ủy quyền RAG Retrieval thành công")
    void testVet_SearchClinicalKnowledge() {
        AuthenticatedUser vet = new AuthenticatedUser(8L, "vet@test.com", "VETERINARIAN");

        ClinicalChunkResponseDto chunk = new ClinicalChunkResponseDto(
                1L, "Bệnh án", "CLINICAL_RECORD", "Triệu chứng", 0.9, Map.of()
        );
        when(clinicalRetrievalService.retrieveSimilarClinicalRecords(eq("viêm gân"), eq(5L), eq("VETERINARIAN"), anyDouble(), anyInt()))
                .thenReturn(List.of(chunk));

        Object result = toolExecutionService.executeTool(
                "searchClinicalKnowledge",
                Map.of("query", "viêm gân", "horseId", 5L),
                vet
        );

        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        assertEquals(1, ((List<?>) result).size());
    }

    @Test
    @DisplayName("VETERINARIAN gọi getPreventiveCareSchedule tra cứu lịch tiêm phòng")
    void testVet_GetPreventiveCareSchedule() {
        AuthenticatedUser vet = new AuthenticatedUser(8L, "vet@test.com", "VETERINARIAN");

        CareSchedule cs = new CareSchedule();
        cs.setId(50L);
        cs.setHorseId(10L);
        cs.setCareType(CareType.ROUTINE);
        cs.setPurpose(CareSchedulePurpose.PREVENTIVE);
        cs.setStatus(CareScheduleStatus.SCHEDULED);
        cs.setScheduledAt(LocalDateTime.of(2026, 10, 25, 9, 0));
        cs.setDescription("Tiêm phòng cúm ngựa định kỳ");

        when(careScheduleRepository.findByHorseIdOrderByScheduledAtAsc(10L))
                .thenReturn(List.of(cs));

        Object result = toolExecutionService.executeTool(
                "getPreventiveCareSchedule",
                Map.of("horseId", 10L),
                vet
        );

        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals(50L, item.get("scheduleId"));
        assertEquals(CareSchedulePurpose.PREVENTIVE, item.get("purpose"));
    }

    @Test
    @DisplayName("VETERINARIAN gọi getQuarantineStatus tra cứu danh sách cách ly")
    void testVet_GetQuarantineStatus() {
        AuthenticatedUser vet = new AuthenticatedUser(8L, "vet@test.com", "VETERINARIAN");

        Area qArea = new Area();
        qArea.setId(99L);
        qArea.setCode("Q-A");
        qArea.setType(AreaType.QUARANTINE);
        when(areaRepository.findByType(AreaType.QUARANTINE)).thenReturn(List.of(qArea));

        StableStall qStall = new StableStall();
        qStall.setId(901L);
        qStall.setAreaId(99L);
        qStall.setStallCode("Q-01");
        when(stableStallRepository.findByAreaIdInOrdered(List.of(99L))).thenReturn(List.of(qStall));

        Horse qHorse = new Horse();
        qHorse.setId(10L);
        qHorse.setName("QUARANTINE HORSE");
        qHorse.setCurrentStallId(901L);
        qHorse.setTrainingDecision(TrainingDecision.BLOCKED);
        when(horseRepository.findByCurrentStallIdIn(List.of(901L))).thenReturn(List.of(qHorse));
        when(horseRepository.findAll()).thenReturn(List.of(qHorse));

        Object result = toolExecutionService.executeTool("getQuarantineStatus", Map.of(), vet);

        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(1, map.get("totalQuarantinedOrBlockedHorses"));
    }

    @Test
    @DisplayName("VETERINARIAN gọi getRecentIncidentReports tra cứu sự cố trong 48h")
    void testVet_GetRecentIncidentReports() {
        AuthenticatedUser vet = new AuthenticatedUser(8L, "vet@test.com", "VETERINARIAN");

        GroomIncidentReport report = new GroomIncidentReport(
                3L, 10L, "Ngựa khập khiễng chân trái", "Groom phát hiện sau khi dắt đi bộ", null, IncidentSeverity.HIGH
        );
        report.setId(12L);
        report.setStatus(IncidentStatus.REPORTED);
        report.setReportedAt(LocalDateTime.now().minusHours(5));

        when(groomIncidentReportRepository.findByReportedAtAfterOrderByReportedAtDesc(any(LocalDateTime.class)))
                .thenReturn(List.of(report));

        Object result = toolExecutionService.executeTool("getRecentIncidentReports", Map.of(), vet);

        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals(12L, item.get("incidentId"));
        assertEquals("Ngựa khập khiễng chân trái", item.get("title"));
    }

    // =================================================================
    // HORSE_OWNER Tests
    // =================================================================

    @Test
    @DisplayName("HORSE_OWNER gọi getMyHorses chỉ xem danh sách ngựa mình sở hữu")
    void testOwner_GetMyHorses() {
        AuthenticatedUser owner = new AuthenticatedUser(100L, "owner@rtms.com", "HORSE_OWNER");

        Horse horse = new Horse();
        horse.setId(5L);
        horse.setName("MY CHAMPION");
        horse.setOwnerId(100L);
        when(horseRepository.findByOwnerId(100L)).thenReturn(List.of(horse));

        Object result = toolExecutionService.executeTool("getMyHorses", Map.of(), owner);

        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals(5L, item.get("horseId"));
    }

    @Test
    @DisplayName("HORSE_OWNER gọi getMyHorseHealthSummary bị chặn nếu không sở hữu ngựa")
    void testOwner_GetMyHorseHealthSummary_ForbiddenIfNotOwner() {
        AuthenticatedUser owner = new AuthenticatedUser(100L, "owner@rtms.com", "HORSE_OWNER");

        Horse horse = new Horse();
        horse.setId(5L);
        horse.setOwnerId(999L); // Khác ownerId
        when(horseRepository.findById(5L)).thenReturn(Optional.of(horse));

        ApiException ex = assertThrows(ApiException.class, () ->
                toolExecutionService.executeTool("getMyHorseHealthSummary", Map.of("horseId", 5L), owner));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("FORBIDDEN_HORSE", ex.getErrorCode());
    }

    @Test
    @DisplayName("HORSE_OWNER gọi getMyHorseHealthSummary thành công khi là chủ sở hữu")
    void testOwner_GetMyHorseHealthSummary_Success() {
        AuthenticatedUser owner = new AuthenticatedUser(100L, "owner@rtms.com", "HORSE_OWNER");

        Horse horse = new Horse();
        horse.setId(5L);
        horse.setName("MY CHAMPION");
        horse.setOwnerId(100L);
        horse.setTrainingDecision(TrainingDecision.ALLOWED);
        when(horseRepository.findById(5L)).thenReturn(Optional.of(horse));

        HealthRecord hr = new HealthRecord();
        hr.setId(1L);
        hr.setDiagnosis("Khỏe mạnh");
        hr.setExaminedAt(LocalDateTime.now().minusDays(1));
        when(healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(5L)).thenReturn(List.of(hr));

        Object result = toolExecutionService.executeTool("getMyHorseHealthSummary", Map.of("horseId", 5L), owner);

        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("MY CHAMPION", map.get("horseName"));
        assertEquals("Khỏe mạnh", map.get("latestDiagnosis"));
    }

    @Test
    @DisplayName("HORSE_OWNER gọi getMyHorseUpcomingSchedule xem lịch tập của ngựa mình")
    void testOwner_GetMyHorseUpcomingSchedule() {
        AuthenticatedUser owner = new AuthenticatedUser(100L, "owner@rtms.com", "HORSE_OWNER");

        TrainingLotRow row = new TrainingLotRow(
                200L, 5L, LocalDate.of(2026, 10, 22),
                LocalTime.of(7, 30), LocalTime.of(8, 30),
                2L, "Canter 1600m", 60, LotStatus.SCHEDULED, 3, 1L
        );
        when(trainingLotRepository.findOwnerLotRows(eq(100L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(row));

        LotHorseNameRow horseRow = new LotHorseNameRow(200L, "MY CHAMPION");
        when(trainingLotRepository.findActiveHorseNamesOfOwner(List.of(200L), 100L))
                .thenReturn(List.of(horseRow));

        Object result = toolExecutionService.executeTool("getMyHorseUpcomingSchedule", Map.of(), owner);

        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals(200L, item.get("lotId"));
        assertEquals("Canter 1600m", item.get("subjectName"));
    }

    @Test
    @DisplayName("HORSE_OWNER gọi getMyHorseAdmissions xem đơn nhập chuồng")
    void testOwner_GetMyHorseAdmissions() {
        AuthenticatedUser owner = new AuthenticatedUser(100L, "owner@rtms.com", "HORSE_OWNER");

        AdmissionApplication app = new AdmissionApplication();
        app.setId(15L);
        app.setStatus(AdmissionStatus.GROOM_REVIEW);
        when(admissionApplicationRepository.findByOwnerIdOrderByCreatedAtDesc(100L))
                .thenReturn(List.of(app));

        Object result = toolExecutionService.executeTool("getMyHorseAdmissions", Map.of(), owner);

        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        assertEquals(1, ((List<?>) result).size());
    }

    // =================================================================
    // CLUB_MANAGER Tests
    // =================================================================

    @Test
    @DisplayName("CLUB_MANAGER gọi getTrainerAreaDistribution thống kê phân bổ khu vực")
    void testManager_GetTrainerAreaDistribution() {
        AuthenticatedUser manager = new AuthenticatedUser(1L, "manager@rtms.com", "CLUB_MANAGER");

        Area area = new Area();
        area.setId(10L);
        area.setCode("A");
        area.setName("Khu A");
        area.setType(AreaType.REGULAR);
        area.setTrainerId(5L);
        when(areaRepository.findAll()).thenReturn(List.of(area));

        StableStall stall = new StableStall();
        stall.setId(101L);
        stall.setAreaId(10L);
        stall.setGroomId(3L);
        when(stableStallRepository.findAll()).thenReturn(List.of(stall));

        Object result = toolExecutionService.executeTool("getTrainerAreaDistribution", Map.of(), manager);

        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("A", item.get("areaCode"));
        assertEquals(1, item.get("totalStalls"));
        assertEquals(1, item.get("assignedGroomsCount"));
    }

    @Test
    @DisplayName("CLUB_MANAGER gọi getClubHealthOverview báo cáo sức khỏe toàn đàn")
    void testManager_GetClubHealthOverview() {
        AuthenticatedUser manager = new AuthenticatedUser(1L, "manager@rtms.com", "CLUB_MANAGER");

        Horse h1 = new Horse();
        h1.setId(1L);
        h1.setTrainingDecision(TrainingDecision.ALLOWED);

        Horse h2 = new Horse();
        h2.setId(2L);
        h2.setTrainingDecision(TrainingDecision.BLOCKED);

        when(horseRepository.findAll()).thenReturn(List.of(h1, h2));
        when(groomIncidentReportRepository.findByReportedAtAfterOrderByReportedAtDesc(any(LocalDateTime.class)))
                .thenReturn(List.of(new GroomIncidentReport()));

        Object result = toolExecutionService.executeTool("getClubHealthOverview", Map.of(), manager);

        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(2, map.get("totalHorses"));
        assertEquals(1L, map.get("allowedHorses"));
        assertEquals(1L, map.get("blockedHorses"));
        assertEquals(1, map.get("weeklyIncidentsCount"));
    }

    // =================================================================
    // GROOM Tests
    // =================================================================

    @Test
    @DisplayName("GROOM gọi getMyAssignedHorses lấy danh sách chuồng và ngựa phụ trách thành công")
    void testGroom_GetMyAssignedHorses() {
        AuthenticatedUser groom = new AuthenticatedUser(10L, "groom1@rtms.com", "GROOM");

        StableStall stall1 = new StableStall();
        stall1.setId(6L);
        stall1.setStallCode("A-01");
        stall1.setStallNumber(1);
        stall1.setAreaId(2L);
        stall1.setGroomId(10L);
        stall1.setStatus(StallStatus.OCCUPIED);

        when(stableStallRepository.findByGroomId(10L)).thenReturn(List.of(stall1));

        Horse horse1 = new Horse();
        horse1.setId(8L);
        horse1.setName("NAUSICAA BY NIKA");
        horse1.setCurrentStallId(6L);
        horse1.setTrainingDecision(TrainingDecision.ALLOWED);

        when(horseRepository.findByCurrentStallIdIn(List.of(6L))).thenReturn(List.of(horse1));

        Object result = toolExecutionService.executeTool("getMyAssignedHorses", Map.of(), groom);

        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> resMap = (Map<?, ?>) result;
        assertEquals(10L, resMap.get("groomId"));
        assertEquals(1, resMap.get("totalAssignedStalls"));
        assertEquals(1, resMap.get("occupiedStalls"));
    }

    @Test
    @DisplayName("GROOM gọi getTodayAggregatedTasks gom việc 3 nguồn thành công")
    void testGroom_GetTodayAggregatedTasks() {
        AuthenticatedUser groom = new AuthenticatedUser(10L, "groom1@rtms.com", "GROOM");

        TodayTaskItemResponse item = new TodayTaskItemResponse(
                TaskSource.PREVENTIVE_CARE, 1L, LocalTime.of(14, 0),
                null, 8L, "NAUSICAA",
                "Care Task", "Notes", "SCHEDULED", false
        );
        when(groomDailyTaskService.getTodayAggregatedTasks(eq(10L), any(LocalDate.class), eq(groom)))
                .thenReturn(List.of(item));

        Object result = toolExecutionService.executeTool("getTodayAggregatedTasks", Map.of(), groom);

        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
    }
}
