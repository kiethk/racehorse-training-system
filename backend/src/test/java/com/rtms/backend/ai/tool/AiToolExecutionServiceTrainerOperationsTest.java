package com.rtms.backend.ai.tool;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.StableStall;

import com.rtms.backend.training.dto.*;
import com.rtms.backend.training.entity.Course;
//
//
import com.rtms.backend.training.enums.SurfaceType;
//
import com.rtms.backend.admission.entity.CandidateHorseProfile;
import com.rtms.backend.training.entity.Exercise;
import com.rtms.backend.training.enums.IntensityLevel;
import com.rtms.backend.training.enums.ExerciseCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class AiToolExecutionServiceTrainerOperationsTest extends AiToolExecutionServiceTestSupport {

    // =================================================================
    // HEAD_TRAINER Tests
    // =================================================================

    @Test
    @DisplayName("HEAD_TRAINER calls getTrainerAssignedGrooms to retrieve collaborating grooms")
    void testTrainer_GetTrainerAssignedGrooms() {
        AuthenticatedUser trainer = new AuthenticatedUser(5L, "trainer@rtms.com", "HEAD_TRAINER");

        Area area = new Area();
        area.setId(1L);
        area.setName("Training Area A");
        area.setTrainerId(5L);
        when(areaRepository.findAll()).thenReturn(List.of(area));

        StableStall stall = new StableStall();
        stall.setId(10L);
        stall.setAreaId(1L);
        stall.setGroomId(7L);
        stall.setStallCode("A-01");
        when(stableStallRepository.findByAreaIdInOrdered(List.of(1L))).thenReturn(List.of(stall));

        User groomUser = new User();
        groomUser.setId(7L);
        groomUser.setFullName("John Groom");
        groomUser.setPhone("0988123456");
        when(userRepository.findById(7L)).thenReturn(Optional.of(groomUser));

        Object result = toolExecutionService.executeTool("getTrainerAssignedGrooms", Map.of(), trainer);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(1, map.get("totalCollaboratingGrooms"));
        List<?> groomList = (List<?>) map.get("groomList");
        assertNotNull(groomList);
        assertEquals(1, groomList.size());
        Map<?, ?> groomItem = (Map<?, ?>) groomList.get(0);
        assertEquals("0988123456", groomItem.get("phone"));
        assertEquals("No email available", groomItem.get("email"));
    }

    @Test
    @DisplayName("HEAD_TRAINER calls getAssignedAdmissionReviews to retrieve applications awaiting review")
    void testTrainer_GetAssignedAdmissionReviews() {
        AuthenticatedUser trainer = new AuthenticatedUser(200L, "trainer@rtms.com", "HEAD_TRAINER");

        AdmissionApplication app = new AdmissionApplication();
        app.setId(301L);
        app.setTrainerId(200L);
        app.setStatus(AdmissionStatus.TRAINER_REVIEW);
        app.setSubmittedAt(LocalDateTime.of(2026, 10, 15, 9, 0));
        when(admissionApplicationRepository.findAllByOrderBySubmittedAtDesc()).thenReturn(List.of(app));

        CandidateHorseProfile cand = new CandidateHorseProfile();
        cand.setAdmissionId(301L);
        cand.setName("WIND RUNNER");
        cand.setBreed("Thoroughbred");
        when(candidateHorseProfileRepository.findByAdmissionIdIn(any())).thenReturn(List.of(cand));

        Object result = toolExecutionService.executeTool("getAssignedAdmissionReviews", Map.of(), trainer);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> response = (Map<?, ?>) result;
        assertEquals(1, response.get("pendingReviewCount"));
        List<?> list = (List<?>) response.get("applicationList");
        assertEquals(1, list.size());
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("WIND RUNNER", item.get("horseName"));
        assertEquals("Thoroughbred", item.get("breed"));
        assertEquals("/trainer/admissions/301", item.get("actionUrl"));
    }

    @Test
    @DisplayName("HEAD_TRAINER calls getCourseCurriculumOverview to retrieve the course and exercise catalog")
    void testTrainer_GetCourseCurriculumOverview() {
        AuthenticatedUser trainer = new AuthenticatedUser(200L, "trainer@rtms.com", "HEAD_TRAINER");

        Course c = new Course();
        c.setId(1L);
        c.setName("High-Speed Course");
        c.setTargetGoal("Improve acceleration");
        c.setTotalSessions(12);
        when(courseRepository.findAll()).thenReturn(List.of(c));

        Exercise s = new Exercise();
        s.setId(10L);
        s.setName("400m Sprint");
        s.setCategory(ExerciseCategory.SPEED);
        s.setSurfaceType(SurfaceType.TURF);
        s.setTargetDistanceMeters(BigDecimal.valueOf(400));
        s.setIntensityLevel(IntensityLevel.HIGH);
        s.setDurationMinutes(45);
        when(exerciseRepository.findAll()).thenReturn(List.of(s));

        Object result = toolExecutionService.executeTool("getCourseCurriculumOverview", Map.of(), trainer);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertTrue(map.containsKey("courseCurriculumList"));
        assertTrue(map.containsKey("technicalExerciseList"));
        assertEquals("/trainer/courses", map.get("actionUrl"));
    }
}
