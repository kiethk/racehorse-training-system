package com.rtms.backend.stable.service;

import com.rtms.backend.stable.dto.AreaDeletionCheck;
import com.rtms.backend.stable.dto.AreaDeletionResult;
import com.rtms.backend.stable.enums.AreaType;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Lifecycle SQL intentionally includes hidden rows; normal repositories exclude them. */
@Service
public class AreaLifecycleService {
    private final JdbcTemplate jdbc;
    private final EntityManager em;

    public AreaLifecycleService(JdbcTemplate jdbc, EntityManager em) { this.jdbc = jdbc; this.em = em; }

    private record AreaRow(long id, String code, Long trainerId, boolean deleted) { }
    private record StallRow(int number, Long groomId, String status, boolean deleted) { }

    @Transactional(readOnly = true)
    public AreaDeletionCheck check(Long id) {
        AreaRow area = loadArea(id, false);
        return inspect(area, loadStalls(id, false), false);
    }

    @Transactional
    public AreaDeletionResult delete(Long id) {
        em.flush();
        lockAssignments();
        AreaRow area = loadArea(id, true);
        // Same order as assignment/Staff: Area first, then stalls by ID. Admission
        // locks only stall rows and never waits for this Area lock.
        List<StallRow> stalls = loadStalls(id, true);
        AreaDeletionCheck check = inspect(area, stalls, false);
        if (!check.canDelete()) throw new ResponseStatusException(HttpStatus.CONFLICT, String.join(" ", check.reasons()));
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("UPDATE stable_stalls SET deleted_at=?,updated_at=? WHERE area_id=?", now, now, id);
        jdbc.update("UPDATE areas SET deleted_at=?,updated_at=? WHERE id=?", now, now, id);
        // SQL lifecycle updates bypass Hibernate; evict its old visibility snapshot.
        em.clear();
        return new AreaDeletionResult(id, area.code(), stalls.size());
    }

    /** Called inside AreaService's creation transaction and its per-type creation lock. */
    @Transactional
    public Long restoreFirst(AreaType type) {
        em.flush();
        lockAssignments();
        List<AreaRow> candidates = jdbc.query("""
                SELECT id,code,trainer_id,deleted_at FROM areas
                WHERE type=? AND deleted_at IS NOT NULL ORDER BY length(code),code FOR UPDATE
                """, (rs, row) -> new AreaRow(rs.getLong("id"), rs.getString("code"),
                rs.getObject("trainer_id", Long.class), rs.getTimestamp("deleted_at") != null), type.name());
        for (AreaRow area : candidates) {
            if (!inspect(area, loadStalls(area.id(), true), true).canDelete()) continue;
            jdbc.update("UPDATE stable_stalls SET deleted_at=NULL,updated_at=CURRENT_TIMESTAMP WHERE area_id=?", area.id());
            jdbc.update("UPDATE areas SET deleted_at=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=?", area.id());
            em.clear();
            return area.id();
        }
        return null;
    }

    private void lockAssignments() {
        jdbc.queryForObject("SELECT 1 FROM pg_advisory_xact_lock(hashtextextended(?,0))", Integer.class, "STABLE_ASSIGNMENTS_LOCK");
    }

    private AreaRow loadArea(Long id, boolean lock) {
        String sql = "SELECT id,code,trainer_id,deleted_at FROM areas WHERE id=?" + (lock ? " FOR UPDATE" : "");
        var rows = jdbc.query(sql, (rs, row) -> new AreaRow(rs.getLong("id"), rs.getString("code"),
                rs.getObject("trainer_id", Long.class), rs.getTimestamp("deleted_at") != null), id);
        if (rows.isEmpty() || rows.getFirst().deleted()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Area not found");
        return rows.getFirst();
    }

    private List<StallRow> loadStalls(long id, boolean lock) {
        String sql = "SELECT stall_number,groom_id,status,deleted_at FROM stable_stalls WHERE area_id=? ORDER BY id"
                + (lock ? " FOR UPDATE" : "");
        return jdbc.query(sql, (rs, row) -> new StallRow(rs.getInt("stall_number"), rs.getObject("groom_id", Long.class),
                rs.getString("status"), rs.getTimestamp("deleted_at") != null), id);
    }

    private AreaDeletionCheck inspect(AreaRow area, List<StallRow> stalls, boolean restoring) {
        List<String> reasons = new ArrayList<>();
        if (area.trainerId() != null) reasons.add("Remove the Trainer assignment first.");
        if (stalls.size() != AreaService.STALLS_PER_AREA || stalls.stream().map(StallRow::number).distinct().count() != 12
                || stalls.stream().anyMatch(s -> s.number() < 1 || s.number() > 12 || s.deleted() != restoring)) {
            reasons.add("Area must contain a complete set of 12 stalls in the expected lifecycle state.");
        }
        if (stalls.stream().anyMatch(s -> s.groomId() != null)) reasons.add("Remove all Groom assignments first.");
        if (stalls.stream().anyMatch(s -> !"AVAILABLE".equals(s.status()))) reasons.add("All stalls must be available, with no reservations or maintenance.");
        if (count("SELECT COUNT(*) FROM horses h JOIN stable_stalls s ON s.id=h.current_stall_id WHERE s.area_id=?", area.id()) > 0) {
            reasons.add("Move all horses out of this Area first.");
        }
        // Include closed/rejected applications: this operation is for unused Areas,
        // not retiring facilities with history. Never silently alter admission history.
        if (count("SELECT COUNT(*) FROM admission_applications a JOIN stable_stalls s ON s.id=a.quarantine_stall_id WHERE s.area_id=?", area.id()) > 0) {
            reasons.add("Area has Admission history and cannot be deleted.");
        }
        return new AreaDeletionCheck(area.id(), area.code(), stalls.size(), reasons.isEmpty(), List.copyOf(reasons));
    }

    private long count(String sql, long id) { return jdbc.queryForObject(sql, Long.class, id); }
}
