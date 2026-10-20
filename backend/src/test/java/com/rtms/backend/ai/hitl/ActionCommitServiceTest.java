package com.rtms.backend.ai.hitl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.ai.dto.ActionCommitRequestDto;
import com.rtms.backend.ai.dto.ActionCommitResponseDto;
import com.rtms.backend.ai.hitl.enums.ActionStatus;
import com.rtms.backend.ai.hitl.model.PendingAgentAction;
import com.rtms.backend.ai.hitl.repository.PendingAgentActionRepository;
import com.rtms.backend.ai.hitl.service.ActionCommitService;
import com.rtms.backend.ai.hitl.service.ActionProposalService;
import com.rtms.backend.audit.entity.AuditLog;
import com.rtms.backend.audit.repository.AuditLogRepository;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ActionCommitServiceTest {

    private PendingAgentActionRepository pendingActionRepository;
    private ActionProposalService proposalService;
    private ObjectMapper objectMapper;
    private AuditLogRepository auditLogRepository;
    private UserRepository userRepository;
    private TrainingWorkoutRepository trainingWorkoutRepository;
    private ActionCommitService commitService;

    @BeforeEach
    void setUp() {
        pendingActionRepository = Mockito.mock(PendingAgentActionRepository.class);
        proposalService = Mockito.mock(ActionProposalService.class);
        objectMapper = new ObjectMapper();
        auditLogRepository = Mockito.mock(AuditLogRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        trainingWorkoutRepository = Mockito.mock(TrainingWorkoutRepository.class);

        commitService = new ActionCommitService(
                pendingActionRepository,
                proposalService,
                objectMapper,
                auditLogRepository,
                userRepository,
                trainingWorkoutRepository
        );
    }

    @Test
    @DisplayName("Commit thành công: chữ ký hợp lệ, quyền hạn chuẩn, cập nhật workout và ghi audit log")
    void testCommitActionSuccess() {
        String token = "nonce-123.valid-hmac-signature";
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@test.com", "HEAD_TRAINER");

        String diffJson = """
                {
                    "title": "Sửa cự ly bài tập #12",
                    "targetEntityId": "12",
                    "proposedValues": {
                        "actualDistanceMeters": "1600.00",
                        "topSpeedKmh": "58.50"
                    }
                }
                """;

        PendingAgentAction action = new PendingAgentAction(
                token,
                5L,
                "HEAD_TRAINER",
                "proposeEditWorkoutMetric",
                "12",
                diffJson,
                LocalDateTime.now().plusMinutes(4)
        );
        action.setId(101L);

        TrainingWorkout workout = new TrainingWorkout();
        workout.setId(12L);
        workout.setActualDistanceMeters(new BigDecimal("1200.00"));

        when(pendingActionRepository.findByActionToken(token)).thenReturn(Optional.of(action));
        when(proposalService.verifyTokenSignature(action)).thenReturn(true);
        when(pendingActionRepository.updateStatusIfStatusIs(101L, ActionStatus.COMMITTED, ActionStatus.PENDING)).thenReturn(1);
        when(trainingWorkoutRepository.findByIdAndTrainerId(12L, 5L)).thenReturn(Optional.of(workout));
        when(userRepository.findById(5L)).thenReturn(Optional.of(new User()));

        ActionCommitRequestDto request = new ActionCommitRequestDto("idemp-key-1", token);
        ActionCommitResponseDto response = commitService.commitAction(request, trainer);

        assertNotNull(response);
        assertEquals(101L, response.actionId());
        assertEquals(ActionStatus.COMMITTED, response.status());
        assertEquals("proposeEditWorkoutMetric", response.toolName());

        // Kiểm tra workout đã được cập nhật giá trị mới
        assertEquals(new BigDecimal("1600.00"), workout.getActualDistanceMeters());
        assertEquals(new BigDecimal("58.50"), workout.getTopSpeedKmh());
        verify(trainingWorkoutRepository).save(workout);

        // Kiểm tra audit log được ghi
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Chủ sở hữu HORSE_OWNER bị từ chối 100% khi cố commit thay đổi (Read-Only invariant)")
    void testCommitAction_HorseOwnerReadOnly() {
        AuthenticatedUser owner = new AuthenticatedUser(10L, "owner@test.com", "HORSE_OWNER");
        ActionCommitRequestDto request = new ActionCommitRequestDto("idemp-key-2", "some-token");

        ApiException ex = assertThrows(ApiException.class, () -> commitService.commitAction(request, owner));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("OWNER_READ_ONLY", ex.getErrorCode());
    }

    @Test
    @DisplayName("Từ chối commit khi đề xuất đã quá hạn TTL (5 phút)")
    void testCommitAction_ExpiredTTL() {
        String token = "expired-token";
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@test.com", "HEAD_TRAINER");

        PendingAgentAction expiredAction = new PendingAgentAction(
                token,
                5L,
                "HEAD_TRAINER",
                "proposeEditWorkoutMetric",
                "12",
                "{}",
                LocalDateTime.now().minusMinutes(1) // Đã quá hạn
        );

        when(pendingActionRepository.findByActionToken(token)).thenReturn(Optional.of(expiredAction));

        ActionCommitRequestDto request = new ActionCommitRequestDto("idemp-key-3", token);
        ApiException ex = assertThrows(ApiException.class, () -> commitService.commitAction(request, trainer));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("ACTION_EXPIRED", ex.getErrorCode());
        assertEquals(ActionStatus.EXPIRED, expiredAction.getStatus());
    }

    @Test
    @DisplayName("Từ chối commit khi phát hiện chữ ký số bị giả mạo / payload can thiệp")
    void testCommitAction_TamperedSignature() {
        String token = "tampered-token";
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@test.com", "HEAD_TRAINER");

        PendingAgentAction action = new PendingAgentAction(
                token,
                5L,
                "HEAD_TRAINER",
                "proposeEditWorkoutMetric",
                "12",
                "{}",
                LocalDateTime.now().plusMinutes(3)
        );

        when(pendingActionRepository.findByActionToken(token)).thenReturn(Optional.of(action));
        when(proposalService.verifyTokenSignature(action)).thenReturn(false); // Chữ ký sai!

        ActionCommitRequestDto request = new ActionCommitRequestDto("idemp-key-4", token);
        ApiException ex = assertThrows(ApiException.class, () -> commitService.commitAction(request, trainer));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("INVALID_SIGNATURE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Từ chối commit khi user cố tình duyệt đề xuất của người khác (Ownership violation)")
    void testCommitAction_WrongUserOwnership() {
        String token = "user-mismatch-token";
        AuthenticatedUser otherTrainer = new AuthenticatedUser(99L, "other@test.com", "HEAD_TRAINER");

        PendingAgentAction action = new PendingAgentAction(
                token,
                5L, // Thuộc về user #5
                "HEAD_TRAINER",
                "proposeEditWorkoutMetric",
                "12",
                "{}",
                LocalDateTime.now().plusMinutes(3)
        );

        when(pendingActionRepository.findByActionToken(token)).thenReturn(Optional.of(action));
        when(proposalService.verifyTokenSignature(action)).thenReturn(true);

        ActionCommitRequestDto request = new ActionCommitRequestDto("idemp-key-5", token);
        ApiException ex = assertThrows(ApiException.class, () -> commitService.commitAction(request, otherTrainer));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("FORBIDDEN_USER", ex.getErrorCode());
    }

    @Test
    @DisplayName("Ngăn chặn commit kép (Idempotency check / double-click)")
    void testCommitAction_AlreadyCommitted_Conflict() {
        String token = "already-committed-token";
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@test.com", "HEAD_TRAINER");

        PendingAgentAction action = new PendingAgentAction(
                token,
                5L,
                "HEAD_TRAINER",
                "proposeEditWorkoutMetric",
                "12",
                "{}",
                LocalDateTime.now().plusMinutes(3)
        );
        action.setStatus(ActionStatus.COMMITTED); // Đã commit trước đó

        when(pendingActionRepository.findByActionToken(token)).thenReturn(Optional.of(action));

        ActionCommitRequestDto request = new ActionCommitRequestDto("idemp-key-6", token);
        ApiException ex = assertThrows(ApiException.class, () -> commitService.commitAction(request, trainer));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("ACTION_ALREADY_COMMITTED", ex.getErrorCode());
    }
}
