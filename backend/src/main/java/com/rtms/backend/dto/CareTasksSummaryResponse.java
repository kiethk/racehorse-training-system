package com.rtms.backend.dto;

public class CareTasksSummaryResponse {
    private long overdueCount;
    private long todayCount;
    private long thisWeekCount;
    private int complianceRate;

    public CareTasksSummaryResponse() {}

    public CareTasksSummaryResponse(long overdueCount, long todayCount, long thisWeekCount, int complianceRate) {
        this.overdueCount = overdueCount;
        this.todayCount = todayCount;
        this.thisWeekCount = thisWeekCount;
        this.complianceRate = complianceRate;
    }

    public long getOverdueCount() { return overdueCount; }
    public void setOverdueCount(long overdueCount) { this.overdueCount = overdueCount; }
    public long getTodayCount() { return todayCount; }
    public void setTodayCount(long todayCount) { this.todayCount = todayCount; }
    public long getThisWeekCount() { return thisWeekCount; }
    public void setThisWeekCount(long thisWeekCount) { this.thisWeekCount = thisWeekCount; }
    public int getComplianceRate() { return complianceRate; }
    public void setComplianceRate(int complianceRate) { this.complianceRate = complianceRate; }
}
