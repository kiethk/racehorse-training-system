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
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
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
import com.rtms.backend.stable.enums.GroomTaskType;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.GroomDailyTaskRepository;
import com.rtms.backend.stable.repository.GroomIncidentReportRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.stable.service.GroomDailyTaskService;
import com.rtms.backend.training.dto.*;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.RacingReadinessAssessment;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.DistanceBand;
import com.rtms.backend.training.enums.LotStatus;
import com.rtms.backend.training.enums.RacingReadinessStatus;
import com.rtms.backend.training.enums.SurfaceType;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.enums.TrainingPlanStatus;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.CourseRepository;
import com.rtms.backend.training.repository.HorseTrainingPlanRepository;
import com.rtms.backend.training.repository.RacingReadinessAssessmentRepository;
import com.rtms.backend.training.repository.TrainingLotRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import com.rtms.backend.training.service.HorsePerformanceService;
import com.rtms.backend.training.service.HorseSpecialtyService;
import com.rtms.backend.audit.entity.AuditLog;
import com.rtms.backend.audit.repository.AuditLogRepository;
import com.rtms.backend.admission.entity.CandidateHorseProfile;
import com.rtms.backend.admission.repository.CandidateHorseProfileRepository;
import com.rtms.backend.racing.entity.RaceRegistration;
import com.rtms.backend.racing.enums.RaceRegistrationStatus;
import com.rtms.backend.racing.repository.RaceRegistrationRepository;
import com.rtms.backend.training.entity.Subject;
import com.rtms.backend.training.enums.IntensityLevel;
import com.rtms.backend.training.enums.SubjectCategory;
import com.rtms.backend.training.repository.SubjectRepository;
import com.rtms.backend.medical.entity.HorseHealthMetric;
import com.rtms.backend.medical.repository.HorseHealthMetricRepository;
import com.rtms.backend.horse.entity.HorsePedigree;
import com.rtms.backend.horse.repository.HorsePedigreeRepository;
import com.rtms.backend.incident.entity.HorseIncidentReport;
import com.rtms.backend.incident.repository.HorseIncidentReportRepository;
import com.rtms.backend.stable.entity.GroomDailyTask;
import com.rtms.backend.stable.repository.GroomTaskCompletionRepository;
import com.rtms.backend.identity.entity.Role;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
    private UserRepository userRepository;
    private CourseRepository courseRepository;
    private AuditLogRepository auditLogRepository;
    private CandidateHorseProfileRepository candidateHorseProfileRepository;
    private RaceRegistrationRepository raceRegistrationRepository;
    private SubjectRepository subjectRepository;
    private HorseHealthMetricRepository horseHealthMetricRepository;
    private HorsePedigreeRepository horsePedigreeRepository;
    private HorseIncidentReportRepository horseIncidentReportRepository;
    private GroomTaskCompletionRepository groomTaskCompletionRepository;

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
        userRepository = Mockito.mock(UserRepository.class);
        courseRepository = Mockito.mock(CourseRepository.class);
        auditLogRepository = Mockito.mock(AuditLogRepository.class);
        candidateHorseProfileRepository = Mockito.mock(CandidateHorseProfileRepository.class);
        raceRegistrationRepository = Mockito.mock(RaceRegistrationRepository.class);
        subjectRepository = Mockito.mock(SubjectRepository.class);
        horseHealthMetricRepository = Mockito.mock(HorseHealthMetricRepository.class);
        horsePedigreeRepository = Mockito.mock(HorsePedigreeRepository.class);
        horseIncidentReportRepository = Mockito.mock(HorseIncidentReportRepository.class);
        groomTaskCompletionRepository = Mockito.mock(GroomTaskCompletionRepository.class);

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
                areaRepository,
                userRepository,
                courseRepository,
                auditLogRepository,
                candidateHorseProfileRepository,
                raceRegistrationRepository,
                subjectRepository,
                horseHealthMetricRepository,
                horsePedigreeRepository,
                horseIncidentReportRepository,
                groomTaskCompletionRepository
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
    @DisplayName("HEAD_TRAINER gọi getTrainerHorses trả về danh sách ngựa quản lý kèm Groom phụ trách")
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
        assertEquals(1, map.get("tongSoNguaQuanLy"));
        assertTrue(map.containsKey("danhSachNgua"));
    }

    @Test
    @DisplayName("HEAD_TRAINER gọi getTrainerSchedule tra cứu ca tập TrainingLot hiển thị rõ Ca tập")
    void testTrainer_GetTrainerSchedule() {
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@rtms.com", "HEAD_TRAINER");

        TrainingLotRow row = new TrainingLotRow(
                100L, 5L, LocalDate.of(2026, 10, 20),
                LocalTime.of(8, 0), LocalTime.of(9, 30),
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
        assertTrue(item.get("caTap").toString().contains("Ca Sáng"));
        assertEquals("Sprint 1000m", item.get("tenBaiTap"));
        assertEquals(2L, item.get("soLuongNgua"));
        List<?> horseNames = (List<?>) item.get("danhSachNgua");
        assertEquals(2, horseNames.size());
    }

    @Test
    @DisplayName("HEAD_TRAINER gọi getTrainerPlansSummary tra cứu kế hoạch kèm Tên kế hoạch, Mục tiêu, Trạng thái")
    void testTrainer_GetTrainerPlansSummary() {
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@rtms.com", "HEAD_TRAINER");

        PlanSummaryResponse summary = new PlanSummaryResponse(
                1L, 10L, "RED RUM", 2L, "Sprint Course",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1),
                TrainingPlanStatus.ACTIVE, 20L, 10L, 0L
        );
        when(horseTrainingPlanRepository.findPlanSummaries(5L, 10L))
                .thenReturn(List.of(summary));

        Course course = new Course();
        course.setId(2L);
        course.setName("Sprint Course");
        course.setTargetGoal("Bứt phá tốc độ cự ly ngắn");
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course));

        Object result = toolExecutionService.executeTool(
                "getTrainerPlansSummary",
                Map.of("horseId", 10L, "status", "ACTIVE"),
                trainer
        );

        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("Sprint Course", item.get("tenKeHoach"));
        assertEquals("Bứt phá tốc độ cự ly ngắn", item.get("mucTieu"));
        assertTrue(item.get("trangThai").toString().contains("ACTIVE"));
    }

    @Test
    @DisplayName("HEAD_TRAINER gọi getHorseSpecialtyAnalysis phân tích sở trường ngựa luôn có mặt sân")
    void testTrainer_GetHorseSpecialtyAnalysis() {
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@rtms.com", "HEAD_TRAINER");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("RED RUM");
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

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
        assertEquals("RED RUM", map.get("tenNgua"));
        assertTrue(map.get("matSanUaThich").toString().contains("TURF"));
    }

    @Test
    @DisplayName("HEAD_TRAINER gọi getTrainerAssignedGrooms tra cứu Groom làm việc cùng")
    void testTrainer_GetTrainerAssignedGrooms() {
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@rtms.com", "HEAD_TRAINER");

        Area area = new Area();
        area.setId(1L);
        area.setName("Khu Huấn Luyện A");
        area.setTrainerId(5L);
        when(areaRepository.findAll()).thenReturn(List.of(area));

        StableStall stall = new StableStall();
        stall.setId(10L);
        stall.setAreaId(1L);
        stall.setGroomId(7L);
        stall.setStallCode("A-01");
        when(stableStallRepository.findByAreaIdInOrdered(List.of(1L))).thenReturn(List.of(stall));

        User groomUser = new User();
        groomUser.setId(7L);
        groomUser.setFullName("Nguyễn Văn Groom");
        groomUser.setPhone("0988123456");
        when(userRepository.findById(7L)).thenReturn(Optional.of(groomUser));

        Object result = toolExecutionService.executeTool("getTrainerAssignedGrooms", Map.of(), trainer);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(1, map.get("tongSoGroomLamViecCung"));
        List<?> groomList = (List<?>) map.get("danhSachGroom");
        assertNotNull(groomList);
        assertEquals(1, groomList.size());
        Map<?, ?> groomItem = (Map<?, ?>) groomList.get(0);
        assertEquals("0988123456", groomItem.get("soDienThoai"));
        assertEquals("Chưa có email", groomItem.get("email"));
    }

    // =================================================================
    // VETERINARIAN Tests
    // =================================================================

    @Test
    @DisplayName("VETERINARIAN gọi getVetExaminationOverview xem tổng thể ngựa đang/chờ/đã khám")
    void testVet_GetVetExaminationOverview() {
        AuthenticatedUser vet = new AuthenticatedUser(8L, "vet@test.com", "VETERINARIAN");

        CareSchedule cs1 = new CareSchedule();
        cs1.setId(1L);
        cs1.setHorseId(10L);
        cs1.setStatus(CareScheduleStatus.IN_PROGRESS);

        CareSchedule cs2 = new CareSchedule();
        cs2.setId(2L);
        cs2.setHorseId(11L);
        cs2.setStatus(CareScheduleStatus.SCHEDULED);

        when(careScheduleRepository.findAll()).thenReturn(List.of(cs1, cs2));

        Object result = toolExecutionService.executeTool("getVetExaminationOverview", Map.of(), vet);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(1, map.get("soCaDangKham"));
        assertEquals(1, map.get("soCaChuaKham"));
    }

    @Test
    @DisplayName("VETERINARIAN gọi getQuarantineStatus: Ngựa cách ly luôn luôn BLOCKED và không được phép tập")
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
        assertEquals(1, map.get("tongSoNguaCachLyHoacCamTap"));

        List<?> list = (List<?>) map.get("danhSachNguaCachLy");
        Map<?, ?> horseItem = (Map<?, ?>) list.get(0);
        assertEquals("BLOCKED", horseItem.get("quyetDinhTapLuyen"));
        assertEquals(false, horseItem.get("duocPhepTapLuyen"));
    }

    // =================================================================
    // GROOM Tests
    // =================================================================

    @Test
    @DisplayName("GROOM gọi getHorseStallLocation hỗ trợ tra cứu vị trí chuồng")
    void testGroom_GetHorseStallLocation() {
        AuthenticatedUser groom = new AuthenticatedUser(10L, "groom1@rtms.com", "GROOM");

        Horse horse = new Horse();
        horse.setId(8L);
        horse.setName("NAUSICAA");
        horse.setCurrentStallId(6L);
        when(horseRepository.findById(8L)).thenReturn(Optional.of(horse));

        StableStall stall = new StableStall();
        stall.setId(6L);
        stall.setStallCode("A-01");
        stall.setStallNumber(1);
        when(stableStallRepository.findById(6L)).thenReturn(Optional.of(stall));

        Object result = toolExecutionService.executeTool("getHorseStallLocation", Map.of("horseId", 8L), groom);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("NAUSICAA", map.get("tenNgua"));
        assertTrue(map.get("viTriChuong").toString().contains("A-01"));
    }

    // =================================================================
    // CLUB_MANAGER Tests
    // =================================================================

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
        assertEquals(2, map.get("tongSoNguaTrongTrangTrai"));
        assertEquals(1L, map.get("soNguaDuocPhepTapLuyen"));
        assertEquals(1L, map.get("soNguaBiCamTapLuyenYTe"));
        assertEquals(1, map.get("soSuCoTrong7NgayQua"));
    }

    @Test
    @DisplayName("CLUB_MANAGER gọi getSystemStaffOverview tra cứu danh bạ nhân sự")
    void testManager_GetSystemStaffOverview() {
        AuthenticatedUser manager = new AuthenticatedUser(1L, "manager@rtms.com", "CLUB_MANAGER");

        User u1 = new User();
        u1.setId(10L);
        u1.setFullName("Nguyễn Văn HLV");
        u1.setEmail("trainer@rtms.com");
        u1.setPhone("0901234567");
        u1.setActive(true);
        Role r1 = new Role();
        r1.setName("HEAD_TRAINER");
        u1.setRole(r1);

        User u2 = new User();
        u2.setId(20L);
        u2.setFullName("Trần Thị Bác Sĩ");
        u2.setEmail("vet@rtms.com");
        u2.setPhone("0907654321");
        u2.setActive(false);
        Role r2 = new Role();
        r2.setName("VETERINARIAN");
        u2.setRole(r2);

        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        Object result = toolExecutionService.executeTool("getSystemStaffOverview", Map.of(), manager);
        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(2, list.size());

        Map<?, ?> item1 = (Map<?, ?>) list.get(0);
        assertEquals("Nguyễn Văn HLV", item1.get("hoTen"));
        assertEquals("Huấn luyện viên trưởng", item1.get("vaiTro"));
        assertEquals("trainer@rtms.com", item1.get("email"));
        assertEquals("0901234567", item1.get("soDienThoai"));
        assertEquals("Đang hoạt động", item1.get("trangThaiHoatDong"));
        assertNotNull(item1.get("thongTinChuyenMon"));

        Map<?, ?> item2 = (Map<?, ?>) list.get(1);
        assertEquals("Đã khóa", item2.get("trangThaiHoatDong"));
        assertEquals("Bác sĩ thú y", item2.get("vaiTro"));

        // Filter by role & isActive
        Object filteredResult = toolExecutionService.executeTool(
                "getSystemStaffOverview",
                Map.of("role", "HEAD_TRAINER", "isActive", true),
                manager
        );
        assertTrue(filteredResult instanceof List<?>);
        List<?> filteredList = (List<?>) filteredResult;
        assertEquals(1, filteredList.size());
        assertEquals("Nguyễn Văn HLV", ((Map<?, ?>) filteredList.get(0)).get("hoTen"));
    }

    @Test
    @DisplayName("CLUB_MANAGER gọi getClubHorsesDirectory tra cứu danh bạ chiến mã kèm trạng thái và HLV phụ trách")
    void testManager_GetClubHorsesDirectory() {
        AuthenticatedUser manager = new AuthenticatedUser(1L, "manager@rtms.com", "CLUB_MANAGER");

        Horse h1 = new Horse();
        h1.setId(100L);
        h1.setName("DARK ANGEL");
        h1.setRegistrationNumber("VN-100");
        h1.setOwnerId(50L);
        h1.setCurrentStallId(1L);
        h1.setCurrentStatus(HorseStatus.ELIGIBLE);
        h1.setTrainingDecision(TrainingDecision.ALLOWED);

        when(horseRepository.findAll()).thenReturn(List.of(h1));

        User owner = new User();
        owner.setId(50L);
        owner.setFullName("Chủ Ngựa Lê Văn");
        when(userRepository.findById(50L)).thenReturn(Optional.of(owner));

        StableStall stall = new StableStall();
        stall.setId(1L);
        stall.setAreaId(10L);
        stall.setStallCode("A-01");
        stall.setStallNumber(1);
        when(stableStallRepository.findById(1L)).thenReturn(Optional.of(stall));

        Area area = new Area();
        area.setId(10L);
        area.setTrainerId(5L);
        when(areaRepository.findById(10L)).thenReturn(Optional.of(area));

        User trainer = new User();
        trainer.setId(5L);
        trainer.setFullName("HLV Park");
        when(userRepository.findById(5L)).thenReturn(Optional.of(trainer));

        Object result = toolExecutionService.executeTool("getClubHorsesDirectory", Map.of(), manager);
        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());

        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("DARK ANGEL", item.get("tenNgua"));
        assertEquals("VN-100", item.get("maSoNgua"));
        assertEquals("Chủ Ngựa Lê Văn", item.get("tenChuNgua"));
        assertTrue(item.get("trangThaiHienTai").toString().contains("ACTIVE"));
        assertTrue(item.get("viTriChuong").toString().contains("A-01"));
        assertEquals("HLV Park", item.get("huanLuyenVienPhuTrach"));
    }

    @Test
    @DisplayName("CLUB_MANAGER gọi getRecentAuditLogs tra cứu nhật ký kiểm toán hệ thống")
    void testManager_GetRecentAuditLogs() {
        AuthenticatedUser manager = new AuthenticatedUser(1L, "manager@rtms.com", "CLUB_MANAGER");

        AuditLog log = new AuditLog();
        log.setId(1L);
        log.setAction("Cập nhật kế hoạch huấn luyện");
        log.setEntityName("HorseTrainingPlan");
        log.setEntityId(10L);
        log.setHttpMethod("PUT");
        log.setRequestPath("/api/training/plans/10");
        log.setStatusCode(200);

        User actor = new User();
        actor.setId(5L);
        actor.setFullName("HLV Nguyễn Văn B");
        Role r = new Role();
        r.setName("HEAD_TRAINER");
        actor.setRole(r);
        log.setUser(actor);

        when(auditLogRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        Object result = toolExecutionService.executeTool("getRecentAuditLogs", Map.of("limit", 10), manager);
        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());

        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("Cập nhật kế hoạch huấn luyện", item.get("hanhDong"));
        assertTrue(item.get("nguoiThucHien").toString().contains("HLV Nguyễn Văn B"));
        assertTrue(item.get("chiTietThayDoi").toString().contains("HorseTrainingPlan"));
    }

    @Test
    @DisplayName("CLUB_MANAGER gọi getAdmissionPipelineDetails tra cứu chi tiết các đơn đăng ký nhập đàn")
    void testManager_GetAdmissionPipelineDetails() {
        AuthenticatedUser manager = new AuthenticatedUser(1L, "manager@rtms.com", "CLUB_MANAGER");

        AdmissionApplication app = new AdmissionApplication();
        app.setId(123L);
        app.setOwnerId(50L);
        app.setStatus(AdmissionStatus.MANAGER_REVIEW);
        app.setSubmittedAt(LocalDateTime.of(2026, 10, 20, 10, 0));
        app.setVeterinarianId(8L);
        app.setTrainerId(5L);

        when(admissionApplicationRepository.findAll()).thenReturn(List.of(app));

        CandidateHorseProfile profile = new CandidateHorseProfile();
        profile.setId(1L);
        profile.setAdmissionId(123L);
        profile.setName("PEGASUS JUNIOR");
        when(candidateHorseProfileRepository.findByAdmissionId(123L)).thenReturn(Optional.of(profile));

        User owner = new User();
        owner.setId(50L);
        owner.setFullName("Chủ Ngựa Trần C");
        when(userRepository.findById(50L)).thenReturn(Optional.of(owner));

        User vet = new User();
        vet.setId(8L);
        vet.setFullName("BS Thú Y Hoàng");
        when(userRepository.findById(8L)).thenReturn(Optional.of(vet));

        User trainer = new User();
        trainer.setId(5L);
        trainer.setFullName("HLV Park");
        when(userRepository.findById(5L)).thenReturn(Optional.of(trainer));

        Object result = toolExecutionService.executeTool("getAdmissionPipelineDetails", Map.of("status", "PENDING"), manager);
        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());

        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("PEGASUS JUNIOR", item.get("tenNgua"));
        assertEquals("Chủ Ngựa Trần C", item.get("tenChuNgua"));
        assertTrue(item.get("buocHienTai").toString().contains("MANAGER_REVIEW"));
        assertEquals("BS Thú Y Hoàng", item.get("bacSiPhuTrach"));
        assertEquals("HLV Park", item.get("huanLuyenVienChiDinh"));
        assertEquals("/manager/admissions/123", item.get("duongDanDieuHuong"));
        assertEquals("/manager/admissions", item.get("actionUrl"));
    }

    // =================================================================
    // New Role Tools Tests
    // =================================================================

    @Test
    @DisplayName("HORSE_OWNER gọi getMyHorseIncidentHistory xem lịch sử sự cố của ngựa")
    void testOwner_GetMyHorseIncidentHistory() {
        AuthenticatedUser owner = new AuthenticatedUser(100L, "owner@rtms.com", "HORSE_OWNER");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("RED RUM");
        horse.setOwnerId(100L);
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        HorseIncidentReport rep = new HorseIncidentReport();
        rep.setId(1L);
        rep.setHorseId(10L);
        rep.setTitle("Trầy xước chân trước");
        rep.setDescription("Bị va chạm nhẹ khi di chuyển");
        rep.setReportedAt(LocalDateTime.now());
        when(horseIncidentReportRepository.findByHorseId(10L)).thenReturn(List.of(rep));

        Object result = toolExecutionService.executeTool("getMyHorseIncidentHistory", Map.of("horseId", 10L), owner);
        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("RED RUM", item.get("tenNgua"));
        assertEquals("Trầy xước chân trước", item.get("tieuDe"));
        assertEquals("/owner/horses", item.get("actionUrl"));
    }

    @Test
    @DisplayName("HORSE_OWNER gọi getMyHorsePedigreeInfo tra cứu phả hệ chiến mã")
    void testOwner_GetMyHorsePedigreeInfo() {
        AuthenticatedUser owner = new AuthenticatedUser(100L, "owner@rtms.com", "HORSE_OWNER");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("RED RUM");
        horse.setOwnerId(100L);
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        HorsePedigree ped = new HorsePedigree();
        ped.setHorseId(10L);
        ped.setSireName("Galileo");
        ped.setDamName("Urban Sea");
        ped.setPedigreeNotes("Xuất xứ Ireland");
        when(horsePedigreeRepository.findByHorseId(10L)).thenReturn(Optional.of(ped));

        Object result = toolExecutionService.executeTool("getMyHorsePedigreeInfo", Map.of("horseId", 10L), owner);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("RED RUM", map.get("tenNgua"));
        assertEquals("Galileo", map.get("nguaCha"));
        assertEquals("Urban Sea", map.get("nguaMe"));
        assertEquals("/owner/horses", map.get("actionUrl"));
    }

    @Test
    @DisplayName("HEAD_TRAINER gọi getMyHorseRaceRegistrations tra cứu đăng ký giải đua")
    void testTrainer_GetMyHorseRaceRegistrations() {
        AuthenticatedUser trainer = new AuthenticatedUser(200L, "trainer@rtms.com", "HEAD_TRAINER");

        RaceRegistration reg = new RaceRegistration();
        reg.setId(1L);
        reg.setTrainerId(200L);
        reg.setHorseId(10L);
        reg.setRaceName("Cúp Mùa Xuân");
        reg.setEventDate(LocalDate.of(2026, 11, 1));
        reg.setStatus(RaceRegistrationStatus.APPROVED);
        when(raceRegistrationRepository.findByTrainerIdOrderByCreatedAtDesc(200L)).thenReturn(List.of(reg));

        Horse h = new Horse();
        h.setId(10L);
        h.setName("THUNDER");
        when(horseRepository.findAll()).thenReturn(List.of(h));

        Object result = toolExecutionService.executeTool("getMyHorseRaceRegistrations", Map.of(), trainer);
        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("THUNDER", item.get("tenNgua"));
        assertEquals("Cúp Mùa Xuân", item.get("tenGiaiDua"));
        assertEquals("/trainer/racing", item.get("actionUrl"));
    }

    @Test
    @DisplayName("HEAD_TRAINER gọi getAssignedAdmissionReviews tra cứu đơn chờ thẩm định")
    void testTrainer_GetAssignedAdmissionReviews() {
        AuthenticatedUser trainer = new AuthenticatedUser(200L, "trainer@rtms.com", "HEAD_TRAINER");

        AdmissionApplication app = new AdmissionApplication();
        app.setId(301L);
        app.setTrainerId(200L);
        app.setStatus(AdmissionStatus.TRAINER_REVIEW);
        app.setSubmittedAt(LocalDateTime.of(2026, 10, 15, 9, 0));
        when(admissionApplicationRepository.findAllByOrderBySubmittedAtDesc()).thenReturn(List.of(app));

        CandidateHorseProfile cand = new CandidateHorseProfile();
        cand.setAdmissionId(301L);
        cand.setName("WIND RUNNER");
        cand.setBreed("Thoroughbred");
        when(candidateHorseProfileRepository.findByAdmissionIdIn(any())).thenReturn(List.of(cand));

        Object result = toolExecutionService.executeTool("getAssignedAdmissionReviews", Map.of(), trainer);
        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("WIND RUNNER", item.get("tenNgua"));
        assertEquals("Thoroughbred", item.get("giongNgua"));
        assertEquals("/trainer/admissions", item.get("actionUrl"));
    }

    @Test
    @DisplayName("HEAD_TRAINER gọi getCourseCurriculumOverview tra cứu danh mục khóa học và môn học")
    void testTrainer_GetCourseCurriculumOverview() {
        AuthenticatedUser trainer = new AuthenticatedUser(200L, "trainer@rtms.com", "HEAD_TRAINER");

        Course c = new Course();
        c.setId(1L);
        c.setName("Khóa Tốc Độ Cao");
        c.setTargetGoal("Nâng cao bứt tốc");
        c.setTotalSessions(12);
        when(courseRepository.findAll()).thenReturn(List.of(c));

        Subject s = new Subject();
        s.setId(10L);
        s.setName("Nước rút 400m");
        s.setCategory(SubjectCategory.SPEED);
        s.setSurfaceType(SurfaceType.TURF);
        s.setTargetDistanceMeters(BigDecimal.valueOf(400));
        s.setIntensityLevel(IntensityLevel.HIGH);
        s.setDurationMinutes(45);
        when(subjectRepository.findAll()).thenReturn(List.of(s));

        Object result = toolExecutionService.executeTool("getCourseCurriculumOverview", Map.of(), trainer);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertTrue(map.containsKey("danhMucKhoaHoc"));
        assertTrue(map.containsKey("danhMucMonHocKyThuat"));
        assertEquals("/trainer/courses", map.get("actionUrl"));
    }

    @Test
    @DisplayName("VETERINARIAN gọi getPendingAdmissionExams tra cứu đơn chờ khám sàng lọc")
    void testVet_GetPendingAdmissionExams() {
        AuthenticatedUser vet = new AuthenticatedUser(300L, "vet@rtms.com", "VETERINARIAN");

        AdmissionApplication app = new AdmissionApplication();
        app.setId(401L);
        app.setStatus(AdmissionStatus.VET_REVIEW);
        app.setSubmittedAt(LocalDateTime.of(2026, 10, 18, 8, 30));
        when(admissionApplicationRepository.findAllByOrderBySubmittedAtDesc()).thenReturn(List.of(app));

        CandidateHorseProfile cand = new CandidateHorseProfile();
        cand.setAdmissionId(401L);
        cand.setName("SILVER BULLET");
        when(candidateHorseProfileRepository.findByAdmissionIdIn(any())).thenReturn(List.of(cand));

        Object result = toolExecutionService.executeTool("getPendingAdmissionExams", Map.of(), vet);
        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("SILVER BULLET", item.get("tenNgua"));
        assertEquals("Chờ bác sĩ khám sàng lọc", item.get("trangThai"));
        assertEquals("/veterinarian/admissions", item.get("actionUrl"));
    }

    @Test
    @DisplayName("VETERINARIAN gọi getUrgentCareAlerts tra cứu ca cấp cứu khẩn cấp")
    void testVet_GetUrgentCareAlerts() {
        AuthenticatedUser vet = new AuthenticatedUser(300L, "vet@rtms.com", "VETERINARIAN");

        CareSchedule cs = new CareSchedule();
        cs.setId(501L);
        cs.setHorseId(10L);
        cs.setCareType(CareType.URGENT);
        cs.setStatus(CareScheduleStatus.SCHEDULED);
        cs.setDescription("Ngựa sốt cao đột ngột");
        cs.setScheduledAt(LocalDateTime.of(2026, 10, 20, 14, 0));
        when(careScheduleRepository.findAll()).thenReturn(List.of(cs));

        Horse h = new Horse();
        h.setId(10L);
        h.setName("LIGHTNING");
        when(horseRepository.findAll()).thenReturn(List.of(h));

        Object result = toolExecutionService.executeTool("getUrgentCareAlerts", Map.of(), vet);
        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("LIGHTNING", item.get("tenNgua"));
        assertEquals("Khẩn cấp", item.get("mucDo"));
        assertEquals("Ngựa sốt cao đột ngột", item.get("trieuChung"));
        assertEquals("/veterinarian/urgent", item.get("actionUrl"));
    }

    @Test
    @DisplayName("VETERINARIAN gọi getHorseVitalSignsTrend tra cứu chỉ số sinh hiệu")
    void testVet_GetHorseVitalSignsTrend() {
        AuthenticatedUser vet = new AuthenticatedUser(300L, "vet@rtms.com", "VETERINARIAN");

        Horse h = new Horse();
        h.setId(10L);
        h.setName("LIGHTNING");
        when(horseRepository.findById(10L)).thenReturn(Optional.of(h));

        HorseHealthMetric metric = new HorseHealthMetric();
        metric.setHorseId(10L);
        metric.setWeight(BigDecimal.valueOf(480));
        metric.setTemperature(BigDecimal.valueOf(38.2));
        metric.setHeartRate(BigDecimal.valueOf(36));
        metric.setRespiratoryRate(BigDecimal.valueOf(12));
        metric.setRecordedAt(LocalDateTime.of(2026, 10, 20, 7, 0));
        when(horseHealthMetricRepository.findByHorseIdOrderByRecordedAtDesc(10L)).thenReturn(List.of(metric));

        Object result = toolExecutionService.executeTool("getHorseVitalSignsTrend", Map.of("horseId", 10L), vet);
        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("LIGHTNING", item.get("tenNgua"));
        assertEquals("480 kg", item.get("canNang"));
        assertEquals("38.2 °C", item.get("thanNhiet"));
        assertEquals("36 bpm", item.get("nhipTim"));
        assertEquals("12 nhịp/phút", item.get("nhipTho"));
    }

    @Test
    @DisplayName("VETERINARIAN gọi getHorseVitalSignsTrend không truyền ngựa hoặc truyền tất cả trả về danh sách các bản ghi gần nhất")
    void testVet_GetHorseVitalSignsTrend_AllHorsesWhenEmptyOrAll() {
        AuthenticatedUser vet = new AuthenticatedUser(300L, "vet@rtms.com", "VETERINARIAN");

        HorseHealthMetric m1 = new HorseHealthMetric();
        m1.setHorseId(1L);
        m1.setRecordedAt(LocalDateTime.of(2026, 10, 20, 8, 0));
        m1.setTemperature(BigDecimal.valueOf(38.4));
        m1.setHeartRate(BigDecimal.valueOf(38));

        HorseHealthMetric m2 = new HorseHealthMetric();
        m2.setHorseId(2L);
        m2.setRecordedAt(LocalDateTime.of(2026, 10, 20, 9, 0));
        m2.setTemperature(BigDecimal.valueOf(38.1));
        m2.setHeartRate(BigDecimal.valueOf(35));

        Horse h1 = new Horse();
        h1.setId(1L);
        h1.setName("BELLA");

        Horse h2 = new Horse();
        h2.setId(2L);
        h2.setName("LIGHTNING");

        when(horseHealthMetricRepository.findAll()).thenReturn(List.of(m1, m2));
        when(horseRepository.findById(1L)).thenReturn(Optional.of(h1));
        when(horseRepository.findById(2L)).thenReturn(Optional.of(h2));

        // 1. Không truyền param
        Object resultEmpty = toolExecutionService.executeTool("getHorseVitalSignsTrend", Map.of(), vet);
        assertNotNull(resultEmpty);
        assertTrue(resultEmpty instanceof List<?>);
        List<?> listEmpty = (List<?>) resultEmpty;
        assertEquals(2, listEmpty.size());
        Map<?, ?> first = (Map<?, ?>) listEmpty.get(0);
        // m2 có recordedAt lúc 9:00 > 8:00 nên đứng đầu
        assertEquals("LIGHTNING", first.get("tenNgua"));
        assertEquals("38.1 °C", first.get("thanNhiet"));

        // 2. Truyền horseName = "tất cả"
        Object resultAll = toolExecutionService.executeTool("getHorseVitalSignsTrend", Map.of("horseName", "tất cả"), vet);
        assertNotNull(resultAll);
        assertEquals(2, ((List<?>) resultAll).size());
    }

    @Test
    @DisplayName("GROOM gọi getMyReportedIncidents tra cứu sự cố do chính Groom báo cáo")
    void testGroom_GetMyReportedIncidents() {
        AuthenticatedUser groom = new AuthenticatedUser(400L, "groom@rtms.com", "GROOM");

        HorseIncidentReport rep = new HorseIncidentReport();
        rep.setId(601L);
        rep.setReportedById(400L);
        rep.setHorseId(10L);
        rep.setTitle("Móng chân trái sau nứt nhẹ");
        rep.setSeverity(com.rtms.backend.incident.enums.IncidentSeverity.LOW);
        rep.setStatus(com.rtms.backend.incident.enums.IncidentStatus.REPORTED);
        rep.setReportedAt(LocalDateTime.now().minusDays(1));
        when(horseIncidentReportRepository.findByReportedById(400L)).thenReturn(List.of(rep));

        Horse h = new Horse();
        h.setId(10L);
        h.setName("SHADOW");
        when(horseRepository.findAll()).thenReturn(List.of(h));

        Object result = toolExecutionService.executeTool("getMyReportedIncidents", Map.of(), groom);
        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("SHADOW", item.get("tenNgua"));
        assertEquals("Móng chân trái sau nứt nhẹ", item.get("tieuDe"));
        assertEquals("/groom/incidents", item.get("actionUrl"));
    }

    @Test
    @DisplayName("GROOM gọi getGroomTaskCompletionRate kiểm tra tiến độ hoàn thành nhiệm vụ")
    void testGroom_GetGroomTaskCompletionRate() {
        AuthenticatedUser groom = new AuthenticatedUser(400L, "groom@rtms.com", "GROOM");

        GroomDailyTask t1 = new GroomDailyTask();
        t1.setId(1L);
        t1.setGroomId(400L);
        t1.setIsCompleted(true);

        GroomDailyTask t2 = new GroomDailyTask();
        t2.setId(2L);
        t2.setGroomId(400L);
        t2.setIsCompleted(false);

        when(groomDailyTaskRepository.findByGroomIdAndScheduledTimeBetween(eq(400L), any(), any()))
                .thenReturn(List.of(t1, t2));

        Object result = toolExecutionService.executeTool("getGroomTaskCompletionRate", Map.of(), groom);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(2, map.get("tongSoNhiemVu"));
        assertEquals(1L, map.get("soNhiemVuHoanThanh"));
        assertEquals("50.0%", map.get("tyLeHoanThanh"));
        assertEquals("/groom/tasks", map.get("actionUrl"));
    }

    @Test
    @DisplayName("HORSE_OWNER gọi analyzeHorseInvestmentAndFitnessROI xem tiến độ thể lực và hiệu quả đầu tư")
    void testOwner_AnalyzeHorseInvestmentAndFitnessROI() {
        AuthenticatedUser owner = new AuthenticatedUser(100L, "owner@rtms.com", "HORSE_OWNER");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("RED RUM");
        horse.setRegistrationNumber("VN-HORSE-10");
        horse.setOwnerId(100L);
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        TrainingWorkout w1 = new TrainingWorkout();
        w1.setId(1L);
        w1.setStatus(WorkoutStatus.COMPLETED);
        w1.setPerformanceRating(8);
        w1.setTopSpeedKmh(new BigDecimal("58.2"));

        when(trainingWorkoutRepository.findByHorseIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(w1));

        Object result = toolExecutionService.executeTool("analyzeHorseInvestmentAndFitnessROI", Map.of("horseId", 10L), owner);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("RED RUM", map.get("tenNgua"));
        assertEquals("VN-HORSE-10", map.get("maDangKy"));
        assertEquals(1, map.get("tongSoBuoiTap"));
        assertEquals(1, map.get("tongSoBuoiTapDatYeuCau"));
        assertNotNull(map.get("tocDoPhatTrienTheLuc"));
        assertNotNull(map.get("danhGiaTiemNangThiDau"));
        assertEquals("/owner/horses", map.get("actionUrl"));
    }

    @Test
    @DisplayName("HEAD_TRAINER gọi analyzeWorkoutLoadAndOvertrainingRisk phân tích nguy cơ quá tải 14 ngày qua")
    void testTrainer_AnalyzeWorkoutLoadAndOvertrainingRisk() {
        AuthenticatedUser trainer = new AuthenticatedUser(200L, "trainer@rtms.com", "HEAD_TRAINER");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("THUNDER");
        horse.setRegistrationNumber("VN-THUNDER");
        horse.setTrainingDecision(TrainingDecision.ALLOWED);
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        TrainingWorkout w1 = new TrainingWorkout();
        w1.setId(1L);
        w1.setActualDistanceMeters(new BigDecimal("1200"));
        w1.setAverageHeartRate(98);
        w1.setMaxHeartRate(112);
        w1.setRecoveryHeartRate(88);
        w1.setTopSpeedKmh(new BigDecimal("54.0"));
        w1.setCreatedAt(LocalDateTime.now().minusDays(2));

        when(trainingWorkoutRepository.findByHorseIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(w1));

        Object result = toolExecutionService.executeTool("analyzeWorkoutLoadAndOvertrainingRisk", Map.of("horseId", 10L), trainer);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("THUNDER", map.get("tenNgua"));
        assertEquals("VN-THUNDER", map.get("maDangKy"));
        assertEquals("1.2 km", map.get("tongCuLy14Ngay"));
        assertEquals(1, map.get("soBuoiTap"));
        assertEquals("98 bpm", map.get("nhipTimTrungBinh"));
        assertEquals("Thấp", map.get("danhGiaNguyCo"));
        assertEquals("/trainer/dashboard", map.get("actionUrl"));
    }

    @Test
    @DisplayName("HEAD_TRAINER gọi recommendOptimalRaceDistanceAndTrack đề xuất cự ly và mặt sân tối ưu")
    void testTrainer_RecommendOptimalRaceDistanceAndTrack() {
        AuthenticatedUser trainer = new AuthenticatedUser(200L, "trainer@rtms.com", "HEAD_TRAINER");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("THUNDER");
        horse.setRegistrationNumber("VN-THUNDER");
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        HorseSpecialtyResponse specialty = new HorseSpecialtyResponse(
                3,
                List.of(DistanceBand.SPRINT),
                SurfaceType.TURF,
                List.of()
        );
        when(horseSpecialtyService.getSpecialties(List.of(10L))).thenReturn(Map.of(10L, specialty));

        TrainingWorkout w1 = new TrainingWorkout();
        w1.setTopSpeedKmh(new BigDecimal("60.5"));
        when(trainingWorkoutRepository.findByHorseIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(w1));

        Object result = toolExecutionService.executeTool("recommendOptimalRaceDistanceAndTrack", Map.of("horseId", 10L), trainer);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("THUNDER", map.get("tenNgua"));
        assertTrue(map.get("cuLyDeXuat").toString().contains("Sprint"));
        assertTrue(map.get("matSanUaThich").toString().contains("Turf"));
        assertEquals("60.5 km/h", map.get("tocDoToiUu"));
        assertEquals("/trainer/racing", map.get("actionUrl"));
    }

    @Test
    @DisplayName("HEAD_TRAINER gọi recommendNextWorkoutAdjustment điều chỉnh bài tập kế tiếp")
    void testTrainer_RecommendNextWorkoutAdjustment() {
        AuthenticatedUser trainer = new AuthenticatedUser(200L, "trainer@rtms.com", "HEAD_TRAINER");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("THUNDER");
        horse.setRegistrationNumber("VN-THUNDER");
        horse.setTrainingDecision(TrainingDecision.ALLOWED);
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        TrainingWorkout w1 = new TrainingWorkout();
        w1.setActualDistanceMeters(new BigDecimal("1000"));
        w1.setMaxHeartRate(125);
        w1.setStatus(WorkoutStatus.COMPLETED);
        when(trainingWorkoutRepository.findByHorseIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(w1));

        Object result = toolExecutionService.executeTool("recommendNextWorkoutAdjustment", Map.of("horseId", 10L), trainer);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("THUNDER", map.get("tenNgua"));
        assertTrue(map.get("nhanXetTheLuc").toString().contains("vượt ngưỡng an toàn"));
        assertTrue(map.get("baiTapKeTiepDeXuat").toString().contains("Giảm tải"));
        assertEquals("/trainer/plans", map.get("actionUrl"));
    }

    @Test
    @DisplayName("HEAD_TRAINER gọi recommendNextWorkoutAdjustment với ngựa bị BLOCKED loại trừ mâu thuẫn dữ liệu")
    void testTrainer_RecommendNextWorkoutAdjustment_Blocked() {
        AuthenticatedUser trainer = new AuthenticatedUser(200L, "trainer@rtms.com", "HEAD_TRAINER");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("THUNDER");
        horse.setRegistrationNumber("VN-THUNDER");
        horse.setTrainingDecision(TrainingDecision.BLOCKED);
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        Object result = toolExecutionService.executeTool("recommendNextWorkoutAdjustment", Map.of("horseId", 10L), trainer);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertTrue(map.get("baiTapKeTiepDeXuat").toString().contains("Nghỉ ngơi"));
    }

    @Test
    @DisplayName("VETERINARIAN gọi analyzeIncidentPatternsAndRecurringRisks phân tích nguy cơ mãn tính")
    void testVet_AnalyzeIncidentPatternsAndRecurringRisks() {
        AuthenticatedUser vet = new AuthenticatedUser(300L, "vet@rtms.com", "VETERINARIAN");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("LIGHTNING");
        horse.setRegistrationNumber("VN-LIGHTNING");
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        HorseIncidentReport ir = new HorseIncidentReport();
        ir.setTitle("Viêm gân nhẹ");
        when(horseIncidentReportRepository.findByHorseId(10L)).thenReturn(List.of(ir));

        HealthRecord hr = new HealthRecord();
        hr.setDiagnosis("Viêm khớp cổ chân");
        when(healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(10L)).thenReturn(List.of(hr));

        Object result = toolExecutionService.executeTool("analyzeIncidentPatternsAndRecurringRisks", Map.of("horseId", 10L), vet);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("LIGHTNING", map.get("tenNgua"));
        assertEquals(1, map.get("tongSoSuCo"));
        assertEquals(1, map.get("tongSoLanKhamBenh"));
        assertTrue(map.get("benhLyPhoBien").toString().contains("Viêm khớp"));
        assertEquals("/veterinarian/admissions", map.get("actionUrl"));
    }

    @Test
    @DisplayName("VETERINARIAN gọi recommendClinicalInterventionPlan đề xuất phác đồ từ RAG")
    void testVet_RecommendClinicalInterventionPlan() {
        AuthenticatedUser vet = new AuthenticatedUser(300L, "vet@rtms.com", "VETERINARIAN");

        ClinicalChunkResponseDto chunk = new ClinicalChunkResponseDto(
                1L, "Phác đồ điều trị Colic ở ngựa", "Tiêu hóa",
                "Đặt ống thông dạ dày, sử dụng Flunixin Meglumine", 0.88, Map.of()
        );
        when(clinicalRetrievalService.retrieveSimilarClinicalRecords(anyString(), any(), anyInt()))
                .thenReturn(List.of(chunk));

        Object result = toolExecutionService.executeTool(
                "recommendClinicalInterventionPlan",
                Map.of("symptoms", "Ngựa có biểu hiện cào đất, bỏ ăn, đau quặn bụng"),
                vet
        );
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertTrue(map.get("phacDoCanThiepDeXuat").toString().contains("Bước 1"));
        assertNotNull(map.get("xetNghiemCanLam"));
        assertNotNull(map.get("thoiGianTheoDoiToiThieu"));
        assertEquals("/veterinarian/urgent", map.get("actionUrl"));
    }

    @Test
    @DisplayName("VETERINARIAN gọi evaluateTrainingResumptionReadiness đánh giá sẵn sàng quay lại tập")
    void testVet_EvaluateTrainingResumptionReadiness() {
        AuthenticatedUser vet = new AuthenticatedUser(300L, "vet@rtms.com", "VETERINARIAN");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("LIGHTNING");
        horse.setRegistrationNumber("VN-LIGHTNING");
        horse.setTrainingDecision(TrainingDecision.ALLOWED);
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        HorseHealthMetric m = new HorseHealthMetric();
        m.setTemperature(new BigDecimal("37.8"));
        m.setHeartRate(new BigDecimal("38"));
        when(horseHealthMetricRepository.findByHorseIdOrderByRecordedAtDesc(10L)).thenReturn(List.of(m));

        Object result = toolExecutionService.executeTool("evaluateTrainingResumptionReadiness", Map.of("horseId", 10L), vet);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("LIGHTNING", map.get("tenNgua"));
        assertEquals("85/100", map.get("diemSanSang"));
        assertEquals("Đủ điều kiện tập nhẹ", map.get("khuyenNghiBacSi"));
        assertEquals("/veterinarian/preventive-care", map.get("actionUrl"));
    }

    @Test
    @DisplayName("GROOM gọi detectHorseBehavioralAnomaly phát hiện sớm triệu chứng bất thường")
    void testGroom_DetectHorseBehavioralAnomaly() {
        AuthenticatedUser groom = new AuthenticatedUser(400L, "groom@rtms.com", "GROOM");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("SHADOW");
        horse.setRegistrationNumber("VN-SHADOW");
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        HorseIncidentReport rep = new HorseIncidentReport();
        rep.setTitle("Ngựa bỏ ăn và cào đất liên tục");
        rep.setDescription("Có dấu hiệu đau bụng không yên");
        when(horseIncidentReportRepository.findByHorseId(10L)).thenReturn(List.of(rep));

        HorseHealthMetric m = new HorseHealthMetric();
        m.setHeartRate(new BigDecimal("48"));
        when(horseHealthMetricRepository.findByHorseIdOrderByRecordedAtDesc(10L)).thenReturn(List.of(m));

        Object result = toolExecutionService.executeTool("detectHorseBehavioralAnomaly", Map.of("horseId", 10L), groom);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("SHADOW", map.get("tenNgua"));
        assertEquals("Khẩn cấp", map.get("mucDoCanhBao"));
        assertTrue(map.get("hanhDongCanLamNgay").toString().contains("Báo ngay cho Bác sĩ"));
        assertEquals("/groom/incidents", map.get("actionUrl"));
    }

    @Test
    @DisplayName("GROOM gọi recommendDailyCarePriorityOrder đề xuất thứ tự ưu tiên chăm sóc trong ngày")
    void testGroom_RecommendDailyCarePriorityOrder() {
        AuthenticatedUser groom = new AuthenticatedUser(400L, "groom@rtms.com", "GROOM");

        GroomDailyTask t1 = new GroomDailyTask(400L, 10L, GroomTaskType.FEEDING, LocalDateTime.now().withHour(8), "Cho ăn");
        GroomDailyTask t2 = new GroomDailyTask(400L, 10L, GroomTaskType.VET_ASSIST, LocalDateTime.now().withHour(7), "Hỗ trợ tiêm phòng");
        when(groomDailyTaskRepository.findByGroomIdAndScheduledTimeBetween(eq(400L), any(), any()))
                .thenReturn(List.of(t1, t2));

        Horse h = new Horse();
        h.setId(10L);
        h.setName("SHADOW");
        when(horseRepository.findAll()).thenReturn(List.of(h));

        Object result = toolExecutionService.executeTool("recommendDailyCarePriorityOrder", Map.of(), groom);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertTrue(map.containsKey("danhSachThuTuUuTien"));
        assertTrue(map.containsKey("luuYVeSinhPhongDich"));
        assertEquals("/groom/tasks", map.get("actionUrl"));
    }

    @Test
    @DisplayName("CLUB_MANAGER gọi analyzeStableCapacityAndStallBottlenecks phân tích công suất chuồng trại")
    void testManager_AnalyzeStableCapacityAndStallBottlenecks() {
        AuthenticatedUser manager = new AuthenticatedUser(1L, "manager@rtms.com", "CLUB_MANAGER");

        when(stableStallRepository.count()).thenReturn(50L);
        when(stableStallRepository.countAvailableRegularStalls()).thenReturn(5L);
        when(stableStallRepository.countAvailableQuarantineStalls()).thenReturn(2L);
        when(stableStallRepository.countOccupiedQuarantineStalls()).thenReturn(8L);

        AdmissionApplication app = new AdmissionApplication();
        app.setStatus(AdmissionStatus.VET_REVIEW);
        when(admissionApplicationRepository.findAll()).thenReturn(List.of(app));

        Object result = toolExecutionService.executeTool("analyzeStableCapacityAndStallBottlenecks", Map.of(), manager);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(50L, map.get("tongSoChuong"));
        assertEquals(7L, map.get("soChuongTrong"));
        assertEquals(8L, map.get("soCaCachLy"));
        assertNotNull(map.get("duBaoDiemNghen"));
        assertEquals("/manager/stable", map.get("actionUrl"));
    }

    @Test
    @DisplayName("CLUB_MANAGER gọi recommendGroomWorkloadRebalancing đề xuất tái cân bằng tải Groom")
    void testManager_RecommendGroomWorkloadRebalancing() {
        AuthenticatedUser manager = new AuthenticatedUser(1L, "manager@rtms.com", "CLUB_MANAGER");

        User g1 = new User();
        g1.setId(10L);
        g1.setFullName("Groom Nam");
        Role r = new Role();
        r.setName("GROOM");
        g1.setRole(r);
        g1.setActive(true);

        User g2 = new User();
        g2.setId(20L);
        g2.setFullName("Groom Tuấn");
        g2.setRole(r);
        g2.setActive(true);

        when(userRepository.findAll()).thenReturn(List.of(g1, g2));
        when(stableStallRepository.findByGroomId(10L)).thenReturn(List.of(new StableStall(), new StableStall(), new StableStall(), new StableStall(), new StableStall(), new StableStall()));
        when(stableStallRepository.findByGroomId(20L)).thenReturn(List.of(new StableStall()));

        Object result = toolExecutionService.executeTool("recommendGroomWorkloadRebalancing", Map.of(), manager);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(2, map.get("tongSoGroom"));
        assertTrue(map.containsKey("danhSachGroomKemTaiTrong"));
        assertNotNull(map.get("deXuatDieuChuyenChuong"));
        assertEquals("/manager/staff", map.get("actionUrl"));
    }
}
