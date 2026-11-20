package com.rtms.backend.ai.tool;

import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.enums.AreaType;

import com.rtms.backend.training.dto.*;
//
//
import com.rtms.backend.training.enums.TrainingDecision;
//
import com.rtms.backend.medical.entity.HorseHealthMetric;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class AiToolExecutionServiceVeterinarianMonitoringTest extends AiToolExecutionServiceTestSupport {

    // =================================================================
    // VETERINARIAN Tests
    // =================================================================

    @Test
    @DisplayName("VETERINARIAN calls getVetExaminationOverview to review horses currently being examined, awaiting examination, and examined")
    void testVet_GetVetExaminationOverview() {
        AuthenticatedUser vet = new AuthenticatedUser(8L, "vet@test.com", "VETERINARIAN");

        CareSchedule cs1 = new CareSchedule();
        cs1.setId(1L);
        cs1.setHorseId(10L);
        cs1.setStatus(CareScheduleStatus.IN_PROGRESS);
        cs1.setVeterinarianId(8L);

        CareSchedule cs2 = new CareSchedule();
        cs2.setId(2L);
        cs2.setHorseId(11L);
        cs2.setStatus(CareScheduleStatus.SCHEDULED);
        cs2.setVeterinarianId(8L);

        when(careScheduleRepository.findByVeterinarianId(8L)).thenReturn(List.of(cs1, cs2));

        Object result = toolExecutionService.executeTool("getVetExaminationOverview", Map.of(), vet);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(1, map.get("inProgressExamsCount"));
        assertEquals(1, map.get("pendingScheduledExamsCount"));
    }

    @Test
    @DisplayName("VETERINARIAN calls getQuarantineStatus: quarantined horses are always BLOCKED and cannot train")
    void testVet_GetQuarantineStatus() {
        AuthenticatedUser vet = new AuthenticatedUser(8L, "vet@test.com", "VETERINARIAN");

        Area qArea = new Area();
        qArea.setId(99L);
        qArea.setCode("Q-A");
        qArea.setType(AreaType.QUARANTINE);
        when(areaRepository.findByType(AreaType.QUARANTINE)).thenReturn(List.of(qArea));

        StableStall qStall = new StableStall();
        qStall.setId(901L);
        qStall.setAreaId(99L);
        qStall.setStallCode("Q-01");
        when(stableStallRepository.findByAreaIdInOrdered(List.of(99L))).thenReturn(List.of(qStall));

        Horse qHorse = new Horse();
        qHorse.setId(10L);
        qHorse.setName("QUARANTINE HORSE");
        qHorse.setCurrentStallId(901L);
        qHorse.setTrainingDecision(TrainingDecision.BLOCKED);
        when(horseRepository.findByCurrentStallIdIn(List.of(901L))).thenReturn(List.of(qHorse));
        when(horseRepository.findAll()).thenReturn(List.of(qHorse));

        Object result = toolExecutionService.executeTool("getQuarantineStatus", Map.of(), vet);

        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(1, map.get("quarantinedOrRestrictedHorsesCount"));

        List<?> list = (List<?>) map.get("quarantinedHorseList");
        Map<?, ?> horseItem = (Map<?, ?>) list.get(0);
        assertEquals("BLOCKED", horseItem.get("trainingDecision"));
        assertEquals(false, horseItem.get("isTrainingAllowed"));
    }

    @Test
    @DisplayName("VETERINARIAN calls getUrgentCareAlerts to retrieve urgent care cases")
    void testVet_GetUrgentCareAlerts() {
        AuthenticatedUser vet = new AuthenticatedUser(300L, "vet@rtms.com", "VETERINARIAN");

        CareSchedule cs = new CareSchedule();
        cs.setId(501L);
        cs.setHorseId(10L);
        cs.setCareType(CareType.URGENT);
        cs.setStatus(CareScheduleStatus.SCHEDULED);
        cs.setDescription("Horse developed a sudden high fever");
        cs.setScheduledAt(LocalDateTime.of(2026, 10, 20, 14, 0));
        cs.setVeterinarianId(300L);
        when(careScheduleRepository.findByVeterinarianId(300L)).thenReturn(List.of(cs));

        Horse h = new Horse();
        h.setId(10L);
        h.setName("LIGHTNING");
        when(horseRepository.findAll()).thenReturn(List.of(h));

        Object result = toolExecutionService.executeTool("getUrgentCareAlerts", Map.of(), vet);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> response = (Map<?, ?>) result;
        assertEquals(1, response.get("totalUrgentAlerts"));
        List<?> list = (List<?>) response.get("alertList");
        assertEquals(1, list.size());
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("LIGHTNING", item.get("horseName"));
        assertEquals("Urgent", item.get("severity"));
        assertEquals("Horse developed a sudden high fever", item.get("symptoms"));
        assertEquals("/veterinarian/urgent/501", item.get("actionUrl"));
    }

    @Test
    @DisplayName("VETERINARIAN calls getHorseVitalSignsTrend to retrieve vital signs")
    void testVet_GetHorseVitalSignsTrend() {
        AuthenticatedUser vet = new AuthenticatedUser(300L, "vet@rtms.com", "VETERINARIAN");

        Horse h = new Horse();
        h.setId(10L);
        h.setName("LIGHTNING");
        when(horseRepository.findById(10L)).thenReturn(Optional.of(h));

        HorseHealthMetric metric = new HorseHealthMetric();
        metric.setHorseId(10L);
        metric.setWeight(BigDecimal.valueOf(480));
        metric.setTemperature(BigDecimal.valueOf(38.2));
        metric.setHeartRate(BigDecimal.valueOf(36));
        metric.setRespiratoryRate(BigDecimal.valueOf(12));
        metric.setRecordedAt(LocalDateTime.of(2026, 10, 20, 7, 0));
        when(horseHealthMetricRepository.findByHorseIdOrderByRecordedAtDesc(10L)).thenReturn(List.of(metric));

        Object result = toolExecutionService.executeTool("getHorseVitalSignsTrend", Map.of("horseId", 10L), vet);
        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("LIGHTNING", item.get("horseName"));
        assertEquals("480 kg", item.get("weight"));
        assertEquals("38.2 °C", item.get("temperature"));
        assertEquals("36 bpm", item.get("heartRate"));
        assertEquals("12 breaths/min", item.get("respiratoryRate"));
    }

    @Test
    @DisplayName("VETERINARIAN calls getHorseVitalSignsTrend without a horse or with all to retrieve the latest records")
    void testVet_GetHorseVitalSignsTrend_AllHorsesWhenEmptyOrAll() {
        AuthenticatedUser vet = new AuthenticatedUser(300L, "vet@rtms.com", "VETERINARIAN");

        HorseHealthMetric m1 = new HorseHealthMetric();
        m1.setHorseId(1L);
        m1.setRecordedAt(LocalDateTime.of(2026, 10, 20, 8, 0));
        m1.setTemperature(BigDecimal.valueOf(38.4));
        m1.setHeartRate(BigDecimal.valueOf(38));

        HorseHealthMetric m2 = new HorseHealthMetric();
        m2.setHorseId(2L);
        m2.setRecordedAt(LocalDateTime.of(2026, 10, 20, 9, 0));
        m2.setTemperature(BigDecimal.valueOf(38.1));
        m2.setHeartRate(BigDecimal.valueOf(35));

        Horse h1 = new Horse();
        h1.setId(1L);
        h1.setName("BELLA");

        Horse h2 = new Horse();
        h2.setId(2L);
        h2.setName("LIGHTNING");

        when(horseHealthMetricRepository.findAll()).thenReturn(List.of(m1, m2));
        when(horseRepository.findById(1L)).thenReturn(Optional.of(h1));
        when(horseRepository.findById(2L)).thenReturn(Optional.of(h2));

        // 1. No parameter
        Object resultEmpty = toolExecutionService.executeTool("getHorseVitalSignsTrend", Map.of(), vet);
        assertNotNull(resultEmpty);
        assertTrue(resultEmpty instanceof List<?>);
        List<?> listEmpty = (List<?>) resultEmpty;
        assertEquals(2, listEmpty.size());
        Map<?, ?> first = (Map<?, ?>) listEmpty.get(0);
        // m2 has a recordedAt of 9:00 > 8:00, so it appears first
        assertEquals("LIGHTNING", first.get("horseName"));
        assertEquals("38.1 °C", first.get("temperature"));

        // 2. Pass horseName = "all"
        Object resultAll = toolExecutionService.executeTool("getHorseVitalSignsTrend", Map.of("horseName", "all"), vet);
        assertNotNull(resultAll);
        assertEquals(2, ((List<?>) resultAll).size());
    }
}
