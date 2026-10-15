package com.rtms.backend.dto;

import java.util.List;

public class BatchCareScheduleResultResponse {
    private int total;
    private int successCount;
    private int failedCount;
    private List<BatchCareScheduleRowResult> results;

    public BatchCareScheduleResultResponse() {}

    public BatchCareScheduleResultResponse(int total, int successCount, int failedCount, List<BatchCareScheduleRowResult> results) {
        this.total = total;
        this.successCount = successCount;
        this.failedCount = failedCount;
        this.results = results;
    }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }
    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
    public List<BatchCareScheduleRowResult> getResults() { return results; }
    public void setResults(List<BatchCareScheduleRowResult> results) { this.results = results; }
}
