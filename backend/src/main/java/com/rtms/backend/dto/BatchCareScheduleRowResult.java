package com.rtms.backend.dto;

public class BatchCareScheduleRowResult {
    private Long scheduleId;
    private String status; // "SUCCESS" | "FAILED"
    private String errorMessage;
    private CareScheduleResponse schedule;

    public BatchCareScheduleRowResult() {}

    public static BatchCareScheduleRowResult success(Long scheduleId, CareScheduleResponse schedule) {
        BatchCareScheduleRowResult result = new BatchCareScheduleRowResult();
        result.setScheduleId(scheduleId);
        result.setStatus("SUCCESS");
        result.setSchedule(schedule);
        return result;
    }

    public static BatchCareScheduleRowResult failed(Long scheduleId, String errorMessage) {
        BatchCareScheduleRowResult result = new BatchCareScheduleRowResult();
        result.setScheduleId(scheduleId);
        result.setStatus("FAILED");
        result.setErrorMessage(errorMessage);
        return result;
    }

    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public CareScheduleResponse getSchedule() { return schedule; }
    public void setSchedule(CareScheduleResponse schedule) { this.schedule = schedule; }
}
