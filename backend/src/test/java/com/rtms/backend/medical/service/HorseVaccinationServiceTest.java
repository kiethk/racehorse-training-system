package com.rtms.backend.medical.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.dto.HorseVaccinationRequest;
import com.rtms.backend.medical.dto.HorseVaccinationResult;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.entity.PreventiveProtocol;
import com.rtms.backend.medical.enums.ProtocolCategory;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.medical.repository.PreventiveProtocolRepository;
import com.rtms.backend.training.enums.TrainingDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HorseVaccinationServiceTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 18);

    @Mock PreventiveProtocolRepository protocols;
    @Mock HorseRepository horses;
    @Mock HealthRecordRepository healthRecords;
    @Mock PreventiveVaccinationService vaccinations;
    @Mock PreventiveStatusService preventiveStatus;

    private HorseVaccinationService service;
    private Horse horse;

    @BeforeEach
    void setUp() {
        service = new HorseVaccinationService(protocols, horses, healthRecords, vaccinations, preventiveStatus,
                Clock.fixed(TODAY.atTime(9, 0).atZone(ZONE).toInstant(), ZONE));
        when(protocols.findById(1L)).thenReturn(Optional.of(protocol(1L, "Equine Influenza")));
        when(protocols.findById(6L)).thenReturn(Optional.of(protocol(6L, "Deworming")));
        when(protocols.findById(404L)).thenReturn(Optional.empty());
        horse = new Horse();
        horse.setId(5L);
        horse.setName("Booster Winner");
        horse.setCurrentStatus(HorseStatus.ELIGIBLE);
        horse.setTrainingDecision(TrainingDecision.BLOCKED);
        when(horses.findByIdForUpdate(5L)).thenReturn(Optional.of(horse));
        when(healthRecords.save(any())).thenAnswer(call -> {
            HealthRecord record = call.getArgument(0);
            record.setId(501L);
            return record;
        });
    }

    @Test
    void recordsSeveralProtocolsOnOneVaccinationRecordWithoutTouchingTraining() {
        HorseVaccinationResult result = service.record(5L, request(null, dose(1L), dose(6L)), 20L);

        assertEquals(new HorseVaccinationResult(5L, 501L), result);
        ArgumentCaptor<HealthRecord> saved = ArgumentCaptor.forClass(HealthRecord.class);
        verify(healthRecords).save(saved.capture());
        assertEquals(HorseVaccinationService.RECORD_TYPE, saved.getValue().getRecordType());
        assertNull(saved.getValue().getCareScheduleId());
        assertNull(saved.getValue().getTrainingDecision());
        assertEquals("Equine Influenza, Deworming", saved.getValue().getTreatment());
        assertEquals(TrainingDecision.BLOCKED, horse.getTrainingDecision(), "vaccinating must not unblock a horse");
        verify(vaccinations).record(eq(501L), argThat(doses -> doses.size() == 2
                && doses.stream().allMatch(d -> d.administeredOn().equals(TODAY))));
        verify(preventiveStatus).upsertMilestone(5L, "preventive:501", 20L, TODAY);
    }

    @Test
    void earlierDayCanBeEnteredButNotAFutureOne() {
        service.record(5L, request(TODAY.minusDays(2), dose(1L)), 20L);
        verify(vaccinations).record(eq(501L), argThat(doses -> doses.get(0).administeredOn().equals(TODAY.minusDays(2))));

        ApiException future = assertThrows(ApiException.class,
                () -> service.record(5L, request(TODAY.plusDays(1), dose(1L)), 20L));
        assertEquals("INVALID_VACCINATION", future.getErrorCode());
    }

    @Test
    void invalidDosesAreRejectedBeforeAnyWrite() {
        assertCode("INVALID_VACCINATION", request(null));
        assertCode("INVALID_VACCINATION", request(null, dose(1L), dose(1L)));
        HorseVaccinationRequest.Dose noDosage = dose(1L);
        noDosage.setDosage(" ");
        assertCode("INVALID_VACCINATION", request(null, noDosage));
        assertCode("PROTOCOL_NOT_FOUND", request(null, dose(404L)));
        verifyNoInteractions(healthRecords, vaccinations, preventiveStatus);
    }

    @Test
    void candidateInQuarantineDoesNotGetAMissingProtocolBookedYet() {
        horse.setCurrentStatus(HorseStatus.CANDIDATE);

        service.record(5L, request(null, dose(1L)), 20L);

        verify(preventiveStatus).upsertMilestone(5L, "preventive:501", 20L, null);
    }

    @Test
    void horseOutsideTheStableIsRejected() {
        horse.setCurrentStatus(HorseStatus.REJECTED);
        assertEquals("HORSE_NOT_IN_STABLE",
                assertThrows(ApiException.class, () -> service.record(5L, request(null, dose(1L)), 20L)).getErrorCode());

        assertEquals("HORSE_NOT_FOUND",
                assertThrows(ApiException.class, () -> service.record(99L, request(null, dose(1L)), 20L)).getErrorCode());
        verifyNoInteractions(healthRecords, vaccinations, preventiveStatus);
    }

    private void assertCode(String code, HorseVaccinationRequest request) {
        assertEquals(code, assertThrows(ApiException.class, () -> service.record(5L, request, 20L)).getErrorCode());
    }

    private static HorseVaccinationRequest request(LocalDate on, HorseVaccinationRequest.Dose... doses) {
        HorseVaccinationRequest request = new HorseVaccinationRequest();
        request.setAdministeredOn(on);
        request.setDoses(new ArrayList<>(List.of(doses)));
        return request;
    }

    private static HorseVaccinationRequest.Dose dose(long protocolId) {
        HorseVaccinationRequest.Dose dose = new HorseVaccinationRequest.Dose();
        dose.setProtocolId(protocolId);
        dose.setDosage("1 dose");
        dose.setRoute("IM");
        return dose;
    }

    private static PreventiveProtocol protocol(long id, String name) {
        PreventiveProtocol protocol = new PreventiveProtocol();
        protocol.setId(id);
        protocol.setCode(name.toUpperCase());
        protocol.setName(name);
        protocol.setCategory(ProtocolCategory.VACCINE);
        protocol.setActive(true);
        return protocol;
    }
}
