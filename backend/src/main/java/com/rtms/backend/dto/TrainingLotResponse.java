package com.rtms.backend.dto;

import com.rtms.backend.entity.TrainingLot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class TrainingLotResponse {

    private Long lotId;
    private LocalDate lotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Long subjectId;
    private String subjectName;
    private Integer durationMinutes;
    private String status;
    private Integer maxCapacity;
    private Integer occupied;
    private Integer remainingSlots;
    private List<String> horseNames;

    public TrainingLotResponse(TrainingLot lot, String subjectName, Integer durationMinutes,
                               int occupied, List<String> horseNames) {
        this.lotId = lot.getId();
        this.lotDate = lot.getLotDate();
        this.startTime = lot.getStartTime();
        this.endTime = lot.getEndTime();
        this.subjectId = lot.getSubjectId();
        this.subjectName = subjectName;
        this.durationMinutes = durationMinutes;
        this.status = lot.getStatus().name();
        this.maxCapacity = lot.getMaxCapacity();
        this.occupied = occupied;
        this.remainingSlots = lot.getMaxCapacity() - occupied;
        this.horseNames = horseNames;
    }

    public Long getLotId() { return lotId; }
    public LocalDate getLotDate() { return lotDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public Long getSubjectId() { return subjectId; }
    public String getSubjectName() { return subjectName; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public String getStatus() { return status; }
    public Integer getMaxCapacity() { return maxCapacity; }
    public Integer getOccupied() { return occupied; }
    public Integer getRemainingSlots() { return remainingSlots; }
    public List<String> getHorseNames() { return horseNames; }
}
