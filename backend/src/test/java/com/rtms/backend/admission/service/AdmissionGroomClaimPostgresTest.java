package com.rtms.backend.admission.service;

import com.rtms.backend.admission.dto.GroomAdmissionReviewRequest;
import com.rtms.backend.admission.enums.ReviewDecision;
import com.rtms.backend.config.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Approving is claiming a stall of one's own, under real row locks. Never enable against a real
 * application database: fixtures are committed, and other quarantine grooms are deactivated so the
 * pool contains only this test's grooms.
 */
@SpringBootTest(properties = {"spring.datasource.url=${RTMS_STABLE_TEST_DB_URL}", "spring.config.import="})
@ActiveProfiles("local")
@EnabledIfEnvironmentVariable(named = "RTMS_STABLE_TEST_DB_URL",
        matches = "jdbc:postgresql://localhost:5433/rtms_stable_test_[a-zA-Z0-9_]+")
class AdmissionGroomClaimPostgresTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired AdmissionGroomReviewService reviews;
    long owner, groomA, groomB, quarantine;
    String tag;

    @BeforeEach void fixtures() {
        assertTrue(System.getenv("RTMS_STABLE_TEST_DB_URL").contains("/rtms_stable_test_"));
        tag = UUID.randomUUID().toString().replace("-", "").substring(0, 9);
        owner = user("HORSE_OWNER", "o");
        groomA = user("GROOM", "a");
        groomB = user("GROOM", "b");
        jdbc.update("INSERT INTO groom_profiles(user_id) VALUES (?), (?)", groomA, groomB);
        // Leftover quarantine grooms from earlier runs would join the pool; keep only ours.
        jdbc.update("""
            UPDATE users SET is_active = FALSE
            WHERE id IN (SELECT ss.groom_id FROM stable_stalls ss JOIN areas a ON a.id = ss.area_id
                         WHERE a.type = 'QUARANTINE' AND ss.groom_id IS NOT NULL)
              AND id NOT IN (?, ?)
            """, groomA, groomB);
        // Leftover pool admissions must not count as older than this test's ones.
        jdbc.update("UPDATE admission_applications SET groom_review_started_at = TIMESTAMP '2999-01-01' WHERE status = 'GROOM_REVIEW'");
        quarantine = area("Q", "QUARANTINE");
        jdbc.update("UPDATE stable_stalls SET groom_id=? WHERE area_id=? AND stall_number BETWEEN 1 AND 3", groomA, quarantine);
        jdbc.update("UPDATE stable_stalls SET groom_id=? WHERE area_id=? AND stall_number BETWEEN 4 AND 6", groomB, quarantine);
        // The reserve policy needs free regular stalls with a groom.
        long regular = area("R", "REGULAR");
        jdbc.update("UPDATE stable_stalls SET groom_id=? WHERE area_id=?", groomA, regular);
    }

    long user(String role, String suffix) {
        return jdbc.queryForObject("""
            INSERT INTO users(full_name,email,password_hash,role_id,is_active)
            SELECT ?,?,'unused',id,true FROM roles WHERE name=? RETURNING id
            """, Long.class, "Claim test " + tag + suffix, tag + suffix + "@test.invalid", role);
    }

    long area(String suffix, String type) {
        String code = "C" + tag + suffix;
        long id = jdbc.queryForObject("INSERT INTO areas(code,name,type) VALUES (?,?,?) RETURNING id",
                Long.class, code, code, type);
        jdbc.update("""
            INSERT INTO stable_stalls(area_id,stall_number,stall_code,status)
            SELECT ?,n,?||LPAD(n::text,2,'0'),'AVAILABLE' FROM generate_series(1,12) n
            """, id, code);
        return id;
    }

    /** An admission waiting for groom review; older start times come first in the pool. */
    long admission(String startedAt) {
        return jdbc.queryForObject("""
            INSERT INTO admission_applications(owner_id,status,submitted_at,groom_review_started_at,created_at,updated_at)
            VALUES (?,'GROOM_REVIEW',?::timestamp,?::timestamp,?::timestamp,?::timestamp) RETURNING id
            """, Long.class, owner, startedAt, startedAt, startedAt, startedAt);
    }

    static GroomAdmissionReviewRequest approve() {
        GroomAdmissionReviewRequest request = new GroomAdmissionReviewRequest();
        request.setDecision(ReviewDecision.APPROVED);
        return request;
    }

    Long reservedStallGroom(long admissionId) {
        return jdbc.queryForObject("""
            SELECT ss.groom_id FROM admission_applications a JOIN stable_stalls ss ON ss.id = a.quarantine_stall_id
            WHERE a.id = ?
            """, Long.class, admissionId);
    }

    @Test void approvalReservesTheReviewersOwnStall() {
        long x = admission("1990-01-01 08:00");

        reviews.review(x, groomB, approve());

        assertEquals("WAITING_FOR_ARRIVAL", jdbc.queryForObject("SELECT status FROM admission_applications WHERE id=?", String.class, x));
        assertEquals(groomB, reservedStallGroom(x));
        assertEquals("RESERVED", jdbc.queryForObject(
                "SELECT ss.status FROM admission_applications a JOIN stable_stalls ss ON ss.id=a.quarantine_stall_id WHERE a.id=?",
                String.class, x));
    }

    @Test void claimingMovesThePoolToTheLessLoadedGroom() {
        long x = admission("1990-01-01 08:00"), y = admission("1990-01-01 09:00");

        reviews.review(x, groomA, approve());
        ApiException notInPool = assertThrows(ApiException.class, () -> reviews.review(y, groomA, approve()));
        reviews.review(y, groomB, approve());

        assertEquals("NOT_IN_GROOM_POOL", notInPool.getErrorCode());
        assertEquals(groomA, reservedStallGroom(x));
        assertEquals(groomB, reservedStallGroom(y));
    }

    @Test void approvalsFollowTheOrderReviewsStarted() {
        admission("1990-01-01 08:00");
        long newer = admission("1990-01-01 09:00");

        ApiException error = assertThrows(ApiException.class, () -> reviews.review(newer, groomA, approve()));

        assertEquals("OLDER_ADMISSION_FIRST", error.getErrorCode());
    }

    @Test void twoGroomsApprovingTheSameAdmissionAtOnceGiveItOneStall() throws Exception {
        long x = admission("1990-01-01 08:00");
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var futures = new ArrayList<Future<Integer>>();
            for (long groom : new long[]{groomA, groomB}) futures.add(executor.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                try {
                    reviews.review(x, groom, approve());
                    return 200;
                } catch (ResponseStatusException ex) {
                    return ex.getStatusCode().value();
                } catch (ApiException ex) {
                    return ex.getStatus().value();
                }
            }));
            var statuses = List.of(futures.get(0).get(25, TimeUnit.SECONDS), futures.get(1).get(25, TimeUnit.SECONDS));
            assertEquals(1, statuses.stream().filter(s -> s == 200).count());
            assertEquals(1, statuses.stream().filter(s -> s == 409).count());
        }
        assertEquals(1, jdbc.queryForObject("""
            SELECT COUNT(*) FROM stable_stalls WHERE area_id=? AND status='RESERVED'
            """, Integer.class, quarantine));
    }
}
