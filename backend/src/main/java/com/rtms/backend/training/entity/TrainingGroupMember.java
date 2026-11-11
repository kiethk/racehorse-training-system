package com.rtms.backend.training.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Một chiến mã trong một nhóm. Mỗi ngựa chỉ có tối đa một dòng (một nhóm một lúc); groomId là ảnh chụp Groom của chuồng
 * lúc vào nhóm. Một Groom có thể dắt nhiều ngựa của cùng một nhóm.
 * (A horse in a group. A horse has at most one row (one group at a time); groomId is a snapshot of the stall Groom at the
 * time of joining. One Groom may lead several horses of a group.)
 */
@Entity
@Table(name = "training_group_members")
public class TrainingGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "horse_id", nullable = false)
    private Long horseId;

    @Column(name = "groom_id")
    private Long groomId;

    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public Long getHorseId() { return horseId; }
    public void setHorseId(Long horseId) { this.horseId = horseId; }

    public Long getGroomId() { return groomId; }
    public void setGroomId(Long groomId) { this.groomId = groomId; }

    public LocalDateTime getJoinedAt() { return joinedAt; }

    public LocalDateTime getLeftAt() { return leftAt; }
    public void setLeftAt(LocalDateTime leftAt) { this.leftAt = leftAt; }
}
