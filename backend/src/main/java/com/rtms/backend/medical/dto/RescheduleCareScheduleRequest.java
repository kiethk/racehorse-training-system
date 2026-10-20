package com.rtms.backend.medical.dto;

import com.rtms.backend.medical.enums.RescheduleReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class RescheduleCareScheduleRequest {

    @NotNull(message = "newDate is required")
    private LocalDate newDate;

    @NotNull(message = "Reschedule reason is required")
    private RescheduleReason reason;

    @Size(max = 1000)
    private String note;

    public LocalDate getNewDate() { return newDate; }
    public void setNewDate(LocalDate newDate) { this.newDate = newDate; }

    public RescheduleReason getReason() { return reason; }
    public void setReason(RescheduleReason reason) { this.reason = reason; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
