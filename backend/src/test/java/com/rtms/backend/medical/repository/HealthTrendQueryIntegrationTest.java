package com.rtms.backend.medical.repository;

import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.RoleRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.medical.dto.HealthReadingRow;
import com.rtms.backend.medical.entity.HorseHealthMetric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Chạy câu lấy các lần đo sức khỏe trên PostgreSQL thật (CAST sang LocalDate, JOIN lấy tên ngựa, nửa khoảng ngày). Dữ
 * liệu tạo trong một transaction rồi rollback.
 * (Runs the health readings query on a real PostgreSQL: the CAST to LocalDate, the JOIN for the horse name and the
 * half-open date range. The data is created inside one transaction and rolled back.)
 */
@SpringBootTest
@Transactional
class HealthTrendQueryIntegrationTest {

    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private HorseRepository horses;
    @Autowired private HorseHealthMetricRepository metrics;

    private final LocalDate today = LocalDate.now();
    private Horse horse1;
    private Horse horse2;

    @BeforeEach
    void buildFixture() {
        User owner = new User();
        owner.setFullName("IT health owner");
        owner.setEmail("it-health-" + System.nanoTime() + "@test.invalid");
        owner.setPasswordHash("not-used");
        owner.setRole(roles.findByName("HORSE_OWNER").orElseThrow());
        owner.setActive(true);
        owner = users.saveAndFlush(owner);

        horse1 = horse("IT Health One", owner);
        horse2 = horse("IT Health Two", owner);
    }

    private Horse horse(String name, User owner) {
        Horse horse = new Horse();
        horse.setName(name);
        horse.setOwnerId(owner.getId());
        return horses.saveAndFlush(horse);
    }

    private void reading(Horse horse, LocalDateTime at, String temperature, String heartRate, String bodyCondition) {
        HorseHealthMetric metric = new HorseHealthMetric();
        metric.setHorseId(horse.getId());
        metric.setRecordedAt(at);
        metric.setTemperature(temperature == null ? null : new BigDecimal(temperature));
        metric.setHeartRate(heartRate == null ? null : new BigDecimal(heartRate));
        metric.setBodyConditionScore(bodyCondition == null ? null : new BigDecimal(bodyCondition));
        metrics.saveAndFlush(metric);
    }

    private List<HealthReadingRow> readingsOf(List<Long> horseIds) {
        return metrics.findHealthReadings(horseIds, today.minusDays(29).atStartOfDay(), today.plusDays(1).atStartOfDay());
    }

    @Test
    void readingsCarryTheDayTheHorseNameAndTheValues() {
        LocalDateTime noon = today.minusDays(1).atTime(12, 0);
        reading(horse1, noon, "38.7", "40", "5.0");
        reading(horse2, noon.plusHours(3), null, "44", "7.0");
        reading(horse1, today.minusDays(40).atTime(9, 0), "40.0", "90", "9.0");   // outside the window

        List<HealthReadingRow> rows = readingsOf(List.of(horse1.getId(), horse2.getId()));

        assertEquals(2, rows.size(), "Only the readings inside the window");
        HealthReadingRow first = rows.get(0);
        assertEquals(today.minusDays(1), first.date());
        assertEquals(horse1.getId(), first.horseId());
        assertEquals("IT Health One", first.horseName());
        assertEquals(0, new BigDecimal("38.7").compareTo(first.temperature()));
        assertNull(rows.get(1).temperature(), "A value nobody measured stays null");
    }

    @Test
    void aReadingOnTheLastDayIsStillInsideTheRange() {
        reading(horse1, today.atTime(23, 30), "38.0", "40", "6.0");

        List<HealthReadingRow> rows = readingsOf(List.of(horse1.getId()));

        assertEquals(1, rows.size());
        assertEquals(today, rows.get(0).date());
    }

    @Test
    void onlyTheGivenHorsesCount() {
        reading(horse1, today.atTime(8, 0), "38.0", "40", "6.0");

        assertTrue(readingsOf(List.of(horse2.getId())).isEmpty());
    }
}
