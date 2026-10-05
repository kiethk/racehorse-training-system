package com.rtms.backend.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.dto.NotificationResponse;
import com.rtms.backend.entity.Notification;
import com.rtms.backend.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Tạo thông báo tổng quát cho bất kỳ Actor nào.
     * Chống trùng lặp an toàn không phụ thuộc vào bắt DataIntegrityViolationException.
     */
    @Transactional
    public Optional<Notification> createNotification(
            Long recipientId,
            String title,
            String message,
            String notificationType,
            String referenceType,
            Long referenceId,
            String deduplicationKey) {

        if (recipientId == null) {
            return Optional.empty();
        }

        if (deduplicationKey != null && !deduplicationKey.isBlank()) {
            if (notificationRepository.existsByDeduplicationKey(deduplicationKey)) {
                log.debug("Notification with deduplication key {} already exists. Skipping duplicate.", deduplicationKey);
                return notificationRepository.findByDeduplicationKey(deduplicationKey);
            }
            try {
                int inserted = notificationRepository.insertIgnoreDuplicate(
                        recipientId, title, message, notificationType, referenceType, referenceId, deduplicationKey
                );
                if (inserted > 0 || notificationRepository.existsByDeduplicationKey(deduplicationKey)) {
                    return notificationRepository.findByDeduplicationKey(deduplicationKey);
                }
            } catch (Exception e) {
                log.debug("Native ON CONFLICT insert fell back or threw: {}. Checking existence.", e.getMessage());
                if (notificationRepository.existsByDeduplicationKey(deduplicationKey)) {
                    return notificationRepository.findByDeduplicationKey(deduplicationKey);
                }
            }
        }

        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(notificationType);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notification.setDeduplicationKey(deduplicationKey);
        notification.setRead(false);

        try {
            return Optional.of(notificationRepository.save(notification));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.debug("Concurrent insert collision for key {}. Retrieving existing notification.", deduplicationKey);
            if (deduplicationKey != null) {
                Optional<Notification> existing = notificationRepository.findByDeduplicationKey(deduplicationKey);
                if (existing.isPresent()) {
                    return existing;
                }
            }
            throw e;
        }
    }

    /**
     * Tiện ích gửi thông báo gán việc (Vet hoặc Trainer) với mã event code cụ thể.
     */
    @Transactional
    public Optional<Notification> sendAssignmentNotification(
            Long recipientId,
            Long admissionId,
            Long horseId,
            String notificationType,
            String title,
            String message) {

        if (recipientId == null) {
            return Optional.empty();
        }

        String refType = admissionId != null ? NotificationTypes.REFERENCE_ADMISSION : "HORSE";
        Long refId = admissionId != null ? admissionId : horseId;
        String eventType = notificationType != null && !notificationType.isBlank()
                ? notificationType
                : "ASSIGNMENT";
        String dedupKey = Notification.buildDeduplicationKey(recipientId, refType, refId, eventType);

        return createNotification(recipientId, title, message, eventType, refType, refId, dedupKey);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Long recipientId, String status, Pageable pageable) {
        if ("UNREAD".equalsIgnoreCase(status)) {
            return notificationRepository.findByRecipientIdAndIsRead(recipientId, false, pageable)
                    .map(NotificationResponse::from);
        } else if ("READ".equalsIgnoreCase(status)) {
            return notificationRepository.findByRecipientIdAndIsRead(recipientId, true, pageable)
                    .map(NotificationResponse::from);
        } else {
            return notificationRepository.findByRecipientId(recipientId, pageable)
                    .map(NotificationResponse::from);
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(Long recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotificationsForUser(Long recipientId) {
        return notificationRepository.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(recipientId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long recipientId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(recipientId);
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long recipientId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "Notification not found"));

        if (!notification.getRecipientId().equals(recipientId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "Notification not found");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification = notificationRepository.save(notification);
        }

        return NotificationResponse.from(notification);
    }

    @Transactional
    public int markAllAsRead(Long recipientId) {
        return notificationRepository.markAllReadByRecipientId(recipientId);
    }
}
