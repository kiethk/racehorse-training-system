package com.rtms.backend.controller;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.dto.ApiResponse;
import com.rtms.backend.dto.NotificationResponse;
import com.rtms.backend.dto.UnreadCountResponse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    private NotificationController notificationController;
    private AuthenticatedUser vetUser;

    @BeforeEach
    void setUp() {
        notificationController = new NotificationController(notificationService);
        vetUser = new AuthenticatedUser(10L, "vet@example.com", "VETERINARIAN");
    }

    @Test
    @DisplayName("GET /api/notifications: Lấy danh sách thông báo phân trang theo principal recipientId")
    void listNotifications_usesPrincipalRecipient() {
        NotificationResponse item = new NotificationResponse(
                1L, "ADMISSION_VET_ASSIGNED", "Giao ca", "Kiem tra", "ADMISSION", 100L, false, LocalDateTime.now()
        );
        Page<NotificationResponse> page = new PageImpl<>(List.of(item));

        when(notificationService.getNotifications(eq(10L), eq("UNREAD"), any(Pageable.class)))
                .thenReturn(page);

        ApiResponse<Page<NotificationResponse>> response = notificationController.listNotifications("UNREAD", 0, 10, vetUser);

        assertNotNull(response);
        assertEquals(1, response.getData().getContent().size());
        verify(notificationService).getNotifications(eq(10L), eq("UNREAD"), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/notifications/unread-count: Lấy số lượng chưa đọc theo principal")
    void getUnreadCount_usesPrincipal() {
        when(notificationService.getUnreadCount(10L)).thenReturn(5L);

        ApiResponse<UnreadCountResponse> response = notificationController.getUnreadCount(vetUser);

        assertNotNull(response);
        assertEquals(5L, response.getData().unreadCount());
        verify(notificationService).getUnreadCount(10L);
    }

    @Test
    @DisplayName("PATCH /api/notifications/{id}/read: Đánh dấu đã đọc thành công")
    void markAsRead_success() {
        NotificationResponse updated = new NotificationResponse(
                1L, "ADMISSION_VET_ASSIGNED", "Giao ca", "Kiem tra", "ADMISSION", 100L, true, LocalDateTime.now()
        );
        when(notificationService.markAsRead(1L, 10L)).thenReturn(updated);

        ApiResponse<NotificationResponse> response = notificationController.markAsRead(1L, vetUser);

        assertTrue(response.getData().isRead());
        verify(notificationService).markAsRead(1L, 10L);
    }

    @Test
    @DisplayName("PATCH /api/notifications/{id}/read: Đánh dấu thông báo không thuộc về mình ném 404")
    void markAsRead_foreignNotification_throwsNotFound() {
        when(notificationService.markAsRead(99L, 10L))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "Notification not found"));

        ApiException ex = assertThrows(ApiException.class,
                () -> notificationController.markAsRead(99L, vetUser));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("NOTIFICATION_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    @DisplayName("PATCH /api/notifications/read-all: Đánh dấu đọc tất cả của principal")
    void markAllAsRead_success() {
        when(notificationService.markAllAsRead(10L)).thenReturn(3);

        ApiResponse<Map<String, Long>> response = notificationController.markAllAsRead(vetUser);

        assertEquals(3L, response.getData().get("updated"));
        verify(notificationService).markAllAsRead(10L);
    }
}
