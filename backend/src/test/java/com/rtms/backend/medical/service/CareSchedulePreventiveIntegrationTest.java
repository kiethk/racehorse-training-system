package com.rtms.backend.medical.service;

import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.audit.repository.AuditLogRepository;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.identity.repository.VeterinarianProfileRepository;
import com.rtms.backend.medical.dto.CompleteCareScheduleRequest;
import com.rtms.backend.medical.dto.VaccinationRequest;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.entity.PreventiveProtocol;
import com.rtms.backend.medical.enums.CareSchedulePurpose;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.enums.VaccinationSource;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.medical.repository.HorseHealthMetricRepository;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.incident.repository.HorseIncidentReportRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.service.TrainingDecisionService;
import com.rtms.backend.training.service.TrainingWorkoutService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** completeCareSchedule + vaccinations[], cửa sổ 14 ngày của auto-assign, guard LAST_FOLLOW_UP. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CareSchedulePreventiveIntegrationTest {

    @Mock CareScheduleRepository schedules;
    @Mock HorseRepository horses;
    @Mock HealthRecordRepository healthRecords;
    @Mock HorseHealthMetricRepository metrics;
    @Mock AdmissionApplicationRepository admissions;
    @Mock StableStallRepository stalls;
    @Mock UserRepository users;
    @Mock HorseIncidentReportRepository incidents;
    @Mock VeterinarianProfileRepository profiles;
    @Mock AuditLogRepository audits;
    @Mock ApplicationEventPublisher events;
    @Mock EntityManager entityManager;
    @Mock NotificationService notifications;
    @Mock TrainingWorkoutService workouts;
    @Mock PreventiveVaccinationService vaccinations;

    private CareScheduleService service;
    private Horse horse;
    private CareSchedule exam;

    @BeforeEach
    void setUp() {
        service = new CareScheduleService(schedules, horses, healthRecords, metrics, admissions, stalls, users,
                incidents, profiles, audits, events, entityManager, notifications,
                new TrainingDecisionService(horses, schedules, workouts), vaccinations);

        horse = new Horse();
        horse.setId(10L);
        horse.setName("Thunder");
        horse.setCurrentStatus(HorseStatus.ELIGIBLE);
        horse.setTrainingDecision(TrainingDecision.ALLOWED);

        exam = new CareSchedule();
        exam.setId(100L);
        exam.setHorseId(10L);
        exam.setVeterinarianId(20L);
        exam.setCareType(CareType.ROUTINE);
        exam.setStatus(CareScheduleStatus.IN_PROGRESS);

        when(schedules.findById(100L)).thenReturn(Optional.of(exam));
        when(schedules.findByIdForUpdate(100L)).thenReturn(Optional.of(exam));
        when(horses.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        when(healthRecords.save(any(HealthRecord.class))).thenAnswer(call -> {
            HealthRecord record = call.getArgument(0);
            record.setId(999L);
            return record;
        });
        when(schedules.save(any(CareSchedule.class))).thenAnswer(call -> call.getArgument(0));
        // Lịch khám lại tự động (AUTO cadence) cũng đi qua saveAndFlush.
        when(schedules.saveAndFlush(any(CareSchedule.class))).thenAnswer(call -> call.getArgument(0));
    }

    // ---- completeCareSchedule + vaccinations[] ----

    @Test
    void vaccinationsAreRecordedOnTheExamRecordThenTheMilestoneMovesAfterCompletion() {
        CompleteCareScheduleRequest request = request();
        request.setVaccinations(List.of(vaccination(1L)));
        List<PreventiveVaccinationService.ValidatedVaccination> validated = List.of(
                new PreventiveVaccinationService.ValidatedVaccination(new PreventiveProtocol(),
                        request.getVaccinations().get(0), VaccinationSource.ADMINISTERED, LocalDate.now()));
        when(vaccinations.validate(request.getVaccinations(), CareType.ROUTINE)).thenReturn(validated);

        service.completeCareSchedule(100L, request, 20L);

        assertEquals(CareScheduleStatus.COMPLETED, exam.getStatus());
        InOrder order = inOrder(vaccinations, healthRecords, schedules);
        order.verify(vaccinations).validate(request.getVaccinations(), CareType.ROUTINE);
        order.verify(healthRecords).save(any(HealthRecord.class));
        order.verify(vaccinations).record(999L, validated);
        order.verify(schedules).save(exam);
        order.verify(vaccinations).refreshMilestone(10L, 999L, 20L, CareType.ROUTINE);
    }

    @Test
    void invalidVaccinationRejectsTheWholeExamBeforeAnythingIsWritten() {
        horse.setTrainingDecision(TrainingDecision.BLOCKED);
        CompleteCareScheduleRequest request = request();
        request.setVaccinations(List.of(vaccination(1L)));
        when(vaccinations.validate(any(), any())).thenThrow(new ApiException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_VACCINATION", "bad"));

        assertThrows(ApiException.class, () -> service.completeCareSchedule(100L, request, 20L));

        assertEquals(CareScheduleStatus.IN_PROGRESS, exam.getStatus());
        assertEquals(TrainingDecision.BLOCKED, horse.getTrainingDecision());
        verify(healthRecords, never()).save(any());
        verify(vaccinations, never()).record(any(), any());
        verify(vaccinations, never()).refreshMilestone(any(), any(), any(), any());
    }

    @Test
    void preventiveVisitWithoutVaccinationStillLeavesTheHorseAMilestone() {
        exam.setPurpose(CareSchedulePurpose.PREVENTIVE);

        service.completeCareSchedule(100L, request(), 20L);

        verify(vaccinations, never()).validate(any(), any());
        verify(vaccinations, never()).record(any(), any());
        verify(vaccinations).refreshMilestone(10L, 999L, 20L, CareType.ROUTINE);
    }

    @Test
    void ordinaryExamWithoutVaccinationDoesNotTouchPreventiveCare() {
        service.completeCareSchedule(100L, request(), 20L);

        assertEquals(CareScheduleStatus.COMPLETED, exam.getStatus());
        verifyNoInteractions(vaccinations);
    }

    // ---- hoãn tiêm ----

    @Test
    void deferringOnAPreventiveVisitBooksTheMilestoneLaterInsteadOfRefreshingIt() {
        exam.setPurpose(CareSchedulePurpose.PREVENTIVE);
        CompleteCareScheduleRequest request = request();
        request.setDeferVaccinationDays(8);

        service.completeCareSchedule(100L, request, 20L);

        assertEquals(CareScheduleStatus.COMPLETED, exam.getStatus());
        verify(vaccinations).deferMilestone(10L, 999L, 20L, 8);
        verify(vaccinations, never()).refreshMilestone(any(), any(), any(), any());
    }

    @Test
    void deferralIsOnlyForPreventiveVisitsWithoutVaccinationsAndWithinSevenToTenDays() {
        CompleteCareScheduleRequest onFollowUp = request();
        onFollowUp.setDeferVaccinationDays(8);
        assertThrows(ApiException.class, () -> service.completeCareSchedule(100L, onFollowUp, 20L));

        exam.setPurpose(CareSchedulePurpose.PREVENTIVE);
        CompleteCareScheduleRequest tooShort = request();
        tooShort.setDeferVaccinationDays(3);
        assertThrows(ApiException.class, () -> service.completeCareSchedule(100L, tooShort, 20L));

        CompleteCareScheduleRequest both = request();
        both.setDeferVaccinationDays(8);
        both.setVaccinations(List.of(vaccination(1L)));
        when(vaccinations.validate(any(), any())).thenReturn(List.of(
                new PreventiveVaccinationService.ValidatedVaccination(new PreventiveProtocol(),
                        both.getVaccinations().get(0), VaccinationSource.ADMINISTERED, LocalDate.now())));
        assertThrows(ApiException.class, () -> service.completeCareSchedule(100L, both, 20L));

        assertEquals(CareScheduleStatus.IN_PROGRESS, exam.getStatus());
        verify(healthRecords, never()).save(any());
    }

    // ---- cửa sổ 14 ngày ----

    @Test
    void routineScheduleBeyondTheHorizonIsNotAssigned() {
        CareSchedule milestone = new CareSchedule();
        milestone.setId(200L);
        milestone.setHorseId(10L);
        milestone.setCareType(CareType.ROUTINE);
        milestone.setPurpose(CareSchedulePurpose.PREVENTIVE);
        milestone.setStatus(CareScheduleStatus.REQUESTED);
        milestone.setRequestedAt(LocalDateTime.now().plusDays(180));

        service.assignRequestedSchedule(milestone);

        assertEquals(CareScheduleStatus.REQUESTED, milestone.getStatus());
        assertNull(milestone.getVeterinarianId());
        verify(horses, never()).findByIdForUpdate(any());
        verify(schedules, never()).save(any());
    }

    @Test
    void horizonOnlyHoldsBackRoutineWorkWithAFarRequestedDate() {
        LocalDateTime horizon = LocalDateTime.of(2026, 11, 1, 0, 0);
        assertTrue(CareScheduleService.isBeyondAssignmentHorizon(schedule(CareType.ROUTINE, horizon), horizon));
        assertFalse(CareScheduleService.isBeyondAssignmentHorizon(schedule(CareType.ROUTINE, horizon.minusMinutes(1)), horizon));
        assertFalse(CareScheduleService.isBeyondAssignmentHorizon(schedule(CareType.ROUTINE, null), horizon));
        assertFalse(CareScheduleService.isBeyondAssignmentHorizon(schedule(CareType.INITIAL, horizon.plusDays(30)), horizon));
        assertFalse(CareScheduleService.isBeyondAssignmentHorizon(schedule(CareType.URGENT, horizon.plusDays(30)), horizon));
    }

    @Test
    void batchQueryIsGivenTheHorizon() {
        when(schedules.lockNextRequestedBatch(anyInt(), any())).thenReturn(List.of());
        LocalDateTime before = LocalDateTime.now().plusDays(CareScheduleService.ASSIGNMENT_HORIZON_DAYS);

        service.assignRequestedSchedules();

        verify(schedules).lockNextRequestedBatch(eq(25), argThat(horizon -> !horizon.isBefore(before)
                && horizon.isBefore(before.plusMinutes(1))));
    }

    // ---- LAST_FOLLOW_UP ----

    @Test
    void blockedHorseCannotLoseItsLastFollowUpEvenIfAPreventiveMilestoneExists() {
        horse.setTrainingDecision(TrainingDecision.BLOCKED);
        exam.setStatus(CareScheduleStatus.SCHEDULED);
        when(users.findById(1L)).thenReturn(Optional.of(manager()));
        when(horses.findById(10L)).thenReturn(Optional.of(horse));
        // Chỉ còn mốc tiêm phòng: query lọc FOLLOW_UP trả false.
        when(schedules.existsByHorseIdAndPurposeAndStatusInAndIdNot(
                eq(10L), eq(CareSchedulePurpose.FOLLOW_UP), anyList(), eq(100L))).thenReturn(false);

        var cancel = new com.rtms.backend.medical.dto.CancelCareScheduleRequest();
        cancel.setReason("Vet on leave");
        ApiException error = assertThrows(ApiException.class, () -> service.cancelCareSchedule(100L, cancel, 1L));

        assertEquals("LAST_FOLLOW_UP", error.getErrorCode());
    }

    @Test
    void preventiveMilestoneOfABlockedHorseCanBeCancelled() {
        horse.setTrainingDecision(TrainingDecision.BLOCKED);
        exam.setStatus(CareScheduleStatus.SCHEDULED);
        exam.setPurpose(CareSchedulePurpose.PREVENTIVE);
        when(users.findById(1L)).thenReturn(Optional.of(manager()));
        when(horses.findById(10L)).thenReturn(Optional.of(horse));

        var cancel = new com.rtms.backend.medical.dto.CancelCareScheduleRequest();
        cancel.setReason("Gộp vào ca khác");
        service.cancelCareSchedule(100L, cancel, 1L);

        assertEquals(CareScheduleStatus.CANCELLED, exam.getStatus());
        verify(schedules, never()).existsByHorseIdAndPurposeAndStatusInAndIdNot(any(), any(), any(), any());
    }

    // ---- helpers ----

    private static CompleteCareScheduleRequest request() {
        CompleteCareScheduleRequest request = new CompleteCareScheduleRequest();
        request.setFindings("Normal");
        request.setDiagnosis("Healthy");
        request.setTrainingDecision(TrainingDecision.ALLOWED);
        return request;
    }

    private static VaccinationRequest vaccination(long protocolId) {
        VaccinationRequest vaccination = new VaccinationRequest();
        vaccination.setProtocolId(protocolId);
        vaccination.setDosage("1 liều");
        return vaccination;
    }

    private static CareSchedule schedule(CareType type, LocalDateTime requestedAt) {
        CareSchedule schedule = new CareSchedule();
        schedule.setCareType(type);
        schedule.setRequestedAt(requestedAt);
        return schedule;
    }

    private static com.rtms.backend.identity.entity.User manager() {
        com.rtms.backend.identity.entity.Role role = new com.rtms.backend.identity.entity.Role();
        role.setName("CLUB_MANAGER");
        com.rtms.backend.identity.entity.User user = new com.rtms.backend.identity.entity.User();
        user.setId(1L);
        user.setRole(role);
        return user;
    }
}
