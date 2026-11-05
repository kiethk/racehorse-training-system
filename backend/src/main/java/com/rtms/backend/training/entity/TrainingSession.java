package com.rtms.backend.training.entity;

import com.rtms.backend.training.enums.SessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Một lần nhóm ra sân trong một ngày, chạy một bài. trainerId được lặp lại từ kế hoạch để DB chặn hai buổi của cùng một
 * Trainer chồng giờ bằng một ràng buộc loại trừ.
 * (One outing of a group on one day, running one Subject. trainerId is repeated from the plan so the DB can refuse two
 * overlapping Sessions of one Trainer with an exclusion constraint.)
 */
@Entity
@Table(name = "training_sessions")
public class TrainingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_plan_id", nullable = false)
    private Long groupPlanId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "trainer_id", nullable = false)
    private Long trainerId;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.SCHEDULED;

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

    /** Giờ bắt đầu thực của buổi. (The actual start moment of the Session.) */
    public LocalDateTime startsAt() {
        return LocalDateTime.of(sessionDate, startTime);
    }

    /** Giờ kết thúc thực của buổi. (The actual end moment of the Session.) */
    public LocalDateTime endsAt() {
        return LocalDateTime.of(sessionDate, endTime);
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGroupPlanId() { return groupPlanId; }
    public void setGroupPlanId(Long groupPlanId) { this.groupPlanId = groupPlanId; }

    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }

    public Long getTrainerId() { return trainerId; }
    public void setTrainerId(Long trainerId) { this.trainerId = trainerId; }

    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
