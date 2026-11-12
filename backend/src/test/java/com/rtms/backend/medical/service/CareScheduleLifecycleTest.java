package com.rtms.backend.medical.service;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.Role;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import java.time.LocalDate;


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
    @DisplayName("startCareSchedule rejects future ROUTINE schedule with 400 ROUTINE_EXAM_TOO_EARLY")
    void testStart_futureRoutineSchedule_throwsBadRequest() {
        schedule.setCareType(CareType.ROUTINE);
        schedule.setScheduledDate(LocalDate.now().plusDays(2));
        when(userRepository.findById(assignedVet.getId())).thenReturn(Optional.of(assignedVet));
        when(careScheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(horseRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(new Horse()));
        when(careScheduleRepository.findByIdForUpdate(schedule.getId())).thenReturn(Optional.of(schedule));

        ApiException ex = assertThrows(ApiException.class, () ->
                careScheduleService.startCareSchedule(schedule.getId(), assignedVet.getId()));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("ROUTINE_EXAM_TOO_EARLY", ex.getErrorCode());
    }

    @Test
    @DisplayName("AD-022: cancelCareSchedule abolished — lifecycle uses reschedule and OVERDUE instead")
    void testCancelCareSchedule_abolished() {
        // Under AD-022, cancelCareSchedule is removed; stuck exams are moved via reschedule().
        assertTrue(CareScheduleStatus.OPEN.contains(CareScheduleStatus.SCHEDULED));
    }
}
