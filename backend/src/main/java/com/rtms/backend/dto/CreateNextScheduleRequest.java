package com.rtms.backend.dto;

import com.rtms.backend.enums.CareType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class CreateNextScheduleRequest {

    @NotNull(message = "Horse ID is required")
    private Long horseId;

    private Long admissionId;

    /**
     * Completed examination that authorizes a standalone follow-up request.
     * Nested completion requests derive this value from the URL schedule instead.
     */
    private Long sourceScheduleId;

    @NotNull(message = "Care type is required")
    private CareType careType;

    private String scheduledDate;
    private LocalDateTime scheduledAt;
    private String description;
    private String idempotencyKey;

    public Long getHorseId() { return horseId; }
    public void setHorseId(Long horseId) { this.horseId = horseId; }

    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }

    public Long getSourceScheduleId() { return sourceScheduleId; }
    public void setSourceScheduleId(Long sourceScheduleId) { this.sourceScheduleId = sourceScheduleId; }

    public CareType getCareType() { return careType; }
    public void setCareType(CareType careType) { this.careType = careType; }

    public String getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(String scheduledDate) { this.scheduledDate = scheduledDate; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
