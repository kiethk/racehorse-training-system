package com.rtms.backend.ai.tool;

import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.incident.enums.IncidentStatus;
import com.rtms.backend.incident.enums.IncidentType;

import com.rtms.backend.stable.enums.GroomTaskType;
import com.rtms.backend.training.dto.*;
//
//
//
import com.rtms.backend.medical.entity.HorseHealthMetric;
import com.rtms.backend.incident.entity.HorseIncidentReport;
import com.rtms.backend.stable.entity.GroomDailyTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class AiToolExecutionServiceGroomHandlerTest extends AiToolExecutionServiceTestSupport {

    // =================================================================
    // GROOM Tests
    // =================================================================

    @Test
    @DisplayName("GROOM calls getHorseStallLocation to look up a horse's stall location")
    void testGroom_GetHorseStallLocation() {
        AuthenticatedUser groom = new AuthenticatedUser(10L, "groom1@rtms.com", "GROOM");

        Horse horse = new Horse();
        horse.setId(8L);
        horse.setName("NAUSICAA");
        horse.setCurrentStallId(6L);
        when(horseRepository.findById(8L)).thenReturn(Optional.of(horse));

        StableStall stall = new StableStall();
        stall.setId(6L);
        stall.setStallCode("A-01");
        stall.setStallNumber(1);
        when(stableStallRepository.findById(6L)).thenReturn(Optional.of(stall));

        Object result = toolExecutionService.executeTool("getHorseStallLocation", Map.of("horseId", 8L), groom);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("NAUSICAA", map.get("horseName"));
        assertTrue(map.get("stallLocation").toString().contains("A-01"));
    }

    @Test
    @DisplayName("GROOM calls getMyReportedIncidents to retrieve incidents reported by that groom")
    void testGroom_GetMyReportedIncidents() {
        AuthenticatedUser groom = new AuthenticatedUser(400L, "groom@rtms.com", "GROOM");

        HorseIncidentReport rep = new HorseIncidentReport();
        rep.setId(601L);
        rep.setReportedById(400L);
        rep.setHorseId(10L);
        rep.setTitle("Minor crack in the left hind hoof");
        rep.setIncidentType(IncidentType.PROBLEM);
        rep.setStatus(IncidentStatus.REPORTED);
        rep.setReportedAt(LocalDateTime.now().minusDays(1));
        when(horseIncidentReportRepository.findByReportedById(400L)).thenReturn(List.of(rep));

        Horse h = new Horse();
        h.setId(10L);
        h.setName("SHADOW");
        when(horseRepository.findAll()).thenReturn(List.of(h));

        Object result = toolExecutionService.executeTool("getMyReportedIncidents", Map.of(), groom);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> response = (Map<?, ?>) result;
        assertEquals(1, response.get("totalReportedIncidents"));
        List<?> list = (List<?>) response.get("incidentList");
        assertEquals(1, list.size());
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("SHADOW", item.get("horseName"));
        assertEquals("Minor crack in the left hind hoof", item.get("title"));
        assertEquals("/groom/incidents", item.get("actionUrl"));
    }

    @Test
    @DisplayName("GROOM calls getGroomTaskCompletionRate to review task completion progress")
    void testGroom_GetGroomTaskCompletionRate() {
        AuthenticatedUser groom = new AuthenticatedUser(400L, "groom@rtms.com", "GROOM");

        GroomDailyTask t1 = new GroomDailyTask();
        t1.setId(1L);
        t1.setGroomId(400L);
        t1.setIsCompleted(true);

        GroomDailyTask t2 = new GroomDailyTask();
        t2.setId(2L);
        t2.setGroomId(400L);
        t2.setIsCompleted(false);

        when(groomDailyTaskRepository.findByGroomIdAndScheduledTimeBetween(eq(400L), any(), any()))
                .thenReturn(List.of(t1, t2));

        Object result = toolExecutionService.executeTool("getGroomTaskCompletionRate", Map.of(), groom);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(2, map.get("totalTasksCount"));
        assertEquals(1L, map.get("completedTasksCount"));
        assertEquals("50.0%", map.get("completionRate"));
        assertEquals("/groom/tasks", map.get("actionUrl"));
    }

    @Test
    @DisplayName("GROOM calls detectHorseBehavioralAnomaly to detect early signs of abnormal behavior")
    void testGroom_DetectHorseBehavioralAnomaly() {
        AuthenticatedUser groom = new AuthenticatedUser(400L, "groom@rtms.com", "GROOM");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("SHADOW");
        horse.setRegistrationNumber("VN-SHADOW");
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        HorseIncidentReport rep = new HorseIncidentReport();
        rep.setTitle("Horse refusing to eat and pawing the ground continuously");
        rep.setDescription("Showing signs of persistent abdominal discomfort");
        when(horseIncidentReportRepository.findByHorseId(10L)).thenReturn(List.of(rep));

        HorseHealthMetric m = new HorseHealthMetric();
        m.setHeartRate(new BigDecimal("48"));
        when(horseHealthMetricRepository.findByHorseIdOrderByRecordedAtDesc(10L)).thenReturn(List.of(m));

        Object result = toolExecutionService.executeTool("detectHorseBehavioralAnomaly", Map.of("horseId", 10L), groom);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("SHADOW", map.get("horseName"));
        assertEquals("Emergency", map.get("alertLevel"));
        assertTrue(map.get("immediateActionRequired").toString().contains("Notify the on-call veterinarian immediately"));
        assertEquals("/groom/incidents", map.get("actionUrl"));
    }

    @Test
    @DisplayName("GROOM calls recommendDailyCarePriorityOrder to recommend the daily care priority order")
    void testGroom_RecommendDailyCarePriorityOrder() {
        AuthenticatedUser groom = new AuthenticatedUser(400L, "groom@rtms.com", "GROOM");

        GroomDailyTask t1 = new GroomDailyTask(400L, 10L, GroomTaskType.FEEDING, LocalDateTime.now().withHour(8), "Feed");
        GroomDailyTask t2 = new GroomDailyTask(400L, 10L, GroomTaskType.VET_ASSIST, LocalDateTime.now().withHour(7), "Assist with vaccination");
        when(groomDailyTaskRepository.findByGroomIdAndScheduledTimeBetween(eq(400L), any(), any()))
                .thenReturn(List.of(t1, t2));

        Horse h = new Horse();
        h.setId(10L);
        h.setName("SHADOW");
        when(horseRepository.findAll()).thenReturn(List.of(h));

        Object result = toolExecutionService.executeTool("recommendDailyCarePriorityOrder", Map.of(), groom);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertTrue(map.containsKey("priorityOrderList"));
        assertTrue(map.containsKey("biosecurityNotes"));
        assertEquals("/groom/tasks", map.get("actionUrl"));
    }
}
