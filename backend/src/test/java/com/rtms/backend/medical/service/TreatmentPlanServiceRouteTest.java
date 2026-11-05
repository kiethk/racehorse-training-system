package com.rtms.backend.medical.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.medical.dto.CreatePrescriptionRequest;
import com.rtms.backend.medical.dto.CreateTreatmentPlanRequest;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.entity.TreatmentPlan;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.medical.repository.PrescriptionRepository;
import com.rtms.backend.medical.repository.TreatmentPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TreatmentPlanServiceRouteTest {
    @Mock private HealthRecordRepository healthRecordRepository;
    @Mock private TreatmentPlanRepository treatmentPlanRepository;
    @Mock private PrescriptionRepository prescriptionRepository;

    private TreatmentPlanService service;

    @BeforeEach
    void setUp() {
        service = new TreatmentPlanService(healthRecordRepository, treatmentPlanRepository, prescriptionRepository);
    }

    @Test
    void rejectsInjectionPrescriptionRouteSpellings() {
        for (String route : List.of("INJECTION_IV", "IV", "Intramuscular (IM)", "SC")) {
            stubExistingPlan();
            CreatePrescriptionRequest request = prescription(route);

            ApiException ex = assertThrows(ApiException.class, () -> service.createPrescription(12L, request));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus(), route);
            assertEquals("INJECTION_NOT_ALLOWED", ex.getErrorCode(), route);
            assertEquals("Treatment plans cannot contain injections", ex.getMessage(), route);
            verify(prescriptionRepository, never()).save(any());
            clearInvocations(treatmentPlanRepository, healthRecordRepository, prescriptionRepository);
        }
    }

    @Test
    void acceptsOralRouteIgnoringCase() {
        for (String route : List.of("ORAL", "oral")) {
            stubExistingPlan();
            when(prescriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            assertNotNull(service.createPrescription(12L, prescription(route)));
            verify(prescriptionRepository).save(any());
            clearInvocations(treatmentPlanRepository, healthRecordRepository, prescriptionRepository);
        }
    }

    @Test
    void rejectsInjectionRouteInJsonInstructions() {
        when(healthRecordRepository.findById(7L)).thenReturn(Optional.of(new HealthRecord()));
        CreateTreatmentPlanRequest request = treatmentPlan("{\"items\":[{\"name\":\"Drug\",\"route\":\"IM\"}]}");

        ApiException ex = assertThrows(ApiException.class, () -> service.createTreatmentPlan(7L, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INJECTION_NOT_ALLOWED", ex.getErrorCode());
        verify(treatmentPlanRepository, never()).save(any());
    }

    @Test
    void acceptsPlainTextInstructions() {
        when(healthRecordRepository.findById(7L)).thenReturn(Optional.of(new HealthRecord()));
        when(treatmentPlanRepository.save(any(TreatmentPlan.class))).thenAnswer(invocation -> {
            TreatmentPlan plan = invocation.getArgument(0);
            plan.setId(20L);
            return plan;
        });
        when(prescriptionRepository.findByTreatmentPlanId(20L)).thenReturn(List.of());
        CreateTreatmentPlanRequest request = treatmentPlan("Give medication with feed and monitor appetite.");

        assertNotNull(service.createTreatmentPlan(7L, request));
        verify(treatmentPlanRepository).save(any(TreatmentPlan.class));
    }

    private void stubExistingPlan() {
        TreatmentPlan plan = new TreatmentPlan();
        plan.setId(12L);
        plan.setHealthRecordId(7L);
        when(treatmentPlanRepository.findById(12L)).thenReturn(Optional.of(plan));
        when(healthRecordRepository.findById(7L)).thenReturn(Optional.of(new HealthRecord()));
    }

    private CreatePrescriptionRequest prescription(String route) {
        CreatePrescriptionRequest request = new CreatePrescriptionRequest();
        request.setMedicationName("Test medicine");
        request.setRoute(route);
        request.setStartDate(LocalDate.of(2026, 11, 1));
        return request;
    }

    private CreateTreatmentPlanRequest treatmentPlan(String instructions) {
        CreateTreatmentPlanRequest request = new CreateTreatmentPlanRequest();
        request.setTreatmentName("Recovery");
        request.setInstructions(instructions);
        return request;
    }
}
