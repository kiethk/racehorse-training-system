package com.rtms.backend.service;

import com.rtms.backend.dto.GroomAdmissionReviewRequest;
import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.entity.CandidateHorseProfile;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.HorsePedigree;
import com.rtms.backend.entity.PreventiveCareSchedule;
import com.rtms.backend.entity.StableStall;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.enums.HorseStatus;
import com.rtms.backend.enums.ReviewDecision;
import com.rtms.backend.enums.StallStatus;
import com.rtms.backend.repository.AdmissionApplicationRepository;
import com.rtms.backend.repository.CandidateHorseProfileRepository;
import com.rtms.backend.repository.HorsePedigreeRepository;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.PreventiveCareScheduleRepository;
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
    private PreventiveCareScheduleRepository preventiveCareScheduleRepository;

    private AdmissionGroomReviewService service;

    @BeforeEach
    void setUp() {
        service = new AdmissionGroomReviewService(
                admissionApplicationRepository,
                candidateHorseProfileRepository,
                stableStallRepository,
                horseRepository,
                horsePedigreeRepository,
                preventiveCareScheduleRepository);
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
                preventiveCareScheduleRepository, stableStallRepository);
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
                preventiveCareScheduleRepository);
    }

    @Test
    @DisplayName("Groom approve đủ capacity: tạo Horse CANDIDATE, chiếm Q stall và tạo INITIAL_EXAM")
    void review_approveWithCapacity_createsCandidateHorse() {
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
        when(preventiveCareScheduleRepository.existsByHorseIdAndCareTypeAndStatusIn(
                eq(20L), eq("INITIAL_EXAM"), any())).thenReturn(false);
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.review(1L, 7L, request(ReviewDecision.APPROVED, "Ready"));

        assertEquals(AdmissionStatus.VET_REVIEW, result.getStatus());
        assertEquals(20L, result.getHorseId());
        assertEquals(99L, result.getQuarantineStallId());
        assertEquals(StallStatus.OCCUPIED, qStall.getStatus());

        ArgumentCaptor<Horse> horseCaptor = ArgumentCaptor.forClass(Horse.class);
        verify(horseRepository).save(horseCaptor.capture());
        Horse savedHorse = horseCaptor.getValue();
        assertEquals("QABALAH MERCURY", savedHorse.getName());
        assertEquals("Arabian", savedHorse.getBreed());
        assertEquals(HorseStatus.CANDIDATE, savedHorse.getCurrentStatus());
        assertEquals(99L, savedHorse.getCurrentStallId());
        assertEquals(5L, savedHorse.getOwnerId());
        assertEquals("FR1234567890123", savedHorse.getRegistrationNumber());

        ArgumentCaptor<HorsePedigree> pedigreeCaptor = ArgumentCaptor.forClass(HorsePedigree.class);
        verify(horsePedigreeRepository).save(pedigreeCaptor.capture());
        assertEquals("FR1234567890001", pedigreeCaptor.getValue().getSireRegistrationNumber());

        ArgumentCaptor<PreventiveCareSchedule> scheduleCaptor =
                ArgumentCaptor.forClass(PreventiveCareSchedule.class);
        verify(preventiveCareScheduleRepository).save(scheduleCaptor.capture());
        PreventiveCareSchedule schedule = scheduleCaptor.getValue();
        assertEquals(20L, schedule.getHorseId());
        assertEquals("INITIAL_EXAM", schedule.getCareType());
        assertEquals("PENDING", schedule.getStatus());
        assertNull(schedule.getScheduledDate());
    }

    @Test
    @DisplayName("WAITING_FOR_STALL retry: không review lại Groom, chỉ allocate khi capacity đủ")
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
        when(horsePedigreeRepository.findByHorseId(20L)).thenReturn(Optional.empty());
        when(preventiveCareScheduleRepository.existsByHorseIdAndCareTypeAndStatusIn(
                eq(20L), eq("INITIAL_EXAM"), any())).thenReturn(false);
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.processWaitingForStall(1L);

        assertEquals(AdmissionStatus.VET_REVIEW, result.getStatus());
        assertEquals(7L, result.getGroomId());
        assertEquals(ReviewDecision.APPROVED, result.getGroomDecision());
        assertEquals(20L, result.getHorseId());
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
        when(preventiveCareScheduleRepository.existsByHorseIdAndCareTypeAndStatusIn(
                eq(20L), eq("INITIAL_EXAM"), any())).thenReturn(true);
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.review(1L, 7L, request(ReviewDecision.APPROVED, null));

        assertEquals(AdmissionStatus.VET_REVIEW, result.getStatus());
        assertEquals(20L, result.getHorseId());
        assertEquals(HorseStatus.CANDIDATE, existingHorse.getCurrentStatus());
        assertEquals(99L, existingHorse.getCurrentStallId());
        verify(preventiveCareScheduleRepository, never()).save(any());
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
                () -> service.review(1L, 7L, request(ReviewDecision.APPROVED, null)));

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
