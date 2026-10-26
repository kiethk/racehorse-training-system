package com.rtms.backend.horse.entity;

import com.rtms.backend.horse.enums.HorseEditRequestStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * An owner's request to change mutable facts of an ELIGIBLE horse. The official
 * record changes only when the groom of the horse's stall approves it.
 * {@code original*} hold what the owner saw, so a stale request can be refused.
 */
@Entity
@Table(name = "horse_edit_requests")
public class HorseEditRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "horse_id", nullable = false)
    private Long horseId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HorseEditRequestStatus status = HorseEditRequestStatus.PENDING;

    @Column(name = "requested_name", nullable = false)
    private String requestedName;

    @Column(name = "requested_breed")
    private String requestedBreed;

    @Column(name = "requested_sire_name")
    private String requestedSireName;

    @Column(name = "requested_sire_registration_number", length = 15)
    private String requestedSireRegistrationNumber;

    @Column(name = "requested_dam_name")
    private String requestedDamName;

    @Column(name = "requested_dam_registration_number", length = 15)
    private String requestedDamRegistrationNumber;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "original_breed")
    private String originalBreed;

    @Column(name = "original_sire_name")
    private String originalSireName;

    @Column(name = "original_sire_registration_number", length = 15)
    private String originalSireRegistrationNumber;

    @Column(name = "original_dam_name")
    private String originalDamName;

    @Column(name = "original_dam_registration_number", length = 15)
    private String originalDamRegistrationNumber;

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(name = "review_feedback", columnDefinition = "TEXT")
    private String reviewFeedback;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getHorseId() { return horseId; }
    public void setHorseId(Long horseId) { this.horseId = horseId; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public HorseEditRequestStatus getStatus() { return status; }
    public void setStatus(HorseEditRequestStatus status) { this.status = status; }

    public String getRequestedName() { return requestedName; }
    public void setRequestedName(String requestedName) { this.requestedName = requestedName; }

    public String getRequestedBreed() { return requestedBreed; }
    public void setRequestedBreed(String requestedBreed) { this.requestedBreed = requestedBreed; }

    public String getRequestedSireName() { return requestedSireName; }
    public void setRequestedSireName(String requestedSireName) { this.requestedSireName = requestedSireName; }

    public String getRequestedSireRegistrationNumber() { return requestedSireRegistrationNumber; }
    public void setRequestedSireRegistrationNumber(String value) { this.requestedSireRegistrationNumber = value; }

    public String getRequestedDamName() { return requestedDamName; }
    public void setRequestedDamName(String requestedDamName) { this.requestedDamName = requestedDamName; }

    public String getRequestedDamRegistrationNumber() { return requestedDamRegistrationNumber; }
    public void setRequestedDamRegistrationNumber(String value) { this.requestedDamRegistrationNumber = value; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getOriginalBreed() { return originalBreed; }
    public void setOriginalBreed(String originalBreed) { this.originalBreed = originalBreed; }

    public String getOriginalSireName() { return originalSireName; }
    public void setOriginalSireName(String originalSireName) { this.originalSireName = originalSireName; }

    public String getOriginalSireRegistrationNumber() { return originalSireRegistrationNumber; }
    public void setOriginalSireRegistrationNumber(String value) { this.originalSireRegistrationNumber = value; }

    public String getOriginalDamName() { return originalDamName; }
    public void setOriginalDamName(String originalDamName) { this.originalDamName = originalDamName; }

    public String getOriginalDamRegistrationNumber() { return originalDamRegistrationNumber; }
    public void setOriginalDamRegistrationNumber(String value) { this.originalDamRegistrationNumber = value; }

    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }

    public String getReviewFeedback() { return reviewFeedback; }
    public void setReviewFeedback(String reviewFeedback) { this.reviewFeedback = reviewFeedback; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
