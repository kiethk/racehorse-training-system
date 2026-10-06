package com.rtms.backend.service;

import com.rtms.backend.dto.GroomAdmissionReviewRequest;
import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.entity.CandidateHorseProfile;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.HorsePedigree;
import com.rtms.backend.entity.CareSchedule;
import com.rtms.backend.enums.CareScheduleStatus;
import com.rtms.backend.enums.CareType;
import com.rtms.backend.entity.StableStall;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.enums.HorseStatus;
import com.rtms.backend.enums.TrainingDecision;
import com.rtms.backend.enums.ReviewDecision;
import com.rtms.backend.enums.StallStatus;
import com.rtms.backend.repository.AdmissionApplicationRepository;
import com.rtms.backend.repository.CandidateHorseProfileRepository;
import com.rtms.backend.repository.HorsePedigreeRepository;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.CareScheduleRepository;
import com.rtms.backend.repository.StableStallRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdmissionGroomReviewServiceTest {

    @Mock
    private AdmissionApplicationRepository admissionApplicationRepository;

    @Mock
    private CandidateHorseProfileRepository candidateHorseProfileRepository;

    @Mock
    private StableStallRepository stableStallRepository;

    @Mock
    private HorseRepository horseRepository;

    @Mock
    private HorsePedigreeRepository horsePedigreeRepository;

    @Mock
    private CareScheduleRepository careScheduleRepository;

    @Mock
    private CareScheduleService careScheduleService;

    @Mock
    private HeadTrainerWorkloadService headTrainerWorkloadService;

    @Mock
    private NotificationService notificationService;

    private AdmissionGroomReviewService service;

    @BeforeEach
    void setUp() {
        service = new AdmissionGroomReviewService(
                admissionApplicationRepository,
                candidateHorseProfileRepository,
                stableStallRepository,
                horseRepository,
                horsePedigreeRepository,
                careScheduleRepository,
                careScheduleService,
                headTrainerWorkloadService,
                notificationService);
        lenient().when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Groom reject: Admission REJECTED và không tạo Horse")
    void review_reject_success() {
        AdmissionApplication admission = admission();
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.review(1L, 7L, request(ReviewDecision.REJECTED, "Missing documents"));

        assertEquals(AdmissionStatus.REJECTED, result.getStatus());
        assertEquals(7L, result.getGroomId());
        assertEquals(ReviewDecision.REJECTED, result.getGroomDecision());
        assertEquals("Missing documents", result.getGroomFeedback());
        assertNotNull(result.getGroomReviewedAt());
        assertNull(result.getHorseId());
        verifyNoInteractions(candidateHorseProfileRepository, horseRepository, horsePedigreeRepository,
                careScheduleRepository, stableStallRepository, headTrainerWorkloadService, notificationService);
    }

    @Test
    @DisplayName("Groom approve thiếu capacity: Admission WAITING_FOR_STALL và chưa tạo Horse")
    void review_approveWithoutCapacity_waitingForStall() {
        AdmissionApplication admission = admission();
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(stableStallRepository.countAvailableQuarantineStalls()).thenReturn(0L);
        when(stableStallRepository.countAvailableRegularStalls()).thenReturn(10L);
        when(stableStallRepository.countOccupiedQuarantineStalls()).thenReturn(1L);
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.review(1L, 7L, request(ReviewDecision.APPROVED, "OK"));

        assertEquals(AdmissionStatus.WAITING_FOR_STALL, result.getStatus());
        assertEquals(ReviewDecision.APPROVED, result.getGroomDecision());
        assertNull(result.getHorseId());
        verify(stableStallRepository).lockAdmissionCapacityStallsForUpdate();
        verifyNoInteractions(candidateHorseProfileRepository, horseRepository, horsePedigreeRepository,
                careScheduleRepository, headTrainerWorkloadService, notificationService);
    }

    @Test
    @DisplayName("Groom approve thiếu regular reserve vẫn duyệt nhưng chờ stall")
    void review_approveWithoutRegularReserve_waitingForStall() {
        AdmissionApplication admission = admission();
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(stableStallRepository.countAvailableQuarantineStalls()).thenReturn(2L);
        when(stableStallRepository.countAvailableRegularStalls()).thenReturn(1L);
        when(stableStallRepository.countOccupiedQuarantineStalls()).thenReturn(1L);
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.review(1L, 7L,
                request(ReviewDecision.APPROVED, "Documents verified"));

        assertEquals(AdmissionStatus.WAITING_FOR_STALL, result.getStatus());
        assertEquals(ReviewDecision.APPROVED, result.getGroomDecision());
        verifyNoInteractions(candidateHorseProfileRepository, horseRepository, horsePedigreeRepository,
                careScheduleRepository, headTrainerWorkloadService, notificationService);
    }

    @Test
    @DisplayName("Groom review rejects blank feedback for both decisions")
    void review_blankFeedback_isRejected() {
        for (ReviewDecision decision : List.of(ReviewDecision.APPROVED, ReviewDecision.REJECTED)) {
            AdmissionApplication admission = admission();
            when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));

            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> service.review(1L, 7L, request(decision, "  \t ")));

            assertEquals(400, exception.getStatusCode().value());
        }
        verify(admissionApplicationRepository, never()).save(any());
        verifyNoInteractions(stableStallRepository, candidateHorseProfileRepository, horseRepository,
                horsePedigreeRepository, careScheduleRepository, headTrainerWorkloadService, notificationService);
    }

    @Test
    @DisplayName("Groom review trims feedback before persisting")
    void review_trimsFeedback() {
        AdmissionApplication admission = admission();
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.review(1L, 7L,
                request(ReviewDecision.REJECTED, "  Missing vaccination record  "));

        assertEquals("Missing vaccination record", result.getGroomFeedback());
    }

    @Test
    @DisplayName("Groom approve đủ capacity: tạo Horse CANDIDATE, chiếm Q stall, tạo INITIAL CareSchedule, gán Vet, gán Trainer và gửi notifications")
    void review_approveWithCapacity_createsCandidateHorseAndAssignsVetAndTrainer() {
        AdmissionApplication admission = admission();
        CandidateHorseProfile candidate = candidate();
        StableStall qStall = stall(99L);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        mockCapacityAvailable();
        when(candidateHorseProfileRepository.findByAdmissionId(1L)).thenReturn(Optional.of(candidate));
        when(stableStallRepository.findFirstAvailableQuarantineStallForUpdate()).thenReturn(Optional.of(qStall));
        when(horseRepository.findByRegistrationNumber("FR1234567890123")).thenReturn(List.of());
        when(horseRepository.save(any(Horse.class))).thenAnswer(inv -> {
            Horse horse = inv.getArgument(0);
            horse.setId(20L);
            return horse;
        });
        when(horsePedigreeRepository.findByHorseId(20L)).thenReturn(Optional.empty());

        // Initial CareSchedule creation
        CareSchedule createdSchedule = new CareSchedule();
        createdSchedule.setId(100L);
        createdSchedule.setHorseId(20L);
        createdSchedule.setAdmissionId(1L);
        createdSchedule.setCareType(CareType.INITIAL);
        createdSchedule.setStatus(CareScheduleStatus.SCHEDULED);
        createdSchedule.setVeterinarianId(15L); // Vet 15 assigned

        when(careScheduleRepository.findFirstByAdmissionIdAndCareTypeOrderByCreatedAtDesc(1L, CareType.INITIAL))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(createdSchedule));
        when(careScheduleRepository.save(any(CareSchedule.class))).thenReturn(createdSchedule);

        // Least loaded Head Trainer selection
        when(headTrainerWorkloadService.selectLeastLoadedHeadTrainerId()).thenReturn(Optional.of(8L));

        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.review(1L, 7L, request(ReviewDecision.APPROVED, "Ready"));

        // 1. Trạng thái Admission
        assertEquals(AdmissionStatus.VET_REVIEW, result.getStatus());
        assertEquals(20L, result.getHorseId());
        assertEquals(99L, result.getQuarantineStallId());
        assertEquals(StallStatus.OCCUPIED, qStall.getStatus());

        // 2. Gán Trainer vào AdmissionApplication.trainerId
        assertEquals(8L, result.getTrainerId());

        // 3. Horse CANDIDATE
        ArgumentCaptor<Horse> horseCaptor = ArgumentCaptor.forClass(Horse.class);
        verify(horseRepository).save(horseCaptor.capture());
        Horse savedHorse = horseCaptor.getValue();
        assertEquals("QABALAH MERCURY", savedHorse.getName());
        assertEquals("Arabian", savedHorse.getBreed());
        assertEquals(HorseStatus.CANDIDATE, savedHorse.getCurrentStatus());
        assertTrue(savedHorse.isTrainingLocked());
        assertEquals(TrainingDecision.BLOCKED, savedHorse.getTrainingStatus());
        assertEquals(99L, savedHorse.getCurrentStallId());
        assertEquals(5L, savedHorse.getOwnerId());
        assertEquals("FR1234567890123", savedHorse.getRegistrationNumber());

        // 4. CareSchedule INITIAL & gán Vet
        verify(careScheduleService).assignRequestedSchedules();

        // 5. Notifications cho cả Vet và Trainer
        verify(notificationService).sendAssignmentNotification(
                eq(15L), eq(1L), eq(20L), eq(NotificationTypes.ADMISSION_VET_ASSIGNED), anyString(), anyString());
        verify(notificationService).sendAssignmentNotification(
                eq(8L), eq(1L), eq(20L), eq(NotificationTypes.ADMISSION_TRAINER_ASSIGNED), anyString(), anyString());
    }

    @Test
    @DisplayName("Retry Groom confirm không tạo trùng CareSchedule, không đổi Trainer đã gán, không duplicate notification")
    void review_retry_idempotent_preservesAssignments() {
        AdmissionApplication admission = admission();
        admission.setTrainerId(8L); // Trainer đã gán từ trước
        CandidateHorseProfile candidate = candidate();
        StableStall qStall = stall(99L);

        CareSchedule existingSchedule = new CareSchedule();
        existingSchedule.setId(100L);
        existingSchedule.setHorseId(20L);
        existingSchedule.setAdmissionId(1L);
        existingSchedule.setCareType(CareType.INITIAL);
        existingSchedule.setStatus(CareScheduleStatus.SCHEDULED);
        existingSchedule.setVeterinarianId(15L);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        mockCapacityAvailable();
        when(candidateHorseProfileRepository.findByAdmissionId(1L)).thenReturn(Optional.of(candidate));
        when(stableStallRepository.findFirstAvailableQuarantineStallForUpdate()).thenReturn(Optional.of(qStall));
        when(horseRepository.findByRegistrationNumber("FR1234567890123")).thenReturn(List.of());
        when(horseRepository.save(any(Horse.class))).thenAnswer(inv -> {
            Horse horse = inv.getArgument(0);
            horse.setId(20L);
            return horse;
        });
        when(horsePedigreeRepository.findByHorseId(20L)).thenReturn(Optional.empty());

        // Existing schedule found -> not recreated
        when(careScheduleRepository.findFirstByAdmissionIdAndCareTypeOrderByCreatedAtDesc(1L, CareType.INITIAL))
                .thenReturn(Optional.of(existingSchedule));

        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.review(1L, 7L, request(ReviewDecision.APPROVED, "Retry approve"));

        // TrainerId cũ 8L được giữ nguyên, không gọi lại service chọn trainer
        assertEquals(8L, result.getTrainerId());
        verifyNoInteractions(headTrainerWorkloadService);

        // Không tạo mới schedule
        verify(careScheduleRepository, never()).save(any(CareSchedule.class));

        // Notifications được gọi qua dedup service
        verify(notificationService).sendAssignmentNotification(
                eq(15L), eq(1L), eq(20L), eq(NotificationTypes.ADMISSION_VET_ASSIGNED), anyString(), anyString());
        verify(notificationService).sendAssignmentNotification(
                eq(8L), eq(1L), eq(20L), eq(NotificationTypes.ADMISSION_TRAINER_ASSIGNED), anyString(), anyString());
    }

    @Test
    @DisplayName("WAITING_FOR_STALL retry: gán Trainer và Vet khi capacity đủ")
    void processWaitingForStall_success() {
        AdmissionApplication admission = admission();
        admission.setStatus(AdmissionStatus.WAITING_FOR_STALL);
        admission.setGroomId(7L);
        admission.setGroomDecision(ReviewDecision.APPROVED);
        CandidateHorseProfile candidate = candidate();
        StableStall qStall = stall(99L);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        mockCapacityAvailable();
        when(candidateHorseProfileRepository.findByAdmissionId(1L)).thenReturn(Optional.of(candidate));
        when(stableStallRepository.findFirstAvailableQuarantineStallForUpdate()).thenReturn(Optional.of(qStall));
        when(horseRepository.findByRegistrationNumber("FR1234567890123")).thenReturn(List.of());
        when(horseRepository.save(any(Horse.class))).thenAnswer(inv -> {
            Horse horse = inv.getArgument(0);
            horse.setId(20L);
            return horse;
        });
        when(headTrainerWorkloadService.selectLeastLoadedHeadTrainerId()).thenReturn(Optional.of(12L));
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.processWaitingForStall(1L);

        assertEquals(AdmissionStatus.VET_REVIEW, result.getStatus());
        assertEquals(7L, result.getGroomId());
        assertEquals(ReviewDecision.APPROVED, result.getGroomDecision());
        assertEquals(20L, result.getHorseId());
        assertEquals(12L, result.getTrainerId());
    }

    @Test
    @DisplayName("Reuse Horse REJECTED cùng owner theo UELN")
    void review_reusesRejectedHorseSameOwner() {
        AdmissionApplication admission = admission();
        CandidateHorseProfile candidate = candidate();
        StableStall qStall = stall(99L);
        Horse existingHorse = new Horse();
        existingHorse.setId(20L);
        existingHorse.setOwnerId(5L);
        existingHorse.setCurrentStatus(HorseStatus.REJECTED);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        mockCapacityAvailable();
        when(candidateHorseProfileRepository.findByAdmissionId(1L)).thenReturn(Optional.of(candidate));
        when(stableStallRepository.findFirstAvailableQuarantineStallForUpdate()).thenReturn(Optional.of(qStall));
        when(horseRepository.findByRegistrationNumber("FR1234567890123")).thenReturn(List.of(existingHorse));
        when(horseRepository.save(existingHorse)).thenReturn(existingHorse);
        when(horsePedigreeRepository.findByHorseId(20L)).thenReturn(Optional.of(new HorsePedigree()));
        when(careScheduleRepository.findFirstByAdmissionIdAndCareTypeOrderByCreatedAtDesc(1L, CareType.INITIAL))
                .thenReturn(Optional.empty());
        when(headTrainerWorkloadService.selectLeastLoadedHeadTrainerId()).thenReturn(Optional.of(3L));
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.review(1L, 7L, request(ReviewDecision.APPROVED, "Verified"));

        assertEquals(AdmissionStatus.VET_REVIEW, result.getStatus());
        assertEquals(20L, result.getHorseId());
        assertEquals(3L, result.getTrainerId());
        assertEquals(HorseStatus.CANDIDATE, existingHorse.getCurrentStatus());
        assertTrue(existingHorse.isTrainingLocked());
        assertEquals(99L, existingHorse.getCurrentStallId());
    }

    @Test
    @DisplayName("Active Horse cùng UELN: không tạo duplicate")
    void review_existingActiveHorseSameUeln_throws() {
        AdmissionApplication admission = admission();
        CandidateHorseProfile candidate = candidate();
        StableStall qStall = stall(99L);
        Horse existingHorse = new Horse();
        existingHorse.setId(20L);
        existingHorse.setOwnerId(5L);
        existingHorse.setCurrentStatus(HorseStatus.CANDIDATE);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        mockCapacityAvailable();
        when(candidateHorseProfileRepository.findByAdmissionId(1L)).thenReturn(Optional.of(candidate));
        when(stableStallRepository.findFirstAvailableQuarantineStallForUpdate()).thenReturn(Optional.of(qStall));
        when(horseRepository.findByRegistrationNumber("FR1234567890123")).thenReturn(List.of(existingHorse));

        assertThrows(ResponseStatusException.class,
                () -> service.review(1L, 7L, request(ReviewDecision.APPROVED, "Verified")));

        verify(horseRepository, never()).save(any());
        verify(admissionApplicationRepository, never()).save(any());
    }

    private void mockCapacityAvailable() {
        when(stableStallRepository.countAvailableQuarantineStalls()).thenReturn(1L);
        when(stableStallRepository.countAvailableRegularStalls()).thenReturn(2L);
        when(stableStallRepository.countOccupiedQuarantineStalls()).thenReturn(1L);
    }

    private AdmissionApplication admission() {
        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(1L);
        admission.setOwnerId(5L);
        admission.setStatus(AdmissionStatus.GROOM_REVIEW);
        return admission;
    }

    private CandidateHorseProfile candidate() {
        CandidateHorseProfile candidate = new CandidateHorseProfile();
        candidate.setId(11L);
        candidate.setAdmissionId(1L);
        candidate.setName("QABALAH MERCURY");
        candidate.setBreed("Arabian");
        candidate.setDateOfBirth(LocalDate.of(2021, 3, 4));
        candidate.setRegistrationNumber("FR1234567890123");
        candidate.setRegistryName("SIRE");
        candidate.setSireName("Mercury Sire");
        candidate.setSireRegistrationNumber("FR1234567890001");
        candidate.setDamName("Mercury Dam");
        candidate.setDamRegistrationNumber("FR1234567890002");
        candidate.setPedigreeNotes("Clean pedigree snapshot");
        return candidate;
    }

    private StableStall stall(Long id) {
        StableStall stall = new StableStall();
        stall.setId(id);
        stall.setStatus(StallStatus.AVAILABLE);
        return stall;
    }

    private GroomAdmissionReviewRequest request(ReviewDecision decision, String feedback) {
        GroomAdmissionReviewRequest request = new GroomAdmissionReviewRequest();
        request.setDecision(decision);
        request.setFeedback(feedback);
        return request;
    }
}
