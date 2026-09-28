package com.rtms.backend.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.dto.CompleteVetExamRequest;
import com.rtms.backend.dto.VetExamResponse;
import com.rtms.backend.dto.VetReviewRequest;
import com.rtms.backend.dto.VetReviewResponse;
import com.rtms.backend.entity.*;
import com.rtms.backend.enums.*;
import com.rtms.backend.repository.*;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Compatibility adapter for the existing Admission Vet UI. */
@Service
public class AdmissionReviewService {
    private final AdmissionApplicationRepository admissions;
    private final VetExamRepository exams;
    private final HorseRepository horses;
    private final StableStallRepository stalls;
    private final VetExamService vetExamService;

    public AdmissionReviewService(AdmissionApplicationRepository admissions,
            VetExamRepository exams, HorseRepository horses, StableStallRepository stalls,
            VetExamService vetExamService) {
        this.admissions = admissions;
        this.exams = exams;
        this.horses = horses;
        this.stalls = stalls;
        this.vetExamService = vetExamService;
    }

    @Transactional
    public VetReviewResponse reviewByVet(Long admissionId, VetReviewRequest request, Long actorId) {
        AdmissionApplication before = admissions.findById(admissionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "RESOURCE_NOT_FOUND", "Admission not found"));
        if (before.getStatus() != AdmissionStatus.VET_REVIEW
                && before.getStatus() != AdmissionStatus.PENDING_RECHECK) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_REVIEW_STATE",
                    "Admission is not awaiting a vet examination");
        }
        VetExam exam = exams.findFirstByAdmissionIdAndExamTypeOrderByCreatedAtDesc(admissionId,
                before.getStatus() == AdmissionStatus.VET_REVIEW
                        ? VetExamType.INITIAL : VetExamType.FOLLOW_UP)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT,
                        "INVALID_REVIEW_STATE", "Admission has no veterinary exam request"));
        if (exam.getStatus() == VetExamStatus.SCHEDULED) {
            vetExamService.start(exam.getId(), actorId);
        } else if (exam.getStatus() != VetExamStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_REVIEW_STATE",
                    "Admission exam must be scheduled and started before review");
        }

        CompleteVetExamRequest completion = new CompleteVetExamRequest();
        completion.setSymptoms(request.getSymptoms());
        completion.setFindings(request.getFindings());
        completion.setDiagnosis(request.getDiagnosis());
        completion.setTreatment(request.getTreatment());
        completion.setNote(request.getNotes() == null ? request.getFeedback() : request.getNotes());
        completion.setVetDecision(request.getDecision());
        completion.setRejectionReason(request.getRejectionReason() == null
                ? request.getFeedback() : request.getRejectionReason());
        completion.setFollowUpDate(request.getFollowUpDate());
        completion.setMetrics(request.getMetrics());
        VetExamResponse completed = vetExamService.complete(exam.getId(), completion, actorId);

        AdmissionApplication admission = admissions.findById(admissionId).orElseThrow();
        Horse horse = horses.findById(admission.getHorseId()).orElseThrow();
        String qStallCode = Optional.ofNullable(admission.getQuarantineStallId())
                .flatMap(stalls::findById).map(StableStall::getStallCode).orElse(null);
        return new VetReviewResponse(admission.getId(), admission.getStatus(), actorId,
                request.getDecision(), admission.getVetFeedback(), admission.getVetReviewedAt(),
                horse.getId(), horse.getCurrentStatus(), admission.getQuarantineStallId(),
                qStallCode, completed.status(), completed.healthRecordId(), completed.id());
    }
}
