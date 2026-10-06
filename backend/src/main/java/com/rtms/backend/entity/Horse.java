package com.rtms.backend.entity;
import com.rtms.backend.enums.HorseStatus;
import com.rtms.backend.enums.TrainingStatus;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "horses")
public class Horse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String breed;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false)
    private HorseStatus currentStatus = HorseStatus.ELIGIBLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "training_status", nullable = false)
    private TrainingStatus trainingStatus = TrainingStatus.ALLOWED;

    @Column(name = "stable_location")
    private String stableLocation;

    @Column(name = "current_stall_id", unique = true)
    private Long currentStallId;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "registry_name")
    private String registryName;

    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(name = "training_locked", nullable = false)
    private boolean trainingLocked;

    @Column(name = "training_lock_reason", columnDefinition = "TEXT")
    private String trainingLockReason;

    @Column(name = "training_lock_review_date")
    private LocalDate trainingLockReviewDate;

    @Column(name = "training_lock_vet_id")
    private Long trainingLockVetId;

    @Column(name = "training_lock_updated_at")
    private LocalDateTime trainingLockUpdatedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (trainingStatus == null) {
            trainingStatus = trainingLocked ? TrainingStatus.BLOCKED : TrainingStatus.ALLOWED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public HorseStatus getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(HorseStatus currentStatus) { this.currentStatus = currentStatus; }

    public TrainingStatus getTrainingStatus() { return trainingStatus; }
    public void setTrainingStatus(TrainingStatus trainingStatus) {
        this.trainingStatus = trainingStatus != null ? trainingStatus : TrainingStatus.ALLOWED;
        this.trainingLocked = (this.trainingStatus != TrainingStatus.ALLOWED);
    }

    public String getStableLocation() { return stableLocation; }
    public void setStableLocation(String stableLocation) { this.stableLocation = stableLocation; }

    public Long getCurrentStallId() { return currentStallId; }
    public void setCurrentStallId(Long currentStallId) { this.currentStallId = currentStallId; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public String getRegistryName() { return registryName; }
    public void setRegistryName(String registryName) { this.registryName = registryName; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public boolean isTrainingLocked() { return trainingLocked; }
    public void setTrainingLocked(boolean trainingLocked) {
        this.trainingLocked = trainingLocked;
        if (!trainingLocked) {
            this.trainingStatus = TrainingStatus.ALLOWED;
        } else if (this.trainingStatus == TrainingStatus.ALLOWED || this.trainingStatus == null) {
            this.trainingStatus = TrainingStatus.BLOCKED;
        }
    }

    public String getTrainingLockReason() { return trainingLockReason; }
    public void setTrainingLockReason(String trainingLockReason) { this.trainingLockReason = trainingLockReason; }

    public LocalDate getTrainingLockReviewDate() { return trainingLockReviewDate; }
    public void setTrainingLockReviewDate(LocalDate trainingLockReviewDate) { this.trainingLockReviewDate = trainingLockReviewDate; }

    public Long getTrainingLockVetId() { return trainingLockVetId; }
    public void setTrainingLockVetId(Long trainingLockVetId) { this.trainingLockVetId = trainingLockVetId; }

    public LocalDateTime getTrainingLockUpdatedAt() { return trainingLockUpdatedAt; }
    public void setTrainingLockUpdatedAt(LocalDateTime trainingLockUpdatedAt) { this.trainingLockUpdatedAt = trainingLockUpdatedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}