package com.rtms.backend.horse.service;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.horse.dto.HorseAdmissionHistoryResponse;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HorseAdmissionServiceTest {

    @Mock
    private HorseRepository horseRepo;
    @Mock
    private AdmissionApplicationRepository admissionRepo;
    @Mock
    private UserRepository userRepo;

    private HorseAdmissionService service;

    @BeforeEach
    void setUp() {
        service = new HorseAdmissionService(horseRepo, admissionRepo, userRepo);
    }

    private Horse createHorse(Long id, Long ownerId) {
        Horse h = new Horse();
        h.setId(id);
        h.setName("Thunder");
        h.setOwnerId(ownerId);
        return h;
    }

    private AdmissionApplication createAdmission(Long id, Long horseId, Long groomId, Long vetId) {
        AdmissionApplication a = new AdmissionApplication();
        a.setId(id);
        a.setHorseId(horseId);
        a.setOwnerId(1L);
        a.setStatus(AdmissionStatus.APPROVED);
        a.setSubmittedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        a.setGroomId(groomId);
        a.setVeterinarianId(vetId);
        return a;
    }

    private User createUser(Long id, String name) {
        User u = new User();
        u.setId(id);
        u.setFullName(name);
        return u;
    }

    @Test
    @DisplayName("getAdmissionsForHorse: Horse not found throws 404")
    void horseNotFound_throws404() {
        when(horseRepo.findById(999L)).thenReturn(Optional.empty());

        AuthenticatedUser manager = new AuthenticatedUser(1L, "mgr@rtms.com", "CLUB_MANAGER");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getAdmissionsForHorse(999L, manager));

        assertEquals(404, ex.getStatusCode().value());
        verifyNoInteractions(admissionRepo);
    }

    @Test
    @DisplayName("getAdmissionsForHorse: Owner of different horse throws AccessDeniedException")
    void otherOwner_throwsAccessDenied() {
        Horse h = createHorse(10L, 1L); // owned by user 1
        when(horseRepo.findById(10L)).thenReturn(Optional.of(h));

        AuthenticatedUser owner2 = new AuthenticatedUser(2L, "owner2@rtms.com", "HORSE_OWNER");
        assertThrows(AccessDeniedException.class,
                () -> service.getAdmissionsForHorse(10L, owner2));

        verifyNoInteractions(admissionRepo);
    }

    @Test
    @DisplayName("getAdmissionsForHorse: Queries strictly by horseId and batch resolves reviewer names")
    void validManager_queriesStrictlyByHorseId_resolvesNames() {
        Long horseId = 10L;
        Horse h = createHorse(horseId, 1L);
        when(horseRepo.findById(horseId)).thenReturn(Optional.of(h));

        AdmissionApplication adm = createAdmission(50L, horseId, 101L, 102L);
        when(admissionRepo.findByHorseIdOrderBySubmittedAtDesc(horseId)).thenReturn(List.of(adm));

        when(userRepo.findAllById(any())).thenReturn(List.of(
                createUser(101L, "Groom Gary"),
                createUser(102L, "Dr. Victor")
        ));

        AuthenticatedUser manager = new AuthenticatedUser(99L, "mgr@rtms.com", "CLUB_MANAGER");
        List<HorseAdmissionHistoryResponse> result = service.getAdmissionsForHorse(horseId, manager);

        // Verified query is strictly by horseId
        verify(admissionRepo, times(1)).findByHorseIdOrderBySubmittedAtDesc(horseId);

        assertEquals(1, result.size());
        HorseAdmissionHistoryResponse item = result.get(0);
        assertEquals(50L, item.admissionId());
        assertEquals(horseId, item.horseId());
        assertEquals("Groom Gary", item.groomName());
        assertEquals("Dr. Victor", item.vetName());
    }

    @Test
    @DisplayName("getAdmissionsForHorse: Empty admission list returns empty without user queries")
    void emptyAdmissions_returnsEmptyList() {
        Long horseId = 15L;
        Horse h = createHorse(horseId, 1L);
        when(horseRepo.findById(horseId)).thenReturn(Optional.of(h));
        when(admissionRepo.findByHorseIdOrderBySubmittedAtDesc(horseId)).thenReturn(List.of());

        AuthenticatedUser manager = new AuthenticatedUser(99L, "mgr@rtms.com", "CLUB_MANAGER");
        List<HorseAdmissionHistoryResponse> result = service.getAdmissionsForHorse(horseId, manager);

        assertTrue(result.isEmpty());
        verifyNoInteractions(userRepo);
    }
}
