package com.rtms.backend.entity;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.enums.ArrivalStatus;
import com.rtms.backend.enums.ReviewDecision;
import com.rtms.backend.enums.VetDecision;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admission_applications")
public class AdmissionApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AdmissionStatus status = AdmissionStatus.GROOM_REVIEW;

    @Column(name = "quarantine_stall_id")
    private Long quarantineStallId;

    @Column(name = "groom_id")
    private Long groomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "groom_decision", length = 20)
    private ReviewDecision groomDecision;

    @Column(name = "groom_feedback", columnDefinition = "TEXT")
    private String groomFeedback;

    @Column(name = "groom_reviewed_at")
    private LocalDateTime groomReviewedAt;

    @Column(name = "veterinarian_id")
    private Long veterinarianId;

    @Enumerated(EnumType.STRING)
    @Column(name = "vet_decision", length = 20)
    private VetDecision vetDecision;

    @Column(name = "vet_training_decision", length = 30)
    private String vetTrainingDecision;

    @Column(name = "vet_feedback", columnDefinition = "TEXT")
    private String vetFeedback;

    @Column(name = "vet_reviewed_at")
    private LocalDateTime vetReviewedAt;

    @Column(name = "trainer_id")
    private Long trainerId;

    @Column(name = "trainer_feedback", columnDefinition = "TEXT")
    private String trainerFeedback;

    @Column(name = "trainer_reviewed_at")
    private LocalDateTime trainerReviewedAt;

    @Column(name = "manager_id")
    private Long managerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "manager_decision", length = 20)
    private ReviewDecision managerDecision;

    @Column(name = "manager_feedback", columnDefinition = "TEXT")
    private String managerFeedback;

    @Column(name = "manager_reviewed_at")
    private LocalDateTime managerReviewedAt;

    @Column(name = "horse_id")
    private Long horseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "arrival_status", nullable = false, length = 20)
    private ArrivalStatus arrivalStatus = ArrivalStatus.PENDING;

    @Column(name = "arrival_confirmed_at")
    private LocalDateTime arrivalConfirmedAt;

    @Column(name = "arrival_confirmed_by")
    private Long arrivalConfirmedBy;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (submittedAt == null) {
            submittedAt = now;
        }

        if (status == null) {
            status = AdmissionStatus.GROOM_REVIEW;
        }
        if (arrivalStatus == null) {
            arrivalStatus = ArrivalStatus.PENDING;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public AdmissionStatus getStatus() {
        return status;
    }

    public void setStatus(AdmissionStatus status) {
        this.status = status;
    }

    public Long getQuarantineStallId() {
        return quarantineStallId;
    }

    public void setQuarantineStallId(Long quarantineStallId) {
        this.quarantineStallId = quarantineStallId;
    }

    public Long getGroomId() {
        return groomId;
    }

    public void setGroomId(Long groomId) {
        this.groomId = groomId;
    }

    public ReviewDecision getGroomDecision() {
        return groomDecision;
    }

    public void setGroomDecision(ReviewDecision groomDecision) {
        this.groomDecision = groomDecision;
    }

    public String getGroomFeedback() {
        return groomFeedback;
    }

    public void setGroomFeedback(String groomFeedback) {
        this.groomFeedback = groomFeedback;
    }

    public LocalDateTime getGroomReviewedAt() {
        return groomReviewedAt;
    }

    public void setGroomReviewedAt(LocalDateTime groomReviewedAt) {
        this.groomReviewedAt = groomReviewedAt;
    }

    public Long getVeterinarianId() {
        return veterinarianId;
    }

    public void setVeterinarianId(Long veterinarianId) {
        this.veterinarianId = veterinarianId;
    }

    public VetDecision getVetDecision() {
        return vetDecision;
    }

    public void setVetDecision(VetDecision vetDecision) {
        this.vetDecision = vetDecision;
    }

    public String getVetTrainingDecision() {
        return vetTrainingDecision;
    }

    public void setVetTrainingDecision(String vetTrainingDecision) {
        this.vetTrainingDecision = vetTrainingDecision;
    }

    public String getVetFeedback() {
        return vetFeedback;
    }

    public void setVetFeedback(String vetFeedback) {
        this.vetFeedback = vetFeedback;
    }

    public LocalDateTime getVetReviewedAt() {
        return vetReviewedAt;
    }

    public void setVetReviewedAt(LocalDateTime vetReviewedAt) {
        this.vetReviewedAt = vetReviewedAt;
    }

    public Long getTrainerId() {
        return trainerId;
    }

    public void setTrainerId(Long trainerId) {
        this.trainerId = trainerId;
    }

    public String getTrainerFeedback() {
        return trainerFeedback;
    }

    public void setTrainerFeedback(String trainerFeedback) {
        this.trainerFeedback = trainerFeedback;
    }

    public LocalDateTime getTrainerReviewedAt() {
        return trainerReviewedAt;
    }

    public void setTrainerReviewedAt(LocalDateTime trainerReviewedAt) {
        this.trainerReviewedAt = trainerReviewedAt;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public ReviewDecision getManagerDecision() {
        return managerDecision;
    }

    public void setManagerDecision(ReviewDecision managerDecision) {
        this.managerDecision = managerDecision;
    }

    public String getManagerFeedback() {
        return managerFeedback;
    }

    public void setManagerFeedback(String managerFeedback) {
        this.managerFeedback = managerFeedback;
    }

    public LocalDateTime getManagerReviewedAt() {
        return managerReviewedAt;
    }

    public void setManagerReviewedAt(LocalDateTime managerReviewedAt) {
        this.managerReviewedAt = managerReviewedAt;
    }

    public Long getHorseId() {
        return horseId;
    }

    public void setHorseId(Long horseId) {
        this.horseId = horseId;
    }

    public ArrivalStatus getArrivalStatus() {
        return arrivalStatus;
    }

    public void setArrivalStatus(ArrivalStatus arrivalStatus) {
        this.arrivalStatus = arrivalStatus;
    }

    public LocalDateTime getArrivalConfirmedAt() {
        return arrivalConfirmedAt;
    }

    public void setArrivalConfirmedAt(LocalDateTime arrivalConfirmedAt) {
        this.arrivalConfirmedAt = arrivalConfirmedAt;
    }

    public Long getArrivalConfirmedBy() {
        return arrivalConfirmedBy;
    }

    public void setArrivalConfirmedBy(Long arrivalConfirmedBy) {
        this.arrivalConfirmedBy = arrivalConfirmedBy;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
