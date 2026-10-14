package com.rtms.backend.horse.controller;

import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.enums.ReviewDecision;
import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.horse.dto.HorseAdmissionHistoryResponse;
import com.rtms.backend.horse.service.HorseAdmissionService;
import com.rtms.backend.horse.service.HorsePhotoService;
import com.rtms.backend.horse.service.HorseService;
import com.rtms.backend.training.service.HorsePerformanceService;
import com.rtms.backend.training.service.HorseSpecialtyService;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.enums.TrainingDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HorseAdmissionEndpointTest {

    @Mock
    private HorseService horseService;
    @Mock
    private HorsePerformanceService performanceService;
    @Mock
    private HorseSpecialtyService specialtyService;
    @Mock
    private HorsePhotoService photoService;
    @Mock
    private HorseAdmissionService admissionService;

    private HorseController controller;

    @BeforeEach
    void setUp() {
        controller = new HorseController(horseService, performanceService, specialtyService, photoService, admissionService);
    }

    @Test
    @DisplayName("GET /api/horses/{id}/admissions: Owner of different horse is blocked with AccessDeniedException")
    void getAdmissions_ownerNotOwningHorse_throwsAccessDenied() {
        Long horseId = 55L;
        AuthenticatedUser unauthOwner = new AuthenticatedUser(2L, "other@rtms.com", "HORSE_OWNER");

        when(admissionService.getAdmissionsForHorse(horseId, unauthOwner))
                .thenThrow(new AccessDeniedException("Access denied: You do not own this horse."));

        assertThrows(AccessDeniedException.class, () -> controller.getAdmissions(horseId, unauthOwner));
        verify(admissionService, times(1)).getAdmissionsForHorse(horseId, unauthOwner);
    }

    @Test
    @DisplayName("GET /api/horses/{id}/admissions: Manager receives admissions linked strictly to horseId with milestones")
    void getAdmissions_manager_returnsAdmissionsLinkedToHorseId() {
        Long horseId = 10L;
        AuthenticatedUser manager = new AuthenticatedUser(99L, "manager@rtms.com", "CLUB_MANAGER");

        LocalDateTime submitted = LocalDateTime.of(2026, 1, 15, 9, 0);
        LocalDateTime groomRev = LocalDateTime.of(2026, 1, 16, 10, 0);
        LocalDateTime arrived = LocalDateTime.of(2026, 1, 17, 14, 0);
        LocalDateTime vetRev = LocalDateTime.of(2026, 1, 18, 11, 30);
        LocalDateTime trainerRev = LocalDateTime.of(2026, 1, 19, 15, 0);
        LocalDateTime managerRev = LocalDateTime.of(2026, 1, 20, 16, 0);

        HorseAdmissionHistoryResponse historyItem = new HorseAdmissionHistoryResponse(
                101L,
                horseId,
                AdmissionStatus.APPROVED,
                submitted,
                3L,
                "Groom John",
                groomRev,
                ReviewDecision.APPROVED,
                "Health checked",
                arrived,
                4L,
                "Dr. Smith",
                vetRev,
                TrainingDecision.ALLOWED,
                "Cleared for training",
                5L,
                "Trainer Bob",
                trainerRev,
                "Ready for flat racing",
                99L,
                "Manager Alice",
                managerRev,
                ReviewDecision.APPROVED,
                "Final approval"
        );

        when(admissionService.getAdmissionsForHorse(horseId, manager))
                .thenReturn(List.of(historyItem));

        ApiResponse<List<HorseAdmissionHistoryResponse>> response = controller.getAdmissions(horseId, manager);

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        HorseAdmissionHistoryResponse item = response.getData().get(0);
        assertEquals(101L, item.admissionId());
        assertEquals(horseId, item.horseId());
        assertEquals(AdmissionStatus.APPROVED, item.status());
        assertEquals(submitted, item.submittedAt());
        assertEquals("Groom John", item.groomName());
        assertEquals("Dr. Smith", item.vetName());
        assertEquals(TrainingDecision.ALLOWED, item.vetTrainingDecision());
        assertEquals("Trainer Bob", item.trainerName());
        assertEquals("Manager Alice", item.managerName());
    }

    @Test
    @DisplayName("GET /api/horses/{id}/admissions: Horse with no admissions returns empty list")
    void getAdmissions_noHistory_returnsEmptyList() {
        Long horseId = 20L;
        AuthenticatedUser manager = new AuthenticatedUser(99L, "manager@rtms.com", "CLUB_MANAGER");

        when(admissionService.getAdmissionsForHorse(horseId, manager))
                .thenReturn(Collections.emptyList());

        ApiResponse<List<HorseAdmissionHistoryResponse>> response = controller.getAdmissions(horseId, manager);

        assertTrue(response.isSuccess());
        assertTrue(response.getData().isEmpty());
    }
}
