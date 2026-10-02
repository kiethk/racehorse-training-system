package com.rtms.backend.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import javax.sql.DataSource;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CareScheduleMergeMigrationTest {
    @Autowired DataSource dataSource;

    @Test
    void preservesHistoryLinksDatesAndForeignKeysInIsolatedSchema() throws Exception {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            c.setAutoCommit(false);
            try {
                String schema = "care_merge_test_" + UUID.randomUUID().toString().replace("-", "");
                s.execute("CREATE SCHEMA " + schema);
                s.execute("SET LOCAL search_path TO " + schema);
                s.execute("""
                    CREATE TABLE horses (id BIGINT PRIMARY KEY, training_locked BOOLEAN, training_status VARCHAR(50));
                    CREATE TABLE users (id BIGINT PRIMARY KEY);
                    CREATE TABLE admission_applications (id BIGINT PRIMARY KEY);
                    CREATE TABLE preventive_care_schedules (id BIGINT PRIMARY KEY, horse_id BIGINT,
                        veterinarian_id BIGINT, care_type VARCHAR(50), status VARCHAR(50),
                        scheduled_date DATE, description TEXT, created_at TIMESTAMP DEFAULT NOW(), updated_at TIMESTAMP DEFAULT NOW());
                    CREATE TABLE health_records (id BIGINT PRIMARY KEY, examined_at TIMESTAMP,
                        preventive_care_schedule_id BIGINT UNIQUE REFERENCES preventive_care_schedules(id));
                    CREATE TABLE vet_exams (id BIGINT PRIMARY KEY, horse_id BIGINT, admission_id BIGINT,
                        exam_type VARCHAR(20), status VARCHAR(20), assigned_vet_id BIGINT, scheduled_at TIMESTAMP,
                        duration_minutes INT DEFAULT 30, reason TEXT, health_record_id BIGINT REFERENCES health_records(id),
                        requested_for_date DATE, preferred_vet_id BIGINT,
                        created_at TIMESTAMP DEFAULT NOW(), updated_at TIMESTAMP DEFAULT NOW());
                    INSERT INTO horses VALUES (1, false, 'ALLOWED');
                    INSERT INTO users VALUES (7);
                    INSERT INTO admission_applications VALUES (1);
                    """);
                s.execute(migration("V57__refactor_vet_care_schedule_mvp.sql"));
                s.execute("""
                    INSERT INTO care_schedules (id, horse_id, admission_id, care_type) VALUES (100, 1, 1, 'INITIAL');
                    INSERT INTO vet_offers (care_schedule_id, veterinarian_id) VALUES (100, 7);
                    INSERT INTO preventive_care_schedules (id, horse_id, veterinarian_id, care_type, status, scheduled_date, description)
                    VALUES (1, 1, 7, 'VACCINATION', 'COMPLETED', '2026-09-01', 'flu vaccine'),
                           (2, 1, 7, 'DEWORMING', 'OVERDUE', '2026-10-10', 'next dose'),
                           (3, 1, 7, 'INITIAL_EXAM', 'CANCELLED', '2026-10-01', 'initial');
                    INSERT INTO health_records (id, examined_at, preventive_care_schedule_id)
                    VALUES (1, '2026-09-01 14:00', 1), (2, '2026-09-02 14:00', NULL);
                    INSERT INTO vet_exams (id, horse_id, admission_id, exam_type, status, assigned_vet_id,
                        scheduled_at, reason, health_record_id, requested_for_date, preferred_vet_id)
                    VALUES (1, 1, 1, 'INITIAL', 'SCHEDULED', 7, '2026-10-01 14:00', 'initial', NULL, NULL, 7),
                           (2, 1, NULL, 'FOLLOW_UP', 'COMPLETED', 7, '2026-09-02 14:00', 'recheck', 2, NULL, 7),
                           (3, 1, NULL, 'URGENT', 'REQUESTED', NULL, NULL, 'urgent', NULL, '2026-10-02', 7);
                    """);
                s.execute(migration("V59__merge_legacy_schedules_into_care_schedule.sql"));
                s.execute(migration("V60__preserve_requested_care_time.sql"));
                assertEquals(0, count(s, "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '" + schema
                        + "' AND table_name IN ('vet_exams', 'preventive_care_schedules', 'care_schedules')"));
                assertEquals(5, count(s, "SELECT COUNT(*) FROM care_schedule"));
                assertEquals(6, count(s, "SELECT SUM(jsonb_array_length(legacy_data)) FROM care_schedule"));
                assertEquals(2, count(s, "SELECT COUNT(*) FROM health_records hr JOIN care_schedule cs ON cs.id = hr.care_schedule_id"));
                assertEquals(1, count(s, "SELECT COUNT(*) FROM care_schedule WHERE id = 100 AND status = 'SCHEDULED' AND veterinarian_id = 7 AND jsonb_array_length(legacy_data) = 2"));
                assertEquals(1, count(s, "SELECT COUNT(*) FROM care_schedule WHERE status = 'REQUESTED' AND requested_at = '2026-10-10 14:00' AND scheduled_at IS NULL"));
                assertEquals(1, count(s, "SELECT COUNT(*) FROM care_schedule WHERE care_type = 'URGENT' AND requested_at = '2026-10-02 14:00'"));
                assertEquals(1, count(s, "SELECT COUNT(*) FROM health_records hr JOIN care_schedule cs ON cs.id = hr.care_schedule_id WHERE hr.id = 2 AND cs.care_type = 'ROUTINE' AND cs.status = 'COMPLETED'"));
                assertThrows(SQLException.class, () -> s.execute("DELETE FROM care_schedule WHERE id = (SELECT care_schedule_id FROM health_records WHERE id = 1)"));
            } finally {
                c.rollback();
            }
        }
    }

    private static String migration(String name) throws Exception {
        try (var stream = CareScheduleMergeMigrationTest.class.getResourceAsStream("/db/migration/" + name)) {
            assertNotNull(stream);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static long count(Statement s, String sql) throws SQLException {
        try (ResultSet r = s.executeQuery(sql)) { r.next(); return r.getLong(1); }
    }
}
