package com.rtms.backend.medical.service;

import com.rtms.backend.audit.entity.AuditLog;
import com.rtms.backend.audit.repository.AuditLogRepository;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.medical.dto.PreventiveProtocolStatus;
import com.rtms.backend.medical.dto.RescheduleCareScheduleRequest;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareSchedulePurpose;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.enums.PreventiveDueStatus;
import com.rtms.backend.medical.enums.ProtocolCategory;
import com.rtms.backend.medical.enums.RescheduleReason;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CareRescheduleServiceTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 18);

    @Mock CareScheduleRepository schedules;
    @Mock UserRepository users;
    @Mock AuditLogRepository audits;
    @Mock PreventiveStatusService preventiveStatus;

    private final AuthenticatedUser manager = new AuthenticatedUser(1L, "manager@rtms.com", "CLUB_MANAGER");
    private final AuthenticatedUser assignedVet = new AuthenticatedUser(20L, "vet@rtms.com", "VETERINARIAN");
    private final AuthenticatedUser otherVet = new AuthenticatedUser(21L, "vet2@rtms.com", "VETERINARIAN");
    private CareRescheduleService service;
    private CareSchedule schedule;

    @BeforeEach
    void setUp() {
        service = new CareRescheduleService(schedules, users, audits, preventiveStatus,
                Clock.fixed(TODAY.atTime(9, 0).atZone(ZONE).toInstant(), ZONE));
        schedule = new CareSchedule();
        schedule.setId(100L);
        schedule.setHorseId(10L);
        schedule.setCareType(CareType.ROUTINE);
        schedule.setStatus(CareScheduleStatus.SCHEDULED);
        schedule.setVeterinarianId(20L);
        schedule.setScheduledAt(TODAY.plusDays(2).atTime(9, 0));
        when(schedules.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));
        when(schedules.save(any())).thenAnswer(call -> call.getArgument(0));
        when(users.findById(any())).thenReturn(Optional.empty());
        when(preventiveStatus.statusFor(10L)).thenReturn(List.of());
    }

    @Test
    void assignedVetMovesAScheduledExamBackToTheQueue() {
        var response = service.reschedule(100L, request(TODAY.plusDays(9), RescheduleReason.UNDER_TREATMENT, " sốt "),
                assignedVet);

        assertEquals(CareScheduleStatus.REQUESTED, schedule.getStatus());
        assertNull(schedule.getVeterinarianId());
        assertNull(schedule.getScheduledAt());
        assertEquals(TODAY.plusDays(9).atTime(14, 0), schedule.getRequestedAt());
        assertEquals(RescheduleReason.UNDER_TREATMENT, schedule.getRescheduleReason());
        assertEquals("sốt", schedule.getRescheduleNote());
        assertEquals(CareScheduleStatus.REQUESTED, response.schedule().status());
        assertTrue(response.warnings().isEmpty());
    }

    @Test
    void everyRescheduleIsAudited() {
        service.reschedule(100L, request(TODAY.plusDays(9), RescheduleReason.WEATHER, null), manager);

        ArgumentCaptor<AuditLog> audit = ArgumentCaptor.forClass(AuditLog.class);
        verify(audits).save(audit.capture());
        assertEquals("Care Schedule Rescheduled", audit.getValue().getAction());
        assertEquals(100L, audit.getValue().getEntityId());
        assertEquals("Care schedule #100 rescheduled to " + TODAY.plusDays(9) + " (Reason: WEATHER)",
                audit.getValue().getDetails());
    }

    @Test
    void anotherVetCannotMoveSomeoneElsesSchedule() {
        assertForbidden(otherVet);
    }

    @Test
    void unassignedScheduleCanOnlyBeMovedByAManager() {
        schedule.setStatus(CareScheduleStatus.REQUESTED);
        schedule.setVeterinarianId(null);
        schedule.setScheduledAt(null);

        assertForbidden(assignedVet);

        service.reschedule(100L, request(TODAY.plusDays(30), RescheduleReason.OWNER_REQUEST, null), manager);
        assertEquals(TODAY.plusDays(30).atTime(14, 0), schedule.getRequestedAt());
        assertEquals(CareScheduleStatus.REQUESTED, schedule.getStatus());
    }

    @Test
    void onlyRoutineSchedulesInAPendingStateCanMove() {
        schedule.setCareType(CareType.INITIAL);
        assertCode("UNSUPPORTED_CARE_TYPE", manager);

        schedule.setCareType(CareType.ROUTINE);
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        assertCode("INVALID_STATUS", manager);
    }

    @Test
    void newDateMustBeInTheFutureAndAReasonIsRequired() {
        ApiException past = assertThrows(ApiException.class, () ->
                service.reschedule(100L, request(TODAY, RescheduleReason.OTHER, null), manager));
        assertEquals("INVALID_DATE", past.getErrorCode());

        ApiException noReason = assertThrows(ApiException.class, () ->
                service.reschedule(100L, request(TODAY.plusDays(3), null, null), manager));
        assertEquals("VALIDATION_ERROR", noReason.getErrorCode());
        verify(schedules, never()).save(any());
    }

    @Test
    void movingAPreventiveMilestonePastADueDateWarnsWithoutBlocking() {
        schedule.setPurpose(CareSchedulePurpose.PREVENTIVE);
        when(preventiveStatus.statusFor(10L)).thenReturn(List.of(
                status("Cúm ngựa", true, TODAY.plusDays(10), PreventiveDueStatus.DUE_SOON),
                status("Uốn ván", true, TODAY.plusDays(200), PreventiveDueStatus.OK),
                status("Tẩy giun", true, null, PreventiveDueStatus.MISSING),
                status("Dại", false, TODAY.minusDays(5), PreventiveDueStatus.OVERDUE)));

        var response = service.reschedule(100L, request(TODAY.plusDays(20), RescheduleReason.RACE_CONFLICT, null), manager);

        assertEquals(List.of("The horse will be overdue for Cúm ngựa from 28/10/2026.",
                "The horse has no record for Tẩy giun."), response.warnings());
        assertEquals(CareScheduleStatus.REQUESTED, schedule.getStatus());
        response.warnings().forEach(warning -> assertFalse(warning.toLowerCase().contains("race")));
    }

    @Test
    void followUpExamsGetNoVaccinationWarnings() {
        service.reschedule(100L, request(TODAY.plusDays(20), RescheduleReason.OTHER, null), manager);
        verifyNoInteractions(preventiveStatus);
    }

    // ---- helpers ----

    private void assertForbidden(AuthenticatedUser actor) {
        ApiException error = assertThrows(ApiException.class, () ->
                service.reschedule(100L, request(TODAY.plusDays(5), RescheduleReason.OTHER, null), actor));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
        verify(schedules, never()).save(any());
    }

    private void assertCode(String code, AuthenticatedUser actor) {
        ApiException error = assertThrows(ApiException.class, () ->
                service.reschedule(100L, request(TODAY.plusDays(5), RescheduleReason.OTHER, null), actor));
        assertEquals(code, error.getErrorCode());
    }

    private static RescheduleCareScheduleRequest request(LocalDate date, RescheduleReason reason, String note) {
        RescheduleCareScheduleRequest request = new RescheduleCareScheduleRequest();
        request.setNewDate(date);
        request.setReason(reason);
        request.setNote(note);
        return request;
    }

    private static PreventiveProtocolStatus status(String name, boolean mandatory, LocalDate nextDue,
            PreventiveDueStatus dueStatus) {
        return new PreventiveProtocolStatus(10L, 1L, name, name, ProtocolCategory.VACCINE, mandatory,
                nextDue == null ? null : nextDue.minusDays(180), nextDue, dueStatus);
    }
}
