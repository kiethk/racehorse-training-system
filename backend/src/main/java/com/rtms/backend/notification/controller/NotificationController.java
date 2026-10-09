package com.rtms.backend.notification.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.notification.dto.NotificationResponse;
import com.rtms.backend.notification.dto.UnreadCountResponse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.notification.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<Page<NotificationResponse>> listNotifications(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        int pageSize = Math.min(Math.max(1, size), 50);
        int pageIndex = Math.max(0, page);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt", "id"));

        Page<NotificationResponse> result = notificationService.getNotifications(currentUser.getUserId(), status, pageable);
        return ApiResponse.success(result);
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        long count = notificationService.getUnreadCount(currentUser.getUserId());
        return ApiResponse.success(new UnreadCountResponse(count));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        NotificationResponse response = notificationService.markAsRead(id, currentUser.getUserId());
        return ApiResponse.success(response);
    }

    @PatchMapping("/read-all")
    public ApiResponse<Map<String, Long>> markAllAsRead(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        int updated = notificationService.markAllAsRead(currentUser.getUserId());
        return ApiResponse.success(Map.of("updated", (long) updated));
    }
}
