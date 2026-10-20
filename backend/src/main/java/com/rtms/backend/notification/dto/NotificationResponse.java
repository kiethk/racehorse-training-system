package com.rtms.backend.notification.dto;

import com.rtms.backend.notification.entity.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String notificationType,
        String title,
        String message,
        String referenceType,
        Long referenceId,
        boolean read,
        LocalDateTime createdAt
) {
    public boolean isRead() {
        return read;
    }

    public static NotificationResponse from(Notification n) {
        if (n == null) return null;
        return new NotificationResponse(
                n.getId(),
                n.getNotificationType(),
                n.getTitle(),
                n.getMessage(),
                n.getReferenceType(),
                n.getReferenceId(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
