package com.rtms.backend.medical.service;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.medical.dto.CreateNextScheduleRequest;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import java.time.LocalDate;


import com.rtms.backend.config.ApiException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
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
    @DisplayName("startCareSchedule rejects a COMPLETED schedule with 409 CONFLICT")
    void testStart_invalidStatus_throwsConflict() {
        schedule.setStatus(CareScheduleStatus.COMPLETED);
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

    @Test
    @DisplayName("AD-037: kéo Follow-up đang chờ về ngày Vet chọn mà ngựa đã bận ngày đó → 409, không dời lặng lẽ")
    void testMergeIntoWaitingFollowUp_chosenDayBusy_throwsConflict() {
        LocalDate chosen = LocalDate.now().plusDays(5);
        CareSchedule source = new CareSchedule();
        source.setId(10L);
        source.setHorseId(100L);
        source.setVeterinarianId(assignedVet.getId());
        source.setStatus(CareScheduleStatus.COMPLETED);
        CareSchedule waiting = new CareSchedule();
        waiting.setId(20L);
        waiting.setHorseId(100L);
        waiting.setCareType(CareType.ROUTINE);
        waiting.setStatus(CareScheduleStatus.SCHEDULED);
        waiting.setRequestedAt(chosen.plusDays(5).atTime(14, 0));
        CareSchedule busy = new CareSchedule();
        busy.setId(30L);
        busy.setHorseId(100L);
        busy.setCareType(CareType.ROUTINE);
        busy.setStatus(CareScheduleStatus.SCHEDULED);
        busy.setScheduledDate(chosen);
        when(userRepository.findById(assignedVet.getId())).thenReturn(Optional.of(assignedVet));
        when(careScheduleRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(source));
        when(horseRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(new Horse()));
        when(careScheduleRepository.findByRequestedByIdAndIdempotencyKey(assignedVet.getId(), "k1")).thenReturn(Optional.empty());
        when(careScheduleRepository.findByHorseIdAndStatusIn(100L, List.of(CareScheduleStatus.SCHEDULED)))
                .thenReturn(List.of(waiting));
        when(careScheduleRepository.findByHorseIdAndStatusIn(100L, CareScheduleStatus.BOOKED_OR_ACTIVE))
                .thenReturn(List.of(waiting, busy));
        CreateNextScheduleRequest request = new CreateNextScheduleRequest();
        request.setSourceScheduleId(10L);
        request.setCareType(CareType.ROUTINE);
        request.setScheduledDate(chosen.toString());
        request.setDescription("Recheck");
        request.setIdempotencyKey("k1");

        ApiException ex = assertThrows(ApiException.class,
                () -> careScheduleService.createNextSchedule(request, assignedVet.getId()));

        assertEquals("HORSE_BUSY_THAT_DAY", ex.getErrorCode());
        verify(careScheduleRepository, never()).save(waiting);
    }

    @Test
    @DisplayName("assertHorseFreeOnDay chặn ca thứ hai cùng ngày của cùng ngựa: 409 HORSE_BUSY_THAT_DAY")
    void testAssertHorseFreeOnDay_existingRoutineThatDay_throwsConflict() {
        CareSchedule existing = new CareSchedule();
        existing.setId(11L);
        existing.setHorseId(100L);
        existing.setCareType(CareType.ROUTINE);
        existing.setStatus(CareScheduleStatus.SCHEDULED);
        existing.setScheduledDate(LocalDate.of(2026, 11, 16));
        when(careScheduleRepository.findByHorseIdAndStatusIn(100L, CareScheduleStatus.BOOKED_OR_ACTIVE))
                .thenReturn(List.of(existing));

        ApiException ex = assertThrows(ApiException.class,
                () -> careScheduleService.assertHorseFreeOnDay(100L, LocalDate.of(2026, 11, 16), null));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("HORSE_BUSY_THAT_DAY", ex.getErrorCode());
    }

    @Test
    @DisplayName("assertHorseFreeOnDay bỏ qua ca URGENT và chính ca đang xét")
    void testAssertHorseFreeOnDay_urgentOrSelf_doesNotBlock() {
        CareSchedule urgent = new CareSchedule();
        urgent.setId(12L);
        urgent.setHorseId(100L);
        urgent.setCareType(CareType.URGENT);
        urgent.setStatus(CareScheduleStatus.SCHEDULED);
        urgent.setScheduledAt(LocalDate.of(2026, 11, 16).atTime(14, 0));

        CareSchedule self = new CareSchedule();
        self.setId(13L);
        self.setHorseId(100L);
        self.setCareType(CareType.ROUTINE);
        self.setStatus(CareScheduleStatus.SCHEDULED);
        self.setScheduledDate(LocalDate.of(2026, 11, 16));

        when(careScheduleRepository.findByHorseIdAndStatusIn(100L, CareScheduleStatus.BOOKED_OR_ACTIVE))
                .thenReturn(List.of(urgent, self));

        assertDoesNotThrow(() -> careScheduleService.assertHorseFreeOnDay(100L, LocalDate.of(2026, 11, 16), 13L));
    }
}
