package com.rtms.backend.service;
import com.rtms.backend.dto.GroomAdmissionReviewRequest;
import com.rtms.backend.dto.GroomArrivalConfirmationRequest;
import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.entity.CandidateHorseProfile;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.enums.ArrivalStatus;
import com.rtms.backend.enums.ReviewDecision;
import com.rtms.backend.repository.AdmissionApplicationRepository;
import com.rtms.backend.repository.CandidateHorseProfileRepository;
import com.rtms.backend.repository.CareScheduleRepository;
import com.rtms.backend.service.CareScheduleService;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.enums.HorseStatus;
import com.rtms.backend.repository.HorsePedigreeRepository;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.entity.StableStall;
import com.rtms.backend.enums.StallStatus;
import com.rtms.backend.repository.StableStallRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
                careScheduleService);
    }

    @Test
    void reviewRejectsAdmissionWithoutCreatingHorse() {
        AdmissionApplication admission = admission();
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(admissionApplicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AdmissionApplication result = service.review(1L, 7L, reviewRequest(ReviewDecision.REJECTED, "Missing documents"));

        assertEquals(AdmissionStatus.REJECTED, result.getStatus());
        assertEquals(7L, result.getGroomId());
        assertEquals(ReviewDecision.REJECTED, result.getGroomDecision());
        assertNull(result.getHorseId());
        verifyNoInteractions(candidateHorseProfileRepository, horseRepository, horsePedigreeRepository,
                careScheduleRepository, stableStallRepository);
    }

    @Test
    void reviewApprovalWithoutCapacityWaitsForStall() {
        AdmissionApplication admission = admission();
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(stableStallRepository.countAvailableQuarantineStalls()).thenReturn(0L);
        when(stableStallRepository.countAvailableRegularStalls()).thenReturn(10L);
        when(stableStallRepository.countOccupiedQuarantineStalls()).thenReturn(1L);
        when(admissionApplicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AdmissionApplication result = service.review(1L, 7L, reviewRequest(ReviewDecision.APPROVED, "Ready"));

        assertEquals(AdmissionStatus.WAITING_FOR_STALL, result.getStatus());
        assertNull(result.getHorseId());
        verify(stableStallRepository).lockAdmissionCapacityStallsForUpdate();
        verifyNoInteractions(candidateHorseProfileRepository, horseRepository, horsePedigreeRepository,
                careScheduleRepository, careScheduleService);
    }

    @Test
    void reviewApprovalWithCapacityReservesStallAndWaitsForArrival() {
        AdmissionApplication admission = admission();
        StableStall qStall = stall(99L);
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        mockCapacityAvailable();
        when(stableStallRepository.findFirstAvailableQuarantineStallForUpdate()).thenReturn(Optional.of(qStall));
        when(admissionApplicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AdmissionApplication result = service.review(1L, 7L, reviewRequest(ReviewDecision.APPROVED, "Ready"));

        assertEquals(AdmissionStatus.WAITING_FOR_ARRIVAL, result.getStatus());
        assertEquals(ArrivalStatus.PENDING, result.getArrivalStatus());
        assertNull(result.getHorseId());
        assertEquals(99L, result.getQuarantineStallId());
        assertEquals(StallStatus.OCCUPIED, qStall.getStatus());
        verifyNoInteractions(candidateHorseProfileRepository, horseRepository, horsePedigreeRepository,
                careScheduleRepository, careScheduleService);
    }

    @Test
    void processWaitingForStallReservesStallWithoutCreatingHorse() {
        AdmissionApplication admission = admission();
        admission.setStatus(AdmissionStatus.WAITING_FOR_STALL);
        admission.setGroomId(7L);
        admission.setGroomDecision(ReviewDecision.APPROVED);
        StableStall qStall = stall(99L);
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        mockCapacityAvailable();
        when(stableStallRepository.findFirstAvailableQuarantineStallForUpdate()).thenReturn(Optional.of(qStall));
        when(admissionApplicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AdmissionApplication result = service.processWaitingForStall(1L);

        assertEquals(AdmissionStatus.WAITING_FOR_ARRIVAL, result.getStatus());
        assertEquals(7L, result.getGroomId());
        assertNull(result.getHorseId());
        verifyNoInteractions(candidateHorseProfileRepository, horseRepository, horsePedigreeRepository,
                careScheduleRepository, careScheduleService);
    }

    @Test
    void confirmArrivalCreatesHorseAndInitialSchedule() {
        AdmissionApplication admission = admission();
        admission.setStatus(AdmissionStatus.WAITING_FOR_ARRIVAL);
        admission.setQuarantineStallId(99L);
        CandidateHorseProfile candidate = candidate();
        StableStall qStall = stall(99L);
        qStall.setStatus(StallStatus.OCCUPIED);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(candidateHorseProfileRepository.findByAdmissionId(1L)).thenReturn(Optional.of(candidate));
        when(stableStallRepository.findQuarantineStallByIdForUpdate(99L)).thenReturn(Optional.of(qStall));
        when(horseRepository.findByRegistrationNumber("FR1234567890123")).thenReturn(List.of());
        when(horseRepository.save(any(Horse.class))).thenAnswer(invocation -> {
            Horse horse = invocation.getArgument(0);
            horse.setId(20L);
            return horse;
        });
        when(horsePedigreeRepository.findByHorseId(20L)).thenReturn(Optional.empty());
        when(admissionApplicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AdmissionApplication result = service.confirmArrival(1L, 7L,
                new GroomArrivalConfirmationRequest(true, "Horse checked at gate"));

        assertEquals(AdmissionStatus.VET_REVIEW, result.getStatus());
        assertEquals(20L, result.getHorseId());
        assertEquals(ArrivalStatus.CONFIRMED, result.getArrivalStatus());
        assertEquals(7L, result.getArrivalConfirmedBy());
        verify(careScheduleService).createInitialSchedule(1L, 20L);
    }

    @Test
    void confirmArrivalRejectsMismatchAndReleasesStall() {
        AdmissionApplication admission = admission();
        admission.setStatus(AdmissionStatus.WAITING_FOR_ARRIVAL);
        admission.setQuarantineStallId(99L);
        StableStall qStall = stall(99L);
        qStall.setStatus(StallStatus.OCCUPIED);
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(stableStallRepository.findQuarantineStallByIdForUpdate(99L)).thenReturn(Optional.of(qStall));
        when(admissionApplicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AdmissionApplication result = service.confirmArrival(1L, 7L,
                new GroomArrivalConfirmationRequest(false, "UELN does not match the application"));

        assertEquals(AdmissionStatus.REJECTED, result.getStatus());
        assertNull(result.getQuarantineStallId());
        assertEquals("UELN does not match the application", result.getGroomFeedback());
        assertEquals(StallStatus.AVAILABLE, qStall.getStatus());
        verify(stableStallRepository).save(qStall);
        verifyNoInteractions(candidateHorseProfileRepository, horseRepository, horsePedigreeRepository,
                careScheduleRepository, careScheduleService);
    }

    @Test
    void confirmArrivalRejectsDuplicateActiveUeln() {
        AdmissionApplication admission = admission();
        admission.setStatus(AdmissionStatus.WAITING_FOR_ARRIVAL);
        admission.setQuarantineStallId(99L);
        CandidateHorseProfile candidate = candidate();
        StableStall qStall = stall(99L);
        qStall.setStatus(StallStatus.OCCUPIED);
        Horse existingHorse = new Horse();
        existingHorse.setId(20L);
        existingHorse.setCurrentStatus(com.rtms.backend.enums.HorseStatus.CANDIDATE);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(candidateHorseProfileRepository.findByAdmissionId(1L)).thenReturn(Optional.of(candidate));
        when(stableStallRepository.findQuarantineStallByIdForUpdate(99L)).thenReturn(Optional.of(qStall));
        when(horseRepository.findByRegistrationNumber("FR1234567890123")).thenReturn(List.of(existingHorse));

        assertThrows(ResponseStatusException.class, () -> service.confirmArrival(1L, 7L,
                new GroomArrivalConfirmationRequest(true, "Verified")));

        verify(horseRepository, never()).save(any());
        verify(admissionApplicationRepository, never()).save(any());
    }

    @Test
    void confirmationRejectsMissingMismatchFeedback() {
        AdmissionApplication admission = admission();
        admission.setStatus(AdmissionStatus.WAITING_FOR_ARRIVAL);
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.confirmArrival(1L, 7L, new GroomArrivalConfirmationRequest(false, "  ")));

        assertEquals(400, exception.getStatusCode().value());
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

    private GroomAdmissionReviewRequest reviewRequest(ReviewDecision decision, String feedback) {
        GroomAdmissionReviewRequest request = new GroomAdmissionReviewRequest();
        request.setDecision(decision);
        request.setFeedback(feedback);
        return request;
    }
}
