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
import com.rtms.backend.medical.dto.InjuryMarkerRequest;
import com.rtms.backend.medical.dto.PrescriptionItemRequest;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.entity.InjuryRecord;
import com.rtms.backend.medical.entity.Prescription;
import com.rtms.backend.medical.entity.TreatmentPlan;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareSchedulePurpose;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.enums.MedicationRoute;
import com.rtms.backend.medical.event.PrescriptionCreatedEvent;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.medical.repository.HorseHealthMetricRepository;
import com.rtms.backend.medical.repository.InjuryRecordRepository;
import com.rtms.backend.medical.repository.PrescriptionRepository;
import com.rtms.backend.medical.repository.TreatmentPlanRepository;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.service.TrainingDecisionService;
import com.rtms.backend.training.service.TrainingGroupService;
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
    @Mock private TrainingGroupService groupSync;
    @Mock private PreventiveVaccinationService preventiveVaccinationService;
    @Mock private InjuryRecordRepository injuryRecordRepository;
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
                new TrainingDecisionService(horseRepository, careScheduleRepository, groupSync),
                preventiveVaccinationService, injuryRecordRepository,
                treatmentPlanRepository, prescriptionRepository);

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
        next.setScheduledAt(LocalDateTime.now().plusDays(7).withHour(14).withMinute(0).withSecond(0).withNano(0));
        next.setDescription("Follow-up recheck");
        req.setNextSchedule(next);
        return req;
    }

    private PrescriptionItemRequest rxItem(String medicationName, MedicationRoute route) {
        PrescriptionItemRequest item = new PrescriptionItemRequest();
        item.setMedicationName(medicationName);
        item.setDosage("10ml");
        item.setRoute(route);
        item.setFrequency("BID");
        item.setStartDate(LocalDate.of(2026, 11, 1));
        item.setEndDate(LocalDate.of(2026, 11, 5));
        return item;
    }

    @Test
    @DisplayName("Free-text medication name is saved trimmed")
    void freeTextMedicationName_isSavedTrimmed() {
        stubCompletion();
        when(treatmentPlanRepository.save(any(TreatmentPlan.class))).thenAnswer(i -> {
            TreatmentPlan p = i.getArgument(0);
            p.setId(500L);
            return p;
        });
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(i -> i.getArgument(0));
        when(incidentReportRepository.findByHorseIdAndStatusIn(eq(10L), anyCollection())).thenReturn(List.of());
        CompleteCareScheduleRequest req = baseRequest();
        req.setPrescriptions(List.of(rxItem("  Free text medicine  ", MedicationRoute.ORAL)));

        service.completeCareSchedule(100L, req, 20L);

        ArgumentCaptor<Prescription> rxCap = ArgumentCaptor.forClass(Prescription.class);
        verify(prescriptionRepository).save(rxCap.capture());
        assertEquals("Free text medicine", rxCap.getValue().getMedicationName());
    }

    @Test
    @DisplayName("Medication names differing only by case are duplicates")
    void duplicateMedicationNamesIgnoringCase_areRejected() {
        CompleteCareScheduleRequest req = baseRequest();
        req.setPrescriptions(List.of(
                rxItem("  Medicine A ", MedicationRoute.ORAL),
                rxItem("medicine a", MedicationRoute.ORAL)));

        ApiException ex = assertThrows(ApiException.class, () -> service.completeCareSchedule(100L, req, 20L));
        assertEquals("DUPLICATE_MEDICATION", ex.getErrorCode());
        assertEquals("Medication medicine a is already prescribed in this examination", ex.getMessage());
        verify(careScheduleRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("Injection route is rejected with INJECTION_NOT_ALLOWED before state changes")
    void injectionRoute_isRejected() {
        CompleteCareScheduleRequest req = baseRequest();
        req.setPrescriptions(List.of(rxItem("Flunixin Meglumine", MedicationRoute.INJECTION_IV)));

        ApiException ex = assertThrows(ApiException.class, () -> service.completeCareSchedule(100L, req, 20L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INJECTION_NOT_ALLOWED", ex.getErrorCode());
        assertEquals("Treatment plans cannot contain injections", ex.getMessage());
        verify(careScheduleRepository, never()).findByIdForUpdate(any());
        verify(treatmentPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("Kê đơn thành công: plan ACTIVE với MIN/MAX dates, oral rx -> GROOM, event AFTER_COMMIT")
    void prescriptions_persistAtomically_withRoleSplitAndPlanRange() {
        stubCompletion();
        when(treatmentPlanRepository.save(any(TreatmentPlan.class))).thenAnswer(i -> {
            TreatmentPlan p = i.getArgument(0);
            p.setId(500L);
            return p;
        });
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(i -> i.getArgument(0));
        when(incidentReportRepository.findByHorseIdAndStatusIn(eq(10L), anyCollection())).thenReturn(List.of());

        CompleteCareScheduleRequest req = baseRequest();
        PrescriptionItemRequest iv = rxItem("Flunixin Meglumine", MedicationRoute.ORAL);
        PrescriptionItemRequest oral = rxItem("Phenylbutazone", MedicationRoute.ORAL);
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
        assertEquals("GROOM", rxCap.getAllValues().get(0).getAdministeredByRole());
        assertEquals("GROOM", rxCap.getAllValues().get(1).getAdministeredByRole());
        assertEquals("Flunixin Meglumine", rxCap.getAllValues().get(0).getMedicationName());

        verify(eventPublisher).publishEvent(any(PrescriptionCreatedEvent.class));
    }

    @Test
    @DisplayName("US-2 (C1): save HealthRecord ném exception => không có plan/rx/event, ca vẫn IN_PROGRESS")
    void exceptionAfterRecordSave_leavesNoPartialWrites() {
        when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));
        when(healthRecordRepository.save(any(HealthRecord.class)))
                .thenThrow(new RuntimeException("simulated FK violation on prescriptions"));

        CompleteCareScheduleRequest req = baseRequest();
        req.setPrescriptions(List.of(rxItem("Flunixin Meglumine", MedicationRoute.ORAL)));

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
    @DisplayName("Coupling marker => BLOCKED: ca ROUTINE có marker mà chọn ALLOWED bị chặn 400")
    void injuryMarkersWithAllowedDecisionOnRoutineExam_isRejected() {
        schedule.setCareType(CareType.ROUTINE);
        schedule.setPurpose(CareSchedulePurpose.FOLLOW_UP);
        schedule.setSourceIncidentId(5L);
        when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));
        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));

        CompleteCareScheduleRequest req = baseRequest();
        InjuryMarkerRequest marker = new InjuryMarkerRequest();
        marker.setBodyRegionId(1L);
        marker.setDescription("Swelling over the left forelimb");
        req.setInjuryMarkers(List.of(marker));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.completeCareSchedule(100L, req, 20L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("MARKERS_BLOCK_TRAINING", ex.getErrorCode());
    }

    @Test
    @DisplayName("3D marker chỉ gửi vị trí (bodyRegionId, zoneId) + ghi chú (description) vẫn lưu thành công")
    void injuryMarkerWithPositionAndNoteOnly_persistsNoteDirectly() {
        stubCompletion();
        when(incidentReportRepository.findByHorseIdAndStatusIn(eq(10L), anyCollection())).thenReturn(List.of());
        com.rtms.backend.medical.entity.HorseBodyRegion region = new com.rtms.backend.medical.entity.HorseBodyRegion();
        region.setId(5L);
        region.setCode("foreleg_left");
        region.setDisplayName("Left forelimb");
        when(entityManager.find(com.rtms.backend.medical.entity.HorseBodyRegion.class, 5L)).thenReturn(region);

        CompleteCareScheduleRequest req = baseRequest();
        com.rtms.backend.medical.dto.InjuryMarkerRequest marker = new com.rtms.backend.medical.dto.InjuryMarkerRequest();
        marker.setBodyRegionId(5L);
        marker.setZoneId("foreleg_left");
        marker.setDescription("Swelling and heat over left superficial digital flexor tendon");
        req.setInjuryMarkers(List.of(marker));

        service.completeCareSchedule(100L, req, 20L);

        ArgumentCaptor<InjuryRecord> captor = ArgumentCaptor.forClass(InjuryRecord.class);
        verify(injuryRecordRepository).save(captor.capture());
        InjuryRecord saved = captor.getValue();
        assertEquals(5L, saved.getBodyRegionId());
        assertEquals("foreleg_left", saved.getZoneId());
        assertEquals("Swelling and heat over left superficial digital flexor tendon", saved.getDescription());
        assertEquals("Left forelimb", saved.getInjuryType());
        assertEquals("MODERATE", saved.getSeverity());
    }
}
