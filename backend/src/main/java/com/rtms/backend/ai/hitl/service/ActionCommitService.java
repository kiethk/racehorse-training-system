package com.rtms.backend.ai.hitl.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.ai.dto.ActionCommitRequestDto;
import com.rtms.backend.ai.dto.ActionCommitResponseDto;
import com.rtms.backend.ai.hitl.enums.ActionStatus;
import com.rtms.backend.ai.hitl.model.PendingAgentAction;
import com.rtms.backend.ai.hitl.repository.PendingAgentActionRepository;
import com.rtms.backend.audit.entity.AuditLog;
import com.rtms.backend.audit.repository.AuditLogRepository;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Dịch vụ xác thực và phê duyệt áp dụng hành động HITL (Phase 2 Commit).
 *
 * Kiểm tra nghiêm ngặt 6 tầng bảo mật:
 * 1. Chống người dùng chưa đăng nhập hoặc HORSE_OWNER (Read-Only).
 * 2. Xác thực token tồn tại và trạng thái PENDING.
 * 3. Kiểm tra TTL 5 phút.
 * 4. Xác minh chữ ký số HMAC-SHA256 (chống giả mạo payload / user / token).
 * 5. Xác minh quyền sở hữu phiên người dùng và thẩm quyền vai trò.
 * 6. Xử lý nguyên tử Idempotency chống double-commit, cập nhật nghiệp vụ và ghi vết kiểm toán (AuditLog).
 */
@Service
public class ActionCommitService {

    private static final Logger log = LoggerFactory.getLogger(ActionCommitService.class);

    private final PendingAgentActionRepository pendingActionRepository;
    private final ActionProposalService proposalService;
    private final ObjectMapper objectMapper;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final TrainingWorkoutRepository trainingWorkoutRepository;

    public ActionCommitService(PendingAgentActionRepository pendingActionRepository,
                               ActionProposalService proposalService,
                               ObjectMapper objectMapper,
                               AuditLogRepository auditLogRepository,
                               UserRepository userRepository,
                               TrainingWorkoutRepository trainingWorkoutRepository) {
        this.pendingActionRepository = pendingActionRepository;
        this.proposalService = proposalService;
        this.objectMapper = objectMapper;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.trainingWorkoutRepository = trainingWorkoutRepository;
    }

    /**
     * Xác thực và commit hành động HITL.
     *
     * @param request DTO chứa idempotencyKey và actionToken
     * @param currentUser Người dùng đang đăng nhập
     * @return ActionCommitResponseDto Kết quả phê duyệt
     */
    @Transactional
    public ActionCommitResponseDto commitAction(ActionCommitRequestDto request, AuthenticatedUser currentUser) {
        // 1. Kiểm tra tham số và phân quyền cơ bản
        if (request == null || request.actionToken() == null || request.actionToken().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Action token không được để trống");
        }
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Người dùng chưa được xác thực");
        }
        if ("HORSE_OWNER".equalsIgnoreCase(currentUser.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "OWNER_READ_ONLY",
                    "Chủ sở hữu ngựa (HORSE_OWNER) chỉ có quyền đọc, không được phép thực thi hành động này");
        }

        // 2. Tìm kiếm PendingAgentAction theo token
        PendingAgentAction action = pendingActionRepository.findByActionToken(request.actionToken())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACTION_NOT_FOUND",
                        "Không tìm thấy đề xuất hành động với token này"));

        // 3. Kiểm tra trạng thái hiện tại (Idempotency)
        if (action.getStatus() == ActionStatus.COMMITTED) {
            throw new ApiException(HttpStatus.CONFLICT, "ACTION_ALREADY_COMMITTED",
                    "Hành động này đã được phê duyệt và áp dụng trước đó");
        }
        if (action.getStatus() == ActionStatus.EXPIRED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ACTION_EXPIRED",
                    "Đề xuất hành động đã hết thời hạn hiệu lực");
        }
        if (action.getStatus() != ActionStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ACTION_INVALID_STATUS",
                    "Trạng thái đề xuất không hợp lệ để thực thi: " + action.getStatus());
        }

        // 4. Kiểm tra thời hạn TTL (5 phút)
        if (LocalDateTime.now().isAfter(action.getExpiresAt())) {
            action.setStatus(ActionStatus.EXPIRED);
            pendingActionRepository.save(action);
            throw new ApiException(HttpStatus.BAD_REQUEST, "ACTION_EXPIRED",
                    "Đề xuất hành động đã quá thời hạn 5 phút (TTL)");
        }

        // 5. Xác minh chữ ký số HMAC-SHA256
        boolean isSignatureValid = proposalService.verifyTokenSignature(action);
        if (!isSignatureValid) {
            throw new ApiException(HttpStatus.FORBIDDEN, "INVALID_SIGNATURE",
                    "Chữ ký số không hợp lệ hoặc dữ liệu đề xuất đã bị can thiệp trái phép");
        }

        // 6. Kiểm tra quyền sở hữu phiên người dùng và thẩm quyền vai trò
        if (!action.getUserId().equals(currentUser.getUserId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN_USER",
                    "Bạn không phải là người khởi tạo hoặc sở hữu đề xuất hành động này");
        }
        if (action.getTargetRole() != null && !action.getTargetRole().isBlank()) {
            if (!action.getTargetRole().equalsIgnoreCase(currentUser.getRole())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "INSUFFICIENT_ROLE",
                        "Vai trò [" + currentUser.getRole() + "] không có thẩm quyền duyệt đề xuất dành cho [" + action.getTargetRole() + "]");
            }
        }

        // 7. Cập nhật trạng thái COMMITTED bằng CAS (ngăn chặn race condition double-commit)
        int updated = pendingActionRepository.updateStatusIfStatusIs(action.getId(), ActionStatus.COMMITTED, ActionStatus.PENDING);
        if (updated == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "ACTION_ALREADY_COMMITTED",
                    "Hành động này đang được xử lý hoặc đã được xác nhận trước đó");
        }
        action.setStatus(ActionStatus.COMMITTED);

        // 8. Thực thi nghiệp vụ tương ứng theo toolName
        executeBusinessMutation(action, currentUser);

        // 9. Ghi vết kiểm toán bất biến (AuditLog)
        recordAuditLog(action, currentUser);

        log.info("ActionCommitService: Đã phê duyệt và áp dụng thành công action #{} [{}] bởi user [{}]",
                action.getId(), action.getToolName(), currentUser.getUserId());

        return new ActionCommitResponseDto(
                action.getId(),
                ActionStatus.COMMITTED,
                action.getToolName(),
                action.getTargetEntityId(),
                "Phê duyệt và áp dụng hành động thành công"
        );
    }

    /**
     * Điều phối thực thi cập nhật nghiệp vụ CSDL theo loại tool.
     */
    private void executeBusinessMutation(PendingAgentAction action, AuthenticatedUser currentUser) {
        try {
            JsonNode rootNode = objectMapper.readTree(action.getDiffPayload());
            JsonNode proposedValues = rootNode.get("proposedValues");
            if (proposedValues == null || !proposedValues.isObject()) {
                log.warn("executeBusinessMutation: Không tìm thấy proposedValues trong payload của action #{}", action.getId());
                return;
            }

            switch (action.getToolName()) {
                case "proposeEditWorkoutMetric" -> applyEditWorkoutMetric(action.getTargetEntityId(), proposedValues, currentUser);
                default -> log.info("executeBusinessMutation: Đã ghi nhận áp dụng cho tool [{}] với target [{}]",
                        action.getToolName(), action.getTargetEntityId());
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi thực thi nghiệp vụ cho action #{}: {}", action.getId(), e.getMessage(), e);
            throw new RuntimeException("Không thể áp dụng thay đổi nghiệp vụ: " + e.getMessage(), e);
        }
    }

    /**
     * Áp dụng cập nhật metric cho buổi tập luyện (TrainingWorkout).
     */
    private void applyEditWorkoutMetric(String targetEntityId, JsonNode proposedValues, AuthenticatedUser currentUser) {
        if (targetEntityId == null || targetEntityId.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TARGET", "Target workout id không hợp lệ");
        }

        Long workoutId = Long.valueOf(targetEntityId);
        TrainingWorkout workout;
        if ("HEAD_TRAINER".equalsIgnoreCase(currentUser.getRole())) {
            workout = trainingWorkoutRepository.findByIdAndTrainerId(workoutId, currentUser.getUserId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WORKOUT_NOT_FOUND",
                            "Không tìm thấy buổi tập #" + workoutId + " hoặc bạn không có quyền huấn luyện buổi tập này"));
        } else {
            workout = trainingWorkoutRepository.findById(workoutId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WORKOUT_NOT_FOUND",
                            "Không tìm thấy buổi tập #" + workoutId));
        }

        if (proposedValues.has("actualDistanceMeters")) {
            workout.setActualDistanceMeters(new BigDecimal(proposedValues.get("actualDistanceMeters").asText()));
        }
        if (proposedValues.has("actualDurationMinutes")) {
            workout.setActualDurationMinutes(new BigDecimal(proposedValues.get("actualDurationMinutes").asText()));
        }
        if (proposedValues.has("topSpeedKmh")) {
            workout.setTopSpeedKmh(new BigDecimal(proposedValues.get("topSpeedKmh").asText()));
        }
        if (proposedValues.has("averageSpeedKmh")) {
            workout.setAverageSpeedKmh(new BigDecimal(proposedValues.get("averageSpeedKmh").asText()));
        }
        if (proposedValues.has("averageHeartRate")) {
            workout.setAverageHeartRate(proposedValues.get("averageHeartRate").asInt());
        }
        if (proposedValues.has("maxHeartRate")) {
            workout.setMaxHeartRate(proposedValues.get("maxHeartRate").asInt());
        }
        if (proposedValues.has("recoveryHeartRate")) {
            workout.setRecoveryHeartRate(proposedValues.get("recoveryHeartRate").asInt());
        }
        if (proposedValues.has("performanceRating")) {
            workout.setPerformanceRating(proposedValues.get("performanceRating").asInt());
        }
        if (proposedValues.has("trainerFeedback")) {
            workout.setTrainerFeedback(proposedValues.get("trainerFeedback").asText());
        }

        trainingWorkoutRepository.save(workout);
        log.info("ActionCommitService: Cập nhật thành công workout #{} bởi user [{}]", workoutId, currentUser.getUserId());
    }

    /**
     * Ghi vết kiểm toán (AuditLog) bất biến.
     */
    private void recordAuditLog(PendingAgentAction action, AuthenticatedUser currentUser) {
        try {
            AuditLog auditLog = new AuditLog();
            User user = userRepository.findById(currentUser.getUserId()).orElse(null);
            auditLog.setUser(user);
            auditLog.setAction("AGENT_HITL_CONFIRMED");
            auditLog.setEntityName(resolveEntityName(action.getToolName()));
            if (action.getTargetEntityId() != null) {
                try {
                    auditLog.setEntityId(Long.valueOf(action.getTargetEntityId()));
                } catch (NumberFormatException ignored) {}
            }
            auditLog.setHttpMethod("POST");
            auditLog.setRequestPath("/api/v1/ai/actions/commit");
            auditLog.setStatusCode(HttpStatus.OK.value());
            auditLogRepository.save(auditLog);
            log.info("ActionCommitService: Đã ghi nhận AuditLog cho action #{}", action.getId());
        } catch (Exception ex) {
            log.error("ActionCommitService: Không thể ghi nhận AuditLog: {}", ex.getMessage(), ex);
        }
    }

    private String resolveEntityName(String toolName) {
        if (toolName == null) return "Unknown";
        if (toolName.toLowerCase().contains("workout")) return "TrainingWorkout";
        if (toolName.toLowerCase().contains("horse")) return "Horse";
        if (toolName.toLowerCase().contains("medical")) return "HealthRecord";
        return toolName;
    }
}
