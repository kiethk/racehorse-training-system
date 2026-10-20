package com.rtms.backend.stable.service;

import com.rtms.backend.stable.dto.AreaTrainerChangeCheck;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/** Read-only eligibility checks; training ownership and schedules are never transferred. */
@Service
public class AreaTrainerAssignmentPolicy {
    private final JdbcTemplate jdbc;

    public AreaTrainerAssignmentPolicy(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final String CHECK_SQL = """
        SELECT a.id, a.trainer_id, a.type,
            EXISTS (SELECT 1 FROM horses h JOIN stable_stalls s ON s.id=h.current_stall_id
                    WHERE s.area_id=a.id) AS has_horses,
            EXISTS (SELECT 1 FROM stable_stalls s WHERE s.area_id=a.id
                    AND s.status IN ('OCCUPIED','RESERVED')) AS has_occupied_stalls,
            (EXISTS (SELECT 1 FROM horse_training_plans p WHERE p.trainer_id=a.trainer_id
                     AND p.status IN ('UPCOMING','ACTIVE'))
             OR EXISTS (SELECT 1 FROM training_lots l WHERE l.trainer_id=a.trainer_id
                        AND l.status='SCHEDULED')
             OR EXISTS (SELECT 1 FROM training_workouts w
                        JOIN horse_training_plans p ON p.id=w.plan_id
                        JOIN training_lots l ON l.id=w.lot_id
                        WHERE (p.trainer_id=a.trainer_id OR l.trainer_id=a.trainer_id)
                          AND w.status='SCHEDULED' AND l.status<>'CANCELLED')) AS has_training
        FROM areas a WHERE a.deleted_at IS NULL
        """;

    @Transactional(readOnly = true)
    public List<AreaTrainerChangeCheck> checks() { return readChecks(null); }

    @Transactional(readOnly = true)
    public AreaTrainerChangeCheck check(Long areaId) {
        return readChecks(areaId).stream().findFirst().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Area not found"));
    }

    /** Called only for an actual change, after StableAssignmentService locks Areas and stalls. */
    public void requireChangeAllowed(Long areaId) {
        var check = check(areaId);
        if (!check.canChange()) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Cannot change Trainer: " + String.join(" ", check.reasons()));
    }

    private List<AreaTrainerChangeCheck> readChecks(Long areaId) {
        String sql = CHECK_SQL + (areaId == null ? " ORDER BY a.id" : " AND a.id=?");
        return jdbc.query(sql, (row, index) -> {
            var reasons = new ArrayList<String>();
            if (!"REGULAR".equals(row.getString("type"))) reasons.add("Quarantine areas do not have a Trainer.");
            if (row.getBoolean("has_horses")) reasons.add("Area has horses. Trainer reassignment is unavailable.");
            else if (row.getBoolean("has_occupied_stalls")) reasons.add("Area has occupied or reserved stalls.");
            // A Trainer's lots may contain horses from multiple Areas. There is no safe Area-only transfer.
            if (row.getBoolean("has_training")) reasons.add("Current Trainer has unfinished training. Complete or cancel it before changing assignments.");
            return new AreaTrainerChangeCheck(row.getLong("id"), row.getObject("trainer_id", Long.class),
                    reasons.isEmpty(), List.copyOf(reasons));
        }, areaId == null ? new Object[0] : new Object[]{areaId});
    }
}
