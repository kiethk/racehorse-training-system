package com.rtms.backend.ai.tool;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.security.AuthenticatedUser;

import com.rtms.backend.training.dto.*;
//
//
//
import com.rtms.backend.admission.entity.CandidateHorseProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class AiToolExecutionServiceManagerAdmissionTest extends AiToolExecutionServiceTestSupport {

    // =================================================================
    // CLUB_MANAGER Tests
    // =================================================================

    @Test
    @DisplayName("CLUB_MANAGER calls getAdmissionPipelineDetails to retrieve horse admission application details")
    void testManager_GetAdmissionPipelineDetails() {
        AuthenticatedUser manager = new AuthenticatedUser(1L, "manager@rtms.com", "CLUB_MANAGER");

        AdmissionApplication app = new AdmissionApplication();
        app.setId(123L);
        app.setOwnerId(50L);
        app.setStatus(AdmissionStatus.MANAGER_REVIEW);
        app.setSubmittedAt(LocalDateTime.of(2026, 10, 20, 10, 0));
        app.setVeterinarianId(8L);
        app.setTrainerId(5L);

        when(admissionApplicationRepository.findAll()).thenReturn(List.of(app));

        CandidateHorseProfile profile = new CandidateHorseProfile();
        profile.setId(1L);
        profile.setAdmissionId(123L);
        profile.setName("PEGASUS JUNIOR");
        when(candidateHorseProfileRepository.findByAdmissionId(123L)).thenReturn(Optional.of(profile));

        User owner = new User();
        owner.setId(50L);
        owner.setFullName("Horse Owner Carter");
        when(userRepository.findById(50L)).thenReturn(Optional.of(owner));

        User vet = new User();
        vet.setId(8L);
        vet.setFullName("Dr. Hoang");
        when(userRepository.findById(8L)).thenReturn(Optional.of(vet));

        User trainer = new User();
        trainer.setId(5L);
        trainer.setFullName("Trainer Park");
        when(userRepository.findById(5L)).thenReturn(Optional.of(trainer));

        Object result = toolExecutionService.executeTool("getAdmissionPipelineDetails", Map.of("status", "PENDING"), manager);
        assertNotNull(result);
        assertTrue(result instanceof List<?>);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());

        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("PEGASUS JUNIOR", item.get("horseName"));
        assertEquals("Horse Owner Carter", item.get("ownerName"));
        assertTrue(item.get("currentStage").toString().contains("MANAGER_REVIEW"));
        assertEquals("Dr. Hoang", item.get("assignedVet"));
        assertEquals("Trainer Park", item.get("designatedTrainer"));
        assertEquals("/manager/admissions/123", item.get("navigationUrl"));
        assertEquals("/manager/admissions/123", item.get("actionUrl"));
    }

    @Test
    @DisplayName("CLUB_MANAGER calls getAdmissionPipelineOverview to view application counts and admission details")
    void testManager_GetAdmissionPipelineOverview() {
        AuthenticatedUser manager = new AuthenticatedUser(1L, "manager@rtms.com", "CLUB_MANAGER");

        AdmissionApplication app = new AdmissionApplication();
        app.setId(123L);
        app.setOwnerId(50L);
        app.setStatus(AdmissionStatus.MANAGER_REVIEW);
        app.setSubmittedAt(LocalDateTime.of(2026, 10, 20, 10, 0));
        when(admissionApplicationRepository.findAll()).thenReturn(List.of(app));
        when(admissionApplicationRepository.findByStatus(AdmissionStatus.MANAGER_REVIEW)).thenReturn(List.of(app));

        CandidateHorseProfile profile = new CandidateHorseProfile();
        profile.setId(1L);
        profile.setAdmissionId(123L);
        profile.setName("PEGASUS JUNIOR");
        when(candidateHorseProfileRepository.findByAdmissionId(123L)).thenReturn(Optional.of(profile));

        Object result = toolExecutionService.executeTool("getAdmissionPipelineOverview", Map.of("status", "PENDING"), manager);
        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertTrue(map.containsKey("tongQuanSoLuong"));
        assertTrue(map.containsKey("chiTietCacDon"));
        assertEquals("/manager/admissions", map.get("actionUrl"));
    }
}
