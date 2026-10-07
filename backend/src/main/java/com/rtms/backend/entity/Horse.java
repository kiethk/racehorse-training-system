package com.rtms.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.rtms.backend.enums.HorseStatus;
import com.rtms.backend.enums.TrainingDecision;

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

    /**
     * Quyết định y tế đang có hiệu lực: được tập hay đang tạm nghỉ.
     * Cùng kiểu với HealthRecord.trainingDecision — một khái niệm, một tên.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "training_decision", nullable = false)
    private TrainingDecision trainingDecision = TrainingDecision.ALLOWED;

    /** Vì sao bị chặn tập. Chỉ có giá trị khi trainingDecision = BLOCKED. */
    @Column(name = "training_decision_reason", columnDefinition = "TEXT")
    private String trainingDecisionReason;

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

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (trainingDecision == null) {
            trainingDecision = TrainingDecision.ALLOWED;
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

    public TrainingDecision getTrainingDecision() { return trainingDecision; }
    public void setTrainingDecision(TrainingDecision trainingDecision) { this.trainingDecision = trainingDecision; }

    public String getTrainingDecisionReason() { return trainingDecisionReason; }
    public void setTrainingDecisionReason(String trainingDecisionReason) { this.trainingDecisionReason = trainingDecisionReason; }

    /**
     * Quy tắc duy nhất: ngựa đã được nhận vào nuôi VÀ không bị chặn tập.
     *
     * Ngựa CANDIDATE không tập được vì chưa được duyệt nhập — điều đó suy ra từ
     * currentStatus, không lưu thành một cờ khóa riêng.
     */
    public boolean canTrain() {
        return currentStatus == HorseStatus.ELIGIBLE && trainingDecision == TrainingDecision.ALLOWED;
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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}