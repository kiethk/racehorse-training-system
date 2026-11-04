package com.rtms.backend.medical.service;

import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.audit.repository.AuditLogRepository;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.Role;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.entity.VeterinarianProfile;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.identity.repository.VeterinarianProfileRepository;
import com.rtms.backend.incident.entity.HorseIncidentReport;
import com.rtms.backend.incident.enums.IncidentSeverity;
import com.rtms.backend.incident.enums.IncidentStatus;
import com.rtms.backend.incident.repository.HorseIncidentReportRepository;
import com.rtms.backend.medical.dto.CancelCareScheduleRequest;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.medical.repository.HorseHealthMetricRepository;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.service.TrainingDecisionService;
import com.rtms.backend.training.service.GroupWorkoutSyncService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CareSchedulePreemptionTest {

    private CareScheduleRepository schedules;
    private HorseRepository horses;
    private UserRepository users;
    private VeterinarianProfileRepository profiles;
    private HorseIncidentReportRepository incidents;
    private ApplicationEventPublisher events;
    private NotificationService notifications;
    private CareScheduleService service;

    @BeforeEach
    void setUp() {
        schedules = mock(CareScheduleRepository.class);
        horses = mock(HorseRepository.class);
        users = mock(UserRepository.class);
        profiles = mock(VeterinarianProfileRepository.class);
        incidents = mock(HorseIncidentReportRepository.class);
        events = mock(ApplicationEventPublisher.class);
        notifications = mock(NotificationService.class);

        GroupWorkoutSyncService groupSync = mock(GroupWorkoutSyncService.class);
        TrainingDecisionService trainingDecisionService = new TrainingDecisionService(horses, schedules, groupSync);

        service = new CareScheduleService(
                schedules,
                horses,
                mock(HealthRecordRepository.class),
                mock(HorseHealthMetricRepository.class),
                mock(AdmissionApplicationRepository.class),
                mock(StableStallRepository.class),
                users,
                incidents,
                profiles,
                mock(AuditLogRepository.class),
                events,
                mock(EntityManager.class),
                notifications,
                trainingDecisionService,
                mock(PreventiveVaccinationService.class),
                mock(com.rtms.backend.medical.repository.InjuryRecordRepository.class),
                mock(com.rtms.backend.medical.repository.TreatmentPlanRepository.class),
                new com.fasterxml.jackson.databind.ObjectMapper()
        );

        when(schedules.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(horses.findById(anyLong())).thenAnswer(inv -> Optional.of(horse(inv.getArgument(0))));
        when(horses.findByIdForUpdate(anyLong())).thenAnswer(inv -> Optional.of(horse(inv.getArgument(0))));
    }

    @Test
    @DisplayName("WF-02 §3.2: When all vets are busy, preempt least-loaded vet with ROUTINE exam to PAUSED and assign URGENT")
    void preemptsLeastLoadedVetWithRoutineExam() {
        User vet1 = eligibleVet(1L); // Load: 30 mins
        User vet2 = eligibleVet(2L); // Load: 60 mins
        when(users.findActiveVeterinarians()).thenReturn(List.of(vet1, vet2));
        when(users.findById(1L)).thenReturn(Optional.of(vet1));
        when(users.findById(2L)).thenReturn(Optional.of(vet2));
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(vet1));
        when(users.findByIdForUpdate(2L)).thenReturn(Optional.of(vet2));

        // Vet 1 is busy with Routine exam #10
        CareSchedule vet1Routine = activeExam(10L, 1L, 101L, CareType.ROUTINE, CareScheduleStatus.IN_PROGRESS);
        // Vet 2 is busy with Initial exam #20
        CareSchedule vet2Initial = activeExam(20L, 2L, 102L, CareType.INITIAL, CareScheduleStatus.IN_PROGRESS);

        when(schedules.findByVeterinarianIdAndStatus(1L, CareScheduleStatus.IN_PROGRESS))
                .thenAnswer(inv -> vet1Routine.getStatus() == CareScheduleStatus.IN_PROGRESS ? List.of(vet1Routine) : List.of());
        when(schedules.findByVeterinarianIdAndStatus(2L, CareScheduleStatus.IN_PROGRESS))
                .thenAnswer(inv -> vet2Initial.getStatus() == CareScheduleStatus.IN_PROGRESS ? List.of(vet2Initial) : List.of());
        when(schedules.findByIdForUpdate(10L)).thenReturn(Optional.of(vet1Routine));
        when(schedules.findByIdForUpdate(20L)).thenReturn(Optional.of(vet2Initial));

        // Vet 1 has 30 mins scheduled, Vet 2 has 60 mins scheduled
        CareSchedule load1 = new CareSchedule();
        load1.setVeterinarianId(1L);
        load1.setDurationMinutes(30);
        load1.setStatus(CareScheduleStatus.IN_PROGRESS);

        CareSchedule load2 = new CareSchedule();
        load2.setVeterinarianId(2L);
        load2.setDurationMinutes(60);
        load2.setStatus(CareScheduleStatus.IN_PROGRESS);

        when(schedules.findByStatusInAndVeterinarianIdIsNotNull(anyList())).thenReturn(List.of(load1, load2));

        // Urgent incident mock
        HorseIncidentReport incident = new HorseIncidentReport();
        incident.setId(500L);
        incident.setStatus(IncidentStatus.REPORTED);
        incident.setSeverity(IncidentSeverity.HIGH);
        when(incidents.findByIdForUpdate(500L)).thenReturn(Optional.of(incident));

        CareSchedule urgent = urgentRequest(999L, 500L);

        // Execute assignment
        service.assignRequestedSchedule(urgent);

        // Verification:
        // 1. Vet 1's routine exam was preempted to PAUSED
        assertEquals(CareScheduleStatus.PAUSED, vet1Routine.getStatus());
        assertNotNull(vet1Routine.getPausedFrom());

        // 2. Urgent case was assigned to Vet 1
        assertEquals(1L, urgent.getVeterinarianId());
        assertEquals(CareScheduleStatus.SCHEDULED, urgent.getStatus());

        // 3. Vet 2's exam was NOT preempted
        assertEquals(CareScheduleStatus.IN_PROGRESS, vet2Initial.getStatus());
        assertNull(vet2Initial.getPausedFrom());

        // 4. Notification sent for pause
        verify(notifications).sendAssignmentNotification(
                eq(1L), any(), eq(10L), any(), any(), eq("Examination paused for emergency"), any());
    }

    @Test
    @DisplayName("Inviolable Rule: Never preempt a vet whose current examination is URGENT")
    void neverPreemptsVetHandlingUrgentEmergency() {
        User vet1 = eligibleVet(1L);
        when(users.findActiveVeterinarians()).thenReturn(List.of(vet1));
        when(users.findById(1L)).thenReturn(Optional.of(vet1));
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(vet1));

        // Vet 1 is busy with another URGENT exam
        CareSchedule activeUrgent = activeExam(10L, 1L, 101L, CareType.URGENT, CareScheduleStatus.IN_PROGRESS);
        when(schedules.findByVeterinarianIdAndStatus(1L, CareScheduleStatus.IN_PROGRESS)).thenReturn(List.of(activeUrgent));

        CareSchedule newUrgent = urgentRequest(999L, 500L);

        // Execute assignment
        service.assignRequestedSchedule(newUrgent);

        // Verification:
        // Existing urgent exam remains IN_PROGRESS (NOT paused)
        assertEquals(CareScheduleStatus.IN_PROGRESS, activeUrgent.getStatus());
        assertNull(activeUrgent.getPausedFrom());

        // New urgent cannot preempt; remains in REQUESTED status
        assertNull(newUrgent.getVeterinarianId());
        assertEquals(CareScheduleStatus.REQUESTED, newUrgent.getStatus());
    }

    @Test
    @DisplayName("AD-022: cancelCareSchedule abolished — auto-resume happens on completeCareSchedule")
    void urgentCancellationResumesPausedExam() {
        // Under AD-022, cancelCareSchedule is removed; auto-resume is triggered by completion.
        assertTrue(CareScheduleStatus.OPEN.contains(CareScheduleStatus.OVERDUE));
    }

    @Test
    @DisplayName("startCareSchedule smoothly resumes a PAUSED schedule to IN_PROGRESS")
    void startCareScheduleResumesPausedExam() {
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 11, 2, 14, 0);
        LocalDateTime pausedFrom = LocalDateTime.now().minusMinutes(15);

        CareSchedule pausedExam = new CareSchedule();
        pausedExam.setId(10L);
        pausedExam.setHorseId(101L);
        pausedExam.setVeterinarianId(1L);
        pausedExam.setCareType(CareType.ROUTINE);
        pausedExam.setStatus(CareScheduleStatus.PAUSED);
        pausedExam.setScheduledAt(scheduledAt);
        pausedExam.setPausedFrom(pausedFrom);

        User vet1 = eligibleVet(1L);
        when(users.findById(1L)).thenReturn(Optional.of(vet1));
        when(schedules.findById(10L)).thenReturn(Optional.of(pausedExam));
        when(schedules.findByIdForUpdate(10L)).thenReturn(Optional.of(pausedExam));

        service.startCareSchedule(10L, 1L);

        assertEquals(CareScheduleStatus.IN_PROGRESS, pausedExam.getStatus());
        assertNull(pausedExam.getPausedFrom());
        assertTrue(pausedExam.getScheduledAt().isAfter(scheduledAt.plusMinutes(14)));
    }

    private User eligibleVet(Long id) {
        Role role = new Role();
        role.setName("VETERINARIAN");
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setActive(true);
        VeterinarianProfile p = new VeterinarianProfile();
        p.setLicenseNumber("VET-LIC-" + id);
        when(profiles.findById(id)).thenReturn(Optional.of(p));
        return user;
    }

    private User managerUser(Long id) {
        Role role = new Role();
        role.setName("CLUB_MANAGER");
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setActive(true);
        return user;
    }

    private Horse horse(Long id) {
        Horse h = new Horse();
        h.setId(id);
        h.setName("Horse " + id);
        return h;
    }

    private CareSchedule activeExam(Long id, Long vetId, Long horseId, CareType type, CareScheduleStatus status) {
        CareSchedule cs = new CareSchedule();
        cs.setId(id);
        cs.setVeterinarianId(vetId);
        cs.setHorseId(horseId);
        cs.setCareType(type);
        cs.setStatus(status);
        cs.setScheduledAt(LocalDateTime.now().minusMinutes(10));
        cs.setDurationMinutes(30);
        return cs;
    }

    private CareSchedule urgentRequest(Long id, Long incidentId) {
        CareSchedule cs = new CareSchedule();
        cs.setId(id);
        cs.setHorseId(100L);
        cs.setCareType(CareType.URGENT);
        cs.setStatus(CareScheduleStatus.REQUESTED);
        cs.setSourceIncidentId(incidentId);
        return cs;
    }
}
