package com.rtms.backend.ai.hitl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.rtms.backend.ai.dto.ActionProposalDto;
import com.rtms.backend.ai.hitl.enums.ActionStatus;
import com.rtms.backend.ai.hitl.model.PendingAgentAction;
import com.rtms.backend.ai.hitl.repository.PendingAgentActionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Dịch vụ tạo và ký số bản nháp đề xuất hành động HITL (Phase 1 Propose).
 *
 * Mọi đề xuất hành động nhạy cảm (Tier 3 Mutation) đều được cấp một action_token
 * ký số bảo mật bằng HMAC-SHA256 với thời hạn hết hạn mặc định là 5 phút (300s).
 */
@Service
public class ActionProposalService {

    private static final Logger log = LoggerFactory.getLogger(ActionProposalService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long DEFAULT_TTL_SECONDS = 300L; // 5 phút

    private final PendingAgentActionRepository pendingActionRepository;
    private final ObjectMapper objectMapper;
    private final ObjectMapper canonicalMapper;
    private final String hmacSecret;

    public ActionProposalService(PendingAgentActionRepository pendingActionRepository,
                                 ObjectMapper objectMapper,
                                 @Value("${app.security.hmac-secret}") String hmacSecret) {
        this.pendingActionRepository = pendingActionRepository;
        this.objectMapper = objectMapper;
        this.canonicalMapper = new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        this.hmacSecret = hmacSecret;
    }

    /**
     * Tạo và ký số một bản nháp đề xuất hành động mới.
     *
     * @param userId Người dùng đề xuất / sở hữu phiên
     * @param targetRole Vai trò mục tiêu được phép thực thi
     * @param toolName Tên công cụ biến đổi (ví dụ: proposeEditWorkoutMetric)
     * @param title Tiêu đề hiển thị trên UI
     * @param targetEntityId Mã định danh thực thể bị tác động
     * @param currentValues Giá trị hiện tại
     * @param proposedValues Giá trị đề xuất thay đổi
     * @param reason Lý do thay đổi
     * @return ActionProposalDto gửi về cho client
     */
    @Transactional
    public ActionProposalDto createProposal(Long userId,
                                           String targetRole,
                                           String toolName,
                                           String title,
                                           String targetEntityId,
                                           Map<String, Object> currentValues,
                                           Map<String, Object> proposedValues,
                                           String reason) {
        try {
            // Chuẩn hóa thời hạn cắt bỏ phần mili/nano giây để tránh lệch múi giờ/rounding DB
            LocalDateTime expiresAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).plusSeconds(DEFAULT_TTL_SECONDS);

            // Đóng gói Diff Payload an toàn (tránh NullPointerException của Map.of)
            Map<String, Object> diffMap = new LinkedHashMap<>();
            diffMap.put("title", title != null ? title : "");
            diffMap.put("targetEntityId", targetEntityId != null ? targetEntityId : "");
            diffMap.put("currentValues", currentValues != null ? currentValues : Collections.emptyMap());
            diffMap.put("proposedValues", proposedValues != null ? proposedValues : Collections.emptyMap());
            diffMap.put("reason", reason != null ? reason : "");
            String diffJson = objectMapper.writeValueAsString(diffMap);

            // Sinh chữ ký số HMAC-SHA256 chuẩn hóa
            String actionNonce = UUID.randomUUID().toString();
            String signature = generateHmacSignature(userId, toolName, targetEntityId, expiresAt, diffJson, actionNonce);
            String actionToken = actionNonce + "." + signature;

            // Lưu vào bảng pending_agent_actions
            PendingAgentAction pendingAction = new PendingAgentAction(
                    actionToken,
                    userId,
                    targetRole,
                    toolName,
                    targetEntityId,
                    diffJson,
                    expiresAt
            );

            PendingAgentAction saved = pendingActionRepository.save(pendingAction);
            log.info("ActionProposalService: Tạo thành công proposal #{} cho tool [{}] bởi user [{}]",
                    saved.getId(), toolName, userId);

            return new ActionProposalDto(
                    saved.getId(),
                    actionToken,
                    toolName,
                    title,
                    targetEntityId,
                    currentValues,
                    proposedValues,
                    reason,
                    DEFAULT_TTL_SECONDS
            );

        } catch (Exception ex) {
            log.error("ActionProposalService: Lỗi khi tạo proposal: {}", ex.getMessage(), ex);
            throw new RuntimeException("Không thể khởi tạo đề xuất hành động: " + ex.getMessage(), ex);
        }
    }

    /**
     * Xác thực tính toàn vẹn của chữ ký số HMAC-SHA256 trên một action record.
     */
    public boolean verifyTokenSignature(PendingAgentAction action) {
        if (action == null || action.getActionToken() == null) {
            return false;
        }

        String[] parts = action.getActionToken().split("\\.");
        if (parts.length != 2) {
            return false;
        }

        String actionNonce = parts[0];
        String expectedSignature = parts[1];

        String computedSignature = generateHmacSignature(
                action.getUserId(),
                action.getToolName(),
                action.getTargetEntityId(),
                action.getExpiresAt(),
                action.getDiffPayload(),
                actionNonce
        );

        return expectedSignature.equals(computedSignature);
    }

    /**
     * Chuẩn hóa chuỗi JSON để đảm bảo tính xác định (Canonical JSON Scheme):
     * Sắp xếp khóa theo thứ tự và loại bỏ biến động formatting của PostgreSQL JSONB.
     */
    public String canonicalizeJson(String jsonStr) {
        if (jsonStr == null || jsonStr.isBlank()) {
            return "{}";
        }
        try {
            Object obj = objectMapper.readValue(jsonStr, Object.class);
            return canonicalMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return jsonStr;
        }
    }

    /**
     * Thuật toán sinh chữ ký số HMAC-SHA256.
     */
    private String generateHmacSignature(Long userId, String toolName, String targetEntityId,
                                         LocalDateTime expiresAt, String diffJson, String nonce) {
        try {
            String canonicalDiff = canonicalizeJson(diffJson);
            long epochSecond = expiresAt.toEpochSecond(ZoneOffset.UTC);
            String rawPayload = String.format("%s:%s:%s:%d:%s:%s",
                    userId,
                    toolName,
                    targetEntityId != null ? targetEntityId : "",
                    epochSecond,
                    canonicalDiff,
                    nonce);

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKeySpec);

            byte[] hmacBytes = mac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Lỗi sinh chữ ký số HMAC-SHA256", e);
        }
    }
}
