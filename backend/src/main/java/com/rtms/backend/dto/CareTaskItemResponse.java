package com.rtms.backend.dto;

import com.rtms.backend.enums.CareScheduleStatus;
import com.rtms.backend.enums.CareType;
import java.time.LocalDateTime;

public class CareTaskItemResponse {
    private Long id;
    private Long horseId;
    private String horseName;
    private String stallCode;
    private CareType careType;
    private CareScheduleStatus status;
    private LocalDateTime scheduledDate;
    private String urgency; // "OVERDUE" | "TODAY" | "UPCOMING"
    private String description;
    private String veterinarianName;
    private Long veterinarianId;

    public CareTaskItemResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getHorseId() { return horseId; }
    public void setHorseId(Long horseId) { this.horseId = horseId; }
    public String getHorseName() { return horseName; }
    public void setHorseName(String horseName) { this.horseName = horseName; }
    public String getStallCode() { return stallCode; }
    public void setStallCode(String stallCode) { this.stallCode = stallCode; }
    public CareType getCareType() { return careType; }
    public void setCareType(CareType careType) { this.careType = careType; }
    public CareScheduleStatus getStatus() { return status; }
    public void setStatus(CareScheduleStatus status) { this.status = status; }
    public LocalDateTime getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDateTime scheduledDate) { this.scheduledDate = scheduledDate; }
    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getVeterinarianName() { return veterinarianName; }
    public void setVeterinarianName(String veterinarianName) { this.veterinarianName = veterinarianName; }
    public Long getVeterinarianId() { return veterinarianId; }
    public void setVeterinarianId(Long veterinarianId) { this.veterinarianId = veterinarianId; }
}
