package com.rtms.backend.ai.tool;

import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.security.AuthenticatedUser;

import com.rtms.backend.training.repository.projection.SessionHorseNameRow;
import com.rtms.backend.training.repository.projection.SessionRow;
import com.rtms.backend.training.dto.*;
import com.rtms.backend.training.entity.Course;
//
import com.rtms.backend.training.enums.SessionStatus;
//
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.enums.CourseRunStatus;
//
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.rtms.backend.training.dto.HorseAttendanceResponse;
import com.rtms.backend.training.dto.GroupMemberResponse;
import com.rtms.backend.training.dto.SessionSummaryResponse;
import com.rtms.backend.training.dto.SessionDetailResponse;
import com.rtms.backend.training.dto.SessionHorseResponse;
import com.rtms.backend.training.dto.TrainingGroupResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class AiToolExecutionServiceTrainerSessionsTest extends AiToolExecutionServiceTestSupport {

    // =================================================================
    // HEAD_TRAINER Tests
    // =================================================================

    @Test
    @DisplayName("HEAD_TRAINER calls getTrainerHorses to retrieve managed horses and their assigned grooms")
    void testTrainer_GetTrainerHorses() {
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@rtms.com", "HEAD_TRAINER");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("RED RUM");
        horse.setCurrentStatus(HorseStatus.ELIGIBLE);
        horse.setTrainingDecision(TrainingDecision.ALLOWED);

        when(horseRepository.findManagedHorsesByTrainerIdAndStatusIn(eq(5L), anyCollection()))
                .thenReturn(List.of(horse));

        Object result = toolExecutionService.executeTool("getTrainerHorses", Map.of(), trainer);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(1, map.get("totalManagedHorses"));
        assertTrue(map.containsKey("horseList"));
        List<?> list = (List<?>) map.get("horseList");
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertTrue(item.containsKey("specialtyCount"));
        assertTrue(item.containsKey("specialties"));
    }

    @Test
    @DisplayName("HEAD_TRAINER calls getTrainerSchedule to retrieve session details with a clear session time")
    void testTrainer_GetTrainerSchedule() {
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@rtms.com", "HEAD_TRAINER");

        SessionRow row = new SessionRow(
                100L, 5L, LocalDate.of(2026, 10, 20),
                LocalTime.of(8, 0), LocalTime.of(9, 30),
                1L, "Sprint 1000m", 60, SessionStatus.SCHEDULED, 2L
        );
        when(trainingSessionRepository.findTrainerSessionRows(eq(5L), any(LocalDate.class), any(LocalDate.class), eq(false)))
                .thenReturn(List.of(row));

        SessionHorseNameRow name1 = new SessionHorseNameRow(100L, 1L, "THUNDER");
        SessionHorseNameRow name2 = new SessionHorseNameRow(100L, 2L, "LIGHTNING");
        when(trainingSessionRepository.findActiveHorseNames(anyCollection()))
                .thenReturn(List.of(name1, name2));

        Object result = toolExecutionService.executeTool(
                "getTrainerSchedule",
                Map.of("date", "2026-10-20"),
                trainer
        );

        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> response = (Map<?, ?>) result;
        assertEquals(1, response.get("totalSessions"));
        List<?> list = (List<?>) response.get("sessionList");
        assertEquals(1, list.size());
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertTrue(item.get("sessionTime").toString().contains("Morning shift"));
        assertEquals("Sprint 1000m", item.get("exerciseName"));
        assertEquals(2L, item.get("horseCount"));
        List<?> horseNames = (List<?>) item.get("horseList");
        assertEquals(2, horseNames.size());
    }

    @Test
    @DisplayName("HEAD_TRAINER calls getTrainerPlansSummary to retrieve plans with their names, goals, and statuses")
    void testTrainer_GetTrainerPlansSummary() {
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@rtms.com", "HEAD_TRAINER");

        HorseCourseRunResponse summary = new HorseCourseRunResponse(
                1L, 10L, "RED RUM", 2L, "Sprint Course",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1),
                CourseRunStatus.ACTIVE, 20L, 10L, 0L
        );
        when(horseTrainingPlanRepository.findHorseCourseRuns(5L, 10L))
                .thenReturn(List.of(summary));
        when(horseAttendanceService.getHistory(10L, trainer)).thenReturn(List.of(new HorseAttendanceResponse(
                1L, "Group 1", false, "Sprint Course", "ACTIVE",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1), 7, 18, LocalDate.of(2026, 10, 1))));

        Course course = new Course();
        course.setId(2L);
        course.setName("Sprint Course");
        course.setTargetGoal("Improve short-distance speed");
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course));

        Object result = toolExecutionService.executeTool(
                "getTrainerPlansSummary",
                Map.of("horseId", 10L, "status", "ACTIVE"),
                trainer
        );

        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("Sprint Course", item.get("planName"));
        assertEquals("Improve short-distance speed", item.get("targetGoal"));
        assertTrue(item.get("status").toString().contains("ACTIVE"));
        assertEquals("7/18 sessions attended", item.get("progress"));
        assertEquals("Group 1", item.get("groupName"));
    }

    @Test
    @DisplayName("HEAD_TRAINER calls getTrainerGroups to view active groups, horses and grooms, current courses, and disbanded groups")
    void testTrainer_GetTrainerGroups() {
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@rtms.com", "HEAD_TRAINER");
        GroupMemberResponse thunder = new GroupMemberResponse(10L, "THUNDER", 7L, "Groom Andy", LocalDateTime.now());
        when(trainingGroupService.getGroups(trainer)).thenReturn(List.of(new TrainingGroupResponse(
                1L, "Morning Group 1", 5L, List.of(thunder), "Sprint Course", "ACTIVE",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 20), false)));
        when(trainingGroupService.getDisbandedGroups(trainer)).thenReturn(List.of(new TrainingGroupResponse(
                2L, "Old Group", 5L, List.of(), null, null, null, null, true)));

        Map<?, ?> map = (Map<?, ?>) toolExecutionService.executeTool("getTrainerGroups", Map.of(), trainer);

        Map<?, ?> live = (Map<?, ?>) ((List<?>) map.get("groupList")).get(0);
        assertEquals("Morning Group 1", live.get("groupName"));
        assertEquals("1/6", live.get("horseCount"));
        assertEquals(List.of("THUNDER (Groom: Groom Andy)"), live.get("horseList"));
        assertEquals("Sprint Course", live.get("currentCourse"));
        Map<?, ?> gone = (Map<?, ?>) ((List<?>) map.get("disbandedGroupList")).get(0);
        assertEquals("Old Group (Disbanded)", gone.get("groupName"));
    }

    @Test
    @DisplayName("HEAD_TRAINER calls getTrainerSchedule with needsResultOnly to view sessions with horses missing results")
    void testTrainer_GetTrainerScheduleNeedsResult() {
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@rtms.com", "HEAD_TRAINER");
        SessionSummaryResponse session = new SessionSummaryResponse(50L, 1L, 1L, "Morning Group 1", false, "Sprint Course",
                "Gallop 1200m", LocalDate.now().minusDays(1), LocalTime.of(6, 0), LocalTime.of(6, 30), "HELD", 2, 1, 5L, "Daniel Foster", List.of("THUNDER"));
        when(trainingSessionService.getSessions(any(), any(), isNull(), eq(true), eq(trainer))).thenReturn(List.of(session));
        when(trainingSessionService.getSession(50L, trainer)).thenReturn(new SessionDetailResponse(session, List.of(
                new SessionHorseResponse(1L, 10L, "THUNDER", "Groom Andy", "HELD", null, null),
                new SessionHorseResponse(2L, 11L, "BLAZE", "Groom Blake", "RAN", 7, null))));

        Map<?, ?> map = (Map<?, ?>) toolExecutionService.executeTool("getTrainerSchedule", Map.of("needsResultOnly", true), trainer);

        assertEquals(1, map.get("totalSessions"));
        assertEquals(1, map.get("totalWorkoutsNeedingResult"));
        Map<?, ?> item = (Map<?, ?>) ((List<?>) map.get("sessionList")).get(0);
        assertEquals(List.of("THUNDER"), item.get("horseList"));
        assertEquals("Morning Group 1", item.get("groupName"));
    }
}
