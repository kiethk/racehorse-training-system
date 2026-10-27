package com.rtms.backend.admission.service;

import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdmissionUelnPolicyTest {
    private final AdmissionApplicationRepository admissions = mock(AdmissionApplicationRepository.class);
    private final HorseRepository horses = mock(HorseRepository.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query lock = mock(Query.class);
    private final AdmissionUelnPolicy policy = new AdmissionUelnPolicy(admissions, horses, entityManager);
    private static final String UELN = "999999TST000001";

    @BeforeEach
    void setup() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(lock);
        when(lock.setParameter("ueln", UELN)).thenReturn(lock);
    }

    @Test
    void unknownUelnDoesNotBlockUnidentifiedHorses() {
        policy.assertAvailableForSubmission(null);
        verifyNoInteractions(entityManager, admissions, horses);
    }

    @Test
    void availabilityCheckAcceptsIsoAlpha3Format() {
        var res = policy.checkAvailability("VNM22SC00000001");
        assertTrue(res.available());
    }

    @Test
    void checksOpenAdmissionsAfterTakingTransactionLock() {
        when(admissions.existsOpenAdmissionByUeln(eq(UELN), anyCollection())).thenReturn(true);

        ApiException error = assertThrows(ApiException.class, () -> policy.assertAvailableForSubmission(UELN));
        assertEquals(409, error.getStatus().value());
        assertEquals(AdmissionUelnPolicy.CONFLICT_CODE, error.getErrorCode());
        var order = inOrder(lock, admissions);
        order.verify(lock).getSingleResult();
        order.verify(admissions).existsOpenAdmissionByUeln(UELN,
                Set.of(AdmissionStatus.APPROVED, AdmissionStatus.REJECTED));
        verifyNoInteractions(horses);
    }

    @ParameterizedTest
    @EnumSource(value = HorseStatus.class, names = {"CANDIDATE", "ELIGIBLE"})
    void activeHorsePreventsAnotherAdmission(HorseStatus status) {
        Horse horse = new Horse();
        horse.setCurrentStatus(status);
        when(horses.findByRegistrationNumber(UELN)).thenReturn(List.of(horse));

        ApiException error = assertThrows(ApiException.class, () -> policy.assertAvailableForSubmission(UELN));
        assertEquals(409, error.getStatus().value());
        assertEquals(AdmissionUelnPolicy.CONFLICT_CODE, error.getErrorCode());
    }

    @Test
    void rejectedHorseCanBeReadmittedRegardlessOfCurrentOwner() {
        Horse horse = new Horse();
        horse.setCurrentStatus(HorseStatus.REJECTED);
        horse.setOwnerId(42L);
        when(horses.findByRegistrationNumber(UELN)).thenReturn(List.of(horse));

        assertDoesNotThrow(() -> policy.assertAvailableForSubmission(UELN));
        assertEquals(42L, horse.getOwnerId());
        verify(horses, never()).save(any());
    }

    @Test
    void newUelnCanBeSubmitted() {
        assertDoesNotThrow(() -> policy.assertAvailableForSubmission(UELN));
        verify(horses).findByRegistrationNumber(UELN);
    }

    @Test
    void availabilityCheckNormalizesInputAndDoesNotTakeSubmissionLock() {
        when(admissions.existsOpenAdmissionByUeln(eq(UELN), anyCollection())).thenReturn(true);

        var result = policy.checkAvailability(" 999999tst000001 ");

        assertFalse(result.available());
        assertTrue(result.message().contains("already being processed"));
        verifyNoInteractions(entityManager, horses);
    }

    @ParameterizedTest
    @EnumSource(HorseStatus.class)
    void availabilityCheckUsesSameHorseStatusRuleAsSubmission(HorseStatus status) {
        Horse horse = new Horse();
        horse.setCurrentStatus(status);
        when(horses.findByRegistrationNumber(UELN)).thenReturn(List.of(horse));

        assertEquals(status == HorseStatus.REJECTED, policy.checkAvailability(UELN).available());
        verifyNoInteractions(entityManager);
        verify(horses, never()).save(any());
    }

    @Test
    void availabilityCheckRejectsInvalidFormatBeforeReadingDatabase() {
        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> policy.checkAvailability("INVALID_UELN_123"));
        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(entityManager, admissions, horses);
    }
}
