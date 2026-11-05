package com.rtms.backend.notification.dto;

import java.util.List;

public record NotificationFeedResponse(
        List<NotificationResponse> content,
        String nextCursor,
        boolean hasMore
) {
    public static NotificationFeedResponse of(List<NotificationResponse> content, String nextCursor, boolean hasMore) {
        return new NotificationFeedResponse(content, nextCursor, hasMore);
    }
}
