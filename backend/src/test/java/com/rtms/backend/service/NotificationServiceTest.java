package com.rtms.backend.service;

import com.rtms.backend.entity.Notification;
import com.rtms.backend.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository);
    }

    @Test
    @DisplayName("createNotification: Bất kỳ actor nào cũng tạo được thông báo tổng quát")
    void createNotification_genericForAnyActor_success() {
        Long managerId = 1L;
        String title = "Báo cáo sự cố";
        String message = "Ngựa Lucky bị thương";
        String type = "INCIDENT";
        String refType = "INCIDENT_REPORT";
        Long refId = 88L;

        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(10L);
            return n;
        });

        Optional<Notification> result = notificationService.createNotification(
                managerId, title, message, type, refType, refId, null);

        assertTrue(result.isPresent());
        Notification n = result.get();
        assertEquals(10L, n.getId());
        assertEquals(managerId, n.getRecipientId());
        assertEquals("INCIDENT", n.getNotificationType());
        assertEquals("INCIDENT_REPORT", n.getReferenceType());
        assertEquals(88L, n.getReferenceId());
        assertFalse(n.isRead());
    }

    @Test
    @DisplayName("sendAssignmentNotification: Tạo thông báo gán việc với deduplication key")
    void sendAssignmentNotification_createsDedupKeyAndSaves() {
        Long vetId = 15L;
        Long admissionId = 2L;
        Long horseId = 10L;

        when(notificationRepository.existsByDeduplicationKey("15:ADMISSION_2:VET_INITIAL_EXAM")).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(1L);
            return n;
        });

        Optional<Notification> result = notificationService.sendAssignmentNotification(
                vetId, admissionId, horseId, "VET_INITIAL_EXAM", "New assignment", "Assigned to horse");

        assertTrue(result.isPresent());
        assertEquals("15:ADMISSION_2:VET_INITIAL_EXAM", result.get().getDeduplicationKey());
        assertEquals("VET_INITIAL_EXAM", result.get().getNotificationType());
    }

    @Test
    @DisplayName("Deduplication: Trả về thông báo cũ nếu key đã tồn tại, không lưu trùng")
    void deduplication_returnsExistingWhenKeyExists() {
        String dedupKey = "15:ADMISSION_2:VET_INITIAL_EXAM";
        Notification existing = new Notification();
        existing.setId(99L);
        existing.setDeduplicationKey(dedupKey);

        when(notificationRepository.existsByDeduplicationKey(dedupKey)).thenReturn(true);
        when(notificationRepository.findByDeduplicationKey(dedupKey)).thenReturn(Optional.of(existing));

        Optional<Notification> result = notificationService.createNotification(
                15L, "Title", "Msg", "ASSIGNMENT", "ADMISSION", 2L, dedupKey);

        assertTrue(result.isPresent());
        assertEquals(99L, result.get().getId());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Concurrency dedup: DataIntegrityViolationException fallback tìm bản ghi đã có")
    void concurrentDuplicate_handlesDataIntegrityViolation() {
        String dedupKey = "15:ADMISSION_2:VET_INITIAL_EXAM";
        Notification existing = new Notification();
        existing.setId(99L);
        existing.setDeduplicationKey(dedupKey);

        when(notificationRepository.existsByDeduplicationKey(dedupKey)).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(notificationRepository.findByDeduplicationKey(dedupKey)).thenReturn(Optional.of(existing));

        Optional<Notification> result = notificationService.createNotification(
                15L, "Title", "Msg", "ASSIGNMENT", "ADMISSION", 2L, dedupKey);

        assertTrue(result.isPresent());
        assertEquals(99L, result.get().getId());
    }

    @Test
    @DisplayName("markAsRead và unread count hoạt động chuẩn xác")
    void markAsRead_and_unreadCount() {
        Long userId = 7L;
        Notification n = new Notification();
        n.setId(5L);
        n.setRecipientId(userId);
        n.setRead(false);

        when(notificationRepository.findById(5L)).thenReturn(Optional.of(n));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
        when(notificationRepository.countByRecipientIdAndIsReadFalse(userId)).thenReturn(1L);

        assertEquals(1L, notificationService.getUnreadCount(userId));

        com.rtms.backend.dto.NotificationResponse updated = notificationService.markAsRead(5L, userId);
        assertTrue(updated.isRead());
        assertTrue(n.isRead());
        verify(notificationRepository).save(n);

        // User khác không mark được notification của người này
        assertThrows(com.rtms.backend.config.ApiException.class, () -> notificationService.markAsRead(5L, 999L));
    }
}
