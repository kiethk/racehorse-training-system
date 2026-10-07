package com.rtms.backend.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.dto.CompleteCareScheduleRequest;
import com.rtms.backend.dto.UrgentAssignmentAlert;
import com.rtms.backend.entity.*;
import com.rtms.backend.enums.*;
import com.rtms.backend.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrgentCareCompletionTest {

    @Mock private CareScheduleRepository careScheduleRepository;
    @Mock private HorseRepository horseRepository;
    @Mock private HealthRecordRepository healthRecordRepository;
    @Mock private HorseHealthMetricRepository metricRepository;
    @Mock private AdmissionApplicationRepository admissionRepository;
    @Mock private StableStallRepository stallRepository;
    @Mock private UserRepository userRepository;
    @Mock private GroomIncidentReportRepository incidentReportRepository;
    @Mock private VeterinarianProfileRepository veterinarianProfileRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private EntityManager entityManager;
    @Mock private NotificationService notificationService;
    @Mock private HeadTrainerWorkloadService headTrainerWorkloadService;

    @InjectMocks
    private CareScheduleService careScheduleService;

    private Horse horse;
    private User vet;
    private CareSchedule urgentSchedule;
    private GroomIncidentReport incident;

    @BeforeEach
    void setUp() {
        horse = new Horse();
        horse.setId(10L);
        horse.setName("Thunder");
        horse.setCurrentStatus(HorseStatus.ELIGIBLE);
        horse.setTrainingStatus(TrainingDecision.ALLOWED);
        horse.setTrainingLocked(false);

        vet = new User();
        vet.setId(20L);
        vet.setFullName("Dr. Smith");
        vet.setEmail("dr_smith@example.com");
        vet.setActive(true);
        Role role = new Role();
        role.setName("VETERINARIAN");
        vet.setRole(role);

        incident = new GroomIncidentReport();
        incident.setId(30L);
        incident.setStatus(IncidentStatus.IN_REVIEW);

        urgentSchedule = new CareSchedule();
        urgentSchedule.setId(100L);
        urgentSchedule.setHorseId(horse.getId());
        urgentSchedule.setVeterinarianId(vet.getId());
        urgentSchedule.setCareType(CareType.URGENT);
        urgentSchedule.setStatus(CareScheduleStatus.SCHEDULED);
        urgentSchedule.setSourceIncidentId(incident.getId());
    }

    @Test
    @DisplayName("createSchedule URGENT locks training and handles blank description fallback")
    void testCreateUrgentSchedule_fallbackDescription() {
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));

        careScheduleService.createSchedule(10L, CareType.URGENT, null, LocalDateTime.now(), 30L);

        assertTrue(horse.isTrainingLocked());
        assertEquals(TrainingDecision.BLOCKED, horse.getTrainingStatus());
        assertEquals("Urgent veterinary care pending", horse.getTrainingLockReason());
        assertFalse(horse.getTrainingLockReason().endsWith("null"));
    }

    @Test
    @DisplayName("startCareSchedule transitions SCHEDULED -> IN_PROGRESS")
    void testStartCareSchedule_success() {
        when(userRepository.findById(vet.getId())).thenReturn(Optional.of(vet));
        when(careScheduleRepository.findById(urgentSchedule.getId())).thenReturn(Optional.of(urgentSchedule));
        when(horseRepository.findByIdForUpdate(horse.getId())).thenReturn(Optional.of(horse));
        when(careScheduleRepository.findByIdForUpdate(urgentSchedule.getId())).thenReturn(Optional.of(urgentSchedule));
        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));

        var response = careScheduleService.startCareSchedule(urgentSchedule.getId(), vet.getId());

        assertEquals(CareScheduleStatus.IN_PROGRESS, response.status());
        assertEquals(CareScheduleStatus.IN_PROGRESS, urgentSchedule.getStatus());
    }

    @Test
    @DisplayName("completeCareSchedule ALLOWED releases horse training lock and completes schedule")
    void testCompleteUrgentSchedule_allowed_releasesLock() {
        urgentSchedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        horse.setTrainingLocked(true);
        horse.setTrainingStatus(TrainingDecision.BLOCKED);
        horse.setTrainingLockReason("Urgent veterinary care pending");

        when(careScheduleRepository.findById(urgentSchedule.getId())).thenReturn(Optional.of(urgentSchedule));
        when(horseRepository.findByIdForUpdate(horse.getId())).thenReturn(Optional.of(horse));
        when(careScheduleRepository.findByIdForUpdate(urgentSchedule.getId())).thenReturn(Optional.of(urgentSchedule));
        when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(i -> {
            HealthRecord hr = i.getArgument(0);
            hr.setId(999L);
            return hr;
        });
        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));
        when(incidentReportRepository.findById(incident.getId())).thenReturn(Optional.of(incident));

        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
        req.setFindings("Normal recovery");
        req.setDiagnosis("Minor scratch healed");
        req.setTrainingDecision(TrainingDecision.ALLOWED);

        var response = careScheduleService.completeCareSchedule(urgentSchedule.getId(), req, vet.getId());

        assertEquals(CareScheduleStatus.COMPLETED, response.status());
        assertFalse(horse.isTrainingLocked());
        assertNull(horse.getTrainingLockReason());
        assertEquals(TrainingDecision.ALLOWED, horse.getTrainingStatus());
        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());
        verify(healthRecordRepository).save(any(HealthRecord.class));
    }

    @Test
    @DisplayName("completeCareSchedule RESTRICTED keeps horse training lock")
    void testCompleteUrgentSchedule_restricted_keepsLock() {
        urgentSchedule.setStatus(CareScheduleStatus.IN_PROGRESS);

        when(careScheduleRepository.findById(urgentSchedule.getId())).thenReturn(Optional.of(urgentSchedule));
        when(horseRepository.findByIdForUpdate(horse.getId())).thenReturn(Optional.of(horse));
        when(careScheduleRepository.findByIdForUpdate(urgentSchedule.getId())).thenReturn(Optional.of(urgentSchedule));
        when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(i -> {
            HealthRecord hr = i.getArgument(0);
            hr.setId(999L);
            return hr;
        });
        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));

        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
        req.setFindings("Lameness in right foot");
        req.setDiagnosis("Tendon strain");
        req.setTrainingDecision(TrainingDecision.RESTRICTED);
        req.setRestrictionDetails("Walk only for 5 days");

        careScheduleService.completeCareSchedule(urgentSchedule.getId(), req, vet.getId());

        assertTrue(horse.isTrainingLocked());
        assertEquals(TrainingDecision.RESTRICTED, horse.getTrainingStatus());
        assertEquals("Walk only for 5 days", horse.getTrainingLockReason());
    }

    @Test
    @DisplayName("completeCareSchedule is idempotent when called again by assigned vet")
    void testCompleteUrgentSchedule_idempotent() {
        urgentSchedule.setStatus(CareScheduleStatus.COMPLETED);

        when(careScheduleRepository.findById(urgentSchedule.getId())).thenReturn(Optional.of(urgentSchedule));
        when(horseRepository.findByIdForUpdate(horse.getId())).thenReturn(Optional.of(horse));
        when(careScheduleRepository.findByIdForUpdate(urgentSchedule.getId())).thenReturn(Optional.of(urgentSchedule));

        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
        req.setFindings("Check");
        req.setDiagnosis("OK");
        req.setTrainingDecision(TrainingDecision.ALLOWED);

        var response = careScheduleService.completeCareSchedule(urgentSchedule.getId(), req, vet.getId());

        assertEquals(CareScheduleStatus.COMPLETED, response.status());
        verify(healthRecordRepository, never()).save(any());
    }
}
