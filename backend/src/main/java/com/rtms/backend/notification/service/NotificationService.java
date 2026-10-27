package com.rtms.backend.notification.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.notification.dto.NotificationCursor;
import com.rtms.backend.notification.dto.NotificationFeedResponse;
import com.rtms.backend.notification.dto.NotificationResponse;
import com.rtms.backend.notification.entity.Notification;
import com.rtms.backend.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;

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
        } catch (DataIntegrityViolationException e) {
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
     * Phiên bản này nhận referenceType/referenceId một cách rõ ràng, dùng để thông báo
     * cho một care_schedule không liên kết admission (ROUTINE, URGENT).
     */
    @Transactional
    public Optional<Notification> sendAssignmentNotification(
            Long recipientId,
            String referenceType,
            Long referenceId,
            Long horseId,
            String notificationType,
            String title,
            String message) {

        if (recipientId == null) {
            return Optional.empty();
        }

        String eventType = notificationType != null && !notificationType.isBlank()
                ? notificationType
                : "ASSIGNMENT";
        String dedupKey = Notification.buildDeduplicationKey(recipientId, referenceType, referenceId, eventType);

        return createNotification(recipientId, title, message, eventType, referenceType, referenceId, dedupKey);
    }

    /**
     * Phiên bản cũ: giữ nguyên hành vi admission-scoped. Manager, Groom và Trainer
     * flows phụ thuộc vào nó — không được thay đổi.
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
    public NotificationFeedResponse getFeed(Long recipientId, String status, String cursor, int size) {
        int pageSize = Math.min(Math.max(1, size), 50);
        Boolean read = resolveStatus(status);
        NotificationCursor c = (cursor == null || cursor.isBlank()) ? null : NotificationCursor.parse(cursor);

        List<Notification> fetched = notificationRepository.feedBefore(
                recipientId,
                read,
                c == null ? null : c.createdAt(),
                c == null ? null : c.id(),
                PageRequest.of(0, pageSize + 1)
        );

        return buildFeedResponse(fetched, pageSize);
    }

    @Transactional(readOnly = true)
    public NotificationFeedResponse getSince(Long recipientId, String afterCursor, int limit) {
        int pageLimit = Math.min(Math.max(1, limit), 100);
        if (afterCursor == null || afterCursor.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CURSOR", "after cursor is required");
        }
        NotificationCursor c = NotificationCursor.parse(afterCursor);

        List<Notification> fetched = notificationRepository.sinceAfter(
                recipientId,
                c.createdAt(),
                c.id(),
                PageRequest.of(0, pageLimit + 1)
        );

        boolean hasMore = fetched.size() > pageLimit;
        List<Notification> page = hasMore ? fetched.subList(0, pageLimit) : fetched;
        String nextCursor = page.isEmpty()
                ? afterCursor
                : NotificationCursor.from(page.get(page.size() - 1)).format();

        return NotificationFeedResponse.of(
                page.stream().map(NotificationResponse::from).toList(),
                nextCursor,
                hasMore
        );
    }

    private NotificationFeedResponse buildFeedResponse(List<Notification> fetched, int pageSize) {
        boolean hasMore = fetched.size() > pageSize;
        List<Notification> page = hasMore ? fetched.subList(0, pageSize) : fetched;
        String nextCursor = null;
        if (hasMore) {
            nextCursor = NotificationCursor.from(page.get(page.size() - 1)).format();
        }
        return NotificationFeedResponse.of(
                page.stream().map(NotificationResponse::from).toList(),
                nextCursor,
                hasMore
        );
    }

    private Boolean resolveStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        if ("UNREAD".equalsIgnoreCase(status)) {
            return false;
        }
        if ("READ".equalsIgnoreCase(status)) {
            return true;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "Status must be UNREAD or READ");
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
