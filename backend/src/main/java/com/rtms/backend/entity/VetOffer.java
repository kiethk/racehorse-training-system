package com.rtms.backend.entity;
import com.rtms.backend.enums.VetOfferStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vet_offers")
public class VetOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "care_schedule_id", nullable = false)
    private Long careScheduleId;

    @Column(name = "veterinarian_id", nullable = false)
    private Long veterinarianId;

    @Column(name = "proposed_scheduled_at")
    private LocalDateTime proposedScheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VetOfferStatus status = VetOfferStatus.PENDING;

    @Column(nullable = false)
    private int round = 1;

    @Column(name = "offered_at", nullable = false)
    private LocalDateTime offeredAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (offeredAt == null) offeredAt = now;
        if (status == null) status = VetOfferStatus.PENDING;
        if (round <= 0) round = 1;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCareScheduleId() { return careScheduleId; }
    public void setCareScheduleId(Long careScheduleId) { this.careScheduleId = careScheduleId; }

    public Long getVeterinarianId() { return veterinarianId; }
    public void setVeterinarianId(Long veterinarianId) { this.veterinarianId = veterinarianId; }

    public LocalDateTime getProposedScheduledAt() { return proposedScheduledAt; }
    public void setProposedScheduledAt(LocalDateTime proposedScheduledAt) { this.proposedScheduledAt = proposedScheduledAt; }

    public VetOfferStatus getStatus() { return status; }
    public void setStatus(VetOfferStatus status) { this.status = status; }

    public int getRound() { return round; }
    public void setRound(int round) { this.round = round; }

    public LocalDateTime getOfferedAt() { return offeredAt; }
    public void setOfferedAt(LocalDateTime offeredAt) { this.offeredAt = offeredAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}