package com.rtms.backend.service;
import com.rtms.backend.entity.VetOffer;
import com.rtms.backend.enums.VetOfferStatus;
import com.rtms.backend.repository.AdmissionApplicationRepository;
import com.rtms.backend.repository.VetOfferRepository;
import com.rtms.backend.entity.CareSchedule;
import com.rtms.backend.enums.CareScheduleStatus;
import com.rtms.backend.enums.CareType;
import com.rtms.backend.repository.CareScheduleRepository;
import com.rtms.backend.repository.HealthRecordRepository;
import com.rtms.backend.repository.HorseHealthMetricRepository;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.enums.HorseStatus;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.entity.User;
import com.rtms.backend.repository.UserRepository;
import com.rtms.backend.repository.StableStallRepository;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.Lock;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CareScheduleMilestone2BoundaryTest {

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

    User vet1;
    User vet2;
    User vet3;
    Horse horse;
    CareSchedule schedule;

    @BeforeEach
    void setUp() {
        service = new CareScheduleService(
                careScheduleRepository, vetOfferRepository, horseRepository,
                healthRecordRepository, metricRepository, admissionRepository,
                stallRepository, userRepository, entityManager);

        vet1 = new User();
        vet1.setId(101L);
        vet1.setFullName("Dr. Alice");

        vet2 = new User();
        vet2.setId(102L);
        vet2.setFullName("Dr. Bob");

        vet3 = new User();
        vet3.setId(103L);
        vet3.setFullName("Dr. Charlie");

        horse = new Horse();
        horse.setId(10L);
        horse.setCurrentStatus(HorseStatus.CANDIDATE);

        lenient().when(userRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(vet1));
        lenient().when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        schedule = new CareSchedule();
        schedule.setId(500L);
        schedule.setHorseId(10L);
        schedule.setCareType(CareType.INITIAL);
        schedule.setStatus(CareScheduleStatus.REQUESTED);
        schedule.setDurationMinutes(30);
    }

    @Nested
    @DisplayName("1. Slot Overlap Boundary Conditions (Exact Touches vs Overlaps)")
    class SlotOverlapBoundaryTests {

        @Test
        @DisplayName("Boundary Touch 1: Proposed slot starts exactly when existing slot ends (proposedStart == existingEnd) -> NO conflict")
        void proposedStartEqualsExistingEnd_noConflict_dispatchesOffer() {
            LocalDateTime baseTime = LocalDateTime.of(2026, 10, 1, 10, 0);
            // Existing schedule: 10:00 - 10:30 (30 mins)
            CareSchedule existing = new CareSchedule();
            existing.setId(901L);
            existing.setVeterinarianId(101L);
            existing.setStatus(CareScheduleStatus.SCHEDULED);
            existing.setScheduledAt(baseTime);
            existing.setDurationMinutes(30);

            // Proposed schedule: 10:30 - 11:00 (30 mins)
            schedule.setScheduledAt(baseTime.plusMinutes(30));
            schedule.setDurationMinutes(30);

            when(userRepository.findActiveVeterinarians()).thenReturn(List.of(vet1));
            when(careScheduleRepository.existsByVeterinarianIdAndStatus(101L, CareScheduleStatus.IN_PROGRESS)).thenReturn(false);
            when(careScheduleRepository.findScheduledForVet(101L, CareScheduleStatus.SCHEDULED)).thenReturn(List.of(existing));
            when(vetOfferRepository.findByCareScheduleId(500L)).thenReturn(Collections.emptyList());

            service.dispatchOffersForSchedule(schedule);

            ArgumentCaptor<VetOffer> offerCaptor = ArgumentCaptor.forClass(VetOffer.class);
            verify(vetOfferRepository).save(offerCaptor.capture());
            VetOffer captured = offerCaptor.getValue();
            assertEquals(101L, captured.getVeterinarianId(), "Vet 101 should be eligible as boundary touch is not an overlap");
            assertEquals(CareScheduleStatus.AWAITING_VET_CONFIRMATION, schedule.getStatus());
        }

        @Test
        @DisplayName("Boundary Touch 2: Proposed slot ends exactly when existing slot starts (proposedEnd == existingStart) -> NO conflict")
        void proposedEndEqualsExistingStart_noConflict_dispatchesOffer() {
            LocalDateTime baseTime = LocalDateTime.of(2026, 10, 1, 10, 30);
            // Existing schedule: 10:30 - 11:00 (30 mins)
            CareSchedule existing = new CareSchedule();
            existing.setId(901L);
            existing.setVeterinarianId(101L);
            existing.setStatus(CareScheduleStatus.SCHEDULED);
            existing.setScheduledAt(baseTime);
            existing.setDurationMinutes(30);

            // Proposed schedule: 10:00 - 10:30 (30 mins) -> proposedEnd is exactly 10:30
            schedule.setScheduledAt(baseTime.minusMinutes(30));
            schedule.setDurationMinutes(30);

            when(userRepository.findActiveVeterinarians()).thenReturn(List.of(vet1));
            when(careScheduleRepository.existsByVeterinarianIdAndStatus(101L, CareScheduleStatus.IN_PROGRESS)).thenReturn(false);
            when(careScheduleRepository.findScheduledForVet(101L, CareScheduleStatus.SCHEDULED)).thenReturn(List.of(existing));
            when(vetOfferRepository.findByCareScheduleId(500L)).thenReturn(Collections.emptyList());

            service.dispatchOffersForSchedule(schedule);

            ArgumentCaptor<VetOffer> offerCaptor = ArgumentCaptor.forClass(VetOffer.class);
            verify(vetOfferRepository).save(offerCaptor.capture());
            VetOffer captured = offerCaptor.getValue();
            assertEquals(101L, captured.getVeterinarianId(), "Vet 101 should be eligible as boundary touch preceding is not an overlap");
        }

        @Test
        @DisplayName("Overlap 1: 1-minute overlap at end (proposedStart is 1 min before existingEnd) -> CONFLICT")
        void proposedStartsOneMinuteBeforeExistingEnd_conflict_vetSkipped() {
            LocalDateTime baseTime = LocalDateTime.of(2026, 10, 1, 10, 0);
            // Existing: 10:00 - 10:30
            CareSchedule existing = new CareSchedule();
            existing.setId(901L);
            existing.setVeterinarianId(101L);
            existing.setStatus(CareScheduleStatus.SCHEDULED);
            existing.setScheduledAt(baseTime);
            existing.setDurationMinutes(30);

            // Proposed: 10:29 - 10:59
            schedule.setScheduledAt(baseTime.plusMinutes(29));
            schedule.setDurationMinutes(30);

            when(userRepository.findActiveVeterinarians()).thenReturn(List.of(vet1, vet2));
            when(careScheduleRepository.existsByVeterinarianIdAndStatus(anyLong(), eq(CareScheduleStatus.IN_PROGRESS))).thenReturn(false);
            when(careScheduleRepository.findScheduledForVet(101L, CareScheduleStatus.SCHEDULED)).thenReturn(List.of(existing));
            when(careScheduleRepository.findScheduledForVet(102L, CareScheduleStatus.SCHEDULED)).thenReturn(Collections.emptyList());
            when(vetOfferRepository.findByCareScheduleId(500L)).thenReturn(Collections.emptyList());

            service.dispatchOffersForSchedule(schedule);

            ArgumentCaptor<VetOffer> offerCaptor = ArgumentCaptor.forClass(VetOffer.class);
            verify(vetOfferRepository).save(offerCaptor.capture());
            VetOffer captured = offerCaptor.getValue();
            assertEquals(102L, captured.getVeterinarianId(), "Vet 101 must be skipped due to 1-min overlap, offering to Vet 102");
        }

        @Test
        @DisplayName("Overlap 2: Exact identical slot [10:00, 10:30] -> CONFLICT")
        void exactIdenticalSlot_conflict_vetSkipped() {
            LocalDateTime baseTime = LocalDateTime.of(2026, 10, 1, 10, 0);
            CareSchedule existing = new CareSchedule();
            existing.setId(901L);
            existing.setVeterinarianId(101L);
            existing.setStatus(CareScheduleStatus.SCHEDULED);
            existing.setScheduledAt(baseTime);
            existing.setDurationMinutes(30);

            schedule.setScheduledAt(baseTime);
            schedule.setDurationMinutes(30);

            when(userRepository.findActiveVeterinarians()).thenReturn(List.of(vet1, vet2));
            when(careScheduleRepository.existsByVeterinarianIdAndStatus(anyLong(), eq(CareScheduleStatus.IN_PROGRESS))).thenReturn(false);
            when(careScheduleRepository.findScheduledForVet(101L, CareScheduleStatus.SCHEDULED)).thenReturn(List.of(existing));
            when(careScheduleRepository.findScheduledForVet(102L, CareScheduleStatus.SCHEDULED)).thenReturn(Collections.emptyList());
            when(vetOfferRepository.findByCareScheduleId(500L)).thenReturn(Collections.emptyList());

            service.dispatchOffersForSchedule(schedule);

            ArgumentCaptor<VetOffer> offerCaptor = ArgumentCaptor.forClass(VetOffer.class);
            verify(vetOfferRepository).save(offerCaptor.capture());
            assertEquals(102L, offerCaptor.getValue().getVeterinarianId());
        }

        @Test
        @DisplayName("Overlap 3: Proposed encompasses existing [10:00, 11:00] vs [10:15, 10:45] -> CONFLICT")
        void proposedEncompassesExisting_conflict_vetSkipped() {
            LocalDateTime baseTime = LocalDateTime.of(2026, 10, 1, 10, 15);
            CareSchedule existing = new CareSchedule();
            existing.setId(901L);
            existing.setVeterinarianId(101L);
            existing.setStatus(CareScheduleStatus.SCHEDULED);
            existing.setScheduledAt(baseTime);
            existing.setDurationMinutes(30); // 10:15 - 10:45

            schedule.setScheduledAt(LocalDateTime.of(2026, 10, 1, 10, 0));
            schedule.setDurationMinutes(60); // 10:00 - 11:00

            when(userRepository.findActiveVeterinarians()).thenReturn(List.of(vet1, vet2));
            when(careScheduleRepository.existsByVeterinarianIdAndStatus(anyLong(), eq(CareScheduleStatus.IN_PROGRESS))).thenReturn(false);
            when(careScheduleRepository.findScheduledForVet(101L, CareScheduleStatus.SCHEDULED)).thenReturn(List.of(existing));
            when(careScheduleRepository.findScheduledForVet(102L, CareScheduleStatus.SCHEDULED)).thenReturn(Collections.emptyList());
            when(vetOfferRepository.findByCareScheduleId(500L)).thenReturn(Collections.emptyList());

            service.dispatchOffersForSchedule(schedule);

            ArgumentCaptor<VetOffer> offerCaptor = ArgumentCaptor.forClass(VetOffer.class);
            verify(vetOfferRepository).save(offerCaptor.capture());
            assertEquals(102L, offerCaptor.getValue().getVeterinarianId());
        }

        @Test
        @DisplayName("Horse Boundary Touch: Horse scheduled until 10:30, proposed starts at 10:30 -> NO conflict")
        void horseBoundaryTouch_noConflict_dispatchProceeds() {
            LocalDateTime baseTime = LocalDateTime.of(2026, 10, 1, 10, 0);
            CareSchedule horseExisting = new CareSchedule();
            horseExisting.setId(888L);
            horseExisting.setHorseId(10L);
            horseExisting.setStatus(CareScheduleStatus.SCHEDULED);
            horseExisting.setScheduledAt(baseTime);
            horseExisting.setDurationMinutes(30); // 10:00 - 10:30

            schedule.setScheduledAt(baseTime.plusMinutes(30)); // 10:30 - 11:00
            schedule.setDurationMinutes(30);

            when(careScheduleRepository.existsByHorseIdAndStatus(10L, CareScheduleStatus.IN_PROGRESS)).thenReturn(false);
            when(careScheduleRepository.findScheduledForHorse(10L, CareScheduleStatus.SCHEDULED)).thenReturn(List.of(horseExisting));
            when(userRepository.findActiveVeterinarians()).thenReturn(List.of(vet1));
            when(careScheduleRepository.existsByVeterinarianIdAndStatus(101L, CareScheduleStatus.IN_PROGRESS)).thenReturn(false);
            when(careScheduleRepository.findScheduledForVet(101L, CareScheduleStatus.SCHEDULED)).thenReturn(Collections.emptyList());
            when(vetOfferRepository.findByCareScheduleId(500L)).thenReturn(Collections.emptyList());

            service.dispatchOffersForSchedule(schedule);

            verify(vetOfferRepository).save(any(VetOffer.class));
            assertEquals(CareScheduleStatus.AWAITING_VET_CONFIRMATION, schedule.getStatus());
        }

        @Test
        @DisplayName("BR-CARE-13 Boundary Touch: Urgent starts exactly when Routine ends -> Routine is NOT yielded")
        void urgentBoundaryTouchRoutine_notYielded() {
            CareSchedule urgentSchedule = new CareSchedule();
            urgentSchedule.setId(300L);
            urgentSchedule.setHorseId(10L);
            urgentSchedule.setCareType(CareType.URGENT);
            urgentSchedule.setStatus(CareScheduleStatus.AWAITING_VET_CONFIRMATION);
            urgentSchedule.setDurationMinutes(30);
            urgentSchedule.setHorseId(20L);
            when(horseRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(horse));

            LocalDateTime baseTime = LocalDateTime.of(2026, 10, 1, 10, 0);
            // Routine: 10:00 - 10:30
            CareSchedule routineSchedule = new CareSchedule();
            routineSchedule.setId(400L);
            routineSchedule.setCareType(CareType.ROUTINE);
            routineSchedule.setStatus(CareScheduleStatus.SCHEDULED);
            routineSchedule.setVeterinarianId(101L);
            routineSchedule.setScheduledAt(baseTime);
            routineSchedule.setDurationMinutes(30);

            // Urgent offer: 10:30 - 11:00
            VetOffer urgentOffer = new VetOffer();
            urgentOffer.setId(401L);
            urgentOffer.setCareScheduleId(300L);
            urgentOffer.setVeterinarianId(101L);
            urgentOffer.setStatus(VetOfferStatus.PENDING);
            urgentOffer.setProposedScheduledAt(baseTime.plusMinutes(30));

            when(vetOfferRepository.findByIdForUpdate(401L)).thenReturn(Optional.of(urgentOffer));
            when(careScheduleRepository.findByIdForUpdate(300L)).thenReturn(Optional.of(urgentSchedule));
            lenient().when(careScheduleRepository.findById(300L)).thenReturn(Optional.of(urgentSchedule));
            when(careScheduleRepository.findScheduledForVetForUpdate(101L, CareScheduleStatus.SCHEDULED))
                    .thenReturn(List.of(routineSchedule));

            service.acceptOffer(401L, 101L);

            // Routine schedule should NOT be yielded because boundary touch is not overlap
            assertEquals(CareScheduleStatus.SCHEDULED, routineSchedule.getStatus());
            assertEquals(101L, routineSchedule.getVeterinarianId());
            assertEquals(CareScheduleStatus.SCHEDULED, urgentSchedule.getStatus());
        }
    }

    @Nested
    @DisplayName("2. Round Rollover and Candidate Rotation")
    class RoundRolloverAndRotationTests {

        @Test
        @DisplayName("Rotation in Round 1: When Vet 1 declines, candidate rotates to Vet 2 in Round 1")
        void vet1Declines_rotatesToVet2InSameRound() {
            VetOffer offer1 = new VetOffer();
            offer1.setId(601L);
            offer1.setCareScheduleId(500L);
            offer1.setVeterinarianId(101L);
            offer1.setStatus(VetOfferStatus.PENDING);
            offer1.setRound(1);

            when(vetOfferRepository.findByIdForUpdate(601L)).thenReturn(Optional.of(offer1));
            when(careScheduleRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(schedule));
            lenient().when(careScheduleRepository.findById(500L)).thenReturn(Optional.of(schedule));
            when(vetOfferRepository.findFirstByCareScheduleIdAndStatus(500L, VetOfferStatus.PENDING)).thenReturn(Optional.empty());

            // Redispatch setup
            when(userRepository.findActiveVeterinarians()).thenReturn(List.of(vet1, vet2, vet3));
            when(careScheduleRepository.existsByVeterinarianIdAndStatus(anyLong(), eq(CareScheduleStatus.IN_PROGRESS))).thenReturn(false);
            when(careScheduleRepository.findScheduledForVet(anyLong(), eq(CareScheduleStatus.SCHEDULED))).thenReturn(Collections.emptyList());
            when(vetOfferRepository.findByCareScheduleId(500L)).thenReturn(List.of(offer1));

            service.declineOffer(601L, 101L);

            assertEquals(VetOfferStatus.DECLINED, offer1.getStatus());

            ArgumentCaptor<VetOffer> offerCaptor = ArgumentCaptor.forClass(VetOffer.class);
            verify(vetOfferRepository, atLeastOnce()).save(offerCaptor.capture());
            List<VetOffer> allSaved = offerCaptor.getAllValues();
            VetOffer nextOffer = allSaved.get(allSaved.size() - 1);

            assertEquals(102L, nextOffer.getVeterinarianId(), "Offer should rotate to Vet 2 (Dr. Bob)");
            assertEquals(1, nextOffer.getRound(), "Round should remain 1");
            assertEquals(VetOfferStatus.PENDING, nextOffer.getStatus());
        }

        @Test
        @DisplayName("Round Rollover: When all eligible vets in round decline, round advances (Round 1 -> Round 2) and rotates back to first eligible vet")
        void allVetsDecline_advancesRoundAndRotatesBack() {
            VetOffer offer1 = new VetOffer();
            offer1.setId(601L);
            offer1.setCareScheduleId(500L);
            offer1.setVeterinarianId(101L);
            offer1.setStatus(VetOfferStatus.DECLINED);
            offer1.setRound(1);

            VetOffer offer2 = new VetOffer();
            offer2.setId(602L);
            offer2.setCareScheduleId(500L);
            offer2.setVeterinarianId(102L);
            offer2.setStatus(VetOfferStatus.PENDING);
            offer2.setRound(1);

            when(vetOfferRepository.findByIdForUpdate(602L)).thenReturn(Optional.of(offer2));
            when(careScheduleRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(schedule));
            lenient().when(careScheduleRepository.findById(500L)).thenReturn(Optional.of(schedule));
            when(vetOfferRepository.findFirstByCareScheduleIdAndStatus(500L, VetOfferStatus.PENDING)).thenReturn(Optional.empty());

            // Redispatch setup: active vets are Vet 1 and Vet 2
            when(userRepository.findActiveVeterinarians()).thenReturn(List.of(vet1, vet2));
            when(careScheduleRepository.existsByVeterinarianIdAndStatus(anyLong(), eq(CareScheduleStatus.IN_PROGRESS))).thenReturn(false);
            when(careScheduleRepository.findScheduledForVet(anyLong(), eq(CareScheduleStatus.SCHEDULED))).thenReturn(Collections.emptyList());
            when(vetOfferRepository.findByCareScheduleId(500L)).thenReturn(List.of(offer1, offer2));

            service.declineOffer(602L, 102L);

            assertEquals(VetOfferStatus.DECLINED, offer2.getStatus());

            ArgumentCaptor<VetOffer> offerCaptor = ArgumentCaptor.forClass(VetOffer.class);
            verify(vetOfferRepository, atLeastOnce()).save(offerCaptor.capture());
            List<VetOffer> allSaved = offerCaptor.getAllValues();
            VetOffer round2Offer = allSaved.get(allSaved.size() - 1);

            assertEquals(2, round2Offer.getRound(), "Round must increment to 2");
            assertEquals(101L, round2Offer.getVeterinarianId(), "Round 2 must rotate back to Dr. Alice (101L)");
            assertEquals(VetOfferStatus.PENDING, round2Offer.getStatus());
        }

        @Test
        @DisplayName("Candidate Rotation when Intermediate Vet is Busy: Skips busy Vet and advances to next free Vet in same round")
        void intermediateVetBusy_skipsToNextFreeVet() {
            VetOffer offer1 = new VetOffer();
            offer1.setId(601L);
            offer1.setCareScheduleId(500L);
            offer1.setVeterinarianId(101L);
            offer1.setStatus(VetOfferStatus.DECLINED);
            offer1.setRound(1);

            // Vet 2 is busy with IN_PROGRESS exam
            when(userRepository.findActiveVeterinarians()).thenReturn(List.of(vet1, vet2, vet3));
            when(careScheduleRepository.existsByVeterinarianIdAndStatus(101L, CareScheduleStatus.IN_PROGRESS)).thenReturn(false);
            when(careScheduleRepository.existsByVeterinarianIdAndStatus(102L, CareScheduleStatus.IN_PROGRESS)).thenReturn(true); // BUSY
            when(careScheduleRepository.existsByVeterinarianIdAndStatus(103L, CareScheduleStatus.IN_PROGRESS)).thenReturn(false);

            when(careScheduleRepository.findScheduledForVet(101L, CareScheduleStatus.SCHEDULED)).thenReturn(Collections.emptyList());
            when(careScheduleRepository.findScheduledForVet(103L, CareScheduleStatus.SCHEDULED)).thenReturn(Collections.emptyList());
            when(vetOfferRepository.findByCareScheduleId(500L)).thenReturn(List.of(offer1));

            service.dispatchOffersForSchedule(schedule);

            ArgumentCaptor<VetOffer> offerCaptor = ArgumentCaptor.forClass(VetOffer.class);
            verify(vetOfferRepository).save(offerCaptor.capture());
            VetOffer nextOffer = offerCaptor.getValue();

            assertEquals(103L, nextOffer.getVeterinarianId(), "Should skip busy Vet 102 and dispatch directly to Vet 103");
            assertEquals(1, nextOffer.getRound(), "Should remain in round 1");
        }

        @Test
        @DisplayName("All Vets Busy: Dispatches no offers and keeps schedule in REQUESTED status cleanly")
        void allVetsBusy_doesNotDispatch_staysRequested() {
            when(userRepository.findActiveVeterinarians()).thenReturn(List.of(vet1, vet2));
            when(careScheduleRepository.existsByVeterinarianIdAndStatus(anyLong(), eq(CareScheduleStatus.IN_PROGRESS))).thenReturn(true);

            service.dispatchOffersForSchedule(schedule);

            verify(vetOfferRepository, never()).save(any(VetOffer.class));
            assertEquals(CareScheduleStatus.REQUESTED, schedule.getStatus());
        }

        @Test
        @DisplayName("Sequential Multi-Round Rollover (Round 1 -> Round 2 -> Round 3) for Single Vet")
        void singleVetMultiRoundRollover() {
            // Single vet in clinic
            when(userRepository.findActiveVeterinarians()).thenReturn(List.of(vet1));
            when(careScheduleRepository.existsByVeterinarianIdAndStatus(101L, CareScheduleStatus.IN_PROGRESS)).thenReturn(false);
            when(careScheduleRepository.findScheduledForVet(101L, CareScheduleStatus.SCHEDULED)).thenReturn(Collections.emptyList());

            // Suppose Round 1 was declined, Round 2 was declined
            VetOffer r1Offer = new VetOffer();
            r1Offer.setCareScheduleId(500L);
            r1Offer.setVeterinarianId(101L);
            r1Offer.setRound(1);
            r1Offer.setStatus(VetOfferStatus.DECLINED);

            VetOffer r2Offer = new VetOffer();
            r2Offer.setCareScheduleId(500L);
            r2Offer.setVeterinarianId(101L);
            r2Offer.setRound(2);
            r2Offer.setStatus(VetOfferStatus.DECLINED);

            when(vetOfferRepository.findByCareScheduleId(500L)).thenReturn(List.of(r1Offer, r2Offer));

            service.dispatchOffersForSchedule(schedule);

            ArgumentCaptor<VetOffer> offerCaptor = ArgumentCaptor.forClass(VetOffer.class);
            verify(vetOfferRepository).save(offerCaptor.capture());
            VetOffer r3Offer = offerCaptor.getValue();

            assertEquals(3, r3Offer.getRound(), "Round should rollover to 3");
            assertEquals(101L, r3Offer.getVeterinarianId());
            assertEquals(VetOfferStatus.PENDING, r3Offer.getStatus());
        }
    }

    @Nested
    @DisplayName("3. Pessimistic Locking Query Declarations & Service Invocations")
    class PessimisticLockingValidationTests {

        @Test
        @DisplayName("VetOfferRepository declares @Lock(LockModeType.PESSIMISTIC_WRITE) on findByIdForUpdate and findByStatusAndExpiresAtBefore")
        void vetOfferRepository_pessimisticLockAnnotationsPresent() throws NoSuchMethodException {
            Method findByIdForUpdate = VetOfferRepository.class.getMethod("findByIdForUpdate", Long.class);
            Lock lock1 = findByIdForUpdate.getAnnotation(Lock.class);
            assertNotNull(lock1, "findByIdForUpdate must be annotated with @Lock");
            assertEquals(LockModeType.PESSIMISTIC_WRITE, lock1.value(), "findByIdForUpdate must use PESSIMISTIC_WRITE");

            Method findExpired = VetOfferRepository.class.getMethod("findByStatusAndExpiresAtBefore", VetOfferStatus.class, LocalDateTime.class);
            Lock lock2 = findExpired.getAnnotation(Lock.class);
            assertNotNull(lock2, "findByStatusAndExpiresAtBefore must be annotated with @Lock");
            assertEquals(LockModeType.PESSIMISTIC_WRITE, lock2.value(), "findByStatusAndExpiresAtBefore must use PESSIMISTIC_WRITE");
        }

        @Test
        @DisplayName("CareScheduleRepository declares @Lock(LockModeType.PESSIMISTIC_WRITE) on findByIdForUpdate and findScheduledForVetForUpdate")
        void careScheduleRepository_pessimisticLockAnnotationsPresent() throws NoSuchMethodException {
            Method findByIdForUpdate = CareScheduleRepository.class.getMethod("findByIdForUpdate", Long.class);
            Lock lock1 = findByIdForUpdate.getAnnotation(Lock.class);
            assertNotNull(lock1, "findByIdForUpdate must be annotated with @Lock");
            assertEquals(LockModeType.PESSIMISTIC_WRITE, lock1.value(), "findByIdForUpdate must use PESSIMISTIC_WRITE");

            Method findScheduledForUpdate = CareScheduleRepository.class.getMethod("findScheduledForVetForUpdate", Long.class, CareScheduleStatus.class);
            Lock lock2 = findScheduledForUpdate.getAnnotation(Lock.class);
            assertNotNull(lock2, "findScheduledForVetForUpdate must be annotated with @Lock");
            assertEquals(LockModeType.PESSIMISTIC_WRITE, lock2.value(), "findScheduledForVetForUpdate must use PESSIMISTIC_WRITE");
        }

        @Test
        @DisplayName("Service methods invoke pessimistic locking repository methods (no bypassing locks)")
        void serviceMethods_invokePessimisticLockQueries() {
            VetOffer offer = new VetOffer();
            offer.setId(200L);
            offer.setCareScheduleId(500L);
            offer.setVeterinarianId(101L);
            offer.setStatus(VetOfferStatus.PENDING);
            offer.setExpiresAt(LocalDateTime.now().plusHours(1));

            // acceptOffer invokes findByIdForUpdate on both vetOfferRepository and careScheduleRepository
            when(vetOfferRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(offer));
            when(careScheduleRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(schedule));
            lenient().when(careScheduleRepository.findById(500L)).thenReturn(Optional.of(schedule));
            when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

            service.acceptOffer(200L, 101L);

            verify(vetOfferRepository).findByIdForUpdate(200L);
            verify(careScheduleRepository).findByIdForUpdate(500L);

            // expirePendingOffers invokes findByStatusAndExpiresAtBefore with PESSIMISTIC_WRITE
            when(vetOfferRepository.findByStatusAndExpiresAtBefore(eq(VetOfferStatus.PENDING), any(LocalDateTime.class)))
                    .thenReturn(List.of(offer));

            service.expirePendingOffers();
            verify(vetOfferRepository).findByStatusAndExpiresAtBefore(eq(VetOfferStatus.PENDING), any(LocalDateTime.class));
        }
    }
}
