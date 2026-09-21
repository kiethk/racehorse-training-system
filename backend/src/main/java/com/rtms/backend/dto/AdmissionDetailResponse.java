package com.rtms.backend.dto;

import com.rtms.backend.entity.CandidateHorseProfile;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.enums.ReviewDecision;

import java.time.LocalDateTime;

public class AdmissionDetailResponse {

    private final Long id;
    private final AdmissionStatus status;
    private final Long ownerId;
    private final String quarantineStallCode;
    private final CandidateHorseProfile candidate;
    private final ReviewDecision groomDecision;
    private final String groomFeedback;
    private final LocalDateTime groomReviewedAt;
    private final ReviewDecision vetDecision;
    private final String vetFeedback;
    private final LocalDateTime vetReviewedAt;
    private final LocalDateTime physicalExamConfirmedAt;
    private final Long vetReviewedBy;
    private final ReviewDecision trainerDecision;
    private final String trainerFeedback;
    private final LocalDateTime trainerReviewedAt;
    private final ReviewDecision managerDecision;
    private final String managerFeedback;
    private final LocalDateTime managerReviewedAt;
    private final LocalDateTime submittedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public AdmissionDetailResponse(
            Long id,
            AdmissionStatus status,
            Long ownerId,
            String quarantineStallCode,
            CandidateHorseProfile candidate,
            ReviewDecision groomDecision,
            String groomFeedback,
            LocalDateTime groomReviewedAt,
            ReviewDecision vetDecision,
            String vetFeedback,
            LocalDateTime vetReviewedAt,
            LocalDateTime physicalExamConfirmedAt,
            Long vetReviewedBy,
            ReviewDecision trainerDecision,
            String trainerFeedback,
            LocalDateTime trainerReviewedAt,
            ReviewDecision managerDecision,
            String managerFeedback,
            LocalDateTime managerReviewedAt,
            LocalDateTime submittedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.status = status;
        this.ownerId = ownerId;
        this.quarantineStallCode = quarantineStallCode;
        this.candidate = candidate;
        this.groomDecision = groomDecision;
        this.groomFeedback = groomFeedback;
        this.groomReviewedAt = groomReviewedAt;
        this.vetDecision = vetDecision;
        this.vetFeedback = vetFeedback;
        this.vetReviewedAt = vetReviewedAt;
        this.physicalExamConfirmedAt = physicalExamConfirmedAt;
        this.vetReviewedBy = vetReviewedBy;
        this.trainerDecision = trainerDecision;
        this.trainerFeedback = trainerFeedback;
        this.trainerReviewedAt = trainerReviewedAt;
        this.managerDecision = managerDecision;
        this.managerFeedback = managerFeedback;
        this.managerReviewedAt = managerReviewedAt;
        this.submittedAt = submittedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public AdmissionStatus getStatus() { return status; }
    public Long getOwnerId() { return ownerId; }
    public String getQuarantineStallCode() { return quarantineStallCode; }
    public CandidateHorseProfile getCandidate() { return candidate; }
    public ReviewDecision getGroomDecision() { return groomDecision; }
    public String getGroomFeedback() { return groomFeedback; }
    public LocalDateTime getGroomReviewedAt() { return groomReviewedAt; }
    public ReviewDecision getVetDecision() { return vetDecision; }
    public String getVetFeedback() { return vetFeedback; }
    public LocalDateTime getVetReviewedAt() { return vetReviewedAt; }
    public LocalDateTime getPhysicalExamConfirmedAt() { return physicalExamConfirmedAt; }
    public Long getVetReviewedBy() { return vetReviewedBy; }
    public ReviewDecision getTrainerDecision() { return trainerDecision; }
    public String getTrainerFeedback() { return trainerFeedback; }
    public LocalDateTime getTrainerReviewedAt() { return trainerReviewedAt; }
    public ReviewDecision getManagerDecision() { return managerDecision; }
    public String getManagerFeedback() { return managerFeedback; }
    public LocalDateTime getManagerReviewedAt() { return managerReviewedAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
