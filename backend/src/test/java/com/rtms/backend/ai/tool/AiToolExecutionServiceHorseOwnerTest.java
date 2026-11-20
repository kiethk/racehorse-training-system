package com.rtms.backend.ai.tool;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.enums.HorseSex;
import com.rtms.backend.security.AuthenticatedUser;

import com.rtms.backend.training.dto.*;
//
import com.rtms.backend.training.entity.TrainingWorkout;
//
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.enums.WorkoutStatus;
//
import com.rtms.backend.admission.entity.CandidateHorseProfile;
import com.rtms.backend.horse.entity.HorsePedigree;
import com.rtms.backend.incident.entity.HorseIncidentReport;
import com.rtms.backend.identity.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class AiToolExecutionServiceHorseOwnerTest extends AiToolExecutionServiceTestSupport {

    // =================================================================
    // New Role Tools Tests
    // =================================================================

    @Test
    @DisplayName("HORSE_OWNER calls getMyHorseIncidentHistory to view a horse's incident history")
    void testOwner_GetMyHorseIncidentHistory() {
        AuthenticatedUser owner = new AuthenticatedUser(100L, "owner@rtms.com", "HORSE_OWNER");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("RED RUM");
        horse.setOwnerId(100L);
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        HorseIncidentReport rep = new HorseIncidentReport();
        rep.setId(1L);
        rep.setHorseId(10L);
        rep.setTitle("Minor abrasion on the left foreleg");
        rep.setDescription("Sustained a minor collision while moving");
        rep.setReportedAt(LocalDateTime.now());
        when(horseIncidentReportRepository.findByHorseId(10L)).thenReturn(List.of(rep));

        Object result = toolExecutionService.executeTool("getMyHorseIncidentHistory", Map.of("horseId", 10L), owner);
        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("RED RUM", item.get("horseName"));
        assertEquals("Minor abrasion on the left foreleg", item.get("title"));
        assertEquals("/owner/horses/10", item.get("actionUrl"));
    }

    @Test
    @DisplayName("HORSE_OWNER calls getMyHorsePedigreeInfo to retrieve a racehorse's pedigree")
    void testOwner_GetMyHorsePedigreeInfo() {
        AuthenticatedUser owner = new AuthenticatedUser(100L, "owner@rtms.com", "HORSE_OWNER");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("RED RUM");
        horse.setOwnerId(100L);
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        HorsePedigree ped = new HorsePedigree();
        ped.setHorseId(10L);
        ped.setSireName("Galileo");
        ped.setDamName("Urban Sea");
        ped.setPedigreeNotes("Origin: Ireland");
        when(horsePedigreeRepository.findByHorseId(10L)).thenReturn(Optional.of(ped));

        Object result = toolExecutionService.executeTool("getMyHorsePedigreeInfo", Map.of("horseId", 10L), owner);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("RED RUM", map.get("horseName"));
        assertEquals("Galileo", map.get("sire"));
        assertEquals("Urban Sea", map.get("dam"));
        assertEquals("/owner/horses/10", map.get("actionUrl"));
    }

    @Test
    @DisplayName("HORSE_OWNER calls analyzeHorseInvestmentAndFitnessROI to review fitness progress and investment returns")
    void testOwner_AnalyzeHorseInvestmentAndFitnessROI() {
        AuthenticatedUser owner = new AuthenticatedUser(100L, "owner@rtms.com", "HORSE_OWNER");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("RED RUM");
        horse.setRegistrationNumber("VN-HORSE-10");
        horse.setOwnerId(100L);
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        TrainingWorkout w1 = new TrainingWorkout();
        w1.setId(1L);
        w1.setStatus(WorkoutStatus.RAN);
        w1.setActualDistanceMeters(new BigDecimal("1000"));
        w1.setPerformanceRating(8);
        w1.setTopSpeedKmh(new BigDecimal("58.2"));

        when(trainingWorkoutRepository.findPastWorkouts(eq(10L), any())).thenReturn(List.of(ranYesterday(w1)));

        Object result = toolExecutionService.executeTool("analyzeHorseInvestmentAndFitnessROI", Map.of("horseId", 10L), owner);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("RED RUM", map.get("horseName"));
        assertEquals("VN-HORSE-10", map.get("registrationNumber"));
        assertEquals(1, map.get("totalWorkouts"));
        assertEquals(1, map.get("qualifiedWorkoutsCount"));
        assertNotNull(map.get("fitnessProgressionRate"));
        assertNotNull(map.get("racingPotentialEvaluation"));
        assertEquals("/owner/horses/10", map.get("actionUrl"));
    }

    @Test
    @DisplayName("HORSE_OWNER calls getMyHorses to retrieve sex, breed, dateOfBirth, registryName, and horseId")
    void testHorseOwner_GetMyHorses_ContainsEnrichedFields() {
        AuthenticatedUser owner = new AuthenticatedUser(100L, "owner@rtms.com", "HORSE_OWNER");

        Horse h = new Horse();
        h.setId(10L);
        h.setName("RED RUM");
        h.setRegistrationNumber("VN-RED");
        h.setBreed("Thoroughbred");
        h.setSex(HorseSex.MALE);
        h.setDateOfBirth(LocalDate.of(2022, 5, 1));
        h.setOwnerId(100L);
        h.setCurrentStatus(HorseStatus.ELIGIBLE);
        h.setTrainingDecision(TrainingDecision.ALLOWED);
        h.setTrainingDecisionReason("Excellent physical condition");
        h.setRegistryName("Vietnam Stud Book");

        when(horseRepository.findByOwnerId(100L)).thenReturn(List.of(h));

        Object result = toolExecutionService.executeTool("getMyHorses", Map.of(), owner);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        List<?> list = (List<?>) map.get("horseList");
        assertEquals(1, list.size());

        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals(10L, item.get("horseId"));
        assertEquals("RED RUM", item.get("horseName"));
        assertEquals("Thoroughbred", item.get("breed"));
        assertEquals("MALE", item.get("sex"));
        assertEquals("2022-05-01", item.get("dateOfBirth"));
        assertEquals("Excellent physical condition", item.get("trainingDecisionReason"));
        assertEquals("Vietnam Stud Book", item.get("registryName"));
    }

    @Test
    @DisplayName("HORSE_OWNER calls getMyHorseAdmissions to use the CandidateHorseProfile name when horseId is null instead of 'Horse in admission application'")
    void testHorseOwner_GetMyHorseAdmissions_CandidateNameAndEnrichedFields() {
        AuthenticatedUser owner = new AuthenticatedUser(100L, "owner@rtms.com", "HORSE_OWNER");

        AdmissionApplication app = new AdmissionApplication();
        app.setId(55L);
        app.setOwnerId(100L);
        app.setHorseId(null); // Application awaiting review
        app.setStatus(AdmissionStatus.VET_REVIEW);
        app.setSubmittedAt(LocalDateTime.of(2026, 11, 1, 9, 0));

        when(admissionApplicationRepository.findByOwnerIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(app));

        CandidateHorseProfile cand = new CandidateHorseProfile();
        cand.setId(1L);
        cand.setAdmissionId(55L);
        cand.setName("DIVINE STEED");
        cand.setBreed("Arabian");
        cand.setSex(HorseSex.FEMALE);
        cand.setDateOfBirth(LocalDate.of(2023, 1, 15));
        cand.setRegistrationNumber("VN-999");
        when(candidateHorseProfileRepository.findByAdmissionId(55L)).thenReturn(Optional.of(cand));

        Object result = toolExecutionService.executeTool("getMyHorseAdmissions", Map.of(), owner);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        List<?> list = (List<?>) map.get("applicationList");
        assertEquals(1, list.size());

        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("DIVINE STEED", item.get("horseName"));
        assertEquals("DIVINE STEED", item.get("candidateName"));
        assertEquals("Arabian", item.get("breed"));
        assertEquals("FEMALE", item.get("sex"));
        assertEquals("2023-01-15", item.get("dateOfBirth"));
        assertEquals("VN-999", item.get("registrationNumber"));
    }
}
