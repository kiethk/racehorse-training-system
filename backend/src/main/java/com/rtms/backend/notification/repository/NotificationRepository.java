package com.rtms.backend.notification.repository;

import com.rtms.backend.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByDeduplicationKey(String deduplicationKey);

    Optional<Notification> findByDeduplicationKey(String deduplicationKey);

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    List<Notification> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(Long recipientId);

    Page<Notification> findByRecipientId(Long recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndIsRead(Long recipientId, boolean isRead, Pageable pageable);

    long countByRecipientIdAndIsReadFalse(Long recipientId);

    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO notifications (recipient_id, title, message, notification_type, reference_type, reference_id, deduplication_key, is_read, created_at)
            VALUES (:recipientId, :title, :message, :notificationType, :referenceType, :referenceId, :deduplicationKey, false, NOW())
            ON CONFLICT (deduplication_key) DO NOTHING
            """)
    int insertIgnoreDuplicate(
            @Param("recipientId") Long recipientId,
            @Param("title") String title,
            @Param("message") String message,
            @Param("notificationType") String notificationType,
            @Param("referenceType") String referenceType,
            @Param("referenceId") Long referenceId,
            @Param("deduplicationKey") String deduplicationKey
    );

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipientId = :recipientId AND n.isRead = false")
    int markAllReadByRecipientId(@Param("recipientId") Long recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.recipientId = :recipientId")
    int markReadByIdAndRecipientId(@Param("id") Long id, @Param("recipientId") Long recipientId);

    default List<Notification> feedBefore(
            Long recipientId,
            Boolean read,
            LocalDateTime createdAt,
            Long id,
            Pageable pageable
    ) {
        if (createdAt == null) {
            return feedBeforeInitial(recipientId, read, pageable);
        }
        return feedBeforeCursor(recipientId, read, createdAt, id, pageable);
    }

    @Query("""
            SELECT n FROM Notification n
            WHERE n.recipientId = :recipientId
              AND (:read IS NULL OR n.isRead = :read)
            ORDER BY n.createdAt DESC, n.id DESC
            """)
    List<Notification> feedBeforeInitial(
            @Param("recipientId") Long recipientId,
            @Param("read") Boolean read,
            Pageable pageable
    );

    @Query("""
            SELECT n FROM Notification n
            WHERE n.recipientId = :recipientId
              AND (:read IS NULL OR n.isRead = :read)
              AND (n.createdAt < :createdAt
                   OR (n.createdAt = :createdAt AND (:id IS NULL OR n.id < :id)))
            ORDER BY n.createdAt DESC, n.id DESC
            """)
    List<Notification> feedBeforeCursor(
            @Param("recipientId") Long recipientId,
            @Param("read") Boolean read,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("id") Long id,
            Pageable pageable
    );

    @Query("""
            SELECT n FROM Notification n
            WHERE n.recipientId = :recipientId
              AND (n.createdAt > :createdAt OR (n.createdAt = :createdAt AND n.id > :id))
            ORDER BY n.createdAt ASC, n.id ASC
            """)
    List<Notification> sinceAfter(
            @Param("recipientId") Long recipientId,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("id") Long id,
            Pageable pageable
    );
}
