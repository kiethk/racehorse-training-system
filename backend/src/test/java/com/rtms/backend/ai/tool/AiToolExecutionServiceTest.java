package com.rtms.backend.ai.tool;

import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.ai.dto.ActionProposalDto;
import com.rtms.backend.ai.dto.ClinicalChunkResponseDto;
import com.rtms.backend.ai.hitl.service.ActionProposalService;
import com.rtms.backend.ai.rag.service.ClinicalRetrievalService;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.repository.GroomDailyTaskRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
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
}
