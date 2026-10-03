package com.rtms.backend.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.dto.*;
import com.rtms.backend.entity.*;
import com.rtms.backend.enums.*;
import com.rtms.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CareScheduleServiceTest {

    @Mock CareScheduleRepository careScheduleRepository;
    @Mock VetOfferRepository vetOfferRepository;
    @Mock HorseRepository horseRepository;
    @Mock HealthRecordRepository healthRecordRepository;
    @Mock HorseHealthMetricRepository metricRepository;
    @Mock AdmissionApplicationRepository admissionRepository;
    @Mock StableStallRepository stallRepository;
    @Mock UserRepository userRepository;
    @Mock jakarta.persistence.EntityManager entityManager;

    CareScheduleService service;

    Horse horse;
    AdmissionApplication admission;
    CareSchedule schedule;
    VetOffer offer;
    StableStall stall;

    @BeforeEach
    void setUp() {
        service = new CareScheduleService(
                careScheduleRepository, vetOfferRepository, horseRepository,
                healthRecordRepository, metricRepository, admissionRepository,
                stallRepository, userRepository, entityManager);

        horse = new Horse();
        horse.setId(10L);
        horse.setCurrentStatus(HorseStatus.CANDIDATE);
        horse.setCurrentStallId(99L);
        horse.setTrainingStatus(TrainingStatus.BLOCKED);

        admission = new AdmissionApplication();
        admission.setId(1L);
        admission.setHorseId(10L);
        admission.setQuarantineStallId(99L);
        admission.setStatus(AdmissionStatus.VET_REVIEW);

        schedule = new CareSchedule();
        schedule.setId(100L);
        schedule.setAdmissionId(1L);
        schedule.setHorseId(10L);
        schedule.setCareType(CareType.INITIAL);
        schedule.setStatus(CareScheduleStatus.REQUESTED);
        schedule.setDurationMinutes(30);

        offer = new VetOffer();
        offer.setId(200L);
        offer.setCareScheduleId(100L);
        offer.setVeterinarianId(5L);
        offer.setStatus(VetOfferStatus.PENDING);
        offer.setOfferedAt(LocalDateTime.now().minusMinutes(5));
        offer.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        offer.setProposedScheduledAt(LocalDateTime.now().plusHours(1));

        lenient().when(admissionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        lenient().when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        User vet = new User();
        vet.setId(5L);
        lenient().when(userRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.of(vet));

        stall = new StableStall();
        stall.setId(99L);
        stall.setStallCode("Q-01");
        stall.setStatus(StallStatus.OCCUPIED);
    }

    @Test
    @DisplayName("A01: createInitialSchedule creates REQUESTED schedule and locks horse training")
    void createInitialSchedule_createsRequestedScheduleAndLocksHorseTraining() {
        when(careScheduleRepository.existsByHorseIdAndCareTypeAndStatusIn(eq(10L), eq(CareType.INITIAL), any()))
                .thenReturn(false);
        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> {
            CareSchedule cs = i.getArgument(0);
            cs.setId(100L);
            return cs;
        });
        lenient().when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        CareScheduleResponse response = service.createInitialSchedule(1L, 10L);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(CareScheduleStatus.REQUESTED, response.status());
        assertEquals(CareType.INITIAL, response.careType());
        assertEquals(30, response.durationMinutes());
        assertTrue(horse.isTrainingLocked());
        assertEquals(TrainingStatus.BLOCKED, horse.getTrainingStatus());
        verify(careScheduleRepository).save(any(CareSchedule.class));
        verify(horseRepository).save(horse);
    }

    @Test
    @DisplayName("A02: createInitialSchedule returns existing when active schedule exists (idempotency)")
    void createInitialSchedule_whenActiveExists_returnsExisting() {
        when(careScheduleRepository.existsByHorseIdAndCareTypeAndStatusIn(eq(10L), eq(CareType.INITIAL), any()))
                .thenReturn(true);
        when(careScheduleRepository.findFirstByAdmissionIdAndCareTypeOrderByCreatedAtDesc(1L, CareType.INITIAL))
                .thenReturn(Optional.of(schedule));

        CareScheduleResponse response = service.createInitialSchedule(1L, 10L);

        assertEquals(100L, response.id());
        verify(careScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("A05-A07: acceptOffer transitions offer to ACCEPTED and schedule to SCHEDULED")
    void acceptOffer_success_setsOfferAcceptedAndScheduleScheduled() {
        schedule.setStatus(CareScheduleStatus.AWAITING_VET_CONFIRMATION);
        when(vetOfferRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(offer));
        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));
        lenient().when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));
        lenient().when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        VetOfferResponse response = service.acceptOffer(200L, 5L);

        assertEquals(VetOfferStatus.ACCEPTED, offer.getStatus());
        assertNotNull(offer.getRespondedAt());
        assertEquals(CareScheduleStatus.SCHEDULED, schedule.getStatus());
        assertEquals(5L, schedule.getVeterinarianId());
        assertEquals(offer.getProposedScheduledAt(), schedule.getScheduledAt());
        verify(vetOfferRepository).save(offer);
        verify(careScheduleRepository).save(schedule);
    }

    @Test
    @DisplayName("A08: acceptOffer by unauthorized vet throws FORBIDDEN")
    void acceptOffer_wrongVet_throwsForbidden() {
        when(vetOfferRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(offer));

        ApiException ex = assertThrows(ApiException.class, () -> service.acceptOffer(200L, 999L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    @DisplayName("A09: acceptOffer on expired offer marks EXPIRED and throws CONFLICT")
    void acceptOffer_expiredOffer_throwsConflictAndMarksExpired() {
        offer.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(vetOfferRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(offer));

        ApiException ex = assertThrows(ApiException.class, () -> service.acceptOffer(200L, 5L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("OFFER_EXPIRED", ex.getErrorCode());
        assertEquals(VetOfferStatus.EXPIRED, offer.getStatus());
        verify(vetOfferRepository).save(offer);
    }

    @Test
    @DisplayName("A10: acceptOffer on already responded offer throws CONFLICT")
    void acceptOffer_alreadyAccepted_throwsConflict() {
        offer.setStatus(VetOfferStatus.ACCEPTED);
        when(vetOfferRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(offer));

        ApiException ex = assertThrows(ApiException.class, () -> service.acceptOffer(200L, 5L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("INVALID_OFFER_STATUS", ex.getErrorCode());
    }

    @Test
    @DisplayName("A11-A12: BR-CARE-13 Preemption: URGENT offer acceptance releases conflicting INITIAL schedule")
    void acceptOffer_urgentPreemptsConflictingInitialSchedule() {
        CareSchedule urgentSchedule = new CareSchedule();
        urgentSchedule.setId(300L);
        urgentSchedule.setHorseId(10L);
        urgentSchedule.setCareType(CareType.URGENT);
        urgentSchedule.setStatus(CareScheduleStatus.AWAITING_VET_CONFIRMATION);
        urgentSchedule.setDurationMinutes(30);

        LocalDateTime slotTime = LocalDateTime.now().plusHours(1);
        VetOffer urgentOffer = new VetOffer();
        urgentOffer.setId(400L);
        urgentOffer.setCareScheduleId(300L);
        urgentOffer.setVeterinarianId(5L);
        urgentOffer.setStatus(VetOfferStatus.PENDING);
        urgentOffer.setProposedScheduledAt(slotTime);
        urgentOffer.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        CareSchedule conflictingInitial = new CareSchedule();
        conflictingInitial.setId(500L);
        conflictingInitial.setCareType(CareType.INITIAL);
        conflictingInitial.setStatus(CareScheduleStatus.SCHEDULED);
        conflictingInitial.setVeterinarianId(5L);
        conflictingInitial.setScheduledAt(slotTime);
        conflictingInitial.setDurationMinutes(30);

        VetOffer initialOffer = new VetOffer();
        initialOffer.setId(600L);
        initialOffer.setCareScheduleId(500L);
        initialOffer.setVeterinarianId(5L);
        initialOffer.setStatus(VetOfferStatus.ACCEPTED);

        when(vetOfferRepository.findByIdForUpdate(400L)).thenReturn(Optional.of(urgentOffer));
        when(careScheduleRepository.findByIdForUpdate(300L)).thenReturn(Optional.of(urgentSchedule));
        lenient().when(careScheduleRepository.findById(300L)).thenReturn(Optional.of(urgentSchedule));
        when(careScheduleRepository.findScheduledForVet(5L, CareScheduleStatus.SCHEDULED))
                .thenReturn(List.of(conflictingInitial));
        when(vetOfferRepository.findFirstByCareScheduleIdAndStatus(500L, VetOfferStatus.ACCEPTED))
                .thenReturn(Optional.of(initialOffer));

        service.acceptOffer(400L, 5L);

        // Preempted INITIAL schedule should revert to REQUESTED
        assertEquals(CareScheduleStatus.REQUESTED, conflictingInitial.getStatus());
        assertNull(conflictingInitial.getVeterinarianId());
        assertNull(conflictingInitial.getScheduledAt());
        assertEquals(VetOfferStatus.RELEASED, initialOffer.getStatus());

        // Urgent schedule is SCHEDULED
        assertEquals(CareScheduleStatus.SCHEDULED, urgentSchedule.getStatus());
        assertEquals(5L, urgentSchedule.getVeterinarianId());
        assertEquals(VetOfferStatus.ACCEPTED, urgentOffer.getStatus());
    }

    @Test
    @DisplayName("A13: declineOffer marks offer DECLINED and reverts schedule to REQUESTED if no pending remain")
    void declineOffer_declinesAndRevertsSchedule() {
        when(vetOfferRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(offer));
        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));
        lenient().when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));
        when(vetOfferRepository.findFirstByCareScheduleIdAndStatus(100L, VetOfferStatus.PENDING))
                .thenReturn(Optional.empty());

        VetOfferResponse response = service.declineOffer(200L, 5L);

        assertEquals(VetOfferStatus.DECLINED, offer.getStatus());
        assertEquals(CareScheduleStatus.REQUESTED, schedule.getStatus());
    }

    @Test
    @DisplayName("A14: startCareSchedule sets status to IN_PROGRESS for assigned vet")
    void startCareSchedule_success() {
        schedule.setStatus(CareScheduleStatus.SCHEDULED);
        schedule.setVeterinarianId(5L);
        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));
        lenient().when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));
        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));

        CareScheduleResponse response = service.startCareSchedule(100L, 5L);

        assertEquals(CareScheduleStatus.IN_PROGRESS, response.status());
    }

    @Test
    @DisplayName("A15: startCareSchedule by unauthorized vet throws FORBIDDEN")
    void startCareSchedule_wrongVet_throwsForbidden() {
        schedule.setStatus(CareScheduleStatus.SCHEDULED);
        schedule.setVeterinarianId(5L);
        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));
        lenient().when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));

        ApiException ex = assertThrows(ApiException.class, () -> service.startCareSchedule(100L, 999L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    @DisplayName("A16: startCareSchedule when not SCHEDULED throws CONFLICT")
    void startCareSchedule_notScheduled_throwsConflict() {
        schedule.setStatus(CareScheduleStatus.REQUESTED);
        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));
        lenient().when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));

        ApiException ex = assertThrows(ApiException.class, () -> service.startCareSchedule(100L, 5L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    @DisplayName("A17: completeCareSchedule ALLOWED transitions Admission to TRAINER_REVIEW and retains administrative training lock")
    void completeCareSchedule_allowed_success() {
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        schedule.setVeterinarianId(5L);

        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));

        lenient().when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        when(careScheduleRepository.existsByHorseIdAndCareTypeAndStatusIn(eq(10L), eq(CareType.URGENT), any()))
                .thenReturn(false);
        when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(i -> {
            HealthRecord hr = i.getArgument(0);
            hr.setId(50L);
            return hr;
        });
        when(admissionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));

        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
        req.setFindings("Clear lungs, good sound gait");
        req.setDiagnosis("Healthy candidate");
        req.setTrainingDecision(TrainingDecision.ALLOWED);
        req.setFollowUpDate(java.time.LocalDate.now().plusDays(7));

        HorseHealthMetricRequest metric = new HorseHealthMetricRequest();
        metric.setTemperature(java.math.BigDecimal.valueOf(38.0));
        metric.setHeartRate(java.math.BigDecimal.valueOf(40));
        metric.setWeight(java.math.BigDecimal.valueOf(460.0));
        req.setMetrics(List.of(metric));

        CareScheduleResponse response = service.completeCareSchedule(100L, req, 5L);

        assertEquals(CareScheduleStatus.COMPLETED, response.status());
        assertNotNull(response.completedAt());
        assertEquals(TrainingStatus.BLOCKED, horse.getTrainingStatus());
        assertTrue(horse.isTrainingLocked());
        assertEquals("Admission pending trainer and manager review", horse.getTrainingLockReason());
        assertEquals(AdmissionStatus.TRAINER_REVIEW, admission.getStatus());
        assertEquals(VetDecision.APPROVED, admission.getVetDecision());

        ArgumentCaptor<HealthRecord> clinical = ArgumentCaptor.forClass(HealthRecord.class);
        verify(healthRecordRepository).save(clinical.capture());
        assertEquals(req.getFollowUpDate(), clinical.getValue().getFollowUpDate());
        assertEquals(TrainingDecision.ALLOWED, clinical.getValue().getTrainingDecision());
        verify(metricRepository).save(any(HorseHealthMetric.class));
        verify(horseRepository).save(horse);
        verify(admissionRepository).save(admission);
    }

    @Test
    @DisplayName("A18: completeCareSchedule RESTRICTED requires restrictionDetails")
    void completeCareSchedule_restricted_requiresDetails() {
        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
        req.setFindings("Minor splint");
        req.setDiagnosis("Splint bone inflammation");
        req.setTrainingDecision(TrainingDecision.RESTRICTED);
        req.setRestrictionDetails(null); // missing details

        ApiException ex = assertThrows(ApiException.class, () -> service.completeCareSchedule(100L, req, 5L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("VALIDATION_ERROR", ex.getErrorCode());
    }

    @Test
    @DisplayName("A19: completeCareSchedule BLOCKED without rejectAdmission advances to TRAINER_REVIEW and keeps quarantine stall")
    void completeCareSchedule_blocked_withoutRejectAdmission_advancesToTrainerReviewAndKeepsStall() {
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        schedule.setVeterinarianId(5L);

        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));

        lenient().when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(i -> {
            HealthRecord hr = i.getArgument(0);
            hr.setId(51L);
            return hr;
        });
        when(admissionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));

        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
        req.setFindings("Mild splint bone strain, rest required");
        req.setDiagnosis("Splint Bone Desmitis");
        req.setTrainingDecision(TrainingDecision.BLOCKED);
        req.setRestrictionDetails("No strenuous workouts for 14 days");
        req.setRejectAdmission(false);

        CareScheduleResponse response = service.completeCareSchedule(100L, req, 5L);

        assertEquals(CareScheduleStatus.COMPLETED, response.status());
        assertEquals(TrainingStatus.BLOCKED, horse.getTrainingStatus());
        assertTrue(horse.isTrainingLocked());
        assertEquals("No strenuous workouts for 14 days", horse.getTrainingLockReason());
        assertEquals(5L, horse.getTrainingLockVetId());
        assertEquals(HorseStatus.CANDIDATE, horse.getCurrentStatus());
        assertEquals(99L, horse.getCurrentStallId());
        assertEquals(AdmissionStatus.TRAINER_REVIEW, admission.getStatus());
        assertEquals(VetDecision.RECHECK_REQUIRED, admission.getVetDecision());
        assertEquals(StallStatus.OCCUPIED, stall.getStatus());

        verify(stallRepository, never()).save(any());
        verify(admissionRepository).save(admission);
    }

    @Test
    @DisplayName("A19b: completeCareSchedule BLOCKED with explicit rejectAdmission rejects Admission and releases quarantine stall")
    void completeCareSchedule_blocked_withExplicitRejectAdmission_rejectsAdmissionAndReleasesStall() {
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        schedule.setVeterinarianId(5L);

        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));

        lenient().when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(i -> {
            HealthRecord hr = i.getArgument(0);
            hr.setId(51L);
            return hr;
        });
        when(admissionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(stallRepository.findById(99L)).thenReturn(Optional.of(stall));
        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));

        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
        req.setFindings("Severe respiratory infection, contagious strangles suspected");
        req.setDiagnosis("Equine Strangles");
        req.setTrainingDecision(TrainingDecision.BLOCKED);
        req.setRestrictionDetails("Contagious infectious disease; unfit for admission");
        req.setRejectAdmission(true);
        req.setRejectionReason("Equine strangles outbreak risk; medically rejected");

        CareScheduleResponse response = service.completeCareSchedule(100L, req, 5L);

        assertEquals(CareScheduleStatus.COMPLETED, response.status());
        assertEquals(TrainingStatus.BLOCKED, horse.getTrainingStatus());
        assertTrue(horse.isTrainingLocked());
        assertEquals(HorseStatus.REJECTED, horse.getCurrentStatus());
        assertNull(horse.getCurrentStallId());
        assertEquals(AdmissionStatus.REJECTED, admission.getStatus());
        assertEquals(VetDecision.REJECTED, admission.getVetDecision());
        assertEquals(StallStatus.AVAILABLE, stall.getStatus());

        verify(stallRepository).save(stall);
        verify(admissionRepository).save(admission);
    }

    @Test
    @DisplayName("A19c: completeCareSchedule RESTRICTED advances to TRAINER_REVIEW and keeps quarantine stall")
    void completeCareSchedule_restricted_advancesToTrainerReviewAndKeepsStall() {
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        schedule.setVeterinarianId(5L);

        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));

        lenient().when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(i -> {
            HealthRecord hr = i.getArgument(0);
            hr.setId(52L);
            return hr;
        });
        when(admissionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));

        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
        req.setFindings("Mild foot sensitivity");
        req.setDiagnosis("Minor foot soreness");
        req.setTrainingDecision(TrainingDecision.RESTRICTED);
        req.setRestrictionDetails("Light training on soft track only");
        req.setRejectAdmission(false);

        CareScheduleResponse response = service.completeCareSchedule(100L, req, 5L);

        assertEquals(CareScheduleStatus.COMPLETED, response.status());
        assertEquals(TrainingStatus.RESTRICTED, horse.getTrainingStatus());
        assertTrue(horse.isTrainingLocked());
        assertEquals(HorseStatus.CANDIDATE, horse.getCurrentStatus());
        assertEquals(99L, horse.getCurrentStallId());
        assertEquals(AdmissionStatus.TRAINER_REVIEW, admission.getStatus());
        assertEquals(VetDecision.RECHECK_REQUIRED, admission.getVetDecision());
        assertEquals(StallStatus.OCCUPIED, stall.getStatus());

        verify(stallRepository, never()).save(any());
        verify(admissionRepository).save(admission);
    }

    @Test
    @DisplayName("A20: BR-TRN-05: active URGENT care prevents ALLOWED training decision")
    void completeCareSchedule_brTrn05_activeUrgentBlocksAllowed() {
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        schedule.setVeterinarianId(5L);

        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));

        lenient().when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        // Horse has active URGENT care!
        when(careScheduleRepository.existsByHorseIdAndCareTypeAndStatusIn(eq(10L), eq(CareType.URGENT), any()))
                .thenReturn(true);

        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
        req.setFindings("Normal physical exam");
        req.setDiagnosis("Healthy");
        req.setTrainingDecision(TrainingDecision.ALLOWED);

        ApiException ex = assertThrows(ApiException.class, () -> service.completeCareSchedule(100L, req, 5L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("BR_TRN_05", ex.getErrorCode());
    }

    @Test
    @DisplayName("A20b: BR-TRN-05: active URGENT care prevents ALLOWED training decision during ROUTINE care")
    void completeCareSchedule_routineCare_brTrn05_activeUrgentBlocksAllowed() {
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        schedule.setCareType(CareType.ROUTINE);
        schedule.setVeterinarianId(5L);

        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));

        lenient().when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        when(careScheduleRepository.existsByHorseIdAndCareTypeAndStatusIn(eq(10L), eq(CareType.URGENT), any()))
                .thenReturn(true);

        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
        req.setFindings("Routine recheck normal");
        req.setDiagnosis("Healthy");
        req.setTrainingDecision(TrainingDecision.ALLOWED);

        ApiException ex = assertThrows(ApiException.class, () -> service.completeCareSchedule(100L, req, 5L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("BR_TRN_05", ex.getErrorCode());
    }

    @Test
    @DisplayName("A20c: BR-TRN-05: completing URGENT care with another active URGENT care prevents ALLOWED")
    void completeCareSchedule_urgentCare_withAnotherActiveUrgentBlocksAllowed() {
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        schedule.setCareType(CareType.URGENT);
        schedule.setVeterinarianId(5L);

        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));

        lenient().when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        // Another active urgent care exists!
        when(careScheduleRepository.existsByHorseIdAndCareTypeAndStatusInAndIdNot(eq(10L), eq(CareType.URGENT), any(), eq(100L)))
                .thenReturn(true);

        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
        req.setFindings("Laceration repaired");
        req.setDiagnosis("Healed wound");
        req.setTrainingDecision(TrainingDecision.ALLOWED);

        ApiException ex = assertThrows(ApiException.class, () -> service.completeCareSchedule(100L, req, 5L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("BR_TRN_05", ex.getErrorCode());
    }

    @Test
    @DisplayName("A20d: BR-TRN-05: completing URGENT care without other active URGENT care allows ALLOWED")
    void completeCareSchedule_urgentCare_withoutOtherActiveUrgentAllowsAllowed() {
        horse.setCurrentStatus(HorseStatus.ELIGIBLE);
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        schedule.setCareType(CareType.URGENT);
        schedule.setVeterinarianId(5L);

        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));

        lenient().when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));
        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        when(careScheduleRepository.existsByHorseIdAndCareTypeAndStatusInAndIdNot(eq(10L), eq(CareType.URGENT), any(), eq(100L)))
                .thenReturn(false);
        when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));

        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
        req.setFindings("Colic symptoms resolved");
        req.setDiagnosis("Resolved colic");
        req.setTrainingDecision(TrainingDecision.ALLOWED);

        CareScheduleResponse response = service.completeCareSchedule(100L, req, 5L);
        assertEquals(CareScheduleStatus.COMPLETED, response.status());
        assertEquals(TrainingStatus.ALLOWED, horse.getTrainingStatus());
        assertFalse(horse.isTrainingLocked());
    }

    @Test
    @DisplayName("A20e: completeCareSchedule with rejectAdmission=true but missing rejectionReason throws VALIDATION_ERROR")
    void completeCareSchedule_rejectAdmissionWithoutReason_throwsBadRequest() {
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        schedule.setVeterinarianId(5L);

        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
        req.setFindings("Severe strangles");
        req.setDiagnosis("Equine Strangles");
        req.setTrainingDecision(TrainingDecision.BLOCKED);
        req.setRestrictionDetails("Infectious");
        req.setRejectAdmission(true);
        req.setRejectionReason(null); // missing!

        ApiException ex = assertThrows(ApiException.class, () -> service.completeCareSchedule(100L, req, 5L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("VALIDATION_ERROR", ex.getErrorCode());
    }

    @Test
    @DisplayName("A21: cancelCareSchedule requires reason and releases offers")
    void cancelCareSchedule_success() {
        schedule.setStatus(CareScheduleStatus.SCHEDULED);
        schedule.setVeterinarianId(5L);
        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));
        lenient().when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));
        when(vetOfferRepository.findByCareScheduleId(100L))
                .thenReturn(List.of(offer));
        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));

        CancelCareScheduleRequest req = new CancelCareScheduleRequest();
        req.setReason("Horse transported to medical center");

        CareScheduleResponse response = service.cancelCareSchedule(100L, req, 5L);

        assertEquals(CareScheduleStatus.CANCELLED, response.status());
        assertEquals("Horse transported to medical center", response.cancelReason());
        assertEquals(VetOfferStatus.EXPIRED, offer.getStatus());
        verify(vetOfferRepository).save(offer);
    }

    @Test
    @DisplayName("A22: getScheduleDetail uses indexed findByCareScheduleId and populates health record")
    void getScheduleDetail_populatesHealthRecordUsingFindByCareScheduleId() {
        schedule.setStatus(CareScheduleStatus.COMPLETED);
        schedule.setVeterinarianId(5L);

        when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));
        lenient().when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));
        User vetUser = new User();
        vetUser.setId(5L);
        vetUser.setFullName("Dr. Veterinarian");
        vetUser.setEmail("vet@club.com");
        when(userRepository.findById(5L)).thenReturn(Optional.of(vetUser));

        HealthRecord hr = new HealthRecord();
        hr.setId(50L);
        hr.setCareScheduleId(100L);
        hr.setFindings("Clear lungs, sound gait");
        hr.setDiagnosis("Good condition");
        hr.setTreatment("None");
        hr.setTrainingDecision(TrainingDecision.ALLOWED);
        when(healthRecordRepository.findByCareScheduleId(100L)).thenReturn(Optional.of(hr));
        when(vetOfferRepository.findByCareScheduleId(100L)).thenReturn(Collections.emptyList());

        CareScheduleDetailResponse detail = service.getScheduleDetail(100L);

        assertNotNull(detail);
        assertEquals(100L, detail.schedule().id());
        assertNotNull(detail.veterinarian());
        assertEquals("Dr. Veterinarian", detail.veterinarian().fullName());
        assertNotNull(detail.healthRecord());
        assertEquals(50L, detail.healthRecord().id());
        assertEquals("Clear lungs, sound gait", detail.healthRecord().findings());
        assertEquals("ALLOWED", detail.healthRecord().trainingDecision());
        verify(healthRecordRepository).findByCareScheduleId(100L);
        verify(healthRecordRepository, never()).findAll();
    }

    @Test
    @DisplayName("A23: dispatchOffersForSchedule skips veterinarian who currently has an IN_PROGRESS care schedule")
    void dispatchOffersForSchedule_skipsVetWithInProgressExam() {
        User vetBusy = new User();
        vetBusy.setId(5L);
        vetBusy.setFullName("Dr. Busy");

        User vetFree = new User();
        vetFree.setId(6L);
        vetFree.setFullName("Dr. Free");

        when(userRepository.findActiveVeterinarians()).thenReturn(List.of(vetBusy, vetFree));
        when(careScheduleRepository.existsByVeterinarianIdAndStatus(5L, CareScheduleStatus.IN_PROGRESS)).thenReturn(true);
        when(careScheduleRepository.existsByVeterinarianIdAndStatus(6L, CareScheduleStatus.IN_PROGRESS)).thenReturn(false);
        when(vetOfferRepository.findByCareScheduleId(100L)).thenReturn(Collections.emptyList());

        ArgumentCaptor<VetOffer> offerCaptor = ArgumentCaptor.forClass(VetOffer.class);

        service.dispatchOffersForSchedule(schedule);

        verify(vetOfferRepository).save(offerCaptor.capture());
        VetOffer savedOffer = offerCaptor.getValue();
        assertEquals(6L, savedOffer.getVeterinarianId(), "Offer should be dispatched to Dr. Free (id 6L) skipping Dr. Busy (id 5L)");
        assertEquals(CareScheduleStatus.AWAITING_VET_CONFIRMATION, schedule.getStatus());
    }

    @Test
    @DisplayName("A24: dispatchOffersForSchedule skips veterinarian with an overlapping SCHEDULED slot")
    void dispatchOffersForSchedule_skipsVetWithOverlappingScheduledSlot() {
        LocalDateTime proposedTime = LocalDateTime.now().plusHours(2);
        schedule.setScheduledAt(proposedTime);
        schedule.setDurationMinutes(30);

        User vetBusy = new User();
        vetBusy.setId(5L);
        vetBusy.setFullName("Dr. Busy Slot");

        User vetFree = new User();
        vetFree.setId(6L);
        vetFree.setFullName("Dr. Free Slot");

        CareSchedule busyVetSchedule = new CareSchedule();
        busyVetSchedule.setId(777L);
        busyVetSchedule.setVeterinarianId(5L);
        busyVetSchedule.setStatus(CareScheduleStatus.SCHEDULED);
        busyVetSchedule.setScheduledAt(proposedTime.plusMinutes(10)); // overlaps with [proposedTime, proposedTime + 30m]
        busyVetSchedule.setDurationMinutes(30);

        when(userRepository.findActiveVeterinarians()).thenReturn(List.of(vetBusy, vetFree));
        when(careScheduleRepository.existsByVeterinarianIdAndStatus(anyLong(), eq(CareScheduleStatus.IN_PROGRESS))).thenReturn(false);
        when(careScheduleRepository.findScheduledForVet(5L, CareScheduleStatus.SCHEDULED)).thenReturn(List.of(busyVetSchedule));
        when(careScheduleRepository.findScheduledForVet(6L, CareScheduleStatus.SCHEDULED)).thenReturn(Collections.emptyList());
        when(vetOfferRepository.findByCareScheduleId(100L)).thenReturn(Collections.emptyList());

        ArgumentCaptor<VetOffer> offerCaptor = ArgumentCaptor.forClass(VetOffer.class);

        service.dispatchOffersForSchedule(schedule);

        verify(vetOfferRepository).save(offerCaptor.capture());
        VetOffer savedOffer = offerCaptor.getValue();
        assertEquals(6L, savedOffer.getVeterinarianId(), "Offer should be dispatched to Dr. Free Slot (id 6L) skipping Dr. Busy Slot (id 5L)");
        assertEquals(CareScheduleStatus.AWAITING_VET_CONFIRMATION, schedule.getStatus());
    }

    @Test
    @DisplayName("A25: dispatchOffersForSchedule does not dispatch when horse has an IN_PROGRESS or overlapping SCHEDULED schedule")
    void dispatchOffersForSchedule_whenHorseBusy_doesNotDispatch() {
        // Case 1: Horse has IN_PROGRESS schedule
        when(careScheduleRepository.existsByHorseIdAndStatus(10L, CareScheduleStatus.IN_PROGRESS)).thenReturn(true);

        service.dispatchOffersForSchedule(schedule);

        verify(vetOfferRepository, never()).save(any(VetOffer.class));
        assertEquals(CareScheduleStatus.REQUESTED, schedule.getStatus());

        // Case 2: Horse has overlapping SCHEDULED schedule
        when(careScheduleRepository.existsByHorseIdAndStatus(10L, CareScheduleStatus.IN_PROGRESS)).thenReturn(false);
        LocalDateTime proposedTime = LocalDateTime.now().plusHours(2);
        schedule.setScheduledAt(proposedTime);
        schedule.setDurationMinutes(30);

        CareSchedule conflictingHorseSchedule = new CareSchedule();
        conflictingHorseSchedule.setId(888L);
        conflictingHorseSchedule.setHorseId(10L);
        conflictingHorseSchedule.setStatus(CareScheduleStatus.SCHEDULED);
        conflictingHorseSchedule.setScheduledAt(proposedTime.minusMinutes(15));
        conflictingHorseSchedule.setDurationMinutes(30);

        when(careScheduleRepository.findScheduledForHorse(10L, CareScheduleStatus.SCHEDULED))
                .thenReturn(List.of(conflictingHorseSchedule));

        service.dispatchOffersForSchedule(schedule);

        verify(vetOfferRepository, never()).save(any(VetOffer.class));
        assertEquals(CareScheduleStatus.REQUESTED, schedule.getStatus());
    }

    @Test
    @DisplayName("A26: BR-CARE-13: acceptOffer on URGENT yields conflicting schedule and redispatches it to available vet")
    void acceptOffer_urgentYieldsAndRedispatchesConflictingSchedule() {
        CareSchedule urgentSchedule = new CareSchedule();
        urgentSchedule.setId(300L);
        urgentSchedule.setHorseId(10L);
        urgentSchedule.setCareType(CareType.URGENT);
        urgentSchedule.setStatus(CareScheduleStatus.AWAITING_VET_CONFIRMATION);
        urgentSchedule.setDurationMinutes(30);
        urgentSchedule.setHorseId(20L);
        lenient().when(horseRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(horse));

        LocalDateTime slotTime = LocalDateTime.now().plusHours(2);
        VetOffer urgentOffer = new VetOffer();
        urgentOffer.setId(400L);
        urgentOffer.setCareScheduleId(300L);
        urgentOffer.setVeterinarianId(5L);
        urgentOffer.setStatus(VetOfferStatus.PENDING);
        urgentOffer.setProposedScheduledAt(slotTime);
        urgentOffer.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        CareSchedule conflictingRoutine = new CareSchedule();
        conflictingRoutine.setId(500L);
        conflictingRoutine.setCareType(CareType.ROUTINE);
        conflictingRoutine.setStatus(CareScheduleStatus.SCHEDULED);
        conflictingRoutine.setVeterinarianId(5L);
        conflictingRoutine.setScheduledAt(slotTime);
        conflictingRoutine.setDurationMinutes(30);
        conflictingRoutine.setHorseId(10L);

        VetOffer routineOffer = new VetOffer();
        routineOffer.setId(600L);
        routineOffer.setCareScheduleId(500L);
        routineOffer.setVeterinarianId(5L);
        routineOffer.setStatus(VetOfferStatus.ACCEPTED);

        User vet5 = new User();
        vet5.setId(5L);
        vet5.setFullName("Dr. Five");

        User vet6 = new User();
        vet6.setId(6L);
        vet6.setFullName("Dr. Six");

        when(vetOfferRepository.findByIdForUpdate(400L)).thenReturn(Optional.of(urgentOffer));
        when(careScheduleRepository.findByIdForUpdate(300L)).thenReturn(Optional.of(urgentSchedule));
        lenient().when(careScheduleRepository.findById(300L)).thenReturn(Optional.of(urgentSchedule));
        when(careScheduleRepository.findScheduledForVetForUpdate(5L, CareScheduleStatus.SCHEDULED))
                .thenReturn(List.of(conflictingRoutine));
        when(vetOfferRepository.findFirstByCareScheduleIdAndStatus(500L, VetOfferStatus.ACCEPTED))
                .thenReturn(Optional.of(routineOffer));

        // When redispatching conflictingRoutine:
        when(userRepository.findActiveVeterinarians()).thenReturn(List.of(vet5, vet6));
        when(careScheduleRepository.existsByVeterinarianIdAndStatus(anyLong(), eq(CareScheduleStatus.IN_PROGRESS))).thenReturn(false);
        // Vet 5 now has urgentSchedule SCHEDULED at slotTime
        when(careScheduleRepository.findScheduledForVet(5L, CareScheduleStatus.SCHEDULED)).thenReturn(List.of(urgentSchedule));
        when(careScheduleRepository.findScheduledForVet(6L, CareScheduleStatus.SCHEDULED)).thenReturn(Collections.emptyList());
        when(vetOfferRepository.findByCareScheduleId(500L)).thenReturn(Collections.emptyList());

        service.acceptOffer(400L, 5L);

        // Preempted routine schedule yielded and redispatched to Vet 6
        assertEquals(VetOfferStatus.RELEASED, routineOffer.getStatus());
        assertEquals(CareScheduleStatus.AWAITING_VET_CONFIRMATION, conflictingRoutine.getStatus());

        ArgumentCaptor<VetOffer> offerCaptor = ArgumentCaptor.forClass(VetOffer.class);
        verify(vetOfferRepository, atLeastOnce()).save(offerCaptor.capture());
        List<VetOffer> savedOffers = offerCaptor.getAllValues();
        boolean redispatchedToVet6 = savedOffers.stream().anyMatch(vo ->
                vo.getCareScheduleId().equals(500L) && vo.getVeterinarianId().equals(6L) && vo.getStatus() == VetOfferStatus.PENDING);
        assertTrue(redispatchedToVet6, "Conflicting schedule should be re-offered to Vet 6 (id 6L)");

        // Urgent schedule is SCHEDULED with Vet 5
        assertEquals(CareScheduleStatus.SCHEDULED, urgentSchedule.getStatus());
        assertEquals(5L, urgentSchedule.getVeterinarianId());
        assertEquals(VetOfferStatus.ACCEPTED, urgentOffer.getStatus());
    }

    @Test
    @DisplayName("A27: cancelCareSchedule throws 409 CONFLICT when care schedule is IN_PROGRESS")
    void cancelCareSchedule_whenInProgress_throwsConflict() {
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        schedule.setVeterinarianId(5L);
        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));
        lenient().when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));

        CancelCareScheduleRequest req = new CancelCareScheduleRequest();
        req.setReason("Emergency cancellation");

        ApiException ex = assertThrows(ApiException.class, () -> service.cancelCareSchedule(100L, req, 5L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("INVALID_STATUS", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Cannot cancel care schedule that is in progress or completed"));
    }

    @Test
    @DisplayName("A28: cancelCareSchedule throws 403 FORBIDDEN when actor is not assigned vet and not club manager")
    void cancelCareSchedule_unassignedUser_throwsForbidden() {
        schedule.setStatus(CareScheduleStatus.SCHEDULED);
        schedule.setVeterinarianId(5L);
        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));
        lenient().when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));

        User unauthorizedUser = new User();
        unauthorizedUser.setId(999L);
        Role groomRole = new Role();
        groomRole.setName("GROOM");
        unauthorizedUser.setRole(groomRole);
        when(userRepository.findById(999L)).thenReturn(Optional.of(unauthorizedUser));

        CancelCareScheduleRequest req = new CancelCareScheduleRequest();
        req.setReason("Cancel request by unauthorized groom");

        ApiException ex = assertThrows(ApiException.class, () -> service.cancelCareSchedule(100L, req, 999L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("FORBIDDEN", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Only assigned veterinarian or club manager can cancel schedule"));
    }

    @Test
    @DisplayName("A29: cancelCareSchedule succeeds when actor has CLUB_MANAGER role even if unassigned")
    void cancelCareSchedule_clubManager_success() {
        schedule.setStatus(CareScheduleStatus.SCHEDULED);
        schedule.setVeterinarianId(5L);
        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));
        lenient().when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));

        User manager = new User();
        manager.setId(888L);
        Role managerRole = new Role();
        managerRole.setName("CLUB_MANAGER");
        manager.setRole(managerRole);
        when(userRepository.findById(888L)).thenReturn(Optional.of(manager));
        when(vetOfferRepository.findByCareScheduleId(100L)).thenReturn(List.of(offer));

        CancelCareScheduleRequest req = new CancelCareScheduleRequest();
        req.setReason("Club event rescheduled");

        CareScheduleResponse response = service.cancelCareSchedule(100L, req, 888L);

        assertEquals(CareScheduleStatus.CANCELLED, response.status());
        assertEquals("Club event rescheduled", response.cancelReason());
        assertEquals(VetOfferStatus.EXPIRED, offer.getStatus());
    }

    @Test
    @DisplayName("A30: expirePendingOffers expires timed-out offers with lock and re-dispatches schedule")
    void expirePendingOffers_success() {
        VetOffer expiredOffer = new VetOffer();
        expiredOffer.setId(250L);
        expiredOffer.setCareScheduleId(100L);
        expiredOffer.setVeterinarianId(5L);
        expiredOffer.setStatus(VetOfferStatus.PENDING);
        expiredOffer.setExpiresAt(LocalDateTime.now().minusMinutes(5));

        schedule.setStatus(CareScheduleStatus.AWAITING_VET_CONFIRMATION);

        when(vetOfferRepository.findByStatusAndExpiresAtBefore(eq(VetOfferStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(expiredOffer));
        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));
        lenient().when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));
        when(vetOfferRepository.findFirstByCareScheduleIdAndStatus(100L, VetOfferStatus.PENDING))
                .thenReturn(Optional.empty());

        service.expirePendingOffers();

        assertEquals(VetOfferStatus.EXPIRED, expiredOffer.getStatus());
        verify(vetOfferRepository).save(expiredOffer);
        verify(careScheduleRepository).save(schedule);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // R5: Routine Care Scheduling & Idempotency Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("R5-1: createNextSchedule creates routine schedule with null scheduledAt and sets proposedScheduledAt on offer")
    void createNextSchedule_success_createsRoutineScheduleWithNullScheduledAtAndDispatchesOffer() {
        lenient().when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));
        when(careScheduleRepository.findFirstByHorseIdAndCareTypeAndStatusInOrderByCreatedAtDesc(
                eq(10L), eq(CareType.ROUTINE), any()))
                .thenReturn(Optional.empty());

        User activeVet = new User();
        activeVet.setId(5L);
        when(userRepository.findActiveVeterinarians()).thenReturn(List.of(activeVet));
        when(careScheduleRepository.existsByVeterinarianIdAndStatus(5L, CareScheduleStatus.IN_PROGRESS)).thenReturn(false);
        when(careScheduleRepository.findScheduledForVet(5L, CareScheduleStatus.SCHEDULED)).thenReturn(Collections.emptyList());
        when(vetOfferRepository.findByCareScheduleId(any())).thenReturn(Collections.emptyList());

        ArgumentCaptor<CareSchedule> scheduleCaptor = ArgumentCaptor.forClass(CareSchedule.class);
        when(careScheduleRepository.save(scheduleCaptor.capture())).thenAnswer(i -> {
            CareSchedule s = i.getArgument(0);
            if (s.getId() == null) s.setId(201L);
            return s;
        });

        ArgumentCaptor<VetOffer> offerCaptor = ArgumentCaptor.forClass(VetOffer.class);
        when(vetOfferRepository.save(offerCaptor.capture())).thenAnswer(i -> {
            VetOffer vo = i.getArgument(0);
            if (vo.getId() == null) vo.setId(301L);
            return vo;
        });

        CreateNextScheduleRequest req = new CreateNextScheduleRequest();
        req.setHorseId(10L);
        req.setAdmissionId(1L);
        req.setCareType(CareType.ROUTINE);
        req.setScheduledDate("2026-10-15");
        req.setDescription("Post-admission routine follow-up");

        CareScheduleResponse response = service.createNextSchedule(req, 5L);

        assertNotNull(response);
        assertEquals(CareType.ROUTINE, response.careType());
        // scheduledAt MUST remain null before vet offer acceptance!
        assertNull(response.scheduledAt());

        // Schedule is saved with scheduledAt == null and transitions to AWAITING_VET_CONFIRMATION upon offer dispatch
        CareSchedule initialSavedSchedule = scheduleCaptor.getAllValues().get(0);
        assertNull(initialSavedSchedule.getScheduledAt());
        assertEquals(CareScheduleStatus.AWAITING_VET_CONFIRMATION, response.status());

        // Offer dispatched with requested date at 14:00
        VetOffer dispatchedOffer = offerCaptor.getValue();
        assertNotNull(dispatchedOffer);
        assertEquals(5L, dispatchedOffer.getVeterinarianId());
        assertEquals(VetOfferStatus.PENDING, dispatchedOffer.getStatus());
        assertEquals(java.time.LocalDate.of(2026, 10, 15).atTime(14, 0), dispatchedOffer.getProposedScheduledAt());
    }

    @Test
    @DisplayName("R5-2: createNextSchedule idempotency: returns existing active routine schedule without duplicating")
    void createNextSchedule_idempotency_whenActiveRoutineScheduleExists_returnsExisting() {
        lenient().when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        CareSchedule existingRoutine = new CareSchedule();
        existingRoutine.setId(205L);
        existingRoutine.setHorseId(10L);
        existingRoutine.setCareType(CareType.ROUTINE);
        existingRoutine.setStatus(CareScheduleStatus.AWAITING_VET_CONFIRMATION);
        existingRoutine.setDescription("Existing routine check");

        when(careScheduleRepository.findFirstByHorseIdAndCareTypeAndStatusInOrderByCreatedAtDesc(
                eq(10L), eq(CareType.ROUTINE), any()))
                .thenReturn(Optional.of(existingRoutine));

        CreateNextScheduleRequest req = new CreateNextScheduleRequest();
        req.setHorseId(10L);
        req.setCareType(CareType.ROUTINE);
        req.setScheduledDate("2026-10-20");

        CareScheduleResponse response = service.createNextSchedule(req, 5L);

        assertNotNull(response);
        assertEquals(205L, response.id());
        assertEquals(CareScheduleStatus.AWAITING_VET_CONFIRMATION, response.status());

        // Idempotency: no new schedule saved or offer created
        verify(careScheduleRepository, never()).save(any());
        verify(vetOfferRepository, never()).save(any());
    }

    @Test
    @DisplayName("R5-3: createNextSchedule creates new schedule when previous routine care is COMPLETED")
    void createNextSchedule_whenPreviousScheduleIsCompleted_createsNewSchedule() {
        lenient().when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));
        // Completed routine schedules are excluded by activeStatuses filter, so repository returns empty
        when(careScheduleRepository.findFirstByHorseIdAndCareTypeAndStatusInOrderByCreatedAtDesc(
                eq(10L), eq(CareType.ROUTINE), any()))
                .thenReturn(Optional.empty());

        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> {
            CareSchedule s = i.getArgument(0);
            if (s.getId() == null) s.setId(210L);
            return s;
        });

        CreateNextScheduleRequest req = new CreateNextScheduleRequest();
        req.setHorseId(10L);
        req.setCareType(CareType.ROUTINE);
        req.setDescription("Subsequent routine exam");

        CareScheduleResponse response = service.createNextSchedule(req, 5L);

        assertNotNull(response);
        assertEquals(210L, response.id());
        verify(careScheduleRepository, atLeastOnce()).save(any(CareSchedule.class));
    }

    @Test
    @DisplayName("R5-4: createNextSchedule missing horseId throws 400 VALIDATION_ERROR")
    void createNextSchedule_missingHorseId_throwsBadRequest() {
        CreateNextScheduleRequest req = new CreateNextScheduleRequest();
        req.setHorseId(null);

        ApiException ex = assertThrows(ApiException.class, () -> service.createNextSchedule(req, 5L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("VALIDATION_ERROR", ex.getErrorCode());
    }

    @Test
    @DisplayName("R5-5: createNextSchedule horse not found throws 404 HORSE_NOT_FOUND")
    void createNextSchedule_horseNotFound_throwsNotFound() {
        when(horseRepository.findById(999L)).thenReturn(Optional.empty());

        CreateNextScheduleRequest req = new CreateNextScheduleRequest();
        req.setHorseId(999L);

        ApiException ex = assertThrows(ApiException.class, () -> service.createNextSchedule(req, 5L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("HORSE_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    @DisplayName("R5-6: acceptOffer sets CareSchedule.scheduledAt from VetOffer.proposedScheduledAt")
    void createNextSchedule_offerAcceptanceSetsOfficialScheduledAt() {
        CareSchedule routineSchedule = new CareSchedule();
        routineSchedule.setId(300L);
        routineSchedule.setHorseId(10L);
        routineSchedule.setCareType(CareType.ROUTINE);
        routineSchedule.setStatus(CareScheduleStatus.AWAITING_VET_CONFIRMATION);
        routineSchedule.setScheduledAt(null); // null pre-acceptance

        LocalDateTime proposed = LocalDateTime.of(2026, 10, 15, 14, 0);
        VetOffer routineOffer = new VetOffer();
        routineOffer.setId(400L);
        routineOffer.setCareScheduleId(300L);
        routineOffer.setVeterinarianId(5L);
        routineOffer.setStatus(VetOfferStatus.PENDING);
        routineOffer.setProposedScheduledAt(proposed);

        when(vetOfferRepository.findByIdForUpdate(400L)).thenReturn(Optional.of(routineOffer));
        when(careScheduleRepository.findByIdForUpdate(300L)).thenReturn(Optional.of(routineSchedule));
        lenient().when(careScheduleRepository.findById(300L)).thenReturn(Optional.of(routineSchedule));
        lenient().when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));
        when(vetOfferRepository.save(any(VetOffer.class))).thenAnswer(i -> i.getArgument(0));
        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));

        VetOfferResponse response = service.acceptOffer(400L, 5L);

        assertNotNull(response);
        assertEquals(VetOfferStatus.ACCEPTED, response.status());
        assertEquals(CareScheduleStatus.SCHEDULED, routineSchedule.getStatus());
        assertEquals(proposed, routineSchedule.getScheduledAt());
        assertEquals(5L, routineSchedule.getVeterinarianId());
    }

    @Test
    void finalizedAdmissionCannotCreateAnotherInitialExamination() {
        for (AdmissionStatus status : List.of(AdmissionStatus.APPROVED, AdmissionStatus.REJECTED,
                AdmissionStatus.TRAINER_REVIEW, AdmissionStatus.MANAGER_REVIEW)) {
            admission.setStatus(status);
            ApiException ex = assertThrows(ApiException.class, () -> service.createInitialSchedule(1L, 10L));
            assertEquals("INVALID_REVIEW_STATE", ex.getErrorCode());
        }
        verify(careScheduleRepository, never()).save(any());
    }

    @Test
    void genericNextScheduleCannotBypassInitialAdmissionGuard() {
        admission.setStatus(AdmissionStatus.APPROVED);
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));
        CreateNextScheduleRequest request = new CreateNextScheduleRequest();
        request.setHorseId(10L);
        request.setAdmissionId(1L);
        request.setCareType(CareType.INITIAL);
        assertThrows(ApiException.class, () -> service.createNextSchedule(request, 5L));
        verify(careScheduleRepository, never()).save(any());
    }

    @Test
    void staleInitialCannotReopenFinalizedAdmissionOrWriteClinicalData() {
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        schedule.setVeterinarianId(5L);
        admission.setStatus(AdmissionStatus.APPROVED);
        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));
        lenient().when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));
        CompleteCareScheduleRequest request = new CompleteCareScheduleRequest();
        request.setFindings("Normal examination");
        request.setDiagnosis("Healthy");
        request.setTrainingDecision(TrainingDecision.ALLOWED);
        ApiException ex = assertThrows(ApiException.class, () -> service.completeCareSchedule(100L, request, 5L));
        assertEquals("INVALID_REVIEW_STATE", ex.getErrorCode());
        assertEquals(AdmissionStatus.APPROVED, admission.getStatus());
        verify(healthRecordRepository, never()).save(any());
        verify(horseRepository, never()).save(any());
    }

    @Test
    void twoPendingOffersForSameVetCannotBothReserveSameSlot() {
        schedule.setStatus(CareScheduleStatus.AWAITING_VET_CONFIRMATION);
        when(vetOfferRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(offer));
        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));
        lenient().when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));
        service.acceptOffer(200L, 5L);

        CareSchedule second = new CareSchedule();
        second.setId(101L);
        second.setHorseId(10L);
        second.setCareType(CareType.INITIAL);
        second.setStatus(CareScheduleStatus.AWAITING_VET_CONFIRMATION);
        VetOffer secondOffer = new VetOffer();
        secondOffer.setId(201L);
        secondOffer.setCareScheduleId(101L);
        secondOffer.setVeterinarianId(5L);
        secondOffer.setStatus(VetOfferStatus.PENDING);
        secondOffer.setProposedScheduledAt(offer.getProposedScheduledAt());
        when(vetOfferRepository.findByIdForUpdate(201L)).thenReturn(Optional.of(secondOffer));
        when(careScheduleRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(second));
        lenient().when(careScheduleRepository.findById(101L)).thenReturn(Optional.of(second));
        when(careScheduleRepository.findScheduledForVet(5L, CareScheduleStatus.SCHEDULED)).thenReturn(List.of(schedule));
        ApiException ex = assertThrows(ApiException.class, () -> service.acceptOffer(201L, 5L));
        assertEquals("SLOT_UNAVAILABLE", ex.getErrorCode());
        assertEquals(VetOfferStatus.PENDING, secondOffer.getStatus());
        verify(vetOfferRepository, never()).save(secondOffer);
    }

    @Test
    void horseAlreadyBookedWithAnotherVetCannotAcceptOverlappingSlot() {
        schedule.setStatus(CareScheduleStatus.AWAITING_VET_CONFIRMATION);
        CareSchedule booked = new CareSchedule();
        booked.setId(101L);
        booked.setHorseId(10L);
        booked.setVeterinarianId(6L);
        booked.setScheduledAt(offer.getProposedScheduledAt());
        booked.setDurationMinutes(30);
        when(vetOfferRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(offer));
        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));
        lenient().when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));
        when(careScheduleRepository.findScheduledForHorse(10L, CareScheduleStatus.SCHEDULED)).thenReturn(List.of(booked));
        assertEquals("SLOT_UNAVAILABLE", assertThrows(ApiException.class,
                () -> service.acceptOffer(200L, 5L)).getErrorCode());
        verify(vetOfferRepository, never()).save(offer);
    }

    @Test
    void adjacentAcceptedSlotsDoNotConflict() {
        schedule.setStatus(CareScheduleStatus.AWAITING_VET_CONFIRMATION);
        CareSchedule booked = new CareSchedule();
        booked.setId(101L);
        booked.setScheduledAt(offer.getProposedScheduledAt().minusMinutes(30));
        booked.setDurationMinutes(30);
        when(vetOfferRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(offer));
        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));
        lenient().when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));
        when(careScheduleRepository.findScheduledForVet(5L, CareScheduleStatus.SCHEDULED)).thenReturn(List.of(booked));
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));
        assertEquals(VetOfferStatus.ACCEPTED, service.acceptOffer(200L, 5L).status());
    }

    @Test
    void clinicalCompletionRechecksPersistedStateAfterAcquiringLocks() {
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        schedule.setVeterinarianId(5L);
        when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));
        when(careScheduleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(schedule));
        doAnswer(invocation -> {
            if (invocation.getArgument(0) == schedule) schedule.setStatus(CareScheduleStatus.COMPLETED);
            return null;
        }).when(entityManager).refresh(any());
        CompleteCareScheduleRequest request = new CompleteCareScheduleRequest();
        request.setFindings("Normal");
        request.setDiagnosis("Healthy");
        request.setTrainingDecision(TrainingDecision.ALLOWED);
        assertEquals("INVALID_STATUS", assertThrows(ApiException.class,
                () -> service.completeCareSchedule(100L, request, 5L)).getErrorCode());
        verify(healthRecordRepository, never()).save(any());
    }
}
