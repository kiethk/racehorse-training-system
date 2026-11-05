package com.rtms.backend.notification.repository;

import com.rtms.backend.notification.dto.NotificationCursor;
import com.rtms.backend.notification.entity.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local")
class NotificationRepositoryKeysetQueryTest {

    @Autowired
    private NotificationRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long recipientId;
    private final LocalDateTime sharedTimestamp = LocalDateTime.of(2026, 10, 26, 10, 0, 0);

    @BeforeEach
    void setUp() {
        // Ensure a recipient user exists regardless of seed state (self-contained test).
        jdbcTemplate.update(
                "INSERT INTO users (full_name, email, password_hash, role_id, is_active) " +
                "SELECT 'Test Trainer', 'trainer@rtms.com', '$2a$10$2rXcS5LHx7wVYOkClxQ1YuIvg4myh7UgloaGcwcgvnahK9pTWVEJ.', r.id, true " +
                "FROM roles r WHERE r.name = 'HEAD_TRAINER' " +
                "AND NOT EXISTS (SELECT 1 FROM users WHERE email = 'trainer@rtms.com')");
        recipientId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = 'trainer@rtms.com' LIMIT 1", Long.class);
        jdbcTemplate.update("TRUNCATE TABLE notifications RESTART IDENTITY CASCADE");
        for (int i = 1; i <= 4; i++) {
            createNotification(i);
        }
    }

    private Notification createNotification(int i) {
        Notification n = new Notification();
        n.setRecipientId(recipientId);
        n.setTitle("Title " + i);
        n.setMessage("Message " + i);
        n.setNotificationType("TEST");
        n.setRead(i % 2 == 0);
        n.setCreatedAt(sharedTimestamp);
        return repository.save(n);
    }

    @Test
    @DisplayName("feedBefore: phân trang keyset với createdAt trùng nhau, lọc theo trạng thái")
    void feedBefore_paginatesByIdTiebreakWhenCreatedAtEqual() {
        List<Notification> unread = repository.feedBefore(
                recipientId, false, null, null, PageRequest.of(0, 2));

        assertEquals(2, unread.size());
        assertEquals(3L, unread.get(0).getId());
        assertEquals(1L, unread.get(1).getId());

        NotificationCursor cursor = new NotificationCursor(sharedTimestamp, unread.get(1).getId());
        List<Notification> next = repository.feedBefore(
                recipientId, false, cursor.createdAt(), cursor.id(), PageRequest.of(0, 2));

        assertTrue(next.isEmpty(), "No unread notifications before id 1");
    }

    @Test
    @DisplayName("sinceAfter: trả về các thông báo sau cursor với tiebreak id")
    void sinceAfter_returnsNotificationsAfterCursorAscending() {
        List<Notification> page = repository.sinceAfter(
                recipientId, sharedTimestamp, 2L, PageRequest.of(0, 10));

        assertEquals(2, page.size());
        assertEquals(3L, page.get(0).getId());
        assertEquals(4L, page.get(1).getId());
    }
}
