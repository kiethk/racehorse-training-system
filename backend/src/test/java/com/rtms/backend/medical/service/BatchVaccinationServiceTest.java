package com.rtms.backend.medical.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.dto.BatchVaccinationRequest;
import com.rtms.backend.medical.dto.BatchVaccinationResult;
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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BatchVaccinationServiceTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 18);

    @Mock PreventiveProtocolRepository protocols;
    @Mock HorseRepository horses;
    @Mock HealthRecordRepository healthRecords;
    @Mock PreventiveVaccinationService vaccinations;
    @Mock PreventiveStatusService preventiveStatus;

    private BatchVaccinationService service;
    private final Map<Long, Horse> stable = new HashMap<>();

    @BeforeEach
    void setUp() {
        service = new BatchVaccinationService(protocols, horses, healthRecords, vaccinations, preventiveStatus,
                Clock.fixed(TODAY.atTime(9, 0).atZone(ZONE).toInstant(), ZONE));
        PreventiveProtocol ei = new PreventiveProtocol();
        ei.setId(1L);
        ei.setCode("EI");
        ei.setName("Cúm ngựa");
        ei.setCategory(ProtocolCategory.VACCINE);
        ei.setActive(true);
        when(protocols.findById(1L)).thenReturn(Optional.of(ei));
        horse(3L, HorseStatus.ELIGIBLE, TrainingDecision.BLOCKED);
        horse(1L, HorseStatus.ELIGIBLE, TrainingDecision.ALLOWED);
        horse(2L, HorseStatus.CANDIDATE, TrainingDecision.ALLOWED);
        when(horses.findByIdForUpdate(anyLong())).thenAnswer(call -> Optional.ofNullable(stable.get(call.<Long>getArgument(0))));
        AtomicLong ids = new AtomicLong(500);
        when(healthRecords.save(any())).thenAnswer(call -> {
            HealthRecord record = call.getArgument(0);
            record.setId(ids.incrementAndGet());
            return record;
        });
    }

    @Test
    void eachHorseGetsAVaccinationRecordWithoutATrainingDecision() {
        List<BatchVaccinationResult> results = service.record(request(List.of(3L, 1L, 3L)), 20L);

        assertEquals(List.of(new BatchVaccinationResult(1L, 501L), new BatchVaccinationResult(3L, 502L)), results);
        ArgumentCaptor<HealthRecord> saved = ArgumentCaptor.forClass(HealthRecord.class);
        verify(healthRecords, times(2)).save(saved.capture());
        for (HealthRecord record : saved.getAllValues()) {
            assertEquals(BatchVaccinationService.RECORD_TYPE, record.getRecordType());
            assertNull(record.getCareScheduleId());
            assertNull(record.getTrainingDecision());
            assertEquals(20L, record.getVeterinarianId());
        }
        assertEquals(TrainingDecision.BLOCKED, stable.get(3L).getTrainingDecision(), "batch must not unblock a horse");
        verify(vaccinations).record(eq(501L), argThat(doses -> doses.size() == 1
                && doses.get(0).administeredOn().equals(TODAY)));
        verify(preventiveStatus).upsertMilestone(1L, "preventive:501", 20L, TODAY);
        verify(preventiveStatus).upsertMilestone(3L, "preventive:502", 20L, TODAY);
    }

    @Test
    void horsesAreLockedInIdOrder() {
        service.record(request(List.of(3L, 2L, 1L)), 20L);

        InOrder order = inOrder(horses);
        order.verify(horses).findByIdForUpdate(1L);
        order.verify(horses).findByIdForUpdate(2L);
        order.verify(horses).findByIdForUpdate(3L);
    }

    @Test
    void candidateInQuarantineDoesNotGetAMissingProtocolBookedYet() {
        service.record(request(List.of(2L)), 20L);

        verify(preventiveStatus).upsertMilestone(2L, "preventive:501", 20L, null);
    }

    @Test
    void horseOutsideTheStableFailsTheWholeBatch() {
        horse(4L, HorseStatus.REJECTED, TrainingDecision.ALLOWED);

        ApiException error = assertThrows(ApiException.class, () -> service.record(request(List.of(1L, 4L)), 20L));
        assertEquals("HORSE_NOT_IN_STABLE", error.getErrorCode());

        ApiException missing = assertThrows(ApiException.class, () -> service.record(request(List.of(99L)), 20L));
        assertEquals("HORSE_NOT_FOUND", missing.getErrorCode());
    }

    @Test
    void futureDateOrUnknownProtocolIsRejectedBeforeAnyWrite() {
        BatchVaccinationRequest future = request(List.of(1L));
        future.setAdministeredOn(TODAY.plusDays(1));
        assertThrows(ApiException.class, () -> service.record(future, 20L));

        BatchVaccinationRequest unknown = request(List.of(1L));
        unknown.setProtocolId(404L);
        when(protocols.findById(404L)).thenReturn(Optional.empty());
        assertEquals("PROTOCOL_NOT_FOUND",
                assertThrows(ApiException.class, () -> service.record(unknown, 20L)).getErrorCode());

        verifyNoInteractions(healthRecords, vaccinations, preventiveStatus);
    }

    @Test
    void earlierDayCanBeEnteredLater() {
        BatchVaccinationRequest yesterday = request(List.of(1L));
        yesterday.setAdministeredOn(TODAY.minusDays(1));

        service.record(yesterday, 20L);

        verify(vaccinations).record(eq(501L), argThat(doses -> doses.get(0).administeredOn().equals(TODAY.minusDays(1))));
    }

    private void horse(long id, HorseStatus status, TrainingDecision decision) {
        Horse horse = new Horse();
        horse.setId(id);
        horse.setName("Horse " + id);
        horse.setCurrentStatus(status);
        horse.setTrainingDecision(decision);
        stable.put(id, horse);
    }

    private static BatchVaccinationRequest request(List<Long> horseIds) {
        BatchVaccinationRequest request = new BatchVaccinationRequest();
        request.setHorseIds(horseIds);
        request.setProtocolId(1L);
        request.setDosage("1 liều");
        request.setRoute("IM");
        return request;
    }
}
