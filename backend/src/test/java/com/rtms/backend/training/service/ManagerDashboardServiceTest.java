package com.rtms.backend.training.service;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.entity.CandidateHorseProfile;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.admission.repository.CandidateHorseProfileRepository;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.incident.entity.HorseIncidentReport;
import com.rtms.backend.incident.enums.IncidentSeverity;
import com.rtms.backend.incident.enums.IncidentStatus;
import com.rtms.backend.incident.repository.HorseIncidentReportRepository;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.enums.AreaType;
import com.rtms.backend.stable.enums.StallStatus;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.dto.ManagerDashboardSummaryResponse;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.enums.TrainingPlanStatus;
import com.rtms.backend.training.repository.HorseTrainingPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagerDashboardServiceTest {

    @Mock
    private StableStallRepository stableStallRepository;

    @Mock
    private AreaRepository areaRepository;

    @Mock
    private HorseRepository horseRepository;

    @Mock
    private HorseTrainingPlanRepository horseTrainingPlanRepository;

    @Mock
    private AdmissionApplicationRepository admissionApplicationRepository;

    @Mock
    private CandidateHorseProfileRepository candidateHorseProfileRepository;

    @Mock
    private HorseIncidentReportRepository horseIncidentReportRepository;

    @Mock
    private UserRepository userRepository;

    private ManagerDashboardService service;

    @BeforeEach
    void setUp() {
        service = new ManagerDashboardService(
                stableStallRepository,
                areaRepository,
                horseRepository,
                horseTrainingPlanRepository,
                admissionApplicationRepository,
                candidateHorseProfileRepository,
                horseIncidentReportRepository,
                userRepository
        );
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    record TestAreaStallCount(Long areaId, StallStatus status, Long count)
            implements StableStallRepository.AreaStallCountProjection {
        @Override public Long getAreaId() { return areaId; }
        @Override public StallStatus getStatus() { return status; }
        @Override public Long getCount() { return count; }
    }

    @Test
    @DisplayName("getSummary aggregates stall, fleet, admission, and incident metrics accurately")
    void getSummary_Success() {
        // 1. Stalls & Areas
        Area barnA = new Area();
        barnA.setCode("A");
        barnA.setName("Barn Alpha");
        barnA.setType(AreaType.REGULAR);
        setField(barnA, "id", 1L);

        Area quarantineQ = new Area();
        quarantineQ.setCode("Q");
        quarantineQ.setName("Quarantine Zone");
        quarantineQ.setType(AreaType.QUARANTINE);
        setField(quarantineQ, "id", 2L);

        when(areaRepository.findAll()).thenReturn(List.of(barnA, quarantineQ));

        when(stableStallRepository.countStallsByAreaAndStatus()).thenReturn(List.of(
                new TestAreaStallCount(1L, StallStatus.OCCUPIED, 8L),
                new TestAreaStallCount(1L, StallStatus.AVAILABLE, 2L),
                new TestAreaStallCount(2L, StallStatus.OCCUPIED, 3L),
                new TestAreaStallCount(2L, StallStatus.AVAILABLE, 2L)
        ));

        // 2. Fleet
        when(horseRepository.count()).thenReturn(20L);
        when(horseRepository.countByCurrentStatus(HorseStatus.ELIGIBLE)).thenReturn(15L);
        when(horseTrainingPlanRepository.countByStatus(TrainingPlanStatus.ACTIVE)).thenReturn(12L);
        when(horseRepository.countByTrainingDecision(TrainingDecision.BLOCKED)).thenReturn(2L);
        when(horseRepository.countByCurrentStatus(HorseStatus.INACTIVE)).thenReturn(3L);

        // 3. Admission Pipeline
        when(admissionApplicationRepository.countByStatus(AdmissionStatus.MANAGER_REVIEW)).thenReturn(1L);
        when(admissionApplicationRepository.countByStatus(AdmissionStatus.WAITING_FOR_ARRIVAL)).thenReturn(2L);
        when(admissionApplicationRepository.countByStatus(AdmissionStatus.GROOM_REVIEW)).thenReturn(3L);
        when(admissionApplicationRepository.countByStatus(AdmissionStatus.VET_REVIEW)).thenReturn(1L);
        when(admissionApplicationRepository.countByStatus(AdmissionStatus.TRAINER_REVIEW)).thenReturn(1L);

        AdmissionApplication app1 = new AdmissionApplication();
        setField(app1, "id", 101L);
        setField(app1, "ownerId", 201L);
        app1.setStatus(AdmissionStatus.MANAGER_REVIEW);
        setField(app1, "submittedAt", LocalDateTime.now().minusDays(1));

        when(admissionApplicationRepository.findByStatusOrderBySubmittedAtDesc(AdmissionStatus.MANAGER_REVIEW))
                .thenReturn(List.of(app1));
        when(admissionApplicationRepository.findByStatusAndArrivalDeadlineAtBetweenOrderByArrivalDeadlineAtAsc(
                eq(AdmissionStatus.WAITING_FOR_ARRIVAL), any(), any()))
                .thenReturn(List.of());

        CandidateHorseProfile candidate = new CandidateHorseProfile();
        candidate.setAdmissionId(101L);
        candidate.setName("Thunderstorm");
        candidate.setBreed("Thoroughbred");
        when(candidateHorseProfileRepository.findByAdmissionIdIn(anyList())).thenReturn(List.of(candidate));

        User owner = new User();
        setField(owner, "id", 201L);
        owner.setFullName("John Doe");
        when(userRepository.findAllById(anySet())).thenReturn(List.of(owner));

        // 4. Urgent Incidents
        when(horseIncidentReportRepository.countByStatusIn(anyList())).thenReturn(4L);
        when(horseIncidentReportRepository.countByStatusInAndSeverityIn(anyList(), anyList())).thenReturn(1L);

        HorseIncidentReport incident = new HorseIncidentReport();
        setField(incident, "id", 301L);
        incident.setHorseId(401L);
        incident.setReportedById(501L);
        incident.setTitle("Colic symptoms observed");
        incident.setDescription("Horse showing signs of abdominal discomfort");
        incident.setSeverity(IncidentSeverity.HIGH);
        incident.setStatus(IncidentStatus.REPORTED);
        incident.setReportedAt(LocalDateTime.now().minusHours(2));

        when(horseIncidentReportRepository.findUrgentIncidents(anyList(), anyList()))
                .thenReturn(List.of(incident));

        Horse horse = new Horse();
        setField(horse, "id", 401L);
        horse.setName("Pegasus");
        when(horseRepository.findAllById(Set.of(401L))).thenReturn(List.of(horse));

        User reporter = new User();
        setField(reporter, "id", 501L);
        reporter.setFullName("Dr. Emily Vet");
        when(userRepository.findAllById(Set.of(501L))).thenReturn(List.of(reporter));

        // Execute
        ManagerDashboardSummaryResponse response = service.getSummary();

        // Assertions
        assertNotNull(response);

        // Stall Summary
        assertEquals(15L, response.stallSummary().totalStalls());
        assertEquals(11L, response.stallSummary().occupiedStalls());
        assertEquals(4L, response.stallSummary().availableStalls());
        assertEquals(73.3, response.stallSummary().occupancyRate());

        // Barn Occupancies (Quarantine first)
        assertEquals(2, response.barnOccupancies().size());
        assertEquals("QUARANTINE", response.barnOccupancies().get(0).areaType());
        assertEquals("REGULAR", response.barnOccupancies().get(1).areaType());

        // Fleet Summary
        assertEquals(20L, response.fleetSummary().totalHorses());
        assertEquals(15L, response.fleetSummary().activeEligible());
        assertEquals(12L, response.fleetSummary().inTraining());
        assertEquals(2L, response.fleetSummary().medicalBlocked());
        assertEquals(3L, response.fleetSummary().inactive());

        // Admission Summary & Action items
        assertEquals(8L, response.admissionSummary().totalPending());
        assertEquals(1L, response.admissionSummary().managerReviewCount());
        assertEquals(1, response.pendingAdmissions().size());
        assertEquals("Thunderstorm", response.pendingAdmissions().get(0).candidateHorseName());
        assertEquals("John Doe", response.pendingAdmissions().get(0).ownerName());
        assertEquals("MANAGER_REVIEW", response.pendingAdmissions().get(0).status());

        // Urgent Incidents
        assertEquals(4L, response.incidentSummary().openIncidents());
        assertEquals(1L, response.incidentSummary().criticalUrgentIncidents());
        assertEquals(1, response.urgentIncidents().size());
        assertEquals("Pegasus", response.urgentIncidents().get(0).horseName());
        assertEquals("Dr. Emily Vet", response.urgentIncidents().get(0).reporterName());
        assertEquals("HIGH", response.urgentIncidents().get(0).severity());
    }
}
