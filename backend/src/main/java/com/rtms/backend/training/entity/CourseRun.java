package com.rtms.backend.training.entity;

import com.rtms.backend.config.TrainingDaySetConverter;
import com.rtms.backend.training.enums.TrainingDay;
import com.rtms.backend.training.enums.CourseRunStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Một nhóm chạy một khóa từ ngày bắt đầu trên các thứ đã chọn; đóng cùng lúc cho cả nhóm khi buổi cuối kết thúc.
 * groupId rỗng khi nhóm đã bị xóa: kế hoạch đã đóng vẫn được giữ làm lịch sử.
 * (A group running a course from a start date on the chosen weekdays; it closes for the whole group together when the last
 * Session ends. groupId is empty once the group was deleted: a closed course run is kept as history.)
 */
@Entity
@Table(name = "course_runs")
public class CourseRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "trainer_id", nullable = false)
    private Long trainerId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Convert(converter = TrainingDaySetConverter.class)
    @Column(name = "training_days", nullable = false, length = 100)
    private Set<TrainingDay> trainingDays = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseRunStatus status = CourseRunStatus.UPCOMING;

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

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public Long getTrainerId() { return trainerId; }
    public void setTrainerId(Long trainerId) { this.trainerId = trainerId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Set<TrainingDay> getTrainingDays() { return trainingDays; }
    public void setTrainingDays(Set<TrainingDay> trainingDays) { this.trainingDays = trainingDays; }

    public CourseRunStatus getStatus() { return status; }
    public void setStatus(CourseRunStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
