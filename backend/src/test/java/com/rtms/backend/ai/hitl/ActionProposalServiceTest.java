package com.rtms.backend.ai.hitl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.ai.dto.ActionProposalDto;
import com.rtms.backend.ai.hitl.model.PendingAgentAction;
import com.rtms.backend.ai.hitl.repository.PendingAgentActionRepository;
import com.rtms.backend.ai.hitl.service.ActionProposalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActionProposalServiceTest {

    private PendingAgentActionRepository pendingActionRepository;
    private ObjectMapper objectMapper;
    private ActionProposalService proposalService;
    private final String testHmacSecret = "test-very-secure-hmac-secret-key-123456";

    @BeforeEach
    void setUp() {
        pendingActionRepository = Mockito.mock(PendingAgentActionRepository.class);
        objectMapper = new ObjectMapper();
        proposalService = new ActionProposalService(pendingActionRepository, objectMapper, testHmacSecret);
    }

    @Test
    @DisplayName("Tạo Proposal và ký số HMAC-SHA256 thành công")
    void testCreateProposalSuccess() {
        when(pendingActionRepository.save(any(PendingAgentAction.class))).thenAnswer(invocation -> {
            PendingAgentAction action = invocation.getArgument(0);
            action.setId(101L);
            return action;
        });

        ActionProposalDto dto = proposalService.createProposal(
                5L,
                "HEAD_TRAINER",
                "proposeEditWorkoutMetric",
                "Sửa cự ly bài tập #12",
                "12",
                Map.of("actualDistanceMeters", 1200),
                Map.of("actualDistanceMeters", 1600),
                "Nhập nhầm cự ly cảm biến"
        );

        assertNotNull(dto);
        assertEquals(101L, dto.actionId());
        assertNotNull(dto.actionToken());
        assertTrue(dto.actionToken().contains("."));
        assertEquals("proposeEditWorkoutMetric", dto.toolName());
        assertEquals(300L, dto.expiresInSeconds());
    }

    @Test
    @DisplayName("Xác minh chữ ký số HMAC-SHA256 hợp lệ và phát hiện dữ liệu bị can thiệp")
    void testVerifyTokenSignature() {
        ArgumentCaptor<PendingAgentAction> captor = ArgumentCaptor.forClass(PendingAgentAction.class);
        when(pendingActionRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        ActionProposalDto dto = proposalService.createProposal(
                5L,
                "HEAD_TRAINER",
                "proposeEditWorkoutMetric",
                "Sửa cự ly",
                "12",
                Map.of("distance", 1200),
                Map.of("distance", 1600),
                "Lý do mẫu"
        );

        PendingAgentAction savedAction = captor.getValue();
        assertNotNull(savedAction);

        // Chữ ký hợp lệ đối chiếu với bản ghi gốc
        assertTrue(proposalService.verifyTokenSignature(savedAction), "Chữ ký số hợp lệ phải được xác thực thành công");

        // Can thiệp sửa đổi userId khác -> Chữ ký phải bị bác bỏ
        PendingAgentAction tamperedUserAction = new PendingAgentAction(
                savedAction.getActionToken(),
                999L, // User giả mạo
                "HEAD_TRAINER",
                "proposeEditWorkoutMetric",
                "12",
                savedAction.getDiffPayload(),
                savedAction.getExpiresAt()
        );
        assertFalse(proposalService.verifyTokenSignature(tamperedUserAction), "Dữ liệu bị can thiệp userId phải bị từ chối");

        // Can thiệp sửa đổi nội dung diffPayload -> Chữ ký phải bị bác bỏ
        PendingAgentAction tamperedPayloadAction = new PendingAgentAction(
                savedAction.getActionToken(),
                5L,
                "HEAD_TRAINER",
                "proposeEditWorkoutMetric",
                "12",
                "{\"distance\":99999}", // Nội dung giả mạo
                savedAction.getExpiresAt()
        );
        assertFalse(proposalService.verifyTokenSignature(tamperedPayloadAction), "Dữ liệu bị can thiệp payload phải bị từ chối");
    }
}
