package com.rtms.backend.dto;

import com.rtms.backend.entity.TrainingLot;
import com.rtms.backend.entity.TrainingWorkout;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Workout kèm thông tin lot — vì workout không còn tự giữ ngày/giờ/bài tập.
 */
public class PlanWorkoutItemResponse {

    private Long workoutId;
    private Long lotId;
    private LocalDate lotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Long subjectId;
    private String subjectName;
    private String workoutType;
    private Long horseId;
    private Long assignedGroomId;
    private String status;

    private BigDecimal actualDistanceMeters;
    private Integer averageHeartRate;
    private Integer maxHeartRate;
    private BigDecimal topSpeedKmh;
    private Integer performanceRating;
    private String trainerFeedback;

    public PlanWorkoutItemResponse(TrainingWorkout w, TrainingLot lot, String subjectName) {
        this.workoutId = w.getId();
        this.lotId = lot.getId();
        this.lotDate = lot.getLotDate();
        this.startTime = lot.getStartTime();
        this.endTime = lot.getEndTime();
        this.subjectId = lot.getSubjectId();
        this.subjectName = subjectName;
        this.horseId = w.getHorseId();
        this.assignedGroomId = w.getAssignedToId();
        this.status = w.getStatus().name();
        this.actualDistanceMeters = w.getActualDistanceMeters();
        this.averageHeartRate = w.getAverageHeartRate();
        this.maxHeartRate = w.getMaxHeartRate();
        this.topSpeedKmh = w.getTopSpeedKmh();
        this.performanceRating = w.getPerformanceRating();
        this.trainerFeedback = w.getTrainerFeedback();
    }

    public void setWorkoutType(String workoutType) { this.workoutType = workoutType; }

    public Long getWorkoutId() { return workoutId; }
    public Long getLotId() { return lotId; }
    public LocalDate getLotDate() { return lotDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public Long getSubjectId() { return subjectId; }
    public String getSubjectName() { return subjectName; }
    public String getWorkoutType() { return workoutType; }
    public Long getHorseId() { return horseId; }
    public Long getAssignedGroomId() { return assignedGroomId; }
    public String getStatus() { return status; }
    public BigDecimal getActualDistanceMeters() { return actualDistanceMeters; }
    public Integer getAverageHeartRate() { return averageHeartRate; }
    public Integer getMaxHeartRate() { return maxHeartRate; }
    public BigDecimal getTopSpeedKmh() { return topSpeedKmh; }
    public Integer getPerformanceRating() { return performanceRating; }
    public String getTrainerFeedback() { return trainerFeedback; }
}
