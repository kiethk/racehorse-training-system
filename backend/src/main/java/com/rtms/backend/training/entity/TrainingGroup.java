package com.rtms.backend.training.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Nhóm huấn luyện bền vững của một Trainer: tối đa 6 chiến mã cùng ra sân.
 * (A lasting training group of one Trainer: at most 6 horses going out together.)
 */
@Entity
@Table(name = "training_groups")
public class TrainingGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trainer_id", nullable = false)
    private Long trainerId;

    @Column(nullable = false, length = 100)
    private String name;

    /** Thời điểm giải tán; rỗng khi nhóm còn hoạt động. (When the group was disbanded; null while it is live.) */
    @Column(name = "disbanded_at")
    private LocalDateTime disbandedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTrainerId() { return trainerId; }
    public void setTrainerId(Long trainerId) { this.trainerId = trainerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDateTime getDisbandedAt() { return disbandedAt; }
    public void setDisbandedAt(LocalDateTime disbandedAt) { this.disbandedAt = disbandedAt; }
    public boolean isDisbanded() { return disbandedAt != null; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
