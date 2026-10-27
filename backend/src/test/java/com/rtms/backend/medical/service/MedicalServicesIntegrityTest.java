package com.rtms.backend.medical.service;

import com.rtms.backend.audit.repository.AuditLogRepository;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.dto.CorrectInjuryRecordRequest;
import com.rtms.backend.medical.dto.CreateHealthRecordRequest;
import com.rtms.backend.medical.dto.CreatePrescriptionRequest;
import com.rtms.backend.medical.dto.CreateTreatmentPlanRequest;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.entity.InjuryRecord;
import com.rtms.backend.medical.entity.TreatmentPlan;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.medical.repository.InjuryRecordRepository;
import com.rtms.backend.medical.repository.PrescriptionRepository;
import com.rtms.backend.medical.repository.TreatmentPlanRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import com.rtms.backend.identity.repository.UserRepository;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalServicesIntegrityTest {

    @Mock
    private HealthRecordRepository healthRecordRepository;
    @Mock
    private TreatmentPlanRepository treatmentPlanRepository;
    @Mock
    private PrescriptionRepository prescriptionRepository;
    @Mock
    private InjuryRecordRepository injuryRecordRepository;
    @Mock
    private HorseRepository horseRepository;
    @Mock
    private CareScheduleRepository careScheduleRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private TreatmentPlanService treatmentPlanService;
    private InjuryRecordService injuryRecordService;
    private HealthRecordService healthRecordService;

    @BeforeEach
    void setUp() {
        treatmentPlanService = new TreatmentPlanService(
                healthRecordRepository,
                treatmentPlanRepository,
                prescriptionRepository
        );

        injuryRecordService = new InjuryRecordService(
                injuryRecordRepository,
                horseRepository,
                healthRecordRepository,
                careScheduleRepository,
                auditLogRepository, Mockito.mock(UserRepository.class)
        );

        healthRecordService = new HealthRecordService(
                healthRecordRepository,
                horseRepository,
                careScheduleRepository,
                eventPublisher
        );
    }

    @Test
    @DisplayName("AC-2.1: createTreatmentPlan throws ApiException 404 when health record not found")
    void testCreateTreatmentPlan_NotFound() {
        when(healthRecordRepository.findById(999L)).thenReturn(Optional.empty());

        CreateTreatmentPlanRequest req = new CreateTreatmentPlanRequest();
        req.setTreatmentName("Hydrotherapy");
        req.setStartDate(LocalDate.now());

        ApiException ex = assertThrows(ApiException.class, () ->
                treatmentPlanService.createTreatmentPlan(999L, req)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("HEALTH_RECORD_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    @DisplayName("AC-2.1: createTreatmentPlan throws ApiException 400 when start date is after end date")
    void testCreateTreatmentPlan_InvalidDates() {
        when(healthRecordRepository.findById(1L)).thenReturn(Optional.of(new HealthRecord()));

        CreateTreatmentPlanRequest req = new CreateTreatmentPlanRequest();
        req.setTreatmentName("Stall rest");
        req.setStartDate(LocalDate.now().plusDays(5));
        req.setEndDate(LocalDate.now());

        ApiException ex = assertThrows(ApiException.class, () ->
                treatmentPlanService.createTreatmentPlan(1L, req)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_DATE_RANGE", ex.getErrorCode());
    }

    @Test
    @DisplayName("AC-2.1: createPrescription throws ApiException 404 when treatment plan not found")
    void testCreatePrescription_NotFound() {
        when(treatmentPlanRepository.findById(888L)).thenReturn(Optional.empty());

        CreatePrescriptionRequest req = new CreatePrescriptionRequest();
        req.setMedicationName("Phenylbutazone");

        ApiException ex = assertThrows(ApiException.class, () ->
                treatmentPlanService.createPrescription(888L, req)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("TREATMENT_PLAN_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    @DisplayName("AC-2.2: correctInjuryRecord throws ApiException 409 when record is already SUPERSEDED")
    void testCorrectInjuryRecord_AlreadySuperseded() {
        InjuryRecord superseded = new InjuryRecord();
        superseded.setHorseId(10L);
        superseded.setStatus("SUPERSEDED");
        when(injuryRecordRepository.findById(50L)).thenReturn(Optional.of(superseded));

        CorrectInjuryRecordRequest req = new CorrectInjuryRecordRequest(
                "Correction of already superseded item",
                "FRACTURE",
                "HIGH",
                "Updated details",
                "ACTIVE"
        );

        ApiException ex = assertThrows(ApiException.class, () ->
                injuryRecordService.correctInjuryRecord(10L, 50L, req, 10L)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("INJURY_ALREADY_SUPERSEDED", ex.getErrorCode());
        verify(injuryRecordRepository, never()).save(any(InjuryRecord.class));
    }

    @Test
    @DisplayName("AC-2.3: createHealthRecord throws ApiException 404 when horse not found")
    void testCreateHealthRecord_HorseNotFound() {
        when(horseRepository.findById(404L)).thenReturn(Optional.empty());

        CreateHealthRecordRequest req = new CreateHealthRecordRequest();
        req.setHorseId(404L);
        AuthenticatedUser user = new AuthenticatedUser(5L, "vet@rtms.com", "VETERINARIAN");

        ApiException ex = assertThrows(ApiException.class, () ->
                healthRecordService.createHealthRecord(req, user)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("HORSE_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    @DisplayName("AC-2.3: createHealthRecord throws ApiException 409 when careSchedule belongs to another horse")
    void testCreateHealthRecord_ScheduleMismatch() {
        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("Thunder");
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        CareSchedule otherHorseSchedule = new CareSchedule();
        otherHorseSchedule.setId(100L);
        otherHorseSchedule.setHorseId(99L); // Different horse!
        when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(otherHorseSchedule));

        CreateHealthRecordRequest req = new CreateHealthRecordRequest();
        req.setHorseId(10L);
        req.setCareScheduleId(100L);
        AuthenticatedUser user = new AuthenticatedUser(5L, "vet@rtms.com", "VETERINARIAN");

        ApiException ex = assertThrows(ApiException.class, () ->
                healthRecordService.createHealthRecord(req, user)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("SCHEDULE_HORSE_MISMATCH", ex.getErrorCode());
    }
}
