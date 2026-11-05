package com.rtms.backend.admission.service;

import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.dto.AdmissionUelnAvailabilityResponse;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.horse.util.UelnUtils;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class AdmissionUelnPolicy {
    public static final String CONFLICT_CODE = "ADMISSION_UELN_CONFLICT";
    private static final Set<AdmissionStatus> COMPLETED_STATUSES =
            Set.of(AdmissionStatus.APPROVED, AdmissionStatus.REJECTED);

    private final AdmissionApplicationRepository admissions;
    private final HorseRepository horses;
    private final EntityManager entityManager;

    public AdmissionUelnPolicy(AdmissionApplicationRepository admissions, HorseRepository horses,
                               EntityManager entityManager) {
        this.admissions = admissions;
        this.horses = horses;
        this.entityManager = entityManager;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void assertAvailableForSubmission(String normalizedUeln) {
        if (normalizedUeln == null) return;

        // Serialize submissions even when no Horse or candidate row exists yet.
        // PostgreSQL releases this lock at commit/rollback of the submission transaction.
        entityManager.createNativeQuery(
                        "SELECT 1 FROM pg_advisory_xact_lock(hashtextextended(:ueln, 0))")
                .setParameter("ueln", normalizedUeln)
                .getSingleResult();

        var availability = availability(normalizedUeln);
        if (!availability.available()) {
            throw new ApiException(HttpStatus.CONFLICT, CONFLICT_CODE, availability.message());
        }
    }

    @Transactional(readOnly = true)
    public AdmissionUelnAvailabilityResponse checkAvailability(String rawUeln) {
        String ueln = UelnUtils.cleanAndValidate(rawUeln, "UELN");
        return ueln == null ? new AdmissionUelnAvailabilityResponse(true, null) : availability(ueln);
    }

    private AdmissionUelnAvailabilityResponse availability(String normalizedUeln) {
        if (admissions.existsOpenAdmissionByUeln(normalizedUeln, COMPLETED_STATUSES)) {
            return new AdmissionUelnAvailabilityResponse(false,
                    "An admission for this UELN is already being processed.");
        }

        boolean hasActiveHorse = horses.findByRegistrationNumber(normalizedUeln).stream()
                .anyMatch(horse -> horse.getCurrentStatus() != HorseStatus.REJECTED);
        if (hasActiveHorse) {
            return new AdmissionUelnAvailabilityResponse(false,
                    "A horse with this UELN is already registered at the centre.");
        }
        return new AdmissionUelnAvailabilityResponse(true, null);
    }
}
