package com.rtms.backend.ai.tool;

import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.ai.dto.ActionProposalDto;
import com.rtms.backend.ai.dto.ClinicalChunkResponseDto;
import com.rtms.backend.ai.hitl.service.ActionProposalService;
import com.rtms.backend.ai.rag.service.ClinicalRetrievalService;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.dto.TodayTaskItemResponse;
import com.rtms.backend.stable.enums.TaskSource;
import java.time.LocalTime;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.enums.StallStatus;
import com.rtms.backend.stable.repository.GroomDailyTaskRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.stable.service.GroomDailyTaskService;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class AiToolExecutionServiceTest {

    private DynamicToolRegistryService toolRegistryService;
    private ActionProposalService actionProposalService;
    private ClinicalRetrievalService clinicalRetrievalService;
    private HorseRepository horseRepository;
    private TrainingWorkoutRepository trainingWorkoutRepository;
    private HealthRecordRepository healthRecordRepository;
    private CareScheduleRepository careScheduleRepository;
    private StableStallRepository stableStallRepository;
    private GroomDailyTaskRepository groomDailyTaskRepository;
    private GroomDailyTaskService groomDailyTaskService;
    private AdmissionApplicationRepository admissionApplicationRepository;

    private AiToolExecutionService toolExecutionService;

    @BeforeEach
    void setUp() {
        toolRegistryService = new DynamicToolRegistryService();
        actionProposalService = Mockito.mock(ActionProposalService.class);
        clinicalRetrievalService = Mockito.mock(ClinicalRetrievalService.class);
        horseRepository = Mockito.mock(HorseRepository.class);
        trainingWorkoutRepository = Mockito.mock(TrainingWorkoutRepository.class);
        healthRecordRepository = Mockito.mock(HealthRecordRepository.class);
        careScheduleRepository = Mockito.mock(CareScheduleRepository.class);
        stableStallRepository = Mockito.mock(StableStallRepository.class);
        groomDailyTaskRepository = Mockito.mock(GroomDailyTaskRepository.class);
        groomDailyTaskService = Mockito.mock(GroomDailyTaskService.class);
        admissionApplicationRepository = Mockito.mock(AdmissionApplicationRepository.class);

        toolExecutionService = new AiToolExecutionService(
                toolRegistryService,
                actionProposalService,
                clinicalRetrievalService,
                horseRepository,
                trainingWorkoutRepository,
                healthRecordRepository,
                careScheduleRepository,
                stableStallRepository,
                groomDailyTaskRepository,
                groomDailyTaskService,
                admissionApplicationRepository
        );
    }

    @Test
    @DisplayName("Chủ ngựa HORSE_OWNER bị chặn tuyệt đối khi cố gọi tool propose")
    void testOwnerReadOnly_BlocksPropose() {
        AuthenticatedUser owner = new AuthenticatedUser(10L, "owner@test.com", "HORSE_OWNER");

        ApiException ex = assertThrows(ApiException.class, () ->
                toolExecutionService.executeTool("proposeEditWorkoutMetric", Map.of(), owner));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    @DisplayName("HEAD_TRAINER gọi proposeEditWorkoutMetric tạo đề xuất HITL thành công")
    void testTrainerPropose_Success() {
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@test.com", "HEAD_TRAINER");

        TrainingWorkout workout = new TrainingWorkout();
        workout.setId(12L);
        workout.setActualDistanceMeters(new BigDecimal("1200.00"));
        when(trainingWorkoutRepository.findById(12L)).thenReturn(Optional.of(workout));

        ActionProposalDto mockProposal = new ActionProposalDto(
                101L,
                "nonce.token",
                "proposeEditWorkoutMetric",
                "Đề xuất sửa",
                "12",
                Map.of(),
                Map.of("actualDistanceMeters", 1600),
                "Cập nhật",
                300L
        );
        when(actionProposalService.createProposal(anyLong(), anyString(), anyString(), anyString(), anyString(), anyMap(), anyMap(), anyString()))
                .thenReturn(mockProposal);

        Object result = toolExecutionService.executeTool(
                "proposeEditWorkoutMetric",
                Map.of("workoutId", 12L, "actualDistanceMeters", 1600, "reason", "Cập nhật cự ly"),
                trainer
        );

        assertNotNull(result);
        assertTrue(result instanceof Map);
        Map<?, ?> resMap = (Map<?, ?>) result;
        assertEquals("HITL_PROPOSAL_CREATED", resMap.get("actionType"));
        assertEquals("nonce.token", resMap.get("actionToken"));
        assertEquals(101L, resMap.get("actionId"));
    }

    @Test
    @DisplayName("VETERINARIAN gọi searchClinicalKnowledge ủy quyền RAG Retrieval thành công")
    void testVetSearchClinicalKnowledge_Success() {
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
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("Người dùng gọi tool không nằm trong thẩm quyền vai trò sẽ bị từ chối 403")
    void testDisallowedTool_Forbidden() {
        AuthenticatedUser groom = new AuthenticatedUser(3L, "groom@test.com", "GROOM");

        ApiException ex = assertThrows(ApiException.class, () ->
                toolExecutionService.executeTool("searchClinicalKnowledge", Map.of("query", "test"), groom));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("TOOL_FORBIDDEN", ex.getErrorCode());
    }

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
        assertTrue(result instanceof Map);
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
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("GROOM gọi getHorseDietaryAndCareAlerts tra cứu cảnh báo y tế của ngựa thành công")
    void testGroom_GetHorseDietaryAndCareAlerts() {
        AuthenticatedUser groom = new AuthenticatedUser(10L, "groom1@rtms.com", "GROOM");

        Horse horse = new Horse();
        horse.setId(8L);
        horse.setName("NAUSICAA BY NIKA");
        horse.setTrainingDecision(TrainingDecision.ALLOWED);
        horse.setTrainingDecisionReason("Thể lực ổn định");
        when(horseRepository.findById(8L)).thenReturn(Optional.of(horse));

        HealthRecord record = new HealthRecord();
        record.setId(1L);
        record.setDiagnosis("Viêm gân nhẹ");
        record.setTreatment("Nghỉ ngơi 3 ngày, bôi gel lạnh");
        record.setRestrictionDetails("Không tập phi nước đại");
        record.setExaminedAt(LocalDateTime.now());
        when(healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(8L)).thenReturn(List.of(record));

        CareSchedule care = new CareSchedule();
        care.setId(3L);
        care.setCareType(CareType.ROUTINE);
        care.setScheduledAt(LocalDateTime.now().plusHours(2));
        when(careScheduleRepository.findByHorseIdAndStatusIn(eq(8L), anyCollection()))
                .thenReturn(List.of(care));

        Object result = toolExecutionService.executeTool(
                "getHorseDietaryAndCareAlerts",
                Map.of("horseId", 8L),
                groom
        );

        assertNotNull(result);
        assertTrue(result instanceof Map);
        Map<?, ?> resMap = (Map<?, ?>) result;
        assertEquals(8L, resMap.get("horseId"));
        assertEquals("NAUSICAA BY NIKA", resMap.get("horseName"));
        assertEquals("Viêm gân nhẹ", resMap.get("latestDiagnosis"));
        assertEquals("Không tập phi nước đại", resMap.get("restrictionDetails"));
    }

    @Test
    @DisplayName("GROOM gọi checkStallSuppliesStatus kiểm tra trạng thái chuồng thành công")
    void testGroom_CheckStallSuppliesStatus() {
        AuthenticatedUser groom = new AuthenticatedUser(10L, "groom1@rtms.com", "GROOM");

        StableStall stall = new StableStall();
        stall.setId(6L);
        stall.setStallCode("A-01");
        stall.setStallNumber(1);
        stall.setAreaId(2L);
        stall.setStatus(StallStatus.OCCUPIED);
        stall.setGroomId(10L);
        when(stableStallRepository.findById(6L)).thenReturn(Optional.of(stall));

        Horse horse = new Horse();
        horse.setId(8L);
        horse.setName("NAUSICAA BY NIKA");
        horse.setRegistrationNumber("VN2024001");
        when(horseRepository.findByCurrentStallId(6L)).thenReturn(Optional.of(horse));

        Object result = toolExecutionService.executeTool(
                "checkStallSuppliesStatus",
                Map.of("stallId", 6L),
                groom
        );

        assertNotNull(result);
        assertTrue(result instanceof Map);
        Map<?, ?> resMap = (Map<?, ?>) result;
        assertEquals(6L, resMap.get("stallId"));
        assertEquals("A-01", resMap.get("stallCode"));
        assertEquals(true, resMap.get("isOccupied"));
        assertEquals("NAUSICAA BY NIKA", resMap.get("horseName"));
    }
}
