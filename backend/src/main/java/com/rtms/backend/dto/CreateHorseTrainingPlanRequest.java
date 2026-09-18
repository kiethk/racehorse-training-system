package com.rtms.backend.dto;

import java.time.LocalDate;
import java.util.List;

public class CreateHorseTrainingPlanRequest {
    private Long horseId;
    private Long courseId;
    private LocalDate startDate;
    // Danh sách các thứ trong tuần muốn tập: VD ["MONDAY", "WEDNESDAY", "FRIDAY"]
    private List<String> trainingDays;
    private Long assignedToId; // Người phụ trách/nài ngựa (nếu có)
    private String notes;

    public Long getHorseId() { return horseId; }
    public void setHorseId(Long horseId) { this.horseId = horseId; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public List<String> getTrainingDays() { return trainingDays; }
    public void setTrainingDays(List<String> trainingDays) { this.trainingDays = trainingDays; }

    public Long getAssignedToId() { return assignedToId; }
    public void setAssignedToId(Long assignedToId) { this.assignedToId = assignedToId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}