package com.rtms.backend.medical.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.medical.dto.VaccinationRequest;
import com.rtms.backend.medical.entity.Prescription;
import com.rtms.backend.medical.entity.PreventiveProtocol;
import com.rtms.backend.medical.entity.TreatmentPlan;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.enums.ProtocolCategory;
import com.rtms.backend.medical.enums.VaccinationSource;
import com.rtms.backend.medical.repository.PrescriptionRepository;
import com.rtms.backend.medical.repository.PreventiveProtocolRepository;
import com.rtms.backend.medical.repository.TreatmentPlanRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PreventiveVaccinationServiceTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 18);

    @Mock PreventiveProtocolRepository protocols;
    @Mock TreatmentPlanRepository plans;
    @Mock PrescriptionRepository prescriptions;
    @Mock PreventiveStatusService status;

    private PreventiveVaccinationService service;
    private final PreventiveProtocol ei = protocol(1L, "EI", true);
    private final PreventiveProtocol retired = protocol(9L, "OLD", false);

    @BeforeEach
    void setUp() {
        service = new PreventiveVaccinationService(protocols, plans, prescriptions, status,
                Clock.fixed(TODAY.atTime(9, 0).atZone(ZONE).toInstant(), ZONE));
        when(protocols.findById(1L)).thenReturn(Optional.of(ei));
        when(protocols.findById(9L)).thenReturn(Optional.of(retired));
        when(protocols.findById(404L)).thenReturn(Optional.empty());
        when(plans.save(any())).thenAnswer(call -> {
            TreatmentPlan plan = call.getArgument(0);
            plan.setId(55L);
            return plan;
        });
    }

    // ---- validate ----

    @Test
    void administeredDefaultsToToday() {
        var validated = service.validate(List.of(entry(1L, null, null)), CareType.ROUTINE);

        assertEquals(1, validated.size());
        assertEquals(TODAY, validated.get(0).administeredOn());
        assertEquals(VaccinationSource.ADMINISTERED, validated.get(0).source());
    }

    @Test
    void administeredDoseMustBeDatedToday() {
        assertCode("INVALID_VACCINATION", List.of(entry(1L, VaccinationSource.ADMINISTERED, TODAY.minusDays(3))), CareType.ROUTINE);
        assertCode("INVALID_VACCINATION", List.of(entry(1L, null, TODAY.plusDays(1))), CareType.ROUTINE);
    }

    @Test
    void historicalDoseOnlyDuringAdmissionAndOnlyInThePast() {
        VaccinationRequest past = entry(1L, VaccinationSource.HISTORICAL, TODAY.minusDays(120));
        assertEquals(1, service.validate(List.of(past), CareType.INITIAL).size());

        assertCode("INVALID_VACCINATION", List.of(past), CareType.ROUTINE);
        assertCode("INVALID_VACCINATION", List.of(entry(1L, VaccinationSource.HISTORICAL, TODAY)), CareType.INITIAL);
        assertCode("INVALID_VACCINATION", List.of(entry(1L, VaccinationSource.HISTORICAL, null)), CareType.INITIAL);
    }

    @Test
    void sameProtocolTwiceInOneExamIsRejectedButHistoryMayRepeat() {
        assertCode("INVALID_VACCINATION", List.of(entry(1L, null, null), entry(1L, null, null)), CareType.ROUTINE);

        var history = service.validate(List.of(
                entry(1L, VaccinationSource.HISTORICAL, TODAY.minusDays(300)),
                entry(1L, VaccinationSource.HISTORICAL, TODAY.minusDays(120)),
                entry(1L, VaccinationSource.ADMINISTERED, null)), CareType.INITIAL);
        assertEquals(3, history.size());
    }

    @Test
    void unknownOrInactiveProtocolIsRejected() {
        assertCode("PROTOCOL_NOT_FOUND", List.of(entry(404L, null, null)), CareType.ROUTINE);
        assertCode("PROTOCOL_NOT_FOUND", List.of(entry(9L, null, null)), CareType.ROUTINE);
    }

    @Test
    void dosageIsRequired() {
        VaccinationRequest noDose = entry(1L, null, null);
        noDose.setDosage("  ");
        assertCode("INVALID_VACCINATION", List.of(noDose), CareType.ROUTINE);
    }

    @Test
    void emptyListNeedsNoLookup() {
        assertTrue(service.validate(null, CareType.ROUTINE).isEmpty());
        assertTrue(service.validate(List.of(), CareType.ROUTINE).isEmpty());
        verifyNoInteractions(protocols);
    }

    // ---- record ----

    @Test
    void recordsOnePlanWithOnePrescriptionPerDoseLinkedToItsProtocol() {
        VaccinationRequest today = entry(1L, null, null);
        today.setRoute(" SC ");
        today.setInstructions("Theo dõi sốt 24h");
        VaccinationRequest history = entry(1L, VaccinationSource.HISTORICAL, TODAY.minusDays(120));
        var validated = service.validate(List.of(history, today), CareType.INITIAL);

        service.record(999L, validated);

        ArgumentCaptor<TreatmentPlan> plan = ArgumentCaptor.forClass(TreatmentPlan.class);
        verify(plans).save(plan.capture());
        assertEquals(999L, plan.getValue().getHealthRecordId());
        assertEquals(PreventiveVaccinationService.PLAN_NAME, plan.getValue().getTreatmentName());
        assertEquals(TODAY.minusDays(120), plan.getValue().getStartDate());
        assertEquals(TODAY, plan.getValue().getEndDate());
        assertEquals("COMPLETED", plan.getValue().getStatus());

        ArgumentCaptor<Prescription> saved = ArgumentCaptor.forClass(Prescription.class);
        verify(prescriptions, times(2)).save(saved.capture());
        Prescription old = saved.getAllValues().get(0);
        Prescription now = saved.getAllValues().get(1);

        assertEquals(55L, old.getTreatmentPlanId());
        assertEquals(1L, old.getPreventiveProtocolId());
        assertEquals("EI", old.getMedicationName());
        assertEquals("ONCE", old.getFrequency());
        assertEquals("IM", old.getRoute(), "route falls back to the protocol default");
        assertEquals(TODAY.minusDays(120), old.getStartDate());
        assertEquals(old.getStartDate(), old.getEndDate());
        assertEquals("[HISTORICAL]", old.getInstructions());

        assertEquals("SC", now.getRoute());
        assertEquals(TODAY, now.getStartDate());
        assertEquals("Theo dõi sốt 24h", now.getInstructions());
    }

    @Test
    void recordingNothingWritesNothing() {
        service.record(999L, List.of());
        verifyNoInteractions(plans, prescriptions);
    }

    // ---- refreshMilestone ----

    @Test
    void admissionExamIgnoresMissingProtocolsOtherExamsTreatThemAsDueNow() {
        service.refreshMilestone(10L, 999L, 20L, CareType.INITIAL);
        verify(status).upsertMilestone(10L, "preventive:999", 20L, null);

        service.refreshMilestone(10L, 1000L, 20L, CareType.ROUTINE);
        verify(status).upsertMilestone(10L, "preventive:1000", 20L, TODAY);
    }

    // ---- helpers ----

    private void assertCode(String code, List<VaccinationRequest> requests, CareType careType) {
        ApiException error = assertThrows(ApiException.class, () -> service.validate(requests, careType));
        assertEquals(code, error.getErrorCode());
    }

    private static VaccinationRequest entry(long protocolId, VaccinationSource source, LocalDate on) {
        VaccinationRequest request = new VaccinationRequest();
        request.setProtocolId(protocolId);
        request.setDosage("1 liều");
        request.setSource(source);
        request.setAdministeredOn(on);
        return request;
    }

    private static PreventiveProtocol protocol(long id, String code, boolean active) {
        PreventiveProtocol protocol = new PreventiveProtocol();
        protocol.setId(id);
        protocol.setCode(code);
        protocol.setName(code);
        protocol.setCategory(ProtocolCategory.VACCINE);
        protocol.setIntervalDays(180);
        protocol.setWarningDays(14);
        protocol.setMandatory(true);
        protocol.setActive(active);
        protocol.setDefaultRoute("IM");
        return protocol;
    }
}
