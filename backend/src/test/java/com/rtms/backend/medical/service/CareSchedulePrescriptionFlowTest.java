package com.rtms.backend.medical.service;

import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.audit.repository.AuditLogRepository;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.identity.repository.VeterinarianProfileRepository;
import com.rtms.backend.incident.repository.HorseIncidentReportRepository;
import com.rtms.backend.medical.dto.CompleteCareScheduleRequest;
import com.rtms.backend.medical.dto.CreateNextScheduleRequest;
import java.time.LocalDateTime;
import com.rtms.backend.medical.dto.PrescriptionItemRequest;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.entity.ClinicalMedication;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.entity.InjuryRecord;
import com.rtms.backend.medical.entity.Prescription;
import com.rtms.backend.medical.entity.TreatmentPlan;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.enums.MedicationRoute;
import com.rtms.backend.medical.event.PrescriptionCreatedEvent;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.ClinicalMedicationRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.medical.repository.HorseHealthMetricRepository;
import com.rtms.backend.medical.repository.InjuryRecordRepository;
import com.rtms.backend.medical.repository.PrescriptionRepository;
import com.rtms.backend.medical.repository.TreatmentPlanRepository;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.service.TrainingDecisionService;
import com.rtms.backend.training.service.TrainingWorkoutService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Track 0 (ROADMAP v2.2): kê đơn từ danh mục chuẩn + resolvedConditionIds + rollback US-2. */
@ExtendWith(MockitoExtension.class)
class CareSchedulePrescriptionFlowTest {

    @Mock private CareScheduleRepository careScheduleRepository;
    @Mock private HorseRepository horseRepository;
    @Mock private HealthRecordRepository healthRecordRepository;
    @Mock private HorseHealthMetricRepository metricRepository;
    @Mock private AdmissionApplicationRepository admissionRepository;
    @Mock private StableStallRepository stallRepository;
    @Mock private UserRepository userRepository;
    @Mock private HorseIncidentReportRepository incidentReportRepository;
    @Mock private VeterinarianProfileRepository veterinarianProfileRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private EntityManager entityManager;
    @Mock private NotificationService notificationService;
    @Mock private TrainingWorkoutService workoutService;
    @Mock private PreventiveVaccinationService preventiveVaccinationService;
    @Mock private InjuryRecordRepository injuryRecordRepository;
    @Mock private ClinicalMedicationRepository clinicalMedicationRepository;
    @Mock private TreatmentPlanRepository treatmentPlanRepository;
    @Mock private PrescriptionRepository prescriptionRepository;

    private CareScheduleService service;
    private Horse horse;
    private CareSchedule schedule;

    @BeforeEach
    void setUp() {
        service = new CareScheduleService(
                careScheduleRepository, horseRepository, healthRecordRepository, metricRepository,
                admissionRepository, stallRepository, userRepository, incidentReportRepository,
                veterinarianProfileRepository, auditLogRepository, eventPublisher, entityManager,
                notificationService,
                new TrainingDecisionService(horseRepository, careScheduleRepository, workoutService),
                preventiveVaccinationService, injuryRecordRepository,
                clinicalMedicationRepository, treatmentPlanRepository, prescriptionRepository);

        horse = new Horse();
        horse.setId(10L);
        horse.setName("Test Horse");
        horse.setCurrentStatus(HorseStatus.ELIGIBLE);
        horse.setTrainingDecision(TrainingDecision.BLOCKED);

        schedule = new CareSchedule();
        schedule.setId(100L);
        schedule.setHorseId(10L);
        schedule.setVeterinarianId(20L);
        schedule.setCareType(CareType.URGENT);
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
    }

    private void stubCompletion() {
        when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));
        when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(i -> {
            HealthRecord hr = i.getArgument(0);
            hr.setId(999L);
            return hr;
        });
        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));
        when(careScheduleRepository.saveAndFlush(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));
    }

    private CompleteCareScheduleRequest baseRequest() {
        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
        req.setFindings("Heat and swelling over the right fore flexor");
        req.setDiagnosis("Superficial digital flexor tendon strain");
        req.setTrainingDecision(TrainingDecision.ALLOWED);
        CreateNextScheduleRequest next = new CreateNextScheduleRequest();
        next.setCareType(CareType.ROUTINE);
        next.setScheduledAt(LocalDateTime.of(2026, 11, 6, 14, 0));
        next.setDescription("Follow-up recheck");
        req.setNextSchedule(next);
        return req;
    }

    private ClinicalMedication medication(Long id, String name, String route, int withdrawal) {
        ClinicalMedication m = new ClinicalMedication();
        m.setId(id);
        m.setName(name);
        m.setTherapeuticClass("NSAID");
        m.setDefaultRoute(route);
        m.setStandardWithdrawalDays(withdrawal);
        m.setActive(true);
        return m;
    }

    private PrescriptionItemRequest rxItem(Long medId, MedicationRoute route, int withdrawalDays) {
        PrescriptionItemRequest item = new PrescriptionItemRequest();
        item.setMedicationId(medId);
        item.setDosage("10ml");
        item.setRoute(route);
        item.setFrequency("BID");
        item.setStartDate(LocalDate.of(2026, 11, 1));
        item.setEndDate(LocalDate.of(2026, 11, 5));
        item.setWithdrawalDays(withdrawalDays);
        return item;
    }

    @Test
    @DisplayName("Withdrawal thấp hơn chuẩn catalog: 400 INVALID_WITHDRAWAL_DAYS, không khóa/dừng ghi gì")
    void withdrawalBelowStandard_isRejectedBeforeAnyStateChange() {
        when(clinicalMedicationRepository.findById(1L)).thenReturn(Optional.of(medication(1L, "Flunixin Meglumine", "INJECTION_IV", 4)));

        CompleteCareScheduleRequest req = baseRequest();
        req.setPrescriptions(List.of(rxItem(1L, MedicationRoute.INJECTION_IV, 2)));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.completeCareSchedule(100L, req, 20L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("regulatory standard"));
        verify(careScheduleRepository, never()).findByIdForUpdate(any());
        verify(healthRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Thuốc không có trong catalog: 400 MEDICATION_NOT_FOUND")
    void unknownMedication_isRejected() {
        when(clinicalMedicationRepository.findById(99L)).thenReturn(Optional.empty());

        CompleteCareScheduleRequest req = baseRequest();
        req.setPrescriptions(List.of(rxItem(99L, MedicationRoute.ORAL, 4)));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.completeCareSchedule(100L, req, 20L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(careScheduleRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("Kê đơn thành công: plan ACTIVE với MIN/MAX dates, rx IV -> VETERINARIAN, event AFTER_COMMIT")
    void prescriptions_persistAtomically_withRoleSplitAndPlanRange() {
        stubCompletion();
        when(clinicalMedicationRepository.findById(1L)).thenReturn(Optional.of(medication(1L, "Flunixin Meglumine", "INJECTION_IV", 4)));
        when(clinicalMedicationRepository.findById(2L)).thenReturn(Optional.of(medication(2L, "Phenylbutazone", "ORAL", 7)));
        when(treatmentPlanRepository.save(any(TreatmentPlan.class))).thenAnswer(i -> {
            TreatmentPlan p = i.getArgument(0);
            p.setId(500L);
            return p;
        });
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(i -> i.getArgument(0));
        when(incidentReportRepository.findByHorseIdAndStatusIn(eq(10L), anyCollection())).thenReturn(List.of());

        CompleteCareScheduleRequest req = baseRequest();
        PrescriptionItemRequest iv = rxItem(1L, MedicationRoute.INJECTION_IV, 4);
        PrescriptionItemRequest oral = rxItem(2L, MedicationRoute.ORAL, 7);
        oral.setStartDate(LocalDate.of(2026, 11, 2));
        oral.setEndDate(LocalDate.of(2026, 11, 9));
        req.setPrescriptions(List.of(iv, oral));

        service.completeCareSchedule(100L, req, 20L);

        ArgumentCaptor<TreatmentPlan> planCap = ArgumentCaptor.forClass(TreatmentPlan.class);
        verify(treatmentPlanRepository).save(planCap.capture());
        TreatmentPlan plan = planCap.getValue();
        assertEquals("ACTIVE", plan.getStatus());
        assertEquals(LocalDate.of(2026, 11, 1), plan.getStartDate());
        assertEquals(LocalDate.of(2026, 11, 9), plan.getEndDate());

        ArgumentCaptor<Prescription> rxCap = ArgumentCaptor.forClass(Prescription.class);
        verify(prescriptionRepository, times(2)).save(rxCap.capture());
        assertEquals("VETERINARIAN", rxCap.getAllValues().get(0).getAdministeredByRole());
        assertEquals("GROOM", rxCap.getAllValues().get(1).getAdministeredByRole());
        assertEquals("Flunixin Meglumine", rxCap.getAllValues().get(0).getMedicationName());

        verify(eventPublisher).publishEvent(any(PrescriptionCreatedEvent.class));
    }

    @Test
    @DisplayName("US-2 (C1): save HealthRecord ném exception => không có plan/rx/event, ca vẫn IN_PROGRESS")
    void exceptionAfterRecordSave_leavesNoPartialWrites() {
        when(clinicalMedicationRepository.findById(1L)).thenReturn(Optional.of(medication(1L, "Flunixin Meglumine", "INJECTION_IV", 4)));
        when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));
        when(healthRecordRepository.save(any(HealthRecord.class)))
                .thenThrow(new RuntimeException("simulated FK violation on prescriptions"));

        CompleteCareScheduleRequest req = baseRequest();
        req.setPrescriptions(List.of(rxItem(1L, MedicationRoute.INJECTION_IV, 4)));

        assertThrows(RuntimeException.class, () -> service.completeCareSchedule(100L, req, 20L));

        assertEquals(CareScheduleStatus.IN_PROGRESS, schedule.getStatus(),
                "no COMPLETED status may be flushed before the exception rolls the tx back");
        verify(treatmentPlanRepository, never()).save(any());
        verify(prescriptionRepository, never()).save(any());
        verify(careScheduleRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("C7: ALLOWED không còn auto-recovery — chỉ condition Vet tick được RECOVERED + plan cũ COMPLETED")
    void allowed_onlyArchivesVetTickedConditions() {
        stubCompletion();
        when(incidentReportRepository.findByHorseIdAndStatusIn(eq(10L), anyCollection())).thenReturn(List.of());

        InjuryRecord ticked = new InjuryRecord();
        ticked.setId(77L);
        ticked.setHorseId(10L);
        ticked.setHealthRecordId(300L);
        ticked.setStatus("ACTIVE");
        when(injuryRecordRepository.findById(77L)).thenReturn(Optional.of(ticked));
        TreatmentPlan oldPlan = new TreatmentPlan();
        oldPlan.setId(88L);
        oldPlan.setStatus("ACTIVE");
        when(treatmentPlanRepository.findByHealthRecordIdAndStatusIgnoreCase(300L, "ACTIVE"))
                .thenReturn(List.of(oldPlan));

        CompleteCareScheduleRequest req = baseRequest();
        req.setResolvedConditionIds(List.of(77L));

        service.completeCareSchedule(100L, req, 20L);

        assertEquals("RECOVERED", ticked.getStatus());
        assertNotNull(ticked.getRecoveredAt());
        assertEquals("COMPLETED", oldPlan.getStatus());
        verify(injuryRecordRepository).save(ticked);
        verify(treatmentPlanRepository).save(oldPlan);
        // Không quét hàng loạt: repo không được truy vấn mọi ACTIVE của ngựa.
        verify(injuryRecordRepository, never())
                .findByHorseIdAndStatusIgnoreCaseOrderByDiagnosedAtDesc(anyLong(), anyString());
    }

    @Test
    @DisplayName("D4: severity ngoài MILD/MODERATE/SEVERE/CRITICAL bị chặn trước khi khóa ca")
    void injuryMarkerWithLegacySeverity_isRejected() {
        CompleteCareScheduleRequest req = baseRequest();
        com.rtms.backend.medical.dto.InjuryMarkerRequest marker = new com.rtms.backend.medical.dto.InjuryMarkerRequest();
        marker.setInjuryType("TENDON");
        marker.setBodyRegionId(1L);
        marker.setSeverity("LOW");
        marker.setDescription("old vocabulary");
        req.setInjuryMarkers(List.of(marker));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.completeCareSchedule(100L, req, 20L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(careScheduleRepository, never()).findByIdForUpdate(any());
    }
}
