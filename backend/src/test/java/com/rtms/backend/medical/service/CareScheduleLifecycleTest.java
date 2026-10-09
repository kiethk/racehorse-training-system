package com.rtms.backend.medical.service;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.Role;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.repository.CareScheduleRepository;


import com.rtms.backend.config.ApiException;
import com.rtms.backend.medical.dto.CancelCareScheduleRequest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CareScheduleLifecycleTest {

    @Mock private CareScheduleRepository careScheduleRepository;
    @Mock private HorseRepository horseRepository;
    @Mock private AdmissionApplicationRepository admissionRepository;
    @Mock private UserRepository userRepository;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private CareScheduleService careScheduleService;

    private CareSchedule schedule;
    private User assignedVet;
    private User otherVet;

    @BeforeEach
    void setUp() {
        assignedVet = new User();
        assignedVet.setId(1L);

        otherVet = new User();
        otherVet.setId(2L);

        schedule = new CareSchedule();
        schedule.setId(10L);
        schedule.setHorseId(100L);
        schedule.setVeterinarianId(assignedVet.getId());
        schedule.setStatus(CareScheduleStatus.SCHEDULED);
    }

    @Test
    @DisplayName("startCareSchedule rejects unauthorized veterinarian with 403 FORBIDDEN")
    void testStart_unauthorizedVet_throwsForbidden() {
        when(userRepository.findById(otherVet.getId())).thenReturn(Optional.of(otherVet));
        when(careScheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(horseRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(new Horse()));
        when(careScheduleRepository.findByIdForUpdate(schedule.getId())).thenReturn(Optional.of(schedule));

        ApiException ex = assertThrows(ApiException.class, () ->
                careScheduleService.startCareSchedule(schedule.getId(), otherVet.getId()));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("FORBIDDEN", ex.getErrorCode());
    }

    @Test
    @DisplayName("startCareSchedule rejects non-SCHEDULED status with 409 CONFLICT")
    void testStart_invalidStatus_throwsConflict() {
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        when(userRepository.findById(assignedVet.getId())).thenReturn(Optional.of(assignedVet));
        when(careScheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(horseRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(new Horse()));
        when(careScheduleRepository.findByIdForUpdate(schedule.getId())).thenReturn(Optional.of(schedule));

        ApiException ex = assertThrows(ApiException.class, () ->
                careScheduleService.startCareSchedule(schedule.getId(), assignedVet.getId()));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("INVALID_STATUS", ex.getErrorCode());
    }

    @Test
    @DisplayName("cancelCareSchedule permits CLUB_MANAGER and rejects VETERINARIAN")
    void testCancelCareSchedule_authorization() {
        when(careScheduleRepository.findByIdForUpdate(schedule.getId())).thenReturn(Optional.of(schedule));

        Role vetRole = new Role();
        vetRole.setName("VETERINARIAN");
        assignedVet.setRole(vetRole);
        when(userRepository.findById(assignedVet.getId())).thenReturn(Optional.of(assignedVet));

        CancelCareScheduleRequest cancelReq = new CancelCareScheduleRequest();
        cancelReq.setReason("Cancel request");

        ApiException ex = assertThrows(ApiException.class, () ->
                careScheduleService.cancelCareSchedule(schedule.getId(), cancelReq, assignedVet.getId()));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());

        User manager = new User();
        manager.setId(3L);
        Role mgrRole = new Role();
        mgrRole.setName("CLUB_MANAGER");
        manager.setRole(mgrRole);
        when(userRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));

        var response = careScheduleService.cancelCareSchedule(schedule.getId(), cancelReq, manager.getId());
        assertEquals(CareScheduleStatus.CANCELLED, response.status());
    }
}
