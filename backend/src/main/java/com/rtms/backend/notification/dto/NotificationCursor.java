package com.rtms.backend.notification.dto;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.notification.entity.Notification;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public record NotificationCursor(LocalDateTime createdAt, long id) {

    public static NotificationCursor from(Notification notification) {
        return new NotificationCursor(notification.getCreatedAt(), notification.getId());
    }

    public static NotificationCursor parse(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CURSOR", "Cursor is required");
        }
        try {
            int sep = cursor.lastIndexOf('_');
            if (sep < 0) {
                throw new IllegalArgumentException();
            }
            LocalDateTime createdAt = LocalDateTime.parse(cursor.substring(0, sep));
            long id = Long.parseLong(cursor.substring(sep + 1));
            return new NotificationCursor(createdAt, id);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CURSOR", "Invalid cursor format");
        }
    }

    public String format() {
        return createdAt.toString() + "_" + id;
    }
}
