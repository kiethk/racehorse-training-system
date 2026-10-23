package com.rtms.backend.medical.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.medical.dto.CreatePrescriptionRequest;
import com.rtms.backend.medical.dto.CreateTreatmentPlanRequest;
import com.rtms.backend.medical.dto.PrescriptionResponse;
import com.rtms.backend.medical.dto.TreatmentPlanResponse;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.entity.Prescription;
import com.rtms.backend.medical.entity.TreatmentPlan;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.medical.repository.PrescriptionRepository;
import com.rtms.backend.medical.repository.TreatmentPlanRepository;
import com.rtms.backend.security.AuthenticatedUser;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class TreatmentPlanService {
    private final HealthRecordRepository healthRecordRepository;
    private final TreatmentPlanRepository treatmentPlanRepository;
    private final PrescriptionRepository prescriptionRepository;

    public TreatmentPlanService(
            HealthRecordRepository healthRecordRepository,
            TreatmentPlanRepository treatmentPlanRepository,
            PrescriptionRepository prescriptionRepository)
    {
        this.healthRecordRepository = healthRecordRepository;
        this.treatmentPlanRepository = treatmentPlanRepository;
        this.prescriptionRepository = prescriptionRepository;
    }

    private void verifyVetAccess(HealthRecord record, AuthenticatedUser currentUser) {
        AuthenticatedUser user = currentUser;
        if (user == null) {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser authUser) {
                user = authUser;
            }
        }
        if (user != null && "VETERINARIAN".equals(user.getRole())) {
            if (record.getVeterinarianId() != null && !Objects.equals(record.getVeterinarianId(), user.getUserId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You are not assigned to this health record");
            }
        }
    }

    @Transactional
    public TreatmentPlanResponse createTreatmentPlan(
            Long healthRecordId,
            CreateTreatmentPlanRequest request
    ) {
        return createTreatmentPlan(healthRecordId, request, null);
    }

    @Transactional
    public TreatmentPlanResponse createTreatmentPlan(
            Long healthRecordId,
            CreateTreatmentPlanRequest request,
            AuthenticatedUser currentUser
    ) {
        HealthRecord record = healthRecordRepository.findById(healthRecordId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HEALTH_RECORD_NOT_FOUND", "Health record not found with id: " + healthRecordId));

        verifyVetAccess(record, currentUser);

        validateDates(
                request.getStartDate(),
                request.getEndDate()
        );

        TreatmentPlan plan = new TreatmentPlan();
        plan.setHealthRecordId(healthRecordId);
        plan.setTreatmentName(request.getTreatmentName());
        plan.setInstructions(request.getInstructions());
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        plan.setStatus("ACTIVE");

        return toTreatmentPlanResponse(treatmentPlanRepository.save(plan));
    }

    @Transactional
    public PrescriptionResponse createPrescription(
            Long treatmentPlanId,
            CreatePrescriptionRequest request
    ) {
        return createPrescription(treatmentPlanId, request, null);
    }

    @Transactional
    public PrescriptionResponse createPrescription(
            Long treatmentPlanId,
            CreatePrescriptionRequest request,
            AuthenticatedUser currentUser
    ) {
        TreatmentPlan plan = treatmentPlanRepository.findById(treatmentPlanId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TREATMENT_PLAN_NOT_FOUND", "Treatment plan not found with id: " + treatmentPlanId));

        HealthRecord record = healthRecordRepository.findById(plan.getHealthRecordId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HEALTH_RECORD_NOT_FOUND", "Health record not found with id: " + plan.getHealthRecordId()));

        verifyVetAccess(record, currentUser);

        validateDates(request.getStartDate(), request.getEndDate());

        Prescription prescription = new Prescription();

        prescription.setTreatmentPlanId(plan.getId());
        prescription.setMedicationName(request.getMedicationName());
        prescription.setDosage(request.getDosage());
        prescription.setFrequency(request.getFrequency());
        prescription.setRoute(request.getRoute());
        prescription.setStartDate(request.getStartDate());
        prescription.setEndDate(request.getEndDate());
        prescription.setInstructions(request.getInstructions());

        Prescription saved = prescriptionRepository.save(prescription);

        return toPrescriptionResponse(saved);
    }

    @Transactional
    public TreatmentPlanResponse updateTreatmentPlanStatus(Long planId, String status, AuthenticatedUser currentUser) {
        TreatmentPlan plan = treatmentPlanRepository.findById(planId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TREATMENT_PLAN_NOT_FOUND", "Treatment plan not found with id: " + planId));

        HealthRecord record = healthRecordRepository.findById(plan.getHealthRecordId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HEALTH_RECORD_NOT_FOUND", "Health record not found with id: " + plan.getHealthRecordId()));

        verifyVetAccess(record, currentUser);

        if (status == null || status.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "Status cannot be null or empty");
        }

        String upper = status.trim().toUpperCase();
        if (!upper.equals("ACTIVE") && !upper.equals("COMPLETED") && !upper.equals("CANCELLED")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "Invalid treatment plan status: " + status);
        }

        plan.setStatus(upper);
        TreatmentPlan saved = treatmentPlanRepository.save(plan);
        return toTreatmentPlanResponse(saved);
    }

    public List<TreatmentPlanResponse> getByHealthRecordId(Long healthRecordId) {
        healthRecordRepository.findById(healthRecordId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HEALTH_RECORD_NOT_FOUND", "Health record not found with id: " + healthRecordId));
        return treatmentPlanRepository
                .findByHealthRecordId(healthRecordId)
                .stream()
                .map(this::toTreatmentPlanResponse)
                .toList();
    }

    private TreatmentPlanResponse toTreatmentPlanResponse(TreatmentPlan plan) {
        List<PrescriptionResponse> prescriptions =
                prescriptionRepository
                        .findByTreatmentPlanId(plan.getId())
                        .stream()
                        .map(this::toPrescriptionResponse)
                        .toList();
        return new TreatmentPlanResponse(
                plan.getId(),
                plan.getHealthRecordId(),
                plan.getTreatmentName(),
                plan.getInstructions(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getStatus(),
                prescriptions
        );
    }

    private PrescriptionResponse toPrescriptionResponse(Prescription prescription) {
        return new PrescriptionResponse(
                prescription.getId(),
                prescription.getMedicationName(),
                prescription.getDosage(),
                prescription.getFrequency(),
                prescription.getRoute(),
                prescription.getStartDate(),
                prescription.getEndDate(),
                prescription.getInstructions()
        );
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "Start date cannot be after end date");
        }
    }
}
