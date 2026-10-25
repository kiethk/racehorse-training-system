package com.rtms.backend.horse.dto;

import com.rtms.backend.horse.enums.HorseDeactivationReason;
import com.rtms.backend.horse.enums.HorseReactivationReason;
import com.rtms.backend.horse.enums.HorseStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HorseStatusChangeRequest(
        @NotNull(message = "New status is required")
        HorseStatus status,

        String reasonCode,

        @Size(max = 1000, message = "Custom reason cannot exceed 1000 characters")
        String customReason,

        @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
        String reason,

        Long stallId
) {
    public HorseStatusChangeRequest(HorseStatus status, String reason) {
        this(status, "OTHER", reason, reason, null);
    }

    public HorseStatusChangeRequest(HorseStatus status, String reasonCode, String customReason, String reason) {
        this(status, reasonCode, customReason, reason, null);
    }

    /**
     * Resolves human-readable description for display and notifications.
     */
    public String resolveEffectiveReason() {
        if ("OTHER".equalsIgnoreCase(reasonCode)) {
            return (customReason != null && !customReason.isBlank()) ? customReason.trim() : "Other";
        }
        if (reasonCode != null && !reasonCode.isBlank()) {
            if (status == HorseStatus.INACTIVE) {
                try {
                    return HorseDeactivationReason.valueOf(reasonCode.trim().toUpperCase()).getDisplayName();
                } catch (IllegalArgumentException ignored) {
                    return reasonCode;
                }
            } else if (status == HorseStatus.ELIGIBLE) {
                try {
                    return HorseReactivationReason.valueOf(reasonCode.trim().toUpperCase()).getDisplayName();
                } catch (IllegalArgumentException ignored) {
                    return reasonCode;
                }
            }
            return reasonCode;
        }
        if (reason != null && !reason.isBlank()) {
            return reason.trim();
        }
        if (customReason != null && !customReason.isBlank()) {
            return customReason.trim();
        }
        return "";
    }

    /**
     * Resolves uppercase enum code to store in status_reason_code.
     */
    public String resolveReasonCode() {
        if (reasonCode != null && !reasonCode.isBlank()) {
            return reasonCode.trim().toUpperCase();
        }
        return "OTHER";
    }
}
