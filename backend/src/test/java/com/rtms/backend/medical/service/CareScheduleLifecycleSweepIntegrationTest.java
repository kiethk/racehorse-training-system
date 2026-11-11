package com.rtms.backend.medical.service;

import com.rtms.backend.horse.entity.Horse;

import com.rtms.backend.horse.repository.HorseRepository;

import com.rtms.backend.identity.entity.User;

import com.rtms.backend.identity.repository.RoleRepository;

import com.rtms.backend.identity.repository.UserRepository;

import com.rtms.backend.medical.entity.CareSchedule;

import com.rtms.backend.medical.enums.CareScheduleStatus;

import com.rtms.backend.medical.enums.CareType;

import com.rtms.backend.medical.enums.RescheduleReason;

import com.rtms.backend.medical.repository.CareScheduleRepository;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**

 * D1 integration trên PostgreSQL thật (ROADMAP v2.2 §4.5): auto-activate, EOD rollover,

 * URGENT escalation. Dữ liệu tạo trong transaction rồi rollback — không để lại gì trong DB.

 *

 * Chạy với backend TẮT: job 60s thật của scheduler sẽ đua bulk-update trên cùng hàng SCHEDULED

 * (pattern đã ghi ở CareScheduleDirectAssignmentTest / seed).

 * (Run with the backend stopped: the real 60s scheduler job races the bulk update.)

 */

@SpringBootTest

@Transactional

class CareScheduleLifecycleSweepIntegrationTest {

    @Autowired private CareScheduleService careScheduleService;

    @Autowired private CareScheduleRepository careSchedules;

    @Autowired private HorseRepository horses;

    @Autowired private UserRepository users;

    @Autowired private RoleRepository roles;

    @Autowired private EntityManager entityManager;

    private Long vetId;

    private Long horseId;

    @BeforeEach

    void buildFixture() {

        String tag = UUID.randomUUID().toString().substring(0, 8);

        User vet = new User();

        vet.setFullName("Sweep Test Vet");

        vet.setEmail("sweep-test-vet-" + tag + "@rtms.invalid");

        vet.setPasswordHash("unused");

        vet.setRole(roles.findByName("VETERINARIAN").orElseThrow());

        vetId = users.saveAndFlush(vet).getId();

        Horse horse = horses.findAll().stream()
                .filter(h -> !careSchedules.existsByHorseIdAndCareTypeAndStatusIn(h.getId(), CareType.URGENT, CareScheduleStatus.OPEN))
                .findFirst().orElseThrow();

        horseId = horse.getId();

    }

    private CareSchedule schedule(CareType type, CareScheduleStatus status, LocalDateTime scheduledAt) {

        CareSchedule s = new CareSchedule();

        s.setHorseId(horseId);

        s.setVeterinarianId(vetId);

        s.setCareType(type);

        s.setStatus(status);

        s.setScheduledAt(scheduledAt);

        s.setRequestedAt(scheduledAt);

        return careSchedules.saveAndFlush(s);

    }

    @Test

    void sweep_marksUnstartedAsOverdue_andInProgressAsDelayed() {

        LocalDateTime workdayNow = LocalDateTime.now().toLocalDate().atTime(10, 0);

        // Ca 1: SCHEDULED qua 35 phut chua ai vao kham -> OVERDUE (Vet bo ca / quen kham)

        CareSchedule unstarted = schedule(CareType.ROUTINE, CareScheduleStatus.SCHEDULED,

                workdayNow.minusMinutes(35));

        // Ca 2: SCHEDULED moi tre 5 phut -> Van giu SCHEDULED cho Vet

        CareSchedule waiting = schedule(CareType.ROUTINE, CareScheduleStatus.SCHEDULED,

                workdayNow.minusMinutes(5));

        // Ca 3: IN_PROGRESS da vao kham nhung qua gio -> DELAYED (Dang kham keo dai)

        CareSchedule runningLong = schedule(CareType.ROUTINE, CareScheduleStatus.IN_PROGRESS,

                workdayNow.minusMinutes(35));

        careScheduleService.runLifecycleSweep(workdayNow);

        entityManager.flush();

        entityManager.clear();

        assertEquals(CareScheduleStatus.SCHEDULED,

                careSchedules.findById(unstarted.getId()).orElseThrow().getStatus());

        assertEquals(CareScheduleStatus.SCHEDULED,

                careSchedules.findById(waiting.getId()).orElseThrow().getStatus());

        assertEquals(CareScheduleStatus.IN_PROGRESS,

                careSchedules.findById(runningLong.getId()).orElseThrow().getStatus());

    }

    @Test

    void eodRollover_movesUnfinishedInitialToNextWorkingDaySlot() {

        LocalDateTime today = LocalDateTime.now();

        CareSchedule stale = schedule(CareType.INITIAL, CareScheduleStatus.SCHEDULED,

                today.toLocalDate().atTime(9, 0));

        LocalDateTime evening = today.toLocalDate().atTime(17, 5);

        careScheduleService.runLifecycleSweep(evening);

        entityManager.flush();

        entityManager.clear();

        CareSchedule rolled = careSchedules.findById(stale.getId()).orElseThrow();

        assertEquals(CareScheduleStatus.SCHEDULED, rolled.getStatus());

        assertEquals(RescheduleReason.TIME_EXCEEDED, rolled.getRescheduleReason());
        assertEquals(1, rolled.getRolloverCount());
        assertNotNull(rolled.getScheduledDate());

        // Slot hợp lệ: sau giờ đóng cửa 17:05 và nằm trên lưới 30 phút của ngày làm việc kế.

        assertTrue(rolled.getScheduledDate().isAfter(evening.toLocalDate()));
        assertNull(rolled.getScheduledAt());

    }

    @Test

    void eodRollover_neverMovesUrgentCases() {

        LocalDateTime today = LocalDateTime.now();

        CareSchedule urgent = schedule(CareType.URGENT, CareScheduleStatus.IN_PROGRESS,

                today.minusDays(1));

        careScheduleService.runLifecycleSweep(today.toLocalDate().atTime(17, 5));

        entityManager.flush();

        entityManager.clear();

        CareSchedule kept = careSchedules.findById(urgent.getId()).orElseThrow();

        assertEquals(CareScheduleStatus.IN_PROGRESS, kept.getStatus());

        // Postgres ROUNDS nanoseconds to microseconds (±1µs) — so sánh tổng drift, không truncate.

        long driftNanos = Math.abs(java.time.Duration

                .between(urgent.getScheduledAt(), kept.getScheduledAt()).toNanos());

        assertTrue(driftNanos <= 1000,

                "scheduled_at drift beyond 1 microsecond: " + urgent.getScheduledAt() + " vs " + kept.getScheduledAt());

        assertNull(kept.getRescheduleReason());

    }

}

